package de.janrufmonitor.ui.jface.wizards;

import java.util.logging.Level;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.wizards.pages.AbstractPage;
import de.janrufmonitor.ui.jface.wizards.pages.MultiPhoneCallerPage;

public class JournalCallerWizard extends AbstractWizard {

	private String NAMESPACE = "ui.jface.wizards.JournalCallerWizard"; 

	private ICaller m_caller;
	private AbstractPage[] m_pages;
	private IRuntime m_runtime;
	
	public JournalCallerWizard(ICaller caller) {
		super();
		setWindowTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
		if (caller == null) {
			IName name = this.getRuntime().getCallerFactory().createName("", "");
			IPhonenumber pn = this.getRuntime().getCallerFactory().createPhonenumber(false);
			this.m_caller = this.getRuntime().getCallerFactory().createCaller(name, pn);
		} else {
			this.m_caller = caller;
		}
		
		ICaller clonedCaller = null;
		try {
			clonedCaller = (ICaller) this.m_caller.clone();
		} catch (CloneNotSupportedException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		this.m_pages = new AbstractPage[1];
		this.m_pages[0] = new MultiPhoneCallerPage(clonedCaller, false, false, true);
		
		this.addPage(this.m_pages[0]);
	}

	public String getID() {
		return JournalCallerWizard.class.getName();
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public boolean performFinish() {
		if (this.m_pages[0].isPageComplete()) {	
			this.m_caller = ((MultiPhoneCallerPage)this.m_pages[0]).getResult();
			return true;
		}
		return false;
	}

	public boolean performCancel() {
		this.m_caller = null;
		return super.performCancel();
	}
	
	public ICaller getResult() {
		return this.m_caller;
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
}
