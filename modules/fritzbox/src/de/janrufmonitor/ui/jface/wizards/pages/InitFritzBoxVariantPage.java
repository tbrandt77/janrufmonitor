package de.janrufmonitor.ui.jface.wizards.pages;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.framework.monitor.IMonitor;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.controls.HyperLink;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;
import de.janrufmonitor.ui.swt.DisplayManager;

public class InitFritzBoxVariantPage extends InitVariantPage {
	
	private String m_boxuser;
	private boolean m_active;
	private String m_boxpassword;
	private String m_boxip;
	private IRuntime m_runtime;
	
	public InitFritzBoxVariantPage() {}
	
	public void createControl(Composite parent) {
		setTitle(this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "title", "label", this.m_language));
		setDescription(this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "description", "label", this.m_language));

		final Composite c = new Composite(parent, SWT.NONE);
	    c.setLayout(new GridLayout(1, false));
	    c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		BooleanFieldEditor bfe = new BooleanFieldEditor(
				"activemonitor",
			this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "activemonitor2", "label", this.m_language),
			c
		);
		bfe.setPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent e) {
				if (e!=null && e.getNewValue()!=null && e.getNewValue() instanceof Boolean)
					m_active = ((Boolean) e.getNewValue()).booleanValue();
				setPageComplete(isComplete());
			}
			   
		   });
		
		new Label(c, SWT.LEFT);
		
	    StringFieldEditor sfe = new StringFieldEditor(
	    		"boxip",
				this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "boxip", "label", this.m_language),
				c);
	    sfe.setStringValue("fritz.box");
	    this.m_boxip = "fritz.box";
	    
	    sfe.setPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent e) {
				if (e!=null && e.getNewValue()!=null && e.getNewValue() instanceof String)
					m_boxip = (String) e.getNewValue();
				setPageComplete(isComplete());
			}
			   
		   });
	    
	    
	    ComboFieldEditor mode = new ComboFieldEditor(
				"boxloginmode",
				this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "boxloginmode", "label", this.m_language),
				new String[][] { 
					{this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "userpassword", "label", this.m_language), "0"}, 
					{this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "password_only", "label", this.m_language), "1"}
				},
			c
		);
	    
	    final StringFieldEditor sfe1 = new StringFieldEditor(
	    		"boxuser",
				this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "boxuser", "label", this.m_language),
				c);
	    
	    sfe1.setStringValue("");
	    this.m_boxuser = "";
	    
	    
	    mode.setPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent e) {
				if (e!=null && e.getNewValue()!=null && e.getNewValue() instanceof String) {
					String state = (String) e.getNewValue();
					if (state.equalsIgnoreCase("1")) {
						sfe1.setEnabled(false, c);
						sfe1.setStringValue("");
						sfe1.getTextControl(c).setBackground(new Color(DisplayManager.getDefaultDisplay(), 190, 190, 190));
						m_boxuser = "";
					} else {
						sfe1.setEnabled(true, c);
						sfe1.getTextControl(c).setBackground(new Color(DisplayManager.getDefaultDisplay(), 255, 255, 255));
					}
				}
			}
			   
		   });
	    
	    sfe1.setPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent e) {
				if (e!=null && e.getNewValue()!=null && e.getNewValue() instanceof String)
					m_boxuser = (String) e.getNewValue();
				setPageComplete(isComplete());
			}
			   
		   });
	    
	    sfe = new StringFieldEditor(
	    		"boxpassword",
				this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "boxpassword", "label", this.m_language),
				c);
	    
	    sfe.getTextControl(c).setEchoChar('*');
	    sfe.setStringValue("");
	    this.m_boxpassword = "";
	    
	    sfe.setPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent e) {
				if (e!=null && e.getNewValue()!=null && e.getNewValue() instanceof String)
					m_boxpassword = (String) e.getNewValue();
				setPageComplete(isComplete());
			}
			   
		   });
	    
	    new Label(c, SWT.LEFT);
	    Text l = new Text(c, SWT.LEFT | SWT.WRAP);
	    l.setText(this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "openfb", "label", this.m_language));
	    l.setEditable(false);	
	    l.setBackground(parent.getBackground());
	    
	    new Label(c, SWT.LEFT);
	    HyperLink hl = new HyperLink(c, SWT.LEFT | SWT.WRAP);
		hl.setText(this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip", "openfb2", "label", this.m_language));
		hl.addMouseListener( 
			new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (e.button==1)
					Program.launch("http://www.janrufmonitor.de/fritzbox-freischalten");
				}
			}
		);

	    setPageComplete(isComplete());
	    setControl(c);
	}
	
	public boolean isComplete() {
		if (m_boxip.trim().length()>0) {
			
			if (m_boxip.trim().startsWith("http:")) {
				setErrorMessage(this.m_i18n.getString("ui.jface.configuration.pages.FritzBoxVoip",
					"nohttp", "label", this.m_language));
				return false;
			}
			setErrorMessage(null);
			
			return true;
		}
		return false;
	}
	
	public boolean performFinish() {
		if (m_boxip==null || m_boxip.trim().length()==0) {
			try {
				Thread.sleep(550);
			} catch (InterruptedException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
			return true;
		}
		
		getRuntime().getConfigManagerFactory().getConfigManager().setProperty(FritzBoxMonitor.NAMESPACE, "boxip", m_boxip);
		getRuntime().getConfigManagerFactory().getConfigManager().setProperty(FritzBoxMonitor.NAMESPACE, "boxpassword", m_boxpassword);
		getRuntime().getConfigManagerFactory().getConfigManager().setProperty(FritzBoxMonitor.NAMESPACE, "boxuser", m_boxuser);
		getRuntime().getConfigManagerFactory().getConfigManager().setProperty(FritzBoxMonitor.NAMESPACE, "activemonitor", (m_active ? "true" : "false"));
		getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
		
		getRuntime().getConfigurableNotifier().notifyByNamespace(FritzBoxMonitor.NAMESPACE);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
		
		if (m_active && !getRuntime().getMonitorListener().isRunning()) {
			getRuntime().getMonitorListener().start();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				m_logger.log(Level.SEVERE, e.getMessage(), e);
				return false;
			}
			ICommand c = PIMRuntime.getInstance().getCommandFactory().getCommand("Activator");
			if (c!=null) {
				try {
					Map m = new HashMap();
					IMonitor mon = getRuntime().getMonitorListener().getDefaultMonitor();
					m.put("status", (mon.isStarted() ? "revert" : "invert"));
					c.setParameters(m); // this method executes the command as well !!
				} catch (Exception e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
				}
			}
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			m_logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
		return true;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

}
