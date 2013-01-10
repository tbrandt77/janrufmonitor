package de.janrufmonitor.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbMultiPhoneCallerDatabaseHandler;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;

public class CallerDirectory 
	extends AbstractDatabaseCallerManager implements ILocalRepository {

	private static String ID = "CallerDirectory";
    private String NAMESPACE = "repository.CallerDirectory";
    
	private static String CFG_DB= "db";
	private static String CFG_COMMIT_COUNT= "commit";
	private static String CFG_KEEP_ALIVE= "keepalive";
	
	private IRuntime m_runtime;
	
	private class CallerDirectoryHandler extends HsqldbMultiPhoneCallerDatabaseHandler {
		
		private IRuntime m_runtime;

		public CallerDirectoryHandler(String driver, String connection, String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}

		protected void addPreparedStatements() throws SQLException {
			if (!isConnected())
				throw new SQLException("Database is disconnected.");
			
			// check database structure
			Statement stmt = m_con.createStatement();
			try {
				stmt.executeQuery("SELECT count(*) FROM versions;");
			} catch (Exception e) {
				this.m_logger.info("Detected database of Version 4.5.");
				
				// create phones table
				stmt.execute("SELECT uuid, country, areacode, number, phone INTO phones FROM callers");
				stmt.execute("ALTER TABLE callers DROP country;");
				stmt.execute("ALTER TABLE callers DROP areacode;");
				stmt.execute("ALTER TABLE callers DROP number;");
				stmt.execute("ALTER TABLE callers DROP phone;");
				stmt.execute("ALTER TABLE phones ALTER COLUMN uuid RENAME TO ref;");

				stmt.execute("CREATE TABLE versions (version VARCHAR(10));");
				stmt.execute("INSERT INTO versions (version) VALUES ('"+IJAMConst.VERSION_DISPLAY+"');");				
			} finally {
				stmt.close();
			}			
			super.addPreparedStatements();
		}
		
		public String getImageProviderID() {
			return ID;
		}

	}

    public CallerDirectory() {
        super();
		this.getRuntime().getConfigurableNotifier().register(this);
    }
    
	public void startup() {
		String root = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/addressbook.db"));
		
		File props = new File(root + ".properties");
		if (!props.exists())  {
			props.getParentFile().mkdirs();
			ZipArchive z = new ZipArchive(root);
			try {
				File db_raw = new File(root);
				if (db_raw.exists()) {
					z.open();
					if (z.isCreatedByCurrentVersion()) {
						InputStream in = z.get(db_raw.getName()+".properties");
						if (in!=null) {
							FileOutputStream out = new FileOutputStream(db_raw.getAbsolutePath()+".properties");
							Stream.copy(in, out, true);
						}
						in = z.get(db_raw.getName()+".script");
						if (in!=null) {
							FileOutputStream out = new FileOutputStream(db_raw.getAbsolutePath()+".script");
							Stream.copy(in, out, true);
						}						
					} else {
						// check if an old db file was selected
						if (z.size()==3) {
							this.m_logger.info("Found 4.5 compatible database");
							InputStream in = z.get(db_raw.getName()+".properties");
							if (in!=null) {
								FileOutputStream out = new FileOutputStream(db_raw.getAbsolutePath()+".properties");
								Stream.copy(in, out, true);
							}
							in = z.get(db_raw.getName()+".script");
							if (in!=null) {
								FileOutputStream out = new FileOutputStream(db_raw.getAbsolutePath()+".script");
								Stream.copy(in, out, true);
							}						
						}
					}
				}
			} catch (ZipArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} finally {
				try {
					if (z.available())
						z.close();
				} catch (ZipArchiveException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		} else {
			try {
				File db_raw = new File(root);
				ZipArchive z = new ZipArchive(root);
				z.open();
				String[] entries = new String[] { db_raw.getName()+".properties", db_raw.getName()+".script" };
				InputStream[] ins = new InputStream[] { new FileInputStream(db_raw.getAbsolutePath()+".properties"),new FileInputStream(db_raw.getAbsolutePath()+".script") };
				z.add(entries, ins);
				z.close();
			} catch (ZipArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}		
		
		super.startup();
		
		try {
			if (!getDatabaseHandler().isConnected())
				getDatabaseHandler().connect();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
		
	public String getID() {
		return CallerDirectory.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
    public String getNamespace() {
        return this.NAMESPACE;
    }

	protected ICallerDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh==null) {
			String db_path = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/addressbook.db"));
			db_path = StringUtils.replaceString(db_path, "\\", "/");
			File db = new File(db_path + ".properties");
			boolean initialize = false;
			if (!db.exists())  {
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
			this.m_dbh = new CallerDirectoryHandler("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:"+db_path, "sa", "", initialize);
			this.m_dbh.setCommitCount(Integer.parseInt(m_configuration.getProperty(CFG_COMMIT_COUNT, "50")));
			this.m_dbh.setKeepAlive((m_configuration.getProperty(CFG_KEEP_ALIVE, "true").equalsIgnoreCase("true")? true : false));
		}
		return this.m_dbh;
	}

	public String getFile() {
		return PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/addressbook.db"));
	}
	
	public String getFileType() {
		return "*.db";
	}

	public void setFile(String filename) {
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), CFG_DB, filename);
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();		
	}
	

}
