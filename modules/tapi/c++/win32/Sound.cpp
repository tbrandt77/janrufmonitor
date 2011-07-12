/*
*  XTAPI JTapi implementation
*  Copyright (C) 2002 Steven A. Frare
* 
*  This program is free software; you can redistribute it and/or
*  modify it under the terms of the GNU General Public License
*  as published by the Free Software Foundation; either version 2
*  of the License, or (at your option) any later version.
*  
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*  
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
 * @author  Steven A. Frare
 * @version .01
 */

#include "XTapi.h"

//Wave format is CCITT u-Law 8.000 kHz, 8 Bit, Mono CODEC
//per Microsoft Knowledge Base Article ID: Q142745
WAVEFORMATEX messageFormat = {7, 1, 8000, 8000, 1, 8, 0};

// Number of buffers for recording, keep it an even number it is divided by 
// two later on.  Our buffers can get very small, 100ms so even at 20 buffers
// which we begin half way through we only have about a second of buffer
// in the worst case scenario.
const int NUM_BUFFERS = 20;

// TAPI speak for playing / recording devices as mapped to the TAPI device.
const char * waveOut= "wave/out";	
const char * waveIn	= "wave/in";

// Internal function prototypes, see implementation for details
HWAVEOUT playSound (DWORD dw_devid, char *szWaveFile);
HWAVEIN	recordMessage (DWORD dw_devid, const char * filename, HWAVEOUT hWaveOut, DWORD out_id);
long	waveAlloc(HWAVEIN * h_wavein, WAVEHDR * waveHeader, int divisor);
HWAVEOUT openPlayDevice(DWORD device);
int		saveMessage (char *name, LPWAVEHDR hdr, unsigned long ulBytes, FILE *fptr);
int		getDivisor();

// This struct tracks the state of our wave devices.
typedef struct sound{
	bool b_playData;	// finished playing a segment of data
	bool b_recDone;		// Stop recording
	bool b_recData;		// got a segment of data
	bool b_play_Rec;	// this device is busy playing or recording
	bool b_useDefault;	// We are using the default line (mic or speakers)
	WAVEHDR * recHeader;// header to the last recording data we got
	WAVEHDR * playHeader;// header to the last playing data we got
}t_sound;

t_sound * m_devices = NULL;

/*
 * initSound(int num_device	// The number of lines initialized by TAPI.
 * 
 * Initialize the m_devices array to track the state of all wave devices
 * that we may potentially use.
 *
 */
void initSound(int num_devices)
{
	// Over allocate by one since we use the wave id of the device
	// and for useDefaultSpeaker and useDefaultMicrophone that id is the
	// Microsoft MAPPER device which is -1 so we always use (device_id + 1).
	// If we have a full-duplex wave device this may need to be twice as 
	// large so go ahead and allocate for that.
	m_devices = new t_sound[(num_devices * 2) + 1];
}

/*
 * Release any allocated resources.
 */
void teardownSound()
{
	delete [] m_devices;
	m_devices = NULL;
}

/*
 * getWaveID(HLINE	// Handle to the TAPI line
 *			 device	// one of waveOut or waveIn constants defined above.
 * Helper function to get the Wave device ID that correlates to the TAPI line.
 */
long getWaveID(HLINE hLine, const char * device)
{
	DWORD dwWaveDev;
	VARSTRING  *vs;
	LONG lrc;
	DWORD dwSize;

	// allocate memory for structure
    vs = (VARSTRING *) calloc (1, sizeof(VARSTRING));
	// set structure size
    vs->dwTotalSize = sizeof(VARSTRING);
    do {
		// get information into structure
        lrc = lineGetID(hLine, 0L, 0, LINECALLSELECT_LINE, vs, device);
		// bomb out if error
		if (lrc)  {
			free (vs);
			return -1;
		}
		// reallocate and try again
		if (vs->dwTotalSize < vs->dwNeededSize) {
                dwSize = vs->dwNeededSize;
                free (vs);
                vs = (VARSTRING *) calloc(1, dwSize);
                vs->dwTotalSize = dwSize;
				continue;
        } /* end if (need more space) */
        break; /* success  */
    } while (TRUE);

	// copy wave id
    dwWaveDev = (DWORD) *((DWORD *)((LPSTR)vs + vs->dwStringOffset));
    free (vs);

	char szMessage[256];
	wsprintf(szMessage, "%s -> %d",device,dwWaveDev);
	debugString(8,"getWaveID",szMessage,_where());

	return dwWaveDev;
}
/**
 *  Record sound to a file or render the sound to the speakers.
 *
 *  recordSound(JNIEnv		pointer to the JNI Environment
 *				oObj		the java object that called down the JNI layer
 *				oFileName	The file to save the recording to
 *				lLine		The line handle.
 *
 * oFileName may be NULL if that is the case we just render the sound
 * straight to the speakers and do not record it.  There is no facility for
 * doing both (recording to a file and playing out the speakers)
 */
JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_recordSound
  (JNIEnv *pEnv, jobject oObj, jstring oFileName, jint lLine)
{
	const char * szFile = NULL;
	jboolean isCopy ;
	long id;
	HWAVEOUT hWaveOut = NULL;

	hWaveOut = NULL;

	if(oFileName != NULL)
		szFile = pEnv->GetStringUTFChars(oFileName,&isCopy);

	if(lLine != -1)
		id = getWaveID((HLINE)lLine,waveIn);
	else
		id = -1;

	m_devices[id + 1].b_play_Rec = true;
	m_devices[id + 1].b_useDefault = false;

	if(szFile == NULL)
	{
		hWaveOut = openPlayDevice(-1);

		if(hWaveOut == NULL)
		{
			m_devices[id + 1].b_play_Rec = false;
			m_devices[id + 1].b_useDefault = false;
			return 0;
		}

		m_devices[id + 1].b_useDefault = true;
	}

	long lRc = (long)recordMessage(id,szFile,hWaveOut, -1);

	if(JNI_TRUE == isCopy) {
		pEnv->ReleaseStringUTFChars(oFileName,szFile);
	}

	if(hWaveOut != NULL)
	{
		char szMsg[256];
		lRc = waveOutReset(hWaveOut);
		if(lRc != MMSYSERR_NOERROR)
		{
			sprintf(szMsg, "waveOutReset error %d",lRc);
			debugString(1,"recordSound",szMsg,_where());
		}
		lRc = waveOutClose(hWaveOut);
		if(lRc != MMSYSERR_NOERROR)
		{
			sprintf(szMsg, "waveOutClose error %d",lRc);
			debugString(1,"recordSound",szMsg,_where());
		}
	}

	m_devices[id + 1].b_play_Rec = false;
	m_devices[id + 1].b_useDefault = false;

	return 1;
}

/**
 *  Play sound from a file or use the default microphone as the source.
 *
 *  recordSound(JNIEnv		pointer to the JNI Environment
 *				oObj		the java object that called down the JNI layer
 *				oFileName	The file to save the recording to
 *				lLine		The line handle.
 *
 * oFileName may be NULL since we may be rendering from the microphone.
 * If that is the case we just take the sound from the microphone and
 * stream it through TAPI.  
 */
JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_playSound
  (JNIEnv *pEnv, jobject oObj, jstring oFileName, jint lLine)
{
	const char * szFile = NULL;
	jboolean isCopy ;
	long id;
	long lRc = 0;
	HWAVEOUT hWaveOut = NULL;

	if(lLine != -1)
		id = getWaveID((HLINE)lLine,waveOut);
	else
		id = -1;

	m_devices[id + 1].b_useDefault = false;
	m_devices[id + 1].b_play_Rec = true;

	if(oFileName != NULL){
		szFile = pEnv->GetStringUTFChars(oFileName,&isCopy);
		lRc = (long)playSound(id,(char *)szFile);
	}
	else
	{
		hWaveOut = openPlayDevice(id);

		if(hWaveOut == NULL){
			m_devices[id + 1].b_play_Rec = false;
			m_devices[id + 1].b_useDefault = false;
			return 0;
		}

		m_devices[id + 1].b_useDefault = true;

		lRc = (long)recordMessage(-1,szFile, hWaveOut, id);

		if(hWaveOut != NULL)
		{
			char szMsg[256];

			lRc = waveOutReset(hWaveOut);
			if(lRc != MMSYSERR_NOERROR)
			{
				sprintf(szMsg, "waveOutReset error %d",lRc);
				debugString(1,"playSound",szMsg,_where());
			}

			lRc = waveOutClose(hWaveOut);
			if(lRc != MMSYSERR_NOERROR)
			{
				sprintf(szMsg, "waveOutClose error %d",lRc);
				debugString(1,"playSound",szMsg,_where());
			}
		}

	}

	if(JNI_TRUE == isCopy) {
		pEnv->ReleaseStringUTFChars(oFileName,szFile);
	}

	m_devices[id + 1].b_play_Rec = false;
	m_devices[id + 1].b_useDefault = false;

	return lRc;
}


void CALLBACK waveOutProc(HWAVEOUT hwo, UINT uMsg, DWORD dwInstance,  DWORD dwParam1, DWORD dwParam2)
{	
	WAVEHDR * waveHeader;
	if(uMsg == WOM_DONE)
	{
		waveHeader = (WAVEHDR*)dwParam1;
		m_devices[waveHeader->dwUser + 1].b_playData = true;
		m_devices[waveHeader->dwUser + 1].playHeader = (WAVEHDR*)dwParam1;

	}
}

void CALLBACK waveInProc(HWAVEIN hwi, UINT uMsg, DWORD dwInstance,  DWORD dwParam1, DWORD dwParam2)
{
	WAVEHDR * waveHeader;
	if(uMsg == WIM_DATA)
	{
			waveHeader = (WAVEHDR*)dwParam1;
			m_devices[waveHeader->dwUser + 1].b_recData = true;
			m_devices[waveHeader->dwUser + 1].recHeader = (WAVEHDR*)dwParam1;
	}
}

