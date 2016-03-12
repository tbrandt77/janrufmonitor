package de.janrufmonitor.ui.jface.application.action;

import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;

public class HelpAction extends AbstractAction {

	private static String NAMESPACE = "ui.jface.application.action.HelpAction";
	private static String helpPage = "http://www.janrufmonitor.de/dokumentation/";

	private IRuntime m_runtime;

	public HelpAction() {
		super();
		this.setText(
			this.getI18nManager().getString(
				this.getNamespace(),
				"title",
				"label",
				this.getLanguage()
			)
		);
		this.setAccelerator(SWT.F1);
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "help";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		Program.launch(HelpAction.helpPage);
	}
}
