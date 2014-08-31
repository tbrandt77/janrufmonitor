package de.janrufmonitor.ui.jface.application.sound.action;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.types.IWriteCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.sound.SoundConst;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.configuration.pages.Sound.SelectMsnPage;
import de.janrufmonitor.ui.jface.wizards.AbstractWizard;
import de.janrufmonitor.ui.jface.wizards.pages.AbstractPage;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class ChangeAction extends AbstractAction {

	private class SoundDataWizard extends AbstractWizard {

		private String NAMESPACE = "ui.jface.wizards.SoundDataWizard"; 
		
		private String m_sdo;
		private AbstractPage[] m_pages;
		private IRuntime m_runtime;

		public SoundDataWizard(String sdo) {
			super();
	    	setWindowTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));

			this.m_sdo = sdo;
			
			this.m_pages = new AbstractPage[1];
			this.m_pages[0] = new SelectFilePage(this.m_sdo);
			
			this.addPage(this.m_pages[0]);
		}

		public String getID() {
			return SoundDataWizard.class.getName();
		}

		public String getNamespace() {
			return this.NAMESPACE;
		}

		public boolean performFinish() {
			if (this.m_pages[0].isPageComplete()) {
				this.m_sdo = ((SelectFilePage)this.m_pages[0]).getResult();
				return true;
			}
			return false;
		}

		public String getResult() {
			return this.m_sdo;
		}
		
		public boolean performCancel() {
			this.m_sdo = null;
			return super.performCancel();
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
	}

	public class SelectFilePage extends AbstractPage {

		private String NAMESPACE = "ui.jface.wizards.pages.sound.SelectFilePage";

		private String file;
		private IRuntime m_runtime;
		
		public SelectFilePage(String file) {
			super(SelectMsnPage.class.getName());
			if (file==null)
				file = "";
			
			this.file = file;
			
			setTitle(this.m_i18n.getString(getNamespace(), "title", "label", this.m_language));
			setDescription(this.m_i18n.getString(getNamespace(), "description", "label", this.m_language));
		}

		public String getNamespace() {
			return this.NAMESPACE;
		}
		
		public String getResult() {
			return this.file;
		}

		public void createControl(Composite parent) {
		    Composite nameComposite = new Composite(parent, SWT.NONE);
		    nameComposite.setLayout(new GridLayout(2, false));
		    nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		    GridData gd = new GridData();
	        gd.horizontalAlignment = GridData.FILL;
	        gd.grabExcessHorizontalSpace = true;
	        gd.horizontalSpan = 1;
	        gd.widthHint = 200;
	               
			Label l = new Label(nameComposite, SWT.LEFT);
			l.setText(
					this.m_i18n.getString(getNamespace(), "sound", "label", this.m_language)	
			);
			l.pack();
			
			l = new Label(nameComposite, SWT.LEFT);
				
			final Text m_textField = new Text(nameComposite, SWT.SINGLE | SWT.BORDER);
			m_textField.setLayoutData(gd);
			m_textField.setText(file);
			
		    Button b = new Button(nameComposite, SWT.PUSH);
			b.setText(JFaceResources.getString("openBrowse"));
			b.addSelectionListener(new SelectionAdapter() {
	            public void widgetSelected(SelectionEvent evt) {
	                String newValue = changePressed(m_textField);
	                if(newValue != null) {
	                	m_textField.setText(newValue);
	                	file = newValue;
	                }
	                setPageComplete(isComplete());
	            }
	        });
			
			b.pack();
		    
		    setPageComplete(isComplete());
		    setControl(nameComposite);
		}
		
		protected boolean isComplete() {
			return (file.length()>0);
		}
		
		protected String changePressed(Text t) {
			FileDialog dlg = new FileDialog(t.getShell(), SWT.OPEN);
			dlg.setFilterExtensions(new String[] {"*.wav", "*.mp3"});
			dlg.setFileName(t.getText());
			String f = dlg.open();
			return f;
		}
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
	}
	
	private static String NAMESPACE = "ui.jface.application.sound.action.ChangeAction";
	
	private IRuntime m_runtime;

	public ChangeAction() {
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

	public String getID() {
		return "sound_change";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		Viewer v = this.m_app.getApplication().getViewer();
		if (v!=null) {
			IStructuredSelection selection = (IStructuredSelection) v.getSelection();
			if (!selection.isEmpty()) {
				Object o = selection.getFirstElement();
				if (o instanceof ICall) {
					o = ((ICall)o).getCaller();
				}
				if (o instanceof ICaller) {
					if (((ICaller)o).getPhoneNumber().isClired()) return;
					
					IAttribute att = ((ICaller)o).getAttribute(SoundConst.ATTRIBUTE_USER_SOUNDFILE);
					if (att==null) {
						att = this.getRuntime().getCallerFactory().createAttribute(SoundConst.ATTRIBUTE_USER_SOUNDFILE, "");
					}
					
				    // Create the dialog
				    WizardDialog.setDefaultImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
				    SoundDataWizard sdoW = new SoundDataWizard(att.getValue());
				    WizardDialog dlg = new WizardDialog(new Shell(DisplayManager.getDefaultDisplay()), sdoW);
				    dlg.open();
				    if (dlg.getReturnCode() == WizardDialog.OK) {
						att.setValue(sdoW.getResult());
						((ICaller)o).setAttribute(att);
						
						this.m_app.getController().updateElement(o);
						this.m_app.updateViews(false);
				    }
				}
			}
		}
	}
	
	public boolean isEnabled() {
		if (this.m_app!=null && this.m_app.getController()!=null) {
			Object o = this.m_app.getController().getRepository();
			return (o instanceof IWriteCallerRepository);
		}
		return false;
	}
}
