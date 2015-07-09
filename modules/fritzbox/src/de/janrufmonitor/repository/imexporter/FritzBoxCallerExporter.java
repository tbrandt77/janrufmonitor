package de.janrufmonitor.repository.imexporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.repository.imexport.ICallerExporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.string.StringEscapeUtils;

public class FritzBoxCallerExporter implements ICallerExporter {

	private String ID = "FritzBoxCallerExporter";
	private String NAMESPACE = "repository.FritzBoxCallerExporter";

	Logger m_logger;
	ICallerList m_callerList;
	II18nManager m_i18n;
	String m_language;
	String m_filename;
	String m_intArea;
	
	public FritzBoxCallerExporter() {
		m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		m_i18n = PIMRuntime.getInstance().getI18nManagerFactory().getI18nManager();
		m_language = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_LANGUAGE);
		m_intArea = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
	}


	public void setCallerList(ICallerList callerList) {
		this.m_callerList = callerList;
	}

	public boolean doExport() {
		File db = new File(m_filename);
		try {
			FileWriter dbWriter = new FileWriter(db);
			BufferedWriter bufWriter = new BufferedWriter(dbWriter);
			StringBuffer xml = new StringBuffer();
			xml.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");xml.append(IJAMConst.CRLF);
			xml.append("<phonebooks>");xml.append(IJAMConst.CRLF);
			xml.append("<phonebook owner=\"1\" name=\"jAnrufmonitor Kontakte ("+new SimpleDateFormat("dd.MM.yyyy").format(new Date())+")\">");xml.append(IJAMConst.CRLF);
			for (int i = 0; i < this.m_callerList.size(); i++) {
				this.toXml(this.m_callerList.get(i), xml);
			}
			xml.append("</phonebook>");xml.append(IJAMConst.CRLF);
			xml.append("</phonebooks>");
			bufWriter.write(xml.toString());
			bufWriter.flush();
			bufWriter.close();
			dbWriter.close();
		} catch (FileNotFoundException ex) {
			this.m_logger.severe("File not found: " + m_filename);
			return false;
		} catch (IOException ex) {
			this.m_logger.severe("IOException on file " + m_filename);
			return false;
		}
		return true;
	}
	
	private void toXml(ICaller c, StringBuffer s) {
		s.append("<contact>");s.append(IJAMConst.CRLF);
		s.append("<category></category>");s.append(IJAMConst.CRLF);
		s.append("<person>");s.append(IJAMConst.CRLF);
		s.append("<realName>");
		try {
			s.append(StringEscapeUtils.escapeXml(new String(c.getName().getFullname().getBytes("iso-8859-1"))));
		} catch (UnsupportedEncodingException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		s.append("</realName>");s.append(IJAMConst.CRLF);
		s.append("<imageURL></imageURL>");s.append(IJAMConst.CRLF);
		s.append("</person>");s.append(IJAMConst.CRLF);
		s.append("<numbers>");s.append(IJAMConst.CRLF);
		if (c instanceof IMultiPhoneCaller) {
			List numbers = ((IMultiPhoneCaller)c).getPhonenumbers();
			IPhonenumber pn = null;
			String type = null;
			for (int i = 0; i< numbers.size(); i++){
				pn = (IPhonenumber) numbers.get(i);
				if (!PhonenumberAnalyzer.getInstance().isInternal(pn) && !pn.isClired()) {
					type = "home";
					if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber())) {
						type = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber()).getValue();
						if (type.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE)) type = "home";
						if (type.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) type = "mobile";
						if (type.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) type = "work_fax";
					}
					s.append("<number type=\""+type+"\">");
					s.append((pn.getIntAreaCode().equalsIgnoreCase(m_intArea) ? "0" + pn.getTelephoneNumber() : "00" +pn.getIntAreaCode() + pn.getTelephoneNumber()));
					s.append("</number>");s.append(IJAMConst.CRLF);
				}
				
			}
		} else {
			s.append("<number type=\"home\">");
			s.append((c.getPhoneNumber().getIntAreaCode().equalsIgnoreCase(m_intArea) ? "0" + c.getPhoneNumber().getTelephoneNumber() : "00" + c.getPhoneNumber().getIntAreaCode() + c.getPhoneNumber().getTelephoneNumber()));
			s.append("</number>");s.append(IJAMConst.CRLF);
		}
		s.append("</numbers>");s.append(IJAMConst.CRLF);
		s.append("<services>");s.append(IJAMConst.CRLF);
		
		if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_EMAIL)) {
			s.append("<email classifier=\"private\">");
			s.append(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue());
			s.append("</email>");s.append(IJAMConst.CRLF);
		}
		
		s.append("</services>");s.append(IJAMConst.CRLF);
		s.append("</contact>");s.append(IJAMConst.CRLF);
	}

	public String getID() {
		return this.ID;
	}

	public int getMode() {
		return IImExporter.CALLER_MODE;
	}

	public int getType() {
		return IImExporter.EXPORT_TYPE;
	}

	public String getFilterName() {
		return this.m_i18n.getString(this.NAMESPACE, "filtername", "label", this.m_language);
	}

	public String getExtension() {
		return "*.xml";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

}
