package de.janrufmonitor.ui.jface.wizards;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.CallerFilter;
import de.janrufmonitor.repository.filter.CipFilter;
import de.janrufmonitor.repository.filter.DateFilter;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.ItemCountFilter;
import de.janrufmonitor.repository.filter.MsnFilter;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.IFilterManager;
import de.janrufmonitor.ui.jface.application.journal.Journal;
import de.janrufmonitor.ui.jface.application.journal.JournalFilterManager;
import de.janrufmonitor.ui.jface.wizards.pages.AbstractPage;
import de.janrufmonitor.ui.jface.wizards.pages.FilterCallTypePage;
import de.janrufmonitor.ui.jface.wizards.pages.FilterCipPage;
import de.janrufmonitor.ui.jface.wizards.pages.FilterDatePage;
import de.janrufmonitor.ui.jface.wizards.pages.FilterItemCountPage;
import de.janrufmonitor.ui.jface.wizards.pages.FilterMsnPage;
import de.janrufmonitor.ui.jface.wizards.pages.FilterNamePage;
import de.janrufmonitor.ui.jface.wizards.pages.FilterPhonesPage;

public class FilterWizard extends AbstractWizard {

	private String NAMESPACE = "ui.jface.wizards.FilterWizard"; 
	
	private IFilter[] m_filters;
	private AbstractPage[] m_pages;
	private IRuntime m_runtime;
	
	public FilterWizard(IFilter[] filters) {
		super();
    	setWindowTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
		
    	this.m_filters = filters;
		
		this.m_pages = new AbstractPage[7];
		IFilter f = this.getMsnFilter(filters);
		if (f!=null)
			this.m_pages[0] = new FilterMsnPage(((MsnFilter)f).getMsn()[0]);
		else 
			this.m_pages[0] = new FilterMsnPage(null);
		
		f = this.getCipFilter(filters);
		if (f!=null)
			this.m_pages[1] = new FilterCipPage(((CipFilter)f).getCip());
		else
			this.m_pages[1] = new FilterCipPage(null);
		
		f = this.getDateFilter(filters);
		if (f!=null)
			if (((DateFilter)f).getTimeframe()==-1) {
				this.m_pages[2] = new FilterDatePage(
					new Date[]{
						((DateFilter)f).getDateFrom(),
						((DateFilter)f).getDateTo(),
						null
					}
				);
			} else {
				this.m_pages[2] = new FilterDatePage(
					new Date[]{
						null,
						null,
						new Date(((DateFilter)f).getTimeframe())
					}
				);
			}
		else
			this.m_pages[2] = new FilterDatePage(null);
		
		IFilter[] fs = this.getPhoneFilter(filters);
		if (fs!=null){
			IPhonenumber[] phones = new IPhonenumber[fs.length];
			for (int i=0;i<fs.length;i++) {
				phones[i] = ((CallerFilter)fs[i]).getCaller().getPhoneNumber();
			}
			this.m_pages[3] = new FilterPhonesPage(phones);
		} else
			this.m_pages[3] = new FilterPhonesPage(null);
		
		f = this.getItemCountFilter(filters);
		if (f!=null) {
			if (((ItemCountFilter)f).getLimit()>0)
				this.m_pages[4] = new FilterItemCountPage(((ItemCountFilter)f).getLimit());
			else
				this.m_pages[4] = new FilterItemCountPage(0);
		} else
			this.m_pages[4] = new FilterItemCountPage(0);
		
		f = this.getAttributeFilter(filters);
		if (f!=null) {
			if (((AttributeFilter)f).getAttributeMap().size()>0)
				this.m_pages[5] = new FilterCallTypePage(((AttributeFilter)f).getAttributeMap());
			else
				this.m_pages[5] = new FilterCallTypePage(getRuntime().getCallFactory().createAttributeMap());
		} else
			this.m_pages[5] = new FilterCallTypePage(getRuntime().getCallFactory().createAttributeMap());
		
		if (filters!=null) {
			IFilterManager fm = new JournalFilterManager();
			String name = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Journal.NAMESPACE, "filtername_"+fm.getFiltersToString(filters));
			if (name!=null && name.trim().length()>0)
				this.m_pages[6] = new FilterNamePage(name);
			else
				this.m_pages[6] = new FilterNamePage("");
		} else
			this.m_pages[6] = new FilterNamePage("");
		
