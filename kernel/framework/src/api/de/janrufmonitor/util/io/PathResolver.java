package de.janrufmonitor.util.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.string.StringUtils;

/**
 *  This class provides resolving of directory variables like %userhome%, %imagepath%, %installpath%
 * 
 *@author     Thilo Brandt
 *@created    2004/09/04
 */
public class PathResolver {

	private static final String JAR_FILE_PREFIX = "jar:file:";
	private static final String FILE_PREFIX = "file:";

	private static PathResolver m_instance = null;
	private static Logger m_logger;
	private IRuntime m_runtime;
	
	private File appRoot;
	private File installPath;
	private File libPath;
	private File imagePath;
	private File dataPath;
	private File userDataPath;
	private File configPath;
	private File logPath;
	private File userhomePath;
	private File tempPath;
	private File photoPath;

	private PathResolver(IRuntime r) {
		this.m_runtime = r;
	}
	
	/**
	 * Gets an instance of the PathResolver depending on the runtime.
	 * 
	 * @param runtime the current runtime.
	 * @return a valid PathResolver object
	 */
	public static PathResolver getInstance(IRuntime runtime) {
		if (m_instance==null || m_instance.m_runtime==null) {
			m_instance = new PathResolver(runtime);
			m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		}
		return m_instance;
	}
	
	/**
	 * Gets an instance of the PathResolver without runtime dependency.
	 * 
	 * @param runtime the current runtime.
	 * @return a valid PathResolver object
	 */
	public static PathResolver getInstance() {
		if (m_instance==null) {
			m_instance = new PathResolver(null);
		}
		return m_instance;
	}
	
	/**
	 * Initializes the path variables. 
	 */
	public void initialize(){
		this.appRoot = null;
		this.imagePath = null;
		this.dataPath = null;
		this.configPath = null;
		this.logPath = null;
		this.userhomePath = null;
		this.tempPath = null;
		this.photoPath = null;
	}
	
	/**
	 * Resolves the path to a valid String object.
	 */
	public String resolve(String path) {
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_INSTALLPATH, this.getInstallDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_USERHOME, this.getUserhomeDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_IMAGEPATH, this.getImageDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_TEMP, this.getTempDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_DATAPATH, this.getDataDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_USERDATAPATH, this.getUserDataDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_CONFIGPATH, this.getConfigDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_USERCONFIGPATH, this.getConfigDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_LOGPATH, this.getLogDirectory());
		path = StringUtils.replaceString(path, IJAMConst.PATHKEY_PHOTOPATH, this.getPhotoDirectory());
		return path;
	}
	
	/**
	 * Encodes the path with the variables if possible 
	 * 
	 * @param path path to encode
	 * @return encoded path
	 */
	public String encode(String path) {
		path = StringUtils.replaceString(path, this.getPhotoDirectory(), IJAMConst.PATHKEY_PHOTOPATH);
		path = StringUtils.replaceString(path, this.getUserDataDirectory(), IJAMConst.PATHKEY_USERDATAPATH);
		path = StringUtils.replaceString(path, this.getUserhomeDirectory(), IJAMConst.PATHKEY_USERHOME);
		path = StringUtils.replaceString(path, this.getImageDirectory(), IJAMConst.PATHKEY_IMAGEPATH);
		path = StringUtils.replaceString(path, this.getTempDirectory(), IJAMConst.PATHKEY_TEMP);
		path = StringUtils.replaceString(path, this.getDataDirectory(), IJAMConst.PATHKEY_DATAPATH);
		path = StringUtils.replaceString(path, this.getConfigDirectory(), IJAMConst.PATHKEY_CONFIGPATH);
		path = StringUtils.replaceString(path, this.getConfigDirectory(), IJAMConst.PATHKEY_USERCONFIGPATH);
		path = StringUtils.replaceString(path, this.getLogDirectory(), IJAMConst.PATHKEY_LOGPATH);		
		path = StringUtils.replaceString(path, this.getInstallDirectory(), IJAMConst.PATHKEY_INSTALLPATH);
		return path;
	}
