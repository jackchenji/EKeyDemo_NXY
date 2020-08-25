package com.froad.ukey.jni;

import com.froad.ukey.utils.SM2;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.TMKeyLog;

public class GmSSL {

    private final static String TAG = "GmSSL";
    static {
        try {
            //导入动态库
            System.loadLibrary("tmjni");
            TMKeyLog.i(TAG, "loadLibrary tmjni");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public byte[] dnItem = null;

    public static class CERT_ALG_TYPE {
        public static int SHA1 = 1;
        public static int SHA256 = 2;
        public static int MD5 = 3;
    }

    public native byte[] digest(String algor, byte[] data);


    public byte RSAbackHash [] = new byte[64];
    public byte SM2backHash  [] = new byte[64];

    public byte RSAPkcs10 [] = new byte[2048];
    public byte SM2Pkcs10 [] = new byte[2048];
    public byte pemBytes [] = new byte[2048];

    // BOOL genRSAHash(  int iAlgType, BYTE LeField,UCHAR* bPublicKey,USHORT usPubKeyLen,BYTE * backHash)

    /**
     *
     * @param iAlgType 1--SHA1， 2--SHA256， 3--MD5
     * @param LeField
     * @param bPublicKey
     * @param usPubKeyLen
     * @return
     */
    public native long genRSAHash(int iAlgType,boolean isInit, String LeField, byte [] bPublicKey,int usPubKeyLen);
    // BOOL genRSAP10(char* RSAPkcs10, USHORT* RSAPkcs10Len,	BYTE bSignBuffer[512],	USHORT usSignLen)
    public native long genRSAP10 ( byte [] bSignBuffer,int usSignLen);

//    BOOL genSM2Hash(  int iAlgType, BYTE LeField,UCHAR* bPublicKey,	USHORT usPubKeyLen,BYTE * backHash);
//    BOOL genSM2P10( char* SM2Pkcs10, USHORT* SM2Pkcs10Len,	BYTE bSignBuffer[512],	USHORT usSignLen) ;
    public native long genSM2Hash(boolean isInit, String LeField, byte [] bPublicKey,int usPubKeyLen);

    public native long genSM2P10(byte [] bSignBuffer,int usSignLen);

    public native long convertPkcs7ToPemHex(byte [] bCerBuffer,int usCerLen);

    /**
     *
     * @param dn
     * @param tag
     * @return 0-参数错误，1-没有此tag，2-有tag
     */
    public long parseDn (byte[] dn, byte[] tag) {
        if (dn == null || tag == null) {
            return 0;
        }
        String dnStr = new String(dn);
        String tagStr = new String(tag);
        TMKeyLog.d(TAG, "parseDn>>>dn:" + dnStr + ">>>tag:" + tagStr);
        String[] dnStrs = dnStr.split(",");
        int dnStrsLen = dnStrs.length;
        String dnStrItem = "";
        for (int i = 0; i < dnStrsLen; i ++) {
            dnStrItem = dnStrs[i];
            tagStr = new String(tag);
            int dnStrItemLen = dnStrItem.length();
            for (int k = 0; k < dnStrItemLen; k++) {
                if (dnStrItem.charAt(k) != ' ') {
                    dnStrItem = dnStrItem.substring(k);
                    break;
                }
            }
            TMKeyLog.d(TAG, "parseDn>>>111dnStrItem:" + dnStrItem);
            if ((dnStrItem.toUpperCase().startsWith(tagStr.toUpperCase())) && dnStrItem.contains("=")) {
                tagStr = tagStr + "=";
                int temIndex = dnStrItem.indexOf("=");
                String temStr = dnStrItem.substring(0, temIndex);
                temStr = temStr.trim();
                dnStrItem = dnStrItem.substring(temIndex);
                dnStrItem = temStr + dnStrItem;
                TMKeyLog.d(TAG, "parseDn>>>222dnStrItem:" + dnStrItem + ">>>tagStr:" + tagStr);
                if (dnStrItem.toUpperCase().startsWith(tagStr.toUpperCase())) {
                    dnStrItem = dnStrItem.substring(dnStrItem.indexOf("=") + 1);
                    TMKeyLog.d(TAG, "parseDn>>>333dnStrItem:" + dnStrItem);
                    dnItem = dnStrItem.getBytes();
                    return 2;
                }
            }
        }
        return 1;
    }

    public byte[] sm2GetZSM (byte[] pubKey) {
        String sm2SignSupplement = FCharUtils.bytesToHexStr(pubKey);
        String smX = sm2SignSupplement.substring(0, 64);
        String smY = sm2SignSupplement.substring(64);

        byte[] zpading = SM2.getInstance().sm2GetZSM(FCharUtils.hexString2ByteArray(SM2Util.SM2UserId), smX, smY);
        TMKeyLog.d(TAG, "sm2GetZSM>>>zpading:" + FCharUtils.bytesToHexStr(zpading));
        return zpading;
    }
}
