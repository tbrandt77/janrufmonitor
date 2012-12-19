package de.janrufmonitor.fritzbox.firmware;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.fritzbox.FritzBoxMD5Handler;
import de.janrufmonitor.fritzbox.firmware.exception.CreateSessionIDException;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.DoBlockException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxDetectFirmwareException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxInitializationException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.GetAddressbooksException;
import de.janrufmonitor.fritzbox.firmware.exception.GetBlockedListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.util.io.Stream;

public class FritzOSFirmware extends AbstractFritzBoxFirmware implements IFritzBoxFirmware {
	
	private class XMLPeHandler extends DefaultHandler {
		private List contacts = new ArrayList();;
		private AbstractFritzBoxFirmware.PhonebookEntry currentPe;
		
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
				currentPe = new AbstractFritzBoxFirmware.PhonebookEntry(); 
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
			}

		}
		
		public void endElement(String uri, String name, String qname)
		throws SAXException {
			if (qname.equalsIgnoreCase("realname") && currentPe!=null) {
				currentPe.setName(currentValue);
			}
			
			if (qname.equalsIgnoreCase("number") && currentNumber!=null) {
				currentNumber[1] = currentValue;
				currentPe.addNumber(currentNumber[1], currentNumber[0]);
				currentNumber = null;
			}
			
			if (qname.equalsIgnoreCase("contact")) {
				currentPe.setAddressbook(m_ab);
				contacts.add(currentPe);
				currentPe = null;
			}

		}
		
		public List getList() {
			return contacts;
		}
	}
	
	private final static String PATTERN_DETECT_CHALLENGE = "<Challenge>([^<]*)</Challenge>";
	private final static String PATTERN_DETECT_SID = "<SID>([^<]*)</SID>";
	private final static String PATTERN_DETECT_FIRMWARE = "[Firmware|Labor][-| ][V|v]ersion[^\\d]*(\\d\\d\\d*).(\\d\\d).(\\d\\d\\d*)([^<]*)"; 
	private final static String PATTERN_DETECT_LANGUAGE = "\\[\"language\"\\] = \"de\"";
	
	private final static String PATTERN_DETECT_BLOCKED_NUMBER = "] = \"([\\d]*)\",";
	private final static String PATTERN_DETECT_AB = ":([\\d]*)\">([^<]*)</label>";
	
	private String m_sid;
	private String m_response;
	
	public FritzOSFirmware(String box_address, String box_port, String box_password, String box_user) {
		super(box_address, box_port, box_password, box_user);
	}
	
	public void login() throws FritzBoxLoginException {
		if (!this.isInitialized()) throw new FritzBoxLoginException("Could not login to FritzBox: FritzBox firmware not initialized.");
	}

	public void init() throws FritzBoxInitializationException {
		try {
			this.createSessionID();
			this.m_firmware = this.detectFritzBoxFirmware();
		} catch (CreateSessionIDException e) {
			throw new FritzBoxInitializationException("FritzBox initialization failed: "+e.getMessage());
		} catch (FritzBoxDetectFirmwareException e) {
			throw new FritzBoxInitializationException("FritzBox initialization failed: "+e.getMessage());
		}
	}

	public void destroy() {
		final String urlstr = "http://" + this.m_address +":" + this.m_port + "/cgi-bin/webcm";

		try {
			this.executeURL(urlstr, "&security%3Acommand%2Flogout=0&sid="+this.m_sid, false);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		this.m_firmware = null;
	}

	public boolean isInitialized() {
		return this.m_firmware!=null;
	}

	public List getCallList() throws GetCallListException, IOException {
		if (!this.isInitialized()) throw new GetCallListException("Could not get call list from FritzBox: FritzBox firmware not initialized.");
		InputStream in = null;
		try {
			in = this.getCallListAsStream();
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
			result.add(line);
		}
		bufReader.close();
		in.close();
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Callist from FritzBox succuessfully fetched. List size: "+result.size());
		
		return result;
	}
	

	private InputStream getCallListAsStream() throws GetCallListException, IOException {
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving call list...");
		
		// The list should be updated now
		// Get the csv file for processing
		String urlstr = "http://" + this.m_address + ":" + this.m_port + "/fon_num/foncalls_list.lua?csv=&sid="+this.m_sid;

		URL url;
		URLConnection urlConn;

		try {
			this.m_logger.info("Calling FritzBox URL: "+urlstr);
			url = new URL(urlstr);
		} catch (MalformedURLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new GetCallListException("Invalid URL: " + urlstr);
		}

		urlConn = url.openConnection();
		urlConn.setDoInput(true);
		urlConn.setDoOutput(true);
		urlConn.setUseCaches(false);
		//urlConn.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8");
		// Sending postdata to the fritz box
		urlConn.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e2) {
			this.m_logger.log(Level.SEVERE, e2.getMessage(), e2);
		}
		try {
			// Get response data from the box
			ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
			this.m_logger.info("Fetching call list from FritzBox took "+(System.currentTimeMillis()-start)+"ms");
			Stream.copy(urlConn.getInputStream(), bos);

			ByteArrayInputStream bin = new ByteArrayInputStream(bos.toString("iso-8859-1").getBytes("iso-8859-1"));
			//this.m_logger.info(bos.toString());
			this.m_logger.info("Finished retrieving call list took "+(System.currentTimeMillis()-start)+"ms");
			urlConn.getInputStream().close();
			return bin;
		} catch (IOException e1) {
			this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			throw new GetCallListException(e1.getMessage());
		}
	}
	
	public List getCallerList() throws GetCallerListException, IOException {
		return this.getCallerList(0, "Telefonbuch");
	}

	public List getCallerList(int id, String name) throws GetCallerListException, IOException {
		if (!this.isInitialized()) throw new GetCallerListException("Could not get phone book from FritzBox: FritzBox firmware not initialized.");
		InputStream in = null;
		try {
			in = this.getCallerListAsStream(Integer.toString(id),name);
		} catch (IOException e) {
			throw new GetCallerListException(e.getMessage());
		}
		if (in==null) return new ArrayList(0);
		
		List result = new ArrayList();
		StringBuffer xml = new StringBuffer();
		InputStreamReader inr = new InputStreamReader(in, "UTF-8");
		BufferedReader bufReader = new BufferedReader(inr);
		
		String line = bufReader.readLine(); // drop header

		while (bufReader.ready()) {
			line = bufReader.readLine();
			xml.append(line+" ");
		}
		bufReader.close();
		in.close();
		
		result.addAll(parseXML(xml, name));
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Phonebook from FritzBox succuessfully fetched. List size: "+result.size());
		
		return result;
	}
	
	public Map getAddressbooks() throws GetAddressbooksException, IOException {
		if (!this.isInitialized()) throw new GetAddressbooksException("Could not get address book list from FritzBox: FritzBox firmware not initialized.");

		StringBuffer data = new StringBuffer();
		String urlstr = "http://" + this.m_address +":" + this.m_port + "/fon_num/fonbook_select.lua?sid="+this.m_sid;

		try {
			data.append(this.executeURL(
					urlstr,
					null, true).trim());
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.WARNING, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new GetAddressbooksException("Could not get address book list from FritzBox: "+e.getMessage());
		} 
		
		Map abs = new HashMap();
		
		String[] s = data.toString().split("uiBookid");
		Pattern p = Pattern.compile(PATTERN_DETECT_AB, Pattern.UNICODE_CASE);
		for (int i=0,j=s.length;i<j;i++) {
			Matcher m = p.matcher(s[i]);
			while (m.find() && m.groupCount()>1) {
				abs.put(Integer.parseInt(m.group(1)), m.group(2).trim());
			}
		}
		
		return abs;
		
	}
	
	private List parseXML(StringBuffer xml, String ab) {
		try {
			XMLPeHandler handler = new XMLPeHandler(ab);
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			ByteArrayInputStream in = new ByteArrayInputStream(xml.toString().getBytes("ISO-8859-1"));
			p.parse(in, handler);
			return handler.getList();
		} catch (SAXException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	private InputStream getCallerListAsStream(String pb_id, String pb_name)  throws GetCallerListException, IOException {
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving phone book...");
		
		// The list should be updated now
		// Get the csv file for processing
		String urlstr = "http://" + this.m_address + ":" + this.m_port + "/cgi-bin/firmwarecfg";

		URL url;
		URLConnection urlConn;
		DataOutputStream printout;

		try {
			this.m_logger.info("Calling FritzBox URL: "+urlstr);
			url = new URL(urlstr);
		} catch (MalformedURLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new GetCallerListException("Invalid URL: " + urlstr);
		}

		urlConn = url.openConnection();
		urlConn.setDoInput(true);
		urlConn.setDoOutput(true);
		urlConn.setUseCaches(false);
		// Sending postdata to the fritz box
		urlConn.setRequestProperty("Content-Type",
				"multipart/form-data; boundary=---------------------------7dc2fd11290438");
		printout = new DataOutputStream(urlConn.getOutputStream());
		StringBuffer sb = new StringBuffer();
		sb.append("-----------------------------7dc2fd11290438"+IJAMConst.CRLF);
		sb.append("Content-Disposition: form-data; name=\"sid\""+IJAMConst.CRLF);
		sb.append(IJAMConst.CRLF);
		sb.append(this.m_sid+IJAMConst.CRLF);
		sb.append("-----------------------------7dc2fd11290438"+IJAMConst.CRLF);
		sb.append("Content-Disposition: form-data; name=\"PhonebookId\""+IJAMConst.CRLF);
		sb.append(IJAMConst.CRLF);
		sb.append(pb_id+IJAMConst.CRLF);
		sb.append("-----------------------------7dc2fd11290438"+IJAMConst.CRLF);
		sb.append("Content-Disposition: form-data; name=\"PhonebookExportName\""+IJAMConst.CRLF);
		sb.append(IJAMConst.CRLF);
		sb.append(pb_name+IJAMConst.CRLF);
		sb.append("-----------------------------7dc2fd11290438"+IJAMConst.CRLF);
		sb.append("Content-Disposition: form-data; name=\"PhonebookExport\""+IJAMConst.CRLF);
		sb.append(IJAMConst.CRLF);
		sb.append(IJAMConst.CRLF);
		sb.append("-----------------------------7dc2fd11290438--"+IJAMConst.CRLF);
		printout.writeBytes(sb.toString());
		printout.flush();
		printout.close();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e2) {
			this.m_logger.log(Level.SEVERE, e2.getMessage(), e2);
		}
		try {
			// Get response data from the box
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			this.m_logger.info("Fetching call list from FritzBox took "+(System.currentTimeMillis()-start)+"ms");
			Stream.copy(urlConn.getInputStream(), bos);

			ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
			
			//this.m_logger.info(bos.toString());
			this.m_logger.info("Finished retrieving call list took "+(System.currentTimeMillis()-start)+"ms");
			urlConn.getInputStream().close();
			return bin;
		} catch (IOException e1) {
			this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			throw new GetCallerListException(e1.getMessage());
		}
	}

	public void deleteCallList() throws DeleteCallListException, IOException {
		if (!this.isInitialized()) throw new DeleteCallListException("Could not delete call list from FritzBox: FritzBox firmware not initialized.");
		
		String urlstr = "http://" + this.m_address + ":" + this.m_port + "/fon_num/foncalls_list.lua"; 
		String postdata = "usejournal=on&clear=&callstab=all&sid=" + this.m_sid;

		executeURL(urlstr, postdata, false);
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Callist from FritzBox succuessfully deleted.");
	}

	public List getBlockedList() throws GetBlockedListException, IOException {
		if (!this.isInitialized()) throw new GetBlockedListException("Could not get blocked list from FritzBox: FritzBox firmware not initialized.");

		StringBuffer data = new StringBuffer();
		String urlstr = "http://" + this.m_address +":" + this.m_port + "/fon_num/sperre.lua?sid="+this.m_sid;

		try {
			data.append(this.executeURL(
					urlstr,
					null, true).trim());
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.WARNING, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new GetBlockedListException("Could not get blocked list from FritzBox: "+e.getMessage());
		} 
		List blockedNumbers = new ArrayList();
		
		String[] s = data.toString().split("CallerID");
		Pattern p = Pattern.compile(PATTERN_DETECT_BLOCKED_NUMBER, Pattern.UNICODE_CASE);
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
			
		String urlstr = "http://" + this.m_address + ":" + this.m_port + "/fon_num/sperre_edit.lua"; 
		String postdata = ("mode_call=_in&rule_kind=rufnummer&rule_number=$NUMBER&current_rule=&current_mode=_new&backend_validation=false&apply=&sid=".replaceAll("\\$NUMBER", number) + this.m_sid);
		executeURL(urlstr, postdata, false);
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Successfully added numer "+number+" to FritzBox block list.");
	}

	public long getFirmwareTimeout() {
		return ((10 * 60 * 1000)-10000); // set to 10 mins minus 10 sec (buffer)
	}

	public long getSkipBytes() {
		return 0L;
	}

	protected String executeURL(String urlstr, String postdata, boolean retrieveData) throws IOException {
		URL url = null;
		URLConnection urlConn;
		DataOutputStream printout;
		StringBuffer data = new StringBuffer(); 

		try {
			url = new URL(urlstr);
		} catch (MalformedURLException e) {
			throw new IOException("URL invalid: " + urlstr); 
		}

		if (url != null) {
			urlConn = url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			// Sending postdata
			if (postdata != null) {
				urlConn.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				printout = new DataOutputStream(urlConn.getOutputStream());
				printout.writeBytes(postdata);
				printout.flush();
				printout.close();
			}
			try {
				// Get response data
				BufferedReader d = new BufferedReader(new InputStreamReader(urlConn
						.getInputStream()));
				// 2009/06/07: to be optimized for HTML parsing
				d.skip(getSkipBytes());
				String str;
				while (null != ((str = HTMLUtil.stripEntities(d.readLine())))) {
					if (retrieveData){
						data.append(str);
						data.append(IJAMConst.CRLF);
					}						
				}
				d.close();
			} catch (IOException ex) {
				throw new IOException("Network problem occured", ex); //$NON-NLS-1$
			}
		}
		if (this.m_logger.isLoggable(Level.FINE)) {
			this.m_logger.fine("Data received from FritzBox:");
			this.m_logger.fine(data.toString());
		}
		return data.toString();
	}
	
	private FirmwareData detectFritzBoxFirmware() throws FritzBoxDetectFirmwareException {
		StringBuffer data = new StringBuffer();
		String urlstr = "http://" + this.m_address +":" + this.m_port + "/home/pp_fbos.lua?sid="+this.m_sid;
		boolean detected = false;
		
		try {
			data.append(this.executeURL(
					urlstr,
					null, true).trim());
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.WARNING, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new FritzBoxDetectFirmwareException("Could not detect fritzbox firmware: "+e.getMessage());
		} 
	
		Pattern p = Pattern.compile(PATTERN_DETECT_LANGUAGE);
		Matcher m = p.matcher(data);
		if (m.find()) {
			this.m_language = "de";
			detected = true;
		}
		
		if (!detected ) throw new FritzBoxDetectFirmwareException("Unable to detect FritzBox firmware.");
	
		this.m_logger.info("Using firmware detection pattern: "+PATTERN_DETECT_FIRMWARE);
		p = Pattern.compile(PATTERN_DETECT_FIRMWARE);
		m = p.matcher(data);
		if (m.find()) {
			return new FirmwareData(
					m.group(1), 
					m.group(2), 
					m.group(3),
					m.group(4).trim()
			);
		} else {
			throw new FritzBoxDetectFirmwareException(
				"Could not detect FRITZ!Box firmware version."); 
		}
	
	}

	private void createSessionID() throws CreateSessionIDException {
		final String urlstr = "http://" + this.m_address +":" + this.m_port + "/login_sid.lua";

		StringBuffer data = new StringBuffer(); 
		try {
			data.append(this.executeURL(
				urlstr, null, true).trim());
		} catch (UnsupportedEncodingException e) {
			this.m_logger.warning(e.getMessage());
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new CreateSessionIDException("Could not get a valid challenge code from the FritzBox.");
		} 
				
		String challenge = find(Pattern.compile(PATTERN_DETECT_CHALLENGE, Pattern.UNICODE_CASE), data);
		if (challenge!=null) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Detected FritzBox challenge code: "+challenge);
			
			this.m_response = FritzBoxMD5Handler.getResponse(challenge, this.m_password);
			
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Calculated FritzBox response code: "+this.m_response);
			
			data = new StringBuffer(); 
			try {
				data.append(this.executeURL(
					urlstr,
					"username="+((this.m_user!=null && this.m_user.length()>0) ? URLEncoder.encode(this.m_user, "ISO-8859-1") : "")+"&response="
						+ URLEncoder.encode(this.m_response, "ISO-8859-1"), true).trim());
			} catch (UnsupportedEncodingException e) {
				this.m_logger.warning(e.getMessage());
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				throw new CreateSessionIDException("Could not get a valid Session ID from the FritzBox.");
			} 
			
			String sid = find(Pattern.compile(PATTERN_DETECT_SID, Pattern.UNICODE_CASE), data);
			if (sid!=null) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Detected FritzBox SID: "+sid);
				if (sid.equalsIgnoreCase("0000000000000000")) {
					throw new CreateSessionIDException("Session ID is 0000000000000000.");
				}
				this.m_sid = sid;
			} else {
				throw new CreateSessionIDException("Could not get session ID from FritzBox.");		
			}
		} else {
			throw new CreateSessionIDException("Could not generate challenge code for FritzBox password.");
		}
	}

	private String find(Pattern p, StringBuffer c){
		Matcher m = p.matcher(c);
		if (m.find() && m.groupCount()==1) {
			return m.group(1).trim();
		}
		return null;		
	}
	
	public String toString() {
		if (this.m_firmware!=null) {
			StringBuffer s = new StringBuffer(64);
			s.append(this.m_firmware.getFritzBoxName());
			s.append(IJAMConst.CRLF);
			s.append(this.m_firmware.toString());
			s.append(IJAMConst.CRLF);
			s.append(this.m_language);
			return s.toString();
		} 
		return "No Fritz!Box firmware detected.";
	}

	@Override
	IFritzBoxAuthenticator getDetectFirmwareURLAuthenticator() {
		return null;
	}

	@Override
	String[] getDetectFirmwarePOSTData() {
		return null;
	}

	@Override
	String[] getAccessMethodPOSTData() {
		return new String[] {
				"", 
				""};
	}

	@Override
	IFritzBoxAuthenticator getListURLAuthenticator() {
		return null;
	}

	@Override
	String getListPOSTData() {
		return null;
	}

	@Override
	IFritzBoxAuthenticator getFetchCallListURLAuthenticator() {
		return null;
	}

	@Override
	String getFetchCallListPOSTData() {
		return null;
	}

	@Override
	IFritzBoxAuthenticator getFetchCallerListURLAuthenticator() {
		return null;
	}

	@Override
	String getFetchCallerListPOSTData() {
		return null;
	}

	@Override
	IFritzBoxAuthenticator getFetchBlockedListURLAuthenticator() {
		return null;
	}

	@Override
	String getFetchBlockedListPOSTData() {
		return null;
	}

	@Override
	IFritzBoxAuthenticator getClearURLAuthenticator() {
		return null;
	}

	@Override
	String getClearPOSTData() {
		return null;
	}

	String getCallPOSTData() {
		return "&sid=$PASSWORT&telcfg:settings/UseClickToDial=1&telcfg:settings/DialPort=$NEBENSTELLE&telcfg:command/Dial=$NUMMER";
	}

	IFritzBoxAuthenticator getCallURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return FritzOSFirmware.this.m_sid;
			}
			
		};
	}

	@Override
	IFritzBoxAuthenticator getBlockURLAuthenticator() {
		return null;
	}

	@Override
	String getBlockPOSTData() {
		return null;
	}


}
