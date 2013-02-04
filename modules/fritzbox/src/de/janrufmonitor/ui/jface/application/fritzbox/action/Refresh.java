package de.janrufmonitor.ui.jface.application.fritzbox.action;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.fritzbox.FritzBoxCallCsv;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.fritzbox.FritzBoxUUIDManager;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallListException;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.filter.DateFilter;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.imexport.ICallExporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.repository.imexport.ImExportFactory;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IModifierService;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

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
					progressMonitor.beginTask(getI18nManager()
							.getString(getNamespace(),
									"refreshprogress", "label",
									getLanguage()), IProgressMonitor.UNKNOWN);
					
					progressMonitor.worked(1);

					Properties config = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);

					progressMonitor.setTaskName(getI18nManager()
							.getString(getNamespace(),
									"loginprogress", "label",
									getLanguage()));

					FirmwareManager m_fwm = FirmwareManager.getInstance();
					try {
						m_fwm.login();
						
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"getprogress", "label",
										getLanguage()));
						List result = m_fwm.getCallList();
						
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"identifyprogress", "label",
										getLanguage()));
						if (result.size()>0) {
							ICallList m_callList = PIMRuntime.getInstance().getCallFactory().createCallList(result.size());
							FritzBoxCallCsv call = null;
							Properties conf = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
							ICall c = null;
							FritzBoxUUIDManager.getInstance().init();
							for (int i=0,j=result.size();i<j;i++) {
								call = new FritzBoxCallCsv((String) result.get(i), conf);
								c = call.toCall();
								if (c!=null) {
									// added: 2007/07/10: MSN check inserted
									if (getRuntime().getMsnManager().isMsnMonitored(
											c.getMSN())
										) {										
										if (!m_callList.contains(c))
											m_callList.add(c);
										else {
											m_logger.warning("Call already imported from FritzBox: "+c.toString());
											c.setUUID(c.getUUID()+"-1");
											ICip cip = c.getCIP();
											cip.setCIP("4"); // just a dirty hack 
											c.setCIP(cip);
											if (!m_callList.contains(c))
												m_callList.add(c);
											else {
												c.setUUID(c.getUUID()+"-1");
												if (!m_callList.contains(c))
													m_callList.add(c);
											}
										}
									}						
								}
							}
							progressMonitor.setTaskName(getI18nManager()
									.getString(getNamespace(),
											"geocodeprogress", "label",
											getLanguage()));
							
							processModifierServices(m_callList);
							
							progressMonitor.setTaskName(getI18nManager()
									.getString(getNamespace(),
											"synchprogress", "label",
											getLanguage()));
							
							if (m_callList!=null && m_callList.size()>0) {
								String repository = m_app.getApplication().getConfiguration().getProperty("repository");
								ICallManager cm = getRuntime().getCallManagerFactory().getCallManager(repository);
								if (cm!=null && cm.isActive() && cm.isSupported(IWriteCallRepository.class)) {
									ICall ca = null;
									// added 2008/04/22: check sync point cleanup
									Properties cfg = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(NAMESPACE);
									long synctime = Long.parseLong(cfg.getProperty(CFG_SYNCTIME, "-1"));
									// added: 2013/02/04: check sync all
									boolean syncall = cfg.getProperty(CFG_SYNCALL, "false").equalsIgnoreCase("true");
									if (syncall) {
										synctime = -1;
										m_logger.info("Syncing all calls from Fritz!Box.");
									}
									boolean syncclean = cfg.getProperty(CFG_SYNCCLEAN, "false").equalsIgnoreCase("true");
									if (syncclean && synctime>0 && cm.isSupported(IReadCallRepository.class) && cm.isSupported(IWriteCallRepository.class)) {
										progressMonitor.setTaskName(getI18nManager()
												.getString(getNamespace(),
														"syncclean", "label",
														getLanguage()));
										
										IFilter syncFilter = new DateFilter(new Date(System.currentTimeMillis()), new Date(synctime));
										ICallList cl = ((IReadCallRepository)cm).getCalls(syncFilter);
										if (cl.size()>0) {
//											 2009/03/18: added backup of cleaned calls
											IImExporter exp = ImExportFactory.getInstance().getExporter("DatFileCallExporter");
											if (exp!=null & exp instanceof ICallExporter) {
												m_logger.info("Creating backup of cleaned call list...");
												File backupdir = new File(PathResolver.getInstance(getRuntime()).getDataDirectory(), "fritzbox-sync-clean-backup");
												if (!backupdir.exists()) {
													backupdir.mkdirs();
												}
												File backupfile = new File(backupdir, Long.toString(synctime)+".dat");
												((ICallExporter) exp).setFilename(backupfile.getAbsolutePath());
												((ICallExporter) exp).setCallList(cl);
												if (((ICallExporter) exp).doExport()) {
													m_logger.info("Backup of cleaned call list successfully finished.");
												} else {
													m_logger.warning("Backup of cleaned call list failed: "+backupdir.getAbsolutePath());
												}
											}
											((IWriteCallRepository)cm).removeCalls(createRedundancyList(m_callList, synctime));
											try {
												Thread.sleep(500);
											} catch (InterruptedException e) {
											}
										}
									}
									
									cfg.setProperty(CFG_SYNCTIME, Long.toString(System.currentTimeMillis()));
									getRuntime().getConfigManagerFactory().getConfigManager().setProperties(NAMESPACE, cfg);
									getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
									
									for (int i=0,j=m_callList.size();i<j;i++) {
										ca = m_callList.get(i);
										try {
											((IWriteCallRepository)cm).setCall(ca);
										} catch (Exception e) {
											m_logger.info("Call already in DB: "+ca.toString());
										}
									}
								}
								
								boolean syncDelete = (config.getProperty(FritzBoxConst.CFG_SYNCDELETE, "false").equalsIgnoreCase("true") ? true : false);
								
								if (syncDelete) {
									progressMonitor.setTaskName(getI18nManager()
											.getString(getNamespace(),
													"deleteprogress", "label",
													getLanguage()));
									
									m_fwm.deleteCallList();
								}
							}
							
							String text = getI18nManager().getString(getNamespace(),
									"finished", "label",
									getLanguage());
							
							if (m_callList.size()==0)
								text = getI18nManager().getString(getNamespace(),
										"finished0", "label",
										getLanguage());
							
							if (m_callList.size()==1)
								text = getI18nManager().getString(getNamespace(),
										"finished1", "label",
										getLanguage());
							
							progressMonitor.setTaskName(StringUtils.replaceString(text, "{%1}", Integer.toString(m_callList.size())));
							
							PropagationFactory.getInstance().fire(
									new Message(Message.INFO, 
											getI18nManager().getString("monitor.FritzBoxMonitor",
											"title", "label",
											getLanguage()), 
											new Exception(StringUtils.replaceString(text, "{%1}", Integer.toString(m_callList.size())))),
									"Tray");					
							try {
								Thread.sleep(1500);
							} catch (InterruptedException e1) {
								m_logger.log(Level.SEVERE, e1.getMessage(), e1);
							}							
						}
					} catch (IOException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
								getNamespace(),
								"failedrefresh",	
								e));
					} catch (FritzBoxLoginException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
								getNamespace(),
								"failedrefresh",	
								e));
					} catch (DeleteCallListException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
								getNamespace(),
								"failedrefresh",	
								e));
					} catch (GetCallListException e) {
						m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
								getNamespace(),
								"failedrefresh",	
								e));
					} catch (CloneNotSupportedException e) {
						m_logger.warning(e.toString());
					}
					
					progressMonitor.done();
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
	
	private ICallList createRedundancyList(ICallList l, long lastsync) throws CloneNotSupportedException {
		if (l==null) return getRuntime().getCallFactory().createCallList();
		ICallList cl = getRuntime().getCallFactory().createCallList(2*l.size());
		
		ICall c = null;
		ICall cloneCall = null;
		for (int i=0,j=l.size();i<j;i++) {
			c = l.get(i);
			if (c.getDate().getTime()>=lastsync) {
				cloneCall = (ICall) c.clone();
				cloneCall.setDate(new Date(c.getDate().getTime()-60000));
				// create UUID
				StringBuffer uuid = new StringBuffer();
				uuid.append(cloneCall.getDate().getTime());
				uuid.append("-");
				uuid.append(cloneCall.getCaller().getPhoneNumber().getTelephoneNumber());
				uuid.append("-");
				uuid.append(cloneCall.getMSN().getMSN());
				// limit uuid to 32 chars
				if (uuid.length()>31) {
					// reduce byte length to append -1 for redundant calls max -1-1 --> 3 calls
					uuid = new StringBuffer(uuid.substring(0,31));
				}
				cloneCall.setUUID(uuid.toString());
				cl.add(c);
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Cloned call with Date -60000 msec");
				cl.add(cloneCall);
			}
		}
		return cl;
	}
	
	private void processModifierServices(ICallList cl) {
		if (cl!=null && cl.size()>0) {
			List msvc = getRuntime().getServiceFactory().getModifierServices();
			IModifierService s = null;
			for (int k=0,l=msvc.size();k<l;k++) {
				s = (IModifierService) msvc.get(k);
				if (s!=null && s.isEnabled()) {
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Processing modifier service <"+s.getServiceID()+">");
					ICall call = null;
					for (int i=0,j=cl.size();i<j;i++) {
						call = cl.get(i);
						s.modifyObject(call);
					}			
				}
			}
		}
	}

	public boolean isEnabled() {
		if (this.m_app!=null && this.m_app.getController()!=null) {
			Object o = this.m_app.getController().getRepository();
			return (o instanceof IWriteCallRepository);
		}
		return false;
	}
}
