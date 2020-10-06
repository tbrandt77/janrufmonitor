package de.janrufmonitor.repository.imexporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.imexport.ICallExporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;

import de.janrufmonitor.util.string.StringUtils;

public class ICalFileCallExporter implements ICallExporter {

	private String ID = "ICalFileCallExporter";
	private String NAMESPACE = "repository.ICalFileCallExporter";

	Logger m_logger;
	ICallList m_callList;
	II18nManager m_i18n;
	String m_language;
	String m_filename;

	public ICalFileCallExporter() {
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
		return "*.ics";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

	public boolean doExport() {
		File db = new File(this.m_filename);
		try {
			FileWriter dbWriter = new FileWriter(db);
			BufferedWriter bufWriter = new BufferedWriter(dbWriter);
			ICall c = null;
			
			Properties env = System.getProperties();
			Date now = new Date();
			
			bufWriter.write("BEGIN:VCALENDAR"); bufWriter.newLine();
			bufWriter.write("VERSION:2.0"); bufWriter.newLine();
			bufWriter.write("PRODID:-//jAnrufmonitor "+IJAMConst.VERSION_DISPLAY+"//jAnrufmonitor Journal//DE"); bufWriter.newLine();
			bufWriter.write("METHOD:PUBLISH"); bufWriter.newLine();
			for (int i = 0; i < this.m_callList.size(); i++) {
				bufWriter.write("BEGIN:VEVENT"); bufWriter.newLine();
				bufWriter.write("CLASS:PRIVATE"); bufWriter.newLine();
				
				c = this.m_callList.get(i);
				bufWriter.write("UID:");bufWriter.write(c.getUUID());bufWriter.newLine();
				
				IAttribute status = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
				String parseText = null;
				Formatter f = Formatter.getInstance(PIMRuntime.getInstance());

				
				if (status!=null && (status.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_ACCEPTED) || status.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING))) {
					bufWriter.write("TRANSP:OPAQUE"); bufWriter.newLine();
					
					parseText = this.m_i18n.getString(this.NAMESPACE, "event_accepted", "label", this.m_language);
					if (c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null || c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue().trim().length()==0 || c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null || c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue().trim().length()==0)
						parseText = this.m_i18n.getString(this.NAMESPACE, "event_accepted_noname", "label", this.m_language);
					bufWriter.write("SUMMARY:"); bufWriter.write(this.escapeICal(f.parse(parseText, c))); bufWriter.newLine();
					
					parseText = this.m_i18n.getString(this.NAMESPACE, "description_accepted", "label", this.m_language);
					bufWriter.write("DESCRIPTION:"); bufWriter.write(this.escapeICal(f.parse(parseText, c))); bufWriter.newLine();
					
					parseText = this.m_i18n.getString(this.NAMESPACE, "location_accepted", "label", this.m_language);
					bufWriter.write("LOCATION:"); bufWriter.write(this.escapeICal(f.parse(parseText, c))); bufWriter.newLine();
				} else {
					bufWriter.write("TRANSP:TRANSPARENT"); bufWriter.newLine();
					
					parseText = this.m_i18n.getString(this.NAMESPACE, "event_missed", "label", this.m_language);
					if (c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)==null || c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue().trim().length()==0 || c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME)==null || c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME).getValue().trim().length()==0)
						parseText = this.m_i18n.getString(this.NAMESPACE, "event_missed_noname", "label", this.m_language);
					bufWriter.write("SUMMARY:"); bufWriter.write(this.escapeICal(f.parse(parseText, c))); bufWriter.newLine();
					
					parseText = this.m_i18n.getString(this.NAMESPACE, "description_missed", "label", this.m_language);
					bufWriter.write("DESCRIPTION:"); bufWriter.write(this.escapeICal(f.parse(parseText, c))); bufWriter.newLine();
					
					parseText = this.m_i18n.getString(this.NAMESPACE, "location_missed", "label", this.m_language);
					bufWriter.write("LOCATION:"); bufWriter.write(this.escapeICal(f.parse(parseText, c))); bufWriter.newLine();
				}
				
				if (c.getCaller().getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GEO_LAT) && c.getCaller().getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)) {
					bufWriter.write("GEO:");bufWriter.write(c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue());bufWriter.write(";");bufWriter.write(c.getCaller().getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue());bufWriter.newLine();
				}
				
				bufWriter.write("DTSTAMP;TZID="); bufWriter.write(env.getProperty("user.timezone", "Europe/Berlin")); bufWriter.write(":");bufWriter.write(this.getICalDate(now));bufWriter.newLine();
				bufWriter.write("DTSTART;TZID="); bufWriter.write(env.getProperty("user.timezone", "Europe/Berlin")); bufWriter.write(":");bufWriter.write(this.getICalDate(c.getDate()));bufWriter.newLine();
				Calendar cal = Calendar.getInstance();
				cal.setTime(c.getDate());
				cal.add(Calendar.MINUTE, this.getDuration(c));
				bufWriter.write("DTSTAMP;TZID="); bufWriter.write(env.getProperty("user.timezone", "Europe/Berlin")); bufWriter.write(":");bufWriter.write(this.getICalDate(cal.getTime()));bufWriter.newLine();


				bufWriter.write("END:VEVENT"); bufWriter.newLine();
			}
			bufWriter.write("END:VCALENDAR"); bufWriter.newLine();
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

	public void setCallList(ICallList callList) {
		this.m_callList = callList;
	}

	public int getType() {
		return IImExporter.EXPORT_TYPE;
	}
	
	private int getDuration(ICall call) {
		IAttribute ring = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_DURATION);
		if (ring!=null) {
			try {
				return Integer.parseInt(ring.getValue());
			} catch (Exception e) {
			}
		}

		return 0;
	}
	
	private String escapeICal(String t) {
		t = StringUtils.replaceString(t, "\n", "\\n");
		t = StringUtils.replaceString(t, ",", "\\,");
		t = StringUtils.replaceString(t, "  ", " ");
		t = StringUtils.replaceString(t, "\\n\\n", "\\n");
		t = StringUtils.replaceString(t, "\\n \\n", "\\n");
		return t;
	}
	
	private String getICalDate(Date d) {
		SimpleDateFormat sfd = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'"); 
		return sfd.format(d);
	}

}
