package de.janrufmonitor.ui.jface.application.dialer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.client.request.handler.Dial;
import de.janrufmonitor.service.client.request.handler.GetDialExtensions;
import de.janrufmonitor.service.commons.CommonsConst;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IHttpResponse;
import de.janrufmonitor.service.commons.http.IRequester;
import de.janrufmonitor.service.commons.http.RequesterFactory;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.string.StringUtils;

public class ClientDialerDialog extends TitleAreaDialog implements CommonsConst {

	private class ClientHandle {

		int m_handle;
		
		String m_name;

		public ClientHandle(int handle, String name) {
			this.m_handle = handle;
			this.m_name = name;
		}

		public String getLine() {
			return this.m_name;
		}
		
		public String toString() {
			return "Client handle: " + this.m_handle + " - " + this.m_name;
		}

	}
	
	private String NAMESPACE = "ui.jface.application.dialer.ClientDialerCommand";
	
	private II18nManager m_i18n;
	private String m_language;
	private IRuntime m_runtime;
	private Logger m_logger;
	
	private Text dialBox;
	private Combo line;
	private Map handlemap;

	private String num;
	
	public ClientDialerDialog(Shell shell, String num) {
		super(shell);
		handlemap = new HashMap();
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.num = num;
	}
	
	public ClientDialerDialog(Shell shell) {
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
	    

	    line = new Combo(composite, SWT.READ_ONLY | SWT.FLAT);

		String[] lines = this.getLines();

		String preselectedLine = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(NAMESPACE, "lineselection");
		int selectedItemCount = 0, counter = 0;
		if (preselectedLine==null || preselectedLine.length()==0) {
			preselectedLine = "";
		}
		if (lines!=null) {
			for (int i = 0; i < lines.length; i++) {
				handlemap.put(StringUtils.replaceString(lines[i], "$ktoken$", ","),
						new ClientHandle(i, StringUtils.replaceString(lines[i], "$ktoken$", ",")));
				line.add(StringUtils.replaceString(lines[i], "$ktoken$", ","));
				if (preselectedLine.equalsIgnoreCase(StringUtils.replaceString(lines[i], "$ktoken$", ","))) {
					selectedItemCount = counter;
				}
				counter++;
			}
			if (line.getItemCount()>0)
				line.select(selectedItemCount);
		}
	    
	    dialBox.setFocus();
	    
		return super.createDialogArea(parent);
	}

	
	private String[] getLines() {
		IRequester r = this.getRequester(new GetDialExtensions());
		IHttpResponse resp = r.request();
		this.m_logger.info("GetExtensionsList returned "+resp.getCode());
		if (resp.getCode()==200) {
			try {
				String lines = new String(resp.getContent());
				if (lines!=null & lines.length()>0) {
					this.m_logger.info("GetExtensionsList returned "+lines);
					return lines.split(",");
				}
			} catch (Exception e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
		}
		return null;
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
			String dial = PhonenumberAnalyzer.getInstance().toCallable(dialBox.getText());
			// added 2010/03/06: check for dial prefix for outgoing calls
			if (this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Using dial prefix: "+this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX));
				dial = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
			}
			if (dial.trim().length()>0) {
				String text = getI18nManager()
				.getString("ui.jface.application.dialer.ClientDialerCommand",
						"success", "description",
						getLanguage());
				
				text = StringUtils.replaceString(text, "{%1}", dial);
				
				String text2 = this.getI18nManager().getString("ui.jface.application.dialer.ClientDialerCommand", "success", "label", this.getLanguage());
				text2 = StringUtils.replaceString(text2, "{%1}", line.getText());
				
				if (MessageDialog.openConfirm(
						new Shell(DisplayManager.getDefaultDisplay()),
						text2,
						text)
					) {

						String selectedTapi = line.getText();
						ClientHandle th = (ClientHandle) handlemap.get(selectedTapi);
						if (th!=null && th.getLine().length()>0) {
							// save the handle for next time selection
							getRuntime().getConfigManagerFactory().getConfigManager().setProperty(NAMESPACE, "lineselection", selectedTapi);
							getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
							if (m_logger.isLoggable(Level.INFO)) {
								m_logger.info("Selected Client handle: "+th.toString());
							}
							
							try {
								IRequester r = this.getRequester(new Dial(dial, URLEncoder.encode(th.getLine(), "UTF-8")));
								IHttpResponse resp = r.request();
								if (resp.getCode()!=200) {
									m_logger.severe("Invalid client call to server. Error code "+resp.getCode());
								}
							} catch (UnsupportedEncodingException e) {
								m_logger.log(Level.SEVERE, e.getMessage(), e);
							}
						} else {
							m_logger.warning("Invalid Client line for outgoing call selected.");
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
	
	private IRequester getRequester(IHttpRequest request) {
		return RequesterFactory.getInstance().
			createRequester(request);
	}
}
