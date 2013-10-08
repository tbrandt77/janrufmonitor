package de.janrufmonitor.ui.jface.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.wizards.pages.AbstractPage;
import de.janrufmonitor.ui.jface.wizards.pages.InitAreaCodePage;
import de.janrufmonitor.ui.jface.wizards.pages.InitDataPathPage;
import de.janrufmonitor.ui.jface.wizards.pages.InitFinalizePage;
import de.janrufmonitor.ui.jface.wizards.pages.InitMsnPage;
import de.janrufmonitor.ui.jface.wizards.pages.InitNumberFormatPage;
import de.janrufmonitor.ui.jface.wizards.pages.InitVariantPage;
import de.janrufmonitor.ui.jface.wizards.pages.InitWelcomePage;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.io.PathResolver;

public class InitializerWizard extends AbstractWizard {

	private String NAMESPACE = "ui.jface.wizards.InitializerWizard"; 

	private AbstractPage[] m_pages;
	
	private IRuntime m_runtime;
	boolean m_status = true;
	
	public InitializerWizard() {
		super();

		this.m_pages = new AbstractPage[7];
		this.m_pages[0] = new InitWelcomePage("");
		this.m_pages[1] = new InitDataPathPage(PathResolver.getInstance(getRuntime()).getUserDataDirectory());
		this.m_pages[2] = new InitMsnPage("");
		this.m_pages[3] = new InitNumberFormatPage("");
		this.m_pages[4] = new InitFinalizePage("");
		this.m_pages[5] = new InitAreaCodePage("");
		this.m_pages[6] = null;
		
		String clazz = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(InitVariantPage.NAMESPACE, "class");
		if (clazz!=null && clazz.length()>0) {
			try {
				Class c = Thread.currentThread().getContextClassLoader().loadClass(clazz);
				Object o = c.newInstance();
				if (o instanceof InitVariantPage) {
					this.m_pages[6] = (InitVariantPage)o;
				}
			} catch (ClassNotFoundException e) {
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
		}

		this.addPage(this.m_pages[0]);
		if (this.m_pages[6]!=null)
			this.addPage(this.m_pages[6]); // variant
		this.addPage(this.m_pages[5]); // areacode
		this.addPage(this.m_pages[3]); // number format
		this.addPage(this.m_pages[2]); // msn
		this.addPage(this.m_pages[1]); // user stores		
		this.addPage(this.m_pages[4]);
	
		setWindowTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
	}

	public String getID() {
		return InitializerWizard.class.getName();
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public boolean performFinish() {
		
		if (this.m_pages[1].isPageComplete() && 
			this.m_pages[2].isPageComplete()) {
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(DisplayManager.getDefaultDisplay().getActiveShell());	
			try {				
				IRunnableWithProgress r = new IRunnableWithProgress() {
					public void run(IProgressMonitor progressMonitor) {
						progressMonitor.beginTask(getI18nManager()
								.getString(getNamespace(),
										"changeudpath", "label",
										getLanguage()), IProgressMonitor.UNKNOWN);
						
						progressMonitor.worked(1);
						m_status &= ((InitDataPathPage) m_pages[1]).performFinish();
						
						if (m_pages[6]!=null) {
							progressMonitor.setTaskName(getI18nManager()
									.getString(getNamespace(),
											"variant", "label",
											getLanguage()));
							m_status &= ((InitVariantPage) m_pages[6]).performFinish();
						}
					
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"msnsave", "label",
										getLanguage()));
						m_status &= ((InitMsnPage) m_pages[2]).performFinish();
								
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"numberformat", "label",
										getLanguage()));
						m_status &= ((InitNumberFormatPage) m_pages[3]).performFinish();
						
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"areacode", "label",
										getLanguage()));
						m_status &= ((InitAreaCodePage) m_pages[5]).performFinish();
						
						progressMonitor.setTaskName(getI18nManager()
								.getString(getNamespace(),
										"finalize", "label",
										getLanguage()));
						m_status &= ((InitFinalizePage) m_pages[4]).performFinish();

						progressMonitor.done();
					}
				};
				pmd.setBlockOnOpen(false);
				pmd.run(true, false, r);

				//ModalContext.run(r, true, pmd.getProgressMonitor(), DisplayManager.getDefaultDisplay());
			} catch (InterruptedException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (InvocationTargetException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
			} 		
						
			return m_status;
		}
		return false;
	}

	public boolean performCancel() {
		return super.performCancel();
	}

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


	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
}
