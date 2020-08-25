// NXYPreSet.cpp : 定义 DLL 应用程序的导出函数。
//

//#include "stdafx.h"
#include <openssl/x509.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/evp.h>
#include <openssl/ec.h>
#include <openssl/ecdh.h>
#include <openssl/ecdsa.h>
//#include <afxtempl.h>
//#include "NXYPreSet.h"
//#include "LC_SM2CERTPREF83.h"
#include <openssl/bn.h>

#include "Utils.h"
#include "Base64Utils.h"
#include "GenP10.h"
#include "froadLog.h"
//#include "Log.h"


EVP_PKEY *pKey = NULL;
X509_REQ* req = NULL;
RSA *pRSA = NULL;

X509_NAME* name = NULL;

BYTE CN[64];
char backHash_ [64] = {0x00};
BYTE bReqInfo[2048];
/*
 * iAlgType 算法类型 1 sha1 ,2 sha256  3. md5
 * isInit true--第一次初始化创建证书, false--更新证书
 * LeField 如果第一次创建证书则为卡号，否则为旧证书的dn
 *
 */
BOOL genRSAHash(JNIEnv *env, jobject gmSSL, int iAlgType, BOOL isInit, unsigned char*  LeField, UCHAR* bPublicKey,USHORT usPubKeyLen,char * backHash,ULONG * backHashLen)
{
	UCHAR* pReqInfo = bReqInfo;
	req = NULL;
	name = NULL;
	pRSA = NULL;
	RSA *pRSA_EVP = NULL;
	pKey = NULL;

	LOGD("genRSAHash iAlgType : %d",iAlgType);
	LOGD("genRSAHash LeField : %s",LeField);
	LOGD("genRSAHash bPublicKey : %s",bPublicKey);
	LOGD("genRSAHash usPubKeyLen: %d",usPubKeyLen);

	LOGD("genRSAHash X509_REQ_new start ");
	req = X509_REQ_new();
	LOGD("genRSAHash X509_REQ_new end ");

    LOGD("genRSAHash X509_REQ_get_subject_name start ");
	name = X509_REQ_get_subject_name(req);
    LOGD("genRSAHash X509_REQ_get_subject_name end ");

    LOGD("genRSAHash X509_REQ_set_version start ");
    X509_REQ_set_version(req, 0);
    LOGD("genRSAHash X509_REQ_set_version end ");

    //重置为0
    memset(CN, 0x00, sizeof(CN));

//	LOGD("genRSAHash strlen start ");
//	int LeFieldLen = strlen(reinterpret_cast<const char *>(LeField));
//	LOGD("genRSAHash strlen end ");
//
//	LOGD("genRSAHash ByteArrayToHexString start ");
//	ByteArrayToHexString(LeField, LeFieldLen, (UCHAR*)szSN);
//	LOGD("genRSAHash szSN %s",szSN);
//	LOGD("genRSAHash ByteArrayToHexString end ");
	sprintf(reinterpret_cast<char *>(CN), "063@%s", LeField);
	LOGD("genRSAHash CN %s",CN);
	//生成请求DN
	if (isInit) {
        X509_NAME_add_entry_by_txt(name, "C", V_ASN1_UTF8STRING,
                                   reinterpret_cast<const unsigned char *>("CN"), -1, -1, 0);
        LOGD("X509_NAME_add_entry_by_txt C ");
        X509_NAME_add_entry_by_txt(name, "O", V_ASN1_UTF8STRING,
                                   reinterpret_cast<const unsigned char *>("ncc operation ca"), -1, -1, 0);
        LOGD("X509_NAME_add_entry_by_txt O ");
        X509_NAME_add_entry_by_txt(name, "OU", V_ASN1_UTF8STRING,
								   reinterpret_cast<const unsigned char *>("Customers"), -1, -1, 0);
        LOGD("X509_NAME_add_entry_by_txt OU ");
        X509_NAME_add_entry_by_txt(name, "CN", V_ASN1_UTF8STRING, CN, -1, -1, 0);
        LOGD("X509_NAME_add_entry_by_txt CN ");
	} else {
        name = parse_name((char *)LeField, V_ASN1_UTF8STRING, 0);
        X509_REQ_set_subject_name(req, name);
	}

	//产生RSA密钥及PKCS#10请求
    LOGD("genRSAHash RSA_new start ");
    pRSA = RSA_new();
    LOGD("genRSAHash RSA_new end ");

    LOGD("genRSAHash BN_asc2bn start ");
    BN_asc2bn(&pRSA->e, "65537");
    LOGD("genRSAHash BN_asc2bn end ");

    LOGD("genRSAHash BN_bin2bn start ");

    pRSA->n = BN_bin2bn(bPublicKey, usPubKeyLen, pRSA->n);

    LOGD("genRSAHash BN_bin2bn end ");

    LOGD("genRSAHash EVP_PKEY_new start ");
    pKey = EVP_PKEY_new();
    LOGD("genRSAHash EVP_PKEY_new end ");

    LOGD("genRSAHash RSAPublicKey_dup start ");
    pRSA_EVP = RSAPublicKey_dup(pRSA);
    LOGD("genRSAHash RSAPublicKey_dup end ");

    LOGD("genRSAHash EVP_PKEY_set1_RSA start ");
    EVP_PKEY_set1_RSA(pKey, pRSA_EVP);
    LOGD("genRSAHash EVP_PKEY_set1_RSA end ");

    LOGD("genRSAHash X509_REQ_set_pubkey start ");
    X509_REQ_set_pubkey(req, pKey);
    LOGD("genRSAHash X509_REQ_set_pubkey end ");

    req->req_info->attributes = NULL;
		//生成待HASH数据

    LOGD("genRSAHash i2d_X509_REQ_INFO start ");
    int i2dLen = i2d_X509_REQ_INFO(req->req_info, &pReqInfo);
    LOGD("genRSAHash i2d_X509_REQ_INFO end i2dLen-->%d", i2dLen);


//TODO : 做 hash 做签名
    switch (iAlgType){
			case 1:
                LOGD("genRSAHash X509_ALGOR_set0 start ");
                X509_ALGOR_set0(req->sig_alg, OBJ_nid2obj(NID_sha1WithRSAEncryption), V_ASN1_NULL, NULL);
                LOGD("genRSAHash X509_ALGOR_set0 end ");
                digest(env, "sha1",bReqInfo, i2dLen, backHashLen,(unsigned char*)backHash_);
                LOGD("genRSAHash backHashLen --> %d", *backHashLen);
                LOGD("genRSAHash backHash_ --> %s", backHash_);
                memcpy(backHash,backHash_,*backHashLen);
				break;
			case 2:
                LOGD("genRSAHash X509_ALGOR_set0 start ");
                X509_ALGOR_set0(req->sig_alg, OBJ_nid2obj(NID_sha256WithRSAEncryption), V_ASN1_NULL, NULL);
                LOGD("genRSAHash X509_ALGOR_set0 end ");
                digest(env, "sha256",bReqInfo, i2dLen, backHashLen,(unsigned char*)backHash_);
				LOGD("genRSAHash backHashLen --> %d", *backHashLen);
				LOGD("genRSAHash backHash_ --> %s", backHash_);
                memcpy(backHash,backHash_,*backHashLen);
                break;
			case 3:
                LOGD("genRSAHash X509_ALGOR_set0 start ");
                X509_ALGOR_set0(req->sig_alg, OBJ_nid2obj(NID_md5WithRSAEncryption), V_ASN1_NULL, NULL);
                LOGD("genRSAHash X509_ALGOR_set0 end ");
                digest(env, "md5",bReqInfo, i2dLen, backHashLen,(unsigned char*)backHash_);
				LOGD("genRSAHash backHashLen --> %d", *backHashLen);
				LOGD("genRSAHash backHash_ --> %s", backHash_);
                memcpy(backHash,backHash_,*backHashLen);
                break;
			default:
				LOGE("can not support iAlgType : %s ",iAlgType);
				return FALSE;
		}

	return TRUE;
}


