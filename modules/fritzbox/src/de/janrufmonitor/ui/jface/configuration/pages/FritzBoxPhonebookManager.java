package de.janrufmonitor.ui.jface.configuration.pages;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;

import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.FritzOS559Firmware;
import de.janrufmonitor.fritzbox.firmware.FritzOSFirmware;
import de.janrufmonitor.fritzbox.firmware.SessionIDFritzBoxFirmware;
import de.janrufmonitor.fritzbox.firmware.UnitymediaFirmware;
import de.janrufmonitor.fritzbox.firmware.exception.GetAddressbooksException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractServiceFieldEditorConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;


public class FritzBoxPhonebookManager extends AbstractServiceFieldEditorConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.FritzBoxPhonebookManager";
    private String CONFIG_NAMESPACE = "repository.FritzBoxPhonebookManager";

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
		return "FritzBoxPhonebookManager".toLowerCase();
	}

	public int getNodePosition() {
		return 11;
	}
	
	protected void createFieldEditors() {
		String label = this.m_i18n.getString(this.getNamespace(), "enabled", "label", this.m_language);
		if (label.length()<150)
			for (int i=150;i>label.length();i--){
				label += " ";
			}
		
		BooleanFieldEditor bfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"enabled",
			label,
			this.getFieldEditorParent()
		);		
		bfe.setEnabled((FirmwareManager.getInstance().isInstance(UnitymediaFirmware.class)||FirmwareManager.getInstance().isInstance(SessionIDFritzBoxFirmware.class)||FirmwareManager.getInstance().isInstance(FritzOSFirmware.class)||FirmwareManager.getInstance().isInstance(FritzOS559Firmware.class)), this.getFieldEditorParent());
		addField(bfe);
		
		if (FirmwareManager.getInstance().isInstance(FritzOSFirmware.class)||FirmwareManager.getInstance().isInstance(FritzOS559Firmware.class)) {
			try {
				Map adb = FirmwareManager.getInstance().getAddressbooks();
				String[][] list = new String[adb.size()][2];
				Iterator i = adb.keySet().iterator();
				int c = 0;
				while (i.hasNext()) {
					list[c][1] = ((Integer) i.next()).toString();
					list[c][0] = (String) adb.get(Integer.parseInt(list[c][1]));
					c++;
				}
				ComboFieldEditor cfe = new ComboFieldEditor(
					getConfigNamespace()+SEPARATOR+"ab",	
					this.m_i18n.getString(this.getNamespace(), "ab", "label", this.m_language),
					list,	
					this.getFieldEditorParent()
				);
				addField(cfe);
					
			} catch (GetAddressbooksException e) {
				this.m_logger.severe(e.getMessage());
			} catch (IOException e) {
				this.m_logger.severe(e.getMessage());
			}
		}
		
	}

}
