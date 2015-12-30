package de.janrufmonitor.repository;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.CharacterFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.PhonenumberFilter;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;

/**
 *  This abstract class can be used as base class for a new caller manager implementation which
 *  is supporting configuration.
 *
 *@author     Thilo Brandt
 *@created    2003/11/02
 */
public abstract class AbstractConfigurableCallerManager implements ICallerManager, IConfigurable {

	protected String CFG_PRIO = "priority";
	protected String CFG_ENABLED = "enabled";

	protected Properties m_configuration;
	protected Logger m_logger;
	protected String m_externalID; 

	private boolean isStarted;

	public AbstractConfigurableCallerManager() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
	}

	public boolean isActive() {
		return this.m_configuration.getProperty(CFG_ENABLED, "").equalsIgnoreCase("true");
	}

	public int getPriority() {
		int value = 0;
		try {
			String prio = this.m_configuration.getProperty(CFG_PRIO, "0");
			value = Integer.parseInt(prio);
		} catch (Exception ex) {
			this.m_logger.warning("Priority for manager <"+this.getID()+"> could not be read: " + ex.toString());
		}
		return value;  
	}
	
	public String getConfigurableID() {
		return this.getID();
	}

	public void setConfiguration(Properties configuration) {
		this.m_configuration = configuration;
		
		if (this.isActive() && this.getRuntime().getCallerManagerFactory().isManagerAvailable(this.getID()))
			this.restart();
	}
		
	/**
	 * Gets the runtime objects.
	 * 
	 * @return the current runtime object.
	 */
	public abstract IRuntime getRuntime();
	
	/**
	 * Gets the ID of the new caller manager. The ID is taken for registration at 
	 * caller manager factory and at the configurable notifier.
	 * 
	 * @return caller manager ID
	 */
	public abstract String getID();

	public abstract String getNamespace();
	
	/**
	 * Check for internal phone numbers
	 * 
	 * @param pn
	 * @return
	 * @deprecated use PhonenumberAnalyzer.getInstance(this.getRuntime()).isInternal(number) instead
	 */
	protected boolean isInternalNumber(IPhonenumber pn) {
		return PhonenumberAnalyzer.getInstance(this.getRuntime()).isInternal(pn);
	}

	public void setManagerID(String id) { 
		this.m_externalID = id;
	}

	public String getManagerID() {
		return this.getID();
	}

	public void restart() {
		if (this.isStarted)
			this.shutdown();
		this.startup();
	}

	public void shutdown() { 
		this.isStarted = false;
	}

	public void startup() { 
		this.isStarted = true;
	}

	public String toString() {
		return this.getID();
	}
	
	protected void addCreationAttributes(ICaller c) {
	    String value = null;
	    if (!c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_MACHINE_NAME)) {
	        try {
				value = InetAddress.getLocalHost().getHostName();
				c.setAttribute(
					this.getRuntime().getCallFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_MACHINE_NAME,
							value
						)	
					);
			} catch (UnknownHostException e) {
				this.m_logger.warning(e.getMessage());
			}
	    }

	    if (!c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_MACHINE_IP)) {
	        try {
				value = InetAddress.getLocalHost().getHostAddress();
				c.setAttribute(
					this.getRuntime().getCallFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_MACHINE_IP,
							value
						)	
					);
			} catch (UnknownHostException e) {
				this.m_logger.warning(e.getMessage());
			}
	    }
		
	    if (!c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_USER_ACCOUNT)) {
			value = System.getProperty("user.name");
			if (value!=null && value.length()>0) {
				c.setAttribute(
					this.getRuntime().getCallFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_USER_ACCOUNT,
							value
						)	
					);
			}
	    }
	    
	    if (!c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_CREATION)) {
			value = Long.toString(System.currentTimeMillis());
			if (value!=null && value.length()>0) {
				c.setAttribute(
					this.getRuntime().getCallFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_CREATION,
							value
						)	
					);
			}
	    }
	}
	
	protected void addSystemAttributes(ICaller c) {
		IAttribute cm = this.getRuntime().getCallerFactory().createAttribute(
			IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
			this.getID()
		);
		c.getAttributes().add(cm);
	}
	
	protected void applyFilters(ICallerList cl, IFilter[] filters) {
		if (cl==null) return;
		if (filters == null) return;
		IFilter f = null;
		for (int i=0;i<filters.length;i++) {
			f = filters[i];
			if (f.getType()==FilterType.CHARACTER) {
				CharacterFilter cf = ((CharacterFilter)f);
				ICaller c = null;
				for (int j=cl.size()-1;j>=0;j--) {
					c = cl.get(j);
					if (!c.getAttributes().contains(cf.getAttributeName())) {
						cl.remove(c);
					} else if (c.getAttributes().contains(cf.getAttributeName())) {
						if (!c.getAttribute(cf.getAttributeName()).getValue().startsWith(cf.getCharacter())) {
							cl.remove(c);
						}
					}
				}
			}
			if (f.getType()==FilterType.PHONENUMBER) {
				PhonenumberFilter cf = ((PhonenumberFilter)f);
				ICaller c = null;
				for (int j=cl.size()-1;j>=0;j--) {
					c = cl.get(j);
					if (!c.getPhoneNumber().getIntAreaCode().equalsIgnoreCase(cf.getPhonenumber().getIntAreaCode())) {
						cl.remove(c);
					}
					else if (!c.getPhoneNumber().getAreaCode().equalsIgnoreCase(cf.getPhonenumber().getAreaCode())) {
						cl.remove(c);
					}
				}
			}
			if (f.getType()==FilterType.ATTRIBUTE) {
				AttributeFilter cf = ((AttributeFilter)f);
				ICaller c = null;
				for (int j=cl.size()-1;j>=0;j--) {
					c = cl.get(j);
					IAttributeMap atts = cf.getAttributeMap();
					Iterator iter = atts.iterator();
					IAttribute a = null;
					while (iter.hasNext()) {
						a = (IAttribute) iter.next();
						if (!c.getAttributes().contains(a)) {
							cl.remove(c);
						} else if (c.getAttributes().contains(a) && !c.getAttribute(a.getName()).getValue().equalsIgnoreCase(a.getValue())) {
							cl.remove(c);
						}
					}
				}
			}
		}
	}

}
