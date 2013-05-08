package de.janrufmonitor.ui.jface.application.history;

import java.util.List;
import java.util.Properties;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.PhonenumberFilter;
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
import de.janrufmonitor.util.formatter.Formatter;

public class History extends AbstractTableApplication {

	private class HistoryMenuBuilder extends AbstractMenuBuilder {

		private IRuntime m_runtime;
		
		public HistoryMenuBuilder(IApplication app, List l, List popupActions) {
			super(app, l, popupActions);
		}

		public MenuManager createMenu() {
			MenuManager master = new MenuManager();
			
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
			
			this.addAction(master, "clipboard");
			this.addSeparator(master);
			this.addAction(master, "showgrid");
			this.addAction(master, "zoomin");
			this.addAction(master, "zoomout");
			return master.createContextMenu(c);
		}

	}

	
	public static String NAMESPACE = "ui.jface.application.history.History";
	private IRuntime m_runtime;
	private TableContentProvider m_jp;
	private AbstractMenuBuilder m_mb;
	private ICaller m_caller;
	
	public History(ICaller c) {
		this(true, c);
	}
	
	public History(boolean isBlocking, ICaller c) {
		super(isBlocking);
		this.m_caller = c;
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
		return "History";
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
		return null;
	}

	protected AbstractMenuBuilder getMenuBuilder() {
		if (this.m_mb==null)
			this.m_mb = new HistoryMenuBuilder(this, this.getAdditionalMenuActions(), this.getAdditionalPopupActions());
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
	
	protected String getTitleExtension() {
		StringBuffer b = new StringBuffer();
		String name = Formatter.getInstance(getRuntime()).parse("%a:ln%, %a:fn% (%a:add%)", this.m_caller.getName());
		if (name.trim().length()==0) {
			name = Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, this.m_caller.getPhoneNumber());
		}
		b.append(name);
		return b.toString().trim();
	}

	public Properties getConfiguration() {
		Properties saved = super.getConfiguration();
		
		Properties journalConfig = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(Journal.NAMESPACE);
		if (journalConfig!=null && journalConfig.size()>0) {
			saved.setProperty(JournalConfigConst.CFG_REPOSITORY, journalConfig.getProperty(JournalConfigConst.CFG_REPOSITORY));
		}

		IFilter[] filters = null;
		if (this.m_caller instanceof IMultiPhoneCaller) {
			filters = new IFilter[
			     ((IMultiPhoneCaller)this.m_caller).getPhonenumbers().size()
			];
			for (int i=0,j=((IMultiPhoneCaller)this.m_caller).getPhonenumbers().size(); i<j; i++) {
				filters[i] = new PhonenumberFilter(
						(IPhonenumber) ((IMultiPhoneCaller)this.m_caller).getPhonenumbers().get(i)
				);
			}
		} else {
			filters = new IFilter[1];
			if (this.m_caller!=null)
				filters[0] = new PhonenumberFilter(this.m_caller.getPhoneNumber());
		}
		saved.setProperty("filter", 
			new JournalFilterManager().getFiltersToString(filters)
		);
			
		return saved;
	}


}
