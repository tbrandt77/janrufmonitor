package de.janrufmonitor.ui.jface.application;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;

import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;

public class ImageCellLabelProvider extends org.eclipse.jface.viewers.OwnerDrawLabelProvider implements IConfigConst {

	protected String m_renderer;
	
	public ImageCellLabelProvider(String rendererID) {
		this.m_renderer = rendererID;
	}
	
	protected void measure(Event e, Object o) {
		Image img = getImage(o);
		if (img==null) return;
		e.height = img.getBounds().height;
	}

	protected void paint(Event event, Object element) {
		Image img = getImage(element);
		if (img==null) return;
	    Rectangle bounds = ((TableItem) event.item).getBounds(event.index);
	    Rectangle imgBounds = img.getBounds();
	    bounds.width /= 2;     
	    bounds.width -= imgBounds.width / 2;
	    bounds.height /= 2;
	    bounds.height -= imgBounds.height / 2;
	    int x = (bounds.width > 0 ? bounds.x + bounds.width : bounds.x);
	    int y = bounds.height > 0 ? bounds.y + bounds.height : bounds.y;
	    event.gc.drawImage(img, x, y);
	}

	
	private Image getImage(Object o) {
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
