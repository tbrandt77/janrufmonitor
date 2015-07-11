package de.janrufmonitor.ncid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class NcidCallRaw extends AbstractNcidCall {
	
	public NcidCallRaw(String rawline, Properties config) {
		// INCOMING
		//CID: / OUT:
		//	*DATE*<date>*TIME*<time>*LINE*<label>*NMBR*<number>*MESG*<
		//	msg>*NAME*<name>*	

		// DATE*mmddyyyy* where m = month, d = day, y = year
		// TIME*hhmm* where h = hour, m = minute
		// LINE*label* where label is a character string identifier
		// NMBR*number* where number is the phone number, or PRIVATE,
		// ANONYMOUS, NO NUMBER, OUT-OF-AREA
		// or similar
		// MESG*msg* where msg is NONE or a string of characters
		// NAME*name* where name is a string of characters
		
		// sample:
		
		// CID: *DATE*22102014*TIME*2113*LINE*SIPO*NMBR*072651303*MESG*NONE*NAME*NO NAME*
		
		// Date = 2+4
		// MSN = 6
		// callnumber = 8
		
		// CIDINFO: *LINE*1*RING*0*TIME*16:20:05*
		// RING*count* where number is 0, -1, -2 or the ring count
		// 0 = ringing has stopped because call was answered
		// -1 = ringing has stopped, call was not answered
		// -2 = call hangup after being answered
		
		super(rawline, config);
	}

	public ICall toCall() {
		if (this.m_line==null || this.m_line.length()==0) return null;

		if (this.m_call==null) {
			IRuntime r = PIMRuntime.getInstance();
			String[] call = this.m_line.split("\\*");
			if (call.length>=8 && call[0].trim().equalsIgnoreCase("CID:")) {
				// create MSN
				IMsn msn = r.getCallFactory().createMsn(getFestnetzAlias(call[6]), "");
				msn.setAdditional(r.getMsnManager().getMsnLabel(msn));
				
				IPhonenumber pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toClirPhonenumber(call[8].trim());
				
				// if no CLIR call, check internal
				if (pn==null) pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toInternalPhonenumber(call[8].trim(), msn.getMSN());
				// if no internal call, check regular
				if (pn==null)  {
					// if incoming call does not start with 0, the Provider number seems to have the wrong format
					// assume it is an international format 4971657110
					if (!call[8].startsWith("0") && !PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).containsSpecialChars(call[8])) {
						call[8] = "00"+call[8];
					}
					pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toPhonenumber(call[8].trim(), msn.getMSN());
				}
				ICaller caller = r.getCallerFactory().createCaller(pn);
				
				// create call data
				// added 2009/05/01
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmm");
				Date date = new Date(0);
				try {
					date = sdf.parse(call[2]+call[4]);					
				} catch (ParseException e) {
					Logger.getLogger(IJAMConst.DEFAULT_LOGGER).severe("Wrong standard date format detected.");
				}

				ICip cip = r.getCallFactory().createCip(getCip("sip"), "");
				cip.setAdditional(r.getCipManager().getCipLabel(cip, ""));
				
				// create attributes
				IAttributeMap am = r.getCallFactory().createAttributeMap();

				am.add(r.getCallFactory().createAttribute("Ncid.key", call[6]));
				if (call.length>=12) {
					am.add(r.getCallFactory().createAttribute("Ncid.msg", call[10]));
					am.add(r.getCallFactory().createAttribute("Ncid.callername", call[12]));
				}
				am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS
						, IJAMConst.ATTRIBUTE_VALUE_MISSED));
				
				// create UUID
				StringBuffer uuid = new StringBuffer();
				uuid.append(date.getTime());
				uuid.append("-");
				uuid.append(pn.getTelephoneNumber());
				uuid.append("-");
				uuid.append(msn.getMSN());
				
				// limit uuid to 32 chars
				if (uuid.length()>31) {
					// reduce byte length to append -1 for redundant calls max -1-1 --> 3 calls
					uuid = new StringBuffer(uuid.substring(0,31));
				}
				
				this.m_call = r.getCallFactory().createCall(uuid.toString(), caller, msn, cip, date);
				this.m_call.setAttributes(am);
			}
			if (call.length>=8 && call[0].trim().equalsIgnoreCase("OUT:")) {
				// create msn
				IMsn msn = r.getCallFactory().createMsn(getFestnetzAlias(call[6]), "");
				msn.setAdditional(r.getMsnManager().getMsnLabel(msn));
				
				// create caller data
				String callByCall = null;
				
				IPhonenumber pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toClirPhonenumber(call[8].trim());
				
				// if no CLIR call, check internal
				if (pn==null) pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toInternalPhonenumber(call[8].trim(), msn.getMSN());
				// if no internal call, check regular
				if (pn==null)  {
					// added 2006/08/10: trim call-by-call information
					// only can occure on state CALL (out-going calls)
					callByCall = getCallByCall(call[8]);
					if (callByCall!=null) {
						call[8] = call[8].substring(callByCall.length());
					}
					// added 2011/01/13: added areacode additional on special Ncid mode. Having no leading 0, 
					// requires addition of areacode
					if (!call[8].startsWith("0")) {
						Logger.getLogger(IJAMConst.DEFAULT_LOGGER).info("Assuming number "+call[8]+" has missing areacode.");
						call[8] = this.getGeneralAreaCode() + call[8];
						Logger.getLogger(IJAMConst.DEFAULT_LOGGER).info("Added areacode to number "+call[8]);
					}
					
					pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toPhonenumber(call[8].trim(), msn.getMSN());
				}


				ICaller caller = r.getCallerFactory().createCaller(pn);
				
				// create call data
				// added 2009/05/27
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmm");
				Date date = new Date(0);
				try {
					date = sdf.parse(call[2]+call[4]);				
				} catch (ParseException e) {
					Logger.getLogger(IJAMConst.DEFAULT_LOGGER).severe("Wrong standard date format detected.");
				}

				ICip cip = r.getCallFactory().createCip(getCip("sip"), "");
				cip.setAdditional(r.getCipManager().getCipLabel(cip, ""));
				
				// create attributes
				IAttributeMap am = r.getCallFactory().createAttributeMap();

				am.add(r.getCallFactory().createAttribute("Ncid.key", call[6]));
				if (call.length>=12) {
					am.add(r.getCallFactory().createAttribute("Ncid.msg", call[10]));
					am.add(r.getCallFactory().createAttribute("Ncid.callername", call[12]));
				}
				am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_OUTGOING));				
				
				if (callByCall!=null)
					am.add(r.getCallFactory().createAttribute("Ncid.callbycall", callByCall));
								
				// create UUID
				StringBuffer uuid = new StringBuffer();
				uuid.append(date.getTime());
				uuid.append("-");
				uuid.append(pn.getTelephoneNumber());
				uuid.append("-");
				uuid.append(msn.getMSN());
				
				// limit uuid to 32 chars
				if (uuid.length()>31) {
					// reduce byte length to append -1 for redundant calls max -1-1 --> 3 calls
					uuid = new StringBuffer(uuid.substring(0,31));
				}
				this.m_call = r.getCallFactory().createCall(uuid.toString(), caller, msn, cip, date);
				this.m_call.setAttributes(am);
			}
		}
		return this.m_call;
	}
	
	public static String getAction(String call) {
		String [] data = call.split("\\*");
		if (data!=null && data.length>=2) return data[0].trim();
		return null;
	}

	public static String getLine(String c) {
		String[] call = c.split("\\*");
		if (call.length>=6 && (call[0].trim().equalsIgnoreCase("OUT:") || call[0].trim().equalsIgnoreCase("CID:"))) {
			return call[6];
		}
		if (call.length>=4 && call[0].trim().equalsIgnoreCase("CIDINFO:")) {
			return call[2];
		}
		return null;
	}
	
	public static String getCallState(String c) {
		String[] call = c.split("\\*");
		if (call.length>=4 && call[0].trim().equalsIgnoreCase("CIDINFO:")) {
			if (call[2]!=null && call[2].trim().length()>0) {
				if (call[2].equalsIgnoreCase("0")) return IJAMConst.ATTRIBUTE_VALUE_ACCEPTED;
				if (call[2].equalsIgnoreCase("-1")) return IJAMConst.ATTRIBUTE_VALUE_MISSED;
				if (call[2].equalsIgnoreCase("-2")) return IJAMConst.ATTRIBUTE_VALUE_ACCEPTED;
			}
		}
		return IJAMConst.ATTRIBUTE_VALUE_MISSED;
	}
	
	public static String getKey(ICall call) {
		IAttribute att = call.getAttribute("Ncid.key");
		if (att!=null) return att.getValue();
		return null;
	}
}
