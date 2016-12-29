package de.janrufmonitor.application.launcher;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.runtime.launcher.ILauncher;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;

public class ConsoleLauncher implements ILauncher, IConfigurable {

	private String ID = "ConsoleLauncher";
	private String NAMESPACE = "application.ConsoleLauncher";

	private Logger m_logger;
	private Properties m_configuration;
	
	private static String JAVA_INDICATOR = "java -d";
	
	private String CFG_FILENAME ="filename";
	private String CFG_SEPARATOR ="separator";
	
	public ConsoleLauncher() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
	}

	public void addLibrary(String lib) {
		this.m_logger.info("Adding new library to path: "+lib);
		try {
			List cfg = this.getFileContent();
			
			for (int i=0;i<cfg.size();i++) {
				String l = (String)cfg.remove(i);
				if (l.toLowerCase().startsWith(JAVA_INDICATOR)) {
					l = this.addLib((String)l, lib);
					this.m_logger.info("Added new library to path: "+lib);
				}
				cfg.add(i, l);
			}
				
			this.setFileContent(cfg);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private String addLib(String line, String lib) {
		StringTokenizer st = new StringTokenizer(line, " ");
		StringBuffer b = new StringBuffer();
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			if (s.startsWith("-cp") || s.startsWith("-classpath")) {
				String values = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(values, this.getSeparator());
				List l = new ArrayList(st2.countTokens());
				while (st2.hasMoreTokens()) {
					l.add(st2.nextToken());
				}
				if (!l.contains(lib))
					l.add(lib);
				
				b.append("-cp ");
				for (int i=0;i<l.size();i++) {
					b.append(l.get(i) + this.getSeparator());
				}
				s = "";
			}
			b.append(s + " ");
		}
		return b.toString();
	}

	public void removeLibrary(String lib) {
		this.m_logger.info("Removing new library to path: "+lib);
		try {
			List cfg = this.getFileContent();
			for (int i=0, j=cfg.size();i<j;i++) {
				String l = (String)cfg.remove(i);
				if (l.toLowerCase().startsWith(JAVA_INDICATOR)) {
					l = this.removeLib((String) l, lib);
				}
				cfg.add(i, l);
			}
				
			this.setFileContent(cfg);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private String removeLib(String line, String lib) {
		StringTokenizer st = new StringTokenizer(line, " ");
		StringBuffer b = new StringBuffer();
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			if (s.startsWith("-cp") || s.startsWith("-classpath")) {
				String values = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(values, this.getSeparator());
				List l = new ArrayList(st2.countTokens());
				while (st2.hasMoreTokens()) {
				
					l.add(st2.nextToken());
				}
				l.remove(lib);
				b.append("-cp ");
				for (int i=0;i<l.size();i++) {
					b.append(l.get(i) + this.getSeparator());
				}
				s = "";
			}
			b.append(s + " ");
		}
		return b.toString();
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
		return (this.m_configuration.getProperty(this.CFG_FILENAME, (OSUtils.isWindows() ? "jam.bat" : "jam.sh")));
	}
	
	private String getSeparator() {
		return (this.m_configuration.getProperty(this.CFG_SEPARATOR, (OSUtils.isWindows() ? ";" : ":")));
	}
	
	private List getFileContent() throws IOException {
		File configFile = new File(PathResolver.getInstance(PIMRuntime.getInstance()).getAppRoot() + this.getFileName());
		FileReader configReader = new FileReader(configFile);
		BufferedReader bufReader = new BufferedReader(configReader);
		List cfg = new ArrayList();
		this.m_logger.info("Reading file "+this.getFileName()+" ...");
		while (bufReader.ready()) {
			String line = bufReader.readLine();
			cfg.add(line);
			this.m_logger.info(line);
		}
		bufReader.close();
		configReader.close();
		return cfg;
	}
	
	private void setFileContent(List cfg) throws IOException {
		File configFile = new File(PathResolver.getInstance(PIMRuntime.getInstance()).getAppRoot() + this.getFileName());
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
