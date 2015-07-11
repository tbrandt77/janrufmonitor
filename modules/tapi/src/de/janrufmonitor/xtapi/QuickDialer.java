package de.janrufmonitor.xtapi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import net.xtapi.serviceProvider.IXTapiCallBack;
import net.xtapi.serviceProvider.MSTAPI;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.PIMRuntime;

public class QuickDialer implements IXTapiCallBack {
	
	public static synchronized String[] getAllExtensions() throws Exception {
		List l = new ArrayList();
		final MSTAPI m_tapi = new MSTAPI();
		int n = m_tapi.init(new QuickDialer());
		StringBuffer nameOfLine = null;
		
		for (int i = 0; i < n; i++) {
			nameOfLine = new StringBuffer();
			int m_lineHandle = m_tapi.openLineTapi(i, nameOfLine);
			if (m_lineHandle != 0) {
				l.add(nameOfLine.toString());
			}
		}
		
		Thread t = new Thread() {
			
			public void run() {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
				try {
					m_tapi.shutdownTapi();
				} catch (Exception e) {}
			}
		};
		t.setName("JAM-XTapiGetAllExtension-Thread-(non-deamon)");
		t.start();
		
		String[] r = new String[l.size()];
		for (int i=0;i<r.length;i++) {
			r[i] = (String) l.get(i);
		}
		return r;		
	}
	
	public static void dial(IPhonenumber number, String ext) throws Exception {
		if (number.isClired()) throw new Exception ("no number provided");
		
		if (ext==null || ext.trim().length()==0) throw new Exception ("no extension provided");
		
		String dial = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toCallable(number.getTelephoneNumber());
		// added 2010/03/06: check for dial prefix for outgoing calls
		if (PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
			dial = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
		}
		
		final MSTAPI m_tapi = new MSTAPI();
		int n = m_tapi.init(new QuickDialer());
		int callhandle = 0;
		int line = 0;
		StringBuffer nameOfLine = null;

		for (int i = 0; i < n; i++) {
			nameOfLine = new StringBuffer();
			int m_lineHandle = m_tapi.openLineTapi(i, nameOfLine);
			if (m_lineHandle > 0 && nameOfLine.toString().equalsIgnoreCase(ext)) {
				callhandle = m_lineHandle;
				line = i;
				break;
			}
		}
		
		if (callhandle>0) {
			LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER).info("dialing line "+line+", number "+dial+", handle "+callhandle);
			m_tapi.connectCallTapi(line, dial, callhandle);
		} else {
			throw new Exception("extension ["+ext+"] not found.");
		}
		Thread t = new Thread() {
			
			public void run() {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
				m_tapi.shutdownTapi();
			}
		};
		t.setName("JAM-XTapiCallto-Thread-(non-deamon)");
		t.start();
	}

	public void callback(int dwDevice, int dwMessage, int dwInstance,
			int dwParam1, int dwParam2, int dwParam3) {
		
	}
}
