package de.janrufmonitor.ui.jface.application.client.action;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.client.Client;
import de.janrufmonitor.service.client.request.handler.GetDialExtensions;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IHttpResponse;
import de.janrufmonitor.service.commons.http.IRequester;
import de.janrufmonitor.service.commons.http.RequesterFactory;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.jface.application.ITreeItemCallerData;
import de.janrufmonitor.ui.jface.application.dialer.ClientDialerDialog;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class ClientClickDialAction extends AbstractAction  {

	private static String NAMESPACE = "ui.jface.application.client.action.ClientClickDialAction";
	
	private IRuntime m_runtime;

	public ClientClickDialAction() {
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
					if (PhonenumberAnalyzer.getInstance().isInternal((IPhonenumber)o)) {
						dial = ((IPhonenumber)o).getCallNumber();
					} else {
						if (!((IPhonenumber)o).getIntAreaCode().equalsIgnoreCase(this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA))) {
							dial = "00" + ((IPhonenumber)o).getIntAreaCode() + dial;
						}
						if (!dial.startsWith("0")) dial = "0"+dial;
					}
					
					ClientDialerDialog id = new ClientDialerDialog(new Shell(DisplayManager.getDefaultDisplay()), dial);
					id.open();
						
					this.m_app.updateViews(false);					
				}
			}
		}
	}

	public boolean isEnabled() {
		// introduced since some TAPIs might reset and stop monitoring after calling GetDialExtension
		boolean isSecureMonitor = Boolean.parseBoolean(System.getProperty(IJAMConst.SYSTEM_MONITOR_SECURE, "false"));
		if (isSecureMonitor) return false;
		
		if (isConnected()) {
			IRequester r = this.getRequester(new GetDialExtensions());
			IHttpResponse resp = r.request();
			return (resp.getCode()!=404 && resp.getCode()!=500);
		}
		return false;
	}
	
	private boolean isConnected() {
		IService client = PIMRuntime.getInstance().getServiceFactory().getService("Client");
		if (client!=null && client instanceof Client) {
			return ((Client)client).isConnected();
		}
		return false;
	}
	
	private IRequester getRequester(IHttpRequest request) {
		return RequesterFactory.getInstance().
			createRequester(request);
	}

}
