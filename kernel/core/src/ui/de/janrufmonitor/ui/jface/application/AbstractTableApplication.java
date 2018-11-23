package de.janrufmonitor.ui.jface.application;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;


import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.imexport.ITracker;
import de.janrufmonitor.ui.jface.application.action.IAction;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellEditorRenderer;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.string.StringUtils;

public abstract class AbstractTableApplication extends AbstractBaseApplication implements IConfigConst {

	private Combo currentView;
	private int lastMarkedRow = -1;

	public AbstractTableApplication() {
		this(true);
	}
	
	public AbstractTableApplication(boolean isBlocking) {
		super(isBlocking);
	}
	
	public void updateProviders() {
		this.buildTableColumns();
		this.initializeProviders();
		((TableViewer)viewer).setContentProvider(this.getContentProvider());
	}
	
	private String getFilterLabel(IFilterManager fm, IFilter[] f) {
		String name = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), "filtername_"+fm.getFiltersToString(f));
		if (name!=null && name.length()>0) return name;
		return fm.getFiltersToLabelText(f, 45);
	}


	public synchronized void updateViews(Object[] controllerdata, boolean reload) {
		// refresh controll data
		refreshController(reload);
		
		if (controllerdata!=null)
			this.m_controller.generateElementArray(controllerdata);
		
		final IFilterManager fm = this.getFilterManager();
		
		if (fm != null && this.getConfiguration().getProperty(CFG_ENABLE_FILTER_VIEW, "true").equalsIgnoreCase("true")) {
			IFilter[] f = fm.getFiltersFromString(this
					.getConfiguration().getProperty(CFG_FILTER, ""));
			
			String activeFilter = this.getFilterLabel(fm, f);
			
			// get all defined Filters from configuration
			List l = new ArrayList();
			
			l.add(new IFilter[0]);
			IFilterManager jfm = getFilterManager();
			Properties c = this.getConfiguration();
			Iterator iter = c.keySet().iterator();
			String key = null;
			while (iter.hasNext()) {
				key = (String)iter.next();
				if (key.startsWith("filter_")) {
					String filter = c.getProperty(key);
					l.add(jfm.getFiltersFromString(filter));
				}
			}
			
			// added 2015/04/03: added runtime filters
			if (jfm.hasRuntimeFilters()) {
				IFilter[][] rf = jfm.getRuntimeFilters();
				if (rf!=null && rf.length>0) {
					for (int i=0;i<rf.length;i++)
						l.add(rf[i]);
				}
			}
			
			// added 2015/04/01: sort filter list
			Collections.sort(l, new Comparator() {

				public int compare(Object f1, Object f2) {
					if (f1!=null && f2!=null && f1 instanceof IFilter[] && f2 instanceof IFilter[]) {
						return getFilterLabel(getFilterManager(), ((IFilter[])f1)).toLowerCase().compareTo(getFilterLabel(getFilterManager(), ((IFilter[])f2)).toLowerCase());
//						if (((IFilter[])f1).length==((IFilter[])f2).length) {
//							return (((IFilter[])f2)[0].toString().compareTo(((IFilter[])f1)[0].toString()));
//						}
//						if (((IFilter[])f1).length<((IFilter[])f2).length) return -1;
//						return 1;
					}
					return 0;
				}});
			
			String[] filters = new String[l.size()];
			int select = -1;
			String filterAlias = null;
			for (int i=0;i<l.size();i++) {
				filterAlias = this.getFilterLabel(fm, (IFilter[]) l.get(i)); //this.getFilterManager().getFiltersToLabelText((IFilter[]) l.get(i),45);
				
				if (filterAlias.equalsIgnoreCase(activeFilter)) {
					select=i;	
				}
				filters[i] = filterAlias;
				this.currentView.setData(filterAlias, (IFilter[]) l.get(i));
			}
			this.currentView.setItems(filters);
			if (select==-1) {
				getApplication().getConfiguration().setProperty(
					CFG_FILTER, fm.getFiltersToString((IFilter[]) l.get(0))
				);
				getApplication().storeConfiguration();
				select=0;
			}
			this.currentView.select(select);
			this.currentView.setVisibleItemCount((this.currentView.getItemCount()>16 ? 16 : this.currentView.getItemCount()));
		
			this.currentView.pack();
			this.currentView.getParent().pack();
		}		
		
		// update menu bar
		if (this.getMenuBarManager()!=null) {
			Menu m = this.getMenuBarManager().getMenu();
			this.setMenuItemStatus(m);
		}
		
		new SWTExecuter(false, getID()+"-BuildTableThread") {
			protected void execute() {
		
				final ThreadStatus ts = new ThreadStatus();
				
				class WorkerSWTExecuter  extends SWTExecuter {
					public WorkerSWTExecuter(boolean sync, String name){
						super(sync, name);
					}
					
					public void execute() {
						try {
							Cursor c = new Cursor(((TableViewer)viewer).getTable().getDisplay(), SWT.CURSOR_WAIT);
						
							Table table = ((TableViewer)viewer).getTable();
							AbstractMenuBuilder mb = getMenuBuilder();
							if (mb!=null)
								table.setMenu(mb.createPopupMenu(table));
							
							table.getShell().setCursor(c);

							long start = System.currentTimeMillis();
											
							viewer.setInput(null);
							viewer.setInput(m_controller);

							// added: 2007/03/17: avoid dead lock in journal...
							ts.setFinished(true);
							
							table.setFocus();
							table
									.setLinesVisible(getConfiguration()
											.getProperty(
													CFG_SHOW_GRID,
													"true")
											.equalsIgnoreCase(
													"true"));

							TableColumn[] columns = table
									.getColumns();
							for (int i = 0; i < columns.length; i++) {
								columns[i]
										.setImage(null);
							}
							int sortcolumn = getOrderColumn(Integer
									.parseInt(getConfiguration()
											.getProperty(
													CFG_ORDER,
													"0")));
							if (sortcolumn > -1
									&& sortcolumn < columns.length) {
								
								table.setSortColumn(columns[sortcolumn]);
								
								if (getConfiguration()
										.getProperty(
												CFG_DIRECTION,
												"false")
										.equalsIgnoreCase(
												"false")) {
									table.setSortDirection(SWT.DOWN);
								} else {
									table.setSortDirection(SWT.UP);
								}
							}
							
//							IAction action = getColoringAction();
//							if (action!=null)
//								action.run();
							
							IAction action = getHightlightAction();
							if (action!=null)
								action.run();
							
							// set new title text...
							String title_ext = getTitleExtension();
							if (title_ext==null || title_ext.length()==0)
								table.getShell().setText(
										getI18nManager().getString(getNamespace(), "title", "label", getLanguage()));
							else 
								table.getShell().setText(
										getI18nManager().getString(getNamespace(), "title", "label", getLanguage())+ " - "+title_ext);
							
							table.getShell().setCursor(null);	
							c.dispose();
							ts.setFinished(true);					
							
							if (lastMarkedRow>-1) {
								table.setSelection(Math.min(lastMarkedRow, table.getItemCount()));
							}

							checkAmountOfEntries(start, System.currentTimeMillis());
						} catch (Exception e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
							if (ts!=null) ts.setFinished(true);	
							PropagationFactory.getInstance().fire(new Message(Message.ERROR, getNamespace(), "refresherror", e));
						}
					}				
				};
					
				final WorkerSWTExecuter workerThread = new WorkerSWTExecuter(false, getID()+"-SetDataToTableThread");

				if (getConfiguration().getProperty(AbstractTableApplication.CFG_SHOW_REFRESH_POPUP, "true").equalsIgnoreCase("true")) {
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());	
					try {				
						IRunnableWithProgress r = new IRunnableWithProgress() {
							public void run(IProgressMonitor progressMonitor) {
								progressMonitor.beginTask(getI18nManager()
										.getString(getNamespace(),
												"refreshprogress", "label",
												getLanguage()), IProgressMonitor.UNKNOWN);
							
								Thread t = new Thread() {
									public void run() {
										// preload data
										try {
											m_controller.getElementArray();
										} catch (Exception ex) {
											m_logger.log(Level.SEVERE, ex.getMessage(), ex);
										}										
									}
								};
								t.setName("JAM-"+getID()+"#Preloader-Thread-(non-deamon)");
								t.start();
								
								Object repository = m_controller.getRepository();
								
								do {
									try {
										Thread.sleep(500);
									} catch (InterruptedException e) {
									}
									if (repository instanceof ITracker) {
										String msg = getI18nManager()
										.getString(getNamespace(),
												"tracking", "label",
												getLanguage());
										
										int leftover = Math.max(0, (((ITracker)repository).getTotal() - ((ITracker)repository).getCurrent()));
										
										msg = StringUtils.replaceString(msg, "{%1}", Integer.toString(leftover));
										
										progressMonitor.subTask(msg);
									}
									
								} while (t.isAlive());
								
								workerThread.start();
								
								while (!ts.isFinished()) {
									try {
										progressMonitor.worked(1);
										Thread.sleep(100);
									} catch (InterruptedException e) {
										m_logger.log(Level.SEVERE, e.getMessage(), e);
									}
								}
								
								progressMonitor.done();
							}
						};
						pmd.setBlockOnOpen(false);
						pmd.run(true, false, r);

						//ModalContext.run(r, true, pmd.getProgressMonitor(), DisplayManager.getDefaultDisplay());
					} catch (InterruptedException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (InvocationTargetException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					} 			
				} else {
					workerThread.execute();
				}

				// refresh status line
				String message = m_controller.countElements()
				+ getI18nManager().getString(
						getNamespace(), "items",
						"label", getLanguage());
				
				
				if (fm!=null) {
					IFilter[] f = fm.getFiltersFromString(getConfiguration().getProperty(CFG_FILTER, ""));
					
					if (f!=null && f.length>0 && getConfiguration().getProperty(CFG_ENABLE_FILTER_VIEW, "true").equalsIgnoreCase("true")) {
						message += getI18nManager().getString(
								getNamespace(), "items_filtered",
								"label", getLanguage());
					}
					getStatusLineManager().setMessage(message);
				}				
			} 
		}.start();
	}
	
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		
		if (this.getMenuBuilder()!=null) {
			List actions = this.getMenuBuilder().getToolbarActions();
			if (this.isShowToolbar() && actions!=null && actions.size()>0 && this.getToolBarManager()!=null) {
				ToolBar tb = this.getToolBarManager().createControl(composite);
				for (int i=0,j=actions.size();i<j;i++) {
					final org.eclipse.jface.action.IAction a = (org.eclipse.jface.action.IAction) actions.get(i);
					if (a!=null && a.getImageDescriptor()!=null) {
						ToolItem ti = new ToolItem(tb, SWT.PUSH);
						Image img = (a).getImageDescriptor().createImage();
						ti.setImage(img);
						ti.setText(a.getText());
						if (a instanceof AbstractAction) {
							ti.setText(((AbstractAction)a).getShortText());
						}
						ti.setToolTipText(a.getToolTipText());
						ti.addSelectionListener(
							new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									a.run();
								}
							}
						);
					}
				}
			}
		}
		
		addRenderBeforeTableHooks(composite);
	
		// add filter capability
		Composite line = new Composite(composite, SWT.NONE);
		line.setLayout(new GridLayout(6, false));
		if (this.getFilterManager()!=null && this.getConfiguration().getProperty(CFG_ENABLE_FILTER_VIEW, "true").equalsIgnoreCase("true")) {
			Composite view = new Composite(line, SWT.NONE);
			view.setLayout(new GridLayout(2, false));

			new Label(view, SWT.NONE).setText(this.getI18nManager().getString(
					this.getNamespace(), "current_view", "label",
					this.getLanguage()));
			new Label(view, SWT.NONE);
			this.currentView = new Combo(view, SWT.READ_ONLY | SWT.FLAT);
			this.currentView.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					String filterName = currentView.getItem(currentView.getSelectionIndex());
					IFilter[] selectedFilters = (IFilter[]) currentView.getData(filterName);
					
					getApplication().getConfiguration().setProperty(
						CFG_FILTER, getFilterManager().getFiltersToString(selectedFilters)
					);
					getApplication().storeConfiguration();
					updateViews(true);
				}
			});
			
			if (getFilterAction()!=null) {
				Button filterAction = new Button(view, SWT.PUSH);
				filterAction.addSelectionListener(
						new SelectionAdapter() {
							public void widgetSelected(SelectionEvent e) {
								de.janrufmonitor.ui.jface.application.action.IAction action = getFilterAction();
								if (action != null)
									action.run();
							}
						}		
				);
				filterAction.setImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_FILTER_GIF));
				filterAction.pack();	
			}
		}

		final de.janrufmonitor.ui.jface.application.action.IAction action = getQuickSearchAction();
		if (this.isShowQuickSearch() && action!=null) {
			Composite view = new Composite(line, SWT.NONE);
			view.setLayout(new GridLayout(2, false));

			new Label(view, SWT.NONE).setText(this.getI18nManager().getString(
					this.getNamespace(), "quicksearch", "label",
					this.getLanguage()));
			
			new Label(view, SWT.NONE);
			
			final Combo search = new Combo(view, SWT.FLAT);
			final String empty = this.getConfiguration().getProperty(CFG_SEARCHTERMS, "");
			
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.minimumWidth = 300;
			search.setLayoutData(gd);
			
			search.add(empty);
			final List sh = this.getSearchHistory();
			for (int i=0;i<sh.size();i++) {
				if (!empty.equalsIgnoreCase((String) sh.get(i)))
					search.add((String) sh.get(i));
			}
			
			final Button removeFilter = new Button(view, SWT.PUSH);
			
			search.select(0);
			search.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					action.setData(search.getText());
					if (search.getText().length()>0) removeFilter.setEnabled(true);
					action.run();
				}
			});
			search.setToolTipText(this.getI18nManager().getString(
					this.getNamespace(), "quicksearch", "description",
					this.getLanguage()));
			
			search.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e){
					if (e.character == 13) {
						action.setData(search.getText());
						if (search.getText().length()>0) removeFilter.setEnabled(true);
						if (!sh.contains(search.getText())) {
							sh.add(search.getText());
							search.add(search.getText());
						}
						setSearchHistory(sh);
						action.run();
					}
				}	
			});
			
			removeFilter.addSelectionListener(
					new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							removeFilter.setEnabled(false);
							if (!sh.contains(search.getText())) {
								sh.add("");
							}
							search.add(""); 
							search.select(0);
							setSearchHistory(sh);
							action.setData("");
							action.run();
						}
					}		
			);
			removeFilter.setImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_FILTER_REMOVE_GIF));
			removeFilter.pack();	
			
			removeFilter.setEnabled(false);
			if (search.getText().length()>0) removeFilter.setEnabled(true);
			
		}
		
		viewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI | SWT.VIRTUAL);

		final Table table = ((TableViewer)viewer).getTable();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		this.updateProviders();
		
		// added drag and drop feature
		if (this.getDropTargetHandler()!=null) {
			int operations = DND.DROP_MOVE;
			Transfer[] types = new Transfer[] {FileTransfer.getInstance()};
			DropTarget target = new DropTarget(table, operations);
			target.setTransfer(types);

			target.addDropListener (new DropTargetAdapter() {
				public void drop(DropTargetEvent event) {
					
					// A drop has occurred, copy over the data
					if (event.data == null) {
						event.detail = DND.DROP_NONE;
						return;
					}
					getDropTargetHandler().execute((String[]) event.data);
				}
		 	});
			
			
		}

		FontData tableFontData = table.getFont().getFontData()[0];
		int size = Integer.parseInt(this.getConfiguration().getProperty(CFG_FONT_SIZE, "8"));
		table.setFont(this.getSizedFont(tableFontData, size, false));

		AbstractMenuBuilder mb = this.getMenuBuilder();
		if (mb!=null)
			table.setMenu(mb.createPopupMenu(table));
		
		table.addKeyListener(
			new KeyAdapter() {
				public void keyPressed(KeyEvent e){
					if (e.character == SWT.DEL) {
						de.janrufmonitor.ui.jface.application.action.IAction action = getDeleteAction();
						if (action != null && action instanceof org.eclipse.jface.action.IAction && ((org.eclipse.jface.action.IAction)action).isEnabled())
							action.run();
					}
				}	
			}		
		);
		table.addMouseListener(
			new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent e) {
					de.janrufmonitor.ui.jface.application.action.IAction action = getAssignAction();
					if (action != null && action instanceof org.eclipse.jface.action.IAction && ((org.eclipse.jface.action.IAction)action).isEnabled())
						action.run();
				}
			}
		);
		
		// 2006/08/02: this is a workaraound: No multiple line in table possibe
		Listener paintListener = new Listener() {
			int name_column = getColumnNumber("name"); 
			int address_column = getColumnNumber("address");
			int img_column = getColumnNumber("imagepreview");
			// removed 2010/02/17: do to usability notes column is not rendered with SWT.WRAP
			//int notes_column = getColumnNumber("notes");
			public void handleEvent(Event event) {
				if (event.index==img_column) {
					TableItem it = (TableItem)event.item;
					it.setText(event.index, IJAMConst.CRLF+IJAMConst.CRLF+IJAMConst.CRLF);
					switch(event.type) {		
					case SWT.MeasureItem: {
						TableItem item = (TableItem)event.item;
						String text = item.getText(event.index);
						Point size = event.gc.textExtent(text);
						event.width = size.x;
						event.height = Math.max(event.height, size.y + 7);
						break;
					}
					case SWT.PaintItem: {
						TableItem item = (TableItem)event.item;
						String text = item.getText(event.index);
						Point size = event.gc.textExtent(text);					
						int offset2 = (event.index == 0 ? Math.max(0, (event.height - size.y) / 2) : 0) + 3;
						event.gc.drawText(text, event.x + offset2, event.y + offset2, true);
						break;
					}
					case SWT.EraseItem: {	
						event.detail &= ~SWT.FOREGROUND;
						break;
					}
				}
					return;
				}
				
				
				if (event.index!=name_column && event.index!=address_column /**&& event.index!=notes_column*/) return;
				
				switch(event.type) {		
					case SWT.MeasureItem: {
						TableItem item = (TableItem)event.item;
						String text = item.getText(event.index);
						Point size = event.gc.textExtent(text);
						event.width = size.x;
						event.height = Math.max(event.height, size.y + 7);
						break;
					}
					case SWT.PaintItem: {
						TableItem item = (TableItem)event.item;
						String text = item.getText(event.index);
						Point size = event.gc.textExtent(text);					
						int offset2 = (event.index == 0 ? Math.max(0, (event.height - size.y) / 2) : 0) + 3;
						event.gc.drawText(text, event.x + offset2, event.y + offset2, true);
						break;
					}
					case SWT.EraseItem: {	
						event.detail &= ~SWT.FOREGROUND;
						break;
					}
				}
			}

		};
		table.addListener(SWT.MeasureItem, paintListener);
		table.addListener(SWT.PaintItem, paintListener);
		table.addListener(SWT.EraseItem, paintListener);
