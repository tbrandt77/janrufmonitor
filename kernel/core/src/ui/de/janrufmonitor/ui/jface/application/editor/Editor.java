package de.janrufmonitor.ui.jface.application.editor;

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
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractMenuBuilder;
import de.janrufmonitor.ui.jface.application.AbstractTreeTableApplication;
import de.janrufmonitor.ui.jface.application.ActionRegistry;
import de.janrufmonitor.ui.jface.application.IApplication;
import de.janrufmonitor.ui.jface.application.IFilterManager;
import de.janrufmonitor.ui.jface.application.TreeContentProvider;
import de.janrufmonitor.ui.jface.application.action.IAction;
import de.janrufmonitor.ui.jface.application.dnd.IDropTargetHandler;
import de.janrufmonitor.ui.jface.application.editor.action.ImportAction;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IRenderer;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;

public class Editor extends AbstractTreeTableApplication implements EditorConfigConst {
	
	private class EditorMenuBuilder extends AbstractMenuBuilder {

		public EditorMenuBuilder(IApplication app, List additionalActions, List popupActions) {
			super(app, additionalActions, popupActions);
		}

		public IRuntime getRuntime() {
			return Editor.this.getRuntime();
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
			this.addAction(file, "editor_new_editor");
			this.addAction(file, "editor_open_editor");
			this.addAction(file, "editor_lastopen_editor");
			this.addSeparator(file);
			this.addAction(file, "editor_export");
			this.addAction(file, "editor_import");
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
			this.addAction(view, "editor_filter");
			this.addSeparator(view);
			this.addAction(view, "refresh");
			
			// create contact menu
			MenuManager contact = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"contact",
					"label",
					this.getLanguage()
				)
			);
			master.add(contact);
			
			this.addAction(contact, "editor_new");
			this.addSeparator(contact);
			this.addAction(contact, "editor_category");
			this.addSeparator(contact);
			this.addAction(contact, "select_all");
			this.addAction(contact, "delete_all");
			this.addSeparator(contact);
			this.addAction(contact, "search");
			this.addSeparator(contact);
			
