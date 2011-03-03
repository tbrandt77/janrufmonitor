package de.janrufmonitor.ui.jface.application.fritzbox.rendering;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class Duration extends AbstractTableCellRenderer implements IJournalCellRenderer {

	private static String NAMESPACE = "ui.jface.application.fritzbox.rendering.Duration";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				return this.getDuration((ICall)this.m_o);
			}
		}
		return "";
	}

	public String getID() {
		return "FritzBoxDuration".toLowerCase();
	}
	
	private String getDuration(ICall call) {
		IAttribute ring = call.getAttribute("fritzbox.duration");
		if (ring!=null) {
			try {
				int duration = Integer.parseInt(ring.getValue()) / 60;
				StringBuffer sb = new StringBuffer(64);
				if ((duration / 60)>0) {
					sb.append((duration / 60));
					sb.append(" h ");
				}
				sb.append((duration % 60));
				sb.append(" min ");
				return sb.toString();
			} catch (Exception e) {
			}
		}

		return "";
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
}
