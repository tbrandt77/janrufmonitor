package de.janrufmonitor.ui.jface.configuration.pages;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.ui.jface.configuration.AbstractConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.wizards.AbstractWizard;
import de.janrufmonitor.ui.jface.wizards.pages.AbstractPage;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class MailNotification extends AbstractConfigPage {

	private class MailDataWizard extends AbstractWizard {

		private String NAMESPACE = "ui.jface.wizards.MailNotification"; 
		
		private MailDataObject m_sdo;
		private AbstractPage[] m_pages;
		private IRuntime m_runtime;

		protected IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
		
		public MailDataWizard(MailDataObject sdo) {
			super();
	    	setWindowTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
	    	if (sdo==null)
	    		sdo = new MailDataObject(null, "", "", "", "", "", "", null);
	    	
			this.m_sdo = sdo;
			
			this.m_pages = new AbstractPage[4];
			this.m_pages[0] = new SelectMsnPage(this.m_sdo.getMsn());
			this.m_pages[1] = new SelectMailPage(new String[]{this.m_sdo.getMailto(), this.m_sdo.getMailfrom()});
			this.m_pages[2] = new SelectMailTemplatePage(this.m_sdo.getTemplate());
			this.m_pages[3] = new SelectMailContentPage(new String[]{this.m_sdo.getFormat(), this.m_sdo.getEncoding(), this.m_sdo.getSubject(), this.m_sdo.getBody()}, this.m_pages[2]);
			
			this.addPage(this.m_pages[0]);
			this.addPage(this.m_pages[1]);
			this.addPage(this.m_pages[2]);
			this.addPage(this.m_pages[3]);
		}

		public String getID() {
			return MailDataWizard.class.getName();
		}

		public String getNamespace() {
			return this.NAMESPACE;
		}

		public boolean performFinish() {
			if (this.m_pages[0].isPageComplete() && 
				this.m_pages[1].isPageComplete() && 
				this.m_pages[2].isPageComplete() &&
				this.m_pages[3].isPageComplete() ) {
				IMsn m = ((SelectMsnPage)this.m_pages[0]).getResult();
				String[] mails = ((SelectMailPage)this.m_pages[1]).getResult();
				String template = ((SelectMailTemplatePage)this.m_pages[2]).getResult();
				String[] contents = ((SelectMailContentPage)this.m_pages[3]).getResult();
				this.m_sdo = new MailDataObject(m, mails[0], mails[1], contents[0], contents[1] ,contents[2], contents[3], template);
				return true;
			}
			return false;
		}

		public MailDataObject getResult() {
			return this.m_sdo;
		}
		
		public boolean performCancel() {
			this.m_sdo = null;
			return super.performCancel();
		}
		
	}

	public class SelectMsnPage extends AbstractPage {

		private String NAMESPACE = "ui.jface.wizards.pages.notification.SelectMsnPage";

		private IMsn m_msn;
		private IRuntime m_runtime;
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
		
		public SelectMsnPage(IMsn msn) {
			super(SelectMsnPage.class.getName());
			if (msn==null)
				msn = PIMRuntime.getInstance().getCallFactory().createMsn("","");
			
			this.m_msn = msn;
			
			setTitle(this.m_i18n.getString(getNamespace(), "title", "label", this.m_language));
			setDescription(this.m_i18n.getString(getNamespace(), "description", "label", this.m_language));
		}

		public String getNamespace() {
			return this.NAMESPACE;
		}
		
		public IMsn getResult() {
			return this.m_msn;
		}

		public void createControl(Composite parent) {
		    Composite nameComposite = new Composite(parent, SWT.NONE);
		    nameComposite.setLayout(new GridLayout(1, false));
		    nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		    Label ln = new Label(nameComposite, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "name", "label", this.m_language));

			final Combo msn = new Combo (nameComposite, SWT.READ_ONLY);
			
			String[] msns = PIMRuntime.getInstance().getMsnManager().getMsnList();
			String[] msnList = new String[msns.length];
			int select = 0;
			for (int i=0;i<msns.length;i++) {
				String msnalias = msns[i] + " ("+PIMRuntime.getInstance().getMsnManager().getMsnLabel(msns[i]) +")";
				msnList[i] = msnalias;
				msn.setData(msnalias, msns[i]);
				if (msns[i].equalsIgnoreCase(this.m_msn.getMSN())) {
					select=i;	
				}
			}
			msn.setItems(msnList);
			msn.select(select);
			
			if (msn.getSelectionIndex()>=0) {
				String smsn = msn.getItem(msn.getSelectionIndex());
		      	smsn = (String) msn.getData(smsn);
		      	
		        this.m_msn = PIMRuntime.getInstance().getMsnManager().createMsn(smsn);
			}
			
		    // Add the handler to update the name based on input
			msn.addModifyListener(new ModifyListener() {
		      public void modifyText(ModifyEvent event) {
		      	if (msn.getSelectionIndex()>=0) {
			      	String smsn = msn.getItem(msn.getSelectionIndex());
			      	smsn = (String) msn.getData(smsn);
			      	
			        m_msn = PIMRuntime.getInstance().getMsnManager().createMsn(smsn);
		      	}
		        setPageComplete(isComplete());
		      }
		    });
		    
		    setPageComplete(isComplete());
		    setControl(nameComposite);
		}
		
		protected boolean isComplete() {
			return (this.m_msn.getMSN().length()>0);
		}
	}
	
	public class SelectMailPage extends AbstractPage {

		private String NAMESPACE = "ui.jface.wizards.pages.notification.SelectMailPage";

		private String[] mails;
		private IRuntime m_runtime;
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
		
		public SelectMailPage(String[] mails) {
			super(SelectMailPage.class.getName());
			if (mails==null || mails.length<2 || mails[0]==null || mails[1]==null)
				mails = new String[] {"",""};
			
			this.mails = mails;
			
			setTitle(this.m_i18n.getString(getNamespace(), "title", "label", this.m_language));
			setDescription(this.m_i18n.getString(getNamespace(), "description", "label", this.m_language));
		}

		public String getNamespace() {
			return this.NAMESPACE;
		}
		
		public String[] getResult() {
			return this.mails;
		}

		public void createControl(Composite parent) {
		    Composite nameComposite = new Composite(parent, SWT.NONE);
		    nameComposite.setLayout(new GridLayout(1, false));
		    nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		    Label ln = new Label(nameComposite, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "mailto", "label", this.m_language));

			final Text mailto = new Text(nameComposite, SWT.BORDER);
			mailto.setText(
			    this.mails[0]
			);
			mailto.addKeyListener(
				new KeyAdapter() {
					public void keyReleased(KeyEvent e) {
						mails[0] = mailto.getText();
						setPageComplete(isComplete());
					}
				}
			);

			GridData gd = new GridData();
	        gd.horizontalAlignment = GridData.FILL;
	        gd.grabExcessHorizontalSpace = true;
	        gd.widthHint = 150;
	        mailto.setLayoutData(gd);
	        
			ln = new Label(nameComposite, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "mailfrom", "label", this.m_language));

			final Text mailfrom = new Text(nameComposite, SWT.BORDER);
			mailfrom.setText(
			    this.mails[1]
			);
			mailfrom.addKeyListener(
				new KeyAdapter() {
					public void keyReleased(KeyEvent e) {
						mails[1] = mailfrom.getText();
						setPageComplete(isComplete());
					}
				}
			);
			gd = new GridData();
	        gd.horizontalAlignment = GridData.FILL;
	        gd.grabExcessHorizontalSpace = true;
	        gd.widthHint = 150;
	        mailfrom.setLayoutData(gd);
		    
		    setPageComplete(isComplete());
		    setControl(nameComposite);
		}
		
		protected boolean isComplete() {
			if (this.mails.length==2 && mails[0].length()>3 && mails[1].length()>3) {
				if (mails[0].indexOf("@")<0) return false;
				if (mails[1].indexOf("@")<0) return false;
				
				if (mails[0].indexOf(".")<0) return false;
				if (mails[1].indexOf(".")<0) return false;
				
				return true;
			}
			return false;
		}
	}
	public class SelectMailTemplatePage extends AbstractPage {

		private String NAMESPACE = "ui.jface.wizards.pages.notification.SelectMailTemplatePage";

		private String m_template;
		private IRuntime m_runtime;
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
		
		public SelectMailTemplatePage(String template) {
			super(SelectMailPage.class.getName());
			if (template!=null && template.length()>0)
				this.m_template = template;
			
			setTitle(this.m_i18n.getString(getNamespace(), "title", "label", this.m_language));
			setDescription(this.m_i18n.getString(getNamespace(), "description", "label", this.m_language));
		}

		public String getNamespace() {
			return this.NAMESPACE;
		}
		
		public String getResult() {
			return this.m_template;
		}

		public void createControl(Composite parent) {
		    Composite nameComposite = new Composite(parent, SWT.NONE);
		    nameComposite.setLayout(new GridLayout(1, false));
		    nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		    final Button active = new Button(nameComposite, SWT.CHECK);
			active.setText(
					this.m_i18n.getString(getNamespace(), "active", "label", this.m_language)
			);
			active.setSelection(
				this.m_template!=null
			);
			
			final Composite c1 = new Composite(nameComposite, SWT.NONE);
			c1.setLayout(new GridLayout(1, false));
			c1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			c1.setEnabled(this.m_template!=null);
			
			Label ln = new Label(c1, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "templates", "label", this.m_language));

		    final Combo templates = new Combo (c1, SWT.READ_ONLY);
		    GridData gd = new GridData();
	        gd.widthHint = 150;
	        templates.setLayoutData(gd);
	        templates.setEnabled(this.m_template!=null);
	        
			String[] templatess = getTemplates();

			int select = 0;
			for (int i=0;i<templatess.length;i++) {
				if (templatess[i].equalsIgnoreCase(this.m_template)) {
					select=i;	
				}
			}
			templates.setItems(templatess);
			templates.select(select);
			
			
			final Label filename = new Label(c1, SWT.LEFT);
			filename.setText("");
			
			final Text templateText = new Text(c1, SWT.READ_ONLY | SWT.SHADOW_ETCHED_IN | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			gd = new GridData();
	        gd.widthHint = 500;
	        gd.heightHint = 250;
	        templateText.setFont(new Font(DisplayManager.getDefaultDisplay(), "Courier New", 8, SWT.NORMAL));
			templateText.setLayoutData(gd);
			templateText.setText("");
			
			if (this.m_template!=null && this.m_template.length()>0) {
				filename.setText(getTemplatePath(this.m_template));
	      		templateText.setText(getTemplateContent(this.m_template));
			}
						
			active.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
			      	if (active.getSelection()) {
			      		
			      		c1.setEnabled(true);
			      		templates.setEnabled(true);
			      		if (templates.getSelectionIndex()>=0) {
					      	String smsn = templates.getItem(templates.getSelectionIndex());
					      	m_template = smsn;
				      	}
			      	} else {
			      		m_template = "";
			      		c1.setEnabled(false);
			      		templates.setEnabled(false);
			      	}
			        setPageComplete(isComplete());
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			
			templates.addModifyListener(new ModifyListener() {
			      public void modifyText(ModifyEvent event) {
			      	if (templates.getSelectionIndex()>=0) {
				      	String smsn = templates.getItem(templates.getSelectionIndex());
				      	m_template = smsn;
				      	if (smsn.length()>0) {
				      		filename.setText(getTemplatePath(smsn));
				      		templateText.setText(getTemplateContent(smsn));
				      	} else {
				      		filename.setText("");
				      		templateText.setText("");
				      	}
			      	} else {
			      		filename.setText("");
			      		templateText.setText("");
			      	}
			        setPageComplete(isComplete());
			      }
			    });
			
	        
		    setPageComplete(isComplete());
		    setControl(nameComposite);
		}
		
		private String getTemplatePath(String template) {
			File templateDir = new File(PathResolver.getInstance(getRuntime()).getConfigDirectory(), "templates");
			File t = new File(templateDir, template+".template");
			return t.getAbsolutePath();
		}
		
		private String getTemplateContent(String template) {
			File templateDir = new File(PathResolver.getInstance(getRuntime()).getConfigDirectory(), "templates");
			File t = new File(templateDir, template+".template");
			if (t.exists()) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					Stream.copy(new FileInputStream(t), bos, true);
				} catch (FileNotFoundException e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
				} catch (IOException e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
				}
				return new String(bos.toByteArray());
			}
			return "";
		}
		
		private String[] getTemplates() {
			String[] templates = null;
			File templateDir = new File(PathResolver.getInstance(getRuntime()).getConfigDirectory(), "templates");
			if (!templateDir.exists()) templateDir.mkdirs();
			File[] templateList = templateDir.listFiles(new FilenameFilter() {
				public boolean accept(File parent, String name) {
					return name.toLowerCase().endsWith("template");
				}});
			
			templates = new String[templateList.length+1];
			templates[0] = "";
			for (int i=0;i<templateList.length;i++) {
				templates[i+1] = templateList[i].getName().substring(0, templateList[i].getName().lastIndexOf("."));
			}
			return templates;
		}
		
		protected boolean isComplete() {
			return true;
		}
	}

	public class SelectMailContentPage extends AbstractPage {

		private String NAMESPACE = "ui.jface.wizards.pages.notification.SelectMailContentPage";

		private String[] mails;
		private IRuntime m_runtime;
		private IWizardPage m_prev;
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
		
		public SelectMailContentPage(String[] mails, IWizardPage prev) {
			super(SelectMailPage.class.getName());
			this.m_prev = prev;
			if (mails==null || mails.length<4 || (mails[2].length()==0 && mails[3].length()==0 ))
				mails = new String[] {
					"", 
					"", 
					MailNotification.this.getPreferenceStore().getString(CONFIG_NAMESPACE + SEPARATOR + "default_mailsubject"),
					MailNotification.this.getPreferenceStore().getString(CONFIG_NAMESPACE + SEPARATOR + "default_mailcontent")
				};
	
			this.mails = mails;
			
			setTitle(this.m_i18n.getString(getNamespace(), "title", "label", this.m_language));
			setDescription(this.m_i18n.getString(getNamespace(), "description", "label", this.m_language));
		}

		public String getNamespace() {
			return this.NAMESPACE;
		}
		
		public String[] getResult() {
			return this.mails;
		}

		private String[] getValueList(String s){
			StringTokenizer st = new StringTokenizer(s, ",");
			String[] list = new String[st.countTokens()];
			int i=0;
			while (st.hasMoreElements()) {
				list[i] = st.nextToken();
				i++;
			}
			return list;
		}
		
		public void createControl(Composite parent) {
		    final Composite nameComposite = new Composite(parent, SWT.NONE);
		    nameComposite.setLayout(new GridLayout(1, false));
		    nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
		    nameComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					if (m_prev!=null) {
						String r = ((SelectMailTemplatePage)m_prev).getResult();
						
						Control[] c = nameComposite.getChildren();
						for (int i=0;i<c.length;i++)
							c[i].setEnabled(r==null || r.length()==0);
					}		
				}});
		    
		    Label ln = new Label(nameComposite, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "format", "label", this.m_language));

		    final Combo format = new Combo (nameComposite, SWT.READ_ONLY);
			
			String[] formats = getValueList(PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(
					getConfigNamespace(), "mimetype", "values"
			));
			
			int select = 0;
			for (int i=0;i<formats.length;i++) {
				if (formats[i].equalsIgnoreCase(this.mails[0])) {
					select=i;	
				}
			}
			format.setItems(formats);
			format.select(select);
			mails[0] = format.getItem(format.getSelectionIndex());
			GridData gd = new GridData();
	        gd.widthHint = 150;
	        format.setLayoutData(gd);
	        format.addModifyListener(new ModifyListener() {
		      public void modifyText(ModifyEvent event) {
		      	if (format.getSelectionIndex()>=0) {
			      	String smsn = format.getItem(format.getSelectionIndex());
			      	mails[0] = smsn;
		      	}
		        setPageComplete(isComplete());
		      }
		    });
	        
	        
			ln = new Label(nameComposite, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "encoding", "label", this.m_language));

		    final Combo encoding = new Combo (nameComposite, SWT.READ_ONLY);
			
			String[] encodings = getValueList(PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(
					getConfigNamespace(), "encoding", "values"
			));
			
			select = 0;
			for (int i=0;i<formats.length;i++) {
				if (encodings[i].equalsIgnoreCase(this.mails[1])) {
					select=i;	
				}
			}
			encoding.setItems(encodings);
			encoding.select(select);
			mails[1] = encoding.getItem(encoding.getSelectionIndex());
			gd = new GridData();
	        gd.widthHint = 150;
	        encoding.setLayoutData(gd);
	        
	        encoding.addModifyListener(new ModifyListener() {
		      public void modifyText(ModifyEvent event) {
		      	if (encoding.getSelectionIndex()>=0) {
			      	String smsn = encoding.getItem(encoding.getSelectionIndex());
			      	mails[1] = smsn;
		      	}
		        setPageComplete(isComplete());
		      }
		    });
	        
			ln = new Label(nameComposite, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "subject", "label", this.m_language));

			final Text mailfrom = new Text(nameComposite, SWT.BORDER);
			mailfrom.setText(
			    this.mails[2]
			);
			mailfrom.addKeyListener(
				new KeyAdapter() {
					public void keyReleased(KeyEvent e) {
						mails[2] = mailfrom.getText();
						setPageComplete(isComplete());
					}
				}
			);
			gd = new GridData();
	        gd.horizontalAlignment = GridData.FILL;
	        gd.grabExcessHorizontalSpace = true;
	        gd.widthHint = 150;
	        mailfrom.setLayoutData(gd);
		    
	        ln = new Label(nameComposite, SWT.LEFT);
		    ln.setText(this.m_i18n.getString(this.getNamespace(), "content", "label", this.m_language));

			final Text content = new Text(nameComposite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
			content.setText(
			    this.mails[3]
			);
			content.addKeyListener(
				new KeyAdapter() {
					public void keyReleased(KeyEvent e) {
						mails[3] = content.getText();
						setPageComplete(isComplete());
					}
				}
			);
			gd = new GridData();
	        gd.horizontalAlignment = GridData.FILL;
	        gd.grabExcessHorizontalSpace = true;
	        gd.heightHint = 150;
	        content.setLayoutData(gd);
	        
		    setPageComplete(isComplete());
		    setControl(nameComposite);
		}
		
		protected boolean isComplete() {
			return (this.mails.length==4);
		}
	}

	private class MailDataObject  {
		
		private IMsn m;
		private String mailto;	
		private String mailfrom;	
		private String format;	
		private String encoding;	
		private String subject;
		private String body;
		private String template;
		
		public MailDataObject(IMsn msn, String mailto, String mailfrom, String format, String encoding, String subject, String body, String t) {
			this.m = msn;
			this.mailto = mailto;
			this.mailfrom = mailfrom;
			this.format = format;
			this.encoding = encoding;
			this.subject = subject;
			this.body = body;
			this.template = t;
		}
		
		public String getTemplate() {
			return template;
		}
		
		public String getBody() {
			return body;
		}

		public String getEncoding() {
			return encoding;
		}

		public String getFormat() {
			return format;
		}

		public IMsn getMsn() {
			return m;
		}

		public String getSubject() {
			return subject;
		}

		
		public String getMailfrom() {
			return mailfrom;
		}
		public String getMailto() {
			return mailto;
		}
		
		public boolean equals(Object o) {
			if (o !=null && o instanceof MailDataObject) {
				MailDataObject ox = (MailDataObject)o;
				if (ox.m.equals(m))
				return true; 
			}
			return false;
		}
		public int hashCode() {
			return m.hashCode();
		}
	}
	
	private class MailContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object o) {
			List l = (List)o;
			return l.toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {	
		}
		
	}
	
	private class MailLabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		public String getColumnText(Object o, int column) {
			MailDataObject s = (MailDataObject)o;
			
		    switch (column) {
		    case 0:
		      return s.getMsn().getMSN();
		    case 1:
		      return s.getMailto();
		    }
			return null;
		}

		public void addListener(ILabelProviderListener arg0) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		public void removeListener(ILabelProviderListener arg0) {
		}
		
	}
	
	private class MailViewerSorter extends ViewerSorter {
		private static final int ASCENDING = 0;
		private static final int DESCENDING = 1;
	
		private int column;
		private int direction;
	
		public void doSort(int column) {
			if (column == this.column) {
				direction = 1 - direction;
			} else {
				this.column = column;
				direction = ASCENDING;
			}
		}
		
		public int compare(Viewer viewer, Object o1, Object o2) {
			int rc = 0;
			IMsn msn1 = ((MailDataObject)o1).getMsn();
			IMsn msn2 = ((MailDataObject)o2).getMsn();
			
		    switch (column) {
		    case 0:
		      rc = getComparator().compare(msn1.getMSN(), msn2.getMSN());
		      break;
		    case 1:
		      rc = getComparator().compare(msn1.getAdditional(), msn2.getAdditional());
		      break;
		    }
		    
		    if (direction == DESCENDING) rc = -rc;

		    return rc;
		}
	}
	
    private String NAMESPACE = "ui.jface.configuration.pages.MailNotification";
    private String CONFIG_NAMESPACE = "service.MailNotification";
    
	private IRuntime m_runtime;
	private TableViewer tv;
	private Button test;
	private Button active;
	private Button activeout;
	private Button activeacc;
	private Button activemis;
	private Button activerej;
	private Button activepre;
	private Button activeend;
	private Button smtp_auth;
	private Button smtp_ssl;
	private Text server;
	private Text port;
	private Text user;
	private Text pass;
	private Text time;
	private List dataList;
	private List removableData;
	private Combo templates;
	private Text mailfrom;
	private Text mailto;
	
	
	public String getParentNodeID() {
		return IConfigPage.SERVICE_NODE;
	}
	
	public String getNodeID() {
		return "MailNotification".toLowerCase();
	}

	public int getNodePosition() {
		return 25;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) 
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	protected Control createContents(Composite parent) {
		this.setTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
		//this.noDefaultAndApplyButton();
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
	
		String label = this.m_i18n.getString(this.getNamespace(), "enabled", "label", this.m_language);
		if (label.length()<150)
			for (int i=150;i>label.length();i--){
				label += " ";
			}
		
		this.active = new Button(c, SWT.CHECK);
		this.active.setText(
			label
		);
		this.active.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"enabled")
		);
		
		/**  **/
	
		
		Group g = new Group(c, SWT.SHADOW_ETCHED_IN);
		g.setText(this.m_i18n.getString(this.getNamespace(), "events", "label", this.m_language));
		g.setLayout(new GridLayout(2, true));
		GridData gd = new GridData();
        gd.widthHint = 430;
        gd.heightHint = 80;
       	g.setLayoutData(gd);
		
		
		label = this.m_i18n.getString(this.getNamespace(), "pre", "label", this.m_language);
