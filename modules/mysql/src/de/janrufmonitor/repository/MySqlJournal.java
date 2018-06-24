package de.janrufmonitor.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.framework.event.IEventReceiver;
import de.janrufmonitor.repository.db.AbstractCallDatabaseHandler;
import de.janrufmonitor.repository.db.ICallDatabaseHandler;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.DateFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.ItemCountFilter;
import de.janrufmonitor.repository.filter.MonthYearFilter;
import de.janrufmonitor.repository.filter.YearFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;
import de.janrufmonitor.util.string.StringUtils;

public class MySqlJournal extends AbstractDatabaseCallManager implements IRemoteRepository, IEventReceiver {

	private class MySqlHandler extends AbstractCallDatabaseHandler {

		private IRuntime m_runtime;

		public MySqlHandler(String driver, String connection, String user, String password, boolean initialize) {
			super(driver, connection, user, password, initialize);
		}
		
		protected void addPreparedStatements() throws SQLException {
			super.addPreparedStatements();
			
			// prepare MySQL statements
			m_preparedStatements.put("INSERT_CALL", m_con.prepareStatement("INSERT INTO calls (uuid, cuuid, country, areacode, number, msn, cip, cdate, ndate, content) VALUES (?, ?, ?, ?, ?, ?, ?, ?,FROM_UNIXTIME(cdate/1000), ?);"));
			m_preparedStatements.put("UPDATE_CALL", m_con.prepareStatement("UPDATE calls SET cuuid=?, country=?, areacode=?, number=?, msn=?, cip=?, cdate=?, ndate=FROM_UNIXTIME(cdate/1000) , content=? WHERE uuid=?;"));		
			
		}

		protected IRuntime getRuntime() {
			if (this.m_runtime==null)
				this.m_runtime = PIMRuntime.getInstance();
			return this.m_runtime;
		}

		protected void createTables() throws SQLException {
			if (!isConnected()) throw new SQLException ("Database is disconnected.");

			Statement stmt = m_con.createStatement();		
			try {
				stmt.execute("DROP TABLE attributes;");
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			} 
			try {
				stmt.execute("DROP TABLE calls;");
			} catch (SQLException e) {
				this.m_logger.warning(e.getMessage());
			} 
			
			stmt.execute("CREATE TABLE attributes (ref VARCHAR(36), name VARCHAR(64), value VARCHAR(2048));");
			stmt.execute("CREATE TABLE calls (uuid VARCHAR(36) PRIMARY KEY, cuuid VARCHAR(36), country VARCHAR(8), areacode VARCHAR(16), number VARCHAR(64), msn VARCHAR(16), cip VARCHAR(4), cdate BIGINT, ndate TIMESTAMP, content TEXT("+Short.MAX_VALUE+"));");
		}
		
		protected boolean isInitializing() {
			try {
				if (!isConnected()) return false;
			} catch (SQLException e) {
				return false;
			}

			try {
				Statement stmt = m_con.createStatement();	
				stmt.execute("SELECT uuid FROM calls LIMIT 0,1;");
				// table exists
			} catch (SQLException e) {
				return true;
			} 
			
			try {
				Statement stmt = m_con.createStatement();	
				stmt.execute("SELECT ndate FROM calls LIMIT 0,1;");
				// field exists
			} catch (SQLException e) {
				this.m_logger.warning("Found old mysql DB. Try to convert...");
				try {
					Statement stmt = m_con.createStatement();
					stmt.execute("ALTER TABLE calls ADD COLUMN ndate TIMESTAMP NULL DEFAULT NULL AFTER cdate;");
				} catch (SQLException e1) {
					this.m_logger.log(Level.SEVERE, e1.getMessage(), e1);
				}
			} 
			
			return false;
		}
		

		protected ICallList buildCallList(IFilter[] filters) throws SQLException {
			return buildCallList(filters, -1, -1);
		}
		
		protected ICallList buildCallList(IFilter[] filters, int count, int offset) throws SQLException {
			return this.buildCallList(filters, count, offset, null);
		}

