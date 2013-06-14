package de.janrufmonitor.repository;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.mapping.LdapMappingManager;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.util.uuid.UUID;

public class LdapContactsProxy implements ILdapRepositoryConst {
	
	private Logger m_logger;
	private IRuntime m_runtime;

	private LdapContactsProxyDatabaseHandler m_dbh;

	public LdapContactsProxy() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
	}
	
	public synchronized ICallerList getContacts(String category) throws LdapContactsException {
		ICallerList cl = getRuntime().getCallerFactory().createCallerList(
				getMaxResults());
		
		String query = "(objectclass=*)";
		if (category!=null) {
			String ldapAttrib = LdapMappingManager.getInstance().getLdapAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY);
			if (ldapAttrib!=null && ldapAttrib.trim().length()>0) {
				query = "("+ldapAttrib+"="+category+")";
			}
		}
		
		LDAPConnection lc = new LDAPConnection();
        try {
        	lc.connect(getServer(), getPort());
            lc.bind(LDAPConnection.LDAP_V3, getLoginUser(), getLoginPassword().getBytes("UTF-8"));
            LDAPSearchConstraints cons = lc.getSearchConstraints();
            cons.setMaxResults(getMaxResults());
            
            String baseDN = this.getBaseDN();
            String[] bases = null;
            if (baseDN.indexOf("|")>0) {
            	bases = baseDN.split("\\|");
            } else {
            	bases = new String[] {baseDN};
            }
            
            for (int i=0;i<bases.length;i++) {
	            LDAPSearchResults searchResults =
	            	lc.search(bases[i],
	            				getScope(),
	            				query,
	                            null,       // return all attributes
	                            false, 		// return attrs and values
	                            cons);       
	            
	            ICaller c = null;
	            while (searchResults.hasMore()) {
	                LDAPEntry nextEntry = null;
	
	                try {
	                    nextEntry = searchResults.next();
	                } catch(LDAPException e) {
	                    if(e.getResultCode() == LDAPException.LDAP_TIMEOUT || e.getResultCode() == LDAPException.CONNECT_ERROR)
	                       break;
	                    else
	                       continue;
	                }
	                c = LdapMappingManager.getInstance().mapToJamCaller(nextEntry);
	                if (c!=null) {                	
	                	cl.add(c);
	                }
	            }
            }
            // disconnect from the server
            lc.disconnect();
			LdapMappingManager.invalidate();
        } catch(LDAPException e) {
        	this.m_logger.log(Level.SEVERE, e.toString(), e);
        	throw new LdapContactsException(e.toString(), e);
        } catch( UnsupportedEncodingException e) {
        	this.m_logger.log(Level.SEVERE, e.toString(), e);
        }
        
		if (this.m_dbh!=null) {
			try {
				this.m_dbh.deleteAll();
				ICaller c = null;
				for (int i=0,j=cl.size();i<j;i++) {
					c = cl.get(i);
					if (c instanceof IMultiPhoneCaller) {
						List phones = ((IMultiPhoneCaller)c).getPhonenumbers();
						IPhonenumber pn = null;
						for (int k=0;k<phones.size();k++) {
							pn = (IPhonenumber) phones.get(k);
							this.m_dbh.insert(c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
						}
					} else {
						IPhonenumber pn = c.getPhoneNumber();
						this.m_dbh.insert(c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber());
					}
				}
			} catch (SQLException e) {
				throw new LdapContactsException(e.getMessage(), e);
			}
		} else {
			this.m_logger.warning("GoogleContacts proxy datahandler not initialized. Could not insert google contacts...");
		}
        
		return cl;
	}

	public synchronized ICaller identify(IPhonenumber pn) throws LdapContactsException {
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
									this.m_logger.info("LDAP contact found for UUID: "+uuid);
								}	
								contact.setUUID(new UUID().toString());
								if (contact instanceof IMultiPhoneCaller) {
									((IMultiPhoneCaller)contact).getPhonenumbers().clear();
								}
								contact.setPhoneNumber(pn);					
								contact.setAttribute(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION, pn.getTelephoneNumber()));
								return contact;
							}	
						} catch (LdapContactsException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}				
					}
				} else {
					Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(LdapRepository.NAMESPACE);
					if (config.getProperty(CFG_LDAP_KEEPEXT, "false").equalsIgnoreCase("true")) {
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
		if (this.m_logger.isLoggable(Level.INFO)) 
			this.m_logger.info("Caller not identified: "+pn.getTelephoneNumber());
		return null;
	}
	
	private ICaller identifyByUUID(String uuid) throws LdapContactsException {
		LDAPConnection lc = new LDAPConnection();
        try {
        	lc.connect(getServer(), getPort());
            lc.bind(LDAPConnection.LDAP_V3, getLoginUser(), getLoginPassword().getBytes("UTF8"));
            lc.read(uuid);
            LDAPEntry entry = lc.read(uuid); // return attrs and values            
            if (entry!=null) {            	
                ICaller c = LdapMappingManager.getInstance().mapToJamCaller(entry);
                if (c!=null) {                	
                	return c;
                }
            }
        } catch(LDAPException e) {
        	throw new LdapContactsException(e.toString(), e);
        } catch( UnsupportedEncodingException e) {
        	throw new LdapContactsException(e.toString(), e);
        } finally {
        	LdapMappingManager.invalidate();
        	try {
				lc.disconnect();
			} catch (LDAPException e) {
				throw new LdapContactsException(e.toString(), e);
			}			
        }
        return null;
	}
	
	public void start() {
		if (this.m_dbh==null)  {
			String db_path = PathResolver.getInstance(this.getRuntime())
			.resolve(PathResolver.getInstance(this.getRuntime()).getDataDirectory()+ "ldap_cache" + File.separator + "ldap_mapping.db");
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
	
			this.m_dbh = new LdapContactsProxyDatabaseHandler(
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

	private IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	private int getMaxResults() {
		String value = getRuntime().getConfigManagerFactory()
				.getConfigManager().getProperty(
						LdapRepository.NAMESPACE,
						CFG_LDAP_MAXRESULTS);
		return ((value != null && value.length() > 0) ? Integer.parseInt(value)
				: 500);
	}

	private String getLoginUser() {
		return getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(LdapRepository.NAMESPACE,
						CFG_LDAP_USER);
	}

	private String getLoginPassword() {
		return getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(LdapRepository.NAMESPACE,
						CFG_LDAP_PASSWORD);
	}
	
	private String getBaseDN() {
		return getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(LdapRepository.NAMESPACE,
						CFG_LDAP_BASE_DN);
	}
	
	private String getServer() {
		return getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(LdapRepository.NAMESPACE,
						CFG_LDAP_SERVER);
	}

	private int getPort() {
		String value = getRuntime().getConfigManagerFactory()
				.getConfigManager().getProperty(
						LdapRepository.NAMESPACE,
						CFG_LDAP_PORT);
		return ((value != null && value.length() > 0) ? Integer.parseInt(value)
				: LDAPConnection.DEFAULT_PORT);
	}
	
	private int getScope() {
		String value = getRuntime().getConfigManagerFactory()
				.getConfigManager().getProperty(
						LdapRepository.NAMESPACE,
						CFG_LDAP_SCOPE);
		return ((value != null && value.length() > 0) ? Integer.parseInt(value)
				: LDAPConnection.SCOPE_ONE);
	}

}
