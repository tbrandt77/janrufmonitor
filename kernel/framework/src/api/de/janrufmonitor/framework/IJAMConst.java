package de.janrufmonitor.framework;

/**
 *  This interface specifies certain constants which are used by the framework.
 *
 *@author     Thilo Brandt
 *@created    2003/03/01
 */
public interface IJAMConst {


	// VERSION INFORMATION
	
	/**
	 * Major version
	 */
	public final static String VERSION_MAJOR = "5";
	
	/**
	 * Minor version
	 */
	public final static String VERSION_MINOR = "0";
	
	/**
	 * Patch level
	 */
	public final static String VERSION_PATCH = "48";
	
	/**
	 * Build number
	 */
	public final static String VERSION_BUILD = "20140401";

	/**
	 * Display version
	 */
	public final static String VERSION_DISPLAY = VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_PATCH;
	
    /**
	 *  Version of the Framework.
     */
	public final static String VERSION = VERSION_MAJOR + "." + VERSION_MINOR;
	
	
	// CRLF
	/**
	 * Variable which represents the CRLF characters
	 */
	public final static String CRLF = System.getProperty("line.separator");
	
	
	// LOGGER INFORMATION
	
	/**
	 * Wildcard for default logger
	 */
	public final static String DEFAULT_LOGGER = "jamlogger";
	
	/**
	 * Wildcard for trace logger
	 */
	public final static String TRACE_LOGGER = "jamlogger.trace";
	
	
	
    /**
     *  Wildcard for areacode
     */
    public final static String AREACODE = "%areacode%";

    /**
     *  Wildcard for international areacode
     */
    public final static String INTAREACODE = "%intareacode%";

    /**
     *  Wildcard for caller number
     */
    public final static String CALLNUMBER = "%callnumber%";


    
    
    // FILE AND DIRECTORY PLACEHOLDERS
    
    /**
     *  Wildcard for install path
     */
    public final static String PATHKEY_INSTALLPATH = "%installpath%";

	/**
	 *  Wildcard for user home path
	 */
	public final static String PATHKEY_USERHOME = "%userhome%";

	/**
	 *  Wildcard for temp path
	 */
	public final static String PATHKEY_TEMP = "%tempdir%";

    /**
     *  Wildcard for image path
     */
    public final static String PATHKEY_IMAGEPATH = "%imagepath%";

    /**
     *  Wildcard for lib path
     */
    public final static String PATHKEY_LIBPATH = "%libpath%";
    
    /**
     *  Wildcard for deploy path
     */
    public final static String PATHKEY_DEPLOYPATH = "%deploypath%";
    
    /**
     *  Wildcard for data path
     */
    public final static String PATHKEY_DATAPATH = "%datapath%";
    
    /**
     *  Wildcard for data path
     */
    public final static String PATHKEY_USERDATAPATH = "%userdatapath%";
    
    /**
     *  Wildcard for log path
     */
    public final static String PATHKEY_LOGPATH = "%logpath%";
 
    /**
     *  Wildcard for config path
     */
    public final static String PATHKEY_CONFIGPATH = "%configpath%";

    
    /**
     *  Wildcard for photo path
     */
    public final static String PATHKEY_PHOTOPATH = "%photopath%";
 
    // SYSTEM PARAMETERS FOR JAVA VM 
    
    public final static String SYSTEM_MONITOR_SPOOFING = "jam.monitor.spoofing"; // used for checking spoofing numbers on CAPI
    public final static String SYSTEM_MONITOR_SECURE = "jam.monitor.secure"; // added 2011/03/10: used for save TAPI handling for some TAPI drivers
    public final static String SYSTEM_UI_TRAYITEM = "jam.ui.trayitem";
    public final static String SYSTEM_UI_TOPLEVEL = "jam.ui.toplevel"; // true if jam runs as toplevel application, esp. journal and address book
    public final static String SYSTEM_UI_FORCEIMAGE = "jam.ui.forceimage"; // tries to force an image to be rendered. If ImageHandler does not deliver one, the default (File system) ImageHandler is triggered.
    public final static String SYSTEM_INSTALLER_RESTART = "jam.installer.restart";
    public final static String SYSTEM_INSTALLER_RUN = "jam.installer.run";

    // CONFIGURATION PARAMETERS
    
