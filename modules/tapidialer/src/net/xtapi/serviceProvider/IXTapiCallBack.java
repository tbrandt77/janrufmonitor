/*
 * IXTapiCallback.java
 *
 * Created on April 8, 2002, 7:59 PM
 */

package net.xtapi.serviceProvider;

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

public interface IXTapiCallBack {
	
        public void callback(int dwDevice,int dwMessage, int dwInstance,
                          int dwParam1,int dwParam2,int dwParam3);
        
    // TAPI Constants as defined by Microsoft/Intel in tapi.h
    public static final int LINE_ADDRESSSTATE       = 0;
    public static final int LINE_CALLINFO           = 1;
    public static final int LINE_CALLSTATE          = 2;
    public static final int LINE_CLOSE              = 3;
    public static final int LINE_DEVSPECIFIC        = 4;
    public static final int LINE_DEVSPECIFICFEATURE = 5;
    public static final int LINE_GATHERDIGITS       = 6;
    public static final int LINE_GENERATE           = 7;
    public static final int LINE_LINEDEVSTATE       = 8;
    public static final int LINE_MONITORDIGITS      = 9;
    public static final int LINE_MONITORMEDIA       = 10;
    public static final int LINE_MONITORTONE        = 11;
    public static final int LINE_REPLY              = 12;
    public static final int LINE_REQUEST            = 13;
    public static final int PHONE_BUTTON            = 14;
    public static final int PHONE_CLOSE             = 15;
    public static final int PHONE_DEVSPECIFIC       = 16;
    public static final int PHONE_REPLY             = 17;
    public static final int PHONE_STATE             = 18;
    public static final int LINE_CREATE             = 19;
    public static final int PHONE_CREATE            = 20;
    
    // LineDevState Constansts
    public static final int LINEDEVSTATE_OTHER                 = 0x00000001;
    public static final int LINEDEVSTATE_RINGING               = 0x00000002;
    public static final int LINEDEVSTATE_CONNECTED             = 0x00000004;
    public static final int LINEDEVSTATE_DISCONNECTED          = 0x00000008;
    public static final int LINEDEVSTATE_MSGWAITON             = 0x00000010;
    public static final int LINEDEVSTATE_MSGWAITOFF            = 0x00000020;
    public static final int LINEDEVSTATE_INSERVICE             = 0x00000040;
    public static final int LINEDEVSTATE_OUTOFSERVICE          = 0x00000080;
    public static final int LINEDEVSTATE_MAINTENANCE           = 0x00000100;
    public static final int LINEDEVSTATE_OPEN                  = 0x00000200;
    public static final int LINEDEVSTATE_CLOSE                 = 0x00000400;
    public static final int LINEDEVSTATE_NUMCALLS              = 0x00000800;
    public static final int LINEDEVSTATE_NUMCOMPLETIONS        = 0x00001000;
    public static final int LINEDEVSTATE_TERMINALS             = 0x00002000;
    public static final int LINEDEVSTATE_ROAMMODE              = 0x00004000;
    public static final int LINEDEVSTATE_BATTERY               = 0x00008000;
    public static final int LINEDEVSTATE_SIGNAL                = 0x00010000;
    public static final int LINEDEVSTATE_DEVSPECIFIC           = 0x00020000;
    public static final int LINEDEVSTATE_REINIT                = 0x00040000;
    public static final int LINEDEVSTATE_LOCK                  = 0x00080000;
    // TAPI 1.4
    public static final int LINEDEVSTATE_CAPSCHANGE            = 0x00100000; 
    public static final int LINEDEVSTATE_CONFIGCHANGE          = 0x00200000; 
    public static final int LINEDEVSTATE_TRANSLATECHANGE       = 0x00400000;
    public static final int LINEDEVSTATE_COMPLCANCEL           = 0x00800000; 
    public static final int LINEDEVSTATE_REMOVED               = 0x01000000;  
    
    // LineCallState Constants
    public static final int LINECALLSTATE_IDLE                 = 0x00000001;
    public static final int LINECALLSTATE_OFFERING             = 0x00000002;
    public static final int LINECALLSTATE_ACCEPTED             = 0x00000004;
    public static final int LINECALLSTATE_DIALTONE             = 0x00000008;
    public static final int LINECALLSTATE_DIALING              = 0x00000010;
    public static final int LINECALLSTATE_RINGBACK             = 0x00000020;
    public static final int LINECALLSTATE_BUSY                 = 0x00000040;
    public static final int LINECALLSTATE_SPECIALINFO          = 0x00000080;
    public static final int LINECALLSTATE_CONNECTED            = 0x00000100;
    public static final int LINECALLSTATE_PROCEEDING           = 0x00000200;
    public static final int LINECALLSTATE_ONHOLD               = 0x00000400;
    public static final int LINECALLSTATE_CONFERENCED          = 0x00000800;
    public static final int LINECALLSTATE_ONHOLDPENDCONF       = 0x00001000;
    public static final int LINECALLSTATE_ONHOLDPENDTRANSFER   = 0x00002000;
    public static final int LINECALLSTATE_DISCONNECTED         = 0x00004000;
    public static final int LINECALLSTATE_UNKNOWN              = 0x00008000;    
        
}

