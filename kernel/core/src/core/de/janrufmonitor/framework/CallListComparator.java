package de.janrufmonitor.framework;

import java.util.Comparator;

import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;

public class CallListComparator implements Comparator {

    public final static int ORDER_DATE = 0;

    public final static int ORDER_MSN = 1;

    public final static int ORDER_CALLERNAME = 2;

    
    public final static int ORDER_CALLERNUMBER = 3;
    
    public final static int ORDER_CIP = 4;
    
    public final static int ORDER_CATEGORY = 8;

    private int order = 0;
    
	private Formatter m_f;

    public CallListComparator(int order) {
        this.order = order;
    }

    public int compare(Object obj, Object obj1) {
        ICall aCall = (ICall) obj;
        ICall bCall = (ICall) obj1;

        switch (order) {
            case ORDER_DATE:
                if (aCall.getDate().getTime() < bCall.getDate().getTime()) {
                    return -1;
                }
                if (aCall.getDate().getTime() > bCall.getDate().getTime()) {
                    return 1;
                }
                break;
            case ORDER_MSN:
                return aCall.getMSN().getMSN().compareTo(bCall.getMSN().getMSN());
            case ORDER_CALLERNAME:
            	IName aName = aCall.getCaller().getName();
            	IName bName = bCall.getCaller().getName();
            	return this.getCompareName(aName).compareTo(this.getCompareName(bName));
            case ORDER_CALLERNUMBER:
               	IPhonenumber aNum = aCall.getCaller().getPhoneNumber();
            	IPhonenumber bNum = bCall.getCaller().getPhoneNumber();
                return this.getCompareNumber(aNum).compareTo(this.getCompareNumber(bNum));
            case ORDER_CIP:
                return aCall.getCIP().getCIP().compareTo(bCall.getCIP().getCIP());
            case ORDER_CATEGORY:
            	IAttribute cata = aCall.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY);
            	IAttribute catb = bCall.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY);
            	if (cata==null && catb==null) return 0;
                if (cata==null && catb!=null) return -1;
                if (cata!=null && catb==null) return 1;
                return cata.getValue().compareTo(catb.getValue());
        }

        return 0;
    }
    
    private String getCompareNumber(IPhonenumber pn) {
    	return this.getFormatter().parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, pn);
    }
	
    private String getCompareName(IName n) {
    	return this.getFormatter().parse(IJAMConst.GLOBAL_VARIABLE_CALLERNAME, n);
    }
    
	private Formatter getFormatter() {
		if (this.m_f==null) {
			this.m_f = Formatter.getInstance(PIMRuntime.getInstance());
		}
		return this.m_f;
	}
}
