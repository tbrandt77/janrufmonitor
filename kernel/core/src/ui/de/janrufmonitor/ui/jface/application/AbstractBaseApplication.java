package de.janrufmonitor.ui.jface.application;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.ui.jface.application.action.IAction;
import de.janrufmonitor.ui.jface.application.dnd.IDropTargetHandler;
import de.janrufmonitor.ui.jface.application.editor.Editor;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.string.StringUtils;

public abstract class AbstractBaseApplication extends AbstractApplication implements IConfigConst {

	protected class ThreadStatus {
		private boolean isFinished;
		
		public boolean isFinished() {
			return isFinished;
		}
		public void setFinished(boolean isFinished) {
			this.isFinished = isFinished;
		}
	}
	
	protected IApplicationController m_controller;

	public AbstractBaseApplication() {
		this(true);
	}
	
	public AbstractBaseApplication(boolean isBlocking) {
		super(isBlocking);
		this.addMenuBar();
		this.addToolBar(SWT.FLAT);
		this.addStatusLine();
	}

	protected void setMenuItemStatus(Menu m) {
		MenuItem[] items = m.getItems();
		for (int i=0;i<items.length;i++) {
			if (items[i].getData() instanceof MenuManager) {
				setMenuItemStatus(((MenuManager)items[i].getData()).getMenu());
			} else if (items[i].getData() instanceof ActionContributionItem){
				org.eclipse.jface.action.IAction a = ((ActionContributionItem)items[i].getData()).getAction();
				if (a!=null) {
					items[i].setEnabled(a.isEnabled());
				}
			}
		}
	}

	public void initializeController() {
		this.m_controller = null;
	}

	public IApplicationController getController() {
		return this.m_controller;
	}
	
	public void focus() {
		if (this.getShell()!=null)
			this.getShell().forceFocus();
	}

	protected StatusLineManager createStatusLineManager() {
		return new StatusLineManager();
	}
	
	protected MenuManager createMenuManager() {
		if (this.getMenuBuilder()!=null)
			return this.getMenuBuilder().createMenu();
		return null;
	}
	
	protected void addRenderBeforeTableHooks(Composite composite) { }

	protected void addRenderAfterTableHooks(Composite parent) { }

	protected List getAdditionalMenuActions() {
		List actions = new ArrayList();
		
		String actionConfig = this.getConfiguration().getProperty(CFG_MENU_ACTIONS, "");
		if (actionConfig.length()>0) {
			StringTokenizer st = new StringTokenizer(actionConfig, ",");
			while (st.hasMoreTokens()) {
				actions.add(st.nextToken().trim());
			}
		}
		
		return actions;
	}
	
	protected List getAdditionalPopupActions() {
		List actions = new ArrayList();
		
		String actionConfig = this.getConfiguration().getProperty(CFG_POPUP_ACTIONS, "");
		if (actionConfig.length()>0) {
			StringTokenizer st = new StringTokenizer(actionConfig, ",");
			while (st.hasMoreTokens()) {
				actions.add(st.nextToken().trim());
			}
		}
		
		return actions;
	}
	
	protected boolean isShowToolbar() {
		return this.getConfiguration().getProperty(CFG_SHOW_TOOLBAR, "false").equalsIgnoreCase("true");
	}
	
	protected boolean isShowQuickSearch() {
		return (this.getConfiguration().getProperty(CFG_SHOW_QUICKSEARCH, "true").equalsIgnoreCase("true") && this.getQuickSearchAction()!=null && this.getQuickSearchAction().isEnabled());
	}
	
	protected Font getSizedFont(FontData baseFontData, int increment, boolean incremental){
		// set absolut value
		int fontHeight = increment;
		
		// set incremental value
		if (incremental)
			fontHeight = baseFontData.getHeight() + increment;
			
		if (fontHeight<=0) fontHeight = 5;
		if (fontHeight>=72) fontHeight = 72;
		baseFontData.setHeight(fontHeight);
		return new Font(DisplayManager.getDefaultDisplay(), baseFontData);
	}
	
