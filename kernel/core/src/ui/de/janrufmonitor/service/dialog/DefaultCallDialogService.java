package de.janrufmonitor.service.dialog;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.ui.jface.application.dialog.Dialog;
import de.janrufmonitor.ui.jface.application.dialog.ExtendedBalloonDialog;
import de.janrufmonitor.ui.swt.SWTExecuter;

public class DefaultCallDialogService extends AbstractReceiverConfigurableService {

	private String ID = "DefaultCallDialogService";
	private String NAMESPACE = "ui.jface.application.dialog.Dialog";

	private String CFG_BALLOON = "balloon";
	private String CFG_OUTGOING = "outgoing";
	private String CFG_DELAY = "delay";
	
	private IRuntime m_runtime;
	
	public DefaultCallDialogService() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public String getID() {
		return this.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public void shutdown() {
		super.shutdown();
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		this.m_logger.info("DefaultCallDialogService is shut down ...");
	}

	public void startup() {
		super.startup();
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		if (isDetectOutgoing())
			eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		
		this.m_logger.info("DefaultCallDialogService is started ...");
	}

	public void receivedValidRule(final ICall aCall) {
		ICaller theCaller = aCall.getCaller();
		if (theCaller!=null) {		
			this.m_logger.info("Opening new dialog instance...");
			
			final long ms = System.currentTimeMillis();
			
			this.m_logger.fine("Time measure in ms (init): "+(System.currentTimeMillis()-ms));
			
			new SWTExecuter(true, this.getID()) {
				protected void execute() {
					if (m_configuration.getProperty(CFG_BALLOON, "false").equalsIgnoreCase("false")) {
						m_logger.fine("Time measure in ms (SWTExecuter): "+(System.currentTimeMillis()-ms));
						Dialog j = new Dialog(m_configuration, aCall);
						m_logger.fine("Time measure in ms (SWTExecuter.open() - before): "+(System.currentTimeMillis()-ms));
						j.open();
						m_logger.fine("Time measure in ms (SWTExecuter.open() - after): "+(System.currentTimeMillis()-ms));
						j = null;
					} else {
						m_logger.fine("Time measure in ms (SWTExecuter): "+(System.currentTimeMillis()-ms));
						ExtendedBalloonDialog j = new ExtendedBalloonDialog(m_configuration, aCall);
						try {
							Thread.sleep(getDelay());
						} catch (InterruptedException e) {
						}
						j.createDialog();
						m_logger.fine("Time measure in ms (SWTExecuter.open() - before): "+(System.currentTimeMillis()-ms));
						j.open();
						m_logger.fine("Time measure in ms (SWTExecuter.open() - after): "+(System.currentTimeMillis()-ms));
						j = null;
					}
				}
			}.start();
			this.m_logger.fine("Time measure in ms (end): "+(System.currentTimeMillis()-ms));
		} else {
			this.m_logger.warning("Dialog not opened due to invalid caller data.");
		}
	}

	private boolean isDetectOutgoing() {
		return this.m_configuration.getProperty(CFG_OUTGOING, "false").equalsIgnoreCase("true");
	}
	
	private long getDelay() {
		return Long.parseLong(this.m_configuration.getProperty(CFG_DELAY, "0"));
	}
	
}
