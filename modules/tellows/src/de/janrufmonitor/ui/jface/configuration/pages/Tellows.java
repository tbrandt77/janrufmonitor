package de.janrufmonitor.ui.jface.configuration.pages;

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
	}

}
