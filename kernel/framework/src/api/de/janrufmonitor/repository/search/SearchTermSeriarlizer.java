package de.janrufmonitor.repository.search;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import de.janrufmonitor.util.string.StringUtils;

public class SearchTermSeriarlizer {
	
		public String getSearchTermsToString(ISearchTerm[] st) {
			if (st==null) return "";
			StringBuffer sb = new StringBuffer();
			for (int i=0;i<st.length;i++) {
				sb.append(st[i].getSearchTerm());
				sb.append(" ");
				if (i+1<st.length) {
					sb.append(st[i].getOperator().toString());
					sb.append(" ");
				}
			}
			return StringUtils.urlEncode(sb.toString().trim());
		}
	
		public ISearchTerm[] getSearchTermsFromString(String st) {
			st = StringUtils.urlDecode(st);
			if (st!=null && st.trim().length()>0) {
				List terms = new ArrayList();
				StringTokenizer and_t = new StringTokenizer(st, Operator.AND.toString());
				final String[] ands = new String[and_t.countTokens()];
				int i=0;
				while (and_t.hasMoreTokens()) {
					ands[i] = and_t.nextToken().trim();
					i++;
				}
				
				for (i=0;i<ands.length;i++) {
					final String term = ands[i];
					final StringTokenizer or_t = new StringTokenizer(ands[i], Operator.OR.toString());
					if (or_t.countTokens()==1) {
						terms.add(new ISearchTerm() {
							public String getSearchTerm() {
								return term.trim();
							}

							public Operator getOperator() {
								return Operator.AND;
							}
							public String toString() {
								return term + "->"+Operator.AND.toString();
							}});
						or_t.nextToken();
					}
					while (or_t.hasMoreTokens()) {
						final String termo = or_t.nextToken().trim();
						terms.add(new ISearchTerm() {
							public String toString() {
								return termo + "->"+Operator.OR.toString();
							}

							public String getSearchTerm() {
								return termo;
							}

							public Operator getOperator() {
								return Operator.OR;
							}
							
							});
					}
				}
				
				ISearchTerm[] s = new ISearchTerm[terms.size()];
				for (int j=terms.size(), k=0;k<j;k++) {
					s[k] = (ISearchTerm) terms.get(k);
				}
				return s;
			}	
			return null;
		}
	
}
