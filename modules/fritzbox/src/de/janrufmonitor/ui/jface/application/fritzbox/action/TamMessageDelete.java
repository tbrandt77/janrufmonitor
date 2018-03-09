package de.janrufmonitor.ui.jface.application.fritzbox.action;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.util.io.PathResolver;

public class TamMessageDelete extends AbstractAction implements FritzBoxConst {

	private static String NAMESPACE = "ui.jface.application.fritzbox.action.TamMessageDelete";
	
	private IRuntime m_runtime;

	public TamMessageDelete() {
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
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "fritzbox_tamplay";
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
					File tamMessage = new File(new File(PathResolver.getInstance(PIMRuntime.getInstance()).getDataDirectory() + File.separator + "fritzbox-messages"), ((ICall)o).getUUID()+".wav");
					if (tamMessage.exists() && tamMessage.isFile() && tamMessage.length()>0) {
						if (MessageDialog.openConfirm(
								new Shell(DisplayManager.getDefaultDisplay()),
								this.getI18nManager().getString(this.getNamespace(), "delete", "label", this.getLanguage()),
								this.getI18nManager().getString(this.getNamespace(), "delete", "description", this.getLanguage())
							)) {
							if (!tamMessage.delete()) tamMessage.deleteOnExit();
							this.m_app.updateViews(false);
						}
					} else {
						new SWTExecuter() {

							protected void execute() {
								int style = SWT.APPLICATION_MODAL
										| SWT.OK;
								MessageBox messageBox = new MessageBox(
										new Shell(DisplayManager
												.getDefaultDisplay()),
										style);
								messageBox
										.setMessage(getI18nManager()
												.getString(
														getNamespace(),
														"notam",
														"label",
														getLanguage()));
								messageBox.open();
							}
						}.start();
						return;
					}
				}	
			}
		}
		
	}

}
