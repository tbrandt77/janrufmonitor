package de.janrufmonitor.framework;

import de.janrufmonitor.framework.objects.AttributeMap;
import de.janrufmonitor.util.uuid.UUID;

import java.util.Date;

public class CallFactory implements ICallFactory {
    
    private static CallFactory m_instance = null;

    private CallFactory() { }
    
    public static synchronized CallFactory getInstance() {
        if (CallFactory.m_instance == null) {
            CallFactory.m_instance = new CallFactory();
        }
        return CallFactory.m_instance;
    }
    
    public IAttribute createAttribute(String name, String value) {
        return new de.janrufmonitor.framework.objects.Attribute(name, value);
    }
    
    public IAttributeMap createAttributeMap() {
        return new AttributeMap();
    }
    
    public ICall createCall(ICaller caller, IMsn msn, ICip cip, Date date) {
        return createCall(new UUID().toString(), caller, msn, cip, date, new AttributeMap());
    }
    
    public ICall createCall(String uuid, ICaller caller, IMsn msn, ICip cip, Date date, IAttributeMap attributeList) {
        return new de.janrufmonitor.framework.objects.Call(uuid, caller, msn, cip, date, attributeList);
    }
    
    public ICall createCall(String uuid, ICaller caller, IMsn msn, ICip cip, Date date, IAttribute attribute) {
    	AttributeMap attributeList = new AttributeMap();
        attributeList.add(attribute);
        return createCall(uuid, caller, msn, cip, date, attributeList);
    }
    
    public ICall createCall(String uuid, ICaller caller, IMsn msn, ICip cip, Date date){
        return createCall(uuid, caller, msn, cip, date, new AttributeMap());
    }
    
    public ICall createCall(ICaller caller, IMsn msn, ICip cip) {
        return createCall(new UUID().toString(), caller, msn, cip, new Date(), new AttributeMap());
    }
    
    public ICip createCip(String cip, String additional) {
        return new de.janrufmonitor.framework.objects.Cip(cip, additional);
    }
    
    public IMsn createMsn(String msn, String additional) {
        return new de.janrufmonitor.framework.objects.Msn(msn, additional);
    }

    public ICallList createCallList() {
        return new de.janrufmonitor.framework.objects.CallList();
    }    
    
    public ICallList createCallList(int capacity) {
        return new de.janrufmonitor.framework.objects.CallList(capacity);
    }
    
}
