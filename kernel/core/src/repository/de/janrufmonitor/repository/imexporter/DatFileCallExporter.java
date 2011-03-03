package de.janrufmonitor.repository.imexporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.imexport.ICallExporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;

public class DatFileCallExporter implements ICallExporter {

	private String ID = "DatFileCallExporter";
	private String NAMESPACE = "repository.DatFileCallExporter";

	Logger m_logger;
	ICallList m_callList;
	II18nManager m_i18n;
	String m_language;
	String m_filename;

	public DatFileCallExporter() {
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
		return "*.dat";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

	public boolean doExport() {
		File db = new File(this.m_filename);
		try {
			FileWriter dbWriter = new FileWriter(db);
			BufferedWriter bufWriter = new BufferedWriter(dbWriter);
			String aCall = null;
			for (int i = 0; i < this.m_callList.size(); i++) {
				try {
					aCall =
						new String(Serializer.toByteArray(this.m_callList.get(i)));
					bufWriter.write(aCall);
					bufWriter.newLine();
				} catch (SerializerException e) {
					this.m_logger.severe(e.getMessage());
				}
			}
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

}
