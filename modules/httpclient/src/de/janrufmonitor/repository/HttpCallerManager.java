package de.janrufmonitor.repository;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.repository.types.ISearchableCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.client.Client;
import de.janrufmonitor.service.client.HttpImageProvider;
import de.janrufmonitor.service.client.ImageCache;
import de.janrufmonitor.service.client.request.handler.CallerGetHandler;
import de.janrufmonitor.service.client.request.handler.CallerListGetHandler;
import de.janrufmonitor.service.client.request.handler.CallerListRemoveHandler;
import de.janrufmonitor.service.client.request.handler.CallerListSetHandler;
import de.janrufmonitor.service.client.request.handler.CallerListUpdateHandler;
import de.janrufmonitor.service.client.state.ClientStateManager;
import de.janrufmonitor.service.client.state.IClientStateMonitor;
import de.janrufmonitor.service.commons.CommonsConst;
import de.janrufmonitor.service.commons.CompressBase64;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IHttpResponse;
import de.janrufmonitor.service.commons.http.IRequester;
import de.janrufmonitor.service.commons.http.RequesterFactory;
import de.janrufmonitor.util.io.ImageHandler;
import de.janrufmonitor.xml.transformation.XMLSerializer;

public class HttpCallerManager extends AbstractReadWriteCallerManager implements IRemoteRepository, ISearchableCallerRepository {

	private String ID = "HttpCallerManager";
	private String NAMESPACE = "repository.HttpCallerManager";
	private String CFG_CM = "remote_repository";

	private IRuntime m_runtime;
	private String m_cm;
	
	public HttpCallerManager() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public void removeCaller(ICallerList callerList) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return;
		}
		
		IRequester r = null;
		IHttpResponse resp = null;
		
		r = this.getRequester(new CallerListRemoveHandler(callerList, this.getCallerManager()));
		resp = r.request();
		this.handleRequester(resp, r);

	}

	public void setCaller(ICallerList callerList) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return;
		}
		
		IRequester r = null;
		IHttpResponse resp = null;
	
		r = this.getRequester(new CallerListSetHandler(callerList, this.getCallerManager()));
		resp = r.request();
		this.handleRequester(resp, r);
	}
	
	public void setCaller(ICaller caller) {
		ICallerList cl = this.getRuntime().getCallerFactory().createCallerList();
		cl.add(caller);
		this.setCaller(cl);
	}

	public void removeCaller(ICaller caller) {
		ICallerList cl = this.getRuntime().getCallerFactory().createCallerList();
		cl.add(caller);
		this.removeCaller(cl);
	}

	public ICaller getCaller(IPhonenumber number)
		throws CallerNotFoundException {
		
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			throw new CallerNotFoundException("No caller found. Client is not yet connected with the server.");
		}
		
		IRequester r = this.getRequester(new CallerGetHandler(number, this.getCallerManager()));
		IHttpResponse resp = r.request();
		String xml = this.getXmlContent(resp);
		this.handleRequester(resp, r);
			
		ICaller c = XMLSerializer.toCaller(xml);	

		if (c!=null) {
			return c;
		}
			
		throw new CallerNotFoundException("no caller found.");
	}
	
	public ICallerList getCallers(IFilter[] filters, ISearchTerm[] searchTerms) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return this.getRuntime().getCallerFactory().createCallerList();
		}
		
		IRequester r = this.getRequester(new CallerListGetHandler(this.getCallerManager(), filters, searchTerms));
		IHttpResponse resp = r.request();

		String xml = this.getXmlContent(resp);
		this.handleRequester(resp, r);

		ICallerList l = XMLSerializer.toCallerList(xml);	
		if (l!=null) {	
			this.addCallerManagerAttribute(l);
			return l;
		}
	
		this.m_logger.warning("Callerlist from remote host was empty.");
		return this.getRuntime().getCallerFactory().createCallerList();
	}
	
	public ICallerList getCallers(IFilter[] filters) {
		return this.getCallers(filters, null);
	}
	
	public ICallerList getCallers(IFilter filter) {
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return this.getRuntime().getCallerFactory().createCallerList();
		}
		
		if (filter!=null) {
			return this.getCallers(new IFilter[] {filter});
		}
		this.m_logger.info("No filter is applied.");

		IRequester r = this.getRequester(new CallerListGetHandler(this.getCallerManager()));
		IHttpResponse resp = r.request();

		String xml = this.getXmlContent(resp);
		this.handleRequester(resp, r);
		
		ICallerList l = XMLSerializer.toCallerList(xml);	
		if (l!=null) {
			this.addCallerManagerAttribute(l);
			return l;
		}
			
		this.m_logger.warning("Callerlist from remote host was empty.");
		return this.getRuntime().getCallerFactory().createCallerList();
	}
	
	private void addCallerManagerAttribute(ICallerList l) {
		// add caller manager attribute to callers
		for (int i=0;i<l.size();i++) {
			ICaller c = (ICaller)l.get(i);
			if (c!=null) {
				IAttribute cm = this.getRuntime().getCallerFactory().createAttribute(
						IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
						this.getID()
					);
					c.setAttribute(cm);
			}
		}
	}

	public String getID() {
		return this.ID;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public void updateCaller(ICaller caller) {
		ICallerList cl = this.getRuntime().getCallerFactory().createCallerList();
		cl.add(caller);
		
		if (!this.isConnected()) {
			this.m_logger.warning("Client is not yet connected with the server.");
			return;
		}
		
		IRequester r = null;
		IHttpResponse resp = null;
	
		r = this.getRequester(new CallerListUpdateHandler(cl, this.getCallerManager()));
		resp = r.request();
		this.handleRequester(resp, r);
		
		ImageCache.getInstance().remove(caller.getPhoneNumber().getTelephoneNumber());
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
	
	private boolean isConnected() {
		IService client = PIMRuntime.getInstance().getServiceFactory().getService("Client");
		if (client!=null && client instanceof Client) {
			return ((Client)client).isConnected();
		}
		return false;
	}
	
	private String getCallerManager() {
		if (this.m_cm==null) {
			this.m_cm = this.m_configuration.getProperty(CFG_CM, "CallerDirectory");
		}
		return this.m_cm;
	}
	

	public void shutdown() {
		this.m_cm = null;
		ImageHandler.getInstance().removeProvider(new HttpImageProvider());
		ImageHandler.getInstance().removeProvider(new HttpImageProvider(getID()));
		super.shutdown();
	}

	public void startup() {
		super.startup();
		ImageHandler.getInstance().addProvider(new HttpImageProvider());
		ImageHandler.getInstance().addProvider(new HttpImageProvider(getID()));
	}



}
