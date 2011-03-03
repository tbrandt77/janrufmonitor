package de.janrufmonitor.repository.imexporter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.imexport.ICallerExporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringEscapeUtils;
import de.janrufmonitor.util.string.StringUtils;

public class GpxCallerExporter implements ICallerExporter {

	private String ID = "GpxCallerExporter";

	private String NAMESPACE = "repository.GpxCallerExporter";

	Logger m_logger;

	ICallerList m_callerList;

	II18nManager m_i18n;

	String m_language;

	String m_filename;

	public GpxCallerExporter() {
		m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		m_i18n = PIMRuntime.getInstance().getI18nManagerFactory()
				.getI18nManager();
		m_language = PIMRuntime.getInstance().getConfigManagerFactory()
				.getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_LANGUAGE);
	}

	public void setCallerList(ICallerList callerList) {
		this.m_callerList = callerList;
	}

	public boolean doExport() {
		File db = new File(m_filename);
		try {

			StringBuffer xml = new StringBuffer();
			xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");xml.append(IJAMConst.CRLF);
			xml.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"jAnrufmonitor\" version=\"1.1\"");xml.append(IJAMConst.CRLF);
			xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""); xml.append(IJAMConst.CRLF);
			xml.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">");xml.append(IJAMConst.CRLF);

			ICaller c = null;
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			String now = df.format(new Date());
			
			for (int i = 0; i < this.m_callerList.size(); i++) {
				c = this.m_callerList.get(i);
				IAttributeMap attributes = c.getAttributes();
				if (attributes.contains(IJAMConst.ATTRIBUTE_NAME_GEO_ACC) && attributes.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LNG) && attributes.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LAT)) {
					
					xml.append("<wpt lat=\"");
					xml.append(attributes.get(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue());
					xml.append("\" lon=\"");
					xml.append(attributes.get(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue());
					xml.append("\">");xml.append(IJAMConst.CRLF);
					
					xml.append("<time>");
					xml.append(now);
					xml.append("</time>");xml.append(IJAMConst.CRLF);

					xml.append("<name>");
					try {
						xml.append(StringEscapeUtils.escapeXml(StringUtils.replaceString(Formatter.getInstance(PIMRuntime.getInstance()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNAME, c), IJAMConst.CRLF, " ").trim()));
					} catch (Exception e) {
						m_logger.log(Level.SEVERE, e.toString(), e);
					}
					xml.append("</name>");xml.append(IJAMConst.CRLF);
					xml.append("<sym>Dot</sym>");xml.append(IJAMConst.CRLF);

					xml.append("</wpt>");xml.append(IJAMConst.CRLF);				    
				}
			}
			xml.append("</gpx>");
			
			FileOutputStream fo = new FileOutputStream(db);
			ByteArrayInputStream bin = new ByteArrayInputStream(xml.toString().getBytes());
			Stream.copy(bin, fo, true);
			fo.close();
		} catch (FileNotFoundException ex) {
			this.m_logger.severe("File not found: " + m_filename);
			return false;
		} catch (IOException ex) {
			this.m_logger.severe("IOException on file " + m_filename);
			return false;
		}
		return true;
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
		return this.m_i18n.getString(this.NAMESPACE, "filtername", "label",
				this.m_language);
	}

	public String getExtension() {
		return "*.gpx";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

}