package de.janrufmonitor.ui.jface.application.gos.wizard.pages;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.AbstractWebCallerManager;
import de.janrufmonitor.repository.CallerNotFoundException;
import de.janrufmonitor.repository.web.AbstractURLRequester;
import de.janrufmonitor.repository.web.RegExpURLRequester;
import de.janrufmonitor.repository.web.RegExpURLRequesterException;
import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.ui.jface.configuration.controls.DirectoryFieldEditor;
import de.janrufmonitor.ui.jface.wizards.pages.AbstractPage;
import de.janrufmonitor.util.io.PathResolver;

public class GosPage extends AbstractPage {
	
	private class TestWebCallerManager extends AbstractWebCallerManager {

		private static final String CFG_INTAREACODE = "intareacode";
		private static final String CFG_LOCALE = "locale";
		
		private IRuntime m_runtime;
		
		public TestWebCallerManager(Properties config) {
			super();
			this.m_configuration = config;
		}
		
		protected AbstractURLRequester createURLRequester(String url, long skip) {
			return createURLRequester(url, skip, null);
		}
		
		protected AbstractURLRequester createURLRequester(String url, long skip, String pn) {
			return new RegExpURLRequester(url, skip, pn, getNamespace(), this.m_configuration, this.getRuntime(), this.getLocale(), this.getSupportedIntAreaCode());
		}

		protected Locale getLocale() {
			String loc = this.m_configuration.getProperty(CFG_LOCALE, "de_DE");
			if (loc!=null) {
				if (loc.indexOf("_")>0) {
					return new Locale(loc.split("_")[0], loc.split("_")[1]);
				}
				return new Locale(loc);
			}
			return Locale.GERMANY;
		}
		
		protected String getSupportedIntAreaCode() {
			return this.m_configuration.getProperty(CFG_INTAREACODE, "49");
		}

		public String getID() {
			return "TestWebCallerManager";
		}

		public String getNamespace() {
			return "ui.jface.application.gos.wizard.pages.TestWebCallerManager";
		}

		public IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
		
	}

	private String NAMESPACE = "ui.jface.application.gos.wizard.pages.GosPage";
	
	private IRuntime m_runtime;
	
	private DirectoryFieldEditor dfe;
	private FileFieldEditor ffe;
	private Text url;
	private Text name;
	private Text version;
	private Text offset;
	private Text lastname;
	private Text firstname;
	private Text street;
	private Text streetno;
	private Text postal;
	private Text city;
	private Text area;
	private Text number;
	private Text intarea;
	private Combo locale;
	private Combo encoding;

	public GosPage(String name) {
		super(GosPage.class.getName());
		setTitle(this.m_i18n.getString(getNamespace(), "title", "label", this.m_language));
		setDescription(this.m_i18n.getString(getNamespace(), "description", "label", this.m_language));
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}	

	public String getNamespace() {
		return NAMESPACE;
	}

