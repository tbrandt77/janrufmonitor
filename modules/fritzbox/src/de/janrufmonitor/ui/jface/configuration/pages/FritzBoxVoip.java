package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;

public class FritzBoxVoip extends AbstractFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.FritzBoxVoip";

    private IRuntime m_runtime;
    
	public String getConfigNamespace() {
		return FritzBoxMonitor.NAMESPACE;
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
		return IConfigPage.ROOT_NODE;
	}

	public String getNodeID() {
		return "FritzBoxVoip".toLowerCase();
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
					getConfigNamespace()+SEPARATOR+"boxip",
				this.m_i18n.getString(this.getNamespace(), "boxip", "label", this.m_language),
				this.getFieldEditorParent()
			);
			sfe.setEmptyStringAllowed(false);
			addField(sfe);
		}
		
		sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"boxuser",
			this.m_i18n.getString(this.getNamespace(), "boxuser", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(sfe);
		
		sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"boxpassword",
			this.m_i18n.getString(this.getNamespace(), "boxpassword", "label", this.m_language),
			this.getFieldEditorParent()
		);
		sfe.getTextControl(this.getFieldEditorParent()).setEchoChar('*');
		addField(sfe);
		
		if (isExpertMode()) {
			sfe = new StringFieldEditor(
					getConfigNamespace()+SEPARATOR+"boxport",
					this.m_i18n.getString(this.getNamespace(), "boxport", "label", this.m_language),
					this.getFieldEditorParent()
				);
			sfe.setEmptyStringAllowed(false);
			addField(sfe);
		}

		if (isExpertMode()) {
			sfe = new StringFieldEditor(
					getConfigNamespace()+SEPARATOR+"festnetzalias",
				this.m_i18n.getString(this.getNamespace(), "festnetzalias", "label", this.m_language),
				this.getFieldEditorParent()
			);
			sfe.setEmptyStringAllowed(true);
			addField(sfe);	
			
			sfe = new StringFieldEditor(
					getConfigNamespace()+SEPARATOR+"dialprefixes",
				this.m_i18n.getString(this.getNamespace(), "dialprefixes", "label", this.m_language),
				this.getFieldEditorParent()
			);
			sfe.setEmptyStringAllowed(true);
			addField(sfe);	
			
			bfe = new BooleanFieldEditor(
					getConfigNamespace()+SEPARATOR+"syncdelete",
				this.m_i18n.getString(this.getNamespace(), "syncdelete", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);		
			
			IntegerFieldEditor ife = new IntegerFieldEditor(
				getConfigNamespace()+SEPARATOR+"retrymax",
				this.m_i18n.getString(this.getNamespace(), "retrymax", "label", this.m_language),
				this.getFieldEditorParent()
			);
			ife.setTextLimit(2);
			addField(ife);	
			
			ComboFieldEditor cfe = new ComboFieldEditor(
					getConfigNamespace()+SEPARATOR+"boxclickdial",
					this.m_i18n.getString(this.getNamespace(), "boxclickdial", "label", this.m_language),
					new String[][] { 
						{this.m_i18n.getString(this.getNamespace(), "all_analog", "label", this.m_language), "9"}, 
						{this.m_i18n.getString(this.getNamespace(), "all_isdn", "label", this.m_language), "50"},
						{"FON 1", "1"}, 
						{"FON 2", "2"},
						{"FON 3", "3"}, 
						{"ISDN 1", "51"}, 
						{"ISDN 2", "52"}, 
						{"ISDN 3", "53"}, 
						{"ISDN 4", "54"}, 
						{"ISDN 5", "55"}, 
						{"ISDN 6", "56"}, 
						{"ISDN 7", "57"}, 
						{"ISDN 8", "58"}, 
						{"ISDN 9", "59"},
						{"DECT 610", "60"},
						{"DECT 611", "61"},
						{"DECT 612", "62"},
						{"DECT 613", "63"},
						{"DECT 614", "64"},
						{"DECT 615", "65"},	
					},
				this.getFieldEditorParent()
			);
			addField(cfe);	
		}
		
		IMonitor fbMonitor = this.getRuntime().getMonitorListener().getMonitor("FritzBoxMonitor");
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