    /**
     *  Wildcard for the default format of phone number
     */
    public final static String DEFAULTFORMAT = "+" + IJAMConst.INTAREACODE + " (" + IJAMConst.AREACODE + ") " + IJAMConst.CALLNUMBER;

    /**
     *  Wildcard for base configuration file
     */
    public final static String CONFIGURATION_BASE_FILE = "config.properties";

    /**
     *  Wildcard for global namespace
     */
    public final static String GLOBAL_NAMESPACE = "janrufmonitor.global";

    /**
     *  Wildcard for global configuration expert setting
     */
    public final static String GLOBAL_CONFIG_EXPERT_MODE = "expertmode";
    
    /**
     *  Wildcard for global language
     */
    public final static String GLOBAL_LANGUAGE = "language";

    /**
     *  Wildcard for global international areacode
     */
    public final static String GLOBAL_INTAREA = "intarea";
    
    /**
     *  Wildcard for global dial prefix
     */
    public final static String GLOBAL_DIAL_PREFIX = "dialprefix";
    
    /**
     *  Wildcard for global auto analyze number in PhonenumberAnalyzer
     */
    public final static String GLOBAL_AUTO_ANALYZE_NUMBER = "autoanalyzenumber";
    
    /**
     *  Wildcard for global national areacode
     */
    public final static String GLOBAL_AREACODE = "areacode";
    
    /**
     *  Wildcard for global national areacode add length
     */
    public final static String GLOBAL_AREACODE_ADD_LENGTH = "areacodeaddlength"; 

    /**
     *  Wildcard for global international areacode prefix
     */
    public final static String GLOBAL_INTAREA_PREFIX = "intareaprefix";

    /**
     *  Wildcard for global national areacode prefix
     */
    public final static String GLOBAL_AREA_PREFIX = "areaprefix";
    
    /**
     *  Wildcard for global telephonesystem prefix
     */
    public final static String GLOBAL_TELEPHONESYSTEM_PREFIX = "telephonesystemprefix";

    /**
     *  Wildcard for global truncate signs
     */
    public final static String GLOBAL_TRUNCATE = "truncate";

	/**
	 *  Wildcard for CAPI trace option
	 */
	public final static String GLOBAL_TRACE = "trace";
	
	/**
	 *  Wildcard for all MSN detection
	 */
	public final static String GLOBAL_DETECT_ALL_MSN = "detectallmsn";

	/**
	 *  Wildcard for max. length of internal callnumbers
	 */
	public final static String GLOBAL_INTERNAL_LENGTH = "internallength";
		
    /**
     *  Wildcard for global clir sign
     */
    public final static String GLOBAL_CLIR = "clir";

    /**
     *  Wildcard for capi sign clir
     */
    public final static String CLIRED_CALL = "restricted";
    
	/**
	 *  Wildcard for internal call symbol
	 */
	public final static String INTERNAL_CALL = "internal";

	/**
	 *  Wildcard for internal call number symbol
	 */
	public final static String INTERNAL_CALL_NUMBER_SYMBOL = "00";
	
	// COMPLEX PLACEHOLDER
	
    /**
     * Wildcard for variable callername used in services
     */
	public final static String GLOBAL_VARIABLE_CALLERNAME = "%callername%";
	
	/**
	 * Wildcard for variable callernumber used in services
	 */
	public final static String GLOBAL_VARIABLE_CALLERNUMBER = "%callernumber%";
	
	/**
	 * Wildcard for variable calltime used in services
	 */
	public final static String GLOBAL_VARIABLE_CALLTIME = "%calltime%";
	
	/**
	 * Wildcard for variable calltime used in services
	 */
	public final static String GLOBAL_VARIABLE_DATE = "%date%";
	
	/**
	 * Wildcard for variable calltime used in services
	 */
	public final static String GLOBAL_VARIABLE_TIME = "%time%";
	
	/**
	 * Wildcard for variable cip used in services
	 */
	public final static String GLOBAL_VARIABLE_CIP = "%cip%";

	/**
	 * Wildcard for variable msnformat used in services
	 */
	public final static String GLOBAL_VARIABLE_MSNFORMAT = "%msnformat%";
	
	/**
	 * Wildcard for variable msn used in services
	 */
	public final static String GLOBAL_VARIABLE_MSN = "%msn%";
	
