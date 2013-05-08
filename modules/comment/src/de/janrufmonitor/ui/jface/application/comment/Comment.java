package de.janrufmonitor.ui.jface.application.comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem; 

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.types.IWriteCallerRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.comment.CommentCallerHandler;
import de.janrufmonitor.service.comment.CommentService;
import de.janrufmonitor.service.comment.api.IComment;
import de.janrufmonitor.service.comment.api.ICommentCaller;
import de.janrufmonitor.ui.jface.application.AbstractMenuBuilder;
import de.janrufmonitor.ui.jface.application.AbstractTableApplication;
import de.janrufmonitor.ui.jface.application.ActionRegistry;
import de.janrufmonitor.ui.jface.application.IApplication;
import de.janrufmonitor.ui.jface.application.IApplicationController;
import de.janrufmonitor.ui.jface.application.IFilterManager;
import de.janrufmonitor.ui.jface.application.RendererRegistry;
import de.janrufmonitor.ui.jface.application.TableContentProvider;
import de.janrufmonitor.ui.jface.application.action.IAction;
import de.janrufmonitor.ui.jface.application.dnd.IDropTargetHandler;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;
import de.janrufmonitor.ui.swt.DisplayManager;

public class Comment extends AbstractTableApplication {

	private class CommentTableColorLabelContentProvider extends TableContentProvider implements ITableColorProvider {

		public CommentTableColorLabelContentProvider(Properties configuration) {
			super(configuration);
		}

		public Color getBackground(Object o, int arg1) {
			if (o instanceof IComment) {
				IAttribute a = ((IComment)o).getAttributes().get(IComment.COMMENT_ATTRIBUTE_FOLLOWUP);
				if (a!=null && a.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_YES)) {
					String[] color = getRuntime().getConfigManagerFactory().getConfigManager().getProperty("service.CommentService","followupcolor").split(",");
					if (color.length==3)
						return new Color(
							DisplayManager.getDefaultDisplay(),
							Integer.parseInt(color[0]),
							Integer.parseInt(color[1]),
							Integer.parseInt(color[2])
						);
				}
			}
			return null;
		}

