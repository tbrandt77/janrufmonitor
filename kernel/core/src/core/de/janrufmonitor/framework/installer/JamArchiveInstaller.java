package de.janrufmonitor.framework.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class JamArchiveInstaller extends AbstractInstaller {

	private static String STORED_ARCHIVES = "installed-modules"+File.separator;
	private static String FAILED_ARCHIVES = "failed-modules"+File.separator;

	public String getExtension() {
		return InstallerConst.EXTENSION_ARCHIVE;
	}
	
	public Properties getDescriptor() {
		if (this.getFile()==null || this.getFile().isDirectory()) {
			this.m_logger.severe("Installation file is not valid: "+this.getFile());
			return null;
		}
		
		Properties descriptor = null;
		JamArchive p = new JamArchive(this.getFile());
		try {
			p.open();
			
			descriptor = p.getDescritpor();

			p.close();
		} catch (JamArchiveException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return descriptor;
	}	
	
	public Properties getDescriptor(String key) {
		if (key==null) {
			this.m_logger.severe("Key is invalid: "+key);
			return null;
		}
		
		Properties descriptor = null;
		
		File directory = new File(this.getStoreDirectory(), key);
		if (directory.exists() && directory.isDirectory()) {
			File[] archives = directory.listFiles(new FilenameFilter() {
				public boolean accept(File p, String name) {
					return name.toLowerCase().endsWith(InstallerConst.EXTENSION_ARCHIVE);
				}
				
			});

			if (archives==null || archives.length==0) {
				this.m_logger.info("No archives installed for namespace: "+key);
				return null;
			}
			
			List l = new ArrayList(Arrays.asList(archives));
			if (l.size()>1) {
				this.m_logger.warning("There are more than one archives installed for namespace: "+key);
				
				Collections.sort(l, new Comparator() {
					public int compare(Object o1, Object o2) {
						File f1 = (File) o1;
						File f2 = (File) o2;
						
						return (int) (f2.lastModified()-f1.lastModified());
					}}
				);
				for (int i=l.size()-1;i>0;i--) {
					File f = (File) l.remove(i);
					this.m_logger.info("Deleting "+f.getAbsolutePath());
					if (!f.delete()) {
						f.deleteOnExit();
					}
				}
			}
			int i = 0;
			JamArchive p = null;
			while (descriptor==null && i<l.size()) {
				p = new JamArchive((File) l.get(i));
				try {
					p.open();
					
					descriptor = p.getDescritpor();
		
					p.close();
				} catch (JamArchiveException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				i++;
			}
		}
		return descriptor;
	}	

	public String install(boolean overwrite) throws InstallerException {
		if (this.getFile()==null || this.getFile().isDirectory()) {
			throw new InstallerException("Installation file is not valid: "+this.getFile());
		}
		
		String namespace = this.getNamespace(this.getFile() );

		// cleanup old archives, rename them
		File dir = new File(this.getStoreDirectory() + File.separator + namespace);
		if (dir.exists()) {
			File[] files = dir.listFiles();
			for (int i=0;i<files.length;i++) {			
				if (files[i].getAbsolutePath().endsWith(this.getExtension())){
					if (!files[i].renameTo(new File(files[i].getAbsolutePath()+".bak"))) {
						this.m_logger.warning("File "+files[i].getAbsolutePath()+" could not be renamed to "+files[i].getAbsolutePath()+".bak");
						if (!files[i].delete()) files[i].deleteOnExit();
					}
				}
			}
		}

		File renamedArchive = new File(this.getStoreDirectory() + File.separator + namespace, this.getFile().getName());
		
		JamArchive p = new JamArchive(this.getFile());
		try {
			p.open();
			
			List l = p.getInitInfFiles();
			if (l.size()>0)
				this.handleAddInf(p, l, overwrite);
			
			l = p.getInfFiles();
			if (l.size()>0)
				this.handleAddInf(p, l, overwrite);
			
			l = p.getI18nFiles();
			if (l.size()>0)
				this.handleAddI18n(p, l);
			
			l = p.getDeleteFiles();
			if (l.size()>0)
				this.handleDeleteFiles(p, l);
			
			l = p.getLibFiles();
			if (l.size()>0)
				this.handleAddLib(p, l);

			// copy jnilib files to MacOS location for Oracle Java 8+ and MacOS X
			if (OSUtils.isMacOSX() && System.getProperty("java.specification.version").compareTo("1.8")>=0) {
				l = p.getJniLibFiles();
				if (l.size()>0)
					this.handleAddJniLib(p, l);
			}
			if (OSUtils.isMultiuserEnabled() && OSUtils.isMacOSX()) {
				l = p.getRootJarFiles();
				if (l.size()>0)
					this.handleAddRootJar(p, l);
			}
			
			l = p.getOtherFiles();
			if (l.size()>0)
				this.handleAddFiles(p, l);
			
			p.close();
		} catch (JamArchiveException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			renamedArchive = new File(this.getFailedDirectory() + File.separator + namespace, this.getFile().getName());
			renamedArchive.getParentFile().mkdirs();
			this.getFile().renameTo(renamedArchive);
			this.m_logger.info("Created new failed installation module.");
			throw new InstallerException("Installation failed for file: "+this.getFile()+", "+e.getMessage());
		}
		
		renamedArchive.getParentFile().mkdirs();
		try {
			FileOutputStream out = new FileOutputStream(renamedArchive);
			FileInputStream in = new FileInputStream(this.getFile());
			
			Stream.copy(in, out, true);
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}

		this.setFile(null);
		return namespace;
	}

	public boolean uninstall(String namespace) {
		File directory = new File(this.getStoreDirectory(), namespace);
		if (directory.exists() && directory.isDirectory()) {
			File[] archives = directory.listFiles(new FilenameFilter() {
				public boolean accept(File p, String name) {
					return name.toLowerCase().endsWith(InstallerConst.EXTENSION_ARCHIVE);
				}
				
			});
			for (int i=0,j=archives.length;i<j;i++) {
				JamArchive p = new JamArchive(archives[i]);
				try {
					p.open();
					
					List l = p.getInitInfFiles();
					this.handleRemoveInf(p, l);
					
					l = p.getInfFiles();
					this.handleRemoveInf(p, l);
					
					l = p.getI18nFiles();
					//this.handleRemoveI18n(p, l);
					
					l = p.getLibFiles();
					this.handleRemoveLib(p, l);

					// copy jnilib files to MacOS location for Orcale Javav 8+ and MacOS X
					if (OSUtils.isMacOSX() && System.getProperty("java.specification.version").compareTo("1.8")>=0) {
						l = p.getJniLibFiles();
						if (l.size()>0)
							this.handleRemoveJniLib(p, l);
					}
					if (OSUtils.isMultiuserEnabled() && OSUtils.isMacOSX()) {
						l = p.getRootJarFiles();
						if (l.size()>0)
							this.handleRemoveRootJar(p, l);
					}
					
					l = p.getOtherFiles();
					this.handleRemoveFiles(p, l);
					
					p.close();
				} catch (JamArchiveException e) {
					this.m_logger.warning(e.getMessage());
				}
				archives[i].delete();
			}
			directory.delete();
			return true;
		}
		return false;
	}
	
	private void handleAddFiles(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		FileHandler fh = new FileHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				if (content!=null)
					fh.addFile(content, entry);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleAddJniLib(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		JniLibHandler fh = new JniLibHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				if (content!=null)
					fh.addJniLib(content, entry);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleAddRootJar(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		RootJarHandler fh = new RootJarHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				if (content!=null)
					fh.addJar(content, entry);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleRemoveJniLib(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		JniLibHandler fh = new JniLibHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				if (content!=null)
					fh.removeJniLib(entry);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleRemoveRootJar(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		RootJarHandler fh = new RootJarHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				if (content!=null)
					fh.removeJar(entry);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleDeleteFiles(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		FileHandler fh = new FileHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				if (content!=null) {
					BufferedReader reader = new BufferedReader(
	                          new InputStreamReader(content) );
					for ( String line; (line = reader.readLine()) != null; ) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Deleting "+line+"...");
						fh.removeFile(line);
					}
					reader.close();
				}
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleRemoveFiles(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		FileHandler fh = new FileHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				if (content!=null) {
					fh.removeFile(entry);
				}
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleAddLib(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		LibHandler ih = new LibHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				Properties data = new Properties();
				data.load(content);
				content.close();
				ih.addLibData(data);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleRemoveLib(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		LibHandler ih = new LibHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				Properties data = new Properties();
				data.load(content);
				content.close();
				ih.removeLibData(data);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleAddI18n(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		I18nHandler ih = new I18nHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				Properties data = new Properties();
				data.load(content);
				content.close();
				ih.addI18nData(data);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}	
	}
	
	private void handleAddInf(JamArchive p, List l, boolean overwrite) {
		String entry = null;
		InputStream content = null;
		InfHandler ih = new InfHandler();
		ih.setOverwriteConfiguration(overwrite);
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				Properties data = new Properties();
				data.load(content);
				content.close();
				ih.addInfData(data);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	private void handleRemoveInf(JamArchive p, List l) {
		String entry = null;
		InputStream content = null;
		InfHandler ih = new InfHandler();
		for (int i=0,j=l.size();i<j;i++) {
			entry = (String) l.get(i);
			try {
				content = p.getStream(entry);
				Properties data = new Properties();
				data.load(content);
				content.close();
				ih.removeInfData(data);
			} catch (JamArchiveException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	private File getFailedDirectory() {
		return new File(PathResolver.getInstance(this.getRuntime()).getInstallDirectory() + FAILED_ARCHIVES);
	}
	
	private String getNamespace(File f) {
		JamArchive p = new JamArchive(f);
		String namespace = null;
		try {
			p.open();
			
			Properties descriptor = p.getDescritpor();
			if (descriptor!=null) {
				namespace = descriptor.getProperty(JamArchive.DESCRIPTOR_NAMESPACE);
			}
			
			p.close();
		} catch (JamArchiveException e) {
			this.m_logger.warning(e.getMessage());
		}
	
		return (namespace==null ? "none" : namespace);
	}

	protected String getStoreDirectoryName() {
		return JamArchiveInstaller.STORED_ARCHIVES;
	}

}