			// create selected caller menu
			MenuManager selected = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"selected",
					"label",
					this.getLanguage()
				)
			);
			contact.add(selected);
			
			this.addAction(selected, "editor_change");
			this.addAction(selected, "editor_combine");
			this.addAction(selected, "editor_delete");
			this.addSeparator(selected);
			this.addAction(selected, "editor_quickcategory");
			this.addAction(selected, "editor_identify");
			this.addAction(selected, "clipboard");

			
			// create settings menu
			MenuManager settings = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"settings",
					"label",
					this.getLanguage()
				)
			);
			master.add(settings);
			this.addAction(settings, "showgrid");
			this.addAction(settings, "columnselect");
			

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

		public Menu createPopupMenu(Control c) {
			MenuManager master = new MenuManager();
			if (OSUtils.isMacOSX()) {
				master.add(this.createMenu());
				this.addSeparator(master);
			}
			this.addAction(master, "editor_new");
			this.addAction(master, "editor_change");
			this.addAction(master, "editor_combine");
			this.addAction(master, "editor_svcfexport");
			this.addAction(master, "editor_delete");
			this.addSeparator(master);
			this.addAction(master, "editor_quickcategory");
			this.addAction(master, "editor_geocoding");			
			this.addAction(master, "history");
			//this.addAction(master, "editor_quickimgpath");
			this.addAction(master, "editor_identify");
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
	
	private class EditorDropTargetHandler implements IDropTargetHandler {
		public void execute(String[] sources) {
			if (sources==null || sources.length == 0) return;
			IAction a = ActionRegistry.getInstance().getAction("editor_import", getApplication());
			if (a!=null && a instanceof ImportAction && ((ImportAction)a).isEnabled()) {
				((ImportAction)a).setSupressDialogs(true);
				((ImportAction)a).run(sources);
				((ImportAction)a).setSupressDialogs(false);
				updateViews(true);
			}
		}
	}
	
	public static String NAMESPACE = "ui.jface.application.editor.Editor";

	private IRuntime m_runtime;
	private TreeContentProvider m_jp;
	private AbstractMenuBuilder m_mb;
	private IDropTargetHandler m_dth;
	
	public Editor() {
		super(true);
	}

	protected AbstractMenuBuilder getMenuBuilder() {
		if (this.m_mb==null) {
			this.createApplicationController();
			this.m_controller.setConfiguration(this.getConfiguration(), true);
			this.m_mb = new EditorMenuBuilder(this, this.getAdditionalMenuActions(), this.getAdditionalPopupActions());
		}
		return m_mb;
	}
	
	protected IFilterManager getFilterManager() {
		return new EditorFilterManager();
	}

	protected ITreeContentProvider getContentProvider() {
		if (this.m_jp==null)
			this.m_jp = new TreeContentProvider(this.getConfiguration());
		return this.m_jp;
	}

	protected void createApplicationController() {
		if (this.m_controller==null) // 2010/10/20: added for performance reasons
			this.m_controller = new EditorController();
	}

	protected IAction getFilterAction() {
		return ActionRegistry.getInstance().getAction("editor_filter",	getApplication());
	}

	protected IAction getAssignAction() {
		return ActionRegistry.getInstance().getAction("editor_change", getApplication());
	}

	protected IAction getDeleteAction() {
		return ActionRegistry.getInstance().getAction("editor_delete",	getApplication());
	}

	protected IAction getColoringAction() {
		return ActionRegistry.getInstance().getAction("editor_coloring", getApplication());
	}

	protected IAction getOrderAction() {
		return ActionRegistry.getInstance().getAction("editor_order", getApplication());
	}
	
	protected IAction getQuickSearchAction() {
		return ActionRegistry.getInstance().getAction("editor_quicksearchterm", getApplication());
	}

	protected IAction getHightlightAction() {
		return null;
	}
	
	protected void initializeProviders() {
		if (this.m_jp!=null) this.m_jp.dispose();
		this.m_jp = null;
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public String getID() {
		return "Editor";
	}

	protected IDropTargetHandler getDropTargetHandler() {
		if (this.m_dth==null)
			this.m_dth = new EditorDropTargetHandler();
		return m_dth;
	}
	
	protected String getTitleExtension() {
		String id = this.m_configuration.getProperty(CFG_REPOSITORY, "");
		if (id!=null && id.length()>0) {
			ICallerManager cm = getRuntime().getCallerManagerFactory().getCallerManager(id);
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
	
	public boolean isSupportingRenderer(IRenderer r) {
		return (r instanceof IEditorCellRenderer);
	}
	
	protected List getSearchHistory() {
		List l = new ArrayList();

		try {
			File history = new File(PathResolver.getInstance(this.getRuntime()).getConfigDirectory()+File.separator+"contacts.history");
			if (!history.exists()) {
				history.getParentFile().mkdirs();
				history.createNewFile();
			}
			
			FileInputStream fstream = new FileInputStream(history);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;
	
			while ((strLine = br.readLine()) != null)   {
				if (!l.contains(strLine))
					l.add(strLine);
			}
			br.close();
		} catch (FileNotFoundException e) {
			m_logger.log(Level.SEVERE, "Could not read contacts.history file: "+e.getMessage(), e);
		} catch (IOException e) {
			m_logger.log(Level.SEVERE, "Could not read contacts.history file: "+e.getMessage(), e);
		}

		return l;
	}
	
	protected void setSearchHistory(List searchTerms) {
		try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(PathResolver.getInstance(this.getRuntime()).getConfigDirectory()+File.separator+"contacts.history", false)));
		    for (int i=0;i<searchTerms.size();i++)
		    	out.println(searchTerms.get(i));
		    out.flush();
		    out.close();
		} catch (IOException e) {
		    m_logger.log(Level.SEVERE, "Could not write contacts.history file: "+e.getMessage(), e);
		}
	}

}
