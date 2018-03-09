package de.janrufmonitor.fritzbox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.PIMRuntime;

public class FritzBoxUUIDManager {

	private static FritzBoxUUIDManager m_instance = null;
	
	private Logger m_logger;
	private String m_uuid;
	private String m_prevuuid;
	private long m_lastcheck;
    
    private FritzBoxUUIDManager() {
        this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
        this.m_uuid = "0";
        this.m_prevuuid = "0";
        this.m_lastcheck = System.currentTimeMillis();
    }
    
    public static synchronized FritzBoxUUIDManager getInstance() {
        if (FritzBoxUUIDManager.m_instance == null) {
        	FritzBoxUUIDManager.m_instance = new FritzBoxUUIDManager();
        }
        return FritzBoxUUIDManager.m_instance;
    }
    
    public String getUUID(String d, String n, String m) {
    	Date date = null;

    	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm");
		try {
			date = sdf.parse(d);
		} catch (ParseException e) {
			return null;
		}
		
		IPhonenumber pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toIdentifiedPhonenumber(n, true);
    	IMsn msn = PIMRuntime.getInstance().getCallFactory().createMsn(m, "");
    	return this.getUUID(date, pn, msn);
    }
    
    public String getUUID(Date d, IPhonenumber n, IMsn msn) {
    	StringBuffer uuid = new StringBuffer();
		uuid.append(d.getTime());
		uuid.append("-");
		uuid.append(n.getTelephoneNumber());
		uuid.append("-");
		uuid.append(msn.getMSN());
		// limit uuid to 32 chars
		if (uuid.length()>31) {
			// reduce byte length to append -1 for redundant calls max -1-1 --> 3 calls
			uuid = new StringBuffer(uuid.substring(0,31));
		}
    	return uuid.toString();
    }
    
    public String calculateUUID(String uuid) {
    	// check wether 1 minute is already over
    	if (this.isTimeElapsed()) {
    		this.m_uuid = "0";
    		this.m_prevuuid = "0";
    	}
    	
    	if (uuid.equalsIgnoreCase(this.m_uuid) && !uuid.equalsIgnoreCase(this.m_prevuuid)) {
    		this.m_prevuuid = uuid;
    		uuid += "-1";
    		this.m_logger.info("New 1st UUID calculated: "+uuid);
    	}
    	if (uuid.equalsIgnoreCase(this.m_prevuuid)) {
    		uuid += "-1-1";
    		this.m_logger.info("New 2nd UUID calculated: "+uuid);
    	}    	
    	this.m_uuid = uuid;
    	
    	this.m_lastcheck = System.currentTimeMillis();
    	return uuid;
    }
    
    private boolean isTimeElapsed() {
    	return (System.currentTimeMillis() - this.m_lastcheck > 60000); 
    }
    
    public void init() {
    	this.m_uuid = "0";
    	this.m_prevuuid = "0";
        this.m_lastcheck = System.currentTimeMillis();
    }
}
