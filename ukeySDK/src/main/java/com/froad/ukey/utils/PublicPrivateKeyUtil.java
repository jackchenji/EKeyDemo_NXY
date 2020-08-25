package com.froad.ukey.utils;

import org.bc.jce.provider.BCProvider;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;

/*
 *Created by chenji on 2020/3/25
 */ public class PublicPrivateKeyUtil {

     public  static ArrayList getPublicPrivateKey(){
        ArrayList   key=new ArrayList();
//            //生成SM2密钥对
//            // 获取SM2椭圆曲线的参数
         final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
// 获取一个椭圆曲线类型的密钥对生成器
         final KeyPairGenerator kpg;
         try {
             kpg = KeyPairGenerator.getInstance("EC", new BCProvider());
             // 使用SM2参数初始化生成器
             kpg.initialize(sm2Spec);

// 使用SM2的算法区域初始化密钥生成器
//            kpg.initialize(sm2Spec, new SecureRandom());
// 获取密钥对
             KeyPair keyPair = kpg.generateKeyPair();
             PrivateKey mPrivateKey = keyPair.getPrivate();
             PublicKey mPublicKey = keyPair.getPublic();
             key.add(mPrivateKey);
             key.add(mPublicKey);
         } catch (NoSuchAlgorithmException e) {
             e.printStackTrace();
         } catch (InvalidAlgorithmParameterException e) {
             e.printStackTrace();
         }

return  key;
     }


}
