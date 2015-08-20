package de.janrufmonitor.xtapi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.xtapi.serviceProvider.IXTapiCallBack;
import net.xtapi.serviceProvider.MSTAPI;
import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.configuration.IConfigManager;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.framework.monitor.IMonitorListener;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;

public class XTapiMonitor implements IMonitor, IConfigurable, XTapiConst {

	public static String NAMESPACE = "monitor.XTapiMonitor";

	public static String ID = "XTapiMonitor";

	private Logger m_logger;

	private Properties m_configuration;

	private XTapiMonitorNotifier cmn = null;

	private Thread cmnThread = null;

	private IMonitorListener m_ml;

	private class TapiHandle {

		int m_id;

		String m_name;

		public TapiHandle(int id, String name) {
			this.m_id = id;
			this.m_name = name;
		}

		public String getName() {
			return this.m_name;
		}

		public String toString() {
			return "TAPI handle: " + this.m_id + " - " + this.m_name;
		}

	}

	protected class XTapiMonitorNotifier implements Runnable, IEventReceiver, IEventSender,
			IXTapiCallBack {

		private String ID = "XTapiMonitorNotifier";

		private IMonitorListener jml;

		private Properties m_configuration;

		private boolean isRunning = true;

		private IRuntime m_runtime;

		private Map m_connections;

		private MSTAPI m_tapi;

		private Map m_tapiHandles;
		
		private List m_proceededCalls;

		private int m_lineHandle;
		
		private int[] m_deviceIDMapping;

		public void run() {
			try {
				m_tapiHandles = new HashMap();
				m_connections = new HashMap(2);
				m_proceededCalls = new ArrayList(16);
				
				m_tapi = new MSTAPI();
				int n = m_tapi.init(this);
				
				m_deviceIDMapping = new int[n];

				IEventBroker broker = this.getRuntime().getEventBroker();
				broker.register(this, broker
						.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
				broker.register(this, broker
						.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));

				StringBuffer nameOfLine = null;

				for (int i = 0; i < n; i++) {
					nameOfLine = new StringBuffer();
					m_lineHandle = m_tapi.openLineTapi(i, nameOfLine);
					m_deviceIDMapping[i] = -1;
					if (m_lineHandle > 0) {
						m_logger.info("Opening line #" + m_lineHandle);
						m_logger.info("Opening line name "
								+ nameOfLine.toString());
						m_tapiHandles.put(new Integer(m_lineHandle),
								new TapiHandle(m_lineHandle, nameOfLine
										.toString()));
						
						m_deviceIDMapping[i] = Integer.valueOf("0"+nameOfLine.toString().replaceAll("[^0-9]+", ""));
						
						m_logger.info("Device mapping ID["+i+"]: " + m_deviceIDMapping[i]);
					}
				}

				if (n > 0) {
					while (isRunning) {
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
						}
					}
				}
			} catch (Exception e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						getNamespace(),
						"error",									
						e));
			} finally {
				IEventBroker broker = this.getRuntime().getEventBroker();
				if (broker != null) {
					broker.unregister(this,	broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
					broker.unregister(this,	broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
				}
			}
			this.isRunning = false;
		}

		public String toString() {
			return ID;
		}

		public void received(IEvent event) {
			if (event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_CALL || event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL) {
				ICall c = (ICall) event.getData();
				if (c != null) {
					String key = XTapiCall.getKey(c);
					if (key != null && this.m_connections.containsKey(key)) {
						m_logger.info("Found key #" + key
								+ ". adding identified call.");
						this.m_connections.put(key, c);
					}
				}
			}
		}

		public void disconnect() {
			if (this.m_tapi != null)
				m_tapi.shutdownTapi();

			if (m_tapiHandles != null)
				m_tapiHandles.clear();
			m_tapiHandles = null;

			this.isRunning = false;
		}

		public String getReceiverID() {
			return ID;
		}

		public int getPriority() {
			return 0;
		}

		public boolean isRunning() {
			return this.isRunning;
		}

		private IRuntime getRuntime() {
			if (this.m_runtime == null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
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
			if (this.m_tapi != null) {
				Iterator i = this.m_connections.entrySet().iterator();
				while (i.hasNext()) {
					ICall th = (ICall) ((Map.Entry) i.next()).getValue();
					if (th!=null) {
						m_logger.info("Found call for reject: "+th);
						IAttribute deviceAttribute = th.getAttribute("tapi.device");
						if (deviceAttribute!=null) {
							String shandle = deviceAttribute.getValue();
							int handle = Integer.parseInt(shandle);
							m_tapi.dropCallTapi(handle);
						}
					}
				}
			}
		}

		private String[] getCallerInfoFromTapi(int device) {
			String[] callerInfo = this.m_tapi.getCallInfoTapi(device);
			if (callerInfo == null) {
				m_logger.warning("Could not get a valid caller info from TAPI.");
				return null;
			}
			
			int count = 0;
			while (callerInfo[1].length()==2 && count<10) {
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
				}
				callerInfo = this.m_tapi.getCallInfoTapi(device);
				count++;
			}
			return callerInfo;
		}
		
		private synchronized void signalTapiEvent(int dwDevice, int dwInstance, int value) {
			ICall nc = (ICall) m_connections.get(XTapiCall.getKey(
					dwDevice, dwInstance));
			
			if (nc!=null) {
				nc.setAttribute(this.getRuntime().getCallFactory().createAttribute("tapi.value", Integer.toString(value)));
				getRuntime().getEventBroker().register(this);
				getRuntime().getEventBroker().send(this, getRuntime().getEventBroker().createEvent(9999, nc));
				getRuntime().getEventBroker().unregister(this);
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
				}
			}
			
		}
		
		private void signalDoConnected(int dwDevice, int dwInstance) {
			ICall nc = (ICall) m_connections.get(XTapiCall.getKey(
					dwDevice, dwInstance));

			if (nc!=null) {
				IAttribute outgoing = nc.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
				if (outgoing==null || !outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING))
					nc.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_REASON, Integer.toString(IEventConst.EVENT_TYPE_CALLACCEPTED)));
				if (outgoing.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
					nc.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_REASON, Integer.toString(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL_ACCEPTED)));
				}	
				nc.setAttribute(this.getRuntime().getCallFactory().createAttribute("tapi.acceptedtime", Long.toString(System.currentTimeMillis())));
				nc.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_ACTIVE_INDICATOR, IJAMConst.ATTRIBUTE_VALUE_YES));
				this.getListener().doCallDisconnect(nc);	
			}
		}

		private synchronized void signalDoOutgoingCallConnect(int dwDevice, int dwInstance) {
			String[] callerInfo = this.getCallerInfoFromTapi(dwDevice);
			if (callerInfo == null) {
				return;
			}
			
			if (m_logger.isLoggable(Level.INFO)) {
				for (int i=0;i<callerInfo.length;i++)
					m_logger.info("Caller information from TAPI: "+callerInfo[i]);
			}

			ICall c = null;
			if (callerInfo.length>3) {
				m_logger.info("Called extension from TAPI: "+(this.useDeviceIDasMSN() ? getDeviceIDNumber(dwInstance): callerInfo[1]));
				c = new XTapiCall(dwDevice, dwInstance,
						removeSpecialChars(callerInfo[3]), (this.useDeviceIDasMSN() ? getDeviceIDNumber(dwInstance): callerInfo[1]), this.m_configuration).toCall();
				
				c.setAttribute(getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_OUTGOING));
				if (this.useDeviceIDasMSN()) {
					c.setAttribute(getRuntime().getCallFactory().createAttribute("tapi.callednumber", callerInfo[3]));
				}
			} 
			
			IMsn called = c.getMSN();
			if (called != null
					&& getRuntime().getMsnManager().isMsnMonitored(
							called)) {
				this.m_connections.put(XTapiCall.getKey(c), c);
				this.getListener().doCallConnect(c);
			}
		}
		
		private synchronized void signalDoCallConnect(int dwDevice, int dwInstance) {
			String[] callerInfo = this.getCallerInfoFromTapi(dwDevice);
			if (callerInfo == null) {
				return;
			}
			if (m_logger.isLoggable(Level.INFO)) {
				m_logger.info("Device ID: "+dwDevice);
				m_logger.info("Instance ID: "+dwInstance);
				m_logger.info("Caller information length: "+callerInfo.length);
				for (int i=0;i<callerInfo.length;i++) {
					m_logger.info("Caller information ["+i+"]: "+callerInfo[i]);
				}
			}
			
			ICall c = null;
			if (callerInfo.length>3) {
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Called extension from TAPI: "+(this.useDeviceIDasMSN() ? getDeviceIDNumber(dwInstance): callerInfo[3]));
				c = new XTapiCall(dwDevice, dwInstance,
						removeSpecialChars(callerInfo[1]), (this.useDeviceIDasMSN() ? getDeviceIDNumber(dwInstance): callerInfo[3]), this.m_configuration).toCall();
				if (this.useDeviceIDasMSN()) {
					c.setAttribute(getRuntime().getCallFactory().createAttribute("tapi.callednumber", callerInfo[3]));
				}
				
			} else {
				c = new XTapiCall(dwDevice, dwInstance,
						removeSpecialChars(callerInfo[1]), this.m_configuration).toCall();
			}
			
			IMsn called = c.getMSN();
			if (called != null
					&& getRuntime().getMsnManager().isMsnMonitored(
							called)) {
				this.m_connections.put(XTapiCall.getKey(c), c);
				this.getListener().doCallConnect(c);
			}
		}
		
		private String removeSpecialChars(String n) {
			if (n.startsWith("+")) 
				StringUtils.replaceString(n, "+", "00");
			return n;
		}
		
		private String getDeviceIDNumber(int instance) {
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("try to determine DeviceIDNumber from instance #"+instance);
			
			if (this.m_deviceIDMapping.length > instance && this.m_deviceIDMapping[instance]>=0) {
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("DeviceIDNumber found for instance #"+instance+" = "+this.m_deviceIDMapping[instance]);
				return Integer.toString(this.m_deviceIDMapping[instance]);
			}
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("No DeviceIDNumber found for instance #"+instance);
			return Integer.toString(instance);
		}
		
		private boolean useDeviceIDasMSN(){
			if (this.m_configuration!=null) {
				return Boolean.parseBoolean(this.m_configuration.getProperty(CFG_DEVICEID, "false"));
			}
			return false;
		}
		
		private void signalDoCallDisconnect(int dwDevice, int dwInstance) {
			this.m_proceededCalls.remove(Integer.toString(dwDevice));
			ICall nc = (ICall) m_connections.remove(XTapiCall.getKey(
					dwDevice, dwInstance));
			if (nc != null) {
				IAttribute at = nc.getAttribute("tapi.acceptedtime");
				if (at!=null) {
					long st = Long.parseLong(at.getValue());
					long duration = (System.currentTimeMillis() - st) / 1000;
					nc.setAttribute(this.getRuntime().getCallFactory().createAttribute("tapi.duration", Long.toString(duration)));	
					nc.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_DURATION, Long.toString(duration)));	
				}			
				nc.setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_ACTIVE_INDICATOR, IJAMConst.ATTRIBUTE_VALUE_NO));
				this.getListener().doCallDisconnect(nc);	
			}
		}
		
		private void detectTapiPattern(int dwDevice, int dwMessage, int dwInstance,
				int dwParam1, int dwParam2, int dwParam3) {
			
				IConfigManager cfg = this.m_runtime.getConfigManagerFactory().getConfigManager();
				SimpleDateFormat formatter
					 = new SimpleDateFormat(cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_DATE)+ " " +cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_TIME));

				StringBuffer pattern = new StringBuffer();
				pattern.append(formatter.format(new Date()));
				pattern.append(": ");
				pattern.append("*");
				pattern.append(",");
				pattern.append(dwMessage);
				pattern.append(",");
				pattern.append(dwInstance);
				pattern.append(",");
				pattern.append(dwParam1);
				pattern.append(",");
				pattern.append(dwParam2);
				pattern.append(",");
				pattern.append(dwParam3);
				pattern.append(IJAMConst.CRLF);
			
				File patternFile = new File(PathResolver.getInstance(getRuntime()).getLogDirectory()+File.separator + "tapi-pattern.log");
				try {
					FileOutputStream po = null;
					if (patternFile.exists()) {
						po = new FileOutputStream(patternFile, true);
					} else {
						po = new FileOutputStream(patternFile);
					}
					po.write(pattern.toString().getBytes());
					po.flush();
					po.close();
				} catch (FileNotFoundException e) {
					m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
		}
		
		public void callback(int dwDevice, int dwMessage, int dwInstance,
				int dwParam1, int dwParam2, int dwParam3) {

			if (m_logger.isLoggable(Level.INFO)) {
				this.detectTapiPattern(dwDevice, dwMessage, dwInstance, dwParam1, dwParam2, dwParam3);
			}
			
			switch (dwMessage) {
				case LINE_CREATE:
					m_logger.info("TAPI message LINE_CREATE on device: "
							+ dwDevice + ", message: " + dwMessage + ", instance: "
							+ dwInstance + ", parameter 1: " + dwParam1
							+ ", parameter 2: " + dwParam2 + ", parameter 3: "
							+ dwParam3);
					break;
				case LINE_DEVSPECIFIC:
					m_logger.info("TAPI message LINE_DEVSPECIFIC on device: "
							+ dwDevice + ", message: " + dwMessage + ", instance: "
							+ dwInstance + ", parameter 1: " + dwParam1
							+ ", parameter 2: " + dwParam2 + ", parameter 3: "
							+ dwParam3);
					break;					
				case LINE_CALLSTATE: 
					m_logger.info("TAPI message LINE_CALLSTATE on device: "
							+ dwDevice + ", message: " + dwMessage + ", instance: "
							+ dwInstance + ", parameter 1: " + dwParam1
							+ ", parameter 2: " + dwParam2 + ", parameter 3: "
							+ dwParam3);

					switch (dwParam1) {
					case LINECALLSTATE_DIALTONE:
						m_logger.info("TAPI param1: LINECALLSTATE_DIALTONE");
						break;									
					case LINECALLSTATE_OFFERING:
						m_logger.info("TAPI param1: LINECALLSTATE_OFFERING");
						this.signalDoCallConnect(dwDevice, dwInstance);
						break;
					case LINECALLSTATE_DISCONNECTED:
						m_logger.info("TAPI param1: LINECALLSTATE_DISCONNECTED");
						this.signalDoCallDisconnect(dwDevice, dwInstance);
						break;
					case LINECALLSTATE_DIALING:
						m_logger.info("TAPI param1: LINECALLSTATE_DIALING");
						break;
					case LINECALLSTATE_ACCEPTED:
						m_logger.info("TAPI param1: LINECALLSTATE_ACCEPTED");
						break;
					case LINECALLSTATE_BUSY:
						m_logger.info("TAPI param1: LINECALLSTATE_BUSY");
						break;
					case LINECALLSTATE_CONNECTED:
						m_logger.info("TAPI param1: LINECALLSTATE_CONNECTED");
						this.signalDoConnected(dwDevice, dwInstance);
						break;
					case LINECALLSTATE_ONHOLD:
						m_logger.info("TAPI param1: LINECALLSTATE_ONHOLD");
						break;
					case LINECALLSTATE_RINGBACK:
						m_logger.info("TAPI param1: LINECALLSTATE_RINGBACK");
						if (this.m_configuration.getProperty(CFG_OUTGOING, "false").equalsIgnoreCase("true")) {
							if (!this.m_proceededCalls.contains(Integer.toString(dwDevice))) {
								this.m_proceededCalls.add(Integer.toString(dwDevice));
								this.signalDoOutgoingCallConnect(dwDevice, dwInstance);
							} else {
								m_logger.info("TAPI param1: LINECALLSTATE_RINGBACK: Already notified as outgoing call.");
							}
						}
						
						break;		
					case LINECALLSTATE_PROCEEDING:
						m_logger.info("TAPI param1: LINECALLSTATE_PROCEEDING");
						if (this.m_configuration.getProperty(CFG_OUTGOING, "false").equalsIgnoreCase("true")) {
							if (!this.m_proceededCalls.contains(Integer.toString(dwDevice))) {
								this.m_proceededCalls.add(Integer.toString(dwDevice));
								this.signalDoOutgoingCallConnect(dwDevice, dwInstance);
							} else {
								m_logger.info("TAPI param1: LINECALLSTATE_PROCEEDING: Already notified as outgoing call.");
							}
						}
						break;	
					default:
						m_logger.info("unhandled TAPI state: parameter 1: "
								+ dwParam1 + ", parameter 2: " + dwParam2
								+ ", parameter 3: " + dwParam3);
						this.detectTapiPattern(dwDevice, dwMessage, dwInstance, dwParam1, dwParam2, dwParam3);
						this.signalTapiEvent(dwDevice, dwInstance, dwParam1);
						break;
					}
					break;
				default:
					m_logger.info("unhandled TAPI message: device: " + dwDevice
							+ ", message: " + dwMessage + ", instance: "
							+ dwInstance + ", parameter 1: " + dwParam1
							+ ", parameter 2: " + dwParam2 + ", parameter 3: "
							+ dwParam3);
					this.detectTapiPattern(dwDevice, dwMessage, dwInstance, dwParam1, dwParam2, dwParam3);
					this.signalTapiEvent(dwDevice, dwInstance, dwParam1);
			}
		}



		private IMonitorListener getListener() {
			return this.jml;
		}

		public String[] getDescription() {
			String[] info = new String[this.m_tapiHandles.entrySet().size() + 2];
			info[0] = "XTapiMonitor Module 1.0";
			info[1] = "Detected TAPI devices:";
			Iterator i = this.m_tapiHandles.entrySet().iterator();
			int j = 2;
			while (i.hasNext()) {
				TapiHandle th = (TapiHandle) ((Map.Entry) i.next()).getValue();
				info[j] = "\t" + th.getName();
				j++;
			}
			return info;
		}

		public String getSenderID() {
			return this.ID;
		}

	}

	public XTapiMonitor() {
		super();
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
	}

	public void start() {
		if (this.cmn == null) {
			this.m_logger
					.severe("No XTapiMonitorNotifier registered. Could not register on TAPI.");
			this.stop();
			return;
		}

		if (cmnThread == null) {
			this.cmn.setConfiguration(this.m_configuration);
			this.cmnThread = new Thread(cmn);
			this.cmnThread.setName("JAM-"+cmn.toString()+"-Thread-(deamon)");
			this.cmnThread.setDaemon(true);
			this.cmnThread.start();
			this.m_logger.info("New thread for XTapiMonitorNotifier created.");
		} else {
			this.m_logger
					.warning("A XTapiMonitorNotifier thread is still running, could not create a new one.");
		}

		this.m_logger.info("XTapiMonitor started.");
	}

	public void stop() {
		this.release();
		this.m_logger.info("XTapiMonitor stopped.");
	}

	public void setListener(IMonitorListener jml) {
		this.m_ml = jml;
		if (this.cmn == null) {
			this.cmn = new XTapiMonitorNotifier();
			this.cmn.setListener(this.m_ml);
		} else {
			this.cmn.setListener(this.m_ml);
		}
	}

	public void reject(short cause) {
		if (this.cmn != null) {
			this.cmn.reject(cause);
			this.m_logger.info("XTapiMonitor rejected.");
		}
	}

	public void release() {
		if (this.cmn != null) {
			this.cmn.disconnect();
			// added: 2007/05/30: set cmn to null
			this.cmn = null;
		}

		int count = 0;

		while (this.cmnThread != null && this.cmnThread.isAlive() && count < 5) {
			this.m_logger.info("Try to release XTapiMonitor. Attempt #"
					+ (count + 1));
			count++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.cmnThread != null && this.cmnThread.isAlive()) {
			this.m_logger.warning("Could not release XTapiMonitor after "
					+ count + " attempts.");
		}
		this.cmnThread = null;

		this.m_logger.info("XTapiMonitor released TAPI.");
	}

	public boolean isStarted() {
		return (this.cmn != null && this.cmn.isRunning());
	}

	public String[] getDescription() {
		if (this.cmn == null)
			return new String[] { "XTapiMonitor Module 1.0",
					"not connected to TAPI", "", "", "" };

		return this.cmn.getDescription();
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

	public void setConfiguration(Properties configuration) {
		this.m_configuration = configuration;
		if (this.isStarted()) {
			this.stop();
			if (this.m_ml != null)
				this.setListener(this.m_ml);
			this.start();
		}
	}

	public boolean isAvailable() {
		return this.m_configuration.getProperty("activemonitor", "false")
				.equalsIgnoreCase("true");
	}

}
