// Revision 8.01 2002-07-15 20:30:49 reggiehg
// Changed MAXWAITCALL_MS from 3000 to 5000 to give TAPI more time to 
// asynchronously 
// return a valid hCall (call handle) (for slower modems).
// Added Sleep(1000) between lineDrop and lineDeallocateCall to give TAPI more 
// time
// to asynchronously return (for serial modems).

// 07-18-2002	sfrare	Removed Sleep and moved lineDeallocateCall to 
//						be done automatically in response to 
//						LINECALLSTATE_IDLE message.
//				

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

#define TAPIVERSION		0x00010004

#include <sys/timeb.h>						// ANSI Standard
#include <time.h>
#include <process.h>

#include "XTapi.h"

//The Java objects from initialization.
JNIEnv		*g_env = NULL;
jobject		g_thisObj = NULL;

// TAPI object and our instance handle for windows.
HINSTANCE	g_hinstDLL;
HWND		g_hWnd;
HLINEAPP	g_hTAPI;

FILE	*fptr;			// log file pointer
int		iLevel = 10;	// log level, set only at startup

// Some friendly constants

const int MAXCALLWAIT_MS =	5000;	// How long to wait before failing on placing a call..

DWORD WINAPI SecondaryThread(LPVOID pInputParam);

LONG LineCallStateProc(DWORD dwDevice, DWORD dwInstance,DWORD dwParam1,
					   DWORD dwParam2,DWORD dwParam3)
{
	long lRc;
	char szDebug[256];

	switch(dwParam1)
	{
	case LINECALLSTATE_IDLE:	
		lRc = lineDeallocateCall((HCALL)dwDevice);
		wsprintf(szDebug,"lineDeallocateCall returned: %d",lRc);
		debugString(4,"LineCallStateProc",szDebug,_where());
		break;

	default:
		break;
	}

	return 0;
}

/**
 * MS TAPI Call back function
 *
 * dwDDevice 
 *		A handle to either a line device or a call associated with the callback. 
 *
 * dwMessage
 *		A line or call device message. 
 *
 * dwCallbackInstance 
 *		Callback instance data passed back to the application in the callback.
 *
 * dwParam1 
 *		A parameter for the message. 
 *
 * dwParam2 
 *		A parameter for the message. 
 *
 * dwParam3 
 *		A parameter for the message. 
 */
void CALLBACK LineCallBackProc(DWORD dwDevice,DWORD dwMessage,
           DWORD dwInstance,DWORD dwParam1,DWORD dwParam2,DWORD dwParam3)
{

	char szDebug[256];

	// Pre-Process the event to see if we to do any work on it before publishing

	switch(dwMessage)
	{
	case LINE_CALLSTATE:
		LineCallStateProc(dwDevice, dwInstance, dwParam1, dwParam2, dwParam3);
		break;

	default:
		break;
	}


	// Publish this event into the IXTapi Object
	try{
		jclass cls = g_env->GetObjectClass(g_thisObj);
		jmethodID mid = g_env->GetMethodID(cls, "callback", "(IIIIII)V");

		if (mid == 0) {
		debugString(0,"LineCallBackProc","Could not find callback method!"
			,_where());

		}
		else
		{
			g_env->CallVoidMethod(g_thisObj, mid, dwDevice, dwMessage, 
				dwInstance, dwParam1, dwParam2, dwParam3);
		}
 
		wsprintf(szDebug,"LineCallBackProc. dwMessage: %d",dwMessage);
		debugString(8,"LineCallBackProc",szDebug,_where());

	}
	catch(...){
		debugString(0,"LineCallBackProc","Exception Handler.",_where());
	}


}

/*
 * Initialize TAPI.  All we here is see if we can init and figure out how
 * many TAPI lines are on the system.  We may not be able to work with
 * all of the lines on the system.  Which lines we can use is determined
 * when we call lineOpen.
 */
long InitTapi(unsigned long * dwNumLines)
{

	long	lRet = -1;
	char	szDebug[256];

	g_hTAPI = NULL;

	lRet = lineInitialize(&g_hTAPI, g_hinstDLL, LineCallBackProc,
		"TAPIProcess", dwNumLines);

	wsprintf(szDebug,"lineInit returned %d with %u lines",lRet,*dwNumLines );

	if(lRet == 0)
		debugString(4,"InitTapi",szDebug,_where());
	else
		debugString(0,"InitTapi",szDebug,_where());

	return lRet;

}

