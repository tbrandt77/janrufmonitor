package de.janrufmonitor.service.server.http.simple.handler;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.filter.AbstractFilterSerializer;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.MsnFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.search.SearchTermSeriarlizer;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.ISearchableCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IMutableHttpResponse;
import de.janrufmonitor.service.commons.http.handler.HandlerException;
import de.janrufmonitor.service.commons.http.simple.handler.AbstractHandler;
import de.janrufmonitor.service.server.security.SecurityManager;

public class GetCallListCount extends AbstractHandler {

	private class URLFilterManager extends AbstractFilterSerializer {		
		private IRuntime m_runtime;
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
		
	}
	
	public void handleWithException(IHttpRequest req, IMutableHttpResponse resp) throws HandlerException {
		ICallManager mgr = null;
		String manager = null;
		try {
			manager = req.getParameter(GetCallListCount.PARAMETER_CALLMANAGER);
		} catch (Exception e) {
			throw new HandlerException(e.getMessage(), 500);
		}
		
		if (manager==null) mgr = this.getRuntime().getCallManagerFactory().getDefaultCallManager();
		
		if (manager!=null && manager.length()>0)
			mgr = this.getRuntime().getCallManagerFactory().getCallManager(manager);
		 
		if (mgr==null || !mgr.isActive() || !mgr.isSupported(IReadCallRepository.class)) {
			throw new HandlerException("Requested Callmanager does not exist or is not active.", 404);
		}
		
		String filter = null;
		try {
			filter = req.getParameter(GetCallListCount.PARAMETER_FILTER);
		} catch (Exception e) {
			throw new HandlerException(e.getMessage(), 500);
		}
		
		String s = null;
		ISearchTerm[] searchterms = null;
		try {
			s = req.getParameter(GetCallList.PARAMETER_SEARCHTERMS);
		} catch (Exception e) {
			throw new HandlerException(e.getMessage(), 500);
		}
		
		if (s!=null && s.length()>0) {
			this.m_logger.info("SearchTerms: "+s);
			searchterms = new SearchTermSeriarlizer().getSearchTermsFromString(s);
		}
		
		int count = 0;
		
		if (filter==null || filter.length()==0) {
			this.m_logger.info("Filter parameter &filter= was not set.");
			if (mgr.isSupported(ISearchableCallRepository.class)) {
				count = ((ISearchableCallRepository)mgr).getCallCount(getAllowedMsnFilter(req), searchterms);	
			} else
				count = ((IReadCallRepository)mgr).getCallCount(getAllowedMsnFilter(req));			
		} else {
			IFilter[] f = new URLFilterManager().getFiltersFromString(filter);
			f = mergeFilters(f, getAllowedMsnFilter(req));
			if (mgr.isSupported(ISearchableCallRepository.class)) {
				count = ((ISearchableCallRepository)mgr).getCallCount(f, searchterms);	
			} else
				count = ((IReadCallRepository)mgr).getCallCount(f);					
		}
			
		try {
			String xml = Integer.toString(count);
			resp.setParameter("Content-Type", "text/plain");	
			resp.setParameter("Content-Length", Long.toString(xml.length()));
			OutputStream ps = resp.getContentStreamForWrite();
			ps.write(xml.getBytes());
			ps.flush();
			ps.close();
		} catch (Exception e) {
			throw new HandlerException(e.getMessage(), 500);
		}	
	}
	
	private IFilter[] mergeFilters(IFilter[] urlFilters, IFilter[] allowedMsnFilter) {
		if (urlFilters==null || urlFilters.length==0) return allowedMsnFilter;
		
		if (allowedMsnFilter==null || allowedMsnFilter.length==0) return urlFilters;
		
		List filters = new ArrayList();
		for (int i=0;i<allowedMsnFilter.length;i++) {
			filters.add(allowedMsnFilter[i]);
		}

		for (int i=0;i<urlFilters.length;i++) {
			if (urlFilters[i].getType() != FilterType.MSN)
				filters.add(urlFilters[i]);
		}
		
		m_logger.info("Merged list auf filters: "+filters.toString());
		
		IFilter[] f = new IFilter[filters.size()];
		for (int i=0;i<filters.size();i++) {
			f[i] = (IFilter) filters.get(i);
		}
		
		return f;
		
	}
	
	private IFilter[] getAllowedMsnFilter(IHttpRequest req) throws HandlerException {
		try {
			boolean allowedForAllMsns = (SecurityManager.getInstance().getAllowedMSNs(req.getInetAddress().getHostName())==null && SecurityManager.getInstance().getAllowedMSNs(req.getInetAddress().getHostAddress())==null);
			
			if (!allowedForAllMsns) {
				String[] allowedMSNs = SecurityManager.getInstance().getAllowedMSNs(req.getInetAddress().getHostName());
				if (allowedMSNs==null)
					allowedMSNs = SecurityManager.getInstance().getAllowedMSNs(req.getInetAddress().getHostAddress());
				
				if (allowedMSNs!=null) {
					
					IFilter[] allowedMsnFilter = new IFilter[allowedMSNs.length];
					IMsn msn = null;
					for (int i=0;i<allowedMSNs.length;i++) {
						msn = getRuntime().getMsnManager().createMsn(allowedMSNs[i]);
						allowedMsnFilter[i] = new MsnFilter(msn);
						m_logger.info("Adding allowed MSN filter for client: "+allowedMsnFilter[i].toString());
					}
					return allowedMsnFilter;
				}
			}
			
		} catch (Exception e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HandlerException(e.getMessage(), 500);
		}
		return new IFilter[0];
	}
}
