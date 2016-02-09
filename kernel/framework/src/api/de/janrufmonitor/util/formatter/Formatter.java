package de.janrufmonitor.util.formatter;

import java.text.MessageFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.configuration.IConfigManager;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.string.StringUtils;

/**
 *  This class provides formatting functions for phonenumbers and names.
 * 
 *@author     Thilo Brandt
 *@created    2003/10/22
 */
public class Formatter {
	
	private static Formatter m_instance = null;
	
	private String[] removeableStartTags = new String[] {
		", -",
		",",
		"-",
		"+  "
	};
	
	private String[] removeableEndTags = new String[] {
		", -",
		",",
		"-"
	};
	
	private String[] removeableTags = new String[] {
		"+ ()",
		", -",
		" -  ",
		"()",
		"[]",
		"{}"
	};
	
	private String[][] replaceTags = new String[][] {
			{IJAMConst.CRLF+", ", IJAMConst.CRLF}, 
			{IJAMConst.CRLF+IJAMConst.CRLF, IJAMConst.CRLF}
	};
	
	private IRuntime m_runtime;

	private Formatter(IRuntime runtime) { 
		this.m_runtime = runtime;
	}
	
    
    /**
     * Gets the singleton instance of the Formatter for
     * the specified IRuntime instance.
     * 
     * @param runtime
     * @return
     */
	public static synchronized Formatter getInstance(IRuntime runtime) {
		if (Formatter.m_instance == null) {
			Formatter.m_instance = new Formatter(runtime);
		}
		return Formatter.m_instance;
	}
	
	/**
	 * Parses the String object and replaces the standard placeholders with the value of the 
	 * passed Object. The Object is checked for instances of ICall, ICaller, IPhonenumber,
	 * IName and Date. An individual parsing method is called for each instance type. If an unknown Object
	 * instance is passed, the original String is returned back. If null is passed the original 
	 * String is returned back, too.
	 * 
	 * @param text String containing the standard placeholders.
	 * @param o object to get the data for replacement from
	 * @return the parsed String object
	 */
	public String parse(String text, Object o) {
		if (o==null) return text;

		if (o instanceof ICall) return this.getParsedCall(text, (ICall)o);
		if (o instanceof IMsn) return this.getParsedMSNFormat(text, (IMsn)o);
		if (o instanceof ICaller) return this.getParsedCaller(text, (ICaller)o);
		if (o instanceof IPhonenumber) return this.getParsedPhonenumber(text, (IPhonenumber)o);
		if (o instanceof IName) return this.getParsedName(text, (IName)o);
		if (o instanceof Date) return this.getParsedCalltime(text, (Date)o);
		if (o instanceof IAttributeMap) return this.getParsedAttributes(text, (IAttributeMap)o, true);
		return text;		
	}
	
	
	/**
	 * Parses a phone string and tries to build a valid 
	 * IPhonenumber object.
	 * 
	 * @param phone
	 * @return
	 */
	public IPhonenumber getPhonenumber(String phone, String format) {
			
		MessageFormat mf = new MessageFormat(format);
		Object[] objs = mf.parse(phone, new ParsePosition(0));

		IPhonenumber pn = this.m_runtime.getCallerFactory().createPhonenumber(this.normalizePhonenumber(phone, true));
		
		String intarea = "";
		
		if (objs[0]==null) {
			intarea = this.m_runtime.getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
		} else {
			intarea = (String)objs[0];
		}

		intarea = this.normalizePhonenumber(intarea, true);

		String area = (String)objs[1];
		area = this.normalizePhonenumber(area, true);
		
		String number = (String)objs[2];
		number = this.normalizePhonenumber(number, false);
		
		pn.setAreaCode(area);
		pn.setCallNumber(number);
		pn.setIntAreaCode(intarea);
		
		return pn;		
	}
	
