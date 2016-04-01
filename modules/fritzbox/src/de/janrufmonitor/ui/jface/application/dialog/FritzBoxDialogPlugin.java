package de.janrufmonitor.ui.jface.application.dialog;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.util.string.StringUtils;

public class FritzBoxDialogPlugin extends AbstractDialogPlugin implements FritzBoxConst {

	private static String NAMESPACE = "ui.jface.application.fritzbox.action.ClickDialAction";

	public FritzBoxDialogPlugin() {
		super();
	}

	public String getLabel() {
		return this.getI18nManager().getString(this.getNamespace(), "label", "label", this.getLanguage());
	}


	public void run() {
		new SWTExecuter(this.getLabel()) {
			protected void execute() {				
				ICaller o = m_dialog.getCall().getCaller();
				String dial = ((ICaller)o).getPhoneNumber().getTelephoneNumber();
				if (!((ICaller)o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase(getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA))) {
					dial = "00" + ((ICaller)o).getPhoneNumber().getIntAreaCode() + dial;
				}
				if (!dial.startsWith("0")) dial = "0"+dial;

				String text = getI18nManager()
				.getString(getNamespaceDial(),
						"dial", "description",
						getLanguage());
				
				text = StringUtils.replaceString(text, "{%1}", dial);
				
				if (MessageDialog.openConfirm(
						new Shell(DisplayManager.getDefaultDisplay()),
						getI18nManager().getString(getNamespaceDial(), "success", "label", getLanguage()),
						text)
					) {

					Properties config = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
					
					FirmwareManager fwm = FirmwareManager.getInstance();
					try {
						if (!fwm.isLoggedIn())
							fwm.login();
						
						fwm.doCall(dial, config.getProperty(CFG_CLICKDIAL, "50"));
							text = getI18nManager()
							.getString(getNamespaceDial(),
									"success", "description",
									getLanguage());
							
							text = StringUtils.replaceString(text, "{%1}", dial);
							
							MessageDialog.openInformation(
								new Shell(DisplayManager.getDefaultDisplay()),
								getI18nManager()
								.getString(getNamespaceDial(),
										"success", "label",
										getLanguage()),
								text
							);
											
					} catch (IOException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
										getNamespaceDial(),
								"faileddial",	
								e));
					} catch (FritzBoxLoginException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
										getNamespaceDial(),
								"faileddial",	
								e));
					} catch (DoCallException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
										getNamespaceDial(),
								"faileddial",	
								e));
					}
					
				}
			}
		}.start();		
	}

	public boolean isEnabled() {
		if (this.m_dialog.getCall().getCaller().getPhoneNumber().isClired()) return false;
		IMonitor m = this.getRuntime().getMonitorListener().getMonitor(FritzBoxMonitor.ID);
		return (m!=null && m.isStarted());
	}

	private String getNamespace() {
		return NAMESPACE;
	}
	
	private String getNamespaceDial() {
		return "ui.jface.application.fritzbox.action.ClickDialAction";
	}
}