//		if (label.length()<150)
//			for (int i=150;i>label.length();i--){
//				label += " ";
//			}
		
		this.activepre = new Button(g, SWT.CHECK);
		this.activepre.setText(
			label
		);
		this.activepre.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"pre")
		);
		
		/**  **/
		
		label = this.m_i18n.getString(this.getNamespace(), "mis", "label", this.m_language);
//		if (label.length()<150)
//			for (int i=150;i>label.length();i--){
//				label += " ";
//			}
		
		this.activemis = new Button(g, SWT.CHECK);
		this.activemis.setText(
			label
		);
		this.activemis.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"mis")
		);
		
		/**  **/
		
		label = this.m_i18n.getString(this.getNamespace(), "acc", "label", this.m_language);
//		if (label.length()<150)
//			for (int i=150;i>label.length();i--){
//				label += " ";
//			}
		
		this.activeacc = new Button(g, SWT.CHECK);
		this.activeacc.setText(
			label
		);
		this.activeacc.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"acc")
		);
		
		/**  **/
		
		label = this.m_i18n.getString(this.getNamespace(), "end", "label", this.m_language);
//		if (label.length()<150)
//			for (int i=150;i>label.length();i--){
//				label += " ";
//			}
		
		this.activeend = new Button(g, SWT.CHECK);
		this.activeend.setText(
			label
		);
		this.activeend.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"end")
		);
		if (getRuntime().getMonitorListener().getDefaultMonitor()!=null)
			this.activeend.setEnabled(getRuntime().getMonitorListener().getDefaultMonitor().getID().equalsIgnoreCase("FritzBoxMonitor"));
		else
			this.activeend.setEnabled(false);
		
		/**  **/
		
		label = this.m_i18n.getString(this.getNamespace(), "rej", "label", this.m_language);