BOOL genRSAP10(char* RSAPkcs10, USHORT* RSAPkcs10Len,	BYTE bSignBuffer[512],	USHORT usSignLen)
{
	UCHAR* P;
	BYTE TempBuffer[2048];
	USHORT usTempLen;
	if (RSAPkcs10Len != NULL)
	{
		*RSAPkcs10Len = 0;
	}
	LOGD("genRSAP10 usSignLen --> %d", usSignLen);
	ASN1_BIT_STRING_set(req->signature, bSignBuffer, usSignLen);

	P = TempBuffer;
	usTempLen = i2d_X509_REQ(req, &P);
	LOGD("genRSAP10 usTempLen --> %d", usTempLen);

	Base64Encode((char*)TempBuffer, usTempLen, RSAPkcs10, false);

	*RSAPkcs10Len = strlen(RSAPkcs10);
	LOGD("genRSAP10 RSAPkcs10Len --> %d", *RSAPkcs10Len);

	RSA_free(pRSA);

	EVP_PKEY_free(pKey);
	X509_REQ_free(req);

	return TRUE;
}


EC_KEY *pSM2_key = NULL;
EC_GROUP* pSM2_Group = NULL;
LONG lReqInfoLen;

/*
 * isInit true--第一次初始化创建证书, false--更新证书
 * LeField 如果第一次创建证书则为卡号，否则为旧证书的dn
 *
 */
