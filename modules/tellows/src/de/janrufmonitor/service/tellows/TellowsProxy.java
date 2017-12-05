package de.janrufmonitor.service.tellows;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class TellowsProxy {
	
	private class XMLTellowsHandler extends DefaultHandler {
		private IAttributeMap tellowsAttributes = PIMRuntime.getInstance().getCallerFactory().createAttributeMap();
		private IAttribute tellowsAtt;
		
		private String currentValue; 
		private int score = 0;
		
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
				if (qname.equalsIgnoreCase("scoreColor") && this.score >= getMinScore() && isSpamColoring()) {
					this.addSpamColor(currentValue);
				}
				if (qname.equalsIgnoreCase("score")) {
					try {
						this.score = Integer.parseInt(currentValue);
					} catch (Exception e) {};
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

	private static TellowsProxy m_instance = null;
	
	private Logger m_logger;
	private IRuntime m_runtime;
	private Properties m_configuration;
	private String m_language;
	
	private final String NAMESPACE = "service.Tellows";
	private final String TELLOWS_PARTNER = "janrufmonitor";
	
	private final String CFG_SPAMCOLOR = "spamcolor";
	private final String CFG_TELLOWS_APIKEY = "apikey";
	private final String CFG_MIN_SCORE = "minscore";
	
	public static TellowsProxy getInstance() {
		if (m_instance == null) {
			m_instance = new TellowsProxy();
		}
		return m_instance;
	}
	
	public static void invalidate() {
		m_instance = null;
	}
	
	@SuppressWarnings("unchecked")
	private TellowsProxy() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_configuration = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	private boolean isSpamColoring() {
		return (this.m_configuration!=null && this.m_configuration.getProperty(CFG_SPAMCOLOR, "false").equalsIgnoreCase("true"));
	}
	
	private String getTellowsApiKey() {
		return this.m_configuration.getProperty(CFG_TELLOWS_APIKEY, "");
	}
	
	private int getMinScore() {
		return Integer.parseInt(this.m_configuration.getProperty(CFG_MIN_SCORE, "1"));
	}

	public IAttributeMap getTellowsData(String number, String country) throws Exception {
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		StringBuffer url_string = new StringBuffer();
		url_string.append("https://www.tellows.de");
//		if (country.equalsIgnoreCase("49")) {
//			url_string.append("de");
//		} else if (country.equalsIgnoreCase("43")) {
//			url_string.append("at");
//		} else if (country.equalsIgnoreCase("41")) {
//			url_string.append("ch");
//		} else {
//			url_string.append("de");
//		} 
		url_string.append("/basic/num/");
		url_string.append("00");
		url_string.append(country);
		url_string.append((number.startsWith("0") ? number.substring(1) : number));
		url_string.append("?xml=1&partner=");
		url_string.append(TELLOWS_PARTNER);
		url_string.append("&apikey=");
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Tellows API Key: "+getTellowsApiKey());
		url_string.append(getTellowsApiKey());
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Tellows.de URL: "+url_string.toString());
	
		try {
			URL url = new URL(url_string.toString());
			URLConnection c = null; //url.openConnection();
			if (url.getProtocol().equalsIgnoreCase("http")) {
				c = (HttpURLConnection) url.openConnection();
				((HttpURLConnection)c).setRequestMethod("GET");
			}
			if (url.getProtocol().equalsIgnoreCase("https")) {
				c = (HttpsURLConnection) url.openConnection();
				((HttpsURLConnection)c).setRequestMethod("GET");
			}
			c.setDoInput(true);
			c.setRequestProperty(
				"User-Agent",
				"jAnrufmonitor " + IJAMConst.VERSION_DISPLAY);
			c.connect();
	
			StringBuffer content = new StringBuffer();
			
			InputStream o = c.getInputStream();
			if (o != null) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Content successfully retrieved from "+url.getHost()+"...");
				InputStreamReader isr = new InputStreamReader(o, "iso-8859-1");
				Thread.sleep(200);
				
				BufferedReader br = new BufferedReader(isr);
	
				while (br.ready()) {
					content.append(br.readLine());
				}
				
				br.close();
				isr.close();
			}
			
			if (content.toString().indexOf("ERROR")>0) {
				// check for ERROR
				if (this.m_logger.isLoggable(Level.WARNING)) 
					this.m_logger.warning("Invalid tellows API key detected. Service will be not processed.");
	            
				String msg = getRuntime().getI18nManagerFactory().getI18nManager().getString(
	            		"service.Tellows",
						"invalid_api_key", "description",
						getLanguage());
	            if (this.m_logger.isLoggable(Level.INFO)) {
	            	this.m_logger.info(content.toString());
	            }
	            throw new Exception(msg);
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
