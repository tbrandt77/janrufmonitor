package de.janrufmonitor.service.server.http.simple.handler;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.commons.http.IHttpRequest;
import de.janrufmonitor.service.commons.http.IMutableHttpResponse;
import de.janrufmonitor.service.commons.http.handler.HandlerException;
import de.janrufmonitor.service.commons.http.simple.handler.AbstractHandler;

public class Callto extends AbstractHandler{

	public void handleWithException(IHttpRequest req, IMutableHttpResponse resp) throws HandlerException {
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
			String ext = req.getParameter(PARAMETER_CALLTO_EXTENSION);
			String getext = req.getParameter(PARAMETER_CALLTO_GET_EXTENSION);
			String type = req.getParameter(PARAMETER_CALLTO_RETURNTYPE);
			String dial = req.getParameter(PARAMETER_CALLTO_ACTION);
			StringBuffer xhtml = null;
			if (getext!=null && getext.equalsIgnoreCase("true")) {
				xhtml = generateXmlAllExtension(o, dial, ext);
				resp.setParameter("Content-Type", "text/xml");
			} else if (type!=null && type.equalsIgnoreCase("html")) {
				xhtml = generateHtml(o, dial, ext);
				resp.setParameter("Content-Type", "text/html");
			} else {
				xhtml = generateXml(o, dial, ext);
				resp.setParameter("Content-Type", "text/xml");
			}
			
			resp.setParameter("Content-Length", Long.toString(xhtml.length()));
			OutputStream ps = resp.getContentStreamForWrite();
			ps.write(xhtml.toString().getBytes());
			ps.flush();
			ps.close();
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
	
	private StringBuffer generateXmlAllExtension(Object o, String dial,
			String ext) throws UnsupportedEncodingException {
		StringBuffer xml = new StringBuffer();
		xml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
		xml.append(IJAMConst.CRLF);
		xml.append("<extensions>");

		try {
			Method m = o.getClass().getMethod("getAllExtensions", new Class[] {});
			if (m!=null) {
				Object retcode = m.invoke(o, new Object[] {});
				if (retcode!=null && retcode instanceof String[]) {
					xml.append(IJAMConst.CRLF);
					for (int i=0, j=((String[])retcode).length;i<j;i++) {
						xml.append("<extension id=\""+i+"\">");							
						xml.append(URLEncoder.encode(((String[])retcode)[i], "UTF-8"));
						xml.append("</extension>");
						xml.append(IJAMConst.CRLF);
					}
					
				}
			}
		} catch (Exception ex) {
		}
		
		xml.append("</extensions>");
		return xml;
	}

	private StringBuffer generateXml(Object o, String dial, String ext) throws UnsupportedEncodingException {
		StringBuffer xml = new StringBuffer();
		xml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
		xml.append(IJAMConst.CRLF);
		xml.append("<callto>");
		xml.append(IJAMConst.CRLF);
		xml.append("<status>");
		
		if (dial!=null && dial.length()>1) {
			dial = PhonenumberAnalyzer.getInstance().toCallable(dial);
			IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(dial);
			try {
				Method m = o.getClass().getMethod("dial", new Class[] {IPhonenumber.class, String.class});
				if (ext!=null) {
					ext = URLDecoder.decode(ext, "UTF-8");
				}
				m.invoke(o, new Object[] {pn, ext});
				xml.append("OK");
			} catch (Exception ex) {
				xml.append(URLEncoder.encode("ERROR: "+ex.getCause().getMessage(), "utf-8"));
				xml.append(IJAMConst.CRLF);
			}
		} else {
			xml.append("ERROR: no valid number to call");
			xml.append(IJAMConst.CRLF);
		}
		
		xml.append(IJAMConst.CRLF);
		xml.append("</status>");
		xml.append(IJAMConst.CRLF);
		if (dial!=null && dial.length()>1) {
			xml.append("<number>");
			xml.append(URLEncoder.encode(dial, "utf-8"));
			xml.append("</number>");
			xml.append(IJAMConst.CRLF);
		}
		if (ext!=null && ext.length()>1) {
			xml.append("<extension>");
			xml.append(URLEncoder.encode(ext, "utf-8"));
			xml.append("</extension>");
			xml.append(IJAMConst.CRLF);
		}		
		xml.append("</callto>");
		return xml;
	}

	private StringBuffer generateHtml(Object o, String dial, String ext) {
		StringBuffer html = new StringBuffer();
		html.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">"+IJAMConst.CRLF);
		html.append("<html>"+IJAMConst.CRLF);
		html.append("<head>"+IJAMConst.CRLF);
		html.append("<title>");
		html.append("Callto");
		html.append("</title>"+IJAMConst.CRLF);
		html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"+IJAMConst.CRLF);
		html.append("<link rel=\"stylesheet\" href=\"/config/web-config/styles/jam-styles.css\" type=\"text/css\">"+IJAMConst.CRLF);
		html.append("<link rel=\"SHORTCUT ICON\" href=\"/config/web-config/images/favicon.ico\" type=\"image/ico\" />");
		html.append("</head>"+IJAMConst.CRLF);
		html.append("<body>"+IJAMConst.CRLF);
		

		if (dial!=null && dial.length()>1) {
			dial = PhonenumberAnalyzer.getInstance().toCallable(dial);
			IPhonenumber pn = getRuntime().getCallerFactory().createPhonenumber(dial);
				try {
					Method m = o.getClass().getMethod("dial", new Class[] {IPhonenumber.class, String.class});
					if (ext!=null) {
						ext = URLDecoder.decode(ext, "UTF-8");
					}
					m.invoke(o, new Object[] {pn, ext});
					html.append("Call to 0");
					html.append(pn.getTelephoneNumber());
					html.append(" successfully established"+(ext!=null ? " on line "+ext : "")+". Take the head phone...");
					html.append(IJAMConst.CRLF);
				} catch (Exception ex) {
					html.append(ex.getMessage());
					html.append(IJAMConst.CRLF);
				}
	
		} else {
			html.append("Wrong callto syntax: http://&lt;server&gt;:&lt;port&gt;/callto?dial=&lt;number&gt;[&ext=&lt;extension&gt;]");
			html.append(IJAMConst.CRLF);
		}

		html.append("</body>"+IJAMConst.CRLF);
		html.append("</html>"+IJAMConst.CRLF);
		return html;
	}
}
