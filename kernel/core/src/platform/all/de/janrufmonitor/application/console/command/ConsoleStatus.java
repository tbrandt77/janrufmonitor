package de.janrufmonitor.application.console.command;

import java.util.List;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.framework.command.AbstractConsoleCommand;

public class ConsoleStatus extends AbstractConsoleCommand {

	private String ID = "status";
	private String NAMESPACE = "application.console.command.ConsoleStatus";
	
	private IRuntime m_runtime;

	public IRuntime getRuntime() {
		if (m_runtime==null)
			m_runtime = PIMRuntime.getInstance();
		return m_runtime;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public void execute() {
		System.out.println("Status of jAnrufmonitor:");
		System.out.println("---------------------------");
		System.out.println("");
		// TODO: 2005/09/03: Work-a-round for 4.3.x, to be changed !
		if (PIMRuntime.getInstance().getMonitorListener().getDefaultMonitor()==null || !PIMRuntime.getInstance().getMonitorListener().getDefaultMonitor().isStarted()) {
			System.out.println("jAnrufmonitor is stopped.");
			System.out.println("");
			return;
		}
		
		System.out.println("jAnrufmonitor is started.");
		System.out.println("");
		
		// services
		System.out.println("---");
		System.out.println("Running services: ");
		String[] services = PIMRuntime.getInstance().getServiceFactory().getAllServiceIDs();
		for (int i=0;i<services.length-1;i++){
			System.out.print(services[i]+", ");
		}
		System.out.print(services[services.length-1]);
		System.out.println("");
		
		// rules
		List rules = PIMRuntime.getInstance().getRuleEngine().getRules();
		System.out.println("---");
		System.out.println("Rules: ");
		for (int i=0;i<rules.size()-1;i++) {
			System.out.print(rules.get(i)+ ", ");
		}
		System.out.print(rules.get(rules.size()-1));
		System.out.println("");
		
		// caller managers
		System.out.println("---");
		System.out.println("Running caller managers: ");
		String[] cm = PIMRuntime.getInstance().getCallerManagerFactory().getAllCallerManagerIDs();
		for (int i=0;i<cm.length-1;i++){
			System.out.print(cm[i]+", ");
		}
		System.out.print(cm[cm.length-1]);
		System.out.println("");
		
		//	call managers
		System.out.println("---");
		System.out.println("Running call managers: ");
		cm = PIMRuntime.getInstance().getCallManagerFactory().getAllCallManagerIDs();
		for (int i=0;i<cm.length-1;i++){
			System.out.print(cm[i]+", ");
		}
		System.out.print(cm[cm.length-1]);
		System.out.println("");
		
		// monitor listener
		System.out.println("---");
		System.out.println("Monitor listener active: "+PIMRuntime.getInstance().getMonitorListener().isEnabled());
		System.out.println("");
	}

	public boolean isExecutable() {
		return true;
	}

	public boolean isExecuting() {
		return false;
	}

	public String getID() {
		return this.ID;
	}

	public String getLabel() {
		return "Status              - STATUS + <ENTER>";
	}

}
