package de.janrufmonitor.repository.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringEscapeUtils;
import de.janrufmonitor.util.string.StringUtils;

/**
 * This class implements the HTTP call to the web caller manager with certain regular expression for
 * address data, name and phonenumber.
 * 
 *@author     Thilo Brandt
 *@created    2007/03/14
 */
public class RegExpURLRequester extends AbstractURLRequester {

	public static String REGEXP_LASTNAME = "regexp.lastname";
	public static String REGEXP_FIRSTNAME = "regexp.firstname";
	public static String REGEXP_ADDITIONAL = "regexp.additional";
	
	public static String REGEXP_STREET = "regexp.street";
	public static String REGEXP_STREETNO = "regexp.streetno";
	public static String REGEXP_POSTALCODE = "regexp.pcode";
	public static String REGEXP_CITY = "regexp.city";
	
	public static String REGEXP_AREACODE = "regexp.areacode";
	public static String REGEXP_PHONE = "regexp.phone";
	public static String ENCODING = "encoding";
	
	private IRuntime m_runtime;
	private Properties m_config;
	private String m_pn;
	private String m_ns;
	private Locale m_locale;
	private String m_intareacode;
	private String m_ua;
	
	/**
	 * Creates a new instance of a RegExp URL Requester. 
	 * 
	 * @param url URL to be called (containing phone string). Must not be null.
	 * @param skip Bytes to drop from reading
	 * @param pn phonenumber in raw format, e.g. 072657110
	 * @param ns namespace of the calling web caller manager. Must not be null.
	 * @param config configuration from the calling web caller manager. Must not be null.
	 * @param r the current runtime instance. Must not be null.
	 */
	public RegExpURLRequester(String url, long skip, String pn, String ns, Properties config, IRuntime r, Locale l, String intarea, String ua) {
		super(url, skip);
		this.m_config = config;
		this.m_pn = pn;
		this.m_ns = ns;
		this.m_runtime = r;
		this.m_locale = l;
		this.m_intareacode = intarea;
		this.m_ua = ua; 
	}
	
	public RegExpURLRequester(String url, long skip, String pn, String ns, Properties config, IRuntime r, Locale l, String intarea) {
		this(url, skip, pn, ns, config, r, l, intarea, "Mozilla/4.0 (compatible; MSIE; Windows NT)");
	}