/********************************************************************
 *
 * initTapi
 *
 * Inits MS TAPI, sets the m_numLines field in the IXTapi Object to
 * the number of lines we have to use (NOTE: we can only use line
 * we can open and we haven't opened any by the end of this function!)
 *
 * This thread stays in this .dll to run the message pump.  It will
 * return on shudown or if TAPI init failed.
 * 
 * jint initTapi(JNIEnv, jobject	JNI Function sequence
 *
 * @return jint Positive identifier for success or a negative number 
 *				for failure.
 * 
 ********************************************************************/

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_initTapi
  (JNIEnv *pEnv, jobject oObj)
{
	unsigned long dwNumLines;

	g_thisObj = oObj;
	g_env = pEnv;

	long lRet = InitTapi(&dwNumLines);

	if(0 == lRet)
	{
		// Success!

		jclass cls;
		jfieldID fld;


		cls = g_env->GetObjectClass(g_thisObj);

		fld = g_env->GetFieldID(cls,"m_numLines","I");

		g_env->SetIntField(g_thisObj, fld, dwNumLines);

		initSound(dwNumLines);
	}
	else
		return - 1;

	SecondaryThread(0);

	return 0;

}

/********************************************************************
 *
 * openLine
 *
 * This function opens a single MS TAPI line.
 * 
 * jint openLine(JNIEnv, jobject	JNI Function sequence
 *				 lLine				Line to open
 *				 oTerminalName		Java StringBuffer to set the name
 *									of the terminal into.
 *
 * @return jint Positive identifier for this line or a negative number 
 *				for failure.
 * 
 ********************************************************************/

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_openLine
  (JNIEnv *pEnv, jobject oObj, jint lLine, jobject oTerminalName)
{
	long	lRc;
	DWORD	lNegVer;
	char	szDebug[256];

	long	lUnused = 0;
	long	sizeBuffer;
	HLINE	hLine;

	LINEEXTENSIONID lpExtensionID;
	LPLINEDEVCAPS	lpLineDevCaps = NULL;

    lRc = lineNegotiateAPIVersion(g_hTAPI, lLine, TAPIVERSION,
        TAPIVERSION, &lNegVer, &lpExtensionID);

	if(TAPIVERSION == lNegVer)
	{
		wsprintf(szDebug,"Negotiated TAPI 1.4 on line %d.",lLine);
		debugString(8,"openLine",szDebug,_where());
	}
	else
	{
		wsprintf(szDebug,"Failed to Negotiate TAPI 1.4 on line %d.",lLine);
		debugString(4,"openLine",szDebug,_where());
		return -1;
	}


	lRc = lineOpen(g_hTAPI, lLine, &hLine, lNegVer, lUnused, lLine, 
		LINECALLPRIVILEGE_OWNER, LINEMEDIAMODE_AUTOMATEDVOICE, 0);

	wsprintf(szDebug,"lineOpen on line %d returned with %d",lLine, lRc);

	if(lRc < 0)
	{
		debugString(4,"openLine",szDebug,_where());
		return lRc;
	}
	else
		debugString(8,"openLine",szDebug,_where());
	

	sizeBuffer = sizeof(lpLineDevCaps) + 1024;

	lpLineDevCaps = (LINEDEVCAPS *) malloc(sizeBuffer);

	memset(lpLineDevCaps, 0, sizeBuffer);

	lpLineDevCaps->dwTotalSize = sizeBuffer;


	lRc = lineGetDevCaps(g_hTAPI, lLine, lNegVer, 0, lpLineDevCaps);

	wsprintf(szDebug,"lineGetDevCaps returned -> %d",lRc);
	debugString(8,"openLine",szDebug,_where());

	if(0 == lRc)
	{
		if(lpLineDevCaps->dwLineNameSize > 0)
		{
			// lpLineDevCaps holds the name of the terminal, push that name up
			// to Java.

			long lpName = (long)lpLineDevCaps  + lpLineDevCaps->dwLineNameOffset;

			jstring oName = pEnv->NewStringUTF((const char *)lpName);

			jclass cls;
			jmethodID mid;

			cls = pEnv->GetObjectClass (oTerminalName);

			mid = pEnv->GetMethodID(cls,"append",
				"(Ljava/lang/String;)Ljava/lang/StringBuffer;");

			pEnv->CallObjectMethod (oTerminalName, mid, oName);

		}

		lRc = lineSetStatusMessages(hLine,lpLineDevCaps->dwLineStates,0);

		if(lRc != 0)
		{
			debugString(4,"openLine","lineSetStatusMessages failed",_where());
			hLine = (HLINE)-1;
		}
	}

	free(lpLineDevCaps);

	return (long)hLine;
}

