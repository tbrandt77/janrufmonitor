package de.janrufmonitor.service.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.service.client.http.simple.ClientHandler;
import de.janrufmonitor.service.client.request.handler.*;
import de.janrufmonitor.service.client.state.ClientStateManager;
import de.janrufmonitor.service.client.state.IClientStateMonitor;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IRequester;
import de.janrufmonitor.service.commons.http.RequesterFactory;
import de.janrufmonitor.service.commons.http.simple.SimplePortListener;

public class Client extends AbstractReceiverConfigurableService {

	private String ID = "Client";
	private String NAMESPACE = "service.Client";

	private String CFG_PORT = "port";
	private String CFG_AUTOCONNECT = "autoconnect";
	private String CFG_POPUP = "popupclass";
	private String CFG_EVENTS = "events";
	private String DEFAULT_PORT = "5554";

	private IRuntime m_runtime;
	
	private SimplePortListener m_httpSrv;
	private boolean isConnected;
	
	public Client() {
		super();
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
		this.initCsm();
	}

	private void initCsm() {
		String popup = this.m_configuration.getProperty(this.CFG_POPUP, "");
		if (popup.length()>0) {
			try {
				Class popupclass = Thread.currentThread().getContextClassLoader().loadClass(popup);
				if (popupclass!=null)
					popupclass.newInstance();
			} catch (ClassNotFoundException e) {
				this.m_logger.severe(e.getMessage());
			} catch (InstantiationException e) {
				this.m_logger.severe(e.getMessage());
			} catch (IllegalAccessException e) {
				this.m_logger.severe(e.getMessage());
			}
		}
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public String getID() {
		return this.ID;
	}

	public IRuntime getRuntime() {
		if(this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public void shutdown() {
		super.shutdown();
		
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));

		this.disconnect();
		
		ClientStateManager.getInstance().fireState(IClientStateMonitor.CONNECTION_CLOSED, "");

		this.m_logger.info("Client is shut down ...");
	}

	public void startup() {
		super.startup();
		
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
		
		// start HTTP Server
		if (this.isAutoConnect()) {
			if (this.connect()) {
				ClientStateManager.getInstance().fireState(IClientStateMonitor.CONNECTION_OK, "");			
			} else {
				ClientStateManager.getInstance().fireState(IClientStateMonitor.CONNECTION_CLOSED, "");
			}
		} else {
			ClientStateManager.getInstance().fireState(IClientStateMonitor.CONNECTION_CLOSED, "");
			this.m_logger.info("Client is started but not connected with server ...");
		}		
	}
	
	public synchronized boolean connect() {
		if (this.isConnected)
			return true;
		
		this.m_httpSrv = new SimplePortListener(
			new ClientHandler(),
			this.getListenPort()
		);
		
		this.m_httpSrv.start();
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			this.m_logger.severe(e.getMessage());
		}

		// register client at server
		if (this.m_httpSrv!=null) {
			try {
				IHttpRequest rq = new RegisterGetHandler(
						InetAddress.getLocalHost().getHostName(),
						InetAddress.getLocalHost().getHostAddress(),
						this.m_httpSrv.getPort(),
						this.getRegisterEvents());
				
				try {
					this.m_logger.info("Connecting to server with URI: "+rq.getURI());
				} catch (Exception e1) {
					this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
				}
				
				IRequester r =
					RequesterFactory.getInstance().createRequester(rq);
				r.request();
				
				int responseCode = r.request().getCode();
				
				if (responseCode==200) {
					this.isConnected = true;
					this.m_logger.info("Registration of client "+this.m_httpSrv.getServerIP()+" successfull.");
					this.m_logger.info("Client is started and connected with server ...");
					ClientStateManager.getInstance().fireState(IClientStateMonitor.CONNECTION_OK, "");
					return true;
				}
		
				if (responseCode==0){
					this.m_logger.severe("Registration of client "+this.m_httpSrv.getServerIP()+" failed. Server not reachable.");
					ClientStateManager.getInstance().fireState(IClientStateMonitor.SERVER_NOT_FOUND, "Server: "+r.getServer()+"\nPort: "+r.getPort());
				}
		
				if (responseCode==403) {
					this.m_logger.severe("Client "+InetAddress.getLocalHost().getHostName()+" ("+InetAddress.getLocalHost().getHostAddress()+") is not authorized to connect to server: "+r.getServer());
					ClientStateManager.getInstance().fireState(IClientStateMonitor.SERVER_NOT_AUTHORIZED, "Client "+InetAddress.getLocalHost().getHostName()+" ("+InetAddress.getLocalHost().getHostAddress()+") is not authorized to connect to server: "+r.getServer());	
				}
				
				this.m_logger.severe("Response code from server was not OK: "+responseCode);
			} catch (UnknownHostException e) {
				this.m_logger.severe(e.getMessage());
				ClientStateManager.getInstance().fireState(IClientStateMonitor.SERVER_NOT_FOUND, e.getMessage());
				
			}
			this.m_httpSrv.stop();
			this.m_httpSrv=null;	
		}
		return false;
	}
	
