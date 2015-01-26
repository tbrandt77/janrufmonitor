package de.janrufmonitor.ui.swt;

import java.io.File;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.util.io.PathResolver;

public class SWTImageManager {

	private static SWTImageManager m_instance;
	
	private ImageRegistry m_ir;
	private IRuntime m_runtime;
	private Logger m_logger;
	
	private SWTImageManager(IRuntime r) {
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		this.m_runtime = r;
		this.m_ir = new ImageRegistry(DisplayManager.getDefaultDisplay());
		this.init();
	}
	
	public static SWTImageManager getInstance(IRuntime r) {
		if (m_instance == null)
				m_instance = new SWTImageManager(r);
		return m_instance;
	}
	
	public Image get(String key) {
		Image img = this.m_ir.get(key);
		if (img==null) {
			img = this.getImage(key);
			if (img!=null) {
				this.m_ir.put(key, img);
			}
		}
		return img;
	}
	
	public Image getWithoutCache(String key) {
		return this.getImage(key);
	}
	
	public void remove(String key) {
		this.m_ir.remove(key);
	}
	
	private void init() {
		// add all standard images
		try {
			Image img = null;
			img = this.getImage(IJAMConst.IMAGE_KEY_TELEFON_JPG);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_TELEFON_JPG, img);
			
			img = this.getImage(IJAMConst.IMAGE_KEY_EMPTYCALLER_JPG);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_EMPTYCALLER_JPG, img);
			
			img = this.getImage(IJAMConst.IMAGE_KEY_PIM_JPG);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_PIM_JPG, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_PIM_ICON, 16, 16);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_PIM_ICON, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_PIMX_ICON, 16, 16);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_PIMX_ICON, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_ACCEPTED_GIF, 16, 16);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_ACCEPTED_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_CALL_IN_ACTIVE_PNG, 24, 24);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_CALL_IN_ACTIVE_PNG, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_CALL_OUT_ACTIVE_PNG, 24, 24);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_CALL_OUT_ACTIVE_PNG, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_CALL_END_5MIN_PNG, 24, 24);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_CALL_END_5MIN_PNG, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_REJECTED_GIF, 16, 16);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_REJECTED_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_AWAY_GIF, 16, 16);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_AWAY_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_OUTGOING_GIF, 16, 16);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_OUTGOING_GIF, img);
		
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_ADOWN_GIF, 12, 12);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_ADOWN_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_FILTER_GIF, 16, 13);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_FILTER_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_EXPORT_GIF, 14, 14);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_EXPORT_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_DELETE_GIF, 14, 14);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_DELETE_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_CLP_GIF, 14, 14);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_CLP_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_ZIN_GIF, 14, 14);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_ZIN_GIF, img);

			img = this.getResizedImage(IJAMConst.IMAGE_KEY_ZOUT_GIF, 14, 14);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_ZOUT_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_REFRESH_GIF, 14, 11);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_REFRESH_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_REP_GIF, 18, 13);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_REP_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_AUP_GIF, 12, 12);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_AUP_GIF, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_B1_JPG, 12, 12);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_B1_JPG, img);
			
			img = this.getResizedImage(IJAMConst.IMAGE_KEY_B2_JPG, 12, 12);
			if (img!=null)
				this.m_ir.put(IJAMConst.IMAGE_KEY_B2_JPG, img);		
		} catch (Exception ex) {
			this.m_logger.severe(ex.toString() + ", "+ex.getMessage());
		}
	}
	
	private Image getResizedImage(String key, int x, int y) {
		Image img = this.getImage(key);
		if (img!=null) {
			ImageData id = img.getImageData().scaledTo(x, y);
			return new Image(DisplayManager.getDefaultDisplay(), id);
		}
		return null;
	}
	
	private Image getImage(String key) {
		File image = new File(PathResolver.getInstance(this.m_runtime).getImageDirectory()
				+ key);
		if (!image.exists()) {
			return null;
		}
		ImageData id = new Image(DisplayManager.getDefaultDisplay(), image.getAbsolutePath()).getImageData();
		return new Image(DisplayManager.getDefaultDisplay(), id);
	}
	
	public String getImagePath(String key) {
		return PathResolver.getInstance(this.m_runtime).getImageDirectory() + key;
	}
	
	public void loadImage(String key, int x, int y) {
		Image img = null;
		if (x==-1 && y==-1) {
			img = this.getImage(key);
			if (img!=null)
				this.m_ir.put(key, img);	
		} else {
			img = this.getResizedImage(key, x, y);
			if (img!=null)
				this.m_ir.put(key, img);	
		}
	}
	
}
