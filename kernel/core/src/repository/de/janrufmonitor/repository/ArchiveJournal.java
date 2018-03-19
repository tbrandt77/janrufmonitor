package de.janrufmonitor.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.repository.db.ICallDatabaseHandler;
import de.janrufmonitor.repository.db.hsqldb.HsqldbCallDatabaseHandler;
import de.janrufmonitor.repository.filter.DateFilter;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;

public class ArchiveJournal extends AbstractDatabaseCallManager implements ILocalRepository {

	private class ArchiveJournalHandler extends HsqldbCallDatabaseHandler {

		private IRuntime m_runtime;
		
		public ArchiveJournalHandler(String db) {
			super(db);
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
	}
	
	
	private static String ID = "ArchiveJournal";
	private static String NAMESPACE = "repository.ArchiveJournal";
	
	private static String CFG_DB= "db";
	private static String CFG_COMMIT_COUNT= "commit";
	private static String CFG_KEEP_ALIVE= "keepalive";
	private static String CFG_TIMEFRAME= "timeframe";
	
	private IRuntime m_runtime;
	private Thread m_archivingThread;

	public ArchiveJournal() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getID() {
		return ArchiveJournal.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getNamespace() {
		return ArchiveJournal.NAMESPACE;
	}
	
	public boolean isSupported(Class c) {
		if (c.equals(IWriteCallRepository.class)) return false;
		return c.isInstance(this);
	}
	
	
	public void startup() {
		super.startup();
		startArchiving();
	}

	private void startArchiving() {
		if (m_archivingThread==null || !m_archivingThread.isAlive()) {
			m_archivingThread = new Thread(new Runnable() {
				public void run() {
					String currentRepository = getRuntime().getConfigManagerFactory().getConfigManager().getProperty("ui.jface.application.journal.Journal", "repository");
					if (currentRepository!=null && currentRepository.length()>0) {
						ICallManager cmg = getRuntime().getCallManagerFactory().getCallManager(currentRepository);
						if (!currentRepository.equalsIgnoreCase(ID) && cmg!=null && cmg.isActive() && cmg.isSupported(IReadCallRepository.class) && cmg.isSupported(IWriteCallRepository.class)) {
							long timeframe = Long.parseLong(m_configuration.getProperty(CFG_TIMEFRAME, "0"));
							if (timeframe>0) {
								try {
									Thread.sleep(10000);
								} catch (InterruptedException e1) {
								}
								long time = System.currentTimeMillis()-(timeframe * 86400000L);
								IFilter tf = new DateFilter(new Date(time), new Date(0));
								ICallList cl = ((IReadCallRepository)cmg).getCalls(tf);
								if (cl.size()>0) {
									try {
										getDatabaseHandler().setCallList(cl);
										((IWriteCallRepository)cmg).removeCalls(cl);
										// added 2010/12/06: added due to high memory consumption
										getDatabaseHandler().commit();
										getDatabaseHandler().disconnect();
										
										getRuntime().getConfigManagerFactory().getConfigManager().setProperty(NAMESPACE, "lastrun", Long.toString(System.currentTimeMillis()));
										getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
										
										String root = PathResolver.getInstance(getRuntime()).resolve(m_configuration.getProperty(CFG_DB, PathResolver.getInstance(getRuntime()).getDataDirectory()+"/journal.archive"));
										
										if (root!=null && root.length()>64) {
											root = root.substring(0,root.indexOf(File.separator, 4)+1)+"..."+root.substring(root.lastIndexOf(File.separator));
										}
										
										PropagationFactory.getInstance().fire(
											new Message(Message.INFO, "ui.jface.configuration.pages.ArchiveJournal", "success", new String[]{Integer.toString(cl.size()), root}, new Exception())
										);
									} catch (SQLException e) {
										m_logger.log(Level.SEVERE, e.getMessage(), e);
										PropagationFactory.getInstance().fire(
											new Message(Message.ERROR, "ui.jface.configuration.pages.ArchiveJournal", "failed", e)
										);
									}
								}
							} else {
								m_logger.warning("No archiving timeframe is set. Archiving is stopped.");
								PropagationFactory.getInstance().fire(
									new Message(Message.ERROR, "ui.jface.configuration.pages.ArchiveJournal", "failed", new Exception("No archiving timeframe is set. Archiving is stopped."))
								);
							}
						} else {
							m_logger.warning("Journal <"+currentRepository+"> invalid or not enabled. Archiving is stopped.");
						}
					} else {
						m_logger.warning("No journal configured to be archived.");
						PropagationFactory.getInstance().fire(
							new Message(Message.ERROR, "ui.jface.configuration.pages.ArchiveJournal", "failed", new Exception("No journal configured to be archived."))
						);
					}	
				}
			});
			m_archivingThread.setDaemon(true);
			m_archivingThread.setName("JAM-JournalArchiving#"+System.currentTimeMillis()+"-Thread-(non-deamon)");
			m_archivingThread.start();		
		}
	}

	public void shutdown() {
		if (m_archivingThread!=null) {
			int count = 0;
			do {
				count++;
				if (m_archivingThread!=null && !m_archivingThread.isAlive()) {
					m_archivingThread = null;
				} else {
					m_logger.warning("Archiving in progress, could not shutdown archiving service...");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
				}
			} while(count<4 && (m_archivingThread!=null && m_archivingThread.isAlive()));
		}

		try {
			if (this.getDatabaseHandler().isConnected())
				this.getDatabaseHandler().disconnect();
			
			String root = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/journal.archive"));
			File db_raw = new File(root);
			ZipArchive z = new ZipArchive(root);
			z.open();
			String[] entries = new String[2];
			InputStream[] ins = new InputStream[2];
			if (new File(db_raw.getAbsolutePath()+".properties").exists()) {
				entries[0] = db_raw.getName()+".properties";
				ins[0] = new FileInputStream(db_raw.getAbsolutePath()+".properties");
			}
			
			if (new File(db_raw.getAbsolutePath()+".script").exists()) {
				entries[1] = db_raw.getName()+".script";
				ins[1] = new FileInputStream(db_raw.getAbsolutePath()+".script");
			}
			z.add(entries, ins);
			z.close();
		} catch (ZipArchiveException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		super.shutdown();
	}

	protected ICallDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh==null) {
			String db_path = PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"journal.archive"));
			this.m_dbh = new ArchiveJournalHandler(db_path);
			this.m_dbh.setCommitCount(Integer.parseInt(m_configuration.getProperty(CFG_COMMIT_COUNT, "50")));
			this.m_dbh.setKeepAlive((m_configuration.getProperty(CFG_KEEP_ALIVE, "true").equalsIgnoreCase("true")? true : false));
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

	public String getFile() {
		return PathResolver.getInstance(this.getRuntime()).resolve(this.m_configuration.getProperty(CFG_DB, PathResolver.getInstance(this.getRuntime()).getDataDirectory()+"/journal.archive"));
	}
	
	public String getFileType() {
		return "*.archive";
	}

	public void setFile(String filename) {
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), CFG_DB, filename);
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();		
	}

}
