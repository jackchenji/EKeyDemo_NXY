package com.froad.ukey.utils.np;

import com.froad.ukey.utils.SM2;

import bip.Constants;

/**
 * Created by FW on 2017/7/4.
 */

public class SM2Util {

    private final static String TAG = "SM2Utils";
    public final static String SM2UserId = "31323334353637383132333435363738";

    /**
     * 验签
     * @param publicKey 公钥
     * @param sourceData 签名数据
     * @param isNeedAddPubKey 是否需要在签名数据签名添加公钥ZA数据
     * @return
     */
    public static boolean verifySign(byte[] publicKey, byte[] sourceData, byte[] signData, boolean isNeedAddPubKey)
    {
        return verifySign(FCharUtils.hexString2ByteArray(SM2UserId), publicKey, sourceData, signData, isNeedAddPubKey);
    }

    /**
     * 验签
     * @param userId
     * @param publicKey 公钥
     * @param sourceData 签名数据
     * @param isNeedAddPubKey 是否需要在签名数据签名添加公钥ZA数据
     * @return
     */
    public static boolean verifySign(byte[] userId, byte[] publicKey, byte[] sourceData, byte[] signData, boolean isNeedAddPubKey)
    {
        return SM2.getInstance().sm2VerifySign(userId, publicKey, sourceData, signData,isNeedAddPubKey);
    }

    /**
     * 签名
     * @param userId
     * @param privateKey 私钥
     * @param sourceData 签名数据
     * @param isNeedAddPubKey 是否需要在签名数据签名添加公钥ZA数据
     * @param pubKey 公钥
     * @param isCType 是否需要将java签名结果转成C签名格式
     * @return
     */
    public static byte[] sign(byte[] userId, byte[] privateKey, byte[] sourceData,boolean isNeedAddPubKey, byte[] pubKey, boolean isCType) {
        return SM2.getInstance().sign(userId, privateKey, sourceData, isNeedAddPubKey, pubKey, isCType);
    }

    /**
     * 签名
     * @param privateKey 私钥
     * @param sourceData 签名数据
     * @param isNeedAddPubKey 是否需要在签名数据签名添加公钥ZA数据
     * @param pubKey 公钥
     * @param isCType 是否需要将java签名结果转成C签名格式
     * @return
     */
    public static byte[] sign(byte[] privateKey, byte[] sourceData, boolean isNeedAddPubKey, byte[] pubKey, boolean isCType) {
        return sign(FCharUtils.hexString2ByteArray(SM2UserId), privateKey, sourceData, isNeedAddPubKey, pubKey, isCType);
    }

    /**
     * SM2加密
     * @param publicKey
     * @param data
     * @param resType 0--不需要处理，1--处理为c123,2--处理为c132
     * @return
     */
    public static byte[] encrypt(byte[] publicKey, byte[] data, int resType) {
        return SM2.getInstance().sm2Encrypt(publicKey, data, resType);
    }


    /**
     * SM2解密
     * @param privateKey
     * @param encryptedData
     * @return
     */
    public static byte[] decrypt(byte[] privateKey, byte[] encryptedData) {
        return SM2.getInstance().sm2Decrypt(privateKey, encryptedData);
    }

    public static byte[] dealSm2SignResultC(byte[] res) {
        return SM2.getInstance().dealSm2SignResultC(res);
    }



    public static void main (String[] arg0) {
        String sm2PubKey = "60C2F72F69B61A03C7EEAB63BC24CF1074B7F28B55AEAE588708D0B36F9F5AC401686AA575420F7C414D7E498C1D784888243DCBB61CD21A14B9503C4121074C";
        String sm2PriKey = "0DD59589246B681E520D23E421BCABBB60FDE47AB7341BF95F3232C9F7A68595";
        String sourceData = "BA72CBA55FD861D1D97B5297D3CEAF548068C7EEB54A717D05319EC90A2434D8";

//        String sm2EncData = FCharUtils.bytesToHexStr(SM2Util.encrypt(FCharUtils.hexString2ByteArray(sm2PubKey), FCharUtils.hexString2ByteArray(sourceData)));
//        System.out.println("sm2EncData:" + sm2EncData);
//        String sm2DecData = FCharUtils.bytesToHexStr(SM2Util.decrypt(FCharUtils.hexString2ByteArray(sm2PriKey), FCharUtils.hexString2ByteArray(sm2EncData)));
//        System.out.println("sm2DecData:" + sm2DecData);

        String signData = FCharUtils.bytesToHexStr(sign(FCharUtils.hexString2ByteArray(sm2PriKey), FCharUtils.hexString2ByteArray(sourceData), true, FCharUtils.hexString2ByteArray(sm2PubKey), false));
        System.out.println("signData:" + signData);
        signData = "3044022042FB07A787C1E57AC5B637E890FE845A9A69A0875BC499860A3311A1B09786270220108274C51C34C119264A6A909F69A6B594A86986BD66F431FE66B0628F0E17BA";
        boolean verifyRes = verifySign(FCharUtils.hexString2ByteArray(sm2PubKey), FCharUtils.hexString2ByteArray(sourceData), FCharUtils.hexString2ByteArray(signData), false);
        System.out.println("verifyRes:" + verifyRes);

    }
}
