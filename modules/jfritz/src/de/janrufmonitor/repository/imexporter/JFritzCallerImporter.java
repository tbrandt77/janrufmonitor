package de.janrufmonitor.repository.imexporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerFactory;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.CallerNotFoundException;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.repository.imexport.ICallerImporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.string.StringUtils;

public class JFritzCallerImporter implements ICallerImporter {

	private class MigratorThread implements Runnable {

		/**
		 * "Private";"Last Name";"First Name";"Company";"Street";"ZIP Code";"City";"E-Mail";"Home";"Mobile";"Homezone";"Business";"Other";"Fax";"Sip";"Main"
		 * "NO";"Bartholomä";"Dieter";"";"Wittumstr. 11";"74889";"Sinsheim-Eschelbach";"";"+497265315";"";"";"";"";"";"";""
		 * "NO";"Baumgärtner";"Bianca";"";"Rathausstr. 1";"74934";"Reichartshausen";"";"";"+491786011100";"";"";"";"";"";""
		 * "NO";"Baumgärtner";"Hilde";"";"Ringstr. 7";"74934";"Reichartshausen";"";"+4962626007";"";"";"";"";"";"";""
		 * "NO";"Baumgärtner";"Steffen";"";"Ringstr. 7";"74934";"Reichartshausen, Baden-Württemberg";"";"+49626295470";"+491777569679";"";"";"";"";"";""
		 * 
		 */
		
		private ICallerList m_cl;

		private String m_caller;

		private ICallerFactory m_clf;

		public MigratorThread(ICallerList cl, String line) {
			this.m_cl = cl;
			this.m_caller = line;
			this.m_clf = getRuntime().getCallerFactory();
		}

