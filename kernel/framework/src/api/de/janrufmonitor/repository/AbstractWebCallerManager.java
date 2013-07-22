package de.janrufmonitor.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.repository.web.AbstractURLRequester;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.string.StringUtils;

/**
 *  This abstract class can be used as base class for a new call manager implementation
 * using webaccess to the caller persistence.
 *
 *@author     Thilo Brandt
 *@created    2006/08/24
 */
public abstract class AbstractWebCallerManager extends AbstractReadOnlyCallerManager {

	private class Executor implements Runnable {

		boolean m_failed;
		AbstractURLRequester m_r;
		Exception m_ex;
		
		public Executor(AbstractURLRequester r) {
			this.m_r = r;
		}
		
		public void run() {
			try {
				this.m_r.go();		
				this.m_failed = false;
			} catch (Exception e) {
				this.m_failed = true;
				this.m_ex = e;
			}
		}
		
		public boolean isFailed() {
			return m_failed;
		}
		
		public Exception getException() {
			return this.m_ex;
		}
	}
	
	private static String CFG_URL = "url";
	private static String CFG_SKIP = "skipbytes";
	private static String CFG_USECACHE = "usecache";
	private static String CFG_USERAGENT = "useragent";
	private static String CFG_KEEP_SOURCE_NUMBER = "keepsourcenumber";
	
	private Map m_cache;
	private Map m_unidentified;
	
	public AbstractWebCallerManager() {
		super();
	}

	public abstract IRuntime getRuntime();

	public abstract String getID();

	public abstract String getNamespace();
	
	protected abstract AbstractURLRequester createURLRequester(String url, long skip);

	protected AbstractURLRequester createURLRequester(String url, long skip, String pn) {
		return this.createURLRequester(url, skip);
	}
	
