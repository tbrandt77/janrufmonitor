package de.janrufmonitor.xtapi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import net.xtapi.serviceProvider.IXTapiCallBack;
import net.xtapi.serviceProvider.TapiFactory;
import net.xtapi.serviceProvider.TapiFactory.TapiHandle;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.identify.PhonenumberAnalyzer;
import de.janrufmonitor.runtime.PIMRuntime;

public class QuickDialer implements IXTapiCallBack {
	
	public static synchronized String[] getAllExtensions() throws Exception {
		if (!TapiFactory.getInstance().isInitialized()) throw new Exception("TAPI is not initialized. Extensions could not be retrieved.");
		
		List l = new ArrayList();
		
		Iterator i = TapiFactory.getInstance().getTapiHandles().entrySet().iterator();
		while (i.hasNext()) {
			TapiHandle th = (TapiHandle) ((Map.Entry) i.next()).getValue();
			l.add(th.getName());
		}
		

		String[] r = new String[l.size()];
		for (int j=0;j<r.length;j++) {
			r[j] = (String) l.get(j);
		}
		return r;		
	}
	
	public static void dial(IPhonenumber number, String ext) throws Exception {
		if (!TapiFactory.getInstance().isInitialized()) throw new Exception("TAPI is not initialized. Dialing not possible.");
		
		if (number.isClired()) throw new Exception ("no number provided");
		
		if (ext==null || ext.trim().length()==0) throw new Exception ("no extension provided");
		
		String dial = PhonenumberAnalyzer.getInstance(PIMRuntime.getInstance()).toCallable(number.getTelephoneNumber());
		// added 2010/03/06: check for dial prefix for outgoing calls
		if (PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
			dial = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
		}
		
		
		Iterator i = TapiFactory.getInstance().getTapiHandles().entrySet().iterator();
		TapiHandle th = null;
		int line = 0;
		while (i.hasNext()) {
			th = (TapiHandle) ((Map.Entry) i.next()).getValue();
			if (th.getID() > 0 && th.getName().toString().equalsIgnoreCase(ext)) {
				break;
			}
			line++;
		}
		
		if (th!=null && th.getID() > 0) {
			LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER).info("dialing line "+line+", number "+dial+", handle "+th.getID());
			TapiFactory.getInstance().getTapi().connectCallTapi(line, dial, th.getID());
		} else {
			throw new Exception("extension ["+ext+"] not found.");
		}
	}

	public void callback(int dwDevice, int dwMessage, int dwInstance,
			int dwParam1, int dwParam2, int dwParam3) {
		
	}
}
