#include <jni.h>
#include "de_powerisdnmonitor_capi_PIMCapi.h"
#include "capi20.h"

#define ERR_METHOD_NOT_AVAILABLE (jint)-1
#define ERR_NO_APPID (jint)-2
#define ERR_NOT_SUPPORTED (jint)-3
#define ERR_SERIAL_NUMBER (jint)-100 // ToDo: Exceptionhandling
#define ERR_GET_VERSION (jint)-101 // ToDo: Exceptionhandling
#define ERR_GET_MANUFACTURER (jint)-102 // ToDo: Exceptionhandling
#define INFO "Linux native interface version 1.0"

static const char *getErrorMessage(jint rc)
{
	const char *err = "unknown";
	switch (rc)
	{
		case ERR_METHOD_NOT_AVAILABLE 	: err="JCC01 error: CAPI not loaded or method not available"; break;
		case ERR_NO_APPID 				: err="JCC02 error: Cannot write application ID to given array"; break;
		case ERR_NOT_SUPPORTED			: err="JCC03 error: Controller specific request not supported for this platform"; break;
	}

	return err;
}

static void setError(JNIEnv *env, jintArray p_rc, jint error) {
	jint* jap = env->GetIntArrayElements(p_rc, 0);
	if (env->GetArrayLength(p_rc) > 0)
	{
		jap[0] = error;
	}
	env->ReleaseIntArrayElements(p_rc, jap, 0);	/* under control of JVM */
}

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetImplementationInfo
  (JNIEnv *env, jclass) {

	return env->NewStringUTF(INFO);
}

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nInstalled
  (JNIEnv *, jclass) {

	return capi20_isinstalled();
}

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetManufacturer
  (JNIEnv *env, jclass, jint contr, jintArray p_rc) {

	char buf[128];
	if (capi20_get_manufacturer(contr, (unsigned char *)buf))
	{
		setError(env, p_rc, 0);
	}
	else
	{
		setError(env, p_rc, ERR_GET_MANUFACTURER);
		buf[0] = 0; // empty string in case of error
	}
	return env->NewStringUTF(buf);
}  

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetSerialNumber
  (JNIEnv *env, jclass, jint contr, jintArray p_rc) {

	char sn[8];

	if (capi20_get_serial_number(contr, (unsigned char *)&sn)) 
	{
//		printf("DEBUG: serialnumber <%s>\n", sn);
		setError(env, p_rc, ERR_SERIAL_NUMBER);
	}
	else
	{
		setError(env, p_rc, 0);
		sn[0] = 0;		/* empty string */
	}
	return env->NewStringUTF(sn);
} 

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetVersion
  (JNIEnv *env, jclass, jint contr, jintArray ja) {

	unsigned int ca[4];
	jint *jap;
	int i,l=4;
	jsize jal = env->GetArrayLength(ja);
	if (jal<l) l=jal;

	if (capi20_get_version(contr, (unsigned char *)&ca))
	{
		jap = env->GetIntArrayElements(ja, 0);
		for (i=0; i<l; i++)
			jap[i] = ca[i];
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

	if (env->GetArrayLength(ja) < 1)
	{
		return ERR_NO_APPID;
	}
	rc = capi20_register(maxcon,maxblocks,maxlen,&appid);
	jap = env->GetIntArrayElements(ja, 0);
	*jap = appid;
	env->ReleaseIntArrayElements(ja, jap, 0);	/* under control of JVM */
	return rc;
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nRelease
  (JNIEnv *, jclass, jint appid) {

	return capi20_release(appid);
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetProfile
  (JNIEnv *env, jclass, jint contr, jbyteArray ja) {

	char buf[64];
	jbyte *jap;
	jint rc;
	int i,l=64;
	jsize jal = env->GetArrayLength(ja);
	if (jal<l) l=jal;

	rc = capi20_get_profile(contr,(unsigned char*)buf);
	if (rc == 0)
	{
		jap = env->GetByteArrayElements(ja, 0);
		for (i=0; i<l; i++)
			jap[i] = buf[i];
		env->ReleaseByteArrayElements(ja, jap, 0);	/* under control of JVM */
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nPutMessage
  (JNIEnv *env, jclass, jint appid, jbyteArray ja) {

	jbyte *msg;
	jint rc;

	msg = env->GetByteArrayElements(ja, 0);
	rc = capi20_put_message(appid,(unsigned char *)msg);
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

	rc = capi20_get_message(appid,(unsigned char **)&msg);
	jap = env->GetIntArrayElements(p_rc, 0);
	if (env->GetArrayLength(p_rc) > 0)
		jap[0] = rc;
	env->ReleaseIntArrayElements(p_rc, jap, 0);	/* under control of JVM */
	if (rc==0)
	{
		size = *((unsigned short *)msg);	/* get size from "Data length" field */
		jb = env->NewByteArray(size);	/* allocate a new byte array to hold a copy of the message */
		jbp = env->GetByteArrayElements(jb, 0);
		for (i=0;i<size;i++)
			jbp[i] = msg[i];
		env->ReleaseByteArrayElements(jb, jbp, 0);	/* under control of JVM */
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
	return (jlong)jap;
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
	env->ReleaseByteArrayElements(ja, jap, 0);	/* under control of JVM */
	return ja;
}  

JNIEXPORT void JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nReleaseData
  (JNIEnv *env, jclass, jbyteArray ja, jint data) {

	env->ReleaseByteArrayElements(ja, (jbyte *)data, 0);
}  

JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nWaitForSignal
  (JNIEnv *, jclass, jint appid) {

	return capi20_waitformessage(appid, NULL); // ToDo: Timeout
}

JNIEXPORT void JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_init
  (JNIEnv *, jclass) {

}

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetErrorMessage
  (JNIEnv *env, jclass, jint rc) {
  
	return env->NewStringUTF(getErrorMessage(rc));
}  