	public void go() throws Exception {
		List failure = new ArrayList();
		URL url = new URL(this.url);
		
		URLConnection c = null;
		
		if (url.getProtocol().equalsIgnoreCase("https")) {
			c = (HttpsURLConnection) url.openConnection();
			((HttpsURLConnection)c).setRequestMethod("GET");
		} else {
			c = (HttpURLConnection) url.openConnection();
			((HttpURLConnection)c).setRequestMethod("GET");
		}

		c.setDoInput(true);
		c.setRequestProperty(
			"User-Agent",
			this.m_ua); //"Mozilla/4.0 (compatible; MSIE; Windows NT)"
		c.connect();

		Object o = c.getInputStream();
		if (o != null && o instanceof InputStream) {

			this.m_logger.info("Content successfully retrieved from "+url.getHost()+"...");
			InputStreamReader isr = new InputStreamReader((InputStream) o, this.m_config.getProperty(ENCODING,"iso-8859-1"));
			Thread.sleep(200);
			if (m_logger.isLoggable(Level.INFO)) {
	
				File dir = new File(PathResolver.getInstance(this.m_runtime).getLogDirectory(), "~"+this.m_ns.toLowerCase());
				if (!dir.exists()) dir.mkdirs();
				
				File log = new File(dir, this.m_pn+".log");
				if (log.exists()) log.delete();
				
				FileOutputStream fos = new FileOutputStream(log);
				BufferedReader br = new BufferedReader(isr);
				while(br.ready()) {
					fos.write(br.readLine().getBytes());
					fos.write(IJAMConst.CRLF.getBytes());
				}
				fos.flush();
				fos.close();
				br.close();
				br = null;
				isr = new InputStreamReader(new FileInputStream(log)); 
			}
			
			BufferedReader br = new BufferedReader(isr);
			// skip n bytes from the stream
			if (this.m_skip>0)
				br.skip(this.m_skip);

			StringBuffer content = new StringBuffer();
			while (br.ready()) {
				content.append(br.readLine());
			}
			
			br.close();
			isr.close();

			String group = null;
			String patternString = null;

			this.m = m_runtime.getCallerFactory().createAttributeMap();

			// get name patterns
			patternString = this.m_config.getProperty(REGEXP_LASTNAME);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0) {
					//group = StringUtils.replaceString(group, "&amp;", "&");
					// added 2010/04/01: added HTML decoding routine
					// added 2012/01/20: URLDecode
					group = URLDecoder.decode(StringEscapeUtils.unescapeHtml(this.encodeNonUnicode(group)), this.m_config.getProperty(ENCODING,"iso-8859-1"));
					if (this.containsHTML(group)) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Still found HTML tags in pattern value: "+group);
						group = StringEscapeUtils.unescapeHtml(group);
					}
					this.m.add(
						m_runtime.getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_LASTNAME,
							group.trim()
						)
					);
				} else {
					failure.add(REGEXP_LASTNAME);
				}
			} else {
				failure.add(REGEXP_LASTNAME);
			}
			
			patternString = this.m_config.getProperty(REGEXP_FIRSTNAME);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0) {
					//group = StringUtils.replaceString(group, "&amp;", "&");
					// added 2010/04/01: added HTML decoding routine
					// added 2012/01/20: URLDecode
					group = URLDecoder.decode(StringEscapeUtils.unescapeHtml(this.encodeNonUnicode(group)), this.m_config.getProperty(ENCODING,"iso-8859-1"));
					if (this.containsHTML(group)) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Still found HTML tags in pattern value: "+group);
						group = StringEscapeUtils.unescapeHtml(group);
					}
					this.m.add(
						m_runtime.getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
							group.trim()
						)
					);
				}
				else {
					failure.add(REGEXP_FIRSTNAME);
				}
			}else {
				failure.add(REGEXP_FIRSTNAME);
			}
			
			patternString = this.m_config.getProperty(REGEXP_ADDITIONAL);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0){
					//group = StringUtils.replaceString(group, "&amp;", "&");
					// added 2010/04/01: added HTML decoding routine
					// added 2012/01/20: URLDecode
					group = URLDecoder.decode(StringEscapeUtils.unescapeHtml(this.encodeNonUnicode(group)), this.m_config.getProperty(ENCODING,"iso-8859-1"));
					if (this.containsHTML(group)) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Still found HTML tags in pattern value: "+group);
						group = StringEscapeUtils.unescapeHtml(group);
					}
					this.m.add(
						m_runtime.getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_ADDITIONAL,
							group.trim()
						)
					);
				}
				else {
					failure.add(REGEXP_ADDITIONAL);
				}
			}else {
				failure.add(REGEXP_ADDITIONAL);
			}
			
			// get address patterns
			patternString = this.m_config.getProperty(REGEXP_STREET);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0) {
					//group = StringUtils.replaceString(group, "&amp;", "&");
					// added 2010/04/01: added HTML decoding routine
					// added 2012/01/20: URLDecode
					group = URLDecoder.decode(StringEscapeUtils.unescapeHtml(this.encodeNonUnicode(group)), this.m_config.getProperty(ENCODING,"iso-8859-1"));
					if (this.containsHTML(group)) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Still found HTML tags in pattern value: "+group);
						group = StringEscapeUtils.unescapeHtml(group);
					}
					this.m.add(
						m_runtime.getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_STREET,
							group.trim()
						)
					);
				}else {
					failure.add(REGEXP_STREET);
				}
			}else {
				failure.add(REGEXP_STREET);
			}
			
			patternString = this.m_config.getProperty(REGEXP_STREETNO);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0)
					this.m.add(
						m_runtime.getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_STREET_NO,
							group.trim()
						)
					);
				else {
					failure.add(REGEXP_STREETNO);
				}
			} else {
				failure.add(REGEXP_STREETNO);
			}

			patternString = this.m_config.getProperty(REGEXP_POSTALCODE);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0)
					this.m.add(
						m_runtime.getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE,
							group.trim()
						)
					);
				else {
					failure.add(REGEXP_POSTALCODE);
				}
			} else {
				failure.add(REGEXP_POSTALCODE);
			}

			patternString = this.m_config.getProperty(REGEXP_CITY);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0) {
					//group = StringUtils.replaceString(group, "&amp;", "&");
					// added 2010/04/01: added HTML decoding routine
					// added 2012/01/20: URLDecode
					group = URLDecoder.decode(StringEscapeUtils.unescapeHtml(this.encodeNonUnicode(group)), this.m_config.getProperty(ENCODING,"iso-8859-1"));
					if (this.containsHTML(group)) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Still found HTML tags in pattern value: "+group);
						group = StringEscapeUtils.unescapeHtml(group);
					}
					this.m.add(
						m_runtime.getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_CITY,
							group.trim()
						)
					);
				}else {
					failure.add(REGEXP_CITY);
				}
			} else {
				failure.add(REGEXP_CITY);
			}
			
			this.m.add(
					m_runtime.getCallerFactory().createAttribute(
						IJAMConst.ATTRIBUTE_NAME_COUNTRY,
						this.m_locale.getDisplayCountry()
					)
				);
			
			patternString = this.m_config.getProperty(REGEXP_AREACODE);
			if (patternString!=null && patternString.length()>0) {
				group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
				if (group!=null && group.length()>0) {
					group = StringUtils.replaceString(group, " ", "");
					group = group.replaceAll("\t", "");
					this.pn = m_runtime.getCallerFactory().createPhonenumber(
							false
						);
					this.pn.setAreaCode((group.trim().startsWith("0") ? group.trim().substring(1) : group.trim()));
					this.pn.setIntAreaCode(this.m_intareacode);
					
					patternString = this.m_config.getProperty(REGEXP_PHONE);
					if (patternString!=null && patternString.length()>0) {
						group = this.find(Pattern.compile(patternString, Pattern.UNICODE_CASE), content);
						if (group!=null && group.length()>0) {
							group = StringUtils.replaceString(group, " ", "");
							group = group.replaceAll("\t", "");
							this.pn.setCallNumber(group.trim());
						} else {
							this.pn = null;
							failure.add(REGEXP_PHONE);
						}
					} else {
						this.pn = null;	 	
						failure.add(REGEXP_PHONE);
					}
				}
				else {
					failure.add(REGEXP_AREACODE);
				}
			}
			else {
				failure.add(REGEXP_AREACODE);
			}

			if (this.m==null || this.pn==null || this.m.size()<2) {
				throw new RegExpURLRequesterException("Identification with "+url.getHost()+" failed.", failure); 
			}
			return;
			
		}
		throw new RegExpURLRequesterException("Content from "+this.url+" is invalid.", failure);
	}
	
	private String find(Pattern p, StringBuffer c){
		Matcher m = p.matcher(c);
		if (m.find() && m.groupCount()==1) {
			return m.group(1).trim();
		}
		return null;		
	}
	
	private String encodeNonUnicode(String q) {
//		for (int i =0;i<q.length();i++) {
//			System.out.println(q.charAt(i) + "=" + Integer.toHexString(q.charAt(i)).toUpperCase(Locale.ENGLISH));
//		}
    	q = StringUtils.replaceString(q, "\u00C4", "&Auml;");
    	q = StringUtils.replaceString(q, "\u00E4", "&auml;");
    	q = StringUtils.replaceString(q, "\u00D6", "&Ouml;");
    	q = StringUtils.replaceString(q, "\u00F6", "&ouml;");
    	q = StringUtils.replaceString(q, "\u00DC", "&Uuml;");
    	q = StringUtils.replaceString(q, "\u00FC", "&uuml;");
    	q = StringUtils.replaceString(q, "\u00DF", "&szlig;");
    	return q;
	}
	
	private boolean containsHTML(String q) {
		if (q.indexOf("&Auml;")>-1) return true;
		if (q.indexOf("&auml;")>-1) return true;
		if (q.indexOf("&Ouml;")>-1) return true;
		if (q.indexOf("&ouml;")>-1) return true;
		if (q.indexOf("&Uuml;")>-1) return true;
		if (q.indexOf("&uuml;")>-1) return true;
		if (q.indexOf("&szlig;")>-1) return true;
		return false;
	}

}
