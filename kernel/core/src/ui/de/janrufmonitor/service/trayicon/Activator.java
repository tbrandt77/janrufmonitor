package de.janrufmonitor.service.trayicon;

import java.util.Map;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.AbstractCommand;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.framework.monitor.IMonitorListener;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;

public class Activator extends AbstractCommand implements IEventSender {

	private String ID = "Activator";
	private String NAMESPACE = "service.trayicon.Activator";

	private IRuntime m_runtime;
	
	public void execute() {
		IMonitorListener ml = PIMRuntime.getInstance().getMonitorListener();
		if (ml.isRunning()) {
			ml.stop();
		} else {
			ml.start();					
		}
		IService tray = this.getRuntime().getServiceFactory().getService("TrayIcon");
		if (tray != null && tray instanceof TrayIcon) {
			((TrayIcon) tray).setIconStateMonitorListener();
		}
		
		getRuntime().getEventBroker().register(this);
		
		getRuntime().getEventBroker().send(this, getRuntime().getEventBroker().createEvent(IEventConst.EVENT_TYPE_APPLICATION_READY));
		
		getRuntime().getEventBroker().unregister(this);
	}

	public boolean isExecutable() {
		return true;
	}

	public boolean isExecuting() {
		return false;
	}

	public String getID() {
		return this.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public void setParameters(Map m) {
		if (m!=null) {
			String status = (String) m.get("status");
			if (status!=null && status.length()>0) {
				IMonitorListener ml = PIMRuntime.getInstance().getMonitorListener();
				if (status.equalsIgnoreCase("invert")) {
					ml.stop();
				}
				if (status.equalsIgnoreCase("revert")) {
					ml.start();		
				}
				if (status.equalsIgnoreCase("delay")) {
					// do nothing so far	
				}
				IService tray = this.getRuntime().getServiceFactory().getService("TrayIcon");
				if (tray != null && tray instanceof TrayIcon) {
					((TrayIcon) tray).setIconStateMonitorListener();
				}
				
				if (!status.equalsIgnoreCase("delay"))
					PropagationFactory.getInstance().fire(new Message(
							(ml.isRunning() ? Message.INFO: Message.WARNING),
							getRuntime().getI18nManagerFactory().getI18nManager().getString("monitor.MonitorListener", "title", "label", 
							getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
									IJAMConst.GLOBAL_NAMESPACE,
									IJAMConst.GLOBAL_LANGUAGE)),
							new Exception((ml.isRunning() ? getRuntime().getI18nManagerFactory().getI18nManager().getString("monitor.MonitorListener", "on", "label", 
									getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
											IJAMConst.GLOBAL_NAMESPACE,
											IJAMConst.GLOBAL_LANGUAGE)): 
	
									getRuntime().getI18nManagerFactory().getI18nManager().getString("monitor.MonitorListener", "off", "label", 
											getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
													IJAMConst.GLOBAL_NAMESPACE,
													IJAMConst.GLOBAL_LANGUAGE))))
					), "Tray");
			}
		}
	}

	public String getSenderID() {
		return this.ID;
	}

	public int getPriority() {
		return 0;
	}

}
