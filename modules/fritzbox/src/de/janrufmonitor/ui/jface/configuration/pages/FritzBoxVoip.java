package de.janrufmonitor.ui.jface.configuration.pages;

import java.lang.reflect.Method;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Label;

import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.TR064FritzBoxFirmware;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.ui.jface.configuration.AbstractFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;
import de.janrufmonitor.ui.swt.DisplayManager;

public class FritzBoxVoip extends AbstractFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.FritzBoxVoip";

    private IRuntime m_runtime;
    private StringFieldEditor user;
    private ComboFieldEditor mode;

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
		
		mode = new ComboFieldEditor(
				getConfigNamespace()+SEPARATOR+"boxloginmode",
				this.m_i18n.getString(this.getNamespace(), "boxloginmode", "label", this.m_language),
				new String[][] { 
					{this.m_i18n.getString(this.getNamespace(), "userpassword", "label", this.m_language), "0"}, 
					{this.m_i18n.getString(this.getNamespace(), "password_only", "label", this.m_language), "1"}
				},
			this.getFieldEditorParent()
		);
		addField(mode);	
		
		this.user = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"boxuser",
			this.m_i18n.getString(this.getNamespace(), "boxuser", "label", this.m_language),
			this.getFieldEditorParent()
		);
		addField(this.user);

		String state = this.getPreferenceStore().getString(getConfigNamespace()+SEPARATOR+"boxloginmode");
		if (state.equalsIgnoreCase("1")) {
			this.user.setEnabled(false, this.getFieldEditorParent());
			this.user.getTextControl(getFieldEditorParent()).setBackground(new Color(DisplayManager.getDefaultDisplay(), 190, 190, 190));
			this.user.setStringValue("");
		}
		
		sfe = new StringFieldEditor(
				getConfigNamespace()+SEPARATOR+"boxpassword",
			this.m_i18n.getString(this.getNamespace(), "boxpassword", "label", this.m_language),
			this.getFieldEditorParent()
		);
		sfe.getTextControl(this.getFieldEditorParent()).setEchoChar('*');
		addField(sfe);
		
		if (isExpertMode()) {
			IntegerFieldEditor ife = new IntegerFieldEditor(
				getConfigNamespace()+SEPARATOR+"retrymax",
				this.m_i18n.getString(this.getNamespace(), "retrymax", "label", this.m_language),
				this.getFieldEditorParent()
			);
			ife.setTextLimit(3);
			addField(ife);
			
			ife = new IntegerFieldEditor(
				getConfigNamespace()+SEPARATOR+"retrytimeouts",
				this.m_i18n.getString(this.getNamespace(), "retrytimeouts", "label", this.m_language),
				this.getFieldEditorParent()
			);
			ife.setTextLimit(3);
			addField(ife);	
			
			bfe = new BooleanFieldEditor(
					getConfigNamespace()+SEPARATOR+"tr064off",
				this.m_i18n.getString(this.getNamespace(), "tr064off", "label", this.m_language),
				this.getFieldEditorParent()
			);
			addField(bfe);
			
			new Label(this.getFieldEditorParent(), SWT.NULL);
			new Label(this.getFieldEditorParent(), SWT.NULL);
			
			ComboFieldEditor cfe = new ComboFieldEditor(
					getConfigNamespace()+SEPARATOR+"boxclickdial",
					this.m_i18n.getString(this.getNamespace(), "boxclickdial", "label", this.m_language),
					new String[][] { 
						{this.m_i18n.getString(this.getNamespace(), "manual", "label", this.m_language), "0"}, 
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
			
			sfe = new StringFieldEditor(
					getConfigNamespace()+SEPARATOR+"dialprefixes",
				this.m_i18n.getString(this.getNamespace(), "dialprefixes", "label", this.m_language),
				this.getFieldEditorParent()
			);
			sfe.setEmptyStringAllowed(true);
			addField(sfe);
			
			new Label(this.getFieldEditorParent(), SWT.NULL);
			new Label(this.getFieldEditorParent(), SWT.NULL);
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
		
		new Label(this.getFieldEditorParent(), SWT.NULL);
		Label statusl= new Label(this.getFieldEditorParent(), 0);
		statusl.setText(this.m_i18n.getString(this.getNamespace(), "status", "label", this.m_language));

		Label status_observer = new Label(this.getFieldEditorParent(), SWT.NULL);
		status_observer.setText(this.m_i18n.getString(this.getNamespace(), "statuso", "label", this.m_language)+((fbMonitor!=null && fbMonitor.isStarted()) ? "OK" : "---"));
		new Label(this.getFieldEditorParent(), SWT.NULL);
		
		FirmwareManager fwm = FirmwareManager.getInstance();
		fwm.startup();
		
		if (fwm.isLoggedIn() && !fwm.isInstance(TR064FritzBoxFirmware.class)) {
			Label status_sync = new Label(this.getFieldEditorParent(), SWT.NULL);
			status_sync.setText(this.m_i18n.getString(this.getNamespace(), "statuss", "label", this.m_language)+(fwm.isLoggedIn() ? "OK" : "---"));
			new Label(this.getFieldEditorParent(), SWT.NULL);
			
			if (fbMonitor!=null && fbMonitor.isStarted() && fwm.isLoggedIn()) {
				// set icon to colored
				IService tray = this.getRuntime().getServiceFactory().getService("TrayIcon");
				try {
					Method m = tray.getClass().getMethod("setIconStateActive", new Class[] {});
					if (m!=null) {
						m.invoke(tray, new Object[] {});
					}
				} catch (Exception ex) {
				}
			}
			
			if ((fbMonitor==null || !fbMonitor.isStarted()) && !fwm.isLoggedIn()) {
				// set icon to colored
				IService tray = this.getRuntime().getServiceFactory().getService("TrayIcon");
				try {
					Method m = tray.getClass().getMethod("setIconStateInactive", new Class[] {});
					if (m!=null) {
						m.invoke(tray, new Object[] {});
					}
				} catch (Exception ex) {
				}
			}
		}
	}
	

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event!= null && event.getSource() instanceof ComboFieldEditor) {
			ComboFieldEditor cfe = (ComboFieldEditor) event.getSource();
			if (cfe != null && cfe.getPreferenceName().equalsIgnoreCase(getConfigNamespace()+SEPARATOR+"boxloginmode")) {
				String value = (String) event.getNewValue();
				if (value.equalsIgnoreCase("0")) {
					this.user.setEnabled(true, this.getFieldEditorParent());
					this.user.getTextControl(getFieldEditorParent()).setBackground(new Color(DisplayManager.getDefaultDisplay(), 255, 255, 255));
				} else {
					this.user.setEnabled(false, this.getFieldEditorParent());
					this.user.getTextControl(getFieldEditorParent()).setBackground(new Color(DisplayManager.getDefaultDisplay(), 190, 190, 190));
					this.user.setStringValue("");
				}
			}
		}
	}

}
