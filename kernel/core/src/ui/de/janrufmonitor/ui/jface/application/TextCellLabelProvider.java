package de.janrufmonitor.ui.jface.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.swt.graphics.Color;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.types.IReadCallRepository;
import de.janrufmonitor.repository.types.IWriteCallRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.journal.Journal;
import de.janrufmonitor.ui.jface.application.journal.JournalConfigConst;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;
import de.janrufmonitor.ui.swt.DisplayManager;

public class TextCellLabelProvider extends org.eclipse.jface.viewers.ColumnLabelProvider implements JournalConfigConst{

	protected String m_renderer;
	private Map m_colors;
	private IRuntime m_runtime;
	private Properties m_configuration;
	
	public TextCellLabelProvider(String rendererID) {
		this.m_renderer = rendererID;
	}

	public String getText(Object o) {
		ITableCellRenderer r = RendererRegistry.getInstance().getRenderer(
			this.m_renderer
		);
		if (r!=null) {
			r.updateData(o);
			return r.renderAsText();
		}
		return "";
	}
	
	public Color getBackground(Object o) {
		if (o instanceof ICall) o = ((ICall) o).getCaller();
		if (o instanceof ICaller) {
			if (((ICaller)o).getAttributes().contains(IJAMConst.ATTRIBUTE_NAME_SPAM_COLOR)) {
				String color = ((ICaller)o).getAttribute(IJAMConst.ATTRIBUTE_NAME_SPAM_COLOR).getValue();
				StringTokenizer cs = new StringTokenizer(color, ",");
				if (cs.countTokens()==3)
					return new Color(DisplayManager.getDefaultDisplay(),
							Integer.parseInt(cs.nextToken()),
							Integer.parseInt(cs.nextToken()),
							Integer.parseInt(cs.nextToken()));
			}
		}
 		return null;
	}

	public Color getForeground(Object o) {
		if (o instanceof ICall) {
			if (isCallManagerReadOnly()) {
				return new Color(DisplayManager.getDefaultDisplay(),128,128,128);
			}
			Color c = getMsnColor(((ICall)o).getMSN().getMSN());
			if (c!=null) {
				return c;
			}
		}
		return null;
	}
	
	private Properties getConfiguration() {
		if (this.m_configuration==null) {
			this.m_configuration = this.getRuntime().getConfigManagerFactory().getConfigManager().getProperties(Journal.NAMESPACE);
		}
		return this.m_configuration;
	}
	
	private boolean isCallManagerReadOnly() {
		String man = this.getConfiguration().getProperty(CFG_REPOSITORY);
		if (man!=null && man.length()>0) {
			ICallManager m = PIMRuntime.getInstance().getCallManagerFactory().getCallManager(man);
			if (m!=null) {
				return (m.isSupported(IReadCallRepository.class) && !m.isSupported(IWriteCallRepository.class));
			}
		}
		return false;
	}
	
	
	private Color getMsnColor(String msn){
		if (this.m_colors==null) {
			this.m_colors = new HashMap();
			String colors = this.getConfiguration().getProperty(CFG_MSNFONTCOLOR, "[]");
			StringTokenizer st = new StringTokenizer(colors, "[");

			while (st.hasMoreTokens()) {
				String singleColor = st.nextToken();
				singleColor = singleColor.substring(0, singleColor.length()-1).trim();
				if (singleColor.length()>0) {
					StringTokenizer s = new StringTokenizer(singleColor, "%");
					while (s.hasMoreTokens()) {
						String key = s.nextToken();
						String color = s.nextToken();
						StringTokenizer cs = new StringTokenizer(color, ",");
						// only add if MSNs is existing
						if (this.getRuntime().getMsnManager().existMsn(
							this.getRuntime().getMsnManager().createMsn(key))) {
							
							this.m_colors.put(key, new Color(
								DisplayManager.getDefaultDisplay(),
								Integer.parseInt(cs.nextToken()),
								Integer.parseInt(cs.nextToken()),
								Integer.parseInt(cs.nextToken())
							));
						}
					}
				}
			}
		}
		
		if (this.m_colors.containsKey(msn)) {
			return (Color)this.m_colors.get(msn);
		}
		return null;	
	}
	
	private IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}


}
