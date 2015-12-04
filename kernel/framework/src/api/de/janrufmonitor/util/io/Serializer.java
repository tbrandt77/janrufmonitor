package de.janrufmonitor.util.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.string.StringUtils;

/**
 * This class provides support for serializing framework objects
 * into String and Stream and vice versa.
 * 
 *@author     Thilo Brandt
 *@created    2004/01/31
 */
public class Serializer {
	
	private static String m_token = ";";
	private static String m_atoken = "%";
	private static String m_ctoken = "&";
	private static String m_imglftoken = "\n";

	
	private static final String TOKEN_ESCAPE_SYMBOL = "$token$";
	private static final String ATOKEN_ESCAPE_SYMBOL = "$atoken$";
	private static final String AVTOKEN_ESCAPE_SYMBOL = "$avtoken$";
	private static final String CTOKEN_ESCAPE_SYMBOL = "$ctoken$";
	private static final String IMGLFTOKEN_ESCAPE_SYMBOL = "$imgtoken$";
	
	private static final String EQUAL = "=";
	private static final String BLANK = " ";

	private static String decode(String field) {		
		field = StringUtils.replaceString(field, TOKEN_ESCAPE_SYMBOL, m_token);
		field = StringUtils.replaceString(field, ATOKEN_ESCAPE_SYMBOL, m_atoken);
		field = StringUtils.replaceString(field, CTOKEN_ESCAPE_SYMBOL, m_ctoken);
		return field;
	}
	
	private static String encode(String field) {
		field = StringUtils.replaceString(field, m_token, TOKEN_ESCAPE_SYMBOL);
		field = StringUtils.replaceString(field, m_atoken, ATOKEN_ESCAPE_SYMBOL);
		field = StringUtils.replaceString(field, m_ctoken, CTOKEN_ESCAPE_SYMBOL);
		return field;
	}
	
	private static String encodeAttributeValue(String field) {
		field = StringUtils.replaceString(field, m_token, TOKEN_ESCAPE_SYMBOL);
		field = StringUtils.replaceString(field, m_atoken, AVTOKEN_ESCAPE_SYMBOL);
		return encode(field);
	}
	
	private static String decodeAttributeValue(String field) {		
		field = StringUtils.replaceString(field, TOKEN_ESCAPE_SYMBOL, m_token);
		field = StringUtils.replaceString(field, AVTOKEN_ESCAPE_SYMBOL, m_atoken);
		return decode(field);
	}
	
	private static String toString(ICall call, boolean includeImage) throws SerializerException {
		StringBuffer serialized = new StringBuffer(256);
		
		try {
			// add caller
			serialized.append(toString(call.getCaller(), includeImage));
			serialized.append(m_ctoken);
			
			// add call data
			serialized.append(encode((call.getMSN().getMSN().length()==0 ? BLANK : call.getMSN().getMSN())));
			serialized.append(m_token);
			serialized.append(encode((call.getMSN().getAdditional().length()==0 ? BLANK : call.getMSN().getAdditional())));
			serialized.append(m_token);
			serialized.append(encode((call.getCIP().getCIP().length()==0 ? BLANK : call.getCIP().getCIP())));
			serialized.append(m_token);
			serialized.append(encode((call.getCIP().getAdditional().length()==0 ? BLANK : call.getCIP().getAdditional())));
			serialized.append(m_token);
			serialized.append(encode((call.getUUID().length()==0 ? BLANK : call.getUUID())));
			serialized.append(m_token);
			serialized.append(encode(Long.toString(call.getDate().getTime())));
			serialized.append(m_token);

			IAttributeMap al = call.getAttributes();
			if (al.size()==0)
				serialized.append(BLANK);
			
			Iterator i = al.iterator();
			IAttribute att = null;
			while(i.hasNext()) {
				att = (IAttribute) i.next();
				serialized.append(encode(att.getName()));
				serialized.append(EQUAL);
				serialized.append(encodeAttributeValue(att.getValue()));
				serialized.append(m_atoken);
			}
			
		} catch (Throwable t) {
			throw new SerializerException(t.getMessage());
		}
		
		return serialized.toString();
	}
	
