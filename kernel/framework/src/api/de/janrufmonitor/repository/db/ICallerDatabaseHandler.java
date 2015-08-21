package de.janrufmonitor.repository.db;

import java.sql.SQLException;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;

public interface ICallerDatabaseHandler extends IDatabaseHandler {

	public void insertOrUpdateCallerList(ICallerList cl) throws SQLException;
	
	public void deleteCallerList(ICallerList cl) throws SQLException;
	
	public boolean existsCaller(ICaller c) throws SQLException;
	
	public ICaller getCaller(IPhonenumber pn) throws SQLException;
	
	public ICallerList getCallerList(IFilter[] filters) throws SQLException;
	
	public ICallerList getCallerList(IFilter[] filters, ISearchTerm[] searchTerms) throws SQLException;
}
