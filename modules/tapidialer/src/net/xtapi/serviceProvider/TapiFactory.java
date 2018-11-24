package net.xtapi.serviceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.IJAMConst;

public class TapiFactory {
	
	public class TapiHandle {

		int m_id;

		String m_name;

		public TapiHandle(int id, String name) {
			this.m_id = id;
			this.m_name = name;
		}

		public String getName() {
			return this.m_name;
		}
		
		public int getID() {
			return this.m_id;
		}

		public String toString() {
			return "TAPI handle: " + this.m_id + " - " + this.m_name;
		}

	}

	private static TapiFactory m_instance = null;
	
	private Logger m_logger;
	private MSTAPI m_tapi = null;
	private int m_lines = -1;
	
	private int m_lineHandle;
	private int[] m_deviceIDMapping;
	private Map m_tapiHandles;
	
	private TapiFactory() {
		this.m_logger = LogManager.getLogManager().getLogger(
				IJAMConst.DEFAULT_LOGGER);
	}
	
	public static synchronized TapiFactory getInstance() {
		if (TapiFactory.m_instance == null) {
			TapiFactory.m_instance = new TapiFactory();
		}
		return TapiFactory.m_instance;
	}
	
	public synchronized void init(IXTapiCallBack cb) {
		if (this.isInitialized()) return;
		this.m_tapi = new MSTAPI();
		m_tapiHandles = new HashMap();
		if (m_logger!=null && m_logger.isLoggable(Level.INFO))
			m_logger.info("TAPI created with Callback: "+cb.toString());
		
		this.m_lines = m_tapi.init(cb);
		if (m_logger!=null && m_logger.isLoggable(Level.INFO))
			m_logger.info("TAPI received # of lines: "+this.m_lines);
		
		if (this.m_lines>=0)
			m_deviceIDMapping = new int[this.m_lines];
		else {
			if (m_logger!=null && m_logger.isLoggable(Level.SEVERE))
				m_logger.severe("TAPI provided no lines to observe. Lines count: "+this.m_lines);
		}
		
		StringBuffer nameOfLine = null;

		for (int i = 0; i < this.m_lines; i++) {
			nameOfLine = new StringBuffer();
			m_lineHandle = this.m_tapi.openLineTapi(i, nameOfLine);
			m_deviceIDMapping[i] = -1;
			if (m_lineHandle > 0) {
				m_logger.info("Opening line #" + m_lineHandle);
				m_logger.info("Opening line name "
						+ nameOfLine.toString());
				m_tapiHandles.put(new Integer(m_lineHandle),
						new TapiHandle(m_lineHandle, nameOfLine
								.toString()));
				
				m_deviceIDMapping[i] = Integer.valueOf("0"+nameOfLine.toString().replaceAll("[^0-9]+", ""));
				
				m_logger.info("Device mapping ID["+i+"]: " + m_deviceIDMapping[i]);
			}
		}

	}
	
	public int getLinesCount() {
		return this.m_lines;
	}
	
	public boolean isInitialized() {
		return m_tapi!=null;
	}
	
	public MSTAPI getTapi() {
		return this.m_tapi;
	}
	
	public void shutdown() {
		this.m_tapi.shutdownTapi();
		this.m_tapi = null;
		this.m_lines = -1;
		if (m_logger!=null && m_logger.isLoggable(Level.INFO))
			m_logger.info("TAPI is shutdown.");
		
		if (m_tapiHandles != null)
			m_tapiHandles.clear();
		m_tapiHandles = null;
		
		TapiFactory.m_instance = null;
	}
	
	public String getDeviceIDNumber(int instance) {
		if (m_logger.isLoggable(Level.INFO))
			m_logger.info("try to determine DeviceIDNumber from instance #"+instance);
		
		if (this.m_deviceIDMapping.length > instance && this.m_deviceIDMapping[instance]>=0) {
			if (m_logger.isLoggable(Level.INFO))
				m_logger.info("DeviceIDNumber found for instance #"+instance+" = "+this.m_deviceIDMapping[instance]);
			return Integer.toString(this.m_deviceIDMapping[instance]);
		}
		if (m_logger.isLoggable(Level.INFO))
			m_logger.info("No DeviceIDNumber found for instance #"+instance);
		return Integer.toString(instance);
	}
	
	public Map getTapiHandles() {
		return this.m_tapiHandles;
	}
	
}
