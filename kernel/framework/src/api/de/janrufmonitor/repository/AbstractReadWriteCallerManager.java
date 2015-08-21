package de.janrufmonitor.repository;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.types.IWriteCallerRepository;

/**
 *  This abstract class can be used as base class for a new caller manager implementation which
 *  is supporting configuration.
 *
 *@author     Thilo Brandt
 *@created    2003/11/02
 */
public abstract class AbstractReadWriteCallerManager extends AbstractConfigurableCallerManager implements IIdentifyCallerRepository, IReadCallerRepository, IWriteCallerRepository {

	public AbstractReadWriteCallerManager() {
		super();
	}

	public ICallerList getCallers(IFilter[] filters) {
		if (filters!=null && filters.length>0)
			return this.getCallers(filters[0]);
		return this.getCallers((IFilter)null);
	}

	/**
	 * Updates a caller with the new data. The caller to be updated has to
	 * be determined through its UUID. In this abstract implementation
	 * the updateCaller() method calls the setCaller() method.
	 * 
	 * @param caller caller to be updated
	 */
	public void updateCaller(ICaller caller) {
		this.setCaller(caller);
	}

	public boolean isSupported(Class c) {
		return c.isInstance(this);
	}

	public void setCaller(ICallerList callerList) {
		ICaller c = null;
		for (int i=0,n=callerList.size();i<n;i++) {
			c = callerList.get(i);
			this.addCreationAttributes(c);
			this.addSystemAttributes(c);
			this.setCaller(c);	
		}
	}

	public void removeCaller(ICallerList callerList) {
		for (int i=0,n=callerList.size();i<n;i++) {
			this.removeCaller(callerList.get(i));	
		}
	}

}