	/**
	 * Parses call object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param call object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	public String getParsedCall(String text, ICall call) {
		if (call==null)
			return text;
			
		text = this.getParsedAttributes(text, call.getAttributes(), false);
		
		text = this.getParsedCIP(text, call.getCIP());
		text = this.getParsedMSNFormat(text, call.getMSN());
		
		text = this.getParsedCalltime(text, call.getDate());
		
		text = this.getParsedCaller(text, call.getCaller());
		
		text = this.cleanString(text.trim());
			
		return text.trim();
	}

	/**
	 * Parses date object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param date object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	public String getParsedCalltime(String text, Date date) {
		if (date==null)
			return text;
			
		IConfigManager cfg = this.m_runtime.getConfigManagerFactory().getConfigManager();
		String calltimeExpression = cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLTIME);

		text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_CALLTIME, calltimeExpression);
		text = this.getParsedDate(text, date);
		text = this.cleanString(text);
		
		return text;
	}
	
	/**
	 * Parses date object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param date object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	private String getParsedDate(String text, Date date) {
		if (date==null)
			return text;
		
		IConfigManager cfg = this.m_runtime.getConfigManagerFactory().getConfigManager();

		if (this.containString(text, IJAMConst.GLOBAL_VARIABLE_DATE)) {
			SimpleDateFormat formatter
				 = new SimpleDateFormat(cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_DATE));
			String datetext = formatter.format(date);
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_DATE, datetext);
			text = text.trim();			
		}
		
		if (this.containString(text, IJAMConst.GLOBAL_VARIABLE_TIME)) {
			SimpleDateFormat formatter
				 = new SimpleDateFormat(cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_TIME));
			String timetext = formatter.format(date);
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_TIME, timetext);
			text = text.trim();
		}
		
		return text;
	}
	/**
	 * Parses CIP object in the specified text representation.
 	 * The wildcard definitions are taken for parsing.
 	 *  
 	 * @param text text to parse
 	 * @param CIP object to get the information from
 	 * @return parsed text
 	 * @deprecated
     */
	private String getParsedCIP(String text, ICip cip) {
		if(cip==null)
			return text;
		
		if (this.containString(text, IJAMConst.GLOBAL_VARIABLE_CIP)) {
			String ciptext = cip.getAdditional();
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_CIP, ciptext);
			text = text.trim();
		}
		return text;
	}
	
	/**
	 * Parses MSN object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param MSN object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	private String getParsedMSN(String text, IMsn msn) {
		if(msn==null)
			return text;
		
		if (this.containString(text, IJAMConst.GLOBAL_VARIABLE_MSN)) {
			String msnnumber = msn.getMSN();
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_MSN, msnnumber);
			text = this.cleanString(text.trim());
			text = text.trim();
		}
		if (this.containString(text, IJAMConst.GLOBAL_VARIABLE_MSNALIAS)) {
			String msnalias = msn.getAdditional();
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_MSNALIAS, msnalias);
			text = this.cleanString(text.trim());
			text = text.trim();
		}
		return text;
	}
	
	private String getParsedMSNFormat(String text, IMsn msn) {
		if(msn==null)
			return text;
		
		IConfigManager cfg = this.m_runtime.getConfigManagerFactory().getConfigManager();
		String msnformatExpression = cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_MSNFORMAT);

		text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_MSNFORMAT, msnformatExpression);
		text = this.getParsedMSN(text, msn);
		text = this.cleanString(text);
		
		return text;
	}
	
	/**
	 * Parses caller object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param caller object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	public String getParsedCaller(String text, ICaller caller) {
		if (caller==null)
			return text;
		
		IConfigManager cfg = this.m_runtime.getConfigManagerFactory().getConfigManager();
		
		// komplex types
		String callernameExpression = cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLERNAME);
		text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_CALLERNAME, callernameExpression);	
							
		text = this.getParsedPhonenumber(text, caller.getPhoneNumber());
		text = this.getParsedAttributes(text, caller.getAttributes(), true);
		
		// changed: 2005/11/11: deprecated method is processed last
		text = this.getParsedName(text, caller.getName());	
		text = this.cleanString(text.trim());

		return text.trim();
	}
	
	/**
	 * Parses phonenumber object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param phonenumber object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	public String getParsedPhonenumber(String text, IPhonenumber pn) {
		if (pn==null)
			return text;
		
		IConfigManager cfg = this.m_runtime.getConfigManagerFactory().getConfigManager();

		String callernumberExpression = cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER);

		if (pn.isClired()) {
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, 
					this.m_runtime.getConfigManagerFactory().getConfigManager().getProperty(
						IJAMConst.GLOBAL_NAMESPACE,
						IJAMConst.GLOBAL_CLIR
					)
				);
		} else if (pn.getIntAreaCode().equalsIgnoreCase(IJAMConst.INTERNAL_CALL)) {
			// added: 2009/01/21: handling for internal calls
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, pn.getCallNumber());
		} else {
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, callernumberExpression);
			text = this.getParsedIntAreaCode(text, pn);
			text = this.getParsedAreaCode(text, pn);
			text = this.getParsedCallnumber(text, pn);
		
			// added: 2003/10/29: remove internal string on internal calls
			text = StringUtils.replaceString(text,IJAMConst.INTERNAL_CALL, "");
		}
		
		text = this.cleanString(text.trim());
		
		return text.trim();
	}
	
	/**
	 * Parses Name object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param Name object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	public String getParsedName(String text, IName name) {
		if (name==null)
			return text;
			
		IConfigManager cfg = this.m_runtime.getConfigManagerFactory().getConfigManager();
		
		// komplex types
		String callernameExpression = cfg.getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_VARIABLE_CALLERNAME);
		text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_CALLERNAME, callernameExpression);	
		
		// parse caller attributes
		IAttributeMap m = this.m_runtime.getCallerFactory().createAttributeMap();
		m.add(this.m_runtime.getCallerFactory().createAttribute(
			IJAMConst.ATTRIBUTE_NAME_FIRSTNAME,
			name.getFirstname()
		));
		m.add(this.m_runtime.getCallerFactory().createAttribute(
			IJAMConst.ATTRIBUTE_NAME_LASTNAME,
			name.getLastname()
		));
		m.add(this.m_runtime.getCallerFactory().createAttribute(
			IJAMConst.ATTRIBUTE_NAME_ADDITIONAL,
			name.getAdditional()
		));				
		text = this.getParsedAttributes(text, m, true);
		
		text = StringUtils.replaceString(text, ",  ", " ");	

		text = this.cleanString(text.trim());
		
		return text.trim();
	}
	
	/**
	 * Parses Phonenumber object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param Phonenumber object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	private String getParsedCallnumber(String text, IPhonenumber pn) {
		if (pn==null)
			return text;
		
		if (this.containString(text, IJAMConst.CALLNUMBER)) {
			String number = pn.getCallNumber();
			text = StringUtils.replaceString(text, IJAMConst.CALLNUMBER, number);
			text = this.cleanString(text.trim());
			text = text.trim();
		}
		return text;
	}
	
	/**
	 * Parses Phonenumber object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param Phonenumber object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	private String getParsedAreaCode(String text, IPhonenumber pn) {
		if (pn==null)
			return text;
		
		if (this.containString(text, IJAMConst.AREACODE)) {
			String area = pn.getAreaCode();
			text = StringUtils.replaceString(text, IJAMConst.AREACODE, area);
			text = this.cleanString(text.trim());
			text = text.trim();
		}
		return text;
	}	
	
	/**
	 * Parses Phonenumber object in the specified text representation.
	 * The wildcard definitions are taken for parsing.
	 * 
	 * @param text text to parse
	 * @param Phonenumber object to get the information from
	 * @return parsed text
	 * @deprecated
	 */
	private String getParsedIntAreaCode(String text, IPhonenumber pn) {
		if (pn==null)
			return text;
		
		if (this.containString(text, IJAMConst.INTAREACODE)) {
			String intarea = pn.getIntAreaCode();
			text = StringUtils.replaceString(text, IJAMConst.INTAREACODE, intarea);
			text = this.cleanString(text.trim());
			text = text.trim();
		}
		return text;
	}	

	
	/**
	 * Resolve rendering conditions for dynamic formats.
	 * 
	 * @param text
	 * @param o
	 * @return
	 */
	private String resolveConditions(String text, Object o) {
		// ${%a:add%==??%a::str%:%a:add%}$
		if (text.indexOf("${")>-1 && text.indexOf("}$")>-1) {
			String condition = text.substring(text.indexOf("${")+2, text.indexOf("}$"));
			// check if condition is valid
			if (condition.indexOf("??")>0 && condition.indexOf("::")>0) {
				String cond = condition.substring(0, condition.indexOf("??"));
				String rendertrue = condition.substring(condition.indexOf("??")+2, condition.indexOf("::"));
				String renderfalse = condition.substring(condition.indexOf("::")+2);
				if (isCondition(cond, o)) {
					text = StringUtils.replaceString(text, "${"+condition+"}$", rendertrue);
				} else {
					text = StringUtils.replaceString(text, "${"+condition+"}$", renderfalse);
				}
			}
		}
		return text;
	}
	
	private boolean isCondition(String cond, Object o) {
		boolean valid = false;
		if (cond.indexOf("&&")>0) {
			StringTokenizer st = new StringTokenizer(cond, "&&");
			while (st.hasMoreTokens()) {
				valid &= this.isCondition(st.nextToken(), o);
			}
		}
		String left = null;
		String right = null;
		if (cond.indexOf("==")>0) {
			left = cond.substring(0, cond.indexOf("=="));
			right = cond.substring(cond.indexOf("==")+2);
			if (o instanceof IAttributeMap) valid = this.getParsedAttributes(left, (IAttributeMap)o, true, false).equalsIgnoreCase(right);
		}
		if (cond.indexOf("!=")>0) {
			left = cond.substring(0, cond.indexOf("!="));
			right = cond.substring(cond.indexOf("!=")+2);
			if (o instanceof IAttributeMap) valid = !this.getParsedAttributes(left, (IAttributeMap)o, true, false).equalsIgnoreCase(right);
		}
		return valid;
	}
	
	private String getParsedAttributes(String text, IAttributeMap attMap, boolean cleanup) {
		return this.getParsedAttributes(text, attMap, cleanup, true);
	}
	
	private String getParsedAttributes(String text, IAttributeMap attMap, boolean cleanup, boolean resolveCond) {
		if(attMap==null || attMap.size()==0)
			return text;
		
		
		// added 2010/09/26: render conditions
		if (resolveCond)
			text = this.resolveConditions(text, attMap);
	
		
		IAttribute a = null;
		String value = null;
		Iterator i = attMap.iterator();
		while(i.hasNext()) {
			a = (IAttribute) i.next();
			if (this.containString(text, IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + a.getName() + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX)) {
				value = a.getValue();
				text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX + a.getName() + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX, value);
				text = this.cleanString(text.trim());
				text = text.trim();
			}
		}

		if (cleanup) {
			// find unparsed tokens and remove them
			String unparsedAttribute = null;
			while(text.indexOf(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX)>-1) {
				unparsedAttribute = text.substring(
					text.indexOf(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX),
					text.indexOf(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX, text.indexOf(IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_PREFIX)+1) + IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX.length()	
				);
				text = StringUtils.replaceString(text, unparsedAttribute, "");
				text = this.cleanString(text.trim());
				text = text.trim();
			}
			
			// CRLF replacement
			text = StringUtils.replaceString(text, IJAMConst.GLOBAL_VARIABLE_ATTRIBUTE_CRLF, IJAMConst.CRLF);
			text = cleanString(text);
		}

		return text;
	}
	

	
	private boolean containString(String source, String match) {
		return (source.toLowerCase().indexOf(match.toLowerCase())>-1);
	}
	
	private String cleanString(String text) {
		String tag = null;
		for (int i=0;i<this.removeableStartTags.length;i++) {
			tag = this.removeableStartTags[i];
			if (text.startsWith(tag)) {
				text = text.substring(tag.length());
			}
		}
		
		for (int i=0;i<this.removeableTags.length;i++) {
			tag = this.removeableTags[i];
			text = StringUtils.replaceString(text, tag, "");
		}
		
		for (int i=0;i<this.replaceTags.length;i++) {
			text = StringUtils.replaceString(text, this.replaceTags[i][0], this.replaceTags[i][1]);
		}
		
		for (int i=0;i<this.removeableEndTags.length;i++) {
			tag = this.removeableEndTags[i];
			if (text.endsWith(tag)) {
				text = text.substring(0, text.length() - tag.length());
			}
		}
		
		while (text.indexOf(IJAMConst.CRLF + " ")>-1) {
			text = StringUtils.replaceString(text, IJAMConst.CRLF + " ", IJAMConst.CRLF);
		}
		
		return text;
	}
	
	/**
	 * Normalizes a formatted number to a PIM compliant 
	 * number string.
	 * <br><br>
	 * Examples:<br>
	 * Source format: +49 (1234) 567890<br>
	 * Target format: 0491234567890 (international format)<br><br>
	 * or<br><br>
	 * Source format: (01234) 567890<br>
	 * Target format: 1234567890 (national format)<br>
	 * 
	 * @param phone
	 * @return
	 * @deprecated
	 */
	private String normalizePhonenumber(String phone, boolean trimZeros) {
		phone = phone.trim();
		
		if (trimZeros && phone.startsWith("0")) {
			phone = phone.substring(1);
		}

		// added 2009/07/02
		phone = StringUtils.replaceString(phone, "*31#", ""); // remove CLIR symbol an callernumber
		phone = StringUtils.replaceString(phone, "#31#", ""); // remove CLIR symbol an callernumber
		phone = StringUtils.replaceString(phone, " ", "");
		phone = StringUtils.replaceString(phone, "/", "");
		phone = StringUtils.replaceString(phone, "(0", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, ")", "");
		phone = StringUtils.replaceString(phone, "-", "");
		phone = StringUtils.replaceString(phone, "#", "");
		phone = StringUtils.replaceString(phone, ".", "");
		phone = StringUtils.replaceString(phone, "+", "0");

		return phone;
	}
	
	/**
	 * 
	 * @deprecated
	 * @param phone
	 * @return
	 */
	public String normalizePhonenumber(String phone) {
		return this.normalizePhonenumber(phone, true);
	}
	
	/**
	 * Formats a string with number information in a callable
	 * format.
	 * Example: +4972657110 --> 004972657110
	 *          +49 (7165) 7110 --> 004972657110
	 *  
	 * @param phone
	 * @return
	 * @deprecated
	 */
	public String toCallablePhonenumber(String phone) {
		phone = phone.trim();

		phone = StringUtils.replaceString(phone, " ", "");
		phone = StringUtils.replaceString(phone, "/", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, "(", "");
		phone = StringUtils.replaceString(phone, ")", "");
		phone = StringUtils.replaceString(phone, "-", "");
		// removed 2009/07/02
		//phone = StringUtils.replaceString(phone, "#", "");
		phone = StringUtils.replaceString(phone, ".", "");
		phone = StringUtils.replaceString(phone, "+", "00");

		return phone;
	}
}
