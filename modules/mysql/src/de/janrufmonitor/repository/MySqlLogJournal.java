package de.janrufmonitor.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.db.AbstractDatabaseHandler;
import de.janrufmonitor.repository.db.ICallDatabaseHandler;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.uuid.UUID;

public class MySqlLogJournal extends AbstractDatabaseCallManager {

	private class MySqlHandler extends AbstractDatabaseHandler implements ICallDatabaseHandler {

		private IRuntime m_runtime;

		public MySqlHandler(String driver, String connection, String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}
		
		protected void addPreparedStatements() throws SQLException {
			// prepare MySQL statements
			m_preparedStatements.put("INSERT_CALL", m_con.prepareStatement("INSERT INTO callslog (uuid, country, areacode, number, status, msn, cip, cdate, ndate) VALUES (?, ?, ?, ?, ?, ?, ?, ?,FROM_UNIXTIME(cdate/1000));"));			
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
		
		public void connect() throws SQLException, ClassNotFoundException {
			try {
				super.connect();
			} catch (SQLException e) {
				PropagationFactory.getInstance().fire(
					new Message(Message.ERROR, NAMESPACE, "connecterror", e));
				throw e;
			}
			
		}

		protected void createTables() throws SQLException {
			if (!isConnected()) throw new SQLException ("Database is disconnected.");

			Statement stmt = m_con.createStatement();
			try {
				stmt.execute("DROP TABLE callslog;");
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			} 
			
			stmt.execute("CREATE TABLE callslog (uuid VARCHAR(36), country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64), status VARCHAR(16), msn VARCHAR(8), cip VARCHAR(4), cdate BIGINT, ndate TIMESTAMP);");

//			super.createTables();
		}
		
		private void createCall(PreparedStatement ps, String uuid, String country, String areacode, String number, String status, String msn, String cip, long date) throws SQLException {
			ps.clearParameters();
			ps.setString(1, uuid);
			ps.setString(2, country);
			ps.setString(3, areacode);
			ps.setString(4, number);
			ps.setString(5, status);
			ps.setString(6, msn);
			ps.setString(7, cip);
			ps.setLong(8, date);
			ps.addBatch();
		}
		
		protected boolean isInitializing() {
			try {
				if (!isConnected()) return false;
			} catch (SQLException e) {
				return false;
			}

			try {
				Statement stmt = m_con.createStatement();	
				stmt.execute("SELECT uuid FROM callslog LIMIT 0,1;");
				// table exists
			} catch (SQLException e) {
				return true;
			}
			
			return false;
		}

		public void setCallList(ICallList cl) throws SQLException {
			if (!isConnected())
				try {
					this.connect();
				} catch (ClassNotFoundException e) {
					throw new SQLException(e.getMessage());
				}
				
			List uuid_check = new ArrayList(cl.size());
			
			PreparedStatement ps = this.getStatement("INSERT_CALL");
			ps.clearBatch();

			ICall c = null;
			IPhonenumber pn = null;
			String uuid = null;
			for (int i=0, j=cl.size();i<j;i++) {
				c = cl.get(i);
				if (this.m_logger.isLoggable(Level.INFO) && c!=null)
					this.m_logger.info("Adding to database: "+c.toString());
				
				// check if redundant uuid could occure
				uuid = c.getUUID();
				if (uuid_check.contains(uuid)) {
					this.m_logger.warning("Found duplicated UUID: "+c.toString());
					c.setUUID(new UUID().toString());
					uuid = c.getUUID();
				}
				uuid_check.add(uuid);

				pn = c.getCaller().getPhoneNumber();
				this.createCall(ps, uuid, pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), (c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS)!=null ? c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS).getValue() : ""), c.getMSN().getMSN(), c.getCIP().getCIP(), c.getDate().getTime());
				
				if (i % this.commit_count == 0) {
					try {
						ps.executeBatch();			
						ps.clearBatch();
						this.m_logger.info("-------------------> executed Batch");
					} catch (SQLException e) {	
						this.m_logger.log(Level.SEVERE, e.getMessage()+ c.toString(), e);					
					}
				}
			}
			// execute the rest batch content
			ps.executeBatch();

			uuid_check.clear();
			uuid_check=null;
			
		}

		public void updateCallList(ICallList cl) throws SQLException {
			if (!isConnected())
				try {
					this.connect();
				} catch (ClassNotFoundException e) {
					throw new SQLException(e.getMessage());
				}
				

			PreparedStatement ps = this.getStatement("INSERT_CALL");
			ps.clearBatch();

			ICall c = null;
			IPhonenumber pn = null;
			for (int i=0, j=cl.size();i<j;i++) {
				c = cl.get(i);
				pn = c.getCaller().getPhoneNumber();
				this.createCall(ps, c.getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), (c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS)!=null ? c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS).getValue() : ""), c.getMSN().getMSN(), c.getCIP().getCIP(), c.getDate().getTime());
				
				if (i % this.commit_count == 0) {
					ps.executeBatch();
					ps.clearBatch();
					this.m_logger.info("Executed prepared statement: "+ps.toString());		
				}
			}
			// execute the rest batch content
			ps.executeBatch();
		}

		public void deleteCallList(ICallList cl) throws SQLException {
		}

		public ICallList getCallList(IFilter[] filters) throws SQLException {
			return getRuntime().getCallFactory().createCallList();
		}

		public ICallList getCallList(IFilter[] filters, int count, int offset)
				throws SQLException {
			return getRuntime().getCallFactory().createCallList();
		}

		public int getCallCount(IFilter[] filters) throws SQLException {
			return 0;
		}
		
		public void commit() throws SQLException {
			// do nothing for mysql, since auto-commit is active
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Ignoring COMMIT call, due to auto-commit.");
		}
		
		public void rollback() throws SQLException {
			// do nothing for mysql, since auto-rollback is active
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Ignoring ROLLBACK call, due to auto-commit.");
		}

		public ICallList getCallList(IFilter[] filters, int count, int offset,
				ISearchTerm[] searchTerms) throws SQLException {
			return this.getCallList(filters, count, offset);
		}

		public int getCallCount(IFilter[] filters, ISearchTerm[] searchTerms)
				throws SQLException {
			return 0;
		}
	}
	
	private static String ID = "repository.MySqlLogJournal";
	private static String NAMESPACE = "repository.MySqlLogJournal";
	
	private static String CFG_DB_SERVER = "dbserver";
	private static String CFG_DB_DB = "dbdb";
	private static String CFG_DB_PORT = "dbport";
	private static String CFG_DB_USER = "dbuser";
	private static String CFG_DB_PASSWORD = "dbpassword";
	private static String CFG_KEEP_ALIVE= "keepalive";
	
	private IRuntime m_runtime;

	public MySqlLogJournal() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getID() {
		return MySqlLogJournal.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getNamespace() {
		return MySqlLogJournal.NAMESPACE;
	}

	protected ICallDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh==null) {
			this.m_dbh = new MySqlHandler("com.mysql.jdbc.Driver", "jdbc:mysql://"+this.m_configuration.getProperty(CFG_DB_SERVER, "localhost")+":"+this.m_configuration.getProperty(CFG_DB_PORT, "3306")+"/"+this.m_configuration.getProperty(CFG_DB_DB, "journal"), this.m_configuration.getProperty(CFG_DB_USER), this.m_configuration.getProperty(CFG_DB_PASSWORD), false);
			this.m_dbh.setKeepAlive((m_configuration.getProperty(CFG_KEEP_ALIVE, "false").equalsIgnoreCase("true")? true : false));
		}	
		return this.m_dbh;
	}

}
