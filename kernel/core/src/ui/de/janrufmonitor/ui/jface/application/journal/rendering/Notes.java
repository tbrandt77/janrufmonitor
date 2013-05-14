package de.janrufmonitor.ui.jface.application.journal.rendering;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellEditorRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class Notes extends AbstractTableCellEditorRenderer implements IJournalCellRenderer, IEditorCellRenderer {

	private static String NAMESPACE = "ui.jface.application.journal.rendering.Notes";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				IAttribute att = ((ICall)this.m_o).getAttribute(IJAMConst.ATTRIBUTE_NAME_NOTES);
				if (att!=null) {
					return att.getValue();
				}
			}
			if (this.m_o instanceof ICaller) {
				IAttribute att = ((ICaller)this.m_o).getAttribute(IJAMConst.ATTRIBUTE_NAME_NOTES);
				if (att!=null) {
					return att.getValue();
				}
			}
		}
		return "";
	}
	
	public String getID() {
		return "Notes".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public IAttribute getAttribute() {
		return PIMRuntime.getInstance().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NOTES, "");
	}

	public void applyAttributeChanges(Object o, IAttribute att, Object value) {
		if (o instanceof ICall) {
			if (((String)value).length()>0) {
				att.setValue((String)value);
				((ICall)o).setAttribute(att);
				((ICall)o).setAttribute(PIMRuntime.getInstance().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NOTES_AUTHOR, System.getProperty("user.name", "")));
			} else {
				((ICall)o).getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_NOTES);
				((ICall)o).getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_NOTES_AUTHOR);
			}
		}	
		if (o instanceof ICaller) {
			if (((String)value).length()>0) {
				att.setValue((String)value);
				((ICaller)o).setAttribute(att);
				((ICaller)o).setAttribute(PIMRuntime.getInstance().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_NOTES_AUTHOR, System.getProperty("user.name", "")));
			} else {
				((ICaller)o).getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_NOTES);
				((ICaller)o).getAttributes().remove(IJAMConst.ATTRIBUTE_NAME_NOTES_AUTHOR);
			}
		}	
	}
}
