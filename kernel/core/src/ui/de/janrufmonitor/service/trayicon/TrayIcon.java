
package de.janrufmonitor.service.trayicon;

import java.util.*;
import java.util.logging.Level;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import de.janrufmonitor.exception.IPropagator;
import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.OSUtils;

public class TrayIcon extends AbstractReceiverConfigurableService implements Comparator {

	private static String ID = "TrayIcon";
	private static String NAMESPACE = "service.TrayIcon";

	private String CFG_VISIBLETHREAD = "visiblethread";
	private String CFG_FLATMENU = "flatmenu";
	private IRuntime m_runtime;
	private String m_language;
	private II18nManager m_i18n;
	private IPropagator m_trayPropagator;
	
	// SWT
	private Menu menu;
	private Map menuItemMap;
	private TrayItem trayItem;
	private ActionHandler m_ah;
	private IconVisibleThread ivt;
	private Map m_iconPool;
		
	private class IconVisibleThread extends Thread {
		
		private boolean isRunning;
		
		public IconVisibleThread() {
			this.setName("JAM-IconVisibleCheck-Thread-(non-deamon)");
		}
		
		public void run() {
			while(this.isRunning && OSUtils.isWindows()) {
				if (menu!=null && trayItem!=null) {
					m_logger.fine("Menu is available.");
					new SWTExecuter("IconVisibleThread") {
						protected void execute() {
							if (trayItem==null || trayItem.isDisposed ()) return;
							trayItem.setVisible(false);
							trayItem.setVisible(true);
							// added: 31/10/2004: due to double icon appearance
							if (trayItem.getVisible())
								IconVisibleThread.this.isRunning = false;
						}	
					}.start();
					
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						m_logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
		
		public void setRunning(boolean b) {
			this.isRunning = b;
		}
		
	}
	
	private class ActionHandler implements Listener {
		
		private class SWTHandlerThread extends SWTExecuter {
			private String c;
			
			public SWTHandlerThread(String command) {
				super("HandlerThread#"+command);
				this.c = command;
			}
			
			protected void execute() {
				ICommand cmd = getRuntime().getCommandFactory().getCommand(c);
				
				if (cmd != null) {
					if (cmd.isExecutable() && !cmd.isExecuting()) {
						try {
							cmd.execute();
						} catch (Exception e) {
							m_logger.severe(e.getMessage());
							PropagationFactory.getInstance().fire(new Message(e));
						}
					}
				} else {
					m_logger.warning("Command <"+c+"> not available.");
				}
			}
		}

		private void execute(String e) {
			m_logger.info("Executing <"+e+"> command.");

			if (e==null || e.length()==0)
				return;
				
			if (e.equalsIgnoreCase("exit")) {
				try {
					new SWTExecuter("DisposeDisplay") {
						protected void execute() {
							DisplayManager.dispose();
						}	
					}.start();
					
					getRuntime().shutdown();
				} catch (Exception ex) {
					System.out.println("Errors on jAnrufmonitor shutdown : " + ex);
				}
				
				System.exit(0);
				return;
			}
				
			new SWTHandlerThread(e).start();
		}

		public void handleEvent(Event event) {
			if (event.widget instanceof MenuItem) {
				MenuItem m = (MenuItem)event.widget;
				String command = (String) m.getData("command");
				this.execute(command);
			}
			if (event.widget instanceof TrayItem) {
				this.execute(m_configuration.getProperty("doubleclick_component", ""));
			}
		}
	}
	
	public TrayIcon() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
		
		// initialize display manager
		DisplayManager.getDefaultDisplay();
	}

	public String getNamespace() {
		return TrayIcon.NAMESPACE;
	}

	public String getID() {
		return TrayIcon.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public void setConfiguration(Properties configuration) {
		this.m_i18n = this.getRuntime().getI18nManagerFactory().getI18nManager();
		this.m_language = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_LANGUAGE);		
		super.setConfiguration(configuration);	
	}

	public void startup() {
		super.startup();

		// register as a receiver
		this.getRuntime().getEventBroker().register(this, this.getRuntime().getEventBroker().createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL)); 
		this.getRuntime().getEventBroker().register(this, this.getRuntime().getEventBroker().createEvent(IEventConst.EVENT_TYPE_APPLICATION_READY)); 
		
		this.m_iconPool = new HashMap();
		
		new SWTExecuter(this.getID()) {
			protected void execute() {
				m_logger.info("Creating new tray ...");
				createTray();
				// set default wizard icons
				WizardDialog.setDefaultImage(SWTImageManager.getInstance(getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
			}
		}.start();

		boolean isTrayitemEnabled = Boolean.parseBoolean(System.getProperty(IJAMConst.SYSTEM_UI_TRAYITEM, "true"));
		if (isTrayitemEnabled) {
			this.m_trayPropagator = new TrayIconPropagator();
			PropagationFactory.getInstance().add(this.m_trayPropagator);
		}
		this.m_logger.info("Running new TrayCreator Thread.");
	}

	public void shutdown() {
		super.shutdown();
		
		if (this.m_trayPropagator!=null)
			PropagationFactory.getInstance().remove(this.m_trayPropagator);
		
		this.m_iconPool.clear();
		
		try {
			if (trayItem!=null && !trayItem.isDisposed ()) {
				DisplayManager.getDefaultDisplay().syncExec (new Runnable () {
					public void run () {
						if (ivt!=null)
							ivt.setRunning(false);
						
						if (menu!=null)
							menu.dispose();
						menu=null;

						if (trayItem!=null)
							trayItem.dispose();
						trayItem=null;
						
						ivt = null;
					}
				});
			}
		} catch (Throwable e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}

		// unregister as a receiver
		this.getRuntime().getEventBroker().unregister(this, this.getRuntime().getEventBroker().createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		this.getRuntime().getEventBroker().unregister(this, this.getRuntime().getEventBroker().createEvent(IEventConst.EVENT_TYPE_APPLICATION_READY));
	}
	
	public void setIconStateError() {
		new SWTExecuter() {
			protected void execute() {
				if (trayItem==null || trayItem.isDisposed ()) return;
				if (OSUtils.isWindows())
					trayItem.setVisible(false);
				trayItem.setImage(SWTImageManager.getInstance(getRuntime()).get(IJAMConst.IMAGE_KEY_PIMI_ICON));
				if (OSUtils.isWindows())
					trayItem.setVisible(true);				
			}
		}.start();
	}
	
	
	public void setIconStateMonitorListener() {
		if (getRuntime().getMonitorListener().getDefaultMonitor()==null) return;
		
		final boolean started = getRuntime().getMonitorListener().getDefaultMonitor().isStarted();
		
		new SWTExecuter() {
			protected void execute() {
				if (trayItem==null || trayItem.isDisposed ()) return;
				if (OSUtils.isWindows())
					trayItem.setVisible(false);
				trayItem.setImage(SWTImageManager.getInstance(getRuntime()).get((started ? IJAMConst.IMAGE_KEY_PIM_ICON : IJAMConst.IMAGE_KEY_PIMX_ICON)));
				if (OSUtils.isWindows())
					trayItem.setVisible(true);				
			}
		}.start();
		setItemChecked("Activator", started);
	}

	public void invertImage() {	
		new SWTExecuter() {
			protected void execute() {
				if (trayItem==null || trayItem.isDisposed ()) return;
				if (OSUtils.isWindows())
					trayItem.setVisible(false);
				trayItem.setImage(SWTImageManager.getInstance(getRuntime()).get(IJAMConst.IMAGE_KEY_PIMX_ICON));
				if (OSUtils.isWindows())
					trayItem.setVisible(true);
			}
		}.start();
	}
	
	public void revertImage() {
		new SWTExecuter() {
			protected void execute() {
				if (trayItem==null || trayItem.isDisposed ()) return;
				if (OSUtils.isWindows())
					trayItem.setVisible(false);
				trayItem.setImage(SWTImageManager.getInstance(getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
				if (OSUtils.isWindows())
					trayItem.setVisible(true);
			}
		}.start();
	}
	
	public void receivedValidRule(final ICall aCall) {		
		setToolTipText(getToolTipText(aCall));
	}
	
	public void setItemEnabled(String itemid, final boolean enabled) {
		final MenuItem m = this.findItem(itemid);
		if (m!=null) {
			new SWTExecuter() {
				protected void execute() {
					if (m==null || m.isDisposed ()) return;
					m.setEnabled(enabled);
				}
			}.start();
		}
	}
	
	public void toggleImage(final String imagepath) {
		new SWTExecuter() {
			protected void execute() {
				Image img = null;
				if (m_iconPool.containsKey(imagepath)) {
					img = (Image)m_iconPool.get(imagepath);
				} else {
					img = new Image(DisplayManager.getDefaultDisplay(), imagepath);
					m_iconPool.put(imagepath, img);
				}
				if (trayItem==null || trayItem.isDisposed ()) return;						
					trayItem.setImage(img);
			}
		}.start();		
	}
	
	public void toggleImage(final Image image) {
		new SWTExecuter() {
			protected void execute() {
				if (trayItem==null || trayItem.isDisposed ()) return;						
				trayItem.setImage(image);
			}
		}.start();
	}
	
	public void setItemChecked(String itemid, final boolean enabled) {
		final MenuItem m = this.findItem(itemid);
		if (m!=null) {
			new SWTExecuter() {
				protected void execute() {
					if (m.isDisposed ()) return;
					m.setSelection(enabled);
				}
			}.start();
		}
	}
	
	// Comparator Interface
	public int compare(Object o1, Object o2) {
		String prop1 = (String) o1;
		String prop2 = (String) o2;
		
		try {
			int prop1Pos = new Integer(this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), prop1, "position")).intValue();
			int prop2Pos = new Integer(this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), prop2, "position")).intValue();
			
			if (prop1Pos<prop2Pos) {
				return -1;
			}
			if (prop1Pos>prop2Pos) {
				return 1;
			}
			return 0;
		} catch (Exception ex) { 
			this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
			
		return 0;
	}
	
	private MenuItem findItem(String id) {
		if (this.menuItemMap!=null && !this.menuItemMap.isEmpty()) {
			return (MenuItem)this.menuItemMap.get(id);
		}
		return null;
	}
	
	private String getToolTipText(ICall aCall) {
		// added: 26/06/2004, modified 12/07/2004:
		// cutting tooltip to 128 chars, otherwise
		// menu fails to open.
		if (aCall!=null && aCall.getCaller().getPhoneNumber().isClired()) {
			return this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_CLIR);
		}

		Formatter formatter = Formatter.getInstance(this.getRuntime());

		String p = "\r\n" + formatter.parse(IJAMConst.GLOBAL_VARIABLE_CALLERNUMBER, aCall);
		
		// cut tooltips > 128
		if (p.length()>128)
			return p.substring(2, Math.max(132, p.length())).trim();
			
		String name = formatter.parse(IJAMConst.GLOBAL_VARIABLE_CALLERNAME, aCall);
		if ((name.length() + p.length())>128) {
			name = name.substring(0, (127-3-p.length())) + "...";
		}
		return name + p;
	}

	private boolean isVisibleThreadActive() {
		return this.m_configuration.getProperty(CFG_VISIBLETHREAD, "false").equalsIgnoreCase("true");
	}
	
	private List getSortedItems(String component) {
		List items = new ArrayList();
		Iterator propsIter = this.m_configuration.keySet().iterator();

		while(propsIter.hasNext()) {
			String propname = (String)propsIter.next();
			if (propname.startsWith(component)) {
				items.add(propname);
			}
		}

		Collections.sort(items,this);
		return items;
	}
	
	private boolean isFlatHierarchy() {
		return this.m_configuration.getProperty(this.CFG_FLATMENU, "false").equalsIgnoreCase("true");
	}
	
	private void createMenu() {
		this.menuItemMap = new HashMap();

		Shell shell = new Shell(DisplayManager.getDefaultDisplay());
		menu = new Menu(shell, SWT.POP_UP);

		MenuItem item = null;
			
		// get component list from configuration
		List items = this.getSortedItems("component_");
		        
		for (int i=0;i<items.size();i++) {
			String singleItem = (String)items.get(i);
			String component = this.m_configuration.getProperty(singleItem, "");
			if (component.equalsIgnoreCase("separator")) {
				item = new MenuItem(menu, SWT.SEPARATOR);
			} else if(component.equalsIgnoreCase("submenu")) {
				List subItems = this.getSortedItems("sub_" + singleItem);
				if (subItems.size()>1) {
					String menuTitle = this.m_configuration.getProperty((String)subItems.get(subItems.size()-1),"");
					menuTitle = this.m_i18n.getString(this.getNamespace(), menuTitle, "label", this.m_language);
					
					// flatten hierarchy
					if (this.isFlatHierarchy()) {
						for (int j=0;j<subItems.size()-1;j++) {
							String singleSubItem = (String)subItems.get(j);
							String subComponent = this.m_configuration.getProperty(singleSubItem, "");

							this.createItem(menu, subComponent, this.isItemCheckable(singleSubItem));
						}
					} else {
						item = new MenuItem (menu, SWT.CASCADE);
						item.setText(this.m_i18n.getString(this.getNamespace(), (String)subItems.get(0), "label", this.m_language));
						
						Menu subMenu = new Menu (shell, SWT.DROP_DOWN);
						item.setMenu(subMenu);
						
						for (int j=1;j<subItems.size();j++) {
							String singleSubItem = (String)subItems.get(j);
							String subComponent = this.m_configuration.getProperty(singleSubItem, "");
							this.createItem(subMenu, subComponent, this.isItemCheckable(singleSubItem));
						}
					}
				}
			} else {
				this.createItem(menu, component, this.isItemCheckable(singleItem));
			}
		} 
			
		MenuItem exitItem = new MenuItem(menu, SWT.PUSH);
		exitItem.setText(this.m_i18n.getString(this.getNamespace(), "exit", "label", this.m_language)); 
		exitItem.addListener(SWT.Selection, this.m_ah);
		exitItem.setData("command", "exit");
		
		this.trayItem.addListener (SWT.MenuDetect, new Listener () {
			public void handleEvent (Event event) {
				menu.setVisible(true);
			}
		});
		
		if (OSUtils.isWindows() && (this.ivt==null || !this.ivt.isAlive())){
			this.ivt = new IconVisibleThread();
			this.ivt.setRunning(this.isVisibleThreadActive());
			this.ivt.start();
		}
	}
	
	private boolean isItemCheckable(String item) {
		return (this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(this.getNamespace(), item, "type").equalsIgnoreCase("checked") ? true : false);
	}
	
	private void createItem(Menu menu, String actionCommand, boolean checkable) {
		MenuItem item = null;
		ICommand c = this.getRuntime().getCommandFactory().getCommand(actionCommand);
		String title = actionCommand;
		
		if (c!=null) {
			title = c.getLabel();
		}
		
		if (checkable) {
			item = new MenuItem(menu, SWT.CHECK);
			item.setSelection(true);	
		} else {
			item = new MenuItem(menu, SWT.PUSH);
		}
		item.setData("command", actionCommand);
		item.setText(title);
		
		if (actionCommand.length()==0) {
			item.setEnabled(false);
		} else {
			item.addListener(SWT.Selection, this.m_ah);
			item.setEnabled((c!=null ? c.isExecutable() : false));
		}
		
		this.menuItemMap.put(actionCommand, item);
	}

	private void createTray() {
		this.m_logger.entering(TrayIcon.class.getName(), "createTray");
		try {
			// set action handler
			this.m_logger.info("Creating new action handler...");
			m_ah = new ActionHandler();
			
			// create trayItem
			if (this.trayItem==null) {
				this.m_logger.info("Creating tray icon...");
				createTrayIcon();
			}
				
			if (this.menu==null) {
				this.m_logger.info("Creating popup menu...");
				createMenu();				
			}
		} catch (Throwable t) {
			this.m_logger.severe(t.getMessage());
		}
		this.m_logger.exiting(TrayIcon.class.getName(), "createTray");
	}
	
	private void createTrayIcon() {
		this.m_logger.entering(TrayIcon.class.getName(), "createTrayIcon");
	
		this.m_logger.info("Getting system tray ...");
		Tray tray = DisplayManager.getDefaultDisplay().getSystemTray();
		if (tray!=null) {
			this.trayItem = new TrayItem(tray, SWT.NULL);
			if (OSUtils.isWindows())
				this.trayItem.setVisible(false);
			this.trayItem.setText("jAnrufmonitor");
			this.trayItem.setImage(SWTImageManager.getInstance(this.getRuntime()).get(IJAMConst.IMAGE_KEY_PIM_ICON));
			this.trayItem.setToolTipText(this.m_i18n.getString(this.getNamespace(), "tooltip", "label", this.m_language));
			this.trayItem.addListener(SWT.Selection,
				this.m_ah
			);
			
			TrayItem[] ti = tray.getItems();
			for (int i=0;i<ti.length;i++) {
				this.m_logger.info("Tray icon <"+ti[i].getText()+"> successfully installed.");
			}
			//tip.setVisible(true);
			
		} else {
			this.m_logger.log(Level.SEVERE, "No system tray icon found.");
		}
		this.m_logger.exiting(TrayIcon.class.getName(), "createTrayIcon");
	}
	
	public void setToolTipText(final String s) {
		new SWTExecuter() {
			protected void execute() {
				if (trayItem==null || trayItem.isDisposed ()) return;
				trayItem.setToolTipText(
					s
				);
			}
		}.start();	
	}
	
	public void setToolTip(final String message, final String title, final int status) {
		new SWTExecuter() {
			protected void execute() {
				if (trayItem==null || trayItem.isDisposed ()) return;

				ToolTip tip = new ToolTip(new Shell(DisplayManager.getDefaultDisplay()), SWT.BALLOON | status);
				trayItem.setToolTip(tip);
				tip.setText(title);
				tip.setMessage(message);
				tip.setAutoHide(true);
				tip.setVisible(true);
			}
		}.start();	
	}

	public void receivedOtherEventCall(IEvent event) {
		if (event.getType()==IEventConst.EVENT_TYPE_APPLICATION_READY) {
			if (this.menu!=null) {
				new SWTExecuter() {
					protected void execute() {
						MenuItem[] items = menu.getItems();
						MenuItem item = null;
						for (int i=0;i<items.length;i++) {
							item = items[i];
							if (item!=null) {
								String command = (String) item.getData("command");
								if (command!=null && command.length()>0 && !command.equalsIgnoreCase("exit")) {
									ICommand c = getRuntime().getCommandFactory().getCommand(command);
									if (c!=null) {
										item.setEnabled(c.isExecutable());
									}
								}
							}
						}
					}
				}.start();
			}
		}
		super.receivedOtherEventCall(event);
	}


}
