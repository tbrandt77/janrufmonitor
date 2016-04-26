package de.janrufmonitor.service.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.framework.monitor.IMonitorListener;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.client.state.ClientStateManager;
import de.janrufmonitor.service.client.state.IClientStateMonitor;
import de.janrufmonitor.ui.swt.DisplayManager;

public class ClientMonitor implements IMonitor, IClientStateMonitor,
		IConfigurable {

	private class Reconnector implements Runnable {

		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				m_logger.severe(e.toString());
			}
			
			boolean autoReConnect = "true".equalsIgnoreCase(getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
					"service.Client",
					"autoreconnect"
				));
			
			if (!autoReConnect) {
				Display d = DisplayManager.getDefaultDisplay();
				Shell s = new Shell(d);
				int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO;
				MessageBox messageBox = new MessageBox (s, style);
				messageBox.setMessage (getRuntime().getI18nManagerFactory().getI18nManager().getString(NAMESPACE, "reconnect", "label", getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
						IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_LANGUAGE
					)));
				if (messageBox.open () == SWT.YES) {
					ClientMonitor.this.start();
				}
			} else {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					m_logger.severe(e.toString());
				}
				ClientMonitor.this.start();
			}
		}
	}
	
	
	public static String NAMESPACE = "monitor.ClientMonitor";
	public static String ID = "ClientMonitor";
	
	private Logger m_logger;
	private Properties m_configuration;
	private IRuntime m_runtime;
	
	private IMonitorListener m_ml;
	
	public ClientMonitor() {
		super();
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
		ClientStateManager.getInstance().register(this);
	}

	public String[] getDescription() {
		return new String[]{};
	}

	public String getID() {
		return ID;
	}

	public boolean isAvailable() {
		return (this.m_configuration.getProperty("activemonitor", "false").equalsIgnoreCase("true") && isAutoconnect());
	}
	
	private boolean isAutoconnect() {
		// 2009/02/06: just a work-a-round
		if (!Thread.currentThread().getName().equalsIgnoreCase("JAM-SWT/JFaceUI-Thread-(non-deamon)")) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Client autoconnect is set to "+getRuntime().getConfigManagerFactory().getConfigManager().getProperty("service.Client", "autoconnect"));
			return getRuntime().getConfigManagerFactory().getConfigManager().getProperty("service.Client", "autoconnect").equalsIgnoreCase("true");
		}
		return true;
	}

	public boolean isStarted() {
		Client c = getClientService();
		if (c!=null) {
			return c.isConnected();
		}
		return false;
	}

	public void reject(short cause) {
	}

	public void release() {
	}

	public void setListener(IMonitorListener jml) {
		this.m_ml = jml;
	}

	public void start() {
		Client c = getClientService();
		if (c!=null && !c.isConnected() && c.isEnabled()) {
			if (c.connect()) {
				ClientStateManager.getInstance().fireState(ClientMonitor.CONNECTION_OK, "");
			} else {
				ClientStateManager.getInstance().fireState(ClientMonitor.CONNECTION_CLOSED, "");
			}
		}
	}

	public void stop() {
		Client c = getClientService();
		if (c!=null && c.isConnected()) {
			c.disconnect();
			ClientStateManager.getInstance().fireState(ClientMonitor.CONNECTION_CLOSED, "");
		}
	}

	public void acceptState(int state, String message) {
		if (state==CONNECTION_CLOSED) {
			return;
		}
		if (state==CONNECTION_OK) {
			ICommand c = PIMRuntime.getInstance().getCommandFactory().getCommand("Activator");
			if (c!=null) {
				try {
					Map m = new HashMap();
					m.put("status", "revert");
					c.setParameters(m); // this method executes the command as well !!
				} catch (Exception e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
				}
			}
			return;
		}
		
		if (state==SERVER_SHUTDOWN) {
			DisplayManager.getDefaultDisplay().asyncExec(new Reconnector());
		}		
		
		if (state==SERVER_NOT_FOUND) {
			boolean autoReConnect = "true".equalsIgnoreCase(getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
					"service.Client",
					"autoreconnect"
				));
			if (autoReConnect) {
				Runnable r = new Runnable() {
					public void run() {
						try {
							Thread.sleep(30000);
						} catch (InterruptedException e) {
						}
						ClientMonitor.this.start();
					}};
				Thread t = new Thread(r);
				t.setName("ClientMonitor-HeadlessReconnector");
				t.start();
			}
		}
	}

	public String getConfigurableID() {
		return ID;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void setConfiguration(Properties configuration) {
		this.m_configuration = configuration;		
		if (this.isStarted()){
			this.stop();
			if (this.m_ml!=null)
				this.setListener(this.m_ml);
			this.start();
		}
	}
	
	private Client getClientService() {
		IService client = PIMRuntime.getInstance().getServiceFactory().getService("Client");
		if (client!=null && client instanceof Client) {
			return (Client)client;
		}
		return null;
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

}
