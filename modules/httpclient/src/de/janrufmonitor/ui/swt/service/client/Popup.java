package de.janrufmonitor.ui.swt.service.client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.client.state.*;
import de.janrufmonitor.ui.swt.DisplayManager;

public class Popup implements IClientStateMonitor {

	private class DefaultPopupThread implements Runnable {

		private String m_msg;
		private String m_title;
		private int m_icon;
	
		public DefaultPopupThread(int icon, String title, String message) {
			this.m_msg = message;
			this.m_icon = icon;
			this.m_title = title;
		}

		public void run() {
			Display d = DisplayManager.getDefaultDisplay();
			Shell s = new Shell(d);
			s.setBounds(0,0,0,0);
			MessageBox dialog = new MessageBox(s, this.m_icon);
			dialog.setText(this.m_title);
			dialog.setMessage(this.m_msg);
			dialog.open();
		}
		
	}

	private II18nManager m_i18n;
	private String m_language;
	
	private String NAMESPACE = "ui.swt.service.client.Popup";

	public Popup() {
		ClientStateManager.getInstance().register(this);
		this.m_i18n = PIMRuntime.getInstance().getI18nManagerFactory().getI18nManager();
		this.m_language = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(
			IJAMConst.GLOBAL_NAMESPACE,
			IJAMConst.GLOBAL_LANGUAGE
		);
	}

	public String getID() {
		return "Popup";
	}

	public void acceptState(int state, String message) {
	
		switch(state) {
			case IClientStateMonitor.NETWORK_ERROR: 
				DisplayManager.getDefaultDisplay().asyncExec(
					new DefaultPopupThread(
						SWT.ICON_ERROR,
						this.m_i18n.getString(NAMESPACE, "error", "label", this.m_language),
						this.m_i18n.getString(NAMESPACE, "networkerror", "label", this.m_language) + message
					)); 
				break;
			case IClientStateMonitor.SERVER_NOT_FOUND: 
				DisplayManager.getDefaultDisplay().asyncExec(
					new DefaultPopupThread(
						SWT.ICON_ERROR,
						this.m_i18n.getString(NAMESPACE, "error", "label", this.m_language),
						this.m_i18n.getString(NAMESPACE, "servernotfound", "label", this.m_language) + message
					)); 
				break;
			case IClientStateMonitor.SERVER_NOT_AUTHORIZED: 
				DisplayManager.getDefaultDisplay().asyncExec(
					new DefaultPopupThread(
						SWT.ICON_ERROR, 
						this.m_i18n.getString(NAMESPACE, "error", "label", this.m_language),
						this.m_i18n.getString(NAMESPACE, "servernotauthorized", "label", this.m_language) + message
					)); 
				break;			
			case IClientStateMonitor.SERVER_SHUTDOWN: 
				PropagationFactory.getInstance().fire(
						new Message(Message.WARNING,
							NAMESPACE,
							"servershutdown",
							new String[] {},
							new Exception("Server was shutdown."),
							false),
						"Tray");
				break;
		}
	}

}
