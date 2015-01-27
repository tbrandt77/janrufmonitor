package de.janrufmonitor.ui.jface.application.journal.rendering;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class CallDuration extends AbstractTableCellRenderer implements IJournalCellRenderer {

	private static String NAMESPACE = "ui.jface.application.journal.rendering.CallDuration";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				return this.getDuration((ICall)this.m_o);
			}
		}
		return "";
	}

	public String getID() {
		return "CallDuration".toLowerCase();
	}
	
	private String getDuration(ICall call) {
		IAttribute ring = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_DURATION);
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
