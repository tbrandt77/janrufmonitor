package de.janrufmonitor.runtime.launcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.PIMRuntime;

public class LauncherFactory implements IConfigurable {

	private String ID = "LauncherFactory";
	private String PARAMETERNAME = "launcher_";
	private String NAMESPACE = "runtime.LauncherFactory";

	private static LauncherFactory m_instance = null;

	Logger m_logger;
	List m_launchers;
	
	private LauncherFactory() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);        
	}
    
	public static synchronized LauncherFactory getInstance() {
		if (LauncherFactory.m_instance == null) {
			LauncherFactory.m_instance = new LauncherFactory();
		}
		return LauncherFactory.m_instance;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public String getConfigurableID() {
		return this.ID;
	}

	public void setConfiguration(Properties configuration) {
		this.m_launchers = new ArrayList();
		
		Iterator iter = configuration.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			if (key.startsWith(PARAMETERNAME)) {
				String className = configuration.getProperty(key);
				try {
					Class classObject = Thread.currentThread().getContextClassLoader().loadClass(className);
					ILauncher l = (ILauncher) classObject.newInstance();
					this.m_logger.info("adding new launcher "+l.getID());
					this.m_launchers.add(l);
				} catch (ClassNotFoundException ex) {
					this.m_logger.severe("Could not find class: " + className);
				} catch (InstantiationException ex) {
					this.m_logger.severe("Could not instantiate class: " + className);
				} catch (IllegalAccessException ex) {
					this.m_logger.severe("Could not access class: " + className);
				}
			}
		}
	} 
	
	public ILauncher getLauncher(String id) {
		for (int i=0;i<this.m_launchers.size();i++) {
			ILauncher l = (ILauncher)this.m_launchers.get(i);
			if (l.getID().equalsIgnoreCase(id)) {
				return l;
			}
		}
		this.m_logger.warning("Launcher with ID "+id+" not found.");
		return null;
	}
	
	public String[] getAllLauncherIDs(){
		String[] ids = new String[this.m_launchers.size()];
		for (int i=0;i<this.m_launchers.size();i++) {
			ILauncher l = (ILauncher)this.m_launchers.get(i);
			ids[i] = l.getID();
		}
		return ids;
	}

}
