package de.janrufmonitor.repository.mapping;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.macab.MacAddressBookProxy;
import de.janrufmonitor.repository.IMacAddressBookConst;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IModifierService;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.uuid.UUID;

public class MacAddressBookMappingManager {

	private class PhonenumberTypeComparator implements Comparator {

		IAttributeMap m_m;
		IMacAddressBookMapping m_om;
				
		public PhonenumberTypeComparator(IAttributeMap m, IMacAddressBookMapping om) {
			this.m_m = m;
			this.m_om = om;
		}
		
		public int compare(Object p1, Object p2) {
			if (this.m_m!=null) {
				if (p1 instanceof IPhonenumber && p2 instanceof IPhonenumber) {
					IAttribute a1 = this.m_m.get(IMacAddressBookMapping.MAPPING_ATTTRIBUTE_ID+((IPhonenumber)p1).getTelephoneNumber());
					IAttribute a2 = this.m_m.get(IMacAddressBookMapping.MAPPING_ATTTRIBUTE_ID+((IPhonenumber)p2).getTelephoneNumber());
					if (a1==null) return 1;
					if (a2==null) return -1;

					String[] prio = this.m_om.getPriorityOrder(null);
					for (int i=0;i<prio.length;i++) {
						if (a1.getValue().equalsIgnoreCase(prio[i])) return -1;
						if (a2.getValue().equalsIgnoreCase(prio[i])) return 1;
					}				
				}
			}
			return 0;
		}
		
	}
	
	private static MacAddressBookMappingManager m_instance = null;
	private Logger m_logger;
	private IRuntime m_runtime;
	
	private String ID = "MacAddressBookManager";
	
	private String MSO_IMAGE_CACHE_PATH = PathResolver.getInstance().getDataDirectory() +File.separator+ "photos" + File.separator + "macab-contacts" +File.separator ;

