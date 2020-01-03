package de.janrufmonitor.repository.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.IRuntime;

/**
 *  This abstract class can be used as base class for a database related managers.
 *  It contains methods for database creation and prepared statements for manager handling.
 *
 *@author     Thilo Brandt
 *@created    2006/05/27
 */
public abstract class AbstractDatabaseHandler implements IDatabaseHandler {
	
	protected Connection m_con;
	private String m_driver, m_connection, m_user, m_pass;
	private boolean needCreateDB;
	protected Map m_preparedStatements;
	protected Logger m_logger;
	protected int commit_count = 50;
	private boolean keepAlive = true;

	/**
	 * Constructor dor a data handler calling the underlaying database
	 * 
	 * @param driver JDBC driver string
	 * @param connection connection String
	 * @param user user, if required
	 * @param password password, if user was set
	 * @param initialize indicates wether the db tables must be created or not. 
	 */
	public AbstractDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_preparedStatements = new HashMap();
		this.m_connection = connection;
		this.m_driver = driver;
		this.m_pass = password;
		this.m_user = user;
		this.needCreateDB = initialize;
	}

	protected abstract IRuntime getRuntime();
	
	/**
	 * Creates the database table.
	 * 
	 * @return
	 */
	protected abstract void createTables() throws SQLException;
	
	/**
	 * Add all required prepares statements to the m_preparedStatements map. The key should be provided as a String.
	 * 
	 * @throws SQLException
	 */
	protected abstract void addPreparedStatements() throws SQLException;
	
	/**
	 * Checks wether a connection is established or not.
	 * 
	 * @return true if connection exists.
	 * @throws SQLException
	 */
	public boolean isConnected() throws SQLException {
		return m_con != null && !m_con.isClosed();
	}
	
	/**
	 * Connects to the database specified in the constructor. Tables and prepared statements are created
	 * if teh initialize flag was set.
	 * 
	 * @throws SQLException is thrown, if any exception on the db level occurs.
	 * @throws ClassNotFoundException is thrown, if the db driver could not be loaded.
	 */
	public void connect() throws SQLException, ClassNotFoundException {
		if (m_con!=null) throw new SQLException ("Database already connected.");
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("JDBC driver set to "+this.m_driver); 
		
		if (this.m_driver!=null)
			Class.forName(this.m_driver);
		
		if ((this.m_user==null || this.m_user.trim().length()==0) && (this.m_pass==null || this.m_pass.trim().length()==0)){
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("JDBC driver user and password not set."); 
			m_con = DriverManager.getConnection(this.m_connection);
		} else {
			m_con = DriverManager.getConnection(this.m_connection, this.m_user, this.m_pass);
		}

		// create tables if required
		if (isInitializing()) {
			this.createTables();
		}
		
		// prepare statements
		this.addPreparedStatements();
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("DatabaseHandler successfully connected.");
	}
	
	/**
	 * Must return true, if the database tables should be created.
	 * @return
	 */
	protected boolean isInitializing() {
		return needCreateDB;
	}
	
	/**
	 * Sets the initinal attribute of the database. 
	 * Required if keep-alive is disabled and multiple re-connects are expected.
	 */
	protected void setInitializing(boolean init) {
		this.needCreateDB = init;
	}
	
	protected PreparedStatement getStatement(String id) {
		return (PreparedStatement) m_preparedStatements.get(id);
	}
	
	protected void createAttributes(PreparedStatement ps, String uuid, IAttributeMap m) throws SQLException {
		Iterator i = m.iterator();
		IAttribute att = null;
		while (i.hasNext()) {
			att = (IAttribute) i.next();
			ps.clearParameters();
			ps.setString(1, uuid);
			ps.setString(2, att.getName());
			ps.setString(3, att.getValue());
			ps.addBatch();
		}
	}
	
	protected void deleteAttributes(PreparedStatement ps, String uuid) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.addBatch();
	}
	
	protected void updateAttributes(PreparedStatement ps, String uuid, IAttributeMap m) throws SQLException {
		Iterator i = m.iterator();
		IAttribute att = null;
		while (i.hasNext()) {
			att = (IAttribute) i.next();
			ps.clearParameters();
			ps.setString(1, att.getValue());
			ps.setString(2, uuid);
			ps.setString(3, att.getName());
			ps.addBatch();
		}
	}
	
	/**
	 * Commits the changed data to the database.
	 * 
	 * @throws SQLException
	 */
	public void commit() throws SQLException {
		if (isConnected())
			m_con.commit();	
	}
	
	/**
	 * Rolls back the db changed since the last commit. Should be called if a 
	 * SQLException was thrown anywhere.
	 * 
	 * @throws SQLException
	 */
	public void rollback() throws SQLException {
		if (isConnected())
			m_con.rollback();
	}
	
	/**
	 * Disconnects the current db and shuts it down. The connection is set to null.
	 * 
	 * @throws SQLException
	 */
	public void disconnect() throws SQLException {
		if (this.m_con==null) throw new SQLException ("Database already disconnected.");
		m_con.close();
		m_con = null;
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("DatabaseHandler successfully disconnected.");
	}
	
	public void setCommitCount(int c) {
		if (c>0)
			this.commit_count = c;
	}
	
	public boolean isKeepAlive() {
		return this.keepAlive;
	}
	
	public void setKeepAlive(boolean keep) {
		this.keepAlive = keep;
	}


}
