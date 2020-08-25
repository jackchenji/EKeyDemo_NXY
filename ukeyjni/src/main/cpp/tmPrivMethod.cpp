#include "tmPrivMethod.h"
#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <cstring>


#undef LOG_TAG
#define LOG_TAG "TMKeyJNIPriv"

void release_chararray_bts(unsigned char *bts)
{
	if (bts)
	{
		delete []bts;
		bts = NULL;
	}
}

unsigned char *get_class_jchararray_bts(JNIEnv * env, jcharArray jchary, jlong len)
{
	LOGD("%s>>>len:%d", __FUNCTION__, len);
	unsigned short *orgbt = env->GetCharArrayElements(jchary, NULL);
	int i = 0;
	unsigned char *uchs = new unsigned char[len + 1];
	for (i = 0; i < len; ++i)
	{
		unsigned short sh = orgbt[i];
		unsigned char ch = sh;
		uchs[i] = ch;
	}
	uchs[len] = 0;
	
	return uchs;
}


char* ConvertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray)
{
	char *chars = NULL;
	jbyte *bytes;
	bytes = env->GetByteArrayElements(bytearray, 0);
	int chars_len = env->GetArrayLength(bytearray);
	chars = new char[chars_len + 1];
	memset(chars,0,chars_len + 1);
	memcpy(chars, bytes, chars_len);
	chars[chars_len] = 0;
	env->ReleaseByteArrayElements(bytearray, bytes, 0);
	return chars;
}

jcharArray set_class_jcharrray_bts(JNIEnv * env,  unsigned char *orgbt, unsigned long len)
{
	LOGD("%s>>>orgbt:%s", __FUNCTION__, orgbt);
	int i = 0;
	jcharArray ja = env->NewCharArray(len);
	for (i = 0; i < len; ++i)
	{
		unsigned short ss = orgbt[i];
		env->SetCharArrayRegion(ja, i, 1, (jchar *)&ss);
	}
	
	return ja;
}

jbyteArray set_class_jbytearray_bts(JNIEnv * env,  char *orgbt, unsigned long len)
{
	LOGD("%s>>>orgbt:%s", __FUNCTION__, orgbt);
	int i = 0;
	jbyteArray ja = env->NewByteArray(len);
	for (i = 0; i < len; ++i)
	{
		char ss = orgbt[i];
		env->SetByteArrayRegion(ja, i, 1, (jbyte *)&ss);
	}

	return ja;
}

jbyteArray set_class_byte_lable(JNIEnv * env, unsigned char *lables, int maxlen)
{
	LOGD("%s>>>lables:%s", __FUNCTION__, lables);
	int i = 0;
	bool secoend = false;
	for (i = 0 ; i < maxlen; ++i)
	{
		char ch = lables[i];
		if (ch == 0)
		{
			if (secoend)
			{
				break;
			}
			else
			{
				secoend = true;
			}
		}
		else
			secoend = false;
	}
	jbyteArray jba = env->NewByteArray(i);
	env->SetByteArrayRegion(jba, 0, i, (const jbyte *)lables);
	return jba;
}

void set_long_bit(JNIEnv * env, jlongArray *jla, long *pl, int ct)
{
	int i = 0;
	for (i = 0; i < ct; i++)
	{
		jlong jl = pl[i];
		env->SetLongArrayRegion(*jla, i, 1, (jlong*)&jl);
	}
}

jbyteArray set_class_bytes_value(JNIEnv * env, const char *values, int valueLen)
{
	LOGD("%s>>>value:%s", __FUNCTION__, values);
	jbyteArray joa = env->NewByteArray(valueLen);
	env->SetByteArrayRegion(joa, 0, valueLen, (jbyte*)values);
	LOGD("set_class_bytes_value>>>end");
	return joa;
}

void set_class_object_value(JNIEnv * env, jobject jobj, const char *jname, const char *jsignature, jobject jvalue)
{
	LOGD("%s>>>jname:%s>>>jsignature:%s", __FUNCTION__, jname, jsignature);
	jclass jc = env->GetObjectClass(jobj);
	if (jc == NULL)
	{
		LOGE("set_class_object_value get class error ");
		return ;
	}
	jfieldID jf = env->GetFieldID(jc, jname, jsignature);
	if (jf == NULL)
	{
		LOGE("set_class_object_value get GetFieldID error ");
		return ;
	}
	LOGE("SetObjectField run");
	env->SetObjectField(jobj, jf, jvalue);
	LOGE("SetObjectField run over");
}

void set_class_object_jbyteArray_value(JNIEnv * env, jobject jobj, const char *jname,
									   char* value, int valueLen)
{
	LOGD("%s>>>jname:%s", __FUNCTION__, jname);
	jbyteArray array = set_class_bytes_value(env, value, valueLen);
	LOGD("set_class_object_jbyteArray_value000");
	jclass jc = env->GetObjectClass(jobj);
	LOGD("set_class_object_jbyteArray_value111");
	if (jc == NULL)
	{
		LOGE("set_class_object_value get class error ");
		return ;
	}
	LOGD("set_class_object_jbyteArray_value222");
	jfieldID jf = env->GetFieldID(jc, jname, "[B");
	LOGD("set_class_object_jbyteArray_value333");
	if (jf == NULL)
	{
		LOGE("set_class_object_value get GetFieldID error ");
		return ;
	}
	LOGE("SetObjectField run");
	env->SetObjectField(jobj, jf, array);
	LOGE("SetObjectField run over");
}

