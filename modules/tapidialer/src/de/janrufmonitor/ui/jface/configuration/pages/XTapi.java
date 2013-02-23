package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;
import de.janrufmonitor.xtapi.XTapiMonitor;

public class XTapi extends AbstractFieldEditorConfigPage {

	private static String NAMESPACE = "ui.jface.configuration.pages.XTapi";

    private IRuntime m_runtime;
    
	public String getConfigNamespace() {
		return XTapiMonitor.NAMESPACE;
	}

	public String getNamespace() {
		return XTapi.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) 
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getParentNodeID() {
		return IConfigPage.ROOT_NODE;
	}

	public String getNodeID() {
		return "XTapi".toLowerCase();
	}

	public int getNodePosition() {
		return 2;
	}

	protected void createFieldEditors() {
		StringFieldEditor sfe = null;
		
		BooleanFieldEditor bfe = new BooleanFieldEditor(
				getConfigNamespace()+SEPARATOR+"activemonitor",
			this.m_i18n.getString(this.getNamespace(), "activemonitor", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(bfe);	
		
		if (isExpertMode()) {
			
			bfe = new BooleanFieldEditor(
					getConfigNamespace()+SEPARATOR+"outgoing",
				this.m_i18n.getString(this.getNamespace(), "outgoing", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);	
			
			sfe = new StringFieldEditor(
					getConfigNamespace()+SEPARATOR+"festnetzalias",
				this.m_i18n.getString(this.getNamespace(), "festnetzalias", "label", this.m_language),
				this.getFieldEditorParent()
			);
			sfe.setEmptyStringAllowed(false);
			addField(sfe);		
		}
		
		IMonitor fbMonitor = this.getRuntime().getMonitorListener().getMonitor("XTapiMonitor");
		if (fbMonitor!=null) {
			String[] fbInfos = fbMonitor.getDescription();
			
			Label capi_label = new Label(this.getFieldEditorParent(), 0);
			capi_label.setText(this.m_i18n.getString(this.getNamespace(), "fbinfo", "label", this.m_language));

			for (int i=0;i<fbInfos.length;i++) {
				if (fbInfos[i].trim().length()>0) {
					Label capi = new Label(this.getFieldEditorParent(), SWT.NULL);
					capi.setText(fbInfos[i]);
					new Label(this.getFieldEditorParent(), SWT.NULL);
				}
			}
		}
		
	}
}
