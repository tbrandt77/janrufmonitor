package de.janrufmonitor.fritzbox.mapping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.janrufmonitor.fritzbox.IPhonebookEntry;
import de.janrufmonitor.util.string.StringEscapeUtils;

public class FritzBoxPhonebookEntry implements IPhonebookEntry {
	
	String m_uid;
	String m_eid;
	String m_name;
	String m_ab;
	Map m_phones;
	Map m_phones_type;
	Map m_phones_id;
	Map m_phones_prio;
	Map m_phones_quickdial;
	Map m_phones_vanity;
	Map m_jam_info;
	String m_email;
	String m_image;
	String m_cat;
	String m_image_url;
	String m_modtime;
	
	public FritzBoxPhonebookEntry() {
		m_phones = new HashMap(3);
		m_phones_type = new HashMap(3);
		m_phones_id = new HashMap(3);
		m_phones_prio = new HashMap(3);
		m_phones_quickdial = new HashMap(3);
		m_phones_vanity = new HashMap(3);
		m_jam_info = new HashMap(5);
	}
	
	public void setName(String name) {
		this.m_name = name;
	}
	
	public void setAddressbook(String ab) {
		this.m_ab = ab;
	}
	
	public void setEmail(String e) {
		this.m_email = e;
	}
	
	public void addNumber(String n, String type) {
		m_phones.put(n, type);
	}
	
	public String getName() {
		return this.m_name;
	}
	
	public String getEmail() {
		return this.m_email;
	}
	
	public String getAddressbook() {
		return this.m_ab;
	}
	
	public Map getPhones() {
		return m_phones;
	}

	public void setImageBase64(String b64) {
		this.m_image = b64;
	}

	public String getImageBase64() {
		return this.m_image;
	}

	public void addNumberType(String n, String type) {
		m_phones_type.put(n, type);
	}

	public void addNumberPrio(String n, String prio) {
		m_phones_prio.put(n, prio);
	}

	public void addNumberID(String n, String id) {
		m_phones_id.put(n, id);
	}

	public void addNumberQuickDial(String n, String qd) {
		m_phones_quickdial.put(n, qd);
	}

	public void addNumberVanity(String n, String van) {
		m_phones_vanity.put(n, van);
	}

	public String getNumberType(String n) {
		return (String) m_phones_type.get(n);
	}

	public String getNumberPrio(String n) {
		return (String) m_phones_prio.get(n);
	}

	public String getNumberID(String n) {
		return (String) m_phones_id.get(n);
	}

	public String getNumberQuickDial(String n) {
		return (String) m_phones_quickdial.get(n);
	}

	public String getNumberVanity(String n) {
		return (String) m_phones_vanity.get(n);
	}

	public void setCategory(String cat) {
		this.m_cat = cat;
	}

	public String getCatergory() {
		return this.m_cat;
	}

	public void setImageURL(String url) {
		this.m_image_url = url;
	}

	public String getImageURL() {
		return this.m_image_url;
	}

	public void setModTime(String t) {
		this.m_modtime = t;
	}

	public String getModTime() {
		return this.m_modtime;
	}

	public void setUniqueID(String uid) {
		this.m_uid = uid;
	}

	public String getUniqueID() {
		return this.m_uid;
	}

	public void setEntryID(String eid) {
		this.m_eid = eid;
	}

	public String getEntryID() {
		return this.m_eid;
	}
	
	public int hasCode() {
		return this.m_name.hashCode() + (this.m_uid != null ? this.m_uid.hashCode() : 0);
	}
	
	public void addJamInfo(String key, String value) {
		if (key==null) return;
		this.m_jam_info.put(key, value);
	}

	public String getJamInfo(String key) {
		if (this.m_jam_info.containsKey(key)) return (String) this.m_jam_info.get(key);
		return null;
	}
	
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		s.append("<contact>");
		s.append("<category>");
		if (this.m_cat!=null)
			s.append(this.m_cat);
		s.append("</category>");
		s.append("<person><realName>");
		try {
			s.append(StringEscapeUtils.escapeHtml(this.m_name));
		} catch (Exception e) {
			s.append(this.m_name);
		}
		s.append("</realName>");
		if (this.m_image_url!=null) {
			s.append("<imageURL>");
			s.append(this.m_image_url);
			s.append("</imageURL>");
		}
		if (this.m_jam_info.size()>0) {
			s.append("<jam_info ");
			Iterator<String> i = this.m_jam_info.keySet().iterator();
			String key = null;
			String value = null;
			
			while (i.hasNext()) {
				key = i.next();
				value = (String) this.m_jam_info.get(key);
				if (value!=null && value.length()>0) {
					s.append(key);
					s.append("=\"");
					try {
						s.append(StringEscapeUtils.escapeHtml(value));
					} catch (Exception e) {
						s.append(value);
					}
					s.append("\" ");	
				}
			}
			s.append("/>");
		}
		
		s.append("</person>");
		s.append("<telephony nid=\"");
		s.append(this.m_phones.size());
		s.append("\">");
		Iterator<String> i = this.m_phones.keySet().iterator();
		String n = null;
		while (i.hasNext()) {
			n = i.next();
			s.append("<number");
			if (this.getNumberType(n)!=null) {
				s.append(" type=\"");
				s.append(this.getNumberType(n));
				s.append("\"");
			}
			if (this.getNumberPrio(n)!=null) {
				s.append(" prio=\"");
				s.append(this.getNumberPrio(n));
				s.append("\"");
			}
			if (this.getNumberID(n)!=null) {
				s.append(" id=\"");
				s.append(this.getNumberID(n));
				s.append("\"");
			}
			if (this.getNumberQuickDial(n)!=null) {
				s.append(" quickdial=\"");
				s.append(this.getNumberQuickDial(n));
				s.append("\"");
			}
			if (this.getNumberVanity(n)!=null) {
				s.append(" vanity=\"");
				s.append(this.getNumberVanity(n));
				s.append("\"");
			}
			s.append(">");
			s.append(n);
			s.append("</number>");
		}
		s.append("</telephony>");
		if (this.m_email!=null) {
			s.append("<services nid=\"1\"><email classifier=\"private\" id=\"0\">");
			s.append(this.m_email);
			s.append("</email></services>");
		} else {
			s.append("<services />");
		}
		s.append("<setup />");
		if (this.m_modtime!=null) {
			s.append("<mod_time>");
			s.append(this.m_modtime);
			s.append("</mod_time>");
		}
		
		s.append("<uniqueid>");
		if (this.m_uid!=null)
			s.append(this.m_uid);
		s.append("</uniqueid>");
		s.append("</contact>");
		return s.toString();
	}

}
