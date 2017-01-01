package de.janrufmonitor.ui.jface.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.AbstractConfigurableCommand;
import de.janrufmonitor.framework.configuration.IConfigManager;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class ConfigurationCommand extends AbstractConfigurableCommand implements Comparator {

    private String ID = "ConfigurationCommand";
    private String NAMESPACE = "ui.jface.configuration.ConfigurationCommand";
    private String PARAMETERNAME = "page_";
    
    private IRuntime m_runtime;
    private boolean isExecuting;
    private List m_pages;

	public ConfigurationCommand() {
		super();
		getRuntime().getConfigurableNotifier().register(this);
	}
    
	private void asyncExecute() {
		this.m_pages = new ArrayList();
		
		try {
			Display display = DisplayManager.getDefaultDisplay();
			
			// create a PreferenceManager
			PreferenceManager mgr = new PreferenceManager();
			
			// assign nodes to PreferenceManager
			this.createPreferenceNodes(mgr);
			
			// create a PreferenceDialog
			Shell s = new Shell(display);
			PreferenceDialog.setDefaultImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
			PreferenceDialog dlg = new PreferenceDialog(s, mgr);
			dlg.setPreferenceStore(new PreferenceConfigManagerStore());
			DisplayManager.forceForeground(s);
			dlg.open();
		} catch (Throwable t) {
			t.printStackTrace();
			this.m_logger.log(Level.SEVERE, t.getMessage(), t);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, this.getNamespace(), t.toString().toLowerCase(), t));
		} finally {
			this.isExecuting = false;
			this.m_pages.clear();
		}
	}
	
	public void execute() {
		this.isExecuting = true;
		Thread thread = new Thread () {
			public void run () {
				DisplayManager.getDefaultDisplay().asyncExec(
					new Runnable() {
						public void run() {
							asyncExecute();
						}
					}
				);
			}
		};
		thread.setName(this.getID());
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			this.isExecuting = false;
		}		
	}

	private void createPreferenceNodes(PreferenceManager mgr) {
		this.buildPageList();
		
		List rootNodes = this.getRootPages();
		IConfigPage page = null;
		for (int i=0;i<rootNodes.size();i++) {
			page = (IConfigPage)rootNodes.get(i);
			if (page!=null && page instanceof IPreferencePage) {
				IPreferenceNode n = new PreferenceNode(page.getNodeID(), this.m_i18n.getString(page.getNamespace(), "title", "label", this.m_language), null, page.getClass().getName());
				mgr.addToRoot(n);
			}
		}
		
		for (int i=0;i<this.m_pages.size();i++) {
			page = (IConfigPage)this.m_pages.get(i);
			if (page!=null && page instanceof IPreferencePage) {
				IPreferenceNode parent = mgr.find(page.getParentNodeID());
				if (parent!=null) {
					IPreferenceNode n = new PreferenceNode(page.getNodeID(), this.m_i18n.getString(page.getNamespace(), "title", "label", this.m_language), null, page.getClass().getName());
					parent.add(n);
				}
			}
		}

	}
	
	private void buildPageList() {
        Iterator iter = m_configuration.keySet().iterator();
        IConfigManager cfgm = getRuntime().getConfigManagerFactory().getConfigManager();
        
        boolean isExpertMode = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_CONFIG_EXPERT_MODE).equalsIgnoreCase("true");
        
        String key = null;
        String mode = null;
        while (iter.hasNext()) {
            key = (String) iter.next();            
            if (key.startsWith(PARAMETERNAME)) {
            	mode = cfgm.getProperty(getNamespace(), key, "mode");
            	if (!isExpertMode){       
            		if (mode.equalsIgnoreCase("expert")) continue;
            	}
            	
            	if (mode.equalsIgnoreCase("hidden")) continue;
            		
                String className = m_configuration.getProperty(key);
                try {
                	Class classObject = Thread.currentThread().getContextClassLoader().loadClass(className);
                    IConfigPage page = (IConfigPage) classObject.newInstance();
                    page.setNodeID(key.substring(PARAMETERNAME.length()));
                    this.m_pages.add(page);
                } catch (ClassNotFoundException ex) {
                    this.m_logger.severe("Could not find class: " + className);
                } catch (InstantiationException ex) {
                    this.m_logger.severe("Could not instantiate class: " + className);
                } catch (IllegalAccessException ex) {
                    this.m_logger.severe("Could not access class: " + className);
                } catch (NullPointerException ex) {
					this.m_logger.severe(ex.getMessage());
                } catch (Exception e) {
                	this.m_logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
	    // sort for position
        Collections.sort(this.m_pages, this);
	}
	
	private List getRootPages() {
		List l = new ArrayList();
		IConfigPage p = null;
		for (int i=this.m_pages.size()-1;i>=0;i--) {
			p = (IConfigPage)this.m_pages.get(i);
			if (p.getParentNodeID().equalsIgnoreCase(IConfigPage.ROOT_NODE)) {
				l.add(p);
				this.m_pages.remove(p);
			}
		}
		Collections.sort(l, this);
		return l;
	}

	public boolean isExecutable() {
		return true;
	}

	public boolean isExecuting() {
		return this.isExecuting;
	}

	public String getID() {
		return this.ID;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getLabel() {
		return this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language);
	}
	
	public int compare(Object obj, Object obj1) {
		IConfigPage icp1 = (IConfigPage) obj;
		IConfigPage icp2 = (IConfigPage) obj1;

        if (icp1.getNodePosition() < icp2.getNodePosition()) {
            return -1;
        }

        if (icp1.getNodePosition() > icp2.getNodePosition()) {
            return 1;
        }

		return 0;
	}
}
