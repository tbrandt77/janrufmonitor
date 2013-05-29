package de.janrufmonitor.framework.i18n;

import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.string.StringUtils;
import de.janrufmonitor.framework.configuration.IConfigurable;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.db.AbstractDatabaseHandler;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class DatabaseI18nManager implements II18nManager, IConfigurable {
   
	private long m_refreshTime = -1L;
	private boolean isActive;
	
	private class ConnectionObserver implements Runnable {
		public void run() {
			while (isActive) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				try {
					if (m_refreshTime>0 && DatabaseI18nManager.this.m_dh!=null && DatabaseI18nManager.this.m_dh.isConnected()) {
						if (System.currentTimeMillis() - m_refreshTime > 10000)  {
							DatabaseI18nManager.this.m_dh.setInitializing(false);
							DatabaseI18nManager.this.m_dh.disconnect();
						}
					}
				} catch (SQLException e) {
					DatabaseI18nManager.this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			DatabaseI18nManager.this.m_logger.info("Terminating ConnectionObserver-Thread...");
		}
		
	}
	
	private class I18nDatabaseHandler extends AbstractDatabaseHandler {

		public I18nDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}

		protected IRuntime getRuntime() {
			return PIMRuntime.getInstance();
		}

		public void disconnect() throws SQLException {
			try {
				Statement st = m_con.createStatement();
				st.execute("SHUTDOWN");
			} finally {
				super.disconnect();
			}
		}

		public void commit() throws SQLException {
			Statement st = m_con.createStatement();
			st.execute("COMMIT");
			super.commit();
		}

		protected void createTables() throws SQLException {
			Statement stmt = m_con.createStatement();
			stmt.execute("DROP TABLE attributes IF EXISTS;");
			stmt.execute("DROP TABLE callers IF EXISTS;");
			stmt.execute("CREATE TABLE i18n (namespace VARCHAR(256), id VARCHAR(64), labelid VARCHAR(32), languageid VARCHAR(8), value VARCHAR(8192));");
			stmt.close();
		}

		protected void addPreparedStatements() throws SQLException {
			m_preparedStatements.put("INSERT_I18N", m_con.prepareStatement("INSERT INTO i18n VALUES(?,?,?,?,?);"));
			m_preparedStatements.put("DELETE_I18N", m_con.prepareStatement("DELETE FROM i18n WHERE namespace=? AND id=? AND labelid=? AND languageid=?;"));
			m_preparedStatements.put("DELETE_I18N_NS", m_con.prepareStatement("DELETE FROM i18n WHERE namespace=?;"));
		}
		
		public void setI18nEntry(String namespace, String parameter, String identifier, String language, String value) throws SQLException {
			if (!this.isConnected()) {
				try {
					this.connect();
				} catch (SQLException ex) {
					this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
				} catch (ClassNotFoundException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}				
			}
			
			PreparedStatement ps = this.getStatement("DELETE_I18N");
			ps.clearParameters();
			ps.setString(1, namespace);
			ps.setString(2, parameter);
			ps.setString(3, identifier);
			ps.setString(4, language);
			ps.execute();
			ps = this.getStatement("INSERT_I18N");
			ps.clearParameters();
			ps.setString(1, namespace);
			ps.setString(2, parameter);
			ps.setString(3, identifier);
			ps.setString(4, language);
			ps.setString(5, value);
			ps.execute();
		}
		
		public void deleteI18nNamespace(String namespace) throws SQLException {
			if (!this.isConnected()) {
				try {
					this.connect();
				} catch (SQLException ex) {
					this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
				} catch (ClassNotFoundException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}				
			}
			
			PreparedStatement ps = this.getStatement("DELETE_I18N_NS");
			ps.clearParameters();
			ps.setString(1, namespace);
			ps.execute();
		}
		
		public String getI18nEntry(String namespace, String parameter, String identifier, String language) throws SQLException {
			if (!this.isConnected()) {
				try {
					this.connect();
				} catch (SQLException ex) {
					this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
					return "Error!";
				} catch (ClassNotFoundException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
					return "Error!";
				}				
			}
			
			StringBuffer sql = new StringBuffer();
			Statement stmt = m_con.createStatement();

			// build SQL statement
			sql.append("SELECT value FROM i18n WHERE namespace='");
			sql.append(namespace);
			sql.append("' AND id='");
			sql.append(parameter);
			sql.append("' AND labelid='");
			sql.append(identifier);
			sql.append("' AND languageid='");
			sql.append(language);
			sql.append("';");
			ResultSet rs = stmt.executeQuery(sql.toString());
			while (rs.next()) {
				return rs.getString("value");
			}
			stmt.close();
			return null;
		}
		
		public void setInitializing(boolean init) {
			super.setInitializing(init);
		}
		
	}
	
    private String ID = "DatabaseI18nManager";
    private String NAMESPACE = "i18n.DatabaseI18nManager";
    
    Logger m_logger;
    Properties m_configuration;    
    List m_identifiers;
    I18nDatabaseHandler m_dh;
    
    String CONFIG_KEY = "database";
    String CONFIG_IDENTIFIER = "identifier";
    String CONFIG_LANG = "defaultlanguage";  
    String CONFIG_KEEPALIVE = "keepalive";  
    
    public DatabaseI18nManager() {
        this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
    }
    
    public String[] getIdentifiers() {
    	this.buildIdentifiers();
        String[] identifiers = new String[m_identifiers.size()];
        for (int i=0;i<m_identifiers.size();i++)
			identifiers[i] = (String)m_identifiers.get(i);
        
        return identifiers;        
    }
    
    public String getString(String namespace, String parameter, String identifier, String language) {
        if (this.isIdentifier(identifier) && this.m_dh!=null) {
        	this.m_refreshTime = System.currentTimeMillis();
        	try {
	        	String value = this.m_dh.getI18nEntry(namespace, parameter, identifier, language);
				if (value!=null && value.length()==0) {
	        		value = this.m_dh.getI18nEntry(namespace, parameter, identifier, this.m_configuration.getProperty(this.CONFIG_LANG));
	        	}
	            return (value == null ? parameter : value);
			} catch (SQLException e) {
				this.m_logger.log(Level.WARNING, e.getMessage(), e);
				// added 2013/05/29: due to "session is closed" error try to reconnect to i18n DB
				try {
					if (this.m_dh!=null)
						try {
							this.m_dh.disconnect();
						} catch (SQLException ex) {	}
						
					this.m_dh.connect();
					String value = this.m_dh.getI18nEntry(namespace, parameter, identifier, language);
					if (value!=null && value.length()==0) {
		        		value = this.m_dh.getI18nEntry(namespace, parameter, identifier, this.m_configuration.getProperty(this.CONFIG_LANG));
		        	}
		            return (value == null ? parameter : value);
				} catch (SQLException ex) {
					this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
				} catch (ClassNotFoundException ex) {
					this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}
        }
        this.m_logger.warning("Identifier {" + identifier + "} is not valid.");
        return "";        
    }
    
    public void loadData() {
    	String db_path = PathResolver.getInstance(PIMRuntime.getInstance()).resolve(this.getDatabase()); 
    	db_path = StringUtils.replaceString(db_path, "\\", "/");
		File db = new File(db_path + ".properties");
		boolean initialize = false;
		if (!db.exists())  {
			initialize = true;
		}
    	this.m_dh = new I18nDatabaseHandler("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:"+db_path, "sa", "", initialize);
    	try {
    		this.m_dh.connect();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    public void saveData() { 
    	try {
    		this.m_dh.commit();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    public void setString(String namespace, String parameter, String identifier, String language, String value) {
        if (this.isIdentifier(identifier) && this.m_dh!=null) {
        	this.m_refreshTime = System.currentTimeMillis();
            try {
				this.m_dh.setI18nEntry(namespace, parameter, identifier, language, value);
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
        } else {
            this.m_logger.severe("Identifier {" + identifier + "} not valid.");
        }        
    }
    
    public String getConfigurableID() {
        return this.ID;
    }
    
    public String getNamespace() {
        return this.NAMESPACE;
    }
    
    public void setConfiguration(Properties configuration) {
        this.m_configuration = configuration;    
    }
    
    private String getDatabase(){
        String database = this.m_configuration.getProperty(this.CONFIG_KEY);
        if (database==null) {
            database = "";
            this.m_logger.severe("Attribute database was not set in configuration. Usage of <"+this.ID+"> not possible.");
        }
        return database;
    }
    
    private boolean isIdentifier(String identifier){
        this.buildIdentifiers();
        return this.m_identifiers.contains(identifier);
    }

	public void removeNamespace(String namespace) {
		if (this.m_dh!=null) {
			this.m_refreshTime = System.currentTimeMillis();
			try {
				this.m_dh.deleteI18nNamespace(namespace);
			} catch (SQLException e) {
				this.m_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	private void buildIdentifiers() {
		if (this.m_identifiers!=null) return;
		
        String ident = this.m_configuration.getProperty(this.CONFIG_IDENTIFIER, "label,description");
        if (ident != null) {
            StringTokenizer st = new StringTokenizer(ident, ",");
            
            this.m_identifiers = new ArrayList(st.countTokens());            
            
            while (st.hasMoreTokens()) {
            	this.m_identifiers.add(st.nextToken().trim());
            }
            Collections.sort(m_identifiers);
        }
	}

	public void startup() {
		PIMRuntime.getInstance().getConfigurableNotifier().register(this);
		this.loadData();  
		boolean keepAlive = this.m_configuration!=null && this.m_configuration.getProperty(CONFIG_KEEPALIVE, "false").equalsIgnoreCase("true");
		this.m_logger.info("DatabaseI18nManager keep-alive is switched "+(keepAlive ? "on." : "off."));
		if (!keepAlive) {
			this.isActive = true;
			Thread t = new Thread(new ConnectionObserver());
			t.setName("DatabaseI18nManager-ConnectionObserver");
			t.setName("JAM-DatabaseI18nManagerConnectionObserver-Thread-(deamon)");
			t.setDaemon(true);
			t.start();
		}
	}

	public void shutdown() {
		try {
			this.isActive = false;
			if (this.m_dh!=null && this.m_dh.isConnected())
			this.m_dh.disconnect();
		} catch (SQLException e) {
			this.m_logger.log(Level.SEVERE, e.getMessage(), e);
		}
		PIMRuntime.getInstance().getConfigurableNotifier().unregister(this);
	}
    
}
