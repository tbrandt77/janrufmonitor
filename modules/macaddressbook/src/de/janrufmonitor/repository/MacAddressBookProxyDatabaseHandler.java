package de.janrufmonitor.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.repository.db.AbstractDatabaseHandler;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class MacAddressBookProxyDatabaseHandler extends AbstractDatabaseHandler {

	private IRuntime m_runtime;

	
	public MacAddressBookProxyDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
		super(driver, connection, user, password, initialize);
	}

	@SuppressWarnings("unchecked")
	protected void addPreparedStatements() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");

		// prepare statements
		m_preparedStatements.put("INSERT", m_con.prepareStatement("INSERT INTO mapping (uuid, country, areacode, number) VALUES (?,?,?,?);"));
		m_preparedStatements.put("INSERT_ATTRIBUTES", m_con.prepareStatement("INSERT INTO attributes (uuid, attribute, value) VALUES (?,?,?);"));

		
		m_preparedStatements.put("DELETE", m_con.prepareStatement("DELETE FROM mapping WHERE uuid=?;"));
		m_preparedStatements.put("DELETE_ATTRIBUTES", m_con.prepareStatement("DELETE FROM attributes WHERE uuid=?;"));

		m_preparedStatements.put("SELECT", m_con.prepareStatement("SELECT DISTINCT(uuid) FROM mapping WHERE country=? AND areacode=? AND number=?;"));
		m_preparedStatements.put("SELECT2", m_con.prepareStatement("SELECT DISTINCT(uuid) FROM mapping WHERE country=? AND areacode=?;"));
		m_preparedStatements.put("SELECT_ATT_VALUE", m_con.prepareStatement("SELECT value FROM attributes WHERE uuid=? AND attribute=?;"));
		m_preparedStatements.put("SELECT3", m_con.prepareStatement("SELECT DISTINCT(uuid) FROM attributes WHERE attribute=? AND value like ?;"));

	}

	protected void createTables() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");

		Statement stmt = m_con.createStatement();
		stmt.execute("CREATE TABLE mapping (uuid VARCHAR(36), country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64));");
		stmt.execute("CREATE TABLE attributes (uuid VARCHAR(36), attribute VARCHAR(64), value VARCHAR(32767));");
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	public void insert(String uuid, String country, String areacode, String number) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("INSERT");
		ps.setString(1, uuid);
		ps.setString(2, country);
		ps.setString(3, areacode);
		ps.setString(4, number);
		ps.execute();
		commit();
	}
	
	public void insertAttribute(String uuid, String attribute, String value) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("INSERT_ATTRIBUTES");
		ps.setString(1, uuid);
		ps.setString(2, attribute);
		ps.setString(3, value);
		ps.execute();
		commit();
	}
	
	public void delete(String uuid) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("DELETE");
		ps.setString(1, uuid);
		ps.execute();
		commit();
	}
	
	public void deleteAttributes(String uuid) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("DELETE_ATTRIBUTES");
		ps.setString(1, uuid);
		ps.execute();
		commit();
	}
	
	@SuppressWarnings("unchecked")
	public List select(String country, String areacode, String number) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		List uuids = new ArrayList();
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("SELECT");
		ps.setString(1, country);
		ps.setString(2, areacode);
		ps.setString(3, number);
		ResultSet r = ps.executeQuery();
		while (r.next()) {
			uuids.add(r.getString("uuid"));
		}
	
		return uuids;
	}
	
	@SuppressWarnings("unchecked")
	public List select(String country, String areacode) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		List uuids = new ArrayList();
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("SELECT2");
		ps.setString(1, country);
		ps.setString(2, areacode);
		ResultSet r = ps.executeQuery();
		while (r.next()) {
			uuids.add(r.getString("uuid"));
		}
	
		return uuids;
	}
	
	@SuppressWarnings("unchecked")
	public List select(IAttribute a) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		List uuids = new ArrayList();
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("SELECT3");
		ps.setString(1, a.getName());
		ps.setString(2, a.getValue()+"%");
		ResultSet r = ps.executeQuery();
		while (r.next()) {
			uuids.add(r.getString("uuid"));
		}
	
		return uuids;
	}
	
	public String selectAttribute(String uuid, String attribute) throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
		PreparedStatement ps = (PreparedStatement) this.m_preparedStatements.get("SELECT_ATT_VALUE");
		ps.setString(1, uuid);
		ps.setString(2, attribute);
		ResultSet r = ps.executeQuery();
		while (r.next()) {
			return r.getString("value");
		}

		return null;
	}
	
	public void disconnect() throws SQLException {
		if (isConnected()) {		
			super.setInitializing(false);
			Statement st = m_con.createStatement();
			st.execute("SHUTDOWN");
		}
		super.disconnect();
	}

	public void commit() throws SQLException {
		if (isConnected()) {			
			Statement st = m_con.createStatement();
			st.execute("COMMIT");
		}
		super.commit();
	}

	public void setInitializing(boolean init) {
		super.setInitializing(init);
	}

}
