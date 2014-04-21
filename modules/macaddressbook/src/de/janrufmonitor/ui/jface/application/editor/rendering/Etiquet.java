package de.janrufmonitor.ui.jface.application.editor.rendering;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.mapping.IMacAddressBookNumberMapping;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.ITreeItemCallerData;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class Etiquet extends AbstractTableCellRenderer implements
		IEditorCellRenderer, IJournalCellRenderer {
	
	class TreeItemCallerData implements ITreeItemCallerData {

		IAttributeMap m_m;
		IPhonenumber m_pn;
		
		public TreeItemCallerData(IAttributeMap m, IPhonenumber pn) {
			m_m = m;
			m_pn = pn;
		}
		
		public IAttributeMap getAttributes() {
			return m_m;
		}

		public IPhonenumber getPhone() {
			return m_pn;
		}
		
	}
	
	private static String NAMESPACE = "ui.jface.application.editor.rendering.Etiquet";

	private String m_language;
	
	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				this.m_o = new TreeItemCallerData(((ICaller)this.m_o).getAttributes(), ((ICaller)this.m_o).getPhoneNumber());
			}	
	
			if (this.m_o instanceof ITreeItemCallerData) {
				IAttributeMap m = ((ITreeItemCallerData)m_o).getAttributes();
				IPhonenumber pn = ((ITreeItemCallerData)m_o).getPhone();

				if (m.contains(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber())) {
					IAttribute type = m.get(IMacAddressBookNumberMapping.MAPPING_ATTTRIBUTE_ID+pn.getTelephoneNumber());
					if (type!=null) {
						return PIMRuntime.getInstance().getI18nManagerFactory().getI18nManager().getString(NAMESPACE, type.getValue(), "label", getLanguage());
					}
				}
			}					
		}
		return "";
	}
	
	private String getLanguage() {
		if (this.m_language==null) {
			this.m_language = 
				PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(
					IJAMConst.GLOBAL_NAMESPACE,
					IJAMConst.GLOBAL_LANGUAGE
				);
		}
		return this.m_language;
	}
	
	public String getID() {
		return "Etiquet";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

}
