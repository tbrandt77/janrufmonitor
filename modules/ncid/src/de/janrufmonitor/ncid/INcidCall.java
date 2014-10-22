package de.janrufmonitor.ncid;

import de.janrufmonitor.framework.ICall;

public interface INcidCall {
	
	public ICall toCall();
	
	public boolean isValid();
	
}