/********************************************************************
 *
 * connectCall
 *
 * This function makes a call to a destination address from an open
 * TAPI line.
 * 
 * jint connectCall(JNIEnv, jobject	JNI Function sequence
 *				    lLine			Line number (for debugging)
 *					oDest			Destination address
 *					lHandle			Line Handle
 *
 * @return jint Positive identifier for this call or zero or less on failure
 * 
 ********************************************************************/

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_connectCall
  (JNIEnv *pEnv, jobject oObj, jint lLine, jstring oDest, jint lHandle)
{
	const char* utf_string;
	jboolean isCopy;
	long lRet = 0;
	char szDebug[256];
	HCALL hCall = 0;

	utf_string = pEnv->GetStringUTFChars(oDest,&isCopy);

	wsprintf(szDebug,"Will place call from line %d to %s.",lLine,utf_string);

	debugString(8,"connectCall",szDebug,_where());

	lRet = lineMakeCall((HLINE)lHandle, &hCall, utf_string, 0, 0);

	long lWait = (long)hCall;

	if(JNI_TRUE == isCopy) {
		pEnv->ReleaseStringUTFChars(oDest,utf_string);
	}

	// If lineMakeCall succeeded then lRet > 0.  However lineMakeCall is
	// async so we wait on the change of hCall befor returning if the
	// call to lineMakeCall was successfull....

	int loop = 0;

	wsprintf(szDebug,"lineMakeCall returned -> %d",lRet);

	if(lRet > 0)
	{
		debugString(8,"connectCall",szDebug,_where());

		while((long)hCall == 0)
		{
			Sleep(20);
			loop++;
			if(loop * 20 > MAXCALLWAIT_MS)
				break;	// Bail out!!
		}

	}
	else
	{
		debugString(1,"connectCall",szDebug,_where());
		return - 1;
	}

	wsprintf(szDebug,"Waited %d milliseconds for lineMakeCall",loop *20);
	debugString(8,"connectCall",szDebug,_where());

	wsprintf(szDebug,"hCall = -> %d",(long)hCall);
	debugString(8,"connectCall",szDebug,_where());

	return (long)hCall;

}

/********************************************************************
 *
 * answerCall
 *
 * This function accepts an incoming TAPI call.
 * 
 * jint answerCall(JNIEnv, jobject	JNI Function sequence
 *				   lCallHandle		Handle of call
 *
 * @return jint Positive identifier for this call or a negative number 
 *				for failure.
 * 
 ********************************************************************/

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_answerCall
  (JNIEnv *pEnv, jobject oObj, jint lHandle)
{
	long lRet;
	char szDebug[256];

	wsprintf(szDebug,"answerCall handle %d",lHandle);
	debugString(4,"answerCall",szDebug,_where());

	lRet = lineAnswer((HCALL)lHandle,NULL,0);

	wsprintf(szDebug,"lineAnswer returned %d",lRet);
	debugString(4,"answerCall",szDebug,_where());

	return lRet;
}

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_dropCall
  (JNIEnv *pEnv, jobject oObj, jint lHandle)
{
	long lRc;
	char szDebug[256];

	wsprintf(szDebug,"dropCall dropping call handle %d.",lHandle);
	debugString(4,"dropCall",szDebug,_where());

    lRc = lineDrop((HCALL)lHandle, "", 0);
	wsprintf(szDebug,"lineDrop returned %d.",lRc);
	debugString(4,"dropCall",szDebug,_where());
/*
	-- Removed.  lineDeallocateCall is now called automatically
	-- from the LINECALLSTATE_IDLE message.
	Sleep(1000);

    lRc = lineDeallocateCall((HCALL)lHandle);
	wsprintf(szDebug,"lineDeallocateCall returned %d.",lRc);
	debugString(4,"dropCall",szDebug,_where());
*/
	return 0;
}

