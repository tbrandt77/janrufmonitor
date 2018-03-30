package de.janrufmonitor.ui.swt;

import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.util.io.OSUtils;
import de.janrufmonitor.util.io.PathResolver;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class DisplayManager {
	
	private static boolean isUIThread;
	private static Logger m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
	private static Thread t;
	
	public static Display getDefaultDisplay() {	
		if (m_logger.isLoggable(Level.INFO))
			m_logger.info("Invoking thread name for DisplayManager.getDefaultDisplay(): "+Thread.currentThread().getName());
		if (!isUIThread) {
			int count = 0;
			while (!isUIThread && count < 25) {
				createUIThread();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					m_logger.severe(e.getMessage());
				}
				count ++;
			}

			if (count==25) {
				m_logger.severe("Cannot create a new UI thread instance.");
				// check if Win 64-bit
				if (OSUtils.isWindows() && OSUtils.is64Bit()) {
					m_logger.severe("Emergency exit: Wrong Java architecture for UI library SWT: Java is 64-bit but jAnrufmonitor is 32-bit. Program will be closed.");
					JFrame frame = new JFrame("jAnrufmonitor");
					JOptionPane.showMessageDialog(frame,
						    "Java is 64-bit, but jAnrufmonitor is 32-bit. Program will not start.",
						    "Startup error",
						    JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				}
				
				// check if Win 32-bit
				if (OSUtils.isWindows() && OSUtils.is32Bit()) {
					m_logger.severe("Emergency exit: Wrong Java architecture for UI library SWT: Java is 32-bit but jAnrufmonitor is 64-bit. Program will be closed.");
					JFrame frame = new JFrame("jAnrufmonitor");
					JOptionPane.showMessageDialog(frame,
						    "Java is 32-bit, but jAnrufmonitor is 64-bit. Program will not start.",
						    "Startup error",
						    JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				}
			}
		}
		return Display.getDefault();
	}
	
	public static void dispose() {
		Display d = Display.getDefault();
		if (d!=null) d.dispose();
		d = null;
		if (t!=null) t = null;
		isUIThread = false;
	}

	/**
	 * Force the shell to be displayed on the foreground.
	 * @param shell underlying shell
	 */
	public static void forceForeground(Shell shell){
		if (shell == null) return;
		if (OSUtils.isMacOSX()){
			// #20 forceActive() does not work on Cocoa (tested up to SWT 4.6.2)
			// Workaround is to make the shell visible for a short time
			if (!shell.getVisible()) {
				shell.setVisible(true);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					m_logger.warning("forceForeground() Thread was interrupted.");
				}
			}
			shell.forceActive();
			shell.setVisible(false);
		} else {
			shell.forceActive();
		}
	}

	private synchronized static void createUIThread() {
		if (isUIThread) {
			if (m_logger.isLoggable(Level.WARNING))
				m_logger.warning("JAM-SWT/JFaceUI-Thread-(non-deamon) already created. Calling thread: "+Thread.currentThread().getName());
			return;
		}
		
		// added 2005/08/14: immetiatly set ui thread to true
		// removed 2005/08/31: synchronized method prevent wrong access !
		//isUIThread = true;
		if (t!=null) {
			if (t.isAlive()) {
				try {
					if (!isUIThread) {
						Thread.sleep(2000);
						if (!isUIThread && m_logger.isLoggable(Level.WARNING)) {
							m_logger.warning("JAM-SWT/JFaceUI-Thread-(non-deamon) not created after 2000ms: "+isUIThread);
							m_logger.warning("Tried to start another UIThread. That's not allowed on this platform.");					
						}
					} else {
						if (m_logger.isLoggable(Level.WARNING)) {
							m_logger.warning("Tried to start another UIThread. That's not allowed on this platform.");					
						}
					}
				} catch (InterruptedException e) {
					m_logger.severe(e.getMessage());
				}
				return;
			} else {
				m_logger.warning("UIThread is not alive any longer.");
			}
		}
		
		if (OSUtils.isMacOSX()) {
			final Display d = Display.getDefault();
			isUIThread = true;
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("Display Thread name: "+d.getThread().getName());
			while (!d.isDisposed()) {
				if (!d.readAndDispatch()) d.sleep();
			}
		} else {
			t = new Thread() {
				public void run() {
					Display d = null;
					try {
						d = Display.getDefault();					
						isUIThread = true;
						if (m_logger.isLoggable(Level.INFO))
							m_logger.info("Display Thread name: "+d.getThread().getName());
						while (!d.isDisposed()) {
							if (!d.readAndDispatch()) d.sleep();
						}
					} catch (Exception ex) {
						m_logger.warning(ex.getMessage());
						if (d!=null)
							m_logger.severe("Wrong DisplayThread: "+d.getThread().getName());
						dumpException(ex);
						Display.getDefault().dispose();
						isUIThread = false;
					}
				}
			};
			//t.setName("SWT/JFace.UI.Thread");
			t.setName("JAM-SWT/JFaceUI-Thread-(non-deamon)");
			t.start();
		}
		
	}
	
    private static void dumpException(Exception ex){
    	try {
	    	String dumpPath = PathResolver.getInstance().getLogDirectory()+"_fatal_ui_error.dmp";
	    	FileOutputStream os = new FileOutputStream(dumpPath);
	    	PrintStream ps = new PrintStream(os);
	    	ex.printStackTrace(ps);
	    	ps.flush();
	    	ps.close();
    	} catch (FileNotFoundException e) {
    		m_logger.severe(e.getMessage());
		}
    }

}
