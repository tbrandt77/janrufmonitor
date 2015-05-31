package de.janrufmonitor.ui.jface.configuration.pages;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;

import de.janrufmonitor.framework.installer.InstallerConst;
import de.janrufmonitor.framework.installer.InstallerEngine;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.controls.HyperLink;
import de.janrufmonitor.ui.jface.application.dnd.IDropTargetHandler;
import de.janrufmonitor.ui.jface.configuration.AbstractConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class ModuleManager extends AbstractConfigPage {
	
	private class ModuleManagerDropTargetHandler implements IDropTargetHandler {
		public void execute(String[] sources) {
			boolean installed = false;
			File currentModule = null;
			for (int i=0,j=sources.length;i<j;i++) {
				currentModule = new File(sources[i]);
				m_logger.info("Installing module file " + currentModule);
            
				// only restart, if the last file was installed...
                if (InstallerEngine.getInstance().install(currentModule, (i==j-1), true)) {
                	installed = true;					
				} else {
					String f = m_i18n.getString(getNamespace(), "failmessage", "label", m_language);
					f = StringUtils.replaceString(f, "{%1}", currentModule.getName());
					MessageDialog.openError(
						DisplayManager.getDefaultDisplay().getActiveShell(),
						m_i18n.getString(getNamespace(), "failtitle", "label", m_language),
						f
					);
					installed = false;
				}
			}
			if (installed) {
				MessageDialog.openInformation(
						DisplayManager.getDefaultDisplay().getActiveShell(),
						m_i18n.getString(getNamespace(), "oktitle", "label", m_language),
						m_i18n.getString(getNamespace(), "okmessage", "label", m_language)
					);	
			}
		}
	}
	
	private class ModuleContainer {
		private String m_path;
		private String[] m_files;
		
		public void setDirectory(String d) {
			this.m_path = d;
		}
		
		public void setFilenames(String[] n) {
			this.m_files = n;
		}
		
		public String getDirectory() {
			return this.m_path;
		}
		
		public String[] getFilenames(){
			return this.m_files;
		}
	}
	
    private String NAMESPACE = "ui.jface.configuration.pages.ModuleManager";
    
	private IRuntime m_runtime;
	private IDropTargetHandler dth;
	
	public String getParentNodeID() {
		return IConfigPage.ROOT_NODE;
	}
	
	public String getNodeID() {
		return "ModuleManager".toLowerCase();
	}

	public int getNodePosition() {
		return 50;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) 
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getConfigNamespace() {
		return "";
	}
	
	protected IDropTargetHandler getDropTargetHandler(){
		if (dth==null)
			dth = new ModuleManagerDropTargetHandler();
		return dth;
	}
	
	
	protected Control createContents(Composite parent) {
		this.setTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
		this.noDefaultAndApplyButton();
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(2, false));

		int operations = DND.DROP_MOVE;
		Transfer[] types = new Transfer[] {FileTransfer.getInstance()};
		DropTarget target = new DropTarget(c, operations);
		target.setTransfer(types);

		target.addDropListener (new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {
				// A drop has occurred, copy over the data
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}
				getDropTargetHandler().execute((String[]) event.data);
			}

	 	});
		
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 1;
        gd.widthHint = 200;
               
		Label l = new Label(c, SWT.LEFT);
		l.setText(this.m_i18n.getString(this.getNamespace(), "file", "label", this.m_language));
		l.pack();
		
		l = new Label(c, SWT.LEFT);
				
		final org.eclipse.swt.widgets.List m_fileList = new org.eclipse.swt.widgets.List(c, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		m_fileList.setLayoutData(gd);
		
	    Button b = new Button(c, SWT.PUSH);
		b.setText(JFaceResources.getString("openBrowse"));

		final Button install = new Button(c, SWT.PUSH);
		install.setText(this.m_i18n.getString(this.getNamespace(), "install", "label", this.m_language));
		install.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					ModuleContainer mc = (ModuleContainer) m_fileList.getData("modules");
					if (mc!=null) {
						String[] sources = new String[mc.getFilenames().length];
						
						for (int i=0,j=mc.getFilenames().length;i<j;i++) {
							sources[i] = mc.getDirectory() + File.separator + mc.getFilenames()[i];
						}
						getDropTargetHandler().execute(sources);
					}
					
					install.setEnabled(false);	
					m_fileList.removeAll();
					m_fileList.setData("modules", null);
					
					
				}
			}
		);
		install.setEnabled(false);
		
		b.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
            	ModuleContainer mc = changePressed(m_fileList.getParent());
            	if (mc!=null) {
            		m_fileList.removeAll();
					m_fileList.setData("modules", null);
					
	            	m_fileList.setData("modules", mc);
	            	String[] mods = mc.getFilenames();
	            	
	            	for (int i=0;i<mods.length;i++)
	            		m_fileList.add(mods[i]);
	            	
	                if (mods.length>0) {
	                	install.setEnabled(true);
	                }
            	}
            }
        });
		l = new Label(c, SWT.LEFT);
		
		l = new Label(c, SWT.LEFT);
		l = new Label(c, SWT.LEFT);
	
		l = new Label(c, SWT.LEFT);
		l.setText(this.m_i18n.getString(this.getNamespace(), "remove", "label", this.m_language));
		
		l = new Label(c, SWT.LEFT);
		
		final Combo module = new Combo (c, SWT.READ_ONLY);
		gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 1;
        gd.widthHint = 200;
        module.setLayoutData(gd);
        
		this.buildCombobox(module);
		
		l = new Label(c, SWT.LEFT);
		
		if (module.getItemCount()>0 && !OSUtils.isMultiuserEnabled()) {
			final Group info = new Group(c, SWT.SHADOW_ETCHED_IN);
			info.setLayout(new GridLayout(2, false));
			
			gd = new GridData();
	        gd.horizontalAlignment = GridData.FILL;
	        gd.grabExcessHorizontalSpace = true;
	        gd.horizontalSpan = 2;
	        info.setLayoutData(gd);
	        info.setText(this.m_i18n.getString(this.getNamespace(), "infotitle", "label", this.m_language));
	        info.setVisible(false);
	
			l = new Label(info, SWT.LEFT);
			l.setText(this.m_i18n.getString(this.getNamespace(), "modname", "label", this.m_language));
			
			final Label name = new Label(info, SWT.LEFT);
	
			l = new Label(info, SWT.LEFT);
			l.setText(this.m_i18n.getString(this.getNamespace(), "modversion", "label", this.m_language));
			
			final Label version = new Label(info, SWT.LEFT);
	
			l = new Label(info, SWT.LEFT);
			l.setText(this.m_i18n.getString(this.getNamespace(), "modtype", "label", this.m_language));
			
			final Label type = new Label(info, SWT.LEFT);
			
			l = new Label(info, SWT.LEFT);
			l.setText(this.m_i18n.getString(this.getNamespace(), "modauthor", "label", this.m_language));
			
			final Label author = new Label(info, SWT.LEFT);
	
			l = new Label(info, SWT.LEFT);
			l.setText(this.m_i18n.getString(this.getNamespace(), "modwebsite", "label", this.m_language));
			
			final HyperLink website = new HyperLink(info, SWT.NONE);
			
			website.setEnabled(false);
			website.addMouseListener( 
				new MouseAdapter() {
					public void mouseDown(MouseEvent e) {
						if (e.button==1)
						Program.launch(website.getText());
					}
				}
			);
			
	        String ns = (String) module.getData(module.getText());
	        if (ns!=null && ns.length()>0) {
	        	info.setVisible(true);
	            Properties currentDescriptor = InstallerEngine.getInstance().getDescriptor(ns);
				if (currentDescriptor!=null) {
					name.setText(this.getNamespaceLabel(ns));
					version.setText(currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_VERSION, "-"));
					author.setText(currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_AUTHOR, "-"));
					type.setText(
						m_i18n.getString(getNamespace(), currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_TYPE, "-"), "label", m_language)
					);
					website.setText(currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_WEBSITE, "-"));
					if (website.getText().length()>6 && website.getText().toLowerCase().startsWith("http")) {
						website.setEnabled(true);
					}
				}
	        }
	        
	        module.addSelectionListener(new SelectionAdapter() {
	        	public void widgetSelected(SelectionEvent e) {
	        	      String ns = (String) module.getData(module.getText());
	        	        if (ns!=null && ns.length()>0) {
	        	        	info.setVisible(true);
	        	            Properties currentDescriptor = InstallerEngine.getInstance().getDescriptor(ns);
	        				if (currentDescriptor!=null) {
	        					name.setText(getNamespaceLabel(ns));
	        					name.pack();
	        					version.setText(currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_VERSION, "-"));
	        					version.pack();
	        					author.setText(currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_AUTHOR, "-"));
	        					author.pack();
	        					type.setText(
	    							m_i18n.getString(getNamespace(), currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_TYPE, "-"), "label", m_language)
	    						);   
	        					type.pack();
	        					website.setText(currentDescriptor.getProperty(InstallerConst.DESCRIPTOR_WEBSITE, "-"));
	        					if (website.getText().length()>6 && website.getText().toLowerCase().startsWith("http")) {
	        						website.setEnabled(true);
	        					}
	        					website.pack();
	        				}
	        	        }
	        	}
	        });
		}
		
		final Button uninstall = new Button(c, SWT.PUSH | SWT.LEFT);
		uninstall.setText(this.m_i18n.getString(this.getNamespace(), "uninstall", "label", this.m_language));
	
		if (module.getItemCount()==0 || OSUtils.isMultiuserEnabled()) {
			uninstall.setEnabled(false);
		}
		uninstall.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					String ns = (String) module.getData(module.getText());
					MessageBox messageBox = new MessageBox (uninstall.getShell(), SWT.APPLICATION_MODAL | SWT.YES | SWT.NO);
					messageBox.setMessage (m_i18n.getString(getNamespace(), "confirmdelete", "label", m_language));
					if (messageBox.open () == SWT.YES) {
	            		if (InstallerEngine.getInstance().uninstall(ns)){
	          	       	    m_logger.info( "Removed <" + ns + "> module successfully.");	        	    		
	            			if (module.getItemCount()==0) {
    	        				uninstall.setEnabled(false);
        	    			}
	            			MessageDialog.openInformation(
    							install.getShell(),
    							m_i18n.getString(getNamespace(), "okrtitle", "label", m_language),
    							m_i18n.getString(getNamespace(), "okrmessage", "label", m_language)
    						);						
    					} else {
    						MessageDialog.openError(
    							install.getShell(),
    							m_i18n.getString(getNamespace(), "failrtitle", "label", m_language),
    							m_i18n.getString(getNamespace(), "failrmessage", "label", m_language)
    						);
    					}
	            		buildCombobox(module);
					}
				}
			}
		);	
		
		l = new Label(c, SWT.LEFT);
       
		return c;
	}

	private String getNamespaceLabel(String ns) {
		return this.m_i18n.getString(ns,"title","label",this.m_language);
	}
	
	private void buildCombobox(Combo combo) {

		List allowedNS = InstallerEngine.getInstance().getModuleList();
		
		String[] namespaceList = new String[allowedNS.size()];
		for(int i=0;i<allowedNS.size();i++) {
			String allowedNs = (String) allowedNS.get(i);
			namespaceList[i] = this.getNamespaceLabel(allowedNs);
			this.m_logger.info("Namespace <"+allowedNs+">, Label <"+namespaceList[i]+">");
			combo.setData(namespaceList[i], allowedNs);
		}
		
		combo.setItems(namespaceList);
		combo.select(0);
	}
	
	private ModuleContainer changePressed(Composite parent) {
		FileDialog dlg = new FileDialog(parent.getShell(), SWT.OPEN|SWT.MULTI);
		dlg.setFilterExtensions(new String[]{"*.jam.zip"});
		String path = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), "last");
		if (path==null || path.length()==0) {
			path = PathResolver.getInstance(getRuntime()).getUserhomeDirectory();
		}
		path = PathResolver.getInstance(getRuntime()).resolve(path);

		dlg.setFilterPath(path);
		if (dlg.open()!=null) {
			this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(this.getNamespace(), "last", dlg.getFilterPath());
			
			ModuleContainer m = new ModuleContainer();
			m.setDirectory(dlg.getFilterPath());
			m.setFilenames(dlg.getFileNames());
			
			return m;
		}
		return null;
	}	

}
