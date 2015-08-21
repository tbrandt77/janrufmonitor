package de.janrufmonitor.repository.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.CallerNotFoundException;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;
import de.janrufmonitor.util.uuid.UUID;

/**
 *  This abstract class can be used as base class for a database related caller managers.
 *  It contains methods for database creation and prepared statements for caller manager handling.
 *
 *@author     Thilo Brandt
 *@created    2006/05/29
 */
public abstract class AbstractCallerDatabaseHandler extends AbstractDatabaseHandler implements ICallerDatabaseHandler {

	public AbstractCallerDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
		super(driver, connection, user, password, initialize);
	}

	protected void createTables() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");

		Statement stmt = m_con.createStatement();
		stmt.execute("CREATE TABLE attributes (ref VARCHAR(36), name VARCHAR(64), value VARCHAR(2048));");
		stmt.execute("CREATE TABLE callers (uuid VARCHAR(36) PRIMARY KEY, country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64), phone VARCHAR(128), content VARCHAR("+Short.MAX_VALUE+"));");
	}

	protected void addPreparedStatements() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");

		// prepare statements
		m_preparedStatements.put("INSERT_ATTRIBUTE", m_con.prepareStatement("INSERT INTO attributes (ref, name, value) VALUES (?,?,?);"));
		m_preparedStatements.put("INSERT_CALLER", m_con.prepareStatement("INSERT INTO callers (uuid, country, areacode, number, phone, content) VALUES (?,?,?,?,?,?);"));

		m_preparedStatements.put("UPDATE_ATTRIBUTE", m_con.prepareStatement("UPDATE attributes SET value=? WHERE ref=? AND name=?;"));
		m_preparedStatements.put("UPDATE_CALLER", m_con.prepareStatement("UPDATE callers SET country=?, areacode=?, number=?, phone=?, content=? WHERE uuid=?;"));
		m_preparedStatements.put("UPDATE_CALLER_PHONE", m_con.prepareStatement("UPDATE callers SET uuid=?, content=? WHERE country=? AND areacode=? AND number=? AND phone=?;"));

		
		m_preparedStatements.put("DELETE_ATTRIBUTE", m_con.prepareStatement("DELETE FROM attributes WHERE ref=?;"));
		m_preparedStatements.put("DELETE_CALLER", m_con.prepareStatement("DELETE FROM callers WHERE uuid=?;"));

		m_preparedStatements.put("SELECT_CALLER_COUNT", m_con.prepareStatement("SELECT COUNT(uuid) FROM callers;"));
		m_preparedStatements.put("SELECT_CALLER_UUID_COUNT", m_con.prepareStatement("SELECT COUNT(uuid) FROM callers WHERE uuid=?;"));
		m_preparedStatements.put("SELECT_CALLER_PHONE_COUNT", m_con.prepareStatement("SELECT COUNT(uuid) FROM callers WHERE country=? AND areacode=? AND number=?;"));

		m_preparedStatements.put("SELECT_CALLER", m_con.prepareStatement("SELECT content FROM callers;"));
		m_preparedStatements.put("SELECT_CALLER_PHONE", m_con.prepareStatement("SELECT content FROM callers WHERE phone like ?;"));

		this.m_logger.info("DatabaseHandler successfully connected.");
	}
	

	private void createCaller(PreparedStatement ps, String uuid, String country, String areacode, String number, String phone, byte[] caller) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.setString(2, country);
		ps.setString(3, areacode);
		ps.setString(4, number);
		ps.setString(5, phone);
		ps.setString(6, new String(caller));
		ps.addBatch();
	}
	
	private void updateCaller(PreparedStatement ps, String uuid, String country, String areacode, String number, String phone, byte[] caller) throws SQLException {
		ps.clearParameters(); 
		ps.setString(1, country);
		ps.setString(2, areacode);
		ps.setString(3, number);
		ps.setString(4, phone);
		ps.setString(5, new String(caller));
		ps.setString(6, uuid);
		ps.addBatch();
	}
	
	private void updateCallerPhone(PreparedStatement ps, String uuid, String country, String areacode, String number, String phone, byte[] caller) throws SQLException {
		ps.clearParameters(); 
		ps.setString(1, uuid);
		ps.setString(2, new String(caller));
		ps.setString(3, country);
		ps.setString(4, areacode);
		ps.setString(5, number);
		ps.setString(6, phone);
		ps.addBatch();
	}
	
	private boolean existsCaller(String uuid) throws SQLException {
		PreparedStatement ps = this.getStatement("SELECT_CALLER_UUID_COUNT");
		ps.setString(1, uuid);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) return rs.getInt(1)>0;
		return false;
	}

	private boolean containsListAll(int listsize) throws SQLException {
		PreparedStatement ps = this.getStatement("SELECT_CALLER_COUNT");
		ResultSet rs = ps.executeQuery();
		while (rs.next()) return rs.getInt(1)==listsize;
		return false;
	}
	
	private boolean existsCaller(IPhonenumber pn) throws SQLException {
		if (pn==null) return false;
		
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		PreparedStatement ps = this.getStatement("SELECT_CALLER_PHONE_COUNT");
		ps.setString(1, pn.getIntAreaCode());
		ps.setString(2, pn.getAreaCode());
		ps.setString(3, pn.getCallNumber());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) return rs.getInt(1)>0;
		return false;
	}
	
	private int maxInternalNumberLength() {
		String value = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTERNAL_LENGTH);
		if (value!=null && value.length()>0) {
			try {
				return Integer.parseInt(value);
			} catch (Exception ex) {
				this.m_logger.warning(ex.getMessage());
			}
		}
		return 0;
	}
	
	private String getPrefix(){
		String prefix = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA_PREFIX);
		return (prefix==null ? "0" : prefix);
	}  

	private boolean isInternalNumber(IPhonenumber pn) {
		if (pn==null)
			return false;
					
		String number = pn.getTelephoneNumber();
		
		if (number.trim().length()==0) {
			number = pn.getCallNumber();
		}

		if (number.length()<=maxInternalNumberLength()) {
			return true;
		}
		return false;
	}
	
	private String getDefaultIntAreaCode() {
		return this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
	}
	
	/**
	 * Insert the callers in the list or update the callers if it already exists.
	 * 
	 * @param cl
	 * @throws SQLException
	 */
	public void insertOrUpdateCallerList(ICallerList cl) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
			
		PreparedStatement insert_caller = this.getStatement("INSERT_CALLER");
		PreparedStatement insert_attributes = this.getStatement("INSERT_ATTRIBUTE");
		PreparedStatement update_caller = this.getStatement("UPDATE_CALLER");
		PreparedStatement update_caller_phone = this.getStatement("UPDATE_CALLER_PHONE");
		PreparedStatement update_attributes = this.getStatement("UPDATE_ATTRIBUTE");
		insert_caller.clearBatch();
		insert_attributes.clearBatch();
		update_caller.clearBatch();
		update_caller_phone.clearBatch();
		update_attributes.clearBatch();
		
		List uuid_check = new ArrayList(cl.size());
		
		ICaller c = null;
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
			
			pn = c.getPhoneNumber();
			if (this.existsCaller(c)) {
				// do an update
				try {
					this.updateCaller(update_caller, c.getUUID(),  pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), pn.getTelephoneNumber(), Serializer.toByteArray(c));
					this.updateAttributes(update_attributes, c.getUUID(), c.getAttributes());
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}				
			} else if (this.existsCaller(pn)) {
				try {
					this.updateCallerPhone(update_caller_phone, c.getUUID(),  pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), pn.getTelephoneNumber(), Serializer.toByteArray(c));
					this.createAttributes(insert_attributes, c.getUUID(), c.getAttributes());
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}	
			} else {
				// do an insert
				try {
					this.createCaller(insert_caller, c.getUUID(),  pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), pn.getTelephoneNumber(), Serializer.toByteArray(c));
					this.createAttributes(insert_attributes, c.getUUID(), c.getAttributes());
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}

			if (i % this.commit_count == 0) {
				try {
					insert_caller.executeBatch();
					insert_caller.clearBatch();
					insert_attributes.executeBatch();
					insert_attributes.clearBatch();
					update_caller.executeBatch();
					update_caller.clearBatch();
					update_caller_phone.executeBatch();
					update_caller_phone.clearBatch();
					update_attributes.executeBatch();
					update_attributes.clearBatch();
					this.m_logger.info("-------------------> executed Batch");
				} catch (SQLException e) {	
					this.m_logger.log(Level.SEVERE, e.getMessage()+ c.toString(), e);
					//throw new SQLException("Batch execution failed: ");					
				}
			}
		}
		// execute the rest batch content
		insert_caller.executeBatch();
		insert_attributes.executeBatch();
		update_caller.executeBatch();
		update_caller_phone.executeBatch();
		update_attributes.executeBatch();
	}
	
	/**
	 * Deletes all callers of the submitted caller list.
	 * 
	 * @param cl a list with callers, must not be null.
	 * @throws SQLException
	 */
	public void deleteCallerList(ICallerList cl) throws SQLException {
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
		
		PreparedStatement ps = this.getStatement("DELETE_CALLER");
		PreparedStatement psa = this.getStatement("DELETE_ATTRIBUTE");
		ps.clearBatch();
		psa.clearBatch();
		
		ICaller c = null;
		for (int i=0, j=cl.size();i<j;i++) {
			c = cl.get(i);
			ps.setString(1, c.getUUID());
			ps.addBatch();
			psa.setString(1, c.getUUID());
			psa.addBatch();
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
	 * Checks if the caller with the provided UUID exists
	 * 
	 * @param c a valid caller object
	 * @return
	 * @throws SQLException
	 */
	public boolean existsCaller(ICaller c) throws SQLException {
		if (c==null) return false;
		return this.existsCaller(c.getUUID());
	}

	public ICaller getCaller(IPhonenumber pn) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
			
		PreparedStatement ps = this.getStatement("SELECT_CALLER_PHONE");
		String p = pn.getTelephoneNumber();
		ResultSet rs = null;
		
		// check for internal telephone system numbers
		if (this.isInternalNumber(pn)) {
			ps.setString(1, p);
			rs = ps.executeQuery();
			while (rs.next()) {
				this.m_logger.info("Found exact match of call number: "+p);
				try {
					return Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime());				
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} 
			}
		}

		int maxLength = this.maxInternalNumberLength();		
		// p must be an internal number but has no entry in this caller manager
		if (p.length()<maxLength) return null;
		

		// check for international call
		if (p.startsWith(this.getPrefix())) {
			this.m_logger.info("Found international call number: "+p);
			ICaller internationaCaller = null; 
			ICallerManager cmg = this.getRuntime().getCallerManagerFactory().getDefaultCallerManager();
			if (cmg!=null && cmg.isActive() && cmg.isSupported(IIdentifyCallerRepository.class)) {
				try {
					internationaCaller = ((IIdentifyCallerRepository)cmg).getCaller(pn);
				} catch (CallerNotFoundException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (internationaCaller!=null)
				pn = internationaCaller.getPhoneNumber();
			
			ps.setString(1, pn.getTelephoneNumber());			
			rs = ps.executeQuery();
			while (rs.next()) {
				try {
					ICaller c = Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime());
					if (pn.getTelephoneNumber().equalsIgnoreCase(c.getPhoneNumber().getTelephoneNumber()) && pn.getIntAreaCode().equalsIgnoreCase(c.getPhoneNumber().getIntAreaCode())) {
						// found international number
						return c;
					}
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} 
			}
		}
		
		// check for extension
		if (pn.getIntAreaCode()==null || pn.getIntAreaCode().length()==0){
			pn.setIntAreaCode(this.getDefaultIntAreaCode());
		}
		
		for (int i=0;i<p.length()-maxLength;i++) {			
			ps.setString(1, p.substring(0, p.length()-i)+ "%");			
			rs = ps.executeQuery();
			while (rs.next()) {
				try {
					ICaller c = Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime());
					if (p.startsWith(c.getPhoneNumber().getTelephoneNumber()) && pn.getIntAreaCode().equalsIgnoreCase(c.getPhoneNumber().getIntAreaCode())) {
						// found extension phone						
						String extension = p.substring(c.getPhoneNumber().getTelephoneNumber().length(), p.length());
						this.m_logger.info("Found call extension -"+extension+" for call number: "+p);
						
						c.setUUID(new UUID().toString());
						c.getPhoneNumber().setTelephoneNumber(p);
						c.getPhoneNumber().setCallNumber(c.getPhoneNumber().getCallNumber()+extension);
						// add attributes
						IAttribute att = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_EXTENSION, extension);
						c.setAttribute(att);
						att = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION, p.substring(0, p.length() - extension.length()));
						c.setAttribute(att);
						return c;	
					} 
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} 
			}
		}
		return null;
	}

	/**
	 * Selects the callers by the applied filters from the database
	 * 
	 * @param filters filetrs applied to the result
	 * @return
	 * @throws SQLException
	 */
	public ICallerList getCallerList(IFilter[] filters) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		return this.buildCallerList(filters);			
	}
	
	
	public ICallerList getCallerList(IFilter[] filters,
			ISearchTerm[] searchTerms) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		return this.buildCallerList(filters, searchTerms);		
	}


	
	/**
	 * Create the caller list from a query of the database. This abstract method must be
	 * implemented by all CallerDatabaseHandler.
	 * 
	 * @param filters filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	protected abstract ICallerList buildCallerList(IFilter[] filters) throws SQLException;

	/**
	 * Create the caller list from a query of the database. This abstract method must be
	 * implemented by all CallerDatabaseHandler.
	 * 
	 * @param filters filters applied to the result
	 * @param searchTerms search terms considered on filtering
	 * @return
	 * @throws SQLException
	 */
	protected abstract ICallerList buildCallerList(IFilter[] filters, ISearchTerm[] searchTerms) throws SQLException;
}
