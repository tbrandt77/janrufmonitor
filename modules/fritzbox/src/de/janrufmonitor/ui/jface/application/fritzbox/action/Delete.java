package de.janrufmonitor.ui.jface.application.fritzbox.action;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.swt.DisplayManager;

public class Delete extends AbstractAction implements FritzBoxConst {

	private static String NAMESPACE = "ui.jface.application.fritzbox.action.Delete";
	
	private IRuntime m_runtime;

	public Delete() {
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
		return "fritzbox_delete";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(DisplayManager.getDefaultDisplay().getActiveShell());	
		try {		

			if (MessageDialog.openConfirm(
					new Shell(DisplayManager.getDefaultDisplay()),
					this.getI18nManager().getString(this.getNamespace(), "delete", "label", this.getLanguage()),
					this.getI18nManager().getString(this.getNamespace(), "delete", "description", this.getLanguage())
				)) {
				
				IRunnableWithProgress r = new IRunnableWithProgress() {
				public void run(IProgressMonitor progressMonitor) {
					progressMonitor.beginTask(getI18nManager()
							.getString(getNamespace(),
									"deleteprogress", "label",
									getLanguage()), IProgressMonitor.UNKNOWN);
					
					progressMonitor.worked(1);

					progressMonitor.setTaskName(getI18nManager()
							.getString(getNamespace(),
									"loginprogress", "label",
									getLanguage()));
					FirmwareManager fwm = FirmwareManager.getInstance();
					try {
						fwm.login();
						
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"deleteprogress", "label",
										getLanguage()));
						
						fwm.deleteCallList();	
						
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"finished", "label",
										getLanguage()));
						
					} catch (IOException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
								getNamespace(),
								"faileddelete",	
								e));
					} catch (FritzBoxLoginException e) {
						m_logger.warning(e.toString());
					} catch (DeleteCallListException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
								getNamespace(),
								"faileddelete",	
								e));
					}
					
					progressMonitor.done();
				}
			};

				pmd.setBlockOnOpen(false);
				pmd.run(true, false, r);
				m_app.updateViews(true);
			}
			//ModalContext.run(r, true, pmd.getProgressMonitor(), DisplayManager.getDefaultDisplay());
		} catch (InterruptedException e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (InvocationTargetException e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
		} 
		return;
	}

}
