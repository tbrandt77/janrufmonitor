package de.janrufmonitor.ui.jface.application.editor.rendering;

import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class CallerManagerLogo extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {

	private static String NAMESPACE = "ui.jface.application.editor.rendering.CallerManagerLogo";
	
	public Image renderAsImage() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				IAttribute att = ((ICaller)this.m_o).getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER);
				if (att != null) {
					return SWTImageManager.getInstance(PIMRuntime.getInstance()).get(att.getValue()+".png");	
				}
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
				IAttribute att = ((ICaller)this.m_o).getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLERMANAGER);
				if (att != null) {
					return att.getValue()+".png";	
				}
			}
		}
		return "";
	}

	public String getID() {
		return "CallerManagerLogo".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isRenderImage() {
		return true;
	}
}
