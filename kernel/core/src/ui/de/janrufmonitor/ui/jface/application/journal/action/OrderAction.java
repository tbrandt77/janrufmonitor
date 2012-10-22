package de.janrufmonitor.ui.jface.application.journal.action;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableColumn;

import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.journal.JournalConfigConst;

public class OrderAction extends AbstractAction implements JournalConfigConst {

	private static String NAMESPACE = "ui.jface.application.journal.action.OrderAction";
	
	private IRuntime m_runtime;

	public OrderAction() {
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
		return "journal_order";
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	public void run() {
		this.m_logger.warning("Can't execute run method in action "+this.getID());
	}

	public void runWithEvent(Event e) {
		if (e.widget instanceof TableColumn) {
			TableColumn tc = (TableColumn) e.widget;
			int ordertype = -1;
			String columnId = (String)tc.getData();
			if (columnId!=null && columnId.length()>0) {
				ordertype = this.getOrderType(columnId);
				if (ordertype>-1) {
					int oldordertype = Integer.parseInt(this.m_app.getApplication().getConfiguration().getProperty(CFG_ORDER, "-1"));
					// change order direction if neccessary
					if (oldordertype>-1 && oldordertype==ordertype) {
						boolean direction = Boolean.valueOf(
							this.m_app.getApplication().getConfiguration().getProperty(
								CFG_DIRECTION, "false"
							)	
						).booleanValue();
						this.m_app.getApplication().getConfiguration().setProperty(
							CFG_DIRECTION, (direction ? "false" : "true")
						);
					}
					this.m_app.getApplication().getConfiguration().setProperty(
						CFG_ORDER, Integer.toString(ordertype)
					);	
					this.m_app.getApplication().storeConfiguration();
					this.m_app.getController().setConfiguration(this.m_app.getApplication().getConfiguration(), false);
					this.m_app.getController().sortElements();
					this.m_app.updateViews(false);
				}
			}
		}
	}
	
	private int getOrderType(String columnId) {
		if (columnId.equalsIgnoreCase("date")) return 0;
		if (columnId.equalsIgnoreCase("msn")) return 1;
		if (columnId.equalsIgnoreCase("name") || columnId.equalsIgnoreCase("namedetail")) return 2;
		if (columnId.equalsIgnoreCase("number")) return 3;
		if (columnId.equalsIgnoreCase("cip")) return 4;
		if (columnId.equalsIgnoreCase("category")) return 8;
		
		return -1;
	}
}
