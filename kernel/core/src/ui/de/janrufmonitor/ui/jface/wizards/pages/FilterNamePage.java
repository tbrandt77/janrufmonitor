package de.janrufmonitor.ui.jface.wizards.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class FilterNamePage extends AbstractPage {

	private String NAMESPACE = "ui.jface.wizards.pages.FilterNamePage";

	private String m_name;
	private IRuntime m_runtime;
	
	public FilterNamePage(String name) {
		super(FilterNamePage.class.getName());
		if (name==null)
			name = "";
		
		this.m_name = name;
		
		setTitle(this.m_i18n.getString(getNamespace(), "title", "label", this.m_language));
		setDescription(this.m_i18n.getString(getNamespace(), "description", "label", this.m_language));
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}
	
	public String getResult() {
		return this.m_name;
	}

	public void createControl(Composite parent) {
	    Composite nameComposite = new Composite(parent, SWT.NONE);
	    nameComposite.setLayout(new GridLayout(2, false));
	    nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	    Label ln = new Label(nameComposite, SWT.LEFT);
	    ln.setText(this.m_i18n.getString(this.getNamespace(), "name", "label", this.m_language));
	    final Text name = new Text(nameComposite, SWT.BORDER);
	    name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    name.setText(this.m_name);

	    // Add the handler to update the name based on input
	    name.addModifyListener(new ModifyListener() {
	      public void modifyText(ModifyEvent event) {
	        m_name = name.getText();
	        setPageComplete(isComplete());
	      }
	    });
	    
	    setPageComplete(isComplete());
	    setControl(nameComposite);
	}

	protected IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
}
