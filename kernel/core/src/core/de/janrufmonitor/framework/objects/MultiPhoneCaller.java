package de.janrufmonitor.framework.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IPhonenumber;

public class MultiPhoneCaller extends Caller implements IMultiPhoneCaller {

	private static final long serialVersionUID = -6959809607028817116L;

	private List m_phones;
	
	public MultiPhoneCaller(String uuid, IName name, List phones,
			IAttributeMap attMap) {
		super(uuid, name, (IPhonenumber) phones.get(0), attMap);
		this.m_phones = new ArrayList(1);
		this.m_phones.addAll(phones);
	}

	public void addPhonenumber(IPhonenumber n) {
		if (n!=null && !this.m_phones.contains(n)) this.m_phones.add(n);
	}

	public List getPhonenumbers() {
		return this.m_phones;
	}

	public void setPhonenumbers(List pns) {
		if (this.m_phones!=null) {
			this.m_phones.clear();
		} else {
			this.m_phones = new ArrayList(pns.size());
		}
			
		this.m_phones.addAll(pns);
	}
	
	public void setPhoneNumber(IPhonenumber phonenumber) {
		super.setPhoneNumber(phonenumber);
		this.addPhonenumber(phonenumber);		
	}

	public Object clone() throws CloneNotSupportedException {
		Name cloneName = new Name(this.getName().getFirstname(), this.getName().getLastname(), this.getName().getAdditional());
		
		List clonePhones = new ArrayList(m_phones.size());
		Phonenumber pn = null;
		for (int i=0,j=m_phones.size();i<j;i++) {
			pn = new Phonenumber(((IPhonenumber) m_phones.get(i)).getTelephoneNumber());
			pn.setAreaCode(((IPhonenumber) m_phones.get(i)).getAreaCode());
			pn.setCallNumber(((IPhonenumber) m_phones.get(i)).getCallNumber());
			pn.setIntAreaCode(((IPhonenumber) m_phones.get(i)).getIntAreaCode());
			pn.setClired(((IPhonenumber) m_phones.get(i)).isClired());
			clonePhones.add(pn);
		}
	
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
		
		if (clonePhones.size()==0) clonePhones.add(this.getPhoneNumber());

		MultiPhoneCaller cloneCaller = new MultiPhoneCaller(this.getUUID(), cloneName, clonePhones, cloneAttribs);
		if (clonePhones.size()>0)
			cloneCaller.setPhoneNumber((IPhonenumber) clonePhones.get(0));
		
		return cloneCaller;
	}
	
	public boolean equals(Object c) {
		if (c instanceof MultiPhoneCaller) {
			if (((MultiPhoneCaller)c).getName().equals(this.getName()) &&
					((MultiPhoneCaller)c).getPhonenumbers().hashCode() == this.getPhonenumbers().hashCode() && 
					((MultiPhoneCaller)c).getAttributes().hashCode() == this.getAttributes().hashCode() && 
					((MultiPhoneCaller)c).getUUID().equals(this.getUUID())
				) {
					return true;
				}
		}
		return false;
	}

	public int hashCode() {
		return this.getPhonenumbers().hashCode() + this.getName().hashCode() + this.getAttributes().hashCode() + this.getUUID().hashCode();
	}

	public String toString() {
		StringBuffer callerToString = new StringBuffer();
		callerToString.append("{CALLER: ");
		callerToString.append("[UUID: " + this.getUUID() + "]");
		callerToString.append("[Name: " + this.getName() + "]");
		callerToString.append("[Phonenumbers: " + this.getPhonenumbers() + "]");
		callerToString.append(this.getAttributes().toString());       
		callerToString.append("}");
		return callerToString.toString();
	}


}
