package de.janrufmonitor.service.clipboard;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.formatter.Formatter;

public class ClipboardService extends AbstractReceiverConfigurableService {
		
	private String ID = "ClipboardService";
	private String NAMESPACE = "service.ClipboardService";
	
	private IRuntime m_runtime;

	public ClipboardService() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);	
	}
	
	public void startup() {
		super.startup();
		
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		this.m_logger.info("ClipboardService is started ...");		
	}

	public void shutdown() {
		super.shutdown();
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		this.m_logger.info("ClipboardService is shut down ...");
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

	public void receivedIdentifiedCall(IEvent event) {
		super.receivedIdentifiedCall(event);
		ICall aCall = (ICall)event.getData();
		if (aCall!=null) {
			if (getRuntime().getRuleEngine().validate(this.getID(), aCall.getMSN(), aCall.getCIP(), aCall.getCaller().getPhoneNumber())) {
				if (PhonenumberAnalyzer.getInstance(this.getRuntime()).isClired(aCall.getCaller().getPhoneNumber())) {
					this.m_logger.info("Caller number is clired. Not copied to clipboard: "+aCall);
					return;
				}
				Clipboard cb = new Clipboard(DisplayManager.getDefaultDisplay());
				String textData = "";
				if (this.isRawnumberFormat()) {
					if (PhonenumberAnalyzer.getInstance(this.getRuntime()).getIntAreaPrefix().equalsIgnoreCase(aCall.getCaller().getPhoneNumber().getIntAreaCode())) {
						// recognized national call
						this.m_logger.info("Recognized national call: "+aCall);
						textData = "0" + aCall.getCaller().getPhoneNumber().getAreaCode() + aCall.getCaller().getPhoneNumber().getCallNumber();
					} else {
						this.m_logger.info("Recognized international call: "+aCall);
						textData = "00" + aCall.getCaller().getPhoneNumber().getIntAreaCode()+ aCall.getCaller().getPhoneNumber().getAreaCode() + aCall.getCaller().getPhoneNumber().getCallNumber();
					}
				} else 
					textData = Formatter.getInstance(this.getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, aCall);
				
				TextTransfer textTransfer = TextTransfer.getInstance();
				cb.setContents(new Object[]{(textData!=null ? textData : "")}, new Transfer[]{textTransfer});	
			} else {
				this.m_logger.info("No rule assigned to execute this service for call: "+aCall);
			}
		} 
	}
	
	private boolean isRawnumberFormat() {
		return (this.m_configuration.getProperty("rawnumber", "false").equalsIgnoreCase("true") ? true : false);
	}

}
