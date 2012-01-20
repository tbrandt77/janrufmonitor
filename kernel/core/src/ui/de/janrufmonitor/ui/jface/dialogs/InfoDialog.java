package de.janrufmonitor.ui.jface.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.controls.HyperLink;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.uuid.UUID;

public class InfoDialog extends TitleAreaDialog {

	private static String NAMESPACE = "ui.jface.dialogs.InfoDialog";
	
	private II18nManager m_i18n;
	private String m_language;
	private IRuntime m_runtime;
	
	public InfoDialog(Shell shell) {
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
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(11);
		Font font = new Font(parent.getDisplay(), fd);
		
		Label l = new Label(composite, SWT.NONE);
		l.setText("jAnrufmonitor");
		l.setFont(font);
		l.setLayoutData(gd);
		
		gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 8;
		
		// check for edition
		String edition = getI18nManager().getString(
				NAMESPACE,
				"edition",
				"label",
				getLanguage()
			);
		
		if (edition!=null && edition.trim().length()>0 && !edition.equalsIgnoreCase("edition")) 
			l.setText(l.getText()+" "+edition);

		l = new Label(composite, SWT.NONE);
		l.setImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_TELEFON_JPG));
		l.setLayoutData(gd);
		
		fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		font = new Font(parent.getDisplay(), fd);
		
		l = new Label(composite, SWT.NONE);
		l.setText(
			getI18nManager().getString(
					NAMESPACE,
					"license",
					"label",
					getLanguage()
				)
		);
		l.setFont(font);

		new Label(composite, SWT.NONE).setText(
			getI18nManager().getString(
					NAMESPACE,
					"freeware",
					"label",
					getLanguage()
				)
		);
			
		l= new Label(composite, SWT.NONE);
		l.setText(
			getI18nManager().getString(
					NAMESPACE,
					"version",
					"label",
					getLanguage()
				)
		);
		l.setFont(font);

		new Label(composite, SWT.NONE).setText(
			IJAMConst.VERSION_DISPLAY
		);
		
		l = new Label(composite, SWT.NONE);
		l.setText(
			getI18nManager().getString(
					NAMESPACE,
					"build",
					"label",
					getLanguage()
				)
		);
		l.setFont(font);

		new Label(composite, SWT.NONE).setText(
			IJAMConst.VERSION_BUILD
		);	
		
		l = new Label(composite, SWT.NONE);
		l.setText(
			getI18nManager().getString(
					NAMESPACE,
					"rkey",
					"label",
					getLanguage()
				)
		);
		l.setFont(font);
		
		String key = getRuntime().getConfigManagerFactory().getConfigManager().getProperty("service.update.UpdateManager", "regkey");
		if (key==null || key.length()==0) {
			key = new UUID().toString();
			getRuntime().getConfigManagerFactory().getConfigManager().setProperty("service.update.UpdateManager", "regkey", key);
			getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
		}
		
		HyperLink hl = new HyperLink(composite, SWT.NONE);
		hl.setText(key);
		final String rkey = key;
		hl.addMouseListener( 
			new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (e.button==1)
					Program.launch("http://www.janrufmonitor.de/registry.php?k="+rkey);
				}
			}
		);
		
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		
		l = new Label(composite, SWT.NONE);
		l.setText(
			getI18nManager().getString(
					NAMESPACE,
					"homepage",
					"label",
					getLanguage()
				)
		);
		l.setFont(font);
		
		hl = new HyperLink(composite, SWT.NONE);
		hl.setText("http://www.janrufmonitor.de/");
		hl.addMouseListener( 
			new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (e.button==1)
					Program.launch("http://www.janrufmonitor.de/");
				}
			}
		);
		
		l = new Label(composite, SWT.NONE);
		l.setText(
			getI18nManager().getString(
					NAMESPACE,
					"mail",
					"label",
					getLanguage()
				)
		);
		l.setFont(font);
		
		hl = new HyperLink(composite, SWT.NONE);
		hl.setText("support@janrufmonitor.de");
		hl.addMouseListener( 
			new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (e.button==1)
					Program.launch("http://www.janrufmonitor.de/support-request.php?k="+rkey);
				}
			}
		);		
		
		l = new Label(composite, SWT.NONE);
		l.setText(
			getI18nManager().getString(
					NAMESPACE,
					"donation",
					"label",
					getLanguage()
				)
		);
		l.setFont(font);
		
		hl = new HyperLink(composite, SWT.NONE);
		hl.setText("http://www.janrufmonitor.de/donation/");
		hl.addMouseListener( 
			new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (e.button==1)
					Program.launch("http://www.janrufmonitor.de/donation/");
				}
			}
		);	
	
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		gd.widthHint = 300;
		
		l = new Label(composite, SWT.RIGHT);
		l.setText(
			"(c) 2008 - 2012 by Thilo Brandt      "
		);
		l.setLayoutData(gd);
		
		return super.createDialogArea(parent);
	}

	
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		this.getButton(1).dispose();
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.widthHint = 50;
		
		this.getButton(0).setLayoutData(gd);
		this.getButton(0).setFocus();
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
