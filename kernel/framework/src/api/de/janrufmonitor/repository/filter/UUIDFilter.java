package de.janrufmonitor.repository.filter;

/**
 * This class is a UUID filter.
 * 
 *@author     Thilo Brandt
 *@created    2004/07/17
 */
public class UUIDFilter extends AbstractFilter {

	/**
	 * Creates a new UUID filter object.
	 * @param c a valid UUID
	 */
	public UUIDFilter(String[] c) {
		super();
		this.m_filter = c;
		this.m_type = FilterType.UUID;
	}
	
	/**
	 * Gets the UUID to be filtered.
	 * 
	 * @return a valid string array object.
	 */
	public String[] getUUID() {
		return (String[])this.m_filter;
	}

	public String toString() {
		return UUIDFilter.class.getName()+"#"+this.m_filter;
	}
}
