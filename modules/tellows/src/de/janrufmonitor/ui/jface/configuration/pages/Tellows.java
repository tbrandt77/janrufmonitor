package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class Tellows extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.Tellows";
    private String CONFIG_NAMESPACE = "service.Tellows";

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
		return "Tellows".toLowerCase();
	}

	public int getNodePosition() {
		return 50;
	}
	
	protected void createFieldEditors() {
		super.createFieldEditors();
		
		if (isExpertMode()) {
			// do something...
		}
	
		BooleanFieldEditor bfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"spamcolor",
			this.m_i18n.getString(this.getNamespace(), "spamcolor", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(bfe);
		
		ComboFieldEditor cfe = new ComboFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"minscore",
				this.m_i18n.getString(getNamespace(), "minscore", "label", this.m_language)
				   , new String[][] {
					   { "1", "1" }, 
					   { "2", "2" },
					   { "3", "3" },
					   { "4", "4" },
					   { "5", "5" },
					   { "6", "6" },
					   { "7", "7" },
					   { "8", "8" },
					   { "9", "9" },
				   }, 
				   this.getFieldEditorParent());
		addField(cfe);
		
		StringFieldEditor u = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"apikey",
			this.m_i18n.getString(this.getNamespace(), "apikey", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(u);
	}

}
