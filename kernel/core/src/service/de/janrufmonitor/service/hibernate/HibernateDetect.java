package de.janrufmonitor.service.hibernate;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractConfigurableService;
import de.janrufmonitor.util.io.PathResolver;

public class HibernateDetect extends AbstractConfigurableService implements
		IEventSender {

	private static final String ID = "HibernateDetect";
	private static final String NAMESPACE = "service.HibernateDetect";
	private IRuntime m_runtime;

	private String CFG_DELAYTIME = "delay";
	private boolean isHibernateChecking;
	
	public HibernateDetect() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}
	
	public String getSenderID() {
		return HibernateDetect.ID;
	}

	public String getNamespace() {
		return HibernateDetect.NAMESPACE;
	}

	public String getID() {
		return HibernateDetect.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
	
	public void startup() {
		super.startup();
		getRuntime().getEventBroker().register(this);
		if (isEnabled()) {
			this.isHibernateChecking = true;

			Thread t = new Thread(new Runnable() {
				public void run() {
						// check file system
					File trackFolder =
						new File(
							PathResolver.getInstance(getRuntime()).getLogDirectory(),
							"~" + ID.toLowerCase());
					
					if (!trackFolder.exists())
						trackFolder.mkdirs();

					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Hibernate tracking directory: "+ trackFolder.getAbsolutePath());

					// clear folder if exists
					File[] files = trackFolder.listFiles();
					for (int i = 0; i < files.length; i++) {
						files[i].delete();
					}

					File pid = null;
					while (isHibernateChecking) {
						pid =
							new File(
								trackFolder,
								Long.toString(System.currentTimeMillis()));
						try {
							pid.createNewFile();
						} catch (IOException e) {
							m_logger.log(Level.SEVERE, e.toString(), e);
						}

						try {
							Thread.sleep(getDelayTime());
						} catch (InterruptedException e) {
							m_logger.log(Level.SEVERE, e.toString(), e);
						}

						// check if time stamps are there
						files = trackFolder.listFiles();
						if (files.length == 0) {
							isHibernateChecking = false;
							continue;
						}

						if (files.length == 1) {
							try {
								long l = Long.parseLong(files[0].getName());
								long result = System.currentTimeMillis() - l;
								if (m_logger.isLoggable(Level.INFO))
									m_logger.info("Delta hibernate detection time (ms): "	+ result);
								if (result > (2 * getDelayTime())) {			
									PropagationFactory
									.getInstance()
									.fire(
									new Message(
										Message.WARNING,
										getNamespace(),
										"hibernate",
										new Exception("Programm out of sync: OS was probably in hibernate mode.")));
									getRuntime().getEventBroker().send(HibernateDetect.this, getRuntime().getEventBroker().createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
								}
							} catch (Exception e) {
								m_logger.log(Level.SEVERE, e.toString(), e);
							}
						}
						for (int i = 0; i < files.length; i++) {
							files[i].delete();
						}
					}
				}
			});
			t.setDaemon(true);
			t.setName("JAM-"+ID+"-Thread-(deamon)");
			t.start();
		}
	}

	public void shutdown() {
		this.isHibernateChecking = false;
		getRuntime().getEventBroker().unregister(this);
		super.shutdown();
	}
	
	private long getDelayTime() {
		String value = this.m_configuration.getProperty(CFG_DELAYTIME, "60");
		try {
			return (Long.parseLong(value) * 1000);
		} catch (Exception ex) {
			this.m_logger.warning(ex.getMessage());
		}

		return 60000;
	}

}
