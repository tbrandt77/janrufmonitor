package de.janrufmonitor.framework.configuration;

import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.Base64Decoder;
import de.janrufmonitor.util.io.Base64Encoder;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.framework.IJAMConst;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class TextFileConfigManager implements IConfigManager {
    
    private String ID = "TextFileConfigManager";
    private String SYSTEM_CONFIGURATION_FILE = "janrufmonitor-system.properties";
    private String USER_CONFIGURATION_FILE = "janrufmonitor.properties";
    private Properties m_systemConfiguration;
    private Properties m_userConfiguration;
    private String m_pathToSystemConfiguration;
    private String m_pathToUserConfiguration;
    private Logger m_logger;
    private boolean isSaveTriggered;
    private Timer m_timer;
    
    private static String NAMESPACE_SEPARATOR = ":";
    private static String FILEHEADER = "created by jAnrufmonitor configuration manager";
    
    private String CONFIG_VERSION = "~config.version";
    
    private String DEFAULT_VALUE_IDENTIFIER = "value";
    private String DEFAULT_VALUE_IDENTIFIER_VALUE = "";
    private String DEFAULT_TYPE_IDENTIFIER = "type";
    private String DEFAULT_TYPE_IDENTIFIER_VALUE = "text";
    private String DEFAULT_ACCESS_IDENTIFIER = "access";
    private String DEFAULT_ACCESS_IDENTIFIER_VALUE = "user";
    private String SYSTEM_ACCESS_IDENTIFIER_VALUE = "system";
    private String PASSWORD_TYPE_IDENTIFIER_VALUE = "password";
    private String PASSWORD_ENCRYPTED_TYPE_IDENTIFIER_VALUE = "password_encrypted";
    private String DEFAULT_DEFAULT_IDENTIFIER = "default";
   
    private String DEFAULT_TRUNCATE = "0";
	private String DEFAULT_DETECT_ALL_MSN = "true";
    private String DEFAULT_INTAREA = "49";
    private String DEFAULT_AREACODEADDLENGTH = "6";
    private String DEFAULT_INTAREA_PREFIX = "0";
    private String DEFAULT_CLIR = "( - - - - - )";
	private String DEFAULT_FORMAT = "+%intareacode% (%areacode%) %callnumber%";
	private String DEFAULT_CALLTIME_FORMAT = "%date% %time%";
	private String DEFAULT_TIME_FORMAT = "HH:mm:ss";
	private String DEFAULT_DATE_FORMAT = "dd.MM.yyyy";
	private String DEFAULT_CALLER_FORMAT = new StringBuffer(256).
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_LASTNAME + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX).
		append(", ").
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_FIRSTNAME + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX ).
		append("${").
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_ADDITIONAL + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX ).
		append("==??::").
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_CRLF).
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_ADDITIONAL + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX ).
		append("}$").		
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_CRLF).
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_STREET + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX ).
		append(" ").
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_STREET_NO + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX ).
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_CRLF).
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX ).
		append(" " ).
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_CITY + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX ).
		append(", ").
		append(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + IJAMConst.ATTRIBUTE_NAME_COUNTRY + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX).
		toString();
	private String DEFAULT_MSN_FORMAT = "%msn% (%msnalias%)";
	private String DEFAULT_INTERNAL_LENGTH = "2"; 
	
	private class ConfigSaverTask extends TimerTask {
		public void run() {
	        try {
	        	TextFileConfigManager.this.m_logger.info("Creating new configuration output stream.");

	        	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	        	TextFileConfigManager.this.m_systemConfiguration.store(bout, FILEHEADER);
	        	
	        	StringReader sr = new StringReader(bout.toString());
				BufferedReader bufReader = new BufferedReader(sr);
				List cfg = new ArrayList();
				while (bufReader.ready()) {
					String line = bufReader.readLine();
					if (line!=null)
						cfg.add(line);
					else
						break;
				}
				bufReader.close();
				sr.close();
				
				TextFileConfigManager.this.m_logger.info("Configuration read for sorting.");
				
				Collections.sort(cfg);
				
				TextFileConfigManager.this.m_logger.info("Configuration sorted.");
				
				FileWriter configWriter = new FileWriter(TextFileConfigManager.this.m_pathToSystemConfiguration);
				BufferedWriter bufWriter = new BufferedWriter(configWriter);
				for (int i = 0; i < cfg.size(); i++) {
					bufWriter.write((String)cfg.get(i));
					bufWriter.newLine();
				}
				bufWriter.flush();
				bufWriter.close();
				configWriter.close();
				TextFileConfigManager.this.m_logger.info("sorted Configuration stored.");
				
	        } catch (IOException ex) {
	        	TextFileConfigManager.this.m_logger.severe("IOException occured on file " + TextFileConfigManager.this.m_pathToSystemConfiguration + ". Please check, if file is existing and valid.");
	        }
	        try {
	        	TextFileConfigManager.this.m_logger.info("Creating new configuration output stream.");

	        	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	        	TextFileConfigManager.this.m_userConfiguration.store(bout, FILEHEADER);
	        	
	        	StringReader sr = new StringReader(bout.toString());
				BufferedReader bufReader = new BufferedReader(sr);
				List cfg = new ArrayList();
				while (bufReader.ready()) {
					String line = bufReader.readLine();
					if (line!=null)
						cfg.add(line);
					else
						break;
				}
				bufReader.close();
				sr.close();
				
				TextFileConfigManager.this.m_logger.info("Configuration read for sorting.");
				
				Collections.sort(cfg);
				
				TextFileConfigManager.this.m_logger.info("Configuration sorted.");
				
				FileWriter configWriter = new FileWriter(TextFileConfigManager.this.m_pathToUserConfiguration);
				BufferedWriter bufWriter = new BufferedWriter(configWriter);
				for (int i = 0; i < cfg.size(); i++) {
					bufWriter.write((String)cfg.get(i));
					bufWriter.newLine();
				}
				bufWriter.flush();
				bufWriter.close();
				configWriter.close();
				TextFileConfigManager.this.m_logger.info("sorted Configuration stored.");
				
	        } catch (IOException ex) {
	        	TextFileConfigManager.this.m_logger.severe("IOException occured on file " + TextFileConfigManager.this.m_pathToUserConfiguration + ". Please check, if file is existing and valid.");
	        }        
	        TextFileConfigManager.this.isSaveTriggered = false;
		}
	}
    
    public TextFileConfigManager() {
        this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
    }
    
    public String[] getConfigurationNamespaces() {
        List namespaces = new ArrayList();
        Iterator iter = this.m_systemConfiguration.keySet().iterator();
        String key = null;
        String namespace = null;
        while (iter.hasNext()) {
            key = (String) iter.next();
            namespace = key.substring(0, key.indexOf(NAMESPACE_SEPARATOR));
            if (!namespaces.contains(namespace)) {
                namespaces.add(namespace);
            }
        }
        iter = this.m_userConfiguration.keySet().iterator();
        while (iter.hasNext()) {
            key = (String) iter.next();
            namespace = key.substring(0, key.indexOf(NAMESPACE_SEPARATOR));
            if (!namespaces.contains(namespace)) {
                namespaces.add(namespace);
            }
        }        
        Collections.sort(namespaces);
        
        String[] namespacesList = new String[namespaces.size()];
        for (int i=0;i<namespaces.size();i++) {
			namespacesList[i] = (String) namespaces.get(i);
        }
        return namespacesList;
    }
    
    public Object getConfigurationSource() {
        return this.m_pathToUserConfiguration;
    }
    
    public String getManagerID() {
        return this.ID;
    }
    
    public int getPriority() {
        return -1;
    }
    
    public Properties getProperties(String namespace) {
        return this.getProperties(namespace, false);      
    }
    
    public Properties getProperties(String namespace, boolean selected) {
        Properties props = new Properties();
        Iterator iter = this.m_systemConfiguration.keySet().iterator();
        String key = null;
        while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace) && key.endsWith(this.DEFAULT_VALUE_IDENTIFIER)) {
                String value = this.m_systemConfiguration.getProperty(key);
                key = key.substring(namespace.length() + 1, (key.length() - NAMESPACE_SEPARATOR.length() - DEFAULT_VALUE_IDENTIFIER.length()));
                if (selected) {
                    String access = this.getProperty(namespace, key, DEFAULT_ACCESS_IDENTIFIER);
                    if (access.length()==0 || access.equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER_VALUE))
                    	props.setProperty(key, value);
                } else {
                	props.setProperty(key, value);
                }
            }
        }
        iter = this.m_userConfiguration.keySet().iterator();
        while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace) && key.endsWith(this.DEFAULT_VALUE_IDENTIFIER)) {
                String value = this.m_userConfiguration.getProperty(key);
                key = key.substring(namespace.length() + 1, (key.length() - NAMESPACE_SEPARATOR.length() - DEFAULT_VALUE_IDENTIFIER.length()));
                if (selected) {
                    String access = this.getProperty(namespace, key, DEFAULT_ACCESS_IDENTIFIER);
                    if (access.length()==0 || access.equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER_VALUE))
                    	props.setProperty(key, value);
                } else {
                	props.setProperty(key, value);
                }
            }
        }        
        return props;        
    }   
    
    public Properties getProperties(String namespace, String name) {
        return this.getProperties(namespace, name, false);     
    }
    
    public Properties getProperties(String namespace, String name, boolean selected) {
        Properties props = new Properties();
        Iterator iter = this.m_systemConfiguration.keySet().iterator();
        String key = null;
        while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace + NAMESPACE_SEPARATOR + name) && key.endsWith(this.DEFAULT_VALUE_IDENTIFIER)) {
                String value = this.m_systemConfiguration.getProperty(key);
                key = key.substring(namespace.length() + name.length() + 1, (key.length() - NAMESPACE_SEPARATOR.length() - DEFAULT_VALUE_IDENTIFIER.length()));
                
                if (selected) {
                    String access = this.getProperty(namespace, key, DEFAULT_ACCESS_IDENTIFIER);
                    if (access.length()==0 || access.equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER_VALUE))
                    	if (key.trim().length()>0) 
                     	   props.setProperty(key, value);
                } else {
                	if (key.trim().length()>0) 
                	   props.setProperty(key, value);
                }
            }
        }
        iter = this.m_userConfiguration.keySet().iterator();
        while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace + NAMESPACE_SEPARATOR + name) && key.endsWith(this.DEFAULT_VALUE_IDENTIFIER)) {
                String value = this.m_userConfiguration.getProperty(key);
                key = key.substring(namespace.length() + name.length() + 1, (key.length() - NAMESPACE_SEPARATOR.length() - DEFAULT_VALUE_IDENTIFIER.length()));
                
                if (selected) {
                    String access = this.getProperty(namespace, key, DEFAULT_ACCESS_IDENTIFIER);
                    if (access.length()==0 || access.equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER_VALUE))
                    	if (key.trim().length()>0) 
                     	   props.setProperty(key, value);
                } else {
                	if (key.trim().length()>0) 
                	   props.setProperty(key, value);
                }
            }
        }        
        return props;        
    }
    
    public String getProperty(String namespace, String name) {
        return this.getProperty(namespace, name, this.DEFAULT_VALUE_IDENTIFIER);
    }
    
    public String getProperty(String namespace, String name, String metadata) {
    	String value = this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata);
    	
    	// added 2014/04/29: decrypt password types
    	if (value!=null && value.length()>0) {
    		String type = this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_TYPE_IDENTIFIER);
    		if (type != null && type.length()>0 && (type.equalsIgnoreCase(PASSWORD_ENCRYPTED_TYPE_IDENTIFIER_VALUE))) {
    			try {
					this.m_logger.info("Password decryption step 1 - encrypted data: "+value);
					value = decrypt(value);
					this.m_logger.info("Password decryption step 2 - decrypted data: "+value);
				} catch (Exception e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} 
    		}
    	}
        return (value==null ? this.m_systemConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata, DEFAULT_VALUE_IDENTIFIER_VALUE) : value);        
    }
    
    private boolean existsProperty(String namespace, String name, String metadata) {
    	return this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata)!=null;
    }
    
    public void loadConfiguration() {
		this.m_logger.entering(TextFileConfigManager.class.getName(), "loadConfiguration");
        this.m_systemConfiguration = new Properties();
        this.m_userConfiguration = new Properties();
        try {
            FileInputStream fi = new FileInputStream(this.m_pathToSystemConfiguration);
            this.m_systemConfiguration.load(fi);
            fi.close();
        } catch (IOException ex) {
            this.m_logger.warning("IOException occured on file " + this.m_pathToSystemConfiguration + ". Please check, if file is existing and valid.");
        }
        try {
            FileInputStream fi = new FileInputStream(this.m_pathToUserConfiguration);
            this.m_userConfiguration.load(fi);
            fi.close();
        } catch (IOException ex) {
            this.m_logger.warning("IOException occured on file " + this.m_pathToSystemConfiguration + ". Please check, if file is existing and valid.");
        }        
		this.m_logger.exiting(TextFileConfigManager.class.getName(), "loadConfiguration"); 
    }
    
    public void removeProperties(String namespace) {
    	List listToBeRemoved = new ArrayList();
        Iterator iter = this.m_systemConfiguration.keySet().iterator();
        String key = null;
        while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace)) {
                listToBeRemoved.add(key);
            }
        }
        iter = this.m_userConfiguration.keySet().iterator();
        while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace)) {
                listToBeRemoved.add(key);
            }
        }        
        for (int i = 0; i < listToBeRemoved.size(); i++) {
        	this.m_systemConfiguration.remove(listToBeRemoved.get(i));
        	this.m_userConfiguration.remove(listToBeRemoved.get(i));
        }       
    }
     
    public void removeProperty(String namespace, String name) {
        List listToBeRemoved = new ArrayList();
    	Iterator iter = this.m_systemConfiguration.keySet().iterator();
    	String key = null;
    	while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace + NAMESPACE_SEPARATOR + name)) {
            	listToBeRemoved.add(key);
            }
        }  
    	iter = this.m_userConfiguration.keySet().iterator();
    	while (iter.hasNext()) {
            key = (String) iter.next();
            if (key.startsWith(namespace + NAMESPACE_SEPARATOR + name)) {
            	listToBeRemoved.add(key);
            }
        }      	
        for (int i = 0; i < listToBeRemoved.size(); i++) {
        	this.m_systemConfiguration.remove(listToBeRemoved.get(i));
        	this.m_userConfiguration.remove(listToBeRemoved.get(i));
        }    
    }
    
    public void removeProperty(String namespace, String name, String metadata) {
    	this.m_systemConfiguration.remove(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata);
    	this.m_userConfiguration.remove(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata);
    }
    
    public synchronized void saveConfiguration() {
		this.m_logger.entering(TextFileConfigManager.class.getName(), "saveConfiguration");
 
		if (!this.isSaveTriggered) {
			this.isSaveTriggered = true;
			if (this.m_timer!=null)
				this.m_timer.cancel();
			
			this.m_timer = new Timer();
			this.m_timer.schedule(new ConfigSaverTask(), 750);
		}
		
		this.m_logger.exiting(TextFileConfigManager.class.getName(), "saveConfiguration");      
    }
    
    public void setConfigurationSource(Object obj) {
        this.m_pathToUserConfiguration = (String) obj;
    }
    
    public void setProperties(String namespace, Properties props) {
        Iterator iter = props.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            this.setProperty(namespace, key, props.getProperty(key, DEFAULT_VALUE_IDENTIFIER_VALUE));
        }        
    }
    
    public void setProperty(String namespace, String name, String value) {
        this.setProperty(namespace, name, this.DEFAULT_VALUE_IDENTIFIER, value);
    }
    
    private boolean isUserAccess(String namespace, String name) {
    	String accessType = this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER);
    	if (this.m_logger.isLoggable(Level.FINE)) {
    		this.m_logger.fine("Property is User Access: "+namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER + "=" + (accessType!=null && accessType.equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER_VALUE)));
    	}
    	
    	return (accessType!=null && accessType.equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER_VALUE));
    }
    
    private boolean isSystemAccess(String namespace, String name) {
    	String accessType = this.m_systemConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER);
    	if (this.m_logger.isLoggable(Level.FINE)) {
    		this.m_logger.fine("Property is System Access: "+namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER + "=" + (accessType!=null && accessType.equalsIgnoreCase(SYSTEM_ACCESS_IDENTIFIER_VALUE)));
    	}
    	return (accessType!=null && accessType.equalsIgnoreCase(SYSTEM_ACCESS_IDENTIFIER_VALUE));
    }
    
    private boolean isInitialAccess(Properties p, String namespace, String name) {
    	String accessType = p.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER);
    	if (this.m_logger.isLoggable(Level.FINE)) {
    		this.m_logger.fine("Property is Initial Access: "+namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER + "=" + (accessType==null));
    	}
    	return (accessType==null);
    }
    
    private void interalSetUserProperty(String namespace, String name, String metadata, String value) {
        if (metadata.equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER)) {
			if (value.equalsIgnoreCase(SYSTEM_ACCESS_IDENTIFIER_VALUE)) {
				this.copyToSystem(namespace, name, metadata, value);
				return;
			}
		}   
    	if (!metadata.equalsIgnoreCase(this.DEFAULT_ACCESS_IDENTIFIER)) {
        	String v = this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER);
        	if (v==null) {
        		this.m_userConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER, DEFAULT_ACCESS_IDENTIFIER_VALUE);
        	}
        }
        if (!metadata.equalsIgnoreCase(this.DEFAULT_TYPE_IDENTIFIER)) {
        	String v = this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_TYPE_IDENTIFIER);
        	if (v==null) {
        		this.m_userConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_TYPE_IDENTIFIER, DEFAULT_TYPE_IDENTIFIER_VALUE);
        	}
        }
        
        if (metadata.equalsIgnoreCase(this.DEFAULT_VALUE_IDENTIFIER)) {
        	String v = this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_DEFAULT_IDENTIFIER);
        	if (v==null){
        		this.m_userConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_DEFAULT_IDENTIFIER, value);
        	}
        	
        	// added 2014/04/29: check for password type
        	v =  this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_TYPE_IDENTIFIER);
            if (v!=null && v.length()>0 && value!= null & value.length()>0 && (v.equalsIgnoreCase(PASSWORD_TYPE_IDENTIFIER_VALUE) || v.equalsIgnoreCase(PASSWORD_ENCRYPTED_TYPE_IDENTIFIER_VALUE))) {
            	try {
            		this.m_logger.info("Password encryption step 1 - raw data: "+value);
            		value = encrypt(value);
            		this.m_logger.info("Password encryption step 2 - encrypted data: "+value);
    				this.m_userConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_TYPE_IDENTIFIER, PASSWORD_ENCRYPTED_TYPE_IDENTIFIER_VALUE);
    			} catch (Exception e) {
    				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
    			} 
            }
        }

        // added 2014/04/29: check for password type
        if (metadata.equalsIgnoreCase(this.DEFAULT_TYPE_IDENTIFIER) && value != null && (value.equalsIgnoreCase(PASSWORD_TYPE_IDENTIFIER_VALUE))) {
			String v = this.m_userConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_VALUE_IDENTIFIER);
			if (v!=null && v.length()>0) {
				try {
					this.m_logger.info("Password encryption step 1 - raw data: "+v);
					v = encrypt(v);
					this.m_logger.info("Password encryption step 2 - encrypted data: "+v);
					this.m_userConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_VALUE_IDENTIFIER, v);
					value = PASSWORD_ENCRYPTED_TYPE_IDENTIFIER_VALUE;
				} catch (Exception e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} 
			}
        }
        
        this.m_userConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata, value);     
    }
    
    private void interalSetSystemProperty(String namespace, String name, String metadata, String value) {
    	if (!metadata.equalsIgnoreCase(this.DEFAULT_ACCESS_IDENTIFIER)) {
        	String v = this.m_systemConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER);
        	if (v==null) {
        		this.m_systemConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER, DEFAULT_ACCESS_IDENTIFIER_VALUE);
        	}
        }
        if (!metadata.equalsIgnoreCase(this.DEFAULT_TYPE_IDENTIFIER)) {
        	String v = this.m_systemConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_TYPE_IDENTIFIER);
        	if (v==null) {
        		this.m_systemConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_TYPE_IDENTIFIER, DEFAULT_TYPE_IDENTIFIER_VALUE);
        	}
        } 
        
        if (metadata.equalsIgnoreCase(this.DEFAULT_VALUE_IDENTIFIER)) {
        	String v = this.m_systemConfiguration.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_DEFAULT_IDENTIFIER);
        	if (v==null){
        		this.m_systemConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_DEFAULT_IDENTIFIER, value);
        	}
        }
        this.copyToSystem(namespace, name, metadata, value);
    }
    
    public void setProperty(String namespace, String name, String metadata, String value) {   	
    	this.setProperty(namespace, name, metadata, value, true);
    }
    
    public void setProperty(String namespace, String name, String metadata, String value, boolean overwrite) {
    	if (this.m_logger.isLoggable(Level.FINE)) {
    		this.m_logger.fine("Property set call: "+namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata + "=" + value);
    	}
    	
    	if (!overwrite && metadata.equalsIgnoreCase(DEFAULT_VALUE_IDENTIFIER)) {
    		if (this.existsProperty(namespace, name, metadata)) {
    			this.m_logger.info("Property already extist: "+namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata+ " and will not be overwritten.");
    			return;
    		}
    	}
    	
    	if (this.isUserAccess(namespace, name)) {
    		this.interalSetUserProperty(namespace, name, metadata, value);
    		return;
    	}
    	
    	if (this.isSystemAccess(namespace, name)) {
    		this.interalSetSystemProperty(namespace, name, metadata, value);
    		return;
    	}
    	
    	if (this.isInitialAccess(this.m_userConfiguration, namespace, name) && 
    		this.isInitialAccess(this.m_systemConfiguration, namespace, name)) {
    		
    		this.interalSetUserProperty(namespace, name, metadata, value);
    		return;
    	}
    	
    	// no system property but also not initial in systems -> thats wrong
    	// set it to correct system property
    	if (!this.isSystemAccess(namespace, name) &&
    		!this.isInitialAccess(this.m_systemConfiguration, namespace, name)) {
    		this.m_logger.info("Correcting property set call: "+namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata + "=" + value);
    		this.interalSetSystemProperty(namespace, name, metadata, value);
    		return;
    	}
    	this.m_logger.warning("Invalid property set call: "+namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata + "=" + value);
    }
    
    private void copyToSystem(String namespace, String name, String metadata, String value) {
		Iterator iter = this.m_userConfiguration.keySet().iterator();
    	List l = new ArrayList();
    	String key = null;
    	
    	while (iter.hasNext()) {
    		key = (String) iter.next();
    		if (key.startsWith(namespace + NAMESPACE_SEPARATOR + name)) {
    			l.add(key);
    		}
    	}
    	
    	if (l.size()>0) {
    		String kvalue = null;
    		for (int i=0,size=l.size();i<size;i++) {
    			kvalue = (String) l.get(i);
    			this.m_systemConfiguration.setProperty(
    					kvalue,
					this.m_userConfiguration.getProperty(kvalue)
    			);
    			this.m_userConfiguration.remove(kvalue);
    		}
    	}
		
    	this.m_systemConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata, value);
    	//this.m_systemConfiguration.setProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER, SYSTEM_ACCESS_IDENTIFIER_VALUE);
		this.m_userConfiguration.remove(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + metadata);
    }
    
    public boolean isActive(){
    	return true;
    }
    
    private String encrypt(String s) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
    	return Base64Encoder.encode(s);
    	