/**
 * Given a device id and a path to a wave file 
 * (preferably CCITT u-Law 8kHz 8 bit Mono encoded)
 * plays the wave file out the device.
 */

HWAVEOUT playSound (DWORD dw_devid, char *szWaveFile)
{
HWAVEOUT        hWave;
HMMIO           hmmio;
MMCKINFO        mmckinfoParent;
MMCKINFO        mmckinfoSubchunk;
DWORD           dwFmtSize;
DWORD           dwDataSize;
HPSTR           lpWaveData;
WAVEHDR *		lpWaveHdr;
WAVEFORMATEX *	lpWaveFormat;

char			szMsg[256];

	debugString(8,"playSound","Entered playSound",_where());
    
    // Open wave file 
    hmmio = mmioOpen(szWaveFile, NULL, MMIO_READ | MMIO_ALLOCBUF);
    if(!hmmio)
    {
		debugString(1,"playSound","Failed on mmioOpen",_where());
        return 0;
    }

    // Locate a 'RIFF' chunk with a 'WAVE' form type 
    mmckinfoParent.fccType = mmioFOURCC('W', 'A', 'V', 'E');
    if (mmioDescend(hmmio, (LPMMCKINFO) &mmckinfoParent, NULL, MMIO_FINDRIFF))
    {
		debugString(1,"playSound",
			"Failed to find the WAVE form type.",_where());
        mmioClose(hmmio, 0);
        return 0;
    }
    
    // Find the format chunk 
    mmckinfoSubchunk.ckid = mmioFOURCC('f', 'm', 't', ' ');
    if (mmioDescend(hmmio, &mmckinfoSubchunk, &mmckinfoParent, MMIO_FINDCHUNK))
    {
		debugString(1,"playSound","Failed to find the format chunk.",_where());
        mmioClose(hmmio, 0);
        return 0;
    }

    // Get the size of the format chunk, allocate memory for it 
    dwFmtSize = mmckinfoSubchunk.cksize;
    lpWaveFormat = (WAVEFORMATEX *) calloc (1, dwFmtSize);
    if (!lpWaveFormat)
    {
		debugString(1,"playSound","Failed on lpWaveFormat",_where());
        mmioClose(hmmio, 0);
        return 0;
    }

    // Read the format chunk 
    if (mmioRead(hmmio, (LPSTR) lpWaveFormat, dwFmtSize) != (LONG) dwFmtSize)
    {
		debugString(1,"playSound","Failed to read the format chunk.",_where());
        free( lpWaveFormat );
        mmioClose(hmmio, 0);
        return 0;
    }
    
    // Ascend out of the format subchunk 
    mmioAscend(hmmio, &mmckinfoSubchunk, 0);
    
    // Find the data subchunk 
    mmckinfoSubchunk.ckid = mmioFOURCC('d', 'a', 't', 'a');
    if (mmioDescend(hmmio, &mmckinfoSubchunk, &mmckinfoParent, MMIO_FINDCHUNK))
    {
		debugString(1,"playSound","Failed to find the data chunk.",_where());
        free( lpWaveFormat );
        mmioClose(hmmio, 0);
        return 0;
    }
 
    // Get the size of the data subchunk 
    dwDataSize = mmckinfoSubchunk.cksize;
    if (dwDataSize == 0L)
    {
		debugString(1,"playSound","Failed on dwDataSize",_where());
        free( lpWaveFormat );
        mmioClose(hmmio, 0);
        return 0;
    }
        
    // Allocate and lock memory for the waveform data. 
    lpWaveData = (LPSTR) calloc (1, dwDataSize );
    if (!lpWaveData)
    {
		debugString(1,"playSound","Failed on lpWaveData",_where());
        free( lpWaveFormat );
        mmioClose(hmmio, 0);
        return 0;
    }

    // Read the waveform data subchunk 
    if(mmioRead(hmmio, (HPSTR) lpWaveData, dwDataSize) != (LONG) dwDataSize)
    {
		debugString(1,"playSound","Failed on mmioRead2",_where());
        free(lpWaveFormat);
        free(lpWaveData);
        mmioClose(hmmio, 0);
        return 0;
    }

    // We're done with the file, close it 
    mmioClose(hmmio, 0);

	debugString(8,"playSound","Closed the file",_where());

    // Allocate a waveform data header 
    lpWaveHdr = (WAVEHDR *) calloc (1,(DWORD)sizeof(WAVEHDR));
    if (!lpWaveHdr)
    {
		debugString(1,"playSound",
			"Failed on Allocate a waveform data header",_where());
        free(lpWaveData);
        free(lpWaveFormat);
        return 0;
    }

    // Set up WAVEHDR structure and prepare it to be written to wave device 
    lpWaveHdr->lpData = lpWaveData;
    lpWaveHdr->dwBufferLength = dwDataSize;
    lpWaveHdr->dwFlags = 0L;
    lpWaveHdr->dwLoops = 0L;
    lpWaveHdr->dwUser  = dw_devid;

    // make sure wave device can play our format 
    if (waveOutOpen((LPHWAVEOUT)NULL,
                    (WORD)dw_devid,
                    (LPWAVEFORMATEX)lpWaveFormat, 
                    0L, 0L, WAVE_FORMAT_QUERY | WAVE_MAPPED)) {
        free( lpWaveFormat );
        free(lpWaveData);
        free(lpWaveHdr);
		debugString(1,"playSound","Unsupported wave format.",_where());
        return 0;
    }

    // open the wave device corresponding to the line 
    if (long lErr = waveOutOpen (    &hWave, 
                        (WORD)dw_devid, 
                        (LPWAVEFORMATEX)lpWaveFormat, 
                        (DWORD)waveOutProc, 
                        0L, 
                        CALLBACK_FUNCTION | WAVE_MAPPED))

    {
        free(lpWaveFormat);
        free(lpWaveData);
        free(lpWaveHdr);

		switch(lErr)
		{
		case MMSYSERR_ALLOCATED:
			debugString(1,"playSound","MMSYSERR_ALLOCATED",_where());
			break;

		case MMSYSERR_BADDEVICEID:
			debugString(1,"playSound","MMSYSERR_BADDEVICEID",_where());
			break;

		case MMSYSERR_NODRIVER:
			debugString(1,"playSound","MMSYSERR_NODRIVER",_where());
			break;

		case MMSYSERR_NOMEM:
			debugString(1,"playSound","MMSYSERR_NOMEM",_where());
			break;

		case WAVERR_BADFORMAT:
			debugString(1,"playSound","WAVERR_BADFORMAT",_where());
			break;

		case WAVERR_SYNC:
			debugString(1,"playSound","WAVERR_SYNC",_where());
			break;

		default:
			debugString(1,"playSound","Error not defined",_where());
		}
		debugString(1,"playSound","Error opening wave device.",_where());
		wsprintf(szMsg,"lErr is %d device id -> %d", lErr, dw_devid);
		debugString(1,"playSound",szMsg,_where());
        return 0;
    }
  
    free( lpWaveFormat );

    // prepare the message header for playing
    if (waveOutPrepareHeader (hWave, lpWaveHdr, sizeof(WAVEHDR))) {
		debugString(1,"playSound","Error preparing message header.",_where());
        free(lpWaveData);
        free(lpWaveHdr);
        return 0;
    }

    // play the message right from the data segment;  set the play message flag
    if (waveOutWrite (hWave, lpWaveHdr, sizeof (WAVEHDR))) {
		debugString(1,"playSound","Error writing wave message.",_where());
        free(lpWaveData);
        free(lpWaveHdr);
        return 0;
    }

	m_devices[dw_devid + 1].b_playData = false;

	while(m_devices[dw_devid + 1].b_playData == false)
	{
		Sleep(20);
	}

	MMRESULT  res;

	res = waveOutReset(hWave);

	if(res != MMSYSERR_NOERROR )
		debugString(1,"playSound","Error from waveOutReset",_where());

	res = waveOutClose(hWave);

	if(res != MMSYSERR_NOERROR )
		debugString(1,"playSound","Error from waveOutClose",_where());

    free (lpWaveData); // free the wave data 
    free (lpWaveHdr); // free the header 

	debugString(8,"playSound","returning OK",_where());

    return hWave;

} // end function (play message) 

