package de.janrufmonitor.macab;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.configuration.IConfigManager;
import de.janrufmonitor.macab.listener.AddressBookChangeListener;
import de.janrufmonitor.repository.IMacAddressBookConst;
import de.janrufmonitor.repository.MacAddressBookProxyDatabaseHandler;
import de.janrufmonitor.repository.MacAddressBookProxyException;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.repository.mapping.BusinessMacAddressBookMapping;
import de.janrufmonitor.repository.mapping.IMacAddressBookNumberMapping;
import de.janrufmonitor.repository.mapping.MacAddressBookMappingManager;
import de.janrufmonitor.repository.mapping.PrivateMacAddressBookMapping;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.util.uuid.UUID;

public class MacAddressBookProxy implements AddressBookChangeListener {

	private Logger m_logger;
	
	private IRuntime m_runtime;
	
	private String MSO_IMAGE_CACHE_PATH = PathResolver.getInstance().getPhotoDirectory() + File.separator + "macab-contacts" +File.separator ;
	private String NAMESPACE = "repository.MacAddressBookManager";
	
	private MacAddressBookProxyDatabaseHandler m_dbh;
	private Map m_categories;
	private Map m_rcategories;
	
	private int m_current;
	private int m_total;
	
	private static MacAddressBookProxy m_instance = null;
	
	public native void init();
	private native String getMyUID();
	private native List<?> getNativeAddressBookRecords();
	private native List<?> getRecordsByUIDs(List<String> uids);
	public native byte[] getUserImage(String uid);
	private native void doBeginModification();
	private native void doEndModification();
	private native String addPerson(String fName, String mName, String lName,
			String title, String org, List<Map<String, String>> email,
			List<Map<String, String>> phone,
			List<Map<String, Map<String, String>>> address,
			List<Map<String, String>> chat, boolean isPerson, String groupUID);

	private native boolean addRecordToGroup(String recordUID, String groupUID);

	private native String addGroup(String name, String groupUID);

	private native List<Map<String, ?>> findContactsByLastName(String lastName);

	private native List<Map<String, ?>> findContactsByFirstName(String lastName);

	private native List<Map<String, ?>> findContactsByNameElement(
			String nameElement);

	private native List<Map<String, ?>> findContactsByPhone(String phone);

	private native List<Map<String, ?>> findContactsByEmail(String email);

	private native List<Map<String, ?>> findContactsByAddress(String address);

	private native List<Map<String, ?>> findContactsByFullTextSearch(
			String address);

	private native void revealInAddressBook(String uid, boolean edit);
	
	public static MacAddressBookProxy getInstance() {
		if (m_instance == null) {
			m_instance = new MacAddressBookProxy();
		}
		return m_instance;
	}
	
	public static void invalidate() {
		m_instance = null;
	}
	
	@SuppressWarnings("unchecked")
	private MacAddressBookProxy() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		File cache = new File(MSO_IMAGE_CACHE_PATH);
		if (!cache.exists()) cache.mkdirs();
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Created folder "+cache.getAbsolutePath());
		
		System.loadLibrary("AddressBook"); 
		init();
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Loaded libAddressBook.jnilib");
		
		this.m_categories = new HashMap();
		this.m_rcategories = new HashMap();
		List<Object> categories = getRawMacGroups();
		for (Object r : categories) {
			if (r instanceof List<?>) {
				this.m_categories.put(((List<?>) r).get(1), ((List<?>) r).get(0));
				this.m_rcategories.put(((List<?>) r).get(0), ((List<?>) r).get(1));
			}
		}
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	public MacAddressBookProxyDatabaseHandler getDataHandler() {
		if (this.m_dbh==null) this.start();
		return this.m_dbh;
	}
	
