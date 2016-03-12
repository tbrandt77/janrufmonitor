package de.janrufmonitor.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.logging.Level;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbCallerDatabaseHandler;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.util.uuid.UUID;

public class CountryDirectory extends AbstractReadOnlyDatabaseCallerManager {

	private static String ID = "CountryDirectory";

	private String NAMESPACE = "repository.CountryDirectory";

	private static String CFG_DB = "db";

	private static String CFG_COMMIT_COUNT = "commit";

	private static String CFG_KEEP_ALIVE = "keepalive";

	private static String CFG_DEFAULT_AREACODE_LENGTH = "areacodelength";

	private IRuntime m_runtime;


	private boolean m_isMigrating; 
	
	private class CountryDirectoryHandler extends HsqldbCallerDatabaseHandler {

		private IRuntime m_runtime;

		public CountryDirectoryHandler(String driver, String connection,
				String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime == null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
		
		protected void addPreparedStatements() throws SQLException {
			super.addPreparedStatements();
			
			m_preparedStatements.put("SELECT_CALLER_PHONE2", m_con.prepareStatement("SELECT content FROM callers WHERE country=? AND areacode=? AND number=?;"));
			m_preparedStatements.put("DELETE_ATTRIBUTE_ALL", m_con.prepareStatement("DELETE FROM attributes;"));
			m_preparedStatements.put("DELETE_COUNTRY", m_con.prepareStatement("DELETE FROM callers WHERE country=?;"));
		}
		
		public void insertOrUpdateCallerList(ICallerList cl) throws SQLException {
			if (!isConnected())
				try {
					this.connect();
				} catch (ClassNotFoundException e) {
					throw new SQLException(e.getMessage());
				}
			
			if (cl.size()>0) {
				String countrycode = cl.get(0).getPhoneNumber().getIntAreaCode();
				PreparedStatement ps = this.getStatement("DELETE_COUNTRY");
				ps.setString(1, countrycode);
				ps.execute();
			}
			
			super.insertOrUpdateCallerList(cl);
		}

		public void deleteCallerList(ICallerList cl) throws SQLException {
			super.deleteCallerList(cl);
			
			PreparedStatement ps = this.getStatement("DELETE_ATTRIBUTE_ALL");
			ps.execute();
		}

		public ICaller getCaller(IPhonenumber pnp) throws SQLException {
			if (!isConnected())
				try {
					this.connect();
				} catch (ClassNotFoundException e) {
					throw new SQLException(e.getMessage());
				}

			try {
				IPhonenumber p = this.normalizePhonenumber(pnp);
	
				// check if caller is in local properties file
				PreparedStatement ps = this.getStatement("SELECT_CALLER_PHONE2");
				ps.clearParameters();
				ps.setString(1, p.getIntAreaCode());
				ps.setString(2, p.getAreaCode());
				ps.setString(3, "area");
				ResultSet rs = ps.executeQuery();
				ICaller c = null;
				while (rs.next()) {
					try {
						c = Serializer.toCaller(rs.getString("content").getBytes(),
								this.getRuntime());
						if (c != null) {
							c.getPhoneNumber().setIntAreaCode(p.getIntAreaCode());
							c.getPhoneNumber().setAreaCode(p.getAreaCode());
							c.getPhoneNumber().setCallNumber(p.getCallNumber());
							c.setUUID(new UUID().toString());
							return c;
						}
					} catch (SerializerException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
	
				ps.clearParameters();
				ps.setString(1, p.getIntAreaCode());
				ps.setString(2, "");
				ps.setString(3, "country");
				rs = ps.executeQuery();
				c = null;
				while (rs.next()) {
					try {
						c = Serializer.toCaller(rs.getString("content").getBytes(),
								this.getRuntime());
						if (c != null) {
							c.getPhoneNumber().setIntAreaCode(p.getIntAreaCode());
							c.getPhoneNumber().setAreaCode(p.getAreaCode());
							c.getPhoneNumber().setCallNumber(p.getCallNumber());
							c.setUUID(new UUID().toString());
							return c;
						}
					} catch (SerializerException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			} catch (Exception ex) {
				this.m_logger.log(Level.SEVERE, (pnp!=null ? "Error while analyzing number ["+pnp.getTelephoneNumber() + "]: " : "") +ex.getMessage(), ex);
			}
			if (this.m_logger.isLoggable(Level.SEVERE))
				this.m_logger.severe("Number has invalid data or structure, no country or city found: "+pnp);
			
//			PropagationFactory.getInstance().fire(
//					new Message(Message.ERROR,
//						NAMESPACE,
//						"error_number",
//						new String[] {pnp.getTelephoneNumber()},
//						new Exception("Number has invalid data or structure, no country or city found: "+pnp),
//						false),
//					"Tray");
			
			return null;
		}

		private IPhonenumber normalizePhonenumber(IPhonenumber pn) {
			IPhonenumber nomalizedPhonenumber = this.getRuntime()
					.getCallerFactory().createPhonenumber("");

			// check if pn is still normalized
			if (pn.getIntAreaCode().length() > 0
					&& pn.getAreaCode().length() > 0
					&& pn.getCallNumber().length() > 0) {
				return pn;
			}

			String intAreaCode = this.detectIntAreaCode(pn);
			String areaCode = this.detectAreaCode(pn, intAreaCode);
			String callNumber = this
					.detectCallnumber(pn, intAreaCode, areaCode);

			nomalizedPhonenumber.setIntAreaCode(intAreaCode);
			nomalizedPhonenumber.setAreaCode(areaCode);
			nomalizedPhonenumber.setCallNumber(callNumber);
			return nomalizedPhonenumber;
		}

		private String getPrefix() {
			String prefix = this.getRuntime().getConfigManagerFactory()
					.getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
							IJAMConst.GLOBAL_INTAREA_PREFIX);
			return (prefix == null ? "0" : prefix);
		}

		private String getLocalAreaCode() {
			String ac = this.getRuntime().getConfigManagerFactory()
					.getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
							IJAMConst.GLOBAL_INTAREA);
			return (ac == null ? "" : ac);
		}

		private String detectIntAreaCode(IPhonenumber pn) {

			// first hierarchy level is set
			if (pn.getIntAreaCode().length() > 0) {
				return pn.getIntAreaCode();
			}
			
			// added 2010/11/02: special check for local intarea code 39 (Italy)
			if (pn.getIntAreaCode().length() == 0 && this.isSpecialLocalIntAreaCode())
				return this.getLocalAreaCode();

			// first level is not set
			if (pn.getIntAreaCode().length() == 0
					&& pn.getTelephoneNumber().startsWith(this.getPrefix())) {
				String intAreaCode = pn.getTelephoneNumber();
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Complete number to check int area code: "+intAreaCode);
				
				// remove trailing prefix
				intAreaCode = intAreaCode.substring(this.getPrefix().length(), intAreaCode.length());
				
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Removed prefix ("+this.getPrefix()+"): "+intAreaCode);
				
				for (int i = 1; i < intAreaCode.length() - 1; i++) {
					String check = intAreaCode.substring(0, i);
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Check int area code: "+check);
					if (this.isIntAreaCodeExisting(check)) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Found int area code: "+check);
						return check;
					}
				}
				this.m_logger.warning("number contains invalid int area code: "+pn.getTelephoneNumber());
				if (pn.getTelephoneNumber().length()>3)
					return pn.getTelephoneNumber().substring(1,3);
			}
			return this.getLocalAreaCode();
		}

		private String detectAreaCode(IPhonenumber pn, String intAreaCode) {

			// second hierarchy level is set
			if (pn.getAreaCode().length() > 0) {
				return pn.getAreaCode();
			}

			String areaCode = pn.getTelephoneNumber();
			int to = this.getDefaultAreaCodeLength();

			// no prefix match
			if (!pn.getTelephoneNumber().startsWith(this.getPrefix()) || this.isSpecialLocalIntAreaCode()) {
				for (int i = areaCode.length() - 1; i >= 1; i--) {
					String check = areaCode.substring(0, i);
					if (this.isAreaCodeExisting(intAreaCode, check)) {
						return check;
					}
				}
				return areaCode.substring(0, Math.min(to,
						areaCode.length()));
			}

			// prefix is set
			areaCode = areaCode.substring(this.getPrefix().length()
					+ intAreaCode.length(), areaCode.length());

			for (int i = areaCode.length() - 1; i >= 1; i--) {
				String check = areaCode.substring(0, i);
				if (this.isAreaCodeExisting(intAreaCode, check)) {
					return check;
				}
			}
			return areaCode.substring(0, Math.min(to, areaCode
					.length()));
		}

		private int getDefaultAreaCodeLength() {
			String value = m_configuration.getProperty(
					CFG_DEFAULT_AREACODE_LENGTH, "3");
			try {
				return Integer.parseInt(value);
			} catch (Exception ex) {
				this.m_logger.warning(ex.getMessage());
			}
			return 3;
		}

		private String detectCallnumber(IPhonenumber pn, String intAreaCode,
				String areaCode) {

			// second hierarchy level is set
			if (pn.getCallNumber().length() > 0) {
				return pn.getCallNumber();
			}

			String callNumber = pn.getTelephoneNumber();

			// remove asterix
			if (callNumber.indexOf("*") > -1) {
				if (callNumber.indexOf("*")==0) {
					callNumber = StringUtils.replaceString(callNumber, "*", "");	
				} else 
					callNumber = callNumber.substring(0, callNumber.indexOf("*") - 1);
			}

			// no prefix match
			if (!pn.getTelephoneNumber().startsWith(this.getPrefix()) || this.isSpecialLocalIntAreaCode()) {
				return callNumber.substring(callNumber.indexOf(areaCode)
						+ areaCode.length(), callNumber.length());
			}

			return callNumber.substring(this.getPrefix().length()
					+ intAreaCode.length() + areaCode.length(), callNumber
					.length());
		}

		private boolean isAreaCodeExisting(String intAreaCode, String p) {
			PreparedStatement ps = this
					.getStatement("SELECT_CALLER_PHONE_COUNT");

			try {
				ps.setString(1, intAreaCode);
				ps.setString(2, p);
				ps.setString(3, "area");

				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					if (rs.getInt(1) > 0)
						return true;
				}
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			}

			return false;
		}

		private boolean isIntAreaCodeExisting(String p) {
			PreparedStatement ps = this
					.getStatement("SELECT_CALLER_PHONE_COUNT");

			try {
				ps.setString(1, p);
				ps.setString(2, "");
				ps.setString(3, "country");

				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					if (rs.getInt(1) > 0)
						return true;
				}
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			}

			return false;
		}
		
