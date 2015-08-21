package de.janrufmonitor.ui.jface.application.action;

import de.janrufmonitor.ui.jface.application.IApplication;

public interface IAction {

	public void setApplication(IApplication app);
	
	public String getID();
	
	public void setID(String id);
	
	public void setData(Object data);
	
	public String getNamespace();
	
	public void run();
	
	public boolean hasSubActions();
	
	public IAction[] getSubActions();
	
	public boolean isEnabled();
	
}
