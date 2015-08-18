package de.janrufmonitor.ncid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.framework.monitor.IMonitorListener;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.journal.Journal;

public class NcidMonitor implements IMonitor, IConfigurable, NcidConst {

	protected class NcidMonitorNotifier implements Runnable, IEventReceiver {

		private String ID = "NcidMonitorNotifier";
		
		private IMonitorListener jml;
		private Properties m_configuration;
		private Socket fb_socket;
		private boolean isRunning = true;
		
		private int retryCount = -1;

		private IRuntime m_runtime;
		private Map m_connections;
		
		public void run() {
			retryCount = 0;
			while (retryCount<this.getRetryMaxValue()) {
				if (connect()) {
					IEventBroker broker = this.getRuntime().getEventBroker();
					broker.register(this, broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
					broker.register(this, broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
					
					this.isRunning = true;
					m_connections = new HashMap(5);
					m_logger.info("Retry count currently is "+retryCount);

					try {
						BufferedReader in = new BufferedReader(new InputStreamReader(fb_socket.getInputStream()));
						String currentLine = null;
						
						while(isRunning){
							currentLine = in.readLine();
							//System.out.println(currentLine);
							m_logger.info("Raw data from NCID Server: "+currentLine);
							process(currentLine);
						}
					} catch (IOException e) {
						if (isRunning) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);							
							PropagationFactory.getInstance().fire(
									new Message(Message.ERROR,
									getNamespace(),
									"connectlost",									
									e));
							isRunning = true;
							try {
								Thread.sleep(15000);
							} catch (InterruptedException e1) {
								m_logger.log(Level.SEVERE, e1.getMessage(), e1);
							}							
						} else {
							m_logger.log(Level.INFO, e.getMessage(), e);
							isRunning = false;
						}						
					} finally {
						if (broker!=null) {
							broker.unregister(this, broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
							broker.unregister(this, broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
						}
					}
				} else {
					m_logger.warning("Connect to Ncid failed.");
					isRunning = true;
					try {
						Thread.sleep(15000);
					} catch (InterruptedException e1) {
						m_logger.log(Level.SEVERE, e1.getMessage(), e1);
					}	
				}
			}
			this.isRunning = false;
		}
		
		private synchronized void process(String c) {
			String action = NcidCallRaw.getAction(c);
			if (action==null) return;
						
			if ("CID:".equalsIgnoreCase(action)) {
				NcidCallRaw rawCall = new NcidCallRaw(c, this.m_configuration);
				if (rawCall.isValid()) {
					IMsn called = rawCall.toCall().getMSN();
					if (called!=null && getRuntime().getMsnManager().isMsnMonitored(called)) {
						this.m_connections.put(NcidCallRaw.getLine(c), rawCall.toCall());
						this.getListener().doCallConnect(rawCall.toCall());
					}
				} else {
					m_logger.severe("Call from Ncid is invalid: "+c);
				}
			}
			if ("OUT:".equalsIgnoreCase(action) && this.m_configuration.getProperty(CFG_OUTGOING, "false").equalsIgnoreCase("true")) {
				NcidCallRaw rawCall = new NcidCallRaw(c, this.m_configuration);
				if (rawCall.isValid()) {
					IMsn called = rawCall.toCall().getMSN();
					if (called!=null && getRuntime().getMsnManager().isMsnMonitored(called)) {
						this.m_connections.put(NcidCallRaw.getLine(c), rawCall.toCall());
						this.getListener().doCallConnect(rawCall.toCall());
					}
				} else {
					m_logger.severe("Call from Ncid is invalid: "+c);
				}
			}
			if ("CIDINFO:".equalsIgnoreCase(action)) {
				ICall nc = (ICall) m_connections.get(NcidCallRaw.getLine(c));
				if (nc!=null && NcidCallRaw.getCallState(c).equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_ACCEPTED)) {
					IAttribute outgoing = nc.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
					if (outgoing==null || !outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING))
						nc.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_REASON, Integer.toString(IEventConst.EVENT_TYPE_CALLACCEPTED)));
					if (outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
						nc.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_REASON, Integer.toString(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED)));
					}
					
