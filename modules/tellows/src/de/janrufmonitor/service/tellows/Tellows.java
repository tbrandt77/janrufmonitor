package de.janrufmonitor.service.tellows;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.janrufmonitor.framework.IAttribute;
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
	
	private class XMLTellowsHandler extends DefaultHandler {
		private IAttributeMap tellowsAttributes = PIMRuntime.getInstance().getCallerFactory().createAttributeMap();
		private IAttribute tellowsAtt;
		
		private String currentValue; 
		
		public void characters(char[] ch, int start, int length)
	      throws SAXException {
			currentValue = new String(ch, start, length);
		}
		
		public void startElement(String uri, String name, String qname, Attributes attributes)
		throws SAXException {
			if (qname.equalsIgnoreCase("number") || 
					qname.equalsIgnoreCase("score") || 
					qname.equalsIgnoreCase("searches") || 
					qname.equalsIgnoreCase("comments") || 
					qname.equalsIgnoreCase("scorePath") ||
					qname.equalsIgnoreCase("scoreColor") )
			tellowsAtt = PIMRuntime.getInstance().getCallerFactory().createAttribute("tellows."+qname, ""); 
		}
		
		public void endElement(String uri, String name, String qname)
		throws SAXException {
			if (qname.equalsIgnoreCase("number") || 
					qname.equalsIgnoreCase("score") || 
					qname.equalsIgnoreCase("searches") || 
					qname.equalsIgnoreCase("comments") || 
					qname.equalsIgnoreCase("scorePath") ||
					qname.equalsIgnoreCase("scoreColor") ) {
				this.tellowsAtt.setValue(currentValue);
				this.tellowsAttributes.add(this.tellowsAtt);
				if (qname.equalsIgnoreCase("scoreColor") && isSpamColoring()) {
					this.addSpamColor(currentValue);
				}
			}
		}
		
		private void addSpamColor(String color) {
			if (color!=null && color.length()==7 && color.startsWith("#")) {
				color = color.substring(1);
				IAttribute a = PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_SPAM_COLOR, Integer.parseInt(color.substring(0,2), 16) + "," +
						Integer.parseInt(color.substring(2,4), 16) + "," +
						Integer.parseInt(color.substring(4,6), 16));
				this.tellowsAttributes.add(a);
			}
			
		}

		public IAttributeMap getAttributes() {
			return this.tellowsAttributes;
		}
		
	}
	
	private final String ID = "Tellows";
	private final String NAMESPACE = "service.Tellows";
	
	private final String TELLOWS_PARTNER = "test";
	private final String TELLOWS_APIKEY = "test123";
	
	private final String CFG_SPAMCOLOR = "spamcolor";
	
	private IRuntime m_runtime;
	
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
  
        this.m_logger.info("Tellows is started ...");            
    }
	
	public void receivedValidRule(ICall aCall) {
		// call is identified already
		if (!PhonenumberInfo.isInternalNumber(aCall.getCaller().getPhoneNumber()) && 
			!aCall.getCaller().getPhoneNumber().isClired() &&
			aCall.getCaller().getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49")) {
			
			String num = aCall.getCaller().getPhoneNumber().getTelephoneNumber();
			IAttributeMap m = this.getTellowsDate(num);
			if (m.size()>0) {
				aCall.getCaller().getAttributes().addAll(m);
				IEventBroker eventBroker = this.getRuntime().getEventBroker();
				eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLMARKEDSPAM, aCall));
			}			
		}
		
	}
	
	private IAttributeMap getTellowsDate(String number) {
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		StringBuffer url_string = new StringBuffer();
		url_string.append("http://www.tellows.de/basic/num/");
		url_string.append((number.startsWith("0") ? number : "0"+number));
		url_string.append("?xml=1&partner=");
		url_string.append(TELLOWS_PARTNER);
		url_string.append("&apikey=");
		url_string.append(TELLOWS_APIKEY);
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Tellows.de URL: "+url_string.toString());
	
		try {
			URL url = new URL(url_string.toString());
			URLConnection c = url.openConnection();
	
			c.setDoInput(true);
			c.setRequestProperty(
				"User-Agent",
				"jAnrufmonitor " + IJAMConst.VERSION_DISPLAY);
			c.connect();
	
			StringBuffer content = new StringBuffer();
			
			Object o = c.getContent();
			if (o instanceof InputStream) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Content successfully retrieved from "+url.getHost()+"...");
				InputStreamReader isr = new InputStreamReader((InputStream) o, "iso-8859-1");
				Thread.sleep(200);
				
				BufferedReader br = new BufferedReader(isr);
	
				while (br.ready()) {
					content.append(br.readLine());
				}
				
				br.close();
				isr.close();
			}
			
			if (content.length()>10) { 
				XMLTellowsHandler handler = new XMLTellowsHandler();
				SAXParser p = SAXParserFactory.newInstance().newSAXParser();
				ByteArrayInputStream in = new ByteArrayInputStream(content.toString().getBytes("iso-8859-1"));
				InputSource is = new InputSource(in);
				is.setEncoding("iso-8859-1");
				p.parse(is, handler);
				m.addAll(handler.getAttributes());
			}
		} catch (MalformedURLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (InterruptedException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (SAXException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} 
		return m;
	}
	
	private boolean isSpamColoring() {
		return (this.m_configuration!=null && this.m_configuration.getProperty(CFG_SPAMCOLOR, "false").equalsIgnoreCase("true"));
	}

}
