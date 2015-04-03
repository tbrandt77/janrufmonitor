package de.janrufmonitor.repository.filter;

/**
 * This class defines several FilterTypes for an IFilter implementation.
 * Currenty the following FilterTypes are supported:
 * <ol>
 * <li>FilterType.UNDEFINED - default value, if nothing is specified
 * <li>FilterType.DATE - a date filter
 * <li>FilterType.CALLER - a caller filter
 * <li>FilterType.MSN - a msn filter
 * <li>FilterType.CIP - a cip filter
 * <li>FilterType.ATTRIBUTE - an attribute filter
 * <li>FilterType.PHONENUMBER - a phonenumber filter
 * <li>FilterType.OBJECT_ARRAY - a filter for generic object arrays
 * <li>FilterType.UUID - a UUID filter
 * <li>FilterType.USERDEFINED - a user defined filter
 * <li>FilterType.ITEMCOUNT - a item count filter
 * <li>FilterType.CHARACTER - a starting character filter of an attribute
 * </ol>
 * The concrete FilterType has to be determined and evaluated within the 
 * filter application/component, e.g. a caller manager.
 * 
 *@author     Thilo Brandt
 *@created    2004/07/17
 */
public class FilterType {

	public static FilterType UNDEFINED = new FilterType(0); 
	public static FilterType DATE = new FilterType(1); 
	public static FilterType CALLER = new FilterType(2); 
	public static FilterType MSN = new FilterType(3); 
	public static FilterType CIP = new FilterType(4); 
	public static FilterType ATTRIBUTE = new FilterType(5); 
	public static FilterType PHONENUMBER = new FilterType(6); 
	public static FilterType OBJECT_ARRAY = new FilterType(7);
	public static FilterType UUID = new FilterType(8); 
	public static FilterType USERDEFINED = new FilterType(9); 
	public static FilterType ITEMCOUNT = new FilterType(10); 
	public static FilterType CHARACTER = new FilterType(11); 
	public static FilterType YEAR = new FilterType(12); 
	public static FilterType MONTH_YEAR = new FilterType(13); 
	
	private int _type;
	
	public FilterType(int type) {
		this._type = type;
	}
	
	public boolean equals(Object o) {
		if (o instanceof FilterType) {
			if (this._type == ((FilterType)o)._type)
				return true;	
		}
		return false;
	}

	public int hashCode() {
		return _type;
	}
	
	public String toString() {
		return Integer.toString(this._type);
	}

}