//
//	private String getBaseDefaultUserDirectory() {
//		if (!OSUtils.isMultiuserEnabled()) return null;
//		if (this.installPath==null) {
//			if (this.m_runtime!=null)
//				this.installPath = new File(this.getWorkingDirectory(this.m_runtime.getClass()));
//			else 
//				this.installPath = new File(this.getWorkingDirectory(PathResolver.class));
//		}
//		if (OSUtils.isMacOSX()) return this.installPath.getAbsolutePath() + File.separator;
//		return new File(this.installPath, "users"+File.separator+"default").toString() + File.separator;
//	}
	
	public String getAppRoot() {
		if (this.appRoot==null){
			if (this.m_runtime!=null)
				this.appRoot = new File(this.getWorkingDirectory(this.m_runtime.getClass()));
			else 
				this.appRoot = new File(this.getWorkingDirectory(PathResolver.class));
		}
		return this.appRoot.getAbsolutePath() + File.separator;
	}

	private String getBaseCurrentUserDirectory() {
		if (!OSUtils.isMultiuserEnabled()) return null;
		if (OSUtils.isMacOSX()) return new File(this.getUserhomeDirectory(), "Library"+File.separator+"Application Support"+File.separator+"jAnrufmonitor").getAbsolutePath() + File.separator;
		
		File usersRootPath = null;
		
		File pathFile = new File(this.getAppRoot(), ".paths");
		if (pathFile.exists() && pathFile.isFile()) {
			try {
				FileInputStream in = new FileInputStream(pathFile);
				Properties paths = new Properties();
				paths.load(in);
				in.close();
				
				if (paths.containsKey(IJAMConst.PATHKEY_USERSROOTPATH)) {
					usersRootPath = new File(paths.getProperty(IJAMConst.PATHKEY_USERSROOTPATH, Long.toString(System.currentTimeMillis())));
					if (!usersRootPath.exists()) usersRootPath = null;
				}
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		} 
		boolean useAppData = Boolean.parseBoolean(System.getProperty(IJAMConst.SYSTEM_MULTI_USER_USEAPPDATA, "false"));
		if (usersRootPath==null && useAppData && OSUtils.isWindows()) {
			String appData = System.getenv("APPDATA");
			if (appData!=null && appData.trim().length()>0) {
				File appDataFile = new File(appData);
				if (appDataFile.exists()) {
					usersRootPath = new File(appDataFile, "jAnrufmonitor"+ File.separator+"users"+File.separator);
				}
			}
		}
		if (usersRootPath!=null) {
			return new File(usersRootPath, OSUtils.getLoggedInUser()).toString() + File.separator;
		}
		return new File(this.getAppRoot(), "users"+File.separator+OSUtils.getLoggedInUser()).toString() + File.separator;
	}
	
	/**
	 * Gets the user specific install directory of the application.
	 * 
	 * @return install directory
	 */
	public String getInstallDirectory() {
		if (this.installPath==null) {
			if (this.m_runtime!=null)
				this.installPath = new File(this.getWorkingDirectory(this.m_runtime.getClass()));
			else 
				this.installPath = new File(this.getWorkingDirectory(PathResolver.class));
			
			if (OSUtils.isMultiuserEnabled()) {
				File loggedin_user_dir = new File(this.getBaseCurrentUserDirectory());
				if (!loggedin_user_dir.exists()) {
					loggedin_user_dir.mkdirs();
					System.setProperty("jam.propagate", "false");
				
					try {
						this.copy(this.installPath, loggedin_user_dir, new FileFilter() {
							public boolean accept(File f) {
								if (f.getName().startsWith("users")) return false;
								if (f.getName().startsWith(".paths")) return false;
								if (f.getName().endsWith(".exe")) return false;
								if (f.getName().endsWith(".bat")) return false;
								if (f.getName().endsWith(".sh")) return false;
								if (f.getName().endsWith(".ini")) return false;
								if (f.getName().endsWith(".dll")) return false;
								if (f.getName().endsWith(".jnilib")) return false;
								if (f.getName().endsWith(".jar") && !f.getParentFile().getName().startsWith("lib")) return false;
								return true;
							}});
						
					} catch (FileNotFoundException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
				this.installPath = loggedin_user_dir;
			}
		}
		return this.installPath.getAbsolutePath() + File.separator;
	}
	
	/**
	 * Gets the lib directory of the application.
	 * 
	 * @return lib directory
	 */
	public String getLibDirectory() {
		if (this.libPath==null) {
			this.libPath = new File(this.getInstallDirectory(), "lib");
		}
		return this.libPath.getAbsolutePath() + File.separator;
	}
	
	/**
	 * Gets the image directory of the application.
	 *
	 * @return image directory
	 */
	public String getImageDirectory() {
		if (this.imagePath==null) {
			File pathFile = new File(getInstallDirectory(), ".paths");
			if (pathFile.exists() && pathFile.isFile()) {
				try {
					FileInputStream in = new FileInputStream(pathFile);
					Properties paths = new Properties();
					paths.load(in);
					in.close();
					
					if (paths.containsKey(IJAMConst.PATHKEY_IMAGEPATH)) {
						this.imagePath = new File(paths.getProperty(IJAMConst.PATHKEY_IMAGEPATH, Long.toString(System.currentTimeMillis())));
						if (!this.imagePath.exists()) this.imagePath = null;
					}
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				
			} 
			if (this.imagePath==null){
				this.imagePath = new File(this.getInstallDirectory(), "images");
				Properties paths = new Properties();
				if (pathFile.exists() && pathFile.isFile()) {
					try {
						FileInputStream in = new FileInputStream(pathFile);
					
						paths.load(in);
						in.close();

					} catch (FileNotFoundException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				} 

				paths.put(IJAMConst.PATHKEY_IMAGEPATH, this.imagePath.getAbsolutePath());
				try {
					FileOutputStream out = new FileOutputStream(pathFile);
					paths.store(out, "");
					out.close();
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}			
			
		}
		return this.imagePath.getAbsolutePath() + File.separator;
	}
	
	/**
	 * Gets the userhome directory.
	 * @return userhome directory
	 */
	public String getUserhomeDirectory() {
		if (this.userhomePath==null) {
			String path = System.getProperty("user.home");
			if (path==null || path.length()==0)
				path = this.getInstallDirectory();
			
			this.userhomePath = new File(path);
		}
		return this.userhomePath.getAbsolutePath() + File.separator;
	}
	
	/**
	 * Gets the user data directory of the application.
	 *
	 * @return user data directory
	 */
	public String getUserDataDirectory() {	
		if (this.userDataPath==null) {
			File pathFile = new File(getInstallDirectory(), ".paths");
			if (pathFile.exists() && pathFile.isFile()) {
				try {
					FileInputStream in = new FileInputStream(pathFile);
					Properties paths = new Properties();
					paths.load(in);
					in.close();
					
					if (paths.containsKey(IJAMConst.PATHKEY_DATAPATH)) {
						this.userDataPath = new File(paths.getProperty(IJAMConst.PATHKEY_USERDATAPATH, Long.toString(System.currentTimeMillis())));
						if (!this.userDataPath.exists()) this.userDataPath = null;
					}
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				
			} 
			if (this.userDataPath==null){
				this.userDataPath = new File(this.getUserhomeDirectory(), "Documents"+File.separator+"jAnrufmonitor");
				Properties paths = new Properties();
				if (pathFile.exists() && pathFile.isFile()) {
					try {
						FileInputStream in = new FileInputStream(pathFile);
					
						paths.load(in);
						in.close();
	
					} catch (FileNotFoundException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				} 
	
				paths.put(IJAMConst.PATHKEY_USERDATAPATH, this.userDataPath.getAbsolutePath());
				try {
					FileOutputStream out = new FileOutputStream(pathFile);
					paths.store(out, "");
					out.close();
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return this.userDataPath.getAbsolutePath() + File.separator;
	}

	/**
	 * Gets the temporary directory.
	 * @return temporary directory
	 */
	public String getTempDirectory() {
		if (this.tempPath==null) {
			String path = System.getProperty("java.io.tmpdir");
			if (path==null || path.length()==0)
				path = this.getInstallDirectory() + File.separator + "tmp";
			
			this.tempPath = new File(path);
		}
		return this.tempPath.getAbsolutePath() + File.separator;
	}
	
	/**
	 * Gets the data directory of the application.
	 *
	 * @return data directory
	 */
	public String getDataDirectory() {	
		if (this.dataPath==null) {
			File pathFile = new File(getInstallDirectory(), ".paths");
			if (pathFile.exists() && pathFile.isFile()) {
				try {
					FileInputStream in = new FileInputStream(pathFile);
					Properties paths = new Properties();
					paths.load(in);
					in.close();
					
					if (paths.containsKey(IJAMConst.PATHKEY_DATAPATH)) {
						this.dataPath = new File(paths.getProperty(IJAMConst.PATHKEY_DATAPATH, Long.toString(System.currentTimeMillis())));
						if (!this.dataPath.exists()) this.dataPath = null;
					}
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				
			} 
			if (this.dataPath==null){
				this.dataPath = new File(this.getInstallDirectory(), "data");
				
				Properties paths = new Properties();
				if (pathFile.exists() && pathFile.isFile()) {
					try {
						FileInputStream in = new FileInputStream(pathFile);
					
						paths.load(in);
						in.close();

					} catch (FileNotFoundException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				} 

				paths.put(IJAMConst.PATHKEY_DATAPATH, this.dataPath.getAbsolutePath());
				try {
					FileOutputStream out = new FileOutputStream(pathFile);
					paths.store(out, "");
					out.close();
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return this.dataPath.getAbsolutePath() + File.separator;
	}
	
	

//	/**
//	 * Gets the user config directory of the application.
//	 *
//	 * @return user config directory
//	 */
//	public String getUserConfigDirectory() {
//		if (this.userConfigPath==null) {
//			this.userConfigPath = new File(this.getConfigDirectory());
//			
//			if (OSUtils.isMultiuserEnabled()) {
//				// check for default user
//				File default_user_dir = new File(this.getInstallDirectory(), "users"+File.separator+"default"+File.separator+"config");
//				if (!default_user_dir.exists()) {
//					default_user_dir.mkdirs();
//					try {
//						this.copy(this.userConfigPath, default_user_dir, new FileFilter() {
//							public boolean accept(File f) {
//								if (f.getName().startsWith("janrufmonitor")) return true;
//								if (f.getName().startsWith("i18n.")) return true;
//								return false;
//							}});
//					} catch (FileNotFoundException e) {
//						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					} catch (IOException e) {
//						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					}
//				}
//				File loggedin_user_dir = new File(this.getInstallDirectory(), "users"+File.separator+OSUtils.getLoggedInUser()+File.separator+"config");
//				if (!loggedin_user_dir.exists()) {
//					loggedin_user_dir.mkdirs();
//					try {
//						this.copy(default_user_dir, loggedin_user_dir, new FileFilter() {
//							public boolean accept(File f) {
//								if (f.getName().startsWith("janrufmonitor")) return true;
//								if (f.getName().startsWith("i18n.")) return true;
//								return false;
//							}});
//					} catch (FileNotFoundException e) {
//						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					} catch (IOException e) {
//						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					}
//				}
//				this.userConfigPath = loggedin_user_dir;
//			}
//			if (OSUtils.isMacOSX()) {
//				// check for default user
//				File mac_user_conf_dir = new File(this.getUserhomeDirectory(), "Library"+File.separator+"Application Support"+File.separator+"jAnrufmonitor"+File.separator+"config");
//				if (!mac_user_conf_dir.exists()) {
//					mac_user_conf_dir.mkdirs();
//					try {
//						this.copy(this.userConfigPath, mac_user_conf_dir, null); // copy all files recursively
//					} catch (FileNotFoundException e) {
//						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					} catch (IOException e) {
//						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//					}
//				}
//				this.userConfigPath = mac_user_conf_dir;
//			}
//		}
//		return this.userConfigPath.getAbsolutePath() + File.separator;
//	}

	/**
	 * Gets the config directory of the application.
	 *
	 * @return config directory
	 */
	public String getConfigDirectory() {
		if (this.configPath==null) {
			File pathFile = new File(getInstallDirectory(), ".paths");
			if (pathFile.exists() && pathFile.isFile()) {
				try {
					FileInputStream in = new FileInputStream(pathFile);
					Properties paths = new Properties();
					paths.load(in);
					in.close();
					
					if (paths.containsKey(IJAMConst.PATHKEY_CONFIGPATH)) {
						this.configPath = new File(paths.getProperty(IJAMConst.PATHKEY_CONFIGPATH, Long.toString(System.currentTimeMillis())));
						if (!this.configPath.exists()) this.configPath = null;
					}
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				
			} 
			if (this.configPath==null){
				this.configPath = new File(this.getInstallDirectory(), "config");
				
				if (OSUtils.isMultiuserEnabled()) {
					File loggedin_user_dir = new File(this.getBaseCurrentUserDirectory(), "config");
					if (!loggedin_user_dir.exists()) {
						loggedin_user_dir.mkdirs();
					}
					this.configPath = loggedin_user_dir;
				}
				
				Properties paths = new Properties();
				
				if (pathFile.exists() && pathFile.isFile()) {
					try {
						FileInputStream in = new FileInputStream(pathFile);
					
						paths.load(in);
						in.close();

					} catch (FileNotFoundException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				} 
				
				paths.put(IJAMConst.PATHKEY_CONFIGPATH, this.configPath.getAbsolutePath());
				try {
					FileOutputStream out = new FileOutputStream(pathFile);
					paths.store(out, "");
					out.close();
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			
		}
		return this.configPath.getAbsolutePath() + File.separator;
	}
	
	/**
	 * Gets the central photo directory of the application.
	 *
	 * @return central photo directory
	 */
	public String getPhotoDirectory() {
		if (this.photoPath==null) {
			File pathFile = new File(getInstallDirectory(), ".paths");
			if (pathFile.exists() && pathFile.isFile()) {
				try {
					FileInputStream in = new FileInputStream(pathFile);
					Properties paths = new Properties();
					paths.load(in);
					in.close();
					
					if (paths.containsKey(IJAMConst.PATHKEY_PHOTOPATH)) {
						this.photoPath = new File(paths.getProperty(IJAMConst.PATHKEY_PHOTOPATH, Long.toString(System.currentTimeMillis())));
						if (!this.photoPath.exists()) this.photoPath = null;
					}
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				
			} 
			if (this.photoPath==null){
				this.photoPath = new File(this.getDataDirectory(), "photos");
				
				Properties paths = new Properties();
				
				if (pathFile.exists() && pathFile.isFile()) {
					try {
						FileInputStream in = new FileInputStream(pathFile);
						paths.load(in);
						in.close();
					} catch (FileNotFoundException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				} 
				
				paths.put(IJAMConst.PATHKEY_PHOTOPATH, this.photoPath.getAbsolutePath());
				try {
					FileOutputStream out = new FileOutputStream(pathFile);
					paths.store(out, "");
					out.close();
				} catch (FileNotFoundException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			
		}
		return this.photoPath.getAbsolutePath() + File.separator;
	}

	/**
	 * Gets the log directory of the application.
	 *
	 * @return log directory
	 */
	public String getLogDirectory() {
		if (this.logPath==null) {
			this.logPath = new File(this.getInstallDirectory(), "logs");
			
			if (OSUtils.isMultiuserEnabled())
				this.logPath = new File(this.getBaseCurrentUserDirectory(), "logs");
			
			if (OSUtils.isMacOSX()) 
				this.logPath = new File(this.getUserhomeDirectory(), "Library"+File.separator+"Logs"+File.separator+"jAnrufmonitor");
		}
		return this.logPath.getAbsolutePath() + File.separator;
	}
	
    /**
     * Transforms a file string into an OS compliant 
     * filename string.
     * 
     * @param filename a filename
     * @return an OS compliant filename string
     */
    public String toFilePath(String filename, boolean create) {
		File f = new File(filename);
		if (create && !f.exists()) {
			f.mkdirs();
		}
		return f.getAbsolutePath();
    }
	
	/**
	 * Gets the classpath for a specified class.
	 * 
	 * @param aClass the class to look up.
	 * @return the classpath as a string representation.
	 */
    private String getWorkingDirectory(Class aClass) {  	
    	if (OSUtils.isWindows()) return getClassPathWin32(aClass);
    	
    	if (OSUtils.isLinux()) return getClassPathLinux(aClass);
    	
    	if (OSUtils.isMacOSX()) return getClassPathLinux(aClass);

    	m_logger.warning("Cannot detect opertating system, assuming Windows platform.");
    	
		return getClassPathWin32(aClass);		
    }

	private String getClassPathLinux(Class aClass) {
		try {
			String className = aClass.getName().replace('.', File.separatorChar) + ".class";
			ClassLoader classLoader = aClass.getClassLoader();
			String url = classLoader.getResource(className).toString();
			
			// remove encoding
			url = StringUtils.replaceString(url, "%20", " ");

			if (url.startsWith(JAR_FILE_PREFIX)) {
				url = url.substring(JAR_FILE_PREFIX.length(), url.length() - className.length() - 2);
				String temp = url.substring(0, url.lastIndexOf(File.separator) + 1);
				if (temp.length() < 1) {
					temp = url.substring(0, url.lastIndexOf("\\") + 1);
				}
				if (!temp.endsWith(File.separator)) {
					temp += File.separator;
				}

				File tmp = new File(temp);
				return tmp.getAbsolutePath() + File.separator;
			}
			if (url.startsWith(FILE_PREFIX)) {
				String cp = url.substring(FILE_PREFIX.length(), url.length() - className.length());
				if (!cp.endsWith(File.separator)) {
					cp += File.separator;
				}
                
				File tmp = new File(cp);
				return tmp.getAbsolutePath() + File.separator;
			}
			PathResolver.m_logger.warning("Invalid path detection for URL: "+url);	
		} catch (Exception e) {
			PathResolver.m_logger.severe(e.getMessage());	
		}
		return "";
	}
	
	private String getClassPathWin32(Class aClass) {
		final String JAR_FILE_PREFIX = "jar:file:";
		final String FILE_PREFIX = "file:";

		try {
			String className = aClass.getName().replace('.', '/') + ".class";
			ClassLoader classLoader = aClass.getClassLoader();
			String url = classLoader.getResource(className).toString();
			
			// remove encoding
			url = StringUtils.replaceString(url, "%20", " ");

			if (url.startsWith(JAR_FILE_PREFIX)) {
				url = url.substring(JAR_FILE_PREFIX.length(), url.length() - className.length() - 2);
				String temp = url.substring(0, url.lastIndexOf("/") + 1);
				if (temp.length() < 1) {
					temp = url.substring(0, url.lastIndexOf("\\") + 1);
				}
				if (!temp.endsWith("/")) {
					temp += "/";
				}
				if (temp.startsWith("/")) {
					temp = temp.substring(1);
				}
                
				File tmp = new File(temp);
				return tmp.getAbsolutePath() + File.separator;
			}
			if (url.startsWith(FILE_PREFIX)) {
				String cp = url.substring(FILE_PREFIX.length(), url.length() - className.length());
				if (cp.startsWith("/")) {
					cp = cp.substring(1, cp.length());
				}
				if (!cp.endsWith("/")) {
					cp += "/";
				}
                
				File tmp = new File(cp);
				return tmp.getAbsolutePath() + File.separator;
			}
			PathResolver.m_logger.warning("Invalid path detection for URL: "+url);	
		} catch (Exception e) {
			PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return "";
	}
//	
//	public void setLastCopyTimestamp(File f) {
//		long now = new Date().getTime();
//		ByteArrayInputStream in = new ByteArrayInputStream(Long.toString(now).getBytes());
//		try {
//			Stream.copy(in, new FileOutputStream(new File(f, ".ts")), true);
//		} catch (FileNotFoundException e) {
//			PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//		} catch (IOException e) {
//			PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//		}
//	}
//	
//	public long getLastCopyTimestamp(File f) {
//		if (new File(f, ".ts").exists()) {
//			ByteArrayOutputStream out = new ByteArrayOutputStream();
//			try {
//				Stream.copy(new FileInputStream(new File(f, ".ts")), out,true);
//				return Long.parseLong(out.toString());
//			} catch (FileNotFoundException e) {
//				PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//			} catch (IOException e) {
//				PathResolver.m_logger.log(Level.SEVERE, e.getMessage(), e);
//			}
//		}
//		return -1L;
//	}
	
	private void copy(File source_dir, File target_dir, FileFilter ff) throws FileNotFoundException, IOException {
		if (source_dir.isDirectory()) {
			if (ff==null) ff = new FileFilter() {
				public boolean accept(File pathname) {
					return true;
				}};
			File[] allFiles = source_dir.listFiles(ff);
			for (int i=0;i<allFiles.length;i++) {
				if (allFiles[i].isDirectory()) {
					this.copy(allFiles[i], new File(target_dir, allFiles[i].getName()), ff);
				} else if (allFiles[i].isFile()) {
					if (!target_dir.exists()) target_dir.mkdirs();
					Stream.copy(new FileInputStream(new File(source_dir, allFiles[i].getName())), new FileOutputStream(new File(target_dir, allFiles[i].getName())), true);
				}
			}
		}
	}

	
}