/**
 *  Stop playing sound
 *
 *  stopPlaying(JNIEnv		pointer to the JNI Environment
 *			    oObj		the java object that called down the JNI layer
 *				lHandle		The line handle.
 *
 * We want to stop playing a file or stop streaming to the speakers.
 * The routines to play / record always check a data flag and a done flag
 * so we simply set them to true to end the loop.
 */
JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_stopPlaying
  (JNIEnv * pEnv, jobject oObj, jint lHandle)
{

	long id;

	/** lHandle should not ever be -1 */
	if(lHandle != -1)
		id = getWaveID((HLINE)lHandle,waveIn);
	else
		id = -1;

	/** 
	 *  If we are streaming to the speakers we are recording from the line
	 *  and playing out the speakers.  So we need to stop both streams.
	 *  If b_useDefault == true then we are streaming to the speakers.
	 */
	if(m_devices[id + 1].b_useDefault == true)
	{
		m_devices[0].b_playData = true;
		m_devices[0].b_recDone = true;
		debugString(4,"stopPlaying","shutting down default",_where());
	}

	m_devices[id + 1].b_playData = true;
	m_devices[id + 1].b_recDone = true;

	char szMsg[256];
	wsprintf(szMsg,"stopping wave device %d",id);
	debugString(4,"stopPlaying",szMsg,_where());

	// If another thread is playing/recording give it a chance to check
	// the flags
	while(m_devices[id + 1].b_play_Rec == true)
		Sleep(40);

	Sleep(100);

	return 0;
}

