package de.janrufmonitor.fritzbox.firmware;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;

import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;

public class PasswordFritzBoxFirmware extends AbstractFritzBoxFirmware {
	
	private static String POSTDATA_LOGIN = "&login:command/password=$PASSWORT"; 
	
	public PasswordFritzBoxFirmware(String box_address, String box_port,
			String box_password) {
		super(box_address, box_port, box_password);
	}

	String[] getAccessMethodPOSTData() {
		return new String[] {
				"getpage=../html/de/menus/menu2.html", 
				"getpage=../html/en/menus/menu2.html", 
				"getpage=../html/menus/menu2.html" };
	}

	String[] getDetectFirmwarePOSTData() {
		return new String[] {
			"&var%3Alang=de&var%3Amenu=home&var%3Apagename=home&login%3Acommand%2Fpassword=", 
			"&var%3Alang=en&var%3Amenu=home&var%3Apagename=home&login%3Acommand%2Fpassword="};
	}

	IFritzBoxAuthenticator getDetectFirmwareURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return PasswordFritzBoxFirmware.this.m_password;
			}
			
		};
	}

	public void login() throws FritzBoxLoginException {
		super.login();
	
		try {
			String postdata = POSTDATA_LOGIN.replaceAll("\\$PASSWORT",
					URLEncoder.encode(this.m_password, "ISO-8859-1"));

			postdata = (this.m_language.equalsIgnoreCase("en") ? getAccessMethodPOSTData()[1] : getAccessMethodPOSTData()[0]) + postdata;

			String urlstr = getProtocol() + this.m_address + ":" + this.m_port
					+ "/cgi-bin/webcm";
			executeURL(urlstr, postdata, true);
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new FritzBoxLoginException("Login to FritzBox failed. Please check password.");
		}
		
	}

	String getListPOSTData() {
		return "&var%3Alang=$LANG&var%3Amenu=fon&var%3Apagename=foncalls&login%3Acommand%2Fpassword=";
	}

	IFritzBoxAuthenticator getListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return PasswordFritzBoxFirmware.this.m_password;
			}
			
		};
	}

	String getFetchCallListPOSTData() {
		return "getpage=../html/$LANG/$CSV_FILE&errorpage=..%2Fhtml%2F$LANG%2Fmenus%2Fmenu2.html&var%3Alang=$LANG&var%3Apagename=foncalls&var%3Aerrorpagename=foncalls&var%3Amenu=fon&var%3Apagemaster=&time%3Asettings%2Ftime=1136559837%2C-60";
	}

	IFritzBoxAuthenticator getFetchCallListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return "";
			}
			
		};
	}
	

	String getFetchBlockedListPOSTData() {
		return "";
	}

	IFritzBoxAuthenticator getFetchBlockedListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return "";
			}
			
		};
	}


	String getBlockPOSTData() {
		return "";
	}

	IFritzBoxAuthenticator getBlockURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return "";
			}
			
		};
	}
		
	String getFetchCallerListPOSTData() {
		return "";
	}

	IFritzBoxAuthenticator getFetchCallerListURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return "";
			}
			
		};
	}

	String getClearPOSTData() {
		return "&var%3Alang=$LANG&var%3Apagename=foncalls&var%3Amenu=fon&telcfg%3Asettings/ClearJournal=1";
	}

	IFritzBoxAuthenticator getClearURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return "";
			}
			
		};
	}

	String getCallPOSTData() {
		return "&login:command/password=$PASSWORT&telcfg:settings/UseClickToDial=1&telcfg:settings/DialPort=$NEBENSTELLE&telcfg:command/Dial=$NUMMER";
	}

	IFritzBoxAuthenticator getCallURLAuthenticator() {
		return new IFritzBoxAuthenticator() {

			public String getAuthenticationToken() {
				return PasswordFritzBoxFirmware.this.m_password;
			}
			
		};
	}

}
