package de.janrufmonitor.ui.jface.application.tellows.action;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.program.Program;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.monitor.PhonenumberInfo;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;

public class OpenTellows extends AbstractAction {
	
	private static String NAMESPACE = "ui.jface.application.tellows.action.OpenTellows";
	
	private IRuntime m_runtime;

	public OpenTellows() {
		super();
		this.setText(
			this.getI18nManager().getString(
				this.getNamespace(),
				"title",
				"label",
				this.getLanguage()
			)
		);
	}
	
	public String getID() {
		return "opentellows";
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
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
					if (!((ICaller) o).getPhoneNumber().isClired() && 
						!PhonenumberInfo.isInternalNumber(((ICaller) o).getPhoneNumber()) &&
						((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49")
					   ) {
						String url = "http://www.tellows.de/num/0"+((ICaller) o).getPhoneNumber().getTelephoneNumber();
						if (url!=null && url.trim().length()>0) {
							this.m_logger.info("Found valid web url to execute: "+url);
							Program.launch(url);
						}
					}
				}				
			}
		}
	}

	
}
