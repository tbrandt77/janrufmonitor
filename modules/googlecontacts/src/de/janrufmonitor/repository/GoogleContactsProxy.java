package de.janrufmonitor.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.google.gdata.client.Query;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.Link;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.contacts.ContactGroupEntry;
import com.google.gdata.data.contacts.ContactGroupFeed;
import com.google.gdata.data.contacts.GroupMembershipInfo;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.ExtendedProperty;
import com.google.gdata.data.extensions.FamilyName;
import com.google.gdata.data.extensions.FormattedAddress;
import com.google.gdata.data.extensions.FullName;
import com.google.gdata.data.extensions.GivenName;
import com.google.gdata.data.extensions.Name;
import com.google.gdata.data.extensions.Organization;
import com.google.gdata.data.extensions.PhoneNumber;
import com.google.gdata.data.extensions.PostalAddress;
import com.google.gdata.data.extensions.StructuredPostalAddress;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.configuration.IConfigManager;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.util.uuid.UUID;

public class GoogleContactsProxy implements IGoogleContactsConst {

	private Logger m_logger;
	private IRuntime m_runtime;
	
	private Map m_categories;
	private Map m_reverseCategories;
	private GoogleContactsProxyDatabaseHandler m_dbh;
	private int m_current, m_total;
	
	private String GOOGLE_IMAGE_CACHE_PATH = PathResolver.getInstance()
			.getPhotoDirectory()
			+ File.separator + "google-contacts" + File.separator;