BOOL genSM2Hash(JNIEnv *env, jobject gmSSL, BOOL isInit, unsigned char* LeField,UCHAR* bPublicKey,	USHORT usPubKeyLen,char * backHash,ULONG * backHashLen)
{
	BYTE TempBuffer[2048];

	UCHAR* P;
	req = NULL;
	name = NULL;
	pSM2_key = NULL;
	EC_KEY *pSM2_key_EVP = NULL;
	pKey = NULL;
	EC_POINT* pSM2PublicKey = NULL;
	pSM2_Group = NULL;


	//重置为0
	memset(bReqInfo, 0x00, sizeof(bReqInfo));

	req = X509_REQ_new();
	name = X509_REQ_get_subject_name(req);
	X509_REQ_set_version(req, 0);

	//重置为0
	memset(CN, 0x00, sizeof(CN));
	sprintf(reinterpret_cast<char *>(CN), "063@%s", LeField);

	//生成请求DN
//	X509_NAME_add_entry_by_txt(name, "C", MBSTRING_ASC, (const UCHAR*)"CN", -1, -1, 0);
//	X509_NAME_add_entry_by_txt(name, "O", MBSTRING_ASC, (const UCHAR*)"ncc operation ca", -1, -1, 0);
//	X509_NAME_add_entry_by_txt(name, "CN", MBSTRING_ASC, (const UCHAR*)CN, -1, -1, 0);
//	X509_NAME_add_entry_by_txt(name, "OU", MBSTRING_ASC, (const UCHAR*)"Customers", -1, -1, 0);

	if (isInit) {
        X509_NAME_add_entry_by_txt(name, "C", V_ASN1_UTF8STRING,
                                   reinterpret_cast<const unsigned char *>("CN"), -1, -1, 0);
        LOGD("genSM2Hash-->X509_NAME_add_entry_by_txt C ");
        X509_NAME_add_entry_by_txt(name, "O", V_ASN1_UTF8STRING,
                                   reinterpret_cast<const unsigned char *>("ncc operation ca"), -1, -1, 0);
        LOGD("genSM2Hash-->X509_NAME_add_entry_by_txt O ");
        X509_NAME_add_entry_by_txt(name, "OU", V_ASN1_UTF8STRING,
								   reinterpret_cast<const unsigned char *>("Customers"), -1, -1, 0);
        LOGD("genSM2Hash-->X509_NAME_add_entry_by_txt OU ");
        X509_NAME_add_entry_by_txt(name, "CN", V_ASN1_UTF8STRING, CN, -1, -1, 0);
        LOGD("genSM2Hash-->X509_NAME_add_entry_by_txt CN ");
    } else {
        name = parse_name((char *)LeField, V_ASN1_UTF8STRING, 0);
        X509_REQ_set_subject_name(req, name);
	}

	//产生SM2密钥及PKCS#10请求
		pSM2_key = EC_KEY_new_by_curve_name(NID_sm2p256v1);
		pSM2_Group = EC_GROUP_new_by_curve_name(NID_sm2p256v1);

		ByteArrayToHexString(bPublicKey, usPubKeyLen, TempBuffer + 2);
		TempBuffer[0] = 0x30;
		TempBuffer[1] = 0x34;

		pSM2PublicKey = EC_POINT_hex2point(pSM2_Group,
										   reinterpret_cast<const char *>(TempBuffer), NULL, NULL);
		EC_KEY_set_public_key(pSM2_key, pSM2PublicKey);
		pKey = EVP_PKEY_new();
		pSM2_key_EVP = EC_KEY_dup(pSM2_key);
		EVP_PKEY_set1_EC_KEY(pKey, pSM2_key_EVP);

		X509_REQ_set_pubkey(req, pKey);
		X509_ALGOR_set0(req->sig_alg, OBJ_nid2obj(NID_sm2sign_with_sm3), V_ASN1_NULL, NULL);

		req->req_info->attributes = NULL;

		//生成待HASH数据
		//处理ECC/SM2 ALGID
		UCHAR Temp[1024];
		UCHAR Temp1[1024];
		USHORT usTempLen;
		UCHAR sm2ID[] = "301306072A8648CE3D020106082A811CCF5501822D";

		P = Temp;

		usTempLen = i2d_ASN1_BIT_STRING(req->req_info->pubkey->public_key, &P);


		ByteArrayToHexString(Temp, usTempLen, Temp1);

		sprintf(reinterpret_cast<char *>(Temp), "%s%s", sm2ID, Temp1);

		usTempLen = strlen(reinterpret_cast<const char *>(Temp)) / 2;

		if (usTempLen >= 256)
		{
			sprintf(reinterpret_cast<char *>(bReqInfo), "3082%04X%s", usTempLen, Temp);
		}
		else if (usTempLen >= 128)
		{
			sprintf(reinterpret_cast<char *>(bReqInfo), "3081%02X%s", usTempLen, Temp);
		}
		else
		{
			sprintf(reinterpret_cast<char *>(bReqInfo), "30%02X%s", usTempLen, Temp);
		}

		P = Temp;

		X509_NAME *X509_NAME = X509_REQ_get_subject_name(req);
		usTempLen = i2d_X509_NAME(X509_NAME, &P);


		ByteArrayToHexString(Temp, usTempLen, Temp1);

		sprintf(reinterpret_cast<char *>(Temp), "020100%s%s", Temp1, bReqInfo);

		usTempLen = strlen(reinterpret_cast<const char *>(Temp)) / 2;

		if (usTempLen >= 256)
		{
			sprintf(reinterpret_cast<char *>(Temp1), "3082%04X%s", usTempLen, Temp);
		}
		else if (usTempLen >= 128)
		{
			sprintf(reinterpret_cast<char *>(Temp1), "3081%02X%s", usTempLen, Temp);
		}
		else
		{
			sprintf(reinterpret_cast<char *>(Temp1), "30%02X%s", usTempLen, Temp);
		}

		lReqInfoLen = HexStringToByteArray(reinterpret_cast<char *>(Temp1), strlen(reinterpret_cast<const char *>(Temp1)), bReqInfo);
		//处理ECC/SM2 ID完毕

		//复制bReqInfo临时计算Hash
		BYTE bTempReqInfo[2048] = {0};
        memcpy(bTempReqInfo, bReqInfo, lReqInfoLen);

        memmove(bReqInfo + usPubKeyLen, bReqInfo, lReqInfoLen);
        memcpy(bReqInfo, bPublicKey, usPubKeyLen);

		//获取SM2的ZA信息
        jclass GmSSLClass = env->FindClass("com/froad/ukey/jni/GmSSL");
        if (GmSSLClass == NULL)
        {
            LOGD("genSM2Hash-->FindClass com/froad/ukey/jni/GmSSL failed");
            return 1;
        }

        jmethodID jniMethodSm2GetZSM = env->GetMethodID(GmSSLClass, "sm2GetZSM", "([B)[B");
        jbyteArray pkBytes = CStr2Jba(env, (const char*)bPublicKey, usPubKeyLen);
        jbyteArray zsmJba = (jbyteArray) env->CallObjectMethod (gmSSL, jniMethodSm2GetZSM, pkBytes);
        jsize alen   =   env->GetArrayLength(zsmJba); //获取长度
        LOGD("genSM2Hash alen --> %d", alen);
        char* zsmChar = Jba2CStr(env, zsmJba);

        //将ZA添加到签名信息前
		memmove(bTempReqInfo + alen, bTempReqInfo, lReqInfoLen);
		memcpy(bTempReqInfo, zsmChar, alen);

		//TODO 做 hash
    	digest(env, "sm3",bTempReqInfo, lReqInfoLen + alen, backHashLen,(unsigned char*)backHash_);
		LOGD("genSM2Hash backHashLen --> %d", *backHashLen);
		LOGD("genSM2Hash backHash_ --> %s", backHash_);
    	memcpy(backHash,backHash_,*backHashLen);
    return TRUE;

}

