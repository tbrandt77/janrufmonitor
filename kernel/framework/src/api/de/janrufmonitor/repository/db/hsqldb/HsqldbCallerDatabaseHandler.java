package de.janrufmonitor.repository.db.hsqldb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;


public abstract class HsqldbCallerDatabaseHandler extends AbstractCallerDatabaseHandler {

	public HsqldbCallerDatabaseHandler(String db) {
		super("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:"+StringUtils.replaceString(db, "\\", "/"), "sa", "", false);
		File db_raw = new File(db);
		if (db_raw.exists()) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Database file found: "+db_raw.getAbsolutePath());
			
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Database file size: "+db_raw.length());
			
			if (db_raw.isFile()) {
				File props = new File(db + ".properties");
				File script = new File(db + ".script");
				
				if (this.m_logger.isLoggable(Level.INFO) && !props.exists())
					this.m_logger.info("Database .properties file missing: "+props.getAbsolutePath());
				
				if (this.m_logger.isLoggable(Level.INFO) && !script.exists())
					this.m_logger.info("Database .script file missing: "+script.getAbsolutePath());
				
				if (!props.exists() || !script.exists()) {
					ZipArchive z = new ZipArchive(db_raw.getAbsolutePath());
					try {
						z.open();
						if (z.isCreatedByCurrentVersion()) {
							InputStream in = z.get(props.getName());
							if (in!=null) {
								FileOutputStream out = new FileOutputStream(props);
								Stream.copy(in, out, true);
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("Extracted .properties file from .db: "+props.getAbsolutePath());
							}
							in = z.get(script.getName());
							if (in!=null) {
								FileOutputStream out = new FileOutputStream(script);
								Stream.copy(in, out, true);
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("Extracted .script file from .db: "+script.getAbsolutePath());
							} else {
								this.setInitializing(true);
							}					
						}
					} catch (ZipArchiveException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (FileNotFoundException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} finally {
						try {
							if (z.available())
								z.close();
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Database file size: "+db_raw.length());
						} catch (ZipArchiveException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				} else {
					if (this.m_logger.isLoggable(Level.INFO) && !props.exists())
						this.m_logger.info("Found database .properties file: "+props.getAbsolutePath());
					
					if (this.m_logger.isLoggable(Level.INFO) && !script.exists())
						this.m_logger.info("Found database .script file: "+script.getAbsolutePath());
					
					ZipArchive z = new ZipArchive(db_raw.getAbsolutePath());
					try {
						z.open();
						String[] entries = new String[] { db_raw.getName()+".properties", db_raw.getName()+".script" };
						InputStream[] ins = new InputStream[] { new FileInputStream(db_raw.getAbsolutePath()+".properties"),new FileInputStream(db_raw.getAbsolutePath()+".script") };
						z.add(entries, ins);
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Added .properties file to .db: "+props.getAbsolutePath());
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Added .script file to .db: "+script.getAbsolutePath());
					} catch (ZipArchiveException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (FileNotFoundException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} finally {
						try {
							if (z.available())
								z.close();
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Database file size: "+db_raw.length());
						} catch (ZipArchiveException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				}		
			}
		} else {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Database file does not exist: "+db_raw.getAbsolutePath());
			
			this.setInitializing(true);
			
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Checking and creating directory structure: "+db_raw.getParentFile().getAbsolutePath());
			
			db_raw.getParentFile().mkdirs();
			ZipArchive z = new ZipArchive(db_raw.getAbsolutePath());
			try {
				z.open();
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("New database file created: "+db_raw.getAbsolutePath());
			} catch (ZipArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} finally {
				try {
					if (z.available())
						z.close();
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Database file size: "+db_raw.length());
				} catch (ZipArchiveException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}
	
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
