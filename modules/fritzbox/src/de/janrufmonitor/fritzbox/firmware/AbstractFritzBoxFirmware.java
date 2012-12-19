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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.DoBlockException;
import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxDetectFirmwareException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxInitializationException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.GetAddressbooksException;
import de.janrufmonitor.fritzbox.firmware.exception.GetBlockedListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.util.io.Stream;

public abstract class AbstractFritzBoxFirmware implements IFritzBoxFirmware {

	public class PhonebookEntry {
		
		String m_name;
		String m_ab;
		Map m_phones;
		
		public PhonebookEntry() {
			m_phones = new HashMap(3);
		}
		
		public void setName(String name) {
			this.m_name = name;
		}
		
		public void setAddressbook(String ab) {
			this.m_ab = ab;
		}
		
		public void addNumber(String n, String type) {
			m_phones.put(n, type);
		}
		
		public String getName() {
			return this.m_name;
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
	}
	
	private final static String PATTERN_DETECT_LANGUAGE_DE = "Telefonie";
	private final static String PATTERN_DETECT_LANGUAGE_EN = "Telephony";
	private final static String PATTERN_DETECT_FIRMWARE =   "[Firmware|Labor][-| ][V|v]ersion[^\\d]*(\\d\\d).(\\d\\d).(\\d\\d\\d*)([^<]*)"; 
	private final static String PATTERN_DETECT_FIRMWARE_3 = "[Firmware|Labor][-| ][V|v]ersion[^\\d]*(\\d\\d\\d).(\\d\\d).(\\d\\d\\d*)([^<]*)"; 

	private final static String PATTERN_DETECT_BLOCKED_LIST = "TrIn\\(\"[\\d]+\", \"([\\d]+)\"";
	private final static String PATTERN_DETECT_CALLERLIST_NAME = "TrFonName\\(\"[\\d]*\", \"([^\\\"]*)\"";
	private final static String PATTERN_DETECT_CALLERLIST_HOME = "TrFonNr\\(\"home\", \"([\\d]*)\"";
	private final static String PATTERN_DETECT_CALLERLIST_WORK = "TrFonNr\\(\"work\", \"([\\d]*)\"";
	private final static String PATTERN_DETECT_CALLERLIST_MOBILE = "TrFonNr\\(\"mobile\", \"([\\d]*)\"";
	
	private final static String CSV_FILE_EN = "FRITZ!Box_Calllist.csv";
	private final static String CSV_FILE_DE = "FRITZ!Box_Anrufliste.csv";
	
	
	protected Logger m_logger;
	
	protected String m_address;
	protected String m_port;
	protected String m_password;
	protected String m_language;
	protected String m_user; // new since Fritz!OS Version 05.50
	
	protected FirmwareData m_firmware;
	
	public AbstractFritzBoxFirmware(String box_address, String box_port, String box_password, String box_user) {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_address = box_address;
		this.m_port = box_port;
		this.m_password = box_password;
		this.m_language = "de"; // default 
	}

	public AbstractFritzBoxFirmware(String box_address, String box_port, String box_password) {
		this(box_address, box_port, box_password, null);
	}

	public void init() throws FritzBoxInitializationException {
		try {
			this.m_firmware = this.detectFritzBoxFirmware();
			
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Initializing of FritzBox firmware succuessfully finished: "+this.m_firmware.toString());
		} catch (FritzBoxDetectFirmwareException e) {
			throw new FritzBoxInitializationException("FritzBox initializing failed: "+e.getMessage());
		}
	}
	
	public boolean isInitialized() {
		return this.m_firmware!=null;
	}
	
	public void destroy() {
		this.m_firmware = null;
	}
	
	public void login() throws FritzBoxLoginException {
		if (!this.isInitialized()) throw new FritzBoxLoginException("Could not login to FritzBox: FritzBox firmware not initialized.");
	}
	
