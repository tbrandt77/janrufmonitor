package de.janrufmonitor.fritzbox.firmware.exception;

public class FritzBoxInitializationException extends Exception {

	private static final long serialVersionUID = 7163574592226080798L;
	
	private Exception nestedEx = null;
	
	public FritzBoxInitializationException(String msg) {
		super(msg);
	}
	
	public FritzBoxInitializationException(String msg, Exception e) {
		super(msg);
		this.nestedEx = e;
	}
	
	public Exception getNestedException() {
		return this.nestedEx;
	}
	
	public boolean isUnsupportedFirmware() {
		return this.nestedEx != null && this.nestedEx instanceof FritzBoxDetectFirmwareException;
	}

}
