package de.janrufmonitor.ui.jface.application.journal.rendering;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class NotesAuthor extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {

	private static String NAMESPACE = "ui.jface.application.journal.rendering.NotesAuthor";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				IAttribute att = ((ICall)this.m_o).getAttribute(IJAMConst.ATTRIBUTE_NAME_NOTES_AUTHOR);
				if (att!=null)
					return att.getValue();
			}
		}
		return "";
	}
	
	public String getID() {
		return "NotesAuthor".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
}
