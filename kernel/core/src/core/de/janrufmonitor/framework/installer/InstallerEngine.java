package de.janrufmonitor.framework.installer;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.classloader.JamCacheMasterClassLoader;
import de.janrufmonitor.exception.IPropagator;
import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.framework.installer.rules.InstallerRuleEngine;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class InstallerEngine {
	
	private static String NAMESPACE = "installer.InstallerEngine";
	private static String MODULE_LIST = "modules.lst";
	
	private class InstallerComparator implements Comparator  {
		public int compare(Object obj1, Object obj2) {
			if (obj1 instanceof IInstaller && obj2 instanceof IInstaller) {
				if (((IInstaller)obj1).getPriority()<((IInstaller)obj2).getPriority()) return -1;
				if (((IInstaller)obj1).getPriority()>((IInstaller)obj2).getPriority()) return 1;
			}
			return 0;
		}
	}
	
	private class GenericFileFilter implements java.io.FileFilter {

	    String[] extensions;
	    boolean includeDirectory;

	    
		/**
		 * Constructor for certain extensions.
		 * 
		 * @param ext file extension.
		 * @param includeDirectory directory inclusion flag
		 */
		public GenericFileFilter(String ext, boolean includeDirectory) {
			this(new String[]{ext}, includeDirectory);
		}

		/**
		 * Constructor for multiple extensions and descriptions of directories
		 * 
		 * @param exts extensions
		 * @param desc descriptions
		 * @param includeDirectory include directories for filtering
		 */
	    public GenericFileFilter(String[] exts, boolean includeDirectory) {
	        this.extensions = new String[exts.length];
	        for (int i = exts.length - 1; i >= 0; i--) {
	            this.extensions[i] = exts[i].toLowerCase();
	        }
	        this.includeDirectory = includeDirectory;
	    }

		/**
		 * Checks if the file is accepted by the filter or not.
		 */
	    public boolean accept(File file) {
	        if (file.isDirectory()) {
	            return this.includeDirectory;
	        }

	        String name = file.getName().toLowerCase();
	        for (int i = this.extensions.length - 1; i >= 0; i--) {
	            if (name.endsWith(this.extensions[i])) {
	                return true;
	            }
	        }
	        return false;
	    }

	}
	
	private Logger m_logger;
	private List m_installers;
	private IRuntime m_runtime;
//	private File m_installDir;
//	private File m_userInstallDir;
	private Properties m_installedModules;
		
	private static InstallerEngine m_instance = null;

	private InstallerEngine() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_logger.info("Starting InstallerEngine...");
		this.m_installers = new ArrayList();
		this.m_installedModules = new Properties();
		this.registerStandardInstallers();
	}

    public static synchronized InstallerEngine getInstance() {
        if (InstallerEngine.m_instance == null) {
        	InstallerEngine.m_instance = new InstallerEngine();
        }
        return InstallerEngine.m_instance;
    }
    
    public List getModuleList() {
    	List l = new ArrayList();
    	Enumeration e = this.m_installedModules.propertyNames();
    	String key = null;
    	while(e.hasMoreElements()) {
    		key = (String) e.nextElement();
    		if (!key.equalsIgnoreCase("core") && !key.equalsIgnoreCase("areacode"))
    			l.add(key);
    	}
    	return l;
    }
    
    public Properties getDescriptor(String key, boolean propagate) {
    	int i=0;
    	
    	Properties descriptor = null;
    	IInstaller inst = null;
    	while(descriptor==null && i<this.m_installers.size()) {
    		inst = (IInstaller) this.m_installers.get(i);
    		descriptor = inst.getDescriptor(key);
    		i++;
    	}
    	
    	if (descriptor==null && propagate) {
    		PropagationFactory.getInstance().fire(
	    		new Message(Message.ERROR, NAMESPACE, "invaliddescriptor", new Exception("No descriptor found for namespace: "+key))
			);	
    	}
    	
    	return descriptor;
    }
    
    public Properties getDescriptor(String key) {
    	return this.getDescriptor(key, true);
    }
    
    public boolean install(String name, InputStream in, boolean restart) throws InstallerException {
    	File tmpFile = new File(PathResolver.getInstance().getTempDirectory()+name);
    	try {
			FileOutputStream fos = new FileOutputStream(tmpFile);
			Stream.copy(new BufferedInputStream(in), fos, true);
			System.setProperty("jam.propagate", "true");
			return this.internal_install(tmpFile, restart, false, false);
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		} 
    }
    
    public boolean install(File f, boolean keepFiles) {
    	return this.install(f, true, keepFiles);
    }
    
    public boolean install(File f, boolean isRestart, boolean keepFiles) {
    	boolean restart = false;
    	try {
    		System.setProperty("jam.propagate", "true");
    		restart = this.internal_install(f, isRestart, keepFiles, false);
		} catch (InstallerException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		} finally {
  	    	// set system property for restart flag of installer
    		System.setProperty("jam.installer.restart", Boolean.toString(restart));

			m_logger.info("Flag jam.installer.restart set to "+System.getProperty("jam.installer.restart"));
			
	    	this.popupShutdown(restart);
		}
        return true;
    }
 
    public boolean install(File f) {
    	return this.install(f, false);
    }
    
    public boolean uninstall(String key) {
     	IInstaller installer = null;
     	boolean result = false;
    	for (int i=0,j=this.m_installers.size();i<j;i++) {
    		installer = (IInstaller) this.m_installers.get(i);
    		try {
				result |= installer.uninstall(key);
				this.m_installedModules.remove(key);
				this.saveModuleList();
			} catch (InstallerException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
    	}
    	return result;
    }
    
    public void install() {
    	if (this.check()) {
    		// set system property for check of installer
    		System.setProperty(IJAMConst.SYSTEM_INSTALLER_RUN, "true");
    		
            File directory = new File(PathResolver.getInstance(this.getRuntime()).getInstallDirectory() + "install" + File.separator);
            if (!directory.exists()) directory.mkdirs();
            if (directory.isDirectory()) {
            	IPropagator pg = this.addPropagator();
            	
            	boolean restart = false;
            	// initializing is mandatory before installing other modules
        		File[] files = directory.listFiles(new GenericFileFilter(".init.jam.zip", false));
    			if (files.length>0) {
    				for (int k = files.length - 1; k >= 0; k--) {
    					try {
							restart |= this.internal_install(files[k], true, false, false);
						} catch (InstallerException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}
    				}
    			}
    			
    			files = directory.listFiles(new GenericFileFilter("", false));
    			if (files.length>0) {
    				for (int k = files.length - 1; k >= 0; k--) {
    					try {
							restart |= this.internal_install(files[k], true, false, OSUtils.isMultiuserEnabled());
						} catch (InstallerException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}
    				}
    			}
    			
//    			if (OSUtils.isMultiuserEnabled()) {
//    				directory = this.getUserInstallationDirectory();
//    				if (directory.isDirectory()) {
//    					files = directory.listFiles(new GenericFileFilter("", false));
//    	    			if (files.length>0) {
//    	    				for (int k = files.length - 1; k >= 0; k--) {
//    	    					try {
//    								restart |= this.internal_install(files[k], true, false, true);
//    							} catch (InstallerException e) {
//    								this.m_logger.log(Level.SEVERE, e.getMessage(), e);
//    							}
//    	    				}
//    	    			}
//    				}
//    			}
    			
    	    	if (pg!=null)
    	    		PropagationFactory.getInstance().remove(pg);
    	    	
    	    	// set system property for restart flag of installer
        		System.setProperty(IJAMConst.SYSTEM_INSTALLER_RESTART, Boolean.toString(restart));
        		m_logger.info("Flag jam.installer.restart set to "+System.getProperty(IJAMConst.SYSTEM_INSTALLER_RESTART));
        		this.popupShutdown(restart);
            }
    	} else {
    		System.setProperty(IJAMConst.SYSTEM_INSTALLER_RESTART, Boolean.toString(false));		
    	}    
    }
    
    private boolean internal_install(File f, boolean acceptRestart, boolean keepFiles, boolean isMultiuser) throws InstallerException {
    	if (f==null) {
    		this.m_logger.severe("File to install is null.");
    		return false;
    	}
    	
    	boolean restart = false;
    	
    	IInstaller installer = this.getInstaller(f);
    	if (installer!=null) {
    		Properties descriptor = null;
    		installer.setFile(f);       
    		descriptor = installer.getDescriptor();
    		if (descriptor!=null) {
    			boolean result = this.installWithDescriptor(installer, f, descriptor);
    			if (acceptRestart) restart = result;
    		} else {
    			this.installWithoutDescriptor(installer, f, isMultiuser);
    		}
    		if(!keepFiles) {
    			this.m_logger.info("Deleting installed module file "+f.getName()+", if not moved.");
    	   		if (!f.delete())
        			f.deleteOnExit();
    		}
    		//removed: 2006/12/14: if (acceptRestart && restart)
    		//this.deleteProcessIDs();
    		
			
			// make MacOS X specific test
//			if (OSUtils.isMacOSX()) {
//				File infoPlist = new File (PathResolver.getInstance(getRuntime()).getInstallDirectory(), "../../Info.plist");
//				if (infoPlist.exists()) {
//					// read Info.plist
//					try {
//						FileInputStream in = new FileInputStream(infoPlist);
//						ByteArrayOutputStream out = new ByteArrayOutputStream();
//						Stream.copy(in, out, true);
//
//						String content = new String(out.toByteArray());
//						int start = content.indexOf("<key>ClassPath</key>");
//						int end = content.indexOf("</array>", (start>-1 ? start : 0))+8;
//						if (start>-1) {
//							StringBuffer sb = new StringBuffer();
//							sb.append(content.substring(0, start));
//							sb.append("<key>ClassPath</key>");
//							sb.append(IJAMConst.CRLF);
//							sb.append("<array>");
//							sb.append(IJAMConst.CRLF);
//							// add jar entries
//							File workingDir = new File(PathResolver.getInstance(getRuntime()).getInstallDirectory());
//							File[] jars = workingDir.listFiles(new GenericFileFilter(".jar", false));
//							for (int i=0;i<jars.length;i++) {
//								sb.append("<string>$JAVAROOT/");
//								sb.append(jars[i].getName());
//								sb.append("</string>");
//								sb.append(IJAMConst.CRLF);
//							}
//							File libDir = new File(PathResolver.getInstance(getRuntime()).getLibDirectory());
//							jars = libDir.listFiles(new GenericFileFilter(".jar", false));
//							for (int i=0;i<jars.length;i++) {
//								sb.append("<string>$JAVAROOT/lib/");
//								sb.append(jars[i].getName());
//								sb.append("</string>");
//								sb.append(IJAMConst.CRLF);
//							}
//							sb.append("</array>");
//							sb.append(IJAMConst.CRLF);
//							sb.append(content.substring(end));
//							
//							FileOutputStream fos = new FileOutputStream(infoPlist);
//							ByteArrayInputStream ins = new ByteArrayInputStream(sb.toString().getBytes());
//							Stream.copy(ins, fos, true);
//						}
//					} catch (FileNotFoundException e) {
//						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					} catch (IOException e) {
//						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					}
//				}
//			}
    		
    		this.m_logger.info("Installed new file " + f.getName()+" with extension installer "+installer.getExtension());
    	} else {
    		this.m_logger.warning("No installer for file "+f.getName()+" found.");
    		if(!keepFiles) {
    			if (!f.delete()) f.deleteOnExit();
    		}
    	}
    	if (!restart) {
    		JamCacheMasterClassLoader.renewInstance();
    	}
    	
    	return restart;
    }

    private boolean installWithDescriptor(IInstaller installer, File f, Properties descriptor) throws InstallerException {
		boolean restart = false;
		if (this.isRemoveFlag(descriptor)) {
			// remove module by flag trigger
			this.m_logger.info("Detected remove flag in descriptor.");
			String namespace = this.getNamespace(descriptor);
			return this.uninstall(namespace); // no restart == false
		}
		
    	if (InstallerRuleEngine.getInstance().isValid(descriptor)) {
			// install module
			try {
				boolean overwriteconfig = Boolean.valueOf(descriptor.getProperty(InstallerConst.DESCRIPTOR_CONFIG_OVERWRITE, "true")).booleanValue();
				
				String namespace = installer.install(overwriteconfig);
				if (namespace!=null) {
					this.m_installedModules.setProperty(namespace, namespace);
					this.saveModuleList();
				}
				restart = Boolean.valueOf(descriptor.getProperty(InstallerConst.DESCRIPTOR_RESTART, "false")).booleanValue();
			} catch (InstallerException e) {
				this.m_logger.severe("Installation of file "+f.getName()+" failed.");
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
    	    	PropagationFactory.getInstance().fire(
    	    		new Message(Message.ERROR, NAMESPACE, "unknown", e)
    			);	    					
			}
		} else {
			f.renameTo(new File(f.getPath() + ".err"));
			throw new InstallerException("Installation for descriptor of file "+f.getName()+ " was blocked by a rule.");
		}
    	return restart;
    }
    
    private void installWithoutDescriptor(IInstaller installer, File f, boolean suppressPopup) throws InstallerException {
    	if (!suppressPopup) {
	    	this.m_logger.warning("No module descriptor available. This module is not intended to be installed in this version: "+f.getName());
	    	PropagationFactory.getInstance().fire(
	    		new Message(Message.INFO, NAMESPACE, "null", new Exception("Component with no module descriptor installed."))
			);
    	}
		try {
			installer.install(true);
		} catch (InstallerException e) {
			this.m_logger.severe("Installation of file "+f.getName()+" failed.");
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
	    	PropagationFactory.getInstance().fire(
	    		new Message(Message.ERROR, NAMESPACE, "unknown", e)
			);					
		}
    }
    
    private IInstaller getInstaller(File f) {
    	if (f==null) return null;
     	IInstaller installer = null;
    	for (int i=0,j=this.m_installers.size();i<j;i++) {
    		installer = (IInstaller) this.m_installers.get(i);
    		if (f.getName().toLowerCase().endsWith(installer.getExtension())) {
    			return installer;
    		}
    	}
    	return null;
    }
    
    private void popupShutdown(boolean restart) {
    	 if (restart && !this.isHeadless()) {
			try {
				Class c = Thread.currentThread().getContextClassLoader().loadClass("de.janrufmonitor.ui.swt.controls.ConfigShutdown");
				
				if (c!=null) {
					ICommand shutdown = (ICommand)c.newInstance();
					if (shutdown!=null) {
						try {
							shutdown.execute();
						} catch (Exception ex) {
							this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
						}
					}
				}
			} catch (ClassNotFoundException e) {
				this.m_logger.warning(e.toString()+" : "+e.getMessage());
			} catch (InstantiationException e) {
				this.m_logger.severe(e.toString()+" : "+e.getMessage());
			} catch (IllegalAccessException e) {
				this.m_logger.severe(e.toString()+" : "+e.getMessage());
			}
			this.m_logger.info("Shutdown token found in install directory. Shutting down jAnrufmonitor. Please restart manually.");
        }
    }
    
    private IPropagator addPropagator() {
    	IPropagator pg = null;
    	if (!this.isHeadless()) {
			try {
				Class c =
					Thread.currentThread().getContextClassLoader().loadClass("de.janrufmonitor.ui.jface.dialogs.InstallPropagator");

				if (c!=null) {
					pg = (IPropagator)c.newInstance();
					if (pg!=null) {
						try {
							PropagationFactory.getInstance().remove(pg);
							PropagationFactory.getInstance().add(pg);
						} catch (Exception ex) {
							this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
						}
					}
				}
			} catch (ClassNotFoundException e) {
				this.m_logger.warning(e.toString()+" : "+e.getMessage());
			} catch (InstantiationException e) {
				this.m_logger.severe(e.toString()+" : "+e.getMessage());
			} catch (IllegalAccessException e) {
				this.m_logger.severe(e.toString()+" : "+e.getMessage());
			}
    	}
    	return pg;
    }
       
    private boolean isRemoveFlag(Properties descriptor) {
    	if (descriptor==null || descriptor.size()==0) return false;
    	
    	String rm = descriptor.getProperty(InstallerConst.DESCRIPTOR_REMOVE);
    	
    	return (rm!=null && rm.equalsIgnoreCase("true"));
    }
    
    private String getNamespace(Properties descriptor) {
    	if (descriptor==null || descriptor.size()==0) return null;

    	return descriptor.getProperty(InstallerConst.DESCRIPTOR_NAMESPACE);
    }    
    
    private void saveModuleList() {
		try {
			FileOutputStream fout = new FileOutputStream(
				PathResolver.getInstance(this.getRuntime()).getConfigDirectory() + MODULE_LIST
			);
			this.m_installedModules.store(fout, "");
			fout.flush();
			fout.close();
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    private void registerStandardInstallers() {
    	// get all installed module keys
    	File f = new File(PathResolver.getInstance(this.getRuntime()).getConfigDirectory() + MODULE_LIST);
		try {
			
			FileInputStream fin = new FileInputStream(
				f
			);
			this.m_installedModules.load(fin);
			fin.close();
		} catch (FileNotFoundException e) {
			this.m_logger.info(e.getMessage());
			f.getParentFile().mkdirs();
			try {
				f.createNewFile();
			} catch (IOException e1) {
				this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			}
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}

    	// add .jam.zip installer
    	this.m_installers.add(new JamArchiveInstaller());
    	
    	// add .init.inf installer
    	this.m_installers.add(new InitInfInstaller());
    	
    	// add .inf installer
    	this.m_installers.add(new InfInstaller());
    	
    	// add .i18n installer
    	this.m_installers.add(new I18nInstaller());

    	// add .lib.deploy installer
    	this.m_installers.add(new LibInstaller());
    	
    	// add .delete installer
    	this.m_installers.add(new DeleteFileInstaller());

    	// add default installer
    	
    	Collections.sort(this.m_installers, new InstallerComparator());
    }
	
	private boolean check() {
		File directory = new File(PathResolver.getInstance(this.getRuntime()).getInstallDirectory() + "install" + File.separator);
		if (!directory.exists()) directory.mkdirs();
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files.length>0) {
				this.m_logger.info("New modules for installation found.");
				return true;
            }
//            if (OSUtils.isMultiuserEnabled()) {
//	            directory = this.getUserInstallationDirectory();
//	            if (directory.exists() && directory.isDirectory()) {
//	                files = directory.listFiles();
//	                if (files.length>0) {
//	    				this.m_logger.info("New modules for installation found in user install directory.");
//	    				return true;
//	                }
//	            }
//            }
        } else {
            this.m_logger.warning(directory.getAbsolutePath()+" directory for new modules not found in install path. Please check the configuration file.");
            return false;
        }
        this.m_logger.info("No modules for installation found.");
        return false;
	}
	
//	private File getDefaultInstallationDirectory() {
//		if (this.m_installDir==null) {
//			this.m_installDir = new File(PathResolver.getInstance(this.getRuntime()).getInstallDirectory() + "install" + File.separator);
//			if (!this.m_installDir.exists()) {
//				this.m_logger.warning("Installation directory "+this.m_installDir.getAbsolutePath()+" does not exist. Please create directory structure manually.");
//				this.m_installDir.mkdirs();
//			}
//		}
//		return this.m_installDir;
//	}
//	
//	private File getUserInstallationDirectory() {
//		if (this.m_userInstallDir==null) {
//			this.m_userInstallDir = new File(PathResolver.getInstance(this.getRuntime()).getInstallDirectory() + "users" + File.separator + OSUtils.getLoggedInUser()+File.separator+"install");
//			if (!this.m_userInstallDir.exists()) {
//				this.m_logger.warning("User installation directory "+this.m_userInstallDir.getAbsolutePath()+" does not exist. Please create directory structure manually.");
//				this.m_userInstallDir.mkdirs();
//			}
//		}
//		return this.m_userInstallDir;
//	}
	
    private IRuntime getRuntime() {
    	if (this.m_runtime==null){
    		this.m_runtime = PIMRuntime.getInstance();
    	}
    	return this.m_runtime;
    }
    
    private boolean isHeadless() {
    	String hl = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty("ui.UIFactory", "headless");
    	return (hl.equalsIgnoreCase("true"));
    }
}