	private MacAddressBookMappingManager() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
	}
	
	public static synchronized MacAddressBookMappingManager getInstance() {
		if (MacAddressBookMappingManager.m_instance == null) {
			MacAddressBookMappingManager.m_instance = new MacAddressBookMappingManager();
		}
		return MacAddressBookMappingManager.m_instance;
	}
		
	@SuppressWarnings("unchecked")
	public synchronized ICaller mapToJamCaller(Map<?,?> oCaller, IMacAddressBookMapping om) {
		if (!oCaller.containsKey(IMacAddressBookConst.PHONE)) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("Mac Address Book entry has no phone numbers: "+oCaller);
			}
			return null;
		}
		
		if (((List)oCaller.get(IMacAddressBookConst.PHONE)).size()==0) {
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("Mac Address Book entry phone numbers are empty: "+oCaller);
			}
			return null;
		}
		
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("Appliing mapping: "+om.toString());
		}
		
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		
		// processing the numbers
		List macNumberMappings = om.getSupportedNumbers();
		
		// added 2015/05/03: added generic etiketts 
		if (macNumberMappings.contains("*")) {
			macNumberMappings.addAll(this.getGenericMappings(((List)oCaller.get(IMacAddressBookConst.PHONE))));
		}
		
		List phones = new ArrayList(macNumberMappings.size());
		String numbertype = null;
		String number = null;
		IPhonenumber phone = null;
		for (int i=0,j=macNumberMappings.size();i<j;i++) {
			numbertype = (String) macNumberMappings.get(i);
			while ((number = getRawNumber(((List)oCaller.get(IMacAddressBookConst.PHONE)), numbertype))!=null){
				if (number !=null && number.length()> maxInternalNumberLength()) {
					String nnumber = PhonenumberAnalyzer.getInstance().normalize(number);
					
					// added 2010/03/03 still contains special chars, so it must be internal
					if (PhonenumberAnalyzer.getInstance().containsSpecialChars(nnumber.trim())) {
						phone = getRuntime().getCallerFactory().createInternalPhonenumber(number);
						if (phone.getTelephoneNumber().trim().length()>0 && !phone.isClired()) {
							m.add(getNumberTypeAttribute(numbertype, phone, om));
							m.add(om.createMacAddressBookNumberTypeAttribute(phone, numbertype));
							phones.add(phone);
							if (this.m_logger.isLoggable(Level.INFO)) {
								this.m_logger.info("Added long internal phone "+phone.toString());
							}
						}
					} else {				
						phone = getRuntime().getCallerFactory().createPhonenumber(nnumber);
						ICaller c = Identifier.identifyDefault(getRuntime(), phone);
						if (c!=null) {
							phone = c.getPhoneNumber();
							if (phone.getTelephoneNumber().trim().length()>0 && !phone.isClired()) {
								m.add(getNumberTypeAttribute(numbertype, phone, om));
								m.add(om.createMacAddressBookNumberTypeAttribute(phone, numbertype));
								phones.add(phone);
								if (this.m_logger.isLoggable(Level.INFO)) {
									this.m_logger.info("Added phone "+phone.toString());
								}
							}
						} 
					}
				}
				else if (number !=null && (number.length()> 0 && number.length()<=maxInternalNumberLength())) {
					// found internal number
					phone = getRuntime().getCallerFactory().createInternalPhonenumber(number);
					if (phone.getTelephoneNumber().trim().length()>0 && !phone.isClired()) {
						m.add(getNumberTypeAttribute(numbertype, phone, om));
						m.add(om.createMacAddressBookNumberTypeAttribute(phone, numbertype));
						phones.add(phone);
						if (this.m_logger.isLoggable(Level.INFO)) {
							this.m_logger.info("Added internal phone "+phone.toString());
						}
					}
				}
			}
		}
		
		if (phones.size()>0) {
			if (phones.size()>1)
				Collections.sort(phones, new PhonenumberTypeComparator(m, om));
			
			// process address data
			List macContacFieldMappings = om.getSupportedContactFields();
			String field = null;
			String jamField = null;
			IAttribute a = null;
			if (oCaller.containsKey(IMacAddressBookAddressMapping.ADDRESS)) {
				for (int i=0,j=macContacFieldMappings.size();i<j;i++) {
					field = (String) macContacFieldMappings.get(i);
					jamField = om.mapToJamField(field); 
					if (jamField!=null) {
						a = createAttribute(jamField,
								this.getRawAddress((List) oCaller.get(IMacAddressBookAddressMapping.ADDRESS), om.getSupportedAddressType(), field));
					
						if (a!=null) {
							m.add(a);
							if (this.m_logger.isLoggable(Level.INFO)) {
								this.m_logger.info("Added attribute "+a.toString());
							}
						}
					}
				}
			}
			
			if (oCaller.containsKey(IMacAddressBookAddressMapping.EMAIL)) {
				jamField = om.mapToJamField(IMacAddressBookAddressMapping.EMAIL); 
				a = createAttribute(jamField,
						this.getRawEmail((List) oCaller.get(IMacAddressBookAddressMapping.EMAIL), om.getSupportedEmailType()));
					
				if (a!=null) {
					m.add(a);
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("Added attribute "+a.toString());
					}
				}			
			}
			
			for (int i=0,j=macContacFieldMappings.size();i<j;i++) {
				field = (String) macContacFieldMappings.get(i);
				jamField = om.mapToJamField(field); 
				if (jamField!=null) {
					a = createAttribute(jamField, (String) oCaller.get(field));
				
					if (a!=null) {
						m.add(a);
						if (this.m_logger.isLoggable(Level.INFO)) {
							this.m_logger.info("Added attribute "+a.toString());
						}
					}
				}
			}
			
			// date format 2010-07-22 15:34:45 +0200
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			
			if (oCaller.containsKey(IMacAddressBookConst.CREATION)) {
				try {
					a = createAttribute(IJAMConst.ATTRIBUTE_NAME_CREATION,
							Long.toString(sdf.parse((String) oCaller.get(IMacAddressBookConst.CREATION)).getTime()));
				} catch (ParseException e) {
					this.m_logger.warning("Could not parse creation date: "+oCaller.get(IMacAddressBookConst.CREATION));
				}
			
				if (a!=null) m.add(a);
			}
				
			if (oCaller.containsKey(IMacAddressBookConst.MODIFICATION)) {
				try {
					a = createAttribute(IJAMConst.ATTRIBUTE_NAME_MODIFIED,
							Long.toString(sdf.parse((String) oCaller.get(IMacAddressBookConst.MODIFICATION)).getTime()));
				} catch (ParseException e) {
					this.m_logger.warning("Could not parse modification date: "+oCaller.get(IMacAddressBookConst.MODIFICATION));
				}
			
				if (a!=null) m.add(a);
			}
			
			if (oCaller.containsKey(IMacAddressBookConst.PARENT_GROUPS)) {
				List categories = (List) oCaller.get(IMacAddressBookConst.PARENT_GROUPS);
				if (categories.size()>0) {
					a = createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY,
							MacAddressBookProxy.getInstance().getCategory((String) categories.get(0)));

					if (a!=null) m.add(a);
				}
			}

			
			// TODO: 2008/08/13 - Hack - split up street and street no
			IAttribute street = m.get(IJAMConst.ATTRIBUTE_NAME_STREET);
			if (street != null && street.getValue().trim().length()>0) {
				String[] streetSplit = street.getValue().trim().split(" ");
				if (streetSplit.length>1) {
					street.setValue("");
					for (int i=0;i<streetSplit.length-1;i++) {
						street.setValue(street.getValue()+ " "+streetSplit[i]);
					}
					street.setValue(street.getValue().trim());
					m.add(street);
					IAttribute streetno = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET_NO, streetSplit[streetSplit.length-1]);
					m.add(streetno);
				}
			}
			
			String uuid = (String) oCaller.get(IMacAddressBookConst.UID);
			if (uuid==null || uuid.trim().length()==0)
				uuid = new UUID().toString();
			
			try {
					MacAddressBookProxy.getInstance().getDataHandler().deleteAttributes(uuid);
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
			
			ICaller macCaller = getRuntime().getCallerFactory().createCaller(uuid, null, phones, m);
	
			this.setPictureAttribute(macCaller, oCaller);
			this.setGeoData(macCaller);

			IAttribute cm = getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
				ID
			);
			macCaller.setAttribute(cm);
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("Created Mac address book contact: "+macCaller.toString());
			}
			try {
				if (macCaller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME))
					MacAddressBookProxy.getInstance().getDataHandler().insertAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_LASTNAME, macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue());
				if (macCaller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_CITY))
					MacAddressBookProxy.getInstance().getDataHandler().insertAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_CITY, macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_CITY).getValue());
				if (macCaller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_COUNTRY))
					MacAddressBookProxy.getInstance().getDataHandler().insertAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_COUNTRY, macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue());
				if (macCaller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE))
					MacAddressBookProxy.getInstance().getDataHandler().insertAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue());
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
			return macCaller;
		}
		
		return null;
	}
	
	private void setGeoData(ICaller macCaller) {
		// check cache
		try {
			String LNG = MacAddressBookProxy.getInstance().getDataHandler().selectAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_GEO_LNG);
			String LAT = MacAddressBookProxy.getInstance().getDataHandler().selectAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_GEO_LAT);
			String ACC = MacAddressBookProxy.getInstance().getDataHandler().selectAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_GEO_ACC);
			IAttribute a = null;
			if (LNG!=null && LNG.length()>0 && LAT!=null && LAT.length()>0 ) {
				a = 
				createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG,
						LNG);					
				if (a!=null) macCaller.getAttributes().add(a);
				a = 
					createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT,
							LAT);					
				if (a!=null) macCaller.getAttributes().add(a);
				if (ACC!=null && ACC.length()>0) {
					a = 
					createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_ACC,
							ACC);					
					if (a!=null) macCaller.getAttributes().add(a);
				}
				return;
			}
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

		
		List msvc = getRuntime().getServiceFactory().getModifierServices();
		IModifierService s = null;
		for (int k=0,l=msvc.size();k<l;k++) {
			s = (IModifierService) msvc.get(k);
			if (s!=null && s.getServiceID().equalsIgnoreCase("GeoCoding")  && s.isEnabled()) {
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Processing modifier service <"+s.getServiceID()+">");
				s.modifyObject(macCaller);	
				
				if (macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)!= null && 
					macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT)!= null && 
					macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_ACC)!= null 
				) {
					try {
						MacAddressBookProxy.getInstance().getDataHandler().insertAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_GEO_LNG, macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue());
						MacAddressBookProxy.getInstance().getDataHandler().insertAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_GEO_LAT, macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue());
						MacAddressBookProxy.getInstance().getDataHandler().insertAttribute(macCaller.getUUID(), IJAMConst.ATTRIBUTE_NAME_GEO_ACC, macCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_ACC).getValue());
					} catch (SQLException e) {
						this.m_logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}
			}
		}
	}

	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	private IAttribute getNumberTypeAttribute(String outlookNumberType, IPhonenumber pn, IMacAddressBookMapping om) {
		return getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), om.mapToJamNumberType(outlookNumberType));
	}

	private IAttribute createAttribute(String name, String value) {
		if (name!=null && name.length()>0 && value!=null && value.length()>0) {
			return getRuntime().getCallerFactory().createAttribute(
						name,
						value
					);
		}
		return null;
	}
	
	private int maxInternalNumberLength() {
		String value = this.getRuntime().getConfigManagerFactory()
				.getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTERNAL_LENGTH);
		if (value != null && value.length() > 0) {
			try {
				return Integer.parseInt(value);
			} catch (Exception ex) {
				this.m_logger.warning(ex.getMessage());
			}
		}
		return 0;
	}
	
	private List getGenericMappings(List numbers) {
		List allMappings = new ArrayList();
		for (Object entry : numbers) {
			if (entry instanceof Map <?,?>) {
				Iterator iter = ((Map)entry).keySet().iterator();
				while (iter.hasNext()) {
					allMappings.add(iter.next());
				}
			}
		}
		return allMappings;
	}

	private String getRawNumber(List numbers, String numbertype) {
		for (Object entry : numbers) {
			if (entry instanceof Map <?,?>) {
				if (((Map)entry).containsKey(numbertype)) {
					// 2010/11/12: added .remove() method instead of get()
					return ((String) ((Map)entry).remove(numbertype)).trim();
				}
			}
		}
		return null;
	}
	
	private String getRawAddress(List address, String type, String field) {
		for (Object entry : address) {
			if (entry instanceof Map <?,?> && ((Map) entry).containsKey(type)) {
				Map addressdata = ((Map)((Map)entry).get(type));
				if (addressdata.containsKey(field)) {
					return ((String) addressdata.get(field)).trim();
				}
			}
		}
		return null;
	}
	
	private String getRawEmail(List mails, String type) {
		for (Object entry : mails) {
			if (entry instanceof Map <?,?> && ((Map) entry).containsKey(type)) {
				return ((String)((Map)entry).get(type));
			}
		}
		return null;
	}
	
	private void setPictureAttribute(ICaller macCaller, Map contact) {
		if (contact.containsKey(IMacAddressBookConst.HAS_PICTURE)) {
			long pic = ((Long)contact.get(IMacAddressBookConst.HAS_PICTURE)).longValue();
			if (pic==1) {
				// picture is assigned
				byte[] imgdata = MacAddressBookProxy.getInstance().getUserImage((String) contact.get(IMacAddressBookConst.UID));
				if (imgdata!=null) {
					String imagepath = MSO_IMAGE_CACHE_PATH +macCaller.getUUID() + ".jpg";
					
					File f = new File(imagepath);
					f.getParentFile().mkdirs();

					try {
						FileOutputStream fos = new FileOutputStream(f);
						ByteArrayInputStream bin = new ByteArrayInputStream(imgdata);
						Stream.copy(bin, fos, true);
						
						IAttribute img = getRuntime().getCallerFactory().createAttribute(
								IJAMConst.ATTRIBUTE_NAME_IMAGEPATH,
								PathResolver.getInstance().encode(imagepath)
						);
						macCaller.setAttribute(img);
						return;
					} catch (FileNotFoundException e) {
						this.m_logger.log(Level.WARNING, e.toString(), e);
					} catch (IOException e) {
						this.m_logger.log(Level.WARNING, e.toString(), e);
					}

				}
			}
		}
	}

	
}
