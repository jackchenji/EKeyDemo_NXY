#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/hmac.h>
#include <openssl/sm2.h>
#include <openssl/x509.h>
#include <openssl/crypto.h>
#include "GmSSL.h"
#include "jni.h"
#include "froadLog.h"
#include "GenP10.h"
#include "tmPrivMethod.h"


//JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
//{
//
//    LOGE("JNI_OnLoad");
//    return JNI_VERSION_1_2;
//}
//
//JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
//{
//    LOGE("JNI_OnUnload");
//
//}

ULONG backHashLen = 64 ;
char backHash[128] = {0x00};
JNIEXPORT jbyteArray JNICALL Java_com_froad_ukey_jni_GmSSL_digest(JNIEnv* env, jobject obj, jstring algor, jbyteArray in)
{
    jbyteArray ret = NULL;
    const char *alg = NULL;
    const unsigned char *inbuf = NULL;
    unsigned char outbuf[EVP_MAX_MD_SIZE];
    int inlen;
    unsigned int outlen = sizeof(outbuf);
    const EVP_MD *md;

    OpenSSL_add_all_digests();

    if (!(alg = env->GetStringUTFChars( algor, 0))) {
        LOGE("GetStringUTFChars %s: ",algor);
        goto end;
    }

    LOGD("Java_gmssl_GmSSL_digest alg : %s",alg);

    if (!(inbuf = (unsigned char *)env->GetByteArrayElements( in, 0))) {
        LOGE("GetStringUTFChars %s: ",in);
        goto end;
    }
    if ((inlen = (size_t) env->GetArrayLength( in)) <= 0) {
        LOGE("GetStringUTFChars %d: ",inlen);
        goto end;
    }

    if (!(md = EVP_get_digestbyname(alg))) {
        LOGE("EVP_get_digestbyname");
        goto end;
    }
    if (!EVP_Digest(inbuf, inlen, outbuf, &outlen, md, NULL)) {
        LOGE("EVP_Digest");
        goto end;
    }

    if (!(ret = env->NewByteArray( outlen))) {
        LOGE("NewByteArray : %d",outlen);
        goto end;
    }

    env->SetByteArrayRegion( ret, 0, outlen, (jbyte *)outbuf);

end:
    if (alg) env->ReleaseStringUTFChars(algor, alg);
    if (inbuf) env->ReleaseByteArrayElements( in, (jbyte *)inbuf, JNI_ABORT);
    return ret;
}

JNIEXPORT jlong JNICALL
Java_com_froad_ukey_jni_GmSSL_genRSAHash(JNIEnv *env, jobject instance, jint iAlgType, jboolean jisInit, jstring LeField_,
                            jbyteArray bPublicKey_, jint usPubKeyLen) {
//    UCHAR * bPublicKey = get_class_jchararray_bts(env, bPublicKey_, usPubKeyLen);
//    UCHAR * bPublicKey = reinterpret_cast<UCHAR *>(ConvertJByteaArrayToChars (env, bPublicKey_));
    UCHAR * bPublicKey = reinterpret_cast<UCHAR *>(env->GetByteArrayElements(bPublicKey_, NULL));
    LOGD("Java_gmssl_GmSSL_genRSAHash bPublicKey : %s",bPublicKey);

    const char * LeField =env->GetStringUTFChars( LeField_, 0);
    // TODO
    BOOL isInit = JNI_TRUE;
    if (! jisInit) {
        isInit = JNI_FALSE;
    }
    LONG  aLong = genRSAHash(env, instance, iAlgType, isInit, (unsigned char *) LeField, bPublicKey, usPubKeyLen, backHash,&backHashLen);

    LOGD("Java_gmssl_GmSSL_genRSAHash aLong : %d",aLong);

    LOGD("Java_gmssl_GmSSL_genRSAHash backHash : %s",backHash);
    LOGD("Java_gmssl_GmSSL_genRSAHash backHashLen : %d",backHashLen);
    set_class_object_jbyteArray_value(env, instance, "RSAbackHash",
                                      reinterpret_cast<char *>(backHash), backHashLen);
    env->ReleaseByteArrayElements(bPublicKey_, reinterpret_cast<jbyte *>(bPublicKey), 0);
    return aLong;

}

