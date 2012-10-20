package de.janrufmonitor.framework.monitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.classloader.JamCacheMasterClassLoader;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.logging.LoggingInitializer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;

public class PhonenumberAnalyzer {

	private static PhonenumberAnalyzer m_instance;
	
	private IRuntime m_runtime;
	private Logger m_logger;
	
	private PhonenumberAnalyzer() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
	}
	
    public static synchronized PhonenumberAnalyzer getInstance() {
        if (PhonenumberAnalyzer.m_instance == null) {
        	PhonenumberAnalyzer.m_instance = new PhonenumberAnalyzer();
        }
        return PhonenumberAnalyzer.m_instance;
    }
    
    public IPhonenumber createClirPhonenumberFromRaw(String number) {
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
    	if (PhonenumberInfo.isClired(number)) {
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
    
    public IPhonenumber createInternalPhonenumberFromRaw(String number, String msn) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer detected RAW call number: ["+number+"]");
		}
    	// check for internal telephone system prefix
    	if (PhonenumberInfo.isTelephoneSystemPrefix(number)) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected telephone system prefix: ["+number+"]");
    		}
    		number = PhonenumberInfo.truncateTelephoneSystemPrefix(number);
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer removed telephone system prefix: ["+number+"]");
    		}
    	}
    	
		// check for special chars
    	if (!PhonenumberInfo.containsSpecialChars(number)) {
    		if (PhonenumberInfo.isInternalNumber(number)) {
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer detected internal number call: ["+number+"]");
        		}
    			return this.getRuntime().getCallerFactory().createInternalPhonenumber(number);
    		}
    	} else {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected non-digits in number: ["+number+"]");
    		}
    		int truncate = PhonenumberInfo.getTruncateNumber(msn);
    		if (truncate>0) {
    			String number1 = number.trim().substring(PhonenumberInfo.getTruncateNumber(msn), number.trim().length());
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer remove leading non-digits in number: ["+number1+"]");
        		}
    			if (PhonenumberInfo.containsSpecialChars(number1)) {
    				if (this.m_logger.isLoggable(Level.INFO)) {
    					this.m_logger.info("PhonenumberAnalyzer detected still non-digits in number: ["+number+"]");
    					this.m_logger.info("PhonenumberAnalyzer assumes internal number call: ["+number+"]");
            		}
        			return this.getRuntime().getCallerFactory().createInternalPhonenumber(number);
    			}
    		} else {
    			if (PhonenumberInfo.isInternalNumber(number)) {
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
     * @param number a raw phonenumber starting with 0
     * @param msn a msn string
     * @return a valid IPhonenumber object
     */
    public IPhonenumber createPhonenumberFromRaw(String number, String msn) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("PhonenumberAnalyzer detected RAW call number: ["+number+"]");
		}
		
		// automatically determine truncate value on inital state
		if (isInitial()) {
			this.determindeTruncateValue(number);
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
    	if (PhonenumberInfo.isTelephoneSystemPrefix(number)) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected telephone system prefix: ["+number+"]");
    		}
    		// 2011/09/22 added synthetic prefix for non area-code numbers.
    		number = PhonenumberInfo.truncateTelephoneSystemPrefix(number);
			int truncate = PhonenumberInfo.getTruncateNumber(msn);
			if (truncate>0 && PhonenumberInfo.isMissingAreacode(number) && !PhonenumberInfo.containsSpecialChars(number)) {
				for (int i = 0; i<truncate; i++)
					number = "0"+number;
			}
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer removed telephone system prefix: ["+number+"]");
    		}
    	}
		
    	// check for special chars
    	if (!PhonenumberInfo.containsSpecialChars(number)) {
    		if (this.m_logger.isLoggable(Level.INFO)) {
    			this.m_logger.info("PhonenumberAnalyzer detected regular number call: ["+number+"]");
    		}

    		int truncate = PhonenumberInfo.getTruncateNumber(msn);
    		
    		// check for national call number
    		if (truncate>0) {
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer detected truncate option for MSN ["+msn+"]: "+truncate);
        		}
    			number = number.trim().substring(PhonenumberInfo.getTruncateNumber(msn), number.trim().length());	
    			if (this.m_logger.isLoggable(Level.INFO)) {
        			this.m_logger.info("PhonenumberAnalyzer truncated number to ["+number+"]");
        		}
    		}

    		if (!PhonenumberInfo.isMissingAreacode(number) && number.startsWith("0"+PhonenumberInfo.getPrefix())) {
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
    			
    			if (PhonenumberInfo.isMissingAreacode(number)) {
    				number = PhonenumberInfo.getAreaCode() + number;
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
    	if (this.m_logger.isLoggable(Level.WARNING)) {
			this.m_logger.info("PhonenumberAnalyzer cannot handle number: ["+number+"]");
			this.m_logger.info("PhonenumberAnalyzer assumes internal number call: ["+number+"]");
		}
    	return this.getRuntime().getCallerFactory().createInternalPhonenumber(number);
    }
	
	private void determindeTruncateValue(String number) {
		if (!PhonenumberInfo.containsSpecialChars(number) && !PhonenumberInfo.isInternalNumber(number) && !PhonenumberInfo.isClired(number)) {
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
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
	
	public static void main(String[]args) {
		LoggingInitializer.run();
		
		// set Jam classloader
		if (JamCacheMasterClassLoader.getInstance().isValid()) {
			Thread.currentThread().setContextClassLoader(JamCacheMasterClassLoader.getInstance());
			PIMRuntime.getInstance().startup();
		}
		
		String[] numbers = {
				// CLIR
				"", "BLOCKED", "restricted", 
				// internal
				"11", "0***11#*#*12", "0*26", "1", "02", "0***266", "0abc", "003", 
				// normal
				"0151556565", "072651303", "0072651303", "072651303#", "04972651303", "72651303",
				// international
				"001321987654321", "004972651303"} ; 
		
		for (int i=0;i<numbers.length;i++) {
			IPhonenumber n = PhonenumberAnalyzer.getInstance().createClirPhonenumberFromRaw(numbers[i]);
			if (n==null) n = PhonenumberAnalyzer.getInstance().createInternalPhonenumberFromRaw(numbers[i], null);
			if (n==null) n = PhonenumberAnalyzer.getInstance().createPhonenumberFromRaw(numbers[i], null);
			System.out.println("Detected number: "+n.getIntAreaCode()+", "+n.getAreaCode()+", "+n.getCallNumber()+" = "+n.getTelephoneNumber());
		}
		
		PIMRuntime.getInstance().shutdown();
		System.exit(0);
	}
}
