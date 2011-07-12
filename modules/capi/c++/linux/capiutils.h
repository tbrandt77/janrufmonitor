#ifndef __CAPIUTILS_H__
#define __CAPIUTILS_H__

#include <sys/types.h>
#include <sys/time.h>
#include <time.h>

#include "capicmd.h"

#ifdef __cplusplus
extern "C" {
#endif

/*----- errornumbers -----*/

#define CapiToManyAppls				0x1001
#define CapiLogBlkSizeToSmall			0x1002
#define CapiBuffExeceeds64k			0x1003
#define CapiMsgBufSizeToSmall			0x1004
#define CapiAnzLogConnNotSupported		0x1005
#define CapiRegReserved				0x1006
#define CapiRegBusy				0x1007
#define CapiRegOSResourceErr			0x1008
#define CapiRegNotInstalled			0x1009
#define CapiRegCtrlerNotSupportExtEquip		0x100a
#define CapiRegCtrlerOnlySupportExtEquip	0x100b


#define CapiNoError				0x0000
#define CapiIllAppNr				0x1101
#define CapiIllCmdOrSubcmdOrMsgToSmall		0x1102
#define CapiSendQueueFull			0x1103
#define CapiReceiveQueueEmpty			0x1104
#define CapiReceiveOverflow			0x1105
#define CapiUnknownNotPar			0x1106
#define CapiMsgBusy				0x1107
#define CapiMsgOSResourceErr			0x1108
#define CapiMsgNotInstalled			0x1109
#define CapiMsgCtrlerNotSupportExtEquip		0x110a
#define CapiMsgCtrlerOnlySupportExtEquip	0x110b

/* standard CAPI2.0 function aliases */

#define CAPI20_REGISTER          capi20_register
#define CAPI20_RELEASE           capi20_release
#define CAPI20_PUT_MESSAGE       capi20_put_message
#define CAPI20_GET_MESSAGE       capi20_get_message
#define CAPI20_WaitforMessage	 capi20_waitformessage
#define CAPI20_GET_MANUFACTURER  capi20_get_manufacturer
#define CAPI20_GET_VERSION       capi20_get_version
#define CAPI20_GET_SERIAL_NUMBER capi20_get_serial_number
#define CAPI20_GET_PROFILE       capi20_get_profile
#define CAPI20_ISINSTALLED       capi20_isinstalled


#define CAPI_REGISTER_ERROR	unsigned
#define MESSAGE_EXCHANGE_ERROR	unsigned

typedef unsigned char *CAPI_MESSAGE;

#if defined(__GLIBC__) && defined(__GLIBC_MINOR__)
#if (__GLIBC__ == 2 && __GLIBC_MINOR__ >= 1) || __GLIBC__ > 2
#define HAS_UINT8_T
#include <stdint.h>
#endif
#endif

#ifdef HAS_UINT8_T
typedef uint8_t   _cbyte;
typedef uint16_t  _cword;
typedef uint32_t  _cdword;
typedef uint64_t  _cqword;
#else
typedef unsigned char      _cbyte;
typedef unsigned short     _cword;
typedef unsigned long      _cdword;
typedef unsigned long long _cqword;
#endif

typedef CAPI_MESSAGE   _cstruct;
typedef enum { CAPI_COMPOSE = 0, CAPI_DEFAULT = 1 } _cmstruct;

/*----- CAPI commands -----*/

#define CAPI_ALERT		    0x01
#define CAPI_CONNECT		    0x02
#define CAPI_CONNECT_ACTIVE	    0x03
#define CAPI_CONNECT_B3_ACTIVE	    0x83
#define CAPI_CONNECT_B3 	    0x82
#define CAPI_CONNECT_B3_T90_ACTIVE  0x88
#define CAPI_DATA_B3		    0x86
#define CAPI_DISCONNECT_B3	    0x84
#define CAPI_DISCONNECT 	    0x04
#define CAPI_FACILITY		    0x80
#define CAPI_INFO		    0x08
#define CAPI_LISTEN		    0x05
#define CAPI_MANUFACTURER	    0xff
#define CAPI_RESET_B3		    0x87
#define CAPI_SELECT_B_PROTOCOL	    0x41

/*----- CAPI subcommands -----*/

#define CAPI_REQ    0x80
#define CAPI_CONF   0x81
#define CAPI_IND    0x82
#define CAPI_RESP   0x83

/*-----------------------------------------------------------------------*/

#define CAPIMSG_BASELEN		8
#define CAPIMSG_U8(m, off)	(m[off])
#define CAPIMSG_U16(m, off)	(m[off]|(m[(off)+1]<<8))
#define CAPIMSG_U32(m, off)	(m[off]|(m[(off)+1]<<8)|(m[(off)+2]<<16)|(m[(off)+3]<<24))
#define	CAPIMSG_LEN(m)		CAPIMSG_U16(m,0)
#define	CAPIMSG_APPID(m)	CAPIMSG_U16(m,2)
#define	CAPIMSG_COMMAND(m)	CAPIMSG_U8(m,4)
#define	CAPIMSG_SUBCOMMAND(m)	CAPIMSG_U8(m,5)
#define CAPIMSG_CMD(m)		(((m[4])<<8)|(m[5]))
#define	CAPIMSG_MSGID(m)	CAPIMSG_U16(m,6)
#define CAPIMSG_CONTROLLER(m)	(m[8] & 0x7f)
#define CAPIMSG_CONTROL(m)	CAPIMSG_U32(m, 8)
#define CAPIMSG_NCCI(m)		CAPIMSG_CONTROL(m)
#define CAPIMSG_DATALEN(m)	CAPIMSG_U16(m,16) /* DATA_B3_REQ */

static inline void capimsg_setu8(void *m, int off, _cbyte val)
{
	((_cbyte *)m)[off] = val;
}

static inline void capimsg_setu16(void *m, int off, _cword val)
{
	((_cbyte *)m)[off] = val & 0xff;
	((_cbyte *)m)[off+1] = (val >> 8) & 0xff;
}

static inline void capimsg_setu32(void *m, int off, _cdword val)
{
	((_cbyte *)m)[off] = val & 0xff;
	((_cbyte *)m)[off+1] = (val >> 8) & 0xff;
	((_cbyte *)m)[off+2] = (val >> 16) & 0xff;
	((_cbyte *)m)[off+3] = (val >> 24) & 0xff;
}

#define	CAPIMSG_SETLEN(m, len)		capimsg_setu16(m, 0, len)
#define	CAPIMSG_SETAPPID(m, applid)	capimsg_setu16(m, 2, applid)
#define	CAPIMSG_SETCOMMAND(m,cmd)	capimsg_setu8(m, 4, cmd)
#define	CAPIMSG_SETSUBCOMMAND(m, cmd)	capimsg_setu8(m, 5, cmd)
#define	CAPIMSG_SETMSGID(m, msgid)	capimsg_setu16(m, 6, msgid)
#define	CAPIMSG_SETCONTROL(m, contr)	capimsg_setu32(m, 8, contr)
#define	CAPIMSG_SETDATALEN(m, len)	capimsg_setu16(m, 16, len)


/*
 * The _cmsg structure contains all possible CAPI 2.0 parameter.
 * All parameters are stored here first. The function capi_cmsg2message()
 * assembles the parameter and builds CAPI2.0 conform messages.
 * capi_message2cmsg disassembles CAPI 2.0 messages and stores the
 * parameter in the _cmsg structure
 */

typedef struct {
	/* Header */
	_cword ApplId;
	_cbyte Command;
	_cbyte Subcommand;
	_cword Messagenumber;

	/* Parameter */
	union {
		_cdword adrController;
		_cdword adrPLCI;
		_cdword adrNCCI;
	} adr;

	_cmstruct AdditionalInfo;
	_cstruct B1configuration;
	_cword B1protocol;
	_cstruct B2configuration;
	_cword B2protocol;
	_cstruct B3configuration;
	_cword B3protocol;
	_cstruct BC;
	_cstruct BChannelinformation;
	_cmstruct BProtocol;
	_cstruct CalledPartyNumber;
	_cstruct CalledPartySubaddress;
	_cstruct CallingPartyNumber;
	_cstruct CallingPartySubaddress;
	_cdword CIPmask;
	_cdword CIPmask2;
	_cword CIPValue;
	_cdword Class;
	_cstruct ConnectedNumber;
	_cstruct ConnectedSubaddress;
	_cdword Data32;
	_cqword Data64;
	_cword DataHandle;
	_cword DataLength;
	_cstruct FacilityConfirmationParameter;
	_cstruct Facilitydataarray;
	_cstruct FacilityIndicationParameter;
	_cstruct FacilityRequestParameter;
	_cstruct FacilityResponseParameters;
	_cword FacilitySelector;
	_cword Flags;
	_cdword Function;
#ifndef CAPI_LIBRARY_V2
	_cstruct Globalconfiguration;
#endif 
	_cstruct HLC;
	_cword Info;
	_cstruct InfoElement;
	_cdword InfoMask;
	_cword InfoNumber;
	_cstruct Keypadfacility;
	_cstruct LLC;
	_cstruct ManuData;
	_cdword ManuID;
	_cstruct NCPI;
	_cword Reason;
	_cword Reason_B3;
	_cword Reject;
	_cstruct Useruserdata;
#ifndef CAPI_LIBRARY_V2
	_cstruct SendingComplete;
#endif 
	unsigned char *Data;

	/* intern */
	unsigned l, p;
	unsigned char *par;
	_cbyte *m;

	/* buffer to construct message */
	_cbyte buf[180];

} _cmsg;


/*
 * capi_cmsg2message() assembles the parameter from _cmsg to a CAPI 2.0
 * conform message
 */
#define capi20_cmsg2message	capi_cmsg2message
unsigned capi_cmsg2message(_cmsg *cmsg, unsigned char *msg);

/*
 *  capi20_message2cmsg disassembles a CAPI message an writes the parameter
 *  into _cmsg for easy access
 */
#define capi20_message2cmsg	capi_message2cmsg
unsigned capi_message2cmsg (_cmsg *cmsg, unsigned char *msg);

/*
 * capi20_cmsg_header() fills the _cmsg structure with default values,
 * so only parameter with non default values must be changed before
 * sending the message.
 */

#define CAPI_CMSG_HEADER	capi_cmsg_header
#define capi20_cmsg_header	capi_cmsg_header
unsigned capi_cmsg_header (_cmsg *cmsg, unsigned _ApplId, _cbyte _Command, _cbyte _Subcommand, _cword _Messagenumber, _cdword _Controller);

/*
 * capi_cmsg_answer() is used to answer indications. It changes the header
 * of an indication to a response, and leaves all other parameters the same
 */

#define capi20_cmsg_answer	capi_cmsg_answer

static inline void capi_cmsg_answer(_cmsg * cmsg)
{
	cmsg->Subcommand |= 0x01;
}

/*
 * capi_get_cmsg() works like capi20_get_message() and converts the
 * CAPI message * to a _cmsg with capi_message2cmsg().
 * Possible errors from capi20_get_message() will be returned.
 */

#define CAPI_GET_CMSG	capi_get_cmsg
#define capi20_get_cmsg	capi_get_cmsg
unsigned capi_get_cmsg(_cmsg *cmsg, unsigned applid);

/*
 * capi_put_cmsg() works like capi20_put_message() but it converts the
 * _cmsg * first with capi_cmsg2message(). Possible errors from
 * capi_put_message() will be returned.
 */

#define CAPI_PUT_CMSG	capi_put_cmsg
#define capi20_put_cmsg	capi_put_cmsg
unsigned capi_put_cmsg(_cmsg *cmsg);

/*-----------------------------------------------------------------------*/

/*
 * Debugging / Tracing functions
 */

/*
 * capi_info2str generated a readable string for Capi2.0 reasons.
 */
#define capi20_info2str	capi_info2str
char *capi_info2str(_cword reason);

#define capi20_cmd2str	capi_cmd2str
char *capi_cmd2str(_cbyte cmd, _cbyte subcmd);

#define capi20_cmsg2str	capi_cmsg2str
char *capi_cmsg2str(_cmsg * cmsg);

#define capi20_message2str capi_message2str
char *capi_message2str(_cbyte * msg);

/*-----------------------------------------------------------------------*/

#define ALERT_REQ_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define ALERT_REQ_ADDITIONALINFO(x) ((x)->AdditionalInfo)
		 /* Additional info elements */
#define ALERT_REQ_BCHANNELINFORMATION(x) ((x)->BChannelinformation)
#define ALERT_REQ_KEYPADFACILITY(x) ((x)->Keypadfacility)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define ALERT_REQ_USERUSERDATA(x) ((x)->Useruserdata)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define ALERT_REQ_FACILITYDATAARRAY(x) ((x)->Facilitydataarray)
		 /* which is used to transfer additional parameters coded */
		 /* according to ETS 300 102-1 / Q.931 starting from octet 1.  */
		 /* This field is used to transport one or more complete facility  */
		 /* data information elements. */
#define ALERT_CONF_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define ALERT_CONF_INFO(x) ((x)->Info)
		 /* 0: alert initiated */
		 /* 0x0003: alert already sent by another application */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal PLCI */
		 /* 0x2007: illegal message parameter coding */
#define CONNECT_REQ_CONTROLLER(x) ((x)->adr.adrController)
#define CONNECT_REQ_CIPVALUE(x) ((x)->CIPValue)
		 /* Compatibility Information Profile */
#define CONNECT_REQ_CALLEDPARTYNUMBER(x) ((x)->CalledPartyNumber)
		 /* Called party number */
#define CONNECT_REQ_CALLINGPARTYNUMBER(x) ((x)->CallingPartyNumber)
		 /* Calling party number */
#define CONNECT_REQ_CALLEDPARTYSUBADDRESS(x) ((x)->CalledPartySubaddress)
		 /* Called party subaddress */
#define CONNECT_REQ_CALLINGPARTYSUBADDRESS(x) ((x)->CallingPartySubaddress)
		 /* Calling party subaddress */
#define CONNECT_REQ_BPROTOCOL(x) ((x)->BProtocol)
		 /* B protocol to be used */
#define CONNECT_REQ_B1PROTOCOL(x) ((x)->B1protocol)
		 /* Physical layer and framing */
#define CONNECT_REQ_B2PROTOCOL(x) ((x)->B2protocol)
		 /* Data link layer */
#define CONNECT_REQ_B3PROTOCOL(x) ((x)->B3protocol)
		 /* Network layer */
#define CONNECT_REQ_B1CONFIGURATION(x) ((x)->B1configuration)
		 /* Physical layer and framing parameter */
#define CONNECT_REQ_B2CONFIGURATION(x) ((x)->B2configuration)
		 /* Data link layer parameter */
#define CONNECT_REQ_B3CONFIGURATION(x) ((x)->B3configuration)
		 /* Network layer parameter */
#ifndef CAPI_LIBRARY_V2
#define CONNECT_REQ_GLOBALCONFIGURATION(x) ((x)->Globalconfiguration)
		 /* all layer parameter */
#endif 
#define CONNECT_REQ_BC(x) ((x)->BC)
		 /* Bearer Capability */
#define CONNECT_REQ_LLC(x) ((x)->LLC)
		 /* Low Layer Compatibility */
#define CONNECT_REQ_HLC(x) ((x)->HLC)
		 /* High Layer Compatibility */
#define CONNECT_REQ_ADDITIONALINFO(x) ((x)->AdditionalInfo)
		 /* Additional information elements */
#define CONNECT_REQ_BCHANNELINFORMATION(x) ((x)->BChannelinformation)
#define CONNECT_REQ_KEYPADFACILITY(x) ((x)->Keypadfacility)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define CONNECT_REQ_USERUSERDATA(x) ((x)->Useruserdata)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define CONNECT_REQ_FACILITYDATAARRAY(x) ((x)->Facilitydataarray)
		 /* which is used to transfer additional parameters coded */
		 /* according to ETS 300 102-1 / Q.931 starting from octet 1.  */
		 /* This field is used to transport one or more complete facility  */
		 /* data information elements. */
#define CONNECT_CONF_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define CONNECT_CONF_INFO(x) ((x)->Info)
		 /* 0: connect initiated */
		 /* 0x2002: illegal controller */
		 /* 0x2003: out of PLCI */
		 /* 0x2007: illegal message parameter coding */
		 /* 0x3001: B1 protocol not supported */
		 /* 0x3002: B2 protocol not supported */
		 /* 0x3003: B3 protocol not supported */
		 /* 0x3004: B1 protocol parameter not supported */
		 /* 0x3005: B2 protocol parameter not supported */
		 /* 0x3006: B3 protocol parameter not supported */
		 /* 0x3007: B protocol combination not supported */
		 /* 0x300A: CIP Value unknown */
#define CONNECT_IND_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define CONNECT_IND_CIPVALUE(x) ((x)->CIPValue)
		 /* Compatibility Information Profile */
#define CONNECT_IND_CALLEDPARTYNUMBER(x) ((x)->CalledPartyNumber)
		 /* Called party number */
#define CONNECT_IND_CALLINGPARTYNUMBER(x) ((x)->CallingPartyNumber)
		 /* Calling party number */
#define CONNECT_IND_CALLEDPARTYSUBADDRESS(x) ((x)->CalledPartySubaddress)
		 /* Called party subaddress */
#define CONNECT_IND_CALLINGPARTYSUBADDRESS(x) ((x)->CallingPartySubaddress)
		 /* Calling party subaddress */
#define CONNECT_IND_BC(x) ((x)->BC)
		 /* Bearer compatibility */
#define CONNECT_IND_LLC(x) ((x)->LLC)
		 /* Low Layer Compatibility */
#define CONNECT_IND_HLC(x) ((x)->HLC)
		 /* High Layer Compatibility */
#define CONNECT_IND_ADDITIONALINFO(x) ((x)->AdditionalInfo)
		 /* Additional information elements */
#define CONNECT_IND_BCHANNELINFORMATION(x) ((x)->BChannelinformation)
#define CONNECT_IND_KEYPADFACILITY(x) ((x)->Keypadfacility)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define CONNECT_IND_USERUSERDATA(x) ((x)->Useruserdata)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define CONNECT_IND_FACILITYDATAARRAY(x) ((x)->Facilitydataarray)
		 /* which is used to transfer additional parameters coded */
		 /* according to ETS 300 102-1 / Q.931 starting from octet 1.  */
		 /* This field is used to transport one or more complete facility  */
		 /* data information elements. */
#define CONNECT_RESP_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define CONNECT_RESP_REJECT(x) ((x)->Reject)
		 /* 0: accept call */
		 /* 1: ignore call */
		 /* 2: reject call, normal call clearing */
		 /* 3: reject call, user busy */
		 /* 4: reject call, requestet circuit/channel not available */
		 /* 5: reject call, facility rejected */
		 /* 6: reject call, channel unacceptable */
		 /* 7: reject call, incompatible destination */
		 /* 8: reject call, destination out of order */
#define CONNECT_RESP_BPROTOCOL(x) ((x)->BProtocol)
		 /* B protocol to be used */
#define CONNECT_RESP_B1PROTOCOL(x) ((x)->B1protocol)
		 /* Physical layer and framing */
#define CONNECT_RESP_B2PROTOCOL(x) ((x)->B2protocol)
		 /* Data link layer */
#define CONNECT_RESP_B3PROTOCOL(x) ((x)->B3protocol)
		 /* Network layer */
#define CONNECT_RESP_B1CONFIGURATION(x) ((x)->B1configuration)
		 /* Physical layer and framing parameter */
#define CONNECT_RESP_B2CONFIGURATION(x) ((x)->B2configuration)
		 /* Data link layer parameter */
#define CONNECT_RESP_B3CONFIGURATION(x) ((x)->B3configuration)
		 /* Network layer parameter */
#ifndef CAPI_LIBRARY_V2
#define CONNECT_RESP_GLOBALCONFIGURATION(x) ((x)->Globalconfiguration)
		 /* all layer parameter */
#endif
#define CONNECT_RESP_CONNECTEDNUMBER(x) ((x)->ConnectedNumber)
		 /* Connected number */
#define CONNECT_RESP_CONNECTEDSUBADDRESS(x) ((x)->ConnectedSubaddress)
		 /* Connected subaddress */
#define CONNECT_RESP_LLC(x) ((x)->LLC)
		 /* Low Layer Compatibility */
#define CONNECT_RESP_ADDITIONALINFO(x) ((x)->AdditionalInfo)
		 /* Additional information elements */
#define CONNECT_RESP_BCHANNELINFORMATION(x) ((x)->BChannelinformation)
#define CONNECT_RESP_KEYPADFACILITY(x) ((x)->Keypadfacility)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define CONNECT_RESP_USERUSERDATA(x) ((x)->Useruserdata)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define CONNECT_RESP_FACILITYDATAARRAY(x) ((x)->Facilitydataarray)
		 /* which is used to transfer additional parameters coded */
		 /* according to ETS 300 102-1 / Q.931 starting from octet 1.  */
		 /* This field is used to transport one or more complete facility  */
		 /* data information elements. */
#define CONNECT_ACTIVE_IND_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define CONNECT_ACTIVE_IND_CONNECTEDNUMBER(x) ((x)->ConnectedNumber)
		 /* Connected number */
#define CONNECT_ACTIVE_IND_CONNECTEDSUBADDRESS(x) ((x)->ConnectedSubaddress)
		 /* Connected subaddress */
#define CONNECT_ACTIVE_IND_LLC(x) ((x)->LLC)
		 /* Low Layer Compatibility */
#define CONNECT_ACTIVE_RESP_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define CONNECT_B3_ACTIVE_IND_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define CONNECT_B3_ACTIVE_IND_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define CONNECT_B3_ACTIVE_RESP_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define CONNECT_B3_REQ_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define CONNECT_B3_REQ_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define CONNECT_B3_CONF_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define CONNECT_B3_CONF_INFO(x) ((x)->Info)
		 /* 0: connect initiated */
		 /* 0x0001: NCPI not supported by current protocol, NCPI ignored */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal PLCI */
		 /* 0x2004: out of NCCI */
		 /* 0x3008: NCPI not supported */
#define CONNECT_B3_IND_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define CONNECT_B3_IND_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define CONNECT_B3_RESP_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define CONNECT_B3_RESP_REJECT(x) ((x)->Reject)
		 /* 0: accept call */
		 /* 2: reject call, normal call clearing */
#define CONNECT_B3_RESP_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define CONNECT_B3_T90_ACTIVE_IND_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define CONNECT_B3_T90_ACTIVE_IND_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define CONNECT_B3_T90_ACTIVE_RESP_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DATA_B3_REQ_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DATA_B3_REQ_DATA(x) ((x)->Data)
		 /* Pointer to the data to be sent */
#define DATA_B3_REQ_DATALENGTH(x) ((x)->DataLength)
		 /* Size of data area to be sent */
#define DATA_B3_REQ_DATAHANDLE(x) ((x)->DataHandle)
		 /* Referenced in DATA_B3_CONF */
#define DATA_B3_REQ_FLAGS(x) ((x)->Flags)
		 /* [0]: qualifier bit */
		 /* [1]: more data bit */
		 /* [2]: delivery confirmation bit */
		 /* [3]: expedited data */
		 /* [4] to [15]: reserved */
#define DATA_B3_CONF_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DATA_B3_CONF_DATAHANDLE(x) ((x)->DataHandle)
		 /* Identifies the data area of corresponding DATA_B3_REQ */
#define DATA_B3_CONF_INFO(x) ((x)->Info)
		 /* 0: data transmission initiated */
		 /* 0x0002: flags not supported by current protocol, flags ignored */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal NCCI */
		 /* 0x2007: illegal message parameter coding */
		 /* 0x300A: flags not supported (reserved bits) */
		 /* 0x300C: data length not supported by current protocol */
#define DATA_B3_IND_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DATA_B3_IND_DATA(x) ((x)->Data)
		 /* Pointer to data received */
#define DATA_B3_IND_DATALENGTH(x) ((x)->DataLength)
		 /* Size of data area received */
#define DATA_B3_IND_DATAHANDLE(x) ((x)->DataHandle)
		 /* handle to data area, referenced in DATA_B3_RESP */
#define DATA_B3_IND_FLAGS(x) ((x)->Flags)
		 /* 0: qualifier bit */
		 /* 1: more-data bit */
		 /* 2: delivery confirmation bit */
		 /* 3: expedited data */
		 /* 4 to 14: reserved */
		 /* [15]: framing error bit, data may be invalid (only with corresponding B2 protocol) */
#define DATA_B3_RESP_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DATA_B3_RESP_DATAHANDLE(x) ((x)->DataHandle)
		 /* Data area reference in corresponding DATA_B3_IND */
#define DISCONNECT_B3_REQ_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DISCONNECT_B3_REQ_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define DISCONNECT_B3_CONF_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DISCONNECT_B3_CONF_INFO(x) ((x)->Info)
		 /* 0: disconnect initiated */
		 /* 0x0001: NCPI not supported by current protocol, NCPI ignored */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal NCCI */
		 /* 0x2007: illegal message parameter coding */
		 /* 0x3008: NCPI not supported */
#define DISCONNECT_B3_IND_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DISCONNECT_B3_IND_REASON_B3(x) ((x)->Reason_B3)
		 /* 0: clearing according to protocol */
		 /* 0x3301: protocol error layer 1 */
		 /* 0x3302: protocol error layer 2 */
		 /* 0x3303: protocol error layer 3 */
		 /* protocol dependent values are described in chapter 6 */
#define DISCONNECT_B3_IND_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define DISCONNECT_B3_RESP_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define DISCONNECT_REQ_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define DISCONNECT_REQ_ADDITIONALINFO(x) ((x)->AdditionalInfo)
		 /* Additional information elements */
#define DISCONNECT_REQ_BCHANNELINFORMATION(x) ((x)->BChannelinformation)
#define DISCONNECT_REQ_KEYPADFACILITY(x) ((x)->Keypadfacility)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define DISCONNECT_REQ_USERUSERDATA(x) ((x)->Useruserdata)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define DISCONNECT_REQ_FACILITYDATAARRAY(x) ((x)->Facilitydataarray)
		 /* which is used to transfer additional parameters coded */
		 /* according to ETS 300 102-1 / Q.931 starting from octet 1.  */
		 /* This field is used to transport one or more complete facility  */
		 /* data information elements. */
#define DISCONNECT_CONF_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define DISCONNECT_CONF_INFO(x) ((x)->Info)
		 /* 0: disconnect initiated */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal PLCI */
		 /* 0x2007: illegal message parameter coding */
#define DISCONNECT_IND_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define DISCONNECT_IND_REASON(x) ((x)->Reason)
		 /* 0: no cause available */
		 /* 0x3301: protocol error layer 1 */
		 /* 0x3302: protocol error layer 2 */
		 /* 0x3303: protocol error layer 3 */
		 /* 0x3304: another application got that call */
		 /* 0x34xx: disconnect cause from the network according to Q.931/ETS 300 102-1. In the field 'xx' the cause value received within a cause information element (octet 4) from the network is indicated. */
#define DISCONNECT_RESP_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define FACILITY_REQ_CONTROLLER(x) ((x)->adr.adrController)
#define FACILITY_REQ_PLCI(x) ((x)->adr.adrPLCI)
#define FACILITY_REQ_NCCI(x) ((x)->adr.adrNCCI)
		 /* Depending on the facility selector */
#define FACILITY_REQ_FACILITYSELECTOR(x) ((x)->FacilitySelector)
		 /* 0: Handset Support */
		 /* 1: DTMF */
		 /* 2 to n: reserved */
#define FACILITY_REQ_FACILITYREQUESTPARAMETER(x) ((x)->FacilityRequestParameter)
		 /* Facility depending parameters */
#define FACILITY_CONF_CONTROLLER(x) ((x)->adr.adrController)
#define FACILITY_CONF_PLCI(x) ((x)->adr.adrPLCI)
#define FACILITY_CONF_NCCI(x) ((x)->adr.adrNCCI)
		 /* Depending on the facility selector */
#define FACILITY_CONF_INFO(x) ((x)->Info)
		 /* 0: request accepted */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: incorrect Controller/PLCI/NCCI */
		 /* 0x2007: illegal message parameter coding */
		 /* 0x300B: facility not supported */
#define FACILITY_CONF_FACILITYSELECTOR(x) ((x)->FacilitySelector)
		 /* 0: Handset Support */
		 /* 1: DTMF */
		 /* 2 to n: reserved */
#define FACILITY_CONF_FACILITYCONFIRMATIONPARAMETER(x) ((x)->FacilityConfirmationParameter)
		 /* Facility-depending parameters */
#define FACILITY_IND_CONTROLLER(x) ((x)->adr.adrController)
#define FACILITY_IND_PLCI(x) ((x)->adr.adrPLCI)
#define FACILITY_IND_NCCI(x) ((x)->adr.adrNCCI)
		 /* Depending on the facility selector */
#define FACILITY_IND_FACILITYSELECTOR(x) ((x)->FacilitySelector)
		 /* 0: Handset Support */
		 /* 1: DTMF */
		 /* 2 to n: reserved */
#define FACILITY_IND_FACILITYINDICATIONPARAMETER(x) ((x)->FacilityIndicationParameter)
		 /* Facility-depending parameters */
#define FACILITY_RESP_CONTROLLER(x) ((x)->adr.adrController)
#define FACILITY_RESP_PLCI(x) ((x)->adr.adrPLCI)
#define FACILITY_RESP_NCCI(x) ((x)->adr.adrNCCI)
		 /* Depending on the facility selector */
#define FACILITY_RESP_FACILITYSELECTOR(x) ((x)->FacilitySelector)
		 /* 0: Handset Support */
		 /* 1: DTMF */
		 /* 2 to n: reserved */
		 /* Facility response parameters */
		 /* struct */
		 /* Facility-depending parameters */
#define INFO_REQ_CONTROLLER(x) ((x)->adr.adrController)
#define INFO_REQ_PLCI(x) ((x)->adr.adrPLCI)
		 /* See note */
#define INFO_REQ_CALLEDPARTYNUMBER(x) ((x)->CalledPartyNumber)
		 /* Called party number */
#define INFO_REQ_ADDITIONALINFO(x) ((x)->AdditionalInfo)
		 /* Additional information elements */
#define INFO_REQ_BCHANNELINFORMATION(x) ((x)->BChannelinformation)
#define INFO_REQ_KEYPADFACILITY(x) ((x)->Keypadfacility)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define INFO_REQ_USERUSERDATA(x) ((x)->Useruserdata)
		 /* coded according to ETS 300 102-1 / Q.931 */
#define INFO_REQ_FACILITYDATAARRAY(x) ((x)->Facilitydataarray)
		 /* which is used to transfer additional parameters coded */
		 /* according to ETS 300 102-1 / Q.931 starting from octet 1.  */
		 /* This field is used to transport one or more complete facility  */
		 /* data information elements. */
#define INFO_CONF_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define INFO_CONF_INFO(x) ((x)->Info)
		 /* 0: transmission of information initiated */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal Controller/PLCI */
		 /* 0x2003: out of PLCI */
		 /* 0x2007: illegal message parameter coding */
#define INFO_IND_CONTROLLER(x) ((x)->adr.adrController)
#define INFO_IND_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define INFO_IND_INFONUMBER(x) ((x)->InfoNumber)
		 /* Information element identifier */
#define INFO_IND_INFOELEMENT(x) ((x)->InfoElement)
		 /* Information element dependent structure */
#define INFO_RESP_CONTROLLER(x) ((x)->adr.adrController)
#define INFO_RESP_PLCI(x) ((x)->adr.adrPLCI)
		 /* As in INFO_IND */
#define LISTEN_REQ_CONTROLLER(x) ((x)->adr.adrController)
#define LISTEN_REQ_INFOMASK(x) ((x)->InfoMask)
		 /* Bit field, coding as follows: */
		 /* 0: cause */
		 /* 1: date/Time */
		 /* 2: display */
		 /* 3: user-user information */
		 /* 4: call progression */
		 /* 5: facility */
		 /* 6: charging */
		 /* 7 to 31: reserved */
#define LISTEN_REQ_CIPMASK(x) ((x)->CIPmask)
		 /* Bit field, coding as follows: */
		 /* 0: any match */
		 /* 1: speech */
		 /* 2: unrestricted digital information */
		 /* 3: restricted digital information */
		 /* 4: 3.1 kHz audio */
		 /* 5: 7.0 kHz audio */
		 /* 6: video */
		 /* 7: packet mode */
		 /* 8: 56 kBit/s rate adaptation */
		 /* 9: unrestricted digital information with tones/announcements */
		 /* 10..15: reserved */
		 /* 16: telephony */
		 /* 17: fax group 2/3 */
		 /* 18: fax group 4 class 1 */
		 /* 19: Teletex service (basic and mixed), fax group 4 class 2 */
		 /* 20: Teletex service (basic and processable) */
		 /* 21: Teletex service (basic) */
		 /* 22: Videotex */
		 /* 23: Telex */
		 /* reserved for additional services */
		 /* 25: OSI applications according X.200 */
		 /* 26: 7 kHz Telephony */
		 /* 27: Video Telephony F.721, first connection */
		 /* 28: Video Telephony F.721, second connection */
		 /* 29 to 31: reserved */
#define LISTEN_REQ_CIPMASK2(x) ((x)->CIPmask2)
		 /* reserved for additional services */
#define LISTEN_REQ_CALLINGPARTYNUMBER(x) ((x)->CallingPartyNumber)
		 /* Calling party number */
#define LISTEN_REQ_CALLINGPARTYSUBADDRESS(x) ((x)->CallingPartySubaddress)
		 /* Calling party subaddress */
#define LISTEN_CONF_CONTROLLER(x) ((x)->adr.adrController)
#define LISTEN_CONF_INFO(x) ((x)->Info)
		 /* 0: listen is active */
		 /* 0x2002: illegal controller */
		 /* 0x2005: out of LISTEN-Resources */
		 /* 0x2007: illegal message parameter coding */
#define MANUFACTURER_REQ_CONTROLLER(x) ((x)->adr.adrController)
#define MANUFACTURER_REQ_MANUID(x) ((x)->ManuID)
		 /* Manufacturer specific ID (should be unique) */
		 /* Manufacturer specific */
#define MANUFACTURER_REQ_CLASS(x) ((x)->Class)
#define MANUFACTURER_REQ_FUNCTION(x) ((x)->Function)
#define MANUFACTURER_REQ_MANUDATA(x) ((x)->ManuData)
		 /* Manufacturer specific data */
#define MANUFACTURER_CONF_CONTROLLER(x) ((x)->adr.adrController)
#define MANUFACTURER_CONF_MANUID(x) ((x)->ManuID)
		 /* Manufacturer specific ID (should be unique) */
		 /* Manufacturer specific */
#define MANUFACTURER_CONF_CLASS(x) ((x)->Class)
#define MANUFACTURER_CONF_FUNCTION(x) ((x)->Function)
#define MANUFACTURER_CONF_MANUDATA(x) ((x)->ManuData)
		 /* Manufacturer specific data */
#define MANUFACTURER_IND_CONTROLLER(x) ((x)->adr.adrController)
#define MANUFACTURER_IND_MANUID(x) ((x)->ManuID)
		 /* Manufacturer specific ID (should be unique) */
		 /* Manufacturer specific */
#define MANUFACTURER_IND_CLASS(x) ((x)->Class)
#define MANUFACTURER_IND_FUNCTION(x) ((x)->Function)
#define MANUFACTURER_IND_MANUDATA(x) ((x)->ManuData)
		 /* Manufacturer specific data */
#define MANUFACTURER_RESP_CONTROLLER(x) ((x)->adr.adrController)
#define MANUFACTURER_RESP_MANUID(x) ((x)->ManuID)
		 /* Manufacturer specific ID (should be unique) */
		 /* Manufacturer specific */
#define MANUFACTURER_RESP_CLASS(x) ((x)->Class)
#define MANUFACTURER_RESP_FUNCTION(x) ((x)->Function)
#define MANUFACTURER_RESP_MANUDATA(x) ((x)->ManuData)
		 /* Manufacturer specific data */
#define RESET_B3_REQ_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define RESET_B3_REQ_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define RESET_B3_CONF_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define RESET_B3_CONF_INFO(x) ((x)->Info)
		 /* 0: reset initiated */
		 /* 0x0001: NCPI not supported by current protocol, NCPI ignored */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal NCCI */
		 /* 0x2007: illegal message parameter coding */
		 /* 0x3008: NCPI not supported */
		 /* 0x300D: reset procedure not supported by current protocol */
#define RESET_B3_IND_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define RESET_B3_IND_NCPI(x) ((x)->NCPI)
		 /* Network Control Protocol Information */
#define RESET_B3_RESP_NCCI(x) ((x)->adr.adrNCCI)
		 /* Network Control Connection Identifier */
#define SELECT_B_PROTOCOL_REQ_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define SELECT_B_PROTOCOL_REQ_BPROTOCOL(x) ((x)->BProtocol)
		 /* Protocol definition */
#define SELECT_B_PROTOCOL_REQ_B1PROTOCOL(x) ((x)->B1protocol)
		 /* Physical layer and framing */
#define SELECT_B_PROTOCOL_REQ_B2PROTOCOL(x) ((x)->B2protocol)
		 /* Data link layer */
#define SELECT_B_PROTOCOL_REQ_B3PROTOCOL(x) ((x)->B3protocol)
		 /* Network layer */
#define SELECT_B_PROTOCOL_REQ_B1CONFIGURATION(x) ((x)->B1configuration)
		 /* Physical layer and framing parameter */
#define SELECT_B_PROTOCOL_REQ_B2CONFIGURATION(x) ((x)->B2configuration)
		 /* Data link layer parameter */
#define SELECT_B_PROTOCOL_REQ_B3CONFIGURATION(x) ((x)->B3configuration)
		 /* Network layer parameter */
#ifndef CAPI_LIBRARY_V2
#define SELECT_B_PROTOCOL_REQ_GLOBALCONFIGURATION(x) ((x)->Globalconfiguration)
		 /* all layer parameter */
#endif
#define SELECT_B_PROTOCOL_CONF_PLCI(x) ((x)->adr.adrPLCI)
		 /* Physical Link Connection Identifier */
#define SELECT_B_PROTOCOL_CONF_INFO(x) ((x)->Info)
		 /* 0: protocol switch successful */
		 /* 0x2001: message not supported in current state */
		 /* 0x2002: illegal PLCI */
		 /* 0x2007: illegal message parameter coding */
		 /* 0x3001: B1 protocol not supported */
		 /* 0x3002: B2 protocol not supported */
		 /* 0x3003: B3 protocol not supported */
		 /* 0x3004: B1 protocol parameter not supported */
		 /* 0x3005: B2 protocol parameter not supported */
		 /* 0x3006: B3 protocol parameter not supported */
		 /* 0x3007: B protocol combination not supported */

/*----- tests for specific commands -----*/

#define IS_CONNECT_IND(m) ((m)->Command==0x02 && (m)->Subcommand==0x82)
#define IS_CONNECT_ACTIVE_IND(m) ((m)->Command==0x03 && (m)->Subcommand==0x82)
#define IS_CONNECT_B3_ACTIVE_IND(m) ((m)->Command==0x83 && (m)->Subcommand==0x82)
#define IS_CONNECT_B3_IND(m) ((m)->Command==0x82 && (m)->Subcommand==0x82)
#define IS_CONNECT_B3_T90_ACTIVE_IND(m) ((m)->Command==0x88 && (m)->Subcommand==0x82)
#define IS_DATA_B3_IND(m) ((m)->Command==0x86 && (m)->Subcommand==0x82)
#define IS_DISCONNECT_B3_IND(m) ((m)->Command==0x84 && (m)->Subcommand==0x82)
#define IS_DISCONNECT_IND(m) ((m)->Command==0x04 && (m)->Subcommand==0x82)
#define IS_FACILITY_IND(m) ((m)->Command==0x80 && (m)->Subcommand==0x82)
#define IS_INFO_IND(m) ((m)->Command==0x08 && (m)->Subcommand==0x82)
#define IS_MANUFACTURER_IND(m) ((m)->Command==0xff && (m)->Subcommand==0x82)
#define IS_RESET_B3_IND(m) ((m)->Command==0x87 && (m)->Subcommand==0x82)
#define IS_ALERT_CONF(m) ((m)->Command==0x01 && (m)->Subcommand==0x81)
#define IS_CONNECT_CONF(m) ((m)->Command==0x02 && (m)->Subcommand==0x81)
#define IS_CONNECT_B3_CONF(m) ((m)->Command==0x82 && (m)->Subcommand==0x81)
#define IS_DATA_B3_CONF(m) ((m)->Command==0x86 && (m)->Subcommand==0x81)
#define IS_DISCONNECT_B3_CONF(m) ((m)->Command==0x84 && (m)->Subcommand==0x81)
#define IS_DISCONNECT_CONF(m) ((m)->Command==0x04 && (m)->Subcommand==0x81)
#define IS_FACILITY_CONF(m) ((m)->Command==0x80 && (m)->Subcommand==0x81)
#define IS_INFO_CONF(m) ((m)->Command==0x08 && (m)->Subcommand==0x81)
#define IS_LISTEN_CONF(m) ((m)->Command==0x05 && (m)->Subcommand==0x81)
#define IS_MANUFACTURER_CONF(m) ((m)->Command==0xff && (m)->Subcommand==0x81)
#define IS_RESET_B3_CONF(m) ((m)->Command==0x87 && (m)->Subcommand==0x81)
#define IS_SELECT_B_PROTOCOL_CONF(m) ((m)->Command==0x41 && (m)->Subcommand==0x81)

/*----- header functions that fill the _cmsg structure with default -----*/
/*----- values. Only nonstandard parameter need to be changed -----*/

#define ALERT_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x01,0x80,Messagenumber,adr)
#define CONNECT_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x02,0x80,Messagenumber,adr)
#define CONNECT_B3_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x82,0x80,Messagenumber,adr)
#define DATA_B3_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x86,0x80,Messagenumber,adr)
#define DISCONNECT_B3_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x84,0x80,Messagenumber,adr)
#define DISCONNECT_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x04,0x80,Messagenumber,adr)
#define FACILITY_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x80,0x80,Messagenumber,adr)
#define INFO_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x08,0x80,Messagenumber,adr)
#define LISTEN_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x05,0x80,Messagenumber,adr)
#define MANUFACTURER_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0xff,0x80,Messagenumber,adr)
#define RESET_B3_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x87,0x80,Messagenumber,adr)
#define SELECT_B_PROTOCOL_REQ_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x41,0x80,Messagenumber,adr)
#define CONNECT_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x02,0x83,Messagenumber,adr)
#define CONNECT_ACTIVE_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x03,0x83,Messagenumber,adr)
#define CONNECT_B3_ACTIVE_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x83,0x83,Messagenumber,adr)
#define CONNECT_B3_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x82,0x83,Messagenumber,adr)
#define CONNECT_B3_T90_ACTIVE_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x88,0x83,Messagenumber,adr)
#define DATA_B3_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x86,0x83,Messagenumber,adr)
#define DISCONNECT_B3_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x84,0x83,Messagenumber,adr)
#define DISCONNECT_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x04,0x83,Messagenumber,adr)
#define FACILITY_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x80,0x83,Messagenumber,adr)
#define INFO_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x08,0x83,Messagenumber,adr)
#define MANUFACTURER_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0xff,0x83,Messagenumber,adr)
#define RESET_B3_RESP_HEADER(cmsg,ApplId,Messagenumber,adr) \
		      CAPI_CMSG_HEADER(cmsg,ApplId,0x87,0x83,Messagenumber,adr)

/*
 * CAPI-functions that correspond to the CAPI messages specified in the
 * CAPI 2.0 specification. All possible parameter have to be specified.
 * The CAPI message is sent immediately when calling these functions,
 * return values are the same as in capi_put_message()
 */

unsigned ALERT_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cstruct BChannelinformation
		,_cstruct Keypadfacility
		,_cstruct Useruserdata
		,_cstruct Facilitydataarra
#ifndef CAPI_LIBRARY_V2
		,_cstruct SendingComplete
#endif
		);
