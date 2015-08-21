package de.janrufmonitor.repository.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;
import de.janrufmonitor.util.uuid.UUID;

/**
 *  This abstract class can be used as base class for a database related call managers.
 *  It contains methods for database creation and prepared statements for call manager handling.
 *
 *@author     Thilo Brandt
 *@created    2006/05/27
 */
public abstract class AbstractCallDatabaseHandler extends AbstractDatabaseHandler implements ICallDatabaseHandler {

	public AbstractCallDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
		super(driver, connection, user, password, initialize);
	}

	protected void createTables() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		Statement stmt = m_con.createStatement();
		stmt.execute("CREATE TABLE attributes (ref VARCHAR(36), name VARCHAR(64), value VARCHAR(2048));");
		stmt.execute("CREATE TABLE calls (uuid VARCHAR(36) PRIMARY KEY, cuuid VARCHAR(36), country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64), msn VARCHAR(8), cip VARCHAR(4), cdate BIGINT, content VARCHAR("+Short.MAX_VALUE+"));");
	}

	/**
	 * Connects to the database specified in the constructor. Tables and prepared statements are created
	 * if teh initialize flag was set.
	 * 
	 * @throws SQLException is thrown, if any exception on the db level occurs.
	 * @throws ClassNotFoundException is thrown, if the db driver could not be loaded.
	 */
	protected void addPreparedStatements() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		// prepare statements
		m_preparedStatements.put("INSERT_ATTRIBUTE", m_con.prepareStatement("INSERT INTO attributes (ref, name, value) VALUES (?,?,?);"));
		m_preparedStatements.put("INSERT_CALL", m_con.prepareStatement("INSERT INTO calls (uuid, cuuid, country, areacode, number, msn, cip, cdate, content) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);"));

		m_preparedStatements.put("UPDATE_ATTRIBUTE", m_con.prepareStatement("UPDATE attributes SET value=? WHERE ref=? AND name=?;"));
		m_preparedStatements.put("UPDATE_CALL", m_con.prepareStatement("UPDATE calls SET cuuid=?, country=?, areacode=?, number=?, msn=?, cip=?, cdate=?, content=? WHERE uuid=?;"));

		m_preparedStatements.put("DELETE_ATTRIBUTE", m_con.prepareStatement("DELETE FROM attributes WHERE ref=?;"));
		m_preparedStatements.put("DELETE_CALL", m_con.prepareStatement("DELETE FROM calls WHERE uuid=?;"));

		m_preparedStatements.put("SELECT_CALLER_COUNT", m_con.prepareStatement("SELECT COUNT(cuuid) FROM calls WHERE cuuid=?;"));
		m_preparedStatements.put("SELECT_CALL_COUNT", m_con.prepareStatement("SELECT COUNT(uuid) FROM calls;"));

		m_preparedStatements.put("SELECT_CALL", m_con.prepareStatement("SELECT content FROM calls;"));
		
		this.m_logger.info("DatabaseHandler successfully connected.");
	}

	private void createCall(PreparedStatement ps, String uuid, String cuuid, String country, String areacode, String number, String msn, String cip, long date, byte[] call) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.setString(2, cuuid);
		ps.setString(3, country);
		ps.setString(4, areacode);
		ps.setString(5, number);
		ps.setString(6, msn);
		ps.setString(7, cip);
		ps.setLong(8, date);
		ps.setString(9, new String(call));
		ps.addBatch();
	}
	
	private void updateCall(PreparedStatement ps, String uuid, String cuuid, String country, String areacode, String number, String msn, String cip, long date, byte[] call) throws SQLException {
		ps.clearParameters();
		ps.setString(1, cuuid);
		ps.setString(2, country);
		ps.setString(3, areacode);
		ps.setString(4, number);
		ps.setString(5, msn);
		ps.setString(6, cip);
		ps.setLong(7, date);
		ps.setString(8, new String(call));
		ps.setString(9, uuid);
		ps.addBatch();
	}
	
	private boolean isCallerUsedElsewhere(String uuid) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		PreparedStatement ps = this.getStatement("SELECT_CALLER_COUNT");
		ps.setString(1, uuid);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) return rs.getInt(1)>1;
		return false;
	}
	
	private boolean containsListAll(int listsize) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		PreparedStatement ps = this.getStatement("SELECT_CALL_COUNT");
		ResultSet rs = ps.executeQuery();
		while (rs.next()) return rs.getInt(1)==listsize;
		return false;
	}
	
	/**
	 * Sets all calls in the submitted list to the database.
	 * 
	 * @param cl
	 * @throws SQLException
	 */
	public void setCallList(ICallList cl) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		this.internalDeleteCallList(cl);
		
		List uuid_check = new ArrayList(cl.size());
		
		PreparedStatement ps = this.getStatement("INSERT_CALL");
		PreparedStatement psa = this.getStatement("INSERT_ATTRIBUTE");
		ps.clearBatch();
		psa.clearBatch();
		
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
			try {
				this.createCall(ps, uuid, c.getCaller().getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), c.getMSN().getMSN(), c.getCIP().getCIP(), c.getDate().getTime(), Serializer.toByteArray(c));
				this.createAttributes(psa, uuid, c.getAttributes());
				this.createAttributes(psa, c.getCaller().getUUID(), c.getCaller().getAttributes());
			} catch (SerializerException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			
			if (i % this.commit_count == 0) {
				try {
					ps.executeBatch();	
					psa.executeBatch();			
					ps.clearBatch();
					psa.clearBatch();
					this.m_logger.info("-------------------> executed Batch");
				} catch (SQLException e) {	
					this.m_logger.log(Level.SEVERE, e.getMessage()+ c.toString(), e);
					//throw new SQLException("Batch execution failed: ");					
				}
			}
		}
		// execute the rest batch content
		ps.executeBatch();
		psa.executeBatch();
		
		uuid_check.clear();
		uuid_check=null;
	}
	
	/**
	 * Updates all calls in the submitted list. The select criterion of teh call is its UUID.
	 * @param cl
	 * @throws SQLException
	 */
	public void updateCallList(ICallList cl) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
			
		if (this.containsListAll(cl.size())) {
			this.createTables();
			this.setCallList(cl);
			return;
		}
		
		PreparedStatement ps = this.getStatement("UPDATE_CALL");
		PreparedStatement psa = this.getStatement("UPDATE_ATTRIBUTE");
		ps.clearBatch();
		psa.clearBatch();
		
		ICall c = null;
		IPhonenumber pn = null;
		for (int i=0, j=cl.size();i<j;i++) {
			c = cl.get(i);
			pn = c.getCaller().getPhoneNumber();
			try {
				this.updateCall(ps, c.getUUID(), c.getCaller().getUUID(), pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), c.getMSN().getMSN(), c.getCIP().getCIP(), c.getDate().getTime(), Serializer.toByteArray(c));
				this.updateAttributes(psa, c.getUUID(), c.getAttributes());
				this.updateAttributes(psa, c.getCaller().getUUID(), c.getCaller().getAttributes());
			} catch (SerializerException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			
			if (i % this.commit_count == 0) {
				ps.executeBatch();
				ps.clearBatch();
				this.m_logger.info("Executed prepared statement: "+ps.toString());
				psa.executeBatch();
				psa.clearBatch();
				this.m_logger.info("Executed prepared statement: "+psa.toString());				
			}
		}
		// execute the rest batch content
		ps.executeBatch();
		psa.executeBatch();
	}	
	
	private void internalDeleteCallList(ICallList cl) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
			
		PreparedStatement ps = this.getStatement("DELETE_CALL");
		PreparedStatement psa = this.getStatement("DELETE_ATTRIBUTE");
		ps.clearBatch();
		psa.clearBatch();
		
		ICall c = null;
		ICaller ca = null;
		for (int i=0, j=cl.size();i<j;i++) {
			c = cl.get(i);
			ca = c.getCaller();
			ps.setString(1, c.getUUID());
			ps.addBatch();
			psa.setString(1, c.getUUID());
			psa.addBatch();
			if (!this.isCallerUsedElsewhere(ca.getUUID())) {
				psa.setString(1, ca.getUUID());
				psa.addBatch();
			}

			if (i % this.commit_count == 0) {
				ps.executeBatch();
				ps.clearBatch();
				this.m_logger.info("Executed prepared statement: "+ps.toString());
				psa.executeBatch();
				psa.clearBatch();
				this.m_logger.info("Executed prepared statement: "+psa.toString());
			}
		}
		// execute the rest batch content
		ps.executeBatch();
		psa.executeBatch();
	}
	
	/**
	 * Deletes all calls in the submitted call list.
	 * 
	 * @param cl
	 * @throws SQLException
	 */
	public void deleteCallList(ICallList cl) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
			
		if (this.containsListAll(cl.size())) {
			this.createTables();
			return;
		}
		this.internalDeleteCallList(cl);
	}

	/**
	 * Fetches the calls by the applied filters from the database
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	public ICallList getCallList(IFilter[] filters) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		return this.buildCallList(filters); 	
	}
	
	/**
	 * Fetches the calls by the applied filters from the database
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	public ICallList getCallList(IFilter[] filters, int count, int offset) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		return this.buildCallList(filters, count, offset); 	
	}
	
	/**
	 * Fetches the calls by the applied filters from the database
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	public ICallList getCallList(IFilter[] filters, int count, int offset, ISearchTerm[] searchTerms) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		return this.buildCallList(filters, count, offset, searchTerms); 	
	}

	/**
	 * Counts the number of calls belonging to the filters object.
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	public int getCallCount(IFilter[] filters) throws SQLException {
		return this.getCallCount(filters, null);
	}
	
	public int getCallCount(IFilter[] filters, ISearchTerm[] searchTerms) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		return this.buildCallCount(filters, searchTerms); 	
	}
	
	/**
	 * Counts the calls with a proper database query.
	 * 
	 * @param filters filters applied to the result
	 * @return number of calls belonging to the filters
	 * @throws SQLException
	 */
	protected abstract int buildCallCount(IFilter[] filters) throws SQLException;
	
	/**
	 * Counts the calls with a proper database query.
	 * 
	 * @param filters filters applied to the result
	 * @return number of calls belonging to the filters
	 * @throws SQLException
	 */
	protected abstract int buildCallCount(IFilter[] filters, ISearchTerm[] searchTerms) throws SQLException;
	
	/**
	 * Create the call list from a query of the database. This abstract method must be
	 * implemented by all CallDatabaseHandler.
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	protected abstract ICallList buildCallList(IFilter[] filters) throws SQLException;
	
	/**
	 * Create the call list from a query of the database. This abstract method must be
	 * implemented by all CallDatabaseHandler.
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	protected abstract ICallList buildCallList(IFilter[] filters, int count, int offset) throws SQLException;
	
	/**
	 * Create the call list from a query of the database. This abstract method must be
	 * implemented by all CallDatabaseHandler.
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	protected abstract ICallList buildCallList(IFilter[] filters, int count, int offset, ISearchTerm[] searchTerms) throws SQLException;

}
