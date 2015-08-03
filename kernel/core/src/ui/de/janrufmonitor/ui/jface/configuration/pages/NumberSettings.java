package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Label;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractFieldEditorConfigPage;
import de.janrufmonitor.ui.swt.DisplayManager;

public class NumberSettings extends AbstractFieldEditorConfigPage {
	
    private String NAMESPACE = "ui.jface.configuration.pages.NumberSettings";

	private IRuntime m_runtime;
	
	public String getParentNodeID() {
		return "BasicSettings".toLowerCase();
	}
	
	public String getNodeID() {
		return "NumberSettings".toLowerCase();
	}

	public int getNodePosition() {
		return 1;
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
		return IJAMConst.GLOBAL_NAMESPACE;
	}
	
	private Font getBoldFont(Font f) {
		FontData[] fd = f.getFontData();
		if (fd==null || fd.length==0) return f;
		
		for (int i=0;i<fd.length;i++) {
			fd[i].setStyle(SWT.BOLD);
		}
		f = new Font(DisplayManager.getDefaultDisplay(), fd);		
		return f;
	}
	
	protected void createFieldEditors() {
		this.setTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));

		Label l = new Label(this.getFieldEditorParent(), SWT.NULL);
		l = new Label(this.getFieldEditorParent(), SWT.NULL);
		l = new Label(this.getFieldEditorParent(), SWT.NULL);
		l.setText(this.m_i18n.getString(this.getNamespace(), "general", "label", this.m_language));
		l.setFont(getBoldFont(l.getFont()));
		l = new Label(this.getFieldEditorParent(), SWT.NULL);
		
		StringFieldEditor sfe = new StringFieldEditor(
				IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_AREACODE,
				this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_AREACODE, "label", this.m_language),
				10,
				this.getFieldEditorParent()
			);
		sfe.setEnabled(false, this.getFieldEditorParent());
		addField(sfe);
		
		 ComboFieldEditor cfe = new ComboFieldEditor(
				 IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER
				   , this.m_i18n.getString(getNamespace(), "label", "label", this.m_language)
				   , new String[][] {
					   { this.m_i18n.getString(getNamespace(), "format1", "label", this.m_language), "+%intareacode% (%areacode%) %callnumber%" }, 
					   { this.m_i18n.getString(getNamespace(), "format2", "label", this.m_language), "00%intareacode% (%areacode%) %callnumber%" }, 
					   { this.m_i18n.getString(getNamespace(), "format3", "label", this.m_language), "(0%areacode%) %callnumber%" },
					   { this.m_i18n.getString(getNamespace(), "format4", "label", this.m_language), "+%intareacode% %areacode% %callnumber%" },
					   { this.m_i18n.getString(getNamespace(), "format5", "label", this.m_language), "0%areacode%-%callnumber%" }, 
					   { this.m_i18n.getString(getNamespace(), "format6", "label", this.m_language), "0%areacode%/%callnumber%" }, 
				   }, 
				   this.getFieldEditorParent());
		 addField(cfe);
		
		if (isExpertMode()) {
			sfe = new StringFieldEditor(
					IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_AREACODE_ADD_LENGTH,
					this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_AREACODE_ADD_LENGTH, "label", this.m_language),
					5,
					this.getFieldEditorParent()
				);
			addField(sfe);
			
			sfe = new StringFieldEditor(
					IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_TRUNCATE,
				this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_TRUNCATE, "label", this.m_language),
				5,
				this.getFieldEditorParent());
			addField(sfe);
	
			sfe = new StringFieldEditor(
				IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_INTAREA_PREFIX,
				this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_INTAREA_PREFIX, "label", this.m_language),
				5,
				this.getFieldEditorParent());		
			addField(sfe);
		

			l = new Label(this.getFieldEditorParent(), SWT.NULL);
			l = new Label(this.getFieldEditorParent(), SWT.NULL);
			l = new Label(this.getFieldEditorParent(), SWT.NULL);
			l.setText(this.m_i18n.getString(this.getNamespace(), "tk", "label", this.m_language));
			l.setFont(getBoldFont(l.getFont()));
			l = new Label(this.getFieldEditorParent(), SWT.NULL);
			
			 sfe = new StringFieldEditor(
					IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_INTERNAL_LENGTH,
					this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_INTERNAL_LENGTH, "label", this.m_language),
					5,
					this.getFieldEditorParent()
				);
			addField(sfe);
			
			sfe = new StringFieldEditor(
				IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_INTERNAL_TRUNCATE,
				this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_INTERNAL_TRUNCATE, "label", this.m_language),
				5,
				this.getFieldEditorParent()
			);
			addField(sfe);
			
			sfe = new StringFieldEditor(
					IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_INTERNAL_PREFIX,
					this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_INTERNAL_PREFIX, "label", this.m_language),
					5,
					this.getFieldEditorParent()
				);
			
			addField(sfe);
			sfe = new StringFieldEditor(
				IJAMConst.GLOBAL_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_DIAL_PREFIX,
				this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_DIAL_PREFIX, "label", this.m_language),
				5,
				this.getFieldEditorParent()
			);
			addField(sfe);
		}
	}

}