	private static String toString(IMultiPhoneCaller caller, boolean includeImage) throws SerializerException {			
		StringBuffer serialized = new StringBuffer(128);
		try {
			List pns = caller.getPhonenumbers();
		
			if (pns.size()>0) {
				// add type 
				serialized.append("mpc");
				serialized.append(m_token);

				// add phonelist size 
				serialized.append(pns.size());
				serialized.append(m_token);
				
				for (int i=0, j=pns.size(); i<j; i++) {
					// add int area code
					serialized.append(encode((((IPhonenumber) pns.get(i)).getIntAreaCode().length()==0 ? BLANK : ((IPhonenumber) pns.get(i)).getIntAreaCode())));
					serialized.append(m_token);
					
					// add area code
					serialized.append(encode((((IPhonenumber) pns.get(i)).getAreaCode().length()==0 ? BLANK : ((IPhonenumber) pns.get(i)).getAreaCode())));
					serialized.append(m_token);
					
					// add call number
					serialized.append(encode((((IPhonenumber) pns.get(i)).getCallNumber().length()==0 ? BLANK : ((IPhonenumber) pns.get(i)).getCallNumber())));
					serialized.append(m_token);
				}
			} else {
				// add int area code
				serialized.append(encode((caller.getPhoneNumber().getIntAreaCode().length()==0 ? BLANK : caller.getPhoneNumber().getIntAreaCode())));
				serialized.append(m_token);
				
				// add area code
				serialized.append(encode((caller.getPhoneNumber().getAreaCode().length()==0 ? BLANK : caller.getPhoneNumber().getAreaCode())));
				serialized.append(m_token);
				
				// add call number
				serialized.append(encode((caller.getPhoneNumber().getCallNumber().length()==0 ? BLANK : caller.getPhoneNumber().getCallNumber())));
				serialized.append(m_token);
			}
			
			
			// add firstname
			serialized.append(encode((caller.getName().getFirstname().length()==0 ? BLANK : caller.getName().getFirstname())));
			serialized.append(m_token);
			
			// add lastname
			serialized.append(encode((caller.getName().getLastname().length()==0 ? BLANK : caller.getName().getLastname())));
			serialized.append(m_token);
			
			// add additional
			serialized.append(encode((caller.getName().getAdditional().length()==0 ? BLANK : caller.getName().getAdditional())));
			serialized.append(m_token);
			
			// add caller UUID
			serialized.append(encode((caller.getUUID().length()==0 ? BLANK : caller.getUUID())));
			serialized.append(m_token);
			
			// add attributes
			IAttributeMap al = caller.getAttributes();
			if (al.size()==0)
				serialized.append(BLANK);
			
			if (includeImage && ImageHandler.getInstance().hasImage(caller)) {
				InputStream in = ImageHandler.getInstance().getImageStream(caller);
				if (in!=null) {
					ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
					Base64Encoder b64 = new Base64Encoder(encodedOut);
					Stream.copy(in, b64);						
					b64.flush();
					b64.close();
					
					final String imgdata = StringUtils.replaceString(new String(encodedOut.toByteArray()), m_imglftoken, IMGLFTOKEN_ESCAPE_SYMBOL);
					encodedOut.close();
				
					IAttribute img = new IAttribute() {
						public void setName(String name) {	
						}

						public void setValue(String value) {
						}
						
						public String getName() {
							return IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY;
						}

						public String getValue() {
							return imgdata;
						}
					};
					caller.getAttributes().add(img);
					caller.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEPATH);
					//caller.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEREF);
					caller.getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_IMAGEURL);
				}
			}
				
