package de.janrufmonitor.repository;

import java.sql.SQLException;
import java.util.logging.Level;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.repository.db.ICallDatabaseHandler;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.ISearchableCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;

public abstract class AbstractDatabaseCallManager extends AbstractConfigurableCallManager implements IReadCallRepository, IWriteCallRepository, ISearchableCallRepository {

	protected ICallDatabaseHandler m_dbh;
	
	public AbstractDatabaseCallManager() {
		super();
	}
	
	public boolean isSupported(Class c) {
		return c.isInstance(this);
	}

	public synchronized void setCall(ICall call) {
		ICallList cl = this.getRuntime().getCallFactory().createCallList(1);
		cl.add(call);
		this.setCalls(cl);
	}

	public synchronized void setCalls(ICallList list) {
		try {
			getDatabaseHandler().setCallList(list);
			getDatabaseHandler().commit();
			
			if (!getDatabaseHandler().isKeepAlive())
				getDatabaseHandler().disconnect();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			try {
				getDatabaseHandler().rollback();
			} catch (SQLException e1) {
				this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}

	public synchronized void updateCall(ICall call) {
		ICallList cl = this.getRuntime().getCallFactory().createCallList(1);
		cl.add(call);
		this.updateCalls(cl);
	}

	public synchronized void updateCalls(ICallList list) {
		try {
			getDatabaseHandler().updateCallList(list);
			getDatabaseHandler().commit();
			
			if (!getDatabaseHandler().isKeepAlive())
				getDatabaseHandler().disconnect();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			try {
				getDatabaseHandler().rollback();
			} catch (SQLException e1) {
				this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			}			
		}
	}

	public synchronized void removeCall(ICall call) {
		ICallList cl = this.getRuntime().getCallFactory().createCallList(1);
		cl.add(call);
		this.removeCalls(cl);
	}

	public synchronized void removeCalls(ICallList callList) {
		try {
			getDatabaseHandler().deleteCallList(callList);
			getDatabaseHandler().commit();
			
			if (!getDatabaseHandler().isKeepAlive())
				getDatabaseHandler().disconnect();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			try {
				getDatabaseHandler().rollback();
			} catch (SQLException e1) {
				this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
			}			
		} 
	}

	public void shutdown() {
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

	public ICallList getCalls(IFilter filter) {
		return this.getCalls(new IFilter[] {filter});
	}

	public ICallList getCalls(IFilter[] filters) {
		return this.getCalls(filters, -1, -1);
	}
	
	public ICallList getCalls(IFilter[] filters, int count, int offset) {
		return this.getCalls(filters, count, offset, null);
	}
	

	public synchronized ICallList getCalls(IFilter[] filters, int count, int offset, ISearchTerm[] searchTerms) {
		try {
			ICallList cl = getDatabaseHandler().getCallList(filters, count, offset, searchTerms);
			if (!getDatabaseHandler().isKeepAlive())
				getDatabaseHandler().disconnect();
			
			return cl;
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return this.getRuntime().getCallFactory().createCallList();
	}
	
	/**
	 * Creates a new instance of a specific database handler.
	 * 
	 * @return a valid database handler, must not be null.
	 */
	protected abstract ICallDatabaseHandler getDatabaseHandler();

	
	public synchronized int getCallCount(IFilter[] filters) {
		return this.getCallCount(filters, null);
	}
	
	public synchronized int getCallCount(IFilter[] filters, ISearchTerm[] searchTerms) {
		try {
			int ccount = getDatabaseHandler().getCallCount(filters, searchTerms);
			if (!getDatabaseHandler().isKeepAlive())
				getDatabaseHandler().disconnect();
			
			return ccount;
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return -1;
	}

}
