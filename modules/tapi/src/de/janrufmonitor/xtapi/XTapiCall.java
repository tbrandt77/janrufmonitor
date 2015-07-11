package de.janrufmonitor.xtapi;

import java.util.Date;
import java.util.Properties;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class XTapiCall {

	private Properties m_config;
	private int m_device;
	private int m_instance;
	private String m_number;
	private String m_msn;
	private ICall m_call;
	
	public XTapiCall(int device, int instance, String number, String callednumber, Properties config) {
		this.m_config = config;
		this.m_device = device;
		this.m_instance = instance;
		this.m_number = number;
		this.m_msn = callednumber;
	}
	
	public XTapiCall(int device, int instance, String number, Properties config) {
		this(device, instance, number, null, config);
	}
	
	private String getFestnetzAlias() {
		return this.m_config.getProperty(XTapiConst.CFG_FESTNETZALIAS, "0"); 
	}
	
	private String getDefaultCip() {
		return "4";
	}
	
	public ICall toCall() {
		if (this.m_call==null) {
			IRuntime r = PIMRuntime.getInstance();
			
			
			IPhonenumber phone = PhonenumberAnalyzer.getInstance(r).toClirPhonenumber(this.m_number);
			
			if (phone==null) phone = PhonenumberAnalyzer.getInstance(r).toInternalPhonenumber(this.m_number, this.m_msn);
			
			if (phone==null) phone = PhonenumberAnalyzer.getInstance(r).toPhonenumber(this.m_number, this.m_msn);
						
			ICaller c = r.getCallerFactory().createCaller(phone);
			IMsn msn = null;
			if (this.m_msn==null) {
				msn = r.getCallFactory().createMsn(getFestnetzAlias(), "");
			} else {
				msn = r.getCallFactory().createMsn(this.m_msn, "");
			}
			
			msn.setAdditional(r.getMsnManager().getMsnLabel(msn));
			
			ICip cip = r.getCallFactory().createCip(getDefaultCip(), "");
			cip.setAdditional(r.getCipManager().getCipLabel(cip, ""));
			
			// create attributes
			IAttributeMap am = r.getCallFactory().createAttributeMap();
			am.add(r.getCallFactory().createAttribute("tapi.device", Integer.toString(this.m_device)));
			am.add(r.getCallFactory().createAttribute("tapi.instance", Integer.toString(this.m_instance)));
			am.add(r.getCallFactory().createAttribute("tapi.key", getKey(this.m_device, this.m_instance)));
			am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_MISSED));
			
			this.m_call = r.getCallFactory().createCall(c, msn, cip, new Date());
			this.m_call.setAttributes(am);
			
		}
		return this.m_call;
		
	}
	
	public static String getKey(ICall call) {
		IAttribute att = call.getAttribute("tapi.key");
		if (att!=null) return att.getValue();
		return null;
	}
	
	public static String getKey(int device, int instance) {
		return Integer.toString(device)+"-"+Integer.toString(instance);
	}

}
