package de.janrufmonitor.fritzbox.firmware;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.janrufmonitor.fritzbox.FritzBoxMD5Handler;
import de.janrufmonitor.fritzbox.firmware.exception.CreateSessionIDException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxInitializationException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxNotFoundException;
import de.janrufmonitor.fritzbox.firmware.exception.InvalidSessionIDException;

public class SessionIDFritzBoxFirmware extends AbstractFritzBoxFirmware {

	private final static String PATTERN_DETECT_CHALLENGE = "var challenge = \"([^<]*)\";";
	private final static String PATTERN_DETECT_SID = "name=\"sid\" value=\"([^<]*)\" id=";
	
	private String m_sid;
	private String m_response;
	
	public SessionIDFritzBoxFirmware(String box_address, String box_port,
			String box_password, boolean useHttps) {
		super(box_address, box_port, box_password, null, useHttps);
	}

	public void init() throws FritzBoxInitializationException, FritzBoxNotFoundException, InvalidSessionIDException {
		try {
			this.createSessionID();
		} catch (CreateSessionIDException e) {
			throw new FritzBoxInitializationException("FritzBox initialization failed: "+e.getMessage());
		}
		
		super.init();
	}

	
	public void destroy() {
		final String urlstr = getProtocol() + this.m_address +":" + this.m_port + "/cgi-bin/webcm";

		try {
			this.executeURL(urlstr, "&security%3Acommand%2Flogout=0&sid="+this.m_sid, false);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		super.destroy();
	}
	
	public long getFirmwareTimeout() {
		return ((10 * 60 * 1000)-10000); // set to 10 mins minus 10 sec (buffer)
	}

	String[] getAccessMethodPOSTData() {
		return new String[] {
				"getpage=../html/de/menus/menu2.html", 
				"getpage=../html/en/menus/menu2.html", 
				"getpage=../html/menus/menu2.html" };
	}

	String[] getDetectFirmwarePOSTData() {
		return new String[] {
				"&var%3Alang=de&var%3Amenu=home&var%3Apagename=home&sid=",
				"&var%3Alang=en&var%3Amenu=home&var%3Apagename=home&sid="
				};
	}

	IFritzBoxAuthenticator getDetectFirmwareURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}

	private void createSessionID() throws CreateSessionIDException, FritzBoxNotFoundException, InvalidSessionIDException {
		final String urlstr = getProtocol() + this.m_address +":" + this.m_port + "/cgi-bin/webcm";

		StringBuffer data = new StringBuffer(); 
		try {
			data.append(this.executeURL(
				urlstr,
				getAccessMethodPOSTData()[0] + getDetectFirmwarePOSTData()[0]
					+ URLEncoder.encode(this.m_password, "ISO-8859-1"), true).trim());
		} catch (UnsupportedEncodingException e) {
			this.m_logger.warning(e.getMessage());
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			if (e.getCause() instanceof ConnectException) {
				throw new FritzBoxNotFoundException(this.m_address, this.m_port);
			}
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
					getAccessMethodPOSTData()[0] + "&login%3Acommand%2Fresponse="
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

	String getListPOSTData() {
		return "&var%3Alang=$LANG&var%3Amenu=fon&var%3Apagename=foncalls&sid=";
	}

	IFritzBoxAuthenticator getListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}

	String getFetchCallListPOSTData() {
		return "getpage=../html/$LANG/$CSV_FILE&errorpage=..%2Fhtml%2F$LANG%2Fmenus%2Fmenu2.html&var%3Alang=$LANG&var%3Apagename=foncalls&var%3Aerrorpagename=foncalls&var%3Amenu=fon&var%3Apagemaster=&time%3Asettings%2Ftime=1136559837%2C-60&sid=";
	}

	IFritzBoxAuthenticator getFetchCallListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}
	

	String getFetchCallerListPOSTData() {
		return "getpage=../html/$LANG/home/ppFonbuch.html&sid=";
	}

	IFritzBoxAuthenticator getFetchCallerListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}
	

	String getFetchBlockedListPOSTData() {
		return "&getpage=..%2Fhtml%2F$LANG%2Fmenus%2Fmenu2.html&errorpage=..%2Fhtml%2F$LANG%2Fmenus%2Fmenu2.html&var%3Apagename=sperre&var%3Aerrorpagename=sperre&var%3Amenu=fon&var%3Apagemaster=&time%3Asettings%2Ftime=1258115459%2C-60&var%3AshowDialing=&var%3Atype=0&var%3AvonFoncalls=&var%3AcurrFonbookID=&var%3APhonebookEntryNew=&var%3APhonebookEntryXCount=&var%3APhonebookEntryNumber=&telcfg%3Asettings%2FUseJournal=1&var%3AWaehlhilfeVon=&sid=";
	}

	IFritzBoxAuthenticator getFetchBlockedListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}
	
	String getBlockPOSTData() {
		return "getpage=..%2Fhtml%2F$LANG%2Fmenus%2Fmenu2.html&errorpage=..%2Fhtml%2F$LANG%2Fmenus%2Fmenu2.html&var%3Apagename=sperre&var%3Aerrorpagename=sperre1&var%3Amenu=fon&var%3Apagemaster=&time%3Asettings%2Ftime=1258110017%2C-60&var%3Arul=xxx&var%3Amode=&telcfg%3Asettings%2FCallerIDActions$COUNT%2FCallerID=$NUMBER&telcfg%3Asettings%2FCallerIDActions$COUNT%2FAction=1&telcfg%3Asettings%2FCallerIDActions$COUNT%2FActive=1&sid=";
	}

	IFritzBoxAuthenticator getBlockURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}


	String getClearPOSTData() {
		return "&var%3Alang=$LANG&var%3Apagename=foncalls&var%3Amenu=fon&telcfg%3Asettings%2FClearJournal=&telcfg%3Asettings%2FUseJournal=1&sid=";
	}

	IFritzBoxAuthenticator getClearURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}

	String getCallPOSTData() {
		return "&sid=$PASSWORT&telcfg:settings/UseClickToDial=1&telcfg:settings/DialPort=$NEBENSTELLE&telcfg:command/Dial=$NUMMER";
	}

	IFritzBoxAuthenticator getCallURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return SessionIDFritzBoxFirmware.this.m_sid;
			}
			
		};
	}
	
	public long getSkipBytes() {
		return 4096L;
	}


}
