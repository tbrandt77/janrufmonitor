package de.janrufmonitor.application.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.runtime.launcher.ILauncher;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class Win32Launcher implements ILauncher, IConfigurable {

	private String ID = "Win32Launcher";
	private String NAMESPACE = "application.Win32Launcher";

	Logger m_logger;
	Properties m_configuration;
	
	private String CFG_FILENAME ="filename";
	private String CFG_SEPARATOR ="separator";
	
	public Win32Launcher() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
	}

	public void addLibrary(String lib) {
		try {
			this.m_logger.info("Adding library "+lib);
			List cfg = this.getFileContent();
			
			Map configuration = this.getConfigMap(cfg);
			
			String option = (String)configuration.get("options");
			if (option!=null) {
				Map options = this.getOptionsMap(option);
				
				String classpath = (String)options.remove("-classpath ");
				if (classpath==null)
					classpath = (String)options.remove("-cp ");
				
				if (classpath!=null) {
					StringTokenizer st = new StringTokenizer(classpath, this.getSeparator());
					this.m_logger.info("Classpath before adding library "+classpath);
					classpath = "";
					String aLib = null;
					while(st.hasMoreTokens()) {
						aLib = st.nextToken();
						if (!aLib.equalsIgnoreCase(lib))
							classpath += aLib + this.getSeparator();
					}

					classpath += lib;
					this.m_logger.info("Classpath after adding library "+classpath);
					options.put("-classpath ", classpath);
				}
				
				option = this.setOptionsMap(options);
				configuration.put("options", option);
			} else {
				this.m_logger.severe("options= is not available in file "+this.getFileName()+".");
			}
			
			cfg = this.setConfigMap(configuration);
				
			this.setFileContent(cfg);
		} catch (IOException e) {
			this.m_logger.warning(e.toString()+" - "+e.getMessage());
		}
	}


	private List setConfigMap(Map configuration) {
		List l = new ArrayList();
		Iterator it = configuration.keySet().iterator();
		while (it.hasNext()) {
			String key = (String)it.next();
			l.add(key+"="+(String)configuration.get(key));
		}
		return l;
	}

	public void removeLibrary(String lib) {
		try {
			this.m_logger.info("Removing library "+lib);
			List cfg = this.getFileContent();
			
			Map configuration = this.getConfigMap(cfg);
			
			String option = (String)configuration.get("options");
			if (option!=null) {
				Map options = this.getOptionsMap(option);
				
				String classpath = (String)options.remove("-classpath ");
				if (classpath==null)
					classpath = (String)options.remove("-cp ");
				
				if (classpath!=null) {
					StringTokenizer st = new StringTokenizer(classpath, this.getSeparator());
					this.m_logger.info("Classpath before removing library "+classpath);
					classpath = "";
					while(st.hasMoreTokens()) {
						String aLib = st.nextToken();
						if (!aLib.equalsIgnoreCase(lib))
							classpath += aLib + this.getSeparator();
					}
					this.m_logger.info("Classpath after removing library "+classpath);
					options.put("-classpath ", classpath.substring(0, classpath.length()-1));
				}
				
				option = this.setOptionsMap(options);
				configuration.put("options", option);
			} else {
				this.m_logger.severe("options= is not available in file "+this.getFileName()+".");
			}
			
			cfg = this.setConfigMap(configuration);
				
			this.setFileContent(cfg);
		} catch (IOException e) {
			this.m_logger.warning(e.toString()+" - "+e.getMessage());
		}
	}

	public String getID() {
		return this.ID;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public String getConfigurableID() {
		return this.getID();
	}

	public void setConfiguration(Properties configuration) {
		this.m_configuration = configuration;
	}
	
	private String getFileName() {
		return (this.m_configuration.getProperty(this.CFG_FILENAME, "loader.ini"));
	}
	
	private String getSeparator() {
		return (this.m_configuration.getProperty(this.CFG_SEPARATOR, ";"));
	}
	
	private List getFileContent() throws IOException {
		File configFile = new File(PathResolver.getInstance(PIMRuntime.getInstance()).getAppRoot() + File.separator + this.getFileName());
		FileReader configReader = new FileReader(configFile);
		BufferedReader bufReader = new BufferedReader(configReader);
		List cfg = new ArrayList();
		while (bufReader.ready()) {
			String line = bufReader.readLine();
			cfg.add(line);
		}
		bufReader.close();
		configReader.close();
		return cfg;
	}
	
	private Map getConfigMap(List cfg) {
		Map m = new HashMap();
		
		for (int i=0;i<cfg.size();i++) {
			String line = (String)cfg.get(i);
			if (line.indexOf("=")>-1) {
				int p = line.indexOf("=");
				String key = line.substring(0, p);
				String value = line.substring(p+1);
				m.put(key, value); 
			}
		}
		
		return m;
	}
	
	private Map getOptionsMap(String options) {
		options = StringUtils.replaceString(options, " -", "$");
		StringTokenizer st = new StringTokenizer(options, "$");
		Map m = new HashMap();
		while (st.hasMoreTokens()) {
			String line = st.nextToken();
			if (line.indexOf(" ")>-1) {
				int p = line.indexOf(" ");
				String key = "-"+line.substring(0, p+1);
				String value = line.substring(p+1);
				m.put(key, value);
			}
			if (line.toLowerCase().startsWith("d") && line.indexOf("=")>-1) {
				int p = line.indexOf("=");
				String key = "-"+line.substring(0, p+1);
				String value = line.substring(p+1);
				m.put(key, value);
			}
			if (line.toLowerCase().startsWith("x")) {
				m.put("-"+line, "");
			}			
		}
		return m;
	}
	
	private String setOptionsMap(Map m) {
		String options = "";
		Iterator it = m.keySet().iterator();
		while (it.hasNext()) {
			String key = (String)it.next();
			options += " "+key+(String)m.get(key);
		}
		return options; 
	}
	
	private void setFileContent(List cfg) throws IOException {
		File configFile = new File(PathResolver.getInstance(PIMRuntime.getInstance()).getAppRoot() + File.separator + this.getFileName());
		FileWriter configWriter = new FileWriter(configFile);
		BufferedWriter bufWriter = new BufferedWriter(configWriter);
		for (int i = 0; i < cfg.size(); i++) {
			bufWriter.write((String)cfg.get(i));
			bufWriter.newLine();
		}
		bufWriter.flush();
		bufWriter.close();
		configWriter.close();
	}

}
