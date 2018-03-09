package de.janrufmonitor.fritzbox;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.logging.LoggingInitializer;
import de.janrufmonitor.util.string.StringEscapeUtils;

public class FritzBoxTR064Manager {

	private final static String PATTERN_DETECT_NONCE = "<Nonce>([^<]*)</Nonce>";
	private final static String PATTERN_DETECT_REALM = "<Realm>([^<]*)</Realm>";
	private final static String USER_AGENT = "jAnrufmonitor-SOAP-Client 1.0";
	
	// getCallList PATTERN
	private final static String PATTERN_CSV_CALLLIST = "<NewCallListURL>([^<]*)</NewCallListURL>";
	
	// getPhonebook PATTERN
	private final static String PATTERN_PHONEBOOK_LIST = "<NewPhonebookList>([^<]*)</NewPhonebookList>";
	private final static String PATTERN_PHONEBOOK_NAME = "<NewPhonebookName>([^<]*)</NewPhonebookName>";
	private final static String PATTERN_PHONEBOOK_URL = "<NewPhonebookURL>([^<]*)</NewPhonebookURL>";
	private final static String PATTERN_PHONEBOOK_ENTRY_DATA = "<NewPhonebookEntryData>([^<]*)</NewPhonebookEntryData>";
	private final static String PATTERN_PHONEBOOK_SIZE = "<!-- number of contacts ([^<]*) -->";
	
	// getTAM PATTERN
	private final static String PATTERN_TAM_NAME = "<NewName>([^<]*)</NewName>";
	private final static String PATTERN_TAM_URL = "<NewURL>([^<]*)</NewURL>";
	
	// getSID PATTERN
	private final static String PATTERN_SID = "<NewX_AVM-DE_UrlSID>sid=([^<]*)</NewX_AVM-DE_UrlSID>";
	
	// getDefaultTR064SecurePort PATTERN
	private final static String PATTERN_SECURE_PORT = "<NewSecurityPort>([^<]*)</NewSecurityPort>";
	
	// isTR064Supported PATTERN
	private final static String PATTERN_IS_TR064_SUPPORTED = "<presentationURL>([^<]*)</presentationURL>";
	
	// getFritzBoxFirmwareVersion PATTERN
	private final static String PATTERN_FIMRMWARE_VERSION = "<NewSoftwareVersion>([^<]*)</NewSoftwareVersion>";
	private final static String PATTERN_MODEL_NAME = "<NewModelName>([^<]*)</NewModelName>";
	private final static String PATTERN_SERIAL_NUMBER = "<NewSerialNumber>([^<]*)</NewSerialNumber>";
	
	// getPhonePorts PATTERN
	private final static String PATTERN_PORT_NAME = "<NewX_AVM-DE_PhoneName>([^<]*)</NewX_AVM-DE_PhoneName>";
	
	// getUptime PATTERN
	private final static String PATTERN_UPTIME = "<NewUpTime>([^<]*)</NewUpTime>";
	
	// getSIPResolution
	private final static String PATTERN_SIP_NUMBERS = "<NewNumberList>([^<]*)</NewNumberList>";
	
	// is PasswordValid PATTERN
	private final static String PATTERN_VALID_PASSWORD = "<Status>([^<]*)</Status>";
	
	private static FritzBoxTR064Manager m_instance = null;
	
	private Logger m_logger;
	private String m_cachedSecurePort = null;

