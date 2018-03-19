package de.janrufmonitor.repository;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.fritzbox.IPhonebookEntry;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.TR064FritzBoxFirmware;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallerException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.GetAddressbooksException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.fritzbox.firmware.exception.SetCallerException;
import de.janrufmonitor.fritzbox.mapping.FritzBoxMappingManager;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbMultiPhoneCallerDatabaseHandler;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.repository.types.IWriteCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;

public class FritzBoxPhonebookManager extends AbstractReadWriteCallerManager
		implements IRemoteRepository {


	public static String ID = "FritzBoxPhonebookManager";

	public static String NAMESPACE = "repository.FritzBoxPhonebookManager";

	private static String CFG_COMMIT_COUNT = "commit";

	private static String CFG_KEEP_ALIVE = "keepalive";
	
	private static String CFG_ADDRESSBOOK = "ab";
	
	private static String CFG_SYNC_INTERVAL = "sync_interval";

	private IRuntime m_runtime;
	
	private boolean m_loggedin;
	
	private String m_lastAbHash;
	
	private boolean isSyncing = false;

	private ICallerDatabaseHandler m_dbh;

	private String FBP_CACHE_PATH = PathResolver.getInstance()
			.getDataDirectory()
			+ "fritzbox_phonebook_cache" + File.separator;

	private class FritzBoxPhonebookManagerHandler extends
			HsqldbMultiPhoneCallerDatabaseHandler {

		private IRuntime m_runtime;

		public FritzBoxPhonebookManagerHandler(String db) {
			super(db);
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

			if (cl!=null && cl.size()>0) {
				ICaller c = null;
				for (int i=0;i<cl.size();i++) {
					c = cl.get(i);
					PreparedStatement ps = m_con.prepareStatement("DELETE FROM attributes WHERE ref=?;");
					ps.setString(1, c.getUUID());
					ps.execute();
			
					ps = m_con.prepareStatement("DELETE FROM callers WHERE uuid=?;");
					ps.setString(1, c.getUUID());
					ps.execute();
			
					ps = m_con.prepareStatement("DELETE FROM phones WHERE ref=?;");
					ps.setString(1, c.getUUID());
					ps.execute();
				}
			} else {
				PreparedStatement ps = m_con.prepareStatement("DELETE FROM attributes;");
				ps.execute();
		
				ps = m_con.prepareStatement("DELETE FROM callers;");
				ps.execute();
		
				ps = m_con.prepareStatement("DELETE FROM phones;");
				ps.execute();
			}
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

		int counter = 0;
		while(this.isSyncing && counter < 5) {
			counter++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		
		if (counter==5) {
			this.m_logger.warning("JAM-FritzBoxPhonebookSync-Thread still running for more then 5 sec., but caller identification requested.");
		}
		
		
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
			
			Logger m_logger;

			public void run() {
				isSyncing = true;
				this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
				if (this.m_logger.isLoggable(Level.FINE))
					this.m_logger.fine("Starting JAM-FritzBoxPhonebookSync-Thread");
				File mso_cache = new File(FBP_CACHE_PATH);
				if (!mso_cache.exists())
					mso_cache.mkdirs();

				ICallerList cl = getRuntime().getCallerFactory().createCallerList();
				
				FirmwareManager fwm = FirmwareManager.getInstance();
				fwm.startup();
				try {
					if (!fwm.isLoggedIn())
						fwm.login();
					
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("FritzBox Firmware created.");
					
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Login to FritzBox successfull.");
					
					// check if phonebook is configured
					String abId = getConfiguration().getProperty(CFG_ADDRESSBOOK, "0");
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Getting FritzBox phonebook ID: #"+abId);
					int id = Integer.parseInt(abId);
					String name = null;
					try {
						Map abs = fwm.getAddressbooks();
						if (abs.containsKey(Integer.parseInt(abId))) {
							name = (String) abs.get(Integer.parseInt(abId));
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Getting FritzBox phonebook name: "+name);
						}
					} catch (GetAddressbooksException e) {
						this.m_logger.log(Level.WARNING, e.getMessage(), e);
					}
					
					List callers = null;
					if (name!=null) {
						callers = fwm.getCallerList(id, name);
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Getting FritzBox phonebook callers: "+callers.size());
					} else {
						callers = fwm.getCallerList();
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Getting FritzBox default phonebook callers: "+callers.size());
					}
					if (callers.size()==0) {
						try {
							getDatabaseHandler().deleteCallerList(getRuntime().getCallerFactory().createCallerList());
							getDatabaseHandler().commit();
						} catch (SQLException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
							try {
								getDatabaseHandler().rollback();
							} catch (SQLException e1) {
								this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
							}
						}
						isSyncing = false;
						return;
					}

					cl.add(FritzBoxMappingManager.getInstance().toCallerList(callers));
					
				} catch (FritzBoxLoginException e2) {
					this.m_logger.log(Level.SEVERE, e2.getMessage(), e2);
				} catch (GetCallerListException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (Throwable e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}

				try {
					getDatabaseHandler().deleteCallerList(getRuntime().getCallerFactory().createCallerList());
					getDatabaseHandler().insertOrUpdateCallerList(cl);
					getDatabaseHandler().commit();
				} catch (SQLException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					try {
						getDatabaseHandler().rollback();
					} catch (SQLException e1) {
						this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
					}
				}
				
				isSyncing = false;
				
				if (this.m_logger.isLoggable(Level.FINE))
					this.m_logger.fine("Stopping JAM-FritzBoxPhonebookSync-Thread");
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
			int counter = 0;
			do {
				try {
					counter ++;
					FirmwareManager fwm = FirmwareManager.getInstance();
					fwm.startup();
					if (!fwm.isLoggedIn())
						fwm.login();
					m_loggedin = true;
				} catch (FritzBoxLoginException e) {
					this.m_logger.log(Level.SEVERE, "Login to fritzbox trial #"+counter+ " failed. Retrying.", e);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
			} while (!m_loggedin && counter < 5);
			
			if (m_loggedin) {
				Thread t = new Thread(new Runnable() {
					Logger m_logger;
					
					public void run() {
						this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
						if (this.m_logger.isLoggable(Level.FINE))
							this.m_logger.fine("Starting JAM-FritzBoxPhonebookHashChecker-Thread");
						do {
							FirmwareManager fwm = FirmwareManager.getInstance();
							try {
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("FritzBox Firmware created.");
								fwm.login();
								
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("Login to FritzBox successfull.");
								
								// check if phonebook is configured
								String abId = getConfiguration().getProperty(CFG_ADDRESSBOOK, "0");
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("Getting FritzBox phonebook ID: #"+abId);
								int id = Integer.parseInt(abId);
								String newAbHash = fwm.getAddressbookModificationHash(id);
								if (newAbHash!=null && !newAbHash.equals(m_lastAbHash)) {
									m_lastAbHash = newAbHash;
									createCallerListFromFritzBoxPhonebook();
								}
							} catch (FritzBoxLoginException e2) {
								this.m_logger.log(Level.SEVERE, e2.getMessage(), e2);
							} catch (IOException e) {
								this.m_logger.log(Level.SEVERE, e.getMessage(), e);
							} catch (Throwable e) {
								this.m_logger.log(Level.SEVERE, e.getMessage(), e);
							}
							try {
								Thread.sleep(getSyncInterval());
							} catch (InterruptedException e) {
							}
						} while (m_loggedin);
					}
				});
				t.setDaemon(true);
				t.setName("JAM-FritzBoxPhonebookHashChecker-Thread-(deamon)");
				t.start();
			}
		}
	}

	public void shutdown() {
		this.m_loggedin = false;
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
	
	public void setCaller(ICaller caller) {
		FirmwareManager fwm = FirmwareManager.getInstance();
		fwm.startup();
		try {
			if (!fwm.isLoggedIn())
				fwm.login();
			
			// check if phonebook is configured
			String abId = getConfiguration().getProperty(CFG_ADDRESSBOOK, "0");
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Getting FritzBox phonebook ID: #"+abId);
			int id = Integer.parseInt(abId);
			
			IPhonebookEntry pe = FritzBoxMappingManager.getInstance().mapJamCallerToFritzBox(caller);
			
			fwm.setCaller(id, pe);
			
			ICallerList cl = getRuntime().getCallerFactory().createCallerList(1);
			cl.add(caller);
			try {
				getDatabaseHandler().insertOrUpdateCallerList(cl);
				getDatabaseHandler().commit();
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				try {
					getDatabaseHandler().rollback();
				} catch (SQLException e1) {
					this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
				}
			}
			
		} catch (FritzBoxLoginException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (SetCallerException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} 
		if (!this.isSyncing) this.createCallerListFromFritzBoxPhonebook();
	}

	public void removeCaller(ICaller caller) {
		FirmwareManager fwm = FirmwareManager.getInstance();
		fwm.startup();
		try {
			if (!fwm.isLoggedIn())
				fwm.login();
			
			// check if phonebook is configured
			String abId = getConfiguration().getProperty(CFG_ADDRESSBOOK, "0");
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Getting FritzBox phonebook ID: #"+abId);
			int id = Integer.parseInt(abId);
			
			IPhonebookEntry pe = FritzBoxMappingManager.getInstance().mapJamCallerToFritzBox(caller);
			
			fwm.deleteCaller(id, pe);
			
			ICallerList cl = getRuntime().getCallerFactory().createCallerList(1);
			cl.add(caller);
			try {
				getDatabaseHandler().deleteCallerList(cl);
				getDatabaseHandler().commit();
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				try {
					getDatabaseHandler().rollback();
				} catch (SQLException e1) {
					this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
				}
			}
			
		} catch (FritzBoxLoginException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (DeleteCallerException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} 
		if (!this.isSyncing) this.createCallerListFromFritzBoxPhonebook();
	}
	
	@Override
	public boolean isSupported(Class c) {
		if (c.equals(IWriteCallerRepository.class)) {
			return FirmwareManager.getInstance().isInstance(TR064FritzBoxFirmware.class);
		}
		return super.isSupported(c);
	}
	
	private Properties getConfiguration() {
		return this.m_configuration;
	}
	
	private long getSyncInterval() {
		String iv = this.m_configuration.getProperty(CFG_SYNC_INTERVAL, "1");
		if (iv!=null && iv.length()>0) {
			return (Long.parseLong((iv.equalsIgnoreCase("0") ? "1" : iv)) * 60 * 1000);
		}
		return 60000;
	}

	private ICallerDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh == null) {
			String db_path = PathResolver.getInstance(this.getRuntime())
					.resolve(FBP_CACHE_PATH + "fritzbox_data_cache.db");
			
			this.m_dbh = new FritzBoxPhonebookManagerHandler(db_path);
			this.m_dbh.setCommitCount(Integer.parseInt(m_configuration
					.getProperty(CFG_COMMIT_COUNT, "10")));
			this.m_dbh.setKeepAlive((m_configuration.getProperty(
					CFG_KEEP_ALIVE, "true").equalsIgnoreCase("true") ? true
					: false));
			try {
				this.m_dbh.connect();
			} catch (ClassNotFoundException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return this.m_dbh;
	}

}
