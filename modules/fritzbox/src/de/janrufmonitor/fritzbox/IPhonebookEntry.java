package de.janrufmonitor.fritzbox;

import java.util.Map;

public interface IPhonebookEntry {

	public void setName(String name);
	
	public void setAddressbook(String ab);
	
	public void setEmail(String e);
	
	public void setImageBase64(String b64);
	
	public void addNumber(String n, String type);
	
	public String getName();
	
	public String getEmail();
	
	public String getImageBase64();
	
	public String getAddressbook();
	
	public Map getPhones();
	
	
}
