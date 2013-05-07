package de.janrufmonitor.ui.jface.application.rendering;

import java.io.File;

import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.ITreeItemCallerData;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class Flag extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {

	private static String NAMESPACE = "ui.jface.application.rendering.Flag";
	
	public Image renderAsImage() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller().getPhoneNumber();
			}
			if (this.m_o instanceof ICaller) {
				this.m_o = ((ICaller)this.m_o).getPhoneNumber();
			}
			if (this.m_o instanceof ITreeItemCallerData) {
				this.m_o = ((ITreeItemCallerData) this.m_o).getPhone();
			}
			if (this.m_o instanceof IPhonenumber) {
				if (!((IPhonenumber)this.m_o).isClired()) {
					return SWTImageManager.getInstance(PIMRuntime.getInstance()).get("flags"+File.separator+((IPhonenumber)this.m_o).getIntAreaCode()+".png");	
				}
			}
		}
		return null;
	}
	
	public String renderAsImageID(){
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller().getPhoneNumber();
			}
			if (this.m_o instanceof ITreeItemCallerData) {
				this.m_o = ((ITreeItemCallerData) this.m_o).getPhone();
			}
			if (this.m_o instanceof IPhonenumber) {
				if (!((IPhonenumber)this.m_o).isClired()) {
					return "flags"+File.separator+((IPhonenumber)this.m_o).getIntAreaCode()+".png";	
				}
			}
		}
		return "";
	}

	public String getID() {
		return "Flag".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isRenderImage() {
		return true;
	}
}
