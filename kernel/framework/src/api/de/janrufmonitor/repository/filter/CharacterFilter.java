package de.janrufmonitor.repository.filter;

/**
 * This class is a single character filter.
 * 
 *@author     Thilo Brandt
 *@created    2011/07/03
 */
public class CharacterFilter extends AbstractFilter {

	/**
	 * Creates a new character filter object.
	 * @param c a valid char, e.g. a, b, c, d...
	 */
	public CharacterFilter(String c) {
		super();
		this.m_filter = c;
		this.m_type = FilterType.CHARACTER;
	}
	
	/**
	 * Gets the character to be filtered.
	 * 
	 * @return a valid string object.
	 */
	public String getCharacter() {
		return (String)this.m_filter;
	}

	public String toString() {
		return CharacterFilter.class.getName()+"#"+this.m_filter;
	}
}
