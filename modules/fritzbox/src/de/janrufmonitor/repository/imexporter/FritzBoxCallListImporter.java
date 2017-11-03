package de.janrufmonitor.repository.imexporter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.fritzbox.FritzBoxCallCsv;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.repository.imexport.ICallImporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IModifierService;

public class FritzBoxCallListImporter implements ICallImporter {
	
	private String ID = "FritzBoxCallListImporter";
	private String NAMESPACE = "repository.FritzBoxCallListImporter";

	private Logger m_logger;
	private ICallList m_callList;
	private II18nManager m_i18n;
	private String m_language;
	private String m_filename;

	public FritzBoxCallListImporter() {
		m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		m_i18n = PIMRuntime.getInstance().getI18nManagerFactory().getI18nManager();
		m_language = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_LANGUAGE);
	}

	public String getID() {
		return this.ID;
	}

	public int getMode() {
		return IImExporter.CALL_MODE;
	}

	public String getFilterName() {
		return this.m_i18n.getString(this.NAMESPACE, "filtername", "label", this.m_language);
	}

	public String getExtension() {
		return "*.csv";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

	public ICallList doImport() {
		m_callList = PIMRuntime.getInstance().getCallFactory().createCallList();
		try {
			FileInputStream fin = new FileInputStream(m_filename);
			List result = this.getRawCallList(fin);
			if (result.size()>0) {
				FritzBoxCallCsv call = null;
				Properties conf = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
				ICall c = null;
				for (int i=0,j=result.size();i<j;i++) {
					call = new FritzBoxCallCsv((String) result.get(i), conf);
					c = call.toCall();
					if (c!=null) {
						if (!m_callList.contains(c))
							m_callList.add(c);
						else {
							this.m_logger.warning("Call already imported from FritzBox: "+c.toString());
							c.setUUID(c.getUUID()+"-1");
							ICip cip = c.getCIP();
							cip.setCIP("4"); // just a dirty hack 
							c.setCIP(cip);
							if (!m_callList.contains(c))
								m_callList.add(c);
						}
					}
				}
			}

		} catch (FileNotFoundException e) {
			this.m_logger.severe("File not found: " + m_filename);
		} catch (IOException e) {
			this.m_logger.severe("IO Error on file " + m_filename
					+ ": " + e.getMessage());
		}
		if (m_callList!=null && m_callList.size()>0) {
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("Processing modifier services on call list: "+PIMRuntime.getInstance().getServiceFactory().getModifierServices());
			processModifierServices(m_callList);
		}
		return m_callList;
	}
	
	private List getRawCallList(InputStream in) throws IOException {
		if (in==null) return new ArrayList(0);
		List result = new ArrayList();
		InputStreamReader inr = new InputStreamReader(in);
		BufferedReader bufReader = new BufferedReader(inr);
		
		String line = bufReader.readLine(); // drop header
		
		if (line.startsWith("sep=")) // new fw version
			bufReader.readLine(); // drop header o fnew fw
		
		while (bufReader.ready()) {
			line = bufReader.readLine();
			result.add(line);
		}
		bufReader.close();
		in.close();
		return result;
	}
	
	private void processModifierServices(ICallList cl) {
		if (cl!=null && cl.size()>0) {
			List msvc = PIMRuntime.getInstance().getServiceFactory().getModifierServices();
			IModifierService s = null;
			for (int k=0,l=msvc.size();k<l;k++) {
				s = (IModifierService) msvc.get(k);
				if (s!=null && s.isEnabled()) {
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Processing modifier service <"+s.getServiceID()+">");
					ICall call = null;
					for (int i=0,j=cl.size();i<j;i++) {
						call = cl.get(i);
						s.modifyObject(call);
					}			
				}
			}
		}
	}

	public int getType() {
		return IImExporter.IMPORT_TYPE;
	}
}
