package de.janrufmonitor.repository.mapping;

import java.util.List;

import de.janrufmonitor.repository.IMacAddressBookConst;

public interface IMacAddressBookAddressMapping extends IMacAddressBookConst {
	public List getSupportedContactFields();
	
	public String mapToJamField(String macabField);
	
	public String mapToMacAddressBookField(String jamField);
	
	public String getSupportedAddressType();
	
	public String getSupportedEmailType();
	
}
