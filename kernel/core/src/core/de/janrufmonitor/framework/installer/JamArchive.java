package de.janrufmonitor.framework.installer;

import java.io.*;
import java.util.List;
import java.util.Properties;

import de.janrufmonitor.repository.zip.ZipArchive;
import de.janrufmonitor.repository.zip.ZipArchiveException;
import de.janrufmonitor.util.io.Stream;

public class JamArchive implements InstallerConst {
	
	private static String DESCRIPTOR = "~meta-inf/descriptor.properties";

	private ZipArchive m_zip;
	
	public JamArchive(File archive) {
		this.m_zip = new ZipArchive(archive, false);
	}
	
	public JamArchive(String archive) {
		this(new File(archive));
	}
	
	public void open() throws JamArchiveException {
		if (!this.m_zip.available())
			try {
				this.m_zip.open();
			} catch (ZipArchiveException e) {
				throw new JamArchiveException(e);
			}
	}
	
	public boolean isOpen() {
		return (this.m_zip!=null ? this.m_zip.available() : false);
	}

	public Properties getDescritpor() throws JamArchiveException {
		if (!this.m_zip.available())
			throw new JamArchiveException("JamArchive not opened.");
		
		Properties p = null;
		try {
			List l = this.m_zip.list(new FilenameFilter() {
				public boolean accept(File p, String entry) {
					return entry.toLowerCase().endsWith(JamArchive.DESCRIPTOR);
				}
			});
			
			if (l.size()==1) {
				InputStream in = this.m_zip.get((String) l.get(0));
				if (in!=null) {
					BufferedInputStream bin = new BufferedInputStream(in);
					p = new Properties();
					p.load(bin);
					bin.close();
					return p;
				}
			} else if (l.size()>1) {
				throw new JamArchiveException("More than 1 descriptor is not allowed in a *.jam.zip archive.");
			}
		} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		} catch (IOException e) {
			throw new JamArchiveException(e.getMessage());
		}
		return p;
	}
	
	public void close() throws JamArchiveException {
		try {
			this.m_zip.close();
		} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}

	public void extractTo(List entries, File directory) throws JamArchiveException {
		if (!this.m_zip.available())
			throw new JamArchiveException("JamArchive not opened.");

		if (!directory.exists()) {
			directory.mkdirs();
		}
		
		if (entries==null || entries.size()==0) return;
		
		String entry = null;
		byte[] content = null;
		File targetFile = null;
		FileOutputStream tragetStream = null; 
		ByteArrayInputStream contentStream = null;
		for (int i=0,j=entries.size();i<j;i++) {
			try {
				entry = (String) entries.get(i);
				content = this.m_zip.getContent(entry);
				if (content!=null) {
					// create output file
					targetFile = new File(directory.getAbsolutePath() + File.separator + entry);
					// create directory structure if neccessary
					targetFile.getParentFile().mkdirs();
					tragetStream = new FileOutputStream(targetFile);
					contentStream = new ByteArrayInputStream(content);
					Stream.copy(contentStream, tragetStream, true);

				}
			} catch (ZipArchiveException e){
				throw new JamArchiveException(e);
			} catch (FileNotFoundException e) {
				throw new JamArchiveException(e.getMessage());
			} catch (IOException e) {
				throw new JamArchiveException(e.getMessage());
			}
		}
	}
	
	public void extractTo(File directory) throws JamArchiveException {
		if (!this.m_zip.available())
			throw new JamArchiveException("JamArchive not opened.");

		try {
			this.extractTo(this.m_zip.list(), directory);
		} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}
	
	public List getI18nFiles() throws JamArchiveException {
		try {
			return
				this.m_zip.list(new FilenameFilter() {
					public boolean accept(File p, String entry) {
						return entry.toLowerCase().endsWith(JamArchive.EXTENSION_I18N);
					}
				});
			} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}
	
	public List getInfFiles() throws JamArchiveException {
		try {
			return
				this.m_zip.list(new FilenameFilter() {
					public boolean accept(File p, String entry) {
						return (entry.toLowerCase().endsWith(JamArchive.EXTENSION_INF) && !entry.toLowerCase().endsWith(JamArchive.EXTENSION_INITINF));
					}
				});
			} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}	
	
	public List getInitInfFiles() throws JamArchiveException {
		try {
			return
				this.m_zip.list(new FilenameFilter() {
					public boolean accept(File p, String entry) {
						return (entry.toLowerCase().endsWith(JamArchive.EXTENSION_INITINF));
					}
				});
			} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}	
	
	public List getLibFiles() throws JamArchiveException {
		try {
			return
				this.m_zip.list(new FilenameFilter() {
					public boolean accept(File p, String entry) {
						return (entry.toLowerCase().endsWith(JamArchive.EXTENSION_LIB));
					}
				});
			} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}	
	
	public List getJarFiles() throws JamArchiveException {
		try {
			return
				this.m_zip.list(new FilenameFilter() {
					public boolean accept(File p, String entry) {
						return (entry.toLowerCase().endsWith(JamArchive.EXTENSION_JAR));
					}
				});
			} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}	
	
	public List getOtherFiles() throws JamArchiveException {
		try {
			return
				this.m_zip.list(new FilenameFilter() {
					public boolean accept(File p, String entry) {
						if (!entry.toLowerCase().endsWith(JamArchive.EXTENSION_I18N) &&
							!entry.toLowerCase().endsWith(JamArchive.EXTENSION_INF) &&
							!entry.toLowerCase().endsWith(JamArchive.EXTENSION_INITINF) &&
							!entry.toLowerCase().endsWith(JamArchive.EXTENSION_LIB) &&
							!entry.toLowerCase().startsWith(JamArchive.DESCRIPTOR)
						) return true;
						
		                return false;			
					}
				});
			} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}	
	
	public List getFiles(FilenameFilter fnf) throws JamArchiveException {
		try {
			return
				this.m_zip.list(fnf);
			} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}
	
	public byte[] getContent(String entry) throws JamArchiveException {
		try {
			return this.m_zip.getContent(entry);
		} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
	}
	
	public InputStream getStream(String entry) throws JamArchiveException {
		InputStream in = null;;
		try {
			in = this.m_zip.get(entry);
		} catch (ZipArchiveException e) {
			throw new JamArchiveException(e);
		}
		return (in==null ? null : new BufferedInputStream(in));
	}
	
	public String getFilename() {
		return (this.m_zip!=null ? this.m_zip.toFile().getName() : null);
	}

}
