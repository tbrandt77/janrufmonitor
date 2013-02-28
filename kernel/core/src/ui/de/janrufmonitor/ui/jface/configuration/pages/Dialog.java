package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;

public class Dialog extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.Dialog";
    private String CONFIG_NAMESPACE = "ui.jface.application.dialog.Dialog";
    
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
		return "Dialog".toLowerCase();
	}

	public int getNodePosition() {
		return 0;
	}

	protected void createFieldEditors() {
		super.createFieldEditors();
		
		BooleanFieldEditor bfe = null;
		
		if (isExpertMode()) {
			bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"outgoing",
				this.m_i18n.getString(this.getNamespace(), "outgoing", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);
			
			bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"balloon",
				this.m_i18n.getString(this.getNamespace(), "balloon", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);
			
			bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"focus",
				this.m_i18n.getString(this.getNamespace(), "focus", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);
		}
		
		RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor(
			this.CONFIG_NAMESPACE+SEPARATOR+"position",
			this.m_i18n.getString(this.getNamespace(), "position", "label", this.m_language),
			2,
			new String[][] {
		    	{this.m_i18n.getString(this.getNamespace(), "lefttop", "label", this.m_language), "left-top"}, 
		    	{this.m_i18n.getString(this.getNamespace(), "leftbottom", "label", this.m_language), "left-bottom"},
		    	{this.m_i18n.getString(this.getNamespace(), "righttop", "label", this.m_language), "right-top"},
		    	{this.m_i18n.getString(this.getNamespace(), "rightbottom", "label", this.m_language), "right-bottom"},
		    	{this.m_i18n.getString(this.getNamespace(), "center", "label", this.m_language), "center"}
		    },
			this.getFieldEditorParent(),
			true
		);
		addField(rgfe);
		
		if (isExpertMode()) {
			bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"freepos",
				this.m_i18n.getString(this.getNamespace(), "freepos", "label", this.m_language),
				this.getFieldEditorParent()
			);
			
			addField(bfe);
		}

		StringFieldEditor sfe = null;
		if (isExpertMode()) {
			new Label(this.getFieldEditorParent(), SWT.NULL);
			
			rgfe = new RadioGroupFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"showtime",
				this.m_i18n.getString(this.getNamespace(), "showtime", "label", this.m_language),
				1,
				new String[][] {
			    	{this.m_i18n.getString(this.getNamespace(), "hide", "label", this.m_language), "-1"}, 
			    	{this.m_i18n.getString(this.getNamespace(), "manual", "label", this.m_language), "-2"},
			    	{this.m_i18n.getString(this.getNamespace(), "time", "label", this.m_language), "1"}
			    },
				this.getFieldEditorParent(),
				true
			);
			addField(rgfe);

			sfe = new StringFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"showduration",
				this.m_i18n.getString(this.getNamespace(), "showduration", "label", this.m_language),
				5,
				this.getFieldEditorParent());
			sfe.setEmptyStringAllowed(false);
			addField(sfe);
			
			new Label(this.getFieldEditorParent(), SWT.NULL);
			
			
			bfe = new BooleanFieldEditor(
				this.getConfigNamespace()+SEPARATOR+"assign",
				this.m_i18n.getString(this.getNamespace(), "assign", "label", this.m_language),
				this.getFieldEditorParent()
			);
			
			addField(bfe);
		}
		
		ColorFieldEditor cfe = new ColorFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"fontcolor",
			this.m_i18n.getString(this.getNamespace(), "fontcolor", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(cfe);
		
		bfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"usemsncolor",
			this.m_i18n.getString(this.getNamespace(), "usemsncolor", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(bfe);
		
		bfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"spamcolor",
			this.m_i18n.getString(this.getNamespace(), "spamcolor", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(bfe);
		
		sfe = new StringFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"fontsize",
			this.m_i18n.getString(this.getNamespace(), "fontsize", "label", this.m_language),
			5,
			this.getFieldEditorParent());
		sfe.setEmptyStringAllowed(false);
		addField(sfe);
	}
}