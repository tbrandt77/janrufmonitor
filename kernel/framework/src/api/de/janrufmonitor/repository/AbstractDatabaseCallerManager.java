package de.janrufmonitor.repository;

import java.sql.SQLException;
import java.util.logging.Level;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.db.ICallerDatabaseHandler;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.types.ISearchableCallerRepository;

public abstract class AbstractDatabaseCallerManager extends AbstractReadWriteCallerManager implements ISearchableCallerRepository {
	
	protected ICallerDatabaseHandler m_dbh;

	private boolean m_keepObserverThread = false;
	private boolean m_isRunningProcess = false;
	
	public AbstractDatabaseCallerManager() {
		super();
	}

	public boolean isSupported(Class c) {
		return c.isInstance(this);
	}

	public void startup() {
		super.startup();
		// disconnects and reconnects every 10 mins.
		Thread disconnectObserverThread = new Thread(new Runnable() {
			public void run() {
				while (m_keepObserverThread)
					try {
						if (!m_isRunningProcess && !getDatabaseHandler().isKeepAlive() && getDatabaseHandler().isConnected())
							getDatabaseHandler().disconnect();
						
						Thread.sleep(2000);
						if (!m_isRunningProcess && !getDatabaseHandler().isKeepAlive())
							getDatabaseHandler().connect();
						Thread.sleep(600000);
					} catch (InterruptedException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (SQLException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (ClassNotFoundException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
			}
		});
		disconnectObserverThread.setDaemon(true);
		disconnectObserverThread.setName("JAM-"+getID()+"-Observer-Thread (deamon)");
		this.m_keepObserverThread = !getDatabaseHandler().isKeepAlive();
		disconnectObserverThread.start();
	}
	
	public void shutdown() {
		this.m_keepObserverThread = false;
		if (this.m_dbh!=null)
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
	

	public synchronized ICallerList getCallers(IFilter filter) {
		return this.getCallers(new IFilter[] {filter});
	}
	

	public synchronized ICallerList getCallers(IFilter[] filters) {
		return this.getCallers(filters, null);
	}
	
	public ICallerList getCallers(IFilter[] filters, ISearchTerm[] searchTerms) {
		try {
			m_isRunningProcess = true;
			ICallerList cl = getDatabaseHandler().getCallerList(filters, searchTerms);
			m_isRunningProcess = false;
			return cl;			
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		m_isRunningProcess = false;
		return this.getRuntime().getCallerFactory().createCallerList();
	}
	
	
    public synchronized ICaller getCaller(IPhonenumber number) throws CallerNotFoundException {
    	if (number==null)
			throw new CallerNotFoundException("Phone number is not set (null). No caller found.");
		
		if (number.isClired())
			throw new CallerNotFoundException("Phone number is CLIR. Identification impossible.");
			
		ICaller c = null;
		try {
			m_isRunningProcess = true;
			c = getDatabaseHandler().getCaller(number);
			if (c!=null) return c;
			m_isRunningProcess = false;
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		if (this.isInternalNumber(number)) {
			// no caller folder exists, take default value
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
		
        throw new CallerNotFoundException("No caller entry found for phonenumber : "+number.getTelephoneNumber());
	}
	
	public synchronized void removeCaller(ICallerList callerList) {
		try {
			m_isRunningProcess = true;
			getDatabaseHandler().deleteCallerList(callerList);
			getDatabaseHandler().commit();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			try {
				getDatabaseHandler().rollback();
			} catch (SQLException e1) {
				this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			}			
		}
		m_isRunningProcess = false;
	}
	
	public synchronized void setCaller(ICallerList callerList) {
		try {
			ICaller c = null;
			for (int i=0,j=callerList.size();i<j;i++) {
				c = callerList.get(i);
				this.addCreationAttributes(c);
				this.addSystemAttributes(c);
			}
			m_isRunningProcess = true;
			getDatabaseHandler().insertOrUpdateCallerList(callerList);
			getDatabaseHandler().commit();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			try {
				getDatabaseHandler().rollback();
			} catch (SQLException e1) {
				this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			}			
		}
		m_isRunningProcess = false;
	}
	
	public synchronized void updateCaller(ICaller caller) {
		ICallerList cl = this.getRuntime().getCallerFactory().createCallerList(1);
		cl.add(caller);
		try {
			this.addSystemAttributes(caller);
			m_isRunningProcess = true;
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
		m_isRunningProcess = false;
	}
	
	public synchronized void removeCaller(ICaller caller) {
		ICallerList cl = this.getRuntime().getCallerFactory().createCallerList(1);
		cl.add(caller);
		this.removeCaller(cl);
	}
	
	public synchronized void setCaller(ICaller caller) {
		ICallerList cl = this.getRuntime().getCallerFactory().createCallerList(1);
		cl.add(caller);
		this.setCaller(cl);
	}

	/**
	 * Creates a new instance of a specific database handler.
	 * 
	 * @return a valid database handler, must not be null.
	 */
	protected abstract ICallerDatabaseHandler getDatabaseHandler();

}
