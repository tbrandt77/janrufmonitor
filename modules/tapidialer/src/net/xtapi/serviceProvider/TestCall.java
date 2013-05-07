package net.xtapi.serviceProvider;

public class TestCall implements IXTapiCallBack {

	public void callback(int dwDevice, int dwMessage, int dwInstance,
			int dwParam1, int dwParam2, int dwParam3) {
		System.out.println(dwMessage);
	}

	public void call(String num) {
		MSTAPI m_tapi = new MSTAPI();
		int n = m_tapi.init(this);
		int callhandle = 0;
		int line = 0;
		StringBuffer nameOfLine = null;
		for (int i = 0; i < n; i++) {
			nameOfLine = new StringBuffer();
			int m_lineHandle = m_tapi.openLineTapi(i, nameOfLine);
			if (m_lineHandle > 0) {
				callhandle = m_lineHandle;
				line = i;
				break;
			}
		}
		
		if (callhandle>0) {
			m_tapi.connectCallTapi(line, num, callhandle);
		}
		m_tapi.shutdownTapi();
	}
	
	public static void main(String[] args) {
		TestCall tc = new TestCall();
		tc.call("06227763614");
	}
	
}