	/**
	 * Wildcard for variable msnalias used in services
	 */
	public final static String GLOBAL_VARIABLE_MSNALIAS = "%msnalias%";
	
	/**
	 * Wildcard for variable dynamic web callermanager number
	 */
	public final static String GLOBAL_VARIABLE_WEBCM_NUMBER = "%NUMBER%";
	
	// ATTRIBUTES
	
	/**
	 * Wildcard for variable attribute prefixed used in services
	 * Variables for attribute are constructured %a:attname%
	 */
	public final static String GLOBAL_VARIABLE_ATTRIBUTE_PREFIX = "%a:";
	
	/**
	 * Wildcard for variable attribute prefixed used in services
	 */
	public final static String GLOBAL_VARIABLE_ATTRIBUTE_POSTFIX = "%";
	
	/**
	 * Wildcard for variable attribute line break (CRLF)
	 */
	public final static String GLOBAL_VARIABLE_ATTRIBUTE_CRLF = "%CRLF%";
	
	// CALLER ATTRIBUTES
	
	/**
	 * Attribute name for caller firstname.
	 */
	public final static String ATTRIBUTE_NAME_FIRSTNAME = "fn";
	
	/**
	 * Attribute name for caller lastname.
	 */
	public final static String ATTRIBUTE_NAME_LASTNAME = "ln";
	
	/**
	 * Attribute name for caller additional.
	 */
	public final static String ATTRIBUTE_NAME_ADDITIONAL = "add";
	
	/**
	 * Attribute name for caller street.
	 */
	public final static String ATTRIBUTE_NAME_STREET = "str";

	/**
	 * Attribute name for caller street no.
	 */
	public final static String ATTRIBUTE_NAME_STREET_NO = "no";
	
	/**
	 * Attribute name for caller postal code.
	 */
	public final static String ATTRIBUTE_NAME_POSTAL_CODE = "pcode";
	
	/**
	 * Attribute name for caller city.
	 */
	public final static String ATTRIBUTE_NAME_CITY = "city";
	
	/**
	 * Attribute name for caller country.
	 */
	public final static String ATTRIBUTE_NAME_COUNTRY = "cntry";
	
	/**
	 * Attribute name for caller reject attribute.
	 */
	public final static String ATTRIBUTE_NAME_REJECT = "reject";

	/**
	 * Attribute name for caller image path attribute. Referencing a local 
	 * image path.
	 */
	public final static String ATTRIBUTE_NAME_IMAGEPATH = "imagepath";
	
	/**
	 * Attribute name for caller image url attribute. Referencing a remote
	 * image path.
	 */
	public final static String ATTRIBUTE_NAME_IMAGEURL = "imageurl";
	
	/**
	 * Attribute name for caller image ref attribute. Referencing content
	 * of an image in an ICallerManager store.
	 */
	public final static String ATTRIBUTE_NAME_IMAGEREF = "imageref";
	
	/**
	 * Attribute name for caller image binary attribute. Binary content
	 * of an image base64 encoded.
	 */
	public final static String ATTRIBUTE_NAME_IMAGEBINARY = "imagebinary";
	
	/**
	 * Attribute name for caller e-mail attribute. 
	 */
	public final static String ATTRIBUTE_NAME_EMAIL = "email";
	
	// CALL ATTRIBUTES
	
	/**
	 * Attribute name for callermanager attributes
	 */
	public final static String ATTRIBUTE_NAME_CALLERMANAGER = "callermanager";
	
	/**
	 * Attribute name for accepted call attribute.
	 * @deprecated since 5.0.8
	 */
	public final static String ATTRIBUTE_NAME_ACCEPTED = "accepted";
	
	
	/**
	 * Attribute name for rejected call attribute.
	 * @deprecated since 5.0.8
	 */
	public final static String ATTRIBUTE_NAME_REJECTED = "rejected";
	
	/**
	 * Attribute name for outgoing call attribute.
	 * @deprecated since 5.0.8
	 */
	public final static String ATTRIBUTE_NAME_OUTGOING = "outgoing";
	
	/**
	 * Attribute name for call statsu attribute.
	 */
	public final static String ATTRIBUTE_NAME_CALLSTATUS = "status";
	
	/**
	 * Attribute name for spoofed calls.
	 */
	public final static String ATTRIBUTE_NAME_SPOOFED = "spoofed";
	
	/**
	 * Attribute name for B channel attribute.
	 */
	public final static String ATTRIBUTE_NAME_BCHANNEL = "plci";

	/**
	 * Attribute name for start time (long) of signaling attribute.
	 */
	public final static String ATTRIBUTE_NAME_STARTRING = "start-ring";

	/**
	 * Attribute name for end time (long) of signaling attribute.
	 */
	public final static String ATTRIBUTE_NAME_ENDRING = "end-ring";

	/**
	 * Attribute name for duration (long) attribute.
	 */
	public final static String ATTRIBUTE_NAME_RINGDURATION = "ring-time";

	/**
	 * Attribute name for call disconnect reason attribute.
	 */
	public final static String ATTRIBUTE_NAME_REASON = "reason";
	
	/**
	 * Attribute name for raw number attribute.
	 */
	public final static String ATTRIBUTE_NAME_RAW_NUMBER = "raw-number";
	
	/**
	 * Attribute name for origin machine name attribute.
	 */
	public final static String ATTRIBUTE_NAME_MACHINE_NAME = "machinename";
	
	/**
	 * Attribute name for origin machine IP attribute.
	 */
	public final static String ATTRIBUTE_NAME_MACHINE_IP = "machineip";
	
	/**
	 * Attribute name for creation date attribute.
	 */
	public final static String ATTRIBUTE_NAME_CREATION = "creation";
		
	/**
	 * Attribute name for last modified date attribute.
	 */
	public final static String ATTRIBUTE_NAME_MODIFIED = "modified";
		
	
	/**
	 * Attribute name for origin user account attribute.
	 */
	public final static String ATTRIBUTE_NAME_USER_ACCOUNT = "useraccount";	
		
	/**
	 * Attribute name for extension attribute.
	 */
	public final static String ATTRIBUTE_NAME_EXTENSION = "extension";	
	
	/**
	 * Attribute name for central number of extension attribute.
	 */
	public final static String ATTRIBUTE_NAME_CENTRAL_NUMBER_OF_EXTENSION = "central-number";	
	
	/**
	 * Attribute name for category.
	 */
	public final static String ATTRIBUTE_NAME_CATEGORY = "category";	
	
	/**
	 * Attribute name for number type.
	 */
	public final static String ATTRIBUTE_NAME_NUMBER_TYPE = "number-type-";
	
	/**
	 * Attribute name for notes.
	 */
	public final static String ATTRIBUTE_NAME_NOTES = "notes";
	
	public final static String ATTRIBUTE_NAME_NOTES_AUTHOR = "notes-author";
	
	/**
	 * Attribute name for geo coding.
	 */
	public final static String ATTRIBUTE_NAME_GEO_LNG = "geo-lng";
	
	public final static String ATTRIBUTE_NAME_GEO_LAT = "geo-lat";
	
	public final static String ATTRIBUTE_NAME_GEO_ACC = "geo-acc";
	
	/**
	 * Attribute name for spam color.
	 */
	public final static String ATTRIBUTE_NAME_SPAM_COLOR = "spamcolor";	
	
	/**
	 * Attribute name for GRAVATAR photo.
	 */
	public final static String ATTRIBUTE_NAME_GRAVATAR = "gravatar";	
	
	
	
	// ATTRIBUTES VALUES
	
	/**
	 * Attribute value '257'
	 */
	public final static String ATTRIBUTE_VALUE_B1 = "257";

	/**
	 * Attribute value '513'
	 */
	public final static String ATTRIBUTE_VALUE_B2 = "513";
	
	/**
	 * Attribute value 'yes'
	 */
	public final static String ATTRIBUTE_VALUE_YES = "yes";

	/**
	 * Attribute value 'no'
	 */
	public final static String ATTRIBUTE_VALUE_NO = "no";
	
	/**
	 * Attribute value mobile type caller number
	 */
	public final static String ATTRIBUTE_VALUE_MOBILE_TYPE = "1";
	
	/**
	 * Attribute value landline type caller number
	 */
	public final static String ATTRIBUTE_VALUE_LANDLINE_TYPE = "2";
	
	/**
	 * Attribute value fax type caller number
	 */
	public final static String ATTRIBUTE_VALUE_FAX_TYPE = "3";
	
	/**
	 * Attribute value for accepted call attribute.
	 */
	public final static String ATTRIBUTE_VALUE_ACCEPTED = "accepted";
	
	
	/**
	 * Attribute value for rejected call attribute.
	 */
	public final static String ATTRIBUTE_VALUE_REJECTED = "rejected";
	
	/**
	 * Attribute value for outgoing call attribute.
	 */
	public final static String ATTRIBUTE_VALUE_OUTGOING = "outgoing";

	/**
	 * Attribute value for missed call attribute.
	 */
	public final static String ATTRIBUTE_VALUE_MISSED = "missed";
	
	
	// ICONS AND IMAGES
	
	/**
	 * Image key of SWTImageManager for jam.ico
	 */
	public final static String IMAGE_KEY_PIM_ICON = "jam.ico";
	
	/**
	 * Image key of SWTImageManager for jamx.ico
	 */
	public final static String IMAGE_KEY_PIMX_ICON = "jamx.ico";

	/**
	 * Image key of SWTImageManager for jami.ico
	 */
	public final static String IMAGE_KEY_PIMI_ICON = "jami.ico";
	
	/**
	 * Image key of SWTImageManager for b1.jpg
	 */
	public final static String IMAGE_KEY_B1_JPG = "b1.jpg";
	
	/**
	 * Image key of SWTImageManager for b2.jpg
	 */
	public final static String IMAGE_KEY_B2_JPG = "b2.jpg";
	
	/**
	 * Image key of SWTImageManager for rejected.gif
	 */
	public final static String IMAGE_KEY_REJECTED_GIF = "rejected.gif";
	
	/**
	 * Image key of SWTImageManager for away.gif
	 */
	public final static String IMAGE_KEY_AWAY_GIF = "away.gif";
	
	/**
	 * Image key of SWTImageManager for outgoing.gif
	 */
	public final static String IMAGE_KEY_OUTGOING_GIF = "outgoing.gif";
	
	/**
	 * Image key of SWTImageManager for accepted.gif
	 */
	public final static String IMAGE_KEY_ACCEPTED_GIF = "accepted.gif";
	
	/**
	 * Image key of SWTImageManager for adown.gif
	 */
	public final static String IMAGE_KEY_ADOWN_GIF = "adown.gif";
	
	/**
	 * Image key of SWTImageManager for aup.gif
	 */
	public final static String IMAGE_KEY_AUP_GIF = "aup.gif";
	
	/**
	 * Image key of SWTImageManager for filter.gif
	 */
	public final static String IMAGE_KEY_FILTER_GIF = "filter.gif";
	
	/**
	 * Image key of SWTImageManager for export.gif
	 */
	public final static String IMAGE_KEY_EXPORT_GIF = "export.gif";
	
	/**
	 * Image key of SWTImageManager for delete.gif
	 */
	public final static String IMAGE_KEY_DELETE_GIF = "delete.gif";
	
	/**
	 * Image key of SWTImageManager for clp.gif
	 */
	public final static String IMAGE_KEY_CLP_GIF = "clp.gif";
	
	/**
	 * Image key of SWTImageManager for zin.gif
	 */
	public final static String IMAGE_KEY_ZIN_GIF = "zin.gif";
	
	/**
	 * Image key of SWTImageManager for zout.gif
	 */
	public final static String IMAGE_KEY_ZOUT_GIF = "zout.gif";
	
	/**
	 * Image key of SWTImageManager for refresh.gif
	 */
	public final static String IMAGE_KEY_REFRESH_GIF = "refresh.gif";	
	
	/**
	 * Image key of SWTImageManager for rep.gif
	 */
	public final static String IMAGE_KEY_REP_GIF = "rep.gif";	
	
	/**
	 * Image key of SWTImageManager for telefon.jpg
	 */
	public final static String IMAGE_KEY_TELEFON_JPG = "telefon.jpg";
	
	/**
	 * Image key of SWTImageManager for emptycaller.jpg
	 */
	public final static String IMAGE_KEY_EMPTYCALLER_JPG = "emptycaller.jpg";
	
	/**
	 * Image key of SWTImageManager for jam.jpg
	 */
	public final static String IMAGE_KEY_PIM_JPG = "jam.jpg";
}
