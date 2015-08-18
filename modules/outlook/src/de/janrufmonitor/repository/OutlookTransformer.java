package de.janrufmonitor.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.jacob.com.ComFailException;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;

public class OutlookTransformer {
	private String[] businessPhones = {"BusinessTelephoneNumber", "Business2TelephoneNumber", "BusinessFaxNumber"};
	private String[] privatePhones = {"HomeTelephoneNumber", "Home2TelephoneNumber", "HomeFaxNumber", "ISDNNumber", "MobileTelephoneNumber", "OtherTelephoneNumber", "OtherFaxNumber", "TelexNumber"};

	private Logger m_logger;
	
	private String MSO_IMAGE_CACHE_PATH = PathResolver.getInstance().getPhotoDirectory() + File.separator + "msoutlook-contacts" +File.separator +"imported"+ File.separator;
	private IRuntime m_runtime;
	private String ID = "OutlookCallerManager";
	private String NAMESPACE = "repository.OutlookCallerManager";
	
	public OutlookTransformer() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		File cache = new File(MSO_IMAGE_CACHE_PATH);
		if (!cache.exists()) cache.mkdirs();
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
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	private IAttributeMap createPrivateAddressAttributes(Dispatch contact) throws ComFailException {
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		
		IAttribute attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_CITY,
					Dispatch.get(contact, "HomeAddressCity").toString().trim());
		
