package de.janrufmonitor.service.fritzbox;

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
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.framework.i18n.II18nManager;
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
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.service.IModifierService;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.ui.jface.application.journal.Journal;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class SynchronizerService extends AbstractReceiverConfigurableService implements FritzBoxConst, IEventSender {

	private String ID = "SynchronizerService";
    private String NAMESPACE = "ui.jface.application.fritzbox.action.Refresh";
	private IRuntime m_runtime;
	private II18nManager m_i18n;
	private String m_language;
	private SyncTimerThread syncTimerThread;
	
	private class SyncTimerThread extends Thread {
		private boolean isRunning;
		private long t = 0;
		public void run() {
			this.isRunning = true;
			do {
				try {
					Thread.sleep(t);
				} catch (InterruptedException e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
				}
				if (isRunning)
					synchronize(true);
			} while (isRunning);
			m_logger.info("Finalizing timebased sync thread...");
		}
		
		public void setTimeout(long t) {
			this.t = t;
		}
		
		public void cancel() {
			this.isRunning = false;
		}
	}

    public SynchronizerService() {
    	super();
    	this.getRuntime().getConfigurableNotifier().register(this);	
    }

	public String getNamespace() {
		return NAMESPACE;
	}

	public String getID() {
		return ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
	
	public void startup() {
		super.startup();
		if (isEnabled()) {	
			IEventBroker eventBroker = this.getRuntime().getEventBroker();
			eventBroker.register(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
			
			FirmwareManager.getInstance().startup();
			new SWTExecuter() {
				protected void execute() {
					synchronize(false);
				}}
			.start();
			
			boolean isRefreshAfterCallend = this.m_configuration.getProperty(CFG_REFRESH_AFTER_CALLEND, "false").equalsIgnoreCase("true");
			if (isRefreshAfterCallend) {
				eventBroker.register(this, eventBroker
						.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED));
				
				eventBroker.register(this, eventBroker
						.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));
			}
			timebasedSyncing();
		}
	}
	
	public void receivedOtherEventCall(IEvent event) {
		ICall aCall = (ICall)event.getData();
		if (aCall!=null) {
			if (getRuntime().getRuleEngine().validate(this.getID(), aCall.getMSN(), aCall.getCIP(), aCall.getCaller().getPhoneNumber())) {
				this.receivedValidRule(aCall);
			} else {
				this.m_logger.info("No rule assigned to execute this service for call: "+aCall);
			}
		} 
		
		if (event.getType() == IEventConst.EVENT_TYPE_RETURNED_HIBERNATE) {
			this.cancelingTimebasedSyncing();
			FirmwareManager.getInstance().shutdown();
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			FirmwareManager.getInstance().startup();
			new SWTExecuter() {
				protected void execute() {
					synchronize(false);
				}}
			.start();
			timebasedSyncing();
		}
	}
	
	public void receivedValidRule(ICall aCall) {
		IAttribute status = aCall.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
		if (status!=null && (status.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING) || status.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_ACCEPTED))) {
			IAttribute dur = aCall.getAttribute("fritzbox.duration");
			if (dur!=null && dur.getValue().length()>0 && !dur.getValue().equalsIgnoreCase("0")) {
				new SWTExecuter() {
					protected void execute() {
						synchronize(true);
					}}
				.start();
			}
		} 
	}

	
	public void shutdown() {
		cancelingTimebasedSyncing();
		
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
		
		boolean isRefreshAfterCallend = this.m_configuration.getProperty(CFG_REFRESH_AFTER_CALLEND, "false").equalsIgnoreCase("true");
		if (isRefreshAfterCallend) {
			eventBroker.unregister(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED));
			
			eventBroker.unregister(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));
		}
		FirmwareManager.getInstance().shutdown();
		
		super.shutdown();
	}
	
	protected II18nManager getI18nManager() {
		if (this.m_i18n==null) {
			this.m_i18n = this.getRuntime().getI18nManagerFactory().getI18nManager();
		}
		return this.m_i18n;
	}

	protected String getLanguage() {
		if (this.m_language==null) {
			this.m_language = 
				this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
					IJAMConst.GLOBAL_NAMESPACE,
					IJAMConst.GLOBAL_LANGUAGE
				);
		}
		return this.m_language;
	}

	private synchronized void synchronize(boolean isSuppressed) {
		try {
			Thread.sleep((isSuppressed ? 500 : 1000));
		} catch (InterruptedException e1) {
			this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
		}
		
		if (this.m_configuration.getProperty(CFG_SYNCDIALOG, "false").equalsIgnoreCase("true") || isSuppressed) {
			Properties config = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
			
			FirmwareManager fwm = FirmwareManager.getInstance();
			
			try {
				fwm.login();

				List result = fwm.getCallList();
			
				try {
					Thread.sleep((isSuppressed ? 100 : 250));
				} catch (InterruptedException e1) {
					m_logger.log(Level.SEVERE, e1.getMessage(), e1);
				}
				
				if (result.size()>0) {
					ICallList m_callList = PIMRuntime.getInstance().getCallFactory().createCallList(result.size());
					FritzBoxCallCsv call = null;
					Properties conf = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
					ICall c = null;
					FritzBoxUUIDManager.getInstance().init();
					long synctime = Long.parseLong(SynchronizerService.this.m_configuration.getProperty(CFG_SYNCTIME, "-1"));
					for (int i=0,j=result.size();i<j;i++) {
						call = new FritzBoxCallCsv((String) result.get(i), conf);
						Date calltime = call.getPrecalculatedDate();
						if (calltime!=null && calltime.getTime()<synctime && synctime>0) {
							if (m_logger.isLoggable(Level.INFO))
								m_logger.info("Call import skipped by timestamp (last sync time: "+new Date(synctime).toString()+", call time: "+calltime.toString()+") from FritzBox.");
							continue;
						}
						c = call.toCall();
						if (c!=null) {
							if (getRuntime().getMsnManager().isMsnMonitored(
									c.getMSN())
								) {										
								if (!m_callList.contains(c))
									m_callList.add(c);
								else {
									m_logger.warning("Duplicated call imported from FritzBox: "+c.toString());
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
					this.processModifierServices(m_callList);
					if (m_callList!=null && m_callList.size()>0) {
						String repository = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Journal.NAMESPACE, "repository");
						ICallManager cm = getRuntime().getCallManagerFactory().getCallManager(repository);
						if (cm!=null && cm.isActive() && cm.isSupported(IWriteCallRepository.class)) {
							ICall ca = null;

							// added 2008/04/22: check sync point cleanup
							synctime = Long.parseLong(SynchronizerService.this.m_configuration.getProperty(CFG_SYNCTIME, "-1"));
							boolean syncclean = SynchronizerService.this.m_configuration.getProperty(CFG_SYNCCLEAN, "false").equalsIgnoreCase("true");
							if (syncclean && synctime>0 && cm.isSupported(IReadCallRepository.class) && cm.isSupported(IWriteCallRepository.class)) {
								
								IFilter syncFilter = new DateFilter(new Date(System.currentTimeMillis()), new Date(synctime));
								ICallList cl = ((IReadCallRepository)cm).getCalls(syncFilter);
								if (cl.size()>0) {
									// 2009/03/18: added backup of cleaned calls
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
							for (int i=0,j=m_callList.size();i<j;i++) {
								ca = m_callList.get(i);
								try {
									((IWriteCallRepository)cm).setCall(ca);
								} catch (Exception e) {
									m_logger.info("Call already in DB: "+ca.toString());
								}
							}
							
							// added 2009/01/08: force refresh of journal, if opened
							IEventBroker evtBroker = getRuntime().getEventBroker();
							evtBroker.register(this);
							evtBroker.send(this, evtBroker.createEvent(IEventConst.EVENT_TYPE_CALL_MANAGER_UPDATED));
							evtBroker.unregister(this);
						}
						
						boolean syncDelete = (config.getProperty(FritzBoxConst.CFG_SYNCDELETE, "false").equalsIgnoreCase("true") ? true : false);
						
						if (syncDelete) {
							fwm.deleteCallList();	
						}
						
						// added 2009/01/07: send mail notification after sync with fritzbox
						boolean syncNotify = (SynchronizerService.this.m_configuration.getProperty(FritzBoxConst.CFG_SYNC_NOTIFICATION, "false").equalsIgnoreCase("true") ? true : false);
						if (syncNotify) {
							ICall ca = null;
							for (int i=0,j=m_callList.size();i<j;i++) {
								ca = m_callList.get(i);
								sendMailNotification(ca);
							}
						}
					}
					
					String text = getI18nManager().getString(getNamespace(),
							"finished", "label",
							getLanguage());
					
//					if (m_callList.size()==0)
//						text = getI18nManager().getString(getNamespace(),
//								"finished0", "label",
//								getLanguage());
					
					if (m_callList.size()==1)
						text = getI18nManager().getString(getNamespace(),
								"finished1", "label",
								getLanguage());
										
					if (m_callList.size()>0)
						PropagationFactory.getInstance().fire(
							new Message(Message.INFO, 
									getI18nManager().getString("monitor.FritzBoxMonitor",
									"title", "label",
									getLanguage()), 
									new Exception(StringUtils.replaceString(text, "{%1}", Integer.toString(m_callList.size())))),
							"Tray");	
					
					SynchronizerService.this.m_configuration.setProperty(CFG_SYNCTIME, Long.toString(System.currentTimeMillis()));
					getRuntime().getConfigManagerFactory().getConfigManager().setProperties(NAMESPACE, SynchronizerService.this.m_configuration);
					getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
					
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
			} catch (GetCallListException e) {
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
			} catch (CloneNotSupportedException e) {
				m_logger.warning(e.toString());
			}
			
		} else {
			//ProgressMonitorDialog pmd = new ProgressMonitorDialog(new Shell(DisplayManager.getDefaultDisplay().getActiveShell()));	
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(new Shell(DisplayManager.getDefaultDisplay()));
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
						
						FirmwareManager fwm = FirmwareManager.getInstance();
						try {
							fwm.login();
							
							progressMonitor.setTaskName(getI18nManager()
									.getString(getNamespace(),
											"getprogress", "label",
											getLanguage()));
							List result = fwm.getCallList();	
	
							progressMonitor.setTaskName(getI18nManager()
									.getString(getNamespace(),
											"identifyprogress", "label",
											getLanguage()));
						
							try {
								Thread.sleep(250);
							} catch (InterruptedException e1) {
								m_logger.log(Level.SEVERE, e1.getMessage(), e1);
							}
							
							if (result.size()>0) {
								ICallList m_callList = PIMRuntime.getInstance().getCallFactory().createCallList(result.size());
								FritzBoxCallCsv call = null;
								Properties conf = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
								ICall c = null;
								long synctime = Long.parseLong(SynchronizerService.this.m_configuration.getProperty(CFG_SYNCTIME, "-1"));
								for (int i=0,j=result.size();i<j;i++) {
									call = new FritzBoxCallCsv((String) result.get(i), conf);
									Date calltime = call.getPrecalculatedDate();
									if (calltime!=null && calltime.getTime()<synctime && synctime>0) {
										if (m_logger.isLoggable(Level.INFO))
											m_logger.info("Call import skipped by timestamp (last sync time: "+new Date(synctime).toString()+", call time: "+calltime.toString()+") from FritzBox.");
										continue;
									}
									c = call.toCall();
									if (c!=null) {
										if (getRuntime().getMsnManager().isMsnMonitored(
												c.getMSN())
											) {										
											if (!m_callList.contains(c))
												m_callList.add(c);
											else {
												m_logger.warning("Duplicated call imported from FritzBox: "+c.toString());
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
									String repository = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Journal.NAMESPACE, "repository");
									ICallManager cm = getRuntime().getCallManagerFactory().getCallManager(repository);
									if (cm!=null && cm.isActive() && cm.isSupported(IWriteCallRepository.class)) {
										ICall ca = null;
										// added 2008/04/22: check sync point cleanup
										synctime = Long.parseLong(SynchronizerService.this.m_configuration.getProperty(CFG_SYNCTIME, "-1"));
										boolean syncclean = SynchronizerService.this.m_configuration.getProperty(CFG_SYNCCLEAN, "false").equalsIgnoreCase("true");
										if (syncclean && synctime>0 && cm.isSupported(IReadCallRepository.class)) {
											progressMonitor.setTaskName(getI18nManager()
													.getString(getNamespace(),
															"syncclean", "label",
															getLanguage()));
											
											IFilter syncFilter = new DateFilter(new Date(System.currentTimeMillis()), new Date(synctime));
											ICallList cl = ((IReadCallRepository)cm).getCalls(syncFilter);
											if (cl.size()>0) {
												// 2009/03/18: added backup of cleaned calls
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
										
										fwm.deleteCallList();	
									}
									
									// added 2009/01/07: send mail notification after sync with fritzbox
									boolean syncNotify = (SynchronizerService.this.m_configuration.getProperty(FritzBoxConst.CFG_SYNC_NOTIFICATION, "false").equalsIgnoreCase("true") ? true : false);
									if (syncNotify) {
										ICall ca = null;
										progressMonitor.setTaskName(getI18nManager()
												.getString(getNamespace(),
														"sendnotificationprogress", "label",
														getLanguage()));
										for (int i=0,j=m_callList.size();i<j;i++) {
											ca = m_callList.get(i);											
											sendMailNotification(ca);
										}
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
								
								if (m_callList.size()>0)
									PropagationFactory.getInstance().fire(
										new Message(Message.INFO, 
												getI18nManager().getString("monitor.FritzBoxMonitor",
												"title", "label",
												getLanguage()), 
												new Exception(StringUtils.replaceString(text, "{%1}", Integer.toString(m_callList.size())))),
										"Tray");	
									
								SynchronizerService.this.m_configuration.setProperty(CFG_SYNCTIME, Long.toString(System.currentTimeMillis()));
								getRuntime().getConfigManagerFactory().getConfigManager().setProperties(NAMESPACE, SynchronizerService.this.m_configuration);
								getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();

								try {
									Thread.sleep(1000);
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
						} catch (GetCallListException e) {
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
		}
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
	
	private void sendMailNotification(ICall c) {
		if (getRuntime().getServiceFactory().isServiceAvailable("MailNotification")&& getRuntime().getServiceFactory().isServiceEnabled("MailNotification")) {
			IService notification = getRuntime().getServiceFactory().getService("MailNotification");
			if (notification!=null) {
				if (notification instanceof AbstractReceiverConfigurableService) {
					// send the email as defined in the service configuration
					((AbstractReceiverConfigurableService)notification).receivedValidRule(c);
				}
			}
		} else {
			this.m_logger.warning("MailNotification service is not installed or actived:");
		}
	}
	

	public List getDependencyServices() {
		List dependency = super.getDependencyServices();
		dependency.add("TrayIcon");
		return dependency;
	}
	
	private void timebasedSyncing() {
		long time = Long.parseLong(this.m_configuration.getProperty(CFG_SYNC_TIMER, "0"));
		if (time >0) {
			time = time * 60 * 1000; // make minutes out of it...
			syncTimerThread = new SyncTimerThread();
			syncTimerThread.setTimeout(time);
			syncTimerThread.setDaemon(true);
			syncTimerThread.setName("JAM-FritzBoxSyncTimer#"+System.currentTimeMillis()+"-Thread-(deamon)");
			syncTimerThread.start();
		}
	}
	
	private void cancelingTimebasedSyncing() {
		if (syncTimerThread!=null && syncTimerThread.isAlive() ) {
			syncTimerThread.cancel();
			syncTimerThread = null;
		}
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
						s.modifyObject(call.getCaller());
					}			
				}
			}
		}
	}

	public String getSenderID() {
		return this.ID;
	}


}
