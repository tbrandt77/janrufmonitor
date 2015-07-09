package de.janrufmonitor.repository.mapping;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.util.Base64;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.repository.LdapRepository;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;

public class LdapMappingManager {

	private static LdapMappingManager m_instance = null;
	private Logger m_logger;
	private IRuntime m_runtime;
	
	
	private String LDAP_IMAGE_CACHE_PATH = PathResolver.getInstance()
			.getPhotoDirectory()
			+ File.separator + "ldap-contacts" + File.separator;
	
	Properties m_attributeMappings;
	Properties m_inversAttributeMappings;
	Properties m_phonenumberMappings;
	
	private LdapMappingManager() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		try {
			loadMappings();
		} catch (IOException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
		}
		File cache = new File(LDAP_IMAGE_CACHE_PATH);
		if (!cache.exists())
			cache.mkdirs();
	}
	
	public static synchronized LdapMappingManager getInstance() {
		if (LdapMappingManager.m_instance == null) {
			LdapMappingManager.m_instance = new LdapMappingManager();
		}
		return LdapMappingManager.m_instance;
	}
	
	public static void invalidate() {
		LdapMappingManager.m_instance = null;
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	private void loadMappings() throws IOException {
		m_attributeMappings = new Properties();
		m_inversAttributeMappings = new Properties();
		m_phonenumberMappings = new Properties();
		File attributeMappingFile = new File(PathResolver.getInstance(getRuntime()).getConfigDirectory(), "ldap-attribute-mapping.properties");
		File phonenumberMappingFile = new File(PathResolver.getInstance(getRuntime()).getConfigDirectory(), "ldap-phone-mapping.properties");
		
		if (attributeMappingFile.exists()) {
			FileInputStream fis = new FileInputStream(attributeMappingFile);
			m_attributeMappings.load(fis);
			fis.close();
			
			Iterator i = m_attributeMappings.keySet().iterator();
			String key = null;
			while (i.hasNext()) {
				key = (String) i.next();
				m_inversAttributeMappings.setProperty(m_attributeMappings.getProperty(key), key);
			}
		} 
		
		if (phonenumberMappingFile.exists()) {
			FileInputStream fis = new FileInputStream(phonenumberMappingFile);
			m_phonenumberMappings.load(fis);
			fis.close();
		} 
	}
	
	public ICaller mapToJamCaller(LDAPEntry entry) {
		IAttributeMap attributes = mapAttributes(entry);
		if (attributes==null || attributes.size()==0) {
			m_logger.warning("The LDAP entry "+entry.getDN()+" has no matching attributes.");
			return null;
		}
		
		attributes.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
				LdapRepository.ID));

		List phones = mapPhones(entry, attributes);
		
		if (phones==null||phones.size()==0) {
			m_logger.warning("The LDAP entry "+entry.getDN()+" has no matching phonenumbers.");
			return null;
		}
		return getRuntime().getCallerFactory().createCaller(entry.getDN(), null, phones, attributes);
	}
	
	public String getLdapAttribute(String jamAttrib) {
		return m_attributeMappings.getProperty(jamAttrib);
	}
	
	private IAttributeMap mapAttributes(LDAPEntry entry) {
		IAttributeMap attributes = getRuntime().getCallerFactory().createAttributeMap();
		LDAPAttributeSet attributeSet = entry.getAttributeSet();
        Iterator allAttributes = attributeSet.iterator();

        while(allAttributes.hasNext()) {
            LDAPAttribute attribute =
                        (LDAPAttribute)allAttributes.next();

            if (m_attributeMappings.values().contains(attribute.getName())) {
            	String value = attribute.getStringValue();
                if (m_logger.isLoggable(Level.INFO)) {
                	m_logger.info("Adding LDAP attribute: "+attribute.getName()+", value: "+value);
                }
                if (!m_inversAttributeMappings.getProperty(attribute.getName()).equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)) {
                	attributes.add(getRuntime().getCallerFactory().createAttribute(m_inversAttributeMappings.getProperty(attribute.getName()), value));	
                } else {                        	
                	try {
						addImage(attributes, attribute.getByteValue(), entry);
					} catch (IOException e) {
						m_logger.log(Level.SEVERE, e.toString(), e);
					}
                }  
            }
        }
		return attributes;
	}
	
	private String normalizeDN(String dn) {
		dn = StringUtils.replaceString(dn, "=", "_");
		dn = StringUtils.replaceString(dn, ",", "_");
		dn = StringUtils.replaceString(dn, ";", "_");
		return dn;
	}
	
	private void addImage(IAttributeMap attributes, byte[] imageData, LDAPEntry entry) throws IOException {
		String dn = entry.getDN();
		dn = normalizeDN(dn);

		File imgFile = new File(LDAP_IMAGE_CACHE_PATH, dn+".jpg");
	
		ByteArrayInputStream bin = new ByteArrayInputStream(imageData);
		FileOutputStream fos = new FileOutputStream(imgFile);
		
		Stream.copy(bin, fos, true);
		
		if (imgFile.exists()) {
			attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, imgFile.getAbsolutePath()));
		}
	}

	private List mapPhones(LDAPEntry entry, IAttributeMap attributes) {
		List phones = new ArrayList();
		LDAPAttributeSet attributeSet = entry.getAttributeSet();
        Iterator allAttributes = attributeSet.iterator();

        while(allAttributes.hasNext()) {
            LDAPAttribute attribute =
                        (LDAPAttribute)allAttributes.next();

            if (m_phonenumberMappings.keySet().contains(attribute.getName())) {
            	Enumeration allValues = attribute.getStringValues();
                if( allValues != null) {
                	String value = null;
                    while(allValues.hasMoreElements()) {
                        value = (String) allValues.nextElement();
                        if (!Base64.isLDIFSafe(value)) {
                            value = attribute.getStringValue();
                        }
                        if (m_logger.isLoggable(Level.INFO)) {
                        	m_logger.info("Adding LDAP attribute: "+attribute.getName()+", value: "+value);
                        }
                        value = PhonenumberAnalyzer.getInstance().normalize(value.trim());
                        IPhonenumber pn = PhonenumberAnalyzer.getInstance().toClirPhonenumber(value);
                        if (pn==null) {
                        	pn = PhonenumberAnalyzer.getInstance().toInternalPhonenumber(value, null);
                        	if (pn!=null) {
                        		phones.add(pn);
                            	attributes.add(getNumberTypeAttribute(attribute.getName(), pn));
                        	}
                        }
                        if (pn==null) {
                        	pn = PhonenumberAnalyzer.getInstance().toPhonenumber(value, null);
                        	 ICaller c = Identifier.identifyDefault(getRuntime(), pn);
                             if (c!=null) {
                             	phones.add(c.getPhoneNumber());
                             	attributes.add(getNumberTypeAttribute(attribute.getName(), c.getPhoneNumber()));
                             }
                        }
                    }
                }
            }
        }
		return phones;
	}
	
	private IAttribute getNumberTypeAttribute(String ldapNumberType, IPhonenumber pn) {
		return getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), m_phonenumberMappings.getProperty(ldapNumberType));
	}
}