unsigned CONNECT_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cword CIPValue
		,_cstruct CalledPartyNumber
		,_cstruct CallingPartyNumber
		,_cstruct CalledPartySubaddress
		,_cstruct CallingPartySubaddress
		,_cword B1protocol
		,_cword B2protocol
		,_cword B3protocol
		,_cstruct B1configuration
		,_cstruct B2configuration
		,_cstruct B3configuration
#ifndef CAPI_LIBRARY_V2
		,_cstruct Globalconfiguration
#endif
		,_cstruct BC
		,_cstruct LLC
		,_cstruct HLC
		,_cstruct BChannelinformation
		,_cstruct Keypadfacility
		,_cstruct Useruserdata
		,_cstruct Facilitydataarray
#ifndef CAPI_LIBRARY_V2
		,_cstruct SendingComplete
#endif
		);
unsigned CONNECT_B3_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cstruct NCPI);
unsigned DATA_B3_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,void * Data
		,_cword DataLength
		,_cword DataHandle
		,_cword Flags);
unsigned DISCONNECT_B3_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cstruct NCPI);
unsigned DISCONNECT_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cstruct BChannelinformation
		,_cstruct Keypadfacility
		,_cstruct Useruserdata
		,_cstruct Facilitydataarray);
unsigned FACILITY_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cword FacilitySelector
		,_cstruct FacilityRequestParameter);