		public void run() {
			if (this.m_caller == null) {
				m_logger.warning("Invalid migration entry.");
				return;
			}
			String[] splittedCaller = removeQuotes(this.m_caller).split(";");

			if (m_logger.isLoggable(Level.INFO)) {
				m_logger.info("Migrating JFritz contact: "+removeQuotes(this.m_caller));	
			}
			
			IAttributeMap m = this.m_clf.createAttributeMap();
			
			if (splittedCaller[1].trim().length()>0)
				m.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_LASTNAME, 
					splittedCaller[1].trim()
					)
				);
			
			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, 
				splittedCaller[2].trim()
				)
			);
				
			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_ADDITIONAL, 
				splittedCaller[3].trim()
				)
			);
						
			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_STREET, 
				splittedCaller[4].trim()
				)
			);				

			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, 
				splittedCaller[5].trim()
				)
			);
			if (splittedCaller[6].trim().length()>0)
				m.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CITY, 
					splittedCaller[6].trim()
					)
				);	
			
			List phones = new ArrayList(3);
			
			// homenumber
			IPhonenumber pn = getPhone(splittedCaller[8].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}
			
			pn = getPhone(splittedCaller[9].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE
				));
				phones.add(pn);
			}
			
			pn = getPhone(splittedCaller[10].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			pn = getPhone(splittedCaller[11].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			pn = getPhone(splittedCaller[12].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			pn = getPhone(splittedCaller[13].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE
				));
				phones.add(pn);
			}			
			
			pn = getPhone(splittedCaller[14].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			if (!m.contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME) && 
				!m.contains(IJAMConst.ATTRIBUTE_NAME_CITY) ) {
			
					m_logger.warning("No lastname or country field available for caller: "+removeQuotes(this.m_caller));	
					if (phones.size()>0)
						m.add(getRuntime().getCallerFactory().createAttribute(
								IJAMConst.ATTRIBUTE_NAME_CITY, 
								getCity((IPhonenumber) phones.get(0))
						));
				
			}
			
			if (phones.size()>0) {
				ICaller c = this.m_clf.createCaller(this.m_clf.createName("", ""), phones);
				c.getAttributes().addAll(m);
				this.m_cl.add(c);	
			}

		}
		
		private String getCity(IPhonenumber p) {
			ICallerManager mgr = getRuntime().getCallerManagerFactory()
					.getCallerManager("CountryDirectory");
			if (mgr != null && mgr instanceof IIdentifyCallerRepository) {
				
				try {
					ICaller c = ((IIdentifyCallerRepository) mgr)
							.getCaller(p);
					IAttribute city = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CITY);
					return (city!=null ? city.getValue() : "");
				} catch (CallerNotFoundException ex) {
					return "";
				}
			}	
			return "";
		}
		
		private IPhonenumber getPhone(String p) {
			if (p.trim().length()==0) return null;
			
			if (!p.trim().startsWith("+")) return null;
			
			IPhonenumber pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toIdentifiedPhonenumber(p);
			if (pn != null) return pn;	
			return null;
		}
		
		private String removeQuotes(String s) {
			return StringUtils.replaceString(s, "\"", " ");
		}
	}

	private class MigratorThread2 implements Runnable {

		/**
		 * "Private";"Last Name";"First Name";"Company";"Street";"ZIP Code";"City";"E-Mail";"Picture";"Home";"Mobile";"Homezone";"Business";"Other";"Fax";"Sip";"Main"
		 * "NO";"Bartholomä";"Dieter";"";"Wittumstr. 11";"74889";"Sinsheim-Eschelbach";"";"";"+497265315";"";"";"";"";"";"";""
		 * "NO";"Baumgärtner";"Bianca";"";"Rathausstr. 1";"74934";"Reichartshausen";"";"";"";"+491786011100";"";"";"";"";"";""
		 * "NO";"Baumgärtner";"Hilde";"";"Ringstr. 7";"74934";"Reichartshausen";"";"";"+4962626007";"";"";"";"";"";"";""
		 * "NO";"Baumgärtner";"Steffen";"";"Ringstr. 7";"74934";"Reichartshausen, Baden-Württemberg";"";"";"+49626295470";"+491777569679";"";"";"";"";"";""
		 * 
		 */
		
		private ICallerList m_cl;

		private String m_caller;

		private ICallerFactory m_clf;

		public MigratorThread2(ICallerList cl, String line) {
			this.m_cl = cl;
			this.m_caller = line;
			this.m_clf = getRuntime().getCallerFactory();
		}

		public void run() {
			if (this.m_caller == null) {
				m_logger.warning("Invalid migration entry.");
				return;
			}
			String[] splittedCaller = removeQuotes(this.m_caller).split(";");

			if (splittedCaller.length<15) {
				m_logger.warning("Invalid migration entry. Less then 15 tokens: "+this.m_caller);
				return;
			}
			
			if (m_logger.isLoggable(Level.INFO)) {
				m_logger.info("Migrating JFritz contact: "+removeQuotes(this.m_caller));	
			}
			
			IAttributeMap m = this.m_clf.createAttributeMap();
			
			if (splittedCaller[1].trim().length()>0)
				m.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_LASTNAME, 
					splittedCaller[1].trim()
					)
				);
			
			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, 
				splittedCaller[2].trim()
				)
			);
				
			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_ADDITIONAL, 
				splittedCaller[3].trim()
				)
			);
						
			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_STREET, 
				splittedCaller[4].trim()
				)
			);				

			m.add(
				getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, 
				splittedCaller[5].trim()
				)
			);
			if (splittedCaller[6].trim().length()>0)
				m.add(
					getRuntime().getCallerFactory().createAttribute(
					IJAMConst.ATTRIBUTE_NAME_CITY, 
					splittedCaller[6].trim()
					)
				);	
			
			List phones = new ArrayList(3);
			
			// homenumber
			IPhonenumber pn = getPhone(splittedCaller[9].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}
			
			pn = getPhone(splittedCaller[10].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE
				));
				phones.add(pn);
			}
			
			pn = getPhone(splittedCaller[11].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			pn = getPhone(splittedCaller[12].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			pn = getPhone(splittedCaller[13].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			pn = getPhone(splittedCaller[14].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE
				));
				phones.add(pn);
			}			
			
			pn = getPhone(splittedCaller[15].trim());
			if (pn!=null) {
				m.add(getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE + pn.getTelephoneNumber(), 
				IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE
				));
				phones.add(pn);
			}	
			
			if (!m.contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME) && 
				!m.contains(IJAMConst.ATTRIBUTE_NAME_CITY) ) {
			
					m_logger.warning("No lastname or country field available for caller: "+removeQuotes(this.m_caller));	
					if (phones.size()>0)
						m.add(getRuntime().getCallerFactory().createAttribute(
								IJAMConst.ATTRIBUTE_NAME_CITY, 
								getCity((IPhonenumber) phones.get(0))
						));
				
			}
			
			if (phones.size()>0) {
				ICaller c = this.m_clf.createCaller(this.m_clf.createName("", ""), phones);
				c.getAttributes().addAll(m);
				this.m_cl.add(c);	
			}

		}
		
		private String getCity(IPhonenumber p) {
			ICallerManager mgr = getRuntime().getCallerManagerFactory()
					.getCallerManager("CountryDirectory");
			if (mgr != null && mgr instanceof IIdentifyCallerRepository) {
				
				try {
					ICaller c = ((IIdentifyCallerRepository) mgr)
							.getCaller(p);
					IAttribute city = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CITY);
					return (city!=null ? city.getValue() : "");
				} catch (CallerNotFoundException ex) {
					return "";
				}
			}	
			return "";
		}
		
		private IPhonenumber getPhone(String p) {
			if (p.trim().length()==0) return null;
			
			if (!p.trim().startsWith("+")) return null;
			
			IPhonenumber pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toIdentifiedPhonenumber(p);
			if (pn != null) return pn;	
			return null;
		}
		
		private String removeQuotes(String s) {
			return StringUtils.replaceString(s, "\"", " ");
		}
	}
	
	private String ID = "JFritzCallerImporter";

	private String NAMESPACE = "repository.JFritzCallerImporter";

	Logger m_logger;

	ICallerList m_callerList;

	II18nManager m_i18n;

	String m_language;

	String m_filename;

	private IRuntime m_runtime;

	public JFritzCallerImporter() {
		m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		m_i18n = getRuntime().getI18nManagerFactory().getI18nManager();
		m_language = getRuntime().getConfigManagerFactory().getConfigManager()
				.getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_LANGUAGE);
	}

	public String getExtension() {
		return "contacts.csv";
	}

	public String getFilterName() {
		return this.m_i18n.getString(this.NAMESPACE, "filtername", "label",
				this.m_language);
	}

	public String getID() {
		return ID;
	}

	public int getMode() {
		return IImExporter.CALLER_MODE;
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

	public ICallerList doImport() {
		long startDate = System.currentTimeMillis();
		File jfritzfile = new File(m_filename);

		int size = (int) jfritzfile.length() / 64;
		size = Math.abs(size);

		m_callerList = getRuntime().getCallerFactory().createCallerList(size);

		try {
			FileReader dbReader = new FileReader(jfritzfile);
			BufferedReader bufReader = new BufferedReader(dbReader);
			String line = null;
			boolean isJFRitz0741 = false;
			while (bufReader.ready()) {
				line = bufReader.readLine();
				this.m_logger.info("Line: "+line);
				if (line.startsWith("\"Private\";") && line.indexOf("Picture")>0) isJFRitz0741 = true;
				if (!line.startsWith("\"Private\";") && !isJFRitz0741) {
					this.m_logger.info("Using MigratorThread");
					new MigratorThread(
							this.m_callerList, line).run();

				}
				if (!line.startsWith("\"Private\";") && isJFRitz0741) {
					this.m_logger.info("Using MigratorThread2");
					new MigratorThread2(
							this.m_callerList, line).run();

				}
			}
			bufReader.close();
			dbReader.close();
		} catch (FileNotFoundException ex) {
			this.m_logger.warning("Cannot find call file " + m_filename);
			return this.m_callerList;
		} catch (IOException ex) {
			this.m_logger.severe("IOException on file " + m_filename);
			return this.m_callerList;
		}
		

		long endDate = System.currentTimeMillis();
		this.m_logger.info("Successfully imported call file " + m_filename);
		this.m_logger
				.info("Found " + new Integer(this.m_callerList.size()).toString()
						+ " call items in "
						+ new Float((endDate - startDate) / 1000).toString()
						+ " secs.");
		return m_callerList;
	}

}
