//
// Created by 易松 on 17/2/22.
//
#include <jni.h>
#include <stdio.h>
#include <cstdlib>
#include "froadLog.h"
#include "DesAlgo.h"
#include "Utils.h"

unsigned char lenBytes[3] = {0x00};
unsigned char resBytes[1024] = {0x00};
unsigned char ICV[9] = {0x00};

/**
*将char*转换为jstring
*/
jstring strToJstring(JNIEnv* env, const char* pat)
{
    jclass strClass = env->FindClass( "java/lang/String");
    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    int len = strlen(pat);
    LOGE("strToJstring --------len is %d", len);
    jbyteArray bytes = env->NewByteArray( len);
    env->SetByteArrayRegion( bytes, 0, len, (jbyte*)pat);
    jstring encoding = env->NewStringUTF( "utf-8");
    jstring res = (jstring)env->NewObject( strClass, ctorID, bytes, encoding);
    env->DeleteLocalRef( bytes);
    env->DeleteLocalRef(encoding);
    return res;
}

/**
 * 描述: 将jstring类型字符串转化为char类型字符串
 * 参数:
 * @env JNI上下文
 * @jstr 将要转化的jstring类型字符串
 * 返回值: 转换后的char类型字符串
 */
char* jstringToChar(JNIEnv* env, jstring jstr)
{
    char* rtn = NULL;

    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");

    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    if (mid == NULL) {
        LOGD("get getBytes failed");
        return NULL;
    }

    jobject obj= env->CallObjectMethod(jstr, mid, strencode);
    if (obj == NULL) {
        LOGD("call getBytes failed");
        return NULL;
    }

    jsize alen = env->GetArrayLength( (jbyteArray)obj);
    if (alen == 0) {
        LOGD("GetArrayLength size = 0");
        return NULL;
    }

    jbyte* ba = env->GetByteArrayElements( (jbyteArray)obj, JNI_FALSE);
    if (ba == NULL) {
        LOGD("GetByteArrayElements failed");
        return NULL;
    }
    if (alen > 0)
    {
        LOGD("jstringToChar alen is %d", alen);
        rtn = (char*)malloc(alen + 1);

        memcpy(rtn, ba, alen);

        rtn[alen] = 0;
    }

    env->ReleaseByteArrayElements( (jbyteArray)obj, ba, 0);
    LOGD("rtn = ", rtn);
    return rtn;
}

jbyteArray jHexStringToJbyteArray(JNIEnv* env, jstring jstr) {
    if (jstr == NULL) {
        return NULL;
    }
    char* hexCharArray = jstringToChar(env, jstr);

    LOGD("--debug-- jstringToJbyteArray>>>hexCharArray:%s", hexCharArray);
    jclass FCharUtilsClass = env->FindClass( "com/froad/ukey/utils/np/FCharUtils");
    //获取并执行Java类中的hexString2ByteArray方法
    jmethodID jniMethod = env->GetStaticMethodID( FCharUtilsClass, "hexString2ByteArray", "(Ljava/lang/String;)[B");
    if (jniMethod == NULL)
    {
        LOGD("get hexString2ByteArray() method failed");
        return NULL;
    }
    jobject byteArrayObj = env->CallStaticObjectMethod( FCharUtilsClass, jniMethod, jstr);
    if (byteArrayObj == NULL)
    {
        LOGD("call hexString2ByteArray() method failed");
        return NULL;
    }
    return (jbyteArray)byteArrayObj;
}

jbyteArray jStringToJbyteArray(JNIEnv* env, jstring jstr) {
    if (jstr == NULL) {
        return NULL;
    }

    jclass FCharUtilsClass = env->FindClass( "com/froad/ukey/utils/np/FCharUtils");
    //获取并执行Java类中的hexString2ByteArray方法
    jmethodID jniMethod = env->GetStaticMethodID(FCharUtilsClass, "stringToByteArray", "(Ljava/lang/String;)[B");
    if (jniMethod == NULL)
    {
        LOGD("get hexString2ByteArray() method failed");
        return NULL;
    }
    jobject byteArrayObj = env->CallStaticObjectMethod( FCharUtilsClass, jniMethod, jstr);
    if (byteArrayObj == NULL)
    {
        LOGD("call hexString2ByteArray() method failed");
        return NULL;
    }
    return (jbyteArray)byteArrayObj;
}