int jni_cache_object(JNIEnv * env, const char *cname, jobject *pjobj)
{
	LOGD("%s>>>cname:%s", __FUNCTION__, cname);
	jclass jc = env->FindClass(cname);
	if (jc == NULL)
	{
		return 1;
	}
	
	jmethodID ctor = env->GetMethodID(jc, "<init>", "()V");
	jobject jo = env->NewObject(jc, ctor);
	
	if (jo == NULL)
	{
		return 2;
	}
	
	*pjobj = env->NewGlobalRef(jo);
	return 0;
}

void set_class_int_value(JNIEnv * env, jobject  cls, const char *mname, int v)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "I");
	env->SetIntField(cls, f,(jint)v);
}

long *get_class_jlongarray_bts(JNIEnv * env, jlongArray jlonga, jlong len)
{
	return (long *)(jlong *)env->GetLongArrayElements(jlonga, NULL);
}

int *get_class_jintarray_bts(JNIEnv * env, jintArray jlonga, jlong len)
{
	return (int *)(jint *)env->GetIntArrayElements(jlonga, NULL);
}

char *get_class_jbytearray_bts(JNIEnv * env, jbyteArray jlonga, jlong len)
{
	return (char *)(jbyte *)env->GetByteArrayElements(jlonga, NULL);
}
char *get_class_jbytearray_0_bts(JNIEnv * env, jbyteArray jlonga, jlong len)
{
	char* tm =  (char *)(jbyte *)env->GetByteArrayElements(jlonga, NULL);
	char * tms = new char[len + 1];
	memcpy(tms,tm, len);
	tms[len] = '\0';
	return tms;
}

char *get_class_object_jbytearray_bts(JNIEnv * env, jobject jobj, const char* jsignature)
{
	LOGD("%s>>>jsignature:%s", __FUNCTION__, jsignature);
	jbyteArray jba = get_class_bytearray_value(env, jobj, jsignature);
	int jbaLen = env->GetArrayLength(jba);
	char* jChars = (char *)(jbyte *)env->GetByteArrayElements(jba, NULL);
	return jChars;
}

char *get_class_object_jbytearray_0_bts(JNIEnv * env, jobject jobj, const char* jsignature)
{
	LOGD("%s>>>jsignature:%s", __FUNCTION__, jsignature);
	jbyteArray jba = get_class_bytearray_value(env, jobj, jsignature);
	int jbaLen = env->GetArrayLength(jba);
	char* jChars = (char *)(jbyte *)env->GetByteArrayElements(jba, NULL);
	char* reChars = new char[jbaLen + 1];
	memcpy(reChars, jChars, jbaLen);
	reChars[jbaLen] = '\0';
	return reChars;
}


void set_class_bool_value(JNIEnv * env, jobject  cls, const char *mname, bool v)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "Z");
	env->SetBooleanField(cls, f, v);
}

void set_class_long_value(JNIEnv * env, jobject  cls, const char *mname, long v)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "J");
	env->SetLongField(cls, f,(jint)v);
}

void set_class_byte_value(JNIEnv * env, jobject  cls, const char *mname, unsigned char v)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "B");
	env->SetByteField(cls, f,(jbyte)v);
}

long get_class_long_value(JNIEnv * env, jobject  cls, const char *mname)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "J");
	return (long)(env->GetLongField(cls, f));
}

int get_class_int_value(JNIEnv * env, jobject  cls, const char *mname)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "I");
	return (int)(env->GetIntField(cls, f));
}

jbyteArray get_class_bytearray_value(JNIEnv * env, jobject  cls, const char *mname)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "[B");
	return (jbyteArray)(env->GetObjectField(cls, f));
}

jcharArray get_class_chararray_value(JNIEnv * env, jobject  cls, const char *mname)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "[C");
	return (jcharArray)(env->GetObjectField(cls, f));
}

jlongArray get_class_longarray_value(JNIEnv * env, jobject  cls, const char *mname)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "[J");
	return (jlongArray)(env->GetLongField(cls, f));
}

jintArray get_class_intarray_value(JNIEnv * env, jobject  cls, const char *mname)
{
	LOGD("%s>>>mname:%s", __FUNCTION__, mname);
	jclass c = env->GetObjectClass(cls);
	jfieldID f = env->GetFieldID(c, mname, "[I");
	return (jintArray)(env->GetLongField(cls, f));
}

jobject get_class_object_value(JNIEnv *env, jobject jobj, const char *names, const char *signature)
{
	LOGD("%s>>>names:%s>>>signature:%s", __FUNCTION__, names, signature);
	jclass jc = env->GetObjectClass(jobj);
	if (jc == NULL)
	{
		LOGE("get_class_object_value error %s %s", names, signature);
		return NULL;
	}
	
	jfieldID jid = env->GetFieldID(jc, names, signature);
	if (jid == NULL)
	{
		LOGE("GetFieldID error %s %s", names, signature);
		return NULL;
	}
	
	return env->GetObjectField(jobj, jid);
}