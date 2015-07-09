package de.janrufmonitor.repository.imexporter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.imexport.ICallerImporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.repository.imexport.ITracker;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class CsvCallerImporter implements ICallerImporter, ITracker {

	private String ID = "CsvCallerImporter";

	private String NAMESPACE = "repository.CsvCallerImporter";

	Logger m_logger;
	II18nManager m_i18n;
	String m_language;
	String m_filename;
	
	int m_total, m_current = 0;
	

	public CsvCallerImporter() {
		m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		m_i18n = PIMRuntime.getInstance().getI18nManagerFactory()
				.getI18nManager();
		m_language = PIMRuntime.getInstance().getConfigManagerFactory()
				.getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_LANGUAGE);
	}
	
	public String getID() {
		return this.ID;
	}

	public int getMode() {
		return IImExporter.CALLER_MODE;
	}

	public int getType() {
		return IImExporter.IMPORT_TYPE;
	}

	public String getFilterName() {
		return this.m_i18n.getString(this.NAMESPACE, "filtername", "label",
				this.m_language);
	}

	public String getExtension() {
		return "*.csv";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

	public int getCurrent() {
		return this.m_current;
	}

	public int getTotal() {
		return this.m_total;
	}

	public ICallerList doImport() {
		this.m_current = 0; this.m_total = 0;
		ICallerList cl = PIMRuntime.getInstance().getCallerFactory().createCallerList();
		
		this.m_logger.info("Start reading CSV file "+this.m_filename);
		File csv = new File(this.m_filename);
		if (csv==null || !csv.exists() || !csv.isFile()) return cl;
		
		List contacts = new ArrayList();
		List attributes = new ArrayList();
		try {
			InputStreamReader inr = new InputStreamReader(new FileInputStream(csv));
			BufferedReader bufReader = new BufferedReader(inr);
			
			String line = null;
			String[] tokens = null;
			while (bufReader.ready()) {
				line = bufReader.readLine();
				if (line.trim().startsWith("#")) continue; // ignore comments
				
				tokens = line.split(";");
				if (attributes.size()==0) {
					attributes.addAll(Arrays.asList(tokens));
					continue;
				}
				
				contacts.add(tokens);
			}
			bufReader.close();
			inr.close();
			
			this.m_total = contacts.size();
			
			for (int i=0;i<this.m_total;i++) {
				this.m_current++;
				tokens = (String[]) contacts.get(i);
				if (tokens.length==attributes.size()) {
					List phones = new ArrayList();
					IAttributeMap m = PIMRuntime.getInstance().getCallerFactory().createAttributeMap();
					String attributename = null;
					for (int j=0;j<tokens.length;j++) {
						attributename = (String) attributes.get(j);
						if (attributename.trim().toLowerCase().startsWith("pn:")) {
							IPhonenumber pn = PIMRuntime.getInstance().getCallerFactory().createPhonenumber(PhonenumberAnalyzer.getInstance().normalize(tokens[j]));
							ICaller c = Identifier.identifyDefault(PIMRuntime.getInstance(), pn);
							if (c!=null) {
								phones.add(c.getPhoneNumber());
								if (attributename.endsWith(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE))
									m.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+c.getPhoneNumber().getTelephoneNumber(), IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE));
								if (attributename.endsWith(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE))
									m.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+c.getPhoneNumber().getTelephoneNumber(), IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE));
								if (attributename.endsWith(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE))
									m.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+c.getPhoneNumber().getTelephoneNumber(), IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE));

							}
						} else {
							if (tokens[j].trim().length()>0) {
								m.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(attributename, tokens[j]));
								if (attributename.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_IMAGEURL)) {
									String img = getImageFromURL(tokens[j]);
									if (img!=null) {
										m.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, img));
									}
								}
							}
						}
					}
					if (phones.size()==0) {
						this.m_logger.warning("No phone numbers found. Can't parse CSV contact: "+ Arrays.asList(tokens));
						continue;
					}
					if (!m.contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME)) {
						this.m_logger.warning("No LASTANME (ln) attribute found. Can't parse CSV contact: "+ Arrays.asList(tokens));
						continue;
					}
						
					cl.add(PIMRuntime.getInstance().getCallerFactory().createCaller(null, phones, m));
				} else {
					this.m_logger.warning("Invalid token length. Can't parse CSV contact data: "+ Arrays.asList(tokens));
				}
			}
			
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return cl;
	}
	
	private String getImageFromURL(String url) {
		if (!url.toLowerCase().startsWith("http://")) return null;
		try {
			URL u  = new URL(url);
			URLConnection c = u.openConnection();
	
			c.setDoInput(true);
			c.setRequestProperty(
				"User-Agent",
				"Mozilla/4.0 (compatible; MSIE; Windows NT)");
			c.connect();
			
			Object o = u.openStream();
			if (o instanceof InputStream) {
				this.m_logger.info("Content successfully retrieved from "+url);
				return this.createImage((InputStream) o, (u.getFile().substring(u.getFile().lastIndexOf("/"))));				
			}			
		} catch (MalformedURLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
	
	private String createImage(InputStream in, String filename) throws IOException {		
		BufferedInputStream bin = new BufferedInputStream(in);
		File image_dir = new File(PathResolver.getInstance(PIMRuntime.getInstance()).getDataDirectory() + File.separator + "photos" + File.separator +"csv-contacts");
		if (!image_dir.exists()) {
			image_dir.mkdirs();
		}
		
		File img = new File(image_dir, filename);
		FileOutputStream fos = new FileOutputStream(img);
		Stream.copy(bin, fos, true);
		return img.getAbsolutePath();
	}

}
