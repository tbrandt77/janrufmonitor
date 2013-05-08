package de.janrufmonitor.ui.jface.application;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;

public class ImageTextCellLabelProvider extends ImageCellLabelProvider implements ILabelProvider{

	public ImageTextCellLabelProvider(String rendererID) {
		super(rendererID);
	}

	public Image getImage(Object o) {
		ITableCellRenderer r = RendererRegistry.getInstance().getRenderer(
			this.m_renderer
		);
		if (r!=null) {
			r.updateData(o);
			return r.renderAsImage();
		}
		return null;
	}

	public String getText(Object o) {
		ITableCellRenderer r = RendererRegistry.getInstance().getRenderer(
			this.m_renderer
		);
		if (r!=null) {
			r.updateData(o);
			return r.renderAsText();
		}
		return "";
	}

}
