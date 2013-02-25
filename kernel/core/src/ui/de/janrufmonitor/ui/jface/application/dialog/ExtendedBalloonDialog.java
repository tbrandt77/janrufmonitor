package de.janrufmonitor.ui.jface.application.dialog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.event.*;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.ui.jface.application.RendererRegistry;
import de.janrufmonitor.ui.jface.application.controls.HyperLink;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.ui.swt.SWTExecuter;
import de.janrufmonitor.ui.swt.SWTImageManager;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.PathResolver;

public class ExtendedBalloonDialog extends AbstractBalloonDialog implements IEventSender, IEventReceiver {

	private class LabelAttributes {
		RGB foregroundColor;
		int fontSize = 0;
		int fontStyle = SWT.NORMAL;
		
		public int getFontSize() {
			return fontSize;
		}
		public void setFontSize(int fontSize) {
			this.fontSize = fontSize;
		}
		public int getFontStyle() {
			return fontStyle;
		}
		public void setFontStyle(int fontStyle) {
			this.fontStyle = fontStyle;
		}
		public RGB getForegroundColor() {
			return foregroundColor;
		}
		public void setForegroundColor(RGB foregroundColor) {
			this.foregroundColor = foregroundColor;
		}
		
	}
	
	private class ContentMatrix {
		String content;
		
		public ContentMatrix(String s) {
			this.content = s;
		}
		
		public String[][] getMatrix() {			
			String[] rows = content.split(";");
			int rowCount = rows.length;
			
			String[][] matrix = new String[rowCount][];
			for (int i=0;i<rows.length;i++) {
				matrix[i] = rows[i].split(",");
			}
			
			return matrix;
		}
		
		public int getRowSpan(int col, int startrow, String[][] matrix) {
			if (matrix==null) matrix = getMatrix();
			int span = 0;
			for (int i=startrow;i<matrix.length;i++) {
				if (i==startrow && matrix[i][col].trim().length()==0) return 0;
				if (matrix[i][col].trim().length()==0) span++;
			}
			return (span>0 ? ++span : span);
		}
		
		public int getRowCount(String[][] matrix) {
			if (matrix==null) matrix = getMatrix();
			return matrix.length;
		}
		
		public int getColCount(String[][] matrix) {
			if (matrix==null) matrix = getMatrix();
			return matrix[0].length;
		}
		
	}
	
	public static String NAMESPACE = "ui.jface.application.dialog.Dialog";

	private static String CFG_CUSTOM_DIALOG_PREFIX = "dialog.";
	private static String CFG_CUSTOM_DIALOG_CONTENT = CFG_CUSTOM_DIALOG_PREFIX + "content";
	private static String CFG_CUSTOM_DIALOG_CONTENT_CLIR = CFG_CUSTOM_DIALOG_PREFIX + "content.clir";
	private static String CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE = ".font.rel-size";
	private static String CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR = ".font.color";
	private static String CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE = ".font.style";
	private static String CFG_CUSTOM_DIALOG_POSTFIX_CLIR = ".clir";
	
	private static int m_instance_count = -1;
	
