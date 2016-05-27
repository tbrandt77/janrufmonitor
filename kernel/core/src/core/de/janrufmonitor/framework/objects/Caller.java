package de.janrufmonitor.framework.objects;

import java.io.Serializable;
import java.util.Iterator;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;

public class Caller implements ICaller, Serializable {

	private static final long serialVersionUID = -6649677147340734757L;
	
	private IPhonenumber m_phonenumber;
	private IAttributeMap m_attributeMap;
	private String m_uuid;
	
	public Caller(String uuid, IName name, IPhonenumber phonenumber, IAttributeMap attMap) {
		this.m_uuid = uuid;
		this.m_phonenumber = phonenumber;
		this.m_attributeMap = attMap;
		this.setName(name);
	}

	public void setUUID(String uuid) {
		this.m_uuid = uuid;
	}

	public void setPhoneNumber(IPhonenumber phonenumber) {
		this.m_phonenumber = phonenumber;
	}

	public void setName(IName name) {
		if (name==null) return ;
		this.setAttribute(new Attribute(
			IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
			name.getFirstname()
		));
		this.setAttribute(new Attribute(
			IJAMConst.ATTRIBUTE_NAME_LASTNAME,
			name.getLastname()
		));
		this.setAttribute(new Attribute(
			IJAMConst.ATTRIBUTE_NAME_ADDITIONAL,
			name.getAdditional()
		));	
	}

	public void setAttribute(IAttribute att) {
		if (this.m_attributeMap != null)
			this.m_attributeMap.add(att);
	}

	public void setAttributes(IAttributeMap attList) {
		this.m_attributeMap = attList;
	}

	public String getUUID() {
		return this.m_uuid;
	}

	public IPhonenumber getPhoneNumber() {
		return (this.m_phonenumber==null ? new Phonenumber(true) : this.m_phonenumber);
	}

	public IName getName() {
		return new Name(
			(this.m_attributeMap.contains(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME) ? this.m_attributeMap.get(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue() : ""),
			(this.m_attributeMap.contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME) ? this.m_attributeMap.get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue() : ""),
			(this.m_attributeMap.contains(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL) ? this.m_attributeMap.get(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL).getValue() : "")
		);
	}

	public IAttribute getAttribute(String attName) {
		return this.m_attributeMap.get(attName);
	}

	public IAttributeMap getAttributes() {
		return this.m_attributeMap;
	}
	
	public Object clone() throws CloneNotSupportedException {
		Name cloneName = new Name(this.getName().getFirstname(), this.getName().getLastname(), this.getName().getAdditional());
		Phonenumber clonePhone = new Phonenumber(this.getPhoneNumber().getTelephoneNumber());
		clonePhone.setAreaCode(this.getPhoneNumber().getAreaCode());
		clonePhone.setCallNumber(this.getPhoneNumber().getCallNumber());
		clonePhone.setIntAreaCode(this.getPhoneNumber().getIntAreaCode());
		clonePhone.setClired(this.getPhoneNumber().isClired());
		AttributeMap cloneAttribs = new AttributeMap(this.getAttributes().size());
		
		Iterator i = this.getAttributes().iterator();
		IAttribute att = null;
		while(i.hasNext()) {
			att = (IAttribute)i.next();
			Attribute cloneAttrib =
					new Attribute(att.getName(),
							      att.getValue());
			cloneAttribs.add(cloneAttrib);
		}

		Caller cloneCaller = new Caller(this.getUUID(), cloneName, clonePhone, cloneAttribs);
		return cloneCaller;
	}   

	public boolean equals(Object c) {
		if (c instanceof Caller) {
			if (((Caller)c).getName().equals(this.getName()) &&
				((Caller)c).getUUID().hashCode() == this.m_uuid.hashCode() &&
				((Caller)c).getPhoneNumber().equals(this.getPhoneNumber())
				) {
					return true;
				}
		}
		return false;
	}

	public int hashCode() {
		return this.getPhoneNumber().hashCode() + this.getName().hashCode() + this.m_uuid.hashCode();
	}

	public String toString() {
		StringBuffer callerToString = new StringBuffer();
		callerToString.append("{CALLER: ");
		callerToString.append("[UUID: " + this.getUUID() + "]");
		callerToString.append("[Name: " + this.getName() + "]");
		callerToString.append("[Phonenumber: " + this.getPhoneNumber() + "]");
		callerToString.append(this.m_attributeMap.toString());       
		callerToString.append("}");
		return callerToString.toString();
	}

}
