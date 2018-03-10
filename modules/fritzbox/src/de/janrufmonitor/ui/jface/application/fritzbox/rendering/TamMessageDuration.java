package de.janrufmonitor.ui.jface.application.fritzbox.rendering;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class TamMessageDuration extends AbstractTableCellRenderer implements IJournalCellRenderer {

	private static String NAMESPACE = "ui.jface.application.fritzbox.rendering.TamMessageDuration";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				return this.getDuration((ICall)this.m_o);
			}
		}
		return "";
	}

	public String getID() {
		return "FritzBoxTamDuration".toLowerCase();
	}
	
	private String getDuration(ICall call) {
		IAttribute ring = call.getAttribute("fritzbox.tamduration");
		if (ring!=null) {
			return ring.getValue();
		}

		return "";
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
}