/*
 * We have a chunk of wave data we need to write.  We are either writing
 * to a file or straight to a sound device so either FILE or HWAVEOUT will
 * be NULL.
 */
void writeRecData(WAVEHDR * hdr, FILE * fptr, HWAVEOUT hWaveOut, DWORD dev_id)
{

	if(NULL == fptr)
	{
		// We are rendering directly to the speakers or from the mic out the
		// TAPI line, not saving to a file so 
		// setup a WAVEHDR.struct.  All we need is a pointer to the data and
		// the size of that data which we can get of the WAVEHDR passed in.
		WAVEHDR * lpWaveHdr;

		lpWaveHdr = (WAVEHDR *) calloc (1,(DWORD)sizeof(WAVEHDR));

		lpWaveHdr->lpData = hdr->lpData;
		lpWaveHdr->dwBufferLength = hdr->dwBytesRecorded;
		lpWaveHdr->dwFlags = 0L;
		lpWaveHdr->dwLoops = 0L;
		//lpWaveHdr->dwUser  = (long)lpWaveHdr;	// TODO: Uneeded
		lpWaveHdr->dwUser  = dev_id;

		// Prepare the header
		if (waveOutPrepareHeader (hWaveOut, lpWaveHdr, sizeof(WAVEHDR))) {
			debugString(1,"writeRecData",
				"Error preparing message header.",_where());
		}

		// Render it
		// Once written to the device we will get a WIM_DATA message back when 
		// this segment is done playing.  There we set the m_devices[id] variable 
		// playHeader in the call back procedure waveOutProc which also sets
		// the flag bPlayData signaling us to free the memory we just allocated 
		// for the header.  We free it in the loop we called this procedure from.

		if (waveOutWrite (hWaveOut, lpWaveHdr, sizeof (WAVEHDR))) {
			debugString(1,"writeRecData","waveOutWrite error.",_where());
		}
	}
	else
	{
		fwrite(hdr->lpData,hdr->dwBytesRecorded,1,fptr);
	}

}
/**
 * recordMessage.  
 * Given a device id, filename OR handle to an open wave output device
 * streams audio to that file or device.
 *
 *  Uses a circular buffer for unlimited recording / rendering time
 */