		/**
		 * Checks if a special local intarea code is set, e.g. 39 for Italy
		 * 
		 * @return
		 */
		private boolean isSpecialLocalIntAreaCode() {
			return this.getLocalAreaCode().equalsIgnoreCase("39");
		}

	}

	public CountryDirectory() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public void shutdown() {
		int retry = 0;
		while (m_isMigrating && retry < 10) {
			m_logger.info("repository is shutdown, but still migrating. Retrycount: "+retry);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			retry++;
		}
		if (retry==10)
			m_logger.warning("Aborting migration of areacode files. The database may be corrupted.");
		
		super.shutdown();
	}

	public void startup() {
		String root = PathResolver.getInstance(this.getRuntime()).resolve(
				this.m_configuration.getProperty(CFG_DB, PathResolver
						.getInstance(this.getRuntime()).getDataDirectory()
						+ "/countrycodes.db"));

		File props = new File(root + ".properties");
		if (!props.exists()) {
			props.getParentFile().mkdirs();
			try {
				File db_raw = new File(root);
				if (db_raw.exists()) {
					// exctract old data
					ZipArchive z = new ZipArchive(root);
					z.open();
					if (z.isCreatedByCurrentVersion()) {
						InputStream in = z
								.get(db_raw.getName() + ".properties");
						if (in != null) {
							FileOutputStream out = new FileOutputStream(db_raw
									.getAbsolutePath()
									+ ".properties");
							Stream.copy(in, out, true);
						}
						in = z.get(db_raw.getName() + ".script");
						if (in != null) {
							FileOutputStream out = new FileOutputStream(db_raw
									.getAbsolutePath()
									+ ".script");
							Stream.copy(in, out, true);
						}
					}
					z.close();
				}
			} catch (ZipArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		} else {
			try {
				File db_raw = new File(root);
				ZipArchive z = new ZipArchive(root);
				z.open();
				String[] entries = new String[] {
						db_raw.getName() + ".properties",
						db_raw.getName() + ".script" };
				InputStream[] ins = new InputStream[] {
						new FileInputStream(db_raw.getAbsolutePath()
								+ ".properties"),
						new FileInputStream(db_raw.getAbsolutePath()
								+ ".script") };
				z.add(entries, ins);
				z.close();
			} catch (ZipArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (FileNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		super.startup();

		this.importAreacodeCsvFiles();
	}
	
	private void importAreacodeCsvFiles() {
		String restart = System.getProperty("jam.installer.restart");
		if (restart==null || restart.equalsIgnoreCase("true")) {
			this.m_logger.info("Detected jam.installer.restart flag as: "+System.getProperty("jam.installer.restart"));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			
			restart = System.getProperty("jam.installer.restart");
			if (restart !=null && restart.equalsIgnoreCase("true")) {
				this.m_logger.info("Areacode update is not started, due to installation of new modules.");
				return;
			}
		}
		this.m_isMigrating = true;
		
		File areacodeFolder = new File(PathResolver.getInstance(this.getRuntime())
				.getDataDirectory()
				+ File.separator + "areacodes");
		if (!areacodeFolder.exists()) areacodeFolder.mkdirs();
		File[] areacodeCsvs = areacodeFolder.listFiles(new FilenameFilter() {

			public boolean accept(File f, String name) {
				return name.endsWith(".areacode.csv");
			}});
		if (areacodeCsvs.length>0) {
			File areacodeCsv = null;
			ICallerList l = getRuntime().getCallerFactory().createCallerList();
			for (int i=0;i<areacodeCsvs.length;i++) {
				areacodeCsv = areacodeCsvs[i];
				if (areacodeCsv.isFile() && areacodeCsv.exists()) {
					try {
						// structure of file
						// #intareacode;areacode;country;city
						InputStream content =new FileInputStream(areacodeCsv);
						if (content!=null) {
							BufferedReader reader = new BufferedReader(
			                          new InputStreamReader(content, "ISO-8859-1") );
							String[] entry = new String[4];
							StringTokenizer st = null;
							for ( String line; (line = reader.readLine()) != null; ) {
								if (line.startsWith("#")) {
									if (this.m_logger.isLoggable(Level.INFO))
										this.m_logger.info("Skipping line from import (start comment line #): "+line);
									continue;
								}
								st = new StringTokenizer(line, ";");
								if (st.countTokens()==2) {
									entry[0] = st.nextToken().trim();
									entry[1] = st.nextToken().trim();
									
									IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(entry[0],"","country");
									IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
									if (entry[1].length()>0)
										m.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY, entry[1]));
									
									ICaller c  = getRuntime().getCallerFactory().createCaller(getRuntime().getCallerFactory().createName("", ""), pn, m);
									if (this.m_logger.isLoggable(Level.INFO))
										this.m_logger.info("Adding intareacode entry: "+c);
									l.add(c);
								} else if (st.countTokens()==4) {
									entry[0] = st.nextToken().trim();
									entry[1] = st.nextToken().trim();
									entry[2] = st.nextToken().trim();
									entry[3] = st.nextToken().trim();
									
									IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(entry[0],entry[1],(entry[1].length()==0 ? "country": "area"));
									IAttributeMap m = getRuntime().getCallerFactory().createAttributeMap();
									if (entry[2].length()>0)
										m.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY, entry[2]));
									if (entry[3].length()>0)
										m.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CITY, entry[3]));
									
									ICaller c  = getRuntime().getCallerFactory().createCaller(getRuntime().getCallerFactory().createName("", ""), pn, m);
									if (this.m_logger.isLoggable(Level.INFO))
										this.m_logger.info("Adding areacode entry: "+c);
									l.add(c);
								} else {
									if (this.m_logger.isLoggable(Level.INFO))
										this.m_logger.info("Skipping line from import (invalid token count): "+line);
								}
							}
							reader.close();
						}
						if (l.size()>0)
							this.storeCountryAreacodes(l);
						l.clear();
						if (!areacodeCsv.delete()) areacodeCsv.deleteOnExit();
					} catch (IOException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
			
		} else {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("No .areacode.csv file found in path: "+areacodeFolder.getAbsolutePath());
		}
		this.m_isMigrating = false;
	}
	
	private void storeCountryAreacodes(ICallerList l) {
		try {
			if (l.size() > 0)
				getDatabaseHandler().insertOrUpdateCallerList(l);
				getDatabaseHandler().deleteCallerList(getRuntime().getCallerFactory().createCallerList());
				getDatabaseHandler().commit();
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Committed database entries.");
				
				if (!getDatabaseHandler().isKeepAlive())
					getDatabaseHandler().disconnect();
				
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			this.m_isMigrating = false;
			return;
		}
	}

	public String getID() {
		return CountryDirectory.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	protected ICallerDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh == null) {
			String db_path = PathResolver.getInstance(this.getRuntime())
					.resolve(
							this.m_configuration.getProperty(CFG_DB,
									PathResolver.getInstance(this.getRuntime())
											.getDataDirectory()
											+ "/countrycodes.db"));
			db_path = StringUtils.replaceString(db_path, "\\", "/");
			File db = new File(db_path + ".properties");
			boolean initialize = false;
			if (!db.exists()) {
				initialize = true;
				db.getParentFile().mkdirs();
				try {
					File db_raw = new File(db_path);
					if (!db_raw.exists()) {
						ZipArchive z = new ZipArchive(db_path);
						z.open();
						z.close();
					}
				} catch (ZipArchiveException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			this.m_dbh = new CountryDirectoryHandler("org.hsqldb.jdbcDriver",
					"jdbc:hsqldb:file:" + db_path, "sa", "", initialize);
			this.m_dbh.setCommitCount(Integer.parseInt(m_configuration
					.getProperty(CFG_COMMIT_COUNT, "50")));
			this.m_dbh.setKeepAlive((m_configuration.getProperty(
					CFG_KEEP_ALIVE, "true").equalsIgnoreCase("true") ? true
					: false));
		}
		return this.m_dbh;
	}

	public ICaller getCaller(IPhonenumber number)
			throws CallerNotFoundException {
		if (number == null)
			throw new CallerNotFoundException(
					"Phone number is not set (null). No caller found.");

		if (number.isClired())
			throw new CallerNotFoundException(
					"Phone number is CLIR. Identification impossible.");

		while(this.m_isMigrating) {
			this.m_logger.info("Waiting 1 sec. till update of country directory is finished.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		
		if (PhonenumberAnalyzer.getInstance(this.getRuntime()).isInternal(number)) {
			String language = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_LANGUAGE);
					
			IName name = this.getRuntime().getCallerFactory().createName(
				"",
				this.getRuntime().getI18nManagerFactory().getI18nManager().getString(getNamespace(), IJAMConst.INTERNAL_CALL, "label", language),
				""
			);				

			String n = number.getTelephoneNumber();
			if (n.trim().length()==0)
				n = number.getCallNumber();			
					
			return this.getRuntime().getCallerFactory().createCaller(name, 
					this.getRuntime().getCallerFactory().createInternalPhonenumber(n));
		}
		
		ICaller c = null;
		try {
			c = getDatabaseHandler().getCaller(number);
			
			if (!getDatabaseHandler().isKeepAlive()){
				getDatabaseHandler().disconnect();
			}
				
			
			if (c != null)
				return c;
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		throw new CallerNotFoundException(
				"No caller entry found for phonenumber : "
						+ number.getTelephoneNumber());
	}
}
