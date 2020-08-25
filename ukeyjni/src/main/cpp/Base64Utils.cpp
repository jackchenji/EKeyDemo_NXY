//
// Created by Macbook Pro on 2019/4/4.
//

#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/buffer.h>
#include "Base64Utils.h"
#include "froadLog.h"
#include "Utils.h"

void Base64Encode(const char *buffer, int length, char* output, bool newLine)
{
    BIO *bmem = NULL;
    BIO *b64 = NULL;
    BUF_MEM *bptr;

    b64 = BIO_new(BIO_f_base64());
    if (!newLine) {
        BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    }
    bmem = BIO_new(BIO_s_mem());
    b64 = BIO_push(b64, bmem);
    BIO_write(b64, buffer, length);
    BIO_flush(b64);
    BIO_get_mem_ptr(b64, &bptr);
    BIO_set_close(b64, BIO_NOCLOSE);

    memcpy(output, bptr->data, bptr->length);
    output[bptr->length] = 0;
    BIO_free_all(b64);
}

// base64 解码
int Base64Decode(char *input, int length, char* output, bool newLine)
{
    BIO *b64 = NULL;
    BIO *bmem = NULL;
    int ret;

    b64 = BIO_new(BIO_f_base64());
    if (!newLine) {
        BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    }
    bmem = BIO_new_mem_buf(input, length);
    bmem = BIO_push(b64, bmem);
    ret = BIO_read(bmem, output, length);
    BIO_free_all(bmem);
    return ret;
}


int HexStringToByteArray( char *in, unsigned int inLen, unsigned char *out)
{
    if (!in || !in[0] || inLen%2) return 0;
    if (!out) return inLen/2;

    unsigned char nibble[2];
    unsigned int destlen = inLen/2;
    for (unsigned int i = 0; i < destlen; i ++)
    {
        nibble[0] = *in ++;
        nibble[1] = *in ++;
        for (unsigned int j = 0; j < 2; j ++)
        {
            if (nibble[j] >= 'A' && nibble[j] <= 'F')
                nibble[j] = nibble[j] - 'A' + 10;
            else if (nibble[j] >= 'a' && nibble[j] <= 'f')
                nibble[j] = nibble[j] - 'a' + 10;
            else if (nibble[j] >= '0' && nibble[j] <= '9')
                nibble[j] = nibble[j] - '0';
            else
                return 0;
        }
        out[i] =  nibble[0] << 4;        // Set the high nibble
        out[i] |= nibble[1];                // Set the low nibble
    }
    return destlen;
}


void ByteArrayToHexString(unsigned char* in, unsigned int inLen, unsigned char* out)
{
    int i;

    for (i = 0; i < inLen; i++)
    {
        out += sprintf((char*)out, "%02X", in[i]);
    }
    *out = 0x00;
}

long digest(JNIEnv *env, const char *alg,const unsigned char *inbuf, ULONG inbufLen, ULONG * outLen, unsigned char* outbuf){
    LOGD("digest alg : %s",alg);
    LOGD("digest inbuf : %s",inbuf);
    LOGD("digest inbufLen : %d",inbufLen);

    OpenSSL_add_all_digests();
    char* ret = NULL;
//    int inlen = strlen(reinterpret_cast<const char *>(inbuf));
    int inlen = inbufLen;
    unsigned int outlen = sizeof(outbuf);
    const EVP_MD *md;
    if (!(md = EVP_get_digestbyname(alg))) {
        LOGD("GmSSL找不到 %s",alg);
        return 0;
    }
    if (!EVP_Digest(inbuf, inlen, outbuf, &outlen, md, NULL)) {
        LOGD("GmSSL 调用 %s 算法失败",alg);
        return 1;
    }

    LOGD("digest outbuf : %s",outbuf);
    LOGD("digest outlen : %d",outlen);
    *outLen = outlen;

    return 2;
//    return reinterpret_cast<char *>(outbuf);
}