#include <jni.h>
#include "de_powerisdnmonitor_capi_PIMCapi.h"
#include "capi20.h"

#define ERR_METHOD_NOT_AVAILABLE (jint)-1
#define ERR_NO_APPID (jint)-2
#define ERR_NOT_SUPPORTED (jint)-3
#define ERR_SERIAL_NUMBER (jint)-100 // ToDo: Exceptionhandling
#define ERR_GET_VERSION (jint)-101 // ToDo: Exceptionhandling
#define INFO "Linux native interface version 1.0"

static char *getErrorMessage(jint rc)
{
	char *err = "unknown";
	switch (rc)
	{
		case ERR_METHOD_NOT_AVAILABLE 	: err="JCC01 error: CAPI not loaded or method not available"; break;
		case ERR_NO_APPID 				: err="JCC02 error: Cannot write application ID to given array"; break;
		case ERR_NOT_SUPPORTED			: err="JCC03 error: Controller specific request not supported for this platform"; break;
	}

	return err;
}

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetImplementationInfo
  (JNIEnv *env, jclass) {

	return env->NewStringUTF(INFO);
}

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nInstalled
  (JNIEnv *, jclass) {

	return (jint)capi20_isinstalled();
}

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetManufacturer
  (JNIEnv *env, jclass, jint contr) {

	char buf[128];
	capi20_get_manufacturer(contr, (unsigned char *)buf);
	return env->NewStringUTF(buf);
}  

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetSerialNumber
  (JNIEnv *env, jclass, jint contr, jintArray p_rc) {

	char sn[8];
	jint * jap;
	jint rc;

	rc = capi20_get_serial_number(contr, (unsigned char *)&sn);
//	printf("DEBUG: serialnumber <%s>\n", sn);
	jap = env->GetIntArrayElements(p_rc, 0);
	if (env->GetArrayLength(p_rc) > (jsize)0)
//		jap[0] = rc;
		if (rc)
			jap[0] = 0;
		else
			jap[0] = ERR_SERIAL_NUMBER;
	env->ReleaseIntArrayElements(p_rc, jap, 0);	/* under control of JVM */
//	if (rc!=0)
	if (!rc)
		sn[0] = (char)0;		/* empty string */
	return env->NewStringUTF(sn);
} 

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetVersion
  (JNIEnv *env, jclass, jint contr, jintArray ja) {

	unsigned int ca[4];
	jint rc, *jap;
	int i,l=4;
	jsize jal = env->GetArrayLength(ja);
	if (jal<(jsize)l) l=(int)jal;

	rc = (jint)capi20_get_version(contr, (unsigned char *)&ca);
//	if (rc == (jint)0)
	if (rc != (jint)0)
	{
		jap = env->GetIntArrayElements(ja, 0);
		for (i=0; i<l; i++)
			jap[i] = (jint)ca[i];
		env->ReleaseIntArrayElements(ja, jap, 0);	/* under control of JVM */
		return 0;
	}
	else
	{
		return ERR_GET_VERSION;
	}
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nRegister
  (JNIEnv *env, jclass, jint bufsize, jint maxcon, jint maxblocks, jint maxlen, jintArray ja) {

	jint rc, *jap;
	unsigned int appid;

	if (env->GetArrayLength(ja) < (jsize)1)
	{
		return ERR_NO_APPID;
	}
	rc = (jint)capi20_register((unsigned int)maxcon,(unsigned int)maxblocks,(unsigned int)maxlen,&appid);
	jap = env->GetIntArrayElements(ja, 0);
	*jap = (jint)appid;
	env->ReleaseIntArrayElements(ja, jap, 0);	/* under control of JVM */
	return rc;
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nRelease
  (JNIEnv *, jclass, jint appid) {

	return (jint)capi20_release((unsigned)appid);
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetProfile
  (JNIEnv *env, jclass, jint ctrnr, jbyteArray ja) {

	  char buf[64];
	  jbyte *jap;
	jint rc;
	int i,l=64;
	jsize jal = env->GetArrayLength(ja);
	if (jal<(jsize)l) l=(int)jal;

	rc = (jint)capi20_get_profile((unsigned)buf,(unsigned char*)ctrnr);
	if (rc == (jint)0)
	{
		jap = env->GetByteArrayElements(ja, 0);
		for (i=0; i<l; i++)
			jap[i] = buf[i];
		env->ReleaseByteArrayElements(ja, (jbyte *)jap, 0);	/* under control of JVM */
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nPutMessage
  (JNIEnv *env, jclass, jint appid, jbyteArray ja) {

	jbyte *msg;
	jint rc;

	msg = env->GetByteArrayElements(ja, 0);
	rc = (jint)capi20_put_message((unsigned int)appid,(unsigned char *)msg);
	env->ReleaseByteArrayElements(ja, msg, 0);
	// after call of CapiPutMessage the CAPI works with it's own copy of the message
	return rc;
}  

JNIEXPORT jbyteArray JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetMessage
  (JNIEnv *env, jclass, jint appid, jintArray p_rc) {

	char *msg;
	jbyte *jbp;
	jint rc, *jap;
	int i;
	unsigned short size;
	jbyteArray jb;

	rc = (jint)capi20_get_message((unsigned int)appid,(unsigned char **)&msg);
	jap = env->GetIntArrayElements(p_rc, 0);
	if (env->GetArrayLength(p_rc) > (jsize)0)
		jap[0] = rc;
	env->ReleaseIntArrayElements(p_rc, jap, 0);	/* under control of JVM */
	if (rc==(jint)0)
	{
		size = *((unsigned short *)msg);	/* get size from "Data length" field */
		jb = env->NewByteArray((jsize)size);	/* allocate a new byte array to hold a copy of the message */
		jbp = env->GetByteArrayElements(jb, 0);
		for (i=0;i<size;i++)
			jbp[i] = msg[i];
		env->ReleaseByteArrayElements(jb, (jbyte *)jbp, 0);	/* under control of JVM */
	}
	else
	{
		jb = env->NewByteArray(0);
	}
	return jb;
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetAddress
  (JNIEnv *env, jclass, jbyteArray ja) {

	jbyte *jap;
	jap = env->GetByteArrayElements(ja, 0);
	//(*env)->ReleaseByteArrayElements(env, ja, jap, 0);
	return (jint)jap;
}  

JNIEXPORT jbyteArray JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetData
  (JNIEnv *env, jclass, jint address, jint size) {

	  char *p;
	  jbyte *jap;
	int i;
	jbyteArray ja;

	ja = env->NewByteArray(size);
	jap = env->GetByteArrayElements(ja, 0);
	p = (char *)address;
	for (i=0;i<size;i++)
		jap[i] = p[i];
	env->ReleaseByteArrayElements(ja, (jbyte *)jap, 0);	/* under control of JVM */
	return ja;
}  

JNIEXPORT void JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nReleaseData
  (JNIEnv *env, jclass, jbyteArray ja, jint data) {

	env->ReleaseByteArrayElements(ja, (jbyte *)data, 0);
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nWaitForSignal
  (JNIEnv *, jclass, jint appid) {

	return (jint)capi20_waitformessage((unsigned int)appid, NULL); // ToDo: Timeout
}

JNIEXPORT void JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_init
  (JNIEnv *, jclass) {

}

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetErrorMessage
  (JNIEnv *env, jclass, jint rc) {
  
	return env->NewStringUTF(getErrorMessage(rc));
}  

int main(char** args) {

	return 0;
}