JNIEXPORT jlong JNICALL
Java_com_froad_ukey_jni_GmSSL_genRSAP10(JNIEnv *env, jobject instance,
                           jbyteArray bSignBuffer_, jint usSignLen) {
    BYTE * bSignBuffer = reinterpret_cast<BYTE *>(env->GetByteArrayElements(bSignBuffer_, NULL));
    // TODO
    BYTE RSAPkcs10 [2048] ={0};
    USHORT RSAPkcs10Len  = 2048 ;
    LONG  aLong =  genRSAP10(reinterpret_cast<char *>(RSAPkcs10), &RSAPkcs10Len, bSignBuffer, usSignLen) ;
    LOGD("Java_gmssl_GmSSL_genRSAP10 aLong : %d",aLong);
    LOGD("Java_gmssl_GmSSL_genRSAP10 RSAPkcs10 : %s",RSAPkcs10);
    LOGD("Java_gmssl_GmSSL_genRSAP10 RSAPkcs10Len : %d",RSAPkcs10Len);
    set_class_object_jbyteArray_value(env, instance, "RSAPkcs10",
                                      reinterpret_cast<char *>(RSAPkcs10), RSAPkcs10Len);
    env->ReleaseByteArrayElements(bSignBuffer_, reinterpret_cast<jbyte *>(bSignBuffer), 0);
    return aLong;
}

JNIEXPORT jlong JNICALL
Java_com_froad_ukey_jni_GmSSL_genSM2Hash(JNIEnv *env, jobject instance, jboolean jIsInit, jstring LeField_,
                            jbyteArray bPublicKey_, jint usPubKeyLen) {
    UCHAR * bPublicKey = reinterpret_cast<UCHAR *>(env->GetByteArrayElements(bPublicKey_, NULL));

    const char * LeField =env->GetStringUTFChars( LeField_, 0);
    // TODO
    BOOL isInit = JNI_TRUE;
    if (! jIsInit) {
        isInit = JNI_FALSE;
    }
    LONG  aLong = genSM2Hash(env, instance, isInit, (unsigned char *) LeField, bPublicKey, usPubKeyLen, backHash,&backHashLen);
    LOGD("Java_gmssl_GmSSL_genSM2Hash backHash : %s",backHash);
    LOGD("Java_gmssl_GmSSL_genSM2Hash backHashLen : %d",backHashLen);
    set_class_object_jbyteArray_value(env, instance, "SM2backHash",
                                      reinterpret_cast<char *>(backHash), backHashLen);

    env->ReleaseByteArrayElements(bPublicKey_, reinterpret_cast<jbyte *>(bPublicKey), 0);

    return aLong;
}

JNIEXPORT jlong JNICALL
Java_com_froad_ukey_jni_GmSSL_genSM2P10(JNIEnv *env, jobject instance,
                           jbyteArray bSignBuffer_, jint usSignLen) {
    BYTE * bSignBuffer = reinterpret_cast<BYTE *>(env->GetByteArrayElements(bSignBuffer_, NULL));

    // TODO

    BYTE SM2Pkcs10 [2048] ={0};
    USHORT SM2Pkcs10Len =2048;
    // TODO
    LONG  aLong = genSM2P10(env, reinterpret_cast<char *>(SM2Pkcs10), &SM2Pkcs10Len, bSignBuffer, usSignLen) ;
    LOGD("Java_gmssl_GmSSL_genSM2P10 aLong : %d",aLong);
    LOGD("Java_gmssl_GmSSL_genSM2P10 SM2Pkcs10 : %s",SM2Pkcs10);
    LOGD("Java_gmssl_GmSSL_genSM2P10 SM2Pkcs10Len : %d",SM2Pkcs10Len);
    set_class_object_jbyteArray_value(env, instance, "SM2Pkcs10",
                                      reinterpret_cast<char *>(SM2Pkcs10), SM2Pkcs10Len);
    env->ReleaseByteArrayElements(bSignBuffer_, reinterpret_cast<jbyte *>(bSignBuffer), 0);
    return aLong;
}

JNIEXPORT jlong JNICALL
Java_com_froad_ukey_jni_GmSSL_convertPkcs7ToPemHex(JNIEnv *env, jobject instance,
                           jbyteArray bCerBuffer_, jint usCerLen) {
    UCHAR * bCerBuffer = reinterpret_cast<UCHAR *>(env->GetByteArrayElements(bCerBuffer_, NULL));

    // TODO

    BYTE pemHex [2048] ={0};
    ULONG pemHexLen =2048;
    // TODO
    bool aLong = convertPkcs7ToPemHex(env, bCerBuffer, usCerLen, reinterpret_cast<unsigned char *>(pemHex), &pemHexLen) ;
    LOGD("Java_com_froad_ukey_jni_GmSSL_convertPkcs7ToPemHex aLong : %d",aLong);
    LOGD("Java_com_froad_ukey_jni_GmSSL_convertPkcs7ToPemHex SM2Pkcs10 : %s",pemHex);
    LOGD("Java_com_froad_ukey_jni_GmSSL_convertPkcs7ToPemHex SM2Pkcs10Len : %d",pemHexLen);
    set_class_object_jbyteArray_value(env, instance, "pemBytes",
                                      reinterpret_cast<char *>(pemHex), pemHexLen);
    env->ReleaseByteArrayElements(bCerBuffer_, reinterpret_cast<jbyte *>(bCerBuffer), 0);
    return aLong;
}