unsigned INFO_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cstruct CalledPartyNumber
		,_cstruct BChannelinformation
		,_cstruct Keypadfacility
		,_cstruct Useruserdata
		,_cstruct Facilitydataarray
#ifndef CAPI_LIBRARY_V2
		,_cstruct SendingComplete
#endif
		);
unsigned LISTEN_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cdword InfoMask
		,_cdword CIPmask
		,_cdword CIPmask2
		,_cstruct CallingPartyNumber
		,_cstruct CallingPartySubaddress);
unsigned MANUFACTURER_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cdword ManuID
		,_cdword Class
		,_cdword Function
		,_cstruct ManuData);
unsigned RESET_B3_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cstruct NCPI);
unsigned SELECT_B_PROTOCOL_REQ (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cword B1protocol
		,_cword B2protocol
		,_cword B3protocol
		,_cstruct B1configuration
		,_cstruct B2configuration
		,_cstruct B3configuration
#ifndef CAPI_LIBRARY_V2
		,_cstruct Globalconfiguration
#endif
		);
unsigned CONNECT_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cword Reject
		,_cword B1protocol
		,_cword B2protocol
		,_cword B3protocol
		,_cstruct B1configuration
		,_cstruct B2configuration
		,_cstruct B3configuration
#ifndef CAPI_LIBRARY_V2
		,_cstruct Globalconfiguration