		@Override
		protected ICallList buildCallList(IFilter[] filters, int count, int offset, ISearchTerm[] searchTerms) throws SQLException {
			ICallList cl = this.getRuntime().getCallFactory().createCallList();

			if (!isConnected()) return cl;

			Statement stmt = m_con.createStatement();

			ResultSet rs = stmt.executeQuery(prepareStatement(filters, count, offset, false, searchTerms));
			while (rs.next()) {
				try {
					cl.add(Serializer.toCall(rs.getString("content").getBytes(), this.getRuntime()));
				} catch (SerializerException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} 
			}	
			return cl;
		}
		
		private String createSearchTerm(ISearchTerm[] searchTerms) {
			if (searchTerms==null) return "";
			if (searchTerms.length==0) return "";
			
			StringBuffer sql = new StringBuffer();
			sql.append(" (");
			ISearchTerm st = null;
			for (int i=0, j=searchTerms.length;i<j;i++) {
				st = searchTerms[i];
				sql.append("content like '%");
				sql.append(StringUtils.replaceString(st.getSearchTerm(), "%", "")); // remove % signs in search term
				sql.append("%'");
				if (i<(j-1)) {
					sql.append(" ");
					sql.append(st.getOperator().toString());
					sql.append(" ");
				}
			}
			
			sql.append(")");
			return sql.toString();
		}
		
