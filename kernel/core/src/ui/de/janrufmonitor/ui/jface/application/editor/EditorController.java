package de.janrufmonitor.ui.jface.application.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.search.SearchTermSeriarlizer;
import de.janrufmonitor.repository.types.IReadCallerRepository;
import de.janrufmonitor.repository.types.ISearchableCallerRepository;
import de.janrufmonitor.repository.types.IWriteCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.IApplicationController;
import de.janrufmonitor.util.string.StringUtils;

public class EditorController implements IApplicationController,
		EditorConfigConst {

	protected Logger m_logger;

	protected Properties m_configuration;

	private IRuntime m_runtime;

	private ICallerList m_data;

	public EditorController() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
	}

	public void setConfiguration(Properties configuration, boolean initialize) {
		if (configuration != null)
			this.m_configuration = configuration;
		else {
			this.m_logger
					.severe("Configuration data in controller is invalid.");
		}
		if (initialize)
			this.m_data = null;
	}

	public synchronized Object[] getElementArray() {
		if (this.m_data == null)
			this.buildControllerData();

		return this.m_data.toArray();
	}

	public synchronized void deleteAllElements() {
		if (this.m_data != null) {
			this.deleteElements(this.m_data);
		}
	}

	public synchronized void deleteElements(Object list) {
		if (list != null && list instanceof ICallerList) {
			Map cms = new HashMap();

			ICallerList tmplist = null;
			ICaller c = null;
			for (int i = 0; i < ((ICallerList) list).size(); i++) {
				c = ((ICallerList) list).get(i);
				IAttribute att = c
						.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER);

				String cname = (att == null ? "all" : att.getValue());
				if (cms.containsKey(cname)) {
					tmplist = (ICallerList) cms.get(cname);
					tmplist.add(c);
				} else {
					tmplist = this.getRuntime().getCallerFactory()
							.createCallerList(1);
					tmplist.add(c);
					cms.put(cname, tmplist);
				}
			}

			List managers = this.getActiveCallerManagers();

			ICallerManager mgr = null;
			for (int i = 0; i < managers.size(); i++) {
				mgr = this.getRuntime().getCallerManagerFactory()
						.getCallerManager((String) managers.get(i));
				if (mgr != null) {
					if (mgr.isSupported(IWriteCallerRepository.class)) {
						tmplist = (ICallerList) cms.get("all");
						if (tmplist != null) {
							this.m_logger.info("removing " + tmplist.size()
									+ " callers to manager: "
									+ mgr.getManagerID());
							((IWriteCallerRepository) mgr)
									.removeCaller(tmplist);
						}

						tmplist = (ICallerList) cms.get(mgr.getManagerID());
						if (tmplist != null) {
							this.m_logger.info("removing " + tmplist.size()
									+ " callers to manager: "
									+ mgr.getManagerID());
							((IWriteCallerRepository) mgr)
									.removeCaller(tmplist);
						}
					}
				}
			}
		}
	}

	public synchronized void addElements(Object list) {
		if (list != null && list instanceof ICallerList) {
			ICallerManager mgr = this._getRepository();
			if (mgr != null) {
				if (mgr.isSupported(IWriteCallerRepository.class)) {
					((IWriteCallerRepository) mgr)
							.setCaller((ICallerList) list);
				}
			}
		}
	}

	public synchronized void updateElement(Object element) {
		if (element != null && element instanceof ICallerList) {
			ICallerManager mgr = this._getRepository();
			if (mgr != null) {
				if (mgr.isSupported(IWriteCallerRepository.class)) {
					for (int i=0,j=((ICallerList)element).size();i<j;i++) {
						((IWriteCallerRepository) mgr)
						.updateCaller(((ICallerList)element).get(i));
					}
				}
			}			
		}
		if (element != null && element instanceof ICaller) {
			List managers = this.getActiveCallerManagers();

			ICallerManager mgr = null;
			IAttribute att = ((ICaller) element)
					.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER);

			for (int i = 0; i < managers.size(); i++) {
				mgr = this.getRuntime().getCallerManagerFactory()
						.getCallerManager((String) managers.get(i));
				if (mgr != null) {
					if (mgr.isSupported(IWriteCallerRepository.class)) {
						if (att == null
								|| att.getValue().equalsIgnoreCase(
										mgr.getManagerID()))
							((IWriteCallerRepository) mgr)
									.updateCaller((ICaller) element);
					}
				}
			}
		}
	}

	public synchronized int countElements() {
		if (this.m_data == null)
			this.buildControllerData();

		return this.m_data.size();
	}

	public synchronized void sortElements() {
		if (this.m_data == null)
			this.buildControllerData();
		doSorting();
	}

	public ICallerList getCallerList() {
		return this.m_data;
	}

	private void doSorting() {
		if (this.m_data != null && this.m_data.size() > 1) {
			this.m_data.sort(this.getSortOrder(), this.getSortDirection());
		}
	}

	private void buildControllerData() {
		ICallerManager cm = this._getRepository();
		if (cm != null && cm.isActive() && cm.isSupported(IReadCallerRepository.class)) {
			if (cm.isSupported(ISearchableCallerRepository.class)) {
				this.m_data = ((ISearchableCallerRepository)cm).getCallers(new IFilter[] { this.getFilter() }, new SearchTermSeriarlizer().getSearchTermsFromString(StringUtils.urlEncode(this.m_configuration.getProperty(CFG_SEARCHTERMS, ""))));
			} else {
				this.m_data = ((IReadCallerRepository) cm).getCallers(this.getFilter());
			}
			if (this.m_data == null)
				this.m_data = this.getRuntime().getCallerFactory()
						.createCallerList();
			
			this.doSorting();
			
			List lastnames = new ArrayList(26);
			List cities = new ArrayList(26);
			List pcode = new ArrayList(9);
			List countries = new ArrayList(12);
			ICaller c = null;
			for (int i = 0; i < this.m_data.size(); i++) {
				c = this.m_data.get(i);
				if (c instanceof ICaller) {
					if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_LASTNAME) && c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue().trim().length()>0) {
						if (!hasForbiddenFilterChars(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue().substring(0, 1).toUpperCase()))
							lastnames.add(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_LASTNAME).getValue().substring(0, 1).toUpperCase());
					}
					if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_CITY) && c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue().trim().length()>0) {
						if (!hasForbiddenFilterChars(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue().substring(0, 1).toUpperCase()))
							cities.add(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_CITY).getValue().substring(0, 1).toUpperCase());
					}
					if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE) && c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue().trim().length()>0) {
						if (!hasForbiddenFilterChars(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue().substring(0, 1).toUpperCase()))
							pcode.add(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE).getValue().substring(0, 1).toUpperCase());
					}
					if (c.getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_COUNTRY) && c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue().trim().length()>0) {
						if (!hasForbiddenFilterChars(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue().substring(0, 1).toUpperCase()))
							countries.add(c.getAttributes().get(IJAMConst.ATTRIBUTE_NAME_COUNTRY).getValue().substring(0, 1).toUpperCase());
					}
				}
			}
			
			Collections.sort(lastnames);
			Collections.sort(cities);
			Collections.sort(pcode);
			Collections.sort(countries);
			for (int i=0;i<lastnames.size();i++) {
				this.m_configuration.put("filter_"+IJAMConst.ATTRIBUTE_NAME_LASTNAME+"_"+(String)lastnames.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_LASTNAME+"="+(String)lastnames.get(i)+")");
				this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(Editor.NAMESPACE,"filter_"+IJAMConst.ATTRIBUTE_NAME_LASTNAME+"_"+(String)lastnames.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_LASTNAME+"="+(String)lastnames.get(i)+")");
			}
			for (int i=0;i<cities.size();i++) {
				this.m_configuration.put("filter_"+IJAMConst.ATTRIBUTE_NAME_CITY+"_"+(String)cities.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_CITY+"="+(String)cities.get(i)+")");
				this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(Editor.NAMESPACE,"filter_"+IJAMConst.ATTRIBUTE_NAME_CITY+"_"+(String)cities.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_CITY+"="+(String)cities.get(i)+")");
			}
			for (int i=0;i<pcode.size();i++) {
				this.m_configuration.put("filter_"+IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE+"_"+(String)pcode.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE+"="+(String)pcode.get(i)+")");
				this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(Editor.NAMESPACE,"filter_"+IJAMConst.ATTRIBUTE_NAME_POSTAL_CODE+"_"+(String)pcode.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_LASTNAME+"="+(String)pcode.get(i)+")");
			}
			for (int i=0;i<countries.size();i++) {
				this.m_configuration.put("filter_"+IJAMConst.ATTRIBUTE_NAME_COUNTRY+"_"+(String)countries.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_COUNTRY+"="+(String)countries.get(i)+")");
				this.getRuntime().getConfigManagerFactory().getConfigManager().setProperty(Editor.NAMESPACE,"filter_"+IJAMConst.ATTRIBUTE_NAME_COUNTRY+"_"+(String)countries.get(i), "(11,"+IJAMConst.ATTRIBUTE_NAME_COUNTRY+"="+(String)countries.get(i)+")");
			}
			this.getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
		}
		if (this.m_data == null)
			this.m_data = this.getRuntime().getCallerFactory()
					.createCallerList();
	}
	
	private boolean hasForbiddenFilterChars(String s) {
		if (s==null || s.length()==0) return false;
		if (s.equalsIgnoreCase("(")) return true;
		if (s.equalsIgnoreCase(")")) return true;
		if (s.equalsIgnoreCase("\\")) return true;
		if (s.equalsIgnoreCase("$")) return true;
		if (s.equalsIgnoreCase("¤")) return true;
		if (s.equalsIgnoreCase("!")) return true;
		if (s.equalsIgnoreCase("&")) return true;
		if (s.equalsIgnoreCase("\"")) return true;
		if (s.equalsIgnoreCase("?")) return true;
		if (s.equalsIgnoreCase("=")) return true;
		if (s.equalsIgnoreCase("#")) return true;
		if (s.equalsIgnoreCase("+")) return true;
		if (s.equalsIgnoreCase("*")) return true;
		if (s.equalsIgnoreCase("@")) return true;
		if (s.equalsIgnoreCase(",")) return true;
		if (s.equalsIgnoreCase(" ")) return true;
		return false;
	}

	private ICallerManager _getRepository() {
		String managerID = this.m_configuration.getProperty(CFG_REPOSITORY, "");
		if (managerID.length() > 0) {
			ICallerManager cm = this.getRuntime().getCallerManagerFactory()
					.getCallerManager(managerID);
			if (cm != null)
				return cm;
		}
		this.m_logger.severe("CallerManager with ID " + managerID
				+ " does not exist.");
		return null;
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	protected int getSortOrder() {
		return Integer.parseInt(this.m_configuration
				.getProperty(CFG_ORDER, "2"));
	}

	protected boolean getSortDirection() {
		return (this.m_configuration.getProperty(CFG_DIRECTION, "false"))
				.equalsIgnoreCase("true");
	}

	protected IFilter getFilter() {
		String fstring = this.m_configuration.getProperty(CFG_FILTER, "");
		IFilter[] f = new EditorFilterManager().getFiltersFromString(fstring);
		return (f != null && f.length > 0 ? f[0] : null);
	}

	protected List getActiveCallerManagers() {
		List l = new ArrayList();
		l.add(this._getRepository().getManagerID());
		return l;
	}

	public void generateElementArray(Object[] data) {
		if (data != null) {
			this.m_data = this.getRuntime().getCallerFactory()
					.createCallerList();

			for (int i = 0; i < data.length; i++) {
				if (data[i] instanceof ICaller) 
					this.m_data.add((ICaller) data[i]);
			}
		}
	}

	public Object getRepository() {
		return this._getRepository();
	}
}
