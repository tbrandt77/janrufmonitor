package de.janrufmonitor.runtime;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.janrufmonitor.classloader.JamCacheMasterClassLoader;
import de.janrufmonitor.framework.CallFactory;
import de.janrufmonitor.framework.CallerFactory;
import de.janrufmonitor.framework.ICallFactory;
import de.janrufmonitor.framework.ICallerFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.CommandFactory;
import de.janrufmonitor.framework.command.ICommandFactory;
import de.janrufmonitor.framework.configuration.ConfigManagerFactory;
import de.janrufmonitor.framework.configuration.ConfigurableNotifier;
import de.janrufmonitor.framework.configuration.IConfigManagerFactory;
import de.janrufmonitor.framework.configuration.IConfigurableNotifier;
import de.janrufmonitor.framework.event.EventBroker;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.framework.i18n.I18nManagerFactory;
import de.janrufmonitor.framework.i18n.II18nManagerFactory;
import de.janrufmonitor.framework.installer.InstallerEngine;
import de.janrufmonitor.framework.manager.CipManager;
import de.janrufmonitor.framework.manager.ICipManager;
import de.janrufmonitor.framework.manager.IMsnManager;
import de.janrufmonitor.framework.manager.MsnManager;
import de.janrufmonitor.framework.monitor.IMonitorListener;
import de.janrufmonitor.framework.monitor.MonitorListener;
import de.janrufmonitor.framework.rules.IRuleEngine;
import de.janrufmonitor.framework.rules.RuleEngine;
import de.janrufmonitor.repository.CallManagerFactory;
import de.janrufmonitor.repository.CallerManagerFactory;
import de.janrufmonitor.repository.ICallManagerFactory;
import de.janrufmonitor.repository.ICallerManagerFactory;
import de.janrufmonitor.service.IServiceFactory;
import de.janrufmonitor.service.ServiceFactory;
import de.janrufmonitor.util.io.PathResolver;

public class PIMRuntime implements IRuntime, IEventSender {
	
	class ProxyAuthenticator extends Authenticator {  
		  
	    private String user, password;  
	  
	    public ProxyAuthenticator(String user, String password) {  
	        this.user = user;  
	        this.password = password;  
	    }  
	  
	    protected PasswordAuthentication getPasswordAuthentication() {  
	        return new PasswordAuthentication(user, password.toCharArray());  
	    }  
	}  

	private static String ID = "PIMRuntime";

	private static PIMRuntime m_instance = null;

	private Logger m_logger;

	private boolean running;

	// instances
	private IConfigurableNotifier m_configurableNotifier;

	private IConfigManagerFactory m_configManagerFactory;

	private IEventBroker m_eventBroker;

	private ICallFactory m_callFactory;

	private ICallerFactory m_callerFactory;

	private ICallManagerFactory m_callManagerFactory;

	private ICallerManagerFactory m_callerManagerFactory;

	private IServiceFactory m_serviceFactory;

	private IMsnManager m_msnManager;

	private ICipManager m_cipManager;

	private IMonitorListener m_monitorListener;

	private ICommandFactory m_commandFactory;

	private II18nManagerFactory m_i18nManagerFactory;

	private IRuleEngine m_ruleEngine;

