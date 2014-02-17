package de.janrufmonitor.fritzbox.firmware;

public class FirmwareData {

	private int m_type, m_major, m_minor;
	private String m_mod;
	
	public FirmwareData(int type, int major, int minor , String mod) {
		this.m_type = type;
		this.m_major = major;
		this.m_minor = minor;
		this.m_mod = mod;
	}
	
	public FirmwareData(byte type, byte major, byte minor , String mod) {
		this((int)type, (int) major, (int) minor, mod);
	}
	
	public FirmwareData(byte type, byte major, byte minor) {
		this(type, major, minor, "");
	}
	
	public FirmwareData(String type, String major, String minor , String mod) {
		this(Integer.parseInt(type), Integer.parseInt(major), Integer.parseInt(minor), mod);
	}
	
	public FirmwareData(String type, String major, String minor) {
		this(type, major, minor, "");
	}
	
	public int getMajor() {
		return this.m_major;
	}
	public int getMinor() {
		return this.m_minor;
	}

	public String getFritzBoxName() {
		switch (this.m_type) {
		case IFritzBoxFirmware.TYPE_FRITZBOX_FON:
			return "FRITZ!Box Fon"; 
		case IFritzBoxFirmware.TYPE_FRITZBOX_FON_WLAN:
			return "FRITZ!Box Fon WLAN"; 
		case IFritzBoxFirmware.TYPE_FRITZBOX_7050:
			return "FRITZ!Box 7050"; 
		case IFritzBoxFirmware.TYPE_FRITZBOX_5050:
			return "FRITZ!Box 5050"; 
		case IFritzBoxFirmware.TYPE_EUMEX_300IP:
			return "Eumex 300ip"; 			
		case IFritzBoxFirmware.TYPE_FRITZBOX_ATA:
			return "FRITZ!Box ata"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_5010:
            return "FRITZ!Box 5010"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_5012:
            return "FRITZ!Box 5012"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_7170:
            return "FRITZ!Box Fon 7170";
        case IFritzBoxFirmware.TYPE_FRITZFON_7150:
        	return "FRITZ!Fon 7150";
        case IFritzBoxFirmware.TYPE_SPEEDPORT_W900V:
            return "Speedport W900V";             
        case IFritzBoxFirmware.TYPE_FRITZBOX_5140:
            return "FRITZ!Box Fon 5140";             
        case IFritzBoxFirmware.TYPE_FRITZBOX_7270:
            return "FRITZ!Box 7270"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_7270V3:
            return "FRITZ!Box 7270 v3";             
        case IFritzBoxFirmware.TYPE_FRITZBOX_7570:
            return "FRITZ!Box Fon 7570";      
        case IFritzBoxFirmware.TYPE_FRITZBOX_7113:
            return "FRITZ!Box 7113";   
        case IFritzBoxFirmware.TYPE_FRITZBOX_7240:
            return "FRITZ!Box 7240"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_7141:
            return "FRITZ!Box 7141";    
        case IFritzBoxFirmware.TYPE_FRITZBOX_7112:
            return "FRITZ!Box 7112";     
        case IFritzBoxFirmware.TYPE_FRITZBOX_7140:
            return "FRITZ!Box 7140";         
        case IFritzBoxFirmware.TYPE_FRITZBOX_7140_ANNEXA:
            return "FRITZ!Box 7140 (Annex A/Austria/Schweiz)";   
        case IFritzBoxFirmware.TYPE_FRITZBOX_7390:
            return "FRITZ!Box 7390";   
        case IFritzBoxFirmware.TYPE_FRITZBOX_6360:
            return "FRITZ!Box 6360";   
        case IFritzBoxFirmware.TYPE_FRITZBOX_7320:
            return "FRITZ!Box 7320";   
        case IFritzBoxFirmware.TYPE_FRITZBOX_5124_ANNEXB:
            return "FRITZ!Box 5124 (Annex B)";   
        case IFritzBoxFirmware.TYPE_FRITZBOX_7170_ANNEXA:
            return "FRITZ!Box 7170 (Annex A)";  
        case IFritzBoxFirmware.TYPE_FRITZBOX_7340:
            return "FRITZ!Box 7340";  
        case IFritzBoxFirmware.TYPE_FRITZBOX_6840_LTE:
            return "FRITZ!Box 6840 LTE";  
        case IFritzBoxFirmware.TYPE_FRITZBOX_7330:
            return "FRITZ!Box 7330";
        case IFritzBoxFirmware.TYPE_FRITZBOX_7360_SL:
            return "FRITZ!Box 7360 SL";
        case IFritzBoxFirmware.TYPE_FRITZBOX_7360:
            return "FRITZ!Box 7360";  
        case IFritzBoxFirmware.TYPE_FRITZBOX_6340_CABLE:
            return "FRITZ!Box 6340 (Kabel)"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_7330_SL:
            return "FRITZ!Box 7330 SL";
        case IFritzBoxFirmware.TYPE_FRITZBOX_7312:
            return "FRITZ!Box 7312"; 
        case IFritzBoxFirmware.TYPE_VOIP_GATEWAY_5188:
            return "AVM VoIP Gateway 5188"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_5124:
            return "FRITZ!Box 5124"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_3370:
            return "FRITZ!Box 3370"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_6320:
            return "FRITZ!Box 6320 (Kabel)"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_6842_LTE:
            return "FRITZ!Box 6842 LTE"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_7360_EWE:
            return "FRITZ!Box 7360 (EWE Edition)"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_6320_V2:
            return "FRITZ!Box 6320 (Kabel) v2"; 
        case IFritzBoxFirmware.TYPE_SPEEDPORT_W920V:
            return "Speedport W920V"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_7490:
            return "FRITZ!Box 7490"; 
        case IFritzBoxFirmware.TYPE_FRITZBOX_7362_SL:
            return "FRITZ!Box 7362 SL"; 
		default:			
		}
		return "unknown";
	}
	
	public String toString() {
		StringBuffer fw = new StringBuffer(32);
		fw.append((this.m_type<10 ? "0" : ""));
		fw.append(Integer.toString(this.m_type));
		fw.append(".");
		
		fw.append((this.m_major<10 ? "0" : ""));
		fw.append(Integer.toString(this.m_major));
		fw.append(".");
		
		fw.append((this.m_minor<10 ? "0" : ""));
		fw.append(Integer.toString(this.m_minor));
		fw.append(this.m_mod);
		
		return fw.toString();
	}

	
	
}
