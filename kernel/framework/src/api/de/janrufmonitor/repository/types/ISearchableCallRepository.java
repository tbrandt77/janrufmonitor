package de.janrufmonitor.repository.types;

import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;

/**
 * This type is used for repositories which allow searching in their call information.
 * 
 * @author brandt
 *
 */
public interface ISearchableCallRepository {
	
	/**
	 * Gets a list with all calls of a repository filtered by the
	 * specified filter array. The order of the filtering is done
	 * by the order of the array object.
	 * 
	 * @param f a valid filter array. Array elements must not be null.
	 * @param count number off entries from repository
	 * @param offset starting point in repository
	 * @param searchTerms search terms to be considered while searching in call information
	 * @return list with calls
	 */
	public ICallList getCalls(IFilter[] filters, int count, int offset, ISearchTerm[] searchTerms);

	/**
	 * Gets the number of call entries in the repository for a specific
	 * filter object.
	 * 
	 * @param f a valid filter array. Array elements must not be null.
	 * @param searchTerms search terms to be considered while searching in call information
	 * @return number of calls applied on this filter
	 */
	public int getCallCount(IFilter[] filters, ISearchTerm[] searchTerms);
}
