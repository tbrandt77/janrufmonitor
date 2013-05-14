package de.janrufmonitor.fritzbox.firmware.exception;

public class FritzBoxNotFoundException extends Exception {

	private static final long serialVersionUID = 7697762996486809128L;

	private String m_server;
	private String m_port;
	
	public FritzBoxNotFoundException(String msg) {
		super(msg);
		this.m_port = "";
		this.m_server = "";
	}
	
	public FritzBoxNotFoundException(String server, String port) {
		this.m_server = server;
		this.m_port = port;
	}
	
	public String getServer() {
		return this.m_server;
	}
	
	public String getPort() {
		return this.m_port;
	}
}