	public ExtendedBalloonDialog(Properties config, ICall call) {
		super(DisplayManager.getDefaultDisplay(), SWT.ON_TOP | SWT.CLOSE | SWT.TITLE);
	
		if (m_instance_count>2) m_instance_count = -1;
		m_instance_count++;
		
		this.m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);	

		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.register(this);
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED));   		
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
		eventBroker.register(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLMARKEDSPAM));
		
		this.m_configuration = config;
		this.m_call = call;
		
		this.setAnchor(SWT.NONE);
		
		this.setText(this.getI18nManager().getString(
			this.getNamespace(),
			(isOutgoing(call)? "outgoing" : "call"),
			"label",
			this.getLanguage()
		));
		
		this.setImage(SWTImageManager.getInstance(this.getRuntime()).get((isOutgoing(call) ? IJAMConst.IMAGE_KEY_OUTGOING_GIF : IJAMConst.IMAGE_KEY_ACCEPTED_GIF)));
	}
	
	private String getDialogContent() {
		String c = this.getConfiguration().getProperty(
				CFG_CUSTOM_DIALOG_CONTENT,
				//"%r:imagepreviewbig%,%calltime%,%r:providerlogo%,%r:tellowsimg%; ,MSN: %msn% (%msnalias%), , ; ,%callername%, , ; ,%callernumber%, , ; ,%cip%, , "
				"%r:imagepreviewbig%,%calltime%,%r:providerlogo%; ,MSN: %msn% (%msnalias%), ; ,%callername%, ; ,%callernumber%, ; ,%cip%, "
				//"%r:imagepreview%,%callername%,%r:providerlogo%; ,%callernumber%, "
			);
		
		return c;
	}
	
	private String getDialogContentCLIR() {
		String c = this.getConfiguration().getProperty(
				CFG_CUSTOM_DIALOG_CONTENT_CLIR,
				"%calltime%;MSN: %msn% (%msnalias%);%callernumber%;%cip%"
			);
		
		return c;
	}
	
	private void validConfiguration() {
		String e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR, "0,0,0");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE, "-3");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR, "0,0,0");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE, "-3");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE, "1");
		}	
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%callername%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%callername%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE, "1");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%callernumber%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%callernumber%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE, "-3");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR, "0,0,0");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE, "-3");
		}
		
		// CLIR settings
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%callernumber%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%callernumber%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "1");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "0,0,0");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%calltime%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "-3");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "0,0,0");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "-3");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%msn%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "1");
		}	
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "0,0,0");
		}
		e = this.getConfiguration().getProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR);
		if (e==null) {
			this.getConfiguration().setProperty(CFG_CUSTOM_DIALOG_PREFIX+"%cip%"+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE + CFG_CUSTOM_DIALOG_POSTFIX_CLIR, "-3");
		}	
		
		if (m_logger.isLoggable(Level.INFO)) {
			Properties c = this.getConfiguration();
			try {
				FileOutputStream fos = new FileOutputStream(PathResolver.getInstance(getRuntime()).getLogDirectory()+"~dialog.inf.log");
				c.store(fos, "");
				fos.flush();
				fos.close();
			} catch (IOException ex) {	
			}
		}
	}
	
	private void prepareDialog(Composite c, Shell s) {
		this.validConfiguration();
		ContentMatrix m = null;
		if (this.isCliredCaller()) {
			m = new ContentMatrix(getDialogContentCLIR());
		} else {
			m = new ContentMatrix(getDialogContent());
		}
		
		String[][] matrix = m.getMatrix();
		c.setLayout(new GridLayout(m.getColCount(matrix), false));
		int rowCount = m.getRowCount(matrix);
		int colCount = m.getColCount(matrix);
		for (int i=0;i<rowCount;i++) {
			for (int j=0;j<colCount;j++) {
				String element = (matrix[i][j].trim().length()>0 ? matrix[i][j] : null);
				Image img = null;
				if (element!=null) {
					ITableCellRenderer tcr = getRenderer(element);
					if (tcr!=null) {
						tcr.updateData(this.getCall());
						img = tcr.renderAsImage();
						if (img==null) element = tcr.renderAsText();
					} else {
						element = Formatter.getInstance(this.getRuntime()).parse(element, this.getCall());						
					}
					if (element.equalsIgnoreCase(matrix[i][j])) element = "";
				}
				
				createLabel(c, 
							m.getRowSpan(j, i, matrix), 
							element, 
							img, 
							getLabelAttributes(getElementID((matrix[i][j].trim().length()>0 ? matrix[i][j] : null))));
			}
		}
		
	}
	
	private String getElementID(String s) {
		if (s==null || s.trim().length()==0) return null;
		if (s.indexOf("%")>-1) {
			String id = s.substring(
					s.indexOf("%"),
					s.indexOf("%", s.indexOf("%")+1)+1	
				);
			return id;
		}
		return null;
	}
	
	private ITableCellRenderer getRenderer(String s) {
		if (s.indexOf("%r:")>-1) {
			String id = s.substring(
					s.indexOf("%r:")+"%r:".length(),
					s.indexOf("%", s.indexOf("%r:")+1)	
				);
			return RendererRegistry.getInstance().getRenderer(id.toLowerCase());
		}
		return null;
	}
	
	private LabelAttributes getLabelAttributes(String c) {
		if (c==null) return new LabelAttributes();
		c=c.trim();
		if (!c.startsWith("%")) return new LabelAttributes();
		LabelAttributes la = new LabelAttributes();

		la.setForegroundColor(this.getColor(c));
		la.setFontSize(getFontSize(c));
		la.setFontStyle(getFontStyle(c));
		
		return la;
	}
	
	private void createLabel(Composite c, int span, String content, Image image, LabelAttributes la) {
		if (content==null && image==null) return;
		
		Label l = new Label(c, la.getFontStyle());
		GridData gd = new GridData();
		gd.verticalSpan = span;
		gd.horizontalIndent = 5;
		if (content!=null)
			l.setText(content);
		
		if (image!=null)
			l.setImage(image);
		if (la.getForegroundColor()!=null)
			l.setForeground(new Color(DisplayManager.getDefaultDisplay(), la.getForegroundColor()));
		
		Font initialFont = l.getFont();
		FontData[] fontData = initialFont.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(la.getFontSize());
			fontData[i].setStyle(la.getFontStyle());
		}
		Font newFont = new Font(DisplayManager.getDefaultDisplay(), fontData[0]);
		l.setFont(newFont);
		l.pack();
		
		l.setBackground(c.getBackground());			
		l.setLayoutData(gd);	
	}
	
	private RGB getColor(String c) {
		int red = 0;
		int green = 0;
		int blue = 0;
    	
		String colors = null;
		if (this.isUseMsnColors())
			colors = getMsnColor(this.getCall().getMSN().getMSN());
		if (colors==null) {		
			colors = this.getConfiguration().getProperty(
					CFG_CUSTOM_DIALOG_PREFIX+c+CFG_CUSTOM_DIALOG_POSTFIX_FONTCOLOR + (this.isCliredCaller() ? CFG_CUSTOM_DIALOG_POSTFIX_CLIR : ""), 
					this.getConfiguration().getProperty(CFG_FONTCOLOR, "255,0,0")
				);
		}
		StringTokenizer st = new StringTokenizer(colors, ",");
		red = Integer.parseInt(st.nextToken());
		green = Integer.parseInt(st.nextToken());
		blue = Integer.parseInt(st.nextToken());
		return new RGB(red, green, blue);
	}
	
	private int getFontStyle(String c) {
		String styleString = this.getConfiguration().getProperty(
				CFG_CUSTOM_DIALOG_PREFIX+c+CFG_CUSTOM_DIALOG_POSTFIX_FONTSTYLE + (this.isCliredCaller() ? CFG_CUSTOM_DIALOG_POSTFIX_CLIR : ""),
				"0"
			);
		return Integer.parseInt(styleString);   
	}
	
	private int getFontSize(String c) {
		int size = 0;
		String sizeString = this.getConfiguration().getProperty(CFG_FONTSIZE, "11");
		try {	
			size = Integer.parseInt(sizeString);
		} catch (NumberFormatException e) {
			this.m_logger.log(Level.WARNING, sizeString+" is not a valid parsable parameter.");
			size = 11;
		}
		
		sizeString = this.getConfiguration().getProperty(
				CFG_CUSTOM_DIALOG_PREFIX+c+CFG_CUSTOM_DIALOG_POSTFIX_FONTSIZE + (this.isCliredCaller() ? CFG_CUSTOM_DIALOG_POSTFIX_CLIR : ""),
				"0"
			);
		if (sizeString.startsWith("+") && sizeString.length()>1) sizeString = sizeString.substring(1);
		if (sizeString.length()>0 && !sizeString.equalsIgnoreCase("0")) {
			try {	
				size += Integer.parseInt(sizeString);
			} catch (NumberFormatException e) {
				this.m_logger.log(Level.WARNING, sizeString+" is not a valid parsable parameter.");
			}
		}
		return size;   
	}
	
	public synchronized void createDialog() {		
		Composite c = this.getContents();
		
		prepareDialog(c, getShell());
	
		final Color color = new Color(getShell().getDisplay(), 255, 255, 225);		
		
	    // Buttons
	    Composite buttonBar = new Composite(c, SWT.NONE);
	    GridData gd = new GridData();
	    gd.horizontalSpan = 2;
	    
	    // check for plugins
	    List plugins = this.getPlugins(this.getConfiguration().getProperty("pluginlist", ""));
	    
	    GridLayout gl = new GridLayout(Math.max(5, (plugins.size()+2)), false);
	    gl.marginRight = 20;
	    
	    buttonBar.setLayout(gl);
	    buttonBar.setLayoutData(gd);
	    buttonBar.setBackground(color);

		// check for active reject service
		IService rejectService = this.getRuntime().getServiceFactory().getService("Reject");
		if (rejectService!=null && rejectService.isEnabled()) {
			final HyperLink reject = new HyperLink(buttonBar, SWT.LEFT);
			reject.setBackground(color);
			reject.setText(
				this.getI18nManager().getString(this.getNamespace(), "reject", "label", this.getLanguage())	
			);
			reject.pack();
			
			reject.addMouseListener( 
				new MouseAdapter() {
					public void mouseDown(MouseEvent e) {
						if (e.button==1) {
							reject.setEnabled(false);
							reject.setText(
								getI18nManager().getString(getNamespace(), "rejected", "label", getLanguage())	
							);
							reject.setUnderline(color);
							reject.setActiveUnderline(color);
							reject.setHoverUnderline(color);
							reject.pack(true);
							IEventBroker eventBroker = getRuntime().getEventBroker();
							if (m_call!=null)
								m_call.setAttribute(getRuntime().getCallFactory().createAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS, IJAMConst.ATTRIBUTE_VALUE_REJECTED));
							eventBroker.send(ExtendedBalloonDialog.this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED, m_call));
						}
					}
				}
			);
		}

		if (this.isAssignement() && !this.isCliredCaller()) {
			final IDialogPlugin assignPlugin = new AssignPlugin();
			assignPlugin.setDialog(this);
			
			HyperLink hl = new HyperLink(buttonBar, SWT.LEFT);
			hl.setText(assignPlugin.getLabel());
		    hl.setBackground(color);
			hl.pack();
			hl.addMouseListener( 
				new MouseAdapter() {
					public void mouseDown(MouseEvent e) {
						if (e.button==1) {
							assignPlugin.run();
						}
					}
				}
			);
		} 
		
		// add plugins
		String classString = null;
		for (int i=0,j=plugins.size();i<j;i++) {
			classString = this.getConfiguration().getProperty((String) plugins.get(i));
			if (classString!=null && classString.trim().length()>0) {
				try {
					Class classObject = Thread.currentThread().getContextClassLoader().loadClass(classString);
					final IDialogPlugin plugin = (IDialogPlugin) classObject.newInstance();
					plugin.setDialog(this);
					plugin.setID((String) plugins.get(i));
					if (plugin.isEnabled()) {			
						HyperLink hl = new HyperLink(buttonBar, SWT.LEFT);
						hl.setText(plugin.getLabel());
					    hl.setBackground(color);
						hl.pack();
						hl.addMouseListener( 
							new MouseAdapter() {
								public void mouseDown(MouseEvent e) {
									if (e.button==1) {
										plugin.run();
									}
								}
							}
						);						
					}
				} catch (ClassNotFoundException e) {
					this.m_logger.warning("Class not found: "+classString);
				} catch (InstantiationException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (IllegalAccessException e) {
					this.m_logger.log(Level.SEVERE, e.getMessage(), e);	
				}		
			}
		}
		color.dispose();
		c.pack();
	}
	
	public String getNamespace() {
		return ExtendedBalloonDialog.NAMESPACE;
	}

	public void close() {
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		eventBroker.unregister(this);
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED));
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));  
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));  
		eventBroker.unregister(this, eventBroker.createEvent(IEventConst.EVENT_TYPE_CALLMARKEDSPAM));  

		m_instance_count--;
		m_instance_count = Math.max(m_instance_count, -1);
		
		if (!this.getShell().isDisposed()){
			this.setImage(null);
			super.close();
		}
			
	}

	public void received(IEvent event) {
		if (event.getType() == IEventConst.EVENT_TYPE_CALLCLEARED ||
			event.getType() == IEventConst.EVENT_TYPE_CALLACCEPTED ||
			event.getType() == IEventConst.EVENT_TYPE_CALLREJECTED) {
				
				if (this.getShowTime() == -1) {
					new SWTExecuter(getID()) {
						protected void execute() {
							close();
						}
					}.start();
				} 
			}
		if (event.getType() == IEventConst.EVENT_TYPE_CALLMARKEDSPAM) {
			Object o = event.getData();
			if (o instanceof ICall) {
				this.m_call = (ICall)o;
			}
		}
	}

	protected String getID() {
		return "ExtendedBalloonDialog";
	}

	public String getReceiverID() {
		return this.getID();
	}

	public int getPriority() {
		return 0;
	}
	
	protected void setDialogPosition(Shell shell) {
		String position = this.getConfiguration().getProperty(CFG_POSITION, "left-top");
		if (DisplayManager.getDefaultDisplay()==null) {
			this.m_logger.severe("Could not get monitor device object for dialog.");
			return;
		}
		Monitor primaryMonitor = DisplayManager.getDefaultDisplay().getPrimaryMonitor ();

		int offset = m_instance_count * 15;
		
		int x = 0, y = 0;

		// added 2008/04/07: set minimal width and height
		shell.setBounds(
			x,
			y,
			Math.max(shell.getBounds().width, 165),
			Math.max(shell.getBounds().height, 115)
		);

		if (position.equalsIgnoreCase("left-top")) {
			x=5 + offset;
			y=5 + offset;
		}
		if (position.equalsIgnoreCase("right-top") || position.length()==0) {
			y=5 + offset;
			x=primaryMonitor.getClientArea().width - shell.getBounds().width - 30 - offset;
		}
		if (position.equalsIgnoreCase("left-bottom")) {
			x=5;
			y=primaryMonitor.getClientArea().height - shell.getBounds().height - 35 - offset;
		}
		if (position.equalsIgnoreCase("right-bottom")) {
			x=primaryMonitor.getClientArea().width - shell.getBounds().width - 30 - offset;
			y=primaryMonitor.getClientArea().height - shell.getBounds().height - 35 - offset;
		}
		if (position.equalsIgnoreCase("center")) {
			x=(primaryMonitor.getClientArea().width/2) - (shell.getBounds().width/2) - 1 - offset;
			y=(primaryMonitor.getClientArea().height/2) - (shell.getBounds().height/2) - 35 - offset;
		}
		
		// dialog has free defined position
		if (this.isFreePositioning()) {
			x = this.getFreePosX();
			y = this.getFreePosY();
		}

		shell.setBounds(
			x,
			y,
			shell.getBounds().width,
			shell.getBounds().height
		);
	}
	
	public String getSenderID() {
		return this.getID();
	}
}
