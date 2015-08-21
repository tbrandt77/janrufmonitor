package de.janrufmonitor.repository.db.hsqldb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.logging.Level;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.db.AbstractCallerDatabaseHandler;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.CharacterFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;


public abstract class HsqldbCallerDatabaseHandler extends AbstractCallerDatabaseHandler {

	public HsqldbCallerDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
		super(driver, connection, user, password, initialize);
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
	
	protected void createTables() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");
	
		Statement stmt = m_con.createStatement();
		stmt.execute("DROP TABLE attributes IF EXISTS;");
		stmt.execute("DROP TABLE callers IF EXISTS;");
		stmt.execute("DROP TABLE versions IF EXISTS;");

		stmt.execute("CREATE TABLE versions (version VARCHAR(10));");
		stmt.execute("INSERT INTO versions (version) VALUES ('"+IJAMConst.VERSION_DISPLAY+"');");
		
		super.createTables();
	}
	

	protected ICallerList buildCallerList(IFilter[] filters,
			ISearchTerm[] searchTerms) throws SQLException {
		// searchTerms are NOT considered in this implementation
		return this.buildCallerList(filters);
	}


	protected ICallerList buildCallerList(IFilter[] filters) throws SQLException {
		ICallerList cl = this.getRuntime().getCallerFactory().createCallerList();

		if (!isConnected()) return cl;
		
		StringBuffer sql = new StringBuffer();
		Statement stmt = m_con.createStatement();

		// build SQL statement
		sql.append("SELECT content FROM callers");
		if (hasAttributeFilter(filters))
			sql.append(", attributes");
		
		if (filters!=null && filters.length>0 && filters[0]!=null) {
			IFilter f = null;
			sql.append(" WHERE ");
			for (int i=0;i<filters.length;i++) {
				f = filters[i];
				if (i>0) sql.append(" AND ");

				if (f.getType()==FilterType.PHONENUMBER) {
					IPhonenumber pn = (IPhonenumber)f.getFilterObject();
					sql.append("country='"+pn.getIntAreaCode()+"' AND areacode='"+pn.getAreaCode()+"'");
				}
				
				if (f.getType()==FilterType.ATTRIBUTE) {
					IAttributeMap m = ((AttributeFilter)f).getAttributeMap();
					if (m!=null && m.size()>0) {
						sql.append("(");
						sql.append("callers.uuid=attributes.ref AND (");
						Iterator iter = m.iterator();
						IAttribute a = null;
						while (iter.hasNext()) {
							a = (IAttribute) iter.next();
							sql.append("attributes.name='");
							sql.append(a.getName());
							sql.append("'");
							sql.append(" AND ");
							sql.append("attributes.value='");
							sql.append(a.getValue());
							sql.append("'");
							if (iter.hasNext())
								sql.append(" OR ");
						}
						sql.append("))");	
					}
				}
				if (f.getType() == FilterType.CHARACTER) {
					sql.append("(");
					sql.append("callers.uuid=attributes.ref AND (");
					sql.append("attributes.name='");
					sql.append(((CharacterFilter)f).getAttributeName());
					sql.append("'");
					sql.append(" AND ");
					sql.append("(attributes.value like '");
					sql.append(((CharacterFilter)f).getCharacter().toUpperCase());
					sql.append("%'");
					sql.append(" OR attributes.value like '");
					sql.append(((CharacterFilter)f).getCharacter().toLowerCase());
					sql.append("%'");
					sql.append(")))");	
				}
			}
		}
		
		sql.append(";");
		
		ResultSet rs = stmt.executeQuery(sql.toString());
		while (rs.next()) {
			try {
				cl.add(Serializer.toCaller(rs.getString("content").getBytes(), this.getRuntime()));
			} catch (SerializerException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} 
		}
		return cl;
	}
	
	private boolean hasAttributeFilter(IFilter[] filters) {
		IFilter f = null;
		for (int i=0;i<filters.length;i++) {
			f = filters[i];
			if (f!=null && f.getType()==FilterType.ATTRIBUTE) return true;
			if (f!=null && f.getType()==FilterType.CHARACTER) return true;
		}
		return false;
	}
	
	public void setInitializing(boolean init) {
		super.setInitializing(init);
	}
}
