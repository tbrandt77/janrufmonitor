package de.janrufmonitor.application;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.classloader.JamCacheMasterClassLoader;
import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.logging.LoggingInitializer;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.PIMRuntime;

public class RunUI64 {
    
    private static Logger m_logger;
       
    private static class PIMCommandLine {
    	   	
    	public static String[] getStartupCommands(String[] args) {
    		String commandArg = null;
    		for (int i=0;i<args.length;i++) {
    			if (args[i] != null && args[i].toLowerCase().startsWith("-c:")){
					commandArg = args[i].substring(3);
    			}
    		}
    		
    		if (commandArg!=null) {
    			StringTokenizer st = new StringTokenizer(commandArg, ",");
    			
				String[] sCommands = new String[st.countTokens()];
				int tc = 0;
				while (st.hasMoreTokens()) {
					sCommands[tc] = st.nextToken().trim();
					tc++;
				}
				return sCommands;
    		}
    		return new String[0];
    	}    
    	
    	public static String[] getStartupVMCommands() {
    		String commandArg = System.getProperty(IJAMConst.SYSTEM_UI_STARTCOMMAND, "");
    		
    		if (commandArg!=null && commandArg.length()>0) {
    			StringTokenizer st = new StringTokenizer(commandArg, ",");
    			
				String[] sCommands = new String[st.countTokens()];
				int tc = 0;
				while (st.hasMoreTokens()) {
					sCommands[tc] = st.nextToken().trim();
					tc++;
				}
				return sCommands;
    		}
    		return new String[0];
    	}    	
    }
    
    private static class Extractor {
    	private static final String JAR_FILE_PREFIX = "jar:file:";
    	private static final String FILE_PREFIX = "file:";
    	
