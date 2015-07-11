package de.janrufmonitor.service.server.http.simple.handler;

import java.lang.reflect.Method;
import java.net.URLDecoder;

import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IMutableHttpResponse;
import de.janrufmonitor.service.commons.http.handler.HandlerException;
import de.janrufmonitor.service.commons.http.simple.handler.AbstractHandler;

public class Dial extends AbstractHandler {

	public void handleWithException(IHttpRequest req, IMutableHttpResponse resp) throws HandlerException {
		m_logger.info("Calling server Dial comamnd...");
		String calltoclass = null;
		if (PIMRuntime.getInstance().getMonitorListener().getMonitor("FritzBoxMonitor")!=null) {
			calltoclass = "de.janrufmonitor.fritzbox.QuickDialer";
		} else if (PIMRuntime.getInstance().getMonitorListener().getMonitor("XTapiMonitor")!=null) {
			calltoclass = "de.janrufmonitor.xtapi.QuickDialer";
		} else {
			throw new HandlerException("No FritzBoxMonitor or XTapiMonitor available.", 404);
		}
		
		try {
			Class handler = Thread.currentThread().getContextClassLoader().loadClass(calltoclass);
			Object o = handler.newInstance();
			String ext = req.getParameter(PARAMETER_EXTENSION);
			String dial = req.getParameter(PARAMETER_NUMBER);
			
			m_logger.info("Dial comamnd parameter dial="+dial+", ext="+ext);
			
			if (dial!=null && dial.length()>1) {
				dial = PhonenumberAnalyzer.getInstance(getRuntime()).toCallable(dial);
				IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(dial);
				try {
					Method m = o.getClass().getMethod("dial", new Class[] {IPhonenumber.class, String.class});
					if (ext!=null) {
						ext = URLDecoder.decode(ext, "UTF-8");
					}
					m.invoke(o, new Object[] {pn, ext});

				} catch (Exception ex) {
					throw new HandlerException("Could not dial number.", 500);
				}
			}
			resp.getContentStreamForWrite().close();
		} catch (HandlerException e) {
			throw e;
		} catch (ClassNotFoundException ex) {
			throw new HandlerException("Class not found: "+calltoclass, 500);
		} catch (InstantiationException e) {
			throw new HandlerException("Cannot instantiate class: "+calltoclass, 500);
		} catch (IllegalAccessException e) {
			throw new HandlerException("Illegal access for class: "+calltoclass, 500);
		} catch (Exception e) {
			throw new HandlerException(e.getMessage(), 500);
		}
	}

}
