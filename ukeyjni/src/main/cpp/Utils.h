#include <jni.h>
#include <vector>
#include <cstring>
#include <iostream>

#ifndef EIDDEMO_UTILS_H_H
#define EIDDEMO_UTILS_H_H

jstring strToJstring(JNIEnv* env, const char* pat);

char* jstringToChar(JNIEnv*, jstring);

jbyteArray jHexStringToJbyteArray(JNIEnv* env, jstring jstr);

jbyteArray jStringToJbyteArray(JNIEnv* env, jstring jstr);

jstring jstringToJHexStr(JNIEnv* env, jstring jstr);

jstring jbyteArrayToJHexString(JNIEnv* env, jbyteArray jba);

jbyteArray jDealShift(JNIEnv* env, jbyteArray jba, int f);

jboolean handleException();

int charArray2Len(char*);

char* len2charArray(int);

char* hex2Char(char*);

char* hex2CharNoLen(const char* s, int sl);

char* jBytes2Char(JNIEnv* env, jbyteArray jBytes);

int leftShift( char* pData, int dataLen,  int shiftValue);

char* Jba2CStr(JNIEnv* env, jbyteArray jba);

jbyteArray CStr2Jba (JNIEnv* env, const char* cStr, int cLen);

char* jstringToChar_Len(JNIEnv* env, jstring jstr);

void divsfMacKeyFun(const char* input, char* output);

void getMacFun(const char* keyInput, const char* data, int srcLen, char* mac);

void DesEncryptCBC(const char* keyInput, char* data, int srcLen);

void printJavaHex(JNIEnv* env, char* outbuf, int outLen);

#endif //EIDDEMO_UTILS_H_H
