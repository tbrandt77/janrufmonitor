package de.powerisdnmonitor.capi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.capi.capi20.Capi;
import org.capi.capi20.CapiException;
import org.capi.capi20.CapiListener;
import org.capi.capi20.CapiMessage;

import de.powerisdnmonitor.capi.util.ByteArray;
import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.framework.monitor.IMonitorListener;
import de.janrufmonitor.framework.monitor.MonitorException;
import de.janrufmonitor.logging.TraceFormatter;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class CapiMonitor implements IMonitor, IConfigurable {

	protected class CapiMonitorNotifier implements Runnable, CapiListener, IEventReceiver {

		private String ID = "CapiMonitorNotifier";
		private String SEPARATOR = "/";
				
		private String CFG_BUSY = "busyonbusy";
		private String CFG_SPOOFING = "spoofing";
		private String CFG_MAX_CONNECTION = "maxcon";
		private String CFG_MAX_BLOCK = "maxblock";
		private String CFG_BLOCK_SIZE = "blocksize";
	
		private String SIM_CAPI_MSG_FILE = "capi.msg";
		private String LAST_CAPI_MSG_FILE = "last_capi.raw";
		
		private Logger m_logger;
		private Logger m_tracer;
		private IRuntime m_runtime;
		
		private IMonitorListener jml = null;
		private Properties m_configuration = null;
		
		private Map m_connections;
		
		private Capi capi = null;
		private int appID;
		private List plciList;
		private boolean m_isConnectActInd = false;
		
		public CapiMonitorNotifier() {
			this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
			
			LogManager lm = LogManager.getLogManager();
			// create trace logger
			Logger m_tracer = Logger.getLogger(IJAMConst.TRACE_LOGGER);
			if (lm.getLogger(IJAMConst.TRACE_LOGGER)==null)
				lm.addLogger(m_tracer);
			try {
				FileHandler fh = new FileHandler(PathResolver.getInstance().getLogDirectory()+"capitrace-%g.log");
				fh.setFormatter(new TraceFormatter());
				m_tracer.addHandler(
					fh
				);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			this.m_tracer = LogManager.getLogManager().getLogger(IJAMConst.TRACE_LOGGER);

			this.m_connections = new HashMap();
			this.capi = new PIMCapi();
		}
		
		public void run() {
			IEventBroker broker = this.getRuntime().getEventBroker();
			broker.register(this, broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
			
			try {
				this.register();
			} catch (MonitorException ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
				PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "capierror", ex));
				this.releaseCapi();
			}
		}

		public void capiEventRaised() {
			this.m_logger.entering(CapiMonitor.class.getName(), "capiEventRaised");
			try {
				PIMCapiMessage m = (PIMCapiMessage) capi.getMessage(appID);
				Object plci = m.getValue("PLCI");
				this.m_logger.info("Incoming CAPI_MESSAGE on physical connection #"+plci+ " for application ID "+m.getAppID());
				switch (m.getType()) {
					case PIMCapiMessage.CONNECT_IND:
						this.doConnectInd(m);
						break;
					case PIMCapiMessage.DISCONNECT_IND:
						this.doDisconnectInd(m);
						this.removePLCI(plci);
						break;
					case PIMCapiMessage.INFO_IND:
						this.m_logger.info("unhandled message type: INFO_IND");
					break;
					case PIMCapiMessage.CONNECT_ACTIVE_IND:
						this.m_logger.info("unhandled message type: CONNECT_ACTIVE_IND");
						this.m_isConnectActInd = true;
					break;
					case PIMCapiMessage.ALERT_CONF:
						this.m_logger.info("unhandled message type: ALERT_CONF");
					break;       
					case PIMCapiMessage.ALERT_REQ:
						this.m_logger.info("unhandled message type: ALERT_REQ");
					break;       
					case PIMCapiMessage.CONNECT_ACTIVE_RESP:						
						this.m_logger.info("unhandled message type: CONNECT_ACTIVE_RESP");
					break;   				         
					case PIMCapiMessage.CONNECT_B3_ACTIVE_IND:
						this.m_logger.info("unhandled message type: CONNECT_B3_ACTIVE_IND");
					break; 		
					case PIMCapiMessage.CONNECT_B3_ACTIVE_RESP:
						this.m_logger.info("unhandled message type: CONNECT_B3_ACTIVE_RESP");
					break; 	
					case PIMCapiMessage.CONNECT_B3_CONF:
						this.m_logger.info("unhandled message type: CONNECT_B3_CONF");
					break; 		
					case PIMCapiMessage.CONNECT_B3_IND:
						this.m_logger.info("unhandled message type: CONNECT_B3_IND");
					break; 	
					case PIMCapiMessage.CONNECT_B3_REQ:
						this.m_logger.info("unhandled message type: CONNECT_B3_REQ");
					break; 															
					case PIMCapiMessage.CONNECT_B3_RESP:
						this.m_logger.info("unhandled message type: CONNECT_B3_RESP");
					break; 	
					case PIMCapiMessage.CONNECT_CONF:
						this.m_logger.info("unhandled message type: CONNECT_CONF");
					break; 		
					case PIMCapiMessage.CONNECT_REQ:
						this.m_logger.info("unhandled message type: CONNECT_REQ");
					break;
					case PIMCapiMessage.CONNECT_RESP:
						this.m_logger.info("unhandled message type: CONNECT_RESP");
					break;
					case PIMCapiMessage.LISTEN_CONF:
						this.m_logger.info("unhandled message type: LISTEN_CONF");
					break;
					case PIMCapiMessage.LISTEN_REQ:
						this.m_logger.info("unhandled message type: LISTEN_REQ");
					break;
					case PIMCapiMessage.INFO_RESP:
						this.m_logger.info("unhandled message type: INFO_RESP");
					break;
					case PIMCapiMessage.DISCONNECT_B3_CONF:
						this.m_logger.info("unhandled message type: DISCONNECT_B3_CONF");
					break;
					case PIMCapiMessage.DISCONNECT_B3_IND:
						this.m_logger.info("unhandled message type: DISCONNECT_B3_IND");
					break;
					case PIMCapiMessage.DISCONNECT_B3_REQ:
						this.m_logger.info("unhandled message type: DISCONNECT_B3_REQ");
					break;
					case PIMCapiMessage.DISCONNECT_B3_RESP:
						this.m_logger.info("unhandled message type: DISCONNECT_B3_RESP");
					break;
					case PIMCapiMessage.DISCONNECT_CONF:
						this.m_logger.info("unhandled message type: DISCONNECT_CONF");
					break;						
					case PIMCapiMessage.DISCONNECT_REQ:
						this.m_logger.info("unhandled message type: DISCONNECT_REQ");
					break;
					case PIMCapiMessage.DISCONNECT_RESP:
						this.m_logger.info("unhandled message type: DISCONNECT_RESP");
					break;	
					case PIMCapiMessage.DATA_B3_CONF:
						this.m_logger.info("unhandled message type: DATA_B3_CONF");
					break;
					case PIMCapiMessage.DATA_B3_IND:
						this.m_logger.info("unhandled message type: DATA_B3_IND");
					break;	
					case PIMCapiMessage.DATA_B3_REQ:
						this.m_logger.info("unhandled message type: DATA_B3_REQ");
					break;
					case PIMCapiMessage.DATA_B3_RESP:
						this.m_logger.info("unhandled message type: DATA_B3_RESP");
					break;
					case PIMCapiMessage.FACILITY_IND:
						this.m_logger.info("unhandled message type: FACILITY_IND");
					break;
					case PIMCapiMessage.FACILITY_CONF:
						this.m_logger.info("unhandled message type: FACILITY_CONF");
					break;
					case PIMCapiMessage.FACILITY_RESP:
						this.m_logger.info("unhandled message type: FACILITY_RESP");
					break;
				}
			} catch (Exception ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
			this.m_logger.exiting(CapiMonitor.class.getName(), "capiEventRaised");
		}

		public void capiExceptionThrown(CapiException ex) {
			this.m_logger.severe(ex.getMessage());
		}

		public void received(IEvent event) {
	        if (event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_CALL) {
	        	ICall c = (ICall)event.getData();
	        	if (c!=null) {
	        		this.m_logger.info("Putting identified call to active connections: "+c);
	        		
	        		String callKey = this.getCallKeyForMap(c);
	        		
	        		Iterator iter = this.m_connections.keySet().iterator();
	        		Object foundKey = null;
	        		synchronized(this.m_connections) {
	        			String mapcallKey = null;
	        			ICall call = null;
	        			Object key = null;
	        			PIMCapiData pcd = null;
	        			while (iter.hasNext()) {
							key = iter.next();
							pcd = (PIMCapiData) this.m_connections.get(key);
							if (pcd!=null) {
								call = pcd.getCall();
								mapcallKey = this.getCallKeyForMap(call);
								if (callKey.equalsIgnoreCase(mapcallKey)) foundKey = key;
							}
	        			}
	        		}
	        		if (foundKey!=null) {
	        			this.m_logger.info("Found key #"+foundKey+". adding identified call.");
	        			this.m_connections.put(foundKey, new PIMCapiData(c));
	        		} else {
	        			// 2005/04/20: reduced loglevel to info
	        			this.m_logger.info("Could not find a valid connection for call: "+c);
	        		}
	        		
	        	} else {
	        		this.m_logger.severe("Invalid identified call. Call is dropped.");
	        	}
	        }
		}

		public String getReceiverID() {
			return this.ID;
		}

		public int getPriority() {
			return 0;
		}
		
		public String toString() {
			return this.ID;
		}
		
		protected void releaseCapi() {
			if (this.capi!=null)
				this.capi.removeListener(appID, this);
			this.plciList.clear();
			IEventBroker broker = this.getRuntime().getEventBroker();
			broker.unregister(this, broker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));			
		}
		
		protected void reject(short cause) {
			this.m_logger.entering(CapiMonitor.class.getName(), "reject");

			Object plci = this.getLastPLCI();
			
			if (plci==null) {
				this.m_logger.warning("No physical connection available. Reject is discarded.");
			}
			
			try {
				PIMCapiMessage m = PIMCapiMessageFactory.createConnectRespForRejec(capi, appID, plci, cause);
				capi.putMessage(m);
				this.m_logger.info("Send CONNECT_RESP to CAPI: Cause #"+cause);
				
				// added 2010/12/09: added wait for connect_act_ind
				int count  = 0;
				while (!this.m_isConnectActInd && count <5) {
					count ++;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
				this.m_logger.info("CONNECT_ACTIVE_IND is "+this.m_isConnectActInd);
				
				m = PIMCapiMessageFactory.createDisconnectReq(capi, appID, plci);
				capi.putMessage(m);
				this.m_logger.info("Send DISCONNECT_REQ to CAPI.");
			} catch (CapiException ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
			}	catch (NullPointerException ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
			} finally {
				this.m_isConnectActInd = false;
			}
			this.m_logger.exiting(CapiMonitor.class.getName(), "reject");
		}

		protected void setListener(IMonitorListener jml) {
			if (jml != null) {
				this.jml = jml;
			}
		}
		
		protected int getControllerCount() {
			try {
				return this.capi.getNumberOfControllers();
			} catch (CapiException e) {
				this.m_logger.warning(e.getMessage());
			}
			return 0;
		}
	
		protected String getManufacturer() {
			try {
				return this.capi.getManufacturer();
			} catch (CapiException e) {
				this.m_logger.warning(e.getMessage());
			}
			return "";
		}
	
		protected String getImplementationInfo() {
			return this.capi.getImplementationInfo();
		}
	
		protected String getSerialNumber() {
			try {
				return this.capi.getSerialNumber();
			} catch (CapiException e) {
				this.m_logger.warning(e.getMessage());
			}
			return "";
		}
	
		protected String getCapiVersion() {
			String version = "";
			try {
				version = new Integer(this.capi.getVersion()[0]).toString() + 
						  "." + new Integer(this.capi.getVersion()[1]).toString() +
						  "." + new Integer(this.capi.getVersion()[2]).toString() +
						  "." + new Integer(this.capi.getVersion()[3]).toString();
			} catch (CapiException e) {
				this.m_logger.warning(e.getMessage());
			} 
			return version;
		}
		
		protected void setConfiguration(Properties config) {
			this.m_configuration = config;
		}
		
	    private String getCallKeyForMap(ICall call) {
	    	if (call!= null && call.getCaller()!=null && call.getMSN()!=null) {
	        	IPhonenumber pn = call.getCaller().getPhoneNumber();
	        	if (pn!=null) {
	            	StringBuffer key = new StringBuffer();
	        		if (pn.isClired()) {
	        			key.append(IJAMConst.CLIRED_CALL);
	           		} else {
	           			String tpn = pn.getTelephoneNumber();   			
	           			key.append(tpn.substring(Math.max(0, tpn.length()-4), tpn.length()));	
	        		}
	        		key.append(SEPARATOR);
	        		key.append(call.getMSN().getMSN());
	        		key.append(SEPARATOR);
	        		IAttribute att = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_BCHANNEL);
	        		if (att!=null) {
	        			key.append(att.getValue());
	        		}
	        		this.m_logger.info("Call key: "+key.toString());
	        		return key.toString();
	        	}
	        	this.m_logger.severe("Could not create key. Invalid phonenumber.");
	    	} else {
	    		this.m_logger.severe("Could not create key. Invalid call or caller information.");
	    	}
	    	return null;
	    }
		
		private IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}
		
		private IMonitorListener getListener() {
			return this.jml;
		}

		private void register() throws MonitorException {
			this.m_logger.entering(CapiMonitor.class.getName(), "register");
			try {
				this.plciList = new ArrayList(this.getMaxConnections());
				
				// register this application
				appID = capi.register(this.getMaxConnections(), this.getMaxBlock(), this.getBlockSize());
				this.m_logger.info("jAnrufmonitor registered @ CAPI with ID "+appID);
				this.m_logger.info("Monitoring # of lines: "+this.getMaxConnections());
				this.m_logger.info("Max. blocks sent to CAPI: "+this.getMaxBlock());
				this.m_logger.info("Size [bytes] of a block sent to CAPI: "+this.getBlockSize());

				// build listen request
				PIMCapiMessage msg = PIMCapiMessageFactory.createListenReq(this.capi, appID);

				// send listen request
				capi.putMessage(msg);
				this.m_logger.info("Send LISTEN_REQ to CAPI.");

				// wait for listen conf
				PIMCapiMessage answer = null;
				boolean retry = true;
				do {
					try {
						answer = (PIMCapiMessage) capi.getMessage(appID);
						retry = false;
					} catch (CapiException e) {
						if (e.getCapiCode() != 0x1104) {
							// "queue is empty"
							this.m_logger.severe("Capi error code "+e.getCapiCode()+" occured. Unregistering application from CAPI.");
							throw e;
						}
					}
				} while (retry);

				// read message
				if (answer.getType() != CapiMessage.LISTEN_CONF) {
					throw new CapiException("unexpected response to listen request: 0x" + Integer.toHexString(answer.getType()));
				}
				
				this.m_logger.info("Received LISTEN_CONF from CAPI.");

				int info = answer.getWordValue("Info");
				if (info != 0) {
					throw new CapiException("listen request error: 0x" + Integer.toHexString(info));
				}

				capi.addListener(appID, this);
				this.m_logger.info("Added new CapiListener.");
			} catch (CapiException ex) {
				throw new MonitorException(ex.getMessage());
			}
			this.m_logger.exiting(CapiMonitor.class.getName(), "register");
		}
		
		private int getMaxConnections() {
			return Integer.parseInt(this.m_configuration.getProperty(this.CFG_MAX_CONNECTION, "2"));
		}
		
		private int getMaxBlock() {
			return Integer.parseInt(this.m_configuration.getProperty(this.CFG_MAX_BLOCK, "2"));
		}
		
		private int getBlockSize() {
			return Integer.parseInt(this.m_configuration.getProperty(this.CFG_BLOCK_SIZE, "128"));
		}
		
		private boolean isBusyOnBusy() {
			return (this.m_configuration.getProperty(this.CFG_BUSY, "false").equalsIgnoreCase("true") ? true : false);
		}
		
		private boolean isSpoofing() {
			return (this.m_configuration.getProperty(this.CFG_SPOOFING, "false").equalsIgnoreCase("true") ? true : false);
		}
		
		private void putPLCI(Object plci) {
			if (this.plciList!=null) {
				if (!this.plciList.contains(plci)) {
					this.plciList.add(plci);
					this.m_logger.info("Currently hold PLCI connections: "+this.plciList.toString());
				}
			}
		}
		
		private Object getLastPLCI() {
			if (this.plciList.size()>0){
				return this.plciList.get(this.plciList.size()-1);
			}
			return null;
		}
		
		private void removePLCI(Object plci) {
			if (this.plciList.contains(plci)) {
				this.plciList.remove(plci);
				this.m_logger.info("Currently hold PLCI connections: "+this.plciList.toString());
			}
		}
		
		private boolean containsPLCI(Object plci) {
			return this.plciList.contains(plci);
		}
		
		private int getCipFromCapiMessage(PIMCapiMessage m) {
			try {
				short cip = m.getWordValue("CIP Value");
				if (cip==0) {
					this.m_logger.warning("CIP Value not set. Get CIP from BC.");
					cip = (short) PIMCapiMessageFactory.getCipFromBc(m);
				}
				return cip;
			} catch (CapiException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			return 999;
		}
		
		private boolean isSpoofingCall(PIMCapiMessage msg) throws CapiException {
			if (msg.getType()!=PIMCapiMessage.CONNECT_IND) {
				this.m_logger.severe("CAPI_MESSAGE has wrong message type: "+msg.getType()+"!="+PIMCapiMessage.CONNECT_IND);
				return false;
			}
			
			byte[] cln2 = msg.getStructValue("Calling party number 2");
			return (cln2.length > 2);
		}
		
		private String getUnspoofedCallingParty(PIMCapiMessage msg) throws CapiException {
			if (msg.getType()!=PIMCapiMessage.CONNECT_IND) {
				this.m_logger.severe("CAPI_MESSAGE has wrong message type: "+msg.getType()+"!="+PIMCapiMessage.CONNECT_IND);
				return "";
			}
			
			//added 2010/04/30: for test purposes to avoid spammer numbers
			byte[] cln2 = msg.getStructValue("Calling party number 2");
			if (cln2.length > 2) {
				String calling = new String(ByteArray.getBytes(cln2, 2, cln2.length - 2));
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Calling party number 2 detected: " + calling);
				
				return calling;
			}
			return "";
		}
		
		private String getCallingPartyFromCapiMessage(PIMCapiMessage msg) throws CapiException {
			if (msg.getType()!=PIMCapiMessage.CONNECT_IND) {
				this.m_logger.severe("CAPI_MESSAGE has wrong message type: "+msg.getType()+"!="+PIMCapiMessage.CONNECT_IND);
				return "";
			}
			
			// check if simulation file exists
			File simulationDataFile = new File(PathResolver.getInstance(this.getRuntime()).getDataDirectory() + this.SIM_CAPI_MSG_FILE);
			if (simulationDataFile.exists() && simulationDataFile.isFile()) {
				try {
					FileInputStream in = new FileInputStream(simulationDataFile);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					Stream.copy(new BufferedInputStream(in), bos, true);
					return bos.toString();
				} catch (FileNotFoundException e) {
					this.m_logger.severe("Error while reading simulation file: "+e.getMessage());
				} catch (IOException e) {
					this.m_logger.severe("Error while reading simulation file: "+e.getMessage());
				}
				this.m_logger.info("Processing regular CAPI data ...");
			}
			
			byte[] cln = msg.getStructValue("Calling party number");

			String calling = "restricted";
			int typeOfCall = 0;
			if (cln.length > 2) {
				typeOfCall = cln[0] & 0x70 ;
				calling = new String(ByteArray.getBytes(cln, 2, cln.length - 2));
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Calling party number detected: " + calling);
				
				if (typeOfCall==0x00) {
					this.m_logger.warning("recognized calling party number as unknown.");
				}	
				if (typeOfCall==0x10) {
					if (!calling.startsWith(PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).getIntAreaPrefix()))
						calling = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).getIntAreaPrefix() + calling;
					// 2009/08/03: removed				
//					if (PhonenumberInfo.isAutoDetectInternational())
//						PhonenumberInfo.setDetectedNumberAttributes(calling, true);
					if (this.m_logger.isLoggable(Level.INFO))
						if (this.m_logger.isLoggable(Level.INFO))this.m_logger.info("recognized calling party number as international call.");
				}
				if (typeOfCall==0x20) {
					// 2009/08/03: removed
//					if (PhonenumberInfo.isAutoDetectNational())
//						PhonenumberInfo.setDetectedNumberAttributes(calling, false);
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("recognized calling party number as national call.");
				}
				if (typeOfCall==0x30) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("recognized calling party number as network specific number.");
				}
				if (typeOfCall==0x40) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("recognized calling party number as subscriber number.");
				}
				if (typeOfCall==0x60) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("recognized calling party number as abbreviated number.");
				}
				if (typeOfCall==0x70) {
					this.m_logger.warning("recognized invalid calling party number.");
				}
			} else if (cln.length > 1) {
				typeOfCall = cln[0] & 0x70 ;
				int pi = cln[1] & 0x60;
			
				if (pi == 0x20) {
					calling = "restricted";
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("presentation of calling party number restricted.");
				}
				if (pi == 0x40) {
					calling = "restricted";
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("calling party number not available due to interworking.");
				}
			
				pi = cln[1] & 0x3;
			
				if (pi == 0x0) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("calling party number: user-provided, not screened.");
				}
				if (pi == 0x1) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("calling party number: user-provided, verified and passed.");
				}
				if (pi == 0x2) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("calling party number: user-provided, verified and failed.");
				}
				if (pi == 0x3) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("calling party number: network-provided.");
				}
			}
			return calling;			
		}
		
		private String getCalledPartyFromCapiMessage(PIMCapiMessage msg) throws CapiException {
			if (msg.getType()!=PIMCapiMessage.CONNECT_IND) {
				this.m_logger.severe("CAPI_MESSAGE has wrong message type: "+msg.getType()+"!="+PIMCapiMessage.CONNECT_IND);
				return "";
			}
			
			byte[] cdn = msg.getStructValue("Called party number");
			String called = "";
			if (cdn.length > 1) {
				called = new String(ByteArray.getBytes(cdn, 1, cdn.length - 1));
			}
		
			if (called.trim().length()==0) {
				this.m_logger.warning("No called MSN was set. Setting default value=0 (austrian global number).");
				called = "0";
			}
			return called;
		}

		private void doConnectInd(PIMCapiMessage m) throws CapiException {
			this.m_logger.entering(CapiMonitor.class.getName(), "doConnectInd");
		
			this.m_logger.info("===>>> CALL CONNECTION START ...");

			Object plci = m.getValue("PLCI");
			int mid = m.getMessageID();
			if (!this.containsPLCI(plci)) {
				this.m_logger.info("Current PLCI connection: "+this.plciList.size());
				this.m_logger.info("Max. monitored lines: "+this.getMaxConnections());
				if (this.plciList.size()<this.getMaxConnections()) {
					// add new physical connection to plci list
					this.putPLCI(plci);
					String called = this.getCalledPartyFromCapiMessage(m);
					boolean isSpoofed = false;
					if (getRuntime().getMsnManager().isMsnMonitored(
							getRuntime().getMsnManager().createMsn(called))
						) {
						String calling = this.getCallingPartyFromCapiMessage(m);				
						if (isSpoofing() || System.getProperty(IJAMConst.SYSTEM_MONITOR_SPOOFING, "false").equalsIgnoreCase("true")) {
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Spoofing check is enabled.");
							if (isSpoofingCall(m)) {
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("Number is spoofed: "+calling);
								isSpoofed = true;
								calling = this.getUnspoofedCallingParty(m);
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("Unspoofing number: "+calling);
							}							
						}
						
						int ct = this.getCipFromCapiMessage(m);	
						
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("CONNECT_IND: Data read from CAPI: "+calling+", "+called+", "+ct);
						this.trace(calling+", "+called+", "+ct);
						
						PIMCapiData pcd = new PIMCapiData(calling, called, ct,isSpoofed);
						pcd.setPLCI(plci);
																		
						this.m_connections.put(plci, pcd);
						
						this.createLastCapiRawFile(calling, called, ct);

						this.getListener().doCallConnect(pcd.getCall());
						
						if (this.isBusyOnBusy()) {
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Busy-On-Busy mode is active.");
							m = PIMCapiMessageFactory.createConnectResp(capi, appID, mid, plci, (short)3);
							capi.putMessage(m);
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Send CONNECT_RESP to CAPI: Cause #"+3);
						} else {
							m = PIMCapiMessageFactory.createAlertReq(capi, appID, mid, plci);
							capi.putMessage(m);
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Send ALERT_REQ to CAPI.");
						}
						this.m_logger.exiting(CapiMonitor.class.getName(), "doConnectInd");	 
						return;
					} else{
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("New incoming connection, but MSN "+called+" is not monitored.");
					}
				} else {
					this.m_logger.warning("New incoming connection, but no available monitored line.");
				}
			}
			
			// send a ignore response
			this.m_logger.info("Call was ignored.");
			m = PIMCapiMessageFactory.createConnectResp(capi, appID, mid, plci, (short)1);
			capi.putMessage(m);
			this.m_logger.info("Send CONNECT_RESP to CAPI.");

			this.m_logger.exiting(CapiMonitor.class.getName(), "doConnectInd");	 			
		}

		private void createLastCapiRawFile(String calling, String called, int ct) {
			if (this.isTraceActive()) {
				File lastRawDataFile = new File(PathResolver.getInstance(this.getRuntime()).getDataDirectory() + this.LAST_CAPI_MSG_FILE);
				if (lastRawDataFile.getParentFile().exists()) {
					try {
						FileOutputStream out = new FileOutputStream(lastRawDataFile);
						StringBuffer raw = new StringBuffer();
						raw.append(calling);
						raw.append(";");
						raw.append(called);
						raw.append(";");
						raw.append(ct);
						ByteArrayInputStream bin = new ByteArrayInputStream(raw.toString().getBytes());
						Stream.copy(new BufferedInputStream(bin), out, true);
					} catch (FileNotFoundException e) {
						this.m_logger.severe("Error while writing last raw capi file: "+e.getMessage());
					} catch (IOException e) {
						this.m_logger.severe("Error while writing last raw capi file: "+e.getMessage());
					}
				}
			} else {
				File lastRawDataFile = new File(PathResolver.getInstance(this.getRuntime()).getDataDirectory() + this.LAST_CAPI_MSG_FILE);
				if (lastRawDataFile.exists()) {
					this.m_logger.info("Found "+lastRawDataFile.getAbsolutePath()+" for deletion.");
					if (!lastRawDataFile.delete()) lastRawDataFile.deleteOnExit();
				}
			}
		}

		private void doDisconnectInd(PIMCapiMessage m) throws CapiException {
			this.m_logger.entering(CapiMonitor.class.getName(), "doDisconnectInd");
		 		
			Object plci = m.getValue("PLCI");

			if (this.isBusyOnBusy()) {
				PIMCapiData pcd = (PIMCapiData)this.m_connections.get(plci);		
				Object o = pcd;
				int count = 0;
				while (o==pcd && o!=null && count<100) {
					o = this.m_connections.get(plci);
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
					count++;
					if (o==null) o = pcd;
				}
				this.m_logger.info("Counter for Call finder set to #"+count+", object is "+o);
				if (o!=null)
					pcd = (PIMCapiData) o;
			}
			
			PIMCapiData pcd = (PIMCapiData)this.m_connections.remove(plci);		
			if (pcd!=null) {	
				short reason = m.getWordValue("Reason");

				this.m_logger.info("Active call map: "+this.m_connections.toString());
				if (pcd.getCall()!=null) {
					pcd.getCall().setAttribute(this.getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_REASON, this.getReason(reason)));
					this.getListener().doCallDisconnect(pcd.getCall());	
				} else {
					this.m_logger.warning("Call for physical line #"+plci+" not found in active call map.");
				}
			} else {
				this.m_logger.warning("DISCONNECT_IND: Call for physical line #"+plci+" not found in connections map.");
			}
			
			int mid = m.getMessageID();
			m = PIMCapiMessageFactory.createDisconnectResp(capi, appID, mid, plci);
			capi.putMessage(m);
			this.m_logger.info("Send DISCONNECT_RESP to CAPI.");
			this.m_logger.info("===>>> CALL CONNECTION END ...");
			this.m_logger.exiting(CapiMonitor.class.getName(), "doDisconnectInd");
		}

		private String getReason(int reason) {
			switch (reason) {
				case 13466:
					return Integer.toString(IEventConst.EVENT_TYPE_CALLACCEPTED);
				case 13312:
					return Integer.toString(IEventConst.EVENT_TYPE_MANUALCALLACCEPTED);
				default:
					return Integer.toString(IEventConst.EVENT_TYPE_CALLCLEARED);
			}
		}
		
		private boolean isTraceActive() {
			String trace = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_TRACE);
			if (trace!=null) {
				return trace.equalsIgnoreCase("true");
			}
			return false;
		}
		
		private void trace(String message) {
			if (this.isTraceActive())
				this.m_tracer.info(message);
		}
	}
	
	private String ID = "CapiMonitor";
	private String NAMESPACE = "monitor.CapiMonitor";
	
	private String CFG_AVAILABLE = "available";
	
	private Logger m_logger;
	private Properties m_configuration;
	private boolean started;
	
	private CapiMonitorNotifier cmn = null;
	private Thread cmnThread = null;
	
	public CapiMonitor() {
		super();
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
	}

	public void start() {
		if (this.cmn==null) {
			this.m_logger.severe("No CapiMonitorNotifier registered. Could not register on CAPI.");
			this.stop();
			return;
		}
		
		if (cmnThread==null){
			this.cmn.setConfiguration(this.m_configuration);
			this.cmnThread = new Thread(cmn);
			this.cmnThread.setName("JAM-"+cmn.toString()+"-Thread-(deamon)");
			this.cmnThread.setDaemon(true);
			this.cmnThread.start();	
			this.m_logger.info("New thread for CapiMonitorNotifier created.");
		} else {
			this.m_logger.warning("A CapiMonitorNotifier thread is still running, could not create a new one.");
		}
		
		this.started = true;
        this.m_logger.info("CapiMonitor started.");
	}

	public void stop() {
        this.release();
		this.m_logger.info("CapiMonitor stopped.");
	}

	public void setListener(IMonitorListener jml) {
       	if (this.cmn==null) {
       		this.cmn = new CapiMonitorNotifier();
       		this.cmn.setListener(jml);
       	} else {
       		this.cmn.setListener(jml);
       	}
	}

	public void reject(short cause) {
		if (this.cmn!=null) {
			this.cmn.reject(cause);
			this.m_logger.info("CapiMonitor rejected.");
		}
	}

	public void release() {
		if (this.cmn!=null) {
			this.cmn.releaseCapi();
		}
		
		int count = 0;
		
		while (this.cmnThread!=null && this.cmnThread.isAlive() && count < 5) {
			this.m_logger.info("Try to release CAPI. Attempt #"+(count+1));
			count ++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		
		if (this.cmnThread!=null && this.cmnThread.isAlive()) {
			this.m_logger.warning("Could not release CAPI after "+count+" attempts. CAPI will be released by nulling reference.");
		}
		this.cmnThread = null;
		
		this.m_logger.info("CapiMonitor released CAPI.");
    	this.started = false;
	}

	public boolean isStarted() {
		return this.started;
	}

	public String[] getDescription() {
		if (this.cmn==null) {
			this.m_logger.warning("CAPI Info not available.");
			return new String[] {
				"","","","",""
			};
		}
		
		String[] info = new String[5];

		info[0] = this.cmn.getManufacturer();
		info[1] = this.cmn.getImplementationInfo();
		info[2] = this.cmn.getSerialNumber();
		info[3] = "Controller: " + this.cmn.getControllerCount();
		info[4] = "CAPI version: " + this.cmn.getCapiVersion();

		return info;
	}

	public String getID() {
		return this.ID;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public String getConfigurableID() {
		return this.getID();
	}

	public void setConfiguration(Properties configuration) {
		this.m_configuration = configuration;		
		if (this.isStarted()){
			this.stop();
			this.start();
		}
	}

	public boolean isAvailable() {
		return this.m_configuration.getProperty(CFG_AVAILABLE, "false").equalsIgnoreCase("true");
	}

}
