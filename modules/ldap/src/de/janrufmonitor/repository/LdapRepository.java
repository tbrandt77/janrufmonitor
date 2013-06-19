package de.janrufmonitor.repository;

import java.util.logging.Level;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class LdapRepository extends AbstractReadWriteCallerManager
		implements IRemoteRepository, ILdapRepositoryConst {

	public static String ID = "LdapRepository";
	public static String NAMESPACE = "repository.LdapRepository";
	
	private IRuntime m_runtime;
	private LdapContactsProxy m_proxy = null;
	
	public LdapRepository() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}
	
	public String getID() {
		return LdapRepository.ID;
	}

	public String getNamespace() {
		return LdapRepository.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public ICaller getCaller(IPhonenumber number)
			throws CallerNotFoundException {
		if (number == null)
			throw new CallerNotFoundException(
					"Phone number is not set (null). No caller found.");

		if (number.isClired())
			throw new CallerNotFoundException(
					"Phone number is CLIR. Identification impossible.");
		
		try {
			ICaller c = getProxy().identify(number);
			if (c!=null) return c;
		} catch (LdapContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "loginerror", e));
		}
		throw new CallerNotFoundException(
				"No caller entry found for phonenumber : "
						+ number.getTelephoneNumber());
	}

	public ICallerList getCallers(IFilter filter) {
		try {
			if (filter!=null && filter.getType().equals(FilterType.ATTRIBUTE)) {
				IAttributeMap m = ((AttributeFilter)filter).getAttributeMap();
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_CATEGORY)) {
					IAttribute a = m.get(IJAMConst.ATTRIBUTE_NAME_CATEGORY);
					return getProxy().getContacts(a.getValue());
				}				
			}
			if (filter!=null && filter.getType().equals(FilterType.PHONENUMBER)) {
//				String intarea = ((PhonenumberFilter)filter).getPhonenumber().getIntAreaCode();
//				String area = ((PhonenumberFilter)filter).getPhonenumber().getAreaCode();
//				return getProxy().getContacts(intarea+area);
			}
			return getProxy().getContacts(null);
		} catch (LdapContactsException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "loginerror", e));
		}
		return getRuntime().getCallerFactory().createCallerList();
	}

	// begin - to be changed if write support
	public boolean isSupported(Class c) {
		if ((c.equals(IIdentifyCallerRepository.class) || c.equals(IReadCallerRepository.class) || c.equals(IRemoteRepository.class))) {
			return super.isSupported(c);
		}
		return false;
	}

	public void removeCaller(ICaller caller) {
	}

	public void setCaller(ICaller caller) {
	}
	// end - to be changed if write support
	
	private LdapContactsProxy getProxy() {
		if (this.m_proxy==null) {
			this.m_proxy = new LdapContactsProxy();
			this.m_proxy.start();
		}			
		return m_proxy;
	}
	
	public void shutdown() {
		if (this.m_proxy!=null) this.m_proxy.stop();
		this.m_proxy = null;
		super.shutdown();	
	}
	
	public void startup() {
		super.startup();
		if (this.isActive()) {
			Thread t = new Thread() {
				public void run() {
					try {
						ICallerList c = getProxy().getContacts(null);
						if (m_logger.isLoggable(Level.INFO) && c.size()>0)
							m_logger.info("Found "+c.size()+" LDAP contacts to be synced.");
					} catch (LdapContactsException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			};
			t.setName("JAM-LdapUUIDCheck-Thread-(deamon)");
			t.setDaemon(true);
			t.start();
		}		
	}

}
