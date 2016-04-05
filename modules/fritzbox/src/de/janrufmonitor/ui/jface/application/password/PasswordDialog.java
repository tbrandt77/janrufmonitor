package de.janrufmonitor.ui.jface.application.password;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.string.StringUtils;

public class PasswordDialog extends TitleAreaDialog implements FritzBoxConst {

	private String NAMESPACE = "ui.jface.application.password.PasswordCommand";
	
	private II18nManager m_i18n;
	private String m_language;
	private IRuntime m_runtime;
	private Logger m_logger;
	
	private Text dialBox;

	public PasswordDialog(Shell shell) {
		super(shell);
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
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
		composite.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalIndent = 15;
		gd.verticalIndent = 15;
		composite.setLayoutData(gd);
				
		Label l = new Label(composite, SWT.LEFT);
		
		String user = this.getFritzBoxUser();
		if (user != null && user.length()>0) {
		    l.setText(StringUtils.replaceString(this.getI18nManager().getString(this.getNamespace(), "user", "label", this.getLanguage()), "{%1}", user));
		    l = new Label(composite, SWT.LEFT);
		    l = new Label(composite, SWT.LEFT);
		}
	   
	    l.setText(this.getI18nManager().getString(this.getNamespace(), "password", "label", this.getLanguage()));
	    
	    dialBox = new Text(composite, SWT.BORDER);
	    dialBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    dialBox.setEchoChar('*');

	    dialBox.setFocus();
	    
		return super.createDialogArea(parent);
	}
	
	private String getFritzBoxUser() {
		return getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_USER);
	}

	
	@Override
	protected void cancelPressed() {
		System.setProperty("jam.fritzbox.session.password", "");
		int c = Integer.parseInt(System.getProperty("jam.fritzbox.session.counter", "0"));
		c++;
		System.setProperty("jam.fritzbox.session.counter", Integer.toString(c));
		System.setProperty("jam.fritzbox.session.ispwdialogvisible", "false");
		System.setProperty("jam.fritzbox.session.donotlogin", "true");
		super.cancelPressed();
	}

	protected void okPressed() {
		if (dialBox!=null) {
			String pw = dialBox.getText();
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("FRITZ!Box Fon password received: "+pw);
			System.setProperty("jam.fritzbox.session.counter", "0");
			System.setProperty("jam.fritzbox.session.password", pw);
		}
		System.setProperty("jam.fritzbox.session.ispwdialogvisible", "false");
		super.okPressed();
	}
	
	protected II18nManager getI18nManager() {
		if (this.m_i18n==null) {
			this.m_i18n = this.getRuntime().getI18nManagerFactory().getI18nManager();
		}
		return this.m_i18n;
	}
	
	private String getNamespace() {
		return NAMESPACE;
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
