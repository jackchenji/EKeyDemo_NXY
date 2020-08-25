package com.froad.ukey.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by FW on 2017/4/20.
 */
public class SHA {

    /**
     * 获取SHA1值
     * @param bytes
     * @return
     */
    public static byte[] getSha1(byte[] bytes){
        if (null == bytes || 0 == bytes.length){
            return null;
        }
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("SHA-1");
            mdTemp.update(bytes);

            byte[] md = mdTemp.digest();
            return md;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取SHA256值
     * @param bytes
     * @return
     */
    public static byte[] getSha256(byte[] bytes){
        if (null == bytes || 0 == bytes.length){
            return null;
        }
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("SHA-256");
            mdTemp.update(bytes);

            byte[] md = mdTemp.digest();
            return md;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