#endif
		,_cstruct ConnectedNumber
		,_cstruct ConnectedSubaddress
		,_cstruct LLC
		,_cstruct BChannelinformation
		,_cstruct Keypadfacility
		,_cstruct Useruserdata
		,_cstruct Facilitydataarray);
unsigned CONNECT_ACTIVE_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr);
unsigned CONNECT_B3_ACTIVE_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr);
unsigned CONNECT_B3_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cword Reject
		,_cstruct NCPI);
unsigned CONNECT_B3_T90_ACTIVE_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr);
unsigned DATA_B3_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cword DataHandle);
unsigned DISCONNECT_B3_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr);
unsigned DISCONNECT_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr);
unsigned FACILITY_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cword FacilitySelector
		,_cstruct FacilityResponseParameters);
unsigned INFO_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr);
unsigned MANUFACTURER_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr
		,_cdword ManuID
		,_cdword Class
		,_cdword Function
		,_cstruct ManuData);
unsigned RESET_B3_RESP (_cmsg *cmsg, _cword ApplId, _cword Messagenumber
		,_cdword adr);

/*-----------------------------------------------------------------------*/

static inline void capi_fill_CONNECT_B3_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					    _cdword adr,
					    _cstruct NCPI)
{
	capi_cmsg_header(cmsg, ApplId, 0x82, 0x80, Messagenumber, adr);
	cmsg->NCPI = NCPI;
}

