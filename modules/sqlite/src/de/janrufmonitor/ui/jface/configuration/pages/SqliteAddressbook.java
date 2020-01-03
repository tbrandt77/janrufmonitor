package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.StringFieldEditor;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class SqliteAddressbook extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.SqliteAddressbook";
    private String CONFIG_NAMESPACE = "repository.SqliteAddressbook";

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
		return "SqliteAddressbook".toLowerCase();
	}

	public int getNodePosition() {
		return 1;
	}

	protected void createFieldEditors() {
		super.createFieldEditors();
		
		StringFieldEditor dfe = null;
		
		if (isExpertMode()) {
			dfe = new StringFieldEditor(
				this.CONFIG_NAMESPACE+SEPARATOR+"dbserver",
				this.m_i18n.getString(this.getNamespace(), "dbserver", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(dfe);
			
			dfe = new StringFieldEditor(
				this.CONFIG_NAMESPACE+SEPARATOR+"dbport",
				this.m_i18n.getString(this.getNamespace(), "dbport", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(dfe);
		}
			
		dfe = new StringFieldEditor(
			this.CONFIG_NAMESPACE+SEPARATOR+"dbdb",
			this.m_i18n.getString(this.getNamespace(), "dbdb", "label", this.m_language),
			this.getFieldEditorParent()
		);	
		addField(dfe);
		
		dfe = new StringFieldEditor(
			this.CONFIG_NAMESPACE+SEPARATOR+"dbuser",
			this.m_i18n.getString(this.getNamespace(), "dbuser", "label", this.m_language),
			this.getFieldEditorParent()
		);		
		addField(dfe);
		
		dfe = new StringFieldEditor(
			this.CONFIG_NAMESPACE+SEPARATOR+"dbpassword",
			this.m_i18n.getString(this.getNamespace(), "dbpassword", "label", this.m_language),
			this.getFieldEditorParent()
		);			
		dfe.getTextControl(this.getFieldEditorParent()).setEchoChar('*');
		
		addField(dfe);
	}
}
