package de.janrufmonitor.ui.jface.application.journal.rendering;

import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class CallActive extends AbstractTableCellRenderer implements IJournalCellRenderer {

	private static String NAMESPACE = "ui.jface.application.journal.rendering.CallActive";
	
	public Image renderAsImage() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				ICall call = (ICall)this.m_o;
				IAttribute isCallActive = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_ACTIVE_INDICATOR);
				if (isCallActive!=null && isCallActive.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_YES)) {
					IAttribute att = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
					if (att != null && att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_ACCEPTED)) {
						return SWTImageManager.getInstance(PIMRuntime.getInstance()).get(IJAMConst.IMAGE_KEY_CALL_IN_ACTIVE_PNG);
					}
					if (att != null && att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
						return SWTImageManager.getInstance(PIMRuntime.getInstance()).get(IJAMConst.IMAGE_KEY_CALL_OUT_ACTIVE_PNG);
					}	
				}
				if (isCallActive!=null && isCallActive.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_NO)) {
					IAttribute att = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALL_DURATION);
					if (att!=null && att.getValue().length()>0) {
						try {
							long duration_in_sec = Long.parseLong(att.getValue());
							long start = call.getDate().getTime() / 1000;
							long now = new java.util.Date().getTime() / 1000;
							long passedTime = now - (start + duration_in_sec);
							if (passedTime<300) {
								return SWTImageManager.getInstance(PIMRuntime.getInstance()).get(IJAMConst.IMAGE_KEY_CALL_END_5MIN_PNG);
							}
						} catch (Exception e) { }
					}
				}
			}
		}
		return null;
	}
	
	public String renderAsImageID(){
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				ICall call = (ICall)this.m_o;
				
				IAttribute att = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
				if (att != null && att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_REJECTED)) {          		
					return IJAMConst.IMAGE_KEY_REJECTED_GIF;
				}
				//att = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
				if (att != null && att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_ACCEPTED)) {
					return IJAMConst.IMAGE_KEY_ACCEPTED_GIF;
				}
				//att = call.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
				if (att != null && att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
					return IJAMConst.IMAGE_KEY_OUTGOING_GIF;
				}				
				if (att != null && att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_MISSED)) {
					return IJAMConst.IMAGE_KEY_AWAY_GIF;
				}

			}
		}
		return "";
	}

	public String getID() {
		return "CallActive".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isRenderImage() {
		return true;
	}
}
