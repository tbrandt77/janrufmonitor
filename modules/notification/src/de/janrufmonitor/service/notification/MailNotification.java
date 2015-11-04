package de.janrufmonitor.service.notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;

import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.event.IEvent;
import de.janrufmonitor.framework.event.IEventBroker;
import de.janrufmonitor.framework.event.IEventConst;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.service.AbstractReceiverConfigurableService;
import de.janrufmonitor.util.formatter.Formatter;
import de.janrufmonitor.util.string.StringUtils;

public class MailNotification extends AbstractReceiverConfigurableService {

	private String ID = "MailNotification";

	private String NAMESPACE = "service.MailNotification";

	private static String CONFIG_OUT = "outgoing";
	private static String CONFIG_PRE = "pre";
	private static String CONFIG_ACC = "acc";
	private static String CONFIG_REJ = "rej";
	private static String CONFIG_MIS = "mis";
	private static String CONFIG_END = "end";
	
	private static String CONFIG_SERVER = "smtpserver";

	private static String CONFIG_USER = "smtpuser";

	private static String CONFIG_PASSWORD = "smtppassword";
	
	private static String CONFIG_PORT = "smtpport";

	private static String CONFIG_SUBJECT = "mailsubject";

	private static String CONFIG_CONTENT = "mailcontent";

	private static String CONFIG_MIMETYPE = "mimetype";

	private static String CONFIG_ENCODING = "encoding";

	private static String CONFIG_MAILTO = "mailto";

	private static String CONFIG_MAILFROM = "mailfrom";

	private static String CONFIG_SMTP_AUTH = "smtpauth";
	
	private static String CONFIG_SMTP_SSL = "smtpssl";
	
	private static String CONFIG_TEMPLATE = "mailtemplate";

	private static String CONFIG_SMTP_QUEUETIME = "smtpqueuetime";

	private static String default_prefix = "default_";

	private IRuntime m_runtime;
	
	private Timer m_timer;
	private MailTask m_task;

	private class MailTask extends TimerTask {

		Properties m_configuration;
		List m_queue;
		boolean m_isRunning;

		public MailTask() {
			this.m_queue = new ArrayList();
		}

		public void setConfiguration(Properties p) {
			this.m_configuration = p;
		}

