package de.janrufmonitor.ui.jface.application.journal.action;

import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.types.ISearchableCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.IConfigConst;
import de.janrufmonitor.ui.jface.application.journal.Journal;
import de.janrufmonitor.ui.jface.application.journal.JournalConfigConst;

public class QuickSearchTermAction extends AbstractAction {

	private static String NAMESPACE = "ui.jface.application.journal.action.QuickSearchTermAction";

	private IRuntime m_runtime;

	private String m_searchString;

	public QuickSearchTermAction() {
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
		return "journal_quicksearchterm";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void setData(Object s) {
		if (s instanceof String)
			this.m_searchString = (String) s;
	}

	public void run() {
		if (this.m_searchString != null) {
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(Journal.NAMESPACE, IConfigConst.CFG_SEARCHTERMS, this.m_searchString);
			this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
			this.m_app.getApplication().getConfiguration().setProperty(IConfigConst.CFG_SEARCHTERMS, this.m_searchString);
		}
		
		this.m_app.getApplication().initializeController();
		this.m_app.updateViews(null, true);
	}
	
	public boolean isEnabled() {
		String cmgr = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Journal.NAMESPACE, JournalConfigConst.CFG_REPOSITORY);
		if (cmgr!=null) {
			ICallManager cm = this.getRuntime().getCallManagerFactory().getCallManager(cmgr);
			return (cm!=null && cm.isActive() && cm.isSupported(ISearchableCallRepository.class));
		}
		return false;
	}

}
