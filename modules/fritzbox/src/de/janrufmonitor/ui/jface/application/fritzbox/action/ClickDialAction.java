package de.janrufmonitor.ui.jface.application.fritzbox.action;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.jface.application.ITreeItemCallerData;
import de.janrufmonitor.ui.jface.application.dialer.DialerDialog;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.string.StringUtils;

public class ClickDialAction extends AbstractAction implements FritzBoxConst {

	private static String NAMESPACE = "ui.jface.application.fritzbox.action.ClickDialAction";
	
	private IRuntime m_runtime;

	public ClickDialAction() {
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
		return "fritzbox_clickdial";
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
					if (!((IPhonenumber)o).getIntAreaCode().equalsIgnoreCase(this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA))) {
						dial = "00" + ((IPhonenumber)o).getIntAreaCode() + dial;
					}
					if (!dial.startsWith("0")) dial = "0"+dial;
					
					String prefixes = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, "dialprefixes");
					if (prefixes!=null && prefixes.length()>0) {
						DialerDialog id = new DialerDialog(new Shell(DisplayManager.getDefaultDisplay()), dial);
						id.open();
					} else {					

						// added 2010/03/06: check for dial prefix for outgoing calls
						if (this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Using dial prefix: "+this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX));
							dial = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
						}
						
						String text = getI18nManager()
						.getString(getNamespace(),
								"dial", "description",
								getLanguage());
						
						text = StringUtils.replaceString(text, "{%1}", dial);
										
						if (MessageDialog.openConfirm(
								new Shell(DisplayManager.getDefaultDisplay()),
								this.getI18nManager().getString(this.getNamespace(), "success", "label", this.getLanguage()),
								text)
							) {
	
							Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
							
							FirmwareManager fwm = FirmwareManager.getInstance();
							try {
								fwm.login();
								
								fwm.doCall(dial + "#", config.getProperty(CFG_CLICKDIAL, "50"));
								try {
									final String n = dial;
									ModalContext.run(new IRunnableWithProgress() {
										public void run(IProgressMonitor progressMonitor) {
											progressMonitor.done();
											String text = getI18nManager()
											.getString(getNamespace(),
													"success", "description",
													getLanguage());
											
											text = StringUtils.replaceString(text, "{%1}", n);
											
											MessageDialog.openInformation(
												new Shell(DisplayManager.getDefaultDisplay()),
												getI18nManager()
												.getString(getNamespace(),
														"success", "label",
														getLanguage()),
												text
											);
										}
									}, false, m_app.getApplication().getStatusLineManager().getProgressMonitor(), m_app.getApplication().getShell().getDisplay());
								} catch (InterruptedException e) {
									this.m_logger.log(Level.SEVERE, e.getMessage(), e);
								} catch (InvocationTargetException e) {
									this.m_logger.log(Level.SEVERE, e.getMessage(), e);
								}						
							} catch (IOException e) {
								this.m_logger.warning(e.toString());
								PropagationFactory.getInstance().fire(
										new Message(Message.ERROR,
										getNamespace(),
										"faileddial",	
										e));
							} catch (FritzBoxLoginException e) {
								this.m_logger.warning(e.toString());
							} catch (DoCallException e) {
								this.m_logger.warning(e.toString());
								PropagationFactory.getInstance().fire(
										new Message(Message.ERROR,
										getNamespace(),
										"faileddial",	
										e));
							}
							
							this.m_app.updateViews(false);
						}
					}
				}
			}
		}
	}

}
