package de.janrufmonitor.ui.jface.application;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Item;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.ICallerManager;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.repository.types.IWriteCallerRepository;
import de.janrufmonitor.ui.jface.application.rendering.ITableAttributeCellEditorRenderer;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellEditorRenderer;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;

public class TableCellModifier implements ICellModifier {
	
	private Viewer m_v;
	private IApplication m_ac;
	private boolean m_isReadOnly = false;
	private boolean m_ROchecked = false;
	
	public TableCellModifier(Viewer v, IApplication ac) {
		m_v = v;
		m_ac = ac;
	}
	
	private void checkReadOnly() {
		Object rm = m_ac.getController().getRepository();
		if (rm instanceof ICallManager) {
			m_isReadOnly = !(rm!=null && ((ICallManager)rm).isActive() && ((ICallManager)rm).isSupported(IWriteCallRepository.class));
		}
		if (rm instanceof ICallerManager) {
			m_isReadOnly = !(rm!=null && ((ICallerManager)rm).isActive() && ((ICallerManager)rm).isSupported(IWriteCallerRepository.class));
		}
		m_ROchecked = true;
	}
	
	public boolean canModify(Object o, String column) {
		if (!m_ROchecked) checkReadOnly();
		
		if (!(o instanceof ICall) && !(o instanceof ICaller)) return false;
		
		if (this.m_isReadOnly) return false;
		ITableCellRenderer r = RendererRegistry.getInstance().getRenderer(
				column
			);
		if (r instanceof ITableCellEditorRenderer) return ((ITableCellEditorRenderer)r).isEditable();
		return false;
	}

	public Object getValue(Object o, String column) {
		ITableCellRenderer r = RendererRegistry.getInstance().getRenderer(
			column
		);
		if (r!=null) {
			r.updateData(o);
			if (r instanceof ITableCellEditorRenderer) {
				switch (((ITableCellEditorRenderer)r).getType()) {
					case 1: {
						Integer i = Integer.valueOf(0);
						try {
							i = Integer.valueOf(r.renderAsText());
						} catch(Exception e) {}						
						return i;
					}
					case 2: {
						Boolean i = Boolean.FALSE;
						try {
							i = Boolean.valueOf(r.renderAsText());
						} catch(Exception e) {}						
						return i;
					}
					default:
				}
			}
			
			return r.renderAsText();
		}
		return "";
	}

	public void modify(Object element, String column, Object value) {
		 if (element instanceof Item)
		      element = ((Item) element).getData();
		
		 
		 ITableCellRenderer r = RendererRegistry.getInstance().getRenderer(
					column
				);
		 if (r instanceof ITableAttributeCellEditorRenderer && ((ITableAttributeCellEditorRenderer)r).getAttribute()!=null) {
			 if (element instanceof ICall) {
				 ICall c = ((ICall)element);
				 IAttribute cAtt = c.getAttribute(((ITableAttributeCellEditorRenderer)r).getAttribute().getName());
				 if (cAtt!=null && value instanceof String) {
					 if (cAtt.getValue().equalsIgnoreCase((String)value)) {
						 return;
					 }
				 }
				 IAttribute att = ((ITableAttributeCellEditorRenderer)r).getAttribute();
				 
				 ((ITableAttributeCellEditorRenderer)r).applyAttributeChanges(c, att, value);
							 
				 if (this.m_ac.getController() instanceof IExtendedApplicationController) {
					 ((IExtendedApplicationController)this.m_ac.getController()).updateElement(c, false);
				 } else {
					 this.m_ac.getController().updateElement(c);
				 }
			 }
			 if (element instanceof ICaller) {
				 ICaller c = ((ICaller)element);
				 IAttribute cAtt = c.getAttribute(((ITableAttributeCellEditorRenderer)r).getAttribute().getName());
				 if (cAtt!=null && value instanceof String) {
					 if (cAtt.getValue().equalsIgnoreCase((String)value)) {
						 return;
					 }
				 }
				 IAttribute att = ((ITableAttributeCellEditorRenderer)r).getAttribute();
				 
				 ((ITableAttributeCellEditorRenderer)r).applyAttributeChanges(c, att, value);
							 
				 if (this.m_ac.getController() instanceof IExtendedApplicationController) {
					 ((IExtendedApplicationController)this.m_ac.getController()).updateElement(c, false);
				 } else {
					 this.m_ac.getController().updateElement(c);
				 }
			 }
		 }
		 
		this.m_v.refresh();
	}

}
