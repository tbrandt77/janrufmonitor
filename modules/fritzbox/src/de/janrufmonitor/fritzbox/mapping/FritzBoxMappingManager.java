package de.janrufmonitor.fritzbox.mapping;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.fritzbox.IPhonebookEntry;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.TR064FritzBoxFirmware;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerImageException;
import de.janrufmonitor.repository.FritzBoxPhonebookManager;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.Base64Decoder;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringEscapeUtils;
import de.janrufmonitor.util.string.StringUtils;

public class FritzBoxMappingManager {

	private class XMLPeHandler extends DefaultHandler {
		private List contacts = new ArrayList();;
		private FritzBoxPhonebookEntry currentPe;
		
		private String currentValue; 
		private String[] currentNumber;
		private String m_ab;
		private int eid = 0;
		
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
				currentPe = new FritzBoxPhonebookEntry(); 
				currentPe.setEntryID(Integer.toString(this.eid)); eid++;
			}
			if (qname.equalsIgnoreCase("jam_info")) {
				int length = attributes.getLength();
				for (int i=0;i<length;i++) {
					currentPe.addJamInfo(attributes.getQName(i), attributes.getValue(i));
				}
			}
			if (qname.equalsIgnoreCase("number")) {
				currentNumber = new String[7];
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
				
				currentNumber[2] = attributes.getValue("type");
				currentNumber[3] = attributes.getValue("prio");
				currentNumber[4] = attributes.getValue("id");
				currentNumber[5] = attributes.getValue("quickdial");
				currentNumber[6] = attributes.getValue("vanity");
			}
		}
		
		public void endElement(String uri, String name, String qname)
		throws SAXException {
			if (qname.equalsIgnoreCase("realname") && currentPe!=null) {
				currentPe.setName(currentValue);
			}
			
			if (qname.equalsIgnoreCase("category") && currentPe!=null) {
				currentPe.setCategory(currentValue);
			}
			
			if (qname.equalsIgnoreCase("uniqueid") && currentPe!=null) {
				currentPe.setUniqueID(currentValue);
			}
			
			if (qname.equalsIgnoreCase("mod_time") && currentPe!=null) {
				currentPe.setModTime(currentValue);
			}
			
			if (qname.equalsIgnoreCase("number") && currentNumber!=null && currentPe!=null) {
				currentNumber[1] = currentValue;
				currentPe.addNumber(currentNumber[1], currentNumber[0]);
				if (currentNumber[4]!=null && currentNumber[4].length()>0)
					currentPe.addNumberID(currentNumber[1], currentNumber[4]);
				if (currentNumber[2]!=null && currentNumber[2].length()>0)
					currentPe.addNumberType(currentNumber[1], currentNumber[2]);
				if (currentNumber[3]!=null && currentNumber[3].length()>0)
					currentPe.addNumberPrio(currentNumber[1], currentNumber[3]);
				if (currentNumber[5]!=null && currentNumber[5].length()>0)
					currentPe.addNumberQuickDial(currentNumber[1], currentNumber[5]);
				if (currentNumber[6]!=null && currentNumber[6].length()>0)
					currentPe.addNumberVanity(currentNumber[1], currentNumber[6]);
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
				currentPe.setImageURL(currentValue);
			}

		}
		
		public List getList() {
			return contacts;
		}
	}
	
	private static FritzBoxMappingManager m_instance = null;
	private Logger m_logger;
	private IRuntime m_runtime;
	
	private Formatter m_f;


	private FritzBoxMappingManager() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	private Formatter getFormatter() {
		if (this.m_f==null) {
			this.m_f = Formatter.getInstance(PIMRuntime.getInstance());
		}
		return this.m_f;
	}
	
	public static synchronized FritzBoxMappingManager getInstance() {
		if (FritzBoxMappingManager.m_instance == null) {
			FritzBoxMappingManager.m_instance = new FritzBoxMappingManager();
		}
		return FritzBoxMappingManager.m_instance;
	}
	
	public List toFritzBoxCallerList(ICallerList l) {
		List cl = new ArrayList(l.size());
		IPhonebookEntry pe = null;
		for (int i=0;i<l.size();i++) {
			try {
				pe = this.mapJamCallerToFritzBox((ICaller) l.get(i));
				if (pe!=null)
					cl.add(pe);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return cl;
	}
	
	public ICallerList toCallerList(List l) {
		ICallerList cl = getRuntime().getCallerFactory().createCallerList(l.size());
		ICaller c = null;
		for (int i=0;i<l.size();i++) {
			try {
				c = this.mapFritzBoxCallerToJam((IPhonebookEntry) l.get(i));
				if (c!=null)
					cl.add(c);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return cl;
	}
	
	public List parseXmltoFritzBoxCallerList(StringBuffer xml, String ab) {
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
	
	public IPhonebookEntry mapJamCallerToFritzBox(ICaller caller) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Processing JAM caller: "+caller.toString());
		
		IPhonebookEntry pe = new FritzBoxPhonebookEntry();
		IAttributeMap attributes = caller.getAttributes();
		
		String name = ""; 
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)) {
			name += attributes.get(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue() + " ";
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, attributes.get(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue());
		}
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME)) {
			name+= attributes.get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue();
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_LASTNAME, attributes.get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue());
		}
		pe.setName(name.trim());
		
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_STREET)) {
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_STREET, attributes.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue());
		}
		
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_STREET_NO)) {
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_STREET_NO, attributes.get(IJAMConst.ATTRIBUTE_NAME_STREET_NO).getValue());
		}
		
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_CITY)) {
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_CITY, attributes.get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue());
		}
		
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE)) {
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, attributes.get(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue());
		}
		
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_COUNTRY)) {
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_COUNTRY, attributes.get(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue());
		}
		
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_CATEGORY)) {
			pe.addJamInfo(IJAMConst.ATTRIBUTE_NAME_CATEGORY, attributes.get(IJAMConst.ATTRIBUTE_NAME_CATEGORY).getValue());
		}
		
		if (caller.getUUID().length()<10) // hack, since FB only supports integer values
			pe.setUniqueID(caller.getUUID());
		
		if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_EMAIL))
			pe.setEmail(attributes.get(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue());
		
		if (attributes.contains("fb_entryID"))
			pe.setEntryID(attributes.get("fb_entryID").getValue());
		
		if (attributes.contains("fb_mod_time"))
			pe.setModTime(attributes.get("fb_mod_time").getValue());
		
		if (attributes.contains("fb_category"))
			pe.setCategory(attributes.get("fb_category").getValue());
		
		if (attributes.contains("fb_imageURL")) {
			// replace /download.lua?path=/var by f
			String nurl = attributes.get("fb_imageURL").getValue();
			nurl = StringUtils.replaceString(nurl, "/download.lua?path=/var", "file:///var");
			pe.setImageURL(nurl);	
		}
		
		List phones = new ArrayList();
		
		if (caller instanceof IMultiPhoneCaller) {
			phones = ((IMultiPhoneCaller)caller).getPhonenumbers();
		} else {
			phones.add(caller.getPhoneNumber());
		}
		
		for (int i=0;i<phones.size();i++) {
			IPhonenumber pn = (IPhonenumber) phones.get(i);
			String number = this.getFormatter().parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, pn);
			if (attributes.contains("fb_number_type_"+pn.getTelephoneNumber())) {
				pe.addNumber(number, attributes.get("fb_number_type_"+pn.getTelephoneNumber()).getValue());
				pe.addNumberType(number, attributes.get("fb_number_type_"+pn.getTelephoneNumber()).getValue());
			} else if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber())) {
				if (attributes.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber()).getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
					pe.addNumber(number, "mobile"); pe.addNumberType(number, "mobile");
				} else if (attributes.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber()).getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
					pe.addNumber(number, "fax_work"); pe.addNumberType(number, "fax_work");
				} else {
					pe.addNumber(number, "home"); pe.addNumberType(number, "home");
				}
			} else {
				pe.addNumber(number, "home"); pe.addNumberType(number, "home");
			}
			if (attributes.contains("fb_number_id_"+pn.getTelephoneNumber())) {
				pe.addNumberID(number, attributes.get("fb_number_id_"+pn.getTelephoneNumber()).getValue());
			}
			if (attributes.contains("fb_number_prio_"+pn.getTelephoneNumber())) {
				pe.addNumberPrio(number, attributes.get("fb_number_prio_"+pn.getTelephoneNumber()).getValue());
			}
			if (attributes.contains("fb_number_quickdial_"+pn.getTelephoneNumber())) {
				pe.addNumberQuickDial(number, attributes.get("fb_number_quickdial_"+pn.getTelephoneNumber()).getValue());
			}
			if (attributes.contains("fb_number_vanity_"+pn.getTelephoneNumber())) {
				pe.addNumberVanity(number, attributes.get("fb_number_vanity_"+pn.getTelephoneNumber()).getValue());
			}
		}
		
		return pe;
	}	
	
	public ICaller mapFritzBoxCallerToJam(IPhonebookEntry  pe) throws IOException {
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Processing FritzBox phonebook caller: "+pe.toString());
		
		List phones = new ArrayList(3);
		IAttributeMap attributes = getRuntime().getCallerFactory().createAttributeMap();

		attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER, FritzBoxPhonebookManager.ID));
		
		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_LASTNAME)!=null) {
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_LASTNAME)));
		} else {
			try {
				attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, StringEscapeUtils.unescapeHtml(pe.getName())));
			} catch (Exception ex) {
				this.m_logger.log(Level.WARNING, ex.getMessage(), ex);
				attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, pe.getName()));
			}
		}
		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)!=null) 
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)));

		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_STREET)!=null) 
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_STREET)));

		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_STREET_NO)!=null) 
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET_NO, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_STREET_NO)));

		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE)!=null) 
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE)));

		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_CITY)!=null) 
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CITY, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_CITY)));

		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_COUNTRY)!=null) 
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_COUNTRY)));

		if (pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_CATEGORY)!=null) {
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY, pe.getJamInfo(IJAMConst.ATTRIBUTE_NAME_CATEGORY)));
		}
		
		if (!FirmwareManager.getInstance().isInstance(TR064FritzBoxFirmware.class)){
			if (pe.getAddressbook()!=null && pe.getAddressbook().length()>0)
				attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY, pe.getAddressbook()));
		}
		
		if (pe.getEmail()!=null && pe.getEmail().length()>0) {
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL, pe.getEmail()));
		}
		
		if (pe.getEntryID()!=null && pe.getEntryID().length()>0) {
			attributes.add(getRuntime().getCallerFactory().createAttribute("fb_entryID", pe.getEntryID()));
		}
		
		if (pe.getModTime()!=null && pe.getModTime().length()>0) {
			attributes.add(getRuntime().getCallerFactory().createAttribute("fb_mod_time", pe.getModTime()));
		}
		
		if (pe.getCatergory()!=null && pe.getCatergory().length()>0) {
			attributes.add(getRuntime().getCallerFactory().createAttribute("fb_category", pe.getCatergory()));
		}
		
		if (pe.getImageURL()!=null && pe.getImageURL().length()>0) {
			attributes.add(getRuntime().getCallerFactory().createAttribute("fb_imageURL", pe.getImageURL()));
		}
		
		Map phs = pe.getPhones();
		Iterator entries = phs.keySet().iterator();
		String key = null;
		IPhonenumber phone = null;
		while (entries.hasNext()) {
			key = (String) entries.next();
			
			// added 2016/01/19: remove internal FB AB numbers from contact list.
			if (key!=null && key.startsWith("**")) continue;
			
			if (key !=null && !PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(key) && !PhonenumberAnalyzer.getInstance(getRuntime()).isClired(key)) {
			
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("FritzBox raw number: "+key);
				}
				phone = PhonenumberAnalyzer.getInstance(getRuntime()).toIdentifiedPhonenumber(key);
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("FritzBox identified number: "+phone);
				}
				if (phone!=null) {
					phones.add(phone);
					attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+phone.getTelephoneNumber(), (String) phs.get(key)));
					if (pe.getNumberID(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_id_"+phone.getTelephoneNumber(), pe.getNumberID(key)));
					}
					if (pe.getNumberPrio(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_prio_"+phone.getTelephoneNumber(), pe.getNumberPrio(key)));
					}
					if (pe.getNumberQuickDial(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_quickdial_"+phone.getTelephoneNumber(), pe.getNumberQuickDial(key)));
					}
					if (pe.getNumberType(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_type_"+phone.getTelephoneNumber(), pe.getNumberType(key)));
					}
					if (pe.getNumberVanity(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_vanity_"+phone.getTelephoneNumber(), pe.getNumberVanity(key)));
					}
				}
			}
			if (PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).isInternal((key.trim()))) {
				phone = PhonenumberAnalyzer.getInstance(getRuntime()).toInternalPhonenumber(key);
				if (phone!=null) {
					phones.add(phone);
					attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+phone.getTelephoneNumber(), (String) phs.get(key)));
					if (pe.getNumberID(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_id_"+phone.getTelephoneNumber(), pe.getNumberID(key)));
					}
					if (pe.getNumberPrio(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_prio_"+phone.getTelephoneNumber(), pe.getNumberPrio(key)));
					}
					if (pe.getNumberQuickDial(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_quickdial_"+phone.getTelephoneNumber(), pe.getNumberQuickDial(key)));
					}
					if (pe.getNumberType(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_type_"+phone.getTelephoneNumber(), pe.getNumberType(key)));
					}
					if (pe.getNumberVanity(key)!=null) {
						attributes.add(getRuntime().getCallerFactory().createAttribute("fb_number_vanity_"+phone.getTelephoneNumber(), pe.getNumberVanity(key)));
					}
				}
			}
		}
		if (phones.size()==0) return null;
		
		String img = pe.getImageBase64();
		if (img==null && pe.getImageURL()!=null) {
			// get from FB directly
			try {
				img = pe.getImageURL();
				img = StringUtils.replaceString(img, "file:///var", "/download.lua?path=/var");
				img = FirmwareManager.getInstance().getCallerImage(img);
			} catch (GetCallerImageException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		if (img!=null) {
			// 2015/11/06: added image support in FB phonebook
			ByteArrayInputStream in = new ByteArrayInputStream(Base64Decoder.decode(img).getBytes("iso-8859-1"));
			File photoDir = new File(PathResolver.getInstance().getPhotoDirectory());
			if (!photoDir.exists())
				photoDir.mkdirs();
			FileOutputStream out = new FileOutputStream(new File(photoDir, ((IPhonenumber)phones.get(0)).getTelephoneNumber()+".png"));
			Stream.copy(in, out, true);
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, new File(PathResolver.getInstance().getPhotoDirectory(), ((IPhonenumber)phones.get(0)).getTelephoneNumber()+".png").getAbsolutePath()));
		}
		if (pe.getUniqueID()!=null && pe.getUniqueID().length()>0) {
			return getRuntime().getCallerFactory().createCaller(pe.getUniqueID(), null, phones, attributes);	
		} 
		return getRuntime().getCallerFactory().createCaller(null, phones, attributes);	
	}
	
}
