package de.janrufmonitor.ncid;

import java.util.Properties;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.PIMRuntime;

public abstract class AbstractNcidCall implements INcidCall, NcidConst {

	protected String m_line;
	protected ICall m_call;
	protected Properties m_config;
	
	public AbstractNcidCall(String line, Properties config) {
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
		if (call.trim().toLowerCase().startsWith("festnetz") || call.trim().length()==0) {
			String alias = this.m_config.getProperty(NcidConst.CFG_FESTNETZALIAS); 
			return ((alias==null || alias.trim().length()==0)? call : alias);
		}
		return call;
	}
	
	protected String getGeneralAreaCode() {
		String value = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE);
		if (value==null || value.length()==0) value = "0";
		return value;
	}
	
	public abstract ICall toCall();
	
}