		private String prepareStatement(IFilter[] filters, int count, int offset, boolean isCounter, ISearchTerm[] searchTerms) {
			StringBuffer sql = new StringBuffer();
			// build SQL statement
			sql.append("SELECT");
			
			if (filters.length==1 && filters[0]==null) filters=null;
			
			if (filters!=null && filters.length>0) {
				int limit = -1;
				for (int i=0;i<filters.length;i++) {
					if (filters[i]!=null && filters[i].getType() == FilterType.ITEMCOUNT) {
						limit=((ItemCountFilter)filters[i]).getLimit(); 
					}
				}
				
				if (isCounter) {
					sql.append(" COUNT(*) ");
				} else {
					sql.append(" content ");
				}
				
				sql.append("FROM calls");
				
				if (hasAttributeFilter(filters))
					sql.append(", attributes");
				
			
				if (filters.length==1 && filters[0]!=null && filters[0].getType() == FilterType.ITEMCOUNT)
					sql.append("");
				else
					sql.append(" WHERE ");
				
				IFilter f = null;
				boolean isCallerFilter = false;
				boolean isMsnFilter = false;
				for (int i=0;i<filters.length;i++) {
					if (filters[i]!=null && filters[i].getType() == FilterType.ITEMCOUNT) {
						limit=((ItemCountFilter)filters[i]).getLimit(); 
						continue;
					}
					
					f = filters[i];
					if (f==null) continue;
					
					if (isCallerFilter && (f.getType()==FilterType.CALLER || f.getType()==FilterType.PHONENUMBER)) {
						if (i>0) sql.append(" OR ");
					} else if (isMsnFilter && f.getType()==FilterType.MSN) {
						if (i>0) sql.append(" OR ");
					} else {
						if (i>0 && filters[i-1].getType() != FilterType.ITEMCOUNT) sql.append(" AND ");
					}
					

					if (f.getType()==FilterType.DATE) {
						DateFilter df = (DateFilter)f;
						sql.append("(calls.cdate>");
						sql.append((df.getDateTo()==null ? new Date().getTime() : df.getDateTo().getTime()));
						sql.append(" AND calls.cdate<");
						sql.append((df.getDateFrom()==null ? new Date().getTime() : df.getDateFrom().getTime()));
						sql.append(")");
					}
					
					if (f.getType()==FilterType.YEAR) {
						YearFilter df = (YearFilter)f;
						sql.append("(calls.cdate>");
						sql.append((df.getDateFrom()==null ? new Date().getTime() : df.getDateFrom().getTime()));
						sql.append(" AND calls.cdate<");
						sql.append((df.getDateTo()==null ? new Date().getTime() : df.getDateTo().getTime()));
						sql.append(")");
					}
					
					if (f.getType()==FilterType.MONTH_YEAR) {
						MonthYearFilter df = (MonthYearFilter)f;
						sql.append("(calls.cdate>");
						sql.append((df.getDateFrom()==null ? new Date().getTime() : df.getDateFrom().getTime()));
						sql.append(" AND calls.cdate<");
						sql.append((df.getDateTo()==null ? new Date().getTime() : df.getDateTo().getTime()));
						sql.append(")");
					}
					
					if (f.getType()==FilterType.CALLER) {
						isCallerFilter = true;
						ICaller c = (ICaller)f.getFilterObject();
						IPhonenumber pn = c.getPhoneNumber();
						sql.append("(calls.country='");
						sql.append(pn.getIntAreaCode());
						sql.append("' AND calls.areacode='");
						sql.append(pn.getAreaCode());
						sql.append("' AND calls.number='");
						sql.append(pn.getCallNumber());
						sql.append("')");
					}
					
					if (f.getType()==FilterType.PHONENUMBER) {
						isCallerFilter = true;
						IPhonenumber pn = (IPhonenumber)f.getFilterObject();
						sql.append("(calls.country='");
						sql.append(pn.getIntAreaCode());
						sql.append("' AND calls.areacode='");
						sql.append(pn.getAreaCode());
						sql.append("' AND calls.number='");
						sql.append(pn.getCallNumber());
						sql.append("')");
					}			
					
					if (f.getType()==FilterType.CIP) {
						ICip cip = (ICip)f.getFilterObject();
						sql.append("calls.cip='");
						sql.append(cip.getCIP());
						sql.append("'");
					}		
					
					if (f.getType()==FilterType.MSN) {
						isMsnFilter = true;
						IMsn[] msn = (IMsn[])f.getFilterObject();
						if (msn!=null && msn.length>0) {
							sql.append("(");
							for (int j=0;j<msn.length;j++) {
								if (j>0) sql.append(" OR ");
								sql.append("calls.msn='");
								sql.append(msn[j].getMSN());
								sql.append("'");
							}
							sql.append(")");	
						}
					}			
					
					if (f.getType()==FilterType.UUID) {
						String[] uuids = (String[])f.getFilterObject();
						if (uuids!=null && uuids.length>0) {
							sql.append("(");
							for (int j=0;j<uuids.length;j++) {
								if (j>0) sql.append(" OR ");
								sql.append("calls.uuid='");
								sql.append(uuids[i]);
								sql.append("'");
							}
							sql.append(")");	
						}
					}		
					
					if (f.getType()==FilterType.ATTRIBUTE) {
						IAttributeMap m = ((AttributeFilter)f).getAttributeMap();
						if (m!=null && m.size()>0) {
							sql.append("(");
							sql.append("calls.uuid=attributes.ref AND (");
							Iterator iter = m.iterator();
							IAttribute a = null;
							while (iter.hasNext()) {
								a = (IAttribute) iter.next();
								sql.append("attributes.name='");
								sql.append(a.getName());
								sql.append("'");
								sql.append(" AND ");
								sql.append("attributes.value='");
								sql.append(a.getValue());
								sql.append("'");
								if (iter.hasNext())
									sql.append(" OR ");
							}
							sql.append("))");	
						}
					}							
				}
				if (searchTerms!=null && searchTerms.length>0){ 
					sql.append(" AND");	
					sql.append(createSearchTerm(searchTerms));	
				}
				if (limit>0 && !isCounter) {
					sql.append(" ORDER BY cdate DESC");	
				}			
				
			} else {
				if (isCounter) {
					sql.append(" COUNT(*) ");
				} else {
					sql.append(" content ");
				}
				sql.append(" FROM calls");
				if (count>0 && offset>=0 ) {
					sql.append(" AS rtable");
				}
				if (searchTerms!=null && searchTerms.length>0){ 
					sql.append(" WHERE");	
					sql.append(createSearchTerm(searchTerms));	
				}
			}
			
			if (count>0 && offset>=0 ) {
				sql.append(" LIMIT ");
				sql.append(offset);
				sql.append(",");
				sql.append(count);
			}

			sql.append(";");
			
			
			this.m_logger.info(sql.toString());
			
			return sql.toString();
		}
		
