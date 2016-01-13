package de.janrufmonitor.fritzbox;

import java.io.IOException;
import java.util.Properties;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.PasswordFritzBoxFirmware;
import de.janrufmonitor.fritzbox.firmware.SessionIDFritzBoxFirmware;
import de.janrufmonitor.fritzbox.firmware.UnitymediaFirmware;
import de.janrufmonitor.runtime.PIMRuntime;

public abstract class AbstractFritzBoxCall implements IFritzBoxCall, FritzBoxConst {

	protected String m_line;
	protected ICall m_call;
	protected Properties m_config;
	protected int m_outgoingState = -1;
	
	public AbstractFritzBoxCall(String line, Properties config) {
		this.m_line = line;
		this.m_config = config;
	}

	protected String getCip(String field) {
		if (field.toLowerCase().startsWith("sip:") || field.toLowerCase().startsWith("internet:")) {
			return "100";
		}
		
		return "16";
	}

	public String toString() {
		return this.toCall().toString();
	}

	public boolean isValid() {
		return toCall()!=null;
	}
	
	protected String getCallByCall(String call) {
		if (call.startsWith("0100")) {
			return call.substring(0, "0100yy".length());
		}
		if (call.startsWith("010")) {
			return call.substring(0, "010xy".length());
		}
		return null;
	}
	
	protected String getMsn(String field) {
		if (field.trim().toLowerCase().startsWith("festnetz")) {
			String alias = this.m_config.getProperty(FritzBoxConst.CFG_FESTNETZALIAS); 
			return ((alias==null || alias.trim().length()==0)? field : alias);
		}
		if (field.trim().toLowerCase().startsWith("internet:")) {
			return field.substring("Internet:".length()).trim();
		}
		if (field.trim().toLowerCase().startsWith("sip:")) {
			return field.substring("sip:".length()).trim();
		}
		// added for FritzBoxRawCall on SIP index
		if (field.trim().toLowerCase().startsWith("sip")) {
			try {
				return FirmwareManager.getInstance().getMSNFromSIP(field.substring("sip".length()).trim());
			} catch (IOException e) {
			}
			return null;
		}
		// added 2015/12/01: TR-064 could deliver MSN number or MSN alias in the CSV export file, so check for alias first
		if (!field.matches("[+-]?[0-9]+")) {
			// assume text is in field
			String[] msns = PIMRuntime.getInstance().getMsnManager().getMsnList();
			for (int i=0;i<msns.length;i++) {
				if (field.equalsIgnoreCase(PIMRuntime.getInstance().getMsnManager().getMsnLabel(msns[i]))) return msns[i];
			}
		}
		return field;
	}
	
	protected boolean isSpoofingnumber(String s) {
		int start = s.indexOf("(0");
		int end = s.lastIndexOf(")");
		return (start>0 && end > start);
	}
	
	protected String getSpoofingnumber(String s) {
		int start = s.indexOf("(0");
		int end = s.lastIndexOf(")");
		if (start>0 && end > start) {
			return s.substring(start+1, end);
		}
		return null;
	}
	
	protected int getOutgoingState() {
		if (this.m_outgoingState == -1) {
			this.m_outgoingState = 4;
			if (FirmwareManager.getInstance().isInstance(UnitymediaFirmware.class)) this.m_outgoingState = 3;
			if (FirmwareManager.getInstance().isInstance(SessionIDFritzBoxFirmware.class)) this.m_outgoingState = 3;
			if (FirmwareManager.getInstance().isInstance(PasswordFritzBoxFirmware.class)) this.m_outgoingState = 3;
		}
		return this.m_outgoingState;
	}
	
	protected boolean hasRejectedState(){
		return (getOutgoingState() == 4);
	}
	
	protected int getRejectedState() {
		if (getOutgoingState() == 4) return 3;
		return -1;
	}
	
	public abstract ICall toCall();
	
}
