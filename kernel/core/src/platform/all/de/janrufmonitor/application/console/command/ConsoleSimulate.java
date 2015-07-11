package de.janrufmonitor.application.console.command;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.framework.command.AbstractConsoleCommand;


public class ConsoleSimulate extends AbstractConsoleCommand implements IEventSender {

	private String ID = "simulate";
	private String NAMESPACE = "application.console.command.ConsoleSimulate";
	
	private IRuntime m_runtime;
	private boolean isExecuting; 
	
	public String getLabel() {
		return "Call Simulation     - SIMULATE <number> <msn> <cip> + <ENTER>";
	}
	
	public IRuntime getRuntime() {
		if (m_runtime==null)
			m_runtime = PIMRuntime.getInstance();
		return m_runtime;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public void execute() {
		this.isExecuting = true;
		
		String number = "";
		String cip = "";
		String msn = "";
		
		if (this.getExecuteParams().length<2 || this.getExecuteParams().length>3) {
			System.out.println("ERROR: Invalid SIMULATE command.");
			System.out.println("USAGE for regular call simulation: SIMULATE <number> <msn> <cip>");
			System.out.println("USAGE for CLIR call simulation: SIMULATE <msn> <cip>");
			this.isExecuting = false;
			return;
		}
		
		if (this.getExecuteParams().length==2) {
			msn = this.getExecuteParams()[0];
			cip = this.getExecuteParams()[1];
		}
		
		if (this.getExecuteParams().length==3) {
			number = this.getExecuteParams()[0];
			msn = this.getExecuteParams()[1];
			cip = this.getExecuteParams()[2];
		}
		
		IEventBroker evtBroker = PIMRuntime.getInstance().getEventBroker();

		IPhonenumber phone = PhonenumberAnalyzer.getInstance(getRuntime()).toClirPhonenumber(number);
		if (phone!=null) System.out.println("Call detected as CLIR: " + phone.isClired()); 
		
		if (phone==null) {
			phone = PhonenumberAnalyzer.getInstance(getRuntime()).toInternalPhonenumber(number, msn);
			if (phone!=null) System.out.println("Call detected as internal: " + number.trim());
		}
				
		if (phone==null) {
			phone = PhonenumberAnalyzer.getInstance(getRuntime()).toPhonenumber(number, msn);
			if (phone!=null) System.out.println("Call detected as external: " + number.trim());
		}

		IName name = PIMRuntime.getInstance().getCallerFactory().createName("","");
		ICaller aCaller = PIMRuntime.getInstance().getCallerFactory().createCaller(name, phone);
		ICip ocip = PIMRuntime.getInstance().getCallFactory().createCip(cip, "");
		IMsn omsn = PIMRuntime.getInstance().getCallFactory().createMsn(msn, "");
		ICall newCall = PIMRuntime.getInstance().getCallFactory().createCall(aCaller,omsn,ocip);
		
		evtBroker.register(this);
		IEvent ev = evtBroker.createEvent(IEventConst.EVENT_TYPE_INCOMINGCALL,newCall);
		evtBroker.send(this, ev); 
		evtBroker.unregister(this);

		this.isExecuting = false;
	}

	public boolean isExecutable() {
		return true;
	}

	public boolean isExecuting() {
		return this.isExecuting;
	}

	public String getID() {
		return this.ID;
	}

	public String getSenderID() {
		return this.ID;
	}

	public int getPriority() {
		return 0;
	}

}
