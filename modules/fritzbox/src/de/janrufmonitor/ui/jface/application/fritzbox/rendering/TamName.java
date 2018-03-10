package de.janrufmonitor.ui.jface.application.fritzbox.rendering;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class TamName extends AbstractTableCellRenderer implements IJournalCellRenderer {

	private static String NAMESPACE = "ui.jface.application.fritzbox.rendering.TamName";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				return this.getDuration((ICall)this.m_o);
			}
		}
		return "";
	}

	public String getID() {
		return "FritzBoxTamName".toLowerCase();
	}
	
	private String getDuration(ICall call) {
		IAttribute tn = call.getAttribute("fritzbox.tam");
		if (tn!=null) {
			return tn.getValue();
		}

		return "";
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
}
