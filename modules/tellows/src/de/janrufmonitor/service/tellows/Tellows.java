package de.janrufmonitor.service.tellows;

import java.util.Properties;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.framework.monitor.PhonenumberInfo;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;

public class Tellows extends AbstractReceiverConfigurableService implements
		IEventSender {

	private final String ID = "Tellows";
	private final String NAMESPACE = "service.Tellows";
	
	private final String CFG_TELLOWS_APIKEY = "apikey";

	private IRuntime m_runtime;
	private String m_language;
	
    public Tellows() {
        super();
        this.getRuntime().getConfigurableNotifier().register(this);
    }

	public String getSenderID() {
		return ID;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public String getID() {
		return ID;
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
        eventBroker.unregister(this);
        
        this.m_logger.info("Tellows is shut down ...");
    }
    
    public void startup() {
    	super.startup();
        IEventBroker eventBroker = this.getRuntime().getEventBroker();
        eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
        eventBroker.register(this);
        
        if (this.m_configuration.getProperty(CFG_TELLOWS_APIKEY, "").trim().length()==0) {
        	eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
            eventBroker.unregister(this);
            this.m_logger.warning("No tellows API key found. Service will be stopped.");
            String msg = getRuntime().getI18nManagerFactory().getI18nManager().getString(
					getNamespace(),
					"no_api_key", "description",
					getLanguage());
            
            PropagationFactory.getInstance().fire(
					new Message(Message.WARNING, 
							getRuntime().getI18nManagerFactory().getI18nManager().getString(NAMESPACE,
							"title", "label",
							getLanguage()), 
							new Exception(msg)),
					"Tray");
        }
  
        this.m_logger.info("Tellows is started ...");            
    }
	
	public void receivedValidRule(ICall aCall) {
		// call is identified already
		if (!PhonenumberInfo.isInternalNumber(aCall.getCaller().getPhoneNumber()) && 
			!aCall.getCaller().getPhoneNumber().isClired() &&
			aCall.getCaller().getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49")) {
			
			String num = aCall.getCaller().getPhoneNumber().getTelephoneNumber();
			IAttributeMap m = TellowsProxy.getInstance().getTellowsData(num);
			if (m.size()>0) {
				aCall.getCaller().getAttributes().addAll(m);
				IEventBroker eventBroker = this.getRuntime().getEventBroker();
				eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLMARKEDSPAM, aCall));
			}			
		}
	}
	
	
	@Override
	public void setConfiguration(Properties configuration) {
		super.setConfiguration(configuration);
		TellowsProxy.invalidate();
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
