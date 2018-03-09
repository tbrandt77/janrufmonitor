package de.janrufmonitor.ui.jface.application.fritzbox.rendering;

import java.io.File;

import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.io.PathResolver;

public class TamMessage extends AbstractTableCellRenderer implements IJournalCellRenderer {

	private static String NAMESPACE = "ui.jface.application.fritzbox.rendering.TamMessage";
	
	public Image renderAsImage() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				File tamMessage = new File(new File(PathResolver.getInstance(PIMRuntime.getInstance()).getDataDirectory() + File.separator + "fritzbox-messages"), ((ICall)this.m_o).getUUID()+".wav");
				if (tamMessage.exists() && tamMessage.isFile() && tamMessage.length()>0)
					return SWTImageManager.getInstance(PIMRuntime.getInstance()).getWithoutCache("tam.png");
			}
		}
		return null;
	}
	
	public String renderAsImageID(){
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {File tamMessage = new File(new File(PathResolver.getInstance(PIMRuntime.getInstance()).getDataDirectory() + File.separator + "fritzbox-messages"), ((ICall)this.m_o).getUUID()+".wav");
			if (tamMessage.exists() && tamMessage.isFile() && tamMessage.length()>0)
				return SWTImageManager.getInstance(PIMRuntime.getInstance()).getImagePath("tam.png");
			}
		}
		return "";
	}

	public String getID() {
		return "FritzBoxTamMessage".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isRenderImage() {
		return true;
	}
}
