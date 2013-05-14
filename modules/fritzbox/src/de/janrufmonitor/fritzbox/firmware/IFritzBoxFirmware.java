package de.janrufmonitor.fritzbox.firmware;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.DoBlockException;
import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxInitializationException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxNotFoundException;
import de.janrufmonitor.fritzbox.firmware.exception.GetAddressbooksException;
import de.janrufmonitor.fritzbox.firmware.exception.GetBlockedListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.fritzbox.firmware.exception.InvalidSessionIDException;

public interface IFritzBoxFirmware {
	
	// fritz box types
	public final static byte TYPE_FRITZBOX_FON = 6;
	public final static byte TYPE_FRITZBOX_FON_WLAN = 8;
	public final static byte TYPE_FRITZBOX_ATA = 11;
	public final static byte TYPE_FRITZBOX_5050 = 12;
	public final static byte TYPE_FRITZBOX_7050 = 14;
	public final static byte TYPE_EUMEX_300IP = 15;
    public final static byte TYPE_FRITZBOX_5010 = 23;
    public final static byte TYPE_FRITZBOX_5012 = 25;
    public final static byte TYPE_FRITZBOX_7170 = 29;
    public final static byte TYPE_FRITZBOX_7140 = 30;
    public final static byte TYPE_SPEEDPORT_W900V = 34;
    public final static byte TYPE_FRITZFON_7150 = 38;
    public final static byte TYPE_FRITZBOX_7140_ANNEXA = 39;
    public final static byte TYPE_FRITZBOX_7141 = 40;
    public final static byte TYPE_FRITZBOX_5140 = 43;
    public final static byte TYPE_FRITZBOX_7270 = 54;
    public final static byte TYPE_FRITZBOX_5124_ANNEXB = 56;
    public final static byte TYPE_FRITZBOX_7170_ANNEXA = 58;
    public final static byte TYPE_FRITZBOX_7113 = 60;
    public final static byte TYPE_FRITZBOX_7240 = 73;
    public final static byte TYPE_FRITZBOX_7270V3 = 74;
    public final static byte TYPE_FRITZBOX_7570 = 75;
    public final static byte TYPE_FRITZBOX_7390 = 84;
    public final static byte TYPE_FRITZBOX_6360 = 85;
    public final static byte TYPE_FRITZBOX_7112 = 87;
    public final static byte TYPE_FRITZBOX_7340 = 99;
    public final static byte TYPE_FRITZBOX_7320 = 100;
    
    public final static byte TYPE_FRITZBOX_6840_LTE = 105;
    public final static byte TYPE_FRITZBOX_7330 = 107;
    public final static byte TYPE_FRITZBOX_7360_SL = 109;
    public final static byte TYPE_FRITZBOX_7360 = 111;
    public final static byte TYPE_FRITZBOX_6340_CABLE = 115;
    public final static byte TYPE_FRITZBOX_7330_SL = 116;
    public final static byte TYPE_FRITZBOX_7312 = 117;

    public void login() throws FritzBoxLoginException;
    
    public void init() throws FritzBoxInitializationException, FritzBoxNotFoundException, InvalidSessionIDException;
    
    public void destroy();
    
    public boolean isInitialized();
    
    public List getCallList() throws GetCallListException, IOException;
    
    public List getCallerList() throws GetCallerListException, IOException;
    
    public List getCallerList(int addressbookId, String addressbookName) throws GetCallerListException, IOException;
    
    public Map getAddressbooks() throws GetAddressbooksException, IOException;
    
    public void deleteCallList() throws DeleteCallListException, IOException;
	
    public List getBlockedList() throws GetBlockedListException, IOException;
    
    public void doBlock(String number) throws DoBlockException, IOException;
    
    public void doCall(String number, String extension) throws DoCallException, IOException;
    
    public long getFirmwareTimeout();
    
    public long getSkipBytes();
   
}