HWAVEIN recordMessage (DWORD dw_devid, const char * filename, HWAVEOUT hWaveOut, DWORD out_id)
{
LONG			lrc;					// C function return code
unsigned long	ulBytesRecorded = 0;	// Number of bytes recorded
int				iLoop;					// tracks loop for circular buffer
unsigned long	ulFlags;				// waveInOpen flags differ for device
WAVEHDR			t_inMessageHeader[NUM_BUFFERS];		// Circular buffer
FILE			*fptr = NULL;			// File pointer
char			szTempFileName[MAX_PATH + 1];	// File name buffer
char			szDebug[256];			// Debug string buffer
HWAVEIN			hWavein;				// Handle to wave in device

	if(filename != NULL)
	{
		// We are recording to a file.  We stream to a temp file then
		// write the data to a standard wave file when we are done.

		// Windows will give us a unique file name.
		GetTempFileName(".","XTP",1,szTempFileName);
		fptr = fopen(szTempFileName,"w+b");
	}

	if(dw_devid == -1)
	{
		// The default WAVE_MAPPER device dislikes the WAVE_MAPPED flag.
		ulFlags = CALLBACK_FUNCTION;
	}
	else
	{
		// Modems generally don't work without the WAVE_MAPPED flag...
		ulFlags = CALLBACK_FUNCTION | WAVE_MAPPED;
	}

	 // Open the wave device for recording

	 if (lrc=waveInOpen( &hWavein, dw_devid, &messageFormat,
					 (DWORD)waveInProc, 0, ulFlags )) 
	 {
		 debugString(1,"recordMessage",
			 "Error opening wave record device.",_where());

		switch(lrc)
		{
		case MMSYSERR_ALLOCATED:
			debugString(1,"recordMessage","MMSYSERR_ALLOCATED",_where());
			break;

		case MMSYSERR_BADDEVICEID:
			debugString(1,"recordMessage","MMSYSERR_BADDEVICEID",_where());
			break;

		case MMSYSERR_NODRIVER:
			debugString(1,"recordMessage","MMSYSERR_NODRIVER",_where());
			break;

		case MMSYSERR_NOMEM:
			debugString(1,"recordMessage","MMSYSERR_NOMEM",_where());
			break;

		case WAVERR_BADFORMAT:
			debugString(1,"recordMessage","WAVERR_BADFORMAT",_where());
			break;

		case WAVERR_SYNC:
			debugString(1,"recordMessage","WAVERR_SYNC",_where());
			break;

		default:
			debugString(1,"recordMessage","Error not defined",_where());
		}
		 return 0;
	 }

	 // We successfully opened the wave device!
	 debugString(8,"recordMessage","Opened device",_where());

	// Setup our circular buffer.  We will add 1/2 of the buffers
	// to the wave device, the others get added on as needed.

	// Get the divisor value.
	int iDiv = getDivisor();

	for(iLoop = 0; iLoop < NUM_BUFFERS / 2; iLoop++)
	{
		memset(&t_inMessageHeader[iLoop],0,sizeof(WAVEHDR));
		waveAlloc(&hWavein, &t_inMessageHeader[iLoop], iDiv);
		t_inMessageHeader[iLoop].dwUser = dw_devid;
		waveInAddBuffer (hWavein, 
						&t_inMessageHeader[iLoop], 
						sizeof(WAVEHDR));
	}

	// We still still to prepare the headers we didn't add to
	// the wave device.
	for(iLoop = NUM_BUFFERS / 2 ; iLoop < NUM_BUFFERS; iLoop++)
	{
		memset(&t_inMessageHeader[iLoop],0,sizeof(WAVEHDR));
		waveAlloc(&hWavein, &t_inMessageHeader[iLoop], iDiv);
		t_inMessageHeader[iLoop].dwUser = dw_devid;
	}

	// Start recording
	waveInStart (hWavein);

	m_devices[dw_devid + 1].b_recDone = false;
	m_devices[dw_devid + 1].b_recData = false;
	unsigned long numLoops = NUM_BUFFERS / 2;	// Start in the middle of the array

	/*
	 * We cyle through an array of pre-allocated wave headers.
	 * With the requirement to use JTapi as a speakerphone we
	 * need small buffers (1000ms) so we don't have a large delay
	 * however we need enough buffer time so that we can be 
	 * interrupted by the system without choking so we use
	 * NUM_BUFFERS which is as I write this is 20 (subject to change..)
	 * and upfront we feed in half the array to get us started then as
	 * buffers are used we continue to add them in a circular fashion.
	 */
	while(m_devices[dw_devid + 1].b_recDone == false)
	{
		if(m_devices[out_id + 1].b_playData == true)
		{
			if(filename == NULL)	
			{
				// We are rendering to the speakers as opposed to writing to a file
				// so we need to clean up the wave headers as we go.
				
				long lRet = waveOutUnprepareHeader(hWaveOut,
								m_devices[out_id + 1].playHeader,
								sizeof(WAVEHDR));
				if(lRet != 0)
				{
					switch(lRet)
					{
					case MMSYSERR_INVALHANDLE:
						debugString(1,"recordMessage",
							"waveOutUnprepareHeader MMSYSERR_INVALHANDLE",_where());
						break;

					case MMSYSERR_NODRIVER:
						debugString(1,"recordMessage",
							"waveOutUnprepareHeader MMSYSERR_NODRIVER",_where());
						break;

					case MMSYSERR_NOMEM:
						debugString(1,"recordMessage",
							"waveOutUnprepareHeader MMSYSERR_NOMEM",_where());
						break;

					case WAVERR_STILLPLAYING:
						debugString(1,"recordMessage",
							"waveOutUnprepareHeader WAVERR_STILLPLAYING",_where());
						break;

					default:
						sprintf(szDebug,"waveOutUnprepareHeader error %d", lRet);
						debugString(1,"recordMessage",szDebug,_where());
						break;
					}
				}
				else
				{
					// We didn't get an error during waveOutUnprepareHeader 
					// so assume it is safe to free the header.  
					// NOTE:  
					// Just frees header, does not free data, we always 
					// reuse the allocated data space and only free it at the
					// end of this procedure.
					free(m_devices[out_id + 1].playHeader);
				}

				m_devices[out_id + 1].b_playData = false;
			}
		}

		/**
		 *  We have data so we need to save the data chunk and allow this piece
		 *  of allocated memory to be flagged for re-use.
		 */
		if(m_devices[dw_devid + 1].b_recData == true)
		{
			ulBytesRecorded += m_devices[dw_devid + 1].recHeader->dwBytesRecorded;

			writeRecData(m_devices[dw_devid + 1].recHeader,fptr, hWaveOut,out_id);

			long lRc;

			lRc = waveInAddBuffer (hWavein, 
									&t_inMessageHeader[numLoops%NUM_BUFFERS],
									sizeof(WAVEHDR));
			if(lRc != 0)
			{
				switch(lRc)
				{
				case MMSYSERR_INVALHANDLE:
					debugString(1,"recordMessage",
						"waveInAddBuffer MMSYSERR_INVALHANDLE",_where());
					break;

				case MMSYSERR_NODRIVER:
					debugString(1,"recordMessage",
						"waveInAddBuffer MMSYSERR_NODRIVER",_where());
					break;

				case MMSYSERR_NOMEM:
					debugString(1,"recordMessage",
						"waveInAddBuffer MMSYSERR_NOMEM",_where());
					break;

				case WAVERR_UNPREPARED:
					debugString(1,"recordMessage",
						"waveInAddBuffer WAVERR_UNPREPARED",_where());
					break;

				default:
					sprintf(szDebug,"waveInAddBuffer = %d\n",lRc);
					debugString(1,"recordMessage",szDebug,_where());
					break;
				}
				
			}

			// increment loop counter to track header array for reuse.
			numLoops++;	
			m_devices[dw_devid + 1].b_recData = false;

		}
		else
		{
			// We don't have anything to do so don't use 100% of the CPU
			// servicing this loop.
			// Sleep(0) SHOULD give up our current time slice however on
			// a multi-proc system we still use 100% of the CPU so we sleep
			// for an arbritrary amount of time.
			Sleep(20);
		}
	}

	if(fptr != NULL)
	{
		// We aren't rendering to the speakers or a TAPI line so save the 
		// temp file to a standard wave file.
		fflush(fptr);
		saveMessage((char*)filename, m_devices[dw_devid + 1].recHeader, 
					ulBytesRecorded, fptr);
		fclose(fptr);

		// Delete the temp file.
		DeleteFile(szTempFileName);
	}

	// Reset wave device 
	MMRESULT res;

	res = waveInReset( hWavein );

	if(res != MMSYSERR_NOERROR)
	{
		wsprintf(szDebug,"waveInReset Error -> %d\n",res);
		debugString(1,"recordMessage",szDebug,_where());
		return hWavein;
	}


	// Cleanup the header array
	for(iLoop = 0; iLoop < NUM_BUFFERS; iLoop++)
	{
		waveInUnprepareHeader (hWavein, &t_inMessageHeader[iLoop], sizeof(WAVEHDR));
		GlobalFree (t_inMessageHeader[iLoop].lpData); //free the data buffer 
	}
	
	// Close wave device 
	waveInClose (hWavein); 

	hWavein = NULL;

	return hWavein;
    
} /* end function (record message) */

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_stopRecording
  (JNIEnv *pEnv, jobject oObj, jint lHandle)
{
	long id;

	if(lHandle != -1)
		id = getWaveID((HLINE)lHandle,waveIn);
	else
		id = -1;

	if(m_devices[id + 1].b_useDefault == true)
	{
		m_devices[0].b_playData = true;
		m_devices[0].b_recDone = true;
		debugString(4,"stopRecording","shutting down default",_where());
	}

	char szMsg[256];
	wsprintf(szMsg,"stopping wave device %d",id);
	debugString(4,"stopRecording",szMsg,_where());
	m_devices[id + 1].b_recDone = true;


	// If another thread is playing/recording give it a chance to check
	// the flags
	while(m_devices[id + 1].b_play_Rec == true)
		Sleep(40);

	Sleep(100);

	return 0;
}

