package de.janrufmonitor.service.reject;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.string.StringUtils;

public class Reject extends AbstractReceiverConfigurableService implements IEventSender {
    
    private String ID = "Reject";
    private String NAMESPACE = "service.Reject";
    
    private String CONFIG_ALLCLIR = "allclir";
    private String CONFIG_REJECT_AREACODES = "rejectareacodes";
	private String SEPARATOR = ",";
	private String REJECT_ATTRIBUTE = "reject";
    
    private IRuntime m_runtime;
    private List m_rejectNumbers;
    private String m_language;
    
    public Reject() {
        super();
        this.getRuntime().getConfigurableNotifier().register(this);
    }
    
    public String getSenderID() {
        return this.ID;
    }
    
    public void shutdown() {
    	super.shutdown();
        IEventBroker eventBroker = this.getRuntime().getEventBroker();
        eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
        eventBroker.unregister(this);  
        
        if (m_rejectNumbers!=null) m_rejectNumbers.clear();
        m_rejectNumbers = null;
        
        this.m_logger.info("Reject is shut down ...");
    }
    
    public void startup() {
    	super.startup();

        IEventBroker eventBroker = this.getRuntime().getEventBroker();
        eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
        eventBroker.register(this);
        this.m_logger.info("Reject is started ...");            
    }
    
    public String getNamespace() {
        return this.NAMESPACE;
    }

	public String getID() {
		return this.ID;
	}

	public void receivedIdentifiedCall(IEvent event) {
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		ICall incommingCall = (ICall) event.getData();
		String rejectAttribute = (incommingCall.getCaller().getAttribute(this.REJECT_ATTRIBUTE) == null ? "" : incommingCall.getCaller().getAttribute(this.REJECT_ATTRIBUTE).getValue());
		boolean isClir = incommingCall.getCaller().getPhoneNumber().isClired();
                    
		// check if the caller has the attribute reject=yes
		if (rejectAttribute != null && rejectAttribute.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_YES)) {
			incommingCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_REJECTED));
			eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED, incommingCall));
			this.m_logger.info("Call automatically rejected by caller reject attribute.");
			String msg = getRuntime().getI18nManagerFactory().getI18nManager().getString(
					getNamespace(),
					"reject_caller", "description",
					getLanguage());
					
			msg = StringUtils.replaceString(msg, "{%1}", Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, incommingCall.getCaller()));
			
			PropagationFactory.getInstance().fire(
					new Message(Message.INFO, 
							getRuntime().getI18nManagerFactory().getI18nManager().getString(NAMESPACE,
							"title", "label",
							getLanguage()), 
							new Exception(msg)),
					"Tray");
			return;
		}

		// check if a special MSN should be rejected
		if (this.getRuntime().getRuleEngine().validate(this.ID, incommingCall.getMSN(), incommingCall.getCIP(), incommingCall.getCaller().getPhoneNumber())) {
			incommingCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_REJECTED));
			eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED, incommingCall));
			this.m_logger.info("Call automatically rejected by MSN " + incommingCall.getMSN().getMSN());
			String msg = getRuntime().getI18nManagerFactory().getI18nManager().getString(
					getNamespace(),
					"reject_msn", "description",
					getLanguage());
					
			msg = StringUtils.replaceString(msg, "{%1}", Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, incommingCall.getCaller()));
			
			PropagationFactory.getInstance().fire(
					new Message(Message.INFO, 
							getRuntime().getI18nManagerFactory().getI18nManager().getString(NAMESPACE,
							"title", "label",
							getLanguage()), 
							new Exception(msg)),
					"Tray");
			return;
		}
                    
		// check if all CLIR call should be rejected
		if (isClir && (this.m_configuration.getProperty(CONFIG_ALLCLIR) != null && this.m_configuration.getProperty(CONFIG_ALLCLIR).equalsIgnoreCase("true"))) {
			incommingCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_REJECTED));
			eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED, incommingCall));
			this.m_logger.info("Call automatically rejected because it was a CLIR call.");
			String msg = getRuntime().getI18nManagerFactory().getI18nManager().getString(
					getNamespace(),
					"reject_clir", "description",
					getLanguage());
					
			msg = StringUtils.replaceString(msg, "{%1}", Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, incommingCall.getCaller()));
			
			PropagationFactory.getInstance().fire(
					new Message(Message.INFO, 
							getRuntime().getI18nManagerFactory().getI18nManager().getString(NAMESPACE,
							"title", "label",
							getLanguage()), 
							new Exception(msg)),
					"Tray");
			return;
		}
		
		if (this.isRejectNumber(incommingCall.getCaller().getPhoneNumber().getIntAreaCode() + incommingCall.getCaller().getPhoneNumber().getAreaCode() + incommingCall.getCaller().getPhoneNumber().getCallNumber())) {
			incommingCall.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_REJECTED));
			eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED, incommingCall));
			this.m_logger.info("Call automatically rejected by a blocked configured number.");
			String msg = getRuntime().getI18nManagerFactory().getI18nManager().getString(
					getNamespace(),
					"reject_config", "description",
					getLanguage());
					
			msg = StringUtils.replaceString(msg, "{%1}", Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, incommingCall.getCaller()));
			
			PropagationFactory.getInstance().fire(
					new Message(Message.INFO, 
							getRuntime().getI18nManagerFactory().getI18nManager().getString(NAMESPACE,
							"title", "label",
							getLanguage()), 
							new Exception(msg)),
					"Tray");
			return;
		}
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
	
	private boolean isRejectNumber(String number) {
		if (this.m_rejectNumbers==null) {
			StringTokenizer st = new StringTokenizer(this.m_configuration.getProperty(CONFIG_REJECT_AREACODES, ""), SEPARATOR);
			this.m_rejectNumbers = new ArrayList(st.countTokens());
			while (st.hasMoreTokens()) {
				this.m_rejectNumbers.add(PhonenumberAnalyzer.getInstance().normalize(st.nextToken().trim()));	
			}
		}
		if (this.m_rejectNumbers.contains(number)) return true;
		
		if (number.length()<=1) return false;
		
		return this.isRejectNumber(number.substring(0, number.length()-1));	
	}
	
	private String getLanguage() {
		if (this.m_language==null) {
			this.m_language = 
				this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
					IJAMConst.GLOBAL_NAMESPACE,
					IJAMConst.GLOBAL_LANGUAGE
				);
		}
		return this.m_language;
	}


}
