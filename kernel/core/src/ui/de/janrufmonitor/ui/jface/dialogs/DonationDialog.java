package de.janrufmonitor.ui.jface.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class DonationDialog extends TitleAreaDialog {

	private static String NAMESPACE = "ui.jface.dialogs.DonationDialog";
	
	private II18nManager m_i18n;
	private String m_language;
	private IRuntime m_runtime;
	
	public DonationDialog(Shell shell) {
		super(shell);
	}
	
	protected Control createContents(Composite parent) {
		Control c = super.createContents(parent);

		setTitle(
			getI18nManager().getString(
				NAMESPACE,
				"dialogtitle",
				"label",
				getLanguage()
			)
		);
		
		setMessage(getI18nManager().getString(
				NAMESPACE,
				"dialogtitle",
				"description",
				getLanguage()
			));
		return c;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalIndent = 15;
		gd.verticalIndent = 15;
		composite.setLayoutData(gd);
		
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		
		Label t = new Label(composite, SWT.NONE);
		t.setEnabled(true);
		t.setText(getI18nManager().getString(
				NAMESPACE,
				"disclaimer",
				"label",
				getLanguage()
			));
		t.setSize(100, 100);
		
		return super.createDialogArea(parent);
	}

	
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		this.getButton(1).dispose();
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 1;
		gd.widthHint = 100;
		
		Button b = new Button(this.getButton(0).getParent(), SWT.PUSH);		
		b.setText(getI18nManager().getString(
				NAMESPACE,
				"donate",
				"label",
				getLanguage()
			));
		b.setLayoutData(gd);
		b.setFocus();
		b.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				getRuntime().getConfigManagerFactory().getConfigManager().setProperty("service.DonationService", "intervall", Integer.toString(200));
				getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
				Program.launch("https://www.janrufmonitor.de/ihre-unterstuetzung/");
				close();
			}});
		
		
		this.getButton(0).setLayoutData(gd);
		this.getButton(0).setText(getI18nManager().getString(
				NAMESPACE,
				"nondonate",
				"label",
				getLanguage()
			));
		this.setTitleImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_JPG));
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
	
	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
}
