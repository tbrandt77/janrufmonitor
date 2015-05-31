package de.janrufmonitor.util.io;

import de.janrufmonitor.framework.IJAMConst;

/**
 * This class provides access to OS specific information.
 * 
 *@author     Thilo Brandt
 *@created    2005/04/10
 */
public class OSUtils {

	private static byte m_Windows = -1;
	private static byte m_Linux = -1;
	private static byte m_MacOSX = -1;
	
	private static byte m_32 = -1;
	private static byte m_64 = -1;
	
	/**
	 * Return true whether the current running mode is console
	 * @return
	 */
	public static boolean isConsole() {
		return System.getProperty("jam.console", "false").equalsIgnoreCase("true");	
	}
	
	/**
	 * Return true whether the current running OS is Microsoft Windows
	 * @return
	 */
	public static boolean isWindows() {
		if (m_Windows==-1) {
			String platform = System.getProperty("os.name");
			if (platform!=null && (platform.toLowerCase().indexOf("windows")>-1 || platform.toLowerCase().indexOf("win")>-1)) {
				m_Windows = 1;
			} else {
				m_Windows = 0;
			}
		}
		return (m_Windows>0);	
	}
	
	/**
	 * Return true whether the current running OS is Linux based
	 * @return
	 */
	public static boolean isLinux() {
		if (m_Linux==-1) {
			String platform = System.getProperty("os.name");
			if (platform!=null && platform.toLowerCase().indexOf("linux")>-1) {
				m_Linux = 1;
			} else {
				m_Linux = 0;
			}
		}
		return (m_Linux>0);
	}
	
	/**
	 * Return true whether the current running OS is Mac OSX based
	 * @return
	 */
	public static boolean isMacOSX() {
		if (m_MacOSX==-1) {
			String platform = System.getProperty("os.name");
			if (platform!=null && platform.toLowerCase().indexOf("mac")>-1) {
				m_MacOSX = 1;
			} else {
				m_MacOSX = 0;
			}
		}
		return (m_MacOSX>0);
	}
	
	/**
	 * Return true whether the current OS is a 32-bit OS
	 * @return
	 */
	public static boolean is32Bit() {
		if (m_32==-1) {
			String platform = System.getProperty("sun.arch.data.model");
			if (platform!=null && platform.toLowerCase().indexOf("32")>-1) {
				m_32 = 1;
			} else {
				m_32 = 0;
			}
		}
		return (m_32>0);
	}
	
	/**
	 * Return true whether the current OS is a 64-bit OS
	 * @return
	 */
	public static boolean is64Bit() {
		if (m_64==-1) {
			String platform = System.getProperty("sun.arch.data.model");
			if (platform!=null && platform.toLowerCase().indexOf("64")>-1) {
				m_64 = 1;
			} else {
				m_64 = 0;
			}
		}
		return (m_64>0);
	}
	
	/**
	 * Return true whether jam.multiuser properties is set to true
	 * @return
	 */
	public static boolean isMultiuserEnabled() {
		String multiUser = System.getProperty(IJAMConst.SYSTEM_MULTI_USER);
		return (multiUser!=null && Boolean.parseBoolean(multiUser));
	}
	
	/**
	 * Returns the current logged in user or "default" if not set.
	 * @return
	 */
	public static String getLoggedInUser() {
		String usr = System.getProperty("user.name");
		return (usr!=null && usr.length()>0 ? usr.trim() : "default");
	}
	

}
