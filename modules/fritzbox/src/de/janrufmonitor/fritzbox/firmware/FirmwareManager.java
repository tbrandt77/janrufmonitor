package de.janrufmonitor.fritzbox.firmware;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.command.ICommand;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.framework.event.IEventSender;
import de.janrufmonitor.framework.monitor.IMonitorListener;
import de.janrufmonitor.fritzbox.FritzBoxConst;
import de.janrufmonitor.fritzbox.FritzBoxMonitor;
import de.janrufmonitor.fritzbox.IPhonebookEntry;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallerException;
import de.janrufmonitor.fritzbox.firmware.exception.DoBlockException;
import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxInitializationException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxNotFoundException;
import de.janrufmonitor.fritzbox.firmware.exception.GetAddressbooksException;
import de.janrufmonitor.fritzbox.firmware.exception.GetBlockedListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerImageException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.fritzbox.firmware.exception.InvalidSessionIDException;
import de.janrufmonitor.fritzbox.firmware.exception.SetCallerException;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class FirmwareManager implements IEventReceiver, IEventSender {

	private static FirmwareManager m_instance = null;
	private Logger m_logger;
	private IRuntime m_runtime;
	
	private IFritzBoxFirmware m_fw;
	private Thread m_timeoutThread;
	private Thread m_restartedThread;
	private IEventBroker m_broker;
	
	private boolean m_isReconnecting = false;
	private boolean m_isRunning = false;
	private boolean m_isCreatingFirmware = false;
	private boolean m_isLoggingIn = false;
	private int m_retryCount = 0;
    
    private FirmwareManager() {
        this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
        this.m_broker = getRuntime().getEventBroker();
        this.m_broker.register(this);
		this.m_broker.register(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_RESTARTED));
		this.m_broker.register(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_NETWORK_UNAVAILABLE));
		this.m_broker.register(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
		this.m_broker.register(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_CONNECTION_LOST));
		this.m_broker.register(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_RECONNECTED_SUCCESS));
		this.m_broker.register(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNSUPPORTED));
		this.m_broker.register(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_REFUSED));
    }
    
    public static synchronized FirmwareManager getInstance() {
        if (FirmwareManager.m_instance == null) {
        	FirmwareManager.m_instance = new FirmwareManager();
        }
        return FirmwareManager.m_instance;
    }
    
    public boolean isLoggedIn() {
    	if (this.m_fw==null) {
    		System.setProperty("jam.fritzbox.session.password", "");
        	System.setProperty("jam.fritzbox.session.counter", "0");
        	System.setProperty("jam.fritzbox.session.ispwdialogvisible", "false");
    	}
    	return (this.m_fw!=null);
    }
    
    public void startup() {
    	if (this.m_isRunning) return;
    	
    	System.getProperties().setProperty("jam.fritzbox.tr064off", this.getFritzBoxTR064Off() ? "true": "false");
    	System.setProperty("jam.fritzbox.session.donotlogin", "false");

		this.m_isRunning = true;
    }
    
    public boolean isInstance(Class c) {
    	return c.isInstance(this.m_fw);
    }
    
    public void login() throws FritzBoxLoginException {
    	boolean dnl = Boolean.parseBoolean(System.getProperty("jam.fritzbox.session.donotlogin", "false"));
    	if (dnl) return;
    	
    	while (this.m_isLoggingIn) {
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
    	}
    	this.m_isLoggingIn = true;
		if (this.m_fw==null){ 
			this.promptPassword();
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				this.m_fw = null;
				if (e.isUnsupportedFirmware()) {
					if (this.m_broker!=null)
						this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNSUPPORTED));
				}
				throw new FritzBoxLoginException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				this.m_fw = null;
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new FritzBoxLoginException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				this.m_fw = null;
				if (e.getMessage().indexOf("user/password combination")>0) {
					System.setProperty("jam.fritzbox.session.password", "");
					if (this.getFritzBoxPassword().trim().length()==0) {
						this.m_isLoggingIn = false;
						this.login();
						return;
					}
				}
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new FritzBoxLoginException(e.getMessage()); 
			} finally {
				this.m_isLoggingIn = false;
			}
		}
		this.m_isLoggingIn = false;
		if (this.m_fw==null) throw new FritzBoxLoginException("Login failed due to invalid firmware.");
		this.m_fw.login();
		if (this.m_broker!=null)
			this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_RECONNECTED_SUCCESS));
	}

	public String getFirmwareDescription() throws FritzBoxLoginException {
    	if (this.m_fw==null)
			return "";
		return this.m_fw.toString();
    }
    
    public String getMSNFromSIP(String idx) throws IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
			}
		return (this.m_fw == null ? null : this.m_fw.getMSNFromSIP(idx));
    }
    
    public Map getMSNMap() throws IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
			}
		return this.m_fw.getMSNMap();
    }
    
    public String getTamMessage(String url) throws IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
			}
		return this.m_fw.getTamMessage(url);
    }
    
    public Map getTamMessages(long lastSyncTimestamp) throws IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
			}
		return this.m_fw.getTamMessages(lastSyncTimestamp);
    }
    
    public List getCallList(long lastSyncTimestamp) throws GetCallListException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new GetCallListException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new GetCallListException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new GetCallListException(e.getMessage());
			}
		return this.m_fw.getCallList(lastSyncTimestamp);
    }
    
    public void deleteCaller(int id, String entryID) throws DeleteCallerException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new DeleteCallerException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new DeleteCallerException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new DeleteCallerException(e.getMessage());
			}
		this.m_fw.deleteCaller(id, entryID);
    }
    
    public void deleteCaller(int id, IPhonebookEntry pe) throws DeleteCallerException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new DeleteCallerException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new DeleteCallerException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new DeleteCallerException(e.getMessage());
			}
		this.m_fw.deleteCaller(id, pe);
    }
    
    public void setCaller(int id, IPhonebookEntry pe) throws SetCallerException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new SetCallerException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new SetCallerException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new SetCallerException(e.getMessage());
			}
		this.m_fw.setCaller(id, pe);
    }
    
    public String getCallerImage(String path) throws GetCallerImageException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new GetCallerImageException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new GetCallerImageException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new GetCallerImageException(e.getMessage());
			}
		return this.m_fw.getCallerImage(path);
    }
    
    public List getCallerList() throws GetCallerListException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new GetCallerListException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new GetCallerListException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new GetCallerListException(e.getMessage());
			}
		return this.m_fw.getCallerList();
    }
    
    public List getCallerList(int id, String name) throws GetCallerListException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new GetCallerListException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new GetCallerListException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new GetCallerListException(e.getMessage());
			}
		return this.m_fw.getCallerList(id, name);
    }
    
    public Map getAddressbooks() throws GetAddressbooksException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new GetAddressbooksException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new GetAddressbooksException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new GetAddressbooksException(e.getMessage());
			}
		return this.m_fw.getAddressbooks();
    }
    
    public String getAddressbookModificationHash(int addressbookId) throws GetAddressbooksException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new GetAddressbooksException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new GetAddressbooksException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new GetAddressbooksException(e.getMessage());
			}
		return this.m_fw.getAddressbookModificationHash(addressbookId);
    }
    
    public List getBlockedList() throws GetBlockedListException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new GetBlockedListException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new GetBlockedListException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new GetBlockedListException(e.getMessage());
			}
		return this.m_fw.getBlockedList();
    }
    
    public void deleteCallList() throws DeleteCallListException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new DeleteCallListException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new DeleteCallListException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new DeleteCallListException(e.getMessage());
			}
		this.m_fw.deleteCallList();
    }
    
    public void doBlock(String number) throws DoBlockException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new DoBlockException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new DoBlockException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new DoBlockException(e.getMessage());
			}
		this.m_fw.doBlock(number);
    }
	
    public void doCall(String number, String extension) throws DoCallException, IOException {
    	if (this.m_fw==null)
			try {
				this.createFirmwareInstance();
			} catch (FritzBoxInitializationException e) {
				throw new DoCallException(e.getMessage());
			} catch (FritzBoxNotFoundException e) {
				if (this.m_broker!=null)
					this.m_broker.send(this, this.m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST));
				throw new DoCallException(e.getMessage());
			} catch (InvalidSessionIDException e) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						e,
						true));
				throw new DoCallException(e.getMessage());
			}
		this.m_fw.doCall(number, extension);
    }

    
    public void shutdown() {
		if (m_restartedThread!=null && m_restartedThread.isAlive()){
			m_restartedThread.interrupt();
		}
		
    	if (m_timeoutThread!=null && m_timeoutThread.isAlive()) {
    		m_timeoutThread.interrupt();
    	}
    	
    	if (this.m_fw!=null) {
    		this.m_fw.destroy();
    	}
    	
    	System.setProperty("jam.fritzbox.session.password", "");
    	System.setProperty("jam.fritzbox.session.counter", "0");
    	System.setProperty("jam.fritzbox.session.ispwdialogvisible", "false");
    	System.setProperty("jam.fritzbox.session.donotlogin", "false");
    	this.m_isCreatingFirmware = false;
    	
    	this.m_fw = null;
    	this.m_isRunning = false;
    }

	public void received(IEvent event) {
		if (m_logger.isLoggable(Level.INFO))
			m_logger.info("New event received: "+event.getType());
		switch (event.getType()) {
			case IEventConst. EVENT_TYPE_HARDWARE_UNSUPPORTED: 
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Connection event EVENT_TYPE_HARDWARE_UNSUPPORTED occured.");
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
							"fritzbox.firmware.hardware",
							"unsupported",
							new Exception("Firmware not supported."),
							false),
						"Tray");
				break;
			case IEventConst.EVENT_TYPE_HARDWARE_RESTARTED: 
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Connection event EVENT_TYPE_HARDWARE_RESTARTED occured.");
				PropagationFactory.getInstance().fire(
						new Message(Message.WARNING,
							"fritzbox.firmware.hardware",
							"restarted",
							new String[] {getFritzBoxAddress()},
							new Exception("FRITZ!Box "+getFritzBoxAddress()+" restart detected."),
							false),
						"Tray");
				this.reconnect(10000L);
				break;
			case IEventConst.EVENT_TYPE_HARDWARE_NETWORK_UNAVAILABLE: 
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Connection event EVENT_TYPE_HARDWARE_NETWORK_UNAVAILABLE occured.");
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
							"fritzbox.firmware.hardware",
							"network",
							new String[] {getFritzBoxAddress()},
							new Exception("Network unreachable."),
							false),
						"Tray");
				this.reconnect(getFritzBoxMaxRetryTimeout());
				break;
			case IEventConst.EVENT_TYPE_HARDWARE_CONNECTION_LOST: 
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Connection event EVENT_TYPE_CONNECTION_LOST occured.");
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
							"fritzbox.firmware.hardware",
							"conlost",
							new String[] {getFritzBoxAddress(), getFritzBoxPort()},
							new Exception("Connection to "+getFritzBoxAddress()+":"+getFritzBoxPort()+" lost."),
							false),
						"Tray");
				
				this.reconnect(getFritzBoxMaxRetryTimeout());
				break;
			case IEventConst.EVENT_TYPE_HARDWARE_UNKNOWN_HOST: 
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Connection event EVENT_TYPE_UNKNOWN_HOST occured.");
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
							"fritzbox.firmware.hardware",
							"unknownhost",
							new String[] {getFritzBoxAddress()},
							new Exception("Unknown host "+getFritzBoxAddress()),
							false),
						"Tray");
				this.reconnect(getFritzBoxMaxRetryTimeout());
				break;
			case IEventConst.EVENT_TYPE_HARDWARE_REFUSED: 
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Connection event EVENT_TYPE_HARDWARE_REFUSED occured.");
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
							"fritzbox.firmware.hardware",
							"refused",
							new String[] {getFritzBoxAddress()},
							new Exception("Connection refused: "+getFritzBoxAddress()),
							false));

				this.reconnect(getFritzBoxMaxRetryTimeout());
				break;
			case IEventConst.EVENT_TYPE_HARDWARE_RECONNECTED_SUCCESS: 
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Connection successfully established.");
				
				if (getRuntime().getMonitorListener().isRunning() && this.isLoggedIn()) {
					m_retryCount = 0;
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Retry counter = 0");
				}
				break;
			default: break;
		}
	}

	public String getReceiverID() {
		return "FirmwareManager";
	}

	public int getPriority() {
		return 1;
	}

	public String getSenderID() {
		return "FirmwareManager";
	}
	
	private void promptPassword() throws FritzBoxLoginException {
		boolean dnl = false;
		// check for password
		// password is mandatory
		String pw = this.getFritzBoxPassword();
		if (pw.trim().length()==0) {
			int i = Integer.parseInt(System.getProperty("jam.fritzbox.session.counter", "0"));
			do {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				dnl = Boolean.parseBoolean(System.getProperty("jam.fritzbox.session.donotlogin", "false"));
				boolean isPWDialogRunning = Boolean.parseBoolean(System.getProperty("jam.fritzbox.session.ispwdialogvisible", "false"));
				pw = this.getFritzBoxPassword();
				if (!isPWDialogRunning && pw.trim().length()==0 && i < 4 && !dnl) {
					final ICommand c = this.getRuntime().getCommandFactory().getCommand("PasswordDialog");
					if (c!=null && c.isExecutable() && !c.isExecuting())
						try {
							c.execute();
						} catch (Exception e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
						}		
				}
				
				i = Integer.parseInt(System.getProperty("jam.fritzbox.session.counter", "0"));
			} while (pw.trim().length()==0 && i < 4 && !dnl);
			if (i>=4 || dnl) {
				PropagationFactory.getInstance().fire(
						new Message(Message.ERROR,
						"fritzbox.firmware.login",
						"loginfailed",	
						new Exception("No password set for login."),
						true));
				this.m_isLoggingIn = false;
				throw new FritzBoxLoginException("No password set for login.");
			}
		}
	}
	
    private void reconnect(long timeout) {
    	
    	if (this.m_isReconnecting) {
    		if (m_logger.isLoggable(Level.INFO))
    			m_logger.info("Re-connecting already in progress. Exiting thread "+Thread.currentThread().getName());
    		return;
    	}
    	
    	while (this.m_isReconnecting) {
    		try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
    	}
    	
    	this.m_isReconnecting = true;
    	IMonitorListener ml = PIMRuntime.getInstance().getMonitorListener();
		if (ml!=null && ml.isRunning()) {
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("Disconnecting FritzBox monitor on port 1012.");
			ml.stop();
		}
		
		ICommand c = PIMRuntime.getInstance().getCommandFactory().getCommand("Activator");
		if (c!=null) {
			try {
				Map m = new HashMap();
				m.put("status", "delay");
				c.setParameters(m); // this method executes the command as well !!
			} catch (Exception e) {
				m_logger.log(Level.SEVERE, e.toString(), e);
			}
		}
		
		if (m_logger.isLoggable(Level.INFO))
			m_logger.info("Disconnecting FritzBox sync on port 80 and 49443/49000.");
		
		if (m_restartedThread!=null && m_restartedThread.isAlive()){
			m_restartedThread.interrupt();
		}
		
    	if (m_timeoutThread!=null && m_timeoutThread.isAlive()) {
    		m_timeoutThread.interrupt();
    	}
    	
    	if (this.m_fw!=null) {
    		this.m_fw.destroy();
    	}
    	this.m_fw = null;

		if (getFritzBoxAutoReconnect() && m_retryCount<getFritzBoxMaxRetryCount()) {
			m_retryCount++;
			try {
				Thread.sleep(7000);
			} catch (InterruptedException e1) {
			}
			
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("Re-connecting is configured. Retry counter = "+m_retryCount);
			
			
			PropagationFactory.getInstance().fire(
					new Message(Message.INFO,
						"fritzbox.firmware.hardware",
						"reconnect",
						new String[] {getFritzBoxAddress(), Integer.toString(m_retryCount)},
						new Exception("Reconnecting to FRITZ!Box "+getFritzBoxAddress()),
						false),
					"Tray");
			
			try {
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Sleeping "+timeout+" ms before re-connect try.");
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
			}

			try {
				this.login();
				
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Automatic re-connect to FritzBox done...");
				
			} catch (FritzBoxLoginException e) {
				// check for reason
				m_logger.log(Level.WARNING, e.getMessage(), e);
				if (m_logger.isLoggable(Level.INFO))
					m_logger.info("Automatic re-connect to FritzBox failed...");
			}
			
			Thread t = new Thread(new Runnable() {
				public void run() {
					IMonitorListener ml = PIMRuntime.getInstance().getMonitorListener();
					if (ml!=null && !ml.isRunning()) {
						ml.start();
						ICommand c = PIMRuntime.getInstance().getCommandFactory().getCommand("Activator");
						if (c!=null) {
							try {
								Map m = new HashMap();
								m.put("status", "delay");
								c.setParameters(m); // this method executes the command as well !!
							} catch (Exception e) {
								m_logger.log(Level.SEVERE, e.toString(), e);
							}
						}
					}}});
			t.setName("JAM-FritzBoxFirmwareRestartMonitor-Thread-(daemon)");
			t.setDaemon(true);
			t.start();

		} else if (getFritzBoxAutoReconnect() && m_retryCount>=getFritzBoxMaxRetryCount()) {
			PropagationFactory.getInstance().fire(
					new Message(Message.INFO,
						"fritzbox.firmware.hardware",
						"maxreconnect",
						new String[] {Integer.toString((m_retryCount>getFritzBoxMaxRetryCount() ? getFritzBoxMaxRetryCount() : m_retryCount))},
						new Exception("Maximum retry count reached "+m_retryCount),
						false),
					"Tray");
		}
		this.m_isReconnecting = false;
    }

    private synchronized void createFirmwareInstance() throws FritzBoxInitializationException, FritzBoxNotFoundException, InvalidSessionIDException {
    	do {
    		try {
				Thread.sleep(250);
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Firmware is beeing created. Sleeping thread: "+Thread.currentThread().getName());
			} catch (InterruptedException e) {
			}
    	} while (this.m_isCreatingFirmware);
    	this.m_isCreatingFirmware = true;
    	
    	if (this.m_fw==null) {
    		this.m_fw = new TR064FritzBoxFirmware(getFritzBoxAddress(), getFritzBoxPort(), getFritzBoxPassword(), getFritzBoxUser());
    		try {
    			if (this.m_fw==null) throw new FritzBoxInitializationException("Instantiation of TR064 firmware instance failed.");
    			if (!((TR064FritzBoxFirmware)this.m_fw).isTR064Enabled()) throw new FritzBoxInitializationException("TR064 is not supported or TR064 support disabled by this FRITZ!Box "+this.getFritzBoxAddress()); 
    			if (!this.m_fw.isPasswordValid()) throw new InvalidSessionIDException("Invalid user/password combination: "+this.getFritzBoxUser()+"/"+this.getFritzBoxPassword());
				this.m_fw.init();
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("Detected TR064 Firmware: "+this.m_fw.toString());
			} catch (FritzBoxInitializationException e6) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.log(Level.INFO, "No TR064 Firmware detected.", e6);
				
	    		this.m_fw = new FritzOS559Firmware(getFritzBoxAddress(), getFritzBoxPort(), getFritzBoxPassword(), getFritzBoxUser(), getFritzBoxUseHttps());
	    		try {
	    			if (this.m_fw==null) throw new FritzBoxInitializationException("Instantiation of Fritz!OS 05.59+ firmware instance failed.");
					this.m_fw.init();
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("Detected Fritz!OS 05.59+ firmware: "+this.m_fw.toString());
				} catch (FritzBoxInitializationException e5) {
					if (e5.isUnsupportedFirmware()) {
						this.m_isCreatingFirmware = false;
						throw e5;
					}
					
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("No Fritz!OS 05.59+ Firmware detected.");
		    		this.m_fw = new FritzOSFirmware(getFritzBoxAddress(), getFritzBoxPort(), getFritzBoxPassword(), getFritzBoxUser(), getFritzBoxUseHttps());
		    		try {
		    			if (this.m_fw==null) throw new FritzBoxInitializationException("Instantiation of Fritz!OS 05.50+ firmware instance failed.");
						this.m_fw.init();
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("Detected Fritz!OS 05.50+ firmware: "+this.m_fw.toString());
					} catch (FritzBoxInitializationException e4) {
						if (this.m_logger.isLoggable(Level.INFO))
							this.m_logger.info("No Fritz!OS 05.50+ Firmware detected.");
			    		this.m_fw = new SessionIDFritzBoxFirmware(getFritzBoxAddress(), getFritzBoxPort(), getFritzBoxPassword(), getFritzBoxUseHttps());
			    		try {
			    			if (this.m_fw==null) throw new FritzBoxInitializationException("Invalid Session ID firmware instance failed.");
							this.m_fw.init();
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("Detected FritzBox Session ID firmware: "+this.m_fw.toString());
						} catch (FritzBoxInitializationException e3) {
							if (this.m_logger.isLoggable(Level.INFO))
								this.m_logger.info("No Session ID Firmware detected.");
							this.m_fw = new UnitymediaFirmware(getFritzBoxAddress(), getFritzBoxPort(), getFritzBoxPassword(), getFritzBoxUser(), getFritzBoxUseHttps());
							try {
								if (this.m_fw==null) throw new FritzBoxInitializationException("Instantiation of Unitymedia firmware instance failed.");
								this.m_fw.init();
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("Detected Unitymedia firmware: "+this.m_fw.toString());
							} catch (FritzBoxInitializationException e2) {
								if (this.m_logger.isLoggable(Level.INFO))
									this.m_logger.info("No Unitymedia Firmware detected.");
								this.m_fw = new PasswordFritzBoxFirmware(getFritzBoxAddress(), getFritzBoxPort(), getFritzBoxPassword());
								try {
									if (this.m_fw==null) throw new FritzBoxInitializationException("Instantiation of standard firmware instance failed.");
									this.m_fw.init();
									if (this.m_logger.isLoggable(Level.INFO))
										this.m_logger.info("Detected FritzBox standard firmware (password protected): "+this.m_fw.toString());
								} catch (FritzBoxInitializationException e1) {
									if (this.m_logger.isLoggable(Level.INFO))
										this.m_logger.info("No FritzBox standard Firmware detected.");
									this.m_fw = null;
									this.m_isCreatingFirmware = false;
									throw new InvalidSessionIDException(e1.getMessage());
								} 
							}
						}
					}
				}
			} finally {
				this.m_isCreatingFirmware = false;
			}
			if (this.m_fw!=null) {
				if (this.m_fw.getFirmwareTimeout()>0) {
					if (this.m_logger.isLoggable(Level.INFO))
						this.m_logger.info("FritzBox timeout thread started.");
					this.launchTimeoutThread();
				}
			}
			if (this.m_fw!=null) {
				if (this.m_logger.isLoggable(Level.INFO))
					this.m_logger.info("FritzBox restarted thread started.");
				this.launchRestartedThread();
			}
    	}
    	this.m_isCreatingFirmware = false;
    }
    
    private void launchRestartedThread() {
    	if (m_restartedThread!=null && m_restartedThread.isAlive()) {
    		m_restartedThread.interrupt();
    	}
    	
    	m_restartedThread = new Thread() {
			public void run() {
				try {
					do {
						int clct = getFritzBoxConnectionLostCheckTime();
						Thread.sleep(((clct>0 ? clct : 1) * 60000));
					} while (m_fw!=null && !m_fw.isRestarted()); 
					
					if (m_logger.isLoggable(Level.WARNING))
						m_logger.warning("FritzBox reset or restart detected.");
					
					if (m_broker!=null)
						m_broker.send(FirmwareManager.this, m_broker.createEvent(IEventConst.EVENT_TYPE_HARDWARE_RESTARTED));					
				} catch (InterruptedException e) {
					m_logger.log(Level.INFO,"JAM-FritzBoxFirmwareRestarted-Thread gets interrupted.: "+e.getMessage(), e);
				}
			}
		};
		m_restartedThread.setName("JAM-FritzBoxFirmwareRestarted-Thread-(deamon)");
		m_restartedThread.setDaemon(true);
		m_restartedThread.start();
    }
    
    private void launchTimeoutThread() {
    	if (m_timeoutThread!=null && m_timeoutThread.isAlive()) {
    		m_timeoutThread.interrupt();
    	}
    	
    	m_timeoutThread = new Thread() {
			public void run() {
				try {
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Session ID timeout set to "+(m_fw.getFirmwareTimeout()/1000)+" sec.");
					Thread.sleep(m_fw.getFirmwareTimeout());
					
					if (m_fw!=null) m_fw.destroy();
					m_fw = null;
					if (m_logger.isLoggable(Level.INFO))
						m_logger.info("Automatic FritzBox timeout for logout reached.");
					if (getFritzBoxAutoReconnect()) {
						m_logger.info("Trying automatic re-connect to FritzBox...");
						try {
							createFirmwareInstance();
							if (m_logger.isLoggable(Level.INFO))
								m_logger.info("Automatic re-connect to FritzBox done...");
						} catch (FritzBoxInitializationException e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (FritzBoxNotFoundException e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (InvalidSessionIDException e) {
							m_logger.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				} catch (InterruptedException e) {
					m_logger.log(Level.WARNING,"JAM-FritzBoxFirmwareTimeout-Thread gets interrupted.: "+e.getMessage(), e);
				}
			}
		};
		m_timeoutThread.setName("JAM-FritzBoxFirmwareTimeout-Thread-(deamon)");
		m_timeoutThread.setDaemon(true);
		m_timeoutThread.start();
    }
    
    private String getFritzBoxAddress() {
    	return getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_IP);
    }
    
    private String getFritzBoxUser() {
    	return getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_USER);
    }
    
    private String getFritzBoxPassword() {
    	String pw = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_PASSWORD);
    	if (pw==null || pw.trim().length()==0) pw = System.getProperty("jam.fritzbox.session.password", "");
    	return (pw==null ? "" : pw);
    }
    
    private String getFritzBoxPort() {
    	return getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_PORT);
    }
    
    private int getFritzBoxConnectionLostCheckTime() {
    	String n = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_CONNECTION_LOST_CHECKTIME);
    	return Integer.parseInt((n!=null && n.length()>0 ? n : "1"));
    }
    
    private boolean getFritzBoxAutoReconnect() {
    	return getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_AUTO_RECONNECT_SESSIONID).equalsIgnoreCase("true");
    }
    
    private boolean getFritzBoxTR064Off() {
    	return getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_TR064_OFF).equalsIgnoreCase("true");
    }
    
    private boolean getFritzBoxUseHttps() {
    	return getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_USE_HTTPS).equalsIgnoreCase("true");
    }
	
    private int getFritzBoxMaxRetryCount() {
    	String n = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_RETRYMAX);
    	return Integer.parseInt((n!=null && n.length()>0 ? n : "5"));
	}
	
	private long getFritzBoxMaxRetryTimeout() {
    	String n = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(FritzBoxMonitor.NAMESPACE, FritzBoxConst.CFG_RETRYTIMEOUT);
    	return Long.parseLong((n!=null && n.length()>0 ? n : "30")) * 1000;
	}
    
    private IRuntime getRuntime() {
    	if (this.m_runtime==null) {
    		this.m_runtime = PIMRuntime.getInstance();
    	}
    	return this.m_runtime;
    }

}
