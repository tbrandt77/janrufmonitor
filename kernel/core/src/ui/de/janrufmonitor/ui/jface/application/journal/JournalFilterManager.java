package de.janrufmonitor.ui.jface.application.journal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.janrufmonitor.repository.filter.IFilter;
import de.janrufmonitor.repository.filter.MonthYearFilter;
import de.janrufmonitor.repository.filter.YearFilter;
import de.janrufmonitor.ui.jface.application.AbstractFilterManager;

public class JournalFilterManager extends AbstractFilterManager {

	public String getNamespace() {
		return Journal.NAMESPACE;
	}

	@Override
	public boolean hasRuntimeFilters() {
		return true;
	}

	@Override
	public IFilter[][] getRuntimeFilters() {
		String cfg = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(Journal.NAMESPACE, "rt_filters_years");
		int years = 1;
		if (cfg!=null && cfg.length()>0) {
			years = Integer.parseInt(cfg);
		}
		
		if (years<0) return new IFilter[][] {};
		
		int c_y = Calendar.getInstance().get(Calendar.YEAR);
		int c_m = Calendar.getInstance().get(Calendar.MONTH);
	
		List filters = new ArrayList(years*12+years);
		
		for (int y= c_y - years;y<=c_y;y++) {
			filters.add(new YearFilter(y));
			int max_month = 12;
			if (y == c_y) max_month = c_m+1;
			for (int m=1;m<=max_month;m++) {
				filters.add(new MonthYearFilter(y, m));
			}
		}
		
		IFilter[][] filters2 = new IFilter[filters.size()][1];
		for (int i=0;i<filters2.length;i++)
			filters2[i][0] = (IFilter) filters.get(i);
		
		return filters2;
	}
	
	

}
