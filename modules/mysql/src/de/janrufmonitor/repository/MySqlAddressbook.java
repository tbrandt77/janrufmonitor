package de.janrufmonitor.repository;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbMultiPhoneCallerDatabaseHandler;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.ImageHandler;

public class MySqlAddressbook extends AbstractDatabaseCallerManager implements
		IRemoteRepository, IEventReceiver {

	private static String ID = "MySqlAddressbook";
    private String NAMESPACE = "repository.MySqlAddressbook";

	private static String CFG_DB_SERVER = "dbserver";
	private static String CFG_DB_DB = "dbdb";
	private static String CFG_DB_PORT = "dbport";
	private static String CFG_DB_USER = "dbuser";
	private static String CFG_DB_PASSWORD = "dbpassword";
	private static String CFG_KEEP_ALIVE= "keepalive";
    
    private IRuntime m_runtime;

    private class MySqlHandler extends HsqldbMultiPhoneCallerDatabaseHandler {

    	private IRuntime m_runtime;
    	
		public MySqlHandler(String driver, String connection, String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
		
		protected void createTables() throws SQLException {
			if (!isConnected()) throw new SQLException ("Database is disconnected.");

			Statement stmt = m_con.createStatement();		
			try {
				stmt.execute("DROP TABLE attributes;");
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			} 
			try {
				stmt.execute("DROP TABLE callers;");
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			} 
			try {
				stmt.execute("DROP TABLE phones;");
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			} 
			try {
				stmt.execute("DROP TABLE versions;");
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			} 
			
			stmt.execute("CREATE TABLE versions (version VARCHAR(10));");
			stmt.execute("INSERT INTO versions (version) VALUES ('"+IJAMConst.VERSION_DISPLAY+"');");
			stmt.execute("CREATE TABLE attributes (ref VARCHAR(36), name VARCHAR(64), value VARCHAR(2048));");
			stmt.execute("CREATE TABLE callers (uuid VARCHAR(36) PRIMARY KEY, content TEXT("+ Short.MAX_VALUE + "));");
			stmt.execute("CREATE TABLE phones (ref VARCHAR(36), country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64), phone VARCHAR(128));");

			stmt.close();
		}
		
		protected boolean isInitializing() {
			try {
				if (!isConnected()) return false;
			} catch (SQLException e) {
				return false;
			}

			try {
				Statement stmt = m_con.createStatement();	
				stmt.execute("SELECT uuid FROM callers LIMIT 0,1;");
				// table exists
			} catch (SQLException e) {
				return true;
			} 
			return false;
		}
		
		public void commit() throws SQLException {
			// do nothing for mysql, since auto-commit is active
		}
		
		public void rollback() throws SQLException {
			// do nothing for mysql, since auto-rollback is active
		}

		public void disconnect() throws SQLException {
			if (this.m_ip!=null) ImageHandler.getInstance().removeProvider(this.m_ip);
			
			if (this.m_con==null) throw new SQLException ("Database already disconnected.");
			m_con.close();
			m_con = null;
			this.m_logger.info("DatabaseHandler successfully disconnected.");
		}

		public String getImageProviderID() {
			return ID;
		}

		protected void addPreparedStatements() throws SQLException {
			if (!isConnected())
				throw new SQLException("Database is disconnected.");
			
			// check database structure
			Statement stmt = m_con.createStatement();
			try {
				stmt.executeQuery("SELECT count(*) FROM images;");
			} catch (Exception e) {
				this.m_logger.info("Detected database schema of version 5.0.27 and older.");
				
				stmt.execute("CREATE TABLE images (ref VARCHAR(36), value LONGTEXT);");		
			} finally {
				stmt.close();
			}	
			
			super.addPreparedStatements();
		}
    	
    }
    
    public MySqlAddressbook() {
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
			this.m_dbh = new MySqlHandler("com.mysql.jdbc.Driver", "jdbc:mysql://"+this.m_configuration.getProperty(CFG_DB_SERVER, "localhost")+":"+this.m_configuration.getProperty(CFG_DB_PORT, "3306")+"/"+this.m_configuration.getProperty(CFG_DB_DB, "journal"), this.m_configuration.getProperty(CFG_DB_USER), this.m_configuration.getProperty(CFG_DB_PASSWORD), false);
			this.m_dbh.setKeepAlive((m_configuration.getProperty(CFG_KEEP_ALIVE, "false").equalsIgnoreCase("true")? true : false));
		}	
		return this.m_dbh;
	}
	

	@Override
	public void startup() {
		super.startup();
		
		IEventBroker eb = getRuntime().getEventBroker();
		eb.register(this, eb.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
	}

	@Override
	public void shutdown() {
		IEventBroker eb = getRuntime().getEventBroker();
		eb.register(this, eb.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
		
		super.shutdown();
	}

	public void received(IEvent event) {
		if (event.getType() == IEventConst.EVENT_TYPE_RETURNED_HIBERNATE) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Restarting MySQL connection for address book after hibernate mode.");
			
			if (this.m_dbh!=null) {
				try {
					this.m_dbh.disconnect();
				} catch (SQLException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				this.m_dbh = null;
			}
		}
	}

	public String getReceiverID() {
		return ID;
	}
	
}