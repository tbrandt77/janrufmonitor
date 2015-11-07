package de.janrufmonitor.framework;

import java.util.ArrayList;
import java.util.List;

import de.janrufmonitor.framework.objects.AttributeMap;
import de.janrufmonitor.framework.objects.MultiPhoneCaller;
import de.janrufmonitor.util.uuid.UUID;

public class CallerFactory implements ICallerFactory {

    private static CallerFactory m_instance = null;

    private CallerFactory() { }
    
    public static synchronized CallerFactory getInstance() {
        if (CallerFactory.m_instance == null) {
            CallerFactory.m_instance = new CallerFactory();
        }
        return CallerFactory.m_instance;
    }
    
    public IAttribute createAttribute(String name, String value) {
        return new de.janrufmonitor.framework.objects.Attribute(name, value);
    }
    
	public IAttributeMap createAttributeMap() {
		return new AttributeMap();
	}
    
    public ICaller createCaller(IPhonenumber phone) {
        return createCaller(new UUID().toString(), null, phone, new AttributeMap());
    }
	
    public ICaller createCaller(IName name, IPhonenumber phone) {
        return createCaller(new UUID().toString(), name, phone, new AttributeMap());
    }
    
    public ICaller createCaller(IName name, IPhonenumber phone, IAttribute attribute) {
        return createCaller(new UUID().toString(), name, phone, attribute);
    }
    
    public ICaller createCaller(IName name, IPhonenumber phone, IAttributeMap attributes) {
        return createCaller(new UUID().toString(), name, phone, attributes);
    }
    
    public ICaller createCaller(String uuid, IName name, IPhonenumber phone) {
        return createCaller(uuid, name, phone, new AttributeMap());
    }
    
    public ICaller createCaller(String uuid, IName name, IPhonenumber phone, IAttribute attribute) {
    	AttributeMap attList = new AttributeMap();
        attList.add(attribute);
        return createCaller(uuid, name, phone, attList);
    }
    
    public ICaller createCaller(String uuid, IName name, IPhonenumber phone, IAttributeMap attributes) {
        return new de.janrufmonitor.framework.objects.Caller(uuid, name, phone, attributes);
    }
    
    public IName createName(String firstname, String lastname) {
        return createName(firstname, lastname, ""); 
    }
    
    public IName createName(String firstname, String lastname, String additional) {
        return new de.janrufmonitor.framework.objects.Name(firstname, lastname, additional);
    }
    
    public IPhonenumber createClirPhonenumber() {
    	return createPhonenumber(true);
    }
    
    public IPhonenumber createInternalPhonenumber(String internalNumber) {
    	return createPhonenumber(IJAMConst.INTERNAL_CALL, "", internalNumber);
    }
    
    public IPhonenumber createPhonenumber(boolean isClired) {
        return new de.janrufmonitor.framework.objects.Phonenumber(isClired);
    }
    
    public IPhonenumber createPhonenumber(String telephoneNumber) {
        return new de.janrufmonitor.framework.objects.Phonenumber(telephoneNumber);
    }
    
    public IPhonenumber createPhonenumber(String intAreaCode, String areaCode, String number) {
        return new de.janrufmonitor.framework.objects.Phonenumber(intAreaCode, areaCode, number);
    }
    
    public ICallerList createCallerList() {
        return new de.janrufmonitor.framework.objects.CallerList();
    }
    
    public ICallerList createCallerList(int capacity) {
        return new de.janrufmonitor.framework.objects.CallerList(capacity);
    }

	public IMultiPhoneCaller createCaller(IName name, List phones) {
		return createCaller(new UUID().toString(), name, phones, new AttributeMap());
	}

	public IMultiPhoneCaller createCaller(IName name, List phones, IAttributeMap attributes) {
		return createCaller(new UUID().toString(), name, phones, attributes);
	}

	public IMultiPhoneCaller createCaller(String uuid, IName name, List phones, IAttributeMap attributes) {
		if (phones==null) phones = new ArrayList();
		if (phones.size()==0) phones.add(createPhonenumber(true));
		return new MultiPhoneCaller(uuid, name, phones, attributes);
	}

	public IMultiPhoneCaller toMultiPhoneCaller(ICaller caller) {
		List phones = new ArrayList(1);
		phones.add(caller.getPhoneNumber());
		
		return createCaller(caller.getName(), phones, caller.getAttributes());
	}


}
