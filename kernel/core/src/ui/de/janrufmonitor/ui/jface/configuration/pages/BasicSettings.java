package de.janrufmonitor.ui.jface.configuration.pages;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;

public class BasicSettings extends AbstractConfigPage {
	
    private String NAMESPACE = "ui.jface.configuration.pages.BasicSettings";
    private String CONFIG_NAMESPACE = IJAMConst.GLOBAL_NAMESPACE;
    
	private IRuntime m_runtime;

	StringFieldEditor clir;
	StringFieldEditor intarea;
	StringFieldEditor area;
	StringFieldEditor delay;
	BooleanFieldEditor hina; 

	public String getParentNodeID() {
		return IConfigPage.ROOT_NODE;
	}
	
	public String getNodeID() {
		return "BasicSettings".toLowerCase();
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

	protected Control createContents(Composite parent) {
		this.setTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
		final Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		
		clir = new StringFieldEditor(
			this.CONFIG_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_CLIR,
			this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_CLIR, "label", this.m_language),
			20,
			c);
		clir.setStringValue(this.getPreferenceStore().getString(this.CONFIG_NAMESPACE+SEPARATOR+"clir"));
		
		intarea = new StringFieldEditor(
			this.CONFIG_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_INTAREA,
			this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_INTAREA, "label", this.m_language),
			5,
			c);
		intarea.setStringValue(this.getPreferenceStore().getString(this.CONFIG_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_INTAREA));
			
		
		area = new StringFieldEditor(
			this.CONFIG_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_AREACODE,
			this.m_i18n.getString(this.getNamespace(), IJAMConst.GLOBAL_AREACODE, "label", this.m_language),
			10,
			c
		);
		area.setStringValue(this.getPreferenceStore().getString(this.CONFIG_NAMESPACE+SEPARATOR+IJAMConst.GLOBAL_AREACODE));

		
		if (isExpertMode()) {
			delay = new StringFieldEditor(
				"monitor.MonitorListener"+SEPARATOR+"delay",
				this.m_i18n.getString(this.getNamespace(), "delay", "label", this.m_language),
				3,
				c);
			delay.setStringValue(this.getPreferenceStore().getString("monitor.MonitorListener"+SEPARATOR+"delay"));
			
			hina = new BooleanFieldEditor(
					"service.HibernateDetect"+SEPARATOR+"enabled",
					this.m_i18n.getString(this.getNamespace(), "hina_enabled", "label", this.m_language),
					1,
					c);
			hina.setPreferenceStore(this.getPreferenceStore());
			hina.doLoad();

		}
		return c;
	}
	
	public String getConfigNamespace() {
		return this.CONFIG_NAMESPACE;
	}
	
	protected void performDefaults() {
		super.performDefaults();
		clir.setStringValue(this.getPreferenceStore().getDefaultString(clir.getPreferenceName()));
		intarea.setStringValue(this.getPreferenceStore().getDefaultString(intarea.getPreferenceName()));
		area.setStringValue(this.getPreferenceStore().getDefaultString(area.getPreferenceName()));
		if (isExpertMode()) {
			delay.setStringValue(this.getPreferenceStore().getDefaultString(delay.getPreferenceName()));
			hina.doLoadDefault();
		}
	}
	
	public boolean performOk() {
		this.getPreferenceStore().setValue(clir.getPreferenceName(), clir.getStringValue());
		this.getPreferenceStore().setValue(intarea.getPreferenceName(), intarea.getStringValue());
		this.getPreferenceStore().setValue(area.getPreferenceName(), area.getStringValue());
		if (isExpertMode()) {
			this.getPreferenceStore().setValue(delay.getPreferenceName(), delay.getStringValue());
			this.getPreferenceStore().setValue(hina.getPreferenceName(), hina.getBooleanValue());
		}
		return super.performOk();
	}
}
