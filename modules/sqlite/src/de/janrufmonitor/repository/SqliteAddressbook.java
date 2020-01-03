package de.janrufmonitor.repository;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbMultiPhoneCallerDatabaseHandler;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class SqliteAddressbook extends AbstractDatabaseCallerManager implements ILocalRepository {

	private static String ID = "SqliteAddressbook";
    private String NAMESPACE = "repository.SqliteAddressbook";

	private static String CFG_DB = "db";
	private static String CFG_KEEP_ALIVE= "keepalive";
    
    private IRuntime m_runtime;

	private class SqliteHandler extends HsqldbMultiPhoneCallerDatabaseHandler {
		
		private IRuntime m_runtime;

		public SqliteHandler(String db) {
			super(null, "jdbc:sqlite:"+StringUtils.replaceString(db, "\\", "/"), null, null, false);
			File db_raw = new File(db);
			if (db_raw.exists()) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Database file found: "+db_raw.getAbsolutePath());
				
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Database file size: "+db_raw.length());
			} else {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Database file does not exist: "+db_raw.getAbsolutePath());
				
				this.setInitializing(true);
				
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Checking and creating directory structure: "+db_raw.getParentFile().getAbsolutePath());
				
				db_raw.getParentFile().mkdirs();
			}
		}
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
		
		public String getImageProviderID() {
			return ID;
		}
		
		protected void createTables() throws SQLException {
			if (!isConnected()) throw new SQLException ("Database is disconnected.");

				
			Statement stmt = m_con.createStatement();
			stmt.execute("DROP TABLE IF EXISTS attributes;");
			stmt.execute("DROP TABLE IF EXISTS callers;");
			stmt.execute("DROP TABLE IF EXISTS phones;");
			stmt.execute("DROP TABLE IF EXISTS versions;");

			stmt.execute("CREATE TABLE IF NOT EXISTS versions (version VARCHAR(10));");
			stmt.execute("INSERT INTO versions (version) VALUES ('"+IJAMConst.VERSION_DISPLAY+"');");
			stmt.execute("CREATE TABLE IF NOT EXISTS attributes (ref VARCHAR(36), name VARCHAR(64), value VARCHAR(2048));");
			stmt.execute("CREATE TABLE IF NOT EXISTS callers (uuid VARCHAR(36) PRIMARY KEY, content VARCHAR("+ Short.MAX_VALUE + "));");
			stmt.execute("CREATE TABLE IF NOT EXISTS phones (ref VARCHAR(36), country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64), phone VARCHAR(128));");

		}

	}

    
    public SqliteAddressbook() {
        super();
		this.getRuntime().getConfigurableNotifier().register(this);
    }
    
	public String getID() {
		return ID;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
    
	protected ICallerDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh==null) {
			String db_path = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getUserDataDirectory()+"/phonebook.sqlite"));
			this.m_dbh = new SqliteHandler(db_path);
			//this.m_dbh.setCommitCount(Integer.parseInt(m_configuration.getProperty(CFG_COMMIT_COUNT, "50")));
			this.m_dbh.setKeepAlive((m_configuration.getProperty(CFG_KEEP_ALIVE, "true").equalsIgnoreCase("true")? true : false));
			try {
				this.m_dbh.connect();
			} catch (ClassNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return this.m_dbh;
	}
	
	public String getFile() {
		return PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getUserDataDirectory()+"/phonebook.sqlite"));
	}
	
	public String getFileType() {
		return "*.sqlite";
	}

	public void setFile(String filename) {
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), CFG_DB, filename);
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();		
	}


}