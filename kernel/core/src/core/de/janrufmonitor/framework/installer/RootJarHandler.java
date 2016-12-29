package de.janrufmonitor.framework.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class RootJarHandler {

	private Logger m_logger;

	public RootJarHandler() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
	}

	public void addJar(InputStream in, String path) {
		File file = new File(new File(PathResolver.getInstance(PIMRuntime.getInstance()).getAppRoot()), path);
		file.getParentFile().mkdirs();
		try {
			Stream.copy(in, new FileOutputStream(file), true);
		} catch (FileNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (IOException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void removeJar(String path) {
		File file = new File(new File(PathResolver.getInstance(PIMRuntime.getInstance()).getAppRoot()), path);
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info("Deleting "+file.getAbsolutePath()+"...");
		if (file.exists()) 
			if (!file.delete()) {
				this.m_logger.info("Deleting "+file.getAbsolutePath()+" during application run failed. Delete on exit is triggered.");
				file.deleteOnExit();
			}
	}

}
