package de.janrufmonitor.service.tellows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.service.IModifierService;

public class Tellows extends AbstractReceiverConfigurableService implements
		IEventSender, IModifierService {

	private class URLRequester {
		
		private Logger m_logger;
				
		public URLRequester() {
			this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		}
		
		public void go() {
			StringBuffer agent = new StringBuffer();
			agent.append("jAnrufmonitor Module Tracker (tellows) ");
			agent.append(IJAMConst.VERSION_DISPLAY);	
			
			if (m_logger.isLoggable(Level.INFO))
				this.m_logger.info("User-Agent: "+agent.toString());
			
			try {
				String key = getRuntime().getConfigManagerFactory().getConfigManager().getProperty("service.update.UpdateManager", "regkey");
				URLConnection c = createRequestURL(agent, key);
				c.connect();
				
				Object o = c.getContent();
				if (o instanceof InputStream) {
					InputStreamReader isr = new InputStreamReader((InputStream) o, "iso-8859-1");
					BufferedReader br = new BufferedReader(isr);
					
					while (br.ready()) {
						br.readLine();
					}
					
					br.close();
					isr.close();
				}				
			} catch (MalformedURLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);	
			} 
		}
		
		
		private URLConnection createRequestURL(StringBuffer agent, String key) throws IOException {
			StringBuffer reg = new StringBuffer();
			reg.append(getTellowsRegistry());
			reg.append("?k=");
			try {
				reg.append(URLEncoder.encode(key, "utf-8"));
			} catch (UnsupportedEncodingException ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
			URL url = new URL(reg.toString());
			if (m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Registry call: "+reg.toString());
			URLConnection c = url.openConnection();
			c.setDoInput(true);
			c.setRequestProperty("User-Agent", agent.toString());
			c.setRequestProperty("X-JAM-Module", NAMESPACE);
			c.setRequestProperty("X-JAM-Module-Key", getTellowsApiKey());

			return c;
		}
		
	}

	public static final String ID = "Tellows";
	private final String NAMESPACE = "service.Tellows";
	
	private final String CFG_TELLOWS_APIKEY = "apikey";
	private final String CFG_TELLOWS_REGISTRY = "registry";

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
        eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
        eventBroker.unregister(this);
        
        this.m_logger.info("Tellows is shut down ...");
    }
    
    public void startup() {
    	super.startup();
        IEventBroker eventBroker = this.getRuntime().getEventBroker();
        eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
        eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
        eventBroker.register(this);
        
        if (!this.isTellowsActivated()) {
        	eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
        	eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
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
        } else 
        	new URLRequester().go();
  
        this.m_logger.info("Tellows is started ...");            
    }
	
	public void receivedValidRule(ICall aCall) {
		// call is identified already
		if (!PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(aCall.getCaller().getPhoneNumber()) && 
			!aCall.getCaller().getPhoneNumber().isClired()) {
			
			if (!aCall.getCaller().getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49") && !aCall.getCaller().getPhoneNumber().getIntAreaCode().equalsIgnoreCase("43") && !aCall.getCaller().getPhoneNumber().getIntAreaCode().equalsIgnoreCase("41")) {
				if (this.m_logger.isLoggable(Level.INFO)) 
					this.m_logger.info("Country code not supported by tellows: 00"+aCall.getCaller().getPhoneNumber().getIntAreaCode());
				return;
			}
			
			String num = aCall.getCaller().getPhoneNumber().getTelephoneNumber();
			
			try {
				IAttributeMap m = TellowsProxy.getInstance().getTellowsData(num, aCall.getCaller().getPhoneNumber().getIntAreaCode());
				if (m.size()>0) {
					aCall.getCaller().getAttributes().addAll(m);
					IEventBroker eventBroker = this.getRuntime().getEventBroker();
					eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLMARKEDSPAM, aCall));
				}
			} catch (Exception e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR, 
								getRuntime().getI18nManagerFactory().getI18nManager().getString(NAMESPACE,
								"title", "label",
								getLanguage()), 
								e),
						"Tray");
			}			
		}
	}
	
	
	@Override
	public void setConfiguration(Properties configuration) {
		super.setConfiguration(configuration);
		TellowsProxy.invalidate();
	}
	
	private String getTellowsRegistry() {
		return this.m_configuration.getProperty(CFG_TELLOWS_REGISTRY, "");
	}
	
	private String getTellowsApiKey() {
		return this.m_configuration.getProperty(CFG_TELLOWS_APIKEY, "");
	}
	
	public boolean isTellowsActivated() {
		if (this.m_configuration!=null)
			return this.m_configuration.getProperty(CFG_TELLOWS_APIKEY, "").trim().length()>0;
		return false;
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

	public void modifyObject(Object o) {
		if (this.m_configuration.getProperty(CFG_TELLOWS_APIKEY, "").trim().length()==0) {
			if (this.m_logger.isLoggable(Level.WARNING)) 
				this.m_logger.warning("No tellows API key found. Service will be stopped.");
			return; 
		}
		
		if (o instanceof ICall) {
			o = ((ICall)o).getCaller();
		}
		if (o instanceof ICaller) { 
			ICaller caller = (ICaller)o;
			if (!PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(caller.getPhoneNumber()) && 
				!caller.getPhoneNumber().isClired()) {
					
				if (!caller.getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49") && !caller.getPhoneNumber().getIntAreaCode().equalsIgnoreCase("43") && !caller.getPhoneNumber().getIntAreaCode().equalsIgnoreCase("41")) {
					if (this.m_logger.isLoggable(Level.INFO)) 
						this.m_logger.info("Country code not supported by tellows: 00"+caller.getPhoneNumber().getIntAreaCode());
					return;
				}
				
				String num = caller.getPhoneNumber().getTelephoneNumber();
				
				try {
					IAttributeMap m = TellowsProxy.getInstance().getTellowsData(num, caller.getPhoneNumber().getIntAreaCode());
					if (m.size()>0) {
						caller.getAttributes().addAll(m);
					}
				} catch (Exception e) {
				}			
			}
		}
	}

}
