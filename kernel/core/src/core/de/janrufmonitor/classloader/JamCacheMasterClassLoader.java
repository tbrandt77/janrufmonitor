package de.janrufmonitor.classloader;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;

public class JamCacheMasterClassLoader extends ClassLoader {

	private static JamCacheMasterClassLoader m_instance = null;

	private Logger m_logger;

	private char classNameReplacementChar;
	private Map m_definedClasses;
	private Connection m_cacheConnection;
	private boolean m_valid = true;

	private JamCacheMasterClassLoader() {		
		super();
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		
		m_definedClasses = new HashMap();
	}

	private void cleanJarFromCache(File jar) throws SQLException {
		if (this.m_cacheConnection==null) throw new SQLException("Connection to cache not available...");
		
		Statement stmt = this.m_cacheConnection.createStatement();
		stmt.execute("DELETE FROM jars WHERE jarname='"+jar.getAbsolutePath()+"';");

		stmt.execute("DELETE FROM classes WHERE jarname='"+jar.getAbsolutePath()+"';");
		
		this.m_cacheConnection.commit();		
	}
	
	private void cleanObsoleteJarFromCache() throws SQLException {
		if (this.m_cacheConnection==null) throw new SQLException("Connection to cache not available...");
		
		// clean up old jars...
		if (this.m_cacheConnection!=null) {
			Statement stmt = this.m_cacheConnection.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT jarname FROM jars;");
			String jar = null;
			File j = null;
			while (rs.next()) {
				jar = rs.getString(1);
				j = new File(jar);
				if (!j.exists()) {
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Cleaning obsolte JAR file "+jar+" from classes cache.");
					this.cleanJarFromCache(j);
				}
			
			}		
		}
		
	}
	
	private void insertJarToCache(File jar) throws SQLException {
		if (this.m_cacheConnection==null) throw new SQLException("Connection to cache not available...");
		
		Statement stmt = this.m_cacheConnection.createStatement();
		stmt.execute("INSERT INTO jars (jarname, createdate) VALUES('"+jar.getAbsolutePath()+"', '"+jar.lastModified()+"');");

		ZipArchive jr = new ZipArchive(jar, false);
		try {
			if (!jr.available())
				jr.open();
			
			List entries = jr.list(new FilenameFilter() {

				public boolean accept(File dir, String name) {
					if (name.toLowerCase().endsWith(".properties")) return false;
					if (name.toLowerCase().endsWith(".mf")) return false;
					return true;
				}}
			);
			String entry = null;
			byte[] c = null;
			PreparedStatement ps = this.m_cacheConnection.prepareStatement("INSERT INTO classes (classname, jarname, content) VALUES (?,?,?);");
			
			for (int i=0,j=entries.size();i<j;i++) {
				ps.clearParameters();
				entry = (String) entries.get(i);
				
				c = jr.getContent(entry);
				if (c!=null) {
					if (entry.length()>6 && entry.endsWith(".class"))
						entry = entry.substring(0, entry.length()-6);
					
					if (classNameReplacementChar == '\u0000') {
						entry = entry.replace('/', '.');
					} else {
						// Replace '.' with custom char, such as '_'
						entry = entry.replace(classNameReplacementChar, '.');
					}
					
					ps.setString(1, entry);
					ps.setString(2, jar.getAbsolutePath());
					ps.setObject(3, c);
					ps.addBatch();
					try {
						ps.executeBatch();
						ps.clearBatch();	
					} catch (SQLException ex) {
						m_logger.log(Level.INFO, entry +": "+ex.toString(), ex);
					}
				}
			}
		} catch (ZipArchiveException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
		} finally {
			try {
				jr.close();
			} catch (ZipArchiveException e) {
				m_logger.log(Level.SEVERE, e.toString(), e);
			}
		}
		this.m_cacheConnection.commit();	
	}
	
	private boolean isJarUpToDate(File jar) throws SQLException {	
		if (this.m_cacheConnection==null) throw new SQLException("Connection to cache not available...");
		
		Statement stmt = this.m_cacheConnection.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT createdate FROM jars WHERE jarname='"+jar.getAbsolutePath()+"';");
		while (rs.next()) {
			long ts = rs.getLong(1);
			return ts==jar.lastModified();
		}	
		
		return false;
	}

