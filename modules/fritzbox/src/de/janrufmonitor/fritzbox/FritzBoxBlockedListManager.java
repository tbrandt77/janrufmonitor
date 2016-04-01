package de.janrufmonitor.fritzbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.GetBlockedListException;

public class FritzBoxBlockedListManager {

	private static FritzBoxBlockedListManager m_instance = null;
	
	private Logger m_logger;

	private List m_blockedPhones;
	
    private FritzBoxBlockedListManager() {
        this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
        this.m_blockedPhones = new ArrayList(1);
        this._init();
    }
    
    public static synchronized FritzBoxBlockedListManager getInstance() {
        if (FritzBoxBlockedListManager.m_instance == null) {
        	FritzBoxBlockedListManager.m_instance = new FritzBoxBlockedListManager();
        }
        return FritzBoxBlockedListManager.m_instance;
    }
    
    public static synchronized void invalidate() {
    	if (m_instance!=null)
    		m_instance._invalidate();
    	m_instance = null;
    }

    private void _invalidate() {
    	if (this.m_blockedPhones!=null)
    		this.m_blockedPhones.clear();
    	this.m_blockedPhones = null;
    }
    
    private void _init() {
    	FirmwareManager fwm = FirmwareManager.getInstance();
		try {
			if (!fwm.isLoggedIn())
				fwm.login();						
			this.m_blockedPhones.addAll(fwm.getBlockedList());						
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (FritzBoxLoginException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (GetBlockedListException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    public boolean isBlocked(String n) {
    	boolean isBlocked = false;
    	
    	isBlocked = (this.m_blockedPhones != null && this.m_blockedPhones.contains(n));
    	if (!isBlocked && this.m_blockedPhones != null) {
    		String _n = null;
    		for (int i=0;i<this.m_blockedPhones.size();i++) {
    			_n = (String) this.m_blockedPhones.get(i);
    			if (n.startsWith(_n)) {
    				if (m_logger.isLoggable(Level.INFO))
    					this.m_logger.info("Number "+n+" is blocked by prefix "+_n);
    				return true;
    			}
    		}
    	}
    	return isBlocked;
    }
}
