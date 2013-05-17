package de.janrufmonitor.util.string;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *  This class provides string manipulation functions.
 * 
 *@author     Thilo Brandt
 *@created    2003/10/12
 */
public class StringUtils {

	/**
	 * Replaces a search string within the source string.
	 * 
	 * @param source the source string
	 * @param search the string to be replaced
	 * @param replace the replacement
	 * @return the manipulated string object
	 */
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

	/**
	 * URL encodes a string.
	 * 
	 * @param s the string to be encoded.
	 * @return encoded string
	 */
    public static String urlDecode(String s) {
        StringBuffer b = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '+') {
                b.append(' ');
            } else if (s.charAt(i) == '%') {
                int n = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                b.append((char) n);
                i += 2;
            } else {
                b.append(s.charAt(i));
            }
        }
        return b.toString();
    }

	/**
	 * URL decodes a string.
	 *  
	 * @param s the string to be decoded.
	 * @return decoded string
	 */
    public static String urlEncode(String s) {
        StringBuffer b = new StringBuffer(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                b.append('+');
            } else if (s.charAt(i) > 128 || s.charAt(i) < 44) {
                b.append('%').append(Integer.toString(s.charAt(i), 16));
            } else {
                b.append(s.charAt(i));
            }
        }
        return b.toString();
    }
    
    private static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
        sb.append(Integer.toHexString((array[i]
            & 0xFF) | 0x100).substring(1,3));        
        }
        return sb.toString();
    }
    
    /**
     * Encodes a string to an MD5 hex representation.
     * 
     * @param message
     * @return MD5 hex string
     */
    public static String toMD5Hex(String message) {
        try {
        MessageDigest md = 
            MessageDigest.getInstance("MD5");
        return hex (md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }
}