/********************************************************************
 *
 * monitorDigits
 *
 * This function tells MS TAPI whether or not to send us digit events.
 * 
 * jint monitorDigits(JNIEnv, jobject	JNI Function sequence
 *				      lHandle			Handle of the call to monitor
 *					  enable			True or False
 *
 * @return jint Positive identifier success or a negative number 
 *				for failure.
 * 
 ********************************************************************/

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_monitorDigits
  (JNIEnv *pEnv, jobject oObj, jint lHandle, jboolean enable)
{
	long lRc;
	if(enable)
		lRc = lineMonitorDigits((HCALL)lHandle,LINEDIGITMODE_DTMF);
	else
		lRc = lineMonitorDigits((HCALL)lHandle,0);

	return lRc;
}

/********************************************************************
 *
 * getCallInfo
 *
 * This function gets caller id information
 * 
 * jint getCallInfo(JNIEnv, jobject	JNI Function sequence
 *				    lCallHandle		Handle of call
 *
 * @return String[] Two element string array. s[0] = name  s[1] = address
 * 
 ********************************************************************/

JNIEXPORT jobjectArray JNICALL Java_net_xtapi_serviceProvider_MSTAPI_getCallInfo
  (JNIEnv *pEnv, jobject oObj, jint lHandle)
{
	LPLINECALLINFO	lpCallInfo	= NULL;
	jobjectArray	oCallID		= NULL;		// String array of Name and Address

	try{

		long sizeBuffer = sizeof(lpCallInfo) + 1024;

		lpCallInfo = (LINECALLINFO *) malloc(sizeBuffer);

		lpCallInfo->dwTotalSize = sizeBuffer;

		long lRc = lineGetCallInfo((HCALL)lHandle,lpCallInfo);

		if(0 == lRc)
		{
			if(lpCallInfo->dwOrigin == LINECALLORIGIN_OUTBOUND)
			{
				// We placed this call, we don't need the info!
				debugString(8,"getCallInfo",
					"LINECALLORIGIN_OUTBOUND",_where());
				free(lpCallInfo);
				return NULL;
			}

			char szName[256];
			char szAddress[256];

			strcpy(szAddress,"UNKNOWN");
			strcpy(szName,"UNKNOWN");

			// See if we have the phone number
			if ((lpCallInfo->dwCallerIDFlags & LINECALLPARTYID_ADDRESS) != 0)
			{
				if(lpCallInfo->dwCallerIDSize < 256)
				{
					strcpy(szAddress,
						(char*)lpCallInfo + lpCallInfo->dwCallerIDOffset);
				}
			}

			// See if we have the name
			if ((lpCallInfo->dwCallerIDFlags & LINECALLPARTYID_NAME) != 0)
			{
				if(lpCallInfo->dwCallerIDNameSize < 256)
				{
					strcpy(szName,
						(char*)lpCallInfo + lpCallInfo->dwCallerIDNameOffset);
				}
			}

			if ((lpCallInfo->dwCallerIDFlags & LINECALLPARTYID_BLOCKED) != 0)
			{
				strcpy(szName,"BLOCKED");
				strcpy(szAddress,"BLOCKED");
			}

			if ((lpCallInfo->dwCallerIDFlags & LINECALLPARTYID_OUTOFAREA) != 0)
			{
				strcpy(szName,"OUTOFAREA");
				strcpy(szAddress,"OUTOFAREA");
			}

			jclass		oCls;				// String Class
			jobject		oElement;			// Array element

			oCls = pEnv->FindClass("java/lang/String");

			// Create a two element string array to hold the Name and Address
			oCallID = pEnv->NewObjectArray(2,oCls,NULL);

			oElement = pEnv->NewStringUTF(szName);
			pEnv->SetObjectArrayElement(oCallID, 0, oElement);

			oElement = pEnv->NewStringUTF(szAddress);
			pEnv->SetObjectArrayElement(oCallID, 1, oElement);

  
		}
		else
		{
			char szMsg[256];
			wsprintf(szMsg,"lineGetCallInfo returned %d",lRc);
			debugString(1,"getCallInfo",szMsg,_where());
		}

	}catch(...){
		debugString(1,"getCallInfo","Exception Handler",_where());
		oCallID = NULL;
	}

	free(lpCallInfo);

	return oCallID;
}

/********************************************************************
 *
 * sendDigits
 *
 * This asynchronous function sends a string of digits as DTMF tones.
 * 
 * jint sendDigits(JNIEnv, jobject	JNI Function sequence
 *				   lCallHandle		Handle of call
 *
 * @return jint zero for success or any other number for failure.
 * 
 ********************************************************************/

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_sendDigits
  (JNIEnv * pEnv, jobject oObj, jint lHandle, jstring oDigits)
  
