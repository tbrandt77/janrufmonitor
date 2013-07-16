package de.janrufmonitor.repository;

import java.util.logging.Level;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.imexport.ITracker;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class GoogleContactsCallerManager extends AbstractReadWriteCallerManager
		implements IRemoteRepository, IGoogleContactsConst, ITracker {

	public static String ID = "GoogleContactsCallerManager";
	public static String NAMESPACE = "repository.GoogleContactsCallerManager";
	
	private static String CFG_MODE ="mode";
	
	private IRuntime m_runtime;
	private GoogleContactsProxy m_proxy = null;
	
	public GoogleContactsCallerManager() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}
	
	public String getID() {
		return GoogleContactsCallerManager.ID;
	}

	public String getNamespace() {
		return GoogleContactsCallerManager.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public ICaller getCaller(IPhonenumber number)
			throws CallerNotFoundException {
		if (number == null)
			throw new CallerNotFoundException(
					"Phone number is not set (null). No caller found.");

		if (number.isClired())
			throw new CallerNotFoundException(
					"Phone number is CLIR. Identification impossible.");
		
		if (this.isInternalNumber(number))
			throw new CallerNotFoundException(
				"Phone number is internal phone system number. Identification not possible.");

		try {
			ICaller c = getProxy().identify(number);
			if (c!=null) return c;
		} catch (GoogleContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "loginerror", e));
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
			if (filter!=null && filter.getType().equals(FilterType.CHARACTER)) {
				ICallerList cl = getProxy().getContacts(null);
				this.applyFilters(cl, new IFilter[] { filter });
				return cl;
			}
			return getProxy().getContacts(null);
		} catch (GoogleContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "loginerror", e));
		}
		return getRuntime().getCallerFactory().createCallerList();
	}

	// begin - to be changed if write support
	public boolean isSupported(Class c) {
		if (this.m_configuration==null) return false;
		int mode = Integer.parseInt(this.m_configuration.getProperty(CFG_MODE, "0"));
		if (mode==1) {
			if ((c.equals(IIdentifyCallerRepository.class) || c.equals(IReadCallerRepository.class) || c.equals(IRemoteRepository.class))) {
				return super.isSupported(c);
			}
			return false;
		}
		return super.isSupported(c);
	}

	public void removeCaller(ICallerList callerList) {
		try {
			getProxy().deleteContacts(callerList);
		} catch (GoogleContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "loginerror", e));
		}
	}

	public void updateCaller(ICaller caller) {
		try {
			getProxy().updateContact(caller);
		} catch (GoogleContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "loginerror", e));
		}
	}

	public void removeCaller(ICaller caller) {
		ICallerList cl = getRuntime().getCallerFactory().createCallerList(1);
		cl.add(caller);
		this.removeCaller(cl);
	}

	public void setCaller(ICallerList callerList) {
		try {
			getProxy().createContacts(callerList);
		} catch (GoogleContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "loginerror", e));
		}
	}
	
	public void setCaller(ICaller caller) {
		ICallerList cl = getRuntime().getCallerFactory().createCallerList(1);
		this.addCreationAttributes(caller);
		this.addSystemAttributes(caller);
		cl.add(caller);
		this.setCaller(cl);
	}
	// end - to be changed if write support
	
	private GoogleContactsProxy getProxy() {
		if (this.m_proxy==null) {
			this.m_proxy = new GoogleContactsProxy();
			this.m_proxy.start();
		}			
		return m_proxy;
	}
	
	public void checkAuthentication(String user, String password) throws GoogleContactsLoginException {
		getProxy().checkAuthentication(user, password);
	}
	
	public void shutdown() {
		if (this.m_proxy!=null) this.m_proxy.stop();
		this.m_proxy = null;
		super.shutdown();	
	}
	
	private boolean isSyncStart() {
		return  (this.m_configuration!=null && this.m_configuration.getProperty(CFG_GOOGLE_SYNCSTART, "false").equalsIgnoreCase("true"));
	}
	
	public void startup() {
		super.startup();
		if (this.isActive() && this.isSyncStart()) {
			final Thread t = new Thread() {
				public void run() {
					try {
						getProxy().preload();
					} catch (GoogleContactsException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			};
			t.setName("JAM-GoogleUUIDCheck-Thread-(deamon)");
			t.setDaemon(true);
			t.start();
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			
			// removed: 2010/12/08 optimized preload method, no display required
//			
//			Thread monitor = new Thread() {
//				public void run() {
//					while (t.isAlive()) {
//						try {
//							Thread.sleep(3000);
//						} catch (InterruptedException e) {
//							m_logger.log(Level.SEVERE, e.getMessage(), e);
//						}
//						String text = getRuntime().getI18nManagerFactory().getI18nManager().getString(getNamespace(),
//								"process", "label",
//								getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
//										IJAMConst.GLOBAL_NAMESPACE,
//										IJAMConst.GLOBAL_LANGUAGE
//									)
//								);
//						
//						text = StringUtils.replaceString(text, "{%1}", Integer.toString(getProxy().getCurrent()));
//						text = StringUtils.replaceString(text, "{%2}", Integer.toString(getProxy().getTotal()));
//
//						PropagationFactory.getInstance().fire(
//								new Message(
//										Message.INFO, 
//										getRuntime().getI18nManagerFactory().getI18nManager().getString(getNamespace(),
//												"title", "label",
//												getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
//												IJAMConst.GLOBAL_NAMESPACE,
//												IJAMConst.GLOBAL_LANGUAGE
//												)), 
//										new Exception(text)),
//								"Tray");	
//						
//					
//					}
//					
//					String text = getRuntime().getI18nManagerFactory().getI18nManager().getString(getNamespace(),
//							"finished", "label",
//							getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
//									IJAMConst.GLOBAL_NAMESPACE,
//									IJAMConst.GLOBAL_LANGUAGE
//								)
//							);
//					
//					text = StringUtils.replaceString(text, "{%1}", Integer.toString(getProxy().getTotal()));
//					
//					PropagationFactory.getInstance().fire(
//							new Message(
//									Message.INFO, 
//									getRuntime().getI18nManagerFactory().getI18nManager().getString(getNamespace(),
//											"title", "label",
//											getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
//											IJAMConst.GLOBAL_NAMESPACE,
//											IJAMConst.GLOBAL_LANGUAGE
//											)), 
//									new Exception(text)),
//							"Tray");	
//							
//					
//				}
//			};
//			monitor.setName("JAM-GoogleUUIDMonitor-Thread-(deamon)");
//			monitor.setDaemon(true);
//			monitor.start();
//			
		}		
	}

	public int getCurrent() {
		return getProxy().getCurrent();
	}

	public int getTotal() {
		return getProxy().getTotal();
	}

}