/*
 * Allocate and prepare a wave header.  The smaller dw_bufsz the less latency
 * for a speaker phone.  On a Dialogic 160SC the smallest I could set is
 * 1 second, lower than that and sound was lost!
 */
long waveAlloc(HWAVEIN * h_wavein, WAVEHDR * waveHeader, int divisor)
{
DWORD dw_bufsz = (1L * (long)messageFormat.nSamplesPerSec * 
                            (long)messageFormat.wBitsPerSample/8L) / divisor;
HANDLE hData;
long lrc;


	 // Allocate the message buffer
	 hData = GlobalAlloc (GPTR, dw_bufsz);
	 if ((waveHeader->lpData = (char *)GlobalLock(hData)) == NULL) {
		 debugString(1,"saveMessage","Not enough memory.",_where());
		 return 0;
	 }

	 memset(hData,0,dw_bufsz);
 
	 waveHeader->dwBufferLength = dw_bufsz;
	 if (lrc=waveInPrepareHeader(*h_wavein, waveHeader, sizeof(WAVEHDR))) {
		 GlobalFree (hData);
		 debugString(1,"saveMessage","Error preparing message header.",_where());
		 return 0;
	 }

	 waveHeader->dwUser = (DWORD) hData;
	 
	return 0;
}

/****************************************************************************
    FUNCTION: saveMessage
    PURPOSE:  save a recorded voice message
****************************************************************************/
// save the message after the caller hangs up, or after 60s 
int saveMessage (char *name, LPWAVEHDR hdr, unsigned long ulBytes, FILE * fptr)
{
HANDLE h_file;
unsigned long n_written;

// WAVE header constants.

// RIFF chunk
BYTE ab_riffchunk[] = {'R','I','F','F',  0,0,0,0, 'W','A','V','E'};
// Format chunk tag 
BYTE ab_formatchunktag[] = {'f','m','t',' ',  0,0,0,0};
// Data chunk header
BYTE ab_datachunktag[] = {'d','a','t','a',  0,0,0,0};

    h_file = CreateFile (name, GENERIC_READ|GENERIC_WRITE, 0, 
    					NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL,NULL);
    if (h_file == NULL) {
		debugString(1,"saveMessage",strerror( errno ),_where());
        return -1;
    }
  
    // write out the RIFF chunk 
    *((DWORD *)&ab_riffchunk[4])=4 + sizeof(ab_formatchunktag) + 
                                 sizeof(messageFormat) + sizeof(ab_datachunktag) + 
								 ulBytes;

    WriteFile (h_file, ab_riffchunk, sizeof(ab_riffchunk), &n_written, NULL);
    *((DWORD *)&ab_formatchunktag[4]) = sizeof(messageFormat); /* write  tag */
    WriteFile (h_file, ab_formatchunktag, sizeof(ab_formatchunktag), &n_written, NULL);

    // write out the canned format header 
    WriteFile (h_file, &messageFormat, sizeof(messageFormat), &n_written, NULL);

    // write out the data chunk tag 
    *((DWORD *)&ab_datachunktag[4]) = ulBytes;
    WriteFile (h_file, ab_datachunktag, sizeof(ab_datachunktag), &n_written, NULL);

    // write out the data chunk 
	// Seek to the beginning of the temp file.
	int result = fseek(fptr,0,SEEK_SET);

	if(result)
	{
		debugString(1,"saveMessage",strerror( errno ),_where());
		return 1;
	}

	// Use a 100K buffer to copy the raw data to the wave file data section
	const unsigned long buffsize = 10 * 10 * 1024;

	LPSTR buff = (LPSTR)malloc(buffsize);

	int iRead;

	while(true)
	{
		iRead = fread(buff,1, buffsize, fptr);

		if(iRead > 0)
			WriteFile(h_file,buff,iRead,&n_written,NULL);
		else
			break;
	}

	free(buff);

    CloseHandle (h_file); // close message file 
    return 0;

} // end function saveMessage

