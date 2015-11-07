package de.janrufmonitor.framework;

import java.util.List;

/**
 *  This interface must be implemented by a CallerFactory object, which should be
 *  used in the framework. A CallerFactory should be implemented as a singleton.
 *
 *@author     Thilo Brandt
 *@created    2003/08/10
 */
public interface ICallerFactory {

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  uuid  UUID of the caller
	 *@param  name  name object of the caller
	 *@param  phone  phone number object of the caller
	 *@param  attributes  a attribute list to be set for the caller
	 *@return       a new instance of a caller.
	 */
    public ICaller createCaller(String uuid, IName name, IPhonenumber phone, IAttributeMap attributes);

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  uuid  UUID of the caller
	 *@param  name  name object of the caller
	 *@param  phone  phone number object of the caller
	 *@param  attribute  a attribute to be set for the caller
	 *@return       a new instance of a caller.
	 */
    public ICaller createCaller(String uuid, IName name, IPhonenumber phone, IAttribute attribute);

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  uuid  UUID of the caller
	 *@param  name  name object of the caller
	 *@param  phone  phone number object of the caller
	 *@return       a new instance of a caller.
	 */
    public ICaller createCaller(String uuid, IName name, IPhonenumber phone);

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  name  name object of the caller
	 *@param  phone  phone number object of the caller
	 *@param  attributes  a attribute list to be set for the caller
	 *@return       a new instance of a caller.
	 */
    public ICaller createCaller(IName name, IPhonenumber phone, IAttributeMap attributes);

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  name  name object of the caller
	 *@param  phone  phone number object of the caller
	 *@param  attribute  a attribute to be set for the caller
	 *@return       a new instance of a caller.
	 */
    public ICaller createCaller(IName name, IPhonenumber phone, IAttribute attribute);

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  phone  phone number object of the caller
	 *@return       a new instance of a caller.
	 */
    public ICaller createCaller(IPhonenumber phone);
    
	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  name  name object of the caller
	 *@param  phone  phone number object of the caller
	 *@return       a new instance of a caller.
	 */
    public ICaller createCaller(IName name, IPhonenumber phone);

	/**
	 *  Create a new name object with the specific data.
	 *
	 *@param  firstname  firstname of the name object
	 *@param  lastname  lastname of the name object
	 *@param  additional  additional information of the name object
	 *@return       a new instance of a name object.
	 */
    public IName createName(String firstname, String lastname, String additional);

	/**
	 *  Create a new name object with the specific data.
	 *
	 *@param  firstname  firstname of the name object
	 *@param  lastname  lastname of the name object
	 *@return       a new instance of a name object.
	 */
    public IName createName(String firstname, String lastname);

	/**
	 *  Create a new phone number object with the specific data.
	 *
	 *@param  intAreaCode  international area code of the phone number object
	 *@param  areaCode     local area code of the phone number object
	 *@param  number 	   call number of the phone number object
	 *@return       a new instance of a phone number object.
	 */
    public IPhonenumber createPhonenumber(String intAreaCode, String areaCode, String number);

	/**
	 *  Create a new phone number object with the specific data.
	 *
	 *@param  telephoneNumber  complete telephonenumber of the phone number object
	 *@return       a new instance of a phone number object.
	 */
    public IPhonenumber createPhonenumber(String telephoneNumber);

	/**
	 *  Create a new phone number object with the specific data.
	 *
	 *@param  isClired  true if the phone object should be clired (CLIR).
	 *@return       a new instance of a phone number object.
	 */
    public IPhonenumber createPhonenumber(boolean isClired);

	/**
	 *  Create a new phone number object with CLIR attribute.
	 *
	 *@return       a new instance of a CLIR phone number object.
	 */
    public IPhonenumber createClirPhonenumber();

	/**
	 *  Create a new phone number object with INTERNAL attribute.
	 *
	 *@return       a new instance of a INTERNAL phone number object.
	 */
    public IPhonenumber createInternalPhonenumber(String internalNumber);
    
	/**
	 *  Create an attribute with the specific data.
	 *
	 *@param  name  name of the attribute.
	 *@param  value  value of the attribute.
	 *@return       a new instance of an attribute.
	 */
    public IAttribute createAttribute(String name, String value);
    
	/**
	 *  Create an empty attribute map.
	 *
	 *@return       a new instance of an attribute map.
	 */
    public IAttributeMap createAttributeMap();

	/**
	 *  Create an empty caller list. Initial capacity is set to 1.
	 *
	 *@return       a new instance of an caller list.
	 */
    public ICallerList createCallerList();

	/**
	 *  Create an empty caller list with an initial capacity.
	 *
	 *@return       a new instance of an caller list.
	 */
    public ICallerList createCallerList(int capacity);

	/**
	 *  Create a new multi phone caller object with the specific data.
	 *
	 *@param  uuid  UUID of the caller
	 *@param  name  name object of the caller
	 *@param  phones  a list of phone number objects of the caller
	 *@param  attributes  a attribute list to be set for the caller
	 *@return       a new instance of a caller.
	 */
    public IMultiPhoneCaller createCaller(String uuid, IName name, List phones, IAttributeMap attributes);

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  name  name object of the caller
	 *@param  phones  a list of phone number objects of the caller
	 *@param  attributes  a attribute list to be set for the caller
	 *@return       a new instance of a caller.
	 */
    public IMultiPhoneCaller createCaller(IName name, List phones, IAttributeMap attributes);

	/**
	 *  Create a new caller object with the specific data.
	 *
	 *@param  name  name object of the caller
	 *@param  phones  a list of phone number objects of the caller
	 *@return       a new instance of a caller.
	 */
    public IMultiPhoneCaller createCaller(IName name, List phones);
    
    /**
     * Transforms a ICaller objects to a IMultiPhoneCaller object. 
     * The new IMultiPhoneCaller object gets a new UUID (!).
     * 
     * @param caller  caller to be transformed
     * @return a new IMultiPhoneCaller instance
     */
    public IMultiPhoneCaller toMultiPhoneCaller(ICaller caller);
    
}
