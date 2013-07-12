package de.janrufmonitor.ui.jface.application;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.filter.AbstractFilterSerializer;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.CharacterFilter;
import de.janrufmonitor.repository.filter.CipFilter;
import de.janrufmonitor.repository.filter.DateFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.ItemCountFilter;
import de.janrufmonitor.repository.filter.MsnFilter;
import de.janrufmonitor.repository.filter.PhonenumberFilter;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public abstract class AbstractFilterManager extends AbstractFilterSerializer implements IFilterManager {

	private IRuntime m_runtime;
	private II18nManager m_i18n;
	private String m_language;
	
	public String getFiltersToLabelText(IFilter[] f, int shorten) {
		if (f != null && f.length > 0) {
			boolean processedCaller = false;
			StringBuffer sb = new StringBuffer();
			sb.append(this.getI18nManager().getString(this.getNamespace(),
					"view_limit", "label", this.getLanguage()));
			for (int i = 0; i < f.length; i++) {
				IFilter f1 = f[i];
				if (f1.getType().equals(FilterType.CIP)) {
					sb.append(this.getI18nManager().getString(
							this.getNamespace(), "view_cip", "label",
							this.getLanguage()));
					sb.append(this.getRuntime().getCipManager().getCipLabel(
							((CipFilter) f1).getCip(), this.getLanguage()));
					if ((i + 1) < f.length)
						sb.append(", ");
				}
				if (f1.getType().equals(FilterType.MSN)) {
					sb.append(this.getI18nManager().getString(
							this.getNamespace(), "view_msn", "label",
							this.getLanguage()));
					IMsn[] m = ((MsnFilter) f1).getMsn();
					for (int k = 0, l = m.length; k<l; k++) {
						sb.append(m[k].getMSN());
						m[k].setAdditional(this.getRuntime().getMsnManager()
								.getMsnLabel(m[k]));
						if (m[k].getAdditional().length() > 0) {
							sb.append(" (" + m[k].getAdditional() + ")");
						}
						if ((k + 1) < l)
							sb.append(", ");
					}
					
					if ((i + 1) < f.length)
						sb.append(", ");
				}
				if (f1.getType().equals(FilterType.DATE)) {
					sb.append(this.getI18nManager().getString(
							this.getNamespace(), "view_date", "label",
							this.getLanguage()));
					
					if (((DateFilter) f1).getTimeframe()==-1) {
						Date d1 = ((DateFilter) f1).getDateFrom();
						Date d2 = ((DateFilter) f1).getDateTo();
						
						SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
						sb.append("[");
						sb.append(sdf.format(d2));
						sb.append(" - ");
						if (d1!=null)
							sb.append(sdf.format(d1));
						else {
							sb.append(
								this.getI18nManager().getString(
									this.getNamespace(), "today", "label",
									this.getLanguage())	
							);
						}
						sb.append("]");
						if ((i + 1) < f.length)
							sb.append(", ");
					} else {
						sb.append("[");
						sb.append(this.getI18nManager().getString(
								this.getNamespace(), Long.toString(((DateFilter) f1).getTimeframe()), "label",
								this.getLanguage()));
						sb.append("]");
						if ((i + 1) < f.length)
							sb.append(", ");						
					}
				}
				if (f1.getType().equals(FilterType.CALLER) && !processedCaller) {
					sb.append(this.getI18nManager().getString(
							this.getNamespace(), "view_caller", "label",
							this.getLanguage()));
					
					if ((i + 1) < f.length && !processedCaller)
						sb.append(", ");
					
					processedCaller = true;
				}
				if (f1.getType().equals(FilterType.PHONENUMBER)) {
					PhonenumberFilter cf = ((PhonenumberFilter) f1);
					
					IPhonenumber pnc = getRuntime().getCallerFactory().createPhonenumber(
							cf.getPhonenumber().getIntAreaCode(), cf.getPhonenumber().getAreaCode(), "0000000000");
					
					ICaller c =Identifier.identifyDefault(getRuntime(), pnc);
					if (c!=null) {
						IAttribute city = c.getAttribute(IJAMConst.ATTRIBUTE_NAME_CITY);
						if (city!=null && city.getValue().trim().length()>0) {
							sb.append(this.getI18nManager().getString(
									this.getNamespace(), "view_area", "label",
									this.getLanguage()).substring(2));
							sb.append(city.getValue().trim());
							sb.append(" (0");
							sb.append(cf.getPhonenumber().getAreaCode());
							sb.append(")");
							if ((i + 1) < f.length)
								sb.append(", ");
							continue;
						}
					} 
					
					sb.append(this.getI18nManager().getString(
							this.getNamespace(), "view_intarea", "label",
							this.getLanguage()));
					sb.append("00"+cf.getPhonenumber().getIntAreaCode());
					if (cf.getPhonenumber().getAreaCode().trim().length()>0) {
						sb.append(this.getI18nManager().getString(
								this.getNamespace(), "view_area", "label",
								this.getLanguage()));
						sb.append("0"+cf.getPhonenumber().getAreaCode().trim());

					}
					if ((i + 1) < f.length)
						sb.append(", ");
				}	
				if (f1.getType().equals(FilterType.ITEMCOUNT)) {
					ItemCountFilter cf = ((ItemCountFilter) f1);
					sb.append(this.getI18nManager().getString(
							this.getNamespace(), "view_itemcount", "label",
							this.getLanguage()));
					sb.append(cf.getLimit());
					if ((i + 1) < f.length)
						sb.append(", ");
				}		
				if (f1.getType().equals(FilterType.ATTRIBUTE)) {
					AttributeFilter cf = ((AttributeFilter) f1);
					IAttributeMap m = cf.getAttributeMap();
					if (m!=null && m.size()>0) {
						Iterator it = m.iterator();
						IAttribute a = null;
						while(it.hasNext()) {
							a = (IAttribute) it.next();							
							if (a.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS)) {
								sb.append(this.getI18nManager().getString(
										this.getNamespace(), a.getValue(), "label",
										this.getLanguage()));
							} else if (a.getName().equalsIgnoreCase(IJAMConst.ATTRIBUTE_NAME_CATEGORY)) {
								sb.append(this.getI18nManager().getString(
										this.getNamespace(), a.getName(), "label",
										this.getLanguage()));
								sb.append(a.getValue());
							} else {
								sb.append(this.getI18nManager().getString(
										this.getNamespace(), a.getName(), "label",
										this.getLanguage()));
							}
							if (it.hasNext())
								sb.append(", ");
						
						}
					}
					if ((i + 1) < f.length)
						sb.append(", ");
				}
				if (f1.getType().equals(FilterType.CHARACTER)) {
					CharacterFilter cf = ((CharacterFilter) f1);
					sb.append(this.getI18nManager().getString(
							this.getNamespace(), "view_"+cf.getAttributeName(), "label",
							this.getLanguage()));
					sb.append("[");
					sb.append(cf.getCharacter().toUpperCase());
					sb.append("...]");
					
					if ((i + 1) < f.length)
						sb.append(", ");
				}				
				
			}

			String s = sb.toString();
			if (s.trim().endsWith(","))
				s = s.trim().substring(0, s.trim().length()-1);
			
			if (shorten>-1 && shorten<s.length()) {
					return s.substring(0, Math.min(shorten, s.length())) + "...";
			}
			if (s.trim().length()>0)
				return s;
		}
		return this.getI18nManager().getString(this.getNamespace(), "view_all",
				"label", this.getLanguage());
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}
	
	private II18nManager getI18nManager() {
		if (this.m_i18n==null) {
			this.m_i18n = this.getRuntime().getI18nManagerFactory().getI18nManager();
		}
		return this.m_i18n;
	}
	
	private String getLanguage() {
		if (this.m_language==null) {
			this.m_language = 
				this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
					IJAMConst.GLOBAL_NAMESPACE,
					IJAMConst.GLOBAL_LANGUAGE
				);
		}
		return this.m_language;
	}
	
	public abstract String getNamespace();
}
