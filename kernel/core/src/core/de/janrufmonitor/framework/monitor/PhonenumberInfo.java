package de.janrufmonitor.framework.monitor;

import java.util.logging.LogManager;

import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.runtime.PIMRuntime;

/**
 * 
 * @author Thilo Brandt
 * @deprecated
 */
public class PhonenumberInfo {

	public static int maxInternalNumberLength() {
		String value = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTERNAL_LENGTH);
		if (value!=null && value.length()>0) {
			try {
				return Integer.parseInt(value);
			} catch (Exception ex) {
				LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER).warning(ex.getMessage());
			}
		}
		return 0;
	}
	
	public static boolean startsWithInternationalPrefix(String number) {
		String pfx = getPrefix() + PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
		return number.startsWith(pfx);
	}
	
	public static String truncateInternationalPrefix(String number) {
		String pfx = getPrefix() + PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
		if (number.startsWith(pfx)) {
			LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER).info("Detetced roaming number: "+number);
			number = number.substring(pfx.length());
		}
		return number;
	}
	
	public static boolean containsSpecialChars(String number) {
		try {
			if (number.length()>=Long.toString(Long.MAX_VALUE).length()) {
				LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER).warning("Number too long: "+number);
				number = number.substring(0,Long.toString(Long.MAX_VALUE).length()-1);				
			}
			Long.parseLong(number);			
		} catch (Exception e) {
			return true;
		}
		return false;
	}
	
	public static boolean isInternalNumber(String number) {
		if (number.trim().length()>=1 && number.trim().length()<=maxInternalNumberLength()) {
			return true;
		}
		return false;
	}
	
	public static boolean isInternalNumber(IPhonenumber pn) {
		if (pn==null)
			return false;
		
		if (pn.isClired())
			return false;
		
		if (!pn.getIntAreaCode().equalsIgnoreCase(IJAMConst.INTERNAL_CALL))
			return false;
					
		String number = pn.getTelephoneNumber();
		
		if (number.trim().length()==0) {
			number = pn.getCallNumber();
		}

		if (number.length()<=maxInternalNumberLength() || pn.getIntAreaCode().equalsIgnoreCase(IJAMConst.INTERNAL_CALL)) {
			return true;
		}
		return false;
	}
	
	public static boolean isClired(String number) {
		if (number.trim().length()==0 || number.trim().equalsIgnoreCase(IJAMConst.CLIRED_CALL) || number.trim().indexOf("BLOCKED")>-1 || number.trim().indexOf("UNKNOWN")>-1) {
			return true;
		}
		return false;
	}
	
	private static int getTruncateNumber() {
		String trunc = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRUNCATE);
		if (trunc==null || trunc.length()==0) trunc = "0";
		return Integer.parseInt(trunc);    
	}
	
	public static int getTruncateNumber(IMsn msn) {
		if (msn==null) return getTruncateNumber();
		return getTruncateNumber(msn.getMSN());    
	}
	
	public static int getTruncateNumber(String msn) {
		if (msn==null) return getTruncateNumber();
		
		String trunc = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, msn + "_" + IJAMConst.GLOBAL_TRUNCATE);
		if (trunc==null || trunc.length()==0) 
			return getTruncateNumber();
		
		return Integer.parseInt(trunc);    
	}
	
	public static String getPrefix(){
		String prefix = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA_PREFIX);
		return (prefix==null ? "0" : prefix);
	}  
	
	public static String getAreaCode() {
		String value = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE);
		if (value==null || value.length()==0) value = "0";
		return value;
	}
	
	public static boolean isMissingAreacode(String num) {
		if (num!=null && !isClired(num)) {
			int min_length = -1;
			int max_length = -1;
			String telephonsystemlength = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTERNAL_LENGTH);
			if (telephonsystemlength!=null && telephonsystemlength.trim().length()>0) {
				min_length = Integer.parseInt(telephonsystemlength);
			}
			telephonsystemlength = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE_ADD_LENGTH);
			if (telephonsystemlength!=null && telephonsystemlength.trim().length()>0) {
				max_length = Integer.parseInt(telephonsystemlength);
			}
			if (min_length<max_length) {				
				return (num.length()>min_length && num.length()<=max_length);
			}
		}
		return false;
	}
	
	public static boolean isTelephoneSystemPrefix(String num) {
		if (num!=null && !isClired(num)) {
			String ts_prefix = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,IJAMConst.GLOBAL_INTERNAL_PREFIX);
			if (ts_prefix!=null && ts_prefix.length()>0) {
				return num.startsWith(ts_prefix);
			}
		}
		return false;
	}
	
	public static String truncateTelephoneSystemPrefix(String num) {
		if (num!=null && !isClired(num)) {
			String ts_prefix = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,IJAMConst.GLOBAL_INTERNAL_PREFIX);
			if (ts_prefix!=null && ts_prefix.length()>0) {
				if (num.startsWith(ts_prefix))
					return num.substring(ts_prefix.length());
			}
		}
		return num;
	}
}