	private synchronized void buildJamJarHandler() {
		try {
			if (m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Cleaning classloader cache from obsolete jars...");
			this.cleanObsoleteJarFromCache();
		} catch (SQLException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
		}
		
		if (m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Building classloader cache...");
		File libDirectory = new File(PathResolver.getInstance()
				.getLibDirectory());
		if (libDirectory != null && libDirectory.exists()
				&& libDirectory.isDirectory()) {
			File[] jars = libDirectory.listFiles(new FileFilter() {

				public boolean accept(File pathname) {
					return pathname.getName().toLowerCase().endsWith(".jar");
				}
			});
			
			// check if jar is already in handler map
			if (jars != null && jars.length > 0) {
				for (int i = 0; i < jars.length; i++) {
					try {
						if (!this.isJarUpToDate(jars[i])){
							if (m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Updating jar in classloader cache: "+jars[i].getAbsolutePath());
							cleanJarFromCache(jars[i]);
							insertJarToCache(jars[i]);
						}
					} catch (SQLException e) {
						m_logger.log(Level.SEVERE, e.toString(), e);
					}
				}
			}
		}
	}

	public static synchronized JamCacheMasterClassLoader getInstance() {
		if (JamCacheMasterClassLoader.m_instance == null) {
			JamCacheMasterClassLoader.m_instance = new JamCacheMasterClassLoader();			
			try {
				JamCacheMasterClassLoader.m_instance.connectCache();
			} catch (Exception e) {
				JamCacheMasterClassLoader.m_instance.m_logger.log(Level.SEVERE, e.toString(), e);
				JamCacheMasterClassLoader.m_instance.m_valid = false;
			}
			if (JamCacheMasterClassLoader.m_instance.m_valid)
				JamCacheMasterClassLoader.m_instance.buildJamJarHandler();
		}
		return JamCacheMasterClassLoader.m_instance;
	}

	public static synchronized void invalidateInstance() {
		if (JamCacheMasterClassLoader.m_instance != null) {
			JamCacheMasterClassLoader.m_instance.invalidate();
			JamCacheMasterClassLoader.m_instance = null;
		}
	}

	public static synchronized void renewInstance() {
		if (JamCacheMasterClassLoader.m_instance != null) {
			JamCacheMasterClassLoader.m_instance.buildJamJarHandler();
		}
	}
	
	private void connectCache() throws ClassNotFoundException, SQLException {
		File classesCache = this.getCacheFile();
		
		classesCache.getParentFile().mkdirs();
		
		// check file count
		File[] childs = classesCache.getParentFile().listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().equalsIgnoreCase("classloader.cache.lck");
			}});
		
		if (childs!=null && childs.length>0) {
			for (int i=0;i<childs.length;i++) {
				m_logger.info("Clearing classes cache lock: "+childs[i].getAbsolutePath());
				childs[i].delete();
			}
		}
		
		boolean isCreateTable = ! new File(classesCache.getAbsolutePath()+".properties").exists();
		
		Class.forName("org.hsqldb.jdbcDriver");
		m_cacheConnection = DriverManager.getConnection("jdbc:hsqldb:file:"+classesCache.getAbsolutePath(), "sa", "");
		
		if (isCreateTable) {
			Statement stmt = m_cacheConnection.createStatement();
			stmt.execute("CREATE TABLE jars (jarname VARCHAR(128), createdate BIGINT);");
			stmt.execute("CREATE TABLE classes (classname VARCHAR(1024) PRIMARY KEY, jarname VARCHAR(128), content OTHER);");
			
			m_cacheConnection.commit();
		}

	}
	
	private File getCacheFile() {
		if (OSUtils.isMultiuserEnabled()) {
			return new File(PathResolver.getInstance()
					.getInstallDirectory()+"/users/"+OSUtils.getLoggedInUser()+"/lib/cache/classloader.cache");
		}
		return new File(PathResolver.getInstance()
				.getLibDirectory()+"/cache/classloader.cache");
	}
	
	private void disconnectCache() throws SQLException {
		if (m_cacheConnection==null) return;
		
		m_cacheConnection.commit();
		
		Statement st = m_cacheConnection.createStatement();
		st.execute("SHUTDOWN");
		
		m_cacheConnection.close();
	}

	private void invalidate() {
		//this.m_jjhr.setState(false);
		try {
			this.disconnectCache();
		} catch (SQLException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
		}
	}

	public Class loadClass(String className) throws ClassNotFoundException {
		return (loadClass(className, true));
	}
	
	public boolean isValid() {
		return this.m_valid;
	}

	public synchronized Class loadClass(String className, boolean resolveIt)
			throws ClassNotFoundException {

		Class result = null;

		// ----- Check with the primordial class loader
		try {
			result = super.findSystemClass(className);
			if (m_logger.isLoggable(Level.FINE))
				this.m_logger.fine("Loaded class " + className
						+ " from system classloader.");
			return result;
		} catch (ClassNotFoundException e) {
			if (m_logger.isLoggable(Level.FINE))
				this.m_logger.fine("Class " + className
						+ " not found in system classloader.");
		}
		
		// ----- Try to load it from preferred source
		try {
			byte[] classBytes = getClassFromCache(className);
			if (classBytes == null) {
				if (m_logger.isLoggable(Level.FINE))
					m_logger.fine("Class " + className + " not found in "
							+ this.toString());
				throw new ClassNotFoundException();
			}

			if (!m_definedClasses.containsKey(className)) {
				//		 ----- Define it (parse the class file)
				result = defineClass(className, classBytes, 0, classBytes.length);
				if (result == null) {
					throw new ClassFormatError();
				}
				// ----- Resolve if necessary
				if (resolveIt)
					resolveClass(result);
				
				this.m_definedClasses.put(className, result);	
			} else {
				result = (Class) this.m_definedClasses.get(className);
			}
			
			return result;
		} catch (SQLException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
		}
		

		throw new ClassNotFoundException();
	}

	private byte[] getClassFromCache(String className) throws SQLException {
		if (this.m_cacheConnection==null) throw new SQLException("Connection to cache not available...");

		Statement stmt = this.m_cacheConnection.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT content FROM classes WHERE classname='"+className+"';");
		if (m_logger.isLoggable(Level.FINE))
			m_logger.fine("SELECT content FROM classes WHERE classname='"+className+"';");
		
		while (rs.next()) {
			return (byte[]) rs.getObject(1);
		}	
		
		return null;
	}

	public void setClassNameReplacementChar(char replacement) {
		classNameReplacementChar = replacement;
	}

}
