package de.janrufmonitor.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JOptionPane;

public class LaunchDial {

	/**
	 * Syntax of application parameter:
	 * 
	 * callto:<number>[+gateway=<server>:<port>] or
	 * tel:<number>[+gateway=<server>:<port>]
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args!=null && args.length==1) {
			String[] callto = args[0].split("\\+");
			String number = null;
			String server = null;
			if (callto.length==1) {
				number = toCallablePhonenumber(callto[0].substring(callto[0].startsWith("tel:") ? 4 : 7));
				server = "localhost:5555";
			}
			if (callto.length==2) {
				number = toCallablePhonenumber(callto[0].substring(callto[0].startsWith("tel:") ? 4 : 7));
				if (callto[1].startsWith("gateway")) {
					server = callto[1].substring(8);
				}
			}
			if (number!=null && server!=null) {
				try {
					StringBuffer url = new StringBuffer();
					url.append("http://");
					url.append(server);
					url.append("/callto?dial=");
					url.append(number);
					
					URL ul = new URL(url.toString());
					URLConnection c = ul.openConnection();
					c.setDoInput(true);
					c.setRequestProperty("User-Agent", "Callto command");
					c.connect();

					Object o = c.getContent();
					if (o instanceof InputStream) {
						JOptionPane.showMessageDialog(null, "Ein Anruf zur Rufnummer "+number+" wird hergestellt.\nBitte nehmen Sie den H\u00F6rer an Ihrem Telefon jetzt ab...");  
					}				
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Der Anruf zur Rufnummer "+number+" konnte nicht hergestellt werden.\nEntweder ist der jAnrufmonitor Server nicht gestartet oder vor\u00FCbergehend nicht erreichbar.");  
				} 
			} else {
				JOptionPane.showMessageDialog(null, "Das Programm kann ohne Rufnummer nicht gestartet werden.\n Starten Sie mit \"callto.exe callto:<rufnummer>[+gateway=<server>:<port>]\".");  
			}
		} else {
			JOptionPane.showMessageDialog(null, "Das Programm kann ohne Rufnummer nicht gestartet werden.\n Starten Sie mit \"callto.exe callto:<rufnummer>[+gateway=<server>:<port>]\".");  
		}	
	}
	
	public static String toCallablePhonenumber(String phone) {
		phone = phone.trim();

		phone = replaceString(phone, " ", "");
		phone = replaceString(phone, "/", "");
		phone = replaceString(phone, "(", "");
		phone = replaceString(phone, "(", "");
		phone = replaceString(phone, ")", "");
		phone = replaceString(phone, "-", "");
		phone = replaceString(phone, "#", "");
		phone = replaceString(phone, ".", "");
		phone = replaceString(phone, "+", "00");

		return phone;
	}
	
	public static String replaceString(String source, String search, String replace) {
        int pos = source.indexOf(search);
        if (pos == -1) return source;
        
		StringBuffer result = new StringBuffer(source.length() + replace.length());
        while (pos > -1) {
            result.append(source.substring(0, pos));
            result.append(replace);
            source = source.substring(pos + search.length());
			pos = source.indexOf(search);
        }
        result.append(source);
        
        return result.toString();
    }

}
