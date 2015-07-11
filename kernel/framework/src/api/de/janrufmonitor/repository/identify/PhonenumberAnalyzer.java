package de.janrufmonitor.repository.identify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class PhonenumberAnalyzer {

	private static PhonenumberAnalyzer m_instance;
	
	private IRuntime m_runtime;
	private Logger m_logger;
	
	private PhonenumberAnalyzer(IRuntime r) {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_runtime = r;
	}
	
    public static synchronized PhonenumberAnalyzer getInstance(IRuntime r) {
        if (PhonenumberAnalyzer.m_instance == null) {
        	PhonenumberAnalyzer.m_instance = new PhonenumberAnalyzer(r);
        }
        return PhonenumberAnalyzer.m_instance;
    }
    /**
     * Creates a CLIR phone number object out of a number string
     * 
     * @param number a phone number as String representation
     * @return IPhonenumber object with CLIR state
     */
    public IPhonenumber toClirPhonenumber(String number) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer detected RAW call number: ["+number+"]");
			File rawLog = new File(PathResolver.getInstance(getRuntime()).getLogDirectory(), "raw-number.log");
			try {
				FileOutputStream fos = new FileOutputStream(rawLog, true);
				fos.write(number.getBytes());
				fos.write(IJAMConst.CRLF.getBytes());
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
    	
    	// check CLIR call
    	if (isClired(number)) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected CLIR call: ["+number+"]");
    		}
    		return getRuntime().getCallerFactory().createClirPhonenumber();
    	}
    	
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer NOT detected as CLIR number: ["+number+"]");
		}
    	return null;
    }
    
    /**
     * Creates an internal number object out of a String representation. No MSN specific truncate options are considered.
     * 
     * @param number a String representation of a phone number
     * @return a IPhonenumber object as internal number representation
     */
    public IPhonenumber toInternalPhonenumber(String number) {
    	return toInternalPhonenumber(number, null);
    }
    
    /**
     * Creates an internal number object out of a String representation. MSN specific truncate options are considered.
     * 
     * @param number a String representation of a phone number
     * @param msn MSN as String for MSN specific truncate options, null to ignore MSN options.
     * @return a IPhonenumber object as internal number representation
     */
    public IPhonenumber toInternalPhonenumber(String number, String msn) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer detected RAW call number: ["+number+"]");
		}
    	// check for internal telephone system prefix
    	if (hasTelephoneSystemPrefix(number)) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected telephone system prefix: ["+number+"]");
    		}
    		number = truncateTelephoneSystemPrefix(number);
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer removed telephone system prefix: ["+number+"]");
    		}
    	}
    	
		// check for special chars
    	if (!containsSpecialChars(number)) {
    		if (isInternal(number)) {
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer detected internal number call: ["+number+"]");
        		}
    			return this.getRuntime().getCallerFactory().createInternalPhonenumber(number);
    		}
    	} else {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected non-digits in number: ["+number+"]");
    		}
    		int truncate = getTruncate(msn);
    		if (truncate>0) {
    			String number1 = number.trim().substring(getTruncate(msn), number.trim().length());
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer remove leading non-digits in number: ["+number1+"]");
        		}
    			if (containsSpecialChars(number1)) {
    				if (this.m_logger.isLoggable(Level.INFO)) {
    					this.m_logger.info("PhonenumberAnalyzer detected still non-digits in number: ["+number+"]");
    					this.m_logger.info("PhonenumberAnalyzer assumes internal number call: ["+number+"]");
            		}
        			return this.getRuntime().getCallerFactory().createInternalPhonenumber(number);
    			}
    		} else {
    			if (isInternal(number)) {
        			if (this.m_logger.isLoggable(Level.INFO)) {
            			this.m_logger.info("PhonenumberAnalyzer detected internal number call: ["+number+"]");
            		}
        			return this.getRuntime().getCallerFactory().createInternalPhonenumber(number);
        		}
    		}
    	}
    	
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer NOT detected as internal number: ["+number+"]");
		}
    	return null;
    }
    
    /**
     * Creates a valid IPhonenumber object out of a number string. 
     * MSN is used for truncate option and is optional. MSN could be null for default truncate.
     * 
     * @param number a raw phone number starting with leading 0
     * @param msn a MSN String
     * @return a valid IPhonenumber object
     */
    public IPhonenumber toPhonenumber(String number, String msn) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer detected RAW call number: ["+number+"]");
		}
		
		// automatically determine truncate value on inital state
		if (isInitial()) {
			this.calculateTruncate(number);
		}
		
		// remove hash # at end of number
		if (number.endsWith("#")) {
			if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected # at end of number: ["+number+"]");
    		}
			number = number.substring(0, number.length()-1);
			if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer removed # at end of number: ["+number+"]");
    		}
		}
    	
		// check for internal telephone system prefix
    	if (hasTelephoneSystemPrefix(number)) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected telephone system prefix: ["+number+"]");
    		}
    		// 2011/09/22 added synthetic prefix for non area-code numbers.
    		number = truncateTelephoneSystemPrefix(number);
			int truncate = getTruncate(msn);
			if (truncate>0 && hasMissingAreaCode(number) && !containsSpecialChars(number)) {
				for (int i = 0; i<truncate; i++)
					number = "0"+number;
			}
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer removed telephone system prefix: ["+number+"]");
    		}
    	}
		
    	// check for special chars
    	if (!containsSpecialChars(number)) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected regular number call: ["+number+"]");
    		}

    		int truncate = getTruncate(msn);
    		
    		// check for national call number
    		if (truncate>0) {
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer detected truncate option for MSN ["+msn+"]: "+truncate);
        		}
    			number = number.trim().substring(getTruncate(msn), number.trim().length());	
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer truncated number to ["+number+"]");
        		}
    		}

    		if (!hasMissingAreaCode(number) && number.startsWith("0"+getIntAreaPrefix())) {
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer detected number starts with international prefix: ["+number+"]");
        		}
    			number = number.substring(1);
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer removed international prefix from number: ["+number+"]");
        		}
    			return getRuntime().getCallerFactory().createPhonenumber(number);   
    		}
    		
    		if (!number.startsWith("0")) { // needed for Fritz!Box variant
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer detected number without leading 0. Assuming local number. ["+number+"]");
        		}
    			
    			if (hasMissingAreaCode(number)) {
    				number = getAreaCode() + number;
    				if (this.m_logger.isLoggable(Level.INFO)) {
            			this.m_logger.info("PhonenumberAnalyzer added areacode to number due to number length. ["+number+"]");
            		}
    			} else {
    				number = "0" + number;
    				if (this.m_logger.isLoggable(Level.INFO)) {
            			this.m_logger.info("PhonenumberAnalyzer added 0 to number. ["+number+"]");
            		}
    			}
			}
			return getRuntime().getCallerFactory().createPhonenumber((number.startsWith("0") ? number.substring(1) : number));    		
    	}
    	if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer cannot handle number: ["+number+"]");
			this.m_logger.info("PhonenumberAnalyzer assumes internal number call: ["+number+"]");
		}
    	return this.getRuntime().getCallerFactory().createInternalPhonenumber(number);
    }
    
    /**
     * Creates a valid IPhonenumber object out of a number string. This methods ignores MSN specific settings.
     * 
     * @param number a raw phone number starting with leading 0
     * @return a valid IPhonenumber object
     */
    public IPhonenumber toPhonenumber(String number) {
    	return this.toPhonenumber(number, null);
    }
    
    /**
     * Creates a valid IPhonenumber object out of a number string. This methods ignores MSN specific settings. The IPhonenumber object 
     * is also being identified and phone number is split up into 3 parts: international are code e.g. 49, area code 7261 and phone number 123456789
     * 
     * @param number a raw phone number starting with leading 0
     * @return a valid IPhonenumber object which is split up into international area code, area code and number
     */
    public IPhonenumber toIdentifiedPhonenumber(String number) {
    	long start = System.currentTimeMillis();
    	if (this.m_logger.isLoggable(Level.INFO)) 
    		this.m_logger.info("<---- Begin number identification ---->");		
    	ICaller c = Identifier.identifyDefault(getRuntime(), this.toPhonenumber(this.normalize(number, false), null));
    	if (c!=null) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("PhonenumberAnalyzer split number to: ["+c.getPhoneNumber().toString()+"]");
				this.m_logger.info("<---- Finished number identification ("+(System.currentTimeMillis()-start)+" msec.) ---->");	
    		}
    		return c.getPhoneNumber();
    	}
    	if (this.m_logger.isLoggable(Level.INFO)) {
    		this.m_logger.info("PhonenumberAnalyzer did NOT identify number: ["+number+"], normalized: ["+this.normalize(number)+"]");
    		this.m_logger.info("<---- Finished number identification ("+(System.currentTimeMillis()-start)+" msec.) ---->");	
    	}
    	return null;
    }


	/**
	 * Normalizes a formatted number to a PIM compliant 
	 * number string.
	 * <br><br>
	 * Examples:<br>
	 * Source format: +49 (1234) 567890<br>
	 * Target format: 0491234567890 (international format)<br><br>
	 * or<br><br>
	 * Source format: (01234) 567890<br>
	 * Target format: 1234567890 (national format)<br>
	 * 
	 * @param phone
	 * @return
	 */
	public String normalize(String phone) {
		return this.normalize(phone, true);
	}
	
	
	/**
	 * Normalizes a formatted number to a PIM compliant 
	 * number string.
	 * <br><br>
	 * Examples:<br>
	 * Source format: +49 (1234) 567890<br>
	 * Target format: 0491234567890 (international format)<br><br>
	 * or<br><br>
	 * Source format: (01234) 567890<br>
	 * Target format: 1234567890 (national format)<br>
	 * 
	 * remove leading zero as an option.
	 * 
	 * @param phone a String representation of a phone number
	 * @param trimLeadingZero one leading zero could be remove if set to true
	 * @return a normalized String representation of a phone number
	 */
	public String normalize(String phone, boolean trimLeadingZero) {
		phone = phone.trim();
		
		if (trimLeadingZero && phone.startsWith("0")) {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("PhonenumberAnalyzer trims leading zero: ["+phone+"]");
			phone = phone.substring(1);
		}

		// added 2009/07/02
		phone = StringUtils.replaceString(phone, "*31#", ""); // remove CLIR symbol in caller number
		phone = StringUtils.replaceString(phone, "#31#", ""); // remove CLIR symbol in caller number
		phone = StringUtils.replaceString(phone, " ", "");
		phone = StringUtils.replaceString(phone, "/", "");
		phone = StringUtils.replaceString(phone, "(0", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, ")", "");
		phone = StringUtils.replaceString(phone, "-", "");
		phone = StringUtils.replaceString(phone, "#", "");
		phone = StringUtils.replaceString(phone, ".", "");
		phone = StringUtils.replaceString(phone, "+", "0");

		return phone;
	}
	
	/**
	 * Formats a string with number information in a callable
	 * format.
	 * Example: +4972657110 --> 004972657110
	 *          +49 (7165) 7110 --> 004972657110
	 *  
	 * @param phone
	 * @return
	 */
	public String toCallable(String phone) {
		phone = phone.trim();

		phone = StringUtils.replaceString(phone, " ", "");
		phone = StringUtils.replaceString(phone, "/", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, ")", "");
		phone = StringUtils.replaceString(phone, "-", "");
		// removed 2009/07/02
		//phone = StringUtils.replaceString(phone, "#", "");
		phone = StringUtils.replaceString(phone, ".", "");
		phone = StringUtils.replaceString(phone, "+", "00");

		return phone;
	}
	
	/**
	 * Checks if a number starts with the international prefix
	 * 
	 * @param number a String representation of a phone number
	 * @return true is number starts with international prefix
	 */
	public boolean hasInternationalPrefix(String number) {
		String pfx = getIntAreaPrefix() + this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
		if (this.m_logger.isLoggable(Level.INFO)) 
			this.m_logger.info("PhonenumberAnalyzer international prefix is set to: ["+pfx+"], number is: ["+number+"]");
		return number.startsWith(pfx);
	}

	/**
	 * Checks wether a number String contains the configured telephone system prefix
	 * 
	 * @param num a String representation of a phone number to check for the prefix
	 * @return true if number starts with prefix
	 */
	public boolean hasTelephoneSystemPrefix(String num) {
		if (num!=null && !isClired(num)) {
			String ts_prefix = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,IJAMConst.GLOBAL_TELEPHONESYSTEM_PREFIX);
			if (ts_prefix!=null && ts_prefix.length()>0) {
				if (this.m_logger.isLoggable(Level.INFO)) 
					this.m_logger.info("PhonenumberAnalyzer detects telephone systenm prefix: ["+ts_prefix+", "+num+"]");
				return num.startsWith(ts_prefix);
			}
		}
		return false;
	}

	/**
	 * Checks wether a phone number as String representation lacks an area code.
	 * 
	 * @param num a String representation of a phone number to check
	 * @return true if area code is missing
	 */
	public boolean hasMissingAreaCode(String num) {
		if (num!=null && !isClired(num)) {
			int min_length = -1;
			int max_length = -1;
			String telephonsystemlength = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTERNAL_LENGTH);
			if (telephonsystemlength!=null && telephonsystemlength.trim().length()>0) {
				min_length = Integer.parseInt(telephonsystemlength);
			}
			telephonsystemlength = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE_ADD_LENGTH);
			if (telephonsystemlength!=null && telephonsystemlength.trim().length()>0) {
				max_length = Integer.parseInt(telephonsystemlength);
			}
			if (min_length<max_length) {				
				return (num.length()>min_length && num.length()<=max_length);
			}
		}
		return false;
	}

	/**
	 * Checks if a number IPhonenumber object is a CLIR call
	 * 
	 * @param ph a IPhonenumber object of a phone number to be checked
	 * @return true if number is a CLIR call
	 */
	public boolean isClired(IPhonenumber pn) {
		return pn.isClired();
	}
	
	/**
	 * Checks if a number String representation is a CLIR call
	 * 
	 * @param number a String representation of a phone number to be checked
	 * @return true if number is a CLIR call
	 */
	public boolean isClired(String number) {
		if (number.trim().length()==0 || number.trim().equalsIgnoreCase(IJAMConst.CLIRED_CALL) || number.trim().indexOf("BLOCKED")>-1 || number.trim().indexOf("UNKNOWN")>-1) {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("PhonenumberAnalyzer detetced number as CLIR call: ["+number+"]");
			return true;
		}
		return false;
	}

	/**
	 * Checks if a number IPhonenumber object is an internal number, e.g. intAreaCode is set to IJAMConst.INTERNAL_CALL
	 * 
	 * @param pn a Phonenumber object to check
	 * @return true if Phonenumber object representa an internal number
	 */
	public boolean isInternal(IPhonenumber pn) {
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
	
		if (number.length()<=getInternalNumberMaxLength() || pn.getIntAreaCode().equalsIgnoreCase(IJAMConst.INTERNAL_CALL)) {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("PhonenumberAnalyzer detetced number as internal number: ["+number+"]");
			return true;
		}
		return false;
	}

	/**
	 * Checks if a number String is an internal number, e.g. number string exceed max length of defined internal numbers
	 * 
	 * @param number a String representation of a phone number top check
	 * @return true if number length is smaller then max configured length of internal numbers
	 */
	public boolean isInternal(String number) {
		if (number.trim().length()>=1 && number.trim().length()<=getInternalNumberMaxLength()) {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("PhonenumberAnalyzer detetced number as internal number: ["+number+"]");
			return true;
		}
		return false;
	}

	/**
	 * Get the max length of an internal number String. All lengths smaller than max length are treated as internal numbers.
	 * 
	 * @return int value of max length.
	 */
	public int getInternalNumberMaxLength() {
		String value = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTERNAL_LENGTH);
		if (value!=null && value.length()>0) {
			try {
				if (this.m_logger.isLoggable(Level.INFO)) 
					this.m_logger.info("PhonenumberAnalyzer InternalNumber max length set to: ["+Integer.parseInt(value)+"]");
				return Integer.parseInt(value);
			} catch (Exception ex) {
				if (this.m_logger.isLoggable(Level.WARNING)) 
					this.m_logger.warning(ex.getMessage());
			}
		}
		return 0;
	}
	
	/**
	 * Truncates the international prefix
	 * 
	 * @param number a String representation of a phone number
	 * @return truncated number without international prefix
	 */
	public String truncateInternationalPrefix(String number) {
		String pfx = getIntAreaPrefix() + this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
		if (number.startsWith(pfx)) {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("PhonenumberAnalyzer detetced roaming number: ["+number+"]");
			number = number.substring(pfx.length());
		}
		return number;
	}
	
	/**
	 * Returns the truncated number without telephone system prefix.
	 * 
	 * @param num a String representation of a phone number to truncate the prefix from
	 * @return a truncated phone number as String
	 */
	public String truncateTelephoneSystemPrefix(String num) {
		if (num!=null && !isClired(num)) {
			String ts_prefix = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,IJAMConst.GLOBAL_TELEPHONESYSTEM_PREFIX);
			if (ts_prefix!=null && ts_prefix.length()>0) {
				if (num.startsWith(ts_prefix)) {
					if (this.m_logger.isLoggable(Level.INFO)) 
						this.m_logger.info("PhonenumberAnalyzer truncates telephone systenm prefix: ["+ts_prefix+", "+num+"]");
					return num.substring(ts_prefix.length());
				}
			}
		}
		return num;
	}

	/**
	 * Checks if a number String contains special characters or just contains out of numbers
	 * 
	 * @param number a String representation of a phone number top check
	 * @return true if non-digit numbers or special characters are detected
	 */
	public boolean containsSpecialChars(String number) {
		try {
			if (number.length()>=Long.toString(Long.MAX_VALUE).length()) {
				if (this.m_logger.isLoggable(Level.WARNING)) 
					this.m_logger.warning("PhonenumberAnalyzer number too long: ["+number+"]");
				number = number.substring(0,Long.toString(Long.MAX_VALUE).length()-1);				
			}
			Long.parseLong(number);			
		} catch (Exception e) {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("PhonenumberAnalyzer detetced special characters: ["+number+"]");
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the count of digits to be truncated based on an MSN specific configuration
	 * 
	 * @param msn a Msn object to check, or null if not applicable
	 * @return count of truncatable digits
	 */
	public int getTruncate(IMsn msn) {
		if (msn==null) return getTruncate((String) null);
		return getTruncate(msn.getMSN());    
	}
	
	/**
	 * Gets the count of digits to be truncated based on an MSN specific configuration
	 * 
	 * @param msn a MSN as String representation to check, or null if not applicable
	 * @return count of truncatable digits
	 */
	public int getTruncate(String msn) {
		String trunc = null;
		if (msn!=null && msn.length()>0) {
			trunc = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, msn + "_" + IJAMConst.GLOBAL_TRUNCATE);
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("PhonenumberAnalyzer detetced MSN specific truncate value: ["+msn+", "+trunc+"]");
			if (trunc!=null && trunc.length()>0) 
				return Integer.parseInt(trunc);    
		}

		trunc = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRUNCATE);
		if (trunc==null || trunc.length()==0) trunc = "0";
		
		if (this.m_logger.isLoggable(Level.INFO)) 
			this.m_logger.info("PhonenumberAnalyzer detetced truncate value: ["+trunc+"]");
		
		return Integer.parseInt(trunc);
	}
	
	/**
	 * Returns the international area code code prefix, default: 0
	 * 
	 * @return valid prefix or 0 (zero)
	 */
	public String getIntAreaPrefix(){
		String prefix = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA_PREFIX);
		if (this.m_logger.isLoggable(Level.INFO)) 
			this.m_logger.info("PhonenumberAnalyzer configured int area code prefix: ["+prefix+"]");
		return (prefix==null ? "0" : prefix);
	}  
	
	/**
	 * Returns the configured area code
	 * 
	 * @return a valid area code or 0 (zero)
	 */
	public String getAreaCode() {
		String value = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE);
		if (this.m_logger.isLoggable(Level.INFO)) 
			this.m_logger.info("PhonenumberAnalyzer configured area code: ["+value+"]");
		return ((value==null || value.length()==0) ? "0" : value );
	}
	
	private void calculateTruncate(String number) {
		if (!containsSpecialChars(number) && !isInternal(number) && !isClired(number)) {
			// check for telephone system prefix
			int tsp_count = 0;
			String tsp = "";
			while (!number.startsWith("0") && number.length()>tsp_count && tsp_count<6) {
				tsp += number.substring(0,1);
				number = number.substring(1);
				tsp_count++;
			}
			if (tsp.length()>0 && tsp.length()<5) {
				this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TELEPHONESYSTEM_PREFIX, tsp);
			}
			
			// check for truncate digits
			int t_count = -1;
			while (number.startsWith("0") && number.length()>t_count){
				number = number.substring(1);
				t_count++;
			}
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRUNCATE, Integer.toString((t_count>0 ? t_count : 0)));
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE_ADD_LENGTH, "6");
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AUTO_ANALYZE_NUMBER, "false");
			this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();	
		}
	}

	private boolean isInitial() {
		return !this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AUTO_ANALYZE_NUMBER).equalsIgnoreCase("false");
	}

	private IRuntime getRuntime() {
		return this.m_runtime;
	}

}
