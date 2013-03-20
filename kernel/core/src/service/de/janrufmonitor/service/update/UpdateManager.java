package de.janrufmonitor.service.update;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.installer.InstallerConst;
import de.janrufmonitor.framework.installer.InstallerEngine;
import de.janrufmonitor.framework.installer.rules.InstallerRuleEngine;
import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.util.uuid.UUID;

public class UpdateManager {
	
	private class URLRequester {
		private String url;
		
		private Properties p;
		private Logger m_logger;
				
		public URLRequester(String url) {
			this.url = url;
			this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		}
		
		public void go() {
			StringBuffer agent = new StringBuffer();
			agent.append("jAnrufmonitor Update Manager ");
			agent.append(IJAMConst.VERSION_DISPLAY);
			
			List monitors = getRuntime().getMonitorListener().getMonitors();
			if (monitors.size()>0) {
				agent.append(" (");
				IMonitor m  = null;
				for (int i=0,j=monitors.size();i<j;i++) {
					m = (IMonitor) monitors.get(i);
					agent.append(m.getID());
					if ((i+1)<j)
						agent.append(", ");	
				}
				agent.append(";");
				if (OSUtils.isWindows()) {
					agent.append("Windows");
				} else if (OSUtils.isLinux()) {
					agent.append("Linux");
				} else if (OSUtils.isMacOSX()) {
					agent.append("MacOSX");
				} else {
					agent.append("unknown OS");
				}
				agent.append(")");
			} else {
				Properties cfg = getRuntime().getConfigManagerFactory().getConfigManager().getProperties("monitor.MonitorListener");
				if (cfg!=null && cfg.size()>0) {
					Iterator i = cfg.keySet().iterator();
					String key = null;
					while (i.hasNext()) {
						key = (String) i.next();
						if (key.startsWith("monitor")) {
							agent.append(" (");
							String a = cfg.getProperty(key, "");
							if (a.indexOf(".")>0) {
								a = a.substring(a.lastIndexOf(".")+1);
							}
							agent.append(a);
							
							agent.append(";");
							if (OSUtils.isWindows()) {
								agent.append("Windows");
							} else if (OSUtils.isLinux()) {
								agent.append("Linux");
							} else if (OSUtils.isMacOSX()) {
								agent.append("MacOSX");
							} else {
								agent.append("unknown OS");
							}
							agent.append(")");
							break;
						}
					}
				}
			}
			
			
			
			if (m_logger.isLoggable(Level.INFO))
				this.m_logger.info("User-Agent: "+agent.toString());
			
			boolean retry = false;
			int retry_counter = 0;
			do {
				// added 2009/06/26: Registry call
				// added 2010/01/08: timer, only trigger registry call once a update request
				if (m_registry.length()>0 && m_registry.startsWith("http://") && (System.currentTimeMillis()-m_firstUpdateCallTimestamp)>10000) {
					String key = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(getNamespace(), "regkey");
					if (key==null || key.length()==0 || retry) {
						key = new UUID().toString();
						getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), "regkey", key);
						getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
					}
					try {
						URLConnection c = this.createRequestURL(agent, key); 
						c.connect();
						Object o = c.getContent();
						if (o instanceof InputStream) {
							BufferedInputStream bin = new BufferedInputStream((InputStream) o);						
							StringBuffer retvalue = new StringBuffer();
							BufferedReader br = new BufferedReader(new InputStreamReader(bin));
							while (br.ready()) {
								retvalue.append(br.readLine());
								retvalue.append(IJAMConst.CRLF);
							}
							if (m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Registry return: "+retvalue.toString());
							bin.close();
							
							Properties returnValues = new Properties();
							returnValues.load(new ByteArrayInputStream(retvalue.toString().getBytes()));
							if (returnValues.getProperty("status")!=null) {
								if (returnValues.getProperty("status").equalsIgnoreCase("duplicatedkey")) {
									retry = true;
								} else {
									retry = false;
								}
							}
						}			
						
						m_firstUpdateCallTimestamp = System.currentTimeMillis();
					} catch (MalformedURLException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);		
					}  
				} else {
					retry = false;
				}
				retry_counter++;
			} while(retry && retry_counter<6);
			
