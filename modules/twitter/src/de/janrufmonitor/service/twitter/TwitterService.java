package de.janrufmonitor.service.twitter;

import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.util.formatter.Formatter;


public class TwitterService extends AbstractReceiverConfigurableService implements ITwitterServiceConst {

	private String ID = "TwitterService";
    public static final String NAMESPACE = "service.TwitterService";
    private IRuntime m_runtime;
    
    public TwitterService() {
    	super();
    	this.getRuntime().getConfigurableNotifier().register(this);	
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
	
	public void receivedValidRule(ICall aCall) {
		if (this.m_configuration.getProperty(ITwitterServiceConst.CFG_AUTH1_TOKEN, "").length()==0 || this.m_configuration.getProperty(ITwitterServiceConst.CFG_AUTH2_TOKEN, "").length()==0) {
			this.m_logger.warning("TwitterService is not yet autheticated on twitter.com.");
			return;
		}
		
		OAuthSignpostClient c= new OAuthSignpostClient(ITwitterServiceConst.CONSUMER_KEY, 
				ITwitterServiceConst.CONSUMER_SECRET, this.m_configuration.getProperty(ITwitterServiceConst.CFG_AUTH1_TOKEN, ""), this.m_configuration.getProperty(ITwitterServiceConst.CFG_AUTH2_TOKEN, ""));
		
		Twitter twitter = new Twitter(null, c);
		String msg = Formatter.getInstance(getRuntime()).parse((this.isOutgoing(aCall) ? this.m_configuration.getProperty(ITwitterServiceConst.CFG_OUTMESSAGE, "") : this.m_configuration.getProperty(ITwitterServiceConst.CFG_INMESSAGE, "")), aCall);
		if (msg.length()==0) return;
		if (msg.length()>139) {
			msg = msg.substring(0,139);
			this.m_logger.warning("Message is more then 140 character. Truncated to: "+msg);
		}
		twitter.setSource("jAnrufmonitor "+IJAMConst.VERSION_DISPLAY+" Twitter Module");
		twitter.setStatus(msg);
	}

	private boolean isOutgoing(ICall aCall) {
		if (aCall!=null){
			IAttribute att = aCall.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
			if (att!=null) {
				return att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING);
			}
		}
		return false;
	}
	
	public void startup() {
		super.startup();
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		
		if (this.m_configuration.getProperty(CFG_INCOMING, "false").equalsIgnoreCase("true")) {
			eventBroker.register(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		}
		if (this.m_configuration.getProperty(CFG_OUTGOING, "false").equalsIgnoreCase("true")) {
			eventBroker.register(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		}
	}

	public void shutdown() {
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.unregister(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		super.shutdown();
	}

}
