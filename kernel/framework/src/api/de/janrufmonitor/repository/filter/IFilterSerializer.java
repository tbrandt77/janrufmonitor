package de.janrufmonitor.repository.filter;

/**
 * This interface should be implemented by a class which
 * offeres serializer capabilities for repository filters.
 * 
 *@author     Thilo Brandt
 *@created    2005/06/12
 */
public interface IFilterSerializer {
	
	public IFilter getFilterFromString(String fstring);
	
	public IFilter[] getFiltersFromString(String s);
	
	public String getFilterToString(IFilter f);
	
	public String getFiltersToString(IFilter[] filters);
	
	public boolean hasRuntimeFilters();
	
	public IFilter[][] getRuntimeFilters();
	
}
