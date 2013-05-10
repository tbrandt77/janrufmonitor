package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.ComboFieldEditor;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class GoogleMaps extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.GoogleMaps";
    private String CONFIG_NAMESPACE = "service.GoogleMaps";

    private IRuntime m_runtime;
    
	public String getConfigNamespace() {
		return this.CONFIG_NAMESPACE;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) 
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getParentNodeID() {
		return IConfigPage.SERVICE_NODE;
	}

	public String getNodeID() {
		return "GoogleMaps".toLowerCase();
	}

	public int getNodePosition() {
		return 25;
	}

	protected void createFieldEditors() {
		super.createFieldEditors();
		
		ComboFieldEditor cfe = new ComboFieldEditor(
			getConfigNamespace()+SEPARATOR+"type",	
			this.m_i18n.getString(this.getNamespace(), "type", "label", this.m_language),
			new String[][] { 
				{this.m_i18n.getString(this.getNamespace(), "hybrid", "label", this.m_language), "hybrid"},
				{this.m_i18n.getString(this.getNamespace(), "roadmap", "label", this.m_language), "roadmap"}
			},	
			this.getFieldEditorParent()
		);
		addField(cfe);
		
		cfe = new ComboFieldEditor(
			getConfigNamespace()+SEPARATOR+"zoom",	
			this.m_i18n.getString(this.getNamespace(), "zoom", "label", this.m_language),
			new String[][] { 
				{this.m_i18n.getString(this.getNamespace(), "11", "label", this.m_language),"11"},
				{this.m_i18n.getString(this.getNamespace(), "13", "label", this.m_language), "13"},
				{this.m_i18n.getString(this.getNamespace(), "15", "label", this.m_language), "15"}
			},	
			this.getFieldEditorParent()
		);
		addField(cfe);
	
	}
	
	public boolean performOk() {
		getRuntime().getConfigurableNotifier().notifyByNamespace(CONFIG_NAMESPACE);
		
		return super.performOk();
	}
}
