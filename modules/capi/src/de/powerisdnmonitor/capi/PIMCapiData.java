package de.powerisdnmonitor.capi;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.objects.ICallHandle;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class PIMCapiData implements ICallHandle {

	private ICall m_call;
	
	private String m_calling;
	private String m_called;
	private int m_cip;
	private boolean m_isSpoofed;
	private Object m_plci;

	private IRuntime m_runtime;
	
	public PIMCapiData(ICall call) {
		this.m_call = call;
	}
	
	public PIMCapiData() {
		this(null);
	}
	
	public PIMCapiData(String calling, String called, int cip) {
		this(calling, called, cip, false);
	}
	
	public PIMCapiData(String calling, String called, int cip, boolean isSpoofed) {
		this.m_calling = calling;
		this.m_called = called;
		this.m_cip = cip;
		this.m_isSpoofed = isSpoofed;
	}

	public ICall getCall() {
		if (this.m_call==null) {
			this.m_call = this.createCallObject(m_calling, m_called, m_cip);
			this.m_call.setAttribute(getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_SPOOFED, Boolean.toString(m_isSpoofed)));
		}
		return this.m_call;
	}
	
	public void setCallingParty(String number) {
		this.m_calling = number;
		if (this.m_call!=null) {
			this.m_call.getCaller().setPhoneNumber(
				this.getRuntime().getCallerFactory().createPhonenumber(
					number
				)	
			);
		}
	}
	
	public void setPLCI(Object plci) {
		this.m_plci = plci;
		if (this.m_call!=null) {
			this.m_call.setAttribute(
				this.getRuntime().getCallFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_BCHANNEL,
					plci.toString()
				)	
			);
		}
	}
	
	public void setCalledParty(String number) {
		this.m_called = number;
		if (this.m_call!=null) {
			this.m_call.setMSN(
				this.getRuntime().getCallFactory().createMsn(number, "")	
			);
		}
	}
	
	public void setCIP(int cip) {
		this.m_cip = cip;
		if (this.m_call!=null) {
			this.m_call.setCIP(
				this.getRuntime().getCallFactory().createCip(Integer.toString(cip), "")	
			);
		}
	}

	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	private ICall createCallObject(String number, String msn, int cip) {
		IPhonenumber phone = PhonenumberAnalyzer.getInstance(getRuntime()).toClirPhonenumber(number);
		
		if (phone==null) phone = PhonenumberAnalyzer.getInstance(getRuntime()).toInternalPhonenumber(number, msn);
		
		if (phone==null) phone = PhonenumberAnalyzer.getInstance(getRuntime()).toPhonenumber("0" + number, msn);
        
		IName name = this.getRuntime().getCallerFactory().createName("","");
		ICaller aCaller = this.getRuntime().getCallerFactory().createCaller(name, phone);
		ICip cipObj = this.getRuntime().getCallFactory().createCip(Integer.toString(cip), "");
		IMsn msnObj = this.getRuntime().getCallFactory().createMsn(msn, "");
        ICall c = this.getRuntime().getCallFactory().createCall(aCaller, msnObj, cipObj); 
        c.setAttribute(
			this.getRuntime().getCallFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_BCHANNEL,
					this.m_plci.toString()
				)	
			);
        
        c.setAttribute(
			this.getRuntime().getCallFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_RAW_NUMBER,
					number
				)	
			);      
        c.setAttribute(
			this.getRuntime().getCallFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CALLSTATUS,
					IJAMConst.ATTRIBUTE_VALUE_MISSED
				)	
			);   
        return c;
	}
	
	public String toString() {
		return this.m_plci + "#"+this.getCall();
	}
}
