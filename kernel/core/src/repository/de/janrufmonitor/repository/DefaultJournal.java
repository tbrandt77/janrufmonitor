package de.janrufmonitor.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.logging.Level;

import de.janrufmonitor.repository.db.ICallDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbCallDatabaseHandler;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;

public class DefaultJournal extends AbstractDatabaseCallManager implements ILocalRepository {

	private class DefaultJournalHandler extends HsqldbCallDatabaseHandler {

		private IRuntime m_runtime;
		
		public DefaultJournalHandler(String driver, String connection, String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
	}

	private static String ID = "DefaultJournal";
	private static String NAMESPACE = "repository.DefaultJournal";
	
	private static String CFG_DB= "db";
	private static String CFG_COMMIT_COUNT= "commit";
	private static String CFG_KEEP_ALIVE= "keepalive";
	
	private IRuntime m_runtime;
	
	public DefaultJournal() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getID() {
		return DefaultJournal.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getNamespace() {
		return DefaultJournal.NAMESPACE;
	}
	
	public void startup() {
		String root = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/journal.db"));

		File props = new File(root + ".properties");
		if (!props.exists())  {
			props.getParentFile().mkdirs();
			try {
				File db_raw = new File(root);
				if (db_raw.exists()) {
					// exctract old data
					ZipArchive z = new ZipArchive(root);
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
					z.close();
				}
			} catch (ZipArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
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
	}

	public void shutdown() {
		try {
			if (this.getDatabaseHandler().isConnected())
				this.getDatabaseHandler().disconnect();
			
			String root = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/journal.db"));
			File db_raw = new File(root);
			ZipArchive z = new ZipArchive(root);
			z.open();
			String[] entries = new String[2];
			InputStream[] ins = new InputStream[2];
			if (new File(db_raw.getAbsolutePath()+".properties").exists()) {
				entries[0] = db_raw.getName()+".properties";
				ins[0] = new FileInputStream(db_raw.getAbsolutePath()+".properties");
			}
			
			if (new File(db_raw.getAbsolutePath()+".script").exists()) {
				entries[1] = db_raw.getName()+".script";
				ins[1] = new FileInputStream(db_raw.getAbsolutePath()+".script");
			}
			z.add(entries, ins);
			z.close();
		} catch (ZipArchiveException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		super.shutdown();
	}

	protected ICallDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh==null) {
			String db_path = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"journal.db"));
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
			this.m_dbh = new DefaultJournalHandler("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:"+db_path, "sa", "", initialize);
			this.m_dbh.setCommitCount(Integer.parseInt(m_configuration.getProperty(CFG_COMMIT_COUNT, "50")));
			this.m_dbh.setKeepAlive((m_configuration.getProperty(CFG_KEEP_ALIVE, "true").equalsIgnoreCase("true")? true : false));
		}	
		return this.m_dbh;
	}

	public String getFile() {
		return PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/journal.db"));
	}
	
	public String getFileType() {
		return "*.db";
	}

	public void setFile(String filename) {
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), CFG_DB, filename);
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();		
	}

}
