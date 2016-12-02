package de.janrufmonitor.ui.jface.application.editor.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.AbstractTableLabelProvider;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.editor.Editor;
import de.janrufmonitor.ui.jface.application.editor.EditorConfigConst;
import de.janrufmonitor.ui.jface.application.editor.EditorFilterManager;
import de.janrufmonitor.ui.jface.wizards.EditorCategoryWizard;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;

public class CategoryAction extends AbstractAction implements EditorConfigConst {

	private class CategoryContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object o) {
			List l = (List)o;
			return l.toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {	
		}
		
	}
	
	private class CategoryLabelProvider extends AbstractTableLabelProvider {
		public String getColumnText(Object o, int column) {	
		    switch (column) {
		    case 0:
		      return (String)o;
		    }
			return null;
		}
	}
	
	private class CategoryDialog extends TitleAreaDialog {		
		
		//private String[] m_categories;
		private TableViewer tv; 
		private List f_l;
			
		public CategoryDialog(Shell shell) {
			super(shell);
		}
		
//		public void setCategories(String[] cats) {
//			this.m_categories = cats;
//		}
//		
//		public String[] getCategories() {
//			return this.m_categories;
//		}
		
		protected Control createContents(Composite parent) {
			Control c = super.createContents(parent);
			
			setTitle(
				getI18nManager().getString(
					getNamespace(),
					"dialogtitle",
					"label",
					getLanguage()
				)
			);
			setMessage(getI18nManager().getString(
					getNamespace(),
					"dialogtitle",
					"description",
					getLanguage()
				));
			return c;
		}

		protected Control createDialogArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			
			Composite view = new Composite(composite, SWT.NONE);
			view.setLayout(new GridLayout(2, false));

			tv = new TableViewer(view, SWT.BORDER | SWT.V_SCROLL);
			tv.getTable().setLinesVisible(false);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 80;
			gd.widthHint = 250;
			tv.getTable().setLayoutData(gd);
			tv.setContentProvider(new CategoryContentProvider());
			tv.setLabelProvider(new CategoryLabelProvider());
			f_l = this.getContentAsList();
			tv.setInput(f_l);
			
			tv.getTable().addMouseListener(
				new MouseAdapter() {
					public void mouseDoubleClick(MouseEvent e) {
						StructuredSelection s = (StructuredSelection) tv.getSelection();
						if (!s.isEmpty()) {
							String cat = (String) s.getFirstElement();
							if (cat!=null && cat.length()>0)
								openCategoryWizard(cat);						
						}
					}
				}
			);
			
			Composite buttonarea = new Composite(view, SWT.NONE);
			buttonarea.setLayout(new GridLayout(1, false));

			gd = new GridData(GridData.FILL_BOTH);
			gd.widthHint = 110;
			buttonarea.setLayoutData(gd);
			
			new Label(buttonarea, SWT.NONE);
			
			Button add = new Button(buttonarea, SWT.PUSH);
			add.setText(
				getI18nManager().getString(
						getNamespace(),
						"add",
						"label",
						getLanguage()
					)					
				);
			add.setLayoutData(gd);
			add.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						openCategoryWizard(null);						
					}
				}	
			);
			
			Button edit = new Button(buttonarea, SWT.PUSH);
			edit.setText(
				getI18nManager().getString(
						getNamespace(),
						"edit",
						"label",
						getLanguage()
					)					
				);
			edit.setLayoutData(gd);
			edit.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						StructuredSelection s = (StructuredSelection) tv.getSelection();
						if (!s.isEmpty()) {
							String cat = (String) s.getFirstElement();
							if (cat!=null && cat.length()>0)
								openCategoryWizard(cat);						
						}
					}
				}	
			);			
			
			Button remove = new Button(buttonarea, SWT.PUSH);
			remove.setText(
				getI18nManager().getString(
						getNamespace(),
						"remove",
						"label",
						getLanguage()
					)					
				);
			remove.setLayoutData(gd);
			remove.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						IStructuredSelection s = (IStructuredSelection)tv.getSelection();
						if (!s.isEmpty()) {
							String cat = (String) s.getFirstElement();
							if (cat!=null && cat.length()>0)
								f_l.remove(cat);
							tv.setInput(f_l);
						}
					}
				}			
			);
			
			Button export = new Button(buttonarea, SWT.PUSH);
			export.setText(
				getI18nManager().getString(
						getNamespace(),
						"export",
						"label",
						getLanguage()
					)					
				);
			export.setLayoutData(gd);
			export.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						FileDialog dialog = new FileDialog (new Shell(DisplayManager.getDefaultDisplay()), SWT.SAVE);
						dialog.setText(getI18nManager().getString(getNamespace(), "exporttitle", "label", getLanguage()));
						
						dialog.setFilterNames(new String[] {getI18nManager().getString(getNamespace(), "filtercat", "label", getLanguage())});
						dialog.setFilterExtensions(new String[] {"*.jam.cat"});
						
						String filter = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(getNamespace(), "lastopeneddir");
						if (filter == null || filter.length() == 0 || !new File(filter).exists())
							filter = PathResolver.getInstance(getRuntime()).getDataDirectory();
						dialog.setFilterPath(filter);
						
						final String filename = dialog.open();
						if (filename==null) return;
						
						filter = new File(filename).getParentFile().getAbsolutePath();
						getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), "lastopeneddir", filter);
						
						List cats = f_l;
						StringBuffer b = new StringBuffer();
						for (int i=0;i<cats.size();i++) {
							b.append(cats.get(i));
							b.append(IJAMConst.CRLF);
						}
						try {
							FileOutputStream fos = new FileOutputStream(filename);
							ByteArrayInputStream in = new ByteArrayInputStream(b.toString().getBytes());
							Stream.copy(in, fos, true);
						} catch (FileNotFoundException ex) {
							m_logger.severe(ex.toString());
						} catch (IOException ex) {
							m_logger.severe(ex.toString());
						}
					}
				}
			);
				
				Button importx = new Button(buttonarea, SWT.PUSH);
				importx.setText(
					getI18nManager().getString(
							getNamespace(),
							"import",
							"label",
							getLanguage()
						)					
					);
				importx.setLayoutData(gd);
				importx.addSelectionListener(
					new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							FileDialog dialog = new FileDialog (new Shell(DisplayManager.getDefaultDisplay()), SWT.OPEN);
							dialog.setText(getI18nManager().getString(getNamespace(), "importtitle", "label", getLanguage()));
							
							dialog.setFilterNames(new String[] {getI18nManager().getString(getNamespace(), "filtercat", "label", getLanguage())});
							dialog.setFilterExtensions(new String[] {"*.jam.cat"});
							
							String filter = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(getNamespace(), "lastopeneddir");
							if (filter == null || filter.length() == 0 || !new File(filter).exists())
								filter = PathResolver.getInstance(getRuntime()).getDataDirectory();
							dialog.setFilterPath(filter);
							
							final String filename = dialog.open();
							if (filename==null) return;
							
							filter = new File(filename).getParentFile().getAbsolutePath();
							getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), "lastopeneddir", filter);
							
							try {
								FileInputStream fin = new FileInputStream(filename);
								BufferedReader br = new BufferedReader(new InputStreamReader(fin));
								String cat = null;
								while (br.ready()) {
									cat = br.readLine();
									if (cat != null) {
										cat = cat.trim();
										f_l.remove(cat);
								    	
								    	if (cat!=null && cat.length()>0 && !f_l.contains(cat))
								    		f_l.add(cat);
									}
								}
								br.close();
								Collections.sort(f_l);
								tv.setInput(f_l);
							} catch (FileNotFoundException ex) {
								m_logger.severe(ex.toString());
							} catch (IOException ex) {
								m_logger.severe(ex.toString());
							}
						}
					}			

			);
			
			return super.createDialogArea(parent);
		}
		
		private List getContentAsList() {
			List l = new ArrayList();
			
			String categories = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Editor.NAMESPACE, "categories");
			if (categories!=null && categories.length()>0) {
				StringTokenizer st = new StringTokenizer(categories, ",");
				while (st.hasMoreTokens()) { 
					l.add(st.nextToken());
				}
			}
			Collections.sort(l);
			return l;
		}
		
		private void openCategoryWizard(String cat) {
		    Display display = DisplayManager.getDefaultDisplay();
			Shell shell = new Shell(display);

		    WizardDialog.setDefaultImage(SWTImageManager.getInstance(PIMRuntime.getInstance()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
		    EditorCategoryWizard filterWiz = new EditorCategoryWizard(cat);
		    WizardDialog dlg = new WizardDialog(shell, filterWiz);
		    dlg.open();
		    if (dlg.getReturnCode() == WizardDialog.OK) {
		    	String newCat = filterWiz.getResult();
		    	if (cat!=null)
		    		this.f_l.remove(cat);
		    	
		    	if (newCat!=null && newCat.length()>0 && !this.f_l.contains(newCat))
		    		this.f_l.add(newCat);
		    }
		    Collections.sort(this.f_l);
		    tv.setInput(this.f_l);
		}
		
		private void removeFitersFromConfig(){
			List l = new ArrayList();
			Properties c = m_app.getApplication().getConfiguration();
			Iterator iter = c.keySet().iterator();
			String key = null;
			while (iter.hasNext()) {
				key = (String)iter.next();
				if (key.startsWith("filter_cat_")) {
					l.add(key);
				}
			}
			iter = null;
			for (int i=0;i<l.size();i++) {
				m_app.getApplication().getConfiguration().remove(l.get(i));
				getRuntime().getConfigManagerFactory().getConfigManager().removeProperty(Editor.NAMESPACE, (String)l.get(i));
			}
			getRuntime().getConfigManagerFactory().getConfigManager().saveConfiguration();
		}
		
		protected void okPressed() {
			if (f_l!=null) {
				
				Collections.sort(f_l);
				
				List l = new ArrayList();
				
				EditorFilterManager jfm = new EditorFilterManager();

				// add categories
				StringBuffer categories = new StringBuffer();
				for (int i=f_l.size()-1;i>=0;i--) {
					categories.append(f_l.get(i));
					categories.append(",");
					IAttributeMap m = getRuntime().getCallFactory().createAttributeMap();
					m.add(getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CATEGORY, (String)f_l.get(i)));
					l.add(new IFilter[] {new AttributeFilter(m)});					
				}
				
				
				this.removeFitersFromConfig();
				for (int i=0;i<l.size();i++) {
					IFilter[] f = (IFilter[])l.get(i);
					if (f!=null && f.length>0)
					m_app.getApplication().getConfiguration().setProperty(
						"filter_cat_"+i,
						jfm.getFiltersToString(f)
					);
				}
				
				m_app.getApplication().getConfiguration().setProperty("categories", categories.toString());
				m_app.getApplication().storeConfiguration();
			}
			super.okPressed();
		}
		
	}
	
	private static String NAMESPACE = "ui.jface.application.editor.action.CategoryAction";
	
	private IRuntime m_runtime;

	public CategoryAction() {
		super();
		this.setText(
			this.getI18nManager().getString(
				this.getNamespace(),
				"title",
				"label",
				this.getLanguage()
			)
		);	
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "editor_category";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		CategoryDialog fd = new CategoryDialog(
			new Shell(DisplayManager.getDefaultDisplay())
		);
		fd.setBlockOnOpen(true);
		int result = fd.open();
		if (result==CategoryDialog.OK) {
			this.m_app.getApplication().storeConfiguration();
			this.m_app.updateViews(true);
		}
	}
}
