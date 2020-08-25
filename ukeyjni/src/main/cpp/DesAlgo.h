/************************************************************************
*
*  Module name         : DesAlgo.h
*
*  Module description  :
*     This header file declares function prototypes for DES encryption
*     functions.
*
*  Project             :
*
*  Target platform     :
*
*  Compiler & Library  :
*
*  Author              : Richard Shen
*
*  Creation date       : 20 January, 1999
*
************************************************************************/
#ifndef DesAlgo_h
#define DesAlgo_h

#ifdef __cplusplus
extern "C"
{
#endif /* __cplusplus */

void divsfKeyFun(unsigned char *mainKey,unsigned char *gen, unsigned char *sessionKey);
void getSessionMacFun(unsigned char *sessionKey, unsigned char *input,int length, unsigned char *mac);
void  DesEncrypt(unsigned char *key, unsigned char *data, int length);
void  DesDecrypt(unsigned char *key, unsigned char *data, int length);

#ifdef __cplusplus
};
#endif /* __cplusplus */

#endif /* DesAlgo_h */