	public boolean isConnected() {
		return this.isConnected;
	}
	
	public synchronized boolean disconnect() {
		if (!this.isConnected)
			return true;
		
		// unregister client at server
		if (this.m_httpSrv!=null) {
			try {
				IRequester r =
						RequesterFactory.getInstance().createRequester(
							new UnregisterGetHandler(
								InetAddress.getLocalHost().getHostName(),
								InetAddress.getLocalHost().getHostAddress(),
								this.m_httpSrv.getPort())
								);
				int responseCode = r.request().getCode();
				
				if (responseCode==200) {
					this.m_logger.info("Unregistration of client "+this.m_httpSrv.getServerIP()+" successfull.");
				}
				
				if (responseCode==0)
					this.m_logger.severe("Unregistration of client "+this.m_httpSrv.getServerIP()+" failed. Server not reachable.");
	
				if (responseCode==403)
					this.m_logger.severe("Client "+this.m_httpSrv.getServerIP()+" was rejected by the server. Client is not allowd to connect to server.");
	
			} catch (UnknownHostException e) {
				this.m_logger.severe(e.getMessage());
			}
			this.isConnected = false;
			this.m_httpSrv.stop();
			this.m_httpSrv = null;
		}
		return true;
	}
	
	private int getListenPort() {
		String value = this.m_configuration.getProperty(this.CFG_PORT, this.DEFAULT_PORT);
		return (Integer.parseInt(value));
	}
	
	private List getRegisterEvents() {
		String events = this.m_configuration.getProperty(this.CFG_EVENTS, "");
		List eventList = new ArrayList();
		
		if (events.trim().length()>0) {
			StringTokenizer st = new StringTokenizer(events, ",");
			while (st.hasMoreTokens()) {
				eventList.add(st.nextToken().trim());
			}
		}
		
		return eventList;
	}
	
	public void received(IEvent event) {
		if (event.getType() == IEventConst.EVENT_TYPE_CALLREJECTED) {
			this.m_logger.info("Sending EVENT_TYPE_CALLREJECTED event to server.");
			IHttpRequest request = new RejectHandler((ICall)event.getData());
			this.sendRequest(IEventConst.EVENT_TYPE_CALLREJECTED, request);
		}
		if (event.getType() == IEventConst.EVENT_TYPE_IDENTIFIED_CALL) {
			ICall c = (ICall) event.getData();
			if (c!=null) {
				ClientCallMap.getInstance().setCall(c.getCaller().getPhoneNumber().getTelephoneNumber()+"/"+c.getMSN().getMSN(), c);
				this.m_logger.info("Updating call in ClienTCallMap: "+c);
			}
		}
		if (event.getType() == IEventConst.EVENT_TYPE_RETURNED_HIBERNATE) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Client detected hibernate return mode.");
			
			if (this.isConnected()) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Client detected hibernate return mode, and client is connected.");
				this.disconnect();
				
				if (this.connect()) {
					ClientStateManager.getInstance().fireState(IClientStateMonitor.CONNECTION_OK, "");		
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Client reconnected after hibernate return mode.");
				} else {
					ClientStateManager.getInstance().fireState(IClientStateMonitor.CONNECTION_CLOSED, "");
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Client not reconnected after hibernate return mode.");
				}
			} else 
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Client detected hibernate return mode, but is not connected. No re-connect.");
		}
		
	}
	
	private void sendRequest(int event, IHttpRequest request) {
		try {
			this.m_logger.info("Sending request to server: "+request.getURI());
		} catch (Exception e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		IRequester req = RequesterFactory.getInstance().createRequester(request);
		req.request();
	}
	
	public boolean isAutoConnect() {
		return (this.m_configuration.getProperty(CFG_AUTOCONNECT, "false").equalsIgnoreCase("true") ? true : false);
	}
	
	public List getDependencyServices() {
		List ds = new ArrayList(super.getDependencyServices());
		ds.add("TrayIcon");
		return ds;
	}

}