		if (attribute!=null) m.add(attribute);
		
		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE,
					Dispatch.get(contact, "HomeAddressPostalCode").toString().trim());
		
		if (attribute!=null) m.add(attribute);

		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET,
					Dispatch.get(contact, "HomeAddressStreet").toString().trim());
		
		if (attribute!=null) m.add(attribute);
		

		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY,
					Dispatch.get(contact, "HomeAddressCountry").toString().trim());
		
		if (attribute!=null) m.add(attribute);

		return m;
	}
	
	private IAttributeMap createBusinessAddressAttributes(Dispatch contact) throws ComFailException {
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		
		IAttribute attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_CITY,
					Dispatch.get(contact, "BusinessAddressCity").toString().trim());
		
		if (attribute!=null) m.add(attribute);
		
		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE,
					Dispatch.get(contact, "BusinessAddressPostalCode").toString().trim());
		
		if (attribute!=null) m.add(attribute);

		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET,
					Dispatch.get(contact, "BusinessAddressStreet").toString().trim());
		
		if (attribute!=null) m.add(attribute);

		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET_NO,
					Dispatch.get(contact, "BusinessAddressPostOfficeBox").toString().trim());
		
		if (attribute!=null) m.add(attribute);				
			
		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY,
					Dispatch.get(contact, "BusinessAddressCountry").toString().trim());
		
		if (attribute!=null) m.add(attribute);				
			
		return m;
	}
	
	private IAttribute getNumberTypeAttribute(String outlookType, String number) {
		IAttribute type = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+number, "");
		
		if (outlookType.equalsIgnoreCase("BusinessTelephoneNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		if (outlookType.equalsIgnoreCase("Business2TelephoneNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		if (outlookType.equalsIgnoreCase("BusinessFaxNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE);
		
		if (outlookType.equalsIgnoreCase("HomeTelephoneNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		if (outlookType.equalsIgnoreCase("Home2TelephoneNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		if (outlookType.equalsIgnoreCase("ISDNNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		if (outlookType.equalsIgnoreCase("OtherTelephoneNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		if (outlookType.equalsIgnoreCase("TelexNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		if (outlookType.equalsIgnoreCase("OtherFaxNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE);
		if (outlookType.equalsIgnoreCase("HomeFaxNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE);
		if (outlookType.equalsIgnoreCase("MobileTelephoneNumber")) type.setValue(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE);

		return type;
	}
	
	private ICallerList createPrivateCallerList(Dispatch contact) throws ComFailException {
		ICallerList callerList = getRuntime().getCallerFactory().createCallerList();
	
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		
		IAttribute attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
					Dispatch.get(contact, "Firstname").toString().trim());
		
		if (attribute!=null) m.add(attribute);
		
		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME,
					Dispatch.get(contact, "Lastname").toString().trim());
		
		if (attribute!=null) m.add(attribute);
		
		// check if private caller exists
		if (m.size()>0) {
			m.addAll(createPrivateAddressAttributes(contact));

			List phones = new ArrayList(businessPhones.length);
			
			IPhonenumber phone = getRuntime().getCallerFactory().createPhonenumber(false);
			String number = null;
			for (int i = 0; i < privatePhones.length; i++) {
				number = Dispatch.get(contact, privatePhones[i]).toString().trim();
				if (number !=null && number.length()>0) {
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("OutlookTransformer raw number: "+number);
					}
					phone = PhonenumberAnalyzer.getInstance(getRuntime()).toIdentifiedPhonenumber(number);
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("OutlookTransformer identified number: "+phone);
					}
					if (phone!=null) {
						if (phone.getTelephoneNumber().trim().length()>0 && !phone.isClired()) {
							m.add(getNumberTypeAttribute(privatePhones[i], phone.getTelephoneNumber()));
							phones.add(phone);
						}
					}
				}
			}
			
			if (phones.size()>0) {
				ICaller outlookCaller = getRuntime().getCallerFactory().createCaller(null, phones, m);
				outlookCaller.setUUID(outlookCaller.getName().getLastname()+"_"+outlookCaller.getName().getFirstname()+"_"+outlookCaller.getPhoneNumber().getTelephoneNumber());
		
				this.setPictureAttribute(outlookCaller, contact);

				IAttribute cm = getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
					ID
				);
				outlookCaller.setAttribute(cm);
				this.m_logger.fine("Created Outlook contact: "+outlookCaller.toString());
				callerList.add(outlookCaller);	
			}
		}
		
		return callerList;
	}
	
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
									//imagepath = imagepath.substring(0, imagepath.length()-4) + (i>1? Integer.toString(i) : "") + ".jpg";
									Dispatch.call(item, "SaveAsFile", new String(imagepath));										
								}
							} catch (ComFailException ex) {
								this.m_logger.warning("Could not get FileName attribute: "+ex.getMessage() + ", " + ex.getSource());
							}
						}
						
					}
					
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
	
	private ICallerList createBusinessCallerList(Dispatch contact) throws ComFailException {
		ICallerList callerList = getRuntime().getCallerFactory().createCallerList();
		
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		
		IAttribute attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
					Dispatch.get(contact, "Firstname").toString().trim());
		
		if (attribute!=null) m.add(attribute);
		
		attribute = 
			createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME,
					Dispatch.get(contact, "Lastname").toString().trim());
		
		if (attribute!=null) m.add(attribute);
	
		if (m.size()==0) {				
			attribute = 
				createAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
						Dispatch.get(contact, "CompanyName").toString().trim());
			
			if (attribute!=null) m.add(attribute);
		} else {
			attribute = 
				createAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL,
						Dispatch.get(contact, "CompanyName").toString().trim());
			
			if (attribute!=null) m.add(attribute);
		}

		
		// check if business caller exists
		if (m.size()>0) {
			m.addAll(createBusinessAddressAttributes(contact));
			
			List phones = new ArrayList(businessPhones.length);
			
			IPhonenumber phone = getRuntime().getCallerFactory().createPhonenumber(false);
			String number = null;
			for (int i = 0; i < businessPhones.length; i++) {
				number = Dispatch.get(contact, businessPhones[i]).toString().trim();
				if (number !=null && number.length()>0) {
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("OutlookTransformer raw number: "+number);
					}
					phone = PhonenumberAnalyzer.getInstance(getRuntime()).toIdentifiedPhonenumber(number);
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("OutlookTransformer identified number: "+phone);
					}
					if (phone!=null) {
						if (phone.getTelephoneNumber().trim().length()>0 && !phone.isClired()) {
							m.add(getNumberTypeAttribute(businessPhones[i], phone.getTelephoneNumber()));
							phones.add(phone);
						}
					}
				}
			}
			
			if (phones.size()>0) {					
				ICaller outlookCaller = getRuntime().getCallerFactory().createCaller(null, phones, m);
				//outlookCaller.setAttributes(m);
				outlookCaller.setUUID(outlookCaller.getName().getLastname()+"_"+outlookCaller.getName().getFirstname()+"_"+outlookCaller.getPhoneNumber().getTelephoneNumber());
		
				this.setPictureAttribute(outlookCaller, contact);
				
				IAttribute cm = getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
					ID
				);
				outlookCaller.setAttribute(cm);
				this.m_logger.fine("Created Outlook contact: "+outlookCaller.toString());
				callerList.add(outlookCaller);
			}

		}
		
		return callerList;
	}
	
	private ICallerList getCallerListFromSingleContact(Dispatch contact) throws ComFailException {
		ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		try {
			Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
			if (config.getProperty("split", "true").equalsIgnoreCase("true")) {
				callers.add(createPrivateCallerList(contact));
				callers.add(createBusinessCallerList(contact));
			} else {
				ICaller c = null;
				ICallerList cl = createPrivateCallerList(contact);
				if (cl.size()>0) {
					if (cl.size()>1) {
						c = cl.get(0);
						for (int i=0;i<cl.size();i++) {
							((IMultiPhoneCaller)c).getPhonenumbers().addAll(((IMultiPhoneCaller)cl.get(i)).getPhonenumbers());
						}
					} else {
						c = cl.get(0);
					}
				}
				cl = createBusinessCallerList(contact);
				if (cl.size()>0) {
					if (cl.size()>1) {
						if (c!=null) {
							for (int i=0;i<cl.size();i++) {
								((IMultiPhoneCaller)c).getPhonenumbers().addAll(((IMultiPhoneCaller)cl.get(i)).getPhonenumbers());
							}
						} else {
							c = cl.get(0);
							for (int i=0;i<cl.size();i++) {
								((IMultiPhoneCaller)c).getPhonenumbers().addAll(((IMultiPhoneCaller)cl.get(i)).getPhonenumbers());
							}
						}
					} else {
						if (c!=null) {
							for (int i=0;i<cl.size();i++) {
								((IMultiPhoneCaller)c).getPhonenumbers().addAll(((IMultiPhoneCaller)cl.get(i)).getPhonenumbers());
							}
						} else {
							c = cl.get(0);
						}
					}
				}
				if (c!=null)
					callers.add(c);
			}
			
		} catch (ComFailException ex) {
			this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
			if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
				this.m_logger.log(Level.SEVERE, ex.toString(), ex);
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
			} else
				this.m_logger.warning(ex.toString() + ", " + ex.getMessage());
		} catch (Exception ex) {
			this.m_logger.warning(ex.getMessage() + ", " + ex.toString());
		} finally {
			// added 2006/02/05: clean outlook references
			if (contact!=null)
				contact.release();
		}

		return callers;
	}
	
	public int getContactCount(String folder) {
		int count = 0;
		Dispatch outlook = null;
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			ComThread.InitSTA();
			outlook = new Dispatch("Outlook.Application");
			Variant mapiVariant = new Variant("MAPI");
			mapiNS = Dispatch.call(outlook, "GetNameSpace", mapiVariant).toDispatch();
			mapiVariant.release();
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
	
			// searching subfolders
			this.m_logger.info("Includig outlook contact subfolders");
			contactsSubFolder = Dispatch.call(contactsFolder, "Folders", new Variant(folder)).toDispatch();
			items = Dispatch.get(contactsSubFolder, "Items").toDispatch();
			count = Dispatch.get(items, "Count").getInt();
		} catch (ComFailException ex) {
			this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
			if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
				this.m_logger.log(Level.SEVERE, ex.toString(), ex);
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
			} else
				this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());

		} catch (Exception ex) {
			this.m_logger.warning(ex.getMessage() + ", " + ex.toString());
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.release();
			
			if (contactsFolder!=null)
				contactsFolder.release();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.release();
			
			if (mapiNS!=null)
				mapiNS.release();
			
			if (outlook!=null)
				outlook.release();
			ComThread.Release();
		}

		return count;
	}
	
	public List getConfiguredContactFolders() {
		List subfolders = new ArrayList();
		Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
		Iterator i = config.keySet().iterator();
		String key = null;
		while (i.hasNext()) {
			key = (String) i.next();
			if (key.startsWith("subfolder_") && config.getProperty(key).equalsIgnoreCase("true")) {
				subfolders.add(key.substring("subfolder_".length()));
			}
		}
		this.m_logger.info("List of configured outlook contact subfolders: "+subfolders);
		return subfolders;
	}
	
	public List getAllContactFolders() {
		List subfolders = new ArrayList();
		
		Dispatch outlook = null;
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			ComThread.InitSTA();
			outlook = new Dispatch("Outlook.Application");
			Variant mapiVariant = new Variant("MAPI");
			mapiNS = Dispatch.call(outlook, "GetNameSpace", mapiVariant).toDispatch();
			mapiVariant.release();
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
	
			// searching subfolders
			this.m_logger.info("Includig outlook contact subfolders");
			items = Dispatch.get(contactsFolder, "Folders").toDispatch();
			Variant itemf = Dispatch.call(items, "GetFirst");
			Dispatch f = null;
			while ((itemf != null) && (!itemf.isNull())) {
				f = itemf.toDispatch();
				subfolders.add(Dispatch.get(f, "Name").toString());					
				itemf = Dispatch.call(items, "GetNext");
			}
			if (f!=null) f.release();
			if (itemf!=null) itemf.release();
			this.m_logger.info("List of including outlook contact subfolders: "+subfolders);
			contactsVariant.release();

		} catch (ComFailException ex) {
			this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
			if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
				this.m_logger.log(Level.SEVERE, ex.toString(), ex);
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
			} else
				this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());

		} catch (Exception ex) {
			this.m_logger.warning(ex.getMessage() + ", " + ex.toString());
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.release();
			
			if (contactsFolder!=null)
				contactsFolder.release();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.release();
			
			if (mapiNS!=null)
				mapiNS.release();
			
			if (outlook!=null)
				outlook.release();
			ComThread.Release();
		}
		
		return subfolders;
	}

	public ICallerList getCallerListFromAllContacts() {
		PropagationFactory.getInstance().fire(
			new Message(Message.INFO, OutlookContactManager.NAMESPACE, "sync", new Exception("Sync contact folder with MS Outlook..."))
		);
		
		ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		
		long outlookItemsCount = 0;
		
		Dispatch outlook = null;
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			ComThread.InitSTA();
			outlook = new Dispatch("Outlook.Application");
			Variant mapiVariant = new Variant("MAPI");
			mapiNS = Dispatch.call(outlook, "GetNameSpace", mapiVariant).toDispatch();
			mapiVariant.release();
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			contactsVariant.release();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			subfolders.add("");
			subfolders.addAll(getConfiguredContactFolders());			
			
			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);
				if (folder.trim().length()==0) {
					items = Dispatch.get(contactsFolder, "Items").toDispatch();
				} else {
					// found subfolder
					try {
						contactsSubFolder = Dispatch.call(contactsFolder, "Folders", new Variant(folder)).toDispatch();
						items = Dispatch.get(contactsSubFolder, "Items").toDispatch();
					} catch (ComFailException ex) {
						continue;
					}
				}
				//items = Dispatch.get(contactsFolder, "Items").toDispatch();

				Variant item = Dispatch.call(items, "GetFirst");
				ICallerList cl = null;
				while ((item != null) && (!item.isNull())) {
					try {
						outlookItemsCount++;
						Dispatch contact = item.toDispatch();	
						cl = getCallerListFromSingleContact(contact);
						if (cl.size()>0) {
							if (folder.trim().length()>0) {								
								IAttribute category = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY, folder);
								for (int l=0,m=cl.size();l<m;l++) { 
									cl.get(l).setAttribute(category);
								}
							}
							callers.add(cl);
						}

						if (contact!=null)
							contact.release();
						
					} catch (ComFailException ex) {
						this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
						if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
							this.m_logger.log(Level.SEVERE, ex.toString(), ex);
							PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
						} else
							this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());
					}
					if (item!=null)
						item.release();
					
					item = Dispatch.call(items, "GetNext");
				}
			}
			
			
		} catch (ComFailException ex) {
			this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
			if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
				this.m_logger.log(Level.SEVERE, ex.toString(), ex);
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
			} else
				this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());

		} catch (Exception ex) {
			this.m_logger.warning(ex.getMessage() + ", " + ex.toString());
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.release();
			
			if (contactsFolder!=null)
				contactsFolder.release();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.release();
			
			if (mapiNS!=null)
				mapiNS.release();
			
			if (outlook!=null)
				outlook.release();
			ComThread.Release();
		}

		this.m_logger.info(outlookItemsCount + " Outlook contacts found and " + callers.size() + " numbers available.");

		return callers;
	}
	
	public void removeCallerManagerID(ICallerList cl) {
		ICaller c = null;
		for (int i=0,j=cl.size();i<j;i++) {
			c = cl.get(i);
			c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER);
		}
	}
	
	private String getNamespace() {
		return this.NAMESPACE;
	}
}
