package de.janrufmonitor.repository.identify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.CallerNotFoundException;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.RepositoryManagerComparator;
import de.janrufmonitor.repository.types.IIdentifyCallerRepository;
import de.janrufmonitor.runtime.IRuntime;

/**
 * Static class implementation of an identification which makes use of the framework.
 * 
 * @author Thilo Brandt
 * @created    2006/08/25
 */
public final class Identifier {
	
	private static Logger logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);

	/**
	 * Identifies a caller with all active caller managers specified in the configuration.
	 * 
	 * @param r the current runtime
	 * @param pn a valid number, should not be a clired number or null
	 * @return an identified ICaller object or null, if not identified by any caller manager
	 */
	public static ICaller identify(IRuntime r, IPhonenumber pn) {
		List activeCallerManagers = getAllActiveCallerManagers(r);
		return identify(r, pn, activeCallerManagers);
	}
	
	/**
	 * Identifies a caller with the default caller manager specified in the configuration.
	 * 
	 * @param r the current runtime
	 * @param pn a valid number, should not be a clired number or null
	 * @return an identified ICaller object or null, if not identified by the default caller manager
	 */
	public static ICaller identifyDefault(IRuntime r, IPhonenumber pn) {
		List activeCallerManagers = new ArrayList(1);
		ICallerManager def = r.getCallerManagerFactory().getDefaultCallerManager();
		if (def!=null && def.isActive() && def.isSupported(IIdentifyCallerRepository.class))
			activeCallerManagers.add(def);
		else {
			Identifier.logger.info("Problem with default caller manager: Either reference is null, managers is deactived or it does not support identification.");		
		}
		
		return identify(r, pn, activeCallerManagers);
	}
	
	/**
	 * Identifies a caller with all active caller managers specified in the configuration.
	 * 
	 * @param r the current runtime
	 * @param pn a valid number, should not be a clired number or null
	 * @param activeCallerManagers a list of active caller managers
	 * @return an identified ICaller object or null, if not identified by any caller manager
	 */
	public static ICaller identify(IRuntime r, IPhonenumber pn, List activeCallerManagers) {
		if (activeCallerManagers==null || activeCallerManagers.size()==0) return null;
		
		long start = System.currentTimeMillis();
		Identifier.logger.info("<---- Begin caller identification ---->");		
		
		Identifier.logger.info("Order of identification: "+activeCallerManagers.toString());

		ICaller identifiedCaller = null;

		int i = 0;
		Object obj = null;
		ICallerManager cm = null;
		while (identifiedCaller==null && i<activeCallerManagers.size()) {
			obj = activeCallerManagers.get(i);
			i++;
			if (obj!=null && obj instanceof ICallerManager) {
				cm = (ICallerManager)obj;
				try {
					if (cm.isActive() && cm.isSupported(IIdentifyCallerRepository.class)) {
						identifiedCaller = ((IIdentifyCallerRepository)cm).getCaller(pn);
						Identifier.logger.info("Caller identified by ["+cm.getManagerID()+"]: "+identifiedCaller);						
					}
					
				} catch (CallerNotFoundException e) {
					Identifier.logger.info("Caller was not identified by ["+cm.getManagerID()+"]: "+e.getMessage());
				}
			} else {
				Identifier.logger.severe("Invalid caller manager object: "+obj);
			}
		}
		Identifier.logger.info("<---- Finished caller identification ("+(System.currentTimeMillis()-start)+" msec.) ---->");	
		return identifiedCaller;
	}
	
	private static List getAllActiveCallerManagers(IRuntime r) {
		List allManagers = r.getCallerManagerFactory().getAllCallerManagers();
		List activeManager = new ArrayList();
		Object o = null;
		ICallerManager cm = null;
		for (int i=0;i<allManagers.size();i++) {
			o = allManagers.get(i);
			if (o!=null && o instanceof ICallerManager) {
				cm = (ICallerManager)o;
				if (cm.isActive()) {
					activeManager.add(cm);
				}	
			}
		}
		Collections.sort(activeManager, new RepositoryManagerComparator());

		Identifier.logger.info("List with all caller managers: "+allManagers.toString());	
		Identifier.logger.info("List with all active caller managers: "+activeManager.toString());		
		return activeManager;
	}

}
