package de.janrufmonitor.repository.filter;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.runtime.IRuntime;

/**
 * This class is an abstract implementation of a filter
 * serializer. It has to be implemented by a concrete class.
 * 
 *@author     Thilo Brandt
 *@created    2005/06/12
 */
public abstract class AbstractFilterSerializer {

	/**
	 * Transforms a String representation of a filter into an IFilter object.
	 * @param fstring string representation of the filter
	 * @return a valid IFilter object or null, if string is invalid.
	 */
	public IFilter getFilterFromString(String fstring) {
		//IFilter filter = null;
		if (fstring.length()>0) {
			StringTokenizer st = new StringTokenizer(fstring, ",");
			String token = null;
			if (st.countTokens()>0) {
				token = st.nextToken().trim();
				FilterType ft = new FilterType(Integer.parseInt(token));
				if (ft.equals(FilterType.DATE)) {
					Date d1 = null;
					long dl1 = Long.parseLong(st.nextToken());
					if (dl1>0)
						d1 = new Date(dl1);
					Date d2 = new Date(Long.parseLong(st.nextToken()));
					long frame = -1;
					if (st.hasMoreTokens())
						frame = Long.parseLong(st.nextToken());
					
					// calculate today
					if (frame==-100) {
						Calendar c = Calendar.getInstance();

						c.set(Calendar.HOUR_OF_DAY, 0);
						c.set(Calendar.MINUTE, 0);
						c.set(Calendar.SECOND, 0);
							
						d2 = c.getTime();
						
						c.set(Calendar.HOUR_OF_DAY, 23);
						c.set(Calendar.MINUTE, 59);
						c.set(Calendar.SECOND, 0);
						
						d1 = c.getTime();
						return new DateFilter(d1,d2,frame);
					}
					if (frame==-101) {
						Calendar c = Calendar.getInstance();
						c.setFirstDayOfWeek(Calendar.MONDAY);
					
						c.set(Calendar.DAY_OF_WEEK, c.get(Calendar.DAY_OF_WEEK)-1);
						// 2008/03/25: fixed sunday switch bug
						if (c.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY) {
							c.add(Calendar.WEEK_OF_MONTH, -1);
							c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						}
							
						c.set(Calendar.HOUR_OF_DAY, 0);
						c.set(Calendar.MINUTE, 0);
						c.set(Calendar.SECOND, 0);
							
						d2 = c.getTime();
						
						c.set(Calendar.HOUR_OF_DAY, 23);
						c.set(Calendar.MINUTE, 59);
						c.set(Calendar.SECOND, 0);
						
						d1 = c.getTime();
						return new DateFilter(d1,d2,frame);
					}
					if (frame==-107) {
						Calendar c = Calendar.getInstance();

						c.set(Calendar.HOUR_OF_DAY, 23);
						c.set(Calendar.MINUTE, 59);
						c.set(Calendar.SECOND, 0);

						d1 = c.getTime();
						
						c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
						c.set(Calendar.HOUR_OF_DAY, 0);
						c.set(Calendar.MINUTE, 0);
						c.set(Calendar.SECOND, 0);
						
						d2 = c.getTime();
						return new DateFilter(d1,d2,frame);
					}
					if (frame==-108) {
						Calendar c = Calendar.getInstance();

						c.add(Calendar.WEEK_OF_MONTH, -1);
						c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						c.set(Calendar.HOUR_OF_DAY, 23);
						c.set(Calendar.MINUTE, 59);
						c.set(Calendar.SECOND, 0);
							
						d1 = c.getTime();
						
						c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
						c.set(Calendar.HOUR_OF_DAY, 0);
						c.set(Calendar.MINUTE, 0);
						c.set(Calendar.SECOND, 0);
						
						d2 = c.getTime();
						return new DateFilter(d1,d2,frame);
					}
					if (frame==-130) {
						Calendar c = Calendar.getInstance();

						c.set(Calendar.HOUR_OF_DAY, 23);
						c.set(Calendar.MINUTE, 59);
						c.set(Calendar.SECOND, 0);
							
						d1 = c.getTime();	
						
						c.set(Calendar.DAY_OF_MONTH, 1);
						c.set(Calendar.HOUR_OF_DAY, 0);
						c.set(Calendar.MINUTE, 0);
						c.set(Calendar.SECOND, 0);
						
						d2 = c.getTime();
						return new DateFilter(d1,d2,frame);
					}
					if (frame==-131) {
						Calendar c = Calendar.getInstance();

						c.add(Calendar.MONTH, -1);
						c.set(Calendar.HOUR_OF_DAY, 23);
						c.set(Calendar.MINUTE, 59);
						c.set(Calendar.SECOND, 0);
						c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));	
						
						d1 = c.getTime();
						
						c.set(Calendar.DAY_OF_MONTH, 1);
						c.set(Calendar.HOUR_OF_DAY, 0);
						c.set(Calendar.MINUTE, 0);
						c.set(Calendar.SECOND, 0);
						
						d2 = c.getTime();
						return new DateFilter(d1,d2,frame);
					}	
					if (frame>-1) 
						return new DateFilter(frame);
					
					return new DateFilter(d1,d2);
				}
				if (ft.equals(FilterType.MSN)) {
					IMsn[]msns = new IMsn[st.countTokens()];
					int i = 0;
					while(st.hasMoreTokens()) {
						msns[i] = this.getRuntime().getCallFactory().createMsn(st.nextToken(), "");
						i++;
					}
					return new MsnFilter(msns);
				}
				if (ft.equals(FilterType.CIP)) {
					String cip = st.nextToken();
					return new CipFilter(this.getRuntime().getCallFactory().createCip(cip, ""));
				}
				if (ft.equals(FilterType.CALLER)) {
					String caller = st.nextToken();
					if (caller.equalsIgnoreCase("clired")) {
						IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(true);
						ICaller c = getRuntime().getCallerFactory().createCaller(
							getRuntime().getCallerFactory().createName("", ""),
							pn
						);
						return new CallerFilter(c);
					} 
					
					if (caller.equalsIgnoreCase(IJAMConst.INTERNAL_CALL)) {
						IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(
								caller,
								"",
								st.nextToken()
							);
							ICaller c = getRuntime().getCallerFactory().createCaller(
								getRuntime().getCallerFactory().createName("", ""),
								pn
							);
							return new CallerFilter(c);
					}
					else {
						IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(
							caller,
							st.nextToken(),
							st.nextToken()
						);
						ICaller c = getRuntime().getCallerFactory().createCaller(
							getRuntime().getCallerFactory().createName("", ""),
							pn
						);
						return new CallerFilter(c);
					}
				}
				if (ft.equals(FilterType.PHONENUMBER)) {
					String intarea = st.nextToken();
					String area = "";
					String number = "";
					if (st.hasMoreTokens())
						area = st.nextToken().trim();
					if (area.equalsIgnoreCase("+")) area = "";
					if (st.hasMoreTokens())
						number = st.nextToken().trim();	
					if (number.equalsIgnoreCase("+")) number = "";
					IPhonenumber pn = this.getRuntime().getCallerFactory().createPhonenumber(intarea, area, number);
					return new PhonenumberFilter(pn);
				}		
				if (ft.equals(FilterType.ITEMCOUNT)) {
					String limit = st.nextToken().trim();
					return new ItemCountFilter(Integer.parseInt(limit));
				}		
				if (ft.equals(FilterType.ATTRIBUTE)) {
					IAttributeMap m = getRuntime().getCallFactory().createAttributeMap();
					//return new AttributeFilter(m);
					String[] t = null;
					while(st.hasMoreTokens()) {
						t = st.nextToken().split("=");
						m.add(getRuntime().getCallFactory().createAttribute(t[0], t[1]));
					}
					return new AttributeFilter(m);
				}	
				if (ft.equals(FilterType.CHARACTER)) {
					String[] t = st.nextToken().split("=");
					return new CharacterFilter(t[1].trim(), t[0].trim());
				}	
				if (ft.equals(FilterType.UUID)) {
					String[] uuids = st.nextToken().split(";");
					return new UUIDFilter(uuids);
				}	
				// TODO: more filters to be added here
			}
		}
		return null;
	}
	
	/**
	 * Transforms a String representation of a list of filters into an IFilter[] object.
	 * @param s string representation of a filter list
	 * @return a valid IFilter[] object or null, if string is invalid.
	 */
	public IFilter[] getFiltersFromString(String s){
		StringTokenizer st = new StringTokenizer(s, "(");
		IFilter[] filters = new IFilter[st.countTokens()];
		int i=0;
		while (st.hasMoreTokens()) {
			String filter = st.nextToken();
			filters[i] = this.getFilterFromString(filter.substring(0, filter.length()-1));
			i++;
		}
		return filters;
	}
	
	/**
	 * Transforms a IFilter object into a string representation
	 * @param f a valid IFilter object, should not be null
	 * @return a string representation of the filter.
	 */
	public String getFilterToString(IFilter f) {
		StringBuffer sb = new StringBuffer();
		if (f!=null) {
			FilterType ft = f.getType();
			if (ft.equals(FilterType.DATE)) {
				sb.append(ft.toString());
				sb.append(",");
				if (((DateFilter)f).getTimeframe()==-1) {
					Date d = ((DateFilter)f).getDateFrom();
					if (d==null)
						sb.append(-1);
					else
						sb.append(d.getTime());
					sb.append(",");
					sb.append(((DateFilter)f).getDateTo().getTime());
				} else {
					sb.append("0");
					sb.append(",");
					sb.append("0");
					sb.append(",");
					sb.append(((DateFilter)f).getTimeframe());
				}
			}
			
			if (ft.equals(FilterType.CIP)) {
				sb.append(ft.toString());
				sb.append(",");
				sb.append(((CipFilter)f).getCip().getCIP());
			}
			
			if (ft.equals(FilterType.MSN)) {
				sb.append(ft.toString());
				sb.append(",");
				IMsn[] msns = ((MsnFilter)f).getMsn();
				if (msns.length==1)
					sb.append(((MsnFilter)f).getMsn()[0].getMSN());
				else {
					for (int i=0,j=msns.length;i<j;i++) {
						sb.append(((MsnFilter)f).getMsn()[i].getMSN());
						if (i<j-1)
							sb.append(",");
					}
				}
			}
			
			if (ft.equals(FilterType.CALLER)) {
				sb.append(ft.toString());
				sb.append(",");
				IPhonenumber pn = ((CallerFilter)f).getCaller().getPhoneNumber();
				if (pn.isClired()) {
					sb.append("clired");
				} else {
					sb.append(pn.getIntAreaCode());
					sb.append(",");
					sb.append(pn.getAreaCode());
					sb.append(",");
					sb.append(pn.getCallNumber());
				}
				
			}
			if (ft.equals(FilterType.PHONENUMBER)) {
				sb.append(ft.toString());
				sb.append(",");
				IPhonenumber pn = ((PhonenumberFilter)f).getPhonenumber();
				sb.append(pn.getIntAreaCode());
				sb.append(",");
				sb.append((pn.getAreaCode().trim().length()>0 ? pn.getAreaCode() : "+"));
				sb.append(",");
				sb.append((pn.getCallNumber().trim().length()>0 ? pn.getCallNumber() : "+"));
			}
			if (ft.equals(FilterType.ITEMCOUNT)) {
				sb.append(ft.toString());
				sb.append(",");
				sb.append(((ItemCountFilter)f).getLimit());
			}	
			if (ft.equals(FilterType.ATTRIBUTE)) {
				sb.append(ft.toString());
				sb.append(",");
				IAttributeMap m = ((AttributeFilter)f).getAttributeMap();
				if (m!=null && m.size()>0) {
					Iterator i = m.iterator();
					IAttribute a = null;
					while(i.hasNext()) {
						a = (IAttribute) i.next();
						sb.append(a.getName());
						sb.append("=");
						sb.append(a.getValue());
						if (i.hasNext())
							sb.append(",");
					}
				}
			}			
			if (ft.equals(FilterType.CHARACTER)) {
				sb.append(ft.toString());
				sb.append(",");
				sb.append(((CharacterFilter)f).getAttributeName());
				sb.append("=");
				sb.append(((CharacterFilter)f).getCharacter());
			}	
			if (ft.equals(FilterType.UUID)) {
				sb.append(ft.toString());
				sb.append(",");
				String[] uuids = ((UUIDFilter)f).getUUID();
				for (int i=uuids.length, j=0; j<i; j++) {
					sb.append(uuids[j]);
					if (j<(i-1)) sb.append(";");
				}
			}	
			// TODO: more filters to be added here
		}
		return sb.toString();
	}
	
	/**
	 * Transforms a IFilter[] object into a string representation
	 * @param filters a valid IFilter[] object, should not be null
	 * @return a string representation of a list of filters.
	 */
	public String getFiltersToString(IFilter[] filters) {
		StringBuffer sb = new StringBuffer();
		for (int i=0;i<filters.length;i++) {
			String filter = this.getFilterToString(filters[i]);
			if (filter.trim().length()>0) {
				sb.append("(");
				sb.append(filter);
				sb.append(")");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Returns the current IRuntime, this implementation is using. In standard case this should return PIMRuntime.getInstance().
	 * 
	 * @return a valid IRuntime instance
	 */
	protected abstract IRuntime getRuntime();
	
}
