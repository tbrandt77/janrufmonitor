package de.janrufmonitor.ui.jface.application.dialog;

import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.*;

import de.janrufmonitor.framework.*;
import de.janrufmonitor.framework.event.*;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.filter.CallerFilter;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.repository.types.IWriteCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.controls.BalloonWindow;
import de.janrufmonitor.ui.jface.wizards.JournalCallerWizard;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.ui.swt.SWTImageManager;

public abstract class AbstractBalloonDialog extends BalloonWindow implements IDialog, DialogConst {

	protected class AssignPlugin implements IDialogPlugin {

		private class NameAssignDialog extends Thread {

			private ICaller m_caller;
			private Logger m_logger;

			public NameAssignDialog(ICaller caller) {
				this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
				this.m_caller = caller;
			}
			
			public void run() {
				ICaller newCaller = openCallerWizard(this.m_caller);
				if (newCaller!=null) {
					this.storeCaller(newCaller);
				}
			}
			
			protected ICaller openCallerWizard(ICaller caller) {
			    Display display = DisplayManager.getDefaultDisplay();
				Shell shell = new Shell(display);

			    WizardDialog.setDefaultImage(SWTImageManager.getInstance(getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
			    JournalCallerWizard callerWiz = new JournalCallerWizard(caller);
			    WizardDialog dlg = new WizardDialog(shell, callerWiz);
			    dlg.open();
			    if (dlg.getReturnCode() == WizardDialog.OK) {
			    	return callerWiz.getResult();
			    }
			    return null;
			}

			public void storeCaller(ICaller caller) {
				if (!caller.getPhoneNumber().isClired()) {
					List cpms = PIMRuntime.getInstance().getCallerManagerFactory().getTypedCallerManagers(IWriteCallerRepository.class);
					for (int i = 0; i < cpms.size(); i++) {
						if (((ICallerManager) cpms.get(i)).isActive() && ((ICallerManager) cpms.get(i)).isSupported(IWriteCallerRepository.class))
							((IWriteCallerRepository) cpms.get(i)).updateCaller(caller);
					}

					List cms = PIMRuntime.getInstance().getCallManagerFactory().getTypedCallManagers(IWriteCallRepository.class);
					if (cms!=null && cms.size()>0) {
						ICallManager cmgr = null;
						for (int i=0;i<cms.size();i++) {
							cmgr = (ICallManager) cms.get(i);
							if (cmgr!=null && cmgr.isActive() && cmgr.isSupported(IWriteCallRepository.class) && cmgr.isSupported(IReadCallRepository.class)) {
								this.m_logger.info("Updating call from repository manager <"+cmgr.getManagerID()+">.");
								ICallList oldCalls = ((IReadCallRepository)cmgr).getCalls(
									new CallerFilter(
										caller
									)
								);

								for (int j = 0; j < oldCalls.size(); j++) {
									ICall newCall = oldCalls.get(j);
									newCall.setCaller(caller);
								}
								((IWriteCallRepository)cmgr).updateCalls(oldCalls);		
							}
						}
					}
				}
			}

		}

		private IDialog m_dialog;
		
		public String getLabel() {
			return getI18nManager().getString(getNamespace(), "assign", "label", getLanguage());
		}

		public void setDialog(IDialog d) {
			this.m_dialog =d;
		}

		public void run() {
			Thread thread = new Thread () {
				public void run () {
					DisplayManager.getDefaultDisplay().asyncExec(
						new NameAssignDialog(m_dialog.getCall().getCaller())
					);
				}
			};
			thread.setName(getID());
			thread.start();
		}

		public boolean isEnabled() {
			return isAssignement();
		}

		public void setID(String id) {

		}
		
	}

	protected Properties m_configuration;
	protected ICall m_call;
	
	private II18nManager m_i18n;
	private String m_language;
	private Map m_colors;
	protected Logger m_logger;

	public AbstractBalloonDialog(Display display, int style) {
		super(display, style);
	}
	
	public AbstractBalloonDialog(Shell s, int style) {
		super(s, style);
	}
	
	public abstract void createDialog();
	
	protected List getPlugins(String config) {	
		if (config.trim().length()==0) return new ArrayList(0);
		
		StringTokenizer st = new StringTokenizer(config, ",");
		List l = new ArrayList(st.countTokens());
		while (st.hasMoreTokens()) {
			l.add(st.nextToken());
		}
		return l;
	}

	public abstract String getNamespace();
	
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

	protected Properties getConfiguration() {
		return (this.m_configuration!=null ? this.m_configuration : new Properties());
	}
	