static inline void capi_fill_FACILITY_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					  _cdword adr,
					  _cword FacilitySelector,
				       _cstruct FacilityRequestParameter)
{
	capi_cmsg_header(cmsg, ApplId, 0x80, 0x80, Messagenumber, adr);
	cmsg->FacilitySelector = FacilitySelector;
	cmsg->FacilityRequestParameter = FacilityRequestParameter;
}

static inline void capi_fill_INFO_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
				      _cdword adr,
				      _cstruct CalledPartyNumber,
				      _cstruct BChannelinformation,
				      _cstruct Keypadfacility,
				      _cstruct Useruserdata,
				      _cstruct Facilitydataarray
#ifndef CAPI_LIBRARY_V2
				     ,_cstruct SendingComplete
#endif
				      )
{
	capi_cmsg_header(cmsg, ApplId, 0x08, 0x80, Messagenumber, adr);
	cmsg->CalledPartyNumber = CalledPartyNumber;
	cmsg->BChannelinformation = BChannelinformation;
	cmsg->Keypadfacility = Keypadfacility;
	cmsg->Useruserdata = Useruserdata;
	cmsg->Facilitydataarray = Facilitydataarray;
#ifndef CAPI_LIBRARY_V2
	cmsg->SendingComplete = SendingComplete;
#endif
}

