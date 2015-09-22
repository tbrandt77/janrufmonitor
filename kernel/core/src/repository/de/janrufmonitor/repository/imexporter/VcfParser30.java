package de.janrufmonitor.repository.imexporter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.Base64Decoder;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.uuid.UUID;

public class VcfParser30 {

	protected static final String BEGIN_VCARD = "BEGIN:VCARD";
	protected static final String END_VCARD = "END:VCARD".toLowerCase();
	protected static final String ATTRIBUTE_TYPE = "type=".toLowerCase();
	
	protected static final String N = "N;".toLowerCase();
	protected static final String N2 = "N:".toLowerCase();
	protected static final String ADR = "ADR;".toLowerCase();
	protected static final String[] ADR_TYPES = {"home", "work"};
	
	protected static final String TEL= "TEL;".toLowerCase();
	protected static final String[] TEL_TYPES = {"home", "work", "cell", "fax", "isdn"};
	protected static final String EMAIL= "EMAIL;".toLowerCase();
	protected static final String[] EMAIL_TYPES = {"home", "work"};
	protected static final String ORG = "ORG;".toLowerCase();
	protected static final String TITLE = "TITLE:".toLowerCase();
	protected static final String ORG2 = "ORG:".toLowerCase();
	protected static final String GEO = "GEO:".toLowerCase();
	protected static final String TITLE2 = "TITLE;".toLowerCase();
	protected static final String PHOTO = "PHOTO;".toLowerCase();
	protected static final String MS_CARDPICTURE = "X-MS-CARDPICTURE".toLowerCase();
	protected static final String[] PHOTO_TYPES = {"value=uri", "value=url", "encoding=b", "type="};

	protected String m_file = null;
	protected Logger m_logger;
	protected StringBuffer m_buffer;
	protected int m_current = 1;
	protected int m_total = 1;
	
	public VcfParser30(String vcffile) {
		m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		
		this.m_file = vcffile;
	}
	
	private void read() throws VcfParserException  {
		this.m_logger.info("Start reading vcf file "+this.m_file);
		File vcf = new File(this.m_file);
		if (vcf==null || !vcf.exists() || !vcf.isFile()) throw new VcfParserException("File does not exist or is not a valid file: "+vcf.getAbsolutePath());
		
		this.m_buffer = new StringBuffer((int) (vcf.length()>Integer.MAX_VALUE ? Integer.MAX_VALUE : vcf.length()));
		
		try {
			FileReader vcfReader = new FileReader(vcf);
			BufferedReader bufReader = new BufferedReader(vcfReader);
			while (bufReader.ready()) {
				this.m_buffer.append(bufReader.readLine());
				this.m_buffer.append(IJAMConst.CRLF);
			}
			bufReader.close();
			vcfReader.close();
		} catch (FileNotFoundException ex) {
			throw new VcfParserException("File not found: "+vcf.getAbsolutePath(), ex);
		} catch (IOException ex) {
			throw new VcfParserException("I/O exception on file: "+vcf.getAbsolutePath(), ex);
		}
	}
	
	public void invalidate() {
		if (this.m_buffer!=null) {
			this.m_buffer.delete(0, this.m_buffer.length());
			this.m_buffer = null;
		}
	}
	