	public List getBlockedList() throws IOException, GetBlockedListException {
		if (!this.isInitialized()) throw new GetBlockedListException("Could not get blocked list from FritzBox: FritzBox firmware not initialized.");
		InputStream in = null;
		try {
			in = this.getBlockedListAsStream();
		} catch (IOException e) {
			throw new GetBlockedListException(e.getMessage());
		} catch (DoBlockException e) {
			throw new GetBlockedListException(e.getMessage());
		}
		if (in==null) return new ArrayList(0);

		List result = new ArrayList();
		StringBuffer parseBuffer = new StringBuffer();
		InputStreamReader inr = new InputStreamReader(in);
		BufferedReader bufReader = new BufferedReader(inr);
		
		String line = bufReader.readLine(); // drop header
		// fasten the processing
		bufReader.skip(4096);
		
		while (bufReader.ready()) {
			line = bufReader.readLine();
			parseBuffer.append(line);
			parseBuffer.append(IJAMConst.CRLF);
		}
		bufReader.close();
		in.close();
		
		Pattern p = Pattern.compile(PATTERN_DETECT_BLOCKED_LIST, Pattern.UNICODE_CASE);
		Matcher m = p.matcher(parseBuffer);
		
		while (m.find() && m.groupCount()==1) {
			result.add(m.group(1).trim());
		}

		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Blocked list from FritzBox successfully fetched. List size: "+result.size());
		
		return result;
	}
	
	public List getCallerList() throws GetCallerListException, IOException {
		if (!this.isInitialized()) throw new GetCallerListException("Could not get phone book from FritzBox: FritzBox firmware not initialized.");
		InputStream in = null;
		try {
			in = this.getCallerListAsStream();
		} catch (IOException e) {
			throw new GetCallerListException(e.getMessage());
		}
		if (in==null) return new ArrayList(0);
		
		boolean started = false;
		
		List result = new ArrayList();
		StringBuffer parseBuffer = new StringBuffer();
		InputStreamReader inr = new InputStreamReader(in, "UTF-8");
		BufferedReader bufReader = new BufferedReader(inr);
		
		String line = bufReader.readLine(); // drop header
		// fasten the processing
		bufReader.skip(4096);
		
		while (bufReader.ready()) {
			line = bufReader.readLine();
			if (line.indexOf(">TrFonName")>=0) started = true;
			if (started) {
				parseBuffer.append(line);
				parseBuffer.append(IJAMConst.CRLF);
			}
		}
		bufReader.close();
		in.close();
		
		result.addAll(parseHtml(parseBuffer));
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Phonebook from FritzBox succuessfully fetched. List size: "+result.size());
		
		return result;
	}

