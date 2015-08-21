package de.janrufmonitor.repository.db.hsqldb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.db.AbstractMultiPhoneCallerDatabaseHandler;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.CharacterFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.util.io.Base64Decoder;
import de.janrufmonitor.util.io.Base64Encoder;
import de.janrufmonitor.util.io.IImageProvider;
import de.janrufmonitor.util.io.IImageStreamProvider;
import de.janrufmonitor.util.io.ImageHandler;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;


public abstract class HsqldbMultiPhoneCallerDatabaseHandler extends AbstractMultiPhoneCallerDatabaseHandler {

	private class DBImageProvider implements IImageProvider, IImageStreamProvider {

		private Connection m_connection;
		private Logger m_logger; 
		
		public DBImageProvider(Connection con) {
			this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
			this.m_logger.info("Created new DBImageProvider.");
			this.m_connection = con;
		}
		
		public boolean hasImage(ICaller caller) {
			try {
				if (!isConnected()) return false;
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				return false;
			}
			
			try {
				return internalHasImage(caller);
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			return false;
		}
		

		public InputStream getImageInputStream(ICaller caller) {
			if (!hasImage(caller)) return null;
			
			try {
				return internalGetImageData(caller);
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			return null;
		}

		public String getImagePath(ICaller caller) {	
			IAttribute img_ref = caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
			if (img_ref!=null && img_ref.getValue().length()>0) {
				return "db://"+getID()+"/"+img_ref.getValue();
			}
			return "";
		}

		public File getImage(ICaller caller) {
			return null;
		}

		public String getID() {
			return getImageProviderID();
		}
		
		private boolean isConnected() throws SQLException {
			return m_connection != null && !m_connection.isClosed();
		}

		public String toString() {
			return "db://"+getID()+"/";
		}
	}

	protected IImageProvider m_ip;
	
	public HsqldbMultiPhoneCallerDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
		super(driver, connection, user, password, initialize);
	}
	
	public void disconnect() throws SQLException {
		if (this.m_ip!=null) ImageHandler.getInstance().removeProvider(this.m_ip);
		
		if (isConnected()) {		
			super.setInitializing(false);
			Statement st = m_con.createStatement();
			st.execute("SHUTDOWN");
		}
		super.disconnect();
	}
	
	public void connect() throws SQLException, ClassNotFoundException {
		super.connect();
		this.m_ip = new DBImageProvider(this.m_con);
		ImageHandler.getInstance().addProvider(this.m_ip);
	}

	public void commit() throws SQLException {
		if (isConnected()) {			
			Statement st = m_con.createStatement();
			st.execute("COMMIT");
		}
		super.commit();
	}
	
	public abstract String getImageProviderID();
	
	protected void createTables() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");

			
		Statement stmt = m_con.createStatement();
		stmt.execute("DROP TABLE attributes IF EXISTS;");
		stmt.execute("DROP TABLE callers IF EXISTS;");
		stmt.execute("DROP TABLE phones IF EXISTS;");
		stmt.execute("DROP TABLE versions IF EXISTS;");

		stmt.execute("CREATE TABLE versions (version VARCHAR(10));");
		stmt.execute("INSERT INTO versions (version) VALUES ('"+IJAMConst.VERSION_DISPLAY+"');");
		

		super.createTables();
	}
	
	protected void addPreparedStatements() throws SQLException {
		if (!isConnected())
			throw new SQLException("Database is disconnected.");
		
		// check database structure
		Statement stmt = m_con.createStatement();
		try {
			stmt.executeQuery("SELECT count(ref) FROM images;");
		} catch (Exception e) {
			this.m_logger.info("Detected database schema of version 5.0.27 and older.");
			
			stmt.execute("CREATE TABLE images (ref VARCHAR(36), value VARCHAR(1024000));");		
		} finally {
			stmt.close();
		}	

		m_preparedStatements.put("INSERT_IMAGE", m_con.prepareStatement("INSERT INTO images (ref, value) VALUES (?,?);"));
		m_preparedStatements.put("UPDATE_IMAGE", m_con.prepareStatement("UPDATE images SET value=? WHERE ref=?;"));
		m_preparedStatements.put("DELETE_IMAGE", m_con.prepareStatement("DELETE FROM images WHERE ref=?;"));
		m_preparedStatements.put("SELECT_IMAGE", m_con.prepareStatement("SELECT value FROM images WHERE ref=?;"));
		m_preparedStatements.put("SELECT_COUNT_IMAGE", m_con.prepareStatement("SELECT COUNT(value) FROM images WHERE ref=?;"));
		
		super.addPreparedStatements();
	}
	
