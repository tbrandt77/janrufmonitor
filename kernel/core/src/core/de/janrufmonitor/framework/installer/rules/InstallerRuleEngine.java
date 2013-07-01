package de.janrufmonitor.framework.installer.rules;

import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;

public class InstallerRuleEngine {

	private static String NAMESPACE = "installer.InstallerEngine";
	
	private static InstallerRuleEngine m_instance = null;

	private Logger m_logger;
	private List m_rules;
	
	private InstallerRuleEngine() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.addRules();
	}

    public static synchronized InstallerRuleEngine getInstance() {
        if (InstallerRuleEngine.m_instance == null) {
        	InstallerRuleEngine.m_instance = new InstallerRuleEngine();
        }
        return InstallerRuleEngine.m_instance;
    }
    
    private void addRules() {
    	this.m_rules = new ArrayList();
    	this.m_rules.add(new PIMVersionRule());
    	this.m_rules.add(new ModuleVersionRule());
    	this.m_rules.add(new ModuleArchitectureRule());
    	this.m_rules.add(new DependencyRule());
    	this.m_rules.add(new ConflictRule());
    }
    
    public boolean isValid(Properties descriptor) {
    	IInstallerRule r = null;
    	for (int i=0,j=this.m_rules.size();i<j;i++) {
    		r = (IInstallerRule) this.m_rules.get(i);
    		this.m_logger.info("Installer Rule to be processed: "+r.toString());
    		
    		try {
    			r.validate(descriptor);
    		} catch (InstallerRuleException e) {
    			this.m_logger.warning("Rule <"+r.toString()+"> validation failed: "+e.getMessage());
    	    	PropagationFactory.getInstance().fire(
	        		new Message(Message.ERROR, NAMESPACE, e.getMessageID(), e)
	    		);
    	    	return false;
    		}
     	}
    	return true;
    }
    
    public boolean isValidHidden(Properties descriptor) {
    	IInstallerRule r = null;
    	for (int i=0,j=this.m_rules.size();i<j;i++) {
    		r = (IInstallerRule) this.m_rules.get(i);
    		this.m_logger.info("Installer Rule to be processed: "+r.toString());
    		try {
    			r.validate(descriptor);
    		} catch (InstallerRuleException e) {
    			this.m_logger.warning("Rule <"+r.toString()+"> validation failed: "+e.getMessage());
    			return false;
    		}
    	}
    	return true;
    }
}
