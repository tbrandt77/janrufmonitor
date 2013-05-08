package de.janrufmonitor.ui.jface.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IMultiPhoneCaller;
import de.janrufmonitor.framework.IPhonenumber;


public class TreeContentProvider implements 
		ITreeContentProvider, IConfigConst {

	class TreeItemCallerData implements IExtendedTreeItemCallerData {

		IAttributeMap m_m;
		IPhonenumber m_pn;
		ICaller m_caller;
		
		public TreeItemCallerData(ICaller c, IAttributeMap m, IPhonenumber pn) {
			m_m = m;
			m_pn = pn;
			m_caller = c;
		}
		
		public IAttributeMap getAttributes() {
			return m_m;
		}

		public IPhonenumber getPhone() {
			return m_pn;
		}

		public ICaller getCaller() {
			return m_caller;
		}
		
	}
	
	protected Properties m_configuration;


	public TreeContentProvider(Properties configuration) {
		this.m_configuration = configuration;
	}

	public Object[] getElements(Object o) {
		if (o instanceof IApplicationController) {
			return ((IApplicationController) o).getElementArray();
		}
		return null;
	}


	public Object[] getChildren(Object o) {
		if (o instanceof IMultiPhoneCaller) {

			List items = new ArrayList();
			List pns = ((IMultiPhoneCaller) o).getPhonenumbers();
			for (int i=0,j=pns.size();i<j;i++) {
				items.add(new TreeItemCallerData(
						((IMultiPhoneCaller) o),
						((IMultiPhoneCaller) o).getAttributes(),
						(IPhonenumber) pns.get(i)));
			}

			if (items.size()>1)
				items.remove(0);
			return items.toArray();
		}
		return null;
	}

	public Object getParent(Object o) {
		return null;
	}

	public boolean hasChildren(Object o) {
		if (o instanceof IMultiPhoneCaller) {
			return ((IMultiPhoneCaller) o).getPhonenumbers().size() > 1;
		}
		return false;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
	}

}