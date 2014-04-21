package de.janrufmonitor.repository.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.MacAddressBookManager;
import de.janrufmonitor.runtime.PIMRuntime;

public class DefaultMacAddressBookMapping implements IMacAddressBookMapping {

	public List getSupportedNumbers() {
		List<String> l = new ArrayList <String>(8);
		l.add(DefaultMacAddressBookMapping.HOME);
		l.add(DefaultMacAddressBookMapping.HOME_FAX);
		l.add(DefaultMacAddressBookMapping.WORK);
		l.add(DefaultMacAddressBookMapping.WORK_FAX); 
		l.add(DefaultMacAddressBookMapping.MOBILE);
		l.add(DefaultMacAddressBookMapping.PAGER);
		l.add(DefaultMacAddressBookMapping.MAIN);
		l.add(DefaultMacAddressBookMapping.OTHER);
		l.addAll(getAdditionalSupportedNumbers());
		return l;
	}

	public String mapToJamNumberType(String macabNumberType) {
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.HOME)) return IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE;
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.WORK)) return IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE;
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.MAIN)) return IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE;
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.OTHER)) return IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE;
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.HOME_FAX)) return IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE;
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.WORK_FAX)) return IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE;
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.MOBILE)) return IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE;
		if (macabNumberType.equalsIgnoreCase(DefaultMacAddressBookMapping.PAGER)) return IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE;
		return "";
	}

	public List getSupportedContactFields() {
		List<String> l = new ArrayList <String>(7);
		l.add(DefaultMacAddressBookMapping.STREET);
		l.add(DefaultMacAddressBookMapping.ZIP);
		l.add(DefaultMacAddressBookMapping.CITY);
		l.add(DefaultMacAddressBookMapping.COUNTRY);
		l.add(DefaultMacAddressBookMapping.ORGANIZATION);
		l.add(DefaultMacAddressBookMapping.FIRSTNAME);
		l.add(DefaultMacAddressBookMapping.LASTNAME);
		return l;
	}

	public String mapToJamField(String macabField) {
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.FIRSTNAME)) return IJAMConst.ATTRIBUTE_NAME_FIRSTNAME;
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.LASTNAME)) return IJAMConst.ATTRIBUTE_NAME_LASTNAME;
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.ORGANIZATION)) return IJAMConst.ATTRIBUTE_NAME_ADDITIONAL;
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.STREET)) return IJAMConst.ATTRIBUTE_NAME_STREET;
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.ZIP)) return IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE;
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.CITY)) return IJAMConst.ATTRIBUTE_NAME_CITY;
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.COUNTRY)) return IJAMConst.ATTRIBUTE_NAME_COUNTRY;
		if (macabField.equalsIgnoreCase(DefaultMacAddressBookMapping.EMAIL)) return IJAMConst.ATTRIBUTE_NAME_EMAIL;
		return null;
	}

	public String mapToMacAddressBookField(String jamField) {
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_FIRSTNAME)) return DefaultMacAddressBookMapping.FIRSTNAME;
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_LASTNAME)) return DefaultMacAddressBookMapping.LASTNAME;
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_ADDITIONAL)) return DefaultMacAddressBookMapping.ORGANIZATION;
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_STREET)) return DefaultMacAddressBookMapping.STREET;
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE)) return DefaultMacAddressBookMapping.ZIP;
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_CITY)) return DefaultMacAddressBookMapping.CITY;
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_COUNTRY)) return DefaultMacAddressBookMapping.COUNTRY;
		if (jamField.equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_EMAIL)) return DefaultMacAddressBookMapping.EMAIL;
		return null;
	}

	public IAttribute createMacAddressBookNumberTypeAttribute(IPhonenumber pn,
			String macabNumberType) {
		return PIMRuntime.getInstance().getCallerFactory().createAttribute(DefaultMacAddressBookMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber(), macabNumberType);
	}
	
	public String[] getPriorityOrder(String jamType) {
		if (jamType!=null && jamType.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_LANDLINE_TYPE)) {
			return new String[] {
					DefaultMacAddressBookMapping.HOME,
					DefaultMacAddressBookMapping.WORK,
					DefaultMacAddressBookMapping.MAIN,
					DefaultMacAddressBookMapping.OTHER	
				};
		}
		if (jamType!=null && jamType.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MOBILE_TYPE)) {
			return new String[] {
					DefaultMacAddressBookMapping.MOBILE,
					DefaultMacAddressBookMapping.PAGER
				};
		}	
		if (jamType!=null && jamType.equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_FAX_TYPE)) {
			return new String[] {
					DefaultMacAddressBookMapping.HOME_FAX,
					DefaultMacAddressBookMapping.WORK_FAX
				};
		}				
		return new String[] {
				DefaultMacAddressBookMapping.HOME,
				DefaultMacAddressBookMapping.WORK,
				DefaultMacAddressBookMapping.MAIN,
				DefaultMacAddressBookMapping.OTHER			
			};
	}

	public String getSupportedAddressType() {
		return DefaultMacAddressBookMapping.HOME;
	}

	public String getSupportedEmailType() {
		return DefaultMacAddressBookMapping.HOME;
	}
	
	protected List getAdditionalSupportedNumbers() {
		List<String> l = new ArrayList <String>(5);
		String additionalNumbers = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(MacAddressBookManager.NAMESPACE, "addnumbers");
		if (additionalNumbers!=null && additionalNumbers.trim().length()>0) {
			StringTokenizer st = new StringTokenizer(additionalNumbers, ",");
			while (st.hasMoreTokens()) {
				l.add(st.nextToken().trim());
			}
		}
		return l;
	}

}