//    	// Create key and cipher
//    	Key aesKey = new SecretKeySpec("jAnrufm0nit0r500".getBytes(), "AES");
//    	Cipher cipher = Cipher.getInstance("AES");
//    	
//    	// encrypt the text
//    	cipher.init(Cipher.ENCRYPT_MODE, aesKey);
//    	return new String(cipher.doFinal(s.getBytes()));
    }
    
    private String decrypt(String s) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
    	if (Base64Decoder.isBase64(s))
    		return Base64Decoder.decode(s);
    	return s;
    	
//      // Create key and cipher
//      Key aesKey = new SecretKeySpec("jAnrufm0nit0r500".getBytes(), "AES");
//      Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
//
//      // decrypt the text
//      cipher.init(Cipher.DECRYPT_MODE, aesKey);
//      return new String(cipher.doFinal(s.getBytes()));
    }
  
    private void checkConfigConsistency() {
		this.m_logger.entering(TextFileConfigManager.class.getName(), "checkConfigConsistency");
    	boolean changed = false;
    	
    	// check config version
        String version = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, this.CONFIG_VERSION, "value");
        if (version == null || version.length()==0 || version.compareTo(IJAMConst.VERSION_DISPLAY)<0) {
            //convertConfiguartion();
        	this.setProperty(IJAMConst.GLOBAL_NAMESPACE, this.CONFIG_VERSION, "value", IJAMConst.VERSION_DISPLAY);
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, this.CONFIG_VERSION, "type", "text");
			changed = true;
			this.m_logger.info("Set config version.");
        }     
        
        String intArea = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA, "value");
        if (intArea == null || intArea.length()==0) {
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA, "value", this.DEFAULT_INTAREA);
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA, "type", "text");
			changed = true;
			this.m_logger.info("Changed intarea code.");
        }
        String areacodeaddlength = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE_ADD_LENGTH, "value");
        if (areacodeaddlength == null || areacodeaddlength.length()==0) {
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE_ADD_LENGTH, "value", this.DEFAULT_AREACODEADDLENGTH);
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE_ADD_LENGTH, "type", "text");
			changed = true;
			this.m_logger.info("Changed area code add length.");
        }
        String intAreaPrefix = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA_PREFIX, "value");
        if (intAreaPrefix == null || intAreaPrefix.length()==0) {
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA_PREFIX, "value", this.DEFAULT_INTAREA_PREFIX);
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA_PREFIX, "type", "text");
			changed = true;
			this.m_logger.info("Changed area code.");
        }
        
        String language = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_LANGUAGE, "value");
        if (language == null || language.length()==0) {
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_LANGUAGE, "value", System.getProperty("user.language", "de"));
			changed = true;
			this.m_logger.info("Changed language.");
        }
        String truncate = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRUNCATE, "value");
        if (truncate == null || truncate.length()==0) {
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRUNCATE, "value", this.DEFAULT_TRUNCATE);
			changed = true;
			this.m_logger.info("Changed truncate.");
        }
		String trace = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRACE, "value");
		if (trace == null || trace.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRACE, "value", "false");
			changed = true;
			this.m_logger.info("Changed trace.");
		}
        String clir = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_CLIR, "value");
        if (clir == null || clir.length()==0) {
            this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_CLIR, "value", this.DEFAULT_CLIR);
			changed = true;
			this.m_logger.info("Changed CLIR.");
        }
		
		String detectmsn = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DETECT_ALL_MSN, "value");
		if (detectmsn == null || detectmsn.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DETECT_ALL_MSN, "value", this.DEFAULT_DETECT_ALL_MSN);
			changed = true;
			this.m_logger.info("Changed autodetect all MSNs.");
		}
        
		String ilength = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTERNAL_LENGTH, "value");
		if (ilength == null || ilength.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTERNAL_LENGTH, "value", this.DEFAULT_INTERNAL_LENGTH);
			changed = true;
			this.m_logger.info("Changed internal number length.");
		}
		
		String var_callername = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLERNAME, "value");
		if (var_callername == null || var_callername.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLERNAME, "value", DEFAULT_CALLER_FORMAT);
			changed = true;
			this.m_logger.info("Changed variable callername.");
		}
		
		String var_callernumber = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, "value");
		if (var_callernumber == null || var_callernumber.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, "value", DEFAULT_FORMAT);
			changed = true;
			this.m_logger.info("Changed variable callernumber.");
		}
		
		String var_msnformat = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_MSNFORMAT, "value");
		if (var_msnformat == null || var_msnformat.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_MSNFORMAT, "value", DEFAULT_MSN_FORMAT);
			changed = true;
			this.m_logger.info("Changed variable msnformat.");
		}
		
		String var_calltime = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLTIME, "value");
		if (var_calltime == null || var_calltime.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLTIME, "value", this.DEFAULT_CALLTIME_FORMAT);
			changed = true;
			this.m_logger.info("Changed variable call time.");
		}

		String var_date = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_DATE, "value");
		if (var_date == null || var_date.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_DATE, "value", this.DEFAULT_DATE_FORMAT);
			changed = true;
			this.m_logger.info("Changed variable date.");
		}
		
		String var_time = this.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_TIME, "value");
		if (var_time == null || var_time.length()==0) {
			this.setProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_TIME, "value", this.DEFAULT_TIME_FORMAT);
			changed = true;
			this.m_logger.info("Changed variable time.");
		}
		
        // check for valid i18n manager
        String i18nmanager = this.getProperty("i18n.I18nManagerFactory", "manager", "value");
        if (i18nmanager == null || i18nmanager.length()==0) {
            this.setProperty("i18n.I18nManagerFactory", "manager", "value", "de.janrufmonitor.framework.i18n.DatabaseI18nManager");
            this.setProperty("i18n.I18nManagerFactory", "manager", "access", "system");
            this.setProperty("i18n.DatabaseI18nManager", "defaultlanguage", "value", "de");
            this.setProperty("i18n.DatabaseI18nManager", "defaultlanguage", "access", "system");
            this.setProperty("i18n.DatabaseI18nManager", "identifier", "value", "label, description");
            this.setProperty("i18n.DatabaseI18nManager", "identifier", "access", "system");
			changed = true;
			this.m_logger.info("Changed I18n manager.");
        }
        // 2005/12/27: Always change this property, due to migration from old file strcuture
        this.setProperty("i18n.DatabaseI18nManager", "database", "value", "%configpath%i18n");
        this.setProperty("i18n.DatabaseI18nManager", "database", "access", "system");

        if (changed)
			this.saveConfiguration();
			
        this.m_logger.info("Consistency check completed.");
		this.m_logger.exiting(TextFileConfigManager.class.getName(), "checkConfigConsistency");
    }

