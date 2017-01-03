package de.janrufmonitor.ui.jface.application.journal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractMenuBuilder;
import de.janrufmonitor.ui.jface.application.AbstractTableApplication;
import de.janrufmonitor.ui.jface.application.ActionRegistry;
import de.janrufmonitor.ui.jface.application.IApplication;
import de.janrufmonitor.ui.jface.application.IFilterManager;
import de.janrufmonitor.ui.jface.application.TableContentProvider;
import de.janrufmonitor.ui.jface.application.action.IAction;
import de.janrufmonitor.ui.jface.application.dnd.IDropTargetHandler;
import de.janrufmonitor.ui.jface.application.journal.action.ImportAction;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IRenderer;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;

public final class Journal extends AbstractTableApplication implements IEventSender, IEventReceiver, JournalConfigConst {


	private class JournalMenuBuilder extends AbstractMenuBuilder {

		private IRuntime m_runtime;
		
		public JournalMenuBuilder(IApplication app, List l, List popupActions) {
			super(app, l, popupActions);
		}

		public MenuManager createMenu() {
			MenuManager master = new MenuManager();
			
			if (OSUtils.isMacOSX()) {
				 master = new MenuManager(this.getI18nManager().getString(
							this.m_app.getNamespace(),
							"master",
							"label",
							this.getLanguage()
						));
			}
			
			// create file menu
			MenuManager file = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"file",
					"label",
					this.getLanguage()
				)
			);
			master.add(file);
			this.addAction(file, "journal_new_journal");
			this.addAction(file, "journal_open_journal");
			this.addAction(file, "journal_lastopen_journal");
			this.addSeparator(file);
			this.addAction(file, "journal_export");
			this.addAction(file, "journal_import");
			this.addSeparator(file);
			this.addAction(file, "close");
			