static inline void capi_fill_LISTEN_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					_cdword adr,
					_cdword InfoMask,
					_cdword CIPmask,
					_cdword CIPmask2,
					_cstruct CallingPartyNumber,
					_cstruct CallingPartySubaddress)
{
	capi_cmsg_header(cmsg, ApplId, 0x05, 0x80, Messagenumber, adr);
	cmsg->InfoMask = InfoMask;
	cmsg->CIPmask = CIPmask;
	cmsg->CIPmask2 = CIPmask2;
	cmsg->CallingPartyNumber = CallingPartyNumber;
	cmsg->CallingPartySubaddress = CallingPartySubaddress;
}

static inline void capi_fill_ALERT_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
				       _cdword adr,
				       _cstruct BChannelinformation,
				       _cstruct Keypadfacility,
				       _cstruct Useruserdata,
				       _cstruct Facilitydataarray
#ifndef CAPI_LIBRARY_V2
				       ,_cstruct SendingComplete
#endif
				       )
{
	capi_cmsg_header(cmsg, ApplId, 0x01, 0x80, Messagenumber, adr);
	cmsg->BChannelinformation = BChannelinformation;
	cmsg->Keypadfacility = Keypadfacility;
	cmsg->Useruserdata = Useruserdata;
	cmsg->Facilitydataarray = Facilitydataarray;
#ifndef CAPI_LIBRARY_V2
	cmsg->SendingComplete = SendingComplete;
#endif
}

