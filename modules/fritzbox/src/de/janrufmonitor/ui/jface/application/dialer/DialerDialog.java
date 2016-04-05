package de.janrufmonitor.ui.jface.application.dialer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

//import org.eclipse.jface.dialogs.MessageDialog;
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

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.string.StringUtils;

public class DialerDialog extends TitleAreaDialog implements FritzBoxConst {

	private String NAMESPACE = "ui.jface.application.dialer.DialerCommand";
	
	private II18nManager m_i18n;
	private String m_language;
	private IRuntime m_runtime;
	private Logger m_logger;
	
	private Combo dialBox;
	private Combo dialPrefix;
	private Combo clickDial;
	private String number;
	private List dials;

	public DialerDialog(Shell shell) {
		this(shell, null);		
	}
	
	public DialerDialog(Shell shell, String number) {
		super(shell);
		this.number = number;
		this.dials = this.getLast10DialedNumbers();
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
		
		String clickdial = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, CFG_CLICKDIAL);
		if (clickdial!=null && clickdial.equalsIgnoreCase("0")) {
			String[] pfs = {
					"Alle ISDN Ger\u00E4te",
					"FON 1", "FON 2", "FON 3",
					"ISDN 1", "ISDN 2", "ISDN 3", "ISDN 4", "ISDN 5", "ISDN 6", "ISDN 7", "ISDN 8", "ISDN 9",
					"DECT 610", "DECT 611", "DECT 612", "DECT 613", "DECT 614", "DECT 615"
			};
			int lastext = 0;
			String lext = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, "lastext");
			if (lext!=null && lext.length()>0) {
				for (int i=0;i<pfs.length;i++) {
					if (pfs[i].equalsIgnoreCase(lext)) lastext = i;
				}
			}
			Label l = new Label(composite, SWT.LEFT);
			l.setText(this.getI18nManager().getString(this.getNamespace(), "boxclickdial", "label", this.getLanguage()));
		    l.setToolTipText(
				this.getI18nManager().getString(this.getNamespace(), "boxclickdial", "description", this.getLanguage())
		    );
		       
