package de.janrufmonitor.ui.jface.configuration.pages;


import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.configuration.AbstractConfigPage;
import de.janrufmonitor.ui.jface.configuration.IConfigPage;
import de.janrufmonitor.ui.jface.configuration.controls.BooleanFieldEditor;
import de.janrufmonitor.util.formatter.Formatter;

public class Reject extends AbstractConfigPage {

	private String NAMESPACE = "ui.jface.configuration.pages.Reject";
    private String CONFIG_NAMESPACE = "service.Reject";

	private class AcContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object o) {
			List l = (List)o;
			return l.toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {	
		}
		
	}
	
	private class AcLabelProvider implements ITableLabelProvider {
		
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		public String getColumnText(Object o, int column) {
			String s = (String)o;
			
		    switch (column) {
		    case 0:
		      return s;
		    }
			return null;
		}

		public void addListener(ILabelProviderListener arg0) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		public void removeListener(ILabelProviderListener arg0) {
		}
		
	}
	
	private IRuntime m_runtime;
	private List dataList;
	private TableViewer tv;
	
	private Button active;
	private BooleanFieldEditor bfe;

	public String getConfigNamespace() {
		return this.CONFIG_NAMESPACE;
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) 
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getParentNodeID() {
		return IConfigPage.SERVICE_NODE;
	}

	public String getNodeID() {
		return "Reject".toLowerCase();
	}

	public int getNodePosition() {
		return 2;
	}

	protected Control createContents(Composite parent) {
		this.setTitle(this.m_i18n.getString(this.getNamespace(), "title", "label", this.m_language));
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
	
		String label = this.m_i18n.getString(this.getNamespace(), "enabled", "label", this.m_language);
		if (label.length()<150)
			for (int i=150;i>label.length();i--){
				label += " ";
			}
		
		this.active = new Button(c, SWT.CHECK);
		this.active.setText(
			label
		);
		this.active.setSelection(
			this.getPreferenceStore().getBoolean(this.getConfigNamespace()+SEPARATOR+"enabled")
		);
		
		bfe = new BooleanFieldEditor(
			this.getConfigNamespace()+SEPARATOR+"allclir",
			this.m_i18n.getString(this.getNamespace(), "allclir", "label", this.m_language),
			c
		);
			
		bfe.setPreferenceStore(this.getPreferenceStore());
	    bfe.doLoad();
	    
		Composite c1 = new Composite(c, SWT.NONE);
		c1.setLayout(new GridLayout(3, false));
		
		Composite c11 = new Composite(c1, SWT.NONE);
		c11.setLayout(new GridLayout(1, false));
		
		new Label(c11, SWT.NONE).setText(
			this.m_i18n.getString(this.getNamespace(), "areacodes", "label", this.m_language)
		); 
		
		tv = new TableViewer(c11);
		tv.setContentProvider(new AcContentProvider());
		tv.setLabelProvider(new AcLabelProvider());
		
		Table t = tv.getTable();
		GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL_BOTH;
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = 150;
        gd.heightHint = 100;
		
		t.setLayoutData(gd);
		TableColumn tc = new TableColumn(t, SWT.LEFT);
		tc.getText();

		tv.setInput(this.getDataList());
		
	    for (int i = 0, n = t.getColumnCount(); i < n; i++) {
	    	t.getColumn(i).pack();
	    	t.getColumn(i).setWidth(140);
	    }
	    t.setLinesVisible(false);
	    
	    Composite c12 = new Composite(c1, SWT.NONE);
		c12.setLayout(new GridLayout(1, false));

		Button add = new Button(c12, SWT.PUSH);
		add.setText(
			this.m_i18n.getString(this.getNamespace(), "add", "label", this.m_language)
		);
		
		Button remove = new Button(c12, SWT.PUSH);
		remove.setText(
			this.m_i18n.getString(this.getNamespace(), "remove", "label", this.m_language)
		);
		
		Composite c13 = new Composite(c1, SWT.NONE);
		c13.setLayout(new GridLayout(1, false));
		
		new Label(c13, SWT.NONE).setText(
			this.m_i18n.getString(this.getNamespace(), "edit", "label", this.m_language)
		); 
		
		final Text ip = new Text(c13, SWT.BORDER);
		gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = 100;
        ip.setLayoutData(gd);
        ip.setText("");
        
		add.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (ip.getText().length()>0 && !dataList.contains(ip.getText())){
						ICaller c = Identifier.identifyDefault(getRuntime(), getRuntime().getCallerFactory().createPhonenumber(PhonenumberAnalyzer.getInstance().normalize(ip.getText())));
						if (c!=null) {
							dataList.add(Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, c.getPhoneNumber()));	
						}
						//dataList.add(ip.getText());
						tv.setInput(dataList);
						for (int i = 0, n = tv.getTable().getColumnCount(); i < n; i++) {
							tv.getTable().getColumn(i).pack();
					    }
					}
				}
			}
		);
		
		remove.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					StructuredSelection s = (StructuredSelection) tv.getSelection();
					if (!s.isEmpty()) {
						String o = (String)s.getFirstElement();
						ip.setText(o);
						dataList.remove(o);
						tv.setInput(dataList);
						for (int i = 0, n = tv.getTable().getColumnCount(); i < n; i++) {
							tv.getTable().getColumn(i).pack();
					    }						
					}
				}
			}				
			);
        
	    return c;
	}
	
	public boolean performOk() {
		this.performApply();
		this.dataList.clear();

		boolean ok = super.performOk();
		
		if (ok && this.getConfigNamespace().length()>0)
			this.getRuntime().getConfigurableNotifier().notifyByNamespace(this.getConfigNamespace());
		
		return ok;
	}
	
	protected void performApply() {
		StringBuffer list = new StringBuffer();
		String m = null;
		for (int i=0;i<this.dataList.size();i++) {
			m = (String)this.dataList.get(i);
			list.append(PhonenumberAnalyzer.getInstance().normalize(m));
			list.append(",");
		}
		this.getPreferenceStore().setValue(this.getConfigNamespace() + SEPARATOR + "rejectareacodes", list.toString());
		this.getPreferenceStore().setValue(this.getConfigNamespace() + SEPARATOR + "enabled", active.getSelection());
		bfe.store();
	}
	
	protected void performDefaults() {
		super.performDefaults();
		this.dataList.clear();
		active.setSelection(this.getPreferenceStore().getDefaultBoolean(this.getConfigNamespace()+SEPARATOR + "enabled"));
		bfe.doLoadDefault();
		
		
		this.dataList = this.getDefaultDataList();
		tv.setInput(this.dataList);
	}
	
	private List getDataList() {
		if (this.dataList==null || this.dataList.size()==0) {
			this.dataList = new ArrayList();
			String allowedList = this.getPreferenceStore().getString(this.getConfigNamespace() + SEPARATOR + "rejectareacodes");
			if (allowedList.trim().length()>0) {
				StringTokenizer st = new StringTokenizer(allowedList, ",");
				while (st.hasMoreTokens()) {
					String ip = st.nextToken().trim();
					if (ip.length()>0) {
						ICaller c = Identifier.identifyDefault(getRuntime(), getRuntime().getCallerFactory().createPhonenumber(ip));
						if (c!=null) {
							this.dataList.add(Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, c.getPhoneNumber()));	
						}						
					}
				}
			}
		}
		return this.dataList;
	}
	
	private List getDefaultDataList() {
		if (this.dataList==null || this.dataList.size()==0) {
			this.dataList = new ArrayList();
			String allowedList = this.getPreferenceStore().getDefaultString(this.getConfigNamespace() + SEPARATOR + "rejectareacodes");
			if (allowedList.trim().length()>0) {
				StringTokenizer st = new StringTokenizer(allowedList, ",");
				while (st.hasMoreTokens()) {
					String ip = st.nextToken().trim();
					if (ip.length()>0) {
						ICaller c = Identifier.identifyDefault(getRuntime(), getRuntime().getCallerFactory().createPhonenumber(ip));
						if (c!=null) {
							this.dataList.add(Formatter.getInstance(getRuntime()).parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, c.getPhoneNumber()));	
						}	
					}
				}
			}
		}
		return this.dataList;
	}
}