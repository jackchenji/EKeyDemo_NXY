#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include "DesAlgo.h"
#include "froadLog.h"
#include "Utils.h"
#include "tmPrivMethod.h"

char* FROAD_AID = "A000000046582D552D4B65793031"; //OMA模式的AID
const char* insideMacKey = "5F4E58595F434552545F4D41434B4559";//与卡片交互的数据域MAC密钥
const char* insideDesEseKey = "5F4E58595F434552545F4174684B4559";//与卡片交互的数据域MAC密钥

unsigned char sessionKeys[16] = {0x00};
unsigned char resByte[2048] = {0x00};
unsigned char output[5];

static jboolean checkChannelOPenRes(JNIEnv *env, jobject channelObj);

extern "C"
JNIEXPORT void JNICALL Java_com_froad_ukey_jni_tmjni_setFroadAid(JNIEnv * env,
        jclass jniClass, jstring aid)
{
    if (aid == NULL)
    {
        return;
    }

    FROAD_AID = reinterpret_cast<char *>(aid);
}

extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_froad_ukey_jni_tmjni_encData(JNIEnv * env,
		jclass jniClass, jstring jmainKey, jstring jfac1, jstring jdata) {
	LOGE("LOG string from ndk encData.");
	//处理主密钥
	char mainKeys[16] = {0x00};
    char* mainKeyNew = (char*) mainKeys;
	char *mainKey = jstringToChar_Len(env, jmainKey);
	if (mainKey == NULL) { //使用默认主密钥
      	LOGE("mainKey is NULL");
        char* tk = hex2CharNoLen(insideMacKey, 32);
        memcpy(mainKeyNew, tk, 16);
    } else {
        int mainKeylen = charArray2Len(mainKey);
        LOGE("mainKey leng %d ", mainKeylen);
        char *mainKeyChar = hex2Char(mainKey);

        if (mainKeyChar == NULL) {
            LOGE("mainKey length error mainKeyChar is NULL...");
            return NULL;
        }
        mainKeylen = charArray2Len(mainKeyChar);
        LOGE("mainKey leng %d ", mainKeylen);
        if (mainKeylen != 16) {
            LOGE("mainKey length error...");
            return NULL;
        }

        //将主密钥保存在临时数组空间中
        memcpy(mainKeyNew, mainKeyChar + 2, mainKeylen);
    }

	//处理第一个分散因子
	char *gen1 = jstringToChar_Len(env, jfac1);
	if (gen1 == NULL) {
      	LOGE("gen1 is NULL");
      	return NULL;
      }
	int gen1len = charArray2Len(gen1);
	LOGE("gen1 leng %d ", gen1len);
	char *genChar1 = hex2Char(gen1);

	if (genChar1 == NULL) {
		LOGE("fac1 length error genChar1 is NULL...");
		return NULL;
	}
	int len = charArray2Len(genChar1);
	LOGE("genChar1 leng %d ", len);
	if (len != 8) {
		LOGE("fac1 length error...");
		return NULL;
	}

	unsigned char* sessionKey = (unsigned char*) sessionKeys;
    LOGE("divsfKeyFun start...");
    divsfKeyFun((unsigned char*) mainKeyNew, (unsigned char*) (genChar1 + 2), sessionKeys);
    //打印主密钥
    unsigned int i=0;
    LOGE("**********mainKey start...");
    for(i=0;i<16;i++)
    	LOGE("%02x ", mainKeyNew[i]);
    LOGE("**********mainKey end...");
    //打印分散因子
    LOGE("**********gen start...");
    for(i=0;i<10;i++)
    	LOGE("%02x ", genChar1[i]);
    LOGE("**********gen end...");
    //打印会话密钥
    LOGE("**********sessionKeys start...");
    for(i=0;i<16;i++)
    	LOGE("%02x ", sessionKeys[i]);
    LOGE("**********sessionKeys end...");

	//处理数据
	char *data = jstringToChar_Len(env, jdata);
	if (data == NULL) {
         LOGE("data is NULL");
         return NULL;
     }
	char *dataChar = hex2Char(data);
	if (dataChar == NULL) {
		LOGE("dataChar length error dataChar is NULL...");
		return NULL;
	}
	int lenData = charArray2Len(dataChar);
	if (lenData <= 0) {
		LOGE("dataChar length error...");
		return NULL;
	}

	char* dt = (char*) resByte;
    memcpy(dt, dataChar + 2, lenData);
    dt[lenData] = 0;

	LOGE("dataChar length is %d ", lenData);

	for(i=0;i<lenData;i++)
       LOGE("dt[%d] is %02X ", i, dt[i]);

	LOGE("DesEncrypt start...");
	DesEncrypt(sessionKey, (unsigned char*) dt, lenData);
    LOGE("DesEncrypt end...");
	for(i=0;i<lenData;i++)
		LOGE("%02x ", dt[i]);

	char *jc = (char*) dt;
	jbyteArray jba1 = set_class_bytes_value(env, jc, lenData);
	LOGE("jc is %s ", jc);
	return jba1;
}

extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_froad_ukey_jni_tmjni_decData(JNIEnv * env,
		jclass jniClass, jstring jmainKey, jstring jfac1, jstring jdata) {
	LOGE("LOG string from ndk decData.");
	//处理主密钥
	char mainKeys[16] = {0x00};
    char* mainKeyNew = (char*) mainKeys;
    char *mainKey = jstringToChar_Len(env, jmainKey);
    if (mainKey == NULL) {
        LOGE("mainKey is NULL");
        char* tk = hex2CharNoLen(insideMacKey, 32);
        memcpy(mainKeyNew, tk, 16);
    } else {
        int mainKeylen = charArray2Len(mainKey);
        LOGE("mainKey leng %d ", mainKeylen);
        char *mainKeyChar = hex2Char(mainKey);

        if (mainKeyChar == NULL) {
            LOGE("mainKey length error mainKeyChar is NULL...");
            return NULL;
        }
        mainKeylen = charArray2Len(mainKeyChar);
        LOGE("mainKey leng %d ", mainKeylen);
        if (mainKeylen != 16) {
            LOGE("mainKey length error...");
            return NULL;
        }

        //将主密钥保存在临时数组空间中
        memcpy(mainKeyNew, mainKeyChar + 2, mainKeylen);
    }

    //处理第一个分散因子
    char *gen1 = jstringToChar_Len(env, jfac1);
    if (gen1 == NULL) {
        LOGE("gen1 is NULL");
        return NULL;
      }
    int gen1len = charArray2Len(gen1);
    LOGE("gen1 leng %d ", gen1len);
    char *genChar1 = hex2Char(gen1);

    if (genChar1 == NULL) {
        LOGE("fac1 length error genChar1 is NULL...");
        return NULL;
    }
    int len = charArray2Len(genChar1);
    LOGE("genChar1 leng %d ", len);
    if (len != 8) {
        LOGE("fac1 length error...");
        return NULL;
    }

    unsigned char* sessionKey = (unsigned char*) sessionKeys;
    LOGE("divsfKeyFun start...");
    divsfKeyFun((unsigned char*) mainKeyNew, (unsigned char*) (genChar1 + 2), sessionKeys);
    //打印主密钥
    unsigned int i=0;
    LOGE("**********mainKey start...");
    for(i=0;i<16;i++)
        LOGE("%02x ", mainKeyNew[i]);
    LOGE("**********mainKey end...");
    //打印分散因子
    LOGE("**********gen start...");
    for(i=0;i<10;i++)
        LOGE("%02x ", genChar1[i]);
    LOGE("**********gen end...");
    //打印会话密钥
    LOGE("**********sessionKeys start...");
    for(i=0;i<16;i++)
        LOGE("%02x ", sessionKeys[i]);
    LOGE("**********sessionKeys end...");

    //处理数据
    char *data = jstringToChar_Len(env, jdata);
    if (data == NULL) {
         LOGE("data is NULL");
         return NULL;
     }
    char *dataChar = hex2Char(data);
    if (dataChar == NULL) {
        LOGE("dataChar length error dataChar is NULL...");
        return NULL;
    }
    int lenData = charArray2Len(dataChar);
    if (lenData <= 0) {
        LOGE("dataChar length error...");
        return NULL;
    }

    char* dt = (char*) resByte;
    memcpy(dt, dataChar + 2, lenData);
    dt[lenData] = 0;

    LOGE("dataChar length is %d ", lenData);

     for(i=0;i<lenData;i++)
        LOGE("%02x ", dt[i]);

    LOGE("DesDecrypt start...");
    DesDecrypt(sessionKey, (unsigned char*) dt, lenData);
    LOGE("DesDecrypt end...");
    for(i=0;i<lenData;i++)
        LOGE("%02x ", dt[i]);

    char *jc = (char*) dt;
    jbyteArray jba1 = set_class_bytes_value(env, jc, lenData);
    LOGE("jc is %s ", jc);
    return jba1;
}

extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_froad_ukey_jni_tmjni_mac(JNIEnv * env,
		jclass jniClass, jstring jmainKey, jstring jfac1, jstring jdata) {
	LOGE("LOG string from ndk mac.");
	//处理主密钥
	char mainKeys[16] = {0x00};
    char* mainKeyNew = (char*) mainKeys;
    char *mainKey = jstringToChar_Len(env, jmainKey);
    if (mainKey == NULL) {
       	LOGE("mainKey is NULL");
        char* tk = hex2CharNoLen(insideMacKey, 32);
        memcpy(mainKeyNew, tk, 16);
    } else {
        int mainKeylen = charArray2Len(mainKey);
        LOGE("mainKey leng %d ", mainKeylen);
        char *mainKeyChar = hex2Char(mainKey);
        if (mainKeyChar == NULL) {
            LOGE("mainKey length error mainKeyChar is NULL...");
            return NULL;
        }
        mainKeylen = charArray2Len(mainKeyChar);
        LOGE("mainKey leng %d ", mainKeylen);
        if (mainKeylen != 16) {
            LOGE("mainKey length error...");
            return NULL;
        }
        //将主密钥保存在临时数组空间中
        memcpy(mainKeyNew, mainKeyChar + 2, mainKeylen);
    }

   	//处理第一个分散因子
   	char *gen1 = jstringToChar_Len(env, jfac1);
   	if (gen1 == NULL) {
      	LOGE("gen1 is NULL");
      	return NULL;
    }
   	int gen1len = charArray2Len(gen1);
   	LOGE("gen1 leng %d ", gen1len);
   	char *genChar1 = hex2Char(gen1);

   	if (genChar1 == NULL) {
  		LOGE("fac1 length error genChar1 is NULL...");
   		return NULL;
   	}
   	int len = charArray2Len(genChar1);
   	LOGE("genChar1 leng %d ", len);
   	if (len != 8) {
   		LOGE("fac1 length error...");
   		return NULL;
   	}

   	unsigned char* sessionKey = (unsigned char*) sessionKeys;
    LOGE("divsfKeyFun start...");
    divsfKeyFun((unsigned char*) mainKeyNew, (unsigned char*) (genChar1 + 2), sessionKeys);
    //打印主密钥
    unsigned int i=0;
    LOGE("**********mainKey start...");
    for(i=0;i<16;i++)
    	LOGE("%02x ", mainKeyNew[i]);
    LOGE("**********mainKey end...");
    //打印分散因子
    LOGE("**********gen start...");
    for(i=0;i<10;i++)
    	LOGE("%02x ", genChar1[i]);
    LOGE("**********gen end...");
    //打印会话密钥
    LOGE("**********sessionKeys start...");
    for(i=0;i<16;i++)
     	LOGE("%02x ", sessionKeys[i]);
    LOGE("**********sessionKeys end...");

   	//处理数据
   	char *data = jstringToChar_Len(env, jdata);
   	if (data == NULL) {
        LOGE("data is NULL");
        return NULL;
    }
    char *dataChar = hex2Char(data);
    if (dataChar == NULL) {
    	LOGE("dataChar length error dataChar is NULL...");
    	return NULL;
    }
    int lenData = charArray2Len(dataChar);
    if (lenData <= 0) {
    	LOGE("dataChar length error...");
    	return NULL;
    }

  	char* dt = (char*) resByte;
    memcpy(dt, dataChar + 2, lenData);
    dt[lenData] = 0;

	LOGE("dataChar length is %d ", lenData);
    for(i=0;i<lenData;i++)
       LOGE("%02x ", dt[i]);

	output[4] = '\0';

	LOGE("getSessionMacFun start...");
	getSessionMacFun(sessionKey ,(unsigned char*) dt, lenData, output);
	LOGE("getSessionMacFun end...");
	for(i=0;i<lenData;i++)
        LOGE("mac[%d] is %02x ", i, output[i]);

	char *jc = (char*) output;
	jbyteArray jba1 = set_class_bytes_value(env, jc, 4);
	LOGE("jc is %s ", jc);

	return jba1;
}

extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_froad_ukey_jni_tmjni_insideMac(JNIEnv * env,
		jclass jniClass, jstring jdata) {
	LOGE("LOG string from ndk insideMac.");

    int i =0;
   	//处理数据
   	char *data = jstringToChar_Len(env, jdata);
   	if (data == NULL) {
        LOGE("data is NULL");
        return NULL;
    }
    char *dataChar = hex2Char(data);
    if (dataChar == NULL) {
    	LOGE("dataChar length error dataChar is NULL...");
    	return NULL;
    }
    int lenData = charArray2Len(dataChar);
    if (lenData <= 0) {
    	LOGE("dataChar length error...");
    	return NULL;
    }

  	char* dt = (char*) resByte;
    memcpy(dt, dataChar + 2, lenData);
    dt[lenData] = 0;

	LOGE("dataChar length is %d ", lenData);
    for(i=0;i<lenData;i++)
       LOGE("%02x ", dt[i]);

	output[4] = '\0';

	LOGE("getMacFun start...");
    char* tk = hex2CharNoLen(insideMacKey, 32);
	getMacFun((const char*) tk, (const char*)dt, lenData, reinterpret_cast<char *>(output));
	LOGE("getMacFun end...");
	for(i=0;i<lenData;i++)
        LOGE("mac[%d] is %02x ", i, output[i]);

    //TODO 暂时不做MAC处理，全置为0
    //memset(output, 0, 5);

	char *jc = (char*) output;
	jbyteArray jba1 = set_class_bytes_value(env, jc, 4);
	LOGE("jc is %s ", jc);

	return jba1;
}

extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_froad_ukey_jni_tmjni_deseseEncCbc(JNIEnv * env,
                                                                     jclass jniClass, jbyteArray jdata) {
    LOGE("LOG string from ndk deseseEncCbc.");

    int i =0;
    //处理数据
    char *dataChar = jBytes2Char(env, jdata);

    if (dataChar == NULL) {
        LOGE("dataChar length error dataChar is NULL...");
        return NULL;
    }
    int lenData = charArray2Len(dataChar);
    if (lenData <= 0) {
        LOGE("dataChar length error...");
        return NULL;
    }

    char* dt = (char*) resByte;
    memcpy(dt, dataChar + 2, lenData);
    dt[lenData] = 0;

    LOGE("dataChar length is %d ", lenData);
    for(i=0;i<lenData;i++)
        LOGE("%02x ", dt[i]);

    LOGE("DesEncryptCBC start...");
    char* tk = hex2CharNoLen(insideDesEseKey, 32);
    DesEncryptCBC((const char*) tk, dt, lenData);
    LOGE("DesEncryptCBC end...");
    jbyteArray jba1 = set_class_bytes_value(env, dt, lenData);

    return jba1;
}

