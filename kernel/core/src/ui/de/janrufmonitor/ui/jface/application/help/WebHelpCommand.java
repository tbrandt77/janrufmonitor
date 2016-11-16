package de.janrufmonitor.ui.jface.application.help;

import java.util.Properties;

import org.eclipse.swt.program.Program;

import de.janrufmonitor.framework.command.AbstractCommand;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class WebHelpCommand extends AbstractCommand implements IConfigurable {

	private static String NAMESPACE = "ui.jface.application.help.WebHelpCommand";
	private static String helpPage = "https://www.janrufmonitor.de/documentation.html";

	private IRuntime m_runtime;
	private boolean isExecuting;
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return WebHelpCommand.NAMESPACE;
	}

	public boolean isExecutable() {
		return true;
	}

	public String getID() {
		return "WebHelpCommand";
	}

	public String getConfigurableID() {
		return this.getID();
	}

	public void setConfiguration(Properties configuration) {
	}

	public boolean isExecuting() {
		return this.isExecuting;
	}
	
	public void execute() {
		this.isExecuting = true;
		Program.launch(WebHelpCommand.helpPage);
		this.isExecuting = false;
	}

}
