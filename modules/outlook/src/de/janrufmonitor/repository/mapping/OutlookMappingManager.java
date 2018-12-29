package de.janrufmonitor.repository.mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.jacob.com.ComFailException;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.OutlookContactConst;
import de.janrufmonitor.repository.OutlookDate;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.uuid.UUID;

public class OutlookMappingManager {
	
	private class PhonenumberTypeComparator implements Comparator {

		IAttributeMap m_m;
		IOutlookMapping m_om;
				
		public PhonenumberTypeComparator(IAttributeMap m, IOutlookMapping om) {
			this.m_m = m;
			this.m_om = om;
		}
		
		public int compare(Object p1, Object p2) {
			if (this.m_m!=null) {
				if (p1 instanceof IPhonenumber && p2 instanceof IPhonenumber) {
					IAttribute a1 = this.m_m.get(IOutlookMapping.MAPPING_ATTTRIBUTE_ID+((IPhonenumber)p1).getTelephoneNumber());
					IAttribute a2 = this.m_m.get(IOutlookMapping.MAPPING_ATTTRIBUTE_ID+((IPhonenumber)p2).getTelephoneNumber());
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

	private static OutlookMappingManager m_instance = null;
	private Logger m_logger;
	private IRuntime m_runtime;
	
	private String ID = "OutlookCallerManager";
	
	private String MSO_IMAGE_CACHE_PATH = PathResolver.getInstance().getDataDirectory() +File.separator+ "photos" + File.separator + "msoutlook-contacts" +File.separator ;
	private Formatter m_f;


	private OutlookMappingManager() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
	}
	
	public static synchronized OutlookMappingManager getInstance() {
		if (OutlookMappingManager.m_instance == null) {
			OutlookMappingManager.m_instance = new OutlookMappingManager();
		}
		return OutlookMappingManager.m_instance;
	}
		
	public ICaller mapToJamCaller(Dispatch oCaller, IOutlookMapping om) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("Applying mapping: "+om.toString());
		}
		
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		
		// processing the numbers
		List outlookNumberMappings = om.getSupportedNumbers();
		List phones = new ArrayList(outlookNumberMappings.size());
		String numbertype = null;
		String number = null;
		IPhonenumber phone = null;
		for (int i=0,j=outlookNumberMappings.size();i<j;i++) {
			numbertype = (String) outlookNumberMappings.get(i);
			number = Dispatch.get(oCaller, numbertype).toString().trim();
			if (number !=null && !PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(number) && !PhonenumberAnalyzer.getInstance(getRuntime()).isClired(number)) {
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("OutlookCallerManager raw number: "+number);
				}
				phone = PhonenumberAnalyzer.getInstance(getRuntime()).toIdentifiedPhonenumber(number);
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("OutlookCallerManager identified number: "+phone);
				}
				if (phone.getTelephoneNumber().trim().length()>0 && !phone.isClired()) {
					m.add(getNumberTypeAttribute(numbertype, phone, om));
					m.add(om.createOutlookNumberTypeAttribute(phone, numbertype));
					phones.add(phone);
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("Added phone "+phone.toString());
					}
				}
			}
			else if (number !=null && (number.length()> 0 && PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(number))) {
				// found internal number
				phone = getRuntime().getCallerFactory().createInternalPhonenumber(number);
				if (phone.getTelephoneNumber().trim().length()>0 && !phone.isClired()) {
					m.add(getNumberTypeAttribute(numbertype, phone, om));
					m.add(om.createOutlookNumberTypeAttribute(phone, numbertype));
					phones.add(phone);
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("Added internal phone "+phone.toString());
					}
				}
			}
		}
		
		if (phones.size()>0) {
			if (phones.size()>1)
				Collections.sort(phones, new PhonenumberTypeComparator(m, om));
			
			// process address data
			List outlookContacFieldMappings = om.getSupportedContactFields();
			String field = null;
			String jamField = null;
			IAttribute a = null;
			for (int i=0,j=outlookContacFieldMappings.size();i<j;i++) {
				field = (String) outlookContacFieldMappings.get(i);
				jamField = om.mapToJamField(field); 
				if (jamField!=null) {
					a = createAttribute(jamField,
							Dispatch.get(oCaller, field).toString().trim());
				
					if (a!=null) {
						m.add(a);
						if (this.m_logger.isLoggable(Level.INFO)) {
							this.m_logger.info("Added attribute "+a.toString());
						}
						a = om.createOutlookContactFieldAttribute(field);
						if (a!=null)
							m.add(a);
					}
				}
			}		
			
			a = createAttribute(IJAMConst.ATTRIBUTE_NAME_CREATION,
					Long.toString(new OutlookDate(Dispatch.get(oCaller, "CreationTime").getDate()).getTime()));
		
			if (a!=null) m.add(a);
			
			a = 
				createAttribute(IJAMConst.ATTRIBUTE_NAME_MODIFIED,
						Long.toString(new OutlookDate(Dispatch.get(oCaller, "LastModificationTime").getDate()).getTime()));
			
			if (a!=null) m.add(a);
	
			a = 
				createAttribute("outlook.uuid",
						Dispatch.get(oCaller, OutlookContactConst.User1).toString());
			
			if (a!=null) m.add(a);
			
			String geodata = null;
			if (om.getSupportedContactFields().contains(OutlookContactConst.User3)) {
				geodata = Dispatch.get(oCaller, OutlookContactConst.User3).toString();
			}
			
			if (om.getSupportedContactFields().contains(OutlookContactConst.User4)) {
				geodata = Dispatch.get(oCaller, OutlookContactConst.User4).toString();
			}
			
			if (geodata!=null && geodata.trim().length()>0) {
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("Found geodata: "+geodata);
				}
				String[] values = geodata.split(",");
				if (values.length==3) {
					a = 
						createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_ACC,
								values[0]);					
					if (a!=null) m.add(a);
					a = 
						createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG,
								values[1]);					
					if (a!=null) m.add(a);
					a = 
						createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT,
								values[2]);					
					if (a!=null) m.add(a);
				}
			}
			
			
			IAttributeMap additionalMap = om.getSpecialAttributes();
			if (additionalMap!=null) {
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("Adding special attributes: "+additionalMap.toString());
				}
				m.addAll(additionalMap);
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
		
			ICaller outlookCaller = getRuntime().getCallerFactory().createCaller(null, phones, m);
			String uuid = Dispatch.get(oCaller, OutlookContactConst.User1).toString().trim();
			if (uuid!=null && uuid.length()>0)
				outlookCaller.setUUID(uuid);
			else
				//outlookCaller.setUUID(outlookCaller.getName().getLastname()+"_"+outlookCaller.getName().getFirstname()+"_"+outlookCaller.getPhoneNumber().getTelephoneNumber());
				outlookCaller.setUUID(new UUID().toString());
	
			this.setPictureAttribute(outlookCaller, oCaller);

			IAttribute cm = getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
				ID
			);
			outlookCaller.setAttribute(cm);
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("Created Outlook contact: "+outlookCaller.toString());
			}
			return outlookCaller;
		}
		
		return null;
	}
	
	public void mapToOutlookCaller(Dispatch oCaller, ICaller jamCaller, IOutlookMapping om) {
		if (this.m_logger.isLoggable(Level.INFO)) {
			this.m_logger.info("Appliing mapping: "+om.toString());
		}
		
		// clear all supported fields in outlook
		List outlookContacFieldMappings = om.getSupportedContactFields();
		String field = null;
		for (int i=0,j=outlookContacFieldMappings.size();i<j;i++) {
			field = (String) outlookContacFieldMappings.get(i);
			Dispatch.put(oCaller, field, "");
		}		
		
		// clear all supported numbers in outlook
		List outlookNumberMappings = om.getSupportedNumbers();
		String numbertype = null;
		for (int i=0,j=outlookNumberMappings.size();i<j;i++) {
			numbertype = (String) outlookNumberMappings.get(i);
			Dispatch.put(oCaller, numbertype, "");
		}				
		
		// set address data
		Dispatch.put(oCaller, OutlookContactConst.User1, jamCaller.getUUID());
		Dispatch.put(oCaller, OutlookContactConst.User2, jamCaller.getPhoneNumber().getIntAreaCode()+jamCaller.getPhoneNumber().getAreaCode());
		
		String jamField = null;
		IAttribute a = null;
		for (int i=0,j=outlookContacFieldMappings.size();i<j;i++) {
			field = (String) outlookContacFieldMappings.get(i);
			jamField = om.mapToJamField(field);
			if (jamField!=null) {
				a = jamCaller.getAttribute(jamField);
				if (a!=null) {
					// TODO: 2008/08/13 - Hack for street no
					if (a.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_STREET)) {
						if (jamCaller.getAttributes().contains((IJAMConst.ATTRIBUTE_NAME_STREET_NO))) {
							Dispatch.put(oCaller, field, a.getValue()+" "+jamCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_STREET_NO).getValue());
						} else {
							Dispatch.put(oCaller, field, a.getValue());
						}
					} else if (a.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_GEO_ACC) || a.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_GEO_LNG) || a.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_GEO_LAT)) {
						 StringBuffer geodata = new StringBuffer();
						 geodata.append(jamCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_ACC).getValue());
						 geodata.append(",");
						 geodata.append(jamCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue());
						 geodata.append(",");
						 geodata.append(jamCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue());

						 Dispatch.put(oCaller, field, geodata.toString());
					} else {
						Dispatch.put(oCaller, field, a.getValue());
					}					
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("Setting attribute: "+a.toString());
					}
				}
			}			
		}
		
		// set phone numbers		
		if (jamCaller instanceof IMultiPhoneCaller) {
			List pns = ((IMultiPhoneCaller)jamCaller).getPhonenumbers();
			IPhonenumber pn = null;
			IAttributeMap m = jamCaller.getAttributes();
			for (int k=0;k<pns.size();k++) {
				pn = (IPhonenumber) pns.get(k);
				this.setContactPhone(oCaller, pn, m, om);
			}
		} else {
			this.setContactPhone(oCaller, jamCaller.getPhoneNumber(), jamCaller.getAttributes(), om);
		}
		
		Dispatch.call(oCaller, "RemovePicture");
		if (this.getAttribute(jamCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)).length()>0) {
			try {
				Dispatch.call(oCaller, "AddPicture", PathResolver.getInstance().resolve(this.getAttribute(jamCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH))));	
			} catch (ComFailException e) {
				this.m_logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
		Dispatch.call(oCaller, "Save");
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	private IAttribute getNumberTypeAttribute(String outlookNumberType, IPhonenumber pn, IOutlookMapping om) {
		return getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), om.mapToJamNumberType(outlookNumberType));
	}
	
	@SuppressWarnings("deprecation")
	private void setPictureAttribute(ICaller outlookCaller, Dispatch contact) {
		try {			
			if (Dispatch.get(contact, "HasPicture").getBoolean()) {		
				String imagepath = MSO_IMAGE_CACHE_PATH +outlookCaller.getUUID() + ".jpg";
				if (!new File(imagepath).exists()) {
					Dispatch items = Dispatch.get(contact, "Attachments").toDispatch();
					int count = Dispatch.get(items, "Count").toInt();
					if (count > 1)
						this.m_logger.info("Found "+count+" attachments for outlook contact "+outlookCaller.toString());
					
					for (int i=1; i<=count; i++) {
						Dispatch item = Dispatch.call(items, "Item", new Integer(i)).toDispatch();
						
						int type = Dispatch.get(item, "Type").toInt();
						// type 1 olAttachmentType = olByValue
						if (type==1) {
							try {
								String outlookFilename = Dispatch.get(item, "FileName").toString();
								if (outlookFilename!=null && (outlookFilename.startsWith("ContactPicture") || outlookFilename.startsWith("ContactPhoto"))) {
									Dispatch.call(item, "SaveAsFile", new String(imagepath));										
								}
							} catch (ComFailException ex) {
								this.m_logger.warning("Could not get FileName attribute: "+ex.getMessage() + ", " + ex.getSource());
							}
						}
						if (item!=null)
							item.safeRelease();
					}
					if (items!=null)
						items.safeRelease();
				}
				IAttribute img = getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_IMAGEPATH,
					PathResolver.getInstance().encode(imagepath)
				);
				outlookCaller.setAttribute(img);
			}
		} catch (ComFailException ex) {
			this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());
		}
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

	private String getAttribute(IAttribute a) {
		if (a!=null && a.getValue()!=null) {
			return a.getValue();
		}
		return "";
	}
	
	private Formatter getFormatter() {
		if (this.m_f==null) {
			this.m_f = Formatter.getInstance(PIMRuntime.getInstance());
		}
		return this.m_f;
	}
	
	private boolean isPhoneAlreadyInUse(Dispatch contact, String phone) {
		Variant p = Dispatch.get(contact, phone); 
		if (p!=null) {
			return p.toString().length()>0;
		}
		return false;
	}
	
	private void setContactPhone(Dispatch oCaller, IPhonenumber pn, IAttributeMap m, IOutlookMapping om) {
		if (pn==null || pn.isClired()) return;
		String number = getFormatter().parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, pn);
		
		IAttribute numbertypeMappingAttribute = m.get(IOutlookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
		String outlookNumberType = null;
		if (numbertypeMappingAttribute!=null) {
			outlookNumberType = numbertypeMappingAttribute.getValue();
		} else {
			this.m_logger.info("No number type mapping found for number: "+number);
			// check for jam number type
			IAttribute jamNumberTypeAttribute = m.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber());
			String[] outlookNumbertypePriority = null;
			if (jamNumberTypeAttribute==null) {
				this.m_logger.info("No JAM number type found for number: "+number);
				outlookNumbertypePriority = om.getPriorityOrder(null);
			} else {
				outlookNumbertypePriority = om.getPriorityOrder(jamNumberTypeAttribute.getValue());
			}
			
			// check if a outlolok phone field is empty to store the number in
			for (int i = 0, j=outlookNumbertypePriority.length;i<j;i++) {
				if (!this.isPhoneAlreadyInUse(oCaller, outlookNumbertypePriority[i])) {
					outlookNumberType = outlookNumbertypePriority[i];
					break;
				}
			}
			if (outlookNumberType==null) {
				this.m_logger.warning("No empty phone field found in outlook for "+number);
			}
		}
		Dispatch.put(oCaller, outlookNumberType, number);
	}
}
