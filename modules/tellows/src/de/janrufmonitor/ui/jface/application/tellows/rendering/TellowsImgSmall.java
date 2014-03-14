package de.janrufmonitor.ui.jface.application.tellows.rendering;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class TellowsImgSmall extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {

	private static String NAMESPACE = "ui.jface.application.tellows.rendering.TellowsImgSmall";

	public Image renderAsImage() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				if (((ICaller)this.m_o).getAttribute("tellows.scorePath")!=null) {
					IAttribute timg = ((ICaller)this.m_o).getAttribute("tellows.scorePath");
					if (timg.getValue().length()>0) {
						
						try {
							URL url = new URL(timg.getValue());
							String tellowsImgName = new File(url.getFile()).getName();
							File tellowsImgDir = new File(PathResolver.getInstance().getImageDirectory(), "tellows");
							if (!tellowsImgDir.exists()) tellowsImgDir.mkdirs();
							File tellowsImg = new File(tellowsImgDir, tellowsImgName);
							if (!tellowsImg.exists()) {
								URLConnection con = url.openConnection();
								con.connect();
								Object o = url.openStream();
								if (o instanceof InputStream) {
									Stream.copy(new BufferedInputStream((InputStream)o), new FileOutputStream(tellowsImg), true);
								}
							}
							if (tellowsImg.exists()) {
								return this.getTellowsImage(new FileInputStream(tellowsImg));	
							}
						} catch (MalformedURLException e) {
						} catch (IOException e) {
						}
					}
				}
			}
		}
		return null;
	}
	
	private Image getTellowsImage(InputStream in) {
		if (in!=null) {
			try {
				ImageData id = new ImageData(in);

				// calculate proportions
				if (id.height>id.width) {
					float height = ((float)id.height / (float)id.width) * 60;
					id = id.scaledTo(60, Math.max((int) height, 60));
				} else {
					float width = ((float)id.width / (float)id.height) * 60;
					id = id.scaledTo(Math.max((int) width, 60), 60);
				}
				
				
				in.close();
				return new Image(DisplayManager.getDefaultDisplay(), id);
			} catch (SWTException e) {
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
		return null;
	}

	public String getID() {
		return "TellowsImgSmall".toLowerCase();
	}
	
	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isRenderImage() {
		return true;
	}

}
