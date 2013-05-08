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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.imexport.ITracker;
import de.janrufmonitor.ui.jface.application.action.IAction;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.string.StringUtils;

public abstract class AbstractTreeTableApplication extends
		AbstractBaseApplication implements IConfigConst {

	private Combo currentView;

	private int lastMarkedTreeItem = -1;

	public AbstractTreeTableApplication() {
		this(true);
	}
	
	public AbstractTreeTableApplication(boolean isBlocking) {
		super(isBlocking);
	}

	public void updateProviders() {
		this.buildTableColumns();
		this.initializeProviders();
		((TreeViewer) viewer).setContentProvider(this.getContentProvider());
	}

	public synchronized void updateViews(Object[] controllerdata, boolean reload) {
		// refresh controll data
		refreshController(reload);

		if (controllerdata != null)
			this.m_controller.generateElementArray(controllerdata);

		final IFilterManager fm = this.getFilterManager();

		if (fm != null) {
			IFilter[] f = fm.getFiltersFromString(this.getConfiguration()
					.getProperty(CFG_FILTER, ""));

			String activeFilter = this.getFilterManager()
					.getFiltersToLabelText(f, 45);

			// get all defined Filters from configuration
			List l = new ArrayList();

			l.add(new IFilter[0]);
			final IFilterManager jfm = getFilterManager();
			Properties c = this.getConfiguration();
			Iterator iter = c.keySet().iterator();
			String key = null;
			while (iter.hasNext()) {
				key = (String) iter.next();
				if (key.startsWith("filter_")) {
					String filter = c.getProperty(key);
					l.add(jfm.getFiltersFromString(filter));
				}
			}
			
			// 2009/01/10: added sorting of filters
			Collections.sort(l, new Comparator() {
				public int compare(Object o1, Object o2) {
					if (o1 instanceof IFilter[] && o2 instanceof IFilter[]) {
						return jfm.getFiltersToLabelText((IFilter[]) o1, 45).compareToIgnoreCase(jfm.getFiltersToLabelText((IFilter[]) o2, 45));
					}					
					return 0;
				}
			}
			);

			String[] filters = new String[l.size()];
			int select = -1;
			String filterAlias = null;
			for (int i = 0; i < l.size(); i++) {
				filterAlias = jfm.getFiltersToLabelText(
						(IFilter[]) l.get(i), 45);

				if (filterAlias.equalsIgnoreCase(activeFilter)) {
					select = i;
				}
				filters[i] = filterAlias;
				this.currentView.setData(filterAlias, (IFilter[]) l.get(i));
			}
			this.currentView.setItems(filters);
			if (select == -1) {
				getApplication().getConfiguration().setProperty(
						CFG_FILTER,
						jfm.getFiltersToString(
								(IFilter[]) l.get(0)));
				getApplication().storeConfiguration();
				select = 0;
			}
			this.currentView.select(select);
			this.currentView.setVisibleItemCount((this.currentView.getItemCount()>16 ? 16 : this.currentView.getItemCount()));

			this.currentView.pack();
			this.currentView.getParent().pack();
		}

		// update menu bar
		Menu m = this.getMenuBarManager().getMenu();
		this.setMenuItemStatus(m);

		new SWTExecuter(false, getID() + "-BuildTableThread") {
			protected void execute() {

				final ThreadStatus ts = new ThreadStatus();

				class WorkerSWTExecuter extends SWTExecuter {
					public WorkerSWTExecuter(boolean sync, String name) {
						super(sync, name);
					}

					public void execute() {
						try {
							Cursor c = new Cursor(((TreeViewer) viewer)
									.getTree().getDisplay(), SWT.CURSOR_WAIT);

							Tree tree = ((TreeViewer) viewer).getTree();
							AbstractMenuBuilder mb = getMenuBuilder();
							if (mb != null)
								tree.setMenu(mb.createPopupMenu(tree));

							tree.getShell().setCursor(c);

							long start = System.currentTimeMillis();
				
							viewer.setInput(null);
							viewer.setInput(m_controller);

							// added: 2007/03/17: avoid dead lock in journal...
							ts.setFinished(true);

							tree.setFocus();
							tree.setLinesVisible(getConfiguration()
									.getProperty(CFG_SHOW_GRID, "true")
									.equalsIgnoreCase("true"));

							TreeColumn[] columns = tree.getColumns();
							for (int i = 0; i < columns.length; i++) {
								columns[i].setImage(null);
							}
							int sortcolumn = getOrderColumn(Integer
									.parseInt(getConfiguration().getProperty(
											CFG_ORDER, "0")));
							if (sortcolumn > -1 && sortcolumn < columns.length) {

								tree.setSortColumn(columns[sortcolumn]);

								if (getConfiguration().getProperty(
										CFG_DIRECTION, "false")
										.equalsIgnoreCase("false")) {
									tree.setSortDirection(SWT.DOWN);
								} else {
									tree.setSortDirection(SWT.UP);
								}
							}

							IAction action = getColoringAction();
							if (action != null)
								action.run();

							action = getHightlightAction();
							if (action != null)
								action.run();

							// set new title text...
							String title_ext = getTitleExtension();
							if (title_ext == null || title_ext.length() == 0)
								tree.getShell().setText(
										getI18nManager().getString(
												getNamespace(), "title",
												"label", getLanguage()));
							else
								tree.getShell().setText(
										getI18nManager().getString(
												getNamespace(), "title",
												"label", getLanguage())
												+ " - " + title_ext);

							tree.getShell().setCursor(null);
							c.dispose();
							ts.setFinished(true);

							if (lastMarkedTreeItem > -1
									&& tree.getItemCount() > 0) {
								TreeItem item = tree.getItem(Math.min(
										lastMarkedTreeItem,
										tree.getItemCount() - 1));
								if (item != null) {
									tree.setSelection(item);
									tree.showSelection();
								}
							}

							checkAmountOfEntries(start, System
									.currentTimeMillis());
						} catch (Exception e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
							if (ts != null)
								ts.setFinished(true);
							PropagationFactory.getInstance().fire(
									new Message(Message.ERROR, getNamespace(),
											"refresherror", e));
						}
					}
				}
				;

				final WorkerSWTExecuter workerThread = new WorkerSWTExecuter(
						false, getID() + "-SetDataToTableThread");

				if (getConfiguration()
						.getProperty(
								AbstractTableApplication.CFG_SHOW_REFRESH_POPUP,
								"true").equalsIgnoreCase("true")) {
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(
							getShell());
					try {
						IRunnableWithProgress r = new IRunnableWithProgress() {
							public void run(IProgressMonitor progressMonitor) {
								progressMonitor.beginTask(getI18nManager()
										.getString(getNamespace(),
												"refreshprogress", "label",
												getLanguage()),
										IProgressMonitor.UNKNOWN);

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
										m_logger.log(Level.SEVERE, e
												.getMessage(), e);
									}
								}

								progressMonitor.done();
							}
						};
						pmd.setBlockOnOpen(false);
						pmd.run(true, false, r);

						// ModalContext.run(r, true, pmd.getProgressMonitor(),
						// DisplayManager.getDefaultDisplay());
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
						+ getI18nManager().getString(getNamespace(), "items",
								"label", getLanguage());

				if (fm != null) {
					IFilter[] f = fm.getFiltersFromString(getConfiguration()
							.getProperty(CFG_FILTER, ""));

					if (f != null && f.length > 0) {
						message += getI18nManager().getString(getNamespace(),
								"items_filtered", "label", getLanguage());
					}
					getStatusLineManager().setMessage(message);
				}
			}
		}.start();
	}

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		List actions = this.getMenuBuilder().getToolbarActions();
		if (this.isShowToolbar() && actions != null && actions.size() > 0
				&& this.getToolBarManager() != null) {
			ToolBar tb = this.getToolBarManager().createControl(composite);
			for (int i = 0, j = actions.size(); i < j; i++) {
				final org.eclipse.jface.action.IAction a = (org.eclipse.jface.action.IAction) actions
						.get(i);
				if (a != null && a.getImageDescriptor() != null) {
					ToolItem ti = new ToolItem(tb, SWT.PUSH);
					Image img = (a).getImageDescriptor().createImage();
					ti.setImage(img);
					ti.setText(a.getText());
					if (a instanceof AbstractAction) {
						ti.setText(((AbstractAction) a).getShortText());
					}
					ti.setToolTipText(a.getToolTipText());
					ti.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							a.run();
						}
					});
				}
			}
		}

		addRenderBeforeTableHooks(composite);

		// add filter capability
		Composite line = new Composite(composite, SWT.NONE);
		line.setLayout(new GridLayout(6, false));
		if (this.getFilterManager() != null) {
			Composite view = new Composite(line, SWT.NONE);
			view.setLayout(new GridLayout(2, false));

			new Label(view, SWT.NONE).setText(this.getI18nManager().getString(
					this.getNamespace(), "current_view", "label",
					this.getLanguage()));
			new Label(view, SWT.NONE);
			this.currentView = new Combo(view, SWT.READ_ONLY | SWT.FLAT);
			this.currentView.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					String filterName = currentView.getItem(currentView
							.getSelectionIndex());
					IFilter[] selectedFilters = (IFilter[]) currentView
							.getData(filterName);

					getApplication().getConfiguration().setProperty(
							CFG_FILTER,
							getFilterManager().getFiltersToString(
									selectedFilters));
					getApplication().storeConfiguration();
					updateViews(true);
				}
			});

			if (getFilterAction() != null) {
				Button filterAction = new Button(view, SWT.PUSH);
				filterAction.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						de.janrufmonitor.ui.jface.application.action.IAction action = getFilterAction();
						if (action != null)
							action.run();
					}
				});
				filterAction.setImage(SWTImageManager.getInstance(
						this.getRuntime()).get(IJAMConst.IMAGE_KEY_FILTER_GIF));
				filterAction.pack();
			}
		}

		final de.janrufmonitor.ui.jface.application.action.IAction action = getQuickSearchAction();
		if (this.isShowQuickSearch() && action != null) {
			Composite view = new Composite(line, SWT.NONE);
			view.setLayout(new GridLayout(1, false));

			new Label(view, SWT.NONE).setText(this.getI18nManager().getString(
					this.getNamespace(), "quicksearch", "label",
					this.getLanguage()));

			final Combo search = new Combo(view, SWT.BORDER);
			final String empty = "<...>";

			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.minimumWidth = 100;
			search.setLayoutData(gd);

			search.add(empty);
			search.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (search.getText().indexOf(empty) == -1)
						action.setData(search.getText());
					else
						action.setData("");
					action.run();
				}
			});
			search.setToolTipText(this.getI18nManager().getString(
					this.getNamespace(), "quicksearch", "description",
					this.getLanguage()));
			search.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.character == 13) {
						if (search.getText().indexOf(empty) == -1)
							action.setData(search.getText());
						else
							action.setData("");
						search.add(search.getText());
						action.run();
					}
				}

				public void keyReleased(KeyEvent e) {
					if (search.getText().trim().length() == 0) {
						action.setData("");
						action.run();
					}
				}
			});
		}

		Tree t = new Tree(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.MULTI);
		viewer = new TreeViewer(t);

		Tree tree = ((TreeViewer) viewer).getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.addTreeListener(new TreeAdapter() {
			public void treeExpanded(TreeEvent e) {
				if (e.item != null) {
					TreeItem item = (TreeItem) e.item;
					TreeItem[] items = item.getItems();
					for (int i = 0; i < items.length; i++) {
						items[i].setBackground(item.getBackground());
						items[i].setForeground(item.getForeground());
					}
				}
			}
		});

		this.updateProviders();

		// added drag and drop feature
		if (this.getDropTargetHandler() != null) {
			int operations = DND.DROP_MOVE;
			Transfer[] types = new Transfer[] { FileTransfer.getInstance() };
			DropTarget target = new DropTarget(tree, operations);
			target.setTransfer(types);

			target.addDropListener(new DropTargetAdapter() {
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

		FontData tableFontData = tree.getFont().getFontData()[0];
		int size = Integer.parseInt(this.getConfiguration().getProperty(
				CFG_FONT_SIZE, "8"));
		tree.setFont(this.getSizedFont(tableFontData, size, false));

		AbstractMenuBuilder mb = this.getMenuBuilder();
		if (mb != null)
			tree.setMenu(mb.createPopupMenu(tree));

		tree.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					de.janrufmonitor.ui.jface.application.action.IAction action = getDeleteAction();
					if (action != null)
						action.run();
				}
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				de.janrufmonitor.ui.jface.application.action.IAction action = getAssignAction();
				if (action != null)
					action.run();
			}
		});

		// 2006/08/02: this is a workaround: No multiple line in table possibe
		Listener paintListener = new Listener() {

			int name_column = getColumnNumber("name");

			int address_column = getColumnNumber("address");

			public void handleEvent(Event event) {
				if (event.index != name_column && event.index != address_column)
					return;

				switch (event.type) {
				case SWT.MeasureItem: {
					TreeItem item = (TreeItem) event.item;
					String text = item.getText(event.index);
					Point size = event.gc.textExtent(text);
					event.width = size.x;
					event.height = Math.max(event.height, size.y + 7);
					break;
				}
				case SWT.PaintItem: {
					TreeItem item = (TreeItem) event.item;
					String text = item.getText(event.index);
					Point size = event.gc.textExtent(text);
					int offset2 = (event.index == 0 ? Math.max(0,
							(event.height - size.y) / 2) : 0) + 3;
					event.gc.drawText(text, event.x + offset2, event.y
							+ offset2, true);
					break;
				}
				case SWT.EraseItem: {
					event.detail &= ~SWT.FOREGROUND;
					TreeItem item = (TreeItem) event.item;
					event.gc.setBackground(item.getBackground(event.index));
					event.gc.setForeground(item.getForeground(event.index));

					break;
				}
				}
			}

		};
		tree.addListener(SWT.MeasureItem, paintListener);
		tree.addListener(SWT.PaintItem, paintListener);
		tree.addListener(SWT.EraseItem, paintListener);

		addRenderAfterTableHooks(composite);

		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.widget instanceof Tree) {
					TreeItem[] selectedItems = ((Tree) e.widget).getSelection();
					if (selectedItems != null && selectedItems.length > 0)
						lastMarkedTreeItem = ((Tree) e.widget)
								.indexOf(selectedItems[0]);
					else
						lastMarkedTreeItem = -1;
				}
			}
		});

		updateViews(true);

		List startupActions = this.getMenuBuilder().getStartupActions();
		if (startupActions != null && startupActions.size() > 0) {
			IAction a = null;
			for (int i = 0, j = startupActions.size(); i < j; i++) {
				a = (IAction) startupActions.get(i);
				if (a != null) {
					a.setApplication(this);
					a.run();
				}
			}
		}

		return composite;
	}

	private void buildTableColumns() {
		Tree t = ((TreeViewer) viewer).getTree();
		
		TreeColumn[] cols = t.getColumns();
		for (int i = 0; i < cols.length; i++) {
			cols[i].dispose();
		}
		int columns = this.getTableColumnCount();
		
		String id = "";
		for (int i = 0; i < columns; i++) {
			TreeViewerColumn tc = new TreeViewerColumn((TreeViewer) viewer, SWT.LEFT);
			id = getColumnID(i);
			ITableCellRenderer tr = RendererRegistry.getInstance().getRenderer(
					id);
			tc.getColumn().setText((tr != null ? tr.getHeader() : ""));
			tc.getColumn().setData(id);
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

			tc.setLabelProvider(this.getTextCellLabelProvider(id));

			int width = Integer.parseInt(getConfiguration().getProperty(
					CFG_COLUMN_SIZE + id, "50"));
			if (width > -1) {
				tc.getColumn().setWidth(width);
			}

			tc.getColumn().addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					if (e.widget instanceof TreeColumn) {
						TreeColumn tc = (TreeColumn) e.widget;
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
	}

	
	private CellLabelProvider getTextCellLabelProvider(String renderer) {
		return new TextImageCellLabelProvider(renderer);
	}

	protected abstract ITreeContentProvider getContentProvider();

}
