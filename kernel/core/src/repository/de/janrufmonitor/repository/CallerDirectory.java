package de.janrufmonitor.repository;

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

		public CallerDirectoryHandler(String db) {
			super(db);
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
			String db_path = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getUserDataDirectory()+"/phonebook.db"));
			this.m_dbh = new CallerDirectoryHandler(db_path);
			this.m_dbh.setCommitCount(Integer.parseInt(m_configuration.getProperty(CFG_COMMIT_COUNT, "50")));
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
		return PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getUserDataDirectory()+"/phonebook.db"));
	}
	
	public String getFileType() {
		return "*.db";
	}

	public void setFile(String filename) {
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), CFG_DB, filename);
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();		
	}
	

}
