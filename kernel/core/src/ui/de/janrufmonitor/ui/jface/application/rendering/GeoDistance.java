package de.janrufmonitor.ui.jface.application.rendering;

import java.text.DecimalFormat;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.monitor.PhonenumberAnalyzer;

import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.geo.GeoCoder;
import de.janrufmonitor.util.math.Distance;
import de.janrufmonitor.util.math.Point;

public class GeoDistance extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {
	
	private static String NAMESPACE = "ui.jface.application.rendering.GeoDistance";
	
	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				IAttributeMap m = ((ICaller)(this.m_o)).getAttributes();
				if (m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LAT) && m.contains(IJAMConst.ATTRIBUTE_NAME_GEO_LNG)) {
					return this.getDistance(Double.parseDouble(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_LNG).getValue()), Double.parseDouble(m.get(IJAMConst.ATTRIBUTE_NAME_GEO_LAT).getValue()));
				}				
			}
		}
		return "";
	}
	
	public String getID() {
		return "GeoDistance".toLowerCase();
	}

	public String getNamespace() {
		return NAMESPACE;
	}
	
	private String getDistance(double lng, double lat) {
		// get current location
		Point local = GeoCoder.getInstance().getLocalPosition();
		if (local==null) {
			if (PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty("service.GeoCoding", "local-geo-acc").length()>0) {
				local = new Point(
					Double.parseDouble(PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty("service.GeoCoding", "local-geo-lng")),
					Double.parseDouble(PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty("service.GeoCoding", "local-geo-lat")),
					Integer.parseInt(PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty("service.GeoCoding", "local-geo-acc"))
				);
				GeoCoder.getInstance().setLocalPosition(local);
			} else {
				String areacode = PIMRuntime.getInstance().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_AREACODE);
				if (areacode!=null && areacode.length()>0) {
					IPhonenumber pn = PhonenumberAnalyzer.getInstance().toPhonenumber(areacode + "1234567890", null);
					ICaller caller = Identifier.identifyDefault(PIMRuntime.getInstance(), pn);
					if (caller!=null) {
						local = GeoCoder.getInstance().getCoordinates(caller.getAttributes());
						if (local==null) return "";
						GeoCoder.getInstance().setLocalPosition(local);
					} else {
						return "";
					}
				} else {
					return "";
				}
			}
		}
		StringBuffer km = new StringBuffer(10);
		DecimalFormat df = new DecimalFormat("0.0");
		km.append(df.format(Distance.calculateDistance(new Point(lng, lat), local)));
		km.append(" km");
		return km.toString();
	}
	
}