					this.getListener().doCallDisconnect(nc);	
				}
			}
			if ("CIDLOG:".equalsIgnoreCase(action) && this.isSyncCDILog()) {
				NcidCallRaw rawCall = new NcidCallRaw(c, this.m_configuration);
				if (rawCall.isValid()) {
					ICall call = rawCall.toCall();
					if (call!=null && call.getDate().getTime()>this.getLastSyncTimestamp()) {
						// 2015/08/18: get current journal repository
						m_logger.info("Last sync timestamp: "+new Date(this.getLastSyncTimestamp()));
						m_logger.info("Call timestamp: "+call.getDate());
						String repository = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Journal.NAMESPACE, "repository");
						ICallManager cm = getRuntime().getCallManagerFactory().getCallManager(repository);
						if (cm!=null && cm.isActive() && cm.isSupported(IWriteCallRepository.class)) {
							m_logger.info("Syncronizing call from CIDLOG to journal: "+call);
							((IWriteCallRepository)cm).setCall(call);
						}
					} else {
						m_logger.log(Level.INFO, "Call from CIDLOG not synced (call timestamp before sync timestamp): "+call);
					}
				} else {
					m_logger.severe("Call from Ncid is invalid: "+c);
				}
			}
		}
		
		private IMonitorListener getListener() {
			return this.jml;
		}

		 public void disconnect() {
		 	try {
		 		this.isRunning = false;
		 		retryCount = getRetryMaxValue();
		 		if (fb_socket != null)
		 			fb_socket.close();
	 		} catch (IOException e) {
	 			m_logger.log(Level.SEVERE, e.getMessage(), e);
	 		} finally {
	 			retryCount = getRetryMaxValue();
	 			this.isRunning = false;
	 		}
		 }
		
		protected boolean connect() {
			try {
				m_logger.info("Re-connect try #"+this.retryCount);
				fb_socket = new Socket(this.m_configuration.getProperty(CFG_IP, "192.168.2.1"), Integer.parseInt(this.m_configuration.getProperty(CFG_MONITOR_PORT, "3333")));
				fb_socket.setKeepAlive(true);
				retryCount = 0;
				
				System.currentTimeMillis();
				
				return true;
			} catch (UnknownHostException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
				retryCount++;
			} catch (SocketException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
				retryCount++;
			} catch (IOException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						getNamespace(),
						"connect",	
						new String[] {Integer.toString(getRetryMaxValue() - retryCount)},
						e));
				retryCount++;
			} 
			return false;
		}
		
		protected void setListener(IMonitorListener jml) {
			if (jml != null) {
				this.jml = jml;
			}
		}
		
		protected void setConfiguration(Properties config) {
			this.m_configuration = config;
		}
		
		protected void reject(short cause) {
			
		}
		
		private IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
		
		private int getRetryMaxValue() {
			if (m_configuration!=null)
				return Integer.parseInt(m_configuration.getProperty(CFG_RETRYMAX, "5"));
			return 5;
		}
		
		private boolean isSyncCDILog() {
			if (m_configuration!=null)
				return Boolean.parseBoolean(m_configuration.getProperty(CFG_SYNCCIDLOG, "false"));
			return false;
		}
		
		private long getLastSyncTimestamp() {
			if (m_configuration!=null)
				return Long.parseLong(m_configuration.getProperty(CFG_SYNCTIMESTAMP, "-1"));
			return -1L;
		}
		
		public String toString() {
			return ID;
		}

		public void received(IEvent event) {
			if (event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_CALL || event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL) {
				ICall c = (ICall)event.getData();
	        	if (c!=null) {
	        		String key = NcidCallRaw.getKey(c);
	        		if (key!=null && this.m_connections.containsKey(key)) {
	        			m_logger.info("Found key #"+key+". adding identified call.");
	        			this.m_connections.put(key, c);
	        		}
	        	}
			}
		}

		public String getReceiverID() {
			return ID;
		}

		public int getPriority() {
			return 0;
		}
		
		public String[] getNcidDescription() {
			String[] description = new String[] {
					"NCID Modul f\u00FCr jAnrufmonitor","Verbindung zum NCID Server: OK",this.m_configuration.getProperty(CFG_IP),"Port: "+this.m_configuration.getProperty(CFG_MONITOR_PORT),""
			};
			return description;
		}
		
		public boolean isRunning() {
			return this.isRunning;
		}
	}

	public static String NAMESPACE = "monitor.NcidMonitor";
	public static String ID = "NcidMonitor";

	private Logger m_logger;
	private Properties m_configuration;
	
	private NcidMonitorNotifier cmn = null;
	private Thread cmnThread = null;
	private IMonitorListener m_ml;
	
	public NcidMonitor() {
		super();
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
	}

	public synchronized void start() {
		if (this.cmn==null) {
			this.m_logger.warning("No NcidMonitorNotifier registered. Try to create a new one.");
			if (this.m_ml!=null)
				this.setListener(this.m_ml);
			
			if (this.cmn==null) {
				this.m_logger.severe("No NcidMonitorNotifier registered. Could not register on Ncid.");
				this.stop();
				return;
			}
		}
		
		if (cmnThread==null){
			this.cmn.setConfiguration(this.m_configuration);
			this.cmnThread = new Thread(cmn);
			this.cmnThread.setName("JAM-"+cmn.toString()+"-Thread-(deamon)");
			this.cmnThread.setDaemon(true);
			this.cmnThread.start();	
			this.m_logger.info("New thread for NcidMonitorNotifier created.");
		} else {
			this.m_logger.warning("A NcidMonitorNotifier thread is still running, could not create a new one.");
		}
		
        this.m_logger.info("NcidMonitor started.");
	}

	public void stop() {
		PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().setProperty(ID, NcidConst.CFG_SYNCTIMESTAMP, Long.toString(new Date().getTime()));
		PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().saveConfiguration();
        this.release();
		this.m_logger.info("NcidMonitor stopped.");
	}

	public void setListener(IMonitorListener jml) {
		this.m_ml = jml;
       	if (this.cmn==null) {
       		this.cmn = new NcidMonitorNotifier();
       	} 
       	this.cmn.setListener(this.m_ml);
	}

	public void reject(short cause) {
		if (this.cmn!=null) {
			this.cmn.reject(cause);
			this.m_logger.info("NcidMonitor rejected.");
		}
	}

	public void release() {
		if (this.cmn!=null) {
			this.cmn.disconnect();
			// added: 2007/05/30: set cmn to null
			this.cmn = null;
		}
		
		int count = 0;
		
		while (this.cmnThread!=null && this.cmnThread.isAlive() && count < 5) {
			this.cmnThread.interrupt();
			this.m_logger.info("Try to release NcidMonitor. Attempt #"+(count+1));
			count ++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		
		if (this.cmnThread!=null && this.cmnThread.isAlive()) {
			this.cmnThread.interrupt();
			if (this.cmnThread!=null && this.cmnThread.isAlive())
				this.m_logger.warning("Could not release NcidMonitor after "+count+" attempts.");
		}
		this.cmnThread = null;

		this.m_logger.info("NcidMonitor released Ncid.");
	}

	public boolean isStarted() {
		return (this.cmn!=null && this.cmn.isRunning());
	}

	public String[] getDescription() {
		if (this.cmn==null)
			return new String[] {
				"NCID Modul f\u00FCr jAnrufmonitor","Keine Verbindung zum NCID Server","","",""
			};
		
		return this.cmn.getNcidDescription();
	}

	public String getID() {
		return ID;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public String getConfigurableID() {
		return ID;
	}
	
	public void setConfiguration(final Properties configuration) {
		this.m_configuration = configuration;		
		Thread starterTread = new Thread(new Runnable(){
			public void run() {
				if (isStarted()){
						//stop();
					if (m_ml!=null)
						setListener(m_ml);
					
					if (cmn!=null)
						cmn.setConfiguration(configuration);
					//start();
				}
			}
		});
		starterTread.setName("JAM-StartAfterConfigChange-Thread-(deamon)");
		starterTread.setDaemon(true);
		starterTread.start();	
	}

	public boolean isAvailable() {
		return this.m_configuration.getProperty("activemonitor", "false").equalsIgnoreCase("true");
	}

}
