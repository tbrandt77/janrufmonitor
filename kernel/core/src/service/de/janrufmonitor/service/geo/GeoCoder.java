package de.janrufmonitor.service.geo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.objects.Attribute;
import de.janrufmonitor.framework.objects.AttributeMap;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.math.Point;
import de.janrufmonitor.util.string.StringUtils;

public class GeoCoder {
	
	private static GeoCoder m_instance = null;
	private Logger m_logger;
	private Point m_localPosition;
	 
	private GeoCoder() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);	
	}
	
    public static synchronized GeoCoder getInstance() {
        if (GeoCoder.m_instance == null) {
        	GeoCoder.m_instance = new GeoCoder();
        }
        return GeoCoder.m_instance;
    }
    
    public static void invalidate() {
    	GeoCoder.m_instance = null;
    }
    
    public Point getLocalPosition() {
    	return this.m_localPosition;
    }
    
    public void setLocalPosition(Point p) {
    	this.m_localPosition = p;
    }
    
    public Point getCoordinates(String q) {
    	String result = null;    	
    	q = StringUtils.replaceString(q, "\u00C4", "ae");
    	q = StringUtils.replaceString(q, "\u00E4", "ae");
    	q = StringUtils.replaceString(q, "\u00D6", "oe");
    	q = StringUtils.replaceString(q, "\u00F6", "oe");
    	q = StringUtils.replaceString(q, "\u00DC", "ue");
    	q = StringUtils.replaceString(q, "\u00FC", "ue");
    	q = StringUtils.replaceString(q, "\u00DF", "ss");
    	try {
			q = URLEncoder.encode(q.trim(), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.toString(), e);
		}
		
		try {
			URL url = new URL("http://geo.janrufmonitor.de/code.php?q="+q);
			URLConnection c = url.openConnection();
			c.setDoInput(true);
			c.setRequestProperty("User-Agent", "jAnrufmonitor GeoCoder "+IJAMConst.VERSION_DISPLAY);
			c.connect();
			
			if (this.m_logger!=null && this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Querying URL "+url);
			
			Object o = c.getContent();
			if (o instanceof InputStream) {
				if (this.m_logger!=null)
					this.m_logger.info("Content successfully retrieved from "+url);
				BufferedInputStream bin = new BufferedInputStream((InputStream) o);
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				Stream.copy(bin, bout, true);
				result = bout.toString();
				if (this.m_logger!=null)
					this.m_logger.info("Geocoding raw result: "+result);
				bin.close();
			}				
		} catch (MalformedURLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);			
		} 
		
		if (result!=null) {
			String[] values = result.split(",");
			if (values[0].equalsIgnoreCase("200")) {
				return new Point(Double.parseDouble(values[3]), Double.parseDouble(values[2]), Integer.parseInt(values[1]));				
			}		
			if (values[0].equalsIgnoreCase("201")) {
				this.m_logger.info("geo.janrufmonitor.de cached data: "+result);
				return new Point(Double.parseDouble(values[3]), Double.parseDouble(values[2]), Integer.parseInt(values[1]));				
			}	
		}
    	return null;
    }
    
    private boolean hasStreetNo(String street) {
    	String[] s = street.split(" ");
    	if (s.length>1) {
    		try {
    			Integer.parseInt(s[s.length-1]);
    		} catch (Exception e) {
    			return false;
    		}
    		return true;
    	}
    	return false;
    }
    
    private String getStreetNo(String street) {
    	String[] s = street.split(" ");
    	if (s.length>1) {
    		return s[s.length-1];
    	}
    	return null;
    }
	
    public Point getCoordinates(IAttributeMap atts) {
    	String result = null;    
    	StringBuffer query = new StringBuffer();
    	if (atts.contains(IJAMConst.ATTRIBUTE_NAME_STREET) && atts.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue().length()>0) {
    		// check if street no is available
    		if (!atts.contains(IJAMConst.ATTRIBUTE_NAME_STREET_NO)) {
    			this.m_logger.info("Splitting street attribute: "+atts.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue());
    			if (hasStreetNo(atts.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue())) {
    				String no = getStreetNo(atts.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue());
    				atts.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET_NO, no));
    				atts.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET, 
    						atts.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue().substring(0, atts.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue().length()-no.length()).trim()));
    			}
    		} 
        	query.append("&str=");
        	query.append(encode(atts.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue()));
    	}
    	if (atts.contains(IJAMConst.ATTRIBUTE_NAME_STREET_NO) && atts.get(IJAMConst.ATTRIBUTE_NAME_STREET_NO).getValue().length()>0) {
    		query.append("&no=");
    		query.append(encode(atts.get(IJAMConst.ATTRIBUTE_NAME_STREET_NO).getValue()));
    	}
    	if (atts.contains(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE) && atts.get(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue().length()>0) {
    		query.append("&pcode=");
    		query.append(encode(atts.get(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue()));    		
    	}
    	if (atts.contains(IJAMConst.ATTRIBUTE_NAME_CITY) && atts.get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue().length()>0) {
    		query.append("&city=");
    		query.append(encode(atts.get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue()));
    		// 2013/06/01: check if city is a forbidden city name
    		if (this.isForbiddenCity(encode(atts.get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue()))) return null;
    	}    	
    	if (atts.contains(IJAMConst.ATTRIBUTE_NAME_COUNTRY) && atts.get(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue().length()>0) {
    		query.append("&cntry=");
    		query.append(encode(atts.get(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue()));
    	}    	
    	
    	if (query.length()==0) {
    		this.m_logger.info("No relevant attributes for geo coding found.");
    		return null;
    	}
    	try {
			URL url = new URL("http://geo.janrufmonitor.de/code_v2.php?"+query.toString());
			URLConnection c = url.openConnection();
			c.setDoInput(true);
			c.setRequestProperty("User-Agent", "jAnrufmonitor GeoCoder "+IJAMConst.VERSION_DISPLAY);
			c.connect();
			
			if (this.m_logger!=null && this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Querying URL "+url);
			
			Object o = c.getContent();
			if (o instanceof InputStream) {
				if (this.m_logger!=null)
					this.m_logger.info("Content successfully retrieved from "+url);
				BufferedInputStream bin = new BufferedInputStream((InputStream) o);
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				Stream.copy(bin, bout, true);
				result = bout.toString();
				if (this.m_logger!=null)
					this.m_logger.info("Geocoding raw result: "+result);
				bin.close();
			}				
		} catch (MalformedURLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.INFO, e.getMessage(), e);			
		} 
		
		if (result!=null) {
			String[] values = result.split(",");
			if (values[0].equalsIgnoreCase("200")) {
				return new Point(Double.parseDouble(values[3]), Double.parseDouble(values[2]), Integer.parseInt(values[1]));				
			}		
			if (values[0].equalsIgnoreCase("201")) {
				this.m_logger.info("geo.janrufmonitor.de cached data: "+result);
				return new Point(Double.parseDouble(values[3]), Double.parseDouble(values[2]), Integer.parseInt(values[1]));				
			}	
		}
    	return null;   	
    }
    
    private boolean isForbiddenCity(String city) {
    	if (city==null) return true;
    	if (city.length()==0) return true;
    	if (city.toLowerCase().equalsIgnoreCase("vodafone")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("t-mobile")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("freephone dienste")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("e-plus mobilfunk")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("o2 (germany)")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("mabez rufnummer")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("shared cost-dienste")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("e-plus")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("e-plus mobilfunk gmbh & co. kg")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("swisscom mobile")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("premium rate")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("nationale teilnehmerrufnummer (ntr)")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("mobiltelefonnetz a1")) return true;
    	if (city.toLowerCase().equalsIgnoreCase("premium rate-dienste")) return true;
    	return false;
    }
    
    private String encode(String q) {
    	q = StringUtils.replaceString(q, "\u00C4", "ae");
    	q = StringUtils.replaceString(q, "\u00E4", "ae");
    	q = StringUtils.replaceString(q, "\u00D6", "oe");
    	q = StringUtils.replaceString(q, "\u00F6", "oe");
    	q = StringUtils.replaceString(q, "\u00DC", "ue");
    	q = StringUtils.replaceString(q, "\u00FC", "ue");
    	q = StringUtils.replaceString(q, "\u00DF", "ss");
    	try {
			q = URLEncoder.encode(q.trim(), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.toString(), e);
		}
		return q;
    }
    
    public static void main(String[] args) {
    	IAttributeMap m = new AttributeMap();
    	m.add(new Attribute(IJAMConst.ATTRIBUTE_NAME_CITY, "Angelbachtal"));
    	m.add(new Attribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, "74918"));
    	m.add(new Attribute(IJAMConst.ATTRIBUTE_NAME_STREET, "Lindenweg"));
    	m.add(new Attribute(IJAMConst.ATTRIBUTE_NAME_STREET_NO, "12"));
    	
    	System.out.println(GeoCoder.getInstance().getCoordinates(m));
    }
}