	public List getCallList() throws GetCallListException, IOException{
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
	
	public List getCallerList(int addressbookId, String addressbookName)
			throws GetCallerListException, IOException {
		return this.getCallerList();
	}

	public Map getAddressbooks() throws GetAddressbooksException, IOException {
		return Collections.EMPTY_MAP;
	}

	public void deleteCallList() throws DeleteCallListException, IOException {
		if (!this.isInitialized()) throw new DeleteCallListException("Could not delete call list from FritzBox: FritzBox firmware not initialized.");
	
		String urlstr = "http://" + this.m_address + ":" + this.m_port + getBaseURL(); 
		String postdata = (this.m_language.equalsIgnoreCase("en") ? getAccessMethodPOSTData()[1] : getAccessMethodPOSTData()[0])
				+ this.getClearPOSTData().replaceAll("\\$LANG", this.m_language) + this.getClearURLAuthenticator().getAuthenticationToken();

		executeURL(urlstr, postdata, false);
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Callist from FritzBox succuessfully deleted.");
	}
	
	public void doBlock(String number) throws DoBlockException, IOException {
		if (!this.isInitialized()) throw new DoBlockException("Could not block number "+number+" on FritzBox: FritzBox firmware not initialized.");
	
		if (this.getBlockPOSTData().trim().length()>0) {
			int count = this.getBlockCount();
						
			String urlstr = "http://" + this.m_address + ":" + this.m_port + getBaseURL(); 
			String postdata = (this.m_language.equalsIgnoreCase("en") ? getAccessMethodPOSTData()[1] : getAccessMethodPOSTData()[0])
					+ this.getBlockPOSTData().replaceAll("\\$LANG", this.m_language).replaceAll("\\$NUMBER", number).replaceAll("\\$COUNT", Integer.toString(count)) + this.getBlockURLAuthenticator().getAuthenticationToken();
	
			executeURL(urlstr, postdata, false);
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Successfully added numer "+number+" to FritzBox block list.");
		} else {
			if (this.m_logger.isLoggable(Level.WARNING))
				this.m_logger.warning("Block list is not supported by this FritzBox firmware.");
		}
	}
	
	public void doCall(String number, String extension) throws DoCallException, IOException {
		try {
			login();
			number = number.replaceAll("\\+", "00");

			String portStr = "";
			if (extension.equals("Fon 1")) {
				portStr = "1";
			} else if (extension.equals("Fon 2")) {
				portStr = "2";
			} else if (extension.equals("Fon 3")) {
				portStr = "3";
			} else if (extension.equals("analog_telephones_all")) {
				portStr = "9";                
			} else if (extension.equals("ISDN Alle")) {
				portStr = "50";
			} else if (extension.equals("ISDN 1")) {
				portStr = "51";
			} else if (extension.equals("ISDN 2")) {
				portStr = "52";
			} else if (extension.equals("ISDN 3")) {
				portStr = "53";
			} else if (extension.equals("ISDN 4")) {
				portStr = "54";
			} else if (extension.equals("ISDN 5")) {
				portStr = "55";
			} else if (extension.equals("ISDN 6")) {
				portStr = "56";
			} else if (extension.equals("ISDN 7")) {
				portStr = "57";
			} else if (extension.equals("ISDN 8")) {
				portStr = "58";
			} else if (extension.equals("ISDN 9")) {
				portStr = "59";
			}else {
				portStr = extension;
			}
            String postdata = getCallPOSTData().replaceAll("\\$PASSWORT",
                    URLEncoder.encode(getCallURLAuthenticator().getAuthenticationToken(), "ISO-8859-1"));
            
            
			postdata = postdata.replaceAll("\\$NUMMER", number);
			postdata = postdata.replaceAll("\\$NEBENSTELLE", portStr);

			postdata = (this.m_language.equalsIgnoreCase("en") ? getAccessMethodPOSTData()[1] : getAccessMethodPOSTData()[0]) + postdata;

			String urlstr = "http://"
					+ this.m_address + ":" + this.m_port
					+ getBaseURL();
			this.executeURL(urlstr, postdata, false);
			
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Call with FritzBox succuessfully triggered.");
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (FritzBoxLoginException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new DoCallException("Dialing of number "+number+" failed: "+e.getMessage());
		} 
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
	

	private List parseHtml(StringBuffer parseBuffer) {
		String content = parseBuffer.toString();
		
		if (content.indexOf("TrFon2")<0) return new ArrayList(0);
		
		String[] st = content.split("TrFon2");
		List result = new ArrayList(st.length);
		StringBuffer aToken = null;
		String group = null;
		PhonebookEntry pe = null;
		for (int i=0,j=st.length;i<j;i++){
			pe = new PhonebookEntry();
			aToken = new StringBuffer(st[i]);
			group = this.find(Pattern.compile(PATTERN_DETECT_CALLERLIST_NAME, Pattern.UNICODE_CASE), aToken);
			if (group!=null) {
				pe.setName(group);
			}
			group = this.find(Pattern.compile(PATTERN_DETECT_CALLERLIST_HOME, Pattern.UNICODE_CASE), aToken);
			if (group!=null && group.length()>0) {
				pe.addNumber(group, IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
			}
			group = this.find(Pattern.compile(PATTERN_DETECT_CALLERLIST_WORK, Pattern.UNICODE_CASE), aToken);
			if (group!=null && group.length()>0) {
				pe.addNumber(group, IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
			}
			group = this.find(Pattern.compile(PATTERN_DETECT_CALLERLIST_MOBILE, Pattern.UNICODE_CASE), aToken);
			if (group!=null && group.length()>0) {
				pe.addNumber(group, IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE);
			}			
			
			if (pe.getName()!=null && pe.getName().trim().length()>0)
				result.add(pe);
		}		
		return result;
	}
	
	private String find(Pattern p, StringBuffer c){
		Matcher m = p.matcher(c);
		if (m.find() && m.groupCount()==1) {
			return m.group(1).trim();
		}
		return null;		
	}
	
	private void refreshCallList() throws IOException, GetCallListException {
		if (!isInitialized()) throw new GetCallListException("Cannot get call list from FritzBox. Firmware not initialized.");

		URL url;
		URLConnection urlConn;
		DataOutputStream printout;

		// Attempting to fetch the html version of the call list
		String postdata = (this.m_language.equalsIgnoreCase("en") ? getAccessMethodPOSTData()[1] : getAccessMethodPOSTData()[0])
				+ getListPOSTData().replaceAll("\\$LANG", this.m_language)
				+ URLEncoder.encode(getListURLAuthenticator().getAuthenticationToken(), "ISO-8859-1");

		String urlstr = "http://" + this.m_address +":" + this.m_port + getBaseURL();

		try {
			this.m_logger.info("Calling FritzBox URL: "+urlstr);
			url = new URL(urlstr);
		} catch (MalformedURLException e) {
			throw new GetCallListException("Invalid URL: " + urlstr);
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
				// Get response data from the box
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(urlConn.getInputStream()));

				// read out the response data!
				while (reader.skip(Short.MAX_VALUE) > 0) {
					// kind of stupid, but it has to be
					// If you don't read the list, you may not get an
					// Updated list from the box
				}

				// close the streams
				reader.close();
				urlConn.getInputStream().close();

			} catch (IOException e1) {
				throw new IOException("Network unavailable");
			}
		}
	}
	
	
	private int getBlockCount() throws DoBlockException, IOException {
		if (!this.isInitialized()) throw new DoBlockException("Could get blocked caller list from FritzBox: FritzBox firmware not initialized.");
		InputStream in = null;
		try {
			in = this.getBlockedListAsStream();
		} catch (IOException e) {
			throw new DoBlockException(e.getMessage());
		}
		if (in==null) throw new DoBlockException("Could get blocked caller list from FritzBox: Could not read count from page.");

		int c = 0;
		StringBuffer parseBuffer = new StringBuffer();
		InputStreamReader inr = new InputStreamReader(in);
		BufferedReader bufReader = new BufferedReader(inr);
		
		String line = bufReader.readLine(); // drop header
		// fasten the processing
		bufReader.skip(4096);
		
		while (bufReader.ready()) {
			line = bufReader.readLine();
			parseBuffer.append(line);
			parseBuffer.append(IJAMConst.CRLF);
		}
		bufReader.close();
		in.close();
		
		Pattern p = Pattern.compile("(telcfg:settings/CallerIDActions\\d+)");
		Matcher m = p.matcher(parseBuffer);
		
		while (m.find() && m.groupCount()==1) {
			c++;
		}

		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Found "+c+ " blocked caller numbers in FritzBox.");

		return c;
	}
	
	private InputStream getBlockedListAsStream() throws DoBlockException, IOException {
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving blocked list...");
		
		
		// The list should be updated now
		// Get the csv file for processing
		String urlstr = "http://" + this.m_address + ":" + this.m_port + getBaseURL();

		URL url;
		URLConnection urlConn;
		DataOutputStream printout;

		try {
			this.m_logger.info("Calling FritzBox URL: "+urlstr);
			url = new URL(urlstr);
		} catch (MalformedURLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new DoBlockException("Invalid URL: " + urlstr);
		}

		// If the url is valid load the data
		if (url != null && getFetchBlockedListPOSTData().trim().length()>0) {

			urlConn = url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			// Sending postdata to the fritz box
			urlConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			printout = new DataOutputStream(urlConn.getOutputStream());
			printout.writeBytes(getFetchBlockedListPOSTData().replaceAll(
					"\\$LANG", this.m_language) + getFetchBlockedListURLAuthenticator().getAuthenticationToken());
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

				this.m_logger.info("Fetching blocked list from FritzBox took "+(System.currentTimeMillis()-start)+"ms");
				Stream.copy(urlConn.getInputStream(), bos);

				ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
				//this.m_logger.info(bos.toString());
				this.m_logger.info("Finished retrieving blocked list took "+(System.currentTimeMillis()-start)+"ms");
				urlConn.getInputStream().close();
				return bin;
			} catch (IOException e1) {
				this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
				throw new DoBlockException(e1.getMessage());
			}
		}
		return null;
	}

	private InputStream getCallerListAsStream()  throws GetCallerListException, IOException {
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving phone book...");
		
		// The list should be updated now
		// Get the csv file for processing
		String urlstr = "http://" + this.m_address + ":" + this.m_port + getBaseURL();

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

		// If the url is valid load the data
		if (url != null && getFetchCallerListPOSTData().trim().length()>0) {

			urlConn = url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			// Sending postdata to the fritz box
			urlConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			printout = new DataOutputStream(urlConn.getOutputStream());
			printout.writeBytes(getFetchCallerListPOSTData().replaceAll(
					"\\$LANG", this.m_language) + getFetchCallerListURLAuthenticator().getAuthenticationToken());
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
		return null;
	}

	
	private InputStream getCallListAsStream() throws GetCallListException, IOException {
		long start = System.currentTimeMillis();
		this.m_logger.info("Starting retrieving call list...");
		
		this.refreshCallList();
		
		this.m_logger.info("Update list call took "+(System.currentTimeMillis()-start)+"ms");
		
		// The list should be updated now
		// Get the csv file for processing
		String urlstr = "http://" + this.m_address + ":" + this.m_port + getBaseURL();

		URL url;
		URLConnection urlConn;
		DataOutputStream printout;

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
		printout = new DataOutputStream(urlConn.getOutputStream());
		printout.writeBytes(getFetchCallListPOSTData().replaceAll(
				"\\$LANG", this.m_language).replaceAll(
				"\\$CSV_FILE", (this.m_language.equalsIgnoreCase("en") ? CSV_FILE_EN : CSV_FILE_DE))+ getFetchCallListURLAuthenticator().getAuthenticationToken());
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
	
	private FirmwareData detectFritzBoxFirmware() throws FritzBoxDetectFirmwareException {
		StringBuffer data = new StringBuffer();
		String urlstr = "http://" + this.m_address +":" + this.m_port + getBaseURL();
		boolean detected = false;
		
		String[] access_method_postdata = this.getAccessMethodPOSTData();
		String[] detect_firmware_postdata = this.getDetectFirmwarePOSTData();
		
		for (int i=0; i<(access_method_postdata).length; i++) {
			for (int j=0; j<(detect_firmware_postdata).length; j++) {
				data = new StringBuffer();
				try {
					data.append(this.executeURL(
							urlstr,
							access_method_postdata[i] + detect_firmware_postdata[j]
								+ URLEncoder.encode(getDetectFirmwareURLAuthenticator().getAuthenticationToken(), "ISO-8859-1"), true).trim());
				} catch (UnsupportedEncodingException e) {
					this.m_logger.log(Level.WARNING, e.getMessage(), e);
				} catch (IOException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					throw new FritzBoxDetectFirmwareException("Could not detect fritzbox firmware: "+e.getMessage());
				} 
			
				Pattern p = Pattern.compile(getLanguagePattern());
				Matcher m = p.matcher(data);
				if (m.find()) {
					this.m_language = "de";
					detected = true;
					break;
				}
				
				if (!detected)
				{
					p = Pattern.compile(PATTERN_DETECT_LANGUAGE_EN);
					m = p.matcher(data);
					if (m.find()) {
						this.m_language = "en";
						detected = true;
						break;
					}
				}
			}
			if ( detected ) break;
		}
		
		if (!detected ) throw new FritzBoxDetectFirmwareException("Unable to detect FritzBox firmware.");
	
		this.m_logger.info("Using firmware detection pattern: "+PATTERN_DETECT_FIRMWARE_3);
		Pattern p = Pattern.compile(PATTERN_DETECT_FIRMWARE_3);
		Matcher m = p.matcher(data);
		if (m.find()) {
			return new FirmwareData(
					m.group(1), 
					m.group(2), 
					m.group(3),
					m.group(4).trim()
			);
		} else {
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
	}
	
	abstract IFritzBoxAuthenticator getDetectFirmwareURLAuthenticator();
	abstract String[] getDetectFirmwarePOSTData();
	abstract String[] getAccessMethodPOSTData();
	
	abstract IFritzBoxAuthenticator getListURLAuthenticator();
	abstract String getListPOSTData();
	
	abstract IFritzBoxAuthenticator getFetchCallListURLAuthenticator();
	abstract String getFetchCallListPOSTData();
	
	abstract IFritzBoxAuthenticator getFetchCallerListURLAuthenticator();
	abstract String getFetchCallerListPOSTData();
	
	abstract IFritzBoxAuthenticator getFetchBlockedListURLAuthenticator();
	abstract String getFetchBlockedListPOSTData();
	
	abstract IFritzBoxAuthenticator getClearURLAuthenticator();
	abstract String getClearPOSTData();
	
	abstract IFritzBoxAuthenticator getBlockURLAuthenticator();
	abstract String getBlockPOSTData();
	
	abstract IFritzBoxAuthenticator getCallURLAuthenticator();
	abstract String getCallPOSTData();
	
	String getBaseURL() {
		return "/cgi-bin/webcm"; 
	}
	
	String getLanguagePattern() {
		return PATTERN_DETECT_LANGUAGE_DE;
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

	public long getFirmwareTimeout() {
		return -1; // infinite
	}

	public long getSkipBytes() {
		return (Short.MAX_VALUE/2);
	}
	
}
