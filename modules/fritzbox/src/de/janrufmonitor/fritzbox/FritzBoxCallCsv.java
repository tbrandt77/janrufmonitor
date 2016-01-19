package de.janrufmonitor.fritzbox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.FritzBoxPhonebookManager;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class FritzBoxCallCsv extends AbstractFritzBoxCall {

	public FritzBoxCallCsv(String csvline, Properties config) {
		super(csvline, config);
	}

	public boolean isOutgoingCall() {
		if (this.m_line==null || this.m_line.length()<2) return false;
		int state = Integer.parseInt(this.m_line.substring(0,1));
		return state == this.getOutgoingState();
	}
	
	public Date getPrecalculatedDate() {
		if (this.m_line==null || this.m_line.trim().length()==0) return null;
		
		/**
		 * Added 2011/01/05: added do to NumberFormatException in log, remove the header
		 */
		if (this.m_line.trim().toLowerCase().startsWith("typ;")) return null;
		try {
			String call[] = this.m_line.split(";");
			if (call.length>=7) {
				// create call data
				SimpleDateFormat sdf = new SimpleDateFormat(this.m_config.getProperty(CFG_DATEFORMAT, "dd.MM.yy HH:mm"));
				try {
					return sdf.parse(call[1]);
				} catch (ParseException e) {
					Logger.getLogger(IJAMConst.DEFAULT_LOGGER).severe("Wrong date format detected.");
				}
			}
		} catch (NumberFormatException ex) {
			Logger.getLogger(IJAMConst.DEFAULT_LOGGER).log(Level.SEVERE, ex.toString(), ex);
		} catch (Exception ex) {
			Logger.getLogger(IJAMConst.DEFAULT_LOGGER).warning(ex.toString() + ":" +ex.getMessage() + " : problem with line parsing : "+this.m_line);
			if (ex instanceof NullPointerException)
				Logger.getLogger(IJAMConst.DEFAULT_LOGGER).log(Level.SEVERE, ex.toString(), ex);
		}
		return null;
	}

	public ICall toCall() {
		if (this.m_line==null || this.m_line.trim().length()==0) return null;
		
		/**
		 * Added 2011/01/05: added do to NumberFormatException in log, remove the header
		 * 
		 * Format: Typ;Datum;Name;Rufnummer;Nebenstelle;Eigene Rufnummer;Dauer
		 */
		if (this.m_line.trim().toLowerCase().startsWith("typ;")) return null;

		if (this.m_call==null) {
			try {
				IRuntime r = PIMRuntime.getInstance();
				String call[] = this.m_line.split(";");
				if (call.length>=7) {
					// create msn
					IMsn msn = r.getCallFactory().createMsn(getMsn(call[5]), "");
					msn.setAdditional(r.getMsnManager().getMsnLabel(msn));
					
					// create caller data
					int state = Integer.parseInt(call[0]);
					
					String callByCall = null;
					ICaller caller = null;

					IPhonenumber pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toClirPhonenumber(call[3].trim());
					// if no CLIR call, check internal
					if (pn==null && state != this.getOutgoingState()) pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toInternalPhonenumber(call[3].trim(), msn.getMSN());
					// if no internal call, check regular
					if (pn==null && state != this.getOutgoingState())  {
						// if incoming call does not start with 0, the Provider number seems to have the wrong format
						// assume it is an international format 4971657110
						boolean onlyNumbers = call[3].matches("[+-]?[0-9]+");
						if (!call[3].startsWith("0")&& onlyNumbers) {
							call[3] = "00"+call[3];
						}
						pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toPhonenumber(call[3].trim(), msn.getMSN());
					}
					if (pn==null && state ==  this.getOutgoingState())  {
						// added 2006/08/10: trim call-by-call information
						// only can occure on state 3/4 (out-going calls)
						callByCall = getCallByCall(call[3]);
						if (callByCall!=null) {
							call[3] = call[3].substring(callByCall.length());
						}
						pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toInternalPhonenumber(call[3].trim(), msn.getMSN());
						if (pn==null) {
							// added 2011/01/13: added areacode additional on special FritzBox mode. Having no leading 0, 
							// requires addition of areacode
							if (!call[3].startsWith("0")) {
								Logger.getLogger(IJAMConst.DEFAULT_LOGGER).info("Assuming number "+call[3]+" has missing areacode.");
								call[3] = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).getAreaCode() + call[3];
								Logger.getLogger(IJAMConst.DEFAULT_LOGGER).info("Added areacode to number "+call[3]);
							}
							pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toPhonenumber(call[3].trim(), msn.getMSN());
						}
					}
					caller = Identifier.identify(r, pn);
					
					if (caller==null) {
						caller = r.getCallerFactory().createCaller(r.getCallerFactory().createName("", call[2]), pn);
					}
					
					// added 2016/01/19: FB Name data if jAnrufmonitor did not find any in his callermanagers
					if (caller.getName().getLastname().length()== 0 && caller.getName().getFirstname().length()== 0 && call[2].trim().length()>0) {
						caller.getAttributes().add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, call[2]));
						caller.getAttributes().add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER, FritzBoxPhonebookManager.ID));
						caller.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_CITY);
					}
					
					// create call data
					SimpleDateFormat sdf = new SimpleDateFormat(this.m_config.getProperty(CFG_DATEFORMAT, "dd.MM.yy HH:mm"));
					Date date = new Date(0);
					try {
						date = sdf.parse(call[1]);
					} catch (ParseException e) {
						Logger.getLogger(IJAMConst.DEFAULT_LOGGER).severe("Wrong date format detected.");
					}
									
					ICip cip = r.getCallFactory().createCip(getCip(call[4]), "");
					cip.setAdditional(r.getCipManager().getCipLabel(cip, ""));
					
					// create attributes
					IAttributeMap am = r.getCallFactory().createAttributeMap();
					
					if (state == 1) {
						am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_ACCEPTED));
					} else if (state == this.getOutgoingState()) {
						am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_OUTGOING));
					} else if (this.hasRejectedState() && state == this.getRejectedState()) {
						am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_REJECTED));
					} else {
						am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_MISSED)); 
					}
					am.add(r.getCallFactory().createAttribute("fritzbox.line", call[4]));
					am.add(r.getCallFactory().createAttribute("fritzbox.duration", Integer.toString(getTime(call[6]))));
					am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_DURATION, Integer.toString(getTime(call[6]))));
					am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_ACTIVE_INDICATOR, IJAMConst.ATTRIBUTE_VALUE_NO));
					if (callByCall!=null)
						am.add(r.getCallFactory().createAttribute("fritzbox.callbycall", callByCall));
					
					if (isSpoofingnumber(call[2])) {
						String sn = getSpoofingnumber(call[2]);
						IPhonenumber pnx = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toPhonenumber(sn.trim());
						ICaller cx = Identifier.identify(r, pnx);
						if (cx!=null) {
							am.add(r.getCallFactory().createAttribute("fritzbox.spoofing", cx.getPhoneNumber().getIntAreaCode()+";"+cx.getPhoneNumber().getAreaCode()+";"+cx.getPhoneNumber().getCallNumber()));
							am.add(r.getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_SPOOFED, "true"));
						}
					}
					
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
					//uuid = new StringBuffer(FritzBoxUUIDManager.getInstance().calculateUUID(uuid.toString()));
					
					this.m_call = r.getCallFactory().createCall(uuid.toString(), caller, msn, cip, date);
					this.m_call.setAttributes(am);
				}
			} catch (NumberFormatException ex) {
				Logger.getLogger(IJAMConst.DEFAULT_LOGGER).log(Level.SEVERE, ex.toString(), ex);
			} catch (Exception ex) {
				Logger.getLogger(IJAMConst.DEFAULT_LOGGER).warning(ex.toString() + ":" +ex.getMessage() + " : problem with line parsing : "+this.m_line);
				if (ex instanceof NullPointerException)
					Logger.getLogger(IJAMConst.DEFAULT_LOGGER).log(Level.SEVERE, ex.toString(), ex);
				return null;
			}
		}
		return this.m_call;
	}

	private int getTime(String t) {
		String[] time = t.split(":");
		if (time.length==2) {
			int r = Integer.parseInt(time[0]) * 60 * 60;
			r += (Integer.parseInt(time[1]) * 60);
			return r;
		}
		return 0;
	}
}