		public Color getForeground(Object arg0, int arg1) {
			return null;
		}
		
	}
	
	private class CommentMenuBuilder extends AbstractMenuBuilder {

		private IRuntime m_runtime;
		
		public CommentMenuBuilder(IApplication app, List l, List popupActions) {
			super(app, l, popupActions);
		}

		public MenuManager createMenu() {
			MenuManager master = new MenuManager();
			// create view menu
			MenuManager view = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"view",
					"label",
					this.getLanguage()
				)
			);
			master.add(view);
			this.addAction(view, "comment_new");
			this.addSeparator(view);
			this.addAction(view, "comment_import");
			this.addSeparator(view);
			this.addAction(view, "refresh");
			this.addAction(view, "delete_all");
			this.addSeparator(view);
			this.addAction(view, "search");
			this.addSeparator(view);
			this.addAction(view, "close");
			
			// create ? menu
			MenuManager q = new MenuManager(
				this.getI18nManager().getString(
					this.m_app.getNamespace(),
					"q",
					"label",
					this.getLanguage()
				)
			);
			master.add(q);
			this.addAction(q, "help");
			
			return master;
		}
		
		public IRuntime getRuntime() {
			if (this.m_runtime==null) {
				this.m_runtime = PIMRuntime.getInstance();
			}
			return this.m_runtime;
		}

		public Menu createPopupMenu(Control c) {
			MenuManager master = new MenuManager();

			this.addAction(master, "comment_new");
			this.addAction(master, "comment_change");
			this.addAction(master, "comment_delete");
			this.addSeparator(master);
			this.addAction(master, "comment_export");
			this.addAction(master, "comment_pdfhistory");
			this.addSeparator(master);
			this.addAction(master, "showgrid");
			this.addAction(master, "zoomin");
			this.addAction(master, "zoomout");
			
			return master.createContextMenu(c);
		}

	}

	private class CommentComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			IComment c1 = (IComment)o1;
			IComment c2 = (IComment)o2;
			return c2.getDate().compareTo(c1.getDate());
		}
	}

	private class CommentController implements IApplicationController {

		private ICaller m_currentCaller;
		private List m_data;
		
		public CommentController(ICaller c){
			this.m_currentCaller = c;
		}
		
		public void setConfiguration(Properties configuration, boolean initialize) {

		}

		public Object[] getElementArray() {
			if (this.m_data==null) 
				this.buildControllerData();
			
			return this.m_data.toArray();
		}


		public void deleteAllElements() {
			if (this.m_data==null) 
				this.buildControllerData();
			
			IService srv = getRuntime().getServiceFactory().getService("CommentService");
			if (srv!=null && srv instanceof CommentService) {
				CommentCallerHandler cch = ((CommentService)srv).getHandler();
				if (cch.hasCommentCaller(this.m_currentCaller)) {
					ICommentCaller cc = cch.getCommentCaller(this.m_currentCaller);
					cc.removeComments();
					cch.setCommentCaller(cc);
					this.m_data = null;
				}
			}
		}

		public void deleteElements(Object list) {
			if (this.m_data==null) 
				this.buildControllerData();
			
			IService srv = getRuntime().getServiceFactory().getService("CommentService");
			if (srv!=null && srv instanceof CommentService) {
				CommentCallerHandler cch = ((CommentService)srv).getHandler();
				if (cch.hasCommentCaller(this.m_currentCaller)) {
					ICommentCaller cc = cch.getCommentCaller(this.m_currentCaller);
					List l = (List)list;
					for (int i=0;i<l.size();i++) {
						cc.removeComment(((IComment)l.get(i)).getID());
					}
					cch.setCommentCaller(cc);
					this.m_data = null;
				}
			}	
		}

		public void addElements(Object list) {
			if (this.m_data==null) 
				this.buildControllerData();
			
			IService srv = getRuntime().getServiceFactory().getService("CommentService");
			if (srv!=null && srv instanceof CommentService) {
				CommentCallerHandler cch = ((CommentService)srv).getHandler();
				if (cch.hasCommentCaller(this.m_currentCaller)) {
					ICommentCaller cc = cch.getCommentCaller(this.m_currentCaller);
					List l = (List)list;
					for (int i=0;i<l.size();i++) {
						cc.addComment((IComment)l.get(i));
					}
					cch.setCommentCaller(cc);
					this.m_data = null;
				} else {
					ICommentCaller cc = cch.createCommentCaller(this.m_currentCaller);
					List l = (List)list;
					for (int i=0;i<l.size();i++) {
						cc.addComment((IComment)l.get(i));
					}
					cch.setCommentCaller(cc);
					this.m_data = null;
				}
			}			
			
		}

		public void updateElement(Object element) {
			if (this.m_data==null) 
				this.buildControllerData();
			
			IService srv = getRuntime().getServiceFactory().getService("CommentService");
			if (srv!=null && srv instanceof CommentService) {
				CommentCallerHandler cch = ((CommentService)srv).getHandler();
				if (cch.hasCommentCaller(this.m_currentCaller)) {
					ICommentCaller cc = cch.getCommentCaller(this.m_currentCaller);
					cc.removeComment(((IComment)element).getID());
					cc.addComment((IComment)element);
					cch.setCommentCaller(cc);
					this.m_data = null;
				} else {
					ICommentCaller cc = cch.createCommentCaller(this.m_currentCaller);
					cc.addComment((IComment)element);
					cch.setCommentCaller(cc);
					this.m_data = null;
				}
			}
		}

		public int countElements() {
			if (this.m_data==null) 
				this.buildControllerData();
			
			return this.m_data.size();
		}

		public void sortElements() {
			if (this.m_data==null) 
				this.buildControllerData();
			
			Collections.sort(this.m_data, new CommentComparator());
		}
		
		private void buildControllerData() {
			this.m_data = new ArrayList();
			IService srv = getRuntime().getServiceFactory().getService("CommentService");
			if (srv!=null && srv instanceof CommentService) {
				CommentCallerHandler cch = ((CommentService)srv).getHandler();
				if (cch.hasCommentCaller(this.m_currentCaller)) {
					ICommentCaller cc = cch.getCommentCaller(this.m_currentCaller);
					this.m_data.addAll(cc.getComments());
					Collections.sort(this.m_data, new CommentComparator());
				}
			}
		}

		public void generateElementArray(Object[] data) {

		}

		public Object getRepository() {
			// this is a work-a-round for teh DeleteAllCommand
			return new IWriteCallerRepository () {
				public void removeCaller(ICaller caller) {
				}

				public void removeCaller(ICallerList callerList) {
				}

				public void setCaller(ICaller caller) {
				}

				public void setCaller(ICallerList callerList) {
				}

				public void updateCaller(ICaller caller) {
				}
				
			};
		}
		
	}
	
	public static String NAMESPACE = "ui.jface.application.comment.Comment";

	private IRuntime m_runtime;
	private AbstractMenuBuilder m_mb;
	private CommentTableColorLabelContentProvider m_jp;
	private ICaller m_currentCaller;
	private ICall m_currentCall;
	
	public Comment(ICall c) {
		this(c.getCaller());
		this.m_currentCall = c;
	}
	
	public Comment(ICaller c) {
		super();
		this.m_currentCaller = c;
	}
	
	public Comment() {
		this((ICaller)null);
	}
	
	protected void addRenderBeforeTableHooks(Composite parent) {
		Composite view = new Composite(parent, SWT.NONE);
		view.setLayout(new GridLayout(2, false));
		
		Label l = new Label(view, SWT.NULL);
		l.setText("     ");
		
		l = new Label(view, SWT.NULL);
		l.setFont(this.getBoldFont(l.getFont()));
		
		String msg = "";
		
		ITableCellRenderer tcr = RendererRegistry.getInstance().getRenderer("name");
		if (tcr!=null) {
			tcr.updateData(getCurrentCaller());
			msg += tcr.renderAsText();
		}
		tcr = RendererRegistry.getInstance().getRenderer("number");
		if (tcr!=null) {
			tcr.updateData(getCurrentCaller());
			msg += "\n\n"+tcr.renderAsText()+"\n";
		}
		
		l.setText(msg);
	}
	
	private Font getBoldFont(Font f) {
		FontData[] fd = f.getFontData();
		if (fd==null || fd.length==0) return f;
		
		for (int i=0;i<fd.length;i++) {
			fd[i].setStyle(SWT.BOLD);
		}
		f = new Font(DisplayManager.getDefaultDisplay(), fd);		
		return f;
	}
	
	public ICaller getCurrentCaller() {
		if (this.m_currentCall==null) return this.m_currentCaller;
		return this.m_currentCall.getCaller();
	}
	
	public Object getCallableObject() {
		return (this.m_currentCall==null ? (Object)this.m_currentCaller : (Object)this.m_currentCall);
	}

	protected void addRenderAfterTableHooks(Composite parent) {
		Table table = ((TableViewer)this.getViewer()).getTable();
		// 2006/08/02: this is a workaraound: No multiple line in table possibe
		Listener paintListener = new Listener() {
			int name_column = getColumnNumber("comment"); 
			public void handleEvent(Event event) {
				if (event.index!=name_column) return;
				
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
	}
	
	protected AbstractMenuBuilder getMenuBuilder() {
		if (this.m_mb==null)
			this.m_mb = new CommentMenuBuilder(this, this.getAdditionalMenuActions(), this.getAdditionalPopupActions());
		return m_mb;
	}

	protected IFilterManager getFilterManager() {
		return null;
	}

	protected IDropTargetHandler getDropTargetHandler() {
		return null;
	}

	protected IStructuredContentProvider getContentProvider() {
		if (this.m_jp==null)
			this.m_jp = new CommentTableColorLabelContentProvider(this.getConfiguration());
		return this.m_jp;
	}

	protected void initializeProviders() {
		if (this.m_jp!=null) this.m_jp.dispose();
		this.m_jp = null;
	}

	protected void createApplicationController() {
		if (this.m_controller==null) // added 2006/06/28: refresh performance problem
			this.m_controller = new CommentController(getCurrentCaller());
	}

	protected IAction getFilterAction() {
		return null;
	}

	protected IAction getAssignAction() {
		return ActionRegistry.getInstance().getAction("comment_change", getApplication());
	}

	protected IAction getDeleteAction() {
		return null;
	}

	protected IAction getColoringAction() {
		return null;
	}

	protected IAction getOrderAction() {
		return null;
	}

	protected IAction getHightlightAction() {
		return null;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public String getID() {
		return "Comment";
	}

	protected IAction getQuickSearchAction() {
		return null;
	}


}
