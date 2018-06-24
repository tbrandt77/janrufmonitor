package de.janrufmonitor.ui.jface.application.comment.action;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.IService;
import de.janrufmonitor.service.comment.CommentCallerHandler;
import de.janrufmonitor.service.comment.CommentService;
import de.janrufmonitor.service.comment.api.ICommentCaller;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.comment.Comment;
import de.janrufmonitor.ui.swt.DisplayManager;

public class PDFHistoryAction extends AbstractAction {

	private static String NAMESPACE = "ui.jface.application.comment.action.PDFHistoryAction";
	
	private IRuntime m_runtime;

	public PDFHistoryAction() {
		super();
		this.setText(
			this.getI18nManager().getString(
				this.getNamespace(),
				"title",
				"label",
				this.getLanguage()
			)
		);
	}
	
	public IRuntime getRuntime() {
		if (this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "comment_pdfhistory";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public void run() {
		FileDialog dialog = new FileDialog (new Shell(DisplayManager.getDefaultDisplay()), SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.pdf"});
		dialog.setFilterNames(new String[] {getI18nManager().getString(getNamespace(), "pdffilter", "label", getLanguage())});
		dialog.setText(getI18nManager().getString(getNamespace(), "pdf", "label", getLanguage()));
		String filename = dialog.open();
		if (filename != null && filename.length()>0) {
			if (this.m_app.getApplication() instanceof Comment) {
				ICaller c = ((Comment)this.m_app.getApplication()).getCurrentCaller();
				IService srv = getRuntime().getServiceFactory().getService("CommentService");
				if (srv!=null && srv instanceof CommentService) {
					CommentCallerHandler cch = ((CommentService)srv).getHandler();
					if (cch.hasCommentCaller(c)) {
						ICommentCaller cc = cch.getCommentCaller(c);
						
						try {
							Class pdfclass = Thread.currentThread().getContextClassLoader().loadClass("de.janrufmonitor.ui.jface.application.comment.action.PDFCreator");
							Constructor con = pdfclass.getConstructor(new Class[] {ICommentCaller.class, String.class});
							Object pdfcreator = con.newInstance(new Object[] {cc, filename});
							Method m = pdfclass.getMethod("createPdf");
							m.invoke(pdfcreator);
						} catch (ClassNotFoundException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (SecurityException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (NoSuchMethodException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (IllegalArgumentException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (InstantiationException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (IllegalAccessException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (InvocationTargetException e) {
							this.m_logger.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				}
			}
		}
		
		
	}

	public boolean isEnabled() {
		//	looking up com.lowagie.text.Document
		try {
			Thread.currentThread().getContextClassLoader().loadClass("com.lowagie.text.Document");
		} catch (ClassNotFoundException e) {
			return false;
		}

		return true;
	}

}