	private PIMRuntime() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		this.running = true;
	}

	public static synchronized PIMRuntime getInstance() {
		if (PIMRuntime.m_instance == null) {
			PIMRuntime.m_instance = new PIMRuntime();
			Logger l = LogManager.getLogManager().getLogger(
					IJAMConst.DEFAULT_LOGGER);
			if (l != null)
				l.info("Created new PIMRuntime instance.");
		}
		return PIMRuntime.m_instance;
	}

	public synchronized ICallFactory getCallFactory() {
		if (this.m_callFactory == null) {
			this.m_callFactory = CallFactory.getInstance();
			this.m_logger.info("CallFactory started successfully.");
		}
		return this.m_callFactory;
	}

	public synchronized ICallManagerFactory getCallManagerFactory() {
		if (this.m_callManagerFactory == null) {
			this.m_callManagerFactory = CallManagerFactory.getInstance();
			this.m_logger.info("CallManagerFactory started successfully.");
		}
		return this.m_callManagerFactory;
	}

	public synchronized ICallerFactory getCallerFactory() {
		if (this.m_callerFactory == null) {
			this.m_callerFactory = CallerFactory.getInstance();
			this.m_logger.info("CallerFactory started successfully.");
		}
		return this.m_callerFactory;
	}

	public synchronized ICallerManagerFactory getCallerManagerFactory() {
		if (this.m_callerManagerFactory == null) {
			this.m_callerManagerFactory = CallerManagerFactory.getInstance();
			this.m_logger.info("CallerManagerFactory started successfully.");
		}
		return this.m_callerManagerFactory;
	}

	public synchronized IConfigManagerFactory getConfigManagerFactory() {
		if (this.m_configManagerFactory == null) {
			this.m_configManagerFactory = ConfigManagerFactory.getInstance();
			this.m_logger.info("ConfigManagerFactory started successfully.");
		}
		return this.m_configManagerFactory;
	}

	public synchronized IConfigurableNotifier getConfigurableNotifier() {
		if (this.m_configurableNotifier == null) {
			this.m_configurableNotifier = ConfigurableNotifier.getInstance();
			this.m_logger.info("ConfigurableNotifier started successfully.");
		}
		return this.m_configurableNotifier;
	}

	public synchronized IEventBroker getEventBroker() {
		if (this.m_eventBroker == null) {
			this.m_eventBroker = EventBroker.getInstance();
			this.m_logger.info("EventBroker started successfully.");
		}
		return this.m_eventBroker;
	}

	public synchronized IServiceFactory getServiceFactory() {
		if (this.m_serviceFactory == null) {
			this.m_serviceFactory = ServiceFactory.getInstance();
			this.m_logger.info("ServiceFactory started successfully.");
		}
		return this.m_serviceFactory;
	}

	public synchronized IMsnManager getMsnManager() {
		if (this.m_msnManager == null) {
			this.m_msnManager = MsnManager.getInstance();
			this.m_logger.info("MsnManager started successfully.");
		}
		return this.m_msnManager;
	}

	public synchronized ICipManager getCipManager() {
		if (this.m_cipManager == null) {
			this.m_cipManager = CipManager.getInstance();
			this.m_logger.info("CipManager started successfully.");
		}
		return this.m_cipManager;
	}

	public synchronized IMonitorListener getMonitorListener() {
		if (this.m_monitorListener == null) {
			this.m_monitorListener = MonitorListener.getInstance();
			this.m_logger.info("MonitorListener started successfully.");
		}
		return this.m_monitorListener;
	}

	public synchronized II18nManagerFactory getI18nManagerFactory() {
		if (this.m_i18nManagerFactory == null) {
			this.m_i18nManagerFactory = I18nManagerFactory.getInstance();
			this.m_logger.info("I18nManagerFactory started successfully.");
		}
		return this.m_i18nManagerFactory;
	}

	public ICommandFactory getCommandFactory() {
		if (this.m_commandFactory == null) {
			this.m_commandFactory = CommandFactory.getInstance();
			this.m_logger.info("CommandFactory started successfully.");
		}
		return this.m_commandFactory;
	}

	public synchronized IRuleEngine getRuleEngine() {
		if (this.m_ruleEngine == null) {
			this.m_ruleEngine = RuleEngine.getInstance();
			this.m_logger.info("RuleEngine started successfully.");
		}
		return this.m_ruleEngine;
	}

	public void enableMonitorListener(boolean enable) {
		MonitorListener ml = (MonitorListener) this.m_monitorListener;
		if (ml != null) {
			if (enable) {
				ml.start();
			} else {
				ml.stop();
			}
		}
	}

	public void startup() {
		this.m_logger.entering(PIMRuntime.class.getName(), "startup");
		long start = 0;
		long stop = 0;
		Map times = new HashMap();

		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("Running on platform: "
				+ System.getProperty("os.name"));
			this.m_logger.info("Running on architecture: "
				+ System.getProperty("os.arch"));
			this.m_logger.info("JVM: " + System.getProperty("java.vm.name"));
			this.m_logger.info("JVM Version: "
				+ System.getProperty("java.vm.version"));
			this.m_logger.info("JVM vendor: " + System.getProperty("java.vendor"));
			this.m_logger.info("JVM home: " + System.getProperty("java.home"));
			this.m_logger.info("JVM library path: "
				+ System.getProperty("java.library.path"));
			this.m_logger.info("jAnrufmonitor working directory: "
				+ PathResolver.getInstance(this).getInstallDirectory());
		}
		
		try
	    {
	        // Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	            public void checkClientTrusted(X509Certificate[] certs, String authType) {
	            }
	            public void checkServerTrusted(X509Certificate[] certs, String authType) {
	            }
	        }
	        };

	        // Install the all-trusting trust manager
	        SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	        // Create all-trusting host name verifier
	        HostnameVerifier allHostsValid = new HostnameVerifier() {
	            public boolean verify(String hostname, SSLSession session) {
	                return true;
	            }
	        };

	        // Install the all-trusting host verifier
	        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Added new SSL TrustManager: true");
	    } catch (NoSuchAlgorithmException e) {
	        m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
	    } catch (KeyManagementException e) {
	    	 m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
	    }
		
		
		IRuntime currentRT = PIMRuntime.getInstance();
		
		// check ProxySettings
		if (System.getProperty("http.proxyUser")!=null && System.getProperty("http.proxyUser").length()>0 && System.getProperty("http.proxyPassword")!=null && System.getProperty("http.proxyPassword").length()>0) {
			Authenticator.setDefault(new ProxyAuthenticator(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword")));  
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Setting http.proxyUser="+System.getProperty("http.proxyUser")+", http.proxyPassword="+System.getProperty("http.proxyPassword"));
		}

		// start configuration
		start = System.currentTimeMillis();
		currentRT.getConfigManagerFactory().startup();
		stop = System.currentTimeMillis();
		times.put("ConfigManagerFactory", Long.toString(stop - start));

		start = System.currentTimeMillis();
		currentRT.getConfigurableNotifier().startup();
		stop = System.currentTimeMillis();
		times.put("ConfigurableNotifier", Long.toString(stop - start));

		start = System.currentTimeMillis();
		currentRT.getI18nManagerFactory().startup();
		stop = System.currentTimeMillis();
		times.put("I18nManagerFactory", Long.toString(stop - start));

		// check for new configuration
		InstallerEngine.getInstance().install();
		
		String restart = System.getProperty(IJAMConst.SYSTEM_INSTALLER_RESTART);
		if (restart==null || restart.equalsIgnoreCase("true")) {
			this.m_logger.info("Detected jam.installer.restart flag as: "+System.getProperty(IJAMConst.SYSTEM_INSTALLER_RESTART));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			restart = System.getProperty(IJAMConst.SYSTEM_INSTALLER_RESTART);
			if (restart !=null && restart.equalsIgnoreCase("true")) {
				this.m_logger.info("Entities are not started, due to installation of new modules.");				
				return;
			}
		}
		
		// start framework layer
		currentRT.getCallFactory();
		currentRT.getCallerFactory();

		start = System.currentTimeMillis();
		currentRT.getEventBroker().startup();
		stop = System.currentTimeMillis();
		times.put("EventBroker", Long.toString(stop - start));

		start = System.currentTimeMillis();
		currentRT.getMsnManager().startup();
		stop = System.currentTimeMillis();
		times.put("MsnManager", Long.toString(stop - start));

		start = System.currentTimeMillis();
		currentRT.getCipManager().startup();
		stop = System.currentTimeMillis();
		times.put("CipManager", Long.toString(stop - start));

		start = System.currentTimeMillis();
		currentRT.getRuleEngine().startup();
		stop = System.currentTimeMillis();
		times.put("RuleEngine", Long.toString(stop - start));

		start = System.currentTimeMillis();
		currentRT.getCommandFactory().startup();
		stop = System.currentTimeMillis();
		times.put("CommandFactory", Long.toString(stop - start));

		// start repository layer
		start = System.currentTimeMillis();
		currentRT.getCallManagerFactory().startup();
		stop = System.currentTimeMillis();
		times.put("CallManagerFactory", Long.toString(stop - start));

		start = System.currentTimeMillis();
		currentRT.getCallerManagerFactory().startup();
		stop = System.currentTimeMillis();
		times.put("CallerManagerFactory", Long.toString(stop - start));

		// start service layer
		start = System.currentTimeMillis();
		currentRT.getServiceFactory().startup();
		stop = System.currentTimeMillis();
		times.put("ServiceFactory", Long.toString(stop - start));

		// start monitoring
		start = System.currentTimeMillis();
		currentRT.getMonitorListener().startup();
		stop = System.currentTimeMillis();
		times.put("MonitorListener", Long.toString(stop - start));

		Iterator iter = times.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			this.m_logger.info("Startup time for component <" + key + ">: "
					+ times.get(key) + " ms");
		}
		
		final String param = System.getProperty(IJAMConst.SYSTEM_MONITOR_RESTART_INTERVAL);
		if (param!= null && param.trim().length()>0) {
			Thread autoMonitorListenerReStarter = new Thread(new Runnable() {
				public void run() {	
					m_logger.info("jam.monitor.restart.interval is set to "+param);
					int intervall = -1;
					try {
						intervall = Integer.parseInt(param);
					} catch (Exception ex) {};
					while (intervall > 0) {
						try {
							Thread.sleep((intervall * 60 * 1000));
						} catch (InterruptedException e) {
							intervall = -1;
						}
						if (PIMRuntime.getInstance().getMonitorListener().isRunning()) {
							m_logger.info("Stopping monitor due to jam.monitor.restart.interval");
							PIMRuntime.getInstance().getMonitorListener().stop();
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {}
							m_logger.info("Starting monitor due to jam.monitor.restart.interval");
							PIMRuntime.getInstance().getMonitorListener().start();
						}
					}
				}});
			autoMonitorListenerReStarter.setName("JAM-AutoRestartMonitorByInterval");
			autoMonitorListenerReStarter.start();
		}

		this.m_eventBroker.register(this);
		this.m_eventBroker.send(this, this.m_eventBroker
				.createEvent(IEventConst.EVENT_TYPE_APPLICATION_READY));
		this.m_eventBroker.unregister(this);

		this.m_logger.info("===>>> Startup completed.");
		this.m_logger.exiting(PIMRuntime.class.getName(), "startup");
	}

	public void shutdown() {
		this.m_logger.entering(PIMRuntime.class.getName(), "shutdown");

		if (!this.running)
			return;

		long start = 0;
		long stop = 0;
		Map times = new HashMap();

		this.enableMonitorListener(false);
		if (this.m_monitorListener != null) {
			start = System.currentTimeMillis();
			this.m_monitorListener.shutdown();
			stop = System.currentTimeMillis();
			times.put("MonitorListener", Long.toString(stop - start));
		}
		if (this.m_serviceFactory != null) {
			start = System.currentTimeMillis();
			this.m_serviceFactory.shutdown();
			stop = System.currentTimeMillis();
			times.put("ServiceFactory", Long.toString(stop - start));
		}		
		if (this.m_callerManagerFactory != null) {
			start = System.currentTimeMillis();
			this.m_callerManagerFactory.shutdown();
			stop = System.currentTimeMillis();
			times.put("CallerManagerFactory", Long.toString(stop - start));
		}
		if (this.m_callManagerFactory != null) {
			start = System.currentTimeMillis();
			this.m_callManagerFactory.shutdown();
			stop = System.currentTimeMillis();
			times.put("CallManagerFactory", Long.toString(stop - start));
		}
		if (this.m_commandFactory != null) {
			start = System.currentTimeMillis();
			this.m_commandFactory.shutdown();
			stop = System.currentTimeMillis();
			times.put("CommandFactory", Long.toString(stop - start));
		}
		if (this.m_ruleEngine != null) {
			start = System.currentTimeMillis();
			this.m_ruleEngine.shutdown();
			stop = System.currentTimeMillis();
			times.put("RuleEngine", Long.toString(stop - start));
		}
		if (this.m_cipManager != null) {
			start = System.currentTimeMillis();
			this.m_cipManager.shutdown();
			stop = System.currentTimeMillis();
			times.put("CipManager", Long.toString(stop - start));
		}
		if (this.m_msnManager != null) {
			start = System.currentTimeMillis();
			this.m_msnManager.shutdown();
			stop = System.currentTimeMillis();
			times.put("MsnManager", Long.toString(stop - start));
		}
		if (this.m_eventBroker != null) {
			start = System.currentTimeMillis();
			this.m_eventBroker.shutdown();
			stop = System.currentTimeMillis();
			times.put("EventBroker", Long.toString(stop - start));
		}
		if (this.m_i18nManagerFactory != null) {
			start = System.currentTimeMillis();
			this.m_i18nManagerFactory.shutdown();
			stop = System.currentTimeMillis();
			times.put("I18nManagerFactory", Long.toString(stop - start));
		}
		if (this.m_configurableNotifier != null) {
			start = System.currentTimeMillis();
			this.m_configurableNotifier.shutdown();
			stop = System.currentTimeMillis();
			times.put("ConfigurableNotifier", Long.toString(stop - start));
		}
		if (this.m_configManagerFactory != null) {
			start = System.currentTimeMillis();
			this.m_configManagerFactory.shutdown();
			stop = System.currentTimeMillis();
			times.put("ConfigManagerFactory", Long.toString(stop - start));
		}

		Iterator iter = times.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			this.m_logger.info("Shutdown time for component <" + key + ">: "
					+ times.get(key) + " ms");
		}

		this.cleanUp();
		
		this.m_logger.info("Invalidating classloader...");
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
		JamCacheMasterClassLoader.invalidateInstance();
		
		this.m_logger.info("Application is shut down.");
		this.m_logger.info("===>>> Shutdown completed.");
		this.running = false;
		this.m_logger.exiting(PIMRuntime.class.getName(), "shutdown");
	}

	public void cleanUp() {
		this.m_logger.entering(PIMRuntime.class.getName(), "cleanUp");
		this.m_configurableNotifier = null;
		this.m_configManagerFactory = null;
		this.m_eventBroker = null;
		this.m_callFactory = null;
		this.m_callerFactory = null;
		this.m_callManagerFactory = null;
		this.m_callerManagerFactory = null;
		this.m_serviceFactory = null;
		this.m_msnManager = null;
		this.m_cipManager = null;
		this.m_ruleEngine = null;
		this.m_monitorListener = null;
		this.m_i18nManagerFactory = null;
		this.m_logger.exiting(PIMRuntime.class.getName(), "cleanUp");
	}

	public String getSenderID() {
		return PIMRuntime.ID;
	}

	public int getPriority() {
		return 0;
	}

}
