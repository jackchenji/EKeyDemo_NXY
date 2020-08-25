#ifndef  _SKF_JNI_PRIV_H_
#define _SKF_JNI_PRIV_H_
#include <jni.h>
#include "froadLog.h"

void* get_jvm();
int jni_cache_object(JNIEnv * env, const char *cname, jobject *pjobj);

void release_chararray_bts(unsigned char *bts);

void set_long_bit(JNIEnv * env, jlongArray *jla, long *pl, int ct);
void set_class_object_value(JNIEnv * env, jobject jobj, const char *jname, const char *jsignature, jobject jvalue);
void set_class_byte_value(JNIEnv * env, jobject  cls, const char *mname, unsigned char v);
void set_class_int_value(JNIEnv * env, jobject  cls, const char *mname, int v);
void set_class_long_value(JNIEnv * env, jobject  cls, const char *mname, long v);
void set_class_object_jbyteArray_value(JNIEnv * env, jobject jobj, const char *jname, char* value, int valueLen);

void set_class_bool_value(JNIEnv * env, jobject  cls, const char *mname, bool v);
unsigned char* get_class_jchararray_bts(JNIEnv * env, jcharArray jchary, jlong len);
char* ConvertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray);

char* get_class_jbytearray_bts(JNIEnv * env, jbyteArray jlonga, jlong len);
char* get_class_jbytearray_0_bts(JNIEnv * env, jbyteArray jlonga, jlong len);
char *get_class_object_jbytearray_bts(JNIEnv * env, jobject jobj, const char* jsignature);
char *get_class_object_jbytearray_0_bts(JNIEnv * env, jobject jobj, const char* jsignature);
int* get_class_jintarray_bts(JNIEnv * env, jintArray jlonga, jlong len);
long* get_class_jlongarray_bts(JNIEnv * env, jlongArray jlonga, jlong len);

long get_class_long_value(JNIEnv * env, jobject  cls, const char *mname);

int get_class_int_value(JNIEnv * env, jobject  cls, const char *mname);
jobject    get_class_object_value(JNIEnv *env, jobject jobj, const char *names, const char *signature);
jcharArray set_class_jcharrray_bts(JNIEnv * env,  unsigned char *orgbt, unsigned long len);
jbyteArray set_class_jbytearray_bts(JNIEnv * env,  char *orgbt, unsigned long len);
jbyteArray set_class_byte_lable(JNIEnv * env, unsigned char *lables, int maxlen);
jbyteArray set_class_bytes_value(JNIEnv * env, const char *values, int valueLen);
jbyteArray get_class_bytearray_value(JNIEnv * env, jobject  cls, const char *mname);
jcharArray get_class_chararray_value(JNIEnv * env, jobject  cls, const char *mname);
jlongArray get_class_longarray_value(JNIEnv * env, jobject  cls, const char *mname);
jintArray  get_class_intarray_value(JNIEnv * env, jobject  cls, const char *mname);
#endif

