package de.janrufmonitor.repository.filter;

import de.janrufmonitor.framework.IAttribute;

/**
 * This class is a single character filter.
 * 
 *@author     Thilo Brandt
 *@created    2011/07/03
 */
public class CharacterFilter extends AbstractFilter {

	private String m_attribute;
	
	/**
	 * Creates a new character filter object.
	 * @param c a valid char, e.g. a, b, c, d...
	 * @param att attribute as string to be filtered
	 */
	public CharacterFilter(String c, String att) {
		super();
		this.m_filter = c;
		this.m_attribute = att;
		this.m_type = FilterType.CHARACTER;
	}
	
	public CharacterFilter(String c, IAttribute att) {
		this(c, (att!=null ? att.getName(): ""));
	}
	
	/**
	 * Gets the character to be filtered.
	 * 
	 * @return a valid string object.
	 */
	public String getCharacter() {
		return (String)this.m_filter;
	}
	
	public String getAttributeName() {
		return this.m_attribute;
	}

	public String toString() {
		return CharacterFilter.class.getName()+"#"+this.m_attribute+"#"+this.m_filter;
	}
}
