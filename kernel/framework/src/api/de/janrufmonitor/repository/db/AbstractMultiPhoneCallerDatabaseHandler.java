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
import de.janrufmonitor.framework.IMultiPhoneCaller;
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
 * This abstract class can be used as base class for a database related caller
 * managers. It contains methods for database creation and prepared statements
 * for caller manager handling.
 * 
 * @author Thilo Brandt
 * @created 2007/11/20
 */
public abstract class AbstractMultiPhoneCallerDatabaseHandler extends
		AbstractDatabaseHandler implements ICallerDatabaseHandler {

	public AbstractMultiPhoneCallerDatabaseHandler(String driver,
			String connection, String user, String password, boolean initialize) {
		super(driver, connection, user, password, initialize);
	}

	protected void createTables() throws SQLException {
		if (!isConnected())
			throw new SQLException("Database is disconnected.");

		Statement stmt = m_con.createStatement();
		stmt.execute("CREATE TABLE attributes (ref VARCHAR(36), name VARCHAR(64), value VARCHAR(2048));");
		stmt.execute("CREATE TABLE callers (uuid VARCHAR(36) PRIMARY KEY, content VARCHAR("+ Short.MAX_VALUE + "));");
		stmt.execute("CREATE TABLE phones (ref VARCHAR(36), country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64), phone VARCHAR(128));");

	}

	protected void addPreparedStatements() throws SQLException {
		if (!isConnected())
			throw new SQLException("Database is disconnected.");

		// prepare statements
		m_preparedStatements.put("INSERT_ATTRIBUTE", m_con.prepareStatement("INSERT INTO attributes (ref, name, value) VALUES (?,?,?);"));
		m_preparedStatements.put("UPDATE_ATTRIBUTE", m_con.prepareStatement("UPDATE attributes SET value=? WHERE ref=? AND name=?;"));
		m_preparedStatements.put("DELETE_ATTRIBUTE", m_con.prepareStatement("DELETE FROM attributes WHERE ref=?;"));
		
		m_preparedStatements.put("INSERT_CALLER", m_con.prepareStatement("INSERT INTO callers (uuid, content) VALUES (?,?);"));
		m_preparedStatements.put("UPDATE_CALLER", m_con.prepareStatement("UPDATE callers SET content=? WHERE uuid=?;"));
		m_preparedStatements.put("DELETE_CALLER", m_con.prepareStatement("DELETE FROM callers WHERE uuid=?;"));

		m_preparedStatements.put("INSERT_PHONE", m_con.prepareStatement("INSERT INTO phones (ref, country, areacode, number, phone) VALUES (?,?,?,?,?);"));
		m_preparedStatements.put("UPDATE_PHONE", m_con.prepareStatement("UPDATE phones SET country=?, areacode=?, number=?, phone=? WHERE ref=?;"));
		m_preparedStatements.put("DELETE_PHONE", m_con.prepareStatement("DELETE FROM phones WHERE ref=?;"));
		m_preparedStatements.put("DELETE_PHONE2", m_con.prepareStatement("DELETE FROM phones WHERE country=? AND areacode=? AND number=? AND phone=?;"));
		
		m_preparedStatements.put("SELECT_CALLER_UUID_COUNT", m_con.prepareStatement("SELECT COUNT(uuid) FROM callers WHERE uuid=?;"));
		m_preparedStatements.put("SELECT_CALLER_PHONE", m_con.prepareStatement("SELECT content FROM callers WHERE uuid = (SELECT ref FROM phones WHERE phone like ? LIMIT 1);"));
		m_preparedStatements.put("SELECT_PHONE_COUNT", m_con.prepareStatement("SELECT COUNT(ref) FROM phones WHERE country=? AND areacode=? AND number=?;"));
		m_preparedStatements.put("SELECT_PHONE_REF_COUNT", m_con.prepareStatement("SELECT COUNT(ref) FROM phones WHERE phone like ?;"));
		m_preparedStatements.put("SELECT_PHONE_REF", m_con.prepareStatement("SELECT ref FROM phones WHERE phone like ?;"));
		m_preparedStatements.put("SELECT_CALLER_PHONE2", m_con.prepareStatement("SELECT content FROM callers WHERE uuid = ?;"));
		
		this.m_logger.info("DatabaseHandler successfully connected.");
	}
	
	private void createCaller(PreparedStatement ps, String uuid, byte[] caller) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.setString(2, new String(caller));
		ps.addBatch();
	}
	
	private void updateCaller(PreparedStatement ps, String uuid, byte[] content) throws SQLException {
		ps.clearParameters();
		ps.setString(1, new String(content));
		ps.setString(2, uuid);
		ps.addBatch();
	}
	
	private void deletePhone(PreparedStatement ps, String uuid) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.addBatch();
	}
	
	private void deletePhone(PreparedStatement ps, String country, String areacode, String number, String phone) throws SQLException {
		ps.clearParameters();
		ps.setString(1, country);
		ps.setString(2, areacode);
		ps.setString(3, number);
		ps.setString(4, phone);
		ps.addBatch();
	}
	
	private void createPhone(PreparedStatement ps, String uuid, String country, String areacode, String number, String phone) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.setString(2, country);
		ps.setString(3, areacode);
		ps.setString(4, number);
		ps.setString(5, phone);
		ps.addBatch();
	}

	private int maxInternalNumberLength() {
		String value = this.getRuntime().getConfigManagerFactory()
				.getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTERNAL_LENGTH);
		if (value != null && value.length() > 0) {
			try {
				return Integer.parseInt(value);
			} catch (Exception ex) {
				this.m_logger.warning(ex.getMessage());
			}
		}
		return 0;
	}

	private String getPrefix() {
		String prefix = this.getRuntime().getConfigManagerFactory()
				.getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTAREA_PREFIX);
		return (prefix == null ? "0" : prefix);
	}

	private boolean isInternalNumber(IPhonenumber pn) {
		if (pn == null)
			return false;

		String number = pn.getTelephoneNumber();

		if (number.trim().length() == 0) {
			number = pn.getCallNumber();
		}

		if (number.length() <= maxInternalNumberLength() || pn.getIntAreaCode().equalsIgnoreCase(IJAMConst.INTERNAL_CALL)) {
			return true;
		}
		return false;
	}

	private String getDefaultIntAreaCode() {
		return this.getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_INTAREA);
	}

	/**
	 * Insert the callers in the list or update the callers if it already
	 * exists.
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
		
		// check prepared statements
		PreparedStatement insert_caller = this.getStatement("INSERT_CALLER");
		PreparedStatement insert_attributes = this.getStatement("INSERT_ATTRIBUTE");
		PreparedStatement insert_phones = this.getStatement("INSERT_PHONE");
		
		PreparedStatement update_caller = this.getStatement("UPDATE_CALLER");

		PreparedStatement delete_phones = this.getStatement("DELETE_PHONE");
		PreparedStatement delete_phones2 = this.getStatement("DELETE_PHONE2");
		PreparedStatement delete_attributes = this.getStatement("DELETE_ATTRIBUTE");

		// clean up prpared statements
		insert_caller.clearBatch();
		insert_attributes.clearBatch();
		insert_phones.clearBatch();
		
		update_caller.clearBatch();
		
		delete_attributes.clearBatch();
		delete_phones.clearBatch();
		delete_phones2.clearBatch();
		
		// list for redundant UUIDs checks
		List uuid_check = new ArrayList(cl.size());
		
		ICaller c = null;
		String uuid = null;
		List phones = new ArrayList(1);
		for (int i=0, j = cl.size(); i<j; i++) {
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
			
			// check which type of Interface the caller object implements
			if (!(c instanceof IMultiPhoneCaller)) {
				if (this.m_logger.isLoggable(Level.INFO) && c!=null)
					this.m_logger.info("Caller is not type IMultiPhoneCaller. Transforming is triggered... : "+c.toString());
				c = this.getRuntime().getCallerFactory().toMultiPhoneCaller(c);	
				// 2008/11/30: added to fix duplicated address book entries after category assignement
				c.setUUID(uuid);
			}
			phones.clear();
			phones.addAll(((IMultiPhoneCaller)c).getPhonenumbers());
			
			// check if single caller (uuid) is already in DB
			if (existsCaller(c)) {
				internalUpdate(c);
				if (this.m_logger.isLoggable(Level.INFO) && c!=null)
					this.m_logger.info("Caller already exists in database. Update is triggered: "+c.toString());
				try {
					// update caller table
					this.updateCaller(update_caller, c.getUUID(), Serializer.toByteArray(c));
					// update attributes table				
					this.deleteAttributes(delete_attributes, c.getUUID());
					this.createAttributes(insert_attributes, c.getUUID(), c.getAttributes());
					// update phones table
					this.deletePhone(delete_phones, c.getUUID());
					IPhonenumber p = null;
					for (int a=0,b=phones.size();a<b;a++) {
						p = (IPhonenumber) phones.get(a);
						this.createPhone(insert_phones, c.getUUID(), p.getIntAreaCode(), p.getAreaCode(), p.getCallNumber(), p.getTelephoneNumber());
					}
					
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			} else if (this.existsPhones(phones).size()>0) {
				internalInsert(c);
				// check if phone numnbers are already in DB and overwrite them
				List existingPhones = this.existsPhones(phones);
				IPhonenumber pn = null;
				for (int a=0,b=existingPhones.size();a<b;a++) {
					pn = (IPhonenumber) existingPhones.get(a);
					if (this.m_logger.isLoggable(Level.INFO) && c!=null)
						this.m_logger.info("Phone already exists in database. Update is triggered: "+pn.toString());

					this.deletePhone(delete_phones2, pn.getIntAreaCode(), pn.getAreaCode(), pn.getCallNumber(), pn.getTelephoneNumber());
				}
				try {
					this.createCaller(insert_caller, c.getUUID(), Serializer.toByteArray(c));
					this.createAttributes(insert_attributes, c.getUUID(), c.getAttributes());
					IPhonenumber p = null;
					for (int a=0,b=phones.size();a<b;a++) {
						p = (IPhonenumber) phones.get(a);
						this.createPhone(insert_phones, c.getUUID(), p.getIntAreaCode(), p.getAreaCode(), p.getCallNumber(), p.getTelephoneNumber());
					}
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}	
			} else {
				internalInsert(c);
				// insert new caller 
				try {
					this.createCaller(insert_caller, c.getUUID(), Serializer.toByteArray(c));
					this.createAttributes(insert_attributes, c.getUUID(), c.getAttributes());
					IPhonenumber p = null;
					for (int a=0,b=phones.size();a<b;a++) {
						p = (IPhonenumber) phones.get(a);
						this.createPhone(insert_phones, c.getUUID(), p.getIntAreaCode(), p.getAreaCode(), p.getCallNumber(), p.getTelephoneNumber());
					}
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}				
			}

			if (i % this.commit_count == 0) {
				try {
					delete_attributes.executeBatch();
					delete_attributes.clearBatch();
					this.m_logger.info("Batch DELETE_ATTRIBUTES executed...");
					delete_phones.executeBatch();
					delete_phones.clearBatch();
					this.m_logger.info("Batch DELETE_PHONES executed...");
					delete_phones2.executeBatch();
					delete_phones2.clearBatch();
					this.m_logger.info("Batch DELETE_PHONES2 executed...");

					insert_caller.executeBatch();
					insert_caller.clearBatch();
					this.m_logger.info("Batch INSERT_CALLER executed...");
					insert_attributes.executeBatch();
					insert_attributes.clearBatch();
					this.m_logger.info("Batch INSERT_ATTRIBUTES executed...");
					insert_phones.executeBatch();
					insert_phones.clearBatch();
					this.m_logger.info("Batch INSERT_PHONES executed...");
					
					update_caller.executeBatch();
					update_caller.clearBatch();
					this.m_logger.info("Batch UPDATE_CALLER executed...");
					
				} catch (SQLException e) {	
					this.m_logger.log(Level.SEVERE, e.getMessage()+ c.toString(), e);
					//throw new SQLException("Batch execution failed: ");					
				}
			}
		}
		
		// execute the rest batch content
		delete_attributes.executeBatch();
		delete_attributes.clearBatch();
		this.m_logger.info("Batch DELETE_ATTRIBUTES executed...");
		delete_phones.executeBatch();
		delete_phones.clearBatch();
		this.m_logger.info("Batch DELETE_PHONES executed...");
		delete_phones2.executeBatch();
		delete_phones2.clearBatch();
		this.m_logger.info("Batch DELETE_PHONES2 executed...");

		insert_caller.executeBatch();
		insert_caller.clearBatch();
		this.m_logger.info("Batch INSERT_CALLER executed...");
		insert_attributes.executeBatch();
		insert_attributes.clearBatch();
		this.m_logger.info("Batch INSERT_ATTRIBUTES executed...");
		insert_phones.executeBatch();
		insert_phones.clearBatch();
		this.m_logger.info("Batch INSERT_PHONES executed...");
		
		update_caller.executeBatch();
		update_caller.clearBatch();
		this.m_logger.info("Batch UPDATE_CALLER executed...");
	}

	/**
	 * Called before updating the caller. To be overwritten from sub-classes.
	 * 
	 * @param c a caller object
	 */
	protected void internalUpdate(ICaller c) throws SQLException {
	}

	/**
	 * Called before inserting the caller. To be overwritten from sub-classes.
	 * 
	 * @param c a caller object
	 */
	protected void internalInsert(ICaller c) throws SQLException {
	}

	/**
	 * Called before deleting the caller. To be overwritten from sub-classes.
	 * 
	 * @param c a caller object
	 */
	protected void internalDelete(ICaller c) throws SQLException {

	}

	/**
	 * Deletes all callers of the submitted caller list.
	 * 
	 * @param cl
	 *            a list with callers, must not be null.
	 * @throws SQLException
	 */
	public void deleteCallerList(ICallerList cl) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}

		PreparedStatement ps = this.getStatement("DELETE_CALLER");
		PreparedStatement psa = this.getStatement("DELETE_ATTRIBUTE");
		PreparedStatement psp = this.getStatement("DELETE_PHONE");
		ps.clearBatch();
		psa.clearBatch();
		psp.clearBatch();
		
		ICaller c = null;
		for (int i=0, j=cl.size();i<j;i++) {
			c = cl.get(i);
			internalDelete(c);
			ps.setString(1, c.getUUID());
			ps.addBatch();
			psa.setString(1, c.getUUID());
			psa.addBatch();
			psp.setString(1, c.getUUID());
			psp.addBatch();				
			if (i % this.commit_count == 0) {
				ps.executeBatch();
				ps.clearBatch();
				this.m_logger.info("Executed prepared statement: "+ps.toString());
				psa.executeBatch();
				psa.clearBatch();
				this.m_logger.info("Executed prepared statement: "+psa.toString());
				psp.executeBatch();
				psp.clearBatch();
				this.m_logger.info("Executed prepared statement: "+psp.toString());
			}
		}
		// execute the rest batch content
		ps.executeBatch();
		psa.executeBatch();
		psp.executeBatch();
		
		ps.clearBatch();
		psa.clearBatch();
		psp.clearBatch();
	}

	/**
	 * Checks if the caller with the provided UUID exists
	 * 
	 * @param c
	 *            a valid caller object
	 * @return
	 * @throws SQLException
	 */
	public boolean existsCaller(ICaller c) throws SQLException {
		if (c==null) return false;
		PreparedStatement ps = this.getStatement("SELECT_CALLER_UUID_COUNT");
		ps.setString(1, c.getUUID());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) return rs.getInt(1)>0;
		return false;
	}
	
	private List existsPhones(List phones) throws SQLException {
		if (phones==null || phones.size()==0) return new ArrayList(1);
		
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
		
		List existancePhones = new ArrayList(phones.size());
		
		IPhonenumber pn = null;
		for (int i=0, j=phones.size();i<j;i++) {
			pn = (IPhonenumber) phones.get(i);
			PreparedStatement ps = this.getStatement("SELECT_PHONE_COUNT");
			ps.setString(1, pn.getIntAreaCode());
			ps.setString(2, pn.getAreaCode());
			ps.setString(3, pn.getCallNumber());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getInt(1)>0) {
					existancePhones.add(pn);
				}
			}
		}
		
		return existancePhones;
	}


	@SuppressWarnings("resource")
	public ICaller getCaller(IPhonenumber pn) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}
			
		PreparedStatement ps = this.getStatement("SELECT_CALLER_PHONE");
		String p = pn.getTelephoneNumber();
		ICaller c = null;
		ResultSet rs = null;
		
		// check for internal telephone system numbers
		if (this.isInternalNumber(pn)) {
			ps.setString(1, p);
			rs = ps.executeQuery();
			while (rs.next()) {
				this.m_logger.info("Found exact match of call number: "+p);
				try {
					c = Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime());
					if (c instanceof IMultiPhoneCaller) {
						IPhonenumber cp = null;
						for (int i=0, j=((IMultiPhoneCaller)c).getPhonenumbers().size();i<j;i++) {
							cp = (IPhonenumber) ((IMultiPhoneCaller)c).getPhonenumbers().get(i);
							if (cp.getTelephoneNumber().startsWith(p)) {
								this.m_logger.info("Found correct phonenumber match: "+p+" = "+cp.getTelephoneNumber());
								((IMultiPhoneCaller)c).getPhonenumbers().clear();
								c.setPhoneNumber(cp);
								return c;
							}
						}
					}	
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
					c = Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime());
					if (c instanceof IMultiPhoneCaller) {
						IPhonenumber cp = null;
						for (int i=0, j=((IMultiPhoneCaller)c).getPhonenumbers().size();i<j;i++) {
							cp = (IPhonenumber) ((IMultiPhoneCaller)c).getPhonenumbers().get(i);
							if (pn.getTelephoneNumber().equalsIgnoreCase(cp.getTelephoneNumber()) && pn.getIntAreaCode().equalsIgnoreCase(cp.getIntAreaCode())) {
								this.m_logger.info("Found correct phonenumber match: "+p+" = "+cp.getTelephoneNumber());
								((IMultiPhoneCaller)c).getPhonenumbers().clear();
								c.setPhoneNumber(cp);
								// found international number
								return c;
							}
						}
					} else if (c instanceof ICaller) {
						if (pn.getTelephoneNumber().equalsIgnoreCase(c.getPhoneNumber().getTelephoneNumber()) && pn.getIntAreaCode().equalsIgnoreCase(c.getPhoneNumber().getIntAreaCode())) {
							// found international number
							return c;
						}
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
		
		PreparedStatement ps2 = this.getStatement("SELECT_PHONE_REF_COUNT");
		if (ps2 == null) return null;
		boolean multiprocess = false;
		
		for (int i=0;i<p.length()-maxLength;i++) {	
			ps2.setString(1, p.substring(0, p.length()-i)+ "%");	
			rs = ps2.executeQuery();
			while (rs.next()) {
				multiprocess = rs.getInt(1)>1;
			}
			
			if (!multiprocess) {
				ps.setString(1, p.substring(0, p.length()-i)+ "%");			
				rs = ps.executeQuery();
				while (rs.next()) {
					try {
						c = Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime());
						ICaller nc = this.process(c, pn, p);
						if (nc!=null) return nc;
					} catch (SerializerException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} 
				}
			} else {
				// added 2008/04/18: processing of multiple callers is possible
				PreparedStatement ps3 = this.getStatement("SELECT_PHONE_REF");
				ps3.setString(1, p.substring(0, p.length()-i)+ "%");			
				rs = ps3.executeQuery();
				List uuids = new ArrayList(2);
				while (rs.next()) {
					uuids.add(rs.getString(1));
				}
				// process all UUIDs
				String uuid = null;
				ps3 = this.getStatement("SELECT_CALLER_PHONE2");				
				for (int j=0;j<uuids.size();j++) {
					uuid = (String) uuids.get(j);
					ps3.setString(1, uuid);
					rs = ps3.executeQuery();
					while (rs.next()) {
						try {
							c = Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime());
							ICaller nc = this.process(c, pn, p);
							if (nc!=null) return nc;
						} catch (SerializerException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						} 
					}
				}
			
			}

		}
		return null;
	}
	
	private ICaller process(ICaller c, IPhonenumber pn, String p) {
		if (c instanceof IMultiPhoneCaller) {
			this.m_logger.info("Found multi phone caller.");
			IPhonenumber cp = null;
			for (int x=0, j=((IMultiPhoneCaller)c).getPhonenumbers().size();x<j;x++) {
				cp = (IPhonenumber) ((IMultiPhoneCaller)c).getPhonenumbers().get(x);
				if (p.startsWith(cp.getTelephoneNumber()) && pn.getIntAreaCode().equalsIgnoreCase(cp.getIntAreaCode())) {
					// found extension phone						
					String extension = p.substring(cp.getTelephoneNumber().length(), p.length());
					this.m_logger.info("Found call extension -"+extension+" for call number: "+p);
					
					c.setUUID(new UUID().toString());
					((IMultiPhoneCaller)c).getPhonenumbers().clear();
					cp.setCallNumber(cp.getCallNumber() + extension);
					c.setPhoneNumber(cp);
					
					// add attributes
					IAttribute att = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_EXTENSION, extension);
					c.setAttribute(att);
					att = getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION, p.substring(0, p.length() - extension.length()));
					c.setAttribute(att);
					return c;	
				} 
			}
		} else if (c instanceof ICaller) {
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
		}
		return null;
	}


	/**
	 * Selects the callers by the applied filters from the database
	 * 
	 * @param filters
	 *            filetrs applied to the result
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
	

	public ICallerList getCallerList(IFilter[] filters, ISearchTerm[] searchTerms) throws SQLException {
		if (!isConnected())
			try {
				this.connect();
			} catch (ClassNotFoundException e) {
				throw new SQLException(e.getMessage());
			}

		return this.buildCallerList(filters, searchTerms);
	}

	/**
	 * Create the caller list from a query of the database. This abstract method
	 * must be implemented by all CallerDatabaseHandler.
	 * 
	 * @param filters
	 *            filters applied to the result
	 * @return
	 * @throws SQLException
	 */
	protected abstract ICallerList buildCallerList(IFilter[] filters)
			throws SQLException;
	

	/**
	 * Create the caller list from a query of the database. This abstract method
	 * must be implemented by all CallerDatabaseHandler.
	 * 
	 * @param filters
	 *            filters applied to the result
	 * @param searchTerms search terms to be considered during filtering
	 * @return
	 * @throws SQLException
	 */
	protected abstract ICallerList buildCallerList(IFilter[] filters, ISearchTerm[] searchTerms)
			throws SQLException;

}
