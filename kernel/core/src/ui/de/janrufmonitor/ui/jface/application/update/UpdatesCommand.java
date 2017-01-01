package de.janrufmonitor.ui.jface.application.update;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAsyncDisplayCommand;
import de.janrufmonitor.ui.jface.wizards.UpdateWizard;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;

public class UpdatesCommand extends AbstractAsyncDisplayCommand implements IConfigurable {

	private static String NAMESPACE = "ui.jface.application.update.UpdatesCommand";
	
	private IRuntime m_runtime;
	
	private boolean m_preload;

	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return UpdatesCommand.NAMESPACE;
	}

	public boolean isExecutable() {
		return true;
	}

	public String getID() {
		return "UpdatesCommand";
	}

	public String getConfigurableID() {
		return this.getID();
	}

	public void setConfiguration(Properties configuration) {
	}

	public void asyncExecute() {
		try {
			WizardDialog.setDefaultImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
			UpdateWizard updates = new UpdateWizard(this.m_preload);
			Shell s = new Shell(DisplayManager.getDefaultDisplay());
			WizardDialog dlg = new WizardDialog(s, updates);
			DisplayManager.forceForeground(s);
			dlg.open();
			this.m_preload = false;
			if (dlg.getReturnCode() == WizardDialog.OK) {

			}
		} catch (Throwable t) {
			this.m_logger.log(Level.SEVERE, t.getMessage(), t);
			PropagationFactory.getInstance().fire(new Message(Message.ERROR, this.getNamespace(), t.toString().toLowerCase(), t));
		} finally {
			this.isExecuting = false;
		}
	}
	
	public void setParameters(Map m) {
		if (m!=null && m.containsKey("preload")) this.m_preload = true;
	}

}