		this.addPage(this.m_pages[0]);
		this.addPage(this.m_pages[1]);
		this.addPage(this.m_pages[2]);
		this.addPage(this.m_pages[3]);
		this.addPage(this.m_pages[4]);
		this.addPage(this.m_pages[5]);
		this.addPage(this.m_pages[6]);
	}

	public String getID() {
		return FilterWizard.class.getName();
	}
	
	public String getNamespace() {
		return this.NAMESPACE;
	}

	public boolean performFinish() {
		if (this.m_pages[0].isPageComplete() &&
			this.m_pages[1].isPageComplete() &&
			this.m_pages[2].isPageComplete() &&
			this.m_pages[3].isPageComplete() &&
			this.m_pages[4].isPageComplete() &&
			this.m_pages[5].isPageComplete() &&
			this.m_pages[6].isPageComplete()) {
			
			List filterList = new ArrayList();
			
			IPhonenumber[] phones = ((FilterPhonesPage)this.m_pages[3]).getResult();
			if (phones!=null) {
				filterList.addAll(this.getCallerFilters(phones));
			}
			
			Date[] dates = ((FilterDatePage)this.m_pages[2]).getResult();
			if (dates!=null && dates.length>0) {
				filterList.add(this.getDateFilter(dates));
			}
			
			IMsn msn = ((FilterMsnPage)this.m_pages[0]).getResult();
			if (msn!=null) {
				if (!msn.getMSN().equalsIgnoreCase("*"))
					filterList.add(new MsnFilter(new IMsn[] {msn}));
			}
			
			int limit = ((FilterItemCountPage)this.m_pages[4]).getResult();
			if (limit>0) {
				filterList.add(new ItemCountFilter(limit));
			}
			
			IAttributeMap m = ((FilterCallTypePage)this.m_pages[5]).getResult();
			if (m!=null && m.size()>0) {
				filterList.add(new AttributeFilter(m));
			}
			
			ICip cip = ((FilterCipPage)this.m_pages[1]).getResult();
			if (cip!=null)
				if (!cip.getCIP().equalsIgnoreCase("*"))
					filterList.add(new CipFilter(cip));
			
			this.m_filters = new IFilter[filterList.size()];
			for (int i=0;i<filterList.size();i++) {
				this.m_filters[i] = (IFilter)filterList.get(i);
			}
			
			String name = ((FilterNamePage)this.m_pages[6]).getResult();
			IFilterManager fm = new JournalFilterManager();
			if (name!=null && name.trim().length()>0) {
				this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(Journal.NAMESPACE, "filtername_"+fm.getFiltersToString(getResult()), name);
				this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
			} else {
				this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(Journal.NAMESPACE, "filtername_"+fm.getFiltersToString(getResult()));
				this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
			}
			
			return true;
		}
		this.m_filters = null;
		return false;
	}
	
	public IFilter[] getResult() {
		return this.m_filters;
	}
	
	public boolean performCancel() {
		this.m_filters = null;
		return super.performCancel();
	}
	
	private IFilter getDateFilter(Date[] dates) {
		if (dates==null) return null;
		
		if (dates.length==1) {
			return new DateFilter(null, dates[0]);
		}
		
		if (dates.length==3) {
			if (dates[2]!=null) {
				if (dates[2].getTime()==0) return new DateFilter(1);
				return new DateFilter(dates[2].getTime());
			}
		}
		
		return new DateFilter(dates[0], dates[1]);
	}
	
	private List getCallerFilters(IPhonenumber[] phones) {
		List l = new ArrayList();
		if (phones==null || phones.length==0) return l;
		
		for (int i=0;i<phones.length;i++) {
			ICaller c = getRuntime().getCallerFactory().createCaller(
				getRuntime().getCallerFactory().createName("", ""),
				phones[i]
			);
			l.add(new CallerFilter(c));
		}

		return l;
	}
	
	private IFilter getMsnFilter(IFilter[] f) {
		if (f==null) return null;
		for (int i=0;i<f.length;i++) {
			if (f[i] instanceof MsnFilter) return f[i];
		}
		return null;
	}
	
	private IFilter getItemCountFilter(IFilter[] f) {
		if (f==null) return null;
		for (int i=0;i<f.length;i++) {
			if (f[i] instanceof ItemCountFilter) return f[i];
		}
		return null;
	}
	
	private IFilter getAttributeFilter(IFilter[] f) {
		if (f==null) return null;
		for (int i=0;i<f.length;i++) {
			if (f[i] instanceof AttributeFilter) return f[i];
		}
		return null;
	}
	
	private IFilter getCipFilter(IFilter[] f) {
		if (f==null) return null;
		for (int i=0;i<f.length;i++) {
			if (f[i] instanceof CipFilter) return f[i];
		}
		return null;
	}
	
	private IFilter getDateFilter(IFilter[] f) {
		if (f==null) return null;
		for (int i=0;i<f.length;i++) {
			if (f[i] instanceof DateFilter) return f[i];
		}
		return null;
	}
	
	private IFilter[] getPhoneFilter(IFilter[] f) {
		if (f==null) return null;
		List l = new ArrayList();
		for (int i=0;i<f.length;i++) {
			if (f[i] instanceof CallerFilter) l.add(f[i]);
		}
		
		if (l.size()>0) {
			IFilter[] fs = new IFilter[l.size()];
			for (int i=0;i<l.size();i++) {
				fs[i] = (IFilter)l.get(i);
			}
			return fs;
		}
		return null;
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
}
