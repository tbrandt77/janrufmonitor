package de.janrufmonitor.repository;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.repository.types.ISearchableCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.client.Client;
import de.janrufmonitor.service.client.request.handler.CallListCountGetHandler;
import de.janrufmonitor.service.client.request.handler.CallListGetHandler;
import de.janrufmonitor.service.client.request.handler.CallListRemoveHandler;
import de.janrufmonitor.service.client.request.handler.CallListSetHandler;
import de.janrufmonitor.service.client.state.ClientStateManager;
import de.janrufmonitor.service.client.state.IClientStateMonitor;
import de.janrufmonitor.service.commons.CommonsConst;
import de.janrufmonitor.service.commons.CompressBase64;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IHttpResponse;
import de.janrufmonitor.service.commons.http.IRequester;
import de.janrufmonitor.service.commons.http.RequesterFactory;
import de.janrufmonitor.xml.transformation.XMLSerializer;

public class HttpCallManager extends AbstractFilterCallManager implements IRemoteRepository, IWriteCallRepository, ISearchableCallRepository {

	private String ID = "HttpCallManager";
	private String NAMESPACE = "repository.HttpCallManager";
	private String CFG_CM = "remote_repository";

	private IRuntime m_runtime;
	private String m_cm;

	public HttpCallManager() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getID() {
		return this.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public void setCall(ICall call) {
		ICallList l = this.getRuntime().getCallFactory().createCallList();
		l.add(call);
		this.setCalls(l);
	}

	public void updateCall(ICall call) {
		this.removeCall(call);
		this.setCall(call);
	}

	public void updateCalls(ICallList callist) {
		this.removeCalls(callist);
		this.setCalls(callist);
	}

	public void removeCall(ICall call) {
		ICallList l = this.getRuntime().getCallFactory().createCallList();
		l.add(call);
		this.removeCalls(l);
	}

	public void setCalls(ICallList calllist) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return;
		}
		IRequester r = this.getRequester(new CallListSetHandler(calllist, this.getCallManager()));
		IHttpResponse resp = r.request();
		this.handleRequester(resp, r);
	}
	
	public void removeCalls(ICallList calllist) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return;
		}
		IRequester r = this.getRequester(new CallListRemoveHandler(calllist, this.getCallManager()));
		IHttpResponse resp = r.request();
		this.handleRequester(resp, r);
	}
	
	private IRequester getRequester(IHttpRequest request) {
		return RequesterFactory.getInstance().
			createRequester(request);
	}
	
	private void handleRequester(IHttpResponse r, IRequester req) {
		int rCode = r.getCode();
		if (rCode==0) {
			ClientStateManager.getInstance().fireState(IClientStateMonitor.SERVER_NOT_FOUND, "Server: "+req.getServer()+"\nPort: "+req.getPort());
		}
		
		if (rCode==403) {
			try {
				ClientStateManager.getInstance().fireState(IClientStateMonitor.SERVER_NOT_AUTHORIZED, "Client "+InetAddress.getLocalHost().getHostName()+" ("+InetAddress.getLocalHost().getHostAddress()+") is not authorized to connect to server: "+req.getServer());
			} catch (UnknownHostException e) {
				this.m_logger.severe(e.getMessage());
			}	
		}
	}
	
	private String getXmlContent(IHttpResponse r) {
		byte[] result = null;
		try {
			result = r.getContent();
			boolean isCompressed = (r.getParameter(CommonsConst.PARAMETER_COMPRESSION)!=null ? r.getParameter(CommonsConst.PARAMETER_COMPRESSION).equalsIgnoreCase("true"): false);
			if (isCompressed) {
				// assume it is compressed
				result = CompressBase64.decompressBase64Decode(result);
			}
		} catch (IOException e) {
			this.m_logger.severe("Error during content decompression: "+e.getMessage());
		} catch (Exception e) {
			this.m_logger.severe("Error during content retrieval: "+e.getMessage());
		}
		return (result==null ? "" : new String(result));
	}

	protected synchronized ICallList getInitialCallList(IFilter f) {
		return this.getCalls(new IFilter[] {f}, -1, -1, null);
	}
	
	public synchronized ICallList getCalls(IFilter[] filters, int count, int offset,
			ISearchTerm[] searchTerms) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return this.getRuntime().getCallFactory().createCallList();
		}
		
		IRequester r = this.getRequester(new CallListGetHandler(this.getCallManager(), filters, searchTerms));
		IHttpResponse resp = r.request();
		
		String xml = this.getXmlContent(resp);
		this.handleRequester(resp, r);
 
		if (xml!=null && xml.length()>0) { 
			ICallList l = XMLSerializer.toCallList(xml);	
			if (l!=null)
				return l;
		
			this.m_logger.warning("Calllist from remote host was empty.");
			
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "empty", new Exception("CallList from server has either a wrong format, contains forbidden characters or was empty.")));
		}
		
		return this.getRuntime().getCallFactory().createCallList();
	}

	public int getCallCount(IFilter[] filters, ISearchTerm[] searchTerms) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return 0;
		}
		
		IRequester r = this.getRequester(new CallListCountGetHandler(this.getCallManager(), filters, searchTerms));
		IHttpResponse resp = r.request();
		
		String xml = this.getXmlContent(resp);
		this.handleRequester(resp, r);
 
		if (xml.length()>0) {
			return Integer.parseInt(xml);
		}
	
		return super.getCallCount(filters);
	}
	
	public int getCallCount(IFilter[] filters) {
		return this.getCallCount(filters, null);
	}


	private boolean isConnected() {
		IService client = PIMRuntime.getInstance().getServiceFactory().getService("Client");
		if (client!=null && client instanceof Client) {
			return ((Client)client).isConnected();
		}
		return false;
	}
	
	private String getCallManager() {
		if (this.m_cm==null) {
			this.m_cm = this.m_configuration.getProperty(CFG_CM, "DefaultJournal");
		}
		return this.m_cm;
	}
	
	public void shutdown() {
		this.m_cm = null;
		super.shutdown();
	}


}

	