{
	const char* utf_string;
	jboolean isCopy;

	utf_string = pEnv->GetStringUTFChars(oDigits,&isCopy);

	long lRes = lineGenerateDigits((HCALL)lHandle,
									LINEDIGITMODE_DTMF, 
									utf_string,
									0);
	if(JNI_TRUE == isCopy) {
		pEnv->ReleaseStringUTFChars(oDigits,utf_string);
	}

	if(lRes != 0)
	{
		char szMsg[256];
		wsprintf(szMsg,"lineGenerateDigits returned %d",lRes);
		debugString(1,"sendDigits",szMsg,_where());
	}

	return lRes;
}

/********************************************************************
 *
 * shutDown
 *
 * This function signals our secondary thread (init thread) to shut down.
 * 
 * jint shutDown(JNIEnv, jobject	JNI Function sequence
 *
 * @return jint If the function succeeds, the return value is nonzero.
 * 
 ********************************************************************/

JNIEXPORT jint JNICALL Java_net_xtapi_serviceProvider_MSTAPI_shutDown
  (JNIEnv * pEnv, jobject oObj)
{
	int r;
	r = PostMessage(g_hWnd,		// handle to destination window
					WM_USER + 1,// message
					0,			// first message parameter
					0);			// second message parameter

	return r;
}


// Use a secondary thread to work the windows message pump.
DWORD WINAPI SecondaryThread(LPVOID pInputParam)
{

   MSG msg;

   // msg-pump.
   while (GetMessage(&msg, NULL, 0, 0))
   {
	   //debugString(8,"SecondaryThread","msg-pump",_where());
	   g_hWnd = msg.hwnd;

	   if(msg.message == WM_USER + 1)
	   {	// shutDown was called, shutdown TAPI and return.
		   if( g_hTAPI != NULL)
		   {
			   debugString(4,"SecondaryThread","lineShutdown",_where());
				lineShutdown( g_hTAPI );
				g_hTAPI = NULL;
		   }
		   return 0;
	   }
	   else
	   {
		   TranslateMessage(&msg);
		   DispatchMessage(&msg);
	   }
   }
   return 0;
}

/**
 * Some process has attached to XTapi.dll, startup logging.
 */

extern "C"{
BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved )
{
	if(DLL_PROCESS_ATTACH == fdwReason)
	{
		fptr = fopen("mstapi.log","w");
		if(NULL != fptr)
		{
			struct _timeb timebuffer;
			char *timeline;

			_ftime( &timebuffer );
			timeline = ctime( & ( timebuffer.time ) );

			// put a header on the log entry:
			// char * header = 'Captains log, Star Date  %.19s.%-3hu'
    		char * annoy = "-----------";
    		fprintf(fptr,"\n;\t%s Logging Started %.19s.%-3hu %s \n",
					annoy, timeline, timebuffer.millitm, annoy);
			fprintf(fptr,";Time\t\tSEV\tMessage\t\t\t\t\t\t\tMethod\n");

		}
		debugString(8,"DllMain","DLL_PROCESS_ATTACH",_where());
		g_hinstDLL = (HINSTANCE)hinstDLL;
	}

	if(DLL_PROCESS_DETACH == fdwReason)
	{
		debugString(8,"DllMain","DLL_PROCESS_DETACH",_where());
		teardownSound();
		fclose(fptr);
	}

	return TRUE;
}
}
/*
 * Log debug messages.  Use the _where() macro for the where parameter to get 
 * source file and line number information.
 */
void debugString(int iSev, const char * module, const char * logMsg, 
				 const char * where)
{

	if(iSev < iLevel)
	{
		// Trim off the file path
		char * str = strrchr(where, '\\');

		if(NULL != str)
		{
			where = str + 1;
		}

        struct _timeb	timebuffer;				// time struct
	    char			*pchTimeline = NULL;	// time string

        _ftime( &timebuffer );
	    pchTimeline = ctime( & ( timebuffer.time ) );

		if(NULL != pchTimeline)
		{
			if(NULL != fptr)
			{
				fprintf(fptr,
					   "%.8s.%-3u\t%4d\t%-50s\t%s @ %s\n",
					   &pchTimeline[11], 
					   timebuffer.millitm,
					   iSev,
					   logMsg,
					   module,
					   where);

				if(iSev < 2)	// Flush all errors
				{
					fflush(fptr);
				}
			}
		}
	}
}