//		if (label.length()<150)
//			for (int i=150;i>label.length();i--){
//				label += " ";
//			}
		
		this.activerej = new Button(g, SWT.CHECK);
		this.activerej.setText(
			label
		);
		this.activerej.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"rej")
		);
		
		/**  **/
		
		label = this.m_i18n.getString(this.getNamespace(), "outgoing", "label", this.m_language);
//		if (label.length()<150)
//			for (int i=150;i>label.length();i--){
//				label += " ";
//			}
		
		this.activeout = new Button(g, SWT.CHECK);
		this.activeout.setText(
			label
		);
		this.activeout.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"outgoing")
		);
		if (getRuntime().getMonitorListener().getDefaultMonitor()!=null)
			this.activeout.setEnabled(getRuntime().getMonitorListener().getDefaultMonitor().getID().equalsIgnoreCase("FritzBoxMonitor"));
		else
			this.activeout.setEnabled(false);
		

		
		/** end events **/ 
		
		
		Group g2 = new Group(c, SWT.SHADOW_ETCHED_IN);
		g2.setText(this.m_i18n.getString(this.getNamespace(), "server", "label", this.m_language));
		g2.setLayout(new GridLayout(2, true));
		gd = new GridData();
        gd.widthHint = 430;
		gd.heightHint = 225;
		g2.setLayoutData(gd);
		
		new Label(g2, SWT.NONE).setText(
			this.m_i18n.getString(this.getNamespace(), "smtpserver", "label", this.m_language)
		);
		
		new Label(g2, SWT.NONE).setText(
				this.m_i18n.getString(this.getNamespace(), "smtpport", "label", this.m_language)
			);
		
		this.server = new Text(g2, SWT.BORDER);
		this.server.setText(
			this.getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"smtpserver")	
		);
		

		
		this.port = new Text(g2, SWT.BORDER);
		this.port.setText(
			this.getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"smtpport")	
		);
		this.port.setTextLimit(4);
		
		gd = new GridData();
        gd.widthHint = 150;
        server.setLayoutData(gd);
        
		gd = new GridData();
        gd.widthHint = 40;
        port.setLayoutData(gd);
        
        new Label(g2, SWT.NONE).setText(
			this.m_i18n.getString(this.getNamespace(), "smtpuser", "label", this.m_language)
		);
		
        new Label(g2, SWT.NONE).setText(
    			this.m_i18n.getString(this.getNamespace(), "smtppassword", "label", this.m_language)
    		);
        
		this.user = new Text(g2, SWT.BORDER);
		this.user.setText(
			this.getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"smtpuser")	
		);
		
		gd = new GridData();
        gd.widthHint = 150;
        user.setLayoutData(gd);
        

		
		this.pass = new Text(g2, SWT.BORDER);
		this.pass.setEchoChar('*');
		this.pass.setText(
			this.getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"smtppassword")	
		);
		
		
		this.smtp_auth = new Button(g2, SWT.CHECK);
		this.smtp_auth.setText(
			this.m_i18n.getString(this.getNamespace(), "smtpauth", "label", this.m_language)
		);
		this.smtp_auth.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"smtpauth")	
		);
		
		gd = new GridData();
        gd.widthHint = 300;
        gd.horizontalSpan = 2;
        smtp_auth.setLayoutData(gd);
		
        this.smtp_ssl = new Button(g2, SWT.CHECK);
		this.smtp_ssl.setText(
			this.m_i18n.getString(this.getNamespace(), "smtpssl", "label", this.m_language)
		);
		this.smtp_ssl.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"smtpssl")	
		);
		
		gd = new GridData();
        gd.widthHint = 300;
        gd.horizontalSpan = 2;
        smtp_ssl.setLayoutData(gd);
        
        new Label(g2, SWT.NONE).setText(
			this.m_i18n.getString(this.getNamespace(), "smtpqueuetime", "label", this.m_language)
		);
        new Label(g2, SWT.NONE);
        
		this.time = new Text(g2, SWT.BORDER);
		this.time.setText(
			this.getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"smtpqueuetime")	
		);
		this.time.setTextLimit(3);
		
		gd = new GridData();
        gd.widthHint = 30;
        time.setLayoutData(gd);
        new Label(g2, SWT.NONE);
        
		this.test = new Button(g2, SWT.PUSH);
		this.test.setText(this.m_i18n.getString(this.getNamespace(), "test", "label", this.m_language));
		this.test.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {	
						IService svc = getRuntime().getServiceFactory().getService("MailNotification");
						if (svc!=null && svc instanceof de.janrufmonitor.service.notification.MailNotification) {
							performOk();
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e1) {
							}
							((de.janrufmonitor.service.notification.MailNotification)svc).sendTestMail();
						}
					}
				}
		);
		
		
		gd = new GridData();
        gd.widthHint = 182;
        pass.setLayoutData(gd);
        
        /** end server **/
        

        
        
		Group g3 = new Group(c, SWT.SHADOW_ETCHED_IN);
		g3.setText(this.m_i18n.getString(this.getNamespace(), "templates", "label", this.m_language));
		g3.setLayout(new GridLayout(2, true));
		gd = new GridData();
        gd.widthHint = 430;
		gd.heightHint = 100;
		g3.setLayoutData(gd);
		
		new Label(g3, SWT.LEFT).setText(this.m_i18n.getString(this.getNamespace(), "default_mailfrom", "label", this.m_language));
		
		new Label(g3, SWT.LEFT).setText(this.m_i18n.getString(this.getNamespace(), "default_mailto", "label", this.m_language));
		
		this.mailfrom = new Text(g3, SWT.BORDER);
		this.mailfrom.setText(
			this.getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"default_mailfrom")	
		);
		
		gd = new GridData();
        gd.widthHint = 150;
        mailfrom.setLayoutData(gd);
		

		this.mailto = new Text(g3, SWT.BORDER);
		this.mailto.setText(
			this.getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"default_mailto")	
		);
		
		gd = new GridData();
        gd.widthHint = 150;
        mailto.setLayoutData(gd);
        
        
		new Label(g3, SWT.LEFT).setText(this.m_i18n.getString(this.getNamespace(), "default_mailtemplate", "label", this.m_language));
		new Label(g3, SWT.LEFT);
	    templates = new Combo (g3, SWT.READ_ONLY);
	    gd = new GridData();
        gd.widthHint = 150;
        templates.setLayoutData(gd);
        //templates.setEnabled(getPreferenceStore().getString("default_mailtemplate")!=null);
        
		String[] templatess = getTemplates();

		int select = 0;
		for (int i=0;i<templatess.length;i++) {
			if (templatess[i].equalsIgnoreCase(getPreferenceStore().getString(this.getConfigNamespace()+SEPARATOR+"default_mailtemplate"))) {
				select=i;	
			}
		}
		templates.setItems(templatess);
		templates.select(select);

		new Label(g3, SWT.LEFT);
		/** end templates **/
		
		
		
		
		
		
        new Label(c, SWT.NONE).setText(
			this.m_i18n.getString(this.getNamespace(), "mail", "label", this.m_language)
		);
        
		tv = new TableViewer(c);
		tv.setSorter(new MailViewerSorter());
		tv.setContentProvider(new MailContentProvider());
		tv.setLabelProvider(new MailLabelProvider());
		
		Table t = tv.getTable();
		t.setLayoutData(new GridData(GridData.FILL_BOTH));
		TableColumn tc = new TableColumn(t, SWT.LEFT);
		tc.setText(this.m_i18n.getString(this.getNamespace(), "msn", "label", this.m_language));
		tc.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent event) {
	        ((MailViewerSorter) tv.getSorter())
	            .doSort(0);
	        tv.refresh();
	      }
	    });
		
		tc = new TableColumn(t, SWT.LEFT);
		tc.setText(this.m_i18n.getString(this.getNamespace(), "alias", "label", this.m_language));
		tc.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent event) {
	        ((MailViewerSorter) tv.getSorter())
	            .doSort(1);
	        tv.refresh();
	      }
	    });
		
		tv.setInput(this.getDataList());
		
	    for (int i = 0, n = t.getColumnCount(); i < n; i++) {
	      t.getColumn(i).pack();
	    }

	    t.setHeaderVisible(true);
	    t.setLinesVisible(true);	
	    this.createPopupMenu();
	    
	    new Label(c, SWT.NONE);
	    
		return c;
	}
	
	private void createPopupMenu() {
		final Table t = tv.getTable();
		
		Menu popUpMenu = new Menu (t.getShell(), SWT.POP_UP);
		
		MenuItem item = new MenuItem (popUpMenu, SWT.PUSH);
		item.setText (this.m_i18n.getString(this.getNamespace(), "add", "label", this.m_language));
		item.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					openDataWizard(null);
				}
			}
		);		
		item = new MenuItem (popUpMenu, SWT.PUSH);
		item.setText (this.m_i18n.getString(this.getNamespace(), "remove", "label", this.m_language));
		item.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (t.getSelectionCount()>0) {
						TableItem i = t.getSelection()[0];
						removeMailDataObject(getFromList(i.getText(0)));
					}
				}
			}
		);		
		item = new MenuItem (popUpMenu, SWT.PUSH);
		item.setText (this.m_i18n.getString(this.getNamespace(), "edit", "label", this.m_language));
		item.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (t.getSelectionCount()>0) {
						TableItem i = t.getSelection()[0];
						openDataWizard(getFromList(i.getText(0)));
					}
				}
			}
		);		
		t.setMenu(popUpMenu);
		
		t.addMouseListener(
			new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent e) {
					if ((e.widget instanceof Table)) {
						if (t.getSelectionCount()>0) {
							if (t.getSelectionCount()>0) {
								TableItem i = ((Table) e.widget).getSelection()[0];
								openDataWizard(getFromList(i.getText(0)));
							}
						}
						if (t.getSelectionCount()==0) {
							openDataWizard(null);
						}						
					}
				}
			}
		);
		
		t.addKeyListener(
			new KeyAdapter() {
				public void keyPressed(KeyEvent e){
					if (e.character == SWT.DEL) {
						if (t.getSelectionCount()>0) {
							TableItem i = t.getSelection()[0];
							removeMailDataObject(getFromList(i.getText(0)));
						}
					}
				}	
			}		
		);

	}
	
	private MailDataObject getFromList(String msn) {
		IMsn oldMsn = this.getRuntime().getMsnManager().createMsn(msn);
		MailDataObject md = null;
		for (int i=0;i<this.dataList.size();i++) {
			md = (MailDataObject)this.dataList.get(i);
			if (md.getMsn().equals(oldMsn))
				return md;
		}
		return null;
	}
	
	private void removeMailDataObject(MailDataObject old) {
		if (old!=null && !old.getMsn().getMSN().equalsIgnoreCase("default")) {
			this.dataList.remove(old);
			this.removableData.add(old);
			tv.setInput(this.dataList);
		}
	}
	
	private void openDataWizard(MailDataObject md) {
	    Display display = DisplayManager.getDefaultDisplay();
		Shell shell = new Shell(display);

	    // Create the dialog
	    WizardDialog.setDefaultImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
	    MailDataWizard sdoW = new MailDataWizard(md);
	    WizardDialog dlg = new WizardDialog(shell, sdoW);
	    dlg.open();
	    if (dlg.getReturnCode() == WizardDialog.OK) {
	    	MailDataObject result = sdoW.getResult();
	    	this.dataList.remove(md);
	    	this.dataList.add(result);
	    }
	    tv.setInput(this.dataList);
	}
	
	private List getDataList() {
		if (this.dataList==null || this.dataList.size()==0) {
			this.dataList = new ArrayList();
			this.removableData = new ArrayList();
			String msnList = this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + "list");
			if (msnList.trim().length()>0) {
				StringTokenizer st = new StringTokenizer(msnList, ",");
				while (st.hasMoreTokens()) {
					String number = st.nextToken().trim();
					if (number.length()>0) {
						IMsn msn = this.getRuntime().getMsnManager().createMsn(number);
						MailDataObject mdo = new MailDataObject(
							msn,
							this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + number+ "_mailto"),
							this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + number+ "_mailfrom"),
							this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + number+ "_mimetype"),
							this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + number+ "_encoding"),
							this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + number+ "_mailsubject"),
							this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + number+ "_mailcontent"),
							this.getPreferenceStore().getString(this.CONFIG_NAMESPACE + SEPARATOR + number+ "_mailtemplate")
						);
						if (!this.dataList.contains(mdo))
							this.dataList.add(mdo);
					}
				}
			}
		}
		return this.dataList;
	}
	
	private String[] getTemplates() {
		String[] templates = null;
		File templateDir = new File(PathResolver.getInstance(getRuntime()).getConfigDirectory(), "templates");
		if (!templateDir.exists()) templateDir.mkdirs();
		File[] templateList = templateDir.listFiles(new FilenameFilter() {
			public boolean accept(File parent, String name) {
				return name.toLowerCase().endsWith("template");
			}});
		
		templates = new String[templateList.length+1];
		templates[0] = "";
		for (int i=0;i<templateList.length;i++) {
			templates[i+1] = templateList[i].getName().substring(0, templateList[i].getName().lastIndexOf("."));
		}
		return templates;
	}
	
	public String getConfigNamespace() {
		return this.CONFIG_NAMESPACE;
	}
	
	public boolean performOk() {
		this.performApply();
		
		this.dataList.clear();
		this.removableData.clear();
		
		return super.performOk();
	}
	
	protected void performApply() {
		for (int i=0;i<this.removableData.size();i++) {
			MailDataObject m = (MailDataObject)this.removableData.get(i);
			this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(this.CONFIG_NAMESPACE, m.getMsn().getMSN()+"_mailto");
			this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(this.CONFIG_NAMESPACE, m.getMsn().getMSN()+"_mailfrom");
			this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(this.CONFIG_NAMESPACE, m.getMsn().getMSN()+"_mimetype");
			this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(this.CONFIG_NAMESPACE, m.getMsn().getMSN()+"_encoding");
			this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(this.CONFIG_NAMESPACE, m.getMsn().getMSN()+"_mailsubject");
			this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(this.CONFIG_NAMESPACE, m.getMsn().getMSN()+"_mailcontent");
			this.getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(this.CONFIG_NAMESPACE, m.getMsn().getMSN()+"_mailtemplate");
		}
		
		StringBuffer list = new StringBuffer();
		for (int i=0;i<this.dataList.size();i++) {
			MailDataObject m = (MailDataObject)this.dataList.get(i);
			this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE+SEPARATOR+m.getMsn().getMSN()+"_mailto", m.getMailto());
			this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE+SEPARATOR+m.getMsn().getMSN()+"_mailfrom", m.getMailfrom());
			this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE+SEPARATOR+m.getMsn().getMSN()+"_mimetype", m.getFormat());
			this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE+SEPARATOR+m.getMsn().getMSN()+"_encoding", m.getEncoding());
			this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE+SEPARATOR+m.getMsn().getMSN()+"_mailsubject", m.getSubject());
			this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE+SEPARATOR+m.getMsn().getMSN()+"_mailcontent", m.getBody());
			if (m.getTemplate()!=null)
				this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE+SEPARATOR+m.getMsn().getMSN()+"_mailtemplate", m.getTemplate());
			list.append(m.getMsn().getMSN());
			list.append(",");
		}
		this.getPreferenceStore().setValue(this.CONFIG_NAMESPACE + SEPARATOR + "list", list.toString());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"enabled", active.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"outgoing", activeout.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"pre", activepre.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"mis", activemis.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"acc", activeacc.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"rej", activerej.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"end", activeend.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"smtpauth", smtp_auth.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"smtpssl", smtp_ssl.getSelection());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"smtpserver", server.getText());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"smtpport", port.getText());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"smtpuser", user.getText());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"smtppassword", pass.getText());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"smtpqueuetime", time.getText());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"default_mailto", mailto.getText());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"default_mailfrom", mailfrom.getText());
		this.getPreferenceStore().setValue(this.getConfigNamespace()+SEPARATOR+"default_mailtemplate", templates.getText());
	}	
}
