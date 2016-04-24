package de.janrufmonitor.fritzbox;

import java.util.Map;

public interface IPhonebookEntry {

	public void setName(String name);
	
	public void setAddressbook(String ab);
	
	public void setEmail(String e);
	
	public void setImageBase64(String b64);
	
	public void addJamInfo(String key, String value);
	
	public String getJamInfo(String key);	
	
	public void addNumber(String n, String type);
	
	public void addNumberType(String n, String type);
	
	public void addNumberPrio(String n, String prio);
	
	public void addNumberID(String n, String id);
	
	public void addNumberQuickDial(String n, String qd);
	
	public void addNumberVanity(String n, String van);
	
	public String getNumberType(String n);
	
	public String getNumberPrio(String n);
	
	public String getNumberID(String n);
	
	public String getNumberQuickDial(String n);
	
	public String getNumberVanity(String n);
	
	public String getName();
	
	public String getEmail();
	
	public String getImageBase64();
	
	public String getAddressbook();
	
	public Map getPhones();
	
	public void setCategory(String cat);
	
	public String getCatergory();
	
	public void setImageURL(String url);
	
	public String getImageURL();
	
	public void setModTime(String t);
	
	public String getModTime();
	
	public void setUniqueID(String uid);
	
	public String getUniqueID();	
	
	public void setEntryID(String eid);
	
	public String getEntryID();	
	
}
