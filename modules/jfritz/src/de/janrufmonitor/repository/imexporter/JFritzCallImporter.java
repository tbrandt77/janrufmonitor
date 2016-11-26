package de.janrufmonitor.repository.imexporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICallFactory;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.repository.imexport.ICallImporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.string.StringUtils;

public class JFritzCallImporter implements ICallImporter {

	private class MigratorThread implements Runnable {

		/**
		 * "CallType";"Date";"Time";"Number";"Route";"Port";"Duration";"Name";"Address";"City";"CallByCall";"Comment"
		 * "Outgoing";"27.03.2008";"09:43";"+491608896404";"Festnetz";"Bro";"60";"Brandt, Thilo";"Kelterwiesen 31";"74889 Sinsheim-Dhren";"01055";""
		 * "Outgoing";"27.03.2008";"09:41";"+496227763614";"4063123@1und1.de";"Bro";"60";"Brandt, Thilo";"Kelterwiesen 31";"74889 Sinsheim-Dhren";"";""
		 * 
		 */
		
		private ICallList m_cl;

		private String m_call;

		private ICallFactory m_clf;

		public MigratorThread(ICallList cl, String line) {
			this.m_cl = cl;
			this.m_call = line;
			this.m_clf = getRuntime().getCallFactory();
		}

		public void run() {
			if (this.m_call == null) {
				m_logger.warning("Invalid migration entry.");
				return;
			}
			String[] splittedCall = removeQuotes(this.m_call).split(";");
			if (splittedCall.length<12) {
				m_logger.warning("Invalid migration entry. Less then 12 tokens: "+this.m_call);
				return;
			}
			
			if(splittedCall[3].trim().length()>0 && !splittedCall[3].trim().startsWith("+")) {
				m_logger.warning("Invalid JFritz call: "+removeQuotes(this.m_call));
				return;
			}
			
			if (m_logger.isLoggable(Level.INFO)) {
				m_logger.info("Migrating JFritz call: "+removeQuotes(this.m_call));	
			}
			
			// create caller object
			//IPhonenumber n = getRuntime().getCallerFactory().createPhonenumber(true);
			ICaller c = null;
			if (splittedCall[3].trim().length()>0) {
				IPhonenumber pn = PhonenumberAnalyzer.getInstance(getRuntime()).toIdentifiedPhonenumber(splittedCall[3].trim());
				if (pn==null) return;
				
				c = getRuntime().getCallerFactory().createCaller(pn);	
				
				IAttributeMap m = this.m_clf.createAttributeMap();
				
				m.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_LASTNAME, 
					splittedCall[7].trim()
					)
				);
				
				m.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_STREET, 
					splittedCall[8].trim()
					)
				);
				m.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CITY, 
					splittedCall[9].trim()
					)
				);		
				
				c.setAttributes(m);
			}
			
			if (c==null) return;

			IAttributeMap mc = this.m_clf.createAttributeMap();
			mc.add(
				getRuntime().getCallerFactory().createAttribute(
				"fritzbox.line", 
				splittedCall[5].trim()
				)
			);
			
			mc.add(
				getRuntime().getCallerFactory().createAttribute(
				"fritzbox.duration", 
				splittedCall[6].trim()
				)
			);	
			
			if (splittedCall[11].trim().length()>0)
				mc.add(
					getRuntime().getCallerFactory().createAttribute(
					"fritzbox.callbycall", 
					splittedCall[11].trim()
					)
				);		
			
			if (splittedCall[0].trim().equalsIgnoreCase("Outgoing"))
				mc.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, 
					IJAMConst.ATTRIBUTE_VALUE_OUTGOING
					)
				);		
			
			if (splittedCall[0].trim().equalsIgnoreCase("Incoming"))
				mc.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, 
					IJAMConst.ATTRIBUTE_VALUE_ACCEPTED
					)
				);					
				
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm");
			Date date = new Date(0);
			try {
				date = sdf.parse((splittedCall[1].trim() + " " + splittedCall[2].trim()));
			} catch (ParseException e) {
				Logger.getLogger(IJAMConst.DEFAULT_LOGGER).severe("Wrong date format detected.");
			}
			
			ICip cip = getRuntime().getCipManager().createCip("1");
			IMsn msn = getRuntime().getMsnManager().createMsn((splittedCall[4].trim().indexOf("@")>=0 ? splittedCall[4].trim().substring(0, splittedCall[4].trim().indexOf("@")) : splittedCall[4].trim()));
			
			StringBuffer uuid = new StringBuffer();
			uuid.append(date.getTime());
			uuid.append("-");
			uuid.append(splittedCall[3].trim());
			uuid.append("-");
			uuid.append(msn.getMSN());
			
			this.m_cl.add(this.m_clf.createCall(uuid.toString(), c, msn, cip, date, mc));
		}
		
		private String removeQuotes(String s) {
			return StringUtils.replaceString(s, "\"", " ");
		}
	}

	private String ID = "JFritzCallImporter";

	private String NAMESPACE = "repository.JFritzCallImporter";

	Logger m_logger;

	ICallList m_callList;

	II18nManager m_i18n;

	String m_language;

	String m_filename;

	private IRuntime m_runtime;

	public JFritzCallImporter() {
		m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		m_i18n = getRuntime().getI18nManagerFactory().getI18nManager();
		m_language = getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_LANGUAGE);
	}

	public String getExtension() {
		return "calls.csv";
	}

	public String getFilterName() {
		return this.m_i18n.getString(this.NAMESPACE, "filtername", "label",
				this.m_language);
	}

	public String getID() {
		return ID;
	}

	public int getMode() {
		return IImExporter.CALL_MODE;
	}

	public int getType() {
		return IImExporter.IMPORT_TYPE;
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

	private IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public ICallList doImport() {
		long startDate = System.currentTimeMillis();
		File jfritzfile = new File(m_filename);

		int size = (int) jfritzfile.length() / 64;
		size = Math.abs(size);

		m_callList = getRuntime().getCallFactory().createCallList(size);

		try {
			FileReader dbReader = new FileReader(jfritzfile);
			BufferedReader bufReader = new BufferedReader(dbReader);
			String line = null;
			while (bufReader.ready()) {
				line = bufReader.readLine();
				if (!line.startsWith("\"CallType\";")) {
					new MigratorThread(
							this.m_callList, line).run();			
				}
			}
			bufReader.close();
			dbReader.close();
		} catch (FileNotFoundException ex) {
			this.m_logger.warning("Cannot find call file " + m_filename);
			return this.m_callList;
		} catch (IOException ex) {
			this.m_logger.severe("IOException on file " + m_filename);
			return this.m_callList;
		}

		long endDate = System.currentTimeMillis();
		this.m_logger.info("Successfully imported call file " + m_filename);
		this.m_logger
				.info("Found " + new Integer(this.m_callList.size()).toString()
						+ " call items in "
						+ new Float((endDate - startDate) / 1000).toString()
						+ " secs.");
		return m_callList;
	}

}
