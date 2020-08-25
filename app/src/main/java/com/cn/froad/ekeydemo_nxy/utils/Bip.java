package com.cn.froad.ekeydemo_nxy.utils;

import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM3;
import com.froad.ukey.utils.np.SM4Util;
import com.froad.ukey.utils.np.TMKeyLog;

/*
 *Created by user on 2020/2/28
 */ public class Bip {
    private static String TAG="Bip";

     public  static  String  getSocketMainKey(String yangzhengma){     //获取通道主秘钥
         byte[]  sm3Byte= SM3.sm3Hash(FCharUtils.hexString2ByteArray(yangzhengma));   //使用卡片产生的8位（4字节）随机验证码进行SM3得到32字节摘要，
         byte[]  temp=new byte[sm3Byte.length/2];
         for(int i=0;i<sm3Byte.length/2;i++){
             temp[i]= (byte) (sm3Byte[i]^sm3Byte[sm3Byte.length/2+i]);      //对摘要进行前后16字节异或，得到16字节的通讯主密钥明文
         }
         TMKeyLog.i(TAG,"SM3摘要值："+FCharUtils.bytesToHexStr(sm3Byte));
         TMKeyLog.i(TAG,"16字节的通讯主密钥明文："+FCharUtils.bytesToHexStr(temp));
       return  FCharUtils.bytesToHexStr(temp);
     }


    public  static  String  getBipConnect(String carNo,String yangzhengma,String random){     //获取mac地址 参数为卡号 验证码 随机数
         String  instructCode="31";
         carNo="0833782600162602"; //卡片号码
         String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);
         String encrytData=instructCode+carNo+timeTag;
         String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBip(FCharUtils.hexString2ByteArray(random),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(getSocketMainKey(yangzhengma))));
         TMKeyLog.i(TAG,"mac数据："+mac);
         return "0011"+encrytData+mac;
    }



    public  static  String  getAdpuKey(String carNo,String random){     //获取apdu 会话秘钥

       String fensangyingzi=getSocketMainKey(carNo+random);
       String   adpuKey= FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(fensangyingzi), FCharUtils.hexString2ByteArray(Constants.soKey), SM4Util.ENCRYPT, false, 0));
        TMKeyLog.i("getapdu分散因子：",fensangyingzi );
        TMKeyLog.i("apdu通讯主秘钥：",adpuKey );
        return adpuKey;
    }

    public  static  String  getAdpuInstruct(String instruct,String key){     //获取apdu 指令
         String   adpuKey= FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(instruct), FCharUtils.hexString2ByteArray(key), SM4Util.ENCRYPT, false, 0));
         return adpuKey;
    }


    public  static  String  getAdpuInstructMac(String instruct,String key) {     //获取apdu 指令
        TMKeyLog.i("getAdpuInstructMac 指令码：", instruct);
        TMKeyLog.i("getAdpuInstructMac 秘钥：", key);
        String adpuMac = FCharUtils.bytesToHexStr(SM4Util.getInstance().dealApduMacInstruct(FCharUtils.hexString2ByteArray(instruct), FCharUtils.hexString2ByteArray(key),1));  //1代表加密
        TMKeyLog.i("卡片指令mac：", adpuMac);
        String head = FCharUtils.int2HexStr2((instruct + adpuMac).length() / 2);
        String result = head + instruct + adpuMac;
        return result;
    }


    public  static  Boolean jiaoyangmac(String yuanwen,String key,String mac) {     //处理结果
         String adpuMac = FCharUtils.bytesToHexStr(SM4Util.getInstance().dealApduMacInstruct(FCharUtils.hexString2ByteArray(yuanwen), FCharUtils.hexString2ByteArray(key),1));   //1代表加密
        if(adpuMac.equals(mac)){
            TMKeyLog.i(TAG,"mac校验成功");
            return  true;
        }else {
            TMKeyLog.i(TAG,"mac校验失败");
            return  false;
        }

     }



    public  static  String jiemiResult(String yuanwen,String key) {     //处理结果
         TMKeyLog.i(TAG,"服务器解密原文:"+yuanwen);
         String   result= FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(yuanwen), FCharUtils.hexString2ByteArray(key), SM4Util.DECRYPT, false, 0));
        return  result;
    }

}
