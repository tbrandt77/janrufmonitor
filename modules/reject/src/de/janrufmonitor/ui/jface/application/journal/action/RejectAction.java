package de.janrufmonitor.ui.jface.application.journal.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.journal.JournalConfigConst;
import de.janrufmonitor.util.formatter.Formatter;

public class RejectAction extends AbstractAction implements JournalConfigConst {

	private static String NAMESPACE = "ui.jface.application.journal.action.RejectAction";
	
	private IRuntime m_runtime;

	public RejectAction() {
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
		Viewer v = this.m_app.getApplication().getViewer();
		if (v!=null && v instanceof Viewer) {
			final IStructuredSelection selection = (IStructuredSelection) v.getSelection();
			if (!selection.isEmpty()) {
				Iterator i = selection.iterator();
				Object o = null;
				while (i.hasNext()) {
					o = i.next();
					if (o instanceof ICall) {
						o = ((ICall)o).getCaller();
					}
					if (o instanceof ICaller) {
						if (this.isRejectable(((ICaller)o))) {
							IPhonenumber pn = ((ICaller)o).getPhoneNumber();
							setAsRejectNumber(pn);
						}
					}
				}
				m_app.updateViews(false);
			}
		}
	}
	
	private void setAsRejectNumber(IPhonenumber pn) {
		String rejnums = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty("service.Reject", "rejectareacodes");
		if (rejnums==null || rejnums.trim().length()==0) rejnums = "";
		
		StringTokenizer st = new StringTokenizer(rejnums, ",");
		List list = new ArrayList(st.countTokens()+1);
		while (st.hasMoreTokens()) {
			list.add(st.nextToken().trim());	
		}
		list.add(PhonenumberAnalyzer.getInstance(getRuntime()).normalize(
				Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, pn)
		));
		StringBuffer slist = new StringBuffer();
		String m = null;
		for (int i=0;i<list.size();i++) {
			m = (String)list.get(i);
			slist.append(m);
			slist.append(",");
		}
		
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty("service.Reject", "rejectareacodes", slist.toString());
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
	}

	private boolean isRejectable(ICaller c) {
		return !(c.getPhoneNumber().isClired() || PhonenumberAnalyzer.getInstance(getRuntime()).isInternal(c.getPhoneNumber()));
	}

	public String getID() {
		return "journal_reject";
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isEnabled() {
		return true;
	}

}

