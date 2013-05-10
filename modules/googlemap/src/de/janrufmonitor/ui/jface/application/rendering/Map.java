package de.janrufmonitor.ui.jface.application.rendering;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.swt.graphics.Image;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class Map extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {

	private static String NAMESPACE = "ui.jface.application.rendering.Map";
	
	public Image renderAsImage() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				IAttribute lng = ((ICaller)this.m_o).getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LNG);
				IAttribute lat = ((ICaller)this.m_o).getAttribute(IJAMConst.ATTRIBUTE_NAME_GEO_LAT);
				if (lng!=null && lat!=null) {
					
					String maptype = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty("service.GoogleMaps", "type");
					String zoom = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty("service.GoogleMaps", "zoom"); 
					
					try {
						
						File dir = new File(PathResolver.getInstance(PIMRuntime.getInstance()).getPhotoDirectory()+File.separator+"maps"+File.separator);
						if (!dir.exists()) dir.mkdirs();
						
						File img = new File(dir, lat.getValue()+lng.getValue()+maptype+zoom);
						if (!img.exists()) {
							URL url = new URL("http://maps.googleapis.com/maps/api/staticmap?center="+lat.getValue()+","+lng.getValue()+"&zoom="+zoom+"&size=400x90&maptype="+maptype+"&sensor=false&markers=color:blue%7Clabel:A%7C"+lat.getValue()+","+lng.getValue());
							URLConnection c = url.openConnection();
	
							c.setDoInput(true);
							c.setRequestProperty(
								"User-Agent",
								"Mozilla/4.0 (compatible; MSIE; Windows NT)");
							c.connect();
	
							Object o = url.openStream();
							if (o instanceof InputStream) {
								BufferedInputStream bin = new BufferedInputStream((InputStream) o);
								FileOutputStream fos = new FileOutputStream(img);
								Stream.copy(bin, fos, true);
								return new Image(DisplayManager.getDefaultDisplay(), img.getAbsolutePath());
							}
							return null;
						}
						return new Image(DisplayManager.getDefaultDisplay(), img.getAbsolutePath());
					} catch (MalformedURLException e) {
					} catch (IOException e) {
					}
				}
			}
		}
		return null;
	}

	public String getID() {
		return "Map".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public boolean isRenderImage() {
		return true;
	}
}
