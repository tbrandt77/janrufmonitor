package de.janrufmonitor.repository;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbMultiPhoneCallerDatabaseHandler;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class ThunderbirdCallerManager extends AbstractReadOnlyCallerManager
		implements ILocalRepository, IReadCallerRepository {

	public static String ID = "ThunderbirdCallerManager";

	public static String NAMESPACE = "repository.ThunderbirdCallerManager";

	private static String CFG_COMMIT_COUNT = "commit";

	private static String CFG_KEEP_ALIVE = "keepalive";
	
	private static String CFG_MAB_FILE = "mab";

	private IRuntime m_runtime;

	private ICallerDatabaseHandler m_dbh;

	private String THB_CACHE_PATH = PathResolver.getInstance()
			.getDataDirectory()
			+ "thb_cache" + File.separator;

	private class ThunderbirdCallerManagerHandler extends
			HsqldbMultiPhoneCallerDatabaseHandler {

		private IRuntime m_runtime;

		public ThunderbirdCallerManagerHandler(String driver, String connection,
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

	public ThunderbirdCallerManager() {
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

	private void createCallerListFromThunderbird() {
		final String file = this.m_configuration.getProperty(CFG_MAB_FILE, "");
		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e2) {
				}
				File mso_cache = new File(THB_CACHE_PATH);
				if (!mso_cache.exists())
					mso_cache.mkdirs();

				ICallerList cl = new ThunderbirdTransformer(file, true).getCallers();

				try {
					getDatabaseHandler().deleteCallerList(cl);
					getDatabaseHandler().commit();
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
		t.setName("JAM-ThunderbirdSync-Thread-(non-deamon)");
		t.start();
	}

	public String getNamespace() {
		return ThunderbirdCallerManager.NAMESPACE;
	}

	public void startup() {
		super.startup();
		if (this.isActive()) {
			this.createCallerListFromThunderbird();
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
		return ThunderbirdCallerManager.ID;
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
					.resolve(THB_CACHE_PATH + "thb_data_cache.db");
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
			this.m_dbh = new ThunderbirdCallerManagerHandler(
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

	public String getFile() {
		return PathResolver.getInstance(this.getRuntime()).resolve(m_configuration.getProperty(CFG_MAB_FILE, ""));
	}

	public String getFileType() {
		return "*.mab";
	}

	public void setFile(String filename) {
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), CFG_MAB_FILE, filename);
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();		
	}
}
