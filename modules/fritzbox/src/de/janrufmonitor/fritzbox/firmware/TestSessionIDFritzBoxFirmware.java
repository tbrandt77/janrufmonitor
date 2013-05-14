package de.janrufmonitor.fritzbox.firmware;

import java.io.IOException;

//import de.janrufmonitor.fritzbox.firmware.exception.DeleteCallListException;
//import de.janrufmonitor.fritzbox.firmware.exception.DoCallException;
import de.janrufmonitor.fritzbox.firmware.exception.DoBlockException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxInitializationException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxLoginException;
import de.janrufmonitor.fritzbox.firmware.exception.FritzBoxNotFoundException;
import de.janrufmonitor.fritzbox.firmware.exception.GetBlockedListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallListException;
import de.janrufmonitor.fritzbox.firmware.exception.GetCallerListException;
import de.janrufmonitor.fritzbox.firmware.exception.InvalidSessionIDException;
import de.janrufmonitor.logging.LoggingInitializer;

public class TestSessionIDFritzBoxFirmware {

	public static void main(String[] args) {
		LoggingInitializer.run();
		
		IFritzBoxFirmware fw = new SessionIDFritzBoxFirmware("fritz.box", "80", "diplom2001");
		
		try {
			fw.init();
		} catch (FritzBoxInitializationException e) {
			e.printStackTrace();
		} catch (FritzBoxNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidSessionIDException e) {
			e.printStackTrace();
		} 
		System.out.println(fw.toString());
		try {
			fw.login();
		} catch (FritzBoxLoginException e) {
			e.printStackTrace();
		}
		
		try {
			System.out.println(fw.getCallerList());
		} catch (GetCallerListException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			System.out.println(fw.getCallList());
		} catch (GetCallListException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			fw.doBlock("0123456789");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DoBlockException e) {
			e.printStackTrace();
		}
		
		try {
			System.out.println(fw.getBlockedList());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GetBlockedListException e) {
			e.printStackTrace();
		} 
		
		/**
		try {
			fw.deleteCallList();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DeleteCallListException e) {
			e.printStackTrace();
		}
		*/
		/**
		try {
			fw.doCall("+496227763614#", "1");		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DoCallException e) {
			e.printStackTrace();
		}
		*/
		fw.destroy();
		
	}

}
