package de.janrufmonitor.ui.jface.application.fritzbox.action;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.fritzbox.SynchronizerService;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class Refresh extends AbstractAction implements FritzBoxConst {

	private static String NAMESPACE = "ui.jface.application.fritzbox.action.Refresh";
	
	private IRuntime m_runtime;

	public Refresh() {
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
			SWTImageManager.getInstance(this.getRuntime()).getImagePath("fbrefresh.gif")
		));			
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "fritzbox_refresh";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(DisplayManager.getDefaultDisplay().getActiveShell());	
		try {				
			IRunnableWithProgress r = new IRunnableWithProgress() {
				public void run(IProgressMonitor progressMonitor) {
					IService srv = getRuntime().getServiceFactory().getService(SynchronizerService.ID);
					if (srv!=null && srv instanceof SynchronizerService && srv.isEnabled()){
						((SynchronizerService)srv).synchronize(progressMonitor);
					}
				}
			};
			pmd.setBlockOnOpen(false);
			pmd.run(true, false, r);

			//ModalContext.run(r, true, pmd.getProgressMonitor(), DisplayManager.getDefaultDisplay());
		} catch (InterruptedException e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (InvocationTargetException e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
		} 			
		m_app.updateViews(true);
		return;
	}

	public boolean isEnabled() {
		Properties cfg = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
		boolean isEnabled = Boolean.parseBoolean(cfg.getProperty("enabled", "false"));
		if (!isEnabled) return false;
		if (this.m_app!=null && this.m_app.getController()!=null) {
			Object o = this.m_app.getController().getRepository();
			return (o instanceof IWriteCallRepository);
		}
		return false;
	}
}
