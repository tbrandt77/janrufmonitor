package de.janrufmonitor.ui.jface.application.journal;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.filter.CallerFilter;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.search.Operator;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.ISearchableCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.IExtendedApplicationController;

public class JournalController implements IExtendedApplicationController, JournalConfigConst {
	
	private Logger m_logger;
	private IRuntime m_runtime;
	private Properties m_configuration;
	private ICallList m_data;
	
	public JournalController() {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
	}
	
	public void setConfiguration(Properties configuration, boolean initialize) {
		if (configuration!=null)
			this.m_configuration = configuration;
		else {
			this.m_logger.severe("Configuration data in controller is invalid.");
		}
		if (initialize)
			this.m_data = null;
	}
	
	public synchronized Object[] getElementArray() {
		if (this.m_data==null) 
			this.buildControllerData();

		return this.m_data.toArray();
	}
	
	public synchronized void deleteAllElements() {
		ICallManager cm = this._getRepository();
		if (cm!=null && this.m_data!=null && cm.isActive() && cm.isSupported(IWriteCallRepository.class)) {
			((IWriteCallRepository)cm).removeCalls(this.m_data);
			this.m_data = null;
		}
	}
	
	public synchronized void deleteElements(Object list) {
		if(list instanceof ICallList) {
			ICallManager cm = this._getRepository();
			if (cm!=null && cm.isActive() && cm.isSupported(IWriteCallRepository.class)) {
				((IWriteCallRepository)cm).removeCalls((ICallList)list);
				this.m_data = null;
			}
		}
	}
	
	public synchronized void addElements(Object list) {
		if(list instanceof ICallList) {
			ICallManager cm = this._getRepository();
			if (cm!=null && list!=null && cm.isActive() && cm.isSupported(IWriteCallRepository.class)) {
				((IWriteCallRepository)cm).setCalls((ICallList)list);
				this.m_data = null;
			}
		}
	}
	
	public void updateElement(Object call) {
		this.updateElement(call, true);
	}
	
	public synchronized void updateElement(Object call, boolean isUpdateAll) {
		if(call instanceof ICall) {
			ICallManager cm = this._getRepository();
			if (cm!=null&& cm.isActive() && cm.isSupported(IReadCallRepository.class)&& cm.isSupported(IWriteCallRepository.class)) {
				ICaller caller = ((ICall)call).getCaller();
				if (!caller.getPhoneNumber().isClired() && isUpdateAll) {
					ICallList cl = ((IReadCallRepository)cm).getCalls(
						new CallerFilter(caller)
					);
					ICall aCall = null;
					for (int i=cl.size()-1;i>=0;i--) {
						aCall = cl.get(i);
						aCall.setCaller(caller);
					}
					((IWriteCallRepository)cm).updateCalls(cl);
				} else {
					// update a single CLIR call or isUpdateAll == false
					if (cm.isSupported(IWriteCallRepository.class))
						((IWriteCallRepository)cm).updateCall((ICall) call);
				}
			}
		}
	}
	
	public synchronized int countElements() {
		ICallManager cm  = this._getRepository();
		if (cm!=null && cm.isActive() && cm.isSupported(IReadCallRepository.class)) {
			if (cm.isSupported(ISearchableCallRepository.class)) {
				return ((ISearchableCallRepository)cm).getCallCount(this.getFilters(), this.getSearchTerms());
			}
			return ((IReadCallRepository)cm).getCallCount(this.getFilters());
		}
		return 0;
	}
	
	public synchronized void sortElements() {
		if (this.m_data==null) 
			this.buildControllerData();
	    doSorting();   
	}
	
	public ICallList getCallList() {
		return this.m_data;
	}
	
	private void buildControllerData() {
		ICallManager cm  = this._getRepository();
		if (cm!=null && cm.isActive() && cm.isSupported(IReadCallRepository.class)) {
			if (cm.isSupported(ISearchableCallRepository.class)) {
				this.m_data = ((ISearchableCallRepository)cm).getCalls(this.getFilters(), countElements(), 0, this.getSearchTerms());
			} else {
				this.m_data = ((IReadCallRepository)cm).getCalls(this.getFilters(), countElements(), 0);
			}
			this.doSorting();
		}
		if (this.m_data==null) 
			this.m_data = this.getRuntime().getCallFactory().createCallList();
	}
	
	private void doSorting() {
		if (this.m_data!=null && this.m_data.size()>1) {
			this.m_data.sort(this.getSortOrder(), this.getSortDirection());
		}
	}

	private ICallManager _getRepository() {
		String managerID = this.m_configuration.getProperty(CFG_REPOSITORY, "");
		if (managerID.length()>0) {
			ICallManager cm = this.getRuntime().getCallManagerFactory().getCallManager(managerID);
			if (cm!=null) return cm;
		}
		this.m_logger.severe("CallManager with ID "+managerID+" does not exist.");
		return null;
	}
	
	private IFilter[] getFilters() {
		String fstring = this.m_configuration.getProperty(CFG_FILTER, "");
		return new JournalFilterManager().getFiltersFromString(fstring);
	}
	
	private ISearchTerm[] getSearchTerms() {
		String st = this.m_configuration.getProperty(CFG_SEARCHTERMS, "");
		if (st!=null && st.trim().length()>0) {
			List terms = new ArrayList();
			StringTokenizer and_t = new StringTokenizer(st, Operator.AND.toString());
			final String[] ands = new String[and_t.countTokens()];
			int i=0;
			while (and_t.hasMoreTokens()) {
				ands[i] = and_t.nextToken().trim();
				i++;
			}
			
			for (i=0;i<ands.length;i++) {
				final String term = ands[i];
				final StringTokenizer or_t = new StringTokenizer(ands[i], Operator.OR.toString());
				if (or_t.countTokens()==1) {
					terms.add(new ISearchTerm() {
						public String getSearchTerm() {
							return term.trim();
						}

						public Operator getOperator() {
							return Operator.AND;
						}
						public String toString() {
							return term + "->"+Operator.AND.toString();
						}});
					or_t.nextToken();
				}
				while (or_t.hasMoreTokens()) {
					final String termo = or_t.nextToken().trim();
					terms.add(new ISearchTerm() {
						public String toString() {
							return termo + "->"+Operator.OR.toString();
						}

						public String getSearchTerm() {
							return termo;
						}

						public Operator getOperator() {
							return Operator.OR;
						}
						
						});
				}
			}
			
			ISearchTerm[] s = new ISearchTerm[terms.size()];
			for (int j=terms.size(), k=0;k<j;k++) {
				s[k] = (ISearchTerm) terms.get(k);
			}
			return s;
		}
		return null;
	}
	
	private int getSortOrder() {
		return Integer.parseInt(this.m_configuration.getProperty(CFG_ORDER, "0"));
	}

	private boolean getSortDirection() {
		return (this.m_configuration.getProperty(CFG_DIRECTION, "false")).equalsIgnoreCase("true");
	}

	private IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
	
	public void generateElementArray(Object[] data) {
		if (data != null) {
			this.m_data = this.getRuntime().getCallFactory().createCallList();
			for (int i=0;i<data.length;i++) {
				if (data[i] instanceof ICall)
					this.m_data.add((ICall) data[i]);
			}
		}		
	}

	public Object getRepository() {
		return this._getRepository();
	}

}
