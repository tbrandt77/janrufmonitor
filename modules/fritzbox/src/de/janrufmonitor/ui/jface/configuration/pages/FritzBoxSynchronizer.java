package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Label;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;
import de.janrufmonitor.ui.swt.DisplayManager;

public class FritzBoxSynchronizer extends AbstractServiceFieldEditorConfigPage {
	
    private String NAMESPACE = "ui.jface.configuration.pages.FritzBoxSynchronizer";
    private String CONFIG_NAMESPACE = "ui.jface.application.fritzbox.action.Refresh";
    
	private IRuntime m_runtime;
	
	public String getParentNodeID() {
		return IConfigPage.SERVICE_NODE;
	}
	
	public String getNodeID() {
		return "FritzBoxSynchronizer".toLowerCase();
	}

	public int getNodePosition() {
		return 0;
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
		return this.CONFIG_NAMESPACE;
	}
	
	protected void createFieldEditors() {
		super.createFieldEditors();
	
		new Label(this.getFieldEditorParent(), SWT.NULL);new Label(this.getFieldEditorParent(), SWT.NULL);
		
		Label l = new Label(this.getFieldEditorParent(), SWT.NULL);
		l.setText(this.m_i18n.getString(this.getNamespace(), "times", "label", this.m_language));
		FontData df = new FontData();df.setStyle(SWT.BOLD);
		Font f = new Font(DisplayManager.getDefaultDisplay(), df);
		l.setFont(f);
		new Label(this.getFieldEditorParent(), SWT.NULL);
		
		BooleanFieldEditor sfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"syncstartup",
			this.m_i18n.getString(this.getNamespace(), "syncstartup", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(sfe);
		
		IntegerFieldEditor ife = new IntegerFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"startupdelay",
			this.m_i18n.getString(this.getNamespace(), "startupdelay", "label", this.m_language),
			this.getFieldEditorParent(),
			3
		);
		addField(ife);
		
		sfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"race",
			this.m_i18n.getString(this.getNamespace(), "race", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(sfe);
		
		ife = new IntegerFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"synctimer",
			this.m_i18n.getString(this.getNamespace(), "synctimer", "label", this.m_language),
			this.getFieldEditorParent(),
			3
		);
		addField(ife);
		
		new Label(this.getFieldEditorParent(), SWT.NULL);new Label(this.getFieldEditorParent(), SWT.NULL);
			
		l = new Label(this.getFieldEditorParent(), SWT.NULL);
		l.setText(this.m_i18n.getString(this.getNamespace(), "options", "label", this.m_language));
		l.setFont(f);
		
		new Label(this.getFieldEditorParent(),  SWT.NULL);
		
		sfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"syncall",
			this.m_i18n.getString(this.getNamespace(), "syncall", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(sfe);
		
		sfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"syncclean",
			this.m_i18n.getString(this.getNamespace(), "syncclean", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(sfe);
		sfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"syncdelete",
			this.m_i18n.getString(this.getNamespace(), "syncdelete", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(sfe);
		sfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"syncdialog",
			this.m_i18n.getString(this.getNamespace(), "syncdialog", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(sfe);

		if (getRuntime().getServiceFactory().isServiceAvailable("MailNotification")&& getRuntime().getServiceFactory().isServiceEnabled("MailNotification")) {
			sfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"syncnotification",
				this.m_i18n.getString(this.getNamespace(), "syncnotification", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(sfe);	
		}
	}

}