	protected IRuntime getRuntime() {
		return PIMRuntime.getInstance();
	}
	
		
	public void open() {
		this.getShell().pack();
		this.setDialogPosition(this.getShell());
		
		// added 2007/10/24:
		if (this.getConfiguration().getProperty(CFG_FOCUS, "false").equalsIgnoreCase("true")) {
			this.getShell().forceFocus();
			this.getShell().forceActive();
		}
		
		if (this.getShowTime() > 0) {
			Timer aTimer = new Timer();
			aTimer.schedule(new TimerTask() {
				public void run() {
					new SWTExecuter(getID()) {
						protected void execute() {
							close();
						}
					}.start();
				}
			}, (long) ((this.getShowDuration()+1) * 1000));
		}

		
		super.open();
		
		while (this.getShell()!=null && !this.getShell().isDisposed()) {
			if (!this.getShell().getDisplay().readAndDispatch ()) this.getShell().getDisplay().sleep ();
		}
	}

	public void received(IEvent event) {
		if (event.getType() == IEventConst.EVENT_TYPE_CALLCLEARED ||
			event.getType() == IEventConst.EVENT_TYPE_CALLACCEPTED ||
			event.getType() == IEventConst.EVENT_TYPE_CALLREJECTED) {
				
				if (this.getShowTime() == -1) {
					new SWTExecuter(getID()) {
						protected void execute() {
							close();
						}
					}.start();
				} 
			}	
	}
	
	protected int getShowTime(){
		String sTime = this.getConfiguration().getProperty(CFG_SHOWTIME);
		return Integer.parseInt(((sTime == null || sTime.length()==0) ? "0" : sTime));
	}
	
	protected int getShowDuration(){
		String sTime = this.getConfiguration().getProperty(CFG_SHOWDURATION);
		return Integer.parseInt(((sTime == null || sTime.length()==0) ? "0" : sTime));
	}
	
	protected abstract String getID();
	
	protected abstract void setDialogPosition(Shell shell);

	protected int getFreePosX() {
		String value = this.getConfiguration().getProperty(CFG_FREE_POSX, "0");
		try {
			return Integer.parseInt(value);
		} catch (Exception e){
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return 0;
	}
	
	protected int getFreePosY() {
		String value = this.getConfiguration().getProperty(CFG_FREE_POSY, "0");
		try {
			return Integer.parseInt(value);
		} catch (Exception e){
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return 0;
	}
	
	protected boolean isFreePositioning() {
		return this.getConfiguration().getProperty(CFG_FREE_POS, "false").equalsIgnoreCase("true");
	}
	
	protected boolean isAssignement() {
		return this.getConfiguration().getProperty(CFG_ASSIGNEMENT, "false").equalsIgnoreCase("true");
	}
	
	protected boolean isCliredCaller() {
		return (this.m_call.getCaller()!=null && this.m_call.getCaller().getPhoneNumber().isClired());
	}

	public ICall getCall() {
		return this.m_call;
	}
	
	protected boolean isOutgoing(ICall c) {
		if (c==null) return false;
		IAttribute outgoing = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
		return (outgoing!=null && outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING));
	}
	
	protected boolean isUseMsnColors() {
		return this.getConfiguration().getProperty(CFG_USEMSNCOLOR, "false").equalsIgnoreCase("true");
	}
	
	protected boolean isUseSpamColors() {
		return this.getConfiguration().getProperty(CFG_SPAMCOLOR, "false").equalsIgnoreCase("true");
	}
	
	protected String getMsnColor(String msn){
		if (this.m_colors==null) {
			this.m_colors = new HashMap();
			String colors = getRuntime().getConfigManagerFactory().getConfigManager().getProperty("ui.jface.application.journal.Journal", "msnfontcolor");
			StringTokenizer st = new StringTokenizer(colors, "[");

			while (st.hasMoreTokens()) {
				String singleColor = st.nextToken();
				singleColor = singleColor.substring(0, singleColor.length()-1).trim();
				if (singleColor.length()>0) {
					StringTokenizer s = new StringTokenizer(singleColor, "%");
					while (s.hasMoreTokens()) {
						String key = s.nextToken();
						String color = s.nextToken();
						
						// only add if MSNs is existing
						if (this.getRuntime().getMsnManager().existMsn(
							this.getRuntime().getMsnManager().createMsn(key))) {
							
							this.m_colors.put(key, color);
						}
					}
				}
			}
		}
		
		if (this.m_colors.containsKey(msn)) {
			return (String)this.m_colors.get(msn);
		}
		return null;	
	}
	
	
	protected String getSpamColor(ICaller caller) {
		if (caller!=null && !caller.getPhoneNumber().isClired()) {
			if (caller.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_SPAM_COLOR)) {
				return caller.getAttribute(IJAMConst.ATTRIBUTE_NAME_SPAM_COLOR).getValue();
			}
		}
		return null;
	}
	
}
