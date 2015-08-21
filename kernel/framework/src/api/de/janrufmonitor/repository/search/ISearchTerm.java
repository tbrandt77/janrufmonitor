package de.janrufmonitor.repository.search;

/**
 * This interface is representing a search term for searching in various contexts
 * 
 * @author brandt
 *
 */
public interface ISearchTerm {
	
	public String getSearchTerm();
	
	public Operator getOperator();
	
}
