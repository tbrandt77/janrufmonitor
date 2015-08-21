package de.janrufmonitor.service.server.http.simple.handler;

import java.io.OutputStream;

import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.filter.AbstractFilterSerializer;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.search.SearchTermSeriarlizer;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.types.ISearchableCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.commons.CompressBase64;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IMutableHttpResponse;
import de.janrufmonitor.service.commons.http.handler.HandlerException;
import de.janrufmonitor.service.commons.http.simple.handler.AbstractHandler;
import de.janrufmonitor.xml.transformation.XMLSerializer;

public class GetCallerList extends AbstractHandler {

	private class URLFilterManager extends AbstractFilterSerializer {		
		private IRuntime m_runtime;
		
		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}
		
	}
	
	public void handleWithException(IHttpRequest req, IMutableHttpResponse resp) throws HandlerException {
		ICallerManager mgr = null;
		String manager = null;
		boolean isCompression = false;
		try {
			manager = req.getParameter(GetCallerList.PARAMETER_CALLERMANAGER);
			isCompression = (req.getParameter(GetCallList.PARAMETER_COMPRESSION) != null ? req.getParameter(GetCallList.PARAMETER_COMPRESSION).equalsIgnoreCase("true"): false);
		} catch (Exception e) {
			throw new HandlerException(e.getMessage(), 500);
		}
		
		if (manager==null) mgr = this.getRuntime().getCallerManagerFactory().getDefaultCallerManager();
		
		if (manager!=null && manager.length()>0)
			mgr = this.getRuntime().getCallerManagerFactory().getCallerManager(manager);
		 
		if (mgr==null || !mgr.isActive() || !mgr.isSupported(IReadCallerRepository.class)) {
			throw new HandlerException("Requested Callermanager does not exist or is not active.", 404);
		}
		
		ICallerList l = this.getRuntime().getCallerFactory().createCallerList();
		
		String filter = null;
		try {
			filter = req.getParameter(GetCallerList.PARAMETER_FILTER);
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
			searchterms = new SearchTermSeriarlizer().getSearchTermsFromString(s);
		}
		
		if (filter==null || filter.length()==0) {
			if (mgr.isSupported(ISearchableCallerRepository.class)) {
				l = ((ISearchableCallerRepository)mgr).getCallers(new IFilter[] {}, searchterms);
			} else
				l = ((IReadCallerRepository)mgr).getCallers((IFilter)null);
			this.m_logger.info("Filter parameter &filter= was not set.");
		} else {
			IFilter[] f = new URLFilterManager().getFiltersFromString(filter);
			if (mgr.isSupported(ISearchableCallerRepository.class)) {
				l = ((ISearchableCallerRepository)mgr).getCallers(f, searchterms);
			} else
				l = ((IReadCallerRepository)mgr).getCallers(f);	
		}
		
		try {
			String xml = XMLSerializer.toXML(l, false);
			if (isCompression && xml.length()>1024) {
				this.m_logger.info("Compressing requested data.");
				this.m_logger.info("Uncompressed data size [bytes] : "+xml.length());
				xml = CompressBase64.compressBase64Encode(xml);
				this.m_logger.info("Compressed data size [bytes] : "+xml.length());
				resp.setParameter("Content-Type", "application/janrufmonitor-compressed");
				resp.setParameter(GetCallList.PARAMETER_COMPRESSION, "true");
			} else {
				resp.setParameter("Content-Type", "text/xml");
				resp.setParameter(GetCallList.PARAMETER_COMPRESSION, "false");
			}
			
			resp.setParameter("Content-Length", Long.toString(xml.length()));
			OutputStream ps = resp.getContentStreamForWrite();
			ps.write(xml.getBytes());
			ps.flush();
			ps.close();
		} catch (Exception e) {
			throw new HandlerException(e.getMessage(), 500);
		}	
	}

}
