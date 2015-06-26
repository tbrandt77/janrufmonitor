package de.janrufmonitor.ui.jface.application.fritzbox.rendering;

import java.util.StringTokenizer;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.util.formatter.Formatter;

public class SpoofingNumber extends AbstractTableCellRenderer implements IJournalCellRenderer {

	private static String NAMESPACE = "ui.jface.application.fritzbox.rendering.SpoofingNumber";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				IPhonenumber pn = this.getSpoofingNumber((ICall) this.m_o);
				if (pn!=null)
					return Formatter.getInstance(PIMRuntime.getInstance()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, pn);
			}
		}
		return "";
	}

	public String getID() {
		return "FritzBoxSpoofingNumber".toLowerCase();
	}
	
	private IPhonenumber getSpoofingNumber(ICall call) {
		IAttribute sp = call.getAttribute("fritzbox.spoofing");
		if (sp!=null) {
			StringTokenizer cs = new StringTokenizer(sp.getValue(), ";");
			if (cs.countTokens()==3) {
				return PIMRuntime.getInstance().getCallerFactory().createPhonenumber(cs.nextToken(), cs.nextToken(), cs.nextToken());
			}
		}
		return null;
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
}
