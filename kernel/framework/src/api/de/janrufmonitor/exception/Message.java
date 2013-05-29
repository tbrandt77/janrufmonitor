package de.janrufmonitor.exception;

/**
 *  This class provides a message container which is fired to every IPropagator
 *  implementation for notification.
 *
 *@author     Thilo Brandt
 *@created    2004/11/21
 */
public class Message {

	public static final String ERROR = "error";
	public static final String WARNING = "warning";
	public static final String INFO = "info";
	
	public static final String DEFAULT_NAMESPACE = "exception.Message";
	private static final String DEFAULT_ID = "unknown";
	
	private String m_level;
	private Throwable m_t;
	private String m_namespace;
	private String m_message;
	private String[] m_variables;
	private boolean m_isErrorIcon;
	
	
	public Message(String level,String messageID, Throwable t) {
		this(level, Message.DEFAULT_NAMESPACE, messageID, t);
	}
	
	public Message(Throwable t) {
		this(Message.ERROR, Message.DEFAULT_NAMESPACE, Message.DEFAULT_ID, t);
	}
	
	public Message(String messageID, Throwable t) {
		this(Message.ERROR, Message.DEFAULT_NAMESPACE, messageID, t);
	}
	
	public Message(String level, String namespace, String messageID, Throwable t) {
		this(level, namespace, messageID, new String[0], t);
	}
	
	public Message(String level, String namespace, String messageID, Throwable t, boolean setErrorIcon) {
		this(level, namespace, messageID, new String[0], t, setErrorIcon);
	}
	
	public Message(String level, String namespace, String messageID, String[] variables, Throwable t) {
		this(level, namespace, messageID, variables, t, false);
	}
	
	public Message(String level, String namespace, String messageID, String[] variables, Throwable t, boolean setErrorIcon) {
		this.m_level = level;
		this.m_namespace = namespace;
		this.m_message = messageID;
		this.m_t = t;
		if (variables!=null)
			this.m_variables = variables;
		else this.m_variables = new String[0];
		this.m_isErrorIcon = setErrorIcon;
	}

	public String getLevel() {
		return m_level;
	}
	
	public String getMessage() {
		return m_message;
	}
	
	public String getNamespace() {
		return m_namespace;
	}
	
	public Throwable getThrowable() {
		return m_t;
	}
	
	public String[] getVariables(){
		return this.m_variables;
	}
	
	public boolean isSetErrorIcon() {
		return this.m_isErrorIcon;
	}
}