jstring jstringToJHexStr(JNIEnv* env, jstring jstr) {
    jclass FCharUtilsClass = env->FindClass( "com/froad/ukey/utils/np/FCharUtils");
    //获取并执行Java类中的hexString2ByteArray方法
    jmethodID jniMethod = env->GetStaticMethodID( FCharUtilsClass, "string2HexStr", "(Ljava/lang/String;)Ljava/lang/String;");
    if (jniMethod == NULL)
    {
        LOGD("get string2HexStr method failed");
        return NULL;
    }
    jstr = static_cast<jstring>(env->CallStaticObjectMethod(FCharUtilsClass, jniMethod, jstr));
    if (jstr == NULL)
    {
        LOGD("call string2HexStr method failed");
        return NULL;
    }
    return jstr;
}

jstring jbyteArrayToJHexString(JNIEnv* env, jbyteArray jba) {
    jclass FCharUtilsClass = env->FindClass( "com/froad/ukey/utils/np/FCharUtils");
    //获取并执行Java类中的hexString2ByteArray方法
    jmethodID jniMethod = env->GetStaticMethodID( FCharUtilsClass, "showResult16Str", "([B)Ljava/lang/String;");
    if (jniMethod == NULL)
    {
        LOGD("get string2HexStr method failed");
        return NULL;
    }
    jstring jstr = static_cast<jstring>(env->CallStaticObjectMethod(FCharUtilsClass, jniMethod, jba));
    if (jstr == NULL)
    {
        LOGD("call string2HexStr method failed");
        return NULL;
    }
    return jstr;
}

jbyteArray jDealShift(JNIEnv* env, jbyteArray jba, int f) {
    LOGD("------jDealShift------");
    jclass ByteUtilClass = env->FindClass( "com/froad/ukey/utils/np/ByteUtil");
    if (ByteUtilClass == NULL)
    {
        LOGD("FindClass com/froad/ukey/utils/np/ByteUtil failed");
        return NULL;
    }
    jmethodID jniMethodDealShift = env->GetStaticMethodID( ByteUtilClass, "dealShift", "([BI)[B");
    if (jniMethodDealShift == NULL)
    {
        LOGD("GetStaticMethodID dealShift failed");
        return NULL;
    }
    jba = (jbyteArray)env->CallStaticObjectMethod( ByteUtilClass, jniMethodDealShift, jba, f);
    if (jba == NULL)
    {
        LOGD("CallStaticObjectMethod dealShift failed");
        return NULL;
    }
    return jba;
}

int leftShift( char* pData, int dataLen, int shiftValue)
{
	int i, maskH,maskL;
	if( shiftValue < 1 || shiftValue > 7 )
	{
		return 0;
	}
	maskH = 0xFF << shiftValue;
	maskL = 0xFF >> (8-shiftValue);

	for( i = 0 ; i < dataLen-1; i++ )
	{
		*(pData + i) = ((*(pData + i)) << shiftValue) & maskH;
		*(pData + i) |= ((*(pData + i + 1)) >> ( 8- shiftValue)) & maskL;
	}
	*(pData + i) = ((*(pData + i)) << shiftValue) & maskH;
	return 1;
}

char* Jba2CStr(JNIEnv* env, jbyteArray jba)
{
        char* rtn;
         jsize   alen   =   env->GetArrayLength(jba); //获取长度
    	 LOGD("Jba2CStr>>>alen:%d", alen);
         jbyte*   ba   =   env->GetByteArrayElements(jba,JNI_FALSE); //jbyteArray转为jbyte*

         if(alen   >   0)
         {
         rtn   =   (char*)malloc(alen+1);         //"\0"
         memcpy(rtn,ba,alen);
         rtn[alen]=0;
        }
        env->ReleaseByteArrayElements(jba,ba,0);
        return rtn;
}

jbyteArray CStr2Jba (JNIEnv* env, const char* cStr, int cLen)
{
	LOGD("CStr2Jba --------len is %d", cLen);
    jbyteArray bytes = env->NewByteArray(cLen);
    env->SetByteArrayRegion( bytes, 0, cLen, (jbyte*)cStr);
    return bytes;
}

