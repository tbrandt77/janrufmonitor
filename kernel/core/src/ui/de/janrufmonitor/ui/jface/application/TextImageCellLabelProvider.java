package de.janrufmonitor.ui.jface.application;

import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;

public class TextImageCellLabelProvider extends TextCellLabelProvider {


	public TextImageCellLabelProvider(String rendererID) {
		super(rendererID);
	}
	
	@Override
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


}