/**
* 打开ADN通道
*/
extern "C"
JNIEXPORT void JNICALL Java_com_froad_ukey_jni_tmjni_openDeviceADN(
		JNIEnv * env, jclass obj, jobject smsHelper, jobject c1, jobject c2, jobject c3)
{
    LOGD("--debug-- Java_com_froad_ukey_jni_tmjni_openDeviceADN");
    jclass cls = env->FindClass("java/lang/Class");
    char* smsManagerName= "android.telephony.SmsManager";
    jstring strsn = strToJstring(env, (const char*) smsManagerName);
    jmethodID jniMethod = env->GetStaticMethodID(cls, "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
    jobject hexStringObj = env->CallStaticObjectMethod(cls, jniMethod, strsn);

    jclass smsHelperClass = env->GetObjectClass(smsHelper);
    jfieldID f = env->GetFieldID(smsHelperClass, "mClass", "Ljava/lang/Class;");
    env->SetObjectField(smsHelper, f, hexStringObj);
    jobject mClass = env->GetObjectField(smsHelper, f);


    jmethodID jniMethodgetMethod = env->GetMethodID(cls, "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
    if (jniMethodgetMethod == NULL)
    {
        LOGD("jniMethodgetMethod == NULL ");
    }

    jclass obj_clazz = env->FindClass("java/lang/Class");
    jobjectArray objArray = env->NewObjectArray(3, obj_clazz, NULL);
    env->SetObjectArrayElement(objArray, 0, c1);
    env->SetObjectArrayElement(objArray, 1, c2);
    env->SetObjectArrayElement(objArray, 2, c3);
    jobject updateMessageObj = env->CallObjectMethod(mClass, jniMethodgetMethod, env->NewStringUTF("updateMessageOnIcc"), objArray);
    jfieldID UpdateMessageOnIccFID = env->GetFieldID(smsHelperClass, "method2", "Ljava/lang/reflect/Method;");
    env->SetObjectField(smsHelper, UpdateMessageOnIccFID, updateMessageObj);

    jobject getAllMessagesObj = env->CallObjectMethod(mClass, jniMethodgetMethod, env->NewStringUTF("getAllMessagesFromIcc"), NULL);
    jfieldID getAllMessagesOnIccFID = env->GetFieldID(smsHelperClass, "method1", "Ljava/lang/reflect/Method;");
    env->SetObjectField(smsHelper, getAllMessagesOnIccFID, getAllMessagesObj);
}

/**
*ADN模式发送数据
*/
extern "C"
JNIEXPORT int JNICALL Java_com_froad_ukey_jni_tmjni_transmitHexDataADN(
		JNIEnv * env, jclass jniClass, jobject smsHelper, jstring str)
{
    LOGD("--debug-- Java_com_froad_ukey_jni_tmjni_transmitHexDataADN");

    jbyteArray hexStringByteArray = jHexStringToJbyteArray(env, str);
    if (hexStringByteArray == NULL)
    {
        LOGD("CallObjectMethod jHexStringToJbyteArray failed");
        return 1;
    }

    jclass smsHelperClass = env->GetObjectClass(smsHelper);
    if (smsHelperClass == NULL)
    {
        LOGD("GetObjectClass smsHelperClass failed");
        return 1;
    }
    jfieldID f1 = env->GetFieldID(smsHelperClass, "method2", "Ljava/lang/reflect/Method;");
    jobject _updateMessageOnIcc = env->GetObjectField (smsHelper, f1);
    jfieldID f2 = env->GetFieldID(smsHelperClass, "mSmsManager", "Landroid/telephony/SmsManager;");
    jobject mSmsManager = env->GetObjectField (smsHelper, f2);

    //获取SMSHelper类并调用_updateMessageOnIcc方法
    jclass ByteUtilClass = env->FindClass("com/froad/ukey/utils/np/ByteUtil");
    if (ByteUtilClass == NULL)
    {
         LOGD("FindClass com/froad/ukey/utils/np/ByteUtil failed");
         return 1;
    }
    jmethodID jniMethodDealUpdateMessageOnIcc = env->GetStaticMethodID(ByteUtilClass, "invokeMethod", "(Ljava/lang/reflect/Method;Ljava/lang/Object;II[B)Z");

    jboolean revRes = env->CallStaticBooleanMethod (ByteUtilClass, jniMethodDealUpdateMessageOnIcc,_updateMessageOnIcc, mSmsManager, 1, 2, hexStringByteArray);
    if (revRes) {
        LOGD("CallBooleanMethod _updateMessageOnIcc success");
        return 0;
    } else {
        LOGD("CallBooleanMethod _updateMessageOnIcc failed");
        return 1;
    }
}

/**
*获取数据
*/
extern "C"
JNIEXPORT jobject JNICALL Java_com_froad_ukey_jni_tmjni_method1ADN(
		JNIEnv * env, jclass jniClass, jobject smsHelper)
{
    LOGD("--debug-- Java_com_froad_ukey_jni_tmjni_method1ADN");

    jclass smsHelperClass = env->GetObjectClass(smsHelper);
    if (smsHelperClass == NULL)
    {
        LOGD("GetObjectClass smsHelperClass failed");
        return NULL;
    }
    jfieldID f1 = env->GetFieldID(smsHelperClass, "method1", "Ljava/lang/reflect/Method;");
    jobject _getAllMessagesFromIcc = env->GetObjectField (smsHelper, f1);
    jfieldID f2 = env->GetFieldID(smsHelperClass, "mSmsManager", "Landroid/telephony/SmsManager;");
    jobject mSmsManager = env->GetObjectField (smsHelper, f2);

    //获取SMSHelper类并调用_updateMessageOnIcc方法
    jclass ByteUtilClass = env->FindClass("com/froad/ukey/utils/np/ByteUtil");
    if (ByteUtilClass == NULL)
    {
         LOGD("FindClass com/froad/ukey/utils/np/ByteUtil failed");
         return NULL;
    }
    jmethodID jniMethodDealGetAllMessagesFromIcc = env->GetStaticMethodID(ByteUtilClass, "invokeMethod1", "(Ljava/lang/reflect/Method;Ljava/lang/Object;)Ljava/util/ArrayList;");
    if (jniMethodDealGetAllMessagesFromIcc == NULL) {
        LOGD("GetStaticMethodID jniMethodDealGetAllMessagesFromIcc is NULL");
        return NULL;
    }

    jobject res = env->CallStaticObjectMethod (ByteUtilClass, jniMethodDealGetAllMessagesFromIcc,_getAllMessagesFromIcc, mSmsManager);
    return res;
}

/**
*ADN模式判断是否有卡
*/
extern "C"
JNIEXPORT int JNICALL Java_com_froad_ukey_jni_tmjni_hasCardADN(
		JNIEnv * env, jclass jniClass, jstring str)
{
	LOGD("--debug-- Java_com_froad_ukey_jni_tmjni_hasCardADN");
	//获取java类SIMBaseManager的静态属性值TAR_CARD
    jclass context = env->FindClass("com/froad/ukey/manager/SIMBaseManager");
    if (context == NULL)
    {
        LOGD("FindClass SIMBaseManager failed");
        return 1;
    }
    jfieldID f = env->GetStaticFieldID(context, "TAR_CARD", "Ljava/lang/String;");
    if (f == NULL)
    {
        LOGD("GetStaticFieldID TAR_CARD failed");
        return 1;
    }
    jstring TAR_CARD = static_cast<jstring>(env->GetStaticObjectField(context, f));
    if (f == NULL)
    {
        LOGD("GetStaticObjectField TAR_CARD failed");
        return 1;
    }

    //判断短信内容str是否包含TAR_CARD标志
    jclass clsstring = env->FindClass("java/lang/String");
    if (clsstring == NULL)
    {
        LOGD("FindClass String failed");
        return 1;
    }
    jmethodID jniMethodContains = env->GetMethodID(clsstring, "contains", "(Ljava/lang/CharSequence;)Z");
    if (jniMethodContains == NULL)
    {
        LOGD("GetMethodID contains failed");
        return 1;
    }
    jboolean revRes = env->CallBooleanMethod (str, jniMethodContains, TAR_CARD);
    LOGD("revRes111：%d", revRes);
    if (! revRes) { //不包含TAR_CARD标志
        jbyteArray byteArrayObj = jHexStringToJbyteArray(env, str);
        if (byteArrayObj == NULL)
        {
            LOGD("call jHexStringToJbyteArray method failed");
            return 1;
        }


        //C代码实现移位操作
//        char* pduChar = Jba2CStr(env, byteArrayObj);
//        int pLen = (int) env->GetArrayLength(byteArrayObj); //获取长度
//        LOGD("leftShift before>>>pduChar:%s", pduChar);
//        // left shift 5 bit to get user data
//        if(!leftShift( pduChar, pLen, 5 ))
//        {
//            LOGD("leftShift failed");
//            return 1;
//        }
//        LOGD("leftShift after>>>pduChar:%s", pduChar);
//        jbyteArray hexStringObj = CStr2Jba (env, pduChar, pLen);

        //使用反射调用java将短信内容进行移位处理
        byteArrayObj = jDealShift(env, byteArrayObj, 5);

        //将移位后的短信内容以16进制字符串展开
        jclass FCharUtilsClass = env->FindClass("com/froad/ukey/utils/np/FCharUtils");
        jmethodID jniMethodShowResult16Str = env->GetStaticMethodID(FCharUtilsClass, "showResult16Str", "([B)Ljava/lang/String;");
        if (jniMethodShowResult16Str == NULL)
        {
            LOGD("GetStaticMethodID showResult16Str failed");
            return 1;
        }
        str = (jstring) (env->CallStaticObjectMethod(FCharUtilsClass, jniMethodShowResult16Str, byteArrayObj));
        if (str == NULL)
        {
            LOGD("CallStaticObjectMethod showResult16Str failed");
            return 1;
        }

        if (str != NULL) {
            //判断短信内容是否包含TAR_CARD标志
            revRes = env->CallBooleanMethod (str, jniMethodContains, TAR_CARD);
            LOGD("revRes222：%d", revRes);
            if (revRes) { //如果包含TAR_CARD标志，则说明短信内容需要移位
                //将java类com/froad/ukey/simchannel/SIMHelper的变量值isNeedShiftFID置为true
                jclass simHelperClass = env->FindClass("com/froad/ukey/simchannel/SIMHelper");
                if (simHelperClass == NULL)
                {
                    LOGD("FindClass com/froad/ukey/simchannel/SIMHelper failed");
                    return 1;
                }
                jfieldID isNeedShiftFID = env->GetStaticFieldID(simHelperClass, "isNeedShift", "Z");
                if (isNeedShiftFID == NULL)
                {
                    LOGD("GetStaticFieldID isNeedShift failed");
                     return 1;
                }
                jboolean isNeedShift = (jboolean) env->GetStaticBooleanField(simHelperClass, isNeedShiftFID);
                env->SetStaticBooleanField(simHelperClass, isNeedShiftFID, JNI_TRUE);
            }
        }
    }
    //判断短信内容是否包含E_UKEY值，如果包含则说明是可用的贴膜卡
    jclass SIMBaseManagerClass = env->FindClass("com/froad/ukey/manager/SIMBaseManager");
    if (SIMBaseManagerClass == NULL)
    {
          LOGD("FindClass com/froad/ukey/manager/SIMBaseManager failed");
          return 1;
    }
    jfieldID E_UKEYID = env->GetStaticFieldID(SIMBaseManagerClass, "E_UKEY", "Ljava/lang/String;");
    if (E_UKEYID == NULL)
    {
          LOGD("GetStaticFieldID E_UKEY failed");
          return 1;
    }
    jstring E_UKEY = static_cast<jstring>(env->GetStaticObjectField(SIMBaseManagerClass, E_UKEYID));
    if (E_UKEY == NULL)
    {
          LOGD("GetStaticObjectField E_UKEY failed");
          return 1;
    }

    revRes = env->CallBooleanMethod (str, jniMethodContains, E_UKEY);
    if (str != NULL && revRes) { //是可用的贴膜卡
        //获取版本号
        jmethodID jniMethodIndexOf = env->GetMethodID(clsstring, "indexOf", "(Ljava/lang/String;)I");
        if (jniMethodIndexOf == NULL)
        {
            LOGD("GetMethodID indexOf failed");
            return 1;
        }
        int strIndex = env->CallIntMethod (str, jniMethodIndexOf, E_UKEY);
        jmethodID jniMethodLength = env->GetMethodID(clsstring, "length", "()I");
        if (jniMethodLength == NULL)
        {
            LOGD("GetMethodID length failed");
            return 1;
        }
        int E_UKEY_Len = env->CallIntMethod (E_UKEY, jniMethodLength);
        jmethodID jniMethodSubstring = env->GetMethodID(clsstring, "substring", "(II)Ljava/lang/String;");
        if (jniMethodSubstring == NULL)
        {
            LOGD("GetMethodID jniMethodSubstring failed");
            return 1;
        }
        strIndex = strIndex + E_UKEY_Len;
        jstring StrVer = (jstring) env->CallObjectMethod (str, jniMethodSubstring, strIndex, (strIndex + 2));
        jfieldID fCardSmsVersion = env->GetStaticFieldID(SIMBaseManagerClass, "CardSmsVersion", "Ljava/lang/String;");
        env->SetStaticObjectField(SIMBaseManagerClass, fCardSmsVersion, StrVer);
        //保存卡片信息，返回java层解析
        jfieldID initCardInfoStr = env->GetStaticFieldID(SIMBaseManagerClass, "initCardInfoStr", "Ljava/lang/String;");
        env->SetStaticObjectField(SIMBaseManagerClass, initCardInfoStr, str);
        return 0;
    }
    return 1;
}

/**
*获取短信内容并判断是否需要移位处理
*/
extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_froad_ukey_jni_tmjni_myGetPDUADN(
		JNIEnv * env, jclass jniClass, jobject smsMessage)
{
	LOGD("--debug-- Java_com_froad_ukey_jni_tmjni_myGetPDUADN");
	jclass simHelperClass = env->FindClass("com/froad/ukey/simchannel/SIMHelper");
    if (simHelperClass == NULL)
    {
          LOGD("FindClass com/froad/ukey/simchannel/SIMHelper failed");
          return NULL;
    }
    jfieldID isNeedShiftFID = env->GetStaticFieldID(simHelperClass, "isNeedShift", "Z");
    if (isNeedShiftFID == NULL)
    {
          LOGD("GetStaticFieldID isNeedShift failed");
          return NULL;
    }
    jboolean isNeedShift = (jboolean) env->GetStaticBooleanField(simHelperClass, isNeedShiftFID);

	jclass msmMsg = env->GetObjectClass (smsMessage);
    if (msmMsg == NULL)
    {
          LOGD("GetObjectClass smsMessage failed");
          return NULL;
    }
    jmethodID getPdu = env->GetMethodID (msmMsg, "getPdu", "()[B");
    if (getPdu == NULL)
    {
          LOGD("GetMethodID getPdu failed");
          return NULL;
    }
    jbyteArray pdu = (jbyteArray) env->CallObjectMethod (smsMessage, getPdu);
    if (pdu == NULL)
    {
        LOGD("CallObjectMethod getPdu failed");
        jmethodID getMessageBody = env->GetMethodID (msmMsg, "getMessageBody", "()Ljava/lang/String;");
        if (getMessageBody == NULL)
        {
            LOGD("GetMethodID getMessageBody failed");
            return NULL;
        }
        jstring messageBody = static_cast<jstring>(env->CallObjectMethod(smsMessage, getMessageBody));
        if (messageBody == NULL)
        {
            LOGD("CallObjectMethod messageBody failed");
            return NULL;
        }
        pdu = jStringToJbyteArray(env, messageBody);
        if (pdu == NULL) {
            LOGD("pdu == NULL");
            return NULL;
        }
    }

    LOGD("isNeedShift:%d", isNeedShift);
    if (isNeedShift) { //如果需要移位则将获取到的内容进行移位处理
        pdu = jDealShift(env, pdu, 5);
    }
    return pdu;
}

/**
*ADN模式接收数据
*/
extern "C"
JNIEXPORT long JNICALL Java_com_froad_ukey_jni_tmjni_receiveDataListADN(
		JNIEnv * env, jclass jniClass, jstring str, jobject smsMessage, jobject list)
{
	LOGD("--debug-- Java_com_froad_ukey_jni_tmjni_receiveDataListADN");
	jclass smsMessageClass = env->GetObjectClass (smsMessage);
    if (smsMessageClass == NULL)
    {
          LOGD("GetObjectClass smsMessage failed");
          return 0;
    }
	jmethodID jniMethodGetIndexOnIcc = env->GetMethodID (smsMessageClass, "getIndexOnIcc", "()I");
    if (jniMethodGetIndexOnIcc == NULL)
    {
          LOGD("GetMethodID getIndexOnIcc failed");
          return 0;
    }
	int index = env->CallIntMethod (smsMessage, jniMethodGetIndexOnIcc);
    if (index == 2) {
        //第二条短信不做数据处理，只用作贴膜卡判断
        return 0;
    }

    if (str == NULL) {
        return 0;
    }
    jclass SIMBaseManagerClass = env->FindClass ("com/froad/ukey/manager/SIMBaseManager");
    if (SIMBaseManagerClass == NULL)
    {
          LOGD("FindClass com/froad/ukey/manager/SIMBaseManager failed");
          return 0;
    }
	jfieldID TAR_CARDFID = env->GetStaticFieldID (SIMBaseManagerClass, "TAR_CARD", "Ljava/lang/String;");
    if (TAR_CARDFID == NULL)
    {
          LOGD("GetStaticFieldID TAR_CARD failed");
          return 0;
    }
	jstring TAR_CARD = static_cast<jstring>(env->GetStaticObjectField(SIMBaseManagerClass, TAR_CARDFID));
    if (TAR_CARD == NULL)
    {
          LOGD("GetStaticObjectField TAR_CARD failed");
          return 0;
    }

	jclass StringClass = env->FindClass ("java/lang/String");
    if (StringClass == NULL)
    {
          LOGD("FindClass java/lang/String failed");
          return 0;
    }
	jmethodID jniMethodContains = env->GetMethodID (StringClass, "contains", "(Ljava/lang/CharSequence;)Z");
    if (jniMethodContains == NULL)
    {
        LOGD("GetMethodID contains failed");
        return 0;
    }
    jboolean revRes = env->CallBooleanMethod (str, jniMethodContains, TAR_CARD);
    if (!revRes) {//短信内容不包含TAR_CARD
        str = NULL;
        return 0;
    }
    jmethodID jniMethodIndexOf = env->GetMethodID (StringClass, "indexOf", "(Ljava/lang/String;)I");
    int indexOfRes = env->CallIntMethod (str, jniMethodIndexOf, TAR_CARD);
    jmethodID jniMethodSubstring = env->GetMethodID (StringClass, "substring", "(I)Ljava/lang/String;");
    str = (jstring) env->CallObjectMethod (str, jniMethodSubstring, indexOfRes);
    if (list == NULL) {
        LOGD("list is null");
        return 2;
    }
    jclass listClass = env->GetObjectClass (list);
    if (listClass == NULL) {
        LOGD("Java_com_froad_ukey_jni_tmjni_receiveDataListADN listClass is null");
        return 2;
    }
	jmethodID jniMethodAdd = env->GetMethodID (listClass, "add", "(Ljava/lang/Object;)Z");
    if (jniMethodAdd == NULL) {
        LOGD("Java_com_froad_ukey_jni_tmjni_receiveDataListADN jniMethodAdd is null");
        return 2;
    }
    env->CallBooleanMethod (list, jniMethodAdd, str);
    return 1;
}

/**
*封装ADN的Uri
*/
extern "C"
JNIEXPORT jobject JNICALL Java_com_froad_ukey_jni_tmjni_initUriADN(
		JNIEnv * env, jclass jniClass)
{
	LOGD("--debug-- Java_com_froad_ukey_jni_tmjni_initUriADN");
	char* icc = "content://icc/adn";
	jstring iccStr = strToJstring(env, (const char*)icc);
	jclass UriClass = env->FindClass ("android/net/Uri");
	jmethodID jmethodParse = env->GetStaticMethodID (UriClass, "parse", "(Ljava/lang/String;)Landroid/net/Uri;");
    jobject iccUri = env->CallStaticObjectMethod (UriClass, jmethodParse, iccStr);
	if (iccUri == NULL) {//如果Uri为空则换一个路径
	    icc = "content://icc0/adn";
        iccStr = strToJstring(env, (const char*)icc);
        iccUri = env->CallStaticObjectMethod (UriClass, jmethodParse, iccStr);
	}
	if (iccUri == NULL) {//如果Uri为空则换一个路径
	    icc = "content://icc1/adn";
        iccStr = strToJstring(env, (const char*)icc);
        iccUri = env->CallStaticObjectMethod (UriClass, jmethodParse, iccStr);
	}
    return iccUri;
}

/**
*通过联系人方式进行数据交互
*/
extern "C"
JNIEXPORT int JNICALL Java_com_froad_ukey_jni_tmjni_insetDataADN(
		JNIEnv * env, jclass jniClass
		, jobject smsHelper
		, jobject localCursor
		, jobject contentResolver
		, jobject localUri
		, jobject contentValues
		)
{
	LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN");
    if (smsHelper == NULL || localCursor == NULL || contentResolver == NULL || localUri == NULL || contentValues == NULL) {
        return 1;
    }
    LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111111");
    jclass localCursorClass = env->GetObjectClass (localCursor);
    LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111112");
    if (localCursorClass == NULL) {
        return 1;
    }
    jmethodID jmethodGetCount = env->GetMethodID (localCursorClass, "getCount", "()I");
    LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111113");
    if (jmethodGetCount == NULL) {
        return 1;
    }
    int count = env->CallIntMethod (localCursor, jmethodGetCount);
    LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111114");
    if (count == 0) {
        jclass contentResolverClass = env->GetObjectClass (contentResolver);
        LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111115");
        if (contentResolverClass == NULL) {
            return 1;
        }
        jmethodID jmethodInsert = env->GetMethodID (contentResolverClass, "insert", "(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;");
        LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111116");
        if (jmethodInsert == NULL) {
            return 1;
        }
        jobject insertUri = env->CallObjectMethod (contentResolver, jmethodInsert, localUri, contentValues);
        LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111117");
        //插入联系人失败不作处理，认为是成功
//        if (insertUri == NULL) {
//            return 1;
//        }
    } else {
        int uRes = JNI_FALSE;
        jclass contentResolverClass = env->GetObjectClass (contentResolver);
        LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111118");
        if (contentResolverClass == NULL) {
            return 1;
        }
        jmethodID jmethodInsert = env->GetMethodID (contentResolverClass, "insert", "(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;");
        LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111119");
        if (jmethodInsert == NULL) {
            return 1;
        }
        jobject insertUri = env->CallObjectMethod (contentResolver, jmethodInsert, localUri, contentValues);
        LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111120");
        if (insertUri != NULL) {
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111121");
            uRes = JNI_TRUE;
        } else {
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111122");
            uRes = JNI_FALSE;
        }
        //TODO 异常检测，如果发生异常则将uRes置为JNI_FALSE
        if (! uRes) {
            //获取相应的属性值
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111123");
            jclass smsHelperClass = env->GetObjectClass (smsHelper);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 11111124");
            if (smsHelperClass == NULL) {
                return 1;
            }
            jfieldID PhoneTagStrFID = env->GetFieldID (smsHelperClass, "PhoneTagStr", "Ljava/lang/String;");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111125");
            jstring PhoneTagStr = (jstring) env->GetObjectField (smsHelper, PhoneTagStrFID);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111126");
            jfieldID PhoneNumStrFID = env->GetFieldID (smsHelperClass, "PhoneNumStr", "Ljava/lang/String;");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111127");
            jstring PhoneNumStr = (jstring) env->GetObjectField (smsHelper, PhoneNumStrFID);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111128");

            jclass contentValuesClass = env->GetObjectClass (contentValues);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111129");
            jmethodID jmethodGetAsString = env->GetMethodID (contentValuesClass, "getAsString", "(Ljava/lang/String;)Ljava/lang/String;");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111130");
            jstring tagData = static_cast<jstring>(env->CallObjectMethod (contentValues, jmethodGetAsString, PhoneTagStr));
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111131");
            jstring numData = static_cast<jstring>(env->CallObjectMethod (contentValues, jmethodGetAsString, PhoneNumStr));
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111132");

            jmethodID jmethodIsLast = env->GetMethodID (localCursorClass, "isLast", "()Z");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111133");
            jboolean islast = env->CallBooleanMethod (localCursor, jmethodIsLast);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111134");
            if (! islast) {
                jmethodID jmethodMoveToNext = env->GetMethodID (localCursorClass, "moveToNext", "()Z");
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111135");
                jboolean next = env->CallBooleanMethod (localCursor, jmethodMoveToNext);
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111136");
            }

            //从数据库中获取联系人标签为name的值
            char* name = "name";
            jstring nameStr = strToJstring(env, (const char*)name);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111137");
            jmethodID jmethodGetColumnIndex = env->GetMethodID (localCursorClass, "getColumnIndex", "(Ljava/lang/String;)I");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111138");
            int cIndex = env->CallIntMethod (localCursor, jmethodGetColumnIndex, nameStr);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111139");
            jmethodID jmethodGetString = env->GetMethodID (localCursorClass, "getString", "(I)Ljava/lang/String;");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111140");
            jstring str1 = static_cast<jstring>(env->CallObjectMethod (localCursor, jmethodGetString, cIndex));
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111141");

            //从数据库中获取联系人标签为number的值
            char* number = "number";
            jstring numberStr = strToJstring(env, (const char*)number);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111142");
            cIndex = env->CallIntMethod (localCursor, jmethodGetColumnIndex, numberStr);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111143");
            jstring str2 = static_cast<jstring>(env->CallObjectMethod (localCursor, jmethodGetString, cIndex));
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111144");

            char* newTag = "newTag";
            jstring newTagStr = strToJstring(env, (const char*)newTag);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111145");
            char* newNumber = "newNumber";
            jstring newNumberStr = strToJstring(env, (const char*)newNumber);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111146");
            //交换contentValues中的数据
            jmethodID jmethodPut = env->GetMethodID (contentValuesClass, "put", "(Ljava/lang/String;Ljava/lang/String;)V");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111147");
            env->CallVoidMethod (contentValues, jmethodPut, newTagStr, tagData);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111148");
            env->CallVoidMethod (contentValues, jmethodPut, newNumberStr, numData);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111149");
            env->CallVoidMethod (contentValues, jmethodPut, PhoneTagStr, str1);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111150");
            env->CallVoidMethod (contentValues, jmethodPut, PhoneNumStr, str2);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111151");

            jmethodID jmethodUpdate = env->GetMethodID (contentResolverClass, "update", "(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I");
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111152");
            int updateRes = env->CallIntMethod (contentResolver, jmethodUpdate, localUri, contentValues, NULL, NULL);
            LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111153");

            if (updateRes <= 0) {
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111154");
                //更新失败后不做处理，认为成功
//                return 1;
            } else {
                jmethodID jmethodClear = env->GetMethodID (contentValuesClass, "clear", "()V");
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111155");
                env->CallVoidMethod (contentValues, jmethodClear);
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111156");
                env->CallVoidMethod (contentValues, jmethodPut, PhoneTagStr, tagData);
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111157");
                env->CallVoidMethod (contentValues, jmethodPut, PhoneNumStr, numData);
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111158");
            }
            if (localCursor != NULL) {
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111159");
                jmethodID jmethodClose = env->GetMethodID (localCursorClass, "close", "()V");
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111160");
                env->CallVoidMethod (localCursor, jmethodClose);
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111161");
                localCursor = NULL;
            }
        } else {
            if (localCursor != NULL) {
                jmethodID jmethodClose = env->GetMethodID (localCursorClass, "close", "()V");
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111162");
                env->CallVoidMethod (localCursor, jmethodClose);
                LOGE("--debug-- Java_com_froad_ukey_jni_tmjni_insetDataADN 1111163");
                localCursor = NULL;
            }
        }
    }
    return 0;
}

extern "C"
JNIEXPORT jstring JNICALL Java_com_froad_ukey_jni_tmjni_transHexOma(
		JNIEnv * env, jclass cls, jstring hexString, jobject sesDefaultHelper)
{
	jbyteArray hexStringObj = jHexStringToJbyteArray(env,hexString);
	if (hexStringObj == NULL)
	{
		LOGD("call jHexStringToJbyteArray method failed");
		return NULL;
	}

    jclass sesDefaultHelperClass = env->GetObjectClass (sesDefaultHelper);
    jfieldID channelField = env->GetFieldID (sesDefaultHelperClass, "channel", "Lorg/simalliance/openmobileapi/Channel;");
    if (channelField == NULL) {
        LOGD("get channelField field failed");
        return JNI_FALSE;
    }
    jobject channel = env->GetObjectField ( sesDefaultHelper, channelField);
	//获取并执行Java类中的transmit方法
	jclass channelClass = env->GetObjectClass (channel);
	jmethodID jniMethodTransmit = env->GetMethodID (channelClass, "transmit", "([B)[B");
	if (jniMethodTransmit == NULL)
	{
		LOGD("get transmit() method failed");
		return NULL;
	}
	jobject transObj = env->CallObjectMethod (channel, jniMethodTransmit, hexStringObj);
	if (transObj == NULL)
	{
		LOGD("call transmit() method failed");
		return NULL;
	}

	//获取并执行Java类中的showResult16Str方法
    jclass FCharUtilsClass = env->FindClass ("com/froad/ukey/utils/np/FCharUtils");
	jmethodID showResultMethod = env->GetStaticMethodID (FCharUtilsClass, "showResult16Str", "([B)Ljava/lang/String;");
	if (showResultMethod == NULL)
	{
		LOGD("get showResult16Str() method failed");
		return NULL;
	}
	jstring receiveStr = (jstring)env->CallStaticObjectMethod (FCharUtilsClass, showResultMethod, (jbyteArray)transObj);

	return receiveStr;
}

/**
 * 关闭通道
 */
 extern "C"
JNIEXPORT jboolean JNICALL Java_com_froad_ukey_jni_tmjni_close
		(JNIEnv* env, jclass jniClass, jobject sesDefaultHelper)
{
	//获取Service
	jclass sesDefaultHelperClass = env->GetObjectClass (sesDefaultHelper);
	jfieldID channelField = env->GetFieldID (sesDefaultHelperClass, "channel", "Lorg/simalliance/openmobileapi/Channel;");
	if (channelField == NULL) {
		LOGD("get channelField field failed");
		return JNI_FALSE;
	}

	jobject channel = env->GetObjectField ( sesDefaultHelper, channelField);
	if (channel == NULL)
	{
		LOGD("Channel is closed already");
		return JNI_TRUE;
	}

	//关闭channel
	jclass channelClass = env->GetObjectClass (channel);
	jmethodID jniMethodClose = env->GetMethodID (channelClass, "close", "()V");
	if (jniMethodClose == NULL)
	{
		LOGD("get close() method failed");
		return JNI_FALSE;
	}
	env->CallVoidMethod (channel, jniMethodClose);

//    //异常处理
//    if (handleException(env))
//    {
//        return JNI_FALSE;
//    }

	//获取SEService对象
	jfieldID serviceField = env->GetFieldID (sesDefaultHelperClass, "seService", "Lorg/simalliance/openmobileapi/SEService;");
	if (serviceField == NULL) {
		LOGD("get serviceField failed");
		return JNI_FALSE;
	}
	jobject serviceObj = env->GetObjectField (sesDefaultHelper, serviceField);
	if (serviceObj == NULL)
	{
		LOGD("Channel is closed already");
		return JNI_FALSE;
	}

	//调用Service的isConnected函数
	jclass serviceClass = env->GetObjectClass (serviceObj);
	jmethodID jniMethodIsConnect = env->GetMethodID (serviceClass, "isConnected", "()Z");
	if (jniMethodIsConnect == NULL)
	{
		LOGD("get isConnected() method failed");
		return JNI_FALSE;
	}
	jboolean isConnected = env->CallBooleanMethod (serviceObj, jniMethodIsConnect);
	if (isConnected == JNI_FALSE)
	{
		LOGD("Channel is closed already");
		return JNI_TRUE;
	}

	//调用Service的shutdown函数
	jmethodID jniMethodShutdown = env->GetMethodID (serviceClass, "shutdown", "()V");
	if (jniMethodShutdown == NULL)
	{
		LOGD("get jniMethodShutdown() method failed");
		return JNI_FALSE;
	}
	env->CallVoidMethod (serviceObj, jniMethodShutdown);

	//设置isOpen变量值
	jfieldID isOpenField = env->GetFieldID (sesDefaultHelperClass, "isOpen", "Z");
	if (isOpenField == NULL) {
		LOGD("get isOpenField failed");
		return JNI_FALSE;
	}
	env->SetBooleanField (sesDefaultHelper, isOpenField, JNI_FALSE);

	return JNI_TRUE;
}