/* 
 * If we are rendering to a device such as the speakers or using the 
 * microphone and streaming out to the TAPI line we open the TAPI
 * wave device with this function.
 *
 * NOTE: Probably should call this from playFile()
 *
 */
HWAVEOUT openPlayDevice(DWORD device)
{
long lErr;
unsigned long ulFlags;
char szDebug[256];
HWAVEOUT hWaveOut;

	if(device == -1)
		ulFlags = CALLBACK_FUNCTION;
	else
		ulFlags = CALLBACK_FUNCTION | WAVE_MAPPED;

    /* open the wave device */
    if (lErr = waveOutOpen (&hWaveOut, 
                        device, 
                        (LPWAVEFORMATEX)&messageFormat, 
                        (DWORD)waveOutProc, 
                        0L, 
                        ulFlags))
	{
		switch(lErr)
		{
		case MMSYSERR_ALLOCATED:
			debugString(1,"openPlayDevice","MMSYSERR_ALLOCATED",_where());
			break;

		case MMSYSERR_BADDEVICEID:
			debugString(1,"openPlayDevice","MMSYSERR_BADDEVICEID",_where());
			break;
			
		case MMSYSERR_NODRIVER:
			debugString(1,"openPlayDevice","MMSYSERR_NODRIVER",_where());
			break;

		case MMSYSERR_NOMEM:
			debugString(1,"openPlayDevice","MMSYSERR_NOMEM",_where());
			break;
		case WAVERR_BADFORMAT:
			debugString(1,"openPlayDevice","WAVERR_BADFORMAT",_where());
			break;

		case WAVERR_SYNC:
			debugString(1,"openPlayDevice","WAVERR_SYNC",_where());
			break;

		default:
			sprintf(szDebug,"failed to open output wave device % d\n", lErr);
			debugString(1,"openPlayDevice",szDebug,_where());
		}

		return NULL;
	}

	return hWaveOut;
}

/*
 * When streaming audio (using getDefaultXXXX) it is preferable to have smaller
 * buffers.  However buffers smaller than 1 second do not work correctly on some
 * cards (Dialogic 160SC).  The default buffer size is one second, add a divisor
 * DWORD value in HKEY_LOCAL_MACHINE\SOFTWARE\xtapi to divide one second/divisor.
 *
 * Setting the value to 5 results in 200ms buffers.
 * 
 * HKEY_LOCAL_MACHINE\SOFTWARE\xtapi
 * Name:	divisor
 * Value	DWORD >= 1 <= 10
 */
int getDivisor()
{

	long lRes;
	HKEY hKey;
	DWORD dwSize = 10;
	unsigned char szValue[10];
	unsigned long ulType = REG_DWORD;
	DWORD dwValue = 1;

	lRes = RegOpenKeyEx(HKEY_LOCAL_MACHINE,         // handle to open key
						"SOFTWARE\\xtapi",			// subkey name
						0,							// reserved
						KEY_QUERY_VALUE,			// security access mask
						&hKey);						// handle to open key

	if(lRes == ERROR_SUCCESS)
	{
		lRes = RegQueryValueEx( hKey, "divisor", NULL, &ulType,
				szValue, &dwSize);

		if(lRes == ERROR_SUCCESS)
		{
			dwValue = (DWORD)szValue;

			if(dwValue < 1)
				dwValue = 1;

			if(dwValue > 10)
				dwValue = 10;
		}

		RegCloseKey(hKey);
	}

	return dwValue;
}
