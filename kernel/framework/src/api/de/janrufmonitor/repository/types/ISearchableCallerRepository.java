package de.janrufmonitor.repository.types;

import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;

/**
 * This type is used for repositories which allow searching in their call information.
 * 
 * @author brandt
 *
 */
public interface ISearchableCallerRepository {
	
	
	/**
	 * Gets a list with all callers of a repository filtered by the
	 * specified filter array. The order of the filtering is done
	 * by the order of the array object.
	 * 
	 * @param f a valid filter array. Array elements must not be null.
	 * @param searchTerms search terms to be considered while searching in caller information
	 * @return list with calls
	 */
	public ICallerList getCallers(IFilter[] filters, ISearchTerm[] searchTerms);
	

}
