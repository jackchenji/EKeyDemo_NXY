package com.cn.froad.ekeydemo_nxy.constant;

import com.froad.ukey.utils.np.FCharUtils;

/**
 * Created by FW on 2017/4/28.
 */
public class RSAUtils {

    /**
     * 封装RSA2048密钥
     * @param pKey
     * @return
     */
    public static byte[] packRsa2048PublicKey (String pKey) {
        if (pKey == null || (pKey.length() != 512)) {
            return null;
        }
        String startStr = "30820122300D06092A864886F70D01010105000382010F003082010A0282010100";
        String endStr = "0203010001";
        String pubKey = startStr + pKey + endStr;
        return FCharUtils.hexStrToBytes(pubKey);
    }

}
