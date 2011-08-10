package de.janrufmonitor.repository;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.fritzbox.firmware.AbstractFritzBoxFirmware;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.AbstractFritzBoxFirmware.PhonebookEntry;
import de.janrufmonitor.fritzbox.firmware.HTMLUtil;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbMultiPhoneCallerDatabaseHandler;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringEscapeUtils;
import de.janrufmonitor.util.string.StringUtils;

public class FritzBoxPhonebookManager extends AbstractReadOnlyCallerManager
		implements IRemoteRepository, IReadCallerRepository {

	public static String ID = "FritzBoxPhonebookManager";

	public static String NAMESPACE = "repository.FritzBoxPhonebookManager";

	private static String CFG_COMMIT_COUNT = "commit";

	private static String CFG_KEEP_ALIVE = "keepalive";

	private IRuntime m_runtime;

	private ICallerDatabaseHandler m_dbh;

	private String FBP_CACHE_PATH = PathResolver.getInstance()
			.getDataDirectory()
			+ "fritzbox_phonebook_cache" + File.separator;

	private class FritzBoxPhonebookManagerHandler extends
			HsqldbMultiPhoneCallerDatabaseHandler {

		private IRuntime m_runtime;

		public FritzBoxPhonebookManagerHandler(String driver, String connection,
				String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime == null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}

		public void deleteCallerList(ICallerList cl) throws SQLException {
			if (!isConnected())
				try {
					this.connect();
				} catch (ClassNotFoundException e) {
					throw new SQLException(e.getMessage());
				}

			PreparedStatement ps = m_con
					.prepareStatement("DELETE FROM attributes;");
			ps.execute();

			ps = m_con.prepareStatement("DELETE FROM callers;");
			ps.execute();

			ps = m_con.prepareStatement("DELETE FROM phones;");
			ps.execute();
		}

		public String getImageProviderID() {
			return ID;
		}
	}

	public FritzBoxPhonebookManager() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public ICaller getCaller(IPhonenumber number)
			throws CallerNotFoundException {
		if (number == null)
			throw new CallerNotFoundException(
					"Phone number is not set (null). No caller found.");

		if (number.isClired())
			throw new CallerNotFoundException(
					"Phone number is CLIR. Identification impossible.");

		ICaller c = null;
		try {
			c = getDatabaseHandler().getCaller(number);
			if (c != null)
				return c;
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}

		throw new CallerNotFoundException(
				"No caller entry found for phonenumber : "
						+ number.getTelephoneNumber());
	}

	public ICallerList getCallers(IFilter filter) {
		return this.getCallers(new IFilter[] { filter });
	}

	private void createCallerListFromFritzBoxPhonebook() {
		Thread t = new Thread(new Runnable() {

			public void run() {
				File mso_cache = new File(FBP_CACHE_PATH);
				if (!mso_cache.exists())
					mso_cache.mkdirs();

				ICallerList cl = getRuntime().getCallerFactory().createCallerList();
				
				FirmwareManager fwm = FirmwareManager.getInstance();
				try {
					fwm.login();
					List callers = fwm.getCallerList();
					if (callers.size()==0) return;

					List phones = null;
					IAttributeMap attributes = null;
					AbstractFritzBoxFirmware.PhonebookEntry pe = null;
					for (int i=0,j=callers.size();i<j;i++) {
						pe = (PhonebookEntry) callers.get(i);
						attributes = getRuntime().getCallerFactory().createAttributeMap();
						phones = new ArrayList(3);
						attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER, FritzBoxPhonebookManager.ID));
						try {
							attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, StringEscapeUtils.unescapeHtml(pe.getName())));
						} catch (Exception ex) {
							m_logger.log(Level.WARNING, ex.getMessage(), ex);
							attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, pe.getName()));
						}
						Map phs = pe.getPhones();
						Iterator entries = phs.keySet().iterator();
						String key = null;
						ICaller identified = null;
						while (entries.hasNext()) {
							key = (String) entries.next();
							identified = Identifier.identifyDefault(getRuntime(), getRuntime().getCallerFactory().createPhonenumber(key.substring(1)));
							if (identified!=null) {
								phones.add(identified.getPhoneNumber());
								attributes.add(getRuntime().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+identified.getPhoneNumber().getTelephoneNumber(), (String) phs.get(key)));
							}
						}
						if (phones.size()==0) continue;
						
						cl.add(getRuntime().getCallerFactory().createCaller(null, phones, attributes));						
					}
				} catch (FritzBoxLoginException e2) {
					m_logger.log(Level.SEVERE, e2.getMessage(), e2);
				} catch (GetCallerListException e) {
					m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					m_logger.log(Level.SEVERE, e.getMessage(), e);
				}

				try {
					getDatabaseHandler().deleteCallerList(cl);
					getDatabaseHandler().insertOrUpdateCallerList(cl);
					getDatabaseHandler().commit();
				} catch (SQLException e) {
					m_logger.log(Level.SEVERE, e.getMessage(), e);
					try {
						getDatabaseHandler().rollback();
					} catch (SQLException e1) {
						m_logger.log(Level.SEVERE, e1.getMessage(), e1);
					}
				}
			}
		});
		t.setName("JAM-FritzBoxPhonebookSync-Thread-(non-deamon)");
		t.start();

	}

	public String getNamespace() {
		return FritzBoxPhonebookManager.NAMESPACE;
	}

	public void startup() {
		super.startup();
		if (this.isActive()) {
			this.createCallerListFromFritzBoxPhonebook();
		}
	}

	public void shutdown() {
		if (this.m_dbh != null)
			try {
				getDatabaseHandler().commit();
				if (getDatabaseHandler().isConnected())
					getDatabaseHandler().disconnect();
				this.m_dbh = null;
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		super.shutdown();
	}

	public String getID() {
		return FritzBoxPhonebookManager.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public ICallerList getCallers(IFilter[] filters) {
		try {
			ICallerList cl = getDatabaseHandler().getCallerList(filters);
			if (!getDatabaseHandler().isKeepAlive())
				getDatabaseHandler().disconnect();
			return cl;
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return this.getRuntime().getCallerFactory().createCallerList();
	}

	private ICallerDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh == null) {
			String db_path = PathResolver.getInstance(this.getRuntime())
					.resolve(FBP_CACHE_PATH + "fritzbox_data_cache.db");
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
			this.m_dbh = new FritzBoxPhonebookManagerHandler(
					"org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:" + db_path,
					"sa", "", initialize);
			this.m_dbh.setCommitCount(Integer.parseInt(m_configuration
					.getProperty(CFG_COMMIT_COUNT, "10")));
			this.m_dbh.setKeepAlive((m_configuration.getProperty(
					CFG_KEEP_ALIVE, "true").equalsIgnoreCase("true") ? true
					: false));
		}
		return this.m_dbh;
	}
}