		    clickDial = new Combo(composite, SWT.READ_ONLY);
		    clickDial.setItems(pfs);
		    clickDial.select(lastext);	    
		}
		
		String prefixes = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, "dialprefixes");
		if (prefixes!=null && prefixes.length()>0) {
			String[] pfs = prefixes.split(",");	
			Label l = new Label(composite, SWT.LEFT);
			l.setText(this.getI18nManager().getString(this.getNamespace(), "prefix", "label", this.getLanguage()));
		    l.setToolTipText(
				this.getI18nManager().getString(this.getNamespace(), "prefix", "description", this.getLanguage())
		    );
		       
		    dialPrefix = new Combo(composite, SWT.READ_ONLY);
		    dialPrefix.setItems(pfs);
		    dialPrefix.select(0);	    
		}
		
	    Label l = new Label(composite, SWT.LEFT);
	    l.setText(this.getI18nManager().getString(this.getNamespace(), "number", "label", this.getLanguage()));
	    l.setToolTipText(
    		this.getI18nManager().getString(this.getNamespace(), "number", "description", this.getLanguage())
	    );
	    
	    dialBox = new Combo(composite, SWT.DROP_DOWN);
	    dialBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    String[] ldials = new String[(this.number != null ? (dials.size()+1) : dials.size())];
	    if (this.number!=null) ldials[0] = this.number;
	    int k = 0;
	    if (this.number!=null) k = 1;
	    for (int i=(this.number != null ? 1 : 0); i<ldials.length; i++) {
	    	ldials[i] = (String) dials.get(i-k);
	    }
	    
	    dialBox.setItems(ldials);
	    dialBox.select(0);	 
	    
	    //dialBox.setText(this.number == null ? "" : number);   
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
			String dial = PhonenumberAnalyzer.getInstance(getRuntime()).toCallable(dialBox.getText());
			
			if (dials!=null) {
				int maxnums = Integer.parseInt(this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_LAST_DIALED_NUMBERS));
				if (dials.size()>=maxnums) {
					dials = dials.subList(0, maxnums-1);
				}
				dials.add(0, dial);
				this.setLast10DialedNumbers(dials);
			}
			
			// added 2010/03/06: check for dial prefix for outgoing calls
			if (this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Using dial prefix: "+this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX));
				dial = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
			}
			if (dial.trim().length()>0) {
				if (dialPrefix!=null) {
					if (dialPrefix.getText().trim().length()>0) {
						dial = dialPrefix.getText().trim() + dial;
					}
				}
				Properties config = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
				String clickdial = config.getProperty(CFG_CLICKDIAL, "50");
				if (clickDial!=null) {
					if (clickDial.getText().trim().length()>0) {
						if (clickDial.getText().equalsIgnoreCase("FON 1")) clickdial = "1";
						if (clickDial.getText().equalsIgnoreCase("FON 2")) clickdial = "2";
						if (clickDial.getText().equalsIgnoreCase("FON 3")) clickdial = "3";
						if (clickDial.getText().equalsIgnoreCase("Alle ISDN Ger\u00E4te")) clickdial = "50";
						if (clickDial.getText().equalsIgnoreCase("ISDN 1")) clickdial = "51";
						if (clickDial.getText().equalsIgnoreCase("ISDN 2")) clickdial = "52";
						if (clickDial.getText().equalsIgnoreCase("ISDN 3")) clickdial = "53";
						if (clickDial.getText().equalsIgnoreCase("ISDN 4")) clickdial = "54";
						if (clickDial.getText().equalsIgnoreCase("ISDN 5")) clickdial = "55";
						if (clickDial.getText().equalsIgnoreCase("ISDN 6")) clickdial = "56";
						if (clickDial.getText().equalsIgnoreCase("ISDN 7")) clickdial = "57";
						if (clickDial.getText().equalsIgnoreCase("ISDN 8")) clickdial = "58";
						if (clickDial.getText().equalsIgnoreCase("ISDN 9")) clickdial = "59";
						if (clickDial.getText().equalsIgnoreCase("DECT 610")) clickdial = "60";
						if (clickDial.getText().equalsIgnoreCase("DECT 611")) clickdial = "61";
						if (clickDial.getText().equalsIgnoreCase("DECT 612")) clickdial = "62";
						if (clickDial.getText().equalsIgnoreCase("DECT 613")) clickdial = "63";
						if (clickDial.getText().equalsIgnoreCase("DECT 614")) clickdial = "64";
						if (clickDial.getText().equalsIgnoreCase("DECT 615")) clickdial = "65";
						this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(FritzBoxMonitor.NAMESPACE, "lastext", clickDial.getText());
					}
				}
				String text = getI18nManager()
				.getString("ui.jface.application.fritzbox.action.ClickDialAction",
						"dial", "description",
						getLanguage());
				
				text = StringUtils.replaceString(text, "{%1}", dial);

					FirmwareManager fwm = FirmwareManager.getInstance();
					try {
						if (!fwm.isLoggedIn())
							fwm.login();
						
						fwm.doCall(dial + "#", clickdial);
				
							text = getI18nManager()
							.getString("ui.jface.application.fritzbox.action.ClickDialAction",
									"success", "description",
									getLanguage());
							
							PropagationFactory.getInstance().fire(
									new Message(Message.INFO, 
											getI18nManager().getString("monitor.FritzBoxMonitor",
													"title", "label",
													getLanguage()), 
											new Exception(StringUtils.replaceString(text, "{%1}", dial))),
									"Tray");	
						
					} catch (IOException e) {
						this.m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
										"ui.jface.application.fritzbox.action.ClickDialAction",
								"faileddial",	
								e));
					} catch (FritzBoxLoginException e) {
						this.m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
										"ui.jface.application.fritzbox.action.ClickDialAction",
								"faileddial",	
								e));
					} catch (DoCallException e) {
						this.m_logger.warning(e.toString());
						PropagationFactory.getInstance().fire(
								new Message(Message.ERROR,
										"ui.jface.application.fritzbox.action.ClickDialAction",
								"faileddial",	
								e));
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
	
	private List getLast10DialedNumbers() {
		List l = new ArrayList();
		String dialss = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(NAMESPACE, "lastdials");
		if (dialss!=null && dialss.length()>0) {
			StringTokenizer st = new StringTokenizer(dialss, "$");
			String num = null;
			while (st.hasMoreTokens()) {
				num = st.nextToken();
				if (!l.contains(num))
					l.add(num);
			}
		}
		return l;
	}
	
	private void setLast10DialedNumbers(List l) {
		if (l==null) return;
		StringBuffer sb = new StringBuffer();
		for (int i=0;i<l.size();i++) {
			sb.append(l.get(i));
			if (i<l.size()-1)
				sb.append("$");
		}
		this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(NAMESPACE, "lastdials", sb.toString());
		this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
}
