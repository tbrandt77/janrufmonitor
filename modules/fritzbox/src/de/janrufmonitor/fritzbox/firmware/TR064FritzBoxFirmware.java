package de.janrufmonitor.fritzbox.firmware;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.fritzbox.FritzBoxTR064Manager;
import de.janrufmonitor.fritzbox.IPhonebookEntry;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.DoBlockException;
import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxInitializationException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxNotFoundException;
import de.janrufmonitor.fritzbox.firmware.exception.GetAddressbooksException;
import de.janrufmonitor.fritzbox.firmware.exception.GetBlockedListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.fritzbox.firmware.exception.InvalidSessionIDException;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.Base64Encoder;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;

public class TR064FritzBoxFirmware implements
		IFritzBoxFirmware {
	
	private class XMLMsnMapHandler extends DefaultHandler {
		private Map m;
		
		private String currentValue; 
		private String m_msn;
		private String m_name;
		
		public XMLMsnMapHandler() {
			m = new HashMap();
		}
		
		public void characters(char[] ch, int start, int length)
	      throws SAXException {
			currentValue = new String(ch, start, length);
		}
		
		public void startElement(String uri, String name, String qname, Attributes attributes)
		throws SAXException {
			if (qname.equalsIgnoreCase("item")) {
				this.m_name = null;
				this.m_msn = null;
			}
		}
		
		public void endElement(String uri, String name, String qname)
		throws SAXException {
			if (qname.equalsIgnoreCase("number")) {
				this.m_msn = this.currentValue;
			}
			
			if (qname.equalsIgnoreCase("name")) {
				this.m_name = (this.currentValue == null ? "" : this.currentValue);
			}
			
			if (qname.equalsIgnoreCase("item") && this.m_name!=null && this.m_msn!=null) {
				m.put(this.m_msn, this.m_name);
				this.m_name = null;
				this.m_msn = null;
			}
			this.currentValue = null;
		}
		
		public Map getMap() {
			return this.m;
		}
	}
		
	private class XMLSipMsnHandler extends DefaultHandler {
		private Map m;
		
		private String currentValue; 
		private String m_msn;
		private String m_index;
		
		public XMLSipMsnHandler() {
			m = new HashMap();
		}
		
		public void characters(char[] ch, int start, int length)
	      throws SAXException {
			currentValue = new String(ch, start, length);
		}
		
		public void startElement(String uri, String name, String qname, Attributes attributes)
		throws SAXException {
			if (qname.equalsIgnoreCase("item")) {
				this.m_index = null;
				this.m_msn = null;
			}
		}
		
		public void endElement(String uri, String name, String qname)
		throws SAXException {
			if (qname.equalsIgnoreCase("number")) {
				this.m_msn = this.currentValue;
			}
			
			if (qname.equalsIgnoreCase("index")) {
				this.m_index = this.currentValue;
			}
			
			if (qname.equalsIgnoreCase("item") && this.m_index!=null && this.m_msn!=null) {
				m.put(this.m_index, this.m_msn);
				this.m_index = null;
				this.m_msn = null;
			}

		}
		
		public Map getMap() {
			return this.m;
		}
	}
	
	private class XMLPeHandler extends DefaultHandler {
		private List contacts = new ArrayList();;
		private PhonebookEntry currentPe;
		
		private String currentValue; 
		private String[] currentNumber;
		private String m_ab;
		
		public XMLPeHandler(String ab) {
			this.m_ab = ab;
		}
		
		public void characters(char[] ch, int start, int length)
	      throws SAXException {
			currentValue = new String(ch, start, length);
		}
		
		public void startElement(String uri, String name, String qname, Attributes attributes)
		throws SAXException {
			if (qname.equalsIgnoreCase("contact")) {
				currentPe = new PhonebookEntry(); 
			}
			if (qname.equalsIgnoreCase("number")) {
				currentNumber = new String[2];
				currentNumber[0] = attributes.getValue("type");
				if (currentNumber[0].equalsIgnoreCase("home")) {
					currentNumber[0] = IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE;
				}
				if (currentNumber[0].equalsIgnoreCase("work")) {
					currentNumber[0] = IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE;
				}
				if (currentNumber[0].equalsIgnoreCase("mobile")) {
					currentNumber[0] = IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE;
				}
				if (currentNumber[0].equalsIgnoreCase("fax_work")) {
					currentNumber[0] = IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE;
				}
				if (currentNumber[0].equalsIgnoreCase("work_fax")) {
					currentNumber[0] = IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE;
				}
			}
		}
		
		public void endElement(String uri, String name, String qname)
		throws SAXException {
			if (qname.equalsIgnoreCase("realname") && currentPe!=null) {
				currentPe.setName(currentValue);
			}
			
			if (qname.equalsIgnoreCase("number") && currentNumber!=null && currentPe!=null) {
				currentNumber[1] = currentValue;
				currentPe.addNumber(currentNumber[1], currentNumber[0]);
				currentNumber = null;
			}
			
			if (qname.equalsIgnoreCase("contact") && currentPe!=null) {
				currentPe.setAddressbook(m_ab);
				contacts.add(currentPe);
				currentPe = null;
			}
			
			if (qname.equalsIgnoreCase("email") && currentPe!=null) {
				currentPe.setEmail(currentValue);
			}
			
			if (qname.equalsIgnoreCase("imageURL") && currentPe!=null) {
				String img = createBase64Image(currentValue);
				currentPe.setImageBase64(img);
			}

		}
		
		public List getList() {
			return contacts;
		}
	}
	
	public class PhonebookEntry implements IPhonebookEntry {
		
		String m_name;
		String m_ab;
		Map m_phones;
		String m_email;
		String m_image;
		
		public PhonebookEntry() {
			m_phones = new HashMap(3);
		}
		
		public void setName(String name) {
			this.m_name = name;
		}
		
		public void setAddressbook(String ab) {
			this.m_ab = ab;
		}
		
		public void setEmail(String e) {
			this.m_email = e;
		}
		
		public void addNumber(String n, String type) {
			m_phones.put(n, type);
		}
		
		public String getName() {
			return this.m_name;
		}
		
		public String getEmail() {
			return this.m_email;
		}
		
		public String getAddressbook() {
			return this.m_ab;
		}
		
		public Map getPhones() {
			return m_phones;
		}
		public String toString() {
			return this.m_name + ";" + this.m_phones;
		}

		public void setImageBase64(String b64) {
			this.m_image = b64;
		}

		public String getImageBase64() {
			return this.m_image;
		}
	}
	
	protected Logger m_logger;
	
	protected String m_server;
	protected String m_port;
	protected String m_password;
	protected String m_user; // new since Fritz!OS Version 05.50
	
	private Map m_msnSipMapping;
	private Map m_msnMap;
	private boolean m_useHttp;
	
	protected long m_loginUptime = -1L;
	protected boolean m_hasTR064Checkpassed;
	protected boolean m_isTR064;
	
	protected FirmwareData m_firmware;
	
	public TR064FritzBoxFirmware(String box_address, String box_port, String box_password, String box_user) {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_server = box_address;
		this.m_port = box_port;
		this.m_password = box_password;
		this.m_user = (box_user!=null && box_user.length()>0 ? box_user : "admin");
	}

	public TR064FritzBoxFirmware(String box_address, String box_port, String box_password) {
		this(box_address, box_port, box_password, null);
	}
	
	public void login() throws FritzBoxLoginException {
		if (!this.isInitialized()) throw new FritzBoxLoginException("Could not login to FritzBox: FritzBox firmware not initialized.");
	}
	
	public boolean isTR064Enabled() {
		if (this.m_hasTR064Checkpassed) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("TR064 check already done. TR064 is "+(this.m_isTR064 ? "enabled" : "disabled")+".");
			return this.m_isTR064;
		}
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("TR064 check not yet done.");
		
	    
	    if (PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_TR064_OFF).equalsIgnoreCase("true")) {
	    	if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("TR064 is switched off by user (application wise).");
	    	this.m_hasTR064Checkpassed = true;
	    	this.m_isTR064 = false;
	    	return this.m_isTR064;
	    }
	   
		try {
			this.m_hasTR064Checkpassed = true;
			this.m_isTR064 = FritzBoxTR064Manager.getInstance().isTR064Supported(this.m_server, FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port());
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("TR064 is "+(this.m_isTR064 ? "enabled" : "disabled")+".");
			return this.m_isTR064;
		} catch (IOException e) {
			if (this.m_logger.isLoggable(Level.SEVERE))
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		if (this.m_logger.isLoggable(Level.WARNING))
			this.m_logger.warning("FRITZ!Box "+this.m_server+" does not support TR064 or TR064 is disabled.");
		this.m_isTR064 = false;
		return this.m_isTR064;
	}

	public boolean isPasswordValid() throws FritzBoxInitializationException {
		try {			
			if (this.isTR064Enabled()) {
				this.m_useHttp = Boolean.parseBoolean(System.getProperty("jam.fritzbox.useHttp", "false"));	
				return FritzBoxTR064Manager.getInstance().isPasswordValid(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
			}
			if (this.m_logger.isLoggable(Level.WARNING))
				this.m_logger.warning("FRITZ!Box "+this.m_server+" does not support TR064 or TR064 is disabled.");
		} catch (IOException e) {
			if (this.m_logger.isLoggable(Level.SEVERE))
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new FritzBoxInitializationException(e.getMessage(), e);
		}
		throw new FritzBoxInitializationException("FRITZ!Box "+this.m_server+" does not support TR064 or TR064 is disabled.");
	}

	public void init() throws FritzBoxInitializationException,
			FritzBoxNotFoundException, InvalidSessionIDException {
		try {
			if (!this.isTR064Enabled()) 
				throw new FritzBoxInitializationException("FRITZ!Box "+this.m_server+" does not support TR064 or TR064 is disabled.");
			
			this.m_useHttp = Boolean.parseBoolean(System.getProperty("jam.fritzbox.useHttp", "false"));
			
			String version = FritzBoxTR064Manager.getInstance().getFirmwareVersion(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
			
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("FritzBox firmware version string: "+version);
			
			if (version!=null && version.length()>0) {
				StringTokenizer st = new StringTokenizer(version, ".");
				if (st.countTokens()==3) {
					this.m_firmware = new FirmwareData(st.nextToken(), st.nextToken(), st.nextToken());
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Initializing of FritzBox firmware succuessfully finished: "+this.m_firmware.toString());
					
					this.m_loginUptime = FritzBoxTR064Manager.getInstance().getUptime(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
					if (this.m_loginUptime==-1L) {
						throw new FritzBoxInitializationException("FritzBox did not provide uptime attribute: "+this.m_loginUptime);
					}
				} else {
					throw new FritzBoxInitializationException("FritzBox version string is invalid: "+version);
				}
			} else
				throw new InvalidSessionIDException("FritzBox could not receive firmware version. Invalid Login data.");
		} catch (IOException e) {
			if (this.m_logger.isLoggable(Level.SEVERE))
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			if (e.getMessage().indexOf("DH keypair")>0) {
				if (OSUtils.isMacOSX()) {
					System.setProperty("jam.fritzbox.useHttp", "true");
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("SSL over HTTPS not possible on this Mac. Using HTTP. Set jam.fritzbox.useHttp=true");
					this.init();
					return;
				}
				PropagationFactory.getInstance().fire(
					new Message(Message.ERROR,
						"fritzbox.firmware.hardware",
						"sslerror",
						new String[] {this.m_server, System.getProperty("java.runtime.version", "-")},
						new Exception("SSL Handshake failed for server: "+this.m_server),
						true)
					);
			}
			throw new FritzBoxInitializationException("FritzBox initializing failed: "+e.getMessage());
		}
	}

	public void destroy() {
		this.m_firmware = null;
		this.m_msnSipMapping = null;
		this.m_loginUptime = -1L;
		this.m_hasTR064Checkpassed = false;
		this.m_isTR064 = false;
	}

	public boolean isInitialized() {
		return this.m_firmware!=null;
	}

	public String getMSNFromSIP(String idx) throws IOException {
		if (!this.isInitialized()) return null;
		if (this.m_msnSipMapping==null) {
			String xml = FritzBoxTR064Manager.getInstance().getSIPResolution(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
			if (xml!=null) {
				this.m_msnSipMapping = this.parseXML(xml);
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("SIP -> MSN mapping table: "+this.m_msnSipMapping);
			}
		}
		if (this.m_msnSipMapping!=null && this.m_msnSipMapping.containsKey(idx)) {
			return (String) this.m_msnSipMapping.get(idx);
		}
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("No SIP to MSN mapping found for index # "+idx);
		return null;
	}
	
	public Map getMSNMap() throws IOException {
		if (!this.isInitialized()) return null;
		
		if (this.m_msnMap==null) {
			String xml = FritzBoxTR064Manager.getInstance().getSIPResolution(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
			if (xml!=null) {
				this.m_msnMap = this.parseMsnMapXML(xml);
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("MSN -> Description mapping table: "+this.m_msnMap);
				return this.m_msnMap;
			}
		}
		return null;
	}

	public List getCallList() throws GetCallListException, IOException {
		return this.getCallList(-1L);
	}

	public List getCallList(long lastSyncTimestamp) throws GetCallListException, IOException {
		if (!this.isInitialized()) throw new GetCallListException("Could not get call list from FritzBox: FritzBox firmware not initialized.");
		InputStream in = null;
		try {
			if (lastSyncTimestamp==-1L) {
				in = FritzBoxTR064Manager.getInstance().getCallList(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"), -1);
			} else {
				long now = System.currentTimeMillis();
				int days = (int) ((now - lastSyncTimestamp) / (1000 * 60 * 60 * 24)) + 1;
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Only retrieve call list for the last x days: "+days);
				in = FritzBoxTR064Manager.getInstance().getCallList(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"), (days > 0 ? days : 1));
			}
		} catch (IOException e) {
			throw new GetCallListException(e.getMessage());
		}
		if (in==null) return new ArrayList(0);
		
		List result = new ArrayList();
		InputStreamReader inr = new InputStreamReader(in, "iso-8859-1");
		BufferedReader bufReader = new BufferedReader(inr);
		
		String line = bufReader.readLine(); // drop header
		
		if (line.startsWith("sep=")) // new fw version
			bufReader.readLine(); // drop header of new fw
		
		while (bufReader.ready()) {
			line = bufReader.readLine();
			if (line!=null && line.trim().length()>0)
				result.add(line);
		}
		bufReader.close();
		in.close();
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Callist from FritzBox succuessfully fetched. List size: "+result.size());
		
		return result;
	}

	public List getCallerList() throws GetCallerListException, IOException {
		return this.getCallerList(0, "Telefonbuch");
	}

	public List getCallerList(int addressbookId, String addressbookName)
			throws GetCallerListException, IOException {
		if (!this.isInitialized()) throw new GetCallerListException("Could not get phone book from FritzBox: FritzBox firmware not initialized.");
		InputStream in = null;
		try {
			in = FritzBoxTR064Manager.getInstance().getPhonebook(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"), Integer.toString(addressbookId));
		} catch (IOException e) {
			throw new GetCallerListException(e.getMessage());
		}
		if (in==null) return new ArrayList(0);
		
		List result = new ArrayList();
		StringBuffer xml = new StringBuffer();
		String encoding = "utf-8";
		InputStreamReader inr = new InputStreamReader(in, encoding);
		BufferedReader bufReader = new BufferedReader(inr);
		
		String line = bufReader.readLine(); // drop header

		while (bufReader.ready()) {
			line = bufReader.readLine();
			xml.append(line+" ");
		}
		bufReader.close();
		in.close();
		
		result.addAll(parseXML(xml, addressbookName));
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Phonebook from FritzBox succuessfully fetched. List size: "+result.size());
		
		return result;
	}

	public Map getAddressbooks() throws GetAddressbooksException, IOException {
		if (!this.isInitialized()) throw new GetAddressbooksException("Could not get address book list from FritzBox: FritzBox firmware not initialized.");
		try {
			return FritzBoxTR064Manager.getInstance().getPhonebookList(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
		} catch (IOException e) {
			throw new GetAddressbooksException(e.getMessage());
		}
	}

	public String getAddressbookModificationHash(int addressbookId) throws GetAddressbooksException, IOException {
		if (!this.isInitialized()) throw new GetAddressbooksException("Could not get address book list from FritzBox: FritzBox firmware not initialized.");
		try {
			return FritzBoxTR064Manager.getInstance().getPhonebookHash(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"), Integer.toString(addressbookId));
		} catch (IOException e) {
			throw new GetAddressbooksException(e.getMessage());
		}
	}

	public void deleteCallList() throws DeleteCallListException, IOException {
		if (!this.isInitialized()) throw new DeleteCallListException("Could not delete call list from FritzBox: FritzBox firmware not initialized.");
		
		String u = "http://" + this.m_server + ":" + this.m_port + "/fon_num/foncalls_list.lua"; 
		String body = "usejournal=on&clear=&callstab=all&sid=" + FritzBoxTR064Manager.getInstance().getSID(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));

		doHttpCall(u, "POST", body, new String[][] { {"Content-Length", Integer.toString(body.length())} });
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Callist from FritzBox succuessfully deleted.");
	}

	public List getBlockedList() throws GetBlockedListException, IOException {
		if (!this.isInitialized()) throw new GetBlockedListException("Could not get blocked list from FritzBox: FritzBox firmware not initialized.");

		StringBuffer data = new StringBuffer();
		String u = "http://" + this.m_server +":" + this.m_port + "/fon_num/sperre.lua?sid="+FritzBoxTR064Manager.getInstance().getSID(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));

		try {
			data.append(
				doHttpCall(u, "GET", null, new String[][] {  })
			);
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.WARNING, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new GetBlockedListException("Could not get blocked list from FritzBox: "+e.getMessage());
		} 
		List blockedNumbers = new ArrayList();
		
		String[] s = data.toString().split("CallerID");
		Pattern p = Pattern.compile("] = \"([\\d]*)\",", Pattern.UNICODE_CASE);
		for (int i=0,j=s.length;i<j;i++) {
			Matcher m = p.matcher(s[i]);
			while (m.find()) {
				if (m.group(1).trim().length()>3) 
					blockedNumbers.add(m.group(1).trim());
			}
		}
		
		return blockedNumbers;
	}

	public void doBlock(String number) throws DoBlockException, IOException {
		if (!this.isInitialized()) throw new DoBlockException("Could not block number "+number+" on FritzBox: FritzBox firmware not initialized.");
		
		String u = "http://" + this.m_server + ":" + this.m_port + "/fon_num/sperre_edit.lua"; 
		String body = "mode_call=_in&rule_kind=rufnummer&rule_number="+number+"&current_rule=&current_mode=_new&backend_validation=false&apply=&sid="+ FritzBoxTR064Manager.getInstance().getSID(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
		
		doHttpCall(u, "POST", body, new String[][] { {"Content-Length", Integer.toString(body.length())} });
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Successfully added number "+number+" to FritzBox block list.");
	}

	public void doCall(String number, String extension) throws DoCallException,
			IOException {
		if (!this.isInitialized()) throw new DoCallException("Could not dial number on FritzBox: FritzBox firmware not initialized.");
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("dial number: "+number+", extension: "+extension);
		
		StringBuffer data = new StringBuffer();
		if (number.endsWith("#"))
			number = number.substring(0, number.length()-1);
		
		String u = "http://" + this.m_server +":" + this.m_port;
		
		if (this.m_firmware!=null && this.m_firmware.getMajor()>=6 && this.m_firmware.getMinor()>=30) {
			try {
				String body = "sid="+FritzBoxTR064Manager.getInstance().getSID(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"))+"&clicktodial=on&port="+extension+"&btn_apply=";
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Set up extension for dialing: "+body);
				data.append(
					doHttpCall(u + "/fon_num/dial_foncalls.lua", "POST", body, new String[][] { {"Content-Length", Integer.toString(body.length())} })
				);
	
				String dial_u = u + "/fon_num/fonbook_list.lua?sid="+FritzBoxTR064Manager.getInstance().getSID(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"))+"&dial="+StringUtils.urlEncode(number);
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Dial URL: "+dial_u);
				data.append(
					doHttpCall(dial_u, "GET", null, new String[][] {  })
				);
			} catch (UnsupportedEncodingException e) {
				this.m_logger.log(Level.WARNING, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				throw new DoCallException("Could not dial numer on FritzBox: "+e.getMessage());
			} 
		} else {
			try {
				String body = "&sid="+FritzBoxTR064Manager.getInstance().getSID(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"))+"&telcfg:settings/UseClickToDial=1&telcfg:settings/DialPort="+extension+"&telcfg:command/Dial="+number;
				data.append(
					doHttpCall(u + "/cgi-bin/webcm", "POST", body, new String[][] { {"Content-Length", Integer.toString(body.length())} })
				);
			} catch (UnsupportedEncodingException e) {
				this.m_logger.log(Level.WARNING, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				throw new DoCallException("Could not dial number on FritzBox: "+e.getMessage());
			} 
		}
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Data after call initialization: "+data.toString());
	}

	public long getFirmwareTimeout() {
		return -1;
	}

	public long getSkipBytes() {
		return 0;
	}
	
	public boolean isRestarted() {
		if (this.m_loginUptime==-1L) return true;
		
		long currentUptime = -1L;
		try {
			currentUptime = FritzBoxTR064Manager.getInstance().getUptime(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return (this.m_loginUptime > currentUptime);
	}
	
	public String toString() {
		if (this.m_firmware!=null) {
			StringBuffer s = new StringBuffer(64);
			try {
				String[] desc = FritzBoxTR064Manager.getInstance().getDescription(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
				for (int i=0;i<desc.length;i++) {
					s.append(desc[i]);
					if (i<desc.length-1)
						s.append(IJAMConst.CRLF);
				}
				return s.toString();
			} catch (IOException e) { }
		} 
		return "No FRITZ!Box firmware detected.";
	}
	
	private StringBuffer doHttpCall(String u, String method, String body, String[][] headers) throws IOException {
		return this.doHttpCall(u, method, body, headers, null, null, false);
	}
	
	private StringBuffer doHttpCall(String u, String method, String body, String[][] headers, boolean isBase64Encoded) throws IOException {
		return this.doHttpCall(u, method, body, headers, null, null, isBase64Encoded);
	}
	
	private StringBuffer doHttpCall(String u, String method, String body, String[][] headers, String user, String pw, boolean isBase64Encoded) throws IOException {
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
		if (user!=null && pw!=null) {
			connection.setRequestProperty  ("Authorization", "Basic " + Base64Encoder.encode(user+":"+pw));
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
		
		if (!isBase64Encoded) {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("HTTP response is plain/text.");
			BufferedReader reader = new BufferedReader(
			                          new InputStreamReader(connection.getInputStream()) );

			for ( String line; (line = reader.readLine()) != null; ) {
			  response.append(line); response.append(IJAMConst.CRLF);
			}
			reader.close();
		} else {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("HTTP response is base64 encoded.");
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Base64Encoder b64 = new Base64Encoder(bos);
			Stream.copy(new BufferedInputStream(connection.getInputStream()), b64);
			b64.flush();
			b64.close();
			response.append(new String(bos.toByteArray()));
		}
		
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("HTTP response...");
			this.m_logger.info(response.toString());
		}
		
		return response;
	}
	
	private List parseXML(StringBuffer xml, String ab) {
		try {
			XMLPeHandler handler = new XMLPeHandler(ab);
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			String encoding = "utf-8";
			ByteArrayInputStream in = new ByteArrayInputStream(xml.toString().getBytes(encoding));
			InputSource is = new InputSource(in);
			is.setEncoding(encoding);
			p.parse(is, handler);
			return handler.getList();
		} catch (SAXException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
	
	private Map parseMsnMapXML(String xml) {
		try {
			XMLMsnMapHandler handler = new XMLMsnMapHandler();
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			String encoding = "utf-8";
			ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes(encoding));
			InputSource is = new InputSource(in);
			is.setEncoding(encoding);
			p.parse(is, handler);
			return handler.getMap();
		} catch (SAXException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
	
	private Map parseXML(String xml) {
		try {
			XMLSipMsnHandler handler = new XMLSipMsnHandler();
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			String encoding = "utf-8";
			ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes(encoding));
			InputSource is = new InputSource(in);
			is.setEncoding(encoding);
			p.parse(is, handler);
			return handler.getMap();
		} catch (SAXException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	private String createBase64Image(String path) {
		if (path==null) return null;
		
//		// 2015/11/08: Check for inline URLs and URLEncode
//		if (path!=null && path.indexOf("path=http")>0) {
//			String url_enc_part = path.substring(path.indexOf("path=")+5, path.length());
//			if (this.m_logger.isLoggable(Level.INFO)) 
//				this.m_logger.info("URL to be encoded: "+path);
//			if (this.m_logger.isLoggable(Level.INFO)) 
//				this.m_logger.info("Encodeding part: "+url_enc_part);
//			try {
//				url_enc_part = URLEncoder.encode(url_enc_part, "utf-8");
//				path = path.substring(0, path.indexOf("path=")+5) + url_enc_part;
//				if (this.m_logger.isLoggable(Level.INFO)) 
//					this.m_logger.info("URL encoded: "+path);
//			} catch (UnsupportedEncodingException e) {
//				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
//			}
//		}
		try {
			String u = (this.m_useHttp ? "http://" : "https://")+this.m_server+":"+(this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server))+path+"&sid="+FritzBoxTR064Manager.getInstance().getSID(this.m_user, this.m_password, this.m_server, (this.m_useHttp ? FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064Port() : FritzBoxTR064Manager.getInstance().getDefaultFritzBoxTR064SecurePort(this.m_server)), (this.m_useHttp ? "http" : "https"));
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("requesting URL: "+u);
			return doHttpCall(u, "GET", null, new String[][] {  }, true).toString();
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return null;
	}





}
