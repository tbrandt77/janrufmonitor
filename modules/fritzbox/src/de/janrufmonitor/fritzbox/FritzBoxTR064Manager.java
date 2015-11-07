package de.janrufmonitor.fritzbox;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.janrufmonitor.framework.IJAMConst;

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
	
	private static FritzBoxTR064Manager m_instance = null;
	
	private Logger m_logger;
	private String m_cachedSecurePort = null;

	private FritzBoxTR064Manager() {
		this.m_logger = LogManager.getLogManager().getLogger("");
		//this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
		    public X509Certificate[] getAcceptedIssuers(){return null;}
		    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
		    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
		}};

		// Install the all-trusting trust manager
		try {
		    SSLContext sc = SSLContext.getInstance("TLS");
		    sc.init(null, trustAllCerts, new SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {;
		}
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
			{"Content-Type", "text/xml; charset=\"utf-8\""}, {"Content-Length", Integer.toString(content.length())}, {"SOAPACTION", "\"urn:dslforum-org:service:X_VoIP:1#X_AVM-DE_DialGetConfig\""}, {"User-Agent", USER_AGENT}})
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
			response = doHttpCall(xml_url, "GET", null, new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			this.m_logger.info("Finished retrieving phonebook took "+(System.currentTimeMillis()-start)+"ms");
			 
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
		
		this.m_logger.severe("No valid XML download link provided by fritzbox for has calculation: "+xml_url);
		return null;
    }

    public InputStream getPhonebook(String usr, String passwd, String id) throws IOException {
    	return this.getPhonebook(usr, passwd, this.getDefaultFritzBoxDNS(), this.getDefaultFritzBoxTR064SecurePort(), "https", id);
    }
    
    public InputStream getPhonebook(String usr, String passwd, String server, String id) throws IOException {
    	return this.getPhonebook(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https", id);
    }
    
    public InputStream getPhonebook(String usr, String passwd, String server, String port, String protocol, String id) throws IOException {
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
			response = doHttpCall(xml_url, "GET", null, new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			this.m_logger.info("Finished retrieving phonebook took "+(System.currentTimeMillis()-start)+"ms");
			return new ByteArrayInputStream(response.toString().getBytes("utf-8"));
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
			response = doHttpCall(csv_url+"&type=csv"+(days>0 ? "&days="+days : ""), "GET", null, new String[][] {{"Content-Type", "text/plain"}, {"User-Agent", USER_AGENT}});
			this.m_logger.info("Finished retrieving call list took "+(System.currentTimeMillis()-start)+"ms");
			return new ByteArrayInputStream(response.toString().getBytes("utf-8"));
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
	
	public String[] getDescription(String usr, String passwd, String server) throws IOException {
		return this.getDescription(usr, passwd, server, this.getDefaultFritzBoxTR064SecurePort(server), "https");
	}
	
	public String[] getDescription(String usr, String passwd, String server, String port, String protocol) throws IOException {
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
		StringBuffer response = doHttpCall("http://"+server+":"+port+"/tr64desc.xml", "GET", null, new String[][] { 
			{"Connection", "Close"}, {"User-Agent", USER_AGENT}})
		;

		String isSupported = find(Pattern.compile(PATTERN_IS_TR064_SUPPORTED, Pattern.UNICODE_CASE), response);
		
		return (isSupported!=null && isSupported.length()>0 ? true: false);
	}
	
	private StringBuffer doHttpCall(String u, String method, String body, String[][] headers) throws IOException {
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
	                          new InputStreamReader(connection.getInputStream()) );
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
			//System.out.print(FritzBoxTR064Manager.getInstance().getCallList("thilo.brandt", "Tb2743507", "fritz.box", "49000"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getPhonebookList("thilo.brandt", "Tb2743507", "fritz.box", "49000"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getPhonebook("thilo.brandt", "Tb2743507", "fritz.box", "49443", "https", "0"));
			System.out.println(FritzBoxTR064Manager.getInstance().getPhonebookHash("admin", "Tb2743507", "fritz.box", "0"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort());
			//System.out.print(FritzBoxTR064Manager.getInstance().isTR064Supported("fritz.box", "49000"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getFirmwareVersion("thilo.brandt", "Tb2743507","fritz.box", "49443", "https"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getDescription("thilo.brandt", "Tb2743507","fritz.box"));
			//System.out.print(FritzBoxTR064Manager.getInstance().getPhonePorts("thilo.brandt", "Tb2743507","fritz.box", "49443", "https"));
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

}
