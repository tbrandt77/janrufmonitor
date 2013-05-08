package de.janrufmonitor.ui.jface.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TableContentProvider implements IStructuredContentProvider, IConfigConst {
	
		protected Properties m_configuration;
		protected Map m_rendererMapping; 
		
		public TableContentProvider(Properties configuration) {
			this.m_configuration = configuration;
			this.m_rendererMapping = new HashMap();
		}
		
		public Object[] getElements(Object o) {
			if (o instanceof IApplicationController) { 
				return ((IApplicationController)o).getElementArray();
			}
			return null;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}
		
}