package de.janrufmonitor.repository;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComFailException;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.configuration.IConfigManager;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.repository.mapping.BussinessOutlookMapping;
import de.janrufmonitor.repository.mapping.DefaultOutlookMapping;
import de.janrufmonitor.repository.mapping.IOutlookMapping;
import de.janrufmonitor.repository.mapping.IOutlookNumberMapping;
import de.janrufmonitor.repository.mapping.OutlookMappingManager;
import de.janrufmonitor.repository.mapping.PrivateOutlookMapping;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.util.uuid.UUID;

public class OutlookContactProxy implements OutlookContactConst {

	private Logger m_logger;
	
	private String MSO_IMAGE_CACHE_PATH = PathResolver.getInstance().getPhotoDirectory() + File.separator + "msoutlook-contacts" +File.separator ;
	private IRuntime m_runtime;
	private String NAMESPACE = "repository.OutlookCallerManager";
	private OutlookContactProxyDatabaseHandler m_dbh;
	
	private int m_current;
	private int m_total;
	
	public OutlookContactProxy() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		File cache = new File(MSO_IMAGE_CACHE_PATH);
		if (!cache.exists()) cache.mkdirs();
	}
	
	public synchronized ICallerList findContacts(String searchTerm, String f) throws OutlookContactProxyException {
		if (searchTerm==null || searchTerm.trim().length()==0) return this.getContacts(f);
		
		ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			if (f!=null) {
				subfolders.add(f);
			} else {
				subfolders.add("");
				subfolders.addAll(getAllContactFolders());
			}
			
			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);

				items = Dispatch.get(contactsFolder, "items").toDispatch();
				if (items==null) continue;
				
				String[] filters = {OutlookContactConst.LastName, OutlookContactConst.FirstName, OutlookContactConst.BusinessAddressCity, OutlookContactConst.BusinessAddressStreet, OutlookContactConst.BusinessAddressPostalCode, OutlookContactConst.BusinessAddressCountry, OutlookContactConst.BusinessTelephoneNumber, OutlookContactConst.Business2TelephoneNumber, OutlookContactConst.BusinessFaxNumber, OutlookContactConst.Home2TelephoneNumber, OutlookContactConst.HomeAddressCity, OutlookContactConst.HomeAddressCountry, OutlookContactConst.HomeAddressPostalCode, OutlookContactConst.HomeAddressStreet, OutlookContactConst.HomeFaxNumber, OutlookContactConst.HomeTelephoneNumber, OutlookContactConst.MobileTelephoneNumber};
				
				for (int z=0; z<filters.length;z++) {
					String filter = String.format("["+filters[z]+"] = '%s'", searchTerm);
					Variant item = Dispatch.call(items, "find", filter);
	
					ICallerList cl = null;
					while ((item != null) && (!item.isNull())) {
						try {
							Dispatch contact = item.toDispatch();	
							
							// check UUID in User1 attribute						
							this.checkContactUUID(contact);
							
							cl = getCallerListFromSingleContact(contact);
							if (cl.size()>0) {
								if (folder.trim().length()>0) {								
									IAttribute category = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY, folder);
									for (int l=0,m=cl.size();l<m;l++) { 
										cl.get(l).setAttribute(category);
									}
								}
								for (int k=0;k<cl.size();k++) {
									if (!callers.contains(cl.get(k))) 
										callers.add(cl.get(k));
								}
								
							}
	
							if (contact!=null)
								contact.safeRelease();

						} catch (ComFailException ex) {
							this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
							if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
								this.m_logger.log(Level.SEVERE, ex.toString(), ex);
								PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
							} else
								this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());
						}
						if (item!=null)
							item.safeRelease();
						
						item = Dispatch.call(items, "findNext");
					}
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
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
				outlook.safeRelease();
			
			
		}
		return callers;
	}
	
	public synchronized ICaller findContact(IPhonenumber pn) throws OutlookContactProxyException {
		ICaller c = Identifier.identifyDefault(getRuntime(), pn);

		if (c==null && PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(pn.getTelephoneNumber())) {
			pn = getRuntime().getCallerFactory().createInternalPhonenumber(pn.getTelephoneNumber());
			c = getRuntime().getCallerFactory().createCaller(pn);
		}
		
		if (c!=null) {
			if (PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(pn)) {
				IPhonenumber p = getRuntime().getCallerFactory().createInternalPhonenumber(pn.getTelephoneNumber());
				c.setPhoneNumber(p);
			} 
			pn = c.getPhoneNumber();
			try {
				List uuids = this.m_dbh.select(pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("List of found UUIDs: "+uuids);
				}
				if (uuids.size()>0) {
					String uuid = null;
					for (int k=0;k<uuids.size();k++) {
						uuid = (String) uuids.get(k);
						
						
						ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
						Dispatch mapiNS = null;
						Dispatch contactsFolder = null;
						Dispatch contactsSubFolder = null;
						Dispatch items = null;
						Dispatch contact = null;
						try {
							if (this.m_logger.isLoggable(Level.INFO)) 
								this.m_logger.info("created Outlook.Application dispatch");
							
							if (this.m_logger.isLoggable(Level.INFO)) 
								this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
							
							mapiNS = outlook.getProperty("Session").toDispatch();
							if (this.m_logger.isLoggable(Level.INFO)) 
								this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
							
							Variant contactsVariant = new Variant(10);
							contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
							if (this.m_logger.isLoggable(Level.INFO)) 
								this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
							contactsVariant.safeRelease();
							
							// getting configured subfolders
							List subfolders = new ArrayList();
							subfolders.add("");
							subfolders.addAll(getAllContactFolders());
							
							String folder = null;
							for (int i=0,j=subfolders.size();i<j;i++) {
								folder = (String) subfolders.get(i);
								
								items = this.getItemsOfFolder(contactsFolder, folder, false);
								if (items==null) continue;

								contact = this.findContactByUUID(items, uuid);
								if (contact!=null) {
									if (this.m_logger.isLoggable(Level.INFO)) {
										this.m_logger.info("Outlook contact found for UUID: "+uuid);
									}
									ICallerList cl = getCallerListFromSingleContact(contact);
									if (cl.size()==1) {
										ICaller rc = cl.get(0);
										rc.setUUID(new UUID().toString());
										if (rc instanceof IMultiPhoneCaller) {
											((IMultiPhoneCaller)rc).getPhonenumbers().clear();
										}
										rc.setPhoneNumber(pn);
										IAttribute att = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION, pn.getTelephoneNumber());
										rc.setAttribute(att);
										if (this.m_logger.isLoggable(Level.INFO)) {
											this.m_logger.info("Exact caller match: "+rc.toString());
										}
										return rc;
									}
									if (cl.size()==2) {
										ICaller rc = null;
										for (int x=0;x<cl.size();x++) {
											rc = cl.get(x);
											if (rc instanceof IMultiPhoneCaller) {
												List phones = ((IMultiPhoneCaller)rc).getPhonenumbers();
												IPhonenumber p = null;
												for (int z=0;z<phones.size();z++) {
													p = (IPhonenumber) phones.get(z);
													if (p.getIntAreaCode().equalsIgnoreCase(pn.getIntAreaCode()) && p.getAreaCode().equalsIgnoreCase(pn.getAreaCode()) && pn.getCallNumber().startsWith(p.getCallNumber())){
														if (this.m_logger.isLoggable(Level.INFO)) {
															this.m_logger.info("Caller match (IMultiPhoneCaller): "+rc.toString());
														}
														rc.setUUID(new UUID().toString());
														if (rc instanceof IMultiPhoneCaller) {
															((IMultiPhoneCaller)rc).getPhonenumbers().clear();
														}
														rc.setPhoneNumber(p);
														return rc;
													}
												}
											} else {
												if (rc.getPhoneNumber().getIntAreaCode().equalsIgnoreCase(pn.getIntAreaCode()) && rc.getPhoneNumber().getAreaCode().equalsIgnoreCase(pn.getAreaCode()) && pn.getCallNumber().startsWith(rc.getPhoneNumber().getCallNumber())){
													if (this.m_logger.isLoggable(Level.INFO)) {
														this.m_logger.info("Caller match (ICaller): "+rc.toString());
													}
													return rc;
												}
											}
										}										
									}
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
							throw new OutlookContactProxyException("Error in Application Outlook.", ex);
						} finally {
							// added 2006/02/05: clean outlook references
							if (contact!=null)
								contact.safeRelease();
							
							if (items!=null)
								items.safeRelease();
							
							if (contactsFolder!=null)
								contactsFolder.safeRelease();
							
							if (contactsSubFolder!=null)
								contactsSubFolder.safeRelease();
							
							if (mapiNS!=null)
								mapiNS.safeRelease();
							
							if (outlook!=null)
								outlook.safeRelease();
							
							
						}						
					}
				} else {
					Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
					if (config.getProperty("keepextension", "false").equalsIgnoreCase("true")) {
						// iterate down
						String callnumber = pn.getCallNumber();
						if (callnumber.length()>1) {
							pn.setCallNumber(callnumber.substring(0, callnumber.length()-1));
							ICaller ca = this.findContact(pn);
							if (ca!=null) {
								pn.setCallNumber(callnumber);
								if (ca instanceof IMultiPhoneCaller) {
									((IMultiPhoneCaller)ca).getPhonenumbers().clear();
								}
								ca.setPhoneNumber(pn);
								// set extension
								if (ca.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION)) {
									String centralnumber = ca.getAttribute(IJAMConst.ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION).getValue();
									if (pn.getTelephoneNumber().length()>centralnumber.length()) {
										IAttribute att = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_EXTENSION, pn.getTelephoneNumber().substring(centralnumber.length()));
										ca.setAttribute(att);
									}							
								}
								if (this.m_logger.isLoggable(Level.INFO)) {
									this.m_logger.info("Caller match by central number: "+ca.toString());
								}
								return ca;
							}
						}
					}
				}
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.toString(), e);
			}
		}
		this.m_logger.info("Caller not identified: "+pn.getTelephoneNumber());
		return null;
	}
	
	public synchronized ICallerList getModifiedContacts(long timestamp) throws OutlookContactProxyException {
		ICallerList callers = getRuntime().getCallerFactory().createCallerList();

		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			subfolders.add("");
			subfolders.addAll(getAllContactFolders());
			
			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);
				
				items = this.getItemsOfFolder(contactsFolder, folder, false);
				if (items==null) continue;

				if (this.countContactsByLastModificationTime(items, timestamp)>0) {
					Date d = new Date(timestamp);
					SimpleDateFormat sfd = new SimpleDateFormat("MM/dd/yyyy h:mm a");
					
					Dispatch aContact = Dispatch.call(items, "Find", new Variant("[LastModificationTime] >= '"+sfd.format(d)+"'")).toDispatch();
					if (aContact!=null && aContact.m_pDispatch>0) {	
						do {
							// check UUID in User1 attribute						
							this.checkContactUUID(aContact);
							
							ICallerList cl = getCallerListFromSingleContact(aContact);
							if (cl.size()>0) {
								if (folder.trim().length()>0) {								
									IAttribute category = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY, folder);
									for (int l=0,m=cl.size();l<m;l++) { 
										cl.get(l).setAttribute(category);
									}
								}
								callers.add(cl);
							}
							if (aContact!=null)
								aContact.safeRelease();

							aContact = Dispatch.call(items, "FindNext").toDispatch();
						} while(aContact!=null && aContact.m_pDispatch>0);
					}
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
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
				outlook.safeRelease();
			
			
		}

		return callers;
	}

	
	public synchronized ICallerList getContacts(String f) throws OutlookContactProxyException {
		ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		
		long outlookItemsCount = 0;
		this.m_current = 0;
		this.m_total = (f==null ? this.countAllContact(): this.countContacts(f));
		
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			if (f!=null) {
				subfolders.add(f);
			} else {
				subfolders.add("");
				subfolders.addAll(getAllContactFolders());
			}
			
			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);
								
				items = this.getItemsOfFolder(contactsFolder, folder, false);
				if (items==null) continue;
				
				Variant item = Dispatch.call(items, "GetFirst");
				ICallerList cl = null;
				while ((item != null) && (!item.isNull())) {
					try {
						outlookItemsCount++;
						this.m_current++;
						Dispatch contact = item.toDispatch();	
						
						// check UUID in User1 attribute						
						this.checkContactUUID(contact);
						
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
							contact.safeRelease();
						
					} catch (ComFailException ex) {
						this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
						if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
							this.m_logger.log(Level.SEVERE, ex.toString(), ex);
							PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
						} else
							this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());
					}
					if (item!=null)
						item.safeRelease();
					
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
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
				outlook.safeRelease();
			
			
		}

		this.m_logger.info(outlookItemsCount + " Outlook contacts found and " + callers.size() + " numbers available.");

		return callers;
	}
	
	public synchronized boolean existsContact(String uuid) throws OutlookContactProxyException {
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			subfolders.add("");
			subfolders.addAll(getAllContactFolders());
			
			
			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);
				
				items = this.getItemsOfFolder(contactsFolder, folder, false);
				if (items==null) continue;

				Dispatch aContact = Dispatch.call(items, "Find", new Variant("[User1]='"+uuid+"'")).toDispatch();
				if (aContact!=null && aContact.m_pDispatch>0) {	
					return true;
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
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}
		return false;
	}


	public synchronized ICallerList getContactsByAreaCode(String code) throws OutlookContactProxyException {
		ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		
		long outlookItemsCount = 0;
		this.m_current = 0;
		this.m_total = this.countAllContact();
		
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			subfolders.add("");
			subfolders.addAll(getAllContactFolders());
			
			
			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);
				
				items = this.getItemsOfFolder(contactsFolder, folder, false);
				if (items==null) continue;

				ICallerList cl = null;
				
				Dispatch aContact = Dispatch.call(items, "Find", new Variant("[User2]='"+code+"'")).toDispatch();
				if (aContact!=null && aContact.m_pDispatch>0) {	
					do {
						outlookItemsCount++;
						this.m_current++;
						// check UUID in User1 attribute						
						this.checkContactUUID(aContact);
						cl = getCallerListFromSingleContact(aContact);
						if (cl.size()>0) {
							if (folder.trim().length()>0) {								
								IAttribute category = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY, folder);
								for (int l=0,m=cl.size();l<m;l++) { 
									cl.get(l).setAttribute(category);
								}
							}
							callers.add(cl);
						}

						if (aContact!=null)
							aContact.safeRelease();
						
						aContact = Dispatch.call(items, "FindNext").toDispatch();
					} while(aContact!=null && aContact.m_pDispatch>0);
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
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}

		this.m_logger.info(outlookItemsCount + " Outlook contacts found and " + callers.size() + " numbers available.");

		return callers;
	}

	public synchronized void openContact(ICaller c) throws OutlookContactProxyException {
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			subfolders.add("");
			subfolders.addAll(getAllContactFolders());

			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);
				
				items = this.getItemsOfFolder(contactsFolder, folder, false);
				if (items==null) continue;

				Dispatch contact = this.findContactByUUID(items, c.getUUID());
				if (contact!=null) {
					Dispatch.call(contact, "Display");
					return;
				}		
			}
			PropagationFactory.getInstance().fire(
				new Message(Message.INFO, OutlookContactManager.NAMESPACE, "display", new Exception("Sync contact folder with MS Outlook..."))
			);
				
		} catch (ComFailException ex) {
			this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
			if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
				this.m_logger.log(Level.SEVERE, ex.toString(), ex);
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
			} else
				this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());

		} catch (Exception ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}
	}
	
	public synchronized void createContact(ICaller c) throws OutlookContactProxyException {
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			String folder = this.getAttribute(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY));

			items = this.getItemsOfFolder(contactsFolder, folder, true);
			if (items==null) throw new OutlookContactProxyException("Error in Application Outlook. Folder <"+folder+"> could not be created");

			Dispatch newContact = Dispatch.call(items, "Add").toDispatch(); 
			boolean business = (c.getAttribute("outlook.business")!=null && c.getAttribute("outlook.business").getValue().equalsIgnoreCase("true") ? true : false);
			
			this.setContactData(newContact, c, business);
			
			if (newContact!=null)
				newContact.safeRelease();
			
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("Added new outlook contact in folder "+(folder.length()>0 ? folder : "<root>")+": "+c);
			}
		} catch (ComFailException ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} catch (Exception ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}
	}
	
	public synchronized boolean updateContact(ICaller c) throws OutlookContactProxyException{
		boolean processed = false;
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			String folder = this.getAttribute(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY));

			items = this.getItemsOfFolder(contactsFolder, folder, true);
			if (items==null) throw new OutlookContactProxyException("Error in Application Outlook. Folder <"+folder+"> could not be created");

			Dispatch oldContact = this.findContactByUUID(items, c.getUUID());
			if (oldContact!=null) {
				processed = true;
				boolean business = (c.getAttribute("outlook.business")!=null && c.getAttribute("outlook.business").getValue().equalsIgnoreCase("true") ? true : false);
				this.setContactData(oldContact, c, business);
				this.clearPicture(c);
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("Updating outlook contact in folder "+(folder.length()>0 ? folder : "<root>")+": "+c);
				}
			} else {
				// contact does not exist in this folder maybe somewhere other...
				List subfolders = new ArrayList();
				subfolders.add("");
				subfolders.addAll(getAllContactFolders());
				
				folder = null;				
				for (int i=0,j=subfolders.size();i<j;i++) {
					folder = (String) subfolders.get(i);
					
					items = this.getItemsOfFolder(contactsFolder, folder, false);
					if (items==null) continue;
					
					oldContact = this.findContactByUUID(items, c.getUUID());
					if (oldContact!=null) {
						processed = true;
						boolean business = (c.getAttribute("outlook.business")!=null && c.getAttribute("outlook.business").getValue().equalsIgnoreCase("true") ? true : false);
						this.setContactData(oldContact, c, business);
						this.moveContactToFolder(oldContact, contactsFolder, this.getAttribute(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY)));
						if (this.m_logger.isLoggable(Level.INFO)) {
							this.m_logger.info("Updated outlook contact in folder "+(folder.length()>0 ? folder : "<root>")+" and moved to folder "+this.getAttribute(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY))+": "+c);
						}
						break;
					}
				}
				
				if (!processed) {
					// try to find upon creation and modification date
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("An outlook contact in folder "+(folder.length()>0 ? folder : "<root>")+" could not be found: "+c);
					}
					String[] name = c.getUUID().split("_");
					if (name!=null && name.length>1) {
						folder = null;
						processed = false;
						for (int i=0,j=subfolders.size();i<j;i++) {
							folder = (String) subfolders.get(i);
							
							items = this.getItemsOfFolder(contactsFolder, folder, false);
							if (items==null) continue;
							
							oldContact = this.findContactByName(items, name[1], name[0]);
							if (oldContact!=null) {
								processed = true;
								boolean business = (c.getAttribute("outlook.business")!=null && c.getAttribute("outlook.business").getValue().equalsIgnoreCase("true") ? true : false);
								this.setContactData(oldContact, c, business);
								this.moveContactToFolder(oldContact, contactsFolder, this.getAttribute(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY)));
								if (this.m_logger.isLoggable(Level.INFO)) {
									this.m_logger.info("Updated outlook contact in folder "+(folder.length()>0 ? folder : "<root>")+" and moved to folder "+this.getAttribute(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY))+" after UUID search: "+c);
								}
								break;
							}
						}
						if (!processed) {
							this.m_logger.warning("Contact "+c.toString()+" not found in Outlook contact list by creation and modified date. Contact is not updated.");
						}
					} else
						this.m_logger.warning("Contact "+c.toString()+" not found in Outlook contact list. Contact is not updated.");
				}
			}
			if (oldContact!=null)
				oldContact.safeRelease();
		} catch (ComFailException ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} catch (Exception ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			if (this.m_dbh!=null) {
				try {
					this.m_dbh.delete(c.getUUID());
				} catch (SQLException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}					
			}
			
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}
		return processed;
	}


	public synchronized void removeContact(ICaller c) throws OutlookContactProxyException{
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			String folder = null;
			List subfolders = new ArrayList();
			subfolders.add("");
			subfolders.addAll(getAllContactFolders());

			for (int i=0; i<subfolders.size();i++) {
				folder = (String) subfolders.get(i);
				items = this.getItemsOfFolder(contactsFolder, folder, false);
				if (items==null) continue;

				Dispatch aContact = this.findContactByUUID(items, c.getUUID());
				if (aContact!=null) {
					if (this.countContactsByUUID(items, c.getUUID())>1)
							this.m_logger.warning("More than one UUID found: "+c.getUUID());
					Dispatch.call(aContact, "Delete");
					this.clearPicture(c);
					if (this.m_dbh!=null) {
						this.m_dbh.delete(c.getUUID());
					}
					if (this.m_logger.isLoggable(Level.INFO)) {
						this.m_logger.info("Deleted outlook contact in folder "+(folder.length()>0 ? folder : "<root>")+": "+c);
					}
				}
				if (aContact!=null)
					aContact.safeRelease();
			}
		} catch (ComFailException ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} catch (Exception ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}
	}
	
	private int countAllContact() throws OutlookContactProxyException {
		int count = 0;
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;
		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
			contactsVariant.safeRelease();
			
			// getting configured subfolders
			List subfolders = new ArrayList();
			subfolders.add("");
			subfolders.addAll(getAllContactFolders());
		
			
			String folder = null;
			for (int i=0,j=subfolders.size();i<j;i++) {
				folder = (String) subfolders.get(i);
								
				items = this.getItemsOfFolder(contactsFolder, folder, false);
				if (items==null) continue;
				
				count += Dispatch.get(items, "Count").getInt();
				
			}

		} catch (ComFailException ex) {
			this.m_logger.warning("1 item (e.g. distribution list) was ignored on loading.");
			if (ex.toString().indexOf("Can't get object clsid from progid")>-1) {
				this.m_logger.log(Level.SEVERE, ex.toString(), ex);
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "olstarterror", ex));
			} else
				this.m_logger.warning(ex.getMessage() + ", " + ex.getSource());

		} catch (Exception ex) {
			throw new OutlookContactProxyException("Error in Application Outlook.", ex);
		} finally {
			// added 2006/02/05: clean outlook references
			if (items!=null)
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}
		return count;
	}

	public synchronized int countContacts(String folder) {
		int count = 0;
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;

		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);

	
			// searching subfolders
			this.m_logger.info("Includig outlook contact subfolders");
			contactsSubFolder = Dispatch.call(contactsFolder, "Folders", new Variant(folder)).toDispatch();
			items = Dispatch.get(contactsSubFolder, "Items").toDispatch();
			count = Dispatch.get(items, "Count").getInt();
			if (this.m_logger.isLoggable(Level.INFO)) {
				this.m_logger.info("There are "+count+" outlook contacts in folder "+(folder.length()>0 ? folder : "<root>"));
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
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}

		return count;
	}
	
	public int getCurrent() {
		return m_current;
	}
	
	public int getTotal() {
		return m_total;
	}
	
	public synchronized List getAllContactFolders() {
		List subfolders = new ArrayList();
		
		
		ActiveXComponent outlook = new ActiveXComponent("Outlook.Application");
		Dispatch mapiNS = null;
		Dispatch contactsFolder = null;
		Dispatch contactsSubFolder = null;
		Dispatch items = null;

		try {
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("created Outlook.Application dispatch");
			
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook version: "+Dispatch.get(outlook.getObject(), "Version"));
			
			mapiNS = outlook.getProperty("Session").toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook namespace: "+mapiNS);
			
			Variant contactsVariant = new Variant(10);
			contactsFolder = Dispatch.call(mapiNS, "GetDefaultFolder", contactsVariant).toDispatch();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Microsoft Outlook folder: "+contactsFolder);
	
	
			// searching subfolders
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("Includig outlook contact subfolders");
			items = Dispatch.get(contactsFolder, "Folders").toDispatch();
			Variant itemf = Dispatch.call(items, "GetFirst");
			Dispatch f = null;
			while ((itemf != null) && (!itemf.isNull())) {
				f = itemf.toDispatch();
				subfolders.add(Dispatch.get(f, "Name").toString());					
				itemf = Dispatch.call(items, "GetNext");
			}
			if (f!=null) f.safeRelease();
			if (itemf!=null) itemf.safeRelease();
			if (this.m_logger.isLoggable(Level.INFO)) 
				this.m_logger.info("List of including outlook contact subfolders: "+subfolders);
			contactsVariant.safeRelease();
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
				items.safeRelease();
			
			if (contactsFolder!=null)
				contactsFolder.safeRelease();
			
			if (contactsSubFolder!=null)
				contactsSubFolder.safeRelease();
			
			if (mapiNS!=null)
				mapiNS.safeRelease();
			
			if (outlook!=null)
												outlook.safeRelease();
							
							
		}
		
		// 2009/01/09: removed
		//ensureEditorConfigurationCatergories(subfolders);
		
		return subfolders;
	}
	
	public void start() {
		if (this.m_dbh==null)  {
			String db_path = PathResolver.getInstance(this.getRuntime())
			.resolve(PathResolver.getInstance(this.getRuntime()).getDataDirectory()+ "mso_cache" + File.separator + "mso_mapping.db");
			db_path = StringUtils.replaceString(db_path, "\\", "/");
			File db = new File(db_path + ".properties");
			boolean initialize = false;
			if (!db.exists()) {
				initialize = true;
				db.getParentFile().mkdirs();
				try {
					File db_raw = new File(db_path);
					if (!db_raw.exists()) {
						ZipArchive z = new ZipArchive(db_path);
						z.open();
						z.close();
					}
				} catch (ZipArchiveException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
	
			this.m_dbh = new OutlookContactProxyDatabaseHandler(
					"org.hsqldb.jdbcDriver",
					"jdbc:hsqldb:file:" + db_path, "sa", "", initialize
			);
			try {
				this.m_dbh.connect();
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (ClassNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	public void stop() {
		if (this.m_dbh!=null)  {
			try {
				this.m_dbh.disconnect();
				this.m_dbh=null;
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	
	private void checkContactUUID(Dispatch contact) {
		// check UUID in User1 attribute		
		String user1Field = Dispatch.get(contact, "User1").toString();
		
		if (user1Field.trim().length()==0){ 
			Dispatch.put(contact, "User1", new UUID().toString());
			Dispatch.call(contact, "Save");							
		}
	}

	public void ensureEditorConfigurationCatergories(List subfolders) {
		List ol = new ArrayList();
		IConfigManager mgr = this.getRuntime().getConfigManagerFactory().getConfigManager();
		String value = mgr.getProperty("ui.jface.application.editor.Editor", "categories");
		if (value.trim().length()>0) {
			String[] values = value.split(",");
			for (int i=0;i<values.length;i++) {
				ol.add(values[i]);
			}
		}
		for (int i=0;i<subfolders.size();i++) {
			if (!ol.contains(subfolders.get(i))) {
				ol.add(subfolders.get(i));
				//mgr.setProperty("ui.jface.application.editor.Editor", "filter_cat_ol"+i, "(5,category="+subfolders.get(i)+")");				
			}				
		}
		value = "";
		for (int i=0;i<ol.size();i++) {
			value += ol.get(i) + ",";
		}
		mgr.setProperty("ui.jface.application.editor.Editor", "categories", value);
		mgr.saveConfiguration();
		getRuntime().getConfigurableNotifier().notifyByNamespace("ui.jface.application.editor.Editor");
	}

	private Dispatch getItemsOfFolder(Dispatch rootContactFolder, String folder, boolean doCreate) {
		Dispatch items = null;
		Dispatch subFolder = null;
		if (folder.trim().length()==0) {
			items = Dispatch.get(rootContactFolder, "Items").toDispatch();
		} else {
			// check subfolder
			try {
				subFolder = Dispatch.call(rootContactFolder, "Folders", new Variant(folder)).toDispatch();
				items = Dispatch.get(subFolder, "Items").toDispatch();
			} catch (ComFailException ex) {
				if (doCreate) {
					// sub-folder does not exist, create one
					try {
						subFolder = Dispatch.call(rootContactFolder, "Folders").toDispatch();
						subFolder = Dispatch.call(subFolder, "Add", new Variant(folder)).toDispatch();
						items = Dispatch.get(subFolder, "Items").toDispatch();
					} catch (ComFailException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}					
				}
			}
		}
		return items;
	}
	
	private Dispatch getFolder(Dispatch rootContactFolder, String folder, boolean doCreate) {
		Dispatch subFolder = null;
		if (folder.trim().length()==0) {
			return rootContactFolder;
		} else {
			// check subfolder
			try {
				subFolder = Dispatch.call(rootContactFolder, "Folders", new Variant(folder)).toDispatch();
			} catch (ComFailException ex) {
				if (doCreate) {
					// sub-folder does not exist, create one
					try {
						subFolder = Dispatch.call(rootContactFolder, "Folders").toDispatch();
						subFolder = Dispatch.call(subFolder, "Add", new Variant(folder)).toDispatch();
					} catch (ComFailException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}					
				}
			}
		}
		return subFolder;
	}
	
	private void moveContactToFolder(Dispatch contact, Dispatch rootFolder, String folder) {
		Dispatch subFolder = this.getFolder(rootFolder, folder, true);
		if (subFolder!=null) {
			//Dispatch copy = Dispatch.call(contact, "Copy").toDispatch();
			Dispatch.call(contact, "Move", subFolder);
		} else {
			this.m_logger.warning("Cannot move contact to folder "+folder);
		}
	}
	
	private Dispatch findContactByUUID(Dispatch items, String uuid) {
		Dispatch aContact = Dispatch.call(items, "Find", new Variant("[User1]='"+uuid+"'")).toDispatch();
		if (aContact!=null && aContact.m_pDispatch>0) {	
			return aContact;
		}
		return null;
	}
	
	private Dispatch findContactByName(Dispatch items, String firstname, String lastname) {
		Dispatch aContact = Dispatch.call(items, "Find", new Variant("[LastName]='"+lastname+"' and [FirstName]='"+firstname+"'")).toDispatch();
		if (aContact!=null && aContact.m_pDispatch>0) {	
			return aContact;
		}
		return null;
	}

	private int countContactsByLastModificationTime(Dispatch items, long time) {
		int count = 0;
		Date d = new Date(time);
		SimpleDateFormat sfd = new SimpleDateFormat("MM/dd/yyyy h:mm a");
		
		Dispatch aContact = Dispatch.call(items, "Find", new Variant("[LastModificationTime] >= '"+sfd.format(d)+"'")).toDispatch();
		if (aContact!=null && aContact.m_pDispatch>0) {	
			do {
				count++;
				aContact = Dispatch.call(items, "FindNext").toDispatch();
			} while(aContact!=null && aContact.m_pDispatch>0);
		}
		return count;
	}
	
	private int countContactsByUUID(Dispatch items, String uuid) {
		int count = 0;
		Dispatch aContact = Dispatch.call(items, "Find", new Variant("[User1]='"+uuid+"'")).toDispatch();
		if (aContact!=null && aContact.m_pDispatch>0) {	
			do {
				count++;
				aContact = Dispatch.call(items, "FindNext").toDispatch();
			} while(aContact!=null && aContact.m_pDispatch>0);
		}
		return count;
	}

	private void setContactData(Dispatch contact, ICaller c, boolean business) {
		IOutlookMapping om = null;
		if (business) 
			om = new BussinessOutlookMapping();
		else 
			om = new PrivateOutlookMapping();
		
		Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
		if (config.getProperty("split", "true").equalsIgnoreCase("false")) {
			om = new DefaultOutlookMapping();
			PropagationFactory.getInstance().fire(new Message(Message.WARNING, getNamespace(), "olstore", new Exception("No split option selected.")));
		}
		
		OutlookMappingManager.getInstance().mapToOutlookCaller(contact, c, om);
	}

	private String getAttribute(IAttribute a) {
		if (a!=null && a.getValue()!=null) {
			return a.getValue();
		}
		return "";
	}

	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	private ICaller createPrivateCaller(Dispatch contact) throws ComFailException {
		return OutlookMappingManager.getInstance().mapToJamCaller(contact, new PrivateOutlookMapping());
	}
	
	private void clearPicture(ICaller outlookCaller) {
		String imagepath = MSO_IMAGE_CACHE_PATH +outlookCaller.getUUID() + ".jpg";
		File img = new File(imagepath);
		if (img.exists()) {
			if (!img.delete()) img.deleteOnExit();
		}
	}
	
	
	private ICaller createBusinessCaller(Dispatch contact) throws ComFailException {
		return OutlookMappingManager.getInstance().mapToJamCaller(contact, new BussinessOutlookMapping());
	}
	
	private ICallerList getCallerListFromSingleContact(Dispatch contact) throws ComFailException {
		ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		try {
			Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
			if (config.getProperty("split", "true").equalsIgnoreCase("true")) {
				ICaller privateCaller = createPrivateCaller(contact);
				ICaller businessCaller = createBusinessCaller(contact);
				if (privateCaller==null && businessCaller!=null) {
					callers.add(businessCaller);
				}
				if (privateCaller!=null && businessCaller==null) {
					callers.add(privateCaller);
				}
				if (privateCaller!=null && businessCaller!=null) {					
					if (((IMultiPhoneCaller)businessCaller).getPhonenumbers().size()==1) {
						IPhonenumber pn = (IPhonenumber) ((IMultiPhoneCaller)businessCaller).getPhonenumbers().get(0); // only one entry available
						IAttribute numbertype = businessCaller.getAttribute(IOutlookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
						if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(OutlookContactConst.MobileTelephoneNumber)) {
							this.m_logger.info("Bussiness caller will be dropped. Only mobile number available, but still in private contact: "+businessCaller);
							businessCaller = null;
						}
					}
					if (((IMultiPhoneCaller)privateCaller).getPhonenumbers().size()==1 && businessCaller!=null) {
						IPhonenumber pn = (IPhonenumber) ((IMultiPhoneCaller)privateCaller).getPhonenumbers().get(0); // only one entry available
						IAttribute numbertype = privateCaller.getAttribute(IOutlookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
						if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(OutlookContactConst.MobileTelephoneNumber)) {
							this.m_logger.info("Private caller will be dropped. Only mobile number available, but still in business contact: "+privateCaller);
							privateCaller = null;
						}
					}
					if (privateCaller!=null) {
						callers.add(privateCaller);
					}
					if (businessCaller!=null) {
						callers.add(businessCaller);
					}
				}				
			} else {
				ICaller c = OutlookMappingManager.getInstance().mapToJamCaller(contact, new DefaultOutlookMapping());
				if (c!=null)
					callers.add(c);
			}
			updateProxyDatabase(callers);			
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
				contact.safeRelease();
		}

		return callers;
	}

	private void updateProxyDatabase(ICallerList callers) {
		ICaller c = null;
		
		for (int i=0, j=callers.size();i<j;i++) {
			c = callers.get(i);
			if (c instanceof IMultiPhoneCaller) {
				try {
					// clean up cache
					if (this.m_dbh!=null) 
						this.m_dbh.delete(c.getUUID());
				} catch (SQLException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}	
			}
		}
		
		for (int i=0, j=callers.size();i<j;i++) {
			c = callers.get(i);
			if (c instanceof IMultiPhoneCaller) {
				List pns = ((IMultiPhoneCaller)c).getPhonenumbers();
				IPhonenumber pn = null;
				for (int k=0,l=pns.size();k<l;k++) {
					pn = (IPhonenumber) pns.get(k);
					if (this.m_dbh!=null) {
						try {
							this.m_dbh.insert(c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
						} catch (SQLException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}					
					}
				}
			} else {
				if (this.m_dbh!=null) {
					try {
						this.m_dbh.delete(c.getUUID());
						this.m_dbh.insert(c.getUUID(), c.getPhoneNumber().getIntAreaCode(), c.getPhoneNumber().getAreaCode(), c.getPhoneNumber().getCallNumber());
					} catch (SQLException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}					
				}
			}
		}
	}
	
	private String getNamespace() {
		return this.NAMESPACE;
	}
}
