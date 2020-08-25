//
// Created by Macbook Pro on 2019/4/4.
//

#ifndef EKEYDEMO_AHRCU_BASE64UTILS_H
#define EKEYDEMO_AHRCU_BASE64UTILS_H

#endif //EKEYDEMO_AHRCU_BASE64UTILS_H
#define ENC		0x01
#define DEC		0x02
#define ECB		0x04
#define CBC		0x08
#define DES		0x10
#define TDES	0x20

#include <openssl/skf.h>
#include <jni.h>

void Base64Encode(const char *buffer, int length, char* output, bool newLine);
int Base64Decode(char *input, int length, char* output, bool newLine);

int HexStringToByteArray( char *in, unsigned int inLen, unsigned char *out);
void ByteArrayToHexString(unsigned char* in, unsigned int inLen, unsigned char* out);
long digest(JNIEnv* env, const char *alg,const unsigned char *inbuf, ULONG inbufLen, ULONG * outLen,unsigned char* outbuf);