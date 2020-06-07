package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.BooleanFieldEditor;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class ClipboardService extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.ClipboardService";
    private String CONFIG_NAMESPACE = "service.ClipboardService";

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
		return "ClipboardService".toLowerCase();
	}

	public int getNodePosition() {
		return 50;
	}

	protected void createFieldEditors() {
		super.createFieldEditors();
		
		if (isExpertMode()) {
			BooleanFieldEditor bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"rawnumber",
				this.m_i18n.getString(this.getNamespace(), "rawnumber", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);
		}
	}
}