//		table.addListener(SWT.SetData, new Listener() {
//
//			public void handleEvent(Event e) {
//				TableItem item = (TableItem)e.item;
//				int index = table.indexOf(item);
//				item.setText("Item "+index);
//
//			}
//			
//		});
			
		addRenderAfterTableHooks(composite);

		table.addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.widget instanceof Table) {
						lastMarkedRow = ((Table)e.widget).getSelectionIndex();
					}
				}
			}		
		);
		
		updateViews(true);
		
		if (this.getMenuBuilder()!=null) {
			List startupActions = this.getMenuBuilder().getStartupActions();
			if (startupActions!=null && startupActions.size()>0) {
				IAction a = null;
				for (int i=0,j=startupActions.size();i<j;i++) {
					a = (IAction) startupActions.get(i);
					if (a!=null){
						a.setApplication(this);
						a.run();
					}
				}
			}
		}

		return composite;
	}
	
	private void buildTableColumns() {
		Table t = ((TableViewer)viewer).getTable();

	    int columnCount = this.getTableColumnCount();
		String[] columnHeader = new String[columnCount];
		CellEditor[] editors = new CellEditor[columnCount];
		
		ITableCellRenderer r = null;
		for (int i=0;i<columnCount;i++) {
			columnHeader[i] = this.getColumnID(i);
			// check renderer type
			r = RendererRegistry.getInstance().getRenderer(columnHeader[i]);
			if (r instanceof ITableCellEditorRenderer) {
				switch (((ITableCellEditorRenderer) r).getType()) {
				case 1: 
					editors[i] = new ComboBoxCellEditor(t, ((ITableCellEditorRenderer) r).getValues() ,SWT.READ_ONLY);
					break;
				case 2: 
					editors[i] = new CheckboxCellEditor(t);
					break;
				default:
					//editors[i] = new TextCellEditor(t);
					editors[i] = new TextCellEditor(t/**, SWT.WRAP*/);	
				}
			} else {
				//editors[i] = new TextCellEditor(t);
				editors[i] = new TextCellEditor(t, SWT.WRAP);
			}
		}
		
	    ((TableViewer)this.viewer).setColumnProperties(columnHeader);
		((TableViewer)this.viewer).setCellEditors(editors);
		((TableViewer)this.viewer).setCellModifier(new TableCellModifier(((TableViewer)viewer), this));
		
		TableColumn[] cols = t.getColumns();
		for (int i=0;i<cols.length;i++) {
			cols[i].dispose();
		}
		int columns = this.getTableColumnCount();

		String id = "";
		for (int i = 0; i < columns; i++) {
			TableViewerColumn tc = new TableViewerColumn( ((TableViewer)this.viewer), SWT.LEFT);
			id = getColumnID(i);
			ITableCellRenderer tr = RendererRegistry.getInstance().getRenderer(id);
			tc.getColumn().setText((tr!=null ? tr.getHeader() : ""));
			tc.getColumn().setData(id);
			if (tr!=null && tr.isRenderImage())
				tc.setLabelProvider(this.getImageCellLabelProvider(id));
			else
				tc.setLabelProvider(this.getTextCellLabelProvider(id));
			
			
			
			tc.getColumn().addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					de.janrufmonitor.ui.jface.application.action.IAction action = getOrderAction();
					if (action != null
							&& action instanceof org.eclipse.jface.action.IAction) {
						Event ev = new Event();
						ev.widget = e.widget;
						((org.eclipse.jface.action.IAction) action)
								.runWithEvent(ev);
					}
				}
			});

			int width = Integer.parseInt(getConfiguration().getProperty(
					CFG_COLUMN_SIZE + id, "50"));
			if (width > -1) {
				tc.getColumn().setWidth(width);
			}

			tc.getColumn().addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					if (e.widget instanceof TableColumn) {
						TableColumn tc = (TableColumn) e.widget;
						if (tc.getData() != null) {
							String column = (String) tc.getData();
							int width = tc.getWidth();
							getConfiguration().setProperty(
									CFG_COLUMN_SIZE + column,
									Integer.toString(width));
							storeConfiguration();
							m_logger.info("Set column size to " + width);
						}
					}
				}
			});
		}
		OwnerDrawLabelProvider.setUpOwnerDraw(((TableViewer)this.viewer));
	}

	protected abstract IStructuredContentProvider getContentProvider();
	
	private CellLabelProvider getImageCellLabelProvider(String renderer) {
		return new ImageCellLabelProvider(renderer);
	}
	
	private CellLabelProvider getTextCellLabelProvider(String renderer) {
		return new TextCellLabelProvider(renderer);
	}
	
	protected List getSearchHistory() {
		return Collections.EMPTY_LIST;
	}
	
	protected void setSearchHistory(List searchTerms) {
		
	}
	
}

