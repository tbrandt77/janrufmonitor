package de.janrufmonitor.fritzbox;

import java.util.Properties;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
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

	protected String getCip(String s) {
		if (s.toLowerCase().indexOf("sip")>-1) {
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
	
	protected String getFestnetzAlias(String call) {
		if (call.trim().toLowerCase().startsWith("festnetz")) {
			String alias = this.m_config.getProperty(FritzBoxConst.CFG_FESTNETZALIAS); 
			return ((alias==null || alias.trim().length()==0)? call : alias);
		}
		return call;
	}
	
	protected String getGeneralAreaCode() {
		String value = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE);
		if (value==null || value.length()==0) value = "0";
		return value;
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