			// create view menu
			MenuManager view = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"view",
					"label",
					this.getLanguage()
				)
			);
			master.add(view);
			this.addAction(view, "journal_filter");

			this.addSeparator(view);
			this.addAction(view, "refresh");
			this.addAction(view, "select_all");
			this.addAction(view, "delete_all");
			this.addSeparator(view);
			this.addAction(view, "search");
			this.addSeparator(view);
			
			// create selected caller menu
			MenuManager selected = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"selected",
					"label",
					this.getLanguage()
				)
			);
			view.add(selected);
			
			this.addAction(selected, "journal_assign");
			this.addAction(selected, "journal_websearch");
			this.addAction(selected, "clipboard");
			this.addAction(selected, "journal_delete");

			
			// create advanced menu
			MenuManager advanced = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"advanced",
					"label",
					this.getLanguage()
				)
			);
			master.add(advanced);
			
			// create statistic menu
			MenuManager statistic = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"statistic",
					"label",
					this.getLanguage()
				)
			);
			advanced.add(statistic);
			this.addAction(statistic, "journal_callerstats");
			this.addAction(statistic, "journal_callingstats");
			this.addAction(statistic, "journal_msnstats");
			this.addAction(statistic, "journal_durationstats");
			this.addAction(statistic, "journal_daytimestats");
			this.addAction(statistic, "journal_weektimestats");
			this.addAction(statistic, "journal_monthtimestats");
			
			List add_stats = this.getAdditionalStatistics();
			if (add_stats.size()>0) {
				this.addSeparator(statistic);
					for (int i=0,j=add_stats.size();i<j;i++)
						this.addAction(statistic, (String) (add_stats.get(i)));
			}
			
			this.addSeparator(advanced);
			
			// create settings menu
			MenuManager settings = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"settings",
					"label",
					this.getLanguage()
				)
			);
			advanced.add(settings);
			this.addAction(settings, "showgrid");
			this.addAction(settings, "journal_highlightselect");
			this.addAction(settings, "columnselect");
			this.addAction(settings, "journal_msncolorselect");

			if (this.m_addActions.size()>0) {
				// create service menu
				MenuManager service = new MenuManager(
					this.getI18nManager().getString(
						this.m_app.getNamespace(),
						"service",
						"label",
						this.getLanguage()
					)
				);
				master.add(service);

				for (int i=0;i<this.m_addActions.size();i++) {
					this.addAction(service, (String)this.m_addActions.get(i));
				}
			}
			
			// create ? menu
			MenuManager q = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"q",
					"label",
					this.getLanguage()
				)
			);
			master.add(q);
			this.addAction(q, "help");
			
			return master;
		}
		
		private List getAdditionalStatistics() {
			String actions = getConfiguration().getProperty(CFG_ADDITIONAL_STATS, "");
			if (actions==null || actions.trim().length()==0)
				return new ArrayList(1);
			
			String[] actionx = actions.split(","); 
			List result = new ArrayList(actionx.length);
			for (int i=0;i<actionx.length;i++) {
				result.add(actionx[i]);
			}
			return result;
		}

		public IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}

		public Menu createPopupMenu(Control c) {
			MenuManager master = new MenuManager();
			
			if (OSUtils.isMacOSX()) {
				master.add(this.createMenu());
				this.addSeparator(master);
			}
			
			this.addAction(master, "journal_assign");
			this.addAction(master, "journal_reidentify");
			this.addAction(master, "history");
			this.addAction(master, "journal_websearch");
			this.addAction(master, "clipboard");
			//this.addAction(master, "journal_notesclipboard");
			this.addAction(master, "journal_delete");
			this.addSeparator(master);
			this.addAction(master, "showgrid");
			this.addAction(master, "zoomin");
			this.addAction(master, "zoomout");
			
			if (this.m_addPopupMenuActions.size()>0) {
				this.addSeparator(master);

				for (int i=0;i<this.m_addPopupMenuActions.size();i++) {
					this.addAction(master, (String)this.m_addPopupMenuActions.get(i));
				}
			}		
			
			return master.createContextMenu(c);
		}

		public List getToolbarActions() {
			List actions = new ArrayList();
			this.addAction(actions, "journal_filter");
			this.addAction(actions, "journal_export");
			this.addAction(actions, "refresh");
			this.addAction(actions, "delete_all");
			this.addAction(actions, "search");
			//this.addAction(actions, "journal_journalselect");
			return actions;
		}

		public List getStartupActions() {
			String actions = getConfiguration().getProperty(CFG_STARTUP_ACTIONS, "");
			if (actions==null || actions.trim().length()==0)
				return super.getStartupActions();
			
			String[] actionx = actions.split(","); 
			List result = new ArrayList(actionx.length);
			for (int i=0;i<actionx.length;i++) {
				this.addAction(result, actionx[i]);
			}
			return result;
		}
	}
	
	private class JournalDropTargetHandler implements IDropTargetHandler {
		public void execute(String[] sources) {
			if (sources==null || sources.length == 0) return;
			IAction a = ActionRegistry.getInstance().getAction("journal_import", getApplication());
			if (a!=null && a instanceof ImportAction && ((ImportAction)a).isEnabled()) {
				((ImportAction)a).setSupressDialogs(true);
				((ImportAction)a).run(sources); 
				((ImportAction)a).setSupressDialogs(false);
				updateViews(true);
			}
		}
	}
	
	public static String NAMESPACE = "ui.jface.application.journal.Journal";

	private IRuntime m_runtime;
	private AbstractMenuBuilder m_mb;
	private TableContentProvider m_jp;
	private IDropTargetHandler m_dth;

	public Journal() {
		this(true);
	}
	
	public Journal(boolean isBlocking) {
		super(isBlocking);
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "Journal";
	}

	public String getNamespace() {
		return Journal.NAMESPACE;
	}

	public void received(IEvent event) {
		if (event.getType() == IEventConst.EVENT_TYPE_UPDATE_CALL ||
			event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_CALL ||
			event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL ||
			event.getType() == IEventConst.EVENT_TYPE_CALL_MANAGER_UPDATED) {
        	
			Thread t = new Thread() {
				public void run() {
					DisplayManager.getDefaultDisplay()
						.asyncExec(new Runnable() {
							public void run() {
								updateViews(true);
							}
						});
				}
			};
			t.start();

			// send application ready event
			IEventBroker eventBroker = this.getRuntime().getEventBroker();
			eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_APPLICATION_READY));
        }
	}
	

	public void updateViews(boolean reload) {		
		// initialize menu builder
		this.m_mb = null;
		super.updateViews(reload);
	}

	public String getReceiverID() {
		return this.getID();
	}

	public int getPriority() {
		return 0;
	}

	public String getSenderID() {
		return this.getID();
	}
	
	public boolean close() {
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_UPDATE_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALL_MANAGER_UPDATED));
		eventBroker.unregister(this);
		return super.close();
	}
	
	public int open() {
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_UPDATE_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALL_MANAGER_UPDATED));
		eventBroker.register(this);
		
		eventBroker.send(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_APPLICATION_READY));
		return super.open();
	}

	protected AbstractMenuBuilder getMenuBuilder() {
		if (this.m_mb==null)
			this.m_mb = new JournalMenuBuilder(this, this.getAdditionalMenuActions(), this.getAdditionalPopupActions());
		return m_mb;
	}

	protected IFilterManager getFilterManager() {
		return new JournalFilterManager();
	}

	protected IStructuredContentProvider getContentProvider() {
		if (this.m_jp==null)
			this.m_jp = new TableContentProvider(this.getConfiguration());
		return this.m_jp;
	}
	
	protected void createApplicationController() {
		if (this.m_controller==null) // added 2006/06/28: refresh performance problem
			this.m_controller = new JournalController();
	}

	protected IAction getFilterAction() {
		return ActionRegistry.getInstance().getAction("journal_filter",	getApplication());
	}

	protected IAction getAssignAction() {
		return ActionRegistry.getInstance().getAction("journal_assign", getApplication());
	}

	protected IAction getDeleteAction() {
		return ActionRegistry.getInstance().getAction("journal_delete",	getApplication());
	}

	protected IAction getColoringAction() {
		return ActionRegistry.getInstance().getAction("journal_msncoloring", getApplication());
	}

	protected IAction getOrderAction() {
		return ActionRegistry.getInstance().getAction("journal_order", getApplication());
	}

	protected IAction getHightlightAction() {
		return ActionRegistry.getInstance().getAction("journal_highlight", getApplication());
	}

	protected void initializeProviders() {
		if (this.m_jp!=null) this.m_jp.dispose();
		this.m_jp = null;
	}

	protected IDropTargetHandler getDropTargetHandler() {
		if (this.m_dth==null)
			this.m_dth = new JournalDropTargetHandler();
		return m_dth;
	}

	protected IAction getQuickSearchAction() {
		return ActionRegistry.getInstance().getAction("journal_quicksearchterm", getApplication());
	}
	
	protected String getTitleExtension() {
		String id = this.m_configuration.getProperty(CFG_REPOSITORY, "");
		if (id!=null && id.length()>0) {
			ICallManager cm = getRuntime().getCallManagerFactory().getCallManager(id);
			if (cm!=null && cm.isSupported(ILocalRepository.class)) {
				String title = "";
				if (cm instanceof IConfigurable) {
					title = getI18nManager().getString(((IConfigurable)cm).getNamespace(), "title", "label", getLanguage()) + " - ";
				}
				title += ((ILocalRepository)cm).getFile();
				return title;
			}
			if (cm!=null && cm.isSupported(IRemoteRepository.class)) {
				String title = "";
				if (cm instanceof IConfigurable) {
					title = getI18nManager().getString(((IConfigurable)cm).getNamespace(), "title", "label", getLanguage());
				}
				return title;
			}
		}
		
		return null;
	}
	
	protected List getSearchHistory() {
		List l = new ArrayList();

		try {
			File history = new File(PathResolver.getInstance(this.getRuntime()).getConfigDirectory()+File.separator+"journal.history");
			if (!history.exists()) {
				history.getParentFile().mkdirs();
				history.createNewFile();
			}
			
			FileInputStream fstream = new FileInputStream(PathResolver.getInstance(this.getRuntime()).getConfigDirectory()+File.separator+"journal.history");
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;
	
			while ((strLine = br.readLine()) != null)   {
				if (!l.contains(strLine))
					l.add(strLine);
			}
			br.close();
		} catch (FileNotFoundException e) {
			m_logger.log(Level.SEVERE, "Could not read journal.history file: "+e.getMessage(), e);
		} catch (IOException e) {
			m_logger.log(Level.SEVERE, "Could not read journal.history file: "+e.getMessage(), e);
		}

		return l;
	}
	
	protected void setSearchHistory(List searchTerms) {
		try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(PathResolver.getInstance(this.getRuntime()).getConfigDirectory()+File.separator+"journal.history", false)));
		    for (int i=0;i<searchTerms.size();i++)
		    	out.println(searchTerms.get(i));
		    out.flush();
		    out.close();
		} catch (IOException e) {
		    m_logger.log(Level.SEVERE, "Could not write journal.history file: "+e.getMessage(), e);
		}
	}

	public boolean isSupportingRenderer(IRenderer r) {
		return (r instanceof IJournalCellRenderer);
	}


}