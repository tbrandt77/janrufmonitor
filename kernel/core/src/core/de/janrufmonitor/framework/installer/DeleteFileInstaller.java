package de.janrufmonitor.framework.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class DeleteFileInstaller extends AbstractInstaller {

	private static String STORED_ARCHIVES = "installed-modules"+File.separator+"~delete"+File.separator;
	
	public String getExtension() {
		return DeleteFileInstaller.EXTENSION_DELETE;
	}

	public int getPriority() {
		return 20;
	}

	public String install(boolean overwrite) throws InstallerException {
		if (this.getFile().exists()) {
			try {
				InputStream content =new FileInputStream(this.getFile());
				FileHandler fh = new FileHandler();
				if (content!=null) {
					BufferedReader reader = new BufferedReader(
	                          new InputStreamReader(content) );
					for ( String line; (line = reader.readLine()) != null; ) {
						fh.removeFile(line);
					}
					reader.close();
				}
			} catch (IOException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			
			File renamed = new File(this.getStoreDirectory(), this.getFile().getName());
			renamed.getParentFile().mkdirs();
			this.getFile().renameTo(renamed);
		}
		return null;
	}

	protected String getStoreDirectoryName() {
		return DeleteFileInstaller.STORED_ARCHIVES;
	}

}
