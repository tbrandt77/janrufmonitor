package de.janrufmonitor.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.DateFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.ItemCountFilter;
import de.janrufmonitor.repository.types.IReadCallRepository;

/**
 *  This abstract class can be used as base class for a new call manager implementation which
 *  is supporting configuration and does not offer an own filter mechanism.
 *
 *@author     Thilo Brandt
 *@created    2004/01/31
 */
public abstract class AbstractFilterCallManager extends AbstractConfigurableCallManager implements IReadCallRepository {

	public AbstractFilterCallManager() {
		super();
	}

	/**
	 * Gets the initial call list which can be pre-filtered.
	 * 
	 * @param f inital filter object (optional processing).
	 * @return an inital list of ICall objects.
	 */
	abstract protected ICallList getInitialCallList(IFilter f);
	

	public int getCallCount(IFilter[] filters) {
		return getCalls(filters).size();
	}

	public ICallList getCalls(IFilter[] filters, int count, int offset) {
		return getCalls(filters);
	}

	
	public synchronized ICallList getCalls(IFilter[] filters) {
		if (filters!=null && filters.length>0) {
			if (this.hasMultipleCallerFilter(filters)) {
				this.m_logger.info("Detected multiple caller filters.");
				List callerFilterList = new ArrayList();
				List itemCountFilterList = new ArrayList();
				ICallList cl = null;
				for (int i=0;i<filters.length;i++) {
					if (filters[i].getType().equals(FilterType.CALLER)) {
						// isolate caller filters, apply all others
						callerFilterList.add(filters[i]);
					} else if (filters[i].getType().equals(FilterType.ITEMCOUNT)) {
						// isolate item count filters, apply all others
						itemCountFilterList.add(filters[i]);
					} else {
						if (cl==null) {
							cl = this.filterCalls(filters[i],  null);
						} else {
							cl = this.filterCalls(filters[i], cl);
						}
					}
				}
				if (callerFilterList.size()>0) {
					if (cl==null)
						cl = this.filterCalls(null, null);
					cl = processCallerFilters(cl, callerFilterList);
				}
				
				if (itemCountFilterList.size()>0) {
					if (cl==null)
						cl = this.filterCalls(null, null);
					// only process the first filter, more does not make sense...
					cl = filterCalls((IFilter)itemCountFilterList.get(0), cl);
				}
				
				return cl;
			} else {
				ICallList cl = this.filterCalls(filters[0], null);
				for (int i=1;i<filters.length;i++) {
					cl = this.filterCalls(filters[i], cl);
					this.m_logger.info("Filter "+filters[i]+" is applied by this manager.");
				}
				return cl;
			}
		}
		return this.filterCalls(null, null);
	}

	public synchronized ICallList getCalls(IFilter filter) {
		if (filter != null) {
			IFilter[] filters = new IFilter[] {filter};	
			return getCalls(filters);
		}
		return this.getCalls((IFilter[])null);
	}

	public boolean isSupported(Class c) {
		return c.isInstance(this);
	}

	private ICallList processCallerFilters(ICallList cl, List callerFilter) {
		ICallList mergeList = this.getRuntime().getCallFactory().createCallList();
		ICallList[] filterLists = new ICallList[callerFilter.size()];
		for (int i=0,n=callerFilter.size();i<n;i++) {
			filterLists[i] = this.getRuntime().getCallFactory().createCallList();
			filterLists[i].add(cl);
		}
		
		IFilter f = null;
		for (int i=0,n=callerFilter.size();i<n;i++) {
			f = (IFilter)callerFilter.get(i);
			mergeList.add(this.filterCalls(f, filterLists[i]));
		}
		return mergeList;
	}
	
	private boolean hasMultipleCallerFilter(IFilter[] filters) {
		if (filters==null) return false;
		int count = 0;
		for (int i=0;i<filters.length;i++) {
			if (filters[i].getType().equals(FilterType.CALLER)) count++;
			if (count>1) return true;
		}
		return (count>1);
	}
	