	public void start() {
		if (this.m_dbh==null)  {
			String db_path = PathResolver.getInstance(this.getRuntime())
			.resolve(PathResolver.getInstance(this.getRuntime()).getDataDirectory()+ "macab_cache" + File.separator + "macab_mapping.db");
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
	
			this.m_dbh = new MacAddressBookProxyDatabaseHandler(
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
	
	@SuppressWarnings("unchecked")
	public synchronized ICaller findContact(IPhonenumber pn) throws MacAddressBookProxyException {
		ICaller c = Identifier.identifyDefault(getRuntime(), pn);

		if (c==null && PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(pn)) {
			IPhonenumber p = getRuntime().getCallerFactory().createInternalPhonenumber(pn.getTelephoneNumber());
			c = getRuntime().getCallerFactory().createCaller(p);
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
						List uids = new ArrayList();
						uids.add(uuid);
						List contacts = getRecordsByUIDs(uids);
						if (contacts.size()>0) {
							Object contact = contacts.get(0);
							ICallerList cl = getRuntime().getCallerFactory().createCallerList(2);
							if (contact instanceof Map<?,?>) {
								ICaller businessCaller, privateCaller = null;
								privateCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new PrivateMacAddressBookMapping());
								businessCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new BusinessMacAddressBookMapping());
								
								if (privateCaller==null && businessCaller!=null) {
									cl.add(businessCaller);
								}
								if (privateCaller!=null && businessCaller==null) {
									cl.add(privateCaller);
								}
								if (privateCaller!=null && businessCaller!=null) {					
									if (((IMultiPhoneCaller)businessCaller).getPhonenumbers().size()==1) {
										IPhonenumber pn1 = (IPhonenumber) ((IMultiPhoneCaller)businessCaller).getPhonenumbers().get(0); // only one entry available
										IAttribute numbertype = businessCaller.getAttribute(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn1.getTelephoneNumber());
										if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(IMacAddressBookConst.MOBILE)) {
											this.m_logger.info("Bussiness caller will be dropped. Only mobile number available, but still in private contact: "+businessCaller);
											businessCaller = null;
										}
									}
									if (((IMultiPhoneCaller)privateCaller).getPhonenumbers().size()==1 && businessCaller!=null) {
										IPhonenumber pn1 = (IPhonenumber) ((IMultiPhoneCaller)privateCaller).getPhonenumbers().get(0); // only one entry available
										IAttribute numbertype = privateCaller.getAttribute(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn1.getTelephoneNumber());
										if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(IMacAddressBookConst.MOBILE)) {
											this.m_logger.info("Private caller will be dropped. Only mobile number available, but still in business contact: "+privateCaller);
											privateCaller = null;
										}
									}
									if (privateCaller!=null) {
										cl.add(privateCaller);
									}
									if (businessCaller!=null) {
										cl.add(businessCaller);
									}
								}				
							}
							
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
	
	public synchronized void removeContact(ICaller c) throws MacAddressBookProxyException {
		throw new MacAddressBookProxyException("The method removeContact() is not yet implemented.");
	}
	
	public synchronized void createContact(ICaller c) throws MacAddressBookProxyException { 
		throw new MacAddressBookProxyException("The method createContact() is not yet implemented.");
	}
	
	@SuppressWarnings("unchecked")
	public synchronized ICallerList findContacts(String searchTerm) throws MacAddressBookProxyException {
		this.m_total = 0;
		this.m_current = 0;
		
		final ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		List<Map<String, ?>> ac = this.findContactsByFullTextSearch(searchTerm);

		if (ac==null) return callers;
		
		this.m_total = ac.size();
		
		ICaller businessCaller, privateCaller = null;
		for (Object contact : ac) {
			if (contact instanceof Map<?,?>) {
				this.m_current++;
				privateCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new PrivateMacAddressBookMapping());
				businessCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new BusinessMacAddressBookMapping());
	
				if (privateCaller!=null && businessCaller!=null) {
					// check if firstname, lastname or additional is filled or not
					if (privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null && privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null && privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL)==null) {
						((IMultiPhoneCaller)businessCaller).getPhonenumbers().addAll(((IMultiPhoneCaller)privateCaller).getPhonenumbers());
						privateCaller=null;
					}
					if (businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null && businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null && businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL)==null) {
						((IMultiPhoneCaller)privateCaller).getPhonenumbers().addAll(((IMultiPhoneCaller)businessCaller).getPhonenumbers());
						businessCaller=null;
					}
				}
				
				if (businessCaller!=null) {
					callers.add(businessCaller);
				}
				if (privateCaller!=null) {
					callers.add(privateCaller);
				}				
			}
		}
		if (callers.size()>0) {
			Thread updateDbhThread = new Thread() {
				public void run() {
					updateProxyDatabase(callers);
				}
			};
			updateDbhThread.setName("JAM-MacAddressBookSync-Thread-(deamon)");
			updateDbhThread.start();
		}
		return callers;
	}
	
	
	@SuppressWarnings("unchecked")
	public synchronized ICallerList getContacts(String f) throws MacAddressBookProxyException {
		this.m_total = 0;
		this.m_current = 0;
		
		final ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		List<Object> ac = null;

		// category is set
		if (f!=null && f.length()>0) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Filtering with category "+f);
			String groupUID = (String) this.m_rcategories.get(f);
			if (groupUID!=null && groupUID.length()>0) {
				List uids = new ArrayList();
				uids.add(groupUID);
				List groups = getRecordsByUIDs(uids);
				if (groups.size()>0) {
					List group = (List) groups.get(0);
					if (group.size()>3) {
						uids = (List) group.get(4);
						ac = (List) getRecordsByUIDs(uids);
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Filtered mac contact list "+ac);
					}
				}
			}
		}
		
		
		// get all contacts
		if (f==null) 
			ac = this.getRawMacContacts();
		
		if (ac==null) return callers;
		
		this.m_total = ac.size();
		
		ICaller businessCaller, privateCaller = null;
		for (Object contact : ac) {
			if (contact instanceof Map<?,?>) {
				this.m_current++;
				privateCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new PrivateMacAddressBookMapping());
				businessCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new BusinessMacAddressBookMapping());
	
				if (privateCaller!=null && businessCaller!=null) {
					// check if firstname, lastname or additional is filled or not
					if (privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null && privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null && privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL)==null) {
						((IMultiPhoneCaller)businessCaller).getPhonenumbers().addAll(((IMultiPhoneCaller)privateCaller).getPhonenumbers());
						privateCaller=null;
					}
					if (businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null && businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null && businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL)==null) {
						((IMultiPhoneCaller)privateCaller).getPhonenumbers().addAll(((IMultiPhoneCaller)businessCaller).getPhonenumbers());
						businessCaller=null;
					}
				}
				
				if (businessCaller!=null) {
					callers.add(businessCaller);
				}
				if (privateCaller!=null) {
					callers.add(privateCaller);
				}				
			}
		}
		if (callers.size()>0) {
			Thread updateDbhThread = new Thread() {
				public void run() {
					updateProxyDatabase(callers);
				}
			};
			updateDbhThread.setName("JAM-MacAddressBookSync-Thread-(deamon)");
			updateDbhThread.start();
		}
		return callers;
	}
	
	public void preload() throws MacAddressBookProxyException {
		final ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		List<Object> ac  = this.getRawMacContacts();
		
		if (ac==null) return;
		
		ICaller businessCaller, privateCaller = null;
		for (Object contact : ac) {
			if (contact instanceof Map<?,?>) {
				privateCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new PrivateMacAddressBookMapping());
				businessCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new BusinessMacAddressBookMapping());
	
				if (businessCaller!=null) {
					callers.add(businessCaller);
				}
				if (privateCaller!=null) {
					callers.add(privateCaller);
				}			
			}
		}
		if (callers.size()>0) {
			Thread updateDbhThread = new Thread() {
				public void run() {
					updateProxyDatabase(callers);
				}
			};
			updateDbhThread.setName("JAM-MacAddressBookSync-Thread-(deamon)");
			updateDbhThread.start();
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized ICallerList getContactsByCharAttribute(IAttribute charAttribute) throws MacAddressBookProxyException {
		this.m_total = 0;
		this.m_current = 0;
		
		final ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		if (this.m_dbh!=null) {
			try {
				List uuids = this.m_dbh.select(charAttribute);
				if (uuids.size()>0) {
					List contacts = getRecordsByUIDs(uuids);
					if (contacts.size()>0) {
						this.m_total = contacts.size();
						ICaller businessCaller, privateCaller = null;
						for (Object contact : contacts) {
							if (contact instanceof Map<?,?>) {
								this.m_current++;
								privateCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new PrivateMacAddressBookMapping());
								businessCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new BusinessMacAddressBookMapping());
					
								if (privateCaller!=null && businessCaller!=null) {
									// check if firstname, lastname or additional is filled or not
									if (privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null && privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null && privateCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL)==null) {
										((IMultiPhoneCaller)businessCaller).getPhonenumbers().addAll(((IMultiPhoneCaller)privateCaller).getPhonenumbers());
										privateCaller=null;
									}
									if (businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null && businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null && businessCaller.getAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL)==null) {
										((IMultiPhoneCaller)privateCaller).getPhonenumbers().addAll(((IMultiPhoneCaller)businessCaller).getPhonenumbers());
										businessCaller=null;
									}
								}
								
								if (businessCaller!=null) {
									callers.add(businessCaller);
								}
								if (privateCaller!=null) {
									callers.add(privateCaller);
								}				
							}
						}
					}
				}
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}					
		}
		if (callers.size()>0) {
			Thread updateDbhThread = new Thread() {
				public void run() {
					updateProxyDatabase(callers);
				}
			};
			updateDbhThread.setName("JAM-MacAddressBookSync-Thread-(deamon)");
			updateDbhThread.start();
		}
		return callers;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized ICallerList getContactsByAreaCode(String countrycode, String areacode) throws MacAddressBookProxyException {
		this.m_total = 0;
		this.m_current = 0;
		
		final ICallerList callers = getRuntime().getCallerFactory().createCallerList();
		if (this.m_dbh!=null) {
			try {
				List uuids = this.m_dbh.select(countrycode, areacode);
				if (uuids.size()>0) {
					List contacts = getRecordsByUIDs(uuids);
					if (contacts.size()>0) {
						this.m_total = contacts.size();
						ICaller businessCaller, privateCaller = null;
						for (Object contact : contacts) {
							if (contact instanceof Map<?,?>) {
								this.m_current++;
								privateCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new PrivateMacAddressBookMapping());
								businessCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new BusinessMacAddressBookMapping());
					
								if (privateCaller==null && businessCaller!=null && this.containsCountryAndAreaCode(businessCaller, countrycode, areacode)) {
									callers.add(businessCaller);
								}
								if (privateCaller!=null && businessCaller==null && this.containsCountryAndAreaCode(privateCaller, countrycode, areacode)) {
									callers.add(privateCaller);
								}
								if (privateCaller!=null && businessCaller!=null) {					
									if (((IMultiPhoneCaller)businessCaller).getPhonenumbers().size()==1) {
										IPhonenumber pn = (IPhonenumber) ((IMultiPhoneCaller)businessCaller).getPhonenumbers().get(0); // only one entry available
										IAttribute numbertype = businessCaller.getAttribute(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
										if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(IMacAddressBookConst.MOBILE)) {
											this.m_logger.info("Bussiness caller will be dropped. Only mobile number available, but still in private contact: "+businessCaller);
											businessCaller = null;
										}
									}
									if (((IMultiPhoneCaller)privateCaller).getPhonenumbers().size()==1 && businessCaller!=null) {
										IPhonenumber pn = (IPhonenumber) ((IMultiPhoneCaller)privateCaller).getPhonenumbers().get(0); // only one entry available
										IAttribute numbertype = privateCaller.getAttribute(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
										if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(IMacAddressBookConst.MOBILE)) {
											this.m_logger.info("Private caller will be dropped. Only mobile number available, but still in business contact: "+privateCaller);
											privateCaller = null;
										}
									}
									if (privateCaller!=null && this.containsCountryAndAreaCode(privateCaller, countrycode, areacode)) {
										callers.add(privateCaller);
									}
									if (businessCaller!=null && this.containsCountryAndAreaCode(businessCaller, countrycode, areacode)) {
										callers.add(businessCaller);
									}
								}				
							}
						}
					}
				}
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}					
		}
		if (callers.size()>0) {
			Thread updateDbhThread = new Thread() {
				public void run() {
					updateProxyDatabase(callers);
				}
			};
			updateDbhThread.setName("JAM-MacAddressBookSync-Thread-(deamon)");
			updateDbhThread.start();
		}
		return callers;
	}
	
	@SuppressWarnings("unchecked")
	private boolean containsCountryAndAreaCode(ICaller c, String intarea, String area) {
		if (c instanceof IMultiPhoneCaller) {
			List<IPhonenumber> phones = ((IMultiPhoneCaller) c).getPhonenumbers();
			for (IPhonenumber phone : phones){
				if (phone.getIntAreaCode().equalsIgnoreCase(intarea) && phone.getAreaCode().equalsIgnoreCase(area)) return true;
			}
		} else {
			IPhonenumber phone = c.getPhoneNumber();
			if (phone.getIntAreaCode().equalsIgnoreCase(intarea) && phone.getAreaCode().equalsIgnoreCase(area)) return true;
		}
		
		return false;
	}
	
	public synchronized boolean updateContact(ICaller c) throws MacAddressBookProxyException {
		throw new MacAddressBookProxyException("The method updateContact() is not yet implemented.");
	}
	
	public int getCurrent() {
		return m_current;
	}
	
	public int getTotal() {
		return m_total;
	}

	public String getCategory(String key) {
		return (String) this.m_categories.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public List getCategories() {
		List cats = new ArrayList();
		Iterator i = this.m_categories.keySet().iterator();
		while (i.hasNext())
			cats.add(this.m_categories.get(i.next()));
		return cats;
	}
	
	@SuppressWarnings("unchecked")
	public void addressBookChanged(AddressBookNotification notification) {
		this.m_logger.info("Mac Address Book update for UID "+notification.getUpdatedRecords());
		Set updates = notification.getUpdatedRecords();
		Iterator i = updates.iterator();
		while (i.hasNext()) {
			String uid = (String) i.next();
			List uids = new ArrayList();
			uids.add(uid);
			List contacts = getRecordsByUIDs(uids);
			if (contacts.size()>0) {
				final ICallerList callers = getRuntime().getCallerFactory().createCallerList();
				for (Object contact : contacts) {
					if (contact instanceof Map<?,?>) {
						ICaller privateCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new PrivateMacAddressBookMapping());
						ICaller businessCaller = MacAddressBookMappingManager.getInstance().mapToJamCaller((Map<?, ?>) contact, new BusinessMacAddressBookMapping());
			
						if (privateCaller==null && businessCaller!=null) {
							callers.add(businessCaller);
						}
						if (privateCaller!=null && businessCaller==null) {
							callers.add(privateCaller);
						}
						if (privateCaller!=null && businessCaller!=null) {					
							if (((IMultiPhoneCaller)businessCaller).getPhonenumbers().size()==1) {
								IPhonenumber pn = (IPhonenumber) ((IMultiPhoneCaller)businessCaller).getPhonenumbers().get(0); // only one entry available
								IAttribute numbertype = businessCaller.getAttribute(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
								if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(IMacAddressBookConst.MOBILE)) {
									this.m_logger.info("Bussiness caller will be dropped. Only mobile number available, but still in private contact: "+businessCaller);
									businessCaller = null;
								}
							}
							if (((IMultiPhoneCaller)privateCaller).getPhonenumbers().size()==1 && businessCaller!=null) {
								IPhonenumber pn = (IPhonenumber) ((IMultiPhoneCaller)privateCaller).getPhonenumbers().get(0); // only one entry available
								IAttribute numbertype = privateCaller.getAttribute(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
								if (numbertype!=null && numbertype.getValue().equalsIgnoreCase(IMacAddressBookConst.MOBILE)) {
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
					}
				}
				if (callers.size()>0) {
					Thread updateDbhThread = new Thread() {
						public void run() {
							updateProxyDatabase(callers);
						}
					};
					updateDbhThread.setName("JAM-MacAddressBookSync-Thread-(deamon)");
					updateDbhThread.start();
				}
			}
		}
	}
	
	private void updateProxyDatabase(ICallerList callers) {
		ICaller c = null;
		
		for (int i=0, j=callers.size();i<j;i++) {
			c = callers.get(i);
			if (c instanceof IMultiPhoneCaller) {
				try {
					// clean up cache
					if (this.m_dbh!=null) {
						this.m_dbh.delete(c.getUUID());
						//this.m_dbh.deleteAttributes(c.getUUID());
					}
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
						this.m_dbh.deleteAttributes(c.getUUID());
						this.m_dbh.insert(c.getUUID(), c.getPhoneNumber().getIntAreaCode(), c.getPhoneNumber().getAreaCode(), c.getPhoneNumber().getCallNumber());
					} catch (SQLException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}					
				}
			}
		}
	}

	private synchronized List<Object> getRawMacContacts() {
		List<Object> allContacts = new ArrayList<Object>();
		for (Object rawRecord : getNativeAddressBookRecords()) {
			if (rawRecord instanceof Map<?, ?>) {
				allContacts.add(rawRecord);
			}
		}
		return allContacts;
	}
	
	private synchronized List<Object> getRawMacGroups() {
		List<Object> allGroups = new ArrayList<Object>();
		for (Object rawRecord : getNativeAddressBookRecords()) {
			if (rawRecord instanceof List<?>) {
				allGroups.add(rawRecord);
			}
		}
		return allGroups;
	}
	
	@SuppressWarnings("unchecked")
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
	
	public static void main(String[] args) {
		System.out.println("\u00C4");
		System.out.println("\u00E4");
		System.out.println("\u00D6");
		System.out.println("\u00F6");
		System.out.println("\u00DC");
		System.out.println("\u00FC");
		System.out.println("\u00DF");
		System.out.println(MacAddressBookProxy.getInstance().getNativeAddressBookRecords());
	}
}
