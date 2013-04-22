package de.janrufmonitor.ui.jface.application.journal.action;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.RepositoryManagerComparator;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IModifierService;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.IExtendedApplicationController;
import de.janrufmonitor.ui.jface.wizards.JournalCallerWizard;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class ReIdentifyAction extends AbstractAction {

	private static String NAMESPACE = "ui.jface.application.journal.action.ReIdentifyAction";

	private IRuntime m_runtime;

	public ReIdentifyAction() {
		super();
		this.setText(this.getI18nManager().getString(this.getNamespace(),
				"title", "label", this.getLanguage()));
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "journal_reidentify";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		Viewer v = this.m_app.getApplication().getViewer();
		if (v != null && v instanceof TableViewer) {
			IStructuredSelection selection = (IStructuredSelection) v
					.getSelection();
			if (!selection.isEmpty()) {
				final ICall call = (ICall) selection.getFirstElement();
				ICaller caller = call.getCaller();
				
				if (caller.getPhoneNumber().isClired()) {
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
													"clir",
													"label",
													getLanguage()));
							messageBox.open();
						}
					}.start();
					return;
				}
				
				List cms = getAllActiveCallerManagers(getRuntime());
				if (cms.size()==0) {
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
													"nocms",
													"label",
													getLanguage()));
							messageBox.open();
						}
					}.start();
					return;
				}
				caller = Identifier.identify(getRuntime(), caller.getPhoneNumber(), cms);

				if (caller==null) {
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
													"notidentified",
													"label",
													getLanguage()));
							messageBox.open();
						}
					}.start();
					return;
				}
				
				final ICaller newCaller = openCallerWizard(caller);
				if (newCaller != null) {

					int style = SWT.APPLICATION_MODAL
					| SWT.YES | SWT.NO;
					MessageBox messageBox = new MessageBox(
							DisplayManager.getDefaultDisplay().getActiveShell(),
							style);
					messageBox
							.setMessage(getI18nManager()
									.getString(
											getNamespace(),
											"setall",
											"label",
											getLanguage()));
											
					final int status = messageBox.open();
					
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(
							DisplayManager.getDefaultDisplay().getActiveShell());
					try {
						IRunnableWithProgress r = new IRunnableWithProgress() {
							public void run(IProgressMonitor progressMonitor) {
								progressMonitor.beginTask(getI18nManager()
										.getString(getNamespace(),
												"assignprogress", "label",
												getLanguage()),
										IProgressMonitor.UNKNOWN);

								progressMonitor.worked(1);

								call.setCaller(newCaller);
								
								processModifierServices(call);

								if (status == SWT.NO && m_app.getController() instanceof IExtendedApplicationController) {
									// added 2009/12/31: only update single entry
									progressMonitor.setTaskName(getI18nManager()
											.getString(getNamespace(),
													"assignprogress", "label",
													getLanguage()));
									((IExtendedApplicationController)m_app.getController()).updateElement(call, false);
								} else {
									progressMonitor.setTaskName(getI18nManager()
											.getString(getNamespace(),
													"assignall", "label",
													getLanguage()));
									m_app.getController().updateElement(call);
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
				}
			}
		}
	}
	
	private List getAllActiveCallerManagers(IRuntime r) {
		List allManagers = r.getCallerManagerFactory().getAllCallerManagers();
		List activeManager = new ArrayList();
		Object o = null;
		ICallerManager cm = null;
		for (int i=0;i<allManagers.size();i++) {
			o = allManagers.get(i);
			if (o!=null && o instanceof ICallerManager) {
				cm = (ICallerManager)o;
				if (cm.isActive() && cm.isSupported(IIdentifyCallerRepository.class)) {
					activeManager.add(cm);
				}	
			}
		}
		activeManager.remove(r.getCallerManagerFactory().getDefaultCallerManager());
		
		Collections.sort(activeManager, new RepositoryManagerComparator());

		m_logger.info("List with all active caller managers: "+allManagers.toString());		
		return activeManager;
	}

	private ICaller openCallerWizard(ICaller caller) {
		Display display = DisplayManager.getDefaultDisplay();
		Shell shell = new Shell(display);

		WizardDialog.setDefaultImage(SWTImageManager.getInstance(
				this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
		JournalCallerWizard callerWiz = new JournalCallerWizard(caller);
		WizardDialog dlg = new WizardDialog(shell, callerWiz);
		dlg.open();
		if (dlg.getReturnCode() == WizardDialog.OK) {
			return callerWiz.getResult();
		}
		return null;
	}

	public boolean isEnabled() {
		if (this.m_app != null && this.m_app.getController() != null) {
			Object o = this.m_app.getController().getRepository();
			if (o instanceof ICallManager) {
				return ((ICallManager)o).isSupported(IWriteCallRepository.class);
			}
		}
		return false;
	}
	
	private void processModifierServices(ICall call) {
		if (call!=null) {
			List msvc = getRuntime().getServiceFactory().getModifierServices();
			IModifierService s = null;
			for (int k=0,l=msvc.size();k<l;k++) {
				s = (IModifierService) msvc.get(k);
				if (s!=null && s.isEnabled()) {
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Processing modifier service <"+s.getServiceID()+">");
					
					s.modifyObject(call);
				}
			}
		}
	}

}
