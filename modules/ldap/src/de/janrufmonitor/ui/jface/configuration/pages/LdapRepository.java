package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class LdapRepository extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.LdapRepository";
    private String CONFIG_NAMESPACE = "repository.LdapRepository";

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
		return "LdapRepository".toLowerCase();
	}

	public int getNodePosition() {
		return 11;
	}

	protected void createFieldEditors() {
		super.createFieldEditors();
		
		BooleanFieldEditor bfe = new BooleanFieldEditor(
					getConfigNamespace()+SEPARATOR+"keepextension",
				this.m_i18n.getString(this.getNamespace(), "keepextension", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);	
		
		StringFieldEditor sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"server",
			this.m_i18n.getString(this.getNamespace(), "server", "label", this.m_language),
			this.getFieldEditorParent()
		);
		sfe.setEmptyStringAllowed(false);
		addField(sfe);
		
		sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"port",
			this.m_i18n.getString(this.getNamespace(), "port", "label", this.m_language),
			this.getFieldEditorParent()
		);
		sfe.setEmptyStringAllowed(false);
		addField(sfe);
		
		sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"user",
			this.m_i18n.getString(this.getNamespace(), "user", "label", this.m_language),
			this.getFieldEditorParent()
		);
		sfe.setEmptyStringAllowed(false);
		addField(sfe);
		
		sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"password",
			this.m_i18n.getString(this.getNamespace(), "password", "label", this.m_language),
			this.getFieldEditorParent()
		);
		sfe.getTextControl(this.getFieldEditorParent()).setEchoChar('*');
		addField(sfe);
		
		sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"basedn",
			this.m_i18n.getString(this.getNamespace(), "basedn", "label", this.m_language),
			this.getFieldEditorParent()
		);
		sfe.setEmptyStringAllowed(false);
		addField(sfe);

		IntegerFieldEditor ife = new IntegerFieldEditor(
			getConfigNamespace()+SEPARATOR+"maxresults",
			this.m_i18n.getString(this.getNamespace(), "maxresults", "label", this.m_language),
			this.getFieldEditorParent()
		);
		ife.setTextLimit(3);
		addField(ife);	

	}
}
