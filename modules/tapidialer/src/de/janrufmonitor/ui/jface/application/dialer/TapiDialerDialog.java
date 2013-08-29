package de.janrufmonitor.ui.jface.application.dialer;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.xtapi.serviceProvider.IXTapiCallBack;
import net.xtapi.serviceProvider.MSTAPI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.xtapi.XTapiConst;

public class TapiDialerDialog extends TitleAreaDialog implements XTapiConst, IXTapiCallBack {

	private class TapiHandle {

		int m_handle;
		int m_line; 
		
		String m_name;

		public TapiHandle(int handle, int line, String name) {
			this.m_handle = handle;
			this.m_name = name;
			this.m_line = line;
		}

		public int getHandle() {
			return this.m_handle;
		}
		
		public int getLine() {
			return this.m_line;
		}

		public String toString() {
			return "TAPI handle: " + this.m_handle + " - " + this.m_name;
		}

	}
	
	private String NAMESPACE = "ui.jface.application.dialer.TapiDialerCommand";
	
	private II18nManager m_i18n;
	private String m_language;
	private IRuntime m_runtime;
	private Logger m_logger;
	
	private Text dialBox;
	private Combo tapi;
	private Map handlemap;
	private MSTAPI m_tapi;
	private String num;
	
	public TapiDialerDialog(Shell shell, String num) {
		super(shell);
		handlemap = new HashMap();
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.num = num;
	}
	
	public TapiDialerDialog(Shell shell) {
		this(shell, null);
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
	    l.setText(this.getI18nManager().getString(this.getNamespace(), "number", "label", this.getLanguage()));
	    l.setToolTipText(
    		this.getI18nManager().getString(this.getNamespace(), "number", "description", this.getLanguage())
	    );
	    
	    dialBox = new Text(composite, SWT.BORDER);
	    dialBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    dialBox.setText((num != null ? num : ""));   
	    dialBox.addKeyListener(new KeyAdapter() {
	    	public void keyReleased(KeyEvent e) {
	    		getButton(0).setEnabled((dialBox!=null && dialBox.getText().trim().length()>0));
	    	}
	    });
	    
	    dialBox.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent arg0) {
				getButton(0).setEnabled((dialBox!=null && dialBox.getText().trim().length()>0));
			}

			public void focusLost(FocusEvent arg0) {
				getButton(0).setEnabled((dialBox!=null && dialBox.getText().trim().length()>0));
			}
	    	
	    });
	    
	    tapi = new Combo(composite, SWT.READ_ONLY | SWT.FLAT);
	    m_tapi = new MSTAPI();
		int n = m_tapi.init(this);


		StringBuffer nameOfLine = null;
		String preselectedLine = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(NAMESPACE, "lineselection");
		int selectedItemCount = 0, counter = 0;
		if (preselectedLine==null || preselectedLine.length()==0) {
			preselectedLine = "";
		}
		for (int i = 0; i < n; i++) {
			nameOfLine = new StringBuffer();
			int m_lineHandle = m_tapi.openLineTapi(i, nameOfLine);
			if (m_lineHandle > 0) {
				m_logger.info("Opening line #" + m_lineHandle);
				m_logger.info("Opening line name "
						+ nameOfLine.toString());
				handlemap.put(nameOfLine.toString().trim(),
						new TapiHandle(m_lineHandle, i, nameOfLine
								.toString().trim()));
				tapi.add(nameOfLine.toString().trim());
				if (preselectedLine.equalsIgnoreCase(nameOfLine.toString().trim())) {
					selectedItemCount = counter;
				}
				counter++;
			}
		}
		if (tapi.getItemCount()>0)
			tapi.select(selectedItemCount);


	    dialBox.setFocus();
	    
		return super.createDialogArea(parent);
	}

	
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		this.getButton(1).dispose();
		
		this.getButton(0).setText(this.getI18nManager().getString(this.getNamespace(), "dial", "label", this.getLanguage()));
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.widthHint = 70;
		
		this.getButton(0).setLayoutData(gd);
		//this.getButton(0).setFocus();
	    getButton(0).setEnabled(false);
		
		this.setTitleImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_JPG));
	}
	
	protected void okPressed() {
		if (dialBox!=null) {
			String dial = Formatter.getInstance(getRuntime()).toCallablePhonenumber(dialBox.getText());
			// added 2010/03/06: check for dial prefix for outgoing calls
			if (this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Using dial prefix: "+this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX));
				dial = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
			}
			if (dial.trim().length()>0) {
				String text = getI18nManager()
				.getString("ui.jface.application.dialer.TapiDialerCommand",
						"success", "description",
						getLanguage());
				
				text = StringUtils.replaceString(text, "{%1}", dial);
				
				String text2 = this.getI18nManager().getString("ui.jface.application.dialer.TapiDialerCommand", "success", "label", this.getLanguage());
				text2 = StringUtils.replaceString(text2, "{%1}", tapi.getText());
				
				if (MessageDialog.openConfirm(
						new Shell(DisplayManager.getDefaultDisplay()),
						text2,
						text)
					) {

						String selectedTapi = tapi.getText();
						TapiHandle th = (TapiHandle) handlemap.get(selectedTapi);
						if (th!=null && th.getHandle()>0) {
							// save the handle for next time selection
							getRuntime().getConfigManagerFactory().getConfigManager().setProperty(NAMESPACE, "lineselection", selectedTapi);
							getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
							if (m_logger.isLoggable(Level.INFO)) {
								m_logger.info("Selected TAPI handle: "+th.toString());
							}
							m_tapi.connectCallTapi(th.getLine(), dial, th.getHandle());							
						} else {
							m_logger.warning("Invalid TAPI line for outgoing call selected.");
						}
				}
			
			}
		}
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

	public void callback(int dwDevice, int dwMessage, int dwInstance,
			int dwParam1, int dwParam2, int dwParam3) {
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
		}
		if (m_tapi!=null) {
			m_tapi.shutdownTapi();
			m_tapi = null;
		}
	}
}
