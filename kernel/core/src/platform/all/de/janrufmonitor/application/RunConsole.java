package de.janrufmonitor.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.classloader.JamCacheMasterClassLoader;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.framework.command.IConsoleCommand;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.logging.LoggingInitializer;
import de.janrufmonitor.runtime.PIMRuntime;

public class RunConsole implements IEventSender, IEventReceiver {
	
	private static String NAMESPACE = "application.Run";
	
	Logger m_logger;
	static RunConsole application;
	static boolean started;
	
	public void init() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_logger.info("Starting ...");
		
		try {
			IEventBroker evtBroker = PIMRuntime.getInstance().getEventBroker();
			evtBroker.register(this);
			evtBroker.register(this, evtBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		} catch (NullPointerException e) {
			System.out.println("WARNING: Registering for call events failed. Please restart jAnrufmonitor...");
			System.out.println();
		}
	}

	public static void main(String[] args) {
		LoggingInitializer.run();
		
		Thread.currentThread().setContextClassLoader(JamCacheMasterClassLoader.getInstance());

		System.setProperty("jam.console", "true");
		
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("Welcome to jAnrufmonitor 5 Console:");
		System.out.println("===================================");
		System.out.println("");
		
		try {
			start();
			help();
			prompt();
		} catch (NoClassDefFoundError e) {
			System.out.println("Updates have been installed. jAnrufmonitor needs to be restarted.");
			System.out.println("... have a nice day :-)");
			System.exit(0);
		}
		while (true) {
			try {
				String lineToBeSent = input.readLine();
				if (lineToBeSent.equalsIgnoreCase("quit")) {
					PIMRuntime.getInstance().shutdown();
					System.out.println("... have a nice day :-)");
					System.exit(0);
				} else if (lineToBeSent.toLowerCase().startsWith("help")) {
					help();
				} else if (lineToBeSent.toLowerCase().startsWith("restart")) {
					restart();
				}  else {
					StringTokenizer st = new StringTokenizer(lineToBeSent, " ");
					
					if (st.countTokens()>0){
						String command = st.nextToken().toLowerCase();
						String[] arg = null;
						if (st.hasMoreTokens()) {
							int i=0;
							arg = new String[st.countTokens()];
							while (st.hasMoreTokens()) {
								arg[i] = st.nextToken();
								i++;
							}
						}
						ICommand cmd = PIMRuntime.getInstance().getCommandFactory().getCommand(command);
						if (cmd!=null && cmd instanceof IConsoleCommand) {
							((IConsoleCommand) cmd).setExecuteParams(arg);
							if (cmd.isExecutable() && !cmd.isExecuting()) {
								try {
									cmd.execute();
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else {
								System.out.println("WARNING: Command "+lineToBeSent+ " already executing. Please be patient.");
							}
						} else {
							System.out.println("ERROR: Command "+lineToBeSent+ " not found.");
						}
					}
				}
				prompt();
			} catch (IOException ex) {
				System.out.println("ERROR: " + ex.getMessage());
			}
		}
	}

	private static void restart() {
		System.out.println("Shutting down jAnrufmonitor ...");
		try {
			PIMRuntime.getInstance().shutdown();	
		} catch (NullPointerException e) {
		}
		System.out.println("Re-starting jAnrufmonitor ...");
		start();
	}

	public static void start() {
		System.out.println("Starting jAnrufmonitor ...");
		if (JamCacheMasterClassLoader.getInstance().isValid()) {
			PIMRuntime.getInstance().startup();

			application = new RunConsole();
			application.init();
		} else {
			System.out.println("Updating all files...");
			System.out.println("Done.");
			System.out.println("Update successfully processed. Please restart jAnrufmonitor...");
			System.exit(0);
		}
	}
	
	
	public static void help() {
		System.out.println("Configuration must be done in janrufmonitor.properties.");
		System.out.println("");
		System.out.println("Help                - HELP + <ENTER>");

		String commands = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(
			RunConsole.NAMESPACE,
			"commands"
		);
		
		StringTokenizer st = new StringTokenizer(commands, ",");
		while (st.hasMoreTokens()) {
			try {
				ICommand cmd = PIMRuntime.getInstance().getCommandFactory().getCommand(st.nextToken().trim());
				if (cmd!=null && cmd instanceof IConsoleCommand && cmd.getLabel()!=null) {
					System.out.println(cmd.getLabel());
				}
			} catch (NullPointerException e) {	
			}			
		}
		System.out.println("Re-start            - RESTART + <ENTER>");
		System.out.println("Quit                - QUIT + <ENTER>");
		System.out.println("");
	}
	
	public static void prompt() {
		System.out.print("JAM>");  
	}

	public String getSenderID() {
		return "Run";
	}

	public int getPriority() {
		return 1;
	}

	public void received(IEvent event) {
		if (event.getType()==IEventConst.EVENT_TYPE_IDENTIFIED_CALL) {
			System.out.println("");
			System.out.println("---> new incoming call ...");
			ICall c = (ICall)event.getData();
			System.out.println("---> Called MSN: "+c.getMSN().toString());
			System.out.println("---> Calling number: "+c.getCaller().getPhoneNumber().toString());
			System.out.println("---> Caller name: "+c.getCaller().getName().toString().trim());
			System.out.println("---> Service (CIP): "+c.getCIP().getAdditional());
			System.out.println("");
			prompt();
		}
	}

	public String getReceiverID() {
		return "RunConsole";
	}

}
