package de.janrufmonitor.ui.jface.configuration.pages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.preference.ComboFieldEditor;

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
		
		String[][] filters = new String[l.size()][2];
		for (int i=0;i<l.size();i++) {
			filters[i][0] = jfm.getFiltersToLabelText((IFilter[]) l.get(i), 45);
			filters[i][1] =  jfm.getFiltersToString((IFilter[]) l.get(i));
		}
		
		ComboFieldEditor cfe = new ComboFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"filter"
			   , this.m_i18n.getString(getNamespace(), "filter", "label", this.m_language)
			   , filters, 
			   this.getFieldEditorParent());
		addField(cfe);
		
	}
	
}