static inline void capi_fill_CONNECT_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					 _cdword adr,
					 _cword CIPValue,
					 _cstruct CalledPartyNumber,
					 _cstruct CallingPartyNumber,
					 _cstruct CalledPartySubaddress,
					 _cstruct CallingPartySubaddress,
					 _cword B1protocol,
					 _cword B2protocol,
					 _cword B3protocol,
					 _cstruct B1configuration,
					 _cstruct B2configuration,
					 _cstruct B3configuration,
#ifndef CAPI_LIBRARY_V2
					 _cstruct Globalconfiguration,
#endif
					 _cstruct BC,
					 _cstruct LLC,
					 _cstruct HLC,
					 _cstruct BChannelinformation,
					 _cstruct Keypadfacility,
					 _cstruct Useruserdata,
					 _cstruct Facilitydataarray
#ifndef CAPI_LIBRARY_V2
					 ,_cstruct SendingComplete
#endif
					 )
{

	capi_cmsg_header(cmsg, ApplId, 0x02, 0x80, Messagenumber, adr);
	cmsg->CIPValue = CIPValue;
	cmsg->CalledPartyNumber = CalledPartyNumber;
	cmsg->CallingPartyNumber = CallingPartyNumber;
	cmsg->CalledPartySubaddress = CalledPartySubaddress;
	cmsg->CallingPartySubaddress = CallingPartySubaddress;
	cmsg->B1protocol = B1protocol;
	cmsg->B2protocol = B2protocol;
	cmsg->B3protocol = B3protocol;
	cmsg->B1configuration = B1configuration;
	cmsg->B2configuration = B2configuration;
	cmsg->B3configuration = B3configuration;
#ifndef CAPI_LIBRARY_V2
	cmsg->Globalconfiguration = Globalconfiguration;
#endif
	cmsg->BC = BC;
	cmsg->LLC = LLC;
	cmsg->HLC = HLC;
	cmsg->BChannelinformation = BChannelinformation;
	cmsg->Keypadfacility = Keypadfacility;
	cmsg->Useruserdata = Useruserdata;
	cmsg->Facilitydataarray = Facilitydataarray;
#ifndef CAPI_LIBRARY_V2
	cmsg->SendingComplete = SendingComplete;
#endif
}

