package de.janrufmonitor.repository.imexporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.framework.monitor.PhonenumberInfo;
import de.janrufmonitor.repository.imexport.ICallerExporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.Base64Encoder;
import de.janrufmonitor.util.io.ImageHandler;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class VcfFileCallerExporter implements ICallerExporter {

	private String ID = "VcfFileCallerExporter";

	private String NAMESPACE = "repository.VcfFileCallerExporter";

	Logger m_logger;

	ICallerList m_callerList;

	II18nManager m_i18n;

	String m_language;

	String m_filename;
	
	static final String CRLF = "\r\n";

	public VcfFileCallerExporter() {
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
			StringBuffer vcf = new StringBuffer();
			Formatter f = Formatter.getInstance(PIMRuntime.getInstance());
			String number_pattern = "+%intareacode% (%areacode%) %callnumber%";
			ICaller c = null;
			for (int i = 0, j = this.m_callerList.size(); i<j; i++) {
				c = this.m_callerList.get(i);
				vcf.append("BEGIN:VCARD");vcf.append(CRLF);
				vcf.append("VERSION:3.0");vcf.append(CRLF);
				vcf.append("N;CHARSET=ISO-8859-1:");vcf.append(c.getName().getLastname());vcf.append(";");vcf.append(c.getName().getFirstname());vcf.append(";;");vcf.append(CRLF);
				vcf.append("FN;CHARSET=ISO-8859-1:");vcf.append(c.getName().getFirstname());vcf.append(" ");vcf.append(c.getName().getLastname());vcf.append(CRLF);
				if (c.getName().getAdditional().trim().length()>0)
					vcf.append("ORG;CHARSET=ISO-8859-1:");vcf.append(c.getName().getAdditional());vcf.append(CRLF);
				vcf.append("SORT-STRING:");vcf.append(c.getName().getLastname());vcf.append(CRLF);
				vcf.append("CLASS:PRIVATE");vcf.append(CRLF);
				if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_EMAIL)) {
					vcf.append("EMAIL;TYPE=home:");vcf.append(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL).getValue());vcf.append(CRLF);
				}
				vcf.append("ADR;TYPE=home;CHARSET=ISO-8859-1:;;");vcf.append(this.getAttribute(c, IJAMConst.ATTRIBUTE_NAME_STREET));vcf.append(" ");vcf.append(this.getAttribute(c, IJAMConst.ATTRIBUTE_NAME_STREET_NO));vcf.append(";");vcf.append(this.getAttribute(c, IJAMConst.ATTRIBUTE_NAME_CITY));vcf.append(";;");vcf.append(this.getAttribute(c, IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE));vcf.append(";");vcf.append(this.getAttribute(c, IJAMConst.ATTRIBUTE_NAME_COUNTRY));vcf.append(CRLF);
				
				if (c instanceof IMultiPhoneCaller) {
					List pns = ((IMultiPhoneCaller)c).getPhonenumbers();
					IPhonenumber p = null;
					for (int k=0,l=pns.size();k<l;k++) {
						p = (IPhonenumber) pns.get(k);
						if (p.isClired()) continue;
						String numbertype = this.getPhoneType(p, c);
						if (PhonenumberInfo.isInternalNumber(p)) {
							if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE)) {
								vcf.append("TEL;TYPE=home:");vcf.append(p.getCallNumber());vcf.append(CRLF);
							}	
							if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
								vcf.append("TEL;TYPE=fax,home:");vcf.append(p.getCallNumber());vcf.append(CRLF);
							}
							if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
								vcf.append("TEL;TYPE=cell:");vcf.append(p.getCallNumber());vcf.append(CRLF);
							}
						} else {
							if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE)) {
								vcf.append("TEL;TYPE=home:");vcf.append(f.parse(number_pattern, p));vcf.append(CRLF);
							}	
							if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
								vcf.append("TEL;TYPE=fax,home:");vcf.append(f.parse(number_pattern, p));vcf.append(CRLF);
							}
							if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
								vcf.append("TEL;TYPE=cell:");vcf.append(f.parse(number_pattern, p));vcf.append(CRLF);
							}
						}
					}
				} else {
					String numbertype = this.getPhoneType(c.getPhoneNumber(), c);
					if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE)) {
						vcf.append("TEL;TYPE=home:");vcf.append(f.parse(number_pattern, c.getPhoneNumber()));vcf.append(CRLF);
					}
					if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
						vcf.append("TEL;TYPE=fax,home:");vcf.append(f.parse(number_pattern, c.getPhoneNumber()));vcf.append(CRLF);
					}
					if (numbertype.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
						vcf.append("TEL;TYPE=cell:");vcf.append(f.parse(number_pattern, c.getPhoneNumber()));vcf.append(CRLF);
					}
				}
				
				if (ImageHandler.getInstance().hasImage(c)) {
					vcf.append("PHOTO;ENCODING=b;TYPE=JPEG:");
					InputStream fim = ImageHandler.getInstance().getImageStream(c);
					if (c!=null) {
						ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
						// finalize lines with '\n ' instead of '\n'
						Base64Encoder b64 = new Base64Encoder(encodedOut, " ".getBytes());
						Stream.copy(fim, b64);						
						b64.flush();
						b64.close();
						String imagedata = new String(encodedOut.toByteArray());
						int size = imagedata.length();
						vcf.append(imagedata.substring(0,43));
						int z = 43;
						while (z<imagedata.length()) {
							vcf.append(imagedata.substring(z,Math.min((z+69), size)));
							z +=69;
						}
						
						vcf.append(CRLF);
					}
				} else if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH)) {
					// add embedded photo as base 64 encoded object
					
					String file = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH).getValue();
					File img = new File(PathResolver.getInstance().resolve(file));
					if (img.exists()) {
						vcf.append("PHOTO;ENCODING=b;TYPE=JPEG:");
						FileInputStream fim = new FileInputStream(img);
						ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
						// finalize lines with '\n ' instead of '\n'
						Base64Encoder b64 = new Base64Encoder(encodedOut, " ".getBytes());
						Stream.copy(fim, b64);						
						b64.flush();
						b64.close();
						String imagedata = new String(encodedOut.toByteArray());
						int size = imagedata.length();
						vcf.append(imagedata.substring(0,43));
						int z = 43;
						while (z<imagedata.length()) {
							vcf.append(imagedata.substring(z,Math.min((z+69), size)));
							z +=69;
						}
						
						vcf.append(CRLF);
					}				
				}
				
				if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GEO_LAT) && c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)) {
					// add geo tagging
					vcf.append("GEO:");
					vcf.append(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue());
					vcf.append(";");
					vcf.append(c.getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue());
					vcf.append(CRLF);
				}

				vcf.append("PRODID:-//jAnrufmonitor//www.janrufmonitor.de//Version 5.0");vcf.append(CRLF);				
				vcf.append("UID:JAM-UID-");vcf.append(c.getUUID());vcf.append(CRLF);
				vcf.append("REV:");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
				vcf.append(sdf.format(new Date()));
				vcf.append(CRLF);
				vcf.append("END:VCARD");vcf.append(CRLF);vcf.append(CRLF);
			}
			
			FileOutputStream fos = new FileOutputStream(db);
			ByteArrayInputStream bin = new ByteArrayInputStream(vcf.toString().getBytes());
			
			Stream.copy(bin, fos, true);

		} catch (FileNotFoundException ex) {
			this.m_logger.severe("File not found: " + m_filename);
			return false;
		} catch (IOException ex) {
			this.m_logger.severe("IOException on file " + m_filename);
			return false;
		}
		return true;
	}
	
	private String getPhoneType(IPhonenumber n, ICaller c) {
		IAttribute pa = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+n.getTelephoneNumber());
		if (pa!=null) {
			return pa.getValue();
		}
		return IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE;
	}
	
	private String getAttribute(ICaller c, String name) {
		IAttribute a = c.getAttribute(name);
		if (a!=null) {
			return a.getValue();
		}
		return "";
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
		return "*.vcf";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

}