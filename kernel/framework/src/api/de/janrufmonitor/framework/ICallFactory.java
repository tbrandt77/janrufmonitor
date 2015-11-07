package de.janrufmonitor.framework;

import java.util.Date;

/**
 *  This interface must be implemented by a CallFactory object, which should be
 *  used in the framework. A CallFactory should be implemented as a singleton.
 *
 *@author     Thilo Brandt
 *@created    2003/08/10
 */
public interface ICallFactory {

	/**
	 *  Create a new call object with the specific data.
	 *
	 *@param  uuid  UUID of the call
	 *@param  caller  caller object of the call
	 *@param  msn  MSN of the call
	 *@param  cip  CIP of the call
	 *@param  date  date of the call
	 *@param  attribute  a attribute to be set for the call
	 *@return       a new instance of a call.
	 */
    public ICall createCall(String uuid, ICaller caller, IMsn msn, ICip cip, Date date, IAttribute attribute);

	/**
	 *  Create a new call object with the specific data.
	 *
	 *@param  uuid  UUID of the call
	 *@param  caller  caller object of the call
	 *@param  msn  MSN of the call
	 *@param  cip  CIP of the call
	 *@param  date  date of the call
	 *@param  attributes  a attribute list to be set for the call
	 *@return       a new instance of a call.
	 */
    public ICall createCall(String uuid, ICaller caller, IMsn msn, ICip cip, Date date, IAttributeMap attributeMap);

	/**
	 *  Create a new call object with the specific data.
	 *
	 *@param  uuid  UUID of the call
	 *@param  caller  caller object of the call
	 *@param  msn  MSN of the call
	 *@param  cip  CIP of the call
	 *@param  date  date of the call
	 *@return       a new instance of a call.
	 */
    public ICall createCall(String uuid, ICaller caller, IMsn msn, ICip cip, Date date);

	/**
	 *  Create a new call object with the specific data.
	 *
	 *@param  caller  caller object of the call
	 *@param  msn  MSN of the call
	 *@param  cip  CIP of the call
	 *@param  date  date of the call
	 *@return       a new instance of a call.
	 */
    public ICall createCall(ICaller caller, IMsn msn, ICip cip, Date date);

	/**
	 *  Create a new call object with the specific data.
	 *
	 *@param  caller  caller object of the call
	 *@param  msn  MSN of the call
	 *@param  cip  CIP of the call
	 *@return       a new instance of a call.
	 */
    public ICall createCall(ICaller caller, IMsn msn, ICip cip);

	/**
	 *  Create a new msn object with the specific data.
	 *
	 *@param  msn  MSN value of the msn object
	 *@param  additional  additional information of the msn object
	 *@return       a new instance of a msn object.
	 */
    public IMsn createMsn(String msn, String additional);

	/**
	 *  Create a new cip object with the specific data.
	 *
	 *@param  cip  CIP value of the msn object
	 *@param  additional  additional information of the cip object
	 *@return       a new instance of a cip object.
	 */
    public ICip createCip(String cip, String additional);

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
	 *  Create an empty call list. Initial capacity is set to 1.
	 *
	 *@return       a new instance of an call list.
	 */
    public ICallList createCallList();

	/**
	 *  Create an empty call list with an initial capacity.
	 *
	 *@return       a new instance of an call list.
	 */
    public ICallList createCallList(int capacity);

}
