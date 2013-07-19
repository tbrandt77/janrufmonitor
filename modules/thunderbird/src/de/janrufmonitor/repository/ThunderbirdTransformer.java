package de.janrufmonitor.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import mork.MorkDocument;
import mork.Row;
import mork.Table;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.string.StringUtils;

public class ThunderbirdTransformer {

	public static final String FIRSTNAME = "FirstName";
	public static final String LASTNAME = "LastName";
	public static final String COMPANY = "Company";
	
	public static final String WORK_PHONE = "WorkPhone";
	public static final String HOME_PHONE = "HomePhone";
	public static final String FAX_PHONE = "FaxNumber";
	public static final String MOBILE_PHONE = "CellularNumber";
	public static final String PAGER_PHONE = "PagerNumber";
	
	public static final String HOME_STREET = "HomeAddress";
	public static final String HOME_ZIP = "HomeZipCode";
	public static final String HOME_CITY = "HomeCity";
	public static final String HOME_COUNTRY = "HomeCountry";
	
	public static final String WORK_STREET = "WorkAddress";
	public static final String WORK_ZIP = "WorkZipCode";
	public static final String WORK_CITY = "WorkCity";
	public static final String WORK_COUNTRY = "WorkCountry";
	
	private String m_filename;
	private Logger m_logger;
	
	private int m_current;
	private int m_total;
	private boolean m_sync;
	
	public ThunderbirdTransformer(String f, boolean sync) {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_filename = f;
		this.m_sync = sync;
	}
	
