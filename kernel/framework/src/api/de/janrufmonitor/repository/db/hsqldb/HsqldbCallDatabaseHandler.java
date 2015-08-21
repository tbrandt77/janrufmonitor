package de.janrufmonitor.repository.db.hsqldb;

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
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.repository.db.AbstractCallDatabaseHandler;
import de.janrufmonitor.repository.filter.AttributeFilter;
import de.janrufmonitor.repository.filter.DateFilter;
import de.janrufmonitor.repository.filter.FilterType;
import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.ItemCountFilter;
import de.janrufmonitor.repository.filter.MonthYearFilter;
import de.janrufmonitor.repository.filter.YearFilter;
import de.janrufmonitor.repository.search.ISearchTerm;
import de.janrufmonitor.util.io.Serializer;
import de.janrufmonitor.util.io.SerializerException;
import de.janrufmonitor.util.string.StringUtils;


public abstract class HsqldbCallDatabaseHandler extends AbstractCallDatabaseHandler {


	public HsqldbCallDatabaseHandler(String driver, String connection, String user, String password, boolean initialize) {
		super(driver, connection, user, password, initialize);
	}

	public void disconnect() throws SQLException {
		if (isConnected()) {		
			super.setInitializing(false);
			Statement st = m_con.createStatement();
			st.execute("SHUTDOWN");
		}
		super.disconnect();
	}
	
	public void commit() throws SQLException {
		if (isConnected()) {			
			Statement st = m_con.createStatement();
			st.execute("COMMIT");
		}
		super.commit();
	}
	
	protected void createTables() throws SQLException {
		if (!isConnected()) throw new SQLException ("Database is disconnected.");

		Statement stmt = m_con.createStatement();
		stmt.execute("DROP TABLE attributes IF EXISTS;");
		stmt.execute("DROP TABLE calls IF EXISTS;");
		stmt.execute("DROP TABLE versions IF EXISTS;");

		stmt.execute("CREATE TABLE versions (version VARCHAR(10));");
		stmt.execute("INSERT INTO versions (version) VALUES ('"+IJAMConst.VERSION_DISPLAY+"');");
		
		super.createTables();
	}
	
	protected ICallList buildCallList(IFilter[] filters) throws SQLException {
		return buildCallList(filters, -1, -1);
	}
	
	protected ICallList buildCallList(IFilter[] filters, int count, int offset) throws SQLException {
		return buildCallList(filters, count, offset, null);
	}
	
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
	
	private String prepareStatement(IFilter[] filters, int count, int offset, boolean isCounter, ISearchTerm[] searchTerms) {
		StringBuffer sql = new StringBuffer();
		// build SQL statement
		sql.append("SELECT");
		
		if (filters.length==1 && filters[0]==null) filters=null;
		
		// 2008/11/28: optimized attribute filter call
		if (filters!=null && filters.length>0 && hasOnlyAttributeFilter(filters)) {
			
			if (isCounter) {
				sql.append(" COUNT(calls.uuid) ");
			} else {
				sql.append(" content ");
			}
			
			sql.append("FROM attributes LEFT JOIN calls ON calls.uuid=attributes.ref AND ");
			
			IFilter f = null;
			for (int i=0;i<filters.length;i++) {
				f = filters[i];
				if (f.getType()==FilterType.ATTRIBUTE) {
					IAttributeMap m = ((AttributeFilter)f).getAttributeMap();
					if (m!=null && m.size()>0) {
						sql.append("(");
						sql.append("(");
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
		} else if (filters!=null && filters.length>0) {
			int limit = -1;
			for (int i=0;i<filters.length;i++) {
				if (filters[i]!=null && filters[i].getType() == FilterType.ITEMCOUNT) {
					limit=((ItemCountFilter)filters[i]).getLimit(); 
					if (!isCounter) {
						count = limit; // 2008/05/19: to be done since offset was introduced !!
						offset = 0;
					}					
				}
			}
			
			if (isCounter) {
				sql.append(" COUNT(calls.uuid) ");
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
					//limit=((ItemCountFilter)filters[i]).getLimit(); 
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
					if (!isCallerFilter) sql.append("(");
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
					if (isCallerFilter && ((i+1)>=filters.length || (filters[i+1] != null && (filters[i+1].getType()!=FilterType.CALLER)))) sql.append(")");
				}
				
				if (f.getType()==FilterType.PHONENUMBER) {
					if (!isCallerFilter) sql.append("(");
					isCallerFilter = true;
					IPhonenumber pn = (IPhonenumber)f.getFilterObject();
					sql.append("(calls.country='");
					sql.append(pn.getIntAreaCode());
					sql.append("' AND calls.areacode='");
					sql.append(pn.getAreaCode());
					sql.append("' AND calls.number='");
					sql.append(pn.getCallNumber());
					sql.append("')");
					if (isCallerFilter && ((i+1)>=filters.length || (filters[i+1] != null && (filters[i+1].getType()!=FilterType.PHONENUMBER)))) sql.append(")");
				}			
				
				if (f.getType()==FilterType.CIP) {
					ICip cip = (ICip)f.getFilterObject();
					sql.append("calls.cip='");
					sql.append(cip.getCIP());
					sql.append("'");
				}		
				
				if (f.getType()==FilterType.MSN) {
					IMsn[] msn = (IMsn[])f.getFilterObject();
					if (msn!=null && msn.length>0) {
						if (!isMsnFilter) sql.append("(");
						isMsnFilter = true; 
						sql.append("(");
						for (int j=0;j<msn.length;j++) {
							if (j>0) sql.append(" OR ");
							sql.append("calls.msn='");
							sql.append(msn[j].getMSN());
							sql.append("'");
						}
						sql.append(")"); 
						if (isMsnFilter && ((i+1)>=filters.length || (filters[i+1] != null && (filters[i+1].getType()!=FilterType.MSN)))) sql.append(")");
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
				sql.append(" COUNT(calls.uuid) ");
			} else {
				sql.append(" content ");
			}
			sql.append("FROM calls");
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
			sql.append(count);
			sql.append(" OFFSET ");
			sql.append(offset);
		}

		sql.append(";");
		
		if (this.m_logger.isLoggable(Level.INFO))
			this.m_logger.info(sql.toString());
		
		return sql.toString();
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

	private boolean hasOnlyAttributeFilter(IFilter[] filters) {
		boolean isAttributeFilter = true;
		IFilter f = null;
		for (int i=0;i<filters.length;i++) {
			f = filters[i];
			isAttributeFilter = isAttributeFilter && (f!=null && f.getType()==FilterType.ATTRIBUTE); 
		}
		return isAttributeFilter;
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
	
	public void setInitializing(boolean init) {
		super.setInitializing(init);
	}
}
