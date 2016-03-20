package de.janrufmonitor.ui.jface.configuration.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.IFilterManager;
import de.janrufmonitor.ui.jface.application.journal.JournalFilterManager;
import de.janrufmonitor.ui.jface.configuration.AbstractFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class Journal extends AbstractFieldEditorConfigPage {
	
    private String NAMESPACE = "ui.jface.configuration.pages.Journal";
    
	private IRuntime m_runtime;
	
	public String getParentNodeID() {
		return IConfigPage.ROOT_NODE;
	}
	
	public String getNodeID() {
		return IConfigPage.JOURNAL_NODE;
	}

	public int getNodePosition() {
		return 4;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) 
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
	
	public String getConfigNamespace() {
		return "ui.jface.application.journal.Journal";
	}

	protected void createFieldEditors() {
		this.setTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));

		this.noDefaultAndApplyButton();
		
		List l = new ArrayList();
		l.add(new IFilter[0]);
		IFilterManager jfm = new JournalFilterManager();
		Properties c = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(this.getConfigNamespace());
		Iterator iter = c.keySet().iterator();
		String key = null;
		while (iter.hasNext()) {
			key = (String)iter.next();
			if (key.startsWith("filter_")) {
				String filter = c.getProperty(key);
				l.add(jfm.getFiltersFromString(filter));
			}
		}
		
		// added 2015/04/03: added runtime filters
		if (jfm.hasRuntimeFilters()) {
			IFilter[][] rf = jfm.getRuntimeFilters();
			if (rf!=null && rf.length>0) {
				for (int i=0;i<rf.length;i++)
					l.add(rf[i]);
			}
		}
		
		// added 2015/04/01: sort filter list
		Collections.sort(l, new Comparator() {

			public int compare(Object f1, Object f2) {
				if (f1!=null && f2!=null && f1 instanceof IFilter[] && f2 instanceof IFilter[]) {
					if (((IFilter[])f1).length==((IFilter[])f2).length) {
						return (((IFilter[])f2)[0].toString().compareTo(((IFilter[])f1)[0].toString()));
					}
					if (((IFilter[])f1).length<((IFilter[])f2).length) return -1;
					return 1;
				}
				return 0;
			}});
		
		String[][] filters = new String[l.size()][2];
		for (int i=0;i<l.size();i++) {
			filters[i][0] = getFilterLabel(jfm, (IFilter[]) l.get(i));
			filters[i][1] =  jfm.getFiltersToString((IFilter[]) l.get(i));
		}
		
		ComboFieldEditor cfe = new ComboFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"filter"
			   , this.m_i18n.getString(getNamespace(), "filter", "label", this.m_language)
			   , filters, 
			   this.getFieldEditorParent());
		addField(cfe);
		
		StringFieldEditor sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"rt_filters_years",
				this.m_i18n.getString(this.getNamespace(), "rt_filters_years", "label", this.m_language),
				2,
				this.getFieldEditorParent()
			);
		sfe.setTextLimit(2);
		addField(sfe);
		
	}
	
	private String getFilterLabel(IFilterManager fm, IFilter[] f) {
		String name = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), "filter_name_"+fm.getFiltersToString(f));
		if (name!=null && name.length()>0) return name;
		return fm.getFiltersToLabelText(f, 45);
	}
	
}
