package de.janrufmonitor.ui.jface.application.tellows.action;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.monitor.PhonenumberInfo;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.tellows.Tellows;
import de.janrufmonitor.service.tellows.TellowsProxy;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.jface.application.IExtendedApplicationController;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class ScoreNumberTellows extends AbstractAction {
	
	private static String NAMESPACE = "ui.jface.application.tellows.action.ScoreNumberTellows";
	
	private IRuntime m_runtime;

	public ScoreNumberTellows() {
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
		return "scorenumbertellows";
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
				Object o1 = selection.getFirstElement();
				Object o = null;
				if (o1 instanceof ICall) {
					o = ((ICall)o1).getCaller();
				}
				if (o1 instanceof ICaller) {
					o = o1;
				}
				if (o!=null) { 
					if (!((ICaller) o).getPhoneNumber().isClired() && 
						!PhonenumberInfo.isInternalNumber(((ICaller) o).getPhoneNumber()) &&
						(((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("49") ||
								((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("41")||
								((ICaller) o).getPhoneNumber().getIntAreaCode().equalsIgnoreCase("43"))
					   ) {
						
						final Object o2 = o1;
						final Object o3 = o;
						
						ProgressMonitorDialog pmd = new ProgressMonitorDialog(
								DisplayManager.getDefaultDisplay().getActiveShell());
						try {
							IRunnableWithProgress r = new IRunnableWithProgress() {
								public void run(IProgressMonitor progressMonitor) {
									progressMonitor.beginTask(getI18nManager()
											.getString(getNamespace(),
													"scoring", "label",
													getLanguage()),
											IProgressMonitor.UNKNOWN);

									progressMonitor.worked(1);

									IAttributeMap m = TellowsProxy.getInstance().getTellowsData(((ICaller) o3).getPhoneNumber().getTelephoneNumber(), 
											((ICaller) o3).getPhoneNumber().getIntAreaCode());
									if (m.size()>0) {
										((ICaller) o3).getAttributes().addAll(m);
										if (m_app.getController() instanceof IExtendedApplicationController)
											((IExtendedApplicationController)m_app.getController()).updateElement(o2, false);
										else
											m_app.getController().updateElement(o2);
									}	

									progressMonitor.done();
								}
							};
							pmd.setBlockOnOpen(false);
							pmd.run(true, false, r);

							// ModalContext.run(r, true, pmd.getProgressMonitor(),
							// DisplayManager.getDefaultDisplay());
						} catch (InterruptedException e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (InvocationTargetException e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
						}
						this.m_app.updateViews(true);
						
						this.m_app.updateViews(true);
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