	public ICallerList getCallers() {
		ICallerList cl = PIMRuntime.getInstance().getCallerFactory().createCallerList();
		if (this.m_filename==null) return cl;
		
		File f = new File(this.m_filename);
		if (!f.exists() || !f.isFile()) {
			this.m_logger.warning("Mozilla Thunderbird file "+this.m_filename+" does not exists.");
			PropagationFactory.getInstance().fire(
				new Message(Message.INFO, ThunderbirdCallerManager.NAMESPACE, "nofile", new Exception("Sync Mozilla Thunderbird Adressbook..."))
			);
			return cl;
		}
		
		if(this.m_sync)
			PropagationFactory.getInstance().fire(
				new Message(Message.INFO, ThunderbirdCallerManager.NAMESPACE, "sync", new Exception("Sync Mozilla Thunderbird Adressbook..."))
			);
		
		try {
			List rawData = new ArrayList();
			FileInputStream fis = new FileInputStream(f);
			MorkDocument md = new MorkDocument(new InputStreamReader(fis));
			List l = md.getRows();
			Row r = null;
			for (int i=0,j=l.size();i<j;i++) {
				r = (Row) l.get(i);
				rawData.add(r.getValues());
			}
			
			l = md.getTables();
			Table t =null;
			for (int i=0,j=l.size();i<j;i++) {
				t = (Table) l.get(i);
				for (int k=0;k<t.getRows().size();k++) {
					r = (Row) t.getRows().get(k);
					rawData.add(r.getValues());
				}
			}
			fis.close();
			this.m_total = rawData.size();
			if (rawData.size()>0) {
				this.m_logger.info("Found "+rawData.size()+" Mozilla Thunderbird contacts.");
				this.parseContacts(cl, rawData);
			}
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		this.m_logger.info(cl.size()+" contacts from Mozilla Thunderbird available.");
		return cl;
	}

	private void parseContacts(ICallerList cl, List rawData) {
		Map m = null;
		ICallerList c = null;
		for (int i=0,j=rawData.size();i<j;i++) {
			m = (Map) rawData.get(i);
			if (this.checkPhone(m)) {
				c = parseMap(m);
				if (c!=null && c.size()>0) cl.add(c);
			}
		}
		
	}

	private ICallerList parseMap(Map mx) {
		this.m_current++;
		Map m = new HashMap();
		m.putAll(mx);
		
		ICallerList cl = PIMRuntime.getInstance().getCallerFactory().createCallerList(2);
		ICaller c = null;
		if (this.checkWork(m)) {
			c = buildWorkContact(m);
			if (c!=null)
				cl.add(c);
		}
			
		if (this.checkHome(m)) {
			c = buildHomeContact(m);
			if (c!=null)
				cl.add(c);
		}
		return cl;
	}
	
	private String removeEscapedChars(String s) {
		if (s==null) return s;
		try {
			s = StringUtils.replaceString(s, "$C3$BC", "\u00fc");
			s = StringUtils.replaceString(s, "$C3$B6", "\u00f6");
			s = StringUtils.replaceString(s, "$C3$A4", "\u00e4");
			s = StringUtils.replaceString(s, "$C3$9F", "\u00df");
			
			s = StringUtils.replaceString(s, "$C3$9C", "\u00dc");
			s = StringUtils.replaceString(s, "$C3$96", "\u00d6");
			s = StringUtils.replaceString(s, "$C3$84", "\u00c4");
		} catch (Exception e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}

		return s;
	}
	
	private ICaller buildHomeContact(Map m) {
		IAttributeMap attributes = PIMRuntime.getInstance().getCallerFactory().createAttributeMap();
		
		attributes.add(
			PIMRuntime.getInstance().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, 
				removeEscapedChars((String) m.get(FIRSTNAME)))	
		);
		
		attributes.add(
			PIMRuntime.getInstance().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_LASTNAME, 
				removeEscapedChars((String) m.get(LASTNAME)))	
		);
		
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_STREET, 
					removeEscapedChars((String) m.get(HOME_STREET)))	
			);	
		
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, 
					(String) m.get(HOME_ZIP))	
			);
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CITY, 
					removeEscapedChars((String) m.get(HOME_CITY)))	
			);	
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_COUNTRY, 
					removeEscapedChars((String) m.get(HOME_COUNTRY)))	
			);	
		
		List phones = new ArrayList();
		IPhonenumber pn = null;
		if (m.containsKey(HOME_PHONE)) {
			pn = this.parsePhonenumber((String) m.get(HOME_PHONE));
			if (pn!=null) {
				phones.add(pn);
				attributes.add(
						PIMRuntime.getInstance().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), 
							IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE)	
					);	
			}
		}
		
		if (m.containsKey(FAX_PHONE)) {
			pn = this.parsePhonenumber((String) m.get(FAX_PHONE));
			if (pn!=null) {
				phones.add(pn);
				attributes.add(
						PIMRuntime.getInstance().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), 
							IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)	
					);					
			}
		}		

		if (m.containsKey(MOBILE_PHONE)) {
			pn = this.parsePhonenumber((String) m.get(MOBILE_PHONE));
			if (pn!=null) {
				phones.add(pn);
				attributes.add(
						PIMRuntime.getInstance().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), 
							IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)	
					);					
			}
		}	
		
		if (phones.size()==0) return null;
		

		ICaller c = PIMRuntime.getInstance().getCallerFactory().createCaller(null, phones, attributes);
		c.setUUID(c.getName().getLastname()+"_"+c.getName().getFirstname()+"_"+c.getPhoneNumber().getTelephoneNumber());
		IAttribute cm = PIMRuntime.getInstance().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
				ThunderbirdCallerManager.ID
			);
		c.setAttribute(cm);

		return c;
	}

	private ICaller buildWorkContact(Map m) {
		IAttributeMap attributes = PIMRuntime.getInstance().getCallerFactory().createAttributeMap();
		
		attributes.add(
			PIMRuntime.getInstance().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, 
				removeEscapedChars((String) m.get(FIRSTNAME)))	
		);
		
		attributes.add(
			PIMRuntime.getInstance().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_LASTNAME, 
				removeEscapedChars((String) m.get(LASTNAME)))	
		);
		
		attributes.add(
			PIMRuntime.getInstance().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_ADDITIONAL, 
				removeEscapedChars((String) m.get(COMPANY)))	
		);
		
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_STREET, 
					removeEscapedChars((String) m.get(WORK_STREET)))	
			);	
		
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, 
					(String) m.get(WORK_ZIP))	
			);
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CITY, 
					removeEscapedChars((String) m.get(WORK_CITY)))	
			);	
		attributes.add(
				PIMRuntime.getInstance().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_COUNTRY, 
					removeEscapedChars((String) m.get(WORK_COUNTRY)))	
			);	
		
		List phones = new ArrayList();
		IPhonenumber pn = null;
		if (m.containsKey(WORK_PHONE)) {
			pn = this.parsePhonenumber((String) m.get(WORK_PHONE));
			if (pn!=null) {
				phones.add(pn);
				attributes.add(
						PIMRuntime.getInstance().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), 
							IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE)	
					);	
				m.remove(WORK_PHONE);
			}
		}
		
		if (m.containsKey(FAX_PHONE)) {
			pn = this.parsePhonenumber((String) m.get(FAX_PHONE));
			if (pn!=null) {
				phones.add(pn);
				attributes.add(
						PIMRuntime.getInstance().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), 
							IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)	
					);	
				m.remove(FAX_PHONE);
			}
		}		

		if (m.containsKey(MOBILE_PHONE)) {
			pn = this.parsePhonenumber((String) m.get(MOBILE_PHONE));
			if (pn!=null) {
				phones.add(pn);
				attributes.add(
						PIMRuntime.getInstance().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), 
							IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)	
					);				
				m.remove(MOBILE_PHONE);
			}
		}	
		
		if (phones.size()==0) return null;

		ICaller c = PIMRuntime.getInstance().getCallerFactory().createCaller(null, phones, attributes);
		c.setUUID(c.getName().getLastname()+"_"+c.getName().getFirstname()+"_"+c.getPhoneNumber().getTelephoneNumber());
		IAttribute cm = PIMRuntime.getInstance().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
				ThunderbirdCallerManager.ID
			);
		c.setAttribute(cm);

		return c;
	}

	private IPhonenumber parsePhonenumber(String n) {
		Formatter f = Formatter.getInstance(PIMRuntime.getInstance());
		String normalizedNumber = f.normalizePhonenumber(n.trim());
		ICallerManager mgr = PIMRuntime.getInstance().getCallerManagerFactory()
				.getCallerManager("CountryDirectory");
		if (mgr != null && mgr instanceof IIdentifyCallerRepository) {
			ICaller c = null;
			try {
				c = ((IIdentifyCallerRepository) mgr)
						.getCaller(PIMRuntime.getInstance()
								.getCallerFactory()
								.createPhonenumber(normalizedNumber));
			} catch (CallerNotFoundException ex) {
				m_logger.warning("Normalized number "
						+ normalizedNumber + " not identified.");
			}

			if (c != null) {
				return c.getPhoneNumber();				
			} 
		}
		return null;
	}
	
	private boolean checkWork(Map m) {
		if (m.containsKey(ThunderbirdTransformer.WORK_PHONE) && ((String)m.get(ThunderbirdTransformer.WORK_PHONE)).length()>0) return true;
		if (m.containsKey(ThunderbirdTransformer.WORK_CITY)) return true;
		if (m.containsKey(ThunderbirdTransformer.WORK_COUNTRY)) return true;
		return false;
	}
	
	private boolean checkHome(Map m) {
		if (m.containsKey(ThunderbirdTransformer.HOME_PHONE) && ((String)m.get(ThunderbirdTransformer.HOME_PHONE)).length()>0) return true;
		if (m.containsKey(ThunderbirdTransformer.HOME_CITY)) return true;
		if (m.containsKey(ThunderbirdTransformer.HOME_COUNTRY)) return true;
		return false;
	}
	
	private boolean checkPhone(Map m) {
		if (m.containsKey(ThunderbirdTransformer.HOME_PHONE)) return true;
		if (m.containsKey(ThunderbirdTransformer.WORK_PHONE)) return true;
		if (m.containsKey(ThunderbirdTransformer.FAX_PHONE)) return true;
		if (m.containsKey(ThunderbirdTransformer.MOBILE_PHONE)) return true;
		if (m.containsKey(ThunderbirdTransformer.PAGER_PHONE)) return true;
		return false;
	}

	public int getCurrent() {
		return m_current;
	}
	public int getTotal() {
		return m_total;
	}

	
}
