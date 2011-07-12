#include <jni.h>

#ifndef _Included_de_powerisdnmonitor_capi_PIMCapi
#define _Included_de_powerisdnmonitor_capi_PIMCapi
#ifdef __cplusplus
extern "C" {
#endif
/* Inaccessible static: listeners */
/* Inaccessible static: debug */
/* Inaccessible static: MSG_JCJ01 */
/* Inaccessible static: MSG_JCJ01B */
/* Inaccessible static: MSG_JCJ02 */
/* Inaccessible static: MSG_JCJ03 */
/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nInstalled
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nInstalled
  (JNIEnv *, jclass);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetManufacturer
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetManufacturer
  (JNIEnv *, jclass, jint);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetSerialNumber
 * Signature: ([I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetSerialNumber
  (JNIEnv *, jclass, jint, jintArray);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetVersion
 * Signature: ([I)I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetVersion
  (JNIEnv *, jclass, jint, jintArray);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nRegister
 * Signature: (IIII[I)I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nRegister
  (JNIEnv *, jclass, jint, jint, jint, jint, jintArray);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nRelease
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nRelease
  (JNIEnv *, jclass, jint);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetProfile
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetProfile
  (JNIEnv *, jclass, jint, jbyteArray);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nPutMessage
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nPutMessage
  (JNIEnv *, jclass, jint, jbyteArray);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetMessage
 * Signature: (I[I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetMessage
  (JNIEnv *, jclass, jint, jintArray);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetAddress
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetAddress
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetData
 * Signature: (II)[B
 */
JNIEXPORT jbyteArray JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetData
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nReleaseData
 * Signature: ([BI)V
 */
JNIEXPORT void JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nReleaseData
  (JNIEnv *, jclass, jbyteArray, jint);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nWaitForSignal
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nWaitForSignal
  (JNIEnv *, jclass, jint);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_init
  (JNIEnv *, jclass);

/*
 * Class:     de_powerisdnmonitor_capi_PIMCapi
 * Method:    nGetErrorMessage
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetErrorMessage
  (JNIEnv *, jclass, jint);

JNIEXPORT jstring JNICALL Java_de_powerisdnmonitor_capi_PIMCapi_nGetImplementationInfo
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif

