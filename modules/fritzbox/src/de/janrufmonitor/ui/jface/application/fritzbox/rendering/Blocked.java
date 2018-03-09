package de.janrufmonitor.ui.jface.application.fritzbox.rendering;

import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.fritzbox.FritzBoxBlockedListManager;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.ITreeItemCallerData;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class Blocked extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {

	private static String NAMESPACE = "ui.jface.application.fritzbox.rendering.Blocked";
	
	public Image renderAsImage() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				this.m_o = ((ICaller)this.m_o).getPhoneNumber();
			}
			if (this.m_o instanceof ITreeItemCallerData) {
				this.m_o = ((ITreeItemCallerData)this.m_o).getPhone();
			}		
			if (this.m_o instanceof IPhonenumber) {
				if (((IPhonenumber)this.m_o).isClired()) return null;
				
				String dial = ((IPhonenumber)this.m_o).getTelephoneNumber();
				if (!((IPhonenumber)this.m_o).getIntAreaCode().equalsIgnoreCase(PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA))) {
					dial = "00" + ((IPhonenumber)this.m_o).getIntAreaCode() + dial;
				}
				if (!dial.startsWith("0")) dial = "0"+dial;
				
				if (FritzBoxBlockedListManager.getInstance().isBlocked(dial))
					return SWTImageManager.getInstance(PIMRuntime.getInstance()).getWithoutCache("blocked.gif");
			}
		}
		return null;
	}
	
	public String renderAsImageID(){
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				this.m_o = ((ICaller)this.m_o).getPhoneNumber();
			}
			if (this.m_o instanceof ITreeItemCallerData) {
				this.m_o = ((ITreeItemCallerData)this.m_o).getPhone();
			}	
			if (this.m_o instanceof IPhonenumber) {
				if (((IPhonenumber)this.m_o).isClired()) return null;
				
				String dial = ((IPhonenumber)this.m_o).getTelephoneNumber();
				if (!((IPhonenumber)this.m_o).getIntAreaCode().equalsIgnoreCase(PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_INTAREA))) {
					dial = "00" + ((IPhonenumber)this.m_o).getIntAreaCode() + dial;
				}
				if (!dial.startsWith("0")) dial = "0"+dial;
				
				if (FritzBoxBlockedListManager.getInstance().isBlocked(dial))
					return SWTImageManager.getInstance(PIMRuntime.getInstance()).getImagePath("blocked.gif");
			}
		}
		return "";
	}

	public String getID() {
		return "FritzBoxBlocked".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isRenderImage() {
		return true;
	}
}