	public GoogleContactsProxy() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		File cache = new File(GOOGLE_IMAGE_CACHE_PATH);
		if (!cache.exists())
			cache.mkdirs();
		this.m_categories = new HashMap();
		this.m_reverseCategories = new HashMap();
	}
	
	public void start() {
		if (this.m_dbh==null)  {
			String db_path = PathResolver.getInstance(this.getRuntime())
			.resolve(PathResolver.getInstance(this.getRuntime()).getDataDirectory()+ "google_cache" + File.separator + "google_mapping.db");
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
	
			this.m_dbh = new GoogleContactsProxyDatabaseHandler(
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
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			this.m_dbh=null;
		}
	}

	private ContactsService login(String user, String password) throws GoogleContactsLoginException {
		ContactsService cs = new ContactsService(
				"jam-googlecontacts-callermanager");
		try {
			cs.setUserCredentials(user, password);
			return cs;
		} catch (AuthenticationException e) {
			throw new GoogleContactsLoginException(getLoginUser(), e);
		}
	}

	private ContactsService login() throws GoogleContactsLoginException {
		return this.login(getLoginUser(), getLoginPassword());			
	}
	
	public synchronized ICaller identify(IPhonenumber pn) throws GoogleContactsException {
		ICaller c = Identifier.identifyDefault(getRuntime(), pn);
		if (c!=null) {
			pn = c.getPhoneNumber();
			try {
				List uuids = this.m_dbh.select(pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
				if (this.m_logger.isLoggable(Level.INFO)) {
					this.m_logger.info("List of found UUIDs: "+uuids);
				}
				if (uuids.size()>0) {
					String uuid = null;
					ICaller contact = null;
					for (int k=0;k<uuids.size();k++) {
						uuid = (String) uuids.get(k);
						try {
							contact = this.identifyByUUID(uuid);
							if (contact!=null) {
								if (this.m_logger.isLoggable(Level.INFO)) {
									this.m_logger.info("Google contact found for UUID: "+uuid);
								}	
								contact.setUUID(new UUID().toString());
								if (contact instanceof IMultiPhoneCaller) {
									((IMultiPhoneCaller)contact).getPhonenumbers().clear();
								}
								contact.setPhoneNumber(pn);					
								contact.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION, pn.getTelephoneNumber()));
								return contact;
							}	
						} catch (GoogleContactsException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}				
					}
				} else {
					Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(GoogleContactsCallerManager.NAMESPACE);
					if (config.getProperty(CFG_GOOGLE_KEEPEXT, "false").equalsIgnoreCase("true")) {
						// iterate down
						String callnumber = pn.getCallNumber();
						if (callnumber.length()>1) {
							pn.setCallNumber(callnumber.substring(0, callnumber.length()-1));
							ICaller ca = this.identify(pn);
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

	
	private ICaller identifyByUUID(String uuid) throws GoogleContactsException {
		try {
			fetchCategories();
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}
		
		ContactsService cs = login();
		try {
			URL feedUrl = new URL("http://www.google.com/m8/feeds/contacts/"
					+ getLoginUser() + "/full/"+uuid);
			
			ContactEntry entry = (ContactEntry) cs.getEntry(feedUrl,
					ContactEntry.class);
			return parse(cs, entry);
		} catch (MalformedURLException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
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
	
	public synchronized void checkAuthentication(String user, String password) throws GoogleContactsLoginException {
		ContactsService cs = login(user, password);
		
		try {
		URL feedUrlg = new URL("http://www.google.com/m8/feeds/groups/"
				+ user + "/full");
		cs.getFeed(feedUrlg, ContactGroupFeed.class);
		} catch (IOException e) {
			throw new GoogleContactsLoginException(e);
		} catch (ServiceException e) {
			throw new GoogleContactsLoginException(e);
		}
	}
	
	public synchronized List getCategories() {
		List categories = new ArrayList();
		try {
			fetchCategories();
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ServiceException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (GoogleContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		Iterator iter = this.m_categories.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry e = (Entry) iter.next();
			categories.add(e.getValue());			
		}
		return categories;
	}

	public synchronized ICallerList getContacts(String category) throws GoogleContactsException {
		this.m_current = 0; this.m_total = 0;
		try {
			fetchCategories();
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}

		ICallerList cl = getRuntime().getCallerFactory().createCallerList(
				getMaxResults());
		
		if (category !=null && !this.m_reverseCategories.containsKey(category)) return cl;
		
		ContactsService cs = login();
		try {
			URL feedUrl = new URL("http://www.google.com/m8/feeds/contacts/"
					+ getLoginUser() + "/full");
			Query q = new Query(feedUrl);
			q.setMaxResults(getMaxResults());
			if (category!=null)
				q.setStringCustomParameter("group", (String) this.m_reverseCategories.get(category));
			
			ContactFeed resultFeed = (ContactFeed) cs.getFeed(q,
					ContactFeed.class);
			List entries = resultFeed.getEntries();
			this.m_total = entries.size();
			this.m_logger.info("Fetched " + entries.size()
					+ " entries from google account " + getLoginUser());
			Object o = null;
			for (int i = 0, j = entries.size(); i < j; i++) {
				o = entries.get(i);
				if (o instanceof ContactEntry) { // && (category==null || matchCategory(category, (ContactEntry) o))) {
					ICaller c = this.parse(cs, (ContactEntry) o);
					if (c != null) {
						cl.add(c);
						this.m_current++;
					}
				}
			}
		} catch (MalformedURLException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}
		
		//
		if (this.m_dbh!=null) {
			try {
				if (category==null)
					this.m_dbh.deleteAll();
				
				ICaller c = null;
				for (int i=0,j=cl.size();i<j;i++) {
					c = cl.get(i);
					if (c instanceof IMultiPhoneCaller) {
						List phones = ((IMultiPhoneCaller)c).getPhonenumbers();
						IPhonenumber pn = null;
						if (category!=null)
							this.m_dbh.delete(c.getUUID());
						for (int k=0;k<phones.size();k++) {
							pn = (IPhonenumber) phones.get(k);
							this.m_dbh.insert(c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
						}
					} else {
						IPhonenumber pn = c.getPhoneNumber();
						if (category!=null)
							this.m_dbh.delete(c.getUUID());
						this.m_dbh.insert(c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
					}
				}
			} catch (SQLException e) {
				throw new GoogleContactsException(e.getMessage(), e);
			}
		} else {
			this.m_logger.warning("GoogleContacts proxy datahandler not initialized. Could not insert google contacts...");
		}
		
		return cl;
	}
	
	public void preload() throws GoogleContactsException {
		try {
			fetchCategories();
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}

		ICallerList cl = getRuntime().getCallerFactory().createCallerList(
				getMaxResults());
	
		
		ContactsService cs = login();
		try {
			URL feedUrl = new URL("http://www.google.com/m8/feeds/contacts/"
					+ getLoginUser() + "/full");
			Query q = new Query(feedUrl);
			q.setMaxResults(getMaxResults());
	
			ContactFeed resultFeed = (ContactFeed) cs.getFeed(q,
					ContactFeed.class);
			List entries = resultFeed.getEntries();
			this.m_total = entries.size();
			this.m_logger.info("Fetched " + entries.size()
					+ " entries from google account " + getLoginUser());
			Object o = null;
			for (int i = 0, j = entries.size(); i < j; i++) {
				o = entries.get(i);
				if (o instanceof ContactEntry) { // && (category==null || matchCategory(category, (ContactEntry) o))) {
					ICaller c = this.parse(cs, (ContactEntry) o);
					if (c != null) {
						cl.add(c);
					}
				}
			}
		} catch (MalformedURLException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}
		
		//
		if (this.m_dbh!=null) {
			try {	
				ICaller c = null;
				for (int i=0,j=cl.size();i<j;i++) {
					c = cl.get(i);
					if (c instanceof IMultiPhoneCaller) {
						List phones = ((IMultiPhoneCaller)c).getPhonenumbers();
						IPhonenumber pn = null;
						this.m_dbh.delete(c.getUUID());
						for (int k=0;k<phones.size();k++) {
							pn = (IPhonenumber) phones.get(k);
							this.m_dbh.insert(c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
						}
					} else {
						IPhonenumber pn = c.getPhoneNumber();
						this.m_dbh.delete(c.getUUID());
						this.m_dbh.insert(c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
					}
				}
				this.m_dbh.commit();
			} catch (SQLException e) {
				throw new GoogleContactsException(e.getMessage(), e);
			}
		} else {
			this.m_logger.warning("GoogleContacts proxy datahandler not initialized. Could not insert google contacts...");
		}
	}
	
	public synchronized void createContacts(ICallerList cl) throws GoogleContactsException {
		this.m_current = 0; this.m_total = 0;
		
		this.m_total = cl.size();
		try {
			fetchCategories();
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}
		
		if (cl.size()==0) return;
		
		ContactsService cs = login();
		
		ICaller caller = null;
		ContactEntry entry = null;
		for(int i=0,j=cl.size();i<j;i++) {
			this.m_current++;
			caller = cl.get(i);
			IAttributeMap m = caller.getAttributes();
			try {
				entry = new ContactEntry();
				Name name = new Name();
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME))
					name.setFamilyName(new FamilyName((m.get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue().length()==0 ? " " : m.get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue()), null));
				else
					name.setFamilyName(new FamilyName(" ", null));
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME))					
					name.setGivenName(new GivenName((m.get(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue().length()==0 ? " " : m.get(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue()), null));
				else
					name.setGivenName(new GivenName(" ", null));
				
				name.setFullName(new FullName(Formatter.getInstance(getRuntime()).parse("%a:ln%, %a:fn%", m), null));
				entry.setName(name);
				
				entry.addStructuredPostalAddress(createPostalAddress(m));
				
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_EMAIL)) {
					String emails = m.get(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue();
					Email email = new Email();
					email.setAddress(emails);
					entry.addEmailAddress(email);
				}
				
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_CATEGORY)) {
					String cat = m.get(IJAMConst.ATTRIBUTE_NAME_CATEGORY).getValue();
					if (this.m_reverseCategories.containsKey(cat)) {
						GroupMembershipInfo gmi = new GroupMembershipInfo();
						gmi.setHref((String) this.m_reverseCategories.get(cat));						
						entry.addGroupMembershipInfo(gmi);
					}					
				}
				
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_ACC)) {
					ExtendedProperty acc = new ExtendedProperty();
					acc.setName(IJAMConst.ATTRIBUTE_NAME_GEO_ACC);
					acc.setValue(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_ACC).getValue());
					entry.addExtendedProperty(acc);
				}
				
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)) {
					ExtendedProperty acc = new ExtendedProperty();
					acc.setName(IJAMConst.ATTRIBUTE_NAME_GEO_LNG);
					acc.setValue(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue());
					entry.addExtendedProperty(acc);
				}
				
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LAT)) {
					ExtendedProperty acc = new ExtendedProperty();
					acc.setName(IJAMConst.ATTRIBUTE_NAME_GEO_LAT);
					acc.setValue(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue());
					entry.addExtendedProperty(acc);
				}
				
				PhoneNumber pn = null;
				if (caller instanceof IMultiPhoneCaller) {
					List phones = ((IMultiPhoneCaller)caller).getPhonenumbers();
					IPhonenumber p = null;
					for (int k=0,l=phones.size();k<l;k++) {
						p = (IPhonenumber) phones.get(k);
						pn = new PhoneNumber();
						pn.setPrimary(k==0);
						IAttribute type = m.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + p.getTelephoneNumber());
						if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
							pn.setRel(PhoneNumber.Rel.MOBILE);
						} else if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
							pn.setRel(PhoneNumber.Rel.HOME_FAX);
						} else {
							pn.setRel(PhoneNumber.Rel.HOME);
						}
						
						pn.setPhoneNumber(Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, p));
						entry.addPhoneNumber(pn);
					}
				} else {
					pn = new PhoneNumber();
					IAttribute type = m.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + caller.getPhoneNumber().getTelephoneNumber());
					if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
						pn.setRel(PhoneNumber.Rel.MOBILE);
					} else if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
						pn.setRel(PhoneNumber.Rel.HOME_FAX);
					} else {
						pn.setRel(PhoneNumber.Rel.HOME);
					}
					pn.setPhoneNumber(Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, caller.getPhoneNumber()));
					entry.addPhoneNumber(pn);
				}
								
				URL postUrl = new URL("https://www.google.com/m8/feeds/contacts/"
					+ getLoginUser() + "/full");
				entry = (ContactEntry) cs.insert(postUrl, entry);
				
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)) {
					String file = PathResolver.getInstance(getRuntime()).resolve(m.get(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH).getValue());
					if (new File(file).exists()) {
						FileInputStream in = new FileInputStream(file);
						Link photoLink = entry.getContactPhotoLink();
						URL photoUrl = new URL(photoLink.getHref());

						GDataRequest request = cs.createRequest(GDataRequest.RequestType.UPDATE,
						        photoUrl, new ContentType("image/jpeg"));

						Stream.copy(in, request.getRequestStream());
						request.execute();						
					}					
				}
				

				if (entry!=null && this.m_dbh!=null) {
					try {
						if (caller instanceof IMultiPhoneCaller) {
							List phones = ((IMultiPhoneCaller)caller).getPhonenumbers();
							IPhonenumber p = null;
							for (int k=0;k<phones.size();k++) {
								p = (IPhonenumber) phones.get(k);
								this.m_dbh.insert(caller.getUUID(), p.getIntAreaCode(), p.getAreaCode(), p.getCallNumber());
							}
						} else {
							IPhonenumber p = caller.getPhoneNumber();
							this.m_dbh.insert(caller.getUUID(), p.getIntAreaCode(), p.getAreaCode(), p.getCallNumber());
						}
						
					} catch (SQLException e) {
						throw new GoogleContactsException(e.getMessage(), e);
					}
				} else {
					this.m_logger.warning("GoogleContacts proxy datahandler not initialized. Could not insert google contacts...");
				}
			} catch (MalformedURLException e) {
				this.m_logger.log(Level.SEVERE, e.toString(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.toString(), e);
			} catch (ServiceException e) {
				this.m_logger.log(Level.SEVERE, e.toString(), e);
			}
			
		}
	}
	
	public synchronized void updateContact(ICaller caller) throws GoogleContactsException {
		try {
			fetchCategories();
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}
		
		if (caller==null) return;
		
		ContactsService cs = login();
		
		ContactEntry entry = null;
		IAttributeMap m = caller.getAttributes();
		try {
			if (caller.getAttributes().contains("entryUrl")) {
				String entryUrl = caller.getAttribute("entryUrl").getValue();
				if (entryUrl.length()>0) {
					entry = (ContactEntry) cs.getEntry(new URL(entryUrl), ContactEntry.class);
					if (entry==null) {
						this.m_logger.warning("Cannot update google contact: "+caller.toString());
						return;
					}
				} else {
					this.m_logger.warning("Invalid entryUrl parameter. Cannot update google contact: "+caller.toString());
					return;
				}
			} else {
				// no update possible, contact must be new
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("No update possible dur to missing entryUrl. Creating google contact: "+caller.toString());
				
				ICallerList cl = getRuntime().getCallerFactory().createCallerList();
				cl.add(caller);
				this.createContacts(cl);
				return;
			}
						
			Name name = new Name();
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME))
				name.setFamilyName(new FamilyName((m.get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue().length()==0 ? " " : m.get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue()), null));
			else
				name.setFamilyName(new FamilyName(" ", null));
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME))					
				name.setGivenName(new GivenName((m.get(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue().length()==0 ? " " : m.get(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue()), null));
			else
				name.setGivenName(new GivenName(" ", null));
			
			name.setFullName(new FullName(Formatter.getInstance(getRuntime()).parse("%a:ln%, %a:fn%", m), null));
			entry.setName(name);

			entry.getStructuredPostalAddresses().clear();
			entry.addStructuredPostalAddress(createPostalAddress(m));
			
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_EMAIL)) {
				List emaillist = entry.getEmailAddresses();
				String rel = Email.Rel.HOME;
				if (emaillist.size()>0) {
					Email eold = entry.getEmailAddresses().remove(0);
					rel = eold.getRel();
				}
				Email email = new Email();
				email.setAddress(m.get(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue());
				email.setRel(rel);
				entry.addEmailAddress(email);
			}
			
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_CATEGORY)) {
				String cat = m.get(IJAMConst.ATTRIBUTE_NAME_CATEGORY).getValue();
				if (this.m_reverseCategories.containsKey(cat)) {
					GroupMembershipInfo gmi = new GroupMembershipInfo();
					gmi.setHref((String) this.m_reverseCategories.get(cat));						
					entry.addGroupMembershipInfo(gmi);
				}					
			}
			
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_ACC)) {
				ExtendedProperty acc = new ExtendedProperty();
				acc.setName(IJAMConst.ATTRIBUTE_NAME_GEO_ACC);
				acc.setValue(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_ACC).getValue());
				entry.addExtendedProperty(acc);
			}
			
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)) {
				ExtendedProperty acc = new ExtendedProperty();
				acc.setName(IJAMConst.ATTRIBUTE_NAME_GEO_LNG);
				acc.setValue(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue());
				entry.addExtendedProperty(acc);
			}
			
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LAT)) {
				ExtendedProperty acc = new ExtendedProperty();
				acc.setName(IJAMConst.ATTRIBUTE_NAME_GEO_LAT);
				acc.setValue(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue());
				entry.addExtendedProperty(acc);
			}
			
			PhoneNumber pn = null;
			entry.getPhoneNumbers().clear();
			if (caller instanceof IMultiPhoneCaller) {
				List phones = ((IMultiPhoneCaller)caller).getPhonenumbers();
				IPhonenumber p = null;
				for (int k=0,l=phones.size();k<l;k++) {
					p = (IPhonenumber) phones.get(k);
					pn = new PhoneNumber();
					pn.setPrimary(k==0);
					IAttribute type = m.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + p.getTelephoneNumber());
					if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
						pn.setRel(PhoneNumber.Rel.MOBILE);
					} else if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
						pn.setRel(PhoneNumber.Rel.HOME_FAX);
					} else {
						pn.setRel(PhoneNumber.Rel.HOME);
					}
					
					pn.setPhoneNumber(Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, p));
					entry.addPhoneNumber(pn);				
				}
			} else {
				pn = new PhoneNumber();
				IAttribute type = m.get(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + caller.getPhoneNumber().getTelephoneNumber());
				if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
					pn.setRel(PhoneNumber.Rel.MOBILE);
				} else if (type!=null && type.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
					pn.setRel(PhoneNumber.Rel.HOME_FAX);
				} else {
					pn.setRel(PhoneNumber.Rel.HOME);
				}
				pn.setPhoneNumber(Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, caller.getPhoneNumber()));
				entry.addPhoneNumber(pn);
			}

			URL editUrl = new URL(entry.getEditLink().getHref());
			entry = (ContactEntry) cs.update(editUrl, entry);
			
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)) {
				String file = PathResolver.getInstance(getRuntime()).resolve(m.get(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH).getValue());
				if (new File(file).exists()) {
					FileInputStream in = new FileInputStream(file);
					Link photoLink = entry.getContactPhotoLink();
					URL photoUrl = new URL(photoLink.getHref());

					GDataRequest request = cs.createRequest(GDataRequest.RequestType.UPDATE,
					        photoUrl, new ContentType("image/jpeg"));
					
					if (photoLink.getEtag()!=null) {
						request.setEtag(photoLink.getEtag());
					}

					Stream.copy(in, request.getRequestStream());
					request.execute();						
				}					
			}
			

			if (entry!=null && this.m_dbh!=null) {
				try {
					if (caller instanceof IMultiPhoneCaller) {
						List phones = ((IMultiPhoneCaller)caller).getPhonenumbers();
						IPhonenumber p = null;
						this.m_dbh.delete(caller.getUUID());
						for (int k=0;k<phones.size();k++) {
							p = (IPhonenumber) phones.get(k);
							this.m_dbh.insert(caller.getUUID(), p.getIntAreaCode(), p.getAreaCode(), p.getCallNumber());
						}
					} else {
						IPhonenumber p = caller.getPhoneNumber();
						this.m_dbh.delete(caller.getUUID());
						this.m_dbh.insert(caller.getUUID(), p.getIntAreaCode(), p.getAreaCode(), p.getCallNumber());
					}
					
				} catch (SQLException e) {
					throw new GoogleContactsException(e.getMessage(), e);
				}
			} else {
				this.m_logger.warning("GoogleContacts proxy datahandler not initialized. Could not insert google contacts...");
			}
		} catch (MalformedURLException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}
			
		
	}
	
	public synchronized void deleteContacts(ICallerList cl) throws GoogleContactsException {
		this.m_current = 0; this.m_total = 0;
		this.m_total = cl.size();
		try {
			fetchCategories();
		} catch (IOException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		} catch (ServiceException e) {
			throw new GoogleContactsException(e.getMessage(), e);
		}
		
		ContactsService cs = login();
		ICaller c = null;
		for (int i=0,j=cl.size();i<j;i++) {
			c = cl.get(i);
			try {
				if (c.getAttributes().contains("entryUrl")) {
					String entryUrl = c.getAttribute("entryUrl").getValue();
					if (entryUrl.length()>0) {
						ContactEntry entry = (ContactEntry) cs.getEntry(new URL(entryUrl), ContactEntry.class);
						if (entry!=null) {
							this.m_logger.info("Deleting google contact: "+entry.getEtag());
							entry.delete();
							this.m_current++;
						}
					}					
				}	
				
				if (this.m_dbh!=null) {
					try {
						this.m_dbh.delete(c.getUUID());
					} catch (SQLException e) {
						throw new GoogleContactsException(e.getMessage(), e);
					}
				} else {
					this.m_logger.warning("GoogleContacts proxy datahandler not initialized. Could not insert google contacts...");
				}
			} catch (MalformedURLException e) {
				this.m_logger.log(Level.SEVERE, e.toString(), e);			
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.toString(), e);
			} catch (ServiceException e) {
				this.m_logger.log(Level.SEVERE, e.toString(), e);
			}
		}
	}

	private StructuredPostalAddress createPostalAddress(IAttributeMap m) {
		StructuredPostalAddress pa = new StructuredPostalAddress();
		StringBuffer tmp =new StringBuffer(256);
		if (m.contains(IJAMConst.ATTRIBUTE_NAME_STREET)) {
			tmp.append(m.get(IJAMConst.ATTRIBUTE_NAME_STREET).getValue());
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_STREET_NO)) {
				tmp.append(" ");
				tmp.append(m.get(IJAMConst.ATTRIBUTE_NAME_STREET_NO).getValue());
			}
			tmp.append("\n");
		}
		if (m.contains(IJAMConst.ATTRIBUTE_NAME_CITY)) {
			
			if (m.contains(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE)) {
				tmp.append(m.get(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue());
				tmp.append(" ");
			}
			tmp.append(m.get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue());			
		}
		
		if (m.contains(IJAMConst.ATTRIBUTE_NAME_COUNTRY)) {			
			tmp.append("\n");
			tmp.append(m.get(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue());			
		}
		if (tmp.length()==0) tmp.append(" ");
		pa.setFormattedAddress(new FormattedAddress(tmp.toString()));		
		pa.setRel(PostalAddress.Rel.HOME);
		pa.setPrimary(Boolean.TRUE);
		return pa;
	}

	private synchronized ICaller parse(ContactsService cs, ContactEntry e) {
		if (e == null)
			return null;
		if (e.getPhoneNumbers().size() == 0)
			return null;

		String uuid = parseUUID(e);

		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		m.addAll(parseName(e));

		if (e.hasStructuredPostalAddresses()) {
			StructuredPostalAddress pa = (StructuredPostalAddress) e.getStructuredPostalAddresses().get(0);
			m.addAll(parseAddress(pa));
		}
		
		// 2012/10/20: added email support
		if (e.hasEmailAddresses()) {
			List emaillist = e.getEmailAddresses();
			if (emaillist.size()>0) {
				Email email = (Email) emaillist.get(0);
				m.add(parseEmail(email));
			}
		}

		List gphones = e.getPhoneNumbers();
		List pl = new ArrayList(gphones.size());
		PhoneNumber p = null;
		IPhonenumber pn = null;
		for (int i = 0, j = gphones.size(); i < j; i++) {
			p = (PhoneNumber) gphones.get(i);
			pn = PhonenumberAnalyzer.getInstance(getRuntime()).toIdentifiedPhonenumber(
							p.getPhoneNumber());
			if (pn != null) {
				pl.add(pn);
				m.add(parseNumberType(pn, p));
			}
		}
		if (pl.size() == 0)
			return null;
		
		if (e.getExtendedProperties()!=null) {
			ExtendedProperty exp = null;
			for (int i=0, j= e.getExtendedProperties().size();i<j;i++) {
				exp = (ExtendedProperty) e.getExtendedProperties().get(i);
				
				if (exp.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_GEO_ACC) && exp.getValue().length()>0) {
					m.add(getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_GEO_ACC,
							exp.getValue()));
				}
				if (exp.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_GEO_LNG) && exp.getValue().length()>0) {
					m.add(getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_GEO_LNG,
							exp.getValue()));
				}
				if (exp.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_GEO_LAT) && exp.getValue().length()>0) {
					m.add(getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_GEO_LAT,
							exp.getValue()));
				}
			}
			
		}

		m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
				GoogleContactsCallerManager.ID));
		
		m.add(getRuntime().getCallerFactory().createAttribute(
				"entryUrl",
				e.getSelfLink().getHref()));

		IAttribute a = parseCategory(e);
		if (a != null)
			m.add(a);
		
		try {
			a = parseImage(cs, e);
			if (a != null) {
				m.add(a);
			}
				
		} catch (GoogleContactsException ex) {
			this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
		
		return getRuntime().getCallerFactory().createCaller(uuid, null, pl, m);
	}
	
	private IAttribute parseEmail(Email email) {
		return getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL, email.getAddress());
	}

	private IAttributeMap parseName(ContactEntry e) {
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();

		if (e.getName()!=null && e.getName().getFamilyName()!=null && e.getName().getGivenName()!=null) {
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_LASTNAME,
					e.getName().getFamilyName().getValue()));
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
					e.getName().getGivenName().getValue()));
			return m;
		}
		
		String name = e.getTitle().getPlainText();
		// test name splitting
		String[] tmp = name.split(" ");
		// case: Müller
		// case: Spandauer Holzwaren KG
		if (tmp.length==1 || tmp.length>2)  {
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_LASTNAME,
					name));
		}
		// case: Thilo Brandt
		// case: Brandt, Thilo
		if (tmp.length==2) {
			if (tmp[0].length()<3 || tmp[1].length()<3) {
				m.add(getRuntime().getCallerFactory().createAttribute(
						IJAMConst.ATTRIBUTE_NAME_LASTNAME,
						name));
			} else {
				if (tmp[0].endsWith(",")) {
					m.add(getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_LASTNAME,
							tmp[0].substring(0, tmp[0].length()-1)));
					m.add(getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
							tmp[1]));
				} else {
					m.add(getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_LASTNAME,
							tmp[1]));
					m.add(getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
							tmp[0]));
				}
			}
		}
		
		
		if (e.getOrganizations().size() > 0) {
			Organization o = (Organization) e.getOrganizations().get(0);
			if (o!=null && o.getOrgName()!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
						IJAMConst.ATTRIBUTE_NAME_ADDITIONAL,
						((Organization) e.getOrganizations().get(0)).getOrgName()
								.getValue()));
			}
			
		}
		return m;
	}

	private IAttribute parseImage(ContactsService cs, ContactEntry e) throws GoogleContactsException {
		Link photoLink = e.getContactPhotoLink();
		if (photoLink != null && photoLink.getEtag() != null) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Fetching photo link "+e.getContactPhotoLink().getHref());
			
			
			try {
				File img = new File(GOOGLE_IMAGE_CACHE_PATH, e.getSelfLink().getHref()
						.substring(
								e.getSelfLink().getHref()
										.lastIndexOf('/') + 1)+".jpg");
				GDataRequest r =  cs.createLinkQueryRequest(photoLink);
				r.execute();
				InputStream in = r.getResponseStream();
				FileOutputStream fos = new FileOutputStream(img);
				Stream.copy(in, fos, true);
				
				return getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, img.getAbsolutePath());				
			} catch (IOException ex) {
				throw new GoogleContactsException(ex.getMessage(), ex);
			} catch (ServiceException ex) {
				throw new GoogleContactsException(ex.getMessage(), ex);
			}
		}
		return null;
	}
	