/**
* 遇到异常后,抛出异常并终止程序运行
* @return JNI_TRUE 有异常并处理成功; JNI_FALSE 没有异常
*/
jboolean handleException(JNIEnv *env) {
    //异常处理
    jthrowable exc = NULL;
    exc = env->ExceptionOccurred();  // 返回一个指向当前异常对象的引用

    if (exc) {
        env->ExceptionDescribe(); // 打印Java层抛出的异常堆栈信息
        env->ExceptionClear();    // 清除异常信息

        // 抛出我们自己的异常处理
        jclass newExcCls;
        newExcCls = env->FindClass("java/lang/Exception");

        if (newExcCls == NULL) {
            return JNI_TRUE;
        }
        env->ThrowNew(newExcCls, "throw from C Code.");

        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

/**
* 将长度转换为两个字节数组存贮
*/
char* len2charArray (int l) {
    int t1 = (l >> 8);
    int t2 = l - (t1 << 8);
    char* res = (char*) lenBytes;
//将长度用两字节表示
    res[0] = t1;
    res[1] = t2;
    res[2] = 0;
    return res;
}

/**
*将数据前两字节转换为长度
*/
int charArray2Len (char* ls) {
    int t1 = ls[0] << 8;
    int t2 = ls[1];
    return (t1 + t2);
}


char* hex2Char(char* s) {
    LOGE("hex2Char");
    int i, n = 2;
    char* bits = NULL;
    if (s == NULL) {
        return NULL;
    }
    int sl = charArray2Len(s);
    LOGE("hex2Char>>>s leng %d ", sl);
    if (sl <= 0) {
        return NULL;
    }
    char* tc = len2charArray(sl / 2);
    bits = (char*) resBytes;
    for (i = 2; i < sl + 2; i += 2) {
        if (s[i] >= 'A' && s[i] <= 'F')
            bits[n] = s[i] - 'A' + 10;
        else if (s[i] >= 'a' && s[i] <= 'f')
            bits[n] = s[i] - 'a' + 10;
        else
            bits[n] = s[i] - '0';

        if (s[i + 1] >= 'A' && s[i + 1] <= 'F')
            bits[n] = (bits[n] << 4) | (s[i + 1] - 'A' + 10);
        else if (s[i + 1] >= 'a' && s[i + 1] <= 'f')
            bits[n] = (bits[n] << 4) | (s[i + 1] - 'a' + 10);
        else
            bits[n] = (bits[n] << 4) | (s[i + 1] - '0');
        ++n;
    }
    LOGE("n is %d ", n);
    bits[0] = tc[0];
    bits[1] = tc[1];
    bits[n] = '\0';
    return bits;
}

char* jBytes2Char(JNIEnv* env, jbyteArray jBytes)
{
    jbyte* s= env->GetByteArrayElements(jBytes, JNI_FALSE);

    jsize alen = env->GetArrayLength(jBytes);
    LOGE("jBytes length is %d", (int)alen);
    char* tc = len2charArray(alen);
    char* bits = (char*) resBytes;
    memcpy(bits + 2, s, alen);
    bits[0] = tc[0];
    bits[1] = tc[1];
    env->ReleaseByteArrayElements(jBytes, s, 0);
    return bits;
}

char* hex2CharNoLen(const char* s, int sl) {
    LOGE("hex2CharNoLen");
    int i, n = 0;
    char* bits = NULL;
    if (s == NULL) {
        return NULL;
    }
    LOGE("hex2Char>>>s leng %d ", sl);
    if (sl <= 0) {
        return NULL;
    }
    bits = (char*) resBytes;
    for (i = 0; i < sl; i += 2) {
        if (s[i] >= 'A' && s[i] <= 'F')
            bits[n] = s[i] - 'A' + 10;
        else if (s[i] >= 'a' && s[i] <= 'f')
            bits[n] = s[i] - 'a' + 10;
        else
            bits[n] = s[i] - '0';

        if (s[i + 1] >= 'A' && s[i + 1] <= 'F')
            bits[n] = (bits[n] << 4) | (s[i + 1] - 'A' + 10);
        else if (s[i + 1] >= 'a' && s[i + 1] <= 'f')
            bits[n] = (bits[n] << 4) | (s[i + 1] - 'a' + 10);
        else
            bits[n] = (bits[n] << 4) | (s[i + 1] - '0');
        ++n;
    }
    LOGE("n is %d ", n);
    bits[n] = '\0';
    return bits;
}

//jstring转换为char*，前两位为长度
char* jstringToChar_Len(JNIEnv* env, jstring jstr)
{
	char* rtn = NULL;
	jclass clsstring = env->FindClass( "java/lang/String");
	jstring strencode = env->NewStringUTF( "utf-8");
	jmethodID mid = env->GetMethodID( clsstring, "getBytes",
			"(Ljava/lang/String;)[B");
	jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
	jsize alen = env->GetArrayLength( barr);
	jbyte* ba = env->GetByteArrayElements( barr, JNI_FALSE);
	LOGE("barr length is %d", (int)alen);
	if (alen > 0) {
	    char* tc = len2charArray((int)alen);
		rtn = (char*) resBytes;//第一个地址存长度，最后一个地址存0表示结束
		memcpy(rtn + 2, ba, alen);
		rtn[0] = tc[0];
		rtn[1] = tc[1];
		rtn[alen + 2] = 0;
	}
	env->ReleaseByteArrayElements( barr, ba, 0);
	return rtn;
}

/**
 * diversify session key from main key
 */
void divsfMacKeyFun(const char* input, char* output)
{
	int i = 0;
	LOGE("MAC密钥为：");
	for(i=0;i<16;i++)
        LOGE("%02x ", input[i]);
	for( i=0 ; i < 8 ; i++ )
	{
		*(output + i) = *(input + i) ^ *(input + 8  + i);
	}
	LOGE("MAC密钥分散处理为：");
	for(i=0;i<8;i++)
        LOGE("%02x ", output[i]);

}

/**
 * Calculate mac using DES CBC algorithm
 */
void getMacFun(const char* keyInput, const char* data, int srcLen, char* mac)
{
	int offset = 0;
	int i = 0;
    int KEY_GROUPLEN = 8;
    char keyOutputs[9] = {0x00};
    char* keyOutput = (char*) keyOutputs;
    //处理密钥
    divsfMacKeyFun (keyInput, keyOutputs);

	memset(ICV, 0, 8);

	while (srcLen >= KEY_GROUPLEN)
	{
		for (i=0; i<KEY_GROUPLEN; i++)
		{
			ICV[i] ^= data[offset + i];
		}

		DesEncrypt((unsigned char*) keyOutput, (unsigned char*) ICV, 8);
        LOGE("加密结果：");
        for(i=0;i<8;i++)
            LOGE("%02x ", ICV[i]);
		srcLen -= KEY_GROUPLEN;
		offset += KEY_GROUPLEN;
	}

	for (i = 0; i < KEY_GROUPLEN; i++)
	{
		if (i < srcLen)
		{
			ICV[i] ^= data[offset + i];
		}
		else if(i == srcLen)
		{
			ICV[i] ^= 0x80;
		}
		else
		{
			ICV[i] ^= 0x00;
		}
	}

	DesEncrypt((unsigned char*)keyOutput, (unsigned char*)ICV, 8);
    LOGE("最终加密结果：");
    for(i=0;i<8;i++)
        LOGE("%02x ", ICV[i]);
	memcpy(mac, ICV, 4);
}

/**
 * Calculate mac using DES CBC algorithm
 */
void DesEncryptCBC(const char* keyInput, char* data, int srcLen)
{
    int offset = 0;
    int i = 0;
    int KEY_GROUPLEN = 8;
    char tempKeyEnc[9] = {0};
    char tempKeyDec[9] = {0};

    memset(ICV, 0, 8);
    memcpy(tempKeyEnc, keyInput, 8);
    memcpy(tempKeyDec, keyInput + 8, 8);

    while (srcLen >= KEY_GROUPLEN)
    {
        for (i=0; i<KEY_GROUPLEN; i++)
        {
            ICV[i] ^= data[offset + i];
        }

        DesEncrypt((unsigned char*) tempKeyEnc, (unsigned char*) ICV, 8);
        DesDecrypt((unsigned char*) tempKeyDec, (unsigned char*) ICV, 8);
        DesEncrypt((unsigned char*) tempKeyEnc, (unsigned char*) ICV, 8);
        LOGE("DesEncryptCBC--->加密结果：");
        for(i=0;i<KEY_GROUPLEN;i++) {
            data[offset + i] = ICV[i];
            LOGE("%02x ", ICV[i]);
        }
        srcLen -= KEY_GROUPLEN;
        offset += KEY_GROUPLEN;
    }
}

void printJavaHex (JNIEnv* env, char* outbuf, int outLen) {
    jclass FCharUtilsClass = env->FindClass("com/froad/ukey/utils/np/FCharUtils");
    jmethodID jniMethodbytesToHexStr = env->GetStaticMethodID(FCharUtilsClass, "bytesToHexStr", "([B)Ljava/lang/String;");
    jbyteArray tempBytes = CStr2Jba(env, (const char *)outbuf, outLen);
    env->CallStaticObjectMethod(FCharUtilsClass, jniMethodbytesToHexStr, tempBytes);
}