		public void addMail(ICall c) {
			try {
				// get System properties
				Properties props = this.getMailProperties();
				String server = this.m_configuration.getProperty(
						MailNotification.CONFIG_SERVER, "");

				if (server.trim().length()==0) {
					m_logger.severe("No SMTP Host set.");
					return;
				}

				// obtain a mail session
				Session session = Session.getDefaultInstance(props);
				//session.setDebug(true);
				
				MimeMessage message = createMessage(session, c);

				this.m_queue.add(message);
				if (this.m_queue.size()==1) {
					PropagationFactory.getInstance().fire(
						new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.INFO,
						NAMESPACE,
						"addedmailone",
						new String[] { Integer.toString(this.m_queue.size())},
								new Exception()), "Tray"
					);
				} else {
					PropagationFactory.getInstance().fire(
							new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.INFO,
							NAMESPACE,
							"addedmailmulti",
							new String[] { Integer.toString(this.m_queue.size())},
									new Exception()), "Tray"
						);
				}
			} catch (AddressException e) {
				m_logger.log(Level.SEVERE, e.toString(), e);
				m_logger.info("Mail could not be delivered.");
			} catch (MessagingException e) {
				m_logger.log(Level.SEVERE, e.toString(), e);
				m_logger.info("Mail could not be delivered.");
			}

		}
		
		private Properties getMailProperties() {
			Properties props = System.getProperties();
			
			String server = this.m_configuration.getProperty(
					MailNotification.CONFIG_SERVER, "");
			String user = this.m_configuration.getProperty(
					MailNotification.CONFIG_USER, "");
			String password = this.m_configuration.getProperty(
					MailNotification.CONFIG_PASSWORD, "");
			
			boolean usessl = Boolean.parseBoolean(this.m_configuration
					.getProperty(MailNotification.CONFIG_SMTP_SSL,
							"false"));
			
			boolean auth = Boolean.parseBoolean(this.m_configuration
					.getProperty(MailNotification.CONFIG_SMTP_AUTH,
							"false"));
			
			if (server.length() > 0) {
				props.put("mail.smtp.host", server);
				props.put("mail.smtp.port", getPort());

				if (auth) {
					if (user!=null && user.length()>0 && password!=null) {
						props.put("mail.smtp.auth", "true");
					} else {
						m_logger.warning("mail.smtp.auth was enabled, but no user and password provided.");
						props.put("mail.smtp.auth", "false");
					}							
				} else {
					props.put("mail.smtp.auth", "false");
				}
				
				if (usessl) {
					props.put("mail.smtp.ssl.enable", "true");
					props.put("mail.smtp.ssl.trust", "true");
					props.put("mail.smtp.starttls.enable", "true");
					props.put("mail.transport.protocol", "smtps");
				} else {
					props.put("mail.smtp.ssl.enable", "false");
					props.put("mail.smtp.starttls.enable", "false");
					props.put("mail.transport.protocol", "smtp");
				}	

			} else {
				m_logger.severe("No SMTP Host set.");
			}
			
			if (m_logger.isLoggable(Level.INFO)) {
				m_logger.info("SMTP properties: "+props.toString());
			}
			return props;
		}

		public void run() {
			if (!m_isRunning && this.m_queue.size() > 0) {
				try {
					m_isRunning = true;
					// get System properties
					Properties props = this.getMailProperties();
					String server = this.m_configuration.getProperty(
							MailNotification.CONFIG_SERVER, "");
					String user = this.m_configuration.getProperty(
							MailNotification.CONFIG_USER, "");
					String password = this.m_configuration.getProperty(
							MailNotification.CONFIG_PASSWORD, "");

					if (server.trim().length()==0) {
						m_logger.severe("No SMTP Host set.");
						return;
					}
					
					// obtain a mail session
					Session session = Session.getDefaultInstance(props);
					//session.setDebug(true);

					Transport transport = session.getTransport();

					// 2008/12/17: changed do to auth requests on open SMTP server 
					user = (user.length() == 0 ? null : user);
					password = (password.length() == 0 ? null : password);
					transport.connect(server, getPort(), user, password);
					m_logger.info("Opening SMTP channel to server: " + server);
					m_logger.info("Using user information: " + user + ", "
							+ password);
					
					MimeMessage message = null;
					for (int i = 0; i < this.m_queue.size(); i++) {
						message = (MimeMessage) this.m_queue.get(i);
						message.saveChanges();
						transport.sendMessage(message, message
								.getAllRecipients());
					}

					m_logger.info("Sending message to server: " + server);
					transport.close();

					
					PropagationFactory.getInstance().fire(
							new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.INFO,
							NAMESPACE,
							"sentmail",
							new String[] { Integer.toString(this.m_queue.size())},
									new Exception()), "Tray"
						);
					m_logger.info("Mail successfully sent.");
				} catch (AddressException e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
					m_logger.info("Mail could not be delivered.");
				} catch (MessagingException e) {
					m_logger.log(Level.SEVERE, e.toString(), e);
					m_logger.info("Mail could not be delivered.");
					String error = (e.getMessage()==null ? "auth" : e.getMessage());
					if (error.toLowerCase().indexOf("Could not connect to SMTP host".toLowerCase())>=0) {		
						PropagationFactory.getInstance().fire(
								new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.ERROR,
								NAMESPACE,
								"failserver",
								new String[] {this.m_configuration.getProperty(
										MailNotification.CONFIG_SERVER, ""), Integer.toString(getPort())},
								e), "Tray"
							);
					} else if (error.toLowerCase().indexOf("Unknown SMTP host".toLowerCase())>=0) {
						PropagationFactory.getInstance().fire(
								new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.ERROR,
								NAMESPACE,
								"failhost",
								new String[] {this.m_configuration.getProperty(
										MailNotification.CONFIG_SERVER, "")},
								e), "Tray"
							);
					} else if (error.toLowerCase().indexOf("auth".toLowerCase())>=0) {
						PropagationFactory.getInstance().fire(
								new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.ERROR,
								NAMESPACE,
								"failauth",
								new String[] {this.m_configuration.getProperty(
										MailNotification.CONFIG_SERVER, ""), Integer.toString(getPort()),
										this.m_configuration.getProperty(MailNotification.CONFIG_USER, "")},
								e), "Tray"
							);
					} else {
						PropagationFactory.getInstance().fire(
								new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.ERROR,
								NAMESPACE,
								"failconnect",
								new String[] {this.m_configuration.getProperty(
										MailNotification.CONFIG_SERVER, ""), Integer.toString(getPort()), 
										this.m_configuration.getProperty(MailNotification.CONFIG_USER, "")},
								e), "Tray"
							);
					}
				}
				m_isRunning = false;
				this.m_queue.clear();
			}
		}

	}

	public MailNotification() {
		super();
		this.getRuntime().getConfigurableNotifier().register(this);
	}

	public String getNamespace() {
		return this.NAMESPACE;
	}

	public void startup() {
		super.startup();

		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		if (this.m_configuration.getProperty(CONFIG_PRE, "true").equalsIgnoreCase("true"))
			eventBroker.register(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		
		if (this.m_configuration.getProperty(CONFIG_OUT, "false").equalsIgnoreCase("true") || this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("true"))
			eventBroker.register(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		
		if (this.m_configuration.getProperty(CONFIG_MIS, "false").equalsIgnoreCase("true"))
			eventBroker.register(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED));
		
		if (this.m_configuration.getProperty(CONFIG_ACC, "false").equalsIgnoreCase("true") || this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("true"))
			eventBroker.register(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));
		
		if (this.m_configuration.getProperty(CONFIG_REJ, "false").equalsIgnoreCase("true"))
			eventBroker.register(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
		
		this.m_logger.info("MailNotification is started ...");
		if (this.m_timer==null) {
			this.m_task = new MailTask();
			this.m_task.setConfiguration(this.m_configuration);
			this.m_timer = new Timer(true);
			this.m_timer.schedule(this.m_task, getQueueTime(), getQueueTime());
		}
	}

	private long getQueueTime() {
		try {
			return (Long.parseLong(this.m_configuration.getProperty(CONFIG_SMTP_QUEUETIME, "0"))*60*1000)+1000;
		} catch(NumberFormatException e){
			m_logger.warning(e.getMessage());
		}
		return 0;
	}

	public void shutdown() {
		super.shutdown();
		if (this.m_timer!=null) {
			this.m_task.run();
			this.m_task.cancel();
			this.m_timer.cancel();
			this.m_timer=null;
			this.m_task=null;
		}
		IEventBroker eventBroker = this.getRuntime().getEventBroker();
		//if (this.m_configuration.getProperty(CONFIG_PRE, "true").equalsIgnoreCase("true"))
			eventBroker.unregister(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_CALL));
		
		//if (this.m_configuration.getProperty(CONFIG_OUT, "false").equalsIgnoreCase("true") || this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("true"))
			eventBroker.unregister(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		
		//if (this.m_configuration.getProperty(CONFIG_OUT, "false").equalsIgnoreCase("true"))
			eventBroker.unregister(this, eventBroker
					.createEvent(IEventConst.EVENT_TYPE_IDENTIFIED_OUTGOING_CALL));
		
		//if (this.m_configuration.getProperty(CONFIG_MIS, "false").equalsIgnoreCase("true"))
			eventBroker.unregister(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_CALLCLEARED));
		
		//if (this.m_configuration.getProperty(CONFIG_ACC, "false").equalsIgnoreCase("true") || this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("true"))
			eventBroker.unregister(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_CALLACCEPTED));
		
		//if (this.m_configuration.getProperty(CONFIG_REJ, "false").equalsIgnoreCase("true"))
			eventBroker.unregister(this, eventBroker
				.createEvent(IEventConst.EVENT_TYPE_CALLREJECTED));
		
		this.m_logger.info("MailNotification is shut down ...");
	}
	
	public void sendTestMail() {
		ICaller caller = getRuntime().getCallerFactory().createCaller(
				getRuntime().getCallerFactory().createName("Max", "Mustermann"),
				getRuntime().getCallerFactory().createPhonenumber("49", "30", "1234567890")		
		);
		IMsn msn = getRuntime().getMsnManager().createMsn("12345");
		ICip cip = getRuntime().getCipManager().createCip("999");
		ICall call = getRuntime().getCallFactory().createCall(caller, msn, cip);

		if (this.m_configuration==null) {
			PropagationFactory.getInstance().fire(
					new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.ERROR,
					NAMESPACE,
					"notstarted",
					new String[] { },
							new Exception()), "Tray"
				);
			
			return;
		}
		
		if (this.m_task==null) {			
			this.m_task = new MailTask();
			this.m_logger.info("Created new MailTask instance");
		}
		
		PropagationFactory.getInstance().fire(
				new de.janrufmonitor.exception.Message(de.janrufmonitor.exception.Message.INFO,
				NAMESPACE,
				"testmail",
				new String[] { },
						new Exception()), "Tray"
			);
		
		this.m_task.setConfiguration(this.m_configuration);
		this.m_task.addMail(call);
		this.m_task.run();
	}

	private boolean isConfigurationValid() {

		if (!this.isPropertyValid(MailNotification.CONFIG_SERVER)) {
			this.m_logger
					.severe("Configuration is invalid: No server specified.");
			return false;
		}

		if (!this.isPropertyValid(MailNotification.default_prefix
				+ MailNotification.CONFIG_MAILTO)) {
			this.m_logger
					.warning("Configuration is invalid: No standard mailto address specified.");
		}

		if (!this.isPropertyValid(MailNotification.default_prefix
				+ MailNotification.CONFIG_MAILFROM)) {
			this.m_logger
					.warning("Configuration is invalid: No standard mailfrom address specified.");
		}

		if (!this.isPropertyValid(MailNotification.default_prefix
				+ MailNotification.CONFIG_SUBJECT)) {
			this.m_logger
					.warning("Configuration is invalid: No standard subject specified.");
		}

		if (!this.isPropertyValid(MailNotification.default_prefix
				+ MailNotification.CONFIG_CONTENT)) {
			this.m_logger
					.warning("Configuration is invalid: No standard content specified.");
		}

		if (!this.isPropertyValid(MailNotification.default_prefix
				+ MailNotification.CONFIG_MIMETYPE)) {
			this.m_configuration.setProperty(MailNotification.default_prefix
					+ MailNotification.CONFIG_MIMETYPE, "text/plain");
			this.m_logger
					.warning("Configuration is invalid: No standard mimetype for mail specified. Using default: text/plain");
		}

		if (!this.isPropertyValid(MailNotification.default_prefix
				+ MailNotification.CONFIG_ENCODING)) {
			this.m_configuration.setProperty(MailNotification.default_prefix
					+ MailNotification.CONFIG_ENCODING, "ISO-8859-1");
			this.m_logger
					.warning("Configuration is invalid: No standard encoding for mail specified. Using default: ISO-8859-1");
		}

		return true;

	}

	private boolean isPropertyValid(String propName) {
		String propValue = this.m_configuration.getProperty(propName);
		return ((propValue != null && propValue.length() > 0) ? true : false);
	}

	private String parse(String text, ICall call) {
		Formatter f = Formatter.getInstance(this.getRuntime());
		return f.parse(text, call);
	}
	
	private boolean isMSNConfigured(String msn, Properties cfg) {
		String[] list = cfg.getProperty("list", "").split(",");
		for (int i=0;i<list.length;i++) {
			if (list[i].equalsIgnoreCase(msn)) return true;
		}
		return false;
	}

	private MimeMessage createMessage(Session session, ICall call)
			throws AddressException, MessagingException {

		String prefix = call.getMSN().getMSN() + "_";

		// added 2009/04/18: fixed wrong mail content for MSN specific mails
		if (!isMSNConfigured(call.getMSN().getMSN(), this.m_configuration)) {
			prefix = default_prefix;
		}
		// check for template
		String template = getValidPropertyValue(prefix, CONFIG_TEMPLATE);
		if (template.length()>0) {
			this.m_logger.info("Creating new mail message using template "+template);
			MimeMessage message = MessageBuilder.createMessageFromTemplate(session, call, this.m_configuration);
			if (message!=null)
				return message;
			
			this.m_logger.warning("Problem creating new mail message using template "+template);
		} 
		
		// no template defines, as of 5.0.7 and earlier
		MimeMessage message = new MimeMessage(session);
		this.m_logger.info("Creating new mail message for delivery...");

		
		message.setFrom(new InternetAddress(this.getValidPropertyValue(prefix,
				MailNotification.CONFIG_MAILFROM)));

		this.m_logger.info("Set sender address: "
				+ message.getFrom()[0].toString());

		String[] tos = this.getStringArray(this.getValidPropertyValue(prefix,
				MailNotification.CONFIG_MAILTO), ",");

		Address[] adresses = new InternetAddress[tos.length];

		for (int i = 0; i < tos.length; i++) {
			adresses[i] = new InternetAddress(tos[i]);
			this.m_logger.info("Set receiver address: " + adresses[i]);
		}

		message.setRecipients(Message.RecipientType.TO, adresses);

		message.setSubject(removeCRLF(this.parse(this.getValidPropertyValue(
				prefix, MailNotification.CONFIG_SUBJECT), call)),
				this.getValidPropertyValue(prefix,
						MailNotification.CONFIG_ENCODING));

		this.m_logger.info("Set mail subject: " + message.getSubject());

		message.setSentDate(new Date());
		message.setHeader("X-Mailer", "jAnrufmonitor MailNotificator");

		this.m_logger.info("Set mail sent date: "
				+ message.getSentDate().toString());

		message.setContent(this.parse(this.getValidPropertyValue(prefix,
				MailNotification.CONFIG_CONTENT), call),
				this.getValidPropertyValue(prefix,
						MailNotification.CONFIG_MIMETYPE));

		this.m_logger.info("Created new mail message for delivery.");

		return message;
	}

	private String removeCRLF(String text) {
		return StringUtils.replaceString(text, IJAMConst.CRLF, ", ");
	}

	private String[] getStringArray(String value, String separator) {
		StringTokenizer st = new StringTokenizer(value, separator);
		String[] result = new String[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens()) {
			result[i] = st.nextToken().trim();
			i++;
		}
		return result;
	}

	private String getValidPropertyValue(String prefix, String propName) {
		String value = this.m_configuration.getProperty(prefix + propName);
		if (value == null || value.length() == 0) {
			value = this.m_configuration
					.getProperty(MailNotification.default_prefix + propName);
			if (value == null) {
				this.m_logger.severe("No valid property " + prefix + propName
						+ " found.");
				return "";
			}
		}
		return value;
	}

	private int getPort() {
		return Integer.parseInt(this.m_configuration.getProperty(
				MailNotification.CONFIG_PORT, "25"));
	}

	public String getID() {
		return this.ID;
	}

	public void receivedValidRule(ICall aCall) {
		IAttribute status = aCall.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
		if (status!=null && status.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING)) {
			if (this.m_configuration.getProperty(CONFIG_OUT, "false").equalsIgnoreCase("false") && this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("true")) {
				IAttribute dur = aCall.getAttribute("fritzbox.duration");
				if (dur!=null && dur.getValue().length()>0 && !dur.getValue().equalsIgnoreCase("0")) {
					if (this.isConfigurationValid()) {
						this.m_task.addMail(aCall);
					} else {
						this.m_logger.info("No mail was sent.");
					}						
				}
				return;
			}
			if (this.m_configuration.getProperty(CONFIG_OUT, "false").equalsIgnoreCase("true") && this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("false")) {
				IAttribute dur = aCall.getAttribute("fritzbox.duration");
				if (dur==null) {
					if (this.isConfigurationValid()) {
						this.m_task.addMail(aCall);
					} else {
						this.m_logger.info("No mail was sent.");
					}
				}
				return;
			}					
		} 
		
		if (this.isConfigurationValid()) {
			this.m_task.addMail(aCall);
		} else {
			this.m_logger.info("No mail was sent.");
		}
	}
	
	public void receivedOtherEventCall(IEvent event) {
		ICall aCall = (ICall)event.getData();
		if (aCall!=null) {
			if (getRuntime().getRuleEngine().validate(this.getID(), aCall.getMSN(), aCall.getCIP(), aCall.getCaller().getPhoneNumber())) {
				
				// added 2010/11/01 block any outgoing call, if outgoing is not checked in config
				if (this.m_configuration.getProperty(CONFIG_OUT, "false").equalsIgnoreCase("false") && isOutgoing(aCall)) {
					return;
				}
				if (event.getType()==IEventConst.EVENT_TYPE_CALLACCEPTED) {
					// added 2008/11/14: check for fritzbox.duration
					if (this.m_configuration.getProperty(CONFIG_ACC, "false").equalsIgnoreCase("false") && this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("true")) {
						IAttribute dur = aCall.getAttribute("fritzbox.duration");
						if (dur!=null && dur.getValue().length()>0 && !dur.getValue().equalsIgnoreCase("0")) {
							this.receivedValidRule(aCall);							
						}
						return; 
					}
					if (this.m_configuration.getProperty(CONFIG_ACC, "false").equalsIgnoreCase("true") && this.m_configuration.getProperty(CONFIG_END, "false").equalsIgnoreCase("false")) {
						IAttribute dur = aCall.getAttribute("fritzbox.duration");
						if (dur==null) {
							this.receivedValidRule(aCall);
						}
						return;
					}					
				}
				this.receivedValidRule(aCall);
			} else {
				this.m_logger.info("No rule assigned to execute this service for call: "+aCall);
			}
		} 
	}

	private boolean isOutgoing(ICall aCall) {
		if (aCall!=null){
			IAttribute att = aCall.getAttribute(IJAMConst.ATTRIBUTE_NAME_CALLSTATUS);
			if (att!=null) {
				return att.getValue().equalsIgnoreCase(IJAMConst.ATTRIBUTE_VALUE_OUTGOING);
			}
		}
		return false;
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null)
			this.m_runtime = PIMRuntime.getInstance();
		return this.m_runtime;
	}

}
