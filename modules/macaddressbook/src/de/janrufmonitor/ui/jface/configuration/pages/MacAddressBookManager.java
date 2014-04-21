package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.StringFieldEditor;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;

public class MacAddressBookManager extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.MacAddressBookManager";
    private String CONFIG_NAMESPACE = "repository.MacAddressBookManager";

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
		return IConfigPage.CALLER_NODE;
	}

	public String getNodeID() {
		return "MacAddressBookManager".toLowerCase();
	}

	public int getNodePosition() {
		return 11;
	}

	protected void createFieldEditors() {
		super.createFieldEditors();
		
		if (isExpertMode()) {
			BooleanFieldEditor bfe = new BooleanFieldEditor(
					getConfigNamespace()+SEPARATOR+"keepextension",
				this.m_i18n.getString(this.getNamespace(), "keepextension", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);	
			
			StringFieldEditor sfe = new StringFieldEditor(
					getConfigNamespace()+SEPARATOR+"addnumbers",
				this.m_i18n.getString(this.getNamespace(), "addnumbers", "label", this.m_language),
				this.getFieldEditorParent()
			);
			sfe.setEmptyStringAllowed(false);
			addField(sfe);
		}
	}
}
