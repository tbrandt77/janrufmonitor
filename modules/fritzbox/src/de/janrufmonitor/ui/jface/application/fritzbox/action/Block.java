package de.janrufmonitor.ui.jface.application.fritzbox.action;

import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.fritzbox.FritzBoxBlockedListManager;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.FritzOS559Firmware;
import de.janrufmonitor.fritzbox.firmware.FritzOSFirmware;
import de.janrufmonitor.fritzbox.firmware.SessionIDFritzBoxFirmware;
import de.janrufmonitor.fritzbox.firmware.exception.DoBlockException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.jface.application.ITreeItemCallerData;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.string.StringUtils;

public class Block extends AbstractAction implements FritzBoxConst {

	private static String NAMESPACE = "ui.jface.application.fritzbox.action.Block";
	
	private IRuntime m_runtime;

	public Block() {
		super();
		this.setText(
			this.getI18nManager().getString(
				this.getNamespace(),
				"title",
				"label",
				this.getLanguage()
			)
		);		
		this.setImageDescriptor(new ApplicationImageDescriptor(
				SWTImageManager.getInstance(this.getRuntime()).getImagePath("blocked.gif")
			));		
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "fritzbox_block";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		Viewer v = this.m_app.getApplication().getViewer();
		if (v!=null) {
			IStructuredSelection selection = (IStructuredSelection) v.getSelection();
			if (!selection.isEmpty()) {
				Object o = selection.getFirstElement();
				if (o instanceof ICall) {
					o = ((ICall)o).getCaller();
				}
				if (o instanceof ICaller) {
					o = ((ICaller)o).getPhoneNumber();
				}
				if (o instanceof ITreeItemCallerData) {
					o = ((ITreeItemCallerData)o).getPhone();
				}				
				if (o instanceof IPhonenumber) {
					if (((IPhonenumber)o).isClired()) return;
					
					if (doBlock(((IPhonenumber)o))) {						
						this.m_app.updateViews(false);
					}
				}
			}
		}
	}
	
	private boolean doBlock(IPhonenumber n) {
		String dial = n.getTelephoneNumber();
		if (!n.getIntAreaCode().equalsIgnoreCase(this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA))) {
			dial = "00" + n.getIntAreaCode() + dial;
		}
		if (!dial.startsWith("0")) dial = "0"+dial;

		String text = getI18nManager()
		.getString(getNamespace(),
				"block", "description",
				getLanguage());
		
		text = StringUtils.replaceString(text, "{%1}", dial);
		
		if (MessageDialog.openConfirm(
				new Shell(DisplayManager.getDefaultDisplay()),
				this.getI18nManager().getString(this.getNamespace(), "block", "label", this.getLanguage()),
				text)
			) {

			FirmwareManager fwm = FirmwareManager.getInstance();
			try {
				fwm.login();							
				fwm.doBlock(dial);	
				FritzBoxBlockedListManager.invalidate();
			} catch (IOException e) {
				this.m_logger.warning(e.toString());
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						getNamespace(),
						"failedblock",	
						e));
			} catch (FritzBoxLoginException e) {
				this.m_logger.warning(e.toString());
			} catch (DoBlockException e) {
				this.m_logger.warning(e.toString());
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						getNamespace(),
						"failedblock",	
						e));
			}
			return true;
		}
		return false;
	}


	public boolean isEnabled() {
		return (FirmwareManager.getInstance().isInstance(SessionIDFritzBoxFirmware.class) || FirmwareManager.getInstance().isInstance(FritzOSFirmware.class) || FirmwareManager.getInstance().isInstance(FritzOS559Firmware.class));
	}

}