	protected int getColumnNumber(String id) {
		String renderers = this.getConfiguration().getProperty(
				CFG_RENDERER_LIST, "");
		StringTokenizer st = new StringTokenizer(renderers, ",");
		int i = 0;
		while (st.hasMoreTokens()) {
			if (id.equalsIgnoreCase(st.nextToken()))
				return i;
			i++;
		}
		return -1;
	}
	
	protected int getTableColumnCount() {
		String renderers = this.getConfiguration().getProperty(
				CFG_RENDERER_LIST, "");
		StringTokenizer st = new StringTokenizer(renderers, ",");
		return st.countTokens();
	}

	protected String getColumnID(int column) {
		String renderers = this.getConfiguration().getProperty(
				CFG_RENDERER_LIST, "");
		StringTokenizer st = new StringTokenizer(renderers, ",");
		for (int i = 0; i < column; i++)
			st.nextToken();
		return st.nextToken();
	}
	

	protected void checkAmountOfEntries(long time1, long time2) {
		long delta = time2 - time1;
		long entries = this.m_controller.countElements();
		
		boolean doNotShowmessage = this.getConfiguration().getProperty(CFG_SHOW_ENTRY_WARNING, "false").equalsIgnoreCase("true");
		
		if (!doNotShowmessage && delta>2000 && entries>100) {
			long right_entry = Math.round((entries / (delta/1000)) * 0.9);
			String message = this.getI18nManager().getString(
				this.getNamespace(),
				"timemessage",
				"description",
				this.getLanguage()
			);
			
			message = StringUtils.replaceString(message, "{%1}", Long.toString(entries));
			message = StringUtils.replaceString(message, "{%2}", Long.toString(right_entry));
			
			PropagationFactory.getInstance().fire(
					new Message(Message.WARNING, 
							getI18nManager().getString(Editor.NAMESPACE,
							"title", "label",
							getLanguage()), 
							new Exception(message)),
					"Tray");
			
			MessageDialogWithToggle d = MessageDialogWithToggle.openWarning(
				new Shell(DisplayManager.getDefaultDisplay()),
				this.getI18nManager().getString(
						this.getNamespace(),
						"timemessage",
						"label",
						this.getLanguage()
					),
				message,
				this.getI18nManager().getString(
						this.getNamespace(),
						"confirmtimemessage",
						"label",
						this.getLanguage()
					),
				false,
				null,
				null
			);
			this.getConfiguration().setProperty(CFG_SHOW_ENTRY_WARNING, (d.getToggleState() ? "true" : "false"));
			this.storeConfiguration();
		}
	}

	protected int getOrderColumn(int column) {
		switch (column) {
		case 0:
			return this.getColumnNumber("date");
		case 1:
			return this.getColumnNumber("msn");
		case 2:
			int i = this.getColumnNumber("name");
			if (i==-1) i = this.getColumnNumber("namedetail");
			
			return (i==-1 ? this.getColumnNumber("name2") : i);
		case 3:
			return this.getColumnNumber("number");
		case 4:
			return this.getColumnNumber("cip");
		case 5:
			return this.getColumnNumber("namedetail");
		case 7:
			return this.getColumnNumber("address");		
		case 8:
			return this.getColumnNumber("category");	
		}		
		return -1;
	}
	
	protected void refreshController(boolean reload) {
		this.createApplicationController();

		this.m_controller.setConfiguration(this.getConfiguration(), reload);
	}
	
	protected String getTitleExtension() {
		return null;
	}
	
	public abstract void updateProviders();
		
	protected abstract AbstractMenuBuilder getMenuBuilder();
	
	protected abstract IFilterManager getFilterManager();
	
	protected abstract IDropTargetHandler getDropTargetHandler();
	
	protected abstract void initializeProviders();
	
	protected abstract void createApplicationController();
	
	protected abstract IAction getFilterAction();
	
	protected abstract IAction getAssignAction();
	
	protected abstract IAction getDeleteAction();
	
	protected abstract IAction getColoringAction();
	
	protected abstract IAction getOrderAction();
	
	protected abstract IAction getHightlightAction();
	
	protected abstract IAction getQuickSearchAction();
	
}

