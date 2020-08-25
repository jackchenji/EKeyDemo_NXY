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
#include <openssl/skf.h>
#include "Utils.h"
#include "Base64Utils.h"
//#include "Log.h"

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



#ifdef FOR_RSA_ONLY
DLL_API BOOL WINAPI Vender_GenCertReq(HANDLE reader, int iKeyType, int iAlgType, char* Pkcs10, USHORT* Pkcs10Len, USHORT* SW)
{
	DEBUG_LOG("++++++++++++ Vender_GenCertReq reader:%08X iKeyType:%02X iAlgType:%02X Pkcs10:%08X Pkcs10Len:%08X SW:%08X ++++++++++++", reader, iKeyType, iAlgType, Pkcs10, Pkcs10Len, SW);
	return Vender_GenCertReq_withSM(reader, iKeyType, iAlgType, Pkcs10, Pkcs10Len, NULL, NULL, SW);
}
BOOL WINAPI Vender_GenCertReq_withSM(HANDLE reader, int iKeyType, int iAlgType, char* RSAPkcs10, USHORT* RSAPkcs10Len, char* SM2Pkcs10, USHORT* SM2Pkcs10Len, USHORT* SW)
#else
/*
 * iAlgType 算法类型 1 ,2,RSA  3. SM2 4.  RSA + SM2
 * LeField 卡号
 *
 */
