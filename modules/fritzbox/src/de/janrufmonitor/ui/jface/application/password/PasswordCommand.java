package de.janrufmonitor.ui.jface.application.password;

import java.util.Properties;

import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAsyncDisplayCommand;
import de.janrufmonitor.ui.swt.DisplayManager;


public class PasswordCommand extends AbstractAsyncDisplayCommand implements IConfigurable {

	private static String NAMESPACE = "ui.jface.application.password.PasswordDialog";
	
	private IRuntime m_runtime;
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return PasswordCommand.NAMESPACE;
	}

	public boolean isExecutable() {
		return true;
	}

	public String getID() {
		return "PasswordDialog";
	}

	public String getConfigurableID() {
		return this.getID();
	}

	public void setConfiguration(Properties configuration) {
	}

	public void asyncExecute() {
		System.setProperty("jam.fritzbox.session.ispwdialogvisible", "true");
		PasswordDialog id = new PasswordDialog(new Shell(DisplayManager.getDefaultDisplay()));
		id.open();
	}

}
