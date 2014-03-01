package de.janrufmonitor.framework.monitor;

import de.janrufmonitor.framework.*;
import de.janrufmonitor.framework.event.*;
import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.string.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public class MonitorListener extends AbstractMonitorListener implements IEventReceiver, IEventSender, IConfigurable {
    
	class MonitorActiveCheck extends Thread {
		
		IMonitorListener m_ml;
		boolean m_finished;
		
		public MonitorActiveCheck(IMonitorListener ml) {
			this.m_ml = ml;
		}
		
		public void run() {
			boolean atLeastOneRunning = false;
			do {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {}
				atLeastOneRunning = false;
				
				List monitors = m_ml.getMonitors();
				IMonitor m = null;
				for (int i=0;i<monitors.size();i++) {
					m = (IMonitor) monitors.get(i);
					if (!m.isStarted()){
						try {
							// give 2 more seconds to wait for possible startup
							Thread.sleep(2000);
						} catch (InterruptedException e) {
						}
					}
					if (m.isStarted()) atLeastOneRunning = true;
				}
			} while (!m_finished && atLeastOneRunning);
			if (m_finished) return;
			
			ICommand c = PIMRuntime.getInstance().getCommandFactory().getCommand("Activator");
			if (c!=null) {
				try {
					Map m = new HashMap();
					m.put("status", (atLeastOneRunning ? "revert" : "invert"));
					c.setParameters(m); // this method executes the command as well !!
				} catch (Exception e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
				}
			}
		}
		
		public void setFinished(boolean b) {
			this.m_finished = b;
		}
	}
	
    private String ID = "MonitorListener";
    private String NAMESPACE = "monitor.MonitorListener";
    private static MonitorListener m_instance = null;
     
    //private IMonitor m_monitor;
	private Properties m_configuration;
	private boolean running;
	private MonitorActiveCheck m_mac;
    
	private String CONFIG_MONITOR = "monitor";
	private String CONFIG_ENABLED = "enabled";
	private String CONFIG_DELAY = "delay";
    
    private IRuntime m_runtime;
	
    private MonitorListener() {
    	super();
    }
    
    public static synchronized MonitorListener getInstance() {
        if (MonitorListener.m_instance == null) {
            MonitorListener.m_instance = new MonitorListener();
        }
        return MonitorListener.m_instance;
    }
    
	public void doCallConnect(ICall call) {
		this.m_logger.entering(MonitorListener.class.getName(), "doCallConnect");
		if (call==null) {
			this.m_logger.severe("Call is invalid.");
			return;
		}
		
		if (call.getCaller()==null) {
			this.m_logger.severe("Caller is invalid.");
			return;
		}

		this.m_logger.info("Call connection: "+call);
        IEventBroker eventBroker = this.getRuntime().getEventBroker();
		
		IAttribute outgoing = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
		if (outgoing!=null && outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
			this.m_logger.info("Detected outgoing call: "+call);
			//IEvent ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_OUTGOINGCALL, call, eventCond);
			IEvent ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_OUTGOINGCALL, call);
	        eventBroker.send(this, ev);
		} else {
	        call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_STARTRING, Long.toString(new Date().getTime())));
	        //IEvent ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_INCOMINGCALL, call, eventCond);
	        IEvent ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_INCOMINGCALL, call);
	        eventBroker.send(this, ev);
		}

		this.m_logger.entering(MonitorListener.class.getName(), "doCallConnect");
	}

	public void doCallDisconnect(ICall call) {
		this.m_logger.entering(MonitorListener.class.getName(), "doCallDisconnect");
		if (call==null) {
			this.m_logger.severe("Call is invalid.");
			return;
		}
		
		if (call.getCaller()==null) {
			this.m_logger.severe("Caller is invalid.");
			return;
		}
		
		this.m_logger.info("Call disconnection: "+call);
		
		int reason = 0;
		IAttribute reasonAtt = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_REASON);
		if (reasonAtt!=null) {
			reason = Integer.parseInt(reasonAtt.getValue());
		}
		
		this.m_logger.info("Call disconnect reason: "+reason);

		IAttribute outgoing = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
		
		IEvent ev = null;
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		switch (reason) {
			case IEventConst.EVENT_TYPE_CALLACCEPTED:
				if (outgoing==null || !outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
					long end = new Date().getTime();
					long start = this.getStartTime(call);
					call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_ENDRING, Long.toString(end)));
					call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_RINGDURATION, Long.toString((end-start)/1000)));
				} 
				call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_ACCEPTED));
				this.m_logger.info("Call was accepted by user.");
				ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED, call);
				eventBroker.send(this, ev);
				break;
			case IEventConst.EVENT_TYPE_MANUALCALLACCEPTED:
				if (outgoing==null || !outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
					long end = new Date().getTime();
					long start = this.getStartTime(call);
					call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_ENDRING, Long.toString(end)));
					call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_RINGDURATION, Long.toString((end-start)/1000)));
				} 
				call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_REJECTED));
				this.m_logger.info("Call was rejected by user.");
				ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_MANUALCALLACCEPTED, call);
				eventBroker.send(this, ev);
				break;
			case IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED:
				call.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_OUTGOING));
				this.m_logger.info("Outgoing call was accepted by user.");
				ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED, call);
				eventBroker.send(this, ev);
				break;
			default:
				this.m_logger.info("Call was cleared normally.");
				ev = eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED, call);
				eventBroker.send(this, ev);
		}
		this.m_logger.entering(MonitorListener.class.getName(), "doCallDisconnect");
	}
    
    private long getStartTime(ICall c) {
		IAttribute startring = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_STARTRING);
		if (startring==null) return 0;
		return Long.parseLong(startring.getValue());
    }
    
    public int getPriority() {
        return 0;
    }
    
    public String getReceiverID() {
        return this.getID();
    }
    
    public void received(IEvent event) {
        if (event.getType() == IEventConst.EVENT_TYPE_CALLREJECTED) {
        	List monitors = this.getMonitors();
        	IMonitor m = null;
        	for (int i=0,j=monitors.size();i<j;i++) {
        		m = (IMonitor) monitors.get(i);
        		m.reject((short)0);
        	}
        }
        if (event.getType() == IEventConst.EVENT_TYPE_RETURNED_HIBERNATE) {
        	if (this.isRunning()) {
        		this.stop();
        		try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				this.start();
        	}
        }
    }
    
    public String getSenderID() {
        return this.getID();
    }
    
    public String getConfigurableID() {
        return this.getID();
    }
    
    public String getNamespace() {
        return this.NAMESPACE;
    }
    
    public void setConfiguration(Properties configuration) {
        this.m_configuration = configuration;
    }
    
    private long getDelayTime() {
    	String t = this.m_configuration.getProperty(CONFIG_DELAY, "0");
    	if (t!=null && t.trim().length()>0)
    		return Long.parseLong(t);
    	return 0;
    }
    
    public void start() {
		this.m_logger.entering(MonitorListener.class.getName(), "start");
        if (!this.running) {
        	
        	// check if already initialized
        	while (this.m_configuration==null) {
        		this.m_logger.warning("MonitorListener not yet initialized. Waiting 250ms...");
        		try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
				}
        	}
        	
        	long delayTime = this.getDelayTime() * 1000;
        	if (delayTime>0) {
        		this.m_logger.info("Delaying startup of monitor for "+delayTime+" ms");
        		try {
					Thread.sleep(delayTime);
				} catch (InterruptedException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
        	}
        	List monitors = this.getMonitors();
        	IMonitor m = null;
        	for (int i=0,j=monitors.size();i<j;i++) {
        		m = (IMonitor) monitors.get(i);
        		if (m.isAvailable()) {
            		m.setListener(this);
            		m.start();
            		this.m_logger.info("Started monitor: "+m.getID());
        		} else {
        			this.m_logger.info("Monitor is disabled: "+m.getID());
        		}
        	}
        	
        	for (int i=0,j=monitors.size();i<j;i++) {
        		final IMonitor mon = (IMonitor) monitors.get(i);
        		if (mon!=null) {
        			Thread s_check = new Thread() {
            			public void run() {
            				IMonitor l_mon = mon;
            				try {
        						Thread.sleep(5000);
        					} catch (InterruptedException e) {
        					}
        					if (l_mon !=null && l_mon.isStarted()) {
        						String[] desc = l_mon.getDescription();
        						if (desc!=null && desc.length>0) {
        							StringBuffer d = new StringBuffer();
        							d.append(desc[0]);
        							for (int i=1;i<desc.length;i++) {
        								if (desc[i]!=null && desc[i].length()>0) {
        									desc[i] = StringUtils.replaceString(desc[i], ",", " ");
        									d.append(", ");
        									d.append(desc[i].trim());
        								}
        							}
        							getRuntime().getConfigManagerFactory().getConfigManager().setProperty(IJAMConst.GLOBAL_NAMESPACE, "monitorsignature", d.toString());
        							getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
        						}
        					}
            			}
            		};
            		s_check.setName("JAM-MonitorSignatureCheck-Thread-(non-deamon)");
            		s_check.start();
        		}
        	}
        	
        	

            IEventBroker eventBroker = this.getRuntime().getEventBroker();
            // register as EventSender
            eventBroker.register(this);
            // register as EventReceiver
            eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
            eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
            this.running = true;
            this.m_logger.info("All monitors started on line.");
            for (int i=0;i<this.getCapiInformation().length;i++) {
            	this.m_logger.info(this.getCapiInformation()[i]);
            }
            
            if (m_mac!=null) {
            	m_mac.setFinished(true);
            	m_mac = null;
            }
            
            m_mac = new MonitorActiveCheck(this);
            m_mac.setName("JAM-MonitorActiveCheck-Thread-(non-deamon)");
            m_mac.start();
        }
		this.m_logger.exiting(MonitorListener.class.getName(), "start");
    }


    public void stop() {
		this.m_logger.entering(MonitorListener.class.getName(), "stop");
        if (this.running) {
            IEventBroker eventBroker = this.getRuntime().getEventBroker();
            eventBroker.unregister(this);
            eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
            eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
        	List monitors = this.getMonitors();
        	IMonitor m = null;
        	for (int i=0,j=monitors.size();i<j;i++) {
        		m = (IMonitor) monitors.get(i);
        		m.stop();
        		this.m_logger.info("Stopped monitor: "+m.getID());
        	}
        	
            if (m_mac!=null) {
            	m_mac.setFinished(true);
            	m_mac = null;
            }
        	
            this.running = false;
            this.m_logger.info("All monitors stopped on line.");
        }
		this.m_logger.exiting(MonitorListener.class.getName(), "stop");
    }

	public boolean isEnabled() {
		String bob = this.m_configuration.getProperty(this.CONFIG_ENABLED, "true");
		if (bob.equalsIgnoreCase("false")){
			return false;
		}
		return true;
	}
	
	public boolean isRunning() {
		return this.running;
	}
	
	public IMonitor getDefaultMonitor() {
		if (this.m_monitors==null || this.m_monitors.size()==0) {
			this.m_logger.log(Level.SEVERE, "Monitor instance is null or not set. Cannot recognize any incoming calls.", new Exception("Monitor instance is null or not set."));
			return null;
		}
		return (IMonitor)this.m_monitors.get(0);
	}

	public String getID() {
		return this.ID;
	}

	public void shutdown() {
		this.m_logger.entering(MonitorListener.class.getName(), "shutdown");
		super.shutdown();
		if (this.isEnabled())
			this.stop();
		
		IMonitor m = null;
		for (int i=0;i<this.m_monitors.size();i++) {
			m = (IMonitor) this.m_monitors.get(i);
			if (m!=null) {
				if (m.isAvailable() && m.isStarted()) m.stop();
				if (m instanceof IConfigurable) 
					this.getRuntime().getConfigurableNotifier().unregister((IConfigurable) m);
			}
		}
		
		this.m_monitors.clear();
		m_instance = null;
		
		this.getRuntime().getConfigurableNotifier().unregister(this);
		this.m_logger.exiting(MonitorListener.class.getName(), "shutdown");
	}

	public void startup() {
		this.m_logger.entering(MonitorListener.class.getName(), "startup");
		this.getRuntime().getConfigurableNotifier().register(this);

		super.startup();
        
		
		Iterator iter = this.m_configuration.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			// check for .class attribute in properties file
			if (key.startsWith(CONFIG_MONITOR)) {
				String className = this.m_configuration.getProperty(key);
				try {
					Class classObject = Thread.currentThread().getContextClassLoader().loadClass(className);
					// add a monitor to monitor list
					IMonitor m = (IMonitor) classObject.newInstance();
					this.m_monitors.add(m);
					this.m_logger.info("Registered new monitor <" + m.getID() +">.");
				} catch (ClassNotFoundException ex) {
					this.m_logger.warning("Could not find class: " + className);
					return;
				} catch (InstantiationException ex) {
					this.m_logger.severe("Could not instantiate class: " + className);
					return;
				} catch (IllegalAccessException ex) {
					this.m_logger.severe("Could not access class: " + className);
					return;
				} catch (Exception ex) {
					this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
					return;
				}
			}
		}
		
//		String className = this.m_configuration.getProperty(this.CONFIG_MONITOR, "");
//		if (className.length()==0) {
//			this.m_logger.severe("No monitor component for listening on ISDN line configured.");
//			return;
//		}
        
		if (this.isEnabled())
			this.start();
			
		this.m_logger.exiting(MonitorListener.class.getName(), "startup");
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
}
