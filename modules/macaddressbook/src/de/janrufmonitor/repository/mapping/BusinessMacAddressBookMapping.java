package de.janrufmonitor.repository.mapping;

import java.util.ArrayList;
import java.util.List;

public class BusinessMacAddressBookMapping extends DefaultMacAddressBookMapping {

	public String getSupportedAddressType() {
		return DefaultMacAddressBookMapping.WORK;
	}
	
	public String getSupportedEmailType() {
		return DefaultMacAddressBookMapping.WORK;
	}
	
	public List getSupportedNumbers() {
		List<String> l = new ArrayList <String>(6);
		l.add(DefaultMacAddressBookMapping.WORK);
		l.add(DefaultMacAddressBookMapping.WORK_FAX);
		l.add(DefaultMacAddressBookMapping.MOBILE);
		l.add(DefaultMacAddressBookMapping.PAGER);
		l.add(DefaultMacAddressBookMapping.MAIN);
		l.add(DefaultMacAddressBookMapping.OTHER);
		l.addAll(getAdditionalSupportedNumbers());
		return l;
	}
}
