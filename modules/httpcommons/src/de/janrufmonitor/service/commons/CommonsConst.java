package de.janrufmonitor.service.commons;

public interface CommonsConst {
	
	//URI Paths
	public final static String URI_PATH_CONFIGURATION = "/config";
	public final static String URI_PATH_CALLTO = "/callto";

	// GET actions
	public final static String ACTION_PING ="ping";
	public final static String ACTION_REJECT ="reject";
	public final static String ACTION_REJECTED ="rejected";
	public final static String ACTION_ACCEPT ="accept";
	public final static String ACTION_CLEAR ="clear";
	public final static String ACTION_APPLICATION_READY ="ready";
	public final static String ACTION_GETCALLER ="GetCaller";
	public final static String ACTION_GETCALLERLIST ="GetCallerList";
	public final static String ACTION_GETCALL ="GetCall";
	public final static String ACTION_GETCALLLIST ="GetCallList";
	public final static String ACTION_GETCALLLISTCOUNT ="GetCallListCount";
	public final static String ACTION_REGISTER ="Register";
	public final static String ACTION_UNREGISTER ="Unregister";
	public final static String ACTION_IMAGE ="Image";
	public final static String ACTION_DIAL ="Dial";
	public final static String ACTION_GETDIALEXTENSIONS ="GetDialExtensions";
	
	public final static String ACTION_INCOMINGCALL ="IncomingCall";
	public final static String ACTION_OUTGOINGCALL ="OutgoingCall";
	public final static String ACTION_IDENTIFIEDCALL ="IdentifiedCall";
	public final static String ACTION_IDENTIFIEDOUTGOINGCALL ="IdentifiedOutgoingCall";
	public final static String ACTION_SHUTDOWN ="Shutdown";

	// POST actions
	public final static String ACTION_REMOVECALLERLIST ="RemoveCallerList";
	public final static String ACTION_UPDATECALLERLIST ="UpdateCallerList";
	public final static String ACTION_REMOVECALLLIST ="RemoveCallList";
	public final static String ACTION_SETCALLERLIST ="SetCallerList";
	public final static String ACTION_SETCALLLIST ="SetCallList";

	// URI parameters
	public final static String PARAMETER_ACTION ="action";
	public final static String PARAMETER_CALLERMANAGER ="callermanager";
	public final static String PARAMETER_NUMBER ="number";
	public final static String PARAMETER_MSN ="msn";
	public final static String PARAMETER_DATE ="date";
	public final static String PARAMETER_CIP ="cip";
	public final static String PARAMETER_FILTER ="filter";
	public final static String PARAMETER_SEARCHTERMS ="searchterms";
	public final static String PARAMETER_CALLMANAGER ="callmanager";
	public final static String PARAMETER_UUID ="uuid";
	public final static String PARAMETER_COMPRESSION = "compression";
	public final static String PARAMETER_EXTENSION ="extension";
	
	// URI parameters for configuration
	public final static String PARAMETER_CFG_NAMESPACE="ns";
	public final static String PARAMETER_CFG_ACTION="action";
	
	// POST actions for configuration
	public final static String ACTION_CFG_SAVE ="save";
	public final static String ACTION_CFG_DELETE ="delete";
	public final static String ACTION_CFG_DEFAULT ="default";
	public final static String ACTION_CFG_CLEAR ="clear";
	
	// URI perameters for callto
	public final static String PARAMETER_CALLTO_ACTION ="dial";
	public final static String PARAMETER_CALLTO_EXTENSION =PARAMETER_EXTENSION;
	public final static String PARAMETER_CALLTO_GET_EXTENSION="extensionlist";
	public final static String PARAMETER_CALLTO_RETURNTYPE="type";
	
	// http header parameters
	public final static String PARAMETER_CLIENT ="pimclient";
	public final static String PARAMETER_CLIENT_IP ="pimclientip";
	public final static String PARAMETER_CLIENT_PORT ="pimclientport";
	public final static String PARAMETER_CLIENT_EVENTS ="pimclientevents";

}
