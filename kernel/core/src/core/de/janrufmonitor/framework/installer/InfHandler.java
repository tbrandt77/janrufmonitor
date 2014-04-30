package de.janrufmonitor.framework.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.configuration.IConfigManager;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.string.StringUtils;

public class InfHandler {

	private Logger m_logger;
	private String NAMESPACE_SEPARATOR = ":";
	private boolean overwriteConfig = true;
	
	public InfHandler() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
	}
	
	public void setOverwriteConfiguration(boolean overwrite) {
		this.overwriteConfig = overwrite;
	}
	
	public void removeInfData(Properties infData) {
        try {
            IConfigManager cpm = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager();
            Iterator iter = infData.keySet().iterator();
            String key = null;
            while (iter.hasNext()) {
                key = (String) iter.next();
                StringTokenizer st = new StringTokenizer(key, this.NAMESPACE_SEPARATOR);
                String namespace = st.nextToken();
                String parameter = st.nextToken();
                String metainfo = st.nextToken();
                if (namespace.startsWith("-")) {
                	String value = infData.getProperty(key);
                	namespace = namespace.substring(1);
					this.m_logger.info("Detected module config removal.");
					this.m_logger.info("Adding "+namespace+"\\:"+parameter+"\\:"+metainfo);
					
					String v = cpm.getProperty(namespace, parameter, metainfo);
					if (v==null) v = "";
					//value += infData.getProperty(key);
					v = this.concat(v, infData.getProperty(key));
					cpm.setProperty(namespace, parameter, metainfo, value, true);
                } else if (namespace.startsWith("+")) {
                	namespace = namespace.substring(1);
					this.m_logger.info("Detected module config concatenation.");
					this.m_logger.info("De-Concatenating values for "+namespace+"\\:"+parameter+"\\:"+metainfo);
					String value = cpm.getProperty(namespace, parameter, metainfo);
					value = StringUtils.replaceString(value, infData.getProperty(key, "null"), "");
					if (value==null) value = "";

					cpm.setProperty(namespace, parameter, metainfo, value, true);
                } else if (namespace.startsWith("~")) {
                	namespace = namespace.substring(1);
					this.m_logger.info("Detected module config restore.");
					this.m_logger.info("Restoring values for "+namespace+"\\:"+parameter+"\\:"+metainfo);
					
					String value = cpm.getProperty("_"+namespace, parameter, metainfo);
					if (value==null) value = "";
					
					if (value.length()>0) {
						cpm.setProperty(namespace, parameter, metainfo, value, true); // config restore means allways overwrite !!
					} else {
						cpm.removeProperty(namespace, parameter, metainfo);
					}
					cpm.removeProperty("_"+namespace, parameter, metainfo);
                } else if (namespace.startsWith("?")) { 
                	namespace = namespace.substring(1);
                	this.m_logger.info("Detected module config add only if not present feature.");
                	this.m_logger.info("Removing value for "+namespace+"\\:"+parameter+"\\:"+metainfo);
                	cpm.removeProperty(namespace, parameter, metainfo);
                } else if (namespace.startsWith("%")) { 
                	namespace = namespace.substring(1);
                	this.m_logger.info("Detected module config add always feature.");
                	this.m_logger.info("Removing value for "+namespace+"\\:"+parameter+"\\:"+metainfo);
                	cpm.removeProperty(namespace, parameter, metainfo);
                }
                else {    				
    				cpm.removeProperty(namespace, parameter, metainfo);
                }
            }
            cpm.saveConfiguration();
        } catch (NullPointerException ex) {
            this.m_logger.severe("Configuration is invalid: " + ex.getMessage());
        } catch (NoSuchElementException ex) {
            this.m_logger.severe("Configuration is invalid: " + ex.getMessage());
        }
	}
	
	public void removeInfData(File infFile) {
        if (!infFile.getName().toLowerCase().endsWith(InstallerConst.EXTENSION_INITINF) ||
        	!infFile.getName().toLowerCase().endsWith(InstallerConst.EXTENSION_INF)) {
            this.m_logger.severe("File is not an INF file: " + infFile.getName());
            return;
        }
        Properties props = new Properties();
        
		try {
			FileInputStream fis = new FileInputStream(infFile);
			props.load(fis);
	        fis.close();
	        
	        this.removeInfData(props);
	        
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void addInfData(Properties infData) {
        try {
            IConfigManager cpm = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager();
            Iterator iter = infData.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                StringTokenizer st = new StringTokenizer(key, this.NAMESPACE_SEPARATOR);
                String namespace = st.nextToken();
                String parameter = st.nextToken();
                String metainfo = st.nextToken();
                // remove
                if (namespace.startsWith("-")) {
                	namespace = namespace.substring(1);
					this.m_logger.info("Detected module config removal.");
					this.m_logger.info("Removing "+namespace+"\\:"+parameter+"\\:"+metainfo);
					cpm.removeProperty(namespace, parameter, metainfo);
					
				// add value to current value
                } else if (namespace.startsWith("+")) {
                	namespace = namespace.substring(1);
					this.m_logger.info("Detected module config concatenation.");
					this.m_logger.info("Concatenating values for "+namespace+"\\:"+parameter+"\\:"+metainfo);
					String value = cpm.getProperty(namespace, parameter, metainfo);
					if (value==null) value = "";
					//value += infData.getProperty(key);
					value = this.concat(value, infData.getProperty(key));
					cpm.setProperty(namespace, parameter, metainfo, value, true); // concatenating means allways overwrite !!
				
				// restore option: add always (force) 
                } else if (namespace.startsWith("~")) {
                	// added: 2007/07/09: for config restore capability
                	// restore option trigger
                	namespace = namespace.substring(1);
                	
                	this.m_logger.info("Detected module config restore feature.");
					this.m_logger.info("Preparing restore of values for "+namespace+"\\:"+parameter+"\\:"+metainfo);
					String value = cpm.getProperty(namespace, parameter, metainfo);
					if (value==null) value = "";
					
					if (value.length()>0) {
						cpm.setProperty("_"+namespace, parameter, metainfo, value, true); // config restore means allways overwrite !!
					}
					value = infData.getProperty(key);
					cpm.setProperty(namespace, parameter, metainfo, value, true);	
					
				// add, only if not already set	
                } else if (namespace.startsWith("?")) {
                	namespace = namespace.substring(1);
                	this.m_logger.info("Detected module config add only if not present feature.");
					this.m_logger.info("Checking value for "+namespace+"\\:"+parameter+"\\:"+metainfo);
					String value = cpm.getProperty(namespace, parameter, metainfo);
					if (value==null) {
						value = infData.getProperty(key);
						cpm.setProperty(namespace, parameter, metainfo, value, true);
						this.m_logger.info("Addings value for "+namespace+"\\:"+parameter+"\\:"+metainfo);
					} else {
						this.m_logger.info("Value for "+namespace+"\\:"+parameter+"\\:"+metainfo+" already exists.");
					}
				// add always (force)
                } else if (namespace.startsWith("%")) {
                	namespace = namespace.substring(1);
                	this.m_logger.info("Detected module config add always (force) feature.");
					this.m_logger.info("Forcing value for "+namespace+"\\:"+parameter+"\\:"+metainfo);					
					String value = infData.getProperty(key);
					if (value==null) value = "";
					cpm.setProperty(namespace, parameter, metainfo, value, true);
                } else {
					String value = infData.getProperty(key);
					cpm.setProperty(namespace, parameter, metainfo, value, overwriteConfig);
                }
            }
            cpm.saveConfiguration();
        } catch (NullPointerException ex) {
            this.m_logger.severe("Configuration is invalid: " + ex.getMessage());
        } catch (NoSuchElementException ex) {
            this.m_logger.severe("Configuration is invalid: " + ex.getMessage());
        }
	}
	
	public void addInfData(File infFile) {
        if (!infFile.getName().toLowerCase().endsWith(InstallerConst.EXTENSION_INITINF) &&
        	!infFile.getName().toLowerCase().endsWith(InstallerConst.EXTENSION_INF)) {
            this.m_logger.severe("File is not an INF file: " + infFile.getName());
            return;
        }
        Properties props = new Properties();
		try {
			FileInputStream fis = new FileInputStream(infFile);
			props.load(fis);
	        fis.close();
	        this.addInfData(props);
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private String concat(String oldValue, String add) {
		if (add==null || add.length()==0) return oldValue;
		
		List l = new ArrayList();
		
		StringTokenizer st = new StringTokenizer(oldValue, ",");
		String t = null;
		while(st.hasMoreTokens()) {
			t = st.nextToken();
			if (!l.contains(t))	l.add(t);
		}
		
		StringTokenizer st2 = new StringTokenizer(add, ",");
		while(st2.hasMoreTokens()) {
			t = st2.nextToken();
			if (!l.contains(t)) l.add(t);
		}
		
		StringBuffer b = new StringBuffer(l.toString().length());
		for (int i=0, j=l.size();i<j;i++) {
			b.append((String)l.get(i));
			b.append(",");
		}
		
		return b.toString();
	}
	
}