BOOL Vender_GenCertReq(  int iAlgType, unsigned char*  LeField, char* RSAPkcs10, USHORT* RSAPkcs10Len, char* SM2Pkcs10, USHORT* SM2Pkcs10Len)
#endif
{
//	DEBUG_LOG("++++++++++++ Vender_GenCertReq reader:%08X iKeyType:%02X iAlgType:%02X RSAPkcs10:%08X RSAPkcs10Len:%08X SM2Pkcs10:%08X SM2Pkcs10Len:%08X SW:%08X ++++++++++++", reader, iKeyType, iAlgType, RSAPkcs10, RSAPkcs10Len, SM2Pkcs10, SM2Pkcs10Len, SW);

//	VariableInit();
	BYTE SNLen;
	BYTE TempBuffer[2048];
	BYTE bSignBuffer[512];
	BYTE bPublicKey[512];
	BYTE bReqInfo[2048];
	LONG lReqInfoLen;
	BYTE bHASH[128];
	USHORT usTempLen;
	USHORT usSignLen;
	USHORT usHASHLen;
	USHORT usPubKeyLen;
	UCHAR* P;
	UCHAR* pReqInfo = bReqInfo;
	X509_REQ* req = NULL;
	X509_NAME* name = NULL;
	BYTE CN[64];
	BYTE szSN[17];
	EC_KEY *pSM2_key = NULL;
	EC_KEY *pSM2_key_EVP = NULL;
	RSA *pRSA = NULL;
	RSA *pRSA_EVP = NULL;
	EVP_PKEY *pKey = NULL;
	EC_POINT* pSM2PublicKey = NULL;
	EC_GROUP* pSM2_Group = NULL;

//	DoInit();

	if (RSAPkcs10Len != NULL)
	{
		*RSAPkcs10Len = 0;
	}
	if (SM2Pkcs10Len != NULL)
	{
		*SM2Pkcs10Len = 0;
	}

//	SendApdu("B11000000C");

//	Check9000();

//	VerifyResponse();

//	WaitForSingleObject(OpenSSLMutex, INFINITE);

	req = X509_REQ_new();
	name = X509_REQ_get_subject_name(req);
	X509_REQ_set_version(req, 0);

	int LeFieldLen = strlen(reinterpret_cast<const char *>(LeField));
	ByteArrayToHexString(LeField, LeFieldLen, (UCHAR*)szSN);

	//生成请求DN
	sprintf(reinterpret_cast<char *>(CN), "064@%s", szSN);
	X509_NAME_add_entry_by_txt(name, "C", MBSTRING_ASC, (const UCHAR*)"CN", -1, -1, 0);
	X509_NAME_add_entry_by_txt(name, "O", MBSTRING_ASC, (const UCHAR*)"ncc operation ca", -1, -1, 0);
	X509_NAME_add_entry_by_txt(name, "CN", MBSTRING_ASC, (const UCHAR*)CN, -1, -1, 0);
//	if ((iKeyType == 102) || (iKeyType == 103))
//	{
	X509_NAME_add_entry_by_txt(name, "OU", MBSTRING_ASC, (const UCHAR*)"Customers", -1, -1, 0);
//	}
//	else if ((iKeyType == 902) || (iKeyType == 903))
//	{
//		X509_NAME_add_entry_by_txt(name, "OU", MBSTRING_ASC, (const UCHAR*)"Enterises", -1, -1, 0);
//	}
//	else
//	{
//		PowerOffCard();
//		X509_REQ_free(req);
//		*SW = 0xFFF9;
//		ReleaseMutex(OpenSSLMutex);
//		return FALSE;
//	}

//	ReleaseMutex(OpenSSLMutex);

	//产生RSA密钥及PKCS#10请求
	if ((iAlgType == 1) || (iAlgType == 2) || (iAlgType == 4))
	{
//		if (iAlgType != 4)
//		{
//			sprintf(Cmd, "B1260%X0000", iAlgType);
//		}
//		else
//		{
//			sprintf(Cmd, "B126020000");
//		}
		// TODO 获取公钥
//		bRet = GetPublicKey((HANDLE)reader, Cmd, bPublicKey, &usPubKeyLen, SW);
//		if (bRet == FALSE)
//		{
//			PowerOffCard();
//
//			WaitForSingleObject(OpenSSLMutex, INFINITE);
//			X509_REQ_free(req);
//			ReleaseMutex(OpenSSLMutex);
//
//			return FALSE;
//		}

		P = bPublicKey;

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		pRSA = RSA_new();
		BN_asc2bn(&pRSA->e, "65537");
		pRSA->n = BN_bin2bn(bPublicKey, usPubKeyLen, pRSA->n);
		pKey = EVP_PKEY_new();
		pRSA_EVP = RSAPublicKey_dup(pRSA);
		EVP_PKEY_set1_RSA(pKey, pRSA_EVP);

		X509_REQ_set_pubkey(req, pKey);

		//if (iAlgType != 1)
		//{
		//	X509_ALGOR_set0(req->sig_alg, OBJ_nid2obj(NID_sha256WithRSAEncryption), V_ASN1_NULL, NULL);
		//}
		//else
		//{
		X509_ALGOR_set0(req->sig_alg, OBJ_nid2obj(NID_sha1WithRSAEncryption), V_ASN1_NULL, NULL);
		//}
		req->req_info->attributes = NULL;
		//生成待HASH数据
		lReqInfoLen = i2d_X509_REQ_INFO(req->req_info, &pReqInfo);
//		ReleaseMutex(OpenSSLMutex);

//		if (lReqInfoLen == 0)
//		{
//			PowerOffCard();
//
//			WaitForSingleObject(OpenSSLMutex, INFINITE);
//			RSA_free(pRSA);
//			X509_REQ_free(req);
//			EVP_PKEY_free(pKey);
//			*SW = 0xFFF7;
//			ReleaseMutex(OpenSSLMutex);
//
//			return FALSE;
//		}
//    TODO : 做 hash 做签名
		//if (iAlgType != 1)
		//{
		//	bRet = GetHASH((HANDLE)reader, HASH_PADDING_MODE_SHA256 | HASH_ALG_SHA256, bReqInfo, lReqInfoLen, bHASH, &usHASHLen, SW);
		//}
		//else
//		{
//			bRet = GetHASH((HANDLE)reader, HASH_PADDING_MODE_SHA1 | HASH_ALG_SHA1, bReqInfo, lReqInfoLen, bHASH, &usHASHLen, SW);
//		}
//		if (bRet == FALSE)
//		{
//			PowerOffCard();
//
//			WaitForSingleObject(OpenSSLMutex, INFINITE);
//			RSA_free(pRSA);
//			EVP_PKEY_free(pKey);
//			X509_REQ_free(req);
//			ReleaseMutex(OpenSSLMutex);
//
//			return FALSE;
//		}


		if (iAlgType == 1)
		{
//			bRet = GetSignature((HANDLE)reader, SIGN_RSA1024_WITH_SHA1, bHASH, usHASHLen, bSignBuffer, &usSignLen, SW);
		}
		else
		{
//			bRet = GetSignature((HANDLE)reader, SIGN_RSA2048_WITH_SHA1, bHASH, usHASHLen, bSignBuffer, &usSignLen, SW);
		}
//		if (bRet == FALSE)
//		{
//			PowerOffCard();
//
//			WaitForSingleObject(OpenSSLMutex, INFINITE);
//			RSA_free(pRSA);
//			EVP_PKEY_free(pKey);
//			X509_REQ_free(req);
//			ReleaseMutex(OpenSSLMutex);
//
//			return FALSE;
//		}

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		ASN1_BIT_STRING_set(req->signature, bSignBuffer, usSignLen);

		P = TempBuffer;
		usTempLen = i2d_X509_REQ(req, &P);

		Base64Encode((char*)TempBuffer, usTempLen, (char*)RSAPkcs10, false);

		*RSAPkcs10Len = strlen(RSAPkcs10);
//		DEBUG_HEX("bRSAPKCS10", (char*)RSAPkcs10, *RSAPkcs10Len);
		RSA_free(pRSA);
//		ReleaseMutex(OpenSSLMutex);
	}

	//产生SM2密钥及PKCS#10请求
	if ((iAlgType == 3) || (iAlgType == 4))
	{
//		sprintf(Cmd, "B126030000");
// TODO 获取公钥
//		bRet = GetPublicKey((HANDLE)reader, Cmd, bPublicKey, &usPubKeyLen, SW);
//		if (bRet == FALSE)
//		{
//			PowerOffCard();
//
//			WaitForSingleObject(OpenSSLMutex, INFINITE);
//			X509_REQ_free(req);
//			ReleaseMutex(OpenSSLMutex);
//
//			return FALSE;
//		}
//
//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		pSM2_key = EC_KEY_new_by_curve_name(NID_sm2p256v1);
		pSM2_Group = EC_GROUP_new_by_curve_name(NID_sm2p256v1);
//		ReleaseMutex(OpenSSLMutex);

		ByteArrayToHexString(bPublicKey, usPubKeyLen, TempBuffer + 2);
		TempBuffer[0] = 0x30;
		TempBuffer[1] = 0x34;

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		pSM2PublicKey = EC_POINT_hex2point(pSM2_Group,
										   reinterpret_cast<const char *>(TempBuffer), NULL, NULL);
		EC_KEY_set_public_key(pSM2_key, pSM2PublicKey);
		pKey = EVP_PKEY_new();
		pSM2_key_EVP = EC_KEY_dup(pSM2_key);
		EVP_PKEY_set1_EC_KEY(pKey, pSM2_key_EVP);

		X509_REQ_set_pubkey(req, pKey);
		X509_ALGOR_set0(req->sig_alg, OBJ_nid2obj(NID_sm2sign_with_sm3), V_ASN1_NULL, NULL);
//		ReleaseMutex(OpenSSLMutex);

		req->req_info->attributes = NULL;

		//生成待HASH数据
		//处理ECC/SM2 ALGID
		UCHAR Temp[1024];
		UCHAR Temp1[1024];
		USHORT usTempLen;
		UCHAR sm2ID[] = "301306072A8648CE3D020106082A811CCF5501822D";

		P = Temp;

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		usTempLen = i2d_ASN1_BIT_STRING(req->req_info->pubkey->public_key, &P);
//		ReleaseMutex(OpenSSLMutex);

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

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		usTempLen = i2d_X509_NAME(req->req_info->subject, &P);
//		ReleaseMutex(OpenSSLMutex);

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

		memmove(bReqInfo + usPubKeyLen, bReqInfo, lReqInfoLen);
		memcpy(bReqInfo, bPublicKey, usPubKeyLen);

		// TODO 做 hash 做签名
//		bRet = GetHASH((HANDLE)reader, HASH_MODE_SM3_FORSIGN | HASH_ALG_SM3, bReqInfo, lReqInfoLen + 0x40, bHASH, &usHASHLen, SW);
//		if (bRet == FALSE)
//		{
//			PowerOffCard();
//
//			WaitForSingleObject(OpenSSLMutex, INFINITE);
//			EC_KEY_free(pSM2_key);
//			EC_GROUP_free(pSM2_Group);
//			EVP_PKEY_free(pKey);
//			X509_REQ_free(req);
//			ReleaseMutex(OpenSSLMutex);
//
//			return FALSE;
//		}
//		bRet = GetSignature((HANDLE)reader, SIGN_SM2_WITH_SM3, bHASH, usHASHLen, bSignBuffer, &usSignLen, SW);
//
//		if (bRet == FALSE)
//		{
//			PowerOffCard();
//
//			WaitForSingleObject(OpenSSLMutex, INFINITE);
//			EC_KEY_free(pSM2_key);
//			EC_GROUP_free(pSM2_Group);
//			EVP_PKEY_free(pKey);
//			X509_REQ_free(req);
//			ReleaseMutex(OpenSSLMutex);
//
//			return FALSE;
//		}

		int plusFlag = 0;
		memset(TempBuffer, 0x00, sizeof(TempBuffer));
		TempBuffer[0] = 0x30;
		TempBuffer[1] = 0x44;
		TempBuffer[2] = 0x02;
		TempBuffer[3] = 0x20;
		if (bSignBuffer[0] & 0x80)
		{
			plusFlag++;
			TempBuffer[1]++;
			TempBuffer[3]++;
		}
		memcpy(TempBuffer + 4 + plusFlag, bSignBuffer, 0x20);
		TempBuffer[4 + 0x20 + plusFlag] = 0x02;
		TempBuffer[4 + 0x20 + plusFlag + 1] = 0x20;
		if (bSignBuffer[0x20] & 0x80)
		{
			TempBuffer[1]++;
			TempBuffer[4 + 0x20 + plusFlag + 1]++;
			plusFlag++;
		}
		memcpy(TempBuffer + 4 + 0x20 + plusFlag + 2, bSignBuffer + 0x20, 0x20);

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		ASN1_STRING_set(req->signature, TempBuffer, TempBuffer[1] + 2);

		P = TempBuffer;
		usTempLen = i2d_X509_REQ(req, &P);
//		ReleaseMutex(OpenSSLMutex);

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

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		Base64Encode((char*)TempBuffer, usTempLen, (char*)SM2Pkcs10, false);
//		ReleaseMutex(OpenSSLMutex);

		*SM2Pkcs10Len = strlen(SM2Pkcs10);

//		DEBUG_HEX("bSM2PKCS10", (char*)SM2Pkcs10, *SM2Pkcs10Len);

//		WaitForSingleObject(OpenSSLMutex, INFINITE);
		EC_KEY_free(pSM2_key);
		EC_GROUP_free(pSM2_Group);
//		ReleaseMutex(OpenSSLMutex);
	}

//	WaitForSingleObject(OpenSSLMutex, INFINITE);
	EVP_PKEY_free(pKey);
	X509_REQ_free(req);
//	ReleaseMutex(OpenSSLMutex);

//	DEBUG_LOG("------------ Vender_GenCertReq SW:%02X ------------", *SW);
	//ReleaseMutex(OpenSSLMutex);

	return TRUE;
}