BOOL genSM2P10(JNIEnv* env, char* SM2Pkcs10, USHORT* SM2Pkcs10Len,	BYTE bSignBuffer[512],	USHORT usSignLen)
{
	if (SM2Pkcs10Len != NULL)
	{
		*SM2Pkcs10Len = 0;
	}
	LOGD("genSM2P10 bSignBuffer : %s",bSignBuffer);
	LOGD("genSM2P10 usSignLen : %d",usSignLen);

	BYTE TempBuffer[2048] = {0};
	USHORT usTempLen;
	UCHAR* P;
	int plusFlag = 0;
	memset(TempBuffer, 0x00, sizeof(TempBuffer));
	TempBuffer[0] = 0x30;
	TempBuffer[1] = 0x44;
	TempBuffer[2] = 0x02;
	TempBuffer[3] = 0x20;
	//2019-10-11农信银 不需要补位 00
//	if (bSignBuffer[0] & 0x80)
//	{
//		plusFlag++;
//		TempBuffer[1]++;
//		TempBuffer[3]++;
//	}
	memcpy(TempBuffer + 4 + plusFlag, bSignBuffer, 0x20);
	TempBuffer[4 + 0x20 + plusFlag] = 0x02;
	TempBuffer[4 + 0x20 + plusFlag + 1] = 0x20;
	//2019-10-11农信银 不需要补位 00
//	if (bSignBuffer[0x20] & 0x80)
//	{
//		TempBuffer[1]++;
//		TempBuffer[4 + 0x20 + plusFlag + 1]++;
//		plusFlag++;
//	}
	memcpy(TempBuffer + 4 + 0x20 + plusFlag + 2, bSignBuffer + 0x20, 0x20);

	LOGD("genSM2P10 ASN1_STRING_set start ");
	ASN1_STRING_set(req->signature, TempBuffer, TempBuffer[1] + 2);
	LOGD("genSM2P10 ASN1_STRING_set end ");

	P = TempBuffer;
	LOGD("genSM2P10 i2d_X509_REQ start ");
	usTempLen = i2d_X509_REQ(req, &P);
	LOGD("genSM2P10 i2d_X509_REQ end usTempLen --> %d", usTempLen);


	//替换ReqInfo
	UCHAR* pReqInfoBegin;
	UCHAR* pRemainDataBegin;
	USHORT usReqInfoLen;
	USHORT usRemainDataLen;

	pReqInfoBegin = TempBuffer + 2;
	if (TempBuffer[1] & 0x80)
	{
		pReqInfoBegin += TempBuffer[1] & 0xF;
	}

	if (pReqInfoBegin[1] & 0x80)
	{
		if (pReqInfoBegin[1] == 0x81)
		{
			usReqInfoLen = pReqInfoBegin[2] + 3;
		}
		else
		{
			usReqInfoLen = MAKESHORT(pReqInfoBegin + 2) + 4;
		}
	}
	else
	{
		usReqInfoLen = pReqInfoBegin[1] + 2;
	}

	pRemainDataBegin = pReqInfoBegin + usReqInfoLen;
	usRemainDataLen = usTempLen - (pRemainDataBegin - TempBuffer);

	memcpy(pReqInfoBegin, bReqInfo + 0x40, lReqInfoLen);
	memmove(pReqInfoBegin + lReqInfoLen, pRemainDataBegin, usRemainDataLen);

	usTempLen = lReqInfoLen + usRemainDataLen;

	LOGD("genSM2P10 usTempLen : %d",usTempLen);

	if (usTempLen >= 256)
	{
		memmove(TempBuffer + 4, pReqInfoBegin, usTempLen);
		TempBuffer[1] = 0x82;
		TempBuffer[2] = usTempLen / 256;
		TempBuffer[3] = usTempLen % 256;
		usTempLen += 4;
	}
	else if (usTempLen >= 128)
	{
		memmove(TempBuffer + 3, pReqInfoBegin, usTempLen);
		TempBuffer[1] = 0x81;
		TempBuffer[2] = usTempLen;
		usTempLen += 3;
	}
	else
	{
		memmove(TempBuffer + 2, pReqInfoBegin, usTempLen);
		TempBuffer[1] = usTempLen;
		usTempLen += 2;
	}

	LOGD("genSM2P10 Base64Encode start TempBuffer : %s",TempBuffer);

	Base64Encode((char*)TempBuffer, usTempLen, SM2Pkcs10, false);

	*SM2Pkcs10Len = strlen(SM2Pkcs10);
	LOGD("genSM2P10 strlen end SM2Pkcs10Len : %d",*SM2Pkcs10Len);

	EC_KEY_free(pSM2_key);
	EC_GROUP_free(pSM2_Group);

	EVP_PKEY_free(pKey);
	X509_REQ_free(req);

	return TRUE;
}
//rsa PKCS7 转 Pem 格式
bool convertPkcs7ToPemHex(JNIEnv* env, UCHAR *pkcs7_key, int certLen, UCHAR *outCert, ULONG *oCertLen)
{
	PKCS7* pRSACert = NULL;
	UCHAR* P;
	X509* cert = NULL;
	BYTE TempBuffer[2048] = {0};

	memcpy(TempBuffer, pkcs7_key, certLen);

	P = (UCHAR*)pkcs7_key;

	pRSACert = d2i_PKCS7(NULL, (const unsigned char**)&P, certLen);

	//RSA PKCS7 Parse Failed, Try To Parse X509
	if (pRSACert == NULL) {

		P = (UCHAR*)TempBuffer;

		cert = d2i_X509(NULL, (const unsigned char**)&P, certLen);

		if (cert == NULL)
		{
			return false;
		}
	}

	if (cert == NULL)
	{
		cert = sk_X509_pop(pRSACert->d.sign->cert);
	}

	P = outCert;

	*oCertLen = i2d_X509(cert, &P);

	return *oCertLen > 0;
}

