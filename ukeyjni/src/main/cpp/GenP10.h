//
// Created by Macbook Pro on 2019/4/8.
//

#ifndef EKEYDEMO_AHRCU_NXYPRESET_H
#define EKEYDEMO_AHRCU_NXYPRESET_H

#endif //EKEYDEMO_AHRCU_NXYPRESET_H

#include <openssl/skf.h>
/*
 *基本数据类型
 */
typedef signed char         INT8;
typedef signed short        INT16;
typedef signed int          INT32;
typedef unsigned char       UINT8;
typedef unsigned short      UINT16;
typedef unsigned int        UINT32;
typedef unsigned long       UINT64;
typedef unsigned char       UCHAR;

typedef UINT8               BYTE;
typedef INT16               SHORT;
typedef UINT16              USHORT;
typedef UINT32              UINT;
typedef UINT16              WORD;


#define MAX_RSA_MODULUS_LEN 256        //RSA算法模数的最大长度
#define MAX_RSA_EXPONENT_LEN 4        //RSA算法指数的最大长度

#define MAKESHORT(X) ((UINT16)(X)[0]<<8|(X)[1])

BOOL genRSAHash(JNIEnv *env, jobject gmSSL, int iAlgType, BOOL isInit, unsigned char* LeField,UCHAR* bPublicKey,USHORT usPubKeyLen,char * backHash,ULONG * backHashLen);
BOOL genRSAP10(char* RSAPkcs10, USHORT* RSAPkcs10Len,	BYTE bSignBuffer[512],	USHORT usSignLen) ;
BOOL genSM2Hash(JNIEnv *env, jobject gmSSL, BOOL isInit, unsigned char* LeField,UCHAR* bPublicKey,	USHORT usPubKeyLen,char * backHash,ULONG * backHashLen);
BOOL genSM2P10(JNIEnv* env, char* SM2Pkcs10, USHORT* SM2Pkcs10Len,	BYTE bSignBuffer[512],	USHORT usSignLen) ;

bool convertPkcs7ToPemHex(JNIEnv* env, UCHAR *pkcs7_key, int certLen, UCHAR *outCert, ULONG *oCertLen);

X509_NAME *parse_name(char *subject, long chtype, int multirdn);