			Iterator i = al.iterator();
			IAttribute att = null;
			while(i.hasNext()) {
				att = (IAttribute) i.next();
				serialized.append(encode(att.getName()));
				serialized.append(EQUAL);
				serialized.append(encodeAttributeValue(att.getValue()));
				serialized.append(m_atoken);
			}
		
		} catch (Throwable t) {
			throw new SerializerException(t.getMessage());
		}
		return serialized.toString();
	}

	
	private static String toString(ICaller caller, boolean includeImage) throws SerializerException {
		if (caller instanceof IMultiPhoneCaller) {
			return toString((IMultiPhoneCaller)caller, includeImage);			
		}
		
		StringBuffer serialized = new StringBuffer(128);
		try {
			
			// add int area code
			serialized.append(encode((caller.getPhoneNumber().getIntAreaCode().length()==0 ? BLANK : caller.getPhoneNumber().getIntAreaCode())));
			serialized.append(m_token);
			
			// add area code
			serialized.append(encode((caller.getPhoneNumber().getAreaCode().length()==0 ? BLANK : caller.getPhoneNumber().getAreaCode())));
			serialized.append(m_token);
			
			// add call number
			serialized.append(encode((caller.getPhoneNumber().getCallNumber().length()==0 ? BLANK : caller.getPhoneNumber().getCallNumber())));
			serialized.append(m_token);
			
			// add firstname
			serialized.append(encode((caller.getName().getFirstname().length()==0 ? BLANK : caller.getName().getFirstname())));
			serialized.append(m_token);
			
			// add lastname
			serialized.append(encode((caller.getName().getLastname().length()==0 ? BLANK : caller.getName().getLastname())));
			serialized.append(m_token);
			
			// add additional
			serialized.append(encode((caller.getName().getAdditional().length()==0 ? BLANK : caller.getName().getAdditional())));
			serialized.append(m_token);
			
			// add caller UUID
			serialized.append(encode((caller.getUUID().length()==0 ? BLANK : caller.getUUID())));
			serialized.append(m_token);
			
			// add attributes
			IAttributeMap al = caller.getAttributes();
			if (al.size()==0)
				serialized.append(BLANK);
				
			Iterator i = al.iterator();
			IAttribute att = null;
			while(i.hasNext()) {
				att = (IAttribute) i.next();
				serialized.append(encode(att.getName()));
				serialized.append(EQUAL);
				serialized.append(encodeAttributeValue(att.getValue()));
				serialized.append(m_atoken);
			}
		
		} catch (Throwable t) {
			throw new SerializerException(t.getMessage());
		}
		return serialized.toString();
	}
	
	/**
	 * Serializes a caller object into an byte array.
	 * 
	 * @param caller caller to be serialized
	 * @return serialized byte array
	 * @throws SerializerException
	 */
	public static byte[] toByteArray(ICaller caller, boolean includeImage) throws SerializerException {
		return toString(caller, includeImage).getBytes();
	}
	
	/**
	 * Serializes a caller object into an byte array.
	 * 
	 * @param caller caller to be serialized
	 * @return serialized byte array
	 * @throws SerializerException
	 */
	public static byte[] toByteArray(ICaller caller) throws SerializerException {
		return toString(caller, false).getBytes();
	}
	
	/**
	 * Serializes a caller object into an inputstream.
	 * 
	 * @param caller caller to be serialized
	 * @return serialized inputstream 
	 * @throws SerializerException
	 */
	public static InputStream toInputStream(ICaller caller) throws SerializerException {
		return new ByteArrayInputStream(toByteArray(caller));
	}
	
	/**
	 * Deserializes a byte array stream into an caller object.
	 * 
	 * @param caller caller as a byte array representation.
	 * @param runtime runtime to used
	 * @return caller object 
	 * @throws SerializerException
	 */
	public static ICaller toCaller(byte[] caller, IRuntime runtime) throws SerializerException {
		if (runtime == null)
			throw new SerializerException("Runtime object is not set but required.");
		
		String callerString = new String(caller);
		
		// tokenize the input
		StringTokenizer st = new StringTokenizer(callerString, m_token);
		if (st.countTokens()<8) {
			throw new SerializerException("Caller format is invalid: "+callerString);
		}
		
		try {
			// Check for new mpc format
			String test = st.nextToken().trim();
			List phones = null;
			if (test.equalsIgnoreCase("mpc")) {
				int phone_count = Integer.parseInt(st.nextToken().trim());
				phones = new ArrayList(phone_count);
				for (int i=0; i<phone_count; i++) {
					// build number
					IPhonenumber pn = 
						runtime.getCallerFactory().createPhonenumber(
							decode(st.nextToken().trim()), // token 1
							decode(st.nextToken().trim()),
							decode(st.nextToken().trim())
						);
					if (pn.getCallNumber().length()==0) {
						pn.setClired(true);
					}
					phones.add(pn);
				}
				
				
			} else {
				phones = new ArrayList(1);
				// build number
				IPhonenumber pn = 
					runtime.getCallerFactory().createPhonenumber(
						test, // token 1
						decode(st.nextToken().trim()), // token 2
						decode(st.nextToken().trim())  // token 3
					);
				if (pn.getCallNumber().length()==0) {
					pn.setClired(true);
				}
				phones.add(pn);
			}
			
			// build name
			IName name = runtime.getCallerFactory().createName(
				decode(st.nextToken().trim()), // token 4
				decode(st.nextToken().trim()), // token 5
				decode(st.nextToken().trim())  // token 6
			);
			
			String UUID = decode(st.nextToken().trim()); // token 7
			
			// build attributes
			String attString = decode(st.nextToken().trim());
			IAttributeMap attList = runtime.getCallerFactory().createAttributeMap();
			if (attString.length()>0) {
				StringTokenizer ast = new StringTokenizer(attString, m_atoken);
				String attrib = null;
				while (ast.hasMoreTokens()) {
					attrib = ast.nextToken().trim();
					if (attrib.indexOf(EQUAL)>-1) {
						IAttribute att = runtime.getCallerFactory().createAttribute(
							decode(attrib.substring(0, attrib.indexOf(EQUAL))),
							decodeAttributeValue(attrib.substring(attrib.indexOf(EQUAL)+EQUAL.length()))
						);
						attList.add(att);
					}
				}
				// check for imagebinary
				if (attList.contains(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY)) {
					IAttribute imgbinary = attList.get(IJAMConst.ATTRIBUTE_NAME_IMAGEBINARY);
					if (imgbinary!=null) {
						imgbinary.setValue(StringUtils.replaceString(imgbinary.getValue(), IMGLFTOKEN_ESCAPE_SYMBOL, m_imglftoken));
					}
				}
			}
			
			// create an IMultiPhoneCaller object
			if (test.equalsIgnoreCase("mpc") || phones.size()>1) {
				return runtime.getCallerFactory().createCaller(
						UUID,
						name,
						phones,
						attList
					);
			}
			
			// create an ICaller object
			return runtime.getCallerFactory().createCaller(
				UUID,
				name,
				(IPhonenumber) phones.get(0),
				attList
			);
		} catch (Exception e) {
			throw new SerializerException(e.getMessage());
		}
	}
	
	/**
	 * Serializes a call object into an byte array.
	 * 
	 * @param call call to be serialized
	 * @return serialized byte array
	 * @throws SerializerException
	 */
	public static byte[] toByteArray(ICall call) throws SerializerException {
		return toString(call, false).getBytes();
	}
	
	/**
	 * Serializes a call object into an byte array.
	 * 
	 * @param call call to be serialized
	 * @return serialized byte array
	 * @throws SerializerException
	 */
	public static byte[] toByteArray(ICall call, boolean includeImage) throws SerializerException {
		return toString(call, includeImage).getBytes();
	}
	
	/**
	 * Serializes a call object into an inputstream.
	 * 
	 * @param call call to be serialized
	 * @return serialized inputstream 
	 * @throws SerializerException
	 */
	public static InputStream toInputStream(ICall call) throws SerializerException {
		return new ByteArrayInputStream(toByteArray(call));
	}
	
	/**
	 * Deserializes a byte array stream into an call object.
	 * 
	 * @param call call as a byte array representation.
	 * @param runtime runtime to used
	 * @return call object 
	 * @throws SerializerException
	 */
	public static ICall toCall(byte[] call, IRuntime runtime) throws SerializerException {
		if (runtime == null)
			throw new SerializerException("Runtime object is not set but required.");
		

		String callString = new String(call);
		if(callString.indexOf(m_ctoken)<0)
			throw new SerializerException("Call format is invalid. Call is set <"+callString+">");
		
		// tokenize the whole input
		StringTokenizer st = new StringTokenizer(callString, m_ctoken);
		if (st.countTokens()!=2)
			throw new SerializerException("Call format is invalid. Found "+st.countTokens()+" tokens, but required are exactly 2.");
		
		ICaller caller = toCaller(st.nextToken().getBytes(), runtime);
		
		if (!st.hasMoreTokens())
			throw new SerializerException("Call format is invalid. Second token is empty.");
		
		// tokenize to call data
		callString = st.nextToken();
		st = new StringTokenizer(callString, m_token);
		
		if (st.countTokens()<7) 
			throw new SerializerException("Call format is invalid. Token count < 7.");
		
		// build MSN
		IMsn msn = runtime.getCallFactory().createMsn(
			decode(st.nextToken().trim()), // token 1
			decode(st.nextToken().trim())  // token 2
		);
		
		if (msn.getMSN().equalsIgnoreCase("*")) {
			msn.setMSN("0");
		}
		
		// build CIP
		ICip cip = runtime.getCallFactory().createCip(
			decode(st.nextToken().trim()), // token 3
			decode(st.nextToken().trim())  // token 4
		);
		
		String uuid = decode(st.nextToken().trim()); // token 5
		
		Date date = new Date(Long.parseLong(decode(st.nextToken().trim()))); // token 6

		// build attributes
		String attString = decode(st.nextToken().trim());
		IAttributeMap attList = runtime.getCallFactory().createAttributeMap();
		if (attString.length()>0) {
			StringTokenizer ast = new StringTokenizer(attString, m_atoken);
			String attrib = null;
			while (ast.hasMoreTokens()) {
				attrib = ast.nextToken().trim();
				if (attrib.indexOf(EQUAL)>-1) {
					IAttribute att = runtime.getCallFactory().createAttribute(
						decode(attrib.substring(0, attrib.indexOf(EQUAL))),
						decodeAttributeValue(attrib.substring(attrib.indexOf(EQUAL)+EQUAL.length()))
					);
					attList.add(att);
				}
			}
		}
		
		return runtime.getCallFactory().createCall(
			uuid,
			caller,
			msn,
			cip,
			date,
			attList
		);
	}

}
