package de.janrufmonitor.ui.jface.application.tellows.action;

import java.util.logging.Level;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.program.Program;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.tellows.Tellows;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.swt.SWTImageManager;

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
		this.setImageDescriptor(new ApplicationImageDescriptor(
			SWTImageManager.getInstance(this.getRuntime()).getImagePath("tellows.png")
		));	
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
						!PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(((ICaller) o).getPhoneNumber()) &&
						(((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49") ||
								((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("41")||
								((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("43"))
					   ) {
						StringBuffer url = new StringBuffer();
						url.append("http://www.tellows.");
						if (((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49"))
							url.append("de");
						if (((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("43"))
							url.append("at");
						if (((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("41"))
							url.append("ch");
						url.append("/num/0");
						url.append(((ICaller) o).getPhoneNumber().getTelephoneNumber());

						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Found valid web url to execute: "+url.toString());
						Program.launch(url.toString());
					}
				}				
			}
		}
	}
	
	public boolean isEnabled() {
		IService tellows = getRuntime().getServiceFactory().getService(Tellows.ID);
		if (tellows!=null && tellows.isEnabled() && tellows instanceof Tellows) {
			return ((Tellows)tellows).isTellowsActivated();
		}
		return false;
	}

	
}