	public void createControl(Composite c) {
		Composite co = new Composite(c, SWT.NONE);
		co.setLayout(new GridLayout(2, false));
		co.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		int operations = DND.DROP_MOVE;
		Transfer[] types = new Transfer[] {FileTransfer.getInstance()};
		DropTarget target = new DropTarget(co, operations);
		target.setTransfer(types);

		target.addDropListener (new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {
				
				// A drop has occurred, copy over the data
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}
				
				File f = new File(((String[])event.data)[0]);
				m_logger.info("Detected drag action with file: "+f.getAbsolutePath());
				if (f.exists() && f.getName().toLowerCase().endsWith(".jam.zip")) {
					ZipArchive zip = new ZipArchive(f, false);
					try {
						zip.open();
						List files = zip.list(new FilenameFilter() {
							public boolean accept(File f, String name) {
								return name.toLowerCase().startsWith("install/repository.") && name.toLowerCase().endsWith(".inf");
							}
						});
						if (files.size()==1) {
							m_logger.info("Found a valid generic online module.");
							Properties inf = new Properties();
							inf.load(zip.get((String) files.get(0)));
							String ns = ((String) files.get(0)).substring("install/repository.".length(), ((String) files.get(0)).length()-4);
							name.setText(ns);
							url.setText(inf.getProperty("repository."+ns+":url:value", ""));
							offset.setText(inf.getProperty("repository."+ns+":skipbytes:value", "0"));
							lastname.setText(inf.getProperty("repository."+ns+":regexp.lastname:value", ""));
							firstname.setText(inf.getProperty("repository."+ns+":regexp.firstname:value", ""));
							street.setText(inf.getProperty("repository."+ns+":regexp.street:value", ""));
							streetno.setText(inf.getProperty("repository."+ns+":regexp.streetno:value", ""));
							postal.setText(inf.getProperty("repository."+ns+":regexp.pcode:value", ""));
							city.setText(inf.getProperty("repository."+ns+":regexp.city:value", ""));
							area.setText(inf.getProperty("repository."+ns+":regexp.areacode:value", ""));
							number.setText(inf.getProperty("repository."+ns+":regexp.phone:value", ""));
							intarea.setText(inf.getProperty("repository."+ns+":intareacode:value", ""));
						}
						zip.close();
					} catch (ZipArchiveException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (IOException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
	 	});
	    
	    Label l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "name", "label", this.m_language));
	    GridData gd = new GridData();
	    gd.widthHint = 150;
	    
	    name = new Text(co, SWT.BORDER);
	    name.setLayoutData(gd);
	    name.setText("");
	    name.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "version", "label", this.m_language));
	    gd = new GridData();
	    gd.widthHint = 50;
	    
	    version = new Text(co, SWT.BORDER);
	    version.setLayoutData(gd);
	    version.setText("1.0.0");
	    version.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "url", "label", this.m_language));
 
	    url = new Text(co, SWT.BORDER);
	    gd.widthHint = 300;
	    url.setLayoutData(gd);
	    url.setText("");
	    url.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "offset", "label", this.m_language));
	    offset = new Text(co, SWT.BORDER);
	    offset.setText("");
	    offset.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "lastname", "label", this.m_language));
 
	    lastname = new Text(co, SWT.BORDER);
	    lastname.setLayoutData(gd);
	    lastname.setText("");
	    lastname.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        lastname.setForeground(new Color(lastname.getDisplay(), 0,0,0));
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "firstname", "label", this.m_language));
 
	    firstname = new Text(co, SWT.BORDER);
	    firstname.setText("");
	    firstname.setLayoutData(gd);
	    firstname.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        firstname.setForeground(new Color(firstname.getDisplay(), 0,0,0));
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "street", "label", this.m_language));
 
	    street = new Text(co, SWT.BORDER);
	    street.setLayoutData(gd);
	    street.setText("");
	    street.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        street.setForeground(new Color(street.getDisplay(), 0,0,0));
	      }
	    });
	  
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "streetno", "label", this.m_language));
 
	    streetno = new Text(co, SWT.BORDER);
	    streetno.setLayoutData(gd);
	    streetno.setText("");
	    streetno.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        streetno.setForeground(new Color(streetno.getDisplay(), 0,0,0));
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "postal", "label", this.m_language));
 
	    postal = new Text(co, SWT.BORDER);
	    postal.setLayoutData(gd);
	    postal.setText("");
	    postal.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        postal.setForeground(new Color(postal.getDisplay(), 0,0,0));
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "city", "label", this.m_language));
 
	    city = new Text(co, SWT.BORDER);
	    city.setLayoutData(gd);
	    city.setText("");
	    city.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        city.setForeground(new Color(city.getDisplay(), 0,0,0));
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "area", "label", this.m_language));
 
	    area = new Text(co, SWT.BORDER);
	    area.setLayoutData(gd);
	    area.setText("");
	    area.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        area.setForeground(new Color(area.getDisplay(), 0,0,0));
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "number", "label", this.m_language));
 
	    number = new Text(co, SWT.BORDER);
	    number.setLayoutData(gd);
	    number.setText("");
	    number.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	        number.setForeground(new Color(number.getDisplay(), 0,0,0));
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "intarea", "label", this.m_language));
 
	    intarea = new Text(co, SWT.BORDER);
	    intarea.setText("49");
	    intarea.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	      }
	    });
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "locale", "label", this.m_language));

	    Locale[] locales = Locale.getAvailableLocales();
	    List localeList = new ArrayList(locales.length);
	    Locale lo = null;
	    for (int i=0;i<locales.length;i++) {
	    	lo = locales[i];
	    	if (lo.toString().indexOf("_")>0)
	    		localeList.add(lo);
	    }
	    
	    Collections.sort(localeList, new Comparator() {

			public int compare(Object l1, Object l2) {
				if (l1 instanceof Locale && l2 instanceof Locale) {
					return ((Locale)l1).getDisplayCountry().compareTo(((Locale)l2).getDisplayCountry());
				}
				return 0;
			} 
	    	
	    }
	    );
	    
	    String[] countryNames = new String[localeList.size()];
	    for (int i=0;i<localeList.size();i++) {
	    	countryNames[i] = ((Locale)localeList.get(i)).getDisplayCountry()+ " ("+((Locale)localeList.get(i)).getDisplayLanguage()+")";
	    }
	    
	    
	    locale = new Combo(co, SWT.READ_ONLY);
	    locale.setItems(countryNames);
	    for (int i = 0; i < countryNames.length; i++) {
	    	locale.setData(countryNames[i], localeList.get(i));
	    }
	    locale.select(0);
	    
	    locale.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	      }
	    });
	    
	    
	    l = new Label(co, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "encoding", "label", this.m_language));
	    String[] encodings = new String[] {
	    	"ISO-8859-1",
	    	"UTF-8",
	    	"UTF-16"
	    };
	    encoding = new Combo(co, SWT.READ_ONLY);
	    encoding.setItems(encodings);
	    for (int i = 0; i < encodings.length; i++) {
	    	encoding.setData(encodings[i], encodings[i]);
	    }
	    encoding.select(0);
	    
	    locale.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        setPageComplete(isComplete());
	      }
	    });
	    
	    Composite dco2 = new Composite(co, SWT.NONE);
		dco2.setLayout(new GridLayout(1, false));
		gd = new GridData();
	    gd.horizontalSpan = 2;
		dco2.setLayoutData(gd);
	    
		ffe = new FileFieldEditor(
	    	this.m_i18n.getString(this.getNamespace(), "fname", "label", this.m_language),
	    	this.m_i18n.getString(this.getNamespace(), "flabel", "label", this.m_language),
	    	dco2
	    );
		ffe.setFileExtensions(new String[] {"*.png"});
	    
	    l = new Label(co, SWT.LEFT);
	    l = new Label(co, SWT.LEFT);
		
		Composite dco = new Composite(co, SWT.NONE);
		dco.setLayout(new GridLayout(1, false));
		gd = new GridData();
	    gd.horizontalSpan = 2;
		dco.setLayoutData(gd);
		
	    dfe = new DirectoryFieldEditor(
	    	this.m_i18n.getString(this.getNamespace(), "dname", "label", this.m_language),
	    	this.m_i18n.getString(this.getNamespace(), "dlabel", "label", this.m_language),
	    	this.m_i18n.getString(this.getNamespace(), "dmessage", "label", this.m_language),
			dco
	    );
	    dfe.setStringValue(PathResolver.getInstance().getTempDirectory());

	    Group c2 = new Group(co, SWT.SHADOW_ETCHED_IN);
	    c2.setLayout(new GridLayout(2, false));
	    gd = new GridData();
	    gd.horizontalSpan = 2;
	    gd.horizontalAlignment = GridData.FILL_HORIZONTAL;
		c2.setLayoutData(gd);
	    l = new Label(c2, SWT.LEFT);
	    l.setText(this.m_i18n.getString(this.getNamespace(), "testnumber", "label", this.m_language));
	    l = new Label(c2, SWT.LEFT);
	    
	   
	    final Text testnum = new Text(c2, SWT.BORDER);
	    
	    testnum.setText("");
	    gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		//gd.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
	    testnum.setLayoutData(gd);

	    final Button test = new Button(c2, SWT.PUSH);
	    test.setEnabled(false);
	    test.setText(this.m_i18n.getString(this.getNamespace(), "test", "label", this.m_language));
	    test.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(testnum.getText().startsWith("0") ? testnum.getText().substring(1) : testnum.getText());
				pn.setIntAreaCode(intarea.getText());
				Properties config = new Properties();
				config.setProperty("version", version.getText());
				config.setProperty("skipbytes", offset.getText());
				config.setProperty("url", url.getText());
				config.setProperty("regexp.lastname", lastname.getText());
				config.setProperty("regexp.firstname", firstname.getText());
				config.setProperty("regexp.street", street.getText());
				config.setProperty("regexp.streetno", streetno.getText());
				config.setProperty("regexp.pcode", postal.getText());
				config.setProperty("regexp.city", city.getText());
				config.setProperty("regexp.areacode", area.getText());
				config.setProperty("regexp.phone", number.getText());
				config.setProperty("intareacode", intarea.getText());
				config.setProperty("locale", ((Locale) locale.getData(locale.getText())).toString());
				config.setProperty("encoding", encoding.getText());
				config.setProperty("icon", ffe.getStringValue());
				
				TestWebCallerManager twcm = new TestWebCallerManager(config);
				try {
					ICaller c = twcm.getCaller(pn);
					IService dialog = getRuntime().getServiceFactory().getService("DefaultCallDialogService");
					if (dialog!=null) {
						ICall call = getRuntime().getCallFactory().createCall(
							c, 
							getRuntime().getMsnManager().createMsn("12345"),
							getRuntime().getCipManager().createCip("1")
						);
						if (dialog instanceof AbstractReceiverConfigurableService) {
							((AbstractReceiverConfigurableService)dialog).receivedValidRule(call);
						}
						lastname.setForeground(new Color(lastname.getDisplay(), 0,0,0));
						firstname.setForeground(new Color(firstname.getDisplay(), 0,0,0));
						street.setForeground(new Color(street.getDisplay(), 0,0,0));
						streetno.setForeground(new Color(streetno.getDisplay(), 0,0,0));	
						postal.setForeground(new Color(postal.getDisplay(), 0,0,0));		
						city.setForeground(new Color(city.getDisplay(), 0,0,0));
						area.setForeground(new Color(area.getDisplay(), 0,0,0));
						number.setForeground(new Color(number.getDisplay(), 0,0,0));
					}
				} catch (CallerNotFoundException ex) {
					PropagationFactory.getInstance().fire(
							new Message(Message.INFO, getNamespace(), "notidentified", new String[] {pn.getTelephoneNumber()}, ex)
					);
					Throwable cause = ex.getCause(); // first CallerNotFoundException
					if (cause!=null && !(cause instanceof RegExpURLRequesterException)) {
						cause = cause.getCause();
					}
					if (cause!=null && cause instanceof RegExpURLRequesterException) {
						List failure = ((RegExpURLRequesterException)cause).getFailures();
						String c = null;
						for (int i=0;i<failure.size();i++) {
							c = (String) failure.get(i);
							if (c.equalsIgnoreCase("regexp.lastname")){
								lastname.setForeground(new Color(lastname.getDisplay(), 255,0,0));
							}
							if (c.equalsIgnoreCase("regexp.firstname")){
								firstname.setForeground(new Color(firstname.getDisplay(), 255,0,0));
							}
							if (c.equalsIgnoreCase("regexp.street")){
								street.setForeground(new Color(street.getDisplay(), 255,0,0));
							}
							if (c.equalsIgnoreCase("regexp.streetno")){
								streetno.setForeground(new Color(streetno.getDisplay(), 255,0,0));
							}
							if (c.equalsIgnoreCase("regexp.pcode")){
								postal.setForeground(new Color(postal.getDisplay(), 255,0,0));
							}
							if (c.equalsIgnoreCase("regexp.city")){
								city.setForeground(new Color(city.getDisplay(), 255,0,0));
							}
							if (c.equalsIgnoreCase("regexp.areacode")){
								area.setForeground(new Color(area.getDisplay(), 255,0,0));
							}
							if (c.equalsIgnoreCase("regexp.phone")){
								number.setForeground(new Color(number.getDisplay(), 255,0,0));
							}
						}
					}
				} finally {
					twcm = null;
				}
			}
	    	
	    });
	    
	    testnum.addKeyListener(new KeyListener() {
			public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
				if (testnum.getText().length()>0 && url.getText().length()>0 && number.getText().length()>0) test.setEnabled(true);
				else test.setEnabled(false);
			}

			public void keyReleased(org.eclipse.swt.events.KeyEvent e) {
			}
	    });
	    
	    this.setPageComplete(isComplete());
	    this.setControl(co);
	}
	
	public Map getResult() {
		Map m = new HashMap();
		m.put("url", url.getText());
		m.put("version", version.getText());
		m.put("name", name.getText());
		m.put("directory", dfe.getStringValue());
		m.put("offset", offset.getText());
		m.put("lastname", lastname.getText());
		m.put("firstname", firstname.getText());
		m.put("street", street.getText());
		m.put("streetno", streetno.getText());
		m.put("postal", postal.getText());
		m.put("city", city.getText());
		m.put("area", area.getText());
		m.put("number", number.getText());
		m.put("intarea", intarea.getText());
		m.put("icon", ffe.getStringValue());
		m.put("locale", ((Locale) locale.getData(locale.getText())).toString());
		m.put("encoding", encoding.getText());
		
		return m;
	}

	protected boolean isComplete() {
		super.isComplete();
		if (url.getText().length()==0) return false;
		if (version.getText().length()==0) return false;
		if (name.getText().length()==0) return false;
		if (area.getText().length()==0) return false;
		if (number.getText().length()==0) return false;
		if (intarea.getText().length()==0) return false;
		if (dfe.getStringValue().length()==0) return false;

		return true;
	}
}
