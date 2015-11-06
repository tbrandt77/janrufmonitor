package de.janrufmonitor.fritzbox;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FritzBoxMD5Handler {

	public static String getTR064Auth(String user, String password, String realm, String nonce) {
		StringBuffer response = new StringBuffer();
		response.append(convertToHex(getMD5(convertToHex(getMD5(user+":"+realm+":"+password, "ISO8859_1")) + ":" + nonce, "ISO8859_1")));
		return response.toString();		
	}
	
	public static String getResponse(String challenge, String password) {
		StringBuffer response = new StringBuffer();
		response.append(challenge);
		response.append("-");
		response.append(convertToHex(getMD5(challenge+"-"+password, "UnicodeLittleUnmarked")));
		return response.toString();		
	}
	
	private static byte[] getMD5(String passwd, String enc) {
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance("MD5");
            md.update(passwd.getBytes(enc));
        } catch (NoSuchAlgorithmException ex) {
        } catch (UnsupportedEncodingException e) {
		}

        return md.digest();
    }
	
	private static String convertToHex(byte[] data) {
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

	
}