	public ICallerList parse() throws VcfParserException {
		if (this.m_buffer==null) this.read();
		
		if (this.m_buffer.length()<64) throw new VcfParserException("VCF file contains only "+this.m_buffer.length()+" characters.");
		
		ICallerList cl = PIMRuntime.getInstance().getCallerFactory().createCallerList();
		
		String[] entries = this.m_buffer.toString().split(BEGIN_VCARD);
		this.m_logger.info("Entries in generic VcfParser vcf file: "+entries.length);
		this.m_total = entries.length;

		for (int i=0;i<entries.length;i++) {
			try {
				this.m_current++;
				IAttributeMap private_attributes = PIMRuntime.getInstance().getCallerFactory().createAttributeMap();
				IAttributeMap bussiness_attributes = PIMRuntime.getInstance().getCallerFactory().createAttributeMap();

				List private_phones = new ArrayList();
				List bussiness_phones = new ArrayList();
				String[] lines = entries[i].split(IJAMConst.CRLF);
				// parse single entry
				String line = null;
				String[] value = null;
				for (int j=0;j<lines.length;j++) {
					line = lines[j];
					// check name
					if (line.toLowerCase().startsWith(N) || line.toLowerCase().startsWith(N2)) {
						value = line.substring(line.indexOf(":")+1).split(";");
						if (value.length>=2) {
							this.m_logger.info("Set attribute name.");
							private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, value[0]));
							bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, value[0]));
							private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, value[1]));
							bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME, value[1]));
						}
						if (value.length==1) {
							private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, value[0]));
							bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_LASTNAME, value[0]));
						}
					}
					if (line.toLowerCase().startsWith(ORG) || line.toLowerCase().startsWith(ORG2)) {
						value = line.split(":");
						if (value.length>=2) {
							this.m_logger.info("Set attribute organization.");
							private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL, value[1]));
							bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL, value[1]));
						}
					}					
					if (line.toLowerCase().startsWith(ADR)) {
						String[] tokens = line.split(":");
						if (tokens.length==2) {
							boolean isBusiness = false;
							String[] attributes = tokens[0].split(";");
							for (int k=0;k<attributes.length;k++) {
								if (attributes[k].toLowerCase().startsWith(ATTRIBUTE_TYPE)) {
									String[] values = attributes[k].toLowerCase().substring(ATTRIBUTE_TYPE.length()).split(";");
									for (int l=0;l<values.length;l++) {
										if (values[l].equalsIgnoreCase(ADR_TYPES[1])) {
											isBusiness = true;
										}
									}
								}
								if (attributes[k].toLowerCase().startsWith(ADR_TYPES[1])) {
									isBusiness = true;
								}
							}
							
							value = tokens[1].split(";");
							if (value.length>=6 && (value.length>6 ? value[6].trim().length()>0 : true) && value[3].trim().length()>0) {
								if (isBusiness) {
									this.m_logger.info("Set attribute work address.");
									bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET, value[2]));
									bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, value[5]));
									bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CITY, value[3]+ (value[4].trim().length()>0 ? ", "+value[4]: "")));
									bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY, (value.length>6 ? value[6]: "")));
								} else {
									this.m_logger.info("Set attribute home address.");
									private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STREET, value[2]));
									private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE, value[5]));
									private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CITY, value[3]+ (value[4].trim().length()>0 ? ", "+value[4]: "")));
									private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_COUNTRY, (value.length>6 ? value[6]: "")));
								}
							}
						}
					}
					if (line.toLowerCase().startsWith(TEL)) {
						value = line.split(":");											
						if (value.length==2 && value[1].trim().length()>0) {
							boolean isBusiness = false;
							boolean isFax = false;
							boolean isCell = false;
							String[] attributes = value[0].split(";");
							for (int k=0;k<attributes.length;k++) {
								if (attributes[k].toLowerCase().startsWith(ATTRIBUTE_TYPE)) {
									String[] values = attributes[k].toLowerCase().substring(ATTRIBUTE_TYPE.length()).split(",");
									for (int l=0;l<values.length;l++) {
										if (values[l].equalsIgnoreCase(TEL_TYPES[1])) {
											isBusiness = true;
										}
										if (values[l].equalsIgnoreCase(TEL_TYPES[3])) {
											isFax = true;
										}
										if (values[l].equalsIgnoreCase(TEL_TYPES[2])) {
											isCell = true;
										}
									}
								}
								if (attributes[k].toLowerCase().startsWith(TEL_TYPES[1])) {
									isBusiness = true;
								}
								if (attributes[k].toLowerCase().startsWith(TEL_TYPES[3])) {
									isFax = true;
								}
								if (attributes[k].toLowerCase().startsWith(TEL_TYPES[2])) {
									isCell = true;
								}
							}
							
							
							try {
								IPhonenumber pn = parsePhonenumber(value[1]);
								if (pn!=null && !bussiness_phones.contains(pn)) {
									if (isBusiness) {
										this.m_logger.info("Set attribute work");
										bussiness_phones.add(pn);
										bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), (isFax ? IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE : (isCell ? IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE : IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE))));
									} else {
										this.m_logger.info("Set attribute home");
										private_phones.add(pn);
										private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NUMBER_TYPE+pn.getTelephoneNumber(), (isFax ? IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE : (isCell ? IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE : IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE))));
									}
								}
							} catch (Exception ex) {
								this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
							}							
						}
					}	
					if (line.toLowerCase().startsWith(EMAIL)) {
						value = line.split(":");											
						if (value.length==2 && value[1].trim().length()>0) {
							boolean isBusiness = false;
							String[] attributes = value[0].split(";");
							for (int k=0;k<attributes.length;k++) {
								if (attributes[k].toLowerCase().startsWith(ATTRIBUTE_TYPE)) {
									String[] values = attributes[k].toLowerCase().substring(ATTRIBUTE_TYPE.length()).split(",");
									for (int l=0;l<values.length;l++) {
										if (values[l].equalsIgnoreCase(EMAIL_TYPES[1])) {
											isBusiness = true;
										}
									}
								}
								if (attributes[k].toLowerCase().startsWith(EMAIL_TYPES[1])) {
									isBusiness = true;
								}
							}
							if (isBusiness) {
								this.m_logger.info("Set attribute work");
								bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL, value[1]));
							} else {
								this.m_logger.info("Set attribute home");
								private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_EMAIL, value[1]));
							}					
						}
					}	
					if (line.toLowerCase().startsWith(GEO)) {
						value = line.split(":");
						if (value.length==2) {
							value = value[1].split(";");
							if (value.length==2) {
								bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT, value[0]));
								bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG, value[1]));
								private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT, value[0]));
								private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG, value[1]));
							}
						}
						
					}
					if (line.toLowerCase().startsWith(PHOTO) || line.toLowerCase().startsWith(MS_CARDPICTURE)) {
						value = new String[2];
						value[0] = line.substring(0, line.indexOf(":"));
						value[1] = line.substring(line.indexOf(":")+1);
						if (value.length==2) {
							boolean isUrl = false;
							boolean isEncoding = false;
							String img_type = null;
							String[] attributes = value[0].split(";");
							for (int k=0;k<attributes.length;k++) {
								if (attributes[k].toLowerCase().startsWith(PHOTO_TYPES[0]) || attributes[k].toLowerCase().startsWith(PHOTO_TYPES[1])) {
									isUrl = true;
								}
								if (attributes[k].toLowerCase().startsWith(PHOTO_TYPES[2])) {
									isEncoding = true;
								}
								if (attributes[k].toLowerCase().startsWith(PHOTO_TYPES[3])) {
									img_type = attributes[k].split("=")[1].toLowerCase();
								}
							}
							if (isUrl) {
								String img = getImageFromURL(value[1]);
								if (img!=null) {
									this.m_logger.info("Set attribute photo.");
									private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, PathResolver.getInstance().encode(img)));
									bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, PathResolver.getInstance().encode(img)));
									private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEURL, value[1]));
									bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEURL, value[1]));
								}
							} 
							if (isEncoding) {
								try {
									if (img_type==null || img_type.trim().length()==0) img_type = "jpg";
									StringBuffer img_content = new StringBuffer();
									img_content.append(value[1]);
									img_content.append("\n");
									do {
										j++;
										value[1]= lines[j];		
										if (value[1].startsWith(" ")) value[1] = value[1].substring(1);
										img_content.append(value[1]);
										img_content.append("\n");
									} while (value[1].indexOf("=")<0);
									
									String img = getImageFromBase64(img_content.toString(), img_type);
									if (img!=null) {
										this.m_logger.info("Set attribute photo.");
										private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, PathResolver.getInstance().encode(img)));
										bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH, PathResolver.getInstance().encode(img)));
										private_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEURL, value[1]));
										bussiness_attributes.add(PIMRuntime.getInstance().getCallerFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_IMAGEURL, value[1]));
									}
								} catch (Exception ex) {
									this.m_logger.log(Level.SEVERE, "VCF parsing error: PHOTO: "+ex.getMessage(), ex);
								}
							}
						}
					}				
				}
				
				// create ICaller objects
				IName name = PIMRuntime.getInstance().getCallerFactory().createName("", "");
				if (private_phones.size()>0 && private_attributes.size()>0) {					
					ICaller c = PIMRuntime.getInstance().getCallerFactory().createCaller(name, private_phones);
					c.setAttributes(private_attributes);
					cl.add(c);					
				}
				if (bussiness_phones.size()>0 && bussiness_attributes.size()>0) {					
					ICaller c = PIMRuntime.getInstance().getCallerFactory().createCaller(name, bussiness_phones);
					c.setAttributes(bussiness_attributes);
					cl.add(c);					
				}	
			} catch (Exception e) {
				this.m_logger.log(Level.SEVERE, "VCF parsing error: "+e.getMessage(), e);
				//throw new VcfParserException("parsing error: "+e.getMessage(), e);
			}
		}
		return cl;
	}
	
	private String getImageFromBase64(String base64Image, String type) {
		try {
			ByteArrayInputStream encodedIn = new ByteArrayInputStream(base64Image.getBytes());
			Base64Decoder b64 = new Base64Decoder(encodedIn);
			ByteArrayOutputStream decodedOut = new ByteArrayOutputStream();
			Stream.copy(b64, decodedOut);
			b64.close();
			decodedOut.close();
			
			ByteArrayInputStream decodedIn = new ByteArrayInputStream(decodedOut.toByteArray());
			return this.createImage(decodedIn, (new UUID().toString() + "." + type));	
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
	
	private String getImageFromURL(String url) {
		if (!url.toLowerCase().startsWith("http")) return null;
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
		File image_dir = new File(PathResolver.getInstance(PIMRuntime.getInstance()).getDataDirectory() + File.separator + "photos" + File.separator +"vcf-contacts");
		if (!image_dir.exists()) {
			image_dir.mkdirs();
		}
		
		File img = new File(image_dir, filename);
		FileOutputStream fos = new FileOutputStream(img);
		Stream.copy(bin, fos, true);
		return img.getAbsolutePath();
	}

	protected IPhonenumber parsePhonenumber(String n) {
		if (PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).isClired(n)) return null;

		IPhonenumber pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toInternalPhonenumber(PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).normalize(n.trim()));
		if (pn!=null) return pn;
			
		pn = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toIdentifiedPhonenumber(n.trim());
		if (pn != null) return pn;
		
		return null;
	}
	
	public int getCurrent() {
		return this.m_current;
	}
	
	public int getTotal() {
		return this.m_total;
	}
	
}