	protected ICallList filterCalls(IFilter filter, ICallList cl) {
		if (cl==null) {
			cl = this.getInitialCallList(filter);
		}
		
		long start = System.currentTimeMillis();
		this.m_logger.info("Start filtering with filter: "+filter);
		this.m_logger.info("CallList size before filtering: "+cl.size());
		ICall c = null;
		if (filter!=null) {
			if (filter.getType().equals(FilterType.DATE)) {
				if (filter instanceof DateFilter) {
					DateFilter df = (DateFilter)filter;
					long from = (df.getDateFrom()==null ? 0 : df.getDateFrom().getTime());
					long to = df.getDateTo().getTime();
					
					long cdate = 0;
					for (int i=cl.size()-1;i>=0;i--) {
						c = cl.get(i);
						cdate = c.getDate().getTime();
						if (from>0) {
							if (cdate<to || cdate>from)
								cl.remove(c);
						} else {
							if (cdate<to)
								cl.remove(c);
						}
					}
				}
			}
			if (filter.getType().equals(FilterType.CALLER)) {	
				ICaller cfilter = (ICaller)filter.getFilterObject();
				IPhonenumber pn = cfilter.getPhoneNumber();
				for (int i=cl.size()-1;i>=0;i--) {
					c = cl.get(i);
					if (!c.getCaller().getPhoneNumber().equals(pn))
						cl.remove(c);
				}
			}
			if (filter.getType().equals(FilterType.PHONENUMBER)) {	
				IPhonenumber pn = (IPhonenumber)filter.getFilterObject();
				for (int i=cl.size()-1;i>=0;i--) {
					c = cl.get(i);
					if (!c.getCaller().getPhoneNumber().equals(pn))
						cl.remove(c);
				}
			}
			if (filter.getType().equals(FilterType.CIP)) {
				ICip cip = (ICip)filter.getFilterObject();
				for (int i=cl.size()-1;i>=0;i--) {
					c = cl.get(i);
					if (!c.getCIP().equals(cip))
						cl.remove(c);
				}
			}
			if (filter.getType().equals(FilterType.MSN)) {
				IMsn msn = (IMsn)filter.getFilterObject();
				for (int i=cl.size()-1;i>=0;i--) {
					c = cl.get(i);
					if (!c.getMSN().equals(msn))
						cl.remove(c);
				}
			}
			if (filter.getType().equals(FilterType.UUID)) {
				String[] uuids = (String[])filter.getFilterObject();
				for (int i=cl.size()-1;i>=0;i--) {
					c = cl.get(i);
					boolean hasUUID = false;
					for (int j=0;j<uuids.length;j++) {
						if (c.getUUID().equalsIgnoreCase(uuids[j]))
							hasUUID = true;
					}
					if (!hasUUID)
						cl.remove(c);
				}
			}
			if (filter.getType().equals(FilterType.ATTRIBUTE)) {
				IAttributeMap m = ((AttributeFilter)filter).getAttributeMap();
				if (m!=null && m.size()>0) {
					Iterator iter = m.iterator();
					IAttribute a = null;
					while (iter.hasNext()) {
						a = (IAttribute) iter.next();
						IAttribute ua = null;
						for (int i=cl.size()-1;i>=0;i--) {
							c = cl.get(i);
							ua = c.getAttribute(a.getName());
							if (ua==null || !ua.getValue().equalsIgnoreCase(a.getValue())) {
								cl.remove(c);
							}
						}
					}
				}
			}
			
			if (filter.getType().equals(FilterType.ITEMCOUNT)) {
				int itemcount = ((ItemCountFilter)filter).getLimit();
				if (itemcount>0) {
					ICallList tmpCl = this.getRuntime().getCallFactory().createCallList(itemcount);
					// added: 2006/04/20: just a work-a-round
					cl.sort(0, false);
					
					for (int i=0, j=Math.min(cl.size(), itemcount); i<j;i++) {
						tmpCl.add(cl.get(i));
					}
					cl.clear();
					cl.add(tmpCl);
				}
			}			
		}
		this.m_logger.info("CallList size after filtering: "+cl.size());
		this.m_logger.info("Finished filtering with filter <"+filter+"> in "+Long.toString(System.currentTimeMillis()-start)+" msec.");
		return cl;
	}

}
