package de.janrufmonitor.fritzbox;

import java.io.IOException;
import java.util.Properties;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.fritzbox.firmware.FirmwareManager;
import de.janrufmonitor.runtime.PIMRuntime;

public class QuickDialer implements FritzBoxConst {
	public static void dial(IPhonenumber n, String msn) throws Exception {
		if (n.isClired()) throw new Exception ("no number provided");
		
		String dial = PhonenumberAnalyzer.getInstance().toCallable(n.getTelephoneNumber());
		// added 2010/03/06: check for dial prefix for outgoing calls
		if (PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).length()>0) {
			dial = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_DIAL_PREFIX).trim() + dial;
		}
		
		Properties config = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperties(FritzBoxMonitor.NAMESPACE);
	//	FritzBox m_fb = new FritzBox(config.getProperty(CFG_IP, "fritz.box"), config.getProperty(CFG_PASSWORD, ""), config.getProperty(CFG_PORT, "80"));
		FirmwareManager fwm = FirmwareManager.getInstance();
		try {
			fwm.login();
			
			fwm.doCall(dial + "#", config.getProperty(CFG_CLICKDIAL, "50"));
		} catch (IOException e) {
			throw new Exception (e.getMessage());
		}
	}
	
	public static String[] getAllExtensions() throws Exception {
		String[] r = new String[1];
		r[0] = "AVM Fritz!Box Fon";
		return r;		
	}
}
