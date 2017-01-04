package de.janrufmonitor.service.notification;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.RendererRegistry;
import de.janrufmonitor.ui.jface.application.rendering.ITableCellRenderer;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.io.PathResolver;
import de.janrufmonitor.util.io.Stream;
import de.janrufmonitor.util.string.StringUtils;

public class MessageBuilder {

	private static String CONFIG_TEMPLATE = "mailtemplate";
	private static String CONFIG_MAILTO = "mailto";
	private static String CONFIG_MAILLIST = "list";
	private static String CONFIG_MAILFROM = "mailfrom";
	private static String default_prefix = "default_";
	
	public static synchronized MimeMessage createMessageFromTemplate(Session session, ICall call, Properties cfg) throws AddressException, MessagingException {
		String prefix = call.getMSN().getMSN() + "_";
		
		// added 2009/04/18: fixed wrong mail content for MSN specific mails
		if (!isMSNConfigured(call.getMSN().getMSN(), cfg)) {
			prefix = default_prefix;
		}
				
		String template = getValidPropertyValue(prefix, CONFIG_TEMPLATE, cfg);
		Logger m_logger = Logger.getLogger(IJAMConst.DEFAULT_LOGGER);
		String templatePath = PathResolver.getInstance(PIMRuntime.getInstance()).getConfigDirectory() + File.separator + "templates";
		File templateDir = new File(templatePath);
		templateDir.mkdirs(); // generate path if not exists
		
		File templateFile = new File(templateDir, template+".template");
		if (isClired(call)) {
			templateFile = new File(templateDir, template+".template.clir");
			if (!templateFile.exists()) {
				m_logger.info("Now CLIR mail template found: "+templateFile.getName());
				templateFile = new File(templateDir, template+".template");
			}
		}
		File templatePropertiesFile = new File(templateDir, template+".properties");
		if (templateFile.getName().endsWith(".clir")) {
			templatePropertiesFile = new File(templateDir, template+".properties.clir");
			if (!templatePropertiesFile.exists())
				templatePropertiesFile = new File(templateDir, template+".properties");
		}
		
		if (!templateFile.exists() || !templatePropertiesFile.exists()) {
			m_logger.warning(templateFile.getName() +" or "+ templatePropertiesFile.getName()+" not found.");
			return null;
		}
		
		MimeMessage message = new MimeMessage(session);
		m_logger.info("Creating new mail message for delivery...");

		
		message.setFrom(new InternetAddress(getValidPropertyValue(prefix,
				CONFIG_MAILFROM, cfg)));

		m_logger.info("Set sender address: "
				+ message.getFrom()[0].toString());

		String[] tos = getStringArray(getValidPropertyValue(prefix,
				CONFIG_MAILTO, cfg), ",");

		Address[] adresses = new InternetAddress[tos.length];

		for (int i = 0; i < tos.length; i++) {
			adresses[i] = new InternetAddress(tos[i]);
			m_logger.info("Set receiver address: " + adresses[i]);
		}

		message.setRecipients(Message.RecipientType.TO, adresses);
		
		Properties templateProps = new Properties();
		try {
			templateProps.load(new FileInputStream(templatePropertiesFile));
		} catch (FileNotFoundException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
			return null;
		} catch (IOException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
			return null;
		}
		
		message.setSubject(removeCRLF((translate(parse(templateProps.getProperty("subject"), call))))); 
        message.setSentDate(new Date()); 
        message.setHeader("X-Mailer", "jAnrufmonitor MailNotificator "+IJAMConst.VERSION_DISPLAY); 
        
        //AddHeader for each additional line in template.properties-file
        for(String key : templateProps.stringPropertyNames()) {
        	if(!(key.equalsIgnoreCase("subject") || key.equalsIgnoreCase("mimetype")))
        		message.addHeader(key, removeCRLF((translate(parse(templateProps.getProperty(key), call)))));
        }

        StringBuffer rawcontent = new StringBuffer((int) templateFile.length());
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
			Stream.copy(new FileInputStream(templateFile), bos, true);
		} catch (FileNotFoundException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
			return null;
		} catch (IOException e) {
			m_logger.log(Level.SEVERE, e.toString(), e);
			return null;
		}
		rawcontent.append(new String(bos.toByteArray()));
        
        if (templateProps.getProperty("mimetype").equalsIgnoreCase("text/plain")) {
        	message.setContent(translate(parse(rawcontent.toString(), call)), "text/plain");
        } else if (templateProps.getProperty("mimetype").equalsIgnoreCase("text/html")) {
        	StringBuffer parsedContent = new StringBuffer(translate(parse(rawcontent.toString(), call)));
        	
        	List files = new ArrayList();
        	
        	MimeMultipart mp = new MimeMultipart(); 
            MimeBodyPart mbp = new MimeBodyPart(); 
            
            mbp.setContent(extractFiles(resolvePaths(parsedContent.toString()), files), "text/html"); 
            mp.addBodyPart(mbp); 

            File f = null;
            for (int i=0;i<files.size();i++) {
            	f = (File) files.get(i);
            	if (f.exists()) {
                    mbp = new MimeBodyPart(); 
                    try {
						mbp.attachFile(f);
					} catch (IOException e) {
						m_logger.log(Level.SEVERE, e.toString(), e);
					} 
                    mbp.setContentID(f.getName()); 
                    mp.addBodyPart(mbp); 
            	}
            }
            message.setContent(mp); 
        } else {
        	m_logger.severe("Unknown mail format mimetype: "+templateProps.getProperty("mimetype"));
        	return null;
        }
        