    	public static void prepareFilesystem() {
    		if (!OSUtils.isMacOSX() || System.getProperty("java.specification.version").compareTo("1.8")<0) return;
    		
    		File workingDir = null;
    		
    		try {
    			String className = Extractor.class.getName().replace('.', File.separatorChar) + ".class";
    			ClassLoader classLoader = Extractor.class.getClassLoader();
    			String url = classLoader.getResource(className).toString();

    			
    			// remove encoding
    			url = StringUtils.replaceString(url, "%20", " ");

    			if (url.startsWith(JAR_FILE_PREFIX)) {
    				url = url.substring(JAR_FILE_PREFIX.length(), url.length() - className.length() - 2);
    				String temp = url.substring(0, url.lastIndexOf(File.separator) + 1);
    				if (temp.length() < 1) {
    					temp = url.substring(0, url.lastIndexOf("\\") + 1);
    				}
    				if (!temp.endsWith(File.separator)) {
    					temp += File.separator;
    				}

    				workingDir = new File(temp);
    			}
    			if (url.startsWith(FILE_PREFIX)) {
    				String cp = url.substring(FILE_PREFIX.length(), url.length() - className.length());
    				if (!cp.endsWith(File.separator)) {
    					cp += File.separator;
    				}
                    
    				workingDir = new File(cp);
    			}
    		} catch (Exception e) {
    			System.out.println(e);
    			return;
    		}
    		
    		// extract structure.zip if existing
    		try {
	    		if (workingDir!=null && workingDir.exists() && workingDir.isDirectory()) {
	    			File structureZip = new File(workingDir, "structure.zip");
	    			if (structureZip.exists() && structureZip.isFile()) {
	    				ZipFile szip = new ZipFile(structureZip);
	    				ZipEntry z = null;
	    				String entryname = null;
	    				File newfile = null;
	    				for (Enumeration e = szip.entries();e.hasMoreElements();) {
	    					try {
	    						z = (ZipEntry)e.nextElement();
	    						entryname =  z.getName();
	    						if (z.isDirectory()) {
	    							new File(workingDir, entryname).mkdir();
	    							newfile = null;
	    						} else {
	    							newfile = new File(workingDir, entryname);
	    							newfile.getParentFile().mkdirs();
	    						}
	    						if (newfile!=null) {
	    							InputStream in = szip.getInputStream(z);
	    							if (in!=null) {
	    								Stream.copy(in, new FileOutputStream(newfile), true);
	    							}
	    						}
	    					} catch (Throwable ex) {
	    						System.out.println(ex);
	    					}
	    				}
	    				szip.close();
	    				if (!structureZip.delete()) structureZip.deleteOnExit();
	    			}
	    		}
    		} catch (Exception e) {
    			System.out.println(e);
    			return;
    		}
    		
    		// copy /lib content to install dir
    		File lib = new File(PathResolver.getInstance().getLibDirectory());
    		if (lib!=null && lib.exists() && lib.isDirectory()) {
    			File [] files = lib.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File f, String name) {
						return name.endsWith(".jar");
					}});
    			for (int i=0;i<files.length;i++) {
    				try {
    					if (files[i]!=null && files[i].exists() && files[i].isFile()) {
    						Stream.copy(new FileInputStream(files[i]), new FileOutputStream(new File(PathResolver.getInstance().getInstallDirectory(), files[i].getName())), true);
    						if (!files[i].delete()) files[i].deleteOnExit();
    					}
    				} catch (Exception e) {
    					System.out.println(e);
    				}
    			}
    		}	
    	}
    }
    
    public static void go() {
		try {
			RunUI64.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
			RunUI64.m_logger.info("Starting ...");
			RunUI64.m_logger.info("Running version " + IJAMConst.VERSION_DISPLAY + " (Build: "+IJAMConst.VERSION_BUILD+")");

			if (RunUI64.m_logger.isLoggable(Level.FINE)){
				Properties env = System.getProperties();
				Iterator iter = env.keySet().iterator();
				String key = null;
				RunUI64.m_logger.fine("Reading Java runtime environment...");
				while (iter.hasNext()) {
					key = (String) iter.next();
					System.out.println(key + " = "+env.getProperty(key));
					RunUI64.m_logger.fine(key + " = "+env.getProperty(key));
				}
			}

			boolean isJavaVersionOK = false;
			String javaversion = System.getProperty("java.specification.version");
			if ((javaversion!=null && javaversion.compareTo("1.8")>=0) ) isJavaVersionOK = true;
			
			javaversion = ((System.getProperty("java.version").length()==1 || System.getProperty("java.version").length()==2)? System.getProperty("java.version") : System.getProperty("java.version").substring(0,3));
			if ((!isJavaVersionOK && javaversion.compareTo("1.8")>=0) ) isJavaVersionOK = true;
			
			if (!isJavaVersionOK) {
				Thread t = new Thread () {
					public void run () {
						Display.getDefault().syncExec(
							new Runnable () {
								public void run () {
									Shell shell = new Shell(Display.getDefault());
									shell.setSize(0,0);
									int style = SWT.APPLICATION_MODAL | SWT.OK | SWT.WRAP;
									MessageBox messageBox = new MessageBox (shell, style);
									String lang = System.getProperty("user.language");
									if (lang==null) lang = "de";									
									messageBox.setMessage (lang.equalsIgnoreCase("de") ? "jAnrufmonitor kann nicht gestartet werden, da Java nur in Version "+System.getProperty("java.specification.version")+" installiert ist. Es wird jedoch mindestens Java Version 1.8 ben\u00F6tigt." : "jAnrufmonitor wrong Java version.");
									messageBox.setText(lang.equalsIgnoreCase("de") ? "jAnrufmonitor - Fehler beim Programmstart": "jAnrufmonitor Error...");
									if (messageBox.open () == SWT.OK) {
										RunUI64.m_logger.severe("Emergency exit: Invalid Java Version: "+System.getProperty("java.specification.version"));
										System.exit(0);
									}
								}
							}
						);
					}
				};
				t.setName("JAM-Check-Java-Thread-(non-deamon)");
				t.start();
				
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					RunUI64.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
		
				RunUI64.m_logger.severe("Emergency exit: Invalid Java Version: "+System.getProperty("java.specification.version"));
				System.exit(0);
			}

			// set Jam classloader
			if (JamCacheMasterClassLoader.getInstance().isValid()) {
				//Thread.currentThread().setContextClassLoader(JamCacheMasterClassLoader.getInstance());
				if (RunUI64.m_logger.isLoggable(Level.INFO))
					RunUI64.m_logger.info("Set new context classloader...");
				PIMRuntime.getInstance().startup();
			
				if (RunUI64.m_logger.isLoggable(Level.FINE))
					PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).analyze();
				
				RunUI64.registerShutdownHook();
				
			} else {
				Thread thread = new Thread () {
					public void run () {
						Display.getDefault().syncExec(
							new Runnable () {
								public void run () {
									Shell shell = new Shell(Display.getDefault());
									shell.setSize(0,0);
									int style = SWT.APPLICATION_MODAL | SWT.OK | SWT.WRAP;
									MessageBox messageBox = new MessageBox (shell, style);
									String lang = System.getProperty("user.language");
									if (lang==null) lang = "de";									
									messageBox.setMessage (lang.equalsIgnoreCase("de") ? "jAnrufmonitor kann nicht erneut gestartet werden. Er ist bereits gestartet und kann nur einmal ausgef\u00FChrt werden." : "jAnrufmonitor already running.");
									messageBox.setText(lang.equalsIgnoreCase("de") ? "jAnrufmonitor - Fehler beim Programmstart": "jAnrufmonitor Error...");
									if (messageBox.open () == SWT.OK) {
										RunUI64.m_logger.severe("Emergency exit: jAnrufmonitor already running! Failed to initialize JamCacheMasterClassLoader. Aborting startup. Make sure that only one instance of jAnrufmonitor is running and clear all files from folder: %jam-installpath%/lib/cache.");							
										System.exit(0);
									}
								}
							}
						);
					}
				};
				thread.setName("JAM-ShutdownNotifier-Thread-(non-deamon)");
				thread.start();

				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					RunUI64.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
		
				RunUI64.m_logger.severe("Emergency exit: jAnrufmonitor already running! Failed to initialize JamCacheMasterClassLoader. Aborting startup. Make sure that only one instance of jAnrufmonitor is running and clear all files from folder: %jam-installpath%/lib/cache.");
				System.exit(0);
			}

		} catch (Exception ex) {
			RunUI64.m_logger.severe("Emergency exit: Unexpected error: " + ex.toString() +": "+ex.getMessage()+" - "+ex.getCause());
			RunUI64.m_logger.severe("Program terminated unexpected. Please check your Java installation and jAnrufmonitor configuration for problems.");

			RunUI64.dumpException(ex);
			PropagationFactory.getInstance().fire(new Message(ex));
			
			System.out.println("Emergency exit: Unexpected error: " + ex.toString() +": "+ex.getMessage()+" - "+ex.getCause());
			System.exit(0);
		} 
    }
    
    public static void quit() {
		RunUI64.m_logger.info("Stopping ...");
		PIMRuntime.getInstance().shutdown();
    }
    
    private static void dumpException(Exception ex){
    	try {
	    	String dumpPath = PathResolver.getInstance(PIMRuntime.getInstance()).getLogDirectory()+"_fatal_error.dmp";
	    	FileOutputStream os = new FileOutputStream(dumpPath);
	    	PrintStream ps = new PrintStream(os);
	    	ex.printStackTrace(ps);
	    	ps.flush();
	    	ps.close();
    	} catch (FileNotFoundException e) {
    		RunUI64.m_logger.severe("--> DUMPING ERROR: " + e.toString() +": "+e.getMessage()+" - "+e.getCause());
		}
    }
    
    private static void registerShutdownHook() {
		// register shutdown Hook		
		String value = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, "shutdownhook");
		if (value!=null && value.length()>0) {
			if (value.equalsIgnoreCase("false")) {
				RunUI64.m_logger.info("ShutdownHook registration ignored.");
				return;
			}	
		}
		Runtime.getRuntime().addShutdownHook(new ShutDownHook());
		RunUI64.m_logger.info("ShutdownHook successfully registered.");
    }

	private static void processStartupCommand(String[] commands) {
		if (commands.length==0) return;

		for (int i=0;i<commands.length;i++) {
			RunUI64.startCommandThread(commands[i]);
		}
	}
	
	private static void startCommandThread(final String command) {
		Thread t = new Thread(
			new Runnable() {
				public void run() {
					ICommand c = PIMRuntime.getInstance().getCommandFactory().getCommand(command);
					if (c!=null && c.isExecutable()) {
						RunUI64.m_logger.info("Start executing command <"+c.getID()+">.");
						if (!c.isExecuting()) {
							try {
								c.execute();
							} catch (Exception e) {
								m_logger.severe(e.getMessage());
							}
						} else {
							RunUI64.m_logger.info("Command <"+c.getID()+"> already executing.");
						}
					} else {
						RunUI64.m_logger.warning("Command does not exist.");
					}
				}
			}
		);
		t.setName("JAM-StartupExecuter#"+command+"-Thread-(non-deamon)");
		t.start();
	}

    public static void main(String[] args) { 
    	// added 2016/01/05: Display under Cocoa must be initialized with main-Thread
    	if (OSUtils.isMacOSX()) {
    		Display.setAppName("jAnrufmonitor");
    		Display.getDefault();
    		
    		//if (SWT.getPlatform().equals("cocoa")) {
    		//	new CocoaUIEnhancer().earlyStartup();
    		//}

			Extractor.prepareFilesystem();
    	}
    	
    	final Runnable error = new Runnable () {
			public void run () {
				Shell shell = new Shell(Display.getDefault());
				shell.setSize(0,0);
				int style = SWT.APPLICATION_MODAL | SWT.OK | SWT.WRAP;
				MessageBox messageBox = new MessageBox (shell, style);
				String lang = System.getProperty("user.language");
				if (lang==null) lang = "de";									
				messageBox.setMessage (lang.equalsIgnoreCase("de") ? "jAnrufmonitor muss neu gestartet werden, da eine Aktualisierung durchgef\u00FChrt wurde." : "jAnrufmonitor needs to be restarted due to an update of the core component.");
				messageBox.setText(lang.equalsIgnoreCase("de") ? "jAnrufmonitor - Aktualisierung erfolgreich": "jAnrufmonitor - Update successfully installed");
				if (messageBox.open () == SWT.OK) {
					RunUI64.m_logger.warning("Emergency restart: jAnrufmonitor needs to be restarted due to changes of the core component.");
		    		PIMRuntime.getInstance().shutdown();
					JamCacheMasterClassLoader.invalidateInstance();
					System.exit(0);
				}
			}
		};
		
    	Thread thread = new Thread () {
			public void run () {
				Display.getDefault().syncExec(
					error
				);
			}
		};
		thread.setName("JAM-EmergencyShutdown-Thread-(non-deamon)");
		LoggingInitializer.run();

		final String[] args2 = args;
    	try {
    		Thread mainT = new Thread(new Runnable(){

				public void run() {
		    		RunUI64.go();
		    		RunUI64.processStartupCommand(PIMCommandLine.getStartupCommands(args2));
		    		RunUI64.processStartupCommand(PIMCommandLine.getStartupVMCommands());
				}
    			
    		});
    		mainT.setName("JAM-MAIN-THREAD");
    		mainT.start();

    		
    		try {
				Class clazz = Class.forName("de.janrufmonitor.ui.swt.DisplayManager");
				Method m = clazz.getDeclaredMethod("getDefaultDisplay");
				if (m!=null)
					m.invoke(null);
			} catch (ClassNotFoundException e) {
				System.exit(0);
			} catch (SecurityException e) {
				System.exit(0);
			} catch (NoSuchMethodException e) {
				System.exit(0);
			} catch (IllegalArgumentException e) {
				System.exit(0);
			} catch (IllegalAccessException e) {
				System.exit(0);
			} catch (InvocationTargetException e) {
				System.exit(0);
			}
    	} catch (Error e) {    
    		String isInstallerRun = System.getProperty(IJAMConst.SYSTEM_INSTALLER_RUN);
    		if (isInstallerRun!=null && isInstallerRun.equalsIgnoreCase("true")) {
    			thread.start();			
    		} else {
    			System.out.println("Emergency exit: Unexpected error: " + e.toString() +": "+e.getMessage()+" - "+e.getCause());
    			System.exit(0);
    		}
		}
    }   
}
