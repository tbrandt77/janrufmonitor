package de.janrufmonitor.ui.jface.application.info;

import java.util.Properties;

import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAsyncDisplayCommand;
import de.janrufmonitor.ui.jface.dialogs.InfoDialog;
import de.janrufmonitor.ui.swt.DisplayManager;

public class InfoCommand extends AbstractAsyncDisplayCommand implements IConfigurable {

	private static String NAMESPACE = "ui.jface.dialogs.InfoDialog";
	
	private IRuntime m_runtime;

	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return InfoCommand.NAMESPACE;
	}

	public boolean isExecutable() {
		return true;
	}

	public String getID() {
		return "InfoCommand";
	}

	public String getConfigurableID() {
		return this.getID();
	}

	public void setConfiguration(Properties configuration) {
	}

	public void asyncExecute() {
		Shell s = new Shell(DisplayManager.getDefaultDisplay());
		InfoDialog id = new InfoDialog(s);
		DisplayManager.forceForeground(s);
		id.open();
	}

}
