package de.janrufmonitor.ui.jface.application.last10calls;

import java.util.List;
import java.util.Properties;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.ItemCountFilter;
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
import de.janrufmonitor.ui.jface.application.journal.Journal;
import de.janrufmonitor.ui.jface.application.journal.JournalConfigConst;
import de.janrufmonitor.ui.jface.application.journal.JournalController;
import de.janrufmonitor.ui.jface.application.journal.JournalFilterManager;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IRenderer;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.io.OSUtils;

public class Last10Calls extends AbstractTableApplication implements IEventReceiver {

	private class Last10CallsMenuBuilder extends AbstractMenuBuilder {

		private IRuntime m_runtime;
		
		public Last10CallsMenuBuilder(IApplication app, List l, List popupActions) {
			super(app, l, popupActions);
		}

		public MenuManager createMenu() {
			MenuManager master = new MenuManager();
			if (OSUtils.isMacOSX()) {
				 master = new MenuManager(this.getI18nManager().getString(
						 	Journal.NAMESPACE,
							"master",
							"label",
							this.getLanguage()
						));
			}
			
			// create file menu
			MenuManager file = new MenuManager(
				this.getI18nManager().getString(
					Journal.NAMESPACE,
					"file",
					"label",
					this.getLanguage()
				)
			);
			master.add(file);
			this.addAction(file, "journal_export");
			this.addSeparator(file);
			this.addAction(file, "close");
			
			// create view menu
			MenuManager view = new MenuManager(
				this.getI18nManager().getString(
					Journal.NAMESPACE,
					"view",
					"label",
					this.getLanguage()
				)
			);
			master.add(view);
			this.addAction(view, "refresh");
			this.addSeparator(view);
			this.addAction(view, "search");
			this.addSeparator(view);
			this.addAction(view, "columnselect");
		
			// create ? menu
			MenuManager q = new MenuManager(
				this.getI18nManager().getString(
					Journal.NAMESPACE,
					"q",
					"label",
					this.getLanguage()
				)
			);
			master.add(q);
			this.addAction(q, "help");
			
			return master;
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
			this.addAction(master, "clipboard");
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

	}

	
	public static String NAMESPACE = "ui.jface.application.last10calls.Last10Calls";
	public static String CFG_COUNT = "count";
	private IRuntime m_runtime;
	private TableContentProvider m_jp;
	private AbstractMenuBuilder m_mb;

	public Last10Calls() {
		this(true);
	}
	
	public Last10Calls(boolean isBlocking) {
		super(isBlocking);

	}
	
	public String getNamespace() {
		return NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "Last10Calls";
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

	protected void initializeProviders() {
		if (this.m_jp!=null) this.m_jp.dispose();
		this.m_jp = null;
	}
	
	protected IAction getAssignAction() {
		return null;
	}

	protected IAction getColoringAction() {
		return null;
	}

	protected IAction getDeleteAction() {
		return null;
	}

	protected IDropTargetHandler getDropTargetHandler() {
		return null;
	}

	protected IAction getFilterAction() {
		return null;
	}

	protected IFilterManager getFilterManager() {
		return new JournalFilterManager();
	}

	protected IAction getHightlightAction() {
		return ActionRegistry.getInstance().getAction("journal_highlight", getApplication());
	}

	protected AbstractMenuBuilder getMenuBuilder() {
		if (this.m_mb==null)
			this.m_mb = new Last10CallsMenuBuilder(this, this.getAdditionalMenuActions(), this.getAdditionalPopupActions());
		return m_mb;
	}

	protected IAction getOrderAction() {
		return ActionRegistry.getInstance().getAction("journal_order", getApplication());
	}

	protected IAction getQuickSearchAction() {
		return null;
	}
	
	public boolean isSupportingRenderer(IRenderer r) {
		return (r instanceof IJournalCellRenderer);
	}

	public Properties getConfiguration() {
		Properties saved = super.getConfiguration();
		
		Properties journalConfig = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(Journal.NAMESPACE);
		if (journalConfig!=null && journalConfig.size()>0) {
			saved.setProperty(JournalConfigConst.CFG_REPOSITORY, journalConfig.getProperty(JournalConfigConst.CFG_REPOSITORY));
		}

		IFilter[] filters = new IFilter[1];
		int count = 10;
		try {
			count = Integer.parseInt(saved.getProperty(CFG_COUNT, "10"));
		} catch (Exception e) {}
		filters[0] = new ItemCountFilter(count);
		
		saved.setProperty("filter", 
			new JournalFilterManager().getFiltersToString(filters)
		);
			
		return saved;
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
		}
	}

	public String getReceiverID() {
		return this.getID();
	}

	public int getPriority() {
		return 0;
	}
	
	public boolean close() {
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_UPDATE_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALL_MANAGER_UPDATED));
		return super.close();
	}
	
	public int open() {
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_UPDATE_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALL_MANAGER_UPDATED));
		return super.open();
	}


}
