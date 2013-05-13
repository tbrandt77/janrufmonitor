package de.janrufmonitor.ui.jface.dialogs;

import java.util.logging.Level;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.IPropagator;
import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.AbstractCommand;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.trayicon.TrayIcon;
import de.janrufmonitor.ui.jface.application.controls.ErrorDialog;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.string.StringUtils;

public class DialogPropagator extends AbstractCommand implements IPropagator {

	private String NAMESPACE = "ui.jface.dialogs.DialogPropagator";

	private IRuntime m_runtime;
	
	public DialogPropagator() {
		super();
		PropagationFactory.getInstance().add(this);
	}
	
	public void propagate(final Message m) {
		final Throwable t = m.getThrowable();
		
		Thread td = new Thread(
			new Runnable() {
				public void run() {
					DisplayManager.getDefaultDisplay().asyncExec (new Runnable () {
						public void run () {
							openDialog(m.getLevel(), m.getNamespace(), m.getMessage(), m.getVariables(), (t!=null ? t.toString(): ""));
							if (m.getLevel().equalsIgnoreCase(Message.ERROR)) {
								m_logger.log(Level.SEVERE, m.getMessage(), t);
							}
							if (m.isSetErrorIcon()) {
								IService tray = getRuntime().getServiceFactory().getService("TrayIcon");
								if (tray != null && tray instanceof TrayIcon) {
									((TrayIcon) tray).setIconStateError();
								}
							}
						}
					});
				}
			}
		);
		td.start();
		try {
			td.join();
		} catch (InterruptedException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private synchronized void openDialog(String level, String namespace, String id, String[] variables, String cause) {
		Display d = DisplayManager.getDefaultDisplay();
		Shell shell = new Shell(d);
		shell.getClass();
		
		String message = this.m_i18n.getString(namespace, id, "label", this.m_language);
		if (message.equalsIgnoreCase(id)) {
			namespace = Message.DEFAULT_NAMESPACE;
			message = this.m_i18n.getString(Message.DEFAULT_NAMESPACE, "unknown", "label", this.m_language);
		} else {
			if (variables!=null) {
				for (int i=0;i<variables.length;i++) {
					message = StringUtils.replaceString(message, "{%"+(i+1)+"}", variables[i]);
				}
			}
		}
		
		String state = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(namespace, id+"_state");
		String baseUrl = null;
		String baseExt = null;
		String readMoreLabel = null;
		String url = this.m_i18n.getString(namespace, id, "description", this.m_language);
		if (!url.equalsIgnoreCase(id)) {
			readMoreLabel = this.m_i18n.getString(getNamespace(), "readmore", "label", this.m_language);
			baseUrl = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(getNamespace(), "error_url");
			baseExt = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(getNamespace(), "error_extension");
		}
		
		if (!state.equalsIgnoreCase("true")) {
			ErrorDialog ed = new ErrorDialog(
				this.getLevel(level),
				this.m_i18n.getString(getNamespace(), level, "label", this.m_language),
				message,
				this.m_i18n.getString(getNamespace(), "togglemessage", "label", this.m_language),
				false,
				((baseUrl==null || baseUrl.trim().length()==0)? null : baseUrl + url + baseExt),
				readMoreLabel
			);
			ErrorDialog.setDefaultImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
			ed.open();
			
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(namespace, id+"_state", Boolean.toString(ed.getToggleState()));
			this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
		}	
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public void execute() {

	}

	public boolean isExecutable() {
		return true;
	}

	public boolean isExecuting() {
		return false;
	}

	public String getID() {
		return "Dialog";
		//return DialogPropagator.class.getName();
	}
	
	private int getLevel(String level) {
		if (level.equalsIgnoreCase(Message.INFO)) return ErrorDialog.INFORMATION;
		if (level.equalsIgnoreCase(Message.WARNING)) return ErrorDialog.WARNING;
		return ErrorDialog.ERROR;
	}
}