//	private boolean matchCategory(String cat, ContactEntry e) {
//		IAttribute ca = parseCategory(e);
//		return (ca!=null && ca.getValue().equalsIgnoreCase(cat));
//	}

	private IAttribute parseCategory(ContactEntry e) {
		List cats = e.getGroupMembershipInfos();
		if (cats.size() > 0) {
			GroupMembershipInfo gmi = null;
			for (int i = 0, j = cats.size(); i < j; i++) {
				gmi = (GroupMembershipInfo) cats.get(i);
				if (this.m_categories.containsKey(gmi.getHref())) {
					return getRuntime().getCallerFactory().createAttribute(
							IJAMConst.ATTRIBUTE_NAME_CATEGORY,
							(String) this.m_categories.get(gmi.getHref()));
				}
			}
		}
		return null;
	}

	private IAttribute parseNumberType(IPhonenumber pn, PhoneNumber p) {
		IAttribute nt = getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(),
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE);
		String rel = p.getRel();
		if (rel != null && rel.endsWith("mobile")) {
			nt.setValue(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE);
		}
		if (rel != null && rel.endsWith("fax")) {
			nt.setValue(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE);
		}
		return nt;
	}
	
	
	
	/**
	 * Parsing the address field
	 * 
	 * Street no.1 12345 city
	 * 
	 * @param pa
	 * @return
	 */
	private IAttributeMap parseAddress(StructuredPostalAddress pa) {
		IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
		if (pa == null)
			return m;
		
		if (pa.hasCity()) {
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CITY, pa.getCity().getValue()));
		}
		
		if (pa.hasPostcode()) {
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, pa.getPostcode().getValue()));
		}
		
		if (pa.hasStreet()) {
			String[] street = splitStreet(pa.getStreet().getValue());
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_STREET, street[0]));
			
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_STREET_NO, street[1]));
		}
		
		if (pa.hasCountry()) {
			m.add(getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_COUNTRY, pa.getCountry().getValue()));
		}
		
		return m;
	}
	
	private String[] splitStreet(String str) {
		String[] t = new String[2];
		if (str.trim().indexOf(" ")<0) {
			t[0] = str.trim();
			t[1] = "";
			return t;
		}
		t[0] = str.trim().substring(0, str.trim().lastIndexOf(" ")).trim();
		t[1] = str.trim().substring(str.trim().lastIndexOf(" ")).trim();
		return t;
	}

	private String parseUUID(ContactEntry e) {
		if (e == null)
			return new UUID().toString();

		if (e.getId().lastIndexOf("/") > -1)
			return e.getId().substring(e.getId().lastIndexOf("/")+1);
		return new UUID().toString();
	}

	private void fetchCategories() throws GoogleContactsException, IOException,
			ServiceException {
		ContactsService cs = login();

		URL feedUrlg = new URL("http://www.google.com/m8/feeds/groups/"
				+ getLoginUser() + "/full");
		ContactGroupFeed resultFeedg = (ContactGroupFeed) cs.getFeed(feedUrlg,
				ContactGroupFeed.class);

		this.m_categories.clear();
		this.m_reverseCategories.clear();

		ContactGroupEntry groupEntry = null;
		for (int k = 0; k < resultFeedg.getEntries().size(); k++) {
			groupEntry = (ContactGroupEntry) resultFeedg.getEntries().get(k);
			this.m_logger.info("Adding category " + groupEntry.getId() + ", "
					+ groupEntry.getTitle().getPlainText());
			if (!groupEntry.getTitle().getPlainText().toLowerCase().startsWith(
					"system group:")) {
				this.m_categories.put(groupEntry.getId(), groupEntry.getTitle()
						.getPlainText());
				this.m_reverseCategories.put(groupEntry.getTitle()
						.getPlainText(), groupEntry.getId());
			}
		}

	}

	private IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	private int getMaxResults() {
		String value = getRuntime().getConfigManagerFactory()
				.getConfigManager().getProperty(
						GoogleContactsCallerManager.NAMESPACE,
						CFG_GOOGLE_MAXRESULTS);
		return ((value != null && value.length() > 0) ? Integer.parseInt(value)
				: 500);
	}

	private String getLoginUser() {
		return getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(GoogleContactsCallerManager.NAMESPACE,
						CFG_GOOGLE_USER);
	}

	private String getLoginPassword() {
		return getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(GoogleContactsCallerManager.NAMESPACE,
						CFG_GOOGLE_PASSWORD);
	}
	
	public int getCurrent() {
		return this.m_current;
	}

	public int getTotal() {
		return this.m_total;
	}
}