			try {
				URL url = new URL(this.url);
				URLConnection c = url.openConnection();
				c.setDoInput(true);
				c.setRequestProperty("User-Agent", agent.toString());
				c.connect();
				
				if (m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Querying URL "+this.url);
				
				Object o = c.getContent();
				if (o instanceof InputStream) {
					this.m_logger.info("Content successfully retrieved from "+this.url);
					BufferedInputStream bin = new BufferedInputStream((InputStream) o);
					this.p = new Properties();
					this.p.load(bin);
					bin.close();
				}				
			} catch (MalformedURLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				this.p = null;
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				this.p = null;
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "ioexception", e));				
			} 
		}
		
		public Properties getProperties() {
			return this.p;
		}
		
		private URLConnection createRequestURL(StringBuffer agent, String key) throws IOException {
			StringBuffer reg = new StringBuffer();
			reg.append(m_registry);
			reg.append("?k=");
			try {
				reg.append(URLEncoder.encode(key, "utf-8"));
			} catch (UnsupportedEncodingException ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
			URL url = new URL(reg.toString());
			if (m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Registry call: "+reg.toString());
			URLConnection c = url.openConnection();
			c.setDoInput(true);
			c.setRequestProperty("User-Agent", agent.toString());
			c.setRequestProperty("X-JAM-ModuleCount", Integer.toString(InstallerEngine.getInstance().getModuleList().size()));
			String dc = getDonationContact();
			if (dc!=null && dc.length()>0)
				c.setRequestProperty("X-JAM-DonationContact", dc);
			
			List modules = InstallerEngine.getInstance().getModuleList();
			if (modules.size()>0) {
				StringBuffer mods = new StringBuffer();
				String mod_name = null;
				String mod_version = null;
				for (int i=0; i<modules.size();i++) {
					mod_name = (String) modules.get(i);
					Properties ps = InstallerEngine.getInstance().getDescriptor(mod_name);
					if (ps!=null && ps.getProperty(InstallerConst.DESCRIPTOR_VERSION)!=null) {
						mod_version = ps.getProperty(InstallerConst.DESCRIPTOR_VERSION);
						mods.append(mod_name);
						mods.append(",");
						mods.append(mod_version);
						if (i<modules.size()-1) mods.append(";");
					}
				}
				c.setRequestProperty("X-JAM-Modules", mods.toString());
			
			}
			IMonitor m = null;
			if (getRuntime().getMonitorListener().isRunning()) {
				 m = getRuntime().getMonitorListener().getDefaultMonitor();
			}

			StringBuffer d = new StringBuffer();
			if (m!=null) {
				String[] desc = m.getDescription();						
				if (desc!=null && desc.length>0) {							
					d.append(desc[0]);
					for (int i=1;i<desc.length;i++) {
						if (desc[i]!=null && desc[i].length()>0) {
							desc[i] = StringUtils.replaceString(desc[i], ",", " ");
							d.append(", ");
							d.append(desc[i].trim());
						}
					}	
					int l = d.length();
					c.setRequestProperty("X-JAM-Monitor", d.toString().substring(0, (l>1023 ? 1023 : l)));
				} else {
					String signature = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, "monitorsignature");
					if (signature!=null && signature.length()>0) {
						d.append(signature);
						int l = d.length();
						c.setRequestProperty("X-JAM-Monitor", d.toString().substring(0, (l>1023 ? 1023 : l)));
					}
				}
			} else {
				String signature = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, "monitorsignature");
				if (signature!=null && signature.length()>0) {
					d.append(signature);
					int l = d.length();
					c.setRequestProperty("X-JAM-Monitor", d.toString().substring(0, (l>1023 ? 1023 : l)));
				}
			}
			
			if (m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Request properties: "+c.getRequestProperties().toString());
			
			return c;
		}

		private String getDonationContact() throws IOException {
			File donationfile = new File(PathResolver.getInstance(getRuntime()).getConfigDirectory(), "donation.key");
	    	if (donationfile.exists() && donationfile.length()>0) {
	    		Properties donation_key = new Properties();
	    		FileInputStream fin = new FileInputStream(donationfile);
	    		donation_key.load(fin);
	    		fin.close();
	    		
	    		if (donation_key.getProperty("email")!=null && donation_key.getProperty("email").trim().length()>0)  
	    			return donation_key.getProperty("email");
	    	}
	    	return null;
		}
		
	}
	
	private String NAMESPACE = "service.update.UpdateManager";
	
	private String BASEURL = null;
	protected Logger m_logger;
	private IRuntime m_runtime;
	private boolean isMoreUpdates;
	private String m_registry;
	static long m_firstUpdateCallTimestamp = 0;
	
	public UpdateManager() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.BASEURL = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), "url");
		this.m_registry = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), "registry");
	}
	
	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}	

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isMoreUpdates() {
		return this.isMoreUpdates;
	}
	
	private Properties getModule(String url) {
		if (url==null || url.trim().length()<6) return null;
		
		Properties p = null;
		
		URLRequester r = new URLRequester(url);
		r.go();
		p = r.getProperties();
		
		return p;
	}
	
	private Properties getOverview() {
		Properties p = null;
		
		URLRequester r = new URLRequester(BASEURL + "updates");
		r.go();
		p = r.getProperties();
		
		return p;
	}
	
	public List getUpdates() {
		List l = new ArrayList();
		
		Properties overview = this.getOverview();
		
		if (overview==null) {
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, this.getNamespace(), "nooverview", new Exception("Empty update page.")));
			return l;
		}
		if (overview.size()==0) {
			return l;
		} 

		this.m_logger.info("Available updates modules :"+overview);
		
		// get installed modules
		List installedModules = InstallerEngine.getInstance().getModuleList();
		installedModules.add("core");
		
		if (m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Installed modules :"+installedModules);

		List retrievingModules = new ArrayList();
		Iterator it = installedModules.iterator();		
		String key = null;
		while (it.hasNext()) {
			key = (String) it.next();
			if (overview.containsKey(key)) {
				retrievingModules.add(key);
			}
		}
		
		if (m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Retrieving update information for modules :"+retrievingModules);
		
		String name = null;
		Properties p = null;
		for (int i=0, n=retrievingModules.size();i<n;i++){
			name = (String) retrievingModules.get(i);
			// 2008/08/10: version specific core update capability
			if (name.equalsIgnoreCase("core")) {
				name += this.generatedCoreTag();
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Check updated for module "+name);
				
				p = this.getModule(overview.getProperty(name, "-"));
				if (p!=null) {
					if (InstallerRuleEngine.getInstance().isValidHidden(p)) {
						// check if version is equal
						Properties desc = InstallerEngine.getInstance().getDescriptor("core");
						if (desc!=null && p.getProperty(InstallerConst.DESCRIPTOR_VERSION, "").compareTo(
							desc.getProperty(InstallerConst.DESCRIPTOR_VERSION, "")) > 0) 
							l.add(p);
						continue;
					}
				}	
				name = "core";
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Re-Check updated for module "+name);
			}
			
			p = this.getModule(overview.getProperty(name, "-"));
			if (p!=null) {
				if (InstallerRuleEngine.getInstance().isValidHidden(p)) {
					// check if version is equal
					Properties desc = InstallerEngine.getInstance().getDescriptor(name);
//					if (desc!=null && p.getProperty(InstallerConst.DESCRIPTOR_VERSION, "").compareTo(
//						desc.getProperty(InstallerConst.DESCRIPTOR_VERSION, "")) > 0) 
//						l.add(p);
//					
					if (desc!=null && isUpdateVersion(desc.getProperty(InstallerConst.DESCRIPTOR_VERSION, ""), p.getProperty(InstallerConst.DESCRIPTOR_VERSION, ""))) 
							l.add(p);
				} else {
					if (m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Descriptor "+p+" not used in update list.");
					this.isMoreUpdates = true;
				}
			}	
		}

		return l;
	}
	
	private String generatedCoreTag() {
		StringBuffer s = new StringBuffer();
		s.append(".");
		s.append(IJAMConst.VERSION_DISPLAY);
		if (OSUtils.isConsole()) {
			s.append(".console.32");
		} else if (OSUtils.isWindows() && OSUtils.is32Bit()) {
			s.append(".win.32");
		} else if (OSUtils.isWindows() && OSUtils.is64Bit()) {
			s.append(".win.64");
		} else if (OSUtils.isWindows()) {
			s.append(".win");
		} else if (OSUtils.isLinux() && OSUtils.is32Bit()) {
			s.append(".linux.32");
		} else if (OSUtils.isLinux() && OSUtils.is64Bit()) {
			s.append(".linux.64");
		} else if (OSUtils.isLinux()) {
			s.append(".linux");
		} else if (OSUtils.isMacOSX() && OSUtils.is64Bit()) {
			s.append(".macosx.64");
		} else if (OSUtils.isMacOSX()) {
			s.append(".macosx");
		}
		return s.toString();
	}
	
	private boolean isUpdateVersion(String v1, String v2) {
		String[] v1a = v1.split("\\.");
		String[] v2a = v2.split("\\.");
		if (v1a.length!=3) {
			String[] v1temp = new String[3];
			v1temp[0] = (v1a.length>0 ? v1a[0] : "0");
			v1temp[1] = (v1a.length>1 ? v1a[1] : "0");
			v1temp[2] = (v1a.length>2 ? v1a[2] : "0");			
			v1a = v1temp;
		}
		if (v2a.length!=3) {
			String[] v2temp = new String[3];
			v2temp[0] = (v2a.length>0 ? v2a[0] : "0");
			v2temp[1] = (v2a.length>1 ? v2a[1] : "0");
			v2temp[2] = (v2a.length>2 ? v2a[2] : "0");
			v2a = v2temp;
		}
		if (v2a[2].equalsIgnoreCase("a"))
			v2a[2] = "10";
		if (v1a[2].equalsIgnoreCase("a"))
			v1a[2] = "10";
		if (v1a[0].compareTo(v2a[0])==0) {
			if (v1a[1].compareTo(v2a[1])==0) {
				return Integer.parseInt(v1a[2])<Integer.parseInt(v2a[2]);
			} 
			return v1a[1].compareTo(v2a[1])<0;
		}		
		return v1a[0].compareTo(v2a[0])<0;
	}
}
