package de.janrufmonitor.ui.jface.application.tapi.action;

//import java.util.logging.Level;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.monitor.PhonenumberInfo;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.jface.application.ITreeItemCallerData;
import de.janrufmonitor.ui.jface.application.dialer.TapiDialerDialog;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.xtapi.XTapiConst;

public class TapiClickDialAction extends AbstractAction implements XTapiConst {

	private static String NAMESPACE = "ui.jface.application.tapi.action.TapiClickDialAction";
	
	private IRuntime m_runtime;

	public TapiClickDialAction() {
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
			SWTImageManager.getInstance(this.getRuntime()).getImagePath("docall.gif")
		));			
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "tapi_clickdial";
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
					o = ((ICaller)o).getPhoneNumber();
				}
				if (o instanceof ITreeItemCallerData) {
					o = ((ITreeItemCallerData)o).getPhone();
				}				
				if (o instanceof IPhonenumber) {
					if (((IPhonenumber)o).isClired()) return;
					
					String dial = ((IPhonenumber)o).getTelephoneNumber();
					if (PhonenumberInfo.isInternalNumber(dial)) {
						dial = ((IPhonenumber)o).getCallNumber();
					} else {
						if (!((IPhonenumber)o).getIntAreaCode().equalsIgnoreCase(this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA))) {
							dial = "00" + ((IPhonenumber)o).getIntAreaCode() + dial;
						}
						if (!dial.startsWith("0")) dial = "0"+dial;
						
						// added 2010/03/06: check for dial prefix for outgoing calls
						// removed 2010/06/25: double addition of prefix. Is already added in Dialogue
//						if (this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
//							if (this.m_logger.isLoggable(Level.INFO))
//								this.m_logger.info("Using dial prefix: "+this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX));
//							dial = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
//						}
					}
					
					TapiDialerDialog id = new TapiDialerDialog(new Shell(DisplayManager.getDefaultDisplay()), dial);
					id.open();
						
					this.m_app.updateViews(false);					
				}
			}
		}
	}

}
