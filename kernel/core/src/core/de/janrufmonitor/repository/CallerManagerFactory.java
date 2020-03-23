package de.janrufmonitor.repository;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.runtime.PIMRuntime;

public class CallerManagerFactory implements ICallerManagerFactory,
		IConfigurable {

	private class DummyCallerManager implements ICallerManager,
			IIdentifyCallerRepository {

		public ICaller getCaller(IPhonenumber number)
				throws CallerNotFoundException {
			if (number == null)
				throw new CallerNotFoundException(
						"Phone number is not set (null). No caller found.");

			if (number.isClired())
				throw new CallerNotFoundException(
						"Phone number is CLIR. Identification impossible.");

			throw new CallerNotFoundException("Phone number is "
					+ number.getTelephoneNumber()
					+ ". No identification possible with DummyCallerManager.");
		}

		public boolean isActive() {
			return true;
		}

		public String getManagerID() {
			return "DummyCallerManager";
		}
		
		public void setManagerID(String id) {
		}

		public int getPriority() {
			return 0;
		}

		public void restart() {
		}

		public void shutdown() {
		}

		public void startup() {
		}

		public boolean isSupported(Class c) {
			return c.isInstance(this);
		}

	}

	private String ID = "CallerManagerFactory";

	private String NAMESPACE = "repository.CallerManagerFactory";

	private String PARAMETERNAME = "manager_";

	private String DEFAULT_PARAMETERNAME = "default";

	private static CallerManagerFactory m_instance = null;

	private Logger m_logger;

	private String defaultManager;

	private Map m_callerManagers;

	private Properties m_configuration;

	private boolean m_isCallerManagerFactoryStarted = false;

	private CallerManagerFactory() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
	}

	public static synchronized CallerManagerFactory getInstance() {
		if (CallerManagerFactory.m_instance == null) {
			CallerManagerFactory.m_instance = new CallerManagerFactory();
		}
		return CallerManagerFactory.m_instance;
	}

	public String[] getAllCallerManagerIDs() {
		String[] list = new String[this.m_callerManagers.size()];

		synchronized (this.m_callerManagers) {
			Iterator iter = this.m_callerManagers.keySet().iterator();
			int i = 0;
			String key = null;
			while (iter.hasNext()) {
				key = (String) iter.next();
				list[i] = key;
				i++;
			}
		}
		return list;
	}

	public List getAllCallerManagers() {
		List list = new ArrayList();

		synchronized (this.m_callerManagers) {
			Iterator iter = this.m_callerManagers.keySet().iterator();
			String key = null;
			ICallerManager mgr = null;
			while (iter.hasNext()) {
				key = (String) iter.next();
				mgr = (ICallerManager) this.m_callerManagers.get(key);
				if (mgr != null) {
					list.add(mgr);
				}
			}
		}
		Collections.sort(list, new RepositoryManagerComparator());
		return list;
	}

	public ICallerManager getCallerManager(String id) {
		if (id != null && id.length() == 0)
			id = this.defaultManager;

		if (this.m_callerManagers != null
				&& this.m_callerManagers.containsKey(id)) {
			return (ICallerManager) this.m_callerManagers.get(id);
		}
		this.m_logger.warning("no CallerManager found for ID: " + id);
		return null;
	}

	public String getConfigurableID() {
		return this.ID;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public void setConfiguration(Properties configuration) {
		this.m_configuration = configuration;
	}

	public ICallerManager getDefaultCallerManager() {
		ICallerManager defaultCallerManager = this
				.getCallerManager(this.defaultManager);
		
		int c = 0;
		while (!this.m_isCallerManagerFactoryStarted && c < 4) {
			this.m_logger.warning("CallerManagerFactory not fully started. Retry in 1 sec.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			c++;
		}
		
		if (defaultCallerManager == null) {
			this.m_logger
					.severe("Default caller manager is invalid. Taking fallback...");
			return new DummyCallerManager();
		}
		return defaultCallerManager;
	}

	public void startup() {
		this.m_logger.entering(CallerManagerFactory.class.getName(), "startup");

		this.m_isCallerManagerFactoryStarted = false;
		
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);

		this.m_callerManagers = Collections.synchronizedMap(new HashMap());
		Iterator iter = this.m_configuration.keySet().iterator();
		String key = null;
		while (iter.hasNext()) {
			key = (String) iter.next();
			if (key.startsWith(PARAMETERNAME)) {
				String className = this.m_configuration.getProperty(key);
				try {
					Class classObject = Thread.currentThread()
							.getContextClassLoader().loadClass(className);
					
					ICallerManager cpm = null;
					try {
						Constructor cons = classObject.getConstructor(new Class[] {String.class});
						cpm = (ICallerManager) cons.newInstance(new Object[] {key.substring(PARAMETERNAME.length())});
					} catch (SecurityException e) {
					} catch (NoSuchMethodException e) {
					} catch (IllegalArgumentException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (InvocationTargetException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
					if (cpm==null)
						cpm = (ICallerManager) classObject.newInstance();
					
					// cpm.startup();
					cpm.setManagerID(key.substring(PARAMETERNAME.length()));
					this.m_callerManagers.put(cpm.getManagerID(), cpm);
					this.m_logger.info("Registered new caller manager <"
							+ cpm.getManagerID() + ">.");
				} catch (ClassNotFoundException ex) {
					this.m_logger.warning("Could not find class: " + className);
				} catch (InstantiationException ex) {
					this.m_logger.severe("Could not instantiate class: "
							+ className);
				} catch (IllegalAccessException ex) {
					this.m_logger
							.severe("Could not access class: " + className);
				} catch (NoClassDefFoundError ex) {
					this.m_logger.warning("Could not find class definition: " + className);
				}
			}
		}

		String[] managers = this.getAllCallerManagerIDs();
		for (int i = 0; i < managers.length; i++) {
			this.startManager(managers[i]);
		}

		defaultManager = this.m_configuration.getProperty(
				DEFAULT_PARAMETERNAME, "CountryDirectory");
		ICallerManager cmg = this.getCallerManager(defaultManager);
		if (cmg == null) {
			this.m_logger.warning("Default caller manager ID is invalid: "
					+ defaultManager);
			String[] ids = this.getAllCallerManagerIDs();
			defaultManager = (ids.length > 0 ? ids[0] : null);
			if (defaultManager == null)
				this.m_logger.severe("Default caller manager ID is null.");
		}
		this.m_logger.info("Set default caller manager to <" + defaultManager
				+ ">.");
		
		this.m_isCallerManagerFactoryStarted = true;
		
		this.m_logger.exiting(CallerManagerFactory.class.getName(), "startup");
	}

	private void startManager(String managerID) {
		this.m_logger.entering(CallerManagerFactory.class.getName(),
				"startManager");

		ICallerManager man = this.getCallerManager(managerID);
		if (man == null) {
			this.m_logger.severe("CallerManager <" + managerID
					+ "> has null reference.");
			this.m_logger.exiting(CallerManagerFactory.class.getName(),
					"startManager");
			return;
		}

		try {
			if (man.isActive()) {
				man.startup();
			}
		} catch (Exception ex) {
			this.m_logger.severe(managerID + ": " + ex.toString() + " : "
					+ ex.getMessage());
		}

		this.m_logger.exiting(CallerManagerFactory.class.getName(),
				"startManager");
	}

	public void shutdown() {
		this.m_logger
				.entering(CallerManagerFactory.class.getName(), "shutdown");
		List mgrs = this.getAllCallerManagers();
		ICallerManager mgr = null;
		for (int i = 0; i < mgrs.size(); i++) {
			mgr = (ICallerManager) mgrs.get(i);
			mgr.shutdown();
			if (mgr instanceof IConfigurable)
				PIMRuntime.getInstance().getConfigurableNotifier().unregister(
						(IConfigurable) mgr);
		}

		this.m_callerManagers.clear();

		PIMRuntime.getInstance().getConfigurableNotifier().unregister(this);
		m_instance = null;
		this.m_isCallerManagerFactoryStarted = false;
		this.m_logger.exiting(CallerManagerFactory.class.getName(), "shutdown");
	}

	public boolean isManagerAvailable(String id) {
		return this.m_callerManagers.containsKey(id);
	}

	public List getTypedCallerManagers(Class type) {
		List list = new ArrayList();

		synchronized (this.m_callerManagers) {
			Iterator iter = this.m_callerManagers.keySet().iterator();
			String key = null;
			ICallerManager mgr = null;
			while (iter.hasNext()) {
				key = (String) iter.next();
				mgr = (ICallerManager) this.m_callerManagers.get(key);
				if (mgr != null && mgr.isSupported(type)) {
					list.add(mgr);
				}
			}
		}
		Collections.sort(list, new RepositoryManagerComparator());
		return list;
	}

}