	private FritzBoxTR064Manager() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
    }
    
    public static synchronized FritzBoxTR064Manager getInstance() {
        if (FritzBoxTR064Manager.m_instance == null) {
        	FritzBoxTR064Manager.m_instance = new FritzBoxTR064Manager();
        }
        return FritzBoxTR064Manager.m_instance;
    }
    
    public String getSID(String usr, String passwd) throws IOException {
    	return this.getSID(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https");
    }
    
    public String getSID(String usr, String passwd, String server) throws IOException {
    	return this.getSID(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
    }
    
    public String getSID(String usr, String passwd, String server, String port, String protocol) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getSID(String usr, String passwd, String server, String port, String protocol)");
    	String user = new String(usr.getBytes("utf-8"));
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:X_AVM-DE_CreateUrlSID xmlns:u=\"urn:dslforum-org:service:DeviceConfig:1\">");
		content.append("</u:X_AVM-DE_CreateUrlSID></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceconfig", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceConfig:1#X_AVM-DE_CreateUrlSID\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:X_AVM-DE_CreateUrlSID xmlns:u=\"urn:dslforum-org:service:DeviceConfig:1\">");
		content.append("</u:X_AVM-DE_CreateUrlSID></s:Body></s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceconfig", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceConfig:1#X_AVM-DE_CreateUrlSID\""}, {"User-Agent", USER_AGENT}});
		
		String sid = find(Pattern.compile(PATTERN_SID, Pattern.UNICODE_CASE), response);
		
		return (sid!=null && sid.length()>0 ? sid : null);
    }
    
    public boolean isPasswordValid(String usr, String passwd) throws IOException {
    	return this.isPasswordValid(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https");
    }
    
    public boolean isPasswordValid(String usr, String passwd, String server) throws IOException {
    	return this.isPasswordValid(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
    }
    
    public boolean isPasswordValid(String usr, String passwd, String server, String port, String protocol) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering isPasswordValid(String usr, String passwd, String server, String port, String protocol)");
    	String user = new String(usr.getBytes("utf-8"));
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:X_AVM-DE_CreateUrlSID xmlns:u=\"urn:dslforum-org:service:DeviceConfig:1\">");
		content.append("</u:X_AVM-DE_CreateUrlSID></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceconfig", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceConfig:1#X_AVM-DE_CreateUrlSID\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:X_AVM-DE_CreateUrlSID xmlns:u=\"urn:dslforum-org:service:DeviceConfig:1\">");
		content.append("</u:X_AVM-DE_CreateUrlSID></s:Body></s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceconfig", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceConfig:1#X_AVM-DE_CreateUrlSID\""}, {"User-Agent", USER_AGENT}});
		
		String sid = find(Pattern.compile(PATTERN_VALID_PASSWORD, Pattern.UNICODE_CASE), response);
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Password validation: "+sid);
		
		return (sid!=null && sid.trim().equalsIgnoreCase("authenticated") ? true : false);
    }
    
    public void _hangup(String usr, String passwd, String server, String port, String protocol) throws IOException {
    	String user = new String(usr.getBytes("utf-8"));
    	
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:X_AVM-DE_DialGetConfig xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\">");
		content.append("</u:X_AVM-DE_DialGetConfig></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_DialGetConfig\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:X_AVM-DE_Hangup xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\">");
		content.append("</u:X_AVM-DE_Hangup></s:Body></s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_Hangup\""}, {"User-Agent", USER_AGENT}});
    }
    
    public void _dial(String usr, String passwd, String server, String port, String protocol, String number) throws IOException {
    	String user = new String(usr.getBytes("utf-8"));
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:X_AVM-DE_DialGetConfig xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\">");
		content.append("</u:X_AVM-DE_DialGetConfig></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_DialGetConfig\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:X_AVM-DE_DialGetConfig xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\">");
		content.append("</u:X_AVM-DE_DialGetConfig></s:Body></s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_DialGetConfig\""}, {"User-Agent", USER_AGENT}});
		
		for (int i=1; i<99; i++) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:X_AVM-DE_GetPhonePort xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\"><NewIndex>"+i+"</NewIndex>");
		content.append("</u:X_AVM-DE_GetPhonePort></s:Body></s:Envelope>");
		
		try {
			response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
				{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_GetPhonePort\""}, {"User-Agent", USER_AGENT}});
			System.out.println(i);
		}catch (Exception e) {
			
		}
		}
    }
    

    public Map getPhonePorts(String usr, String passwd, String server, String port, String protocol) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getPhonePorts(String usr, String passwd, String server, String port, String protocol, String id)");
    	String user = new String(usr.getBytes("utf-8"));

    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:X_AVM-DE_GetPhonePort xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\">");
		content.append("</u:X_AVM-DE_GetPhonePort></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_GetPhonePort\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		boolean last = false;
		int c = 1;
		Map m = new HashMap();
		
		while (!last) {
			content = new StringBuffer();
			content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
			content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
			content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
			content.append("<Nonce>"+nonce+"</Nonce>");
			content.append("<Auth>"+auth+"</Auth>");
			content.append("<UserID>"+user+"</UserID>");
			content.append("<Realm>"+realm+"</Realm>");
			content.append("</h:ClientAuth></s:Header>");
			content.append("<s:Body><u:X_AVM-DE_GetPhonePort xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\"><NewIndex>"+c+"</NewIndex>");
			content.append("</u:X_AVM-DE_GetPhonePort></s:Body></s:Envelope>");
			
			try {
				response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
					{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_GetPhonePort\""}, {"User-Agent", USER_AGENT}});
				String port_name = find(Pattern.compile(PATTERN_PORT_NAME, Pattern.UNICODE_CASE), response);
				if (port_name!=null && port_name.length()>0) {
					m.put(new Integer(c), port_name);
				}
			} catch (IOException e) {
				last = true;
			}
			c++;
		}
		return m;
    }
    
    public String getPhonebookHash(String usr, String passwd, String id) throws IOException {
    	return this.getPhonebookHash(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(this.getDefaultFritzBoxDNS()), "https", id);
    }
    
    public String getPhonebookHash(String usr, String passwd, String server, String id) throws IOException {
    	return this.getPhonebookHash(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", id);
    }
    
    public String getPhonebookHash(String usr, String passwd, String server, String port, String protocol, String id) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getPhonebookHash(String usr, String passwd, String server, String port, String protocol, String id)");
    	
    	String user = new String(usr.getBytes("utf-8"));
    	
    	long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving phonebook hash...");
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetPhonebookList></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
    	
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetPhonebook xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"><NewPhonebookID>"+id+"</NewPhonebookID></u:GetPhonebook></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebook\""}, {"User-Agent", USER_AGENT}});
		
		String xml_url = find(Pattern.compile(PATTERN_PHONEBOOK_URL, Pattern.UNICODE_CASE), response);
		if (xml_url!=null && xml_url.length()>0) {
			response = doHttpCall(xml_url, "GET", null, new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			this.m_logger.info("Finished retrieving phonebook hash took "+(System.currentTimeMillis()-start)+"ms");
			 
			MessageDigest md = null;
		    try {
		    	md = MessageDigest.getInstance("MD5");
		    	md.update(response.toString().getBytes("utf-8"));
		    } catch (NoSuchAlgorithmException ex) {
		    } catch (UnsupportedEncodingException e) {
		    }
		    
		    byte[] data = md.digest();
		    StringBuffer buf = new StringBuffer();
		    for (int i = 0; i < data.length; i++) {
		      int halfbyte = (data[i] >>> 4) & 0x0F;
		      int two_halfs = 0;
		      do {
		        if ((0 <= halfbyte) && (halfbyte <= 9))
		          buf.append((char) ('0' + halfbyte));
		        else
		          buf.append((char) ('a' + (halfbyte - 10)));
		        halfbyte = data[i] & 0x0F;
		      } while(two_halfs++ < 1);
		    }
		    return buf.toString();
		} 
		
		this.m_logger.severe("No valid XML download link provided by fritzbox for hash calculation: "+xml_url);
		return null;
    }
    
    public String getSIPResolution(String usr, String passwd) throws IOException {
    	return this.getSIPResolution(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(this.getDefaultFritzBoxDNS()), "https");
    }
    
    public String getSIPResolution(String usr, String passwd, String server) throws IOException {
    	return this.getSIPResolution(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
    }
    
    public String getSIPResolution(String usr, String passwd, String server, String port, String protocol) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getSIPResolution(String usr, String passwd, String server, String port, String protocol)");
    	
    	String user = new String(usr.getBytes("utf-8"));

    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:X_AVM-DE_GetNumbers xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\">");
		content.append("</u:X_AVM-DE_GetNumbers></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_GetNumbers\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetNumbers xmlns:u=\"urn:dslforum-org:service:X_VoIP:1\"></u:GetNumbers></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_voip", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_GetNumbers\""}, {"User-Agent", USER_AGENT}})
		;
		
		String xml = find(Pattern.compile(PATTERN_SIP_NUMBERS, Pattern.UNICODE_CASE), response);
		if (xml!=null && xml.length()>0)
			try {
				xml = StringEscapeUtils.unescapeXml(xml);
			} catch (Exception e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		return (xml!=null && xml.length()>0 ? xml : null);
    }
    
    public String getPhonebookEntry(String usr, String passwd, String id, String entryId) throws IOException {
    	return this.getPhonebookEntry(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", id, entryId);
    }
    
    public String getPhonebookEntry(String usr, String passwd, String server, String id, String entryId) throws IOException {
    	return this.getPhonebookEntry(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", id, entryId);
    }
    
    public String getPhonebookEntry(String usr, String passwd, String server, String port, String protocol, String id, String entryId) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getPhonebookEntry(String usr, String passwd, String server, String port, String protocol, String id, String entryID)");
    	String user = new String(usr.getBytes("utf-8"));
    	
    	long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving phonebook entry...");
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetPhonebookList></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
    	
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetPhonebookEntry xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"><NewPhonebookID>"+id+"</NewPhonebookID><NewPhonebookEntryID>"+entryId+"</NewPhonebookEntryID></u:GetPhonebookEntry></s:Body>");
		content.append("</s:Envelope>");
		
		try {
			response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
				{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookEntry\""}, {"User-Agent", USER_AGENT}});
			
			String xml_url = find(Pattern.compile(PATTERN_PHONEBOOK_ENTRY_DATA, Pattern.UNICODE_CASE), response);
			if (xml_url!=null && xml_url.length()>0) {
				this.m_logger.info("Finished retrieving phonebook entry took "+(System.currentTimeMillis()-start)+"ms");
				
					return StringEscapeUtils.unescapeHtml(xml_url);
				
			} 
		} catch (Exception e) {
		}
		return null;
    }
    
    public int getPhonebookSize(String usr, String passwd, String id) throws IOException {
    	return this.getPhonebookSize(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", id);
    }
    
    public int getPhonebookSize(String usr, String passwd, String server, String id) throws IOException {
    	return this.getPhonebookSize(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", id);
    }
    
    public int getPhonebookSize(String usr, String passwd, String server, String port, String protocol, String id) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getPhonebookSize(String usr, String passwd, String server, String port, String protocol, String id)");
    	String user = new String(usr.getBytes("utf-8"));

		this.m_logger.info("Starting determining phonebook size...");
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetPhonebookList></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
    	
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetPhonebook xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"><NewPhonebookID>"+id+"</NewPhonebookID></u:GetPhonebook></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebook\""}, {"User-Agent", USER_AGENT}});
		
		String xml_url = find(Pattern.compile(PATTERN_PHONEBOOK_URL, Pattern.UNICODE_CASE), response);
		if (xml_url!=null && xml_url.length()>0) {
			response = doHttpCall(xml_url, "GET", null, "iso-8859-1", new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			String size = find(Pattern.compile(PATTERN_PHONEBOOK_SIZE, Pattern.UNICODE_CASE), response);
			if (size!=null && size.length()>0)
				return Integer.parseInt(size);
		} 
		return 0;
    }
    
    public void setPhonebookEntry(String usr, String passwd, String id, String data) throws IOException {
    	 this.setPhonebookEntry(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", id, data);
    }
    
    public void setPhonebookEntry(String usr, String passwd, String server, String id, String data) throws IOException {
    	 this.setPhonebookEntry(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", id, data);
    }
    
    public void setPhonebookEntry(String usr, String passwd, String server, String port, String protocol, String id, String data) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering setPhonebookEntry(String usr, String passwd, String server, String port, String protocol, String id, String data)");
    	String user = new String(usr.getBytes("utf-8"));
    
		this.m_logger.info("Starting retrieving phonebook entry...");
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetPhonebookList></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
    	
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:SetPhonebookEntry xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"><NewPhonebookID>"+id+"</NewPhonebookID><NewPhonebookEntryID></NewPhonebookEntryID><NewPhonebookEntryData>"+data+"</NewPhonebookEntryData></u:SetPhonebookEntry></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#SetPhonebookEntry\""}, {"User-Agent", USER_AGENT}});
		return;
    }

    public void deletePhonebookEntry(String usr, String passwd, String id, String entryID) throws IOException {
   	 	this.deletePhonebookEntry(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", id, entryID);
    }
   
    public void deletePhonebookEntry(String usr, String passwd, String server, String id, String entryID) throws IOException {
   	 	this.deletePhonebookEntry(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", id, entryID);
    }
   
    public void deletePhonebookEntry(String usr, String passwd, String server, String port, String protocol, String id, String entryID) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering deletePhonebookEntry(String usr, String passwd, String server, String port, String protocol, String id, String entryID)");
    	String user = new String(usr.getBytes("utf-8"));
    
		this.m_logger.info("Starting retrieving phonebook entry...");
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetPhonebookList></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
    	
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:DeletePhonebookEntry xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"><NewPhonebookID>"+id+"</NewPhonebookID><NewPhonebookEntryID>"+entryID+"</NewPhonebookEntryID></u:DeletePhonebookEntry></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#DeletePhonebookEntry\""}, {"User-Agent", USER_AGENT}});
		return;
    }
    
    public InputStream getPhonebook(String usr, String passwd, String id) throws IOException {
    	return this.getPhonebook(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", id);
    }
    
    public InputStream getPhonebook(String usr, String passwd, String server, String id) throws IOException {
    	return this.getPhonebook(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", id);
    }
    
    public InputStream getPhonebook(String usr, String passwd, String server, String port, String protocol, String id) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getPhonebook(String usr, String passwd, String server, String port, String protocol, String id)");
    	String user = new String(usr.getBytes("utf-8"));
    	
    	long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving phonebook...");
		
    	StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetPhonebookList></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
    	
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetPhonebook xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"><NewPhonebookID>"+id+"</NewPhonebookID></u:GetPhonebook></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebook\""}, {"User-Agent", USER_AGENT}});
		
		String xml_url = find(Pattern.compile(PATTERN_PHONEBOOK_URL, Pattern.UNICODE_CASE), response);
		if (xml_url!=null && xml_url.length()>0) {
			response = doHttpCall(xml_url, "GET", null, "iso-8859-1", new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			this.m_logger.info("Finished retrieving phonebook took "+(System.currentTimeMillis()-start)+"ms");
			return new ByteArrayInputStream(response.toString().getBytes("iso-8859-1"));
		} 
		
		this.m_logger.severe("No valid XML download link provided by fritzbox: "+xml_url);
		return null;
    }
    
    public Map getPhonebookList(String usr, String passwd) throws IOException {
    	return this.getPhonebookList(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https");
    }
    
    public Map getPhonebookList(String usr, String passwd, String server) throws IOException {
    	return this.getPhonebookList(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
    }
    
    public Map getPhonebookList(String usr, String passwd, String server, String port, String protocol) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getPhonebookList(String usr, String passwd, String server, String port, String protocol)");
    	String user = new String(usr.getBytes("utf-8"));
    	
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving phonebook list...");
		
		StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetPhonebookList></s:Body></s:Envelope>");

		//header.append(content);
		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetCallList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetPhonebookList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"></u:GetPhonebookList></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebookList\""}, {"User-Agent", USER_AGENT}});
		
		String id_list = find(Pattern.compile(PATTERN_PHONEBOOK_LIST, Pattern.UNICODE_CASE), response);
		
		if (id_list!=null && id_list.length()>0) {
			Map m = new HashMap();
			StringTokenizer st = new StringTokenizer(id_list, ",");
			while (st.hasMoreTokens()) {
				String id = st.nextToken();
				content = new StringBuffer();
				content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
				content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
				content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
				content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
				content.append("<Nonce>"+nonce+"</Nonce>");
				content.append("<Auth>"+auth+"</Auth>");
				content.append("<UserID>"+user+"</UserID>");
				content.append("<Realm>"+realm+"</Realm>");
				content.append("</h:ClientAuth></s:Header>");
				content.append("<s:Body><u:GetPhonebook xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"><NewPhonebookID>"+id+"</NewPhonebookID></u:GetPhonebook></s:Body>");
				content.append("</s:Envelope>");
				
				response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
					{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetPhonebook\""}, {"User-Agent", USER_AGENT}});
				
				String pb_name = find(Pattern.compile(PATTERN_PHONEBOOK_NAME, Pattern.UNICODE_CASE), response);
				if (pb_name!=null) {
					m.put(Integer.valueOf(id), pb_name);
				}
			}
			this.m_logger.info("Finished retrieving phonebook list took "+(System.currentTimeMillis()-start)+"ms");
			return m;
		}
		this.m_logger.info("No phonebooks found on fritzbox.");
		return Collections.EMPTY_MAP;
    }
    
    public InputStream getTelephoneAnsweringMachineMessageList(String usr, String passwd, String tam_id) throws IOException {
    	return this.getTelephoneAnsweringMachineMessageList(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", tam_id);
    }
    
    public InputStream getTelephoneAnsweringMachineMessageList(String usr, String passwd, String server, String tam_id) throws IOException {
    	return this.getTelephoneAnsweringMachineMessageList(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", tam_id);
    }
    
    public InputStream getTelephoneAnsweringMachineMessageList(String usr, String passwd, String server, String port, String protocol, String tam_id) throws IOException {
    	String user = new String(usr.getBytes("utf-8"));
    	
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving TAM message list...");
		
		StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetMessageList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_TAM:1\"><NewIndex>"+tam_id+"</NewIndex>");
		content.append("</u:GetMessageList></s:Body></s:Envelope>");

		//header.append(content);
		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_tam", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_TAM:1#GetMessageList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetMessageList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_TAM:1\">");
		content.append("<NewIndex>"+tam_id+"</NewIndex></u:GetMessageList></s:Body></s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_tam", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_TAM:1#GetMessageList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String xml_url = find(Pattern.compile(PATTERN_TAM_URL, Pattern.UNICODE_CASE), response);
		if (xml_url!=null && xml_url.length()>0) {
			response = doHttpCall(xml_url, "GET", null, "utf-8", new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			this.m_logger.info("Finished retrieving TAM message list "+(System.currentTimeMillis()-start)+"ms");
			return new ByteArrayInputStream(response.toString().getBytes("utf-8"));
		} 
		
		this.m_logger.severe("No valid XML download link provided by fritzbox: "+xml_url);
		return null;
    }
    
    public Map getTelephoneAnsweringMachineList(String usr, String passwd) throws IOException {
    	return this.getTelephoneAnsweringMachineList(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https");
    }
    
    public Map getTelephoneAnsweringMachineList(String usr, String passwd, String server) throws IOException {
    	return this.getTelephoneAnsweringMachineList(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
    }
    
    public Map getTelephoneAnsweringMachineList(String usr, String passwd, String server, String port, String protocol) throws IOException {
    	if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getTelephoneAnsweringMachineList(String usr, String passwd, String server, String port, String protocol)");
    	String user = new String(usr.getBytes("utf-8"));
    	
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving TAM list...");
		
		StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_TAM:1\">");
		content.append("</u:GetInfo></s:Body></s:Envelope>");

		//header.append(content);
		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_tam", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_TAM:1#GetInfo\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		Map m = new HashMap();
		int count = 0;
		while (count < 10) {
			content = new StringBuffer();
			content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
			content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
			content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
			content.append("<Nonce>"+nonce+"</Nonce>");
			content.append("<Auth>"+auth+"</Auth>");
			content.append("<UserID>"+user+"</UserID>");
			content.append("<Realm>"+realm+"</Realm>");
			content.append("</h:ClientAuth></s:Header>");
			content.append("<s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_TAM:1\"><NewIndex>"+Integer.valueOf(count)+"</NewIndex>");
			content.append("</u:GetInfo></s:Body></s:Envelope>");
			
			response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_tam", "POST", content.toString(), new String[][] { 
				{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_TAM:1#GetInfo\""}, {"User-Agent", USER_AGENT}})
			;
			
			String pb_name = find(Pattern.compile(PATTERN_TAM_NAME, Pattern.UNICODE_CASE), response);
			if (pb_name!=null && pb_name.trim().length()>0 && !m.containsValue(pb_name)) {
				m.put(Integer.valueOf(count), pb_name);
			}
			count++;
		}
		this.m_logger.info("Finished retrieving TAM list took "+(System.currentTimeMillis()-start)+"ms");
		return m;

    }
    
    public InputStream getCallList(String usr, String passwd) throws IOException {
    	return this.getCallList(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", -1);
    }
    
    public InputStream getCallList(String usr, String passwd, String server) throws IOException {
    	return this.getCallList(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(), "https", -1);
    }
    
    public InputStream getCallList(String usr, String passwd, int days) throws IOException {
    	return this.getCallList(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", days);
    }
    
    public InputStream getCallList(String usr, String passwd, String server, int days) throws IOException {
    	return this.getCallList(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(), "https", days);
    }

	public InputStream getCallList(String usr, String passwd, String server, String port, String protocol, int days) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getCallList(String usr, String passwd, String server, String port, String protocol, int days)");
		String user = new String(usr.getBytes("utf-8"));

		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving call list...");
		
		StringBuffer content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header><s:Body><u:GetCallList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\">");
		content.append("</u:GetCallList></s:Body></s:Envelope>");

		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetCallList\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetCallList xmlns:u=\"urn:dslforum-org:service:X_AVM-DE_OnTel:1\"></u:GetCallList></s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/x_contact", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_AVM-DE_OnTel:1#GetCallList\""}, {"User-Agent", USER_AGENT}});
		
		String csv_url = find(Pattern.compile(PATTERN_CSV_CALLLIST, Pattern.UNICODE_CASE), response);
		
		if (csv_url!=null && csv_url.length()>0) {
			response = doHttpCall(csv_url+"&type=csv"+(days>0 ? "&days="+days : ""), "GET", null, "utf-8", new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			this.m_logger.info("Finished retrieving call list took "+(System.currentTimeMillis()-start)+"ms");
			return new ByteArrayInputStream(response.toString().getBytes("iso-8859-1"));
		} 
		
		this.m_logger.severe("No valid CSV download link provided by fritzbox: "+csv_url);
		return null;
	}
	
	public String getDefaultFritzBoxDNS() {
		return "fritz.box";
	}
	
	public String getDefaultFritzBoxTR064Port() {
		return "49000";
	}
	
	public String getDefaultFritzBoxTR064SecurePort() throws IOException {
		return this.getDefaultFritzBoxTR064SecurePort(this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064Port());
	}
	
	public String getDefaultFritzBoxTR064SecurePort(String server) throws IOException {
		return this.getDefaultFritzBoxTR064SecurePort(server, this.getDefaultFritzBoxTR064Port());
	}
	
	public String getDefaultFritzBoxTR064SecurePort(String server, String port) throws IOException {
		if (this.m_cachedSecurePort!=null) return this.m_cachedSecurePort;
		
		StringBuffer content = new StringBuffer();
		
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"");
		content.append("s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
		content.append("<s:Body><u:GetSecurityPort xmlns:u=\"urn:dslforum-org:service:DeviceInfo:1\"></u:GetSecurityPort>");
		content.append("</s:Body>");
		content.append("</s:Envelope>");
		
		StringBuffer response = doHttpCall("http://"+server+":"+port+"/upnp/control/deviceinfo", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceInfo:1#GetSecurityPort\""}, {"User-Agent", USER_AGENT}})
		;
		
		String s_port = find(Pattern.compile(PATTERN_SECURE_PORT, Pattern.UNICODE_CASE), response);
		
		this.m_cachedSecurePort = s_port;
		
		return (s_port!=null && s_port.length()>0 ? s_port: null);
	}
	
	public String getFirmwareVersion(String usr, String passwd, String server) throws IOException {
		return this.getFirmwareVersion(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
	}

	public String getFirmwareVersion(String usr, String passwd, String server, String port, String protocol) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getFirmwareVersion(String usr, String passwd, String server, String port, String protocol)");
		String user = new String(usr.getBytes("utf-8"));
		
		StringBuffer content = new StringBuffer();
		
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"");
		content.append("s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header>");
		content.append("<s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:DeviceInfo:1\"></u:GetInfo>");
		content.append("</s:Body>");
		content.append("</s:Envelope>");
		
		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceinfo", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceInfo:1#GetInfo\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:DeviceInfo:1\"></u:GetInfo>");
		content.append("</s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceinfo", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceInfo:1#GetInfo\""}, {"User-Agent", USER_AGENT}});
		
		
		String firmware = find(Pattern.compile(PATTERN_FIMRMWARE_VERSION, Pattern.UNICODE_CASE), response);
		
		return (firmware!=null && firmware.length()>0 ? firmware: null);
	}
	
	public long getUptime(String usr, String passwd, String server) throws IOException {
		return this.getUptime(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
	}
	
	public long getUptime(String usr, String passwd, String server, String port, String protocol) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getUptime(String usr, String passwd, String server, String port, String protocol)");
		String user = new String(usr.getBytes("utf-8"));
		
		StringBuffer content = new StringBuffer();
		
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"");
		content.append("s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header>");
		content.append("<s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:DeviceInfo:1\"></u:GetInfo>");
		content.append("</s:Body>");
		content.append("</s:Envelope>");
		
		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceinfo", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceInfo:1#GetInfo\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:DeviceInfo:1\"></u:GetInfo>");
		content.append("</s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceinfo", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceInfo:1#GetInfo\""}, {"User-Agent", USER_AGENT}});
		
		
		String uptime = find(Pattern.compile(PATTERN_UPTIME, Pattern.UNICODE_CASE), response);
		
		return (uptime!=null && uptime.length()>0 ? Long.parseLong(uptime): -1L);
	}
	
	public String[] getDescription(String usr, String passwd, String server) throws IOException {
		return this.getDescription(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
	}
	
	public String[] getDescription(String usr, String passwd, String server, String port, String protocol) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering getDescription(String usr, String passwd, String server, String port, String protocol)");
		String user = new String(usr.getBytes("utf-8"));
		
		StringBuffer content = new StringBuffer();
		
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"");
		content.append("s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
		content.append("<s:Header><h:InitChallenge xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<UserID>"+user+"</UserID>");
		content.append("</h:InitChallenge></s:Header>");
		content.append("<s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:DeviceInfo:1\"></u:GetInfo>");
		content.append("</s:Body>");
		content.append("</s:Envelope>");
		
		StringBuffer response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceinfo", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceInfo:1#GetInfo\""}, {"User-Agent", USER_AGENT}})
		;
		
		String nonce = find(Pattern.compile(PATTERN_DETECT_NONCE, Pattern.UNICODE_CASE), response);
		String realm = find(Pattern.compile(PATTERN_DETECT_REALM, Pattern.UNICODE_CASE), response);
		
		String auth = FritzBoxMD5Handler.getTR064Auth(usr, passwd, realm, nonce);
		
		content = new StringBuffer();
		content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		content.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"");
		content.append("xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" >");
		content.append("<s:Header><h:ClientAuth xmlns:h=\"http://soap-authentication.org/digest/2001/10/\" s:mustUnderstand=\"1\">");
		content.append("<Nonce>"+nonce+"</Nonce>");
		content.append("<Auth>"+auth+"</Auth>");
		content.append("<UserID>"+user+"</UserID>");
		content.append("<Realm>"+realm+"</Realm>");
		content.append("</h:ClientAuth></s:Header>");
		content.append("<s:Body><u:GetInfo xmlns:u=\"urn:dslforum-org:service:DeviceInfo:1\"></u:GetInfo>");
		content.append("</s:Body>");
		content.append("</s:Envelope>");
		
		response = doHttpCall(protocol+"://"+server+":"+port+"/upnp/control/deviceinfo", "POST", content.toString(), new String[][] { 
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:DeviceInfo:1#GetInfo\""}, {"User-Agent", USER_AGENT}});
		
		
		String firmware = find(Pattern.compile(PATTERN_FIMRMWARE_VERSION, Pattern.UNICODE_CASE), response);
		String model = find(Pattern.compile(PATTERN_MODEL_NAME, Pattern.UNICODE_CASE), response);
		String serial = find(Pattern.compile(PATTERN_SERIAL_NUMBER, Pattern.UNICODE_CASE), response);
		
		return new String[] {(model!=null && model.length()>0? model : ""), (firmware!=null && firmware.length()>0? firmware : ""), "de", (serial!=null && serial.length()>0? serial : "") };
	}
	
	public boolean isTR064Supported(String server, String port) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Entering isTR064Supported(String server, String port)");
		if ("true".equalsIgnoreCase(System.getProperty("jam.fritzbox.tr064off", "false"))) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("System property jam.fritzbox.tr064off=true: TR-064 is switched off on jAnrufmonitor");
			return false;
		}
		
		StringBuffer response = null;
		
		try {
			response = doHttpCall("http://"+server+":"+port+"/tr64desc.xml", "GET", null, new String[][] { 
					{"Connection", "Close"}, {"User-Agent", USER_AGENT}})
			;
		} catch (FileNotFoundException ex) {
			if (this.m_logger.isLoggable(Level.WARNING))
				this.m_logger.warning(ex.toString()+": "+ex.getMessage());
			
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Can't access http://"+server+":"+port+"/tr64desc.xml: TR-064 is switched off on FRITZ!Box");
			
			PropagationFactory.getInstance().fire(
					new Message(Message.ERROR,
						"fritzbox.firmware.hardware",
						"notr064",
						new String[] {server},
						ex,
						false));
			
			return false;
		}

		String isSupported = find(Pattern.compile(PATTERN_IS_TR064_SUPPORTED, Pattern.UNICODE_CASE), response);
		
		return (isSupported!=null && isSupported.length()>0 ? true: false);
	}
	
	private StringBuffer doHttpCall(String u, String method, String body, String[][] headers) throws IOException {
		return this.doHttpCall(u, method, body, "UTF-8", headers);
	}
	
	private StringBuffer doHttpCall(String u, String method, String body, String encoding, String[][] headers) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("HTTP call: "+u+", method "+method);
		
		URL url = new URL( u);
		URLConnection connection = null; 
		if (url.getProtocol().equalsIgnoreCase("https")) {
			connection = (HttpsURLConnection) url.openConnection();
			((HttpsURLConnection)connection).setRequestMethod( method );
		} else {
			connection = (HttpURLConnection) url.openConnection();
			((HttpURLConnection)connection).setRequestMethod( method );
		}

		connection.setDoInput( true );
		connection.setDoOutput( true );
		connection.setUseCaches( false );
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("HTTP headers...");
		for (int i=0; i<headers.length; i++) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info(headers[i][0]+": "+headers[i][1]);
			connection.setRequestProperty( headers[i][0], headers[i][1]);
		}
		
		if (body!=null) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("HTTP body...");
				this.m_logger.info(body);
			}
			
			OutputStreamWriter writer = new OutputStreamWriter( connection.getOutputStream() );
			writer.write( body );
			writer.flush();
			writer.close();
		}

		StringBuffer response = new StringBuffer();

		if (this.m_logger.isLoggable(Level.INFO)) 
			this.m_logger.info("HTTP response set as text or XML data");
		BufferedReader reader = new BufferedReader(
	                          new InputStreamReader(connection.getInputStream(), encoding) );
		for ( String line; (line = reader.readLine()) != null; ) {
			response.append(line);
			response.append(IJAMConst.CRLF);
		}
		reader.close();

		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("HTTP response...");
			this.m_logger.info(response.toString());
		}
		
		return response;
	}
	
	private String find(Pattern p, StringBuffer c){
		Matcher m = p.matcher(c);
		if (m.find() && m.groupCount()==1) {
			return m.group(1).trim();
		}
		return null;		
	}
	
	public static void main(String[] args) {
		try {
			LoggingInitializer.run();
			//System.out.print(FritzBoxTR064Manager.getInstance().getCallList("thilo.brandt", "Tb2743507", "fritz.box", "49000", "http", -1));
			//System.out.print(FritzBoxTR064Manager.getInstance().getPhonebookList("thilo.brandt", "Tb2743507", "fritz.box", "49000"));
//			BufferedReader r = new BufferedReader(new InputStreamReader(FritzBoxTR064Manager.getInstance().getPhonebook("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "0")));
//			while (r.ready())
//				System.out.println(r.readLine());
			
			
//			FritzBoxTR064Manager.getInstance().setPhonebookEntry("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "0", 
//					StringEscapeUtils.escapeHtml("<?xml version\"1.0\" encoding=\"utf-8\"?><contact><category>0</category><person><realName>Thilo &amp; Brandt</realName></person><telephony nid=\"1\"><number type=\"mobile\">0160889640117</number></telephony><services /><setup /><mod_time>1461363051</mod_time><uniqueid></uniqueid><myfield>testthewest</myfield></contact>")
//			);
//			
//			System.out.println(FritzBoxTR064Manager.getInstance().getPhonebookEntry("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "0", "3"));
//			
			//System.out.println(FritzBoxTR064Manager.getInstance().getTelephoneAnsweringMachineList("thilo.brandt", "Tb2743507", "fritz.box", "49000", "http"));
			//System.out.println(FritzBoxTR064Manager.getInstance().getTelephoneAnsweringMachineMessageList("thilo.brandt", "Tb2743507", "fritz.box", "49000", "http", "0"));
			BufferedReader r = new BufferedReader(new InputStreamReader(FritzBoxTR064Manager.getInstance().getTelephoneAnsweringMachineMessageList("thilo.brandt", "Tb2743507", "fritz.box", "49000", "http", "0")));
			while (r.ready())
				System.out.println(r.readLine());
//			int size = FritzBoxTR064Manager.getInstance().getPhonebookSize("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "1");
//			size -= 10;
//			System.out.println(size);
//			
//			for (int i = 0;i<size; i++) {
//				System.out.println(i);
//				System.out.println();
//				System.out.println(FritzBoxTR064Manager.getInstance().getPhonebookEntry("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "1", Integer.toString(i)));
//			}
			
			//FritzBoxTR064Manager.getInstance().deletePhonebookEntry("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "1", "20");
			
			//System.out.print(FritzBoxTR064Manager.getInstance().getPhonebook("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "0"));
			//System.out.println(FritzBoxTR064Manager.getInstance().getPhonebookHash("admin", "Tb2743507", "fritz.box", "0"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort());
			//System.out.print(FritzBoxTR064Manager.getInstance().getSID("thilo.brandt", "Tb274350"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getTelephoneAnsweringMachineMessageList("thilo.brandt", "xxxx","fritz.box", "49000", "http", "0"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getDescription("thilo.brandt", "Tb2743507","fritz.box"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getPhonePorts("thilo.brandt", "Tb2743507","fritz.box", "49443", "https"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}