/*
* subject is expected to be in the format /type0=value0/type1=value1/type2=...
* where characters may be escaped by \
*/
X509_NAME *parse_name(char *subject, long chtype, int multirdn)
{
    size_t buflen = strlen(subject)+1; /* to copy the types and values into. due to escaping, the copy can only become shorter */
    char *buf = (char *)OPENSSL_malloc(buflen);
    size_t max_ne = buflen / 2 + 1; /* maximum number of name elements */
    char **ne_types = (char **)OPENSSL_malloc(max_ne * sizeof (char *));
    char **ne_values = (char **)OPENSSL_malloc(max_ne * sizeof (char *));
    int *mval = (int *)OPENSSL_malloc (max_ne * sizeof (int));

    char *sp = subject, *bp = buf;
    int i, ne_num = 0;

    X509_NAME *n = NULL;
    int nid;

    if (!buf || !ne_types || !ne_values || !mval)
    {
        //BIO_printf(bio_err, "malloc error\n");
        goto error;
    }

    if (*subject == ',')
    {
        //BIO_printf(bio_err, "Subject does not start with ','.\n");
        sp++; /* skip leading / */
    }

    /* no multivalued RDN by default */
    mval[ne_num] = 0;

    while (*sp)
    {
        /* collect type */
        ne_types[ne_num] = bp;
        while (*sp)
        {
            if (*sp == '\\') /* is there anything to escape in the type...? */
            {
                if (*++sp)
                    *bp++ = *sp++;
                else
                {
                    //BIO_printf(bio_err, "escape character at end of string\n");
                    goto error;
                }
            }
            else if (*sp == '=')
            {
                sp++;
                *bp++ = '\0';
                break;
            }
            else
                *bp++ = *sp++;
        }
        if (!*sp)
        {
            //BIO_printf(bio_err, "end of string encountered while processing type of subject name element #%d\n", ne_num);
            goto error;
        }
        ne_values[ne_num] = bp;
        while (*sp)
        {
            if (*sp == '\\')
            {
                if (*++sp)
                    *bp++ = *sp++;
                else
                {
                    //BIO_printf(bio_err, "escape character at end of string\n");
                    goto error;
                }
            }
            else if (*sp == ',')
            {
                sp++;
                /* no multivalued RDN by default */
                mval[ne_num+1] = 0;
                break;
            }
            else if (*sp == '+' && multirdn)
            {
                /* a not escaped + signals a mutlivalued RDN */
                sp++;
                mval[ne_num+1] = -1;
                break;
            }
            else
                *bp++ = *sp++;
        }
        *bp++ = '\0';
        ne_num++;
    }

    if (!(n = X509_NAME_new()))
        goto error;

    for (i = 0; i < ne_num; i++)
    {
//        printf("========== %s > %s > %d\n", ne_types[i] , ne_values[i] , mval[i]);
        if ((nid=OBJ_txt2nid(ne_types[i])) == NID_undef)
        {
            //BIO_printf(bio_err, "Subject Attribute %s has no known NID, skipped\n", ne_types[i]);
            continue;
        }

        if (!*ne_values[i])
        {
            //BIO_printf(bio_err, "No value provided for Subject Attribute %s, skipped\n", ne_types[i]);
            continue;
        }

        if (!X509_NAME_add_entry_by_NID(n, nid, (int)chtype, (unsigned char*)ne_values[i], -1, -1, mval[i]))
            goto error;
    }

    OPENSSL_free(ne_values);
    OPENSSL_free(ne_types);
    OPENSSL_free(buf);
    OPENSSL_free(mval);
    return n;

    error:
    X509_NAME_free(n);
    if (ne_values)
        OPENSSL_free(ne_values);
    if (ne_types)
        OPENSSL_free(ne_types);
    if (mval)
        OPENSSL_free(mval);
    if (buf)
        OPENSSL_free(buf);
    return NULL;
}