	protected ICallerList buildCallerList(IFilter[] filters) throws SQLException {
		return this.buildCallerList(filters, null);
	}


	protected ICallerList buildCallerList(IFilter[] filters, ISearchTerm[] searchTerms) throws SQLException {
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
					//sql.append("country='"+pn.getIntAreaCode()+"' AND areacode='"+pn.getAreaCode()+"'");
					ResultSet rs = stmt.executeQuery("SELECT ref FROM phones WHERE country='"+pn.getIntAreaCode()+"' AND areacode='"+pn.getAreaCode()+"';");
					if (rs.next()) {
						sql.append("uuid='");
						sql.append(rs.getString(1));
						sql.append("'");
						while (rs.next()) {
							sql.append(" OR uuid='");
							sql.append(rs.getString(1));
							sql.append("'");
						}
					}
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
			if (searchTerms!=null && searchTerms.length>0){ 
				sql.append(" AND");	
				sql.append(createSearchTerm(searchTerms));	
			}
		} else {
			if (searchTerms!=null && searchTerms.length>0){ 
				sql.append(" WHERE");	
				sql.append(createSearchTerm(searchTerms));	
			}
		}
		
		sql.append(";");
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info(sql.toString());
		
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
		if (filters==null) return false;
		IFilter f = null;
		for (int i=0;i<filters.length;i++) {
			f = filters[i];
			if (f!=null && f.getType()==FilterType.ATTRIBUTE) return true;
			if (f!=null && f.getType()==FilterType.CHARACTER) return true;
		}
		return false;
	}
	
	private boolean internalHasImage(ICaller c) throws SQLException {
		IAttribute img_ref = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
		if (img_ref!=null && img_ref.getValue().length()>0) {
			PreparedStatement ps = this.getStatement("SELECT_COUNT_IMAGE");
			ps.setString(1, img_ref.getValue());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) return rs.getInt(1)>0;
		}
		return false;
	}
	
	private InputStream internalGetImageData(ICaller c) throws SQLException {
		IAttribute img_ref = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
		if (img_ref!=null && img_ref.getValue().length()>0) {
			PreparedStatement ps = this.getStatement("SELECT_IMAGE");
			ps.setString(1, img_ref.getValue());
			ResultSet rs = ps.executeQuery();
			String imgdata = null;
			while (rs.next()) {
				imgdata = rs.getString("value");
			}
			try {
				ByteArrayInputStream encodedIn = new ByteArrayInputStream(imgdata.getBytes());
				Base64Decoder b64 = new Base64Decoder(encodedIn);
				ByteArrayOutputStream decodedOut = new ByteArrayOutputStream();
				Stream.copy(b64, decodedOut);
				b64.close();
				decodedOut.close();
				
				return new ByteArrayInputStream(decodedOut.toByteArray());
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return null;
	}

	protected void internalUpdate(ICaller c) throws SQLException  {
		// check 1) image already exists and gets exchanged
		if (internalHasImage(c)) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Caller "+c.toString()+" has already an image. Updating image.");
			PreparedStatement ps = this.getStatement("UPDATE_IMAGE");
			if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Updating image from ATTRIBUTE_NAME_IMAGEPATH.");
				String imgpath = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH).getValue();
				File img = new File(PathResolver.getInstance(getRuntime()).resolve(imgpath));
				if (img.exists()) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Updating image from ATTRIBUTE_NAME_IMAGEPATH: "+img.getAbsolutePath());
					try {
						FileInputStream fim = new FileInputStream(img);
						ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
						Base64Encoder b64 = new Base64Encoder(encodedOut);
						Stream.copy(fim, b64);						
						b64.flush();
						b64.close();
						IAttribute img_ref = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
						String uuid = null;
						if (img_ref!=null && img_ref.getValue().length()>0) {
							uuid = img_ref.getValue();
						} else uuid = c.getUUID();
						updateImage(ps, uuid, encodedOut.toByteArray());
						if (uuid!=null)
							c.getAttributes().add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF, uuid));
					} catch (FileNotFoundException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
					c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH);
				} else {
					// file does not exist so remove image
					internalDelete(c);
					c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
					c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH);
				}
			} else if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY)) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Updating image from ATTRIBUTE_NAME_IMAGEBINARY.");
				
				String imgdata = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY).getValue();
				IAttribute img_ref = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
				String uuid = null;
				if (img_ref!=null && img_ref.getValue().length()>0) {
					uuid = img_ref.getValue();
				} else uuid = c.getUUID();
				updateImage(ps, uuid, imgdata.getBytes());
				if (uuid!=null)
					c.getAttributes().add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF, uuid));
				c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY);
			} else if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEURL)) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Updating image from ATTRIBUTE_NAME_IMAGEURL.");
				
				// TODO update from URL
				
				c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEURL);
			}
			ps.executeBatch();
		} 
		// check 2) image does not exist and has to be insert
		else if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH) || c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEURL) || c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY)) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Caller "+c.toString()+" has not yet an image. Inserting image.");
			
			PreparedStatement ps = this.getStatement("INSERT_IMAGE");
			if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)) {
				String imgpath = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH).getValue();
				File img = new File(PathResolver.getInstance(getRuntime()).resolve(imgpath));
				if (img.exists()) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Inserting image from ATTRIBUTE_NAME_IMAGEPATH: "+img.getAbsolutePath());
					try {
						FileInputStream fim = new FileInputStream(img);
						ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
						Base64Encoder b64 = new Base64Encoder(encodedOut);
						Stream.copy(fim, b64);						
						b64.flush();
						b64.close();
						createImage(ps, c.getUUID(), encodedOut.toByteArray());
						c.getAttributes().add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF, c.getUUID()));
					} catch (FileNotFoundException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				} 
				c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH);
			} else if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY)) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Inserting image from ATTRIBUTE_NAME_IMAGEBINARY.");
				
				String imgdata = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY).getValue();
				createImage(ps, c.getUUID(), imgdata.getBytes());
				c.getAttributes().add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF, c.getUUID()));
				c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY);
			} else if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEURL)) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Inserting image from ATTRIBUTE_NAME_IMAGEURL.");
				
				// TODO insert from URL
				
				c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEURL);
			}
			ps.executeBatch();
		}
		// check 3) image gets deleted
		else {
			internalDelete(c);
			c.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
		}
	}

	protected void internalInsert(ICaller c) throws SQLException  {
		internalUpdate(c);
	}

	protected void internalDelete(ICaller c) throws SQLException  {
		PreparedStatement ps = this.getStatement("DELETE_IMAGE");
		if (ps!=null) {
			this.deleteImage(ps, c.getUUID());
			ps.executeBatch();
		}
		
		IAttribute img_ref = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
		if (img_ref!=null && img_ref.getValue().length()>0) {
			ps = this.getStatement("DELETE_IMAGE");
			this.deleteImage(ps, img_ref.getValue());
			ps.executeBatch();
		}
	}
	
	private void createImage(PreparedStatement ps, String uuid, byte[] image) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.setString(2, new String(image));
		ps.addBatch();
	}
	
	private void updateImage(PreparedStatement ps, String uuid, byte[] image) throws SQLException {
		ps.clearParameters();
		ps.setString(1, new String(image));
		ps.setString(2, uuid);
		ps.addBatch();
	}

	private void deleteImage(PreparedStatement ps, String uuid) throws SQLException {
		ps.clearParameters();
		ps.setString(1, uuid);
		ps.addBatch();
	}
	
	private String createSearchTerm(ISearchTerm[] searchTerms) {
		if (searchTerms==null) return "";
		if (searchTerms.length==0) return "";
		
		StringBuffer sql = new StringBuffer();
		sql.append(" (");
		ISearchTerm st = null;
		for (int i=0, j=searchTerms.length;i<j;i++) {
			st = searchTerms[i];
			sql.append("content like '%");
			sql.append(StringUtils.replaceString(st.getSearchTerm(), "%", "")); // remove % signs in search term
			sql.append("%'");
			if (i<(j-1)) {
				sql.append(" ");
				sql.append(st.getOperator().toString());
				sql.append(" ");
			}
		}
		
		sql.append(")");
		return sql.toString();
	}
}