		return message;
	}
	
	private static boolean isMSNConfigured(String msn, Properties cfg) {
		String[] list = cfg.getProperty(CONFIG_MAILLIST, "").split(",");
		for (int i=0;i<list.length;i++) {
			if (list[i].equalsIgnoreCase(msn)) return true;
		}
		return false;
	}
	
	private static boolean isClired(ICall c) {
		return c.getCaller().getPhoneNumber().isClired();
	}
	
	private static String parseRenderer(String text, ICall call) {
		String prefix = "%r:";
		String postfix = "%";
		
		while (text.indexOf(prefix)>=0) {
			String id = text.substring(text.indexOf(prefix) + prefix.length(), text.indexOf(postfix, text.indexOf(prefix) + prefix.length()));
			ITableCellRenderer r = RendererRegistry.getInstance().getRenderer(id.toLowerCase());
			if (r!=null) {
				r.updateData(call);
				text = StringUtils.replaceString(text, prefix + id + postfix, r.renderAsText());
			} else {
				text = StringUtils.replaceString(text, prefix + id + postfix, "");
			}
		}
		return text;
	}
	
	private static String extractFiles(String text, List files){
		String prefix = "<!-- start_file:";
		String postfix = ":end_file-->";
		while (text.indexOf(prefix)>=0) {
			String file = text.substring(text.indexOf(prefix) + prefix.length(), text.indexOf(postfix));
			File f = new File(file);
			if (f.exists()) {
				files.add(f);
				text = StringUtils.replaceString(text, prefix + file + postfix, "cid:"+f.getName());
			} else {
				text = StringUtils.replaceString(text, prefix + file + postfix, "");
			}
			
		}
		return text;
	}
	
	private static String resolvePaths(String t) {
		t = StringUtils.replaceString(t, IJAMConst.PATHKEY_CONFIGPATH, PathResolver.getInstance(PIMRuntime.getInstance()).getConfigDirectory());
		t = StringUtils.replaceString(t, IJAMConst.PATHKEY_IMAGEPATH, PathResolver.getInstance(PIMRuntime.getInstance()).getImageDirectory());
		t = StringUtils.replaceString(t, IJAMConst.PATHKEY_INSTALLPATH, PathResolver.getInstance(PIMRuntime.getInstance()).getInstallDirectory());
		t = StringUtils.replaceString(t, IJAMConst.PATHKEY_DATAPATH, PathResolver.getInstance(PIMRuntime.getInstance()).getDataDirectory());
		t = StringUtils.replaceString(t, IJAMConst.PATHKEY_PHOTOPATH, PathResolver.getInstance(PIMRuntime.getInstance()).getPhotoDirectory());
		t = StringUtils.replaceString(t, IJAMConst.PATHKEY_USERHOME, PathResolver.getInstance(PIMRuntime.getInstance()).getUserhomeDirectory());
		return t;
	}
	
	private static String translate(String text) {
		II18nManager i18n = PIMRuntime.getInstance().getI18nManagerFactory().getI18nManager();
		String prefix = "<!-- start_translate:";
		String postfix = ":end_translate-->";
		while (text.indexOf(prefix)>=0) {
			String token = text.substring(text.indexOf(prefix) + prefix.length(), text.indexOf(postfix));
			String[] elements = token.split(",");
			if (elements.length==4) {
				String translation = i18n.getString(elements[0], elements[1], elements[2], elements[3]);
				text = StringUtils.replaceString(text, prefix + token + postfix, translation);
			}
			if (elements.length==3) {
				String translation = i18n.getString(elements[0], elements[1], "label", elements[2]);
				text = StringUtils.replaceString(text, prefix + token + postfix, translation);
			}
			if (elements.length==2) {
				String translation = i18n.getString("service.MailNotification", elements[0], "label", elements[1]);
				text = StringUtils.replaceString(text, prefix + token + postfix, translation);
			}			

		}
		return text;
	}
	
	private static String parse(String text, ICall call) {
		Formatter f = Formatter.getInstance(PIMRuntime.getInstance());
		return parseRenderer(f.parse(text, call), call);
	}
	
	private static String[] getStringArray(String value, String separator) {
		StringTokenizer st = new StringTokenizer(value, separator);
		String[] result = new String[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens()) {
			result[i] = st.nextToken().trim();
			i++;
		}
		return result;
	}

	private static String getValidPropertyValue(String prefix, String propName, Properties cfg) {
		String value =cfg.getProperty(prefix + propName);
		if (value == null || value.length() == 0) {
			value = cfg
					.getProperty(default_prefix + propName);
			if (value == null) {
				return "";
			}
		}
		return value;
	}
	
	private static String removeCRLF(String text) {
		return StringUtils.replaceString(text, IJAMConst.CRLF, ", ");
	}
	
}
