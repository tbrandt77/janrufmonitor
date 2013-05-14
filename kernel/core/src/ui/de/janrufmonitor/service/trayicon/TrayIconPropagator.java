package de.janrufmonitor.service.trayicon;

import org.eclipse.swt.SWT;

import de.janrufmonitor.exception.IPropagator;
import de.janrufmonitor.exception.Message;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.util.string.StringUtils;

public class TrayIconPropagator implements IPropagator {

	protected String m_language;
	protected II18nManager m_i18n;
	
	public TrayIconPropagator() {
		this.m_language = 
			PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(
				IJAMConst.GLOBAL_NAMESPACE,
				IJAMConst.GLOBAL_LANGUAGE
			);
		this.m_i18n = PIMRuntime.getInstance().getI18nManagerFactory().getI18nManager();
	}
	
	public String getID() {
		return "Tray";
	}

	public void propagate(Message m) {
		String message = null;
		String title = null;
		if (m.getNamespace().equalsIgnoreCase(Message.DEFAULT_NAMESPACE) && !m.getMessage().equalsIgnoreCase("unknown") && (m.getVariables()==null || m.getVariables().length==0)) {		
			message = m.getThrowable().getMessage();
			title = m.getMessage();
		} else {
			try {
				message = this.m_i18n.getString(m.getNamespace(), m.getMessage(), "label", this.m_language);
			} catch (NullPointerException ex) {
				message = m.getMessage();
			}
			if (message.equalsIgnoreCase(m.getMessage())) {
				message = this.m_i18n.getString(Message.DEFAULT_NAMESPACE, "unknown", "label", this.m_language);
			} else {
				if (m.getVariables()!=null) {
					for (int i=0;i<m.getVariables().length;i++) {
						message = StringUtils.replaceString(message, "{%"+(i+1)+"}", m.getVariables()[i]);
					}
				}
			}
			title = this.m_i18n.getString(m.getNamespace(), "title", "label", this.m_language);			
		}
		
		IService trayicon = PIMRuntime.getInstance().getServiceFactory().getService("TrayIcon");
		if (trayicon!=null && trayicon.isRunning()) {
			int status = SWT.ICON_INFORMATION;
			
			if (m.getLevel().equalsIgnoreCase(Message.WARNING))
				status = SWT.ICON_WARNING;
			
			if (m.getLevel().equalsIgnoreCase(Message.ERROR))
				status = SWT.ICON_ERROR;
						
			((TrayIcon)trayicon).setToolTip(message, title, status);
		}

	}

}
