package com.froad.ukey.utils;

import java.security.SecureRandom;
import java.util.Random;


/*
 *Created by chenji on 2020/3/25
 */ public class Utils {
    /**
     * 生成16字节 32位随机数
     * @return
     */
    public static String getGUID() {
        StringBuilder uid = new StringBuilder();
        //产生16位的强随机数
        Random rd = new SecureRandom();
        for (int i = 0; i < 32; i++) {
            //产生0-2的3位随机数
            uid.append(rd.nextInt(10));
        }
        return uid.toString();
    }


}