static inline void capi_fill_DATA_B3_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					 _cdword adr,
					 unsigned char *Data,
					 _cword DataLength,
					 _cword DataHandle,
					 _cword Flags)
{

	capi_cmsg_header(cmsg, ApplId, 0x86, 0x80, Messagenumber, adr);
	cmsg->Data = Data;
	cmsg->DataLength = DataLength;
	cmsg->DataHandle = DataHandle;
	cmsg->Flags = Flags;
}

static inline void capi_fill_DISCONNECT_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					    _cdword adr,
					    _cstruct BChannelinformation,
					    _cstruct Keypadfacility,
					    _cstruct Useruserdata,
					    _cstruct Facilitydataarray)
{

	capi_cmsg_header(cmsg, ApplId, 0x04, 0x80, Messagenumber, adr);
	cmsg->BChannelinformation = BChannelinformation;
	cmsg->Keypadfacility = Keypadfacility;
	cmsg->Useruserdata = Useruserdata;
	cmsg->Facilitydataarray = Facilitydataarray;
#ifndef CAPI_LIBRARY_V2
	cmsg->SendingComplete = NULL;
#endif
}

static inline void capi_fill_DISCONNECT_B3_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					       _cdword adr,
					       _cstruct NCPI)
{

	capi_cmsg_header(cmsg, ApplId, 0x84, 0x80, Messagenumber, adr);
	cmsg->NCPI = NCPI;
}

static inline void capi_fill_MANUFACTURER_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					      _cdword adr,
					      _cdword ManuID,
					      _cdword Class,
					      _cdword Function,
					      _cstruct ManuData)
{

	capi_cmsg_header(cmsg, ApplId, 0xff, 0x80, Messagenumber, adr);
	cmsg->ManuID = ManuID;
	cmsg->Class = Class;
	cmsg->Function = Function;
	cmsg->ManuData = ManuData;
}

static inline void capi_fill_RESET_B3_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					  _cdword adr,
					  _cstruct NCPI)
{

	capi_cmsg_header(cmsg, ApplId, 0x87, 0x80, Messagenumber, adr);
	cmsg->NCPI = NCPI;
}

static inline void capi_fill_SELECT_B_PROTOCOL_REQ(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
						   _cdword adr,
						   _cword B1protocol,
						   _cword B2protocol,
						   _cword B3protocol,
						_cstruct B1configuration,
						_cstruct B2configuration,
						_cstruct B3configuration
#ifndef CAPI_LIBRARY_V2
						,_cstruct Globalconfiguration
#endif
						)
{

	capi_cmsg_header(cmsg, ApplId, 0x41, 0x80, Messagenumber, adr);
	cmsg->B1protocol = B1protocol;
	cmsg->B2protocol = B2protocol;
	cmsg->B3protocol = B3protocol;
	cmsg->B1configuration = B1configuration;
	cmsg->B2configuration = B2configuration;
	cmsg->B3configuration = B3configuration;
#ifndef CAPI_LIBRARY_V2
	cmsg->Globalconfiguration = Globalconfiguration;
#endif
}

static inline void capi_fill_CONNECT_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					  _cdword adr,
					  _cword Reject,
					  _cword B1protocol,
					  _cword B2protocol,
					  _cword B3protocol,
					  _cstruct B1configuration,
					  _cstruct B2configuration,
					  _cstruct B3configuration,
#ifndef CAPI_LIBRARY_V2
					  _cstruct Globalconfiguration,
#endif
					  _cstruct ConnectedNumber,
					  _cstruct ConnectedSubaddress,
					  _cstruct LLC,
					  _cstruct BChannelinformation,
					  _cstruct Keypadfacility,
					  _cstruct Useruserdata,
					  _cstruct Facilitydataarray)
{
	capi_cmsg_header(cmsg, ApplId, 0x02, 0x83, Messagenumber, adr);
	cmsg->Reject = Reject;
	cmsg->B1protocol = B1protocol;
	cmsg->B2protocol = B2protocol;
	cmsg->B3protocol = B3protocol;
	cmsg->B1configuration = B1configuration;
	cmsg->B2configuration = B2configuration;
	cmsg->B3configuration = B3configuration;
#ifndef CAPI_LIBRARY_V2
	cmsg->Globalconfiguration = Globalconfiguration;
#endif
	cmsg->ConnectedNumber = ConnectedNumber;
	cmsg->ConnectedSubaddress = ConnectedSubaddress;
	cmsg->LLC = LLC;
	cmsg->BChannelinformation = BChannelinformation;
	cmsg->Keypadfacility = Keypadfacility;
	cmsg->Useruserdata = Useruserdata;
	cmsg->Facilitydataarray = Facilitydataarray;
}

static inline void capi_fill_CONNECT_ACTIVE_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
						 _cdword adr)
{

	capi_cmsg_header(cmsg, ApplId, 0x03, 0x83, Messagenumber, adr);
}

static inline void capi_fill_CONNECT_B3_ACTIVE_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
						    _cdword adr)
{

	capi_cmsg_header(cmsg, ApplId, 0x83, 0x83, Messagenumber, adr);
}

static inline void capi_fill_CONNECT_B3_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					     _cdword adr,
					     _cword Reject,
					     _cstruct NCPI)
{
	capi_cmsg_header(cmsg, ApplId, 0x82, 0x83, Messagenumber, adr);
	cmsg->Reject = Reject;
	cmsg->NCPI = NCPI;
}

static inline void capi_fill_CONNECT_B3_T90_ACTIVE_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
							_cdword adr)
{

	capi_cmsg_header(cmsg, ApplId, 0x88, 0x83, Messagenumber, adr);
}

static inline void capi_fill_DATA_B3_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					  _cdword adr,
					  _cword DataHandle)
{

	capi_cmsg_header(cmsg, ApplId, 0x86, 0x83, Messagenumber, adr);
	cmsg->DataHandle = DataHandle;
}

static inline void capi_fill_DISCONNECT_B3_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
						_cdword adr)
{

	capi_cmsg_header(cmsg, ApplId, 0x84, 0x83, Messagenumber, adr);
}

static inline void capi_fill_DISCONNECT_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					     _cdword adr)
{

	capi_cmsg_header(cmsg, ApplId, 0x04, 0x83, Messagenumber, adr);
}

static inline void capi_fill_FACILITY_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					   _cdword adr,
					   _cword FacilitySelector)
{

	capi_cmsg_header(cmsg, ApplId, 0x80, 0x83, Messagenumber, adr);
	cmsg->FacilitySelector = FacilitySelector;
}

static inline void capi_fill_INFO_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
				       _cdword adr)
{

	capi_cmsg_header(cmsg, ApplId, 0x08, 0x83, Messagenumber, adr);
}

static inline void capi_fill_MANUFACTURER_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					       _cdword adr,
					       _cdword ManuID,
					       _cdword Class,
					       _cdword Function,
					       _cstruct ManuData)
{

	capi_cmsg_header(cmsg, ApplId, 0xff, 0x83, Messagenumber, adr);
	cmsg->ManuID = ManuID;
	cmsg->Class = Class;
	cmsg->Function = Function;
	cmsg->ManuData = ManuData;
}

static inline void capi_fill_RESET_B3_RESP(_cmsg * cmsg, _cword ApplId, _cword Messagenumber,
					   _cdword adr)
{

	capi_cmsg_header(cmsg, ApplId, 0x87, 0x83, Messagenumber, adr);
}

#ifdef __cplusplus
}
#endif

#endif /* __CAPIUTILS_H__ */
