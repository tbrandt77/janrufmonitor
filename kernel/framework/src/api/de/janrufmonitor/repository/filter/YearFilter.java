package de.janrufmonitor.repository.filter;

import java.util.Calendar;
import java.util.Date;

/**
 * This class is a year filter which can be used as 
 * a "from-to date filter" or a "to date filter".<br><br>
 * <code>IFilter df = new YearFilter(2015);</code> - 
 * filter is used as a "yearly filter."
 * 
 *@author     Thilo Brandt
 *@created    2015/04/02
 */
public final class YearFilter extends AbstractFilter {

	private int m_Year;

	/**
	 * Creates a new date filter with the specified dates.
	 * 
	 * @param dateFrom beginning date
	 * @param dateTo end date
	 */
	public YearFilter(int year) {
		super();
		this.m_filter = new Integer(year);
		this.m_Year = year;
		this.m_type = FilterType.YEAR;
	}
	
	/**
	 * Gets the from date argument.
	 * 
	 * @return a valid date object or null.
	 */
	public Date getDateFrom() {
		Calendar c = Calendar.getInstance();
		c.set(this.m_Year, Calendar.JANUARY, 1, 0, 0, 0);
		return c.getTime();
	}
	
	/**
	 * Gets the to date argument.
	 * 
	 * @return a valid date object.
	 */
	public Date getDateTo() {
		Calendar c = Calendar.getInstance();
		c.set((this.m_Year+1), Calendar.JANUARY, 1, 0, 0, 0);
		return c.getTime();
	}
	
	public int getYear() {
		return this.m_Year;
	}


	public String toString() {
		return "RTYearFilter#"+this.m_Year+".13";
	}
}
