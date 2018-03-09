package de.janrufmonitor.ui.jface.application.fritzbox.action;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.ApplicationImageDescriptor;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.io.PathResolver;

public class TamMessagePlay extends AbstractAction implements FritzBoxConst {

	private static String NAMESPACE = "ui.jface.application.fritzbox.action.TamMessagePlay";
	
	private IRuntime m_runtime;

	public TamMessagePlay() {
		super();
		this.setText(
			this.getI18nManager().getString(
				this.getNamespace(),
				"title",
				"label",
				this.getLanguage()
			)
		);	
		this.setImageDescriptor(new ApplicationImageDescriptor(
				SWTImageManager.getInstance(this.getRuntime()).getImagePath("tam.png")
			));
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "fritzbox_tamplay";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		Viewer v = this.m_app.getApplication().getViewer();
		if (v!=null) {
			IStructuredSelection selection = (IStructuredSelection) v.getSelection();
			if (!selection.isEmpty()) {
				Object o = selection.getFirstElement();
				if (o instanceof ICall) {
					File tamMessage = new File(new File(PathResolver.getInstance(PIMRuntime.getInstance()).getDataDirectory() + File.separator + "fritzbox-messages"), ((ICall)o).getUUID()+".wav");
					if (tamMessage.exists() && tamMessage.isFile() && tamMessage.length()>0) {
						try {
							AudioInputStream stream = AudioSystem.getAudioInputStream(tamMessage);
						    AudioFormat format = stream.getFormat();
						    DataLine.Info info = new DataLine.Info(Clip.class, format);
						    Clip clip = (Clip) AudioSystem.getLine(info);
						    clip.open(stream);
						    clip.start();
						} catch (UnsupportedAudioFileException | IOException e) {
							this.m_logger.severe(e.getMessage());
						} catch (LineUnavailableException e) {
							this.m_logger.severe(e.getMessage());
						}
						
					} else {
						new SWTExecuter() {

							protected void execute() {
								int style = SWT.APPLICATION_MODAL
										| SWT.OK;
								MessageBox messageBox = new MessageBox(
										new Shell(DisplayManager
												.getDefaultDisplay()),
										style);
								messageBox
										.setMessage(getI18nManager()
												.getString(
														getNamespace(),
														"notam",
														"label",
														getLanguage()));
								messageBox.open();
							}
						}.start();
						return;
					}
				}	
			}
		}
		
	}

}