		private boolean hasAttributeFilter(IFilter[] filters) {
			IFilter f = null;
			for (int i=0;i<filters.length;i++) {
				f = filters[i];
				if (f!=null && f.getType()==FilterType.ATTRIBUTE) return true;
			}
			return false;
		}
		
		private boolean hasItemCountFilter(IFilter[] filters) {
			IFilter f = null;
			for (int i=0;i<filters.length;i++) {
				f = filters[i];
				if (f!=null && f.getType()==FilterType.ITEMCOUNT) return true;
			}
			return false;
		}
		
		protected int buildCallCount(IFilter[] filters) throws SQLException {
			return this.buildCallCount(filters, null);
		}

		@Override
		protected int buildCallCount(IFilter[] filters, ISearchTerm[] searchTerms) throws SQLException {
			if (!isConnected()) return 0;
			
			int maxresult = -1;
			
			if (hasItemCountFilter(filters)) {
				for (int i=0;i<filters.length;i++) {
					if (filters[i]!=null && filters[i].getType() == FilterType.ITEMCOUNT) {
						maxresult = ((ItemCountFilter)filters[i]).getLimit();
					}
				}
			}

			Statement stmt = m_con.createStatement();

			ResultSet rs = stmt.executeQuery(prepareStatement(filters, -1, -1, true, searchTerms));
			while (rs.next()) {
				if (maxresult==-1) {
					return Math.max(0, rs.getInt(1)); 
				}
				return Math.min(maxresult, rs.getInt(1));
			}	
			return 0;
		}
		
		public void commit() throws SQLException {
			// do nothing for mysql, since auto-commit is active
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Ignoring COMMIT call, due to auto-commit.");
		}
		
		public void rollback() throws SQLException {
			// do nothing for mysql, since auto-rollback is active
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Ignoring ROLLBACK call, due to auto-commit.");
		}
	}
	
	private static String ID = "MySqlJournal";
	private static String NAMESPACE = "repository.MySqlJournal";
	
	private static String CFG_DB_SERVER = "dbserver";
	private static String CFG_DB_DB = "dbdb";
	private static String CFG_DB_PORT = "dbport";
	private static String CFG_DB_USER = "dbuser";
	private static String CFG_DB_PASSWORD = "dbpassword";
	private static String CFG_KEEP_ALIVE= "keepalive";
	
	private IRuntime m_runtime;

	public MySqlJournal() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getID() {
		return MySqlJournal.ID;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime==null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

	public String getNamespace() {
		return MySqlJournal.NAMESPACE;
	}

	protected ICallDatabaseHandler getDatabaseHandler() {
		if (this.m_dbh==null) {
			this.m_dbh = new MySqlHandler("com.mysql.jdbc.Driver", "jdbc:mysql://"+this.m_configuration.getProperty(CFG_DB_SERVER, "localhost")+":"+this.m_configuration.getProperty(CFG_DB_PORT, "3306")+"/"+this.m_configuration.getProperty(CFG_DB_DB, "journal"), this.m_configuration.getProperty(CFG_DB_USER), this.m_configuration.getProperty(CFG_DB_PASSWORD), false);
			this.m_dbh.setKeepAlive((m_configuration.getProperty(CFG_KEEP_ALIVE, "false").equalsIgnoreCase("true")? true : false));
		}	
		return this.m_dbh;
	}

	public void received(IEvent event) {
		if (event.getType() == IEventConst.EVENT_TYPE_RETURNED_HIBERNATE) {
			if (this.m_logger.isLoggable(Level.INFO))
				this.m_logger.info("Restarting MySQL connection for journal after hibernate mode.");
			
			if (this.m_dbh!=null) {
				try {
					this.m_dbh.disconnect();
				} catch (SQLException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				}
				this.m_dbh = null;
			}
		}
	}

	public String getReceiverID() {
		return ID;
	}
	
	@Override
	public void shutdown() {
		IEventBroker eb = getRuntime().getEventBroker();
		eb.register(this, eb.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));

		super.shutdown();
	}
	
	@Override
	public void startup() {
		super.startup();
		
		IEventBroker eb = getRuntime().getEventBroker();
		eb.register(this, eb.createEvent(IEventConst.EVENT_TYPE_RETURNED_HIBERNATE));
	}



}
