/*
 * MSTAPI.java
 *
 * Created on March 9, 2002, 9:48 AM
 */

package net.xtapi.serviceProvider;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;

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

public class MSTAPI implements IXTapi {
    static {
            System.loadLibrary("XTapi");
    }
    
    // Core Package
    private native    int initTapi();
    private native    int openLine(int line, StringBuffer name);
    private native    int connectCall(int line, String address, int handle);
    private native    int answerCall(int handle);
    private native    int dropCall(int handle);
    private native    String[] getCallInfo(int handle);
    private native    int shutDown();
    
    // Media Package
    private native    int monitorDigits(int handle, boolean enable);
    private native    int sendDigits(int handle, String digits);
    private native    int playSound(String file, int lineHandle);
    private native    int recordSound(String file, int lineHandle);
    private native    int stopRecording(int lineHandle);
    private native    int stopPlaying(int lineHandle);
    
    private int m_numLines = -1;
    private Logger m_logger;
    
    IXTapiCallBack m_provider = null;
    
    /** Creates new MSTAPI */
    public MSTAPI() {
    	this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
    }
    
    public void callback(int dwDevice,int dwMessage, int dwInstance,
                          int dwParam1,int dwParam2,int dwParam3)
    {
        m_provider.callback(dwDevice, dwMessage, dwInstance, dwParam1, dwParam2, dwParam3);
    }

    public int init(IXTapiCallBack p) {
        m_provider = p;
          try {
	     	Runnable r = new Runnable() {
		  public void run(){
		    try{
		        initTapi();
		    } catch (Exception excp){
		      m_logger.log(Level.SEVERE, excp.getMessage(), excp);
		    }
			};
		
		};
		Thread T = new Thread(r);
		T.setName("JAM-MSTAPI-Thread-(non-deamon)");
		T.start();
          } catch (Exception ex) {
        	  m_logger.log(Level.SEVERE, ex.getMessage(), ex);
          }        

        // The thread that inits TAPI needs to be the 
        // thread working the message pump so we have
        // a chicken and egg problem.  The thread created above will
        // set our member variable m_TapiRet to the number of lines or -1
        // if initalize was unsuccessful.
        int timeOut = 0;
        while(m_numLines == -1){
        try{
            Thread.sleep(100);
            timeOut++;
            if(timeOut > 50)
            {
                System.out.println("Timed out waiting to initialize TAPI");
                break;
            }
        } catch (Exception excp) {
        	 m_logger.log(Level.SEVERE, excp.getMessage(), excp);
        }
        }          
        
        return m_numLines;

    }
    
    public int openLineTapi(int line,StringBuffer terminalName) {
        return openLine(line, terminalName);
    }
    
    public int connectCallTapi(int line,String address,int handle) {
        return connectCall(line, address, handle);
    }
    
    public int answerCallTapi(int handle) {
        return answerCall(handle);
    }
    
    public int dropCallTapi(int handle) {
        return dropCall(handle);
    }
    
    public String[] getCallInfoTapi(int handle) {
        return getCallInfo(handle);
    }
    
    public int shutdownTapi()  {
        return shutDown();
    }    
    
    public int monitorDigitsTapi(int handle,boolean enable) {
        return monitorDigits(handle,enable);
    }
    
    public int playSoundTapi(String file,int lineHandle) {
        return playSound(file,lineHandle);
    }
    
    public int recordSoundTapi(String file,int lineHandle) {
        return recordSound(file,lineHandle);
    }
    
    public int stopRecordingTapi(int lineHandle) {
        return stopRecording(lineHandle);
    }
    
    public int sendDigitsTapi(int handle,String digits) {
        return sendDigits(handle,digits);
    }    
    
    public int stopPlayingTapi(int lineHandle) {
        return stopPlaying(lineHandle);
    }
    
}
