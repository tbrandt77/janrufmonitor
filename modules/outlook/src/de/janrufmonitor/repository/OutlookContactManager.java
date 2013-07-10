package de.janrufmonitor.repository;

import java.util.logging.Level;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.PhonenumberFilter;
import de.janrufmonitor.repository.imexport.ITracker;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.repository.types.IWriteCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class OutlookContactManager extends AbstractReadWriteCallerManager implements IRemoteRepository, ITracker {

	private static String ID = "OutlookCallerManager";
	public static String NAMESPACE = "repository.OutlookCallerManager";
	
	private static String CFG_TS ="lastsync";
	private static String CFG_INDEX ="index";
	private static String CFG_MODE ="mode";
	
	private IRuntime m_runtime;
	private OutlookContactProxy m_proxy;
	private boolean m_isUUIDCheck;

	public OutlookContactManager() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}
	
	public String getID() {
		return OutlookContactManager.ID;
	}

	public String getNamespace() {
		return OutlookContactManager.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public ICaller getCaller(IPhonenumber number) throws CallerNotFoundException {
		if (number == null)
			throw new CallerNotFoundException(
					"Phone number is not set (null). No caller found.");

		if (number.isClired())
			throw new CallerNotFoundException(
					"Phone number is CLIR. Identification impossible.");
		
//		if (this.isInternalNumber(number))
//			throw new CallerNotFoundException(
//				"Phone number is internal phone system number. Identification not possible.");

		try {
			ICaller c = getProxy().findContact(number);
			if (c!=null) return c;
		} catch (OutlookContactProxyException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		throw new CallerNotFoundException(
				"No caller entry found for phonenumber : "
						+ number.getTelephoneNumber());
	}

	public ICallerList getCallers(IFilter filter) {
		try {
			if (filter!=null && filter.getType().equals(FilterType.ATTRIBUTE)) {
				IAttributeMap m = ((AttributeFilter)filter).getAttributeMap();
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_CATEGORY)) {
					IAttribute a = m.get(IJAMConst.ATTRIBUTE_NAME_CATEGORY);
					return getProxy().getContacts(a.getValue());
				}				
			}
			if (filter!=null && filter.getType().equals(FilterType.PHONENUMBER)) {
				String intarea = ((PhonenumberFilter)filter).getPhonenumber().getIntAreaCode();
				String area = ((PhonenumberFilter)filter).getPhonenumber().getAreaCode();
				return getProxy().getContactsByAreaCode(intarea+area);
			}
			if (filter!=null && filter.getType().equals(FilterType.CHARACTER)) {	
				ICallerList cl = getProxy().getContacts(null);
				this.applyFilters(cl, new IFilter[] { filter });
				return cl;
			}
			return getProxy().getContacts(null);
		} catch (OutlookContactProxyException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return getRuntime().getCallerFactory().createCallerList();
	}

	public void removeCaller(ICaller caller) {
		try {
			getProxy().removeContact(caller);
		} catch (OutlookContactProxyException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void setCaller(ICaller caller) {
		try {
			getProxy().createContact(caller);
		} catch (OutlookContactProxyException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void updateCaller(ICaller caller) {
		try {
			if (!getProxy().updateContact(caller))
				getProxy().createContact(caller);
		} catch (OutlookContactProxyException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
		
	private OutlookContactProxy getProxy() {
		if (this.m_proxy==null) {
			this.m_proxy = new OutlookContactProxy();
			this.m_proxy.start();
		}			
		return m_proxy;
	}

	public void shutdown() {
		this.m_isUUIDCheck = false;
		if (this.m_proxy!=null) this.m_proxy.stop();
		this.m_proxy = null;
		super.shutdown();
		
	}

	public int getCurrent() {
		return getProxy().getCurrent();
	}

	public int getTotal() {
		return getProxy().getTotal();
	}

	public void startup() {
		super.startup();
		
		if (m_logger.isLoggable(Level.INFO))
			m_logger.info("<"+getID()+"> configuration: "+this.m_configuration);
		
		if (this.isActive()) {
			this.m_isUUIDCheck = true;
			Thread t = new Thread() {
				public void run() {					
					int mode = Integer.parseInt(m_configuration.getProperty(CFG_MODE, "0"));
					// 2010-02-05: introduced for identification only use case
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("<"+getID()+"> is set to mode: "+mode);
					
					if (mode == 1) {
						while (m_isUUIDCheck) {
							try {
								ICallerList c = getProxy().getContacts(null);
								if (m_logger.isLoggable(Level.INFO) && c.size()>0)
									m_logger.info("Found "+c.size()+" outlook contacts to be synced.");
							} catch (OutlookContactProxyException e) {
								m_logger.log(Level.SEVERE, e.getMessage(), e);
							}
							try {
								Thread.sleep(getIndexingInterval());
							} catch (InterruptedException e) {
							}
						}
					} else {
						if (m_logger.isLoggable(Level.INFO))
							m_logger.info("Re-indexing interval for outlook contacts set to "+getIndexingInterval()+" ms.");
						long t = Long.parseLong(m_configuration.getProperty(CFG_TS, "0"));
						try {
							Thread.sleep(getIndexingInterval());
						} catch (InterruptedException e) {
						}
						while (m_isUUIDCheck) {
							try {
								ICallerList c = getProxy().getModifiedContacts(t);
								if (m_logger.isLoggable(Level.INFO) && c.size()>0)
									m_logger.info("Found "+c.size()+" modified outlook contacts to be synced.");
							} catch (OutlookContactProxyException e) {
								m_logger.log(Level.SEVERE, e.getMessage(), e);
							}
							try {
								Thread.sleep(getIndexingInterval());
							} catch (InterruptedException e) {
							}
							t = System.currentTimeMillis();
							getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), CFG_TS, Long.toString(t));
							getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
							m_configuration.setProperty(CFG_TS, Long.toString(t));						
						}
					}
				}
			};
			t.setName("JAM-OutlookSync-Thread-(deamon)");
			t.setDaemon(true);
			t.start();	
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("JAM-OutlookSync-Thread-(deamon) started.");
		}		
	}
	
	public boolean isSupported(Class c) {
		if (this.m_configuration!=null && ((c.equals(IIdentifyCallerRepository.class)|| c.equals(IReadCallerRepository.class) || c.equals(IWriteCallerRepository.class)))) {
			int mode = Integer.parseInt(this.m_configuration.getProperty(CFG_MODE, "0"));
			if (mode==0) return super.isSupported(c);
			if (mode == 1) {
				return (c.equals(IIdentifyCallerRepository.class) && super.isSupported(c));
			}
			if (mode == 2) {
				return ((c.equals(IIdentifyCallerRepository.class) || c.equals(IReadCallerRepository.class)) && super.isSupported(c));
			}
			if (mode == 3) {
				return super.isSupported(c);
			}
		} else {
			return super.isSupported(c);
		}
		return false;
	}

	
	private long getIndexingInterval() {
		long t = Long.parseLong(m_configuration.getProperty(CFG_INDEX, "5"));
		return (t * 60 * 1000);
	}
}
