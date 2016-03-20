package de.janrufmonitor.ui.jface.application.journal.action.statistic;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.IFilterManager;
import de.janrufmonitor.ui.jface.application.journal.Journal;
import de.janrufmonitor.ui.jface.application.journal.JournalConfigConst;
import de.janrufmonitor.ui.jface.application.journal.JournalController;
import de.janrufmonitor.ui.jface.application.journal.JournalFilterManager;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.string.StringUtils;

public abstract class AbstractStatisticAction extends AbstractAction implements IStatistic, JournalConfigConst {

	private IRuntime m_runtime;
	protected ICallList m_cl;

	public AbstractStatisticAction() {
		super();
		this.setText(
			this.getI18nManager().getString(
				this.getNamespace(),
				"title",
				"label",
				this.getLanguage()
			)
		);
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	
	public void run() {
		this.setCallList(((JournalController)this.m_app.getController()).getCallList());
		
		StatisticDialog mcd = new StatisticDialog(new Shell(DisplayManager.getDefaultDisplay()), this, true);
		mcd.open();
	}

	public int getMaxItemCount() {
		return this.m_cl.size();
	}

	public String getMessage() {
		IFilterManager fm = new JournalFilterManager();
		
		IFilter[] f = fm.getFiltersFromString(this.m_app.getApplication()
				.getConfiguration().getProperty(CFG_FILTER, ""));

		String activeFilter = getFilterLabel(fm, f);
		
		String description = getI18nManager().getString(
				getNamespace(),
				"current",
				"label",
				getLanguage()
			);
		
		description = StringUtils.replaceString(description, "{%1}", activeFilter);
		
		return description;
	}

	public String getTitle() {
		return getI18nManager().getString(
				getNamespace(),
				"dialogtitle",
				"label",
				getLanguage()
			);
	}

	public String getDescription() {
		IFilterManager fm = new JournalFilterManager();
		
		IFilter[] f = fm.getFiltersFromString(this.m_app.getApplication()
				.getConfiguration().getProperty(CFG_FILTER, ""));

		String activeFilter = getFilterLabel(fm, f);
		
		String description = getI18nManager().getString(
				getNamespace(),
				"dialogtitle",
				"description",
				getLanguage()
			);
		
		description = StringUtils.replaceString(description, "{%1}", activeFilter);		
		return description;
	}

	public void setCallList(ICallList cl) {
		this.m_cl = cl;
	}
	
	public List getStatisticItems(int offset, int count) {
		return getStatisticItems().subList(offset, offset+count);
	}
	

	public int getMaxListItemCount() {
		return m_cl.size();
	}

	private String getFilterLabel(IFilterManager fm, IFilter[] f) {
		String name = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Journal.NAMESPACE, "filtername_"+fm.getFiltersToString(f));
		if (name!=null && name.length()>0) return name;
		return fm.getFiltersToLabelText(f, 45);
	}
}