//	private void convertConfiguartion() {
//		if (this.m_userConfiguration.size()==0) {
//			this.m_logger.info("No conversion needed. User configuration is empty.");
//			return;
//		}
//		Properties tmpOldDate = (Properties) this.m_userConfiguration.clone();
//		this.m_userConfiguration.clear();
//		
//		StringTokenizer st = null;
//		Iterator iter = tmpOldDate.keySet().iterator();
//		String key = null;
//		String namespace = null;
//		String name = null;
//		String metadata = null;
//		while (iter.hasNext()) {
//			key = (String)iter.next();
//			if (key!=null && key.length()>0) {
//				st = new StringTokenizer(key, NAMESPACE_SEPARATOR);
//				if (st.countTokens()==3) {
//					namespace = st.nextToken();
//					name = st.nextToken();
//					metadata = st.nextToken();
//					
//					if (tmpOldDate.getProperty(namespace + NAMESPACE_SEPARATOR + name + NAMESPACE_SEPARATOR + DEFAULT_ACCESS_IDENTIFIER).equalsIgnoreCase(DEFAULT_ACCESS_IDENTIFIER_VALUE)	
//						) {
//						this.setProperty(
//							namespace,
//							name,
//							metadata,
//							tmpOldDate.getProperty(key, "")
//						);
//					} else {
//						this.m_logger.info("System entry was dropped: "+key);
//					}
//				} else {
//					this.m_logger.warning("Error in converting entry: "+key+". Tokencount is "+st.countTokens()+". Valid count is 3");
//				}
//			}
//		}
//		this.m_logger.info("Converted "+tmpOldDate.size()+" old configuration entries.");
//	}

	public void startup() {
		this.m_pathToSystemConfiguration = PathResolver.getInstance(PIMRuntime.getInstance()).getConfigDirectory() + this.SYSTEM_CONFIGURATION_FILE;
		this.m_pathToUserConfiguration = PathResolver.getInstance(PIMRuntime.getInstance()).getConfigDirectory() + this.USER_CONFIGURATION_FILE;
		this.loadConfiguration();
		this.checkConfigConsistency();
	}

	public void shutdown() {
		this.m_systemConfiguration.clear();
		this.m_userConfiguration.clear();
	}

	public void restart() {
		this.shutdown();
		this.startup();
	}

	public boolean isSupported(Class c) {
		return false;
	}

	public void setManagerID(String id) {
	}    
}
