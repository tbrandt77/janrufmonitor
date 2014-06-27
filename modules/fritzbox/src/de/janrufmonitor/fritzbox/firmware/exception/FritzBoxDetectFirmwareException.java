package de.janrufmonitor.fritzbox.firmware.exception;

public class FritzBoxDetectFirmwareException extends Exception {

	private static final long serialVersionUID = -2542570892050623799L;
	
	private boolean m_labor = false;
	
	public FritzBoxDetectFirmwareException(String msg) {
		super(msg);
	}
	
	public FritzBoxDetectFirmwareException(String msg, boolean isLaborFirmware) {
		super(msg);
		this.m_labor = isLaborFirmware;
	}
	
	public boolean isLaborFirmware() {
		return this.m_labor;
	}

}