	protected String getSupportedIntAreaCode() {
		return this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA);
	}
	
	public ICaller getCaller(IPhonenumber number) throws CallerNotFoundException {
    	if (number==null)
			throw new CallerNotFoundException("Phone number is not set (null). No caller found.");
		
		if (number.isClired())
			throw new CallerNotFoundException("Phone number is CLIR. Identification impossible.");
		
		if (this.isInternalNumber(number))
			throw new CallerNotFoundException("Phone number is internal number.");

		if (isUsedCache() && this.m_unidentified!=null && this.m_unidentified.containsKey(number.getTelephoneNumber())) {
			Integer io = (Integer) this.m_unidentified.get(number.getTelephoneNumber());
			if (io.intValue()==10) {
				this.m_unidentified.remove(number.getTelephoneNumber());
			} else {
				this.m_unidentified.put(number.getTelephoneNumber(), new Integer(io.intValue()+1));
			}
			
			throw new CallerNotFoundException("Phone number "+number.getTelephoneNumber()+" not identified. (cached)");
		}
		
		ICaller caller = null;
		
		if (isUsedCache() && this.m_cache!=null && this.m_cache.containsKey(number)) {
			caller = (ICaller) this.m_cache.get(number);
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Taking caller from cache: "+caller);
			if (caller!=null)
				return caller;
		} else {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Number is not cached, start identifiing ...");
			
			IPhonenumber pn = null;
			
			try {
				ICallerManager defaultManager = this.getRuntime().getCallerManagerFactory().getDefaultCallerManager();
				if (defaultManager!=null && defaultManager.isActive()&& defaultManager.isSupported(IIdentifyCallerRepository.class)) {
					ICaller defaultIdentified = ((IIdentifyCallerRepository)defaultManager).getCaller(number);
					pn = defaultIdentified.getPhoneNumber();
				}
			} catch (CallerNotFoundException e) {
				// ignore this exception
			}
			
			// check correct country code
			if (!this.getSupportedIntAreaCode().equalsIgnoreCase("00")) {
				if (pn!=null && !pn.getIntAreaCode().equalsIgnoreCase(this.getSupportedIntAreaCode()))
					throw new CallerNotFoundException("Phone number has country code "+pn.getIntAreaCode()+". Caller manager "+this.getID()+" is supporting only "+this.getSupportedIntAreaCode());
			
				// added 2013/07/22: number format compatibility 0911234567 must be equal +49 (911) 234567
				if (pn!=null) number.setTelephoneNumber(pn.getTelephoneNumber());
			}
			
			// added 2009/05/28
			// add detection of web services which provides number in middle of URL
			String url = this.getURL();
			if (url.indexOf(IJAMConst.GLOBAL_VARIABLE_WEBCM_NUMBER)>0) {
				url = StringUtils.replaceString(url, IJAMConst.GLOBAL_VARIABLE_WEBCM_NUMBER, (number.getTelephoneNumber().startsWith("0") ? "" : "0") + number.getTelephoneNumber());
			} else {
				// added 2010/11/18: added URL attribute parsing
				String urlx = url;
				url = Formatter.getInstance(this.getRuntime()).parse(url, pn);
				if (urlx.equalsIgnoreCase(url))
					url += (number.getTelephoneNumber().startsWith("0") ? "" : "0") + number.getTelephoneNumber();
			}

			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("URL to call: "+url);

			AbstractURLRequester r = this.createURLRequester(
					url,
					this.getSkipBytes(),
					number.getTelephoneNumber()
				);
			
			try {
				long ts = System.currentTimeMillis();
				Executor ex = new Executor(r);
				Thread t = new Thread(ex);
				t.start();
				while (t.isAlive() && (System.currentTimeMillis() - ts < r.getTimeout())) {
					Thread.sleep(100);
				}
				
				if (ex!=null && ex.isFailed()){
					if (this.m_unidentified!=null && !this.m_unidentified.containsKey(number.getTelephoneNumber())) 
						this.m_unidentified.put(number.getTelephoneNumber(), new Integer(1));
					throw new CallerNotFoundException("Phone number "+number.getTelephoneNumber()+" not identified.", (ex.getException()!=null ? ex.getException() : new Exception()));
				}
				
				if (t.isAlive()) throw new Exception ("Identification thread is blocking.");
				
				//r.go();
			} catch (Exception e) {
				if (this.m_unidentified!=null && !this.m_unidentified.containsKey(number.getTelephoneNumber())) 
					this.m_unidentified.put(number.getTelephoneNumber(), new Integer(1));
				throw new CallerNotFoundException("Phone number "+number.getTelephoneNumber()+" not identified: "+e.getMessage(), e);
			}
			
			IAttributeMap m = r.getAttributes();
			if (pn==null) pn = r.getPhonenumber();
			
			if (!getKeepSourceNumber() && pn!=null && r.getPhonenumber()!=null && !r.getPhonenumber().getTelephoneNumber().endsWith(pn.getTelephoneNumber())) {
				pn = r.getPhonenumber();
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Incoming call number "+number.getTelephoneNumber()+" was not identified but extension "+pn.getTelephoneNumber());
			}
			
			caller = this.getRuntime().getCallerFactory().createCaller(
				pn
			);
			
			caller.setAttributes(m);
			
			IAttribute cm = this.getRuntime().getCallerFactory().createAttribute(
				IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER,
				this.getID()
			);
			caller.setAttribute(cm);
			
			// add caller to cache
			if (this.m_cache!=null)
				this.m_cache.put(number, caller);
			
			return caller;
		}
		throw new CallerNotFoundException("No caller found for number "+number);
	}
	
	public void shutdown() {
		super.shutdown();
		if (this.m_cache!=null)
			this.m_cache.clear();
		this.m_cache = null;
		
		if (this.m_unidentified!=null)
			this.m_unidentified.clear();
		this.m_unidentified = null;
		
	}
	public void startup() {
		super.startup();
		this.m_cache = new HashMap();
		this.m_unidentified = new HashMap();
	}
	
	private String getURL() {
		return this.m_configuration.getProperty(CFG_URL, "");
	}
	
	private long getSkipBytes() {
		return Long.parseLong(this.m_configuration.getProperty(CFG_SKIP, "1"));
	}
	
	private boolean getKeepSourceNumber() {
		return this.m_configuration.getProperty(CFG_KEEP_SOURCE_NUMBER, "true").equalsIgnoreCase("true");
	}
	
	private boolean isUsedCache() {
		return this.m_configuration.getProperty(CFG_USECACHE, "true").equalsIgnoreCase("true");
	}
	
	protected String getUserAgent() {
		return this.m_configuration.getProperty(CFG_USERAGENT, "Mozilla/4.0 (compatible; MSIE; Windows NT)");
	}

}
