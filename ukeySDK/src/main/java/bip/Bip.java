package bip;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.utils.PublicPrivateKeyUtil;
import com.froad.ukey.utils.SM2;
import com.froad.ukey.utils.Utils;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.SM3;
import com.froad.ukey.utils.np.SM4Util;
import com.froad.ukey.utils.np.TMKeyLog;
import com.micronet.bakapp.utils.SM2Utils;

import org.bc.pqc.math.linearalgebra.CharUtils;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

/*
 *Created by chenji on 2020/2/28
 */ public class Bip {
    private static String TAG="Bip";

     public  static  String  getSocketMainKey(String yangzhengma){     //获取通道主秘钥
         if (yangzhengma.length() > 32) {
             yangzhengma = yangzhengma.substring(yangzhengma.length() - 32);
         } else {
             while (yangzhengma.length() < 32) {
                 yangzhengma += "20";
             }
         }
         TMKeyLog.i(TAG,"16字节的通讯主密钥明文："+yangzhengma);
       return  yangzhengma;
     }


    public  static  String  getBipConnect(String carNo,String socketKey,String random){     //获取mac地址 参数为卡号 验证码 随机数
         String  instructCode="31";
       //  carNo="0833782600162602"; //卡片号码
         String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);
         String encrytData=instructCode+carNo+timeTag+Constants.SDK_NAME;
         String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBipNew(FCharUtils.hexString2ByteArray(random),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(socketKey)));
         TMKeyLog.i(TAG,"random："+random);
         TMKeyLog.i(TAG,"encrytData："+encrytData);
         TMKeyLog.i(TAG,"socketKey："+socketKey);
         TMKeyLog.i(TAG,"mac数据："+mac);

         return FCharUtils.hexStr2LV_2(encrytData+mac);
    }


    public  static  String  getBipConnectForLast(String carNo,String socketKey,String random){     //获取mac地址 参数为卡号 验证码 随机数
        String head="";
        String  instructCode="12";
        String  baowencounter="1";


        String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);
        String encrytData=instructCode+carNo+timeTag+Constants.SDK_NAME;
        String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBipNew(FCharUtils.hexString2ByteArray(random),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(socketKey)));
        TMKeyLog.i(TAG,"random："+random);
        TMKeyLog.i(TAG,"encrytData："+encrytData);
        TMKeyLog.i(TAG,"socketKey："+socketKey);
        TMKeyLog.i(TAG,"mac数据："+mac);

        return FCharUtils.hexStr2LV_2(encrytData+mac);
    }



    public  static  String  getAdpuKey(String carNo,String random){     //获取apdu 会话秘钥

       String fensangyingzi=getSocketMainKey(carNo+random);
       String   adpuKey= FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(fensangyingzi), FCharUtils.hexString2ByteArray(Constants.soKey), SM4Util.ENCRYPT, false, 0));
        TMKeyLog.i("getapdu分散因子：",fensangyingzi );
        TMKeyLog.i("apdu通讯主秘钥：",adpuKey );
        return adpuKey;
    }

    public  static  String  getAdpuKeyforLast(String carNo,String timeFlag,String key,String count){     //获取ap

        String fensangyingzi=getSocketMainKey(carNo+timeFlag+count);  //卡号+时间戳+记数器
        String   adpuKey= FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(fensangyingzi), FCharUtils.hexString2ByteArray(key), SM4Util.ENCRYPT, false, 0));
        TMKeyLog.i("getapdu分散因子：",fensangyingzi );
        TMKeyLog.i("apdu通讯主秘钥：",adpuKey );
        return adpuKey;
    }


    public  static  String  getAdpuInstruct(String instruct,String key){     //获取apdu 指令
          TMKeyLog.i("apdu加密前数据：",instruct );
          TMKeyLog.i("apdu加密秘钥：",key );
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

    public  static  String  getAdpuInstructMacForLast(String data,String key,String adpuData) {     //获取apdu 指令
        TMKeyLog.i("getAdpuInstructMac data：", data);
        TMKeyLog.i("getAdpuInstructMac 秘钥：", key);
        String adpuMac = FCharUtils.bytesToHexStr(SM4Util.getInstance().dealApduMacInstruct(FCharUtils.hexString2ByteArray(data+adpuData), FCharUtils.hexString2ByteArray(key),1));  //1代表加密
        TMKeyLog.i("卡片指令mac：", adpuMac);
        String head = FCharUtils.int2HexStr2(("120001"+adpuData + adpuMac).length() / 2);
        String result = head + data+adpuData + adpuMac;
        return result;
    }



    public  static  Boolean jiaoyangmac(String yuanwen,String key,String mac) {     //处理结果
         String adpuMac = FCharUtils.bytesToHexStr(SM4Util.getInstance().dealApduMacInstruct(FCharUtils.hexString2ByteArray(yuanwen), FCharUtils.hexString2ByteArray(key),1));   //1代表加密

         TMKeyLog.i(TAG,"服务器mac==>"+mac);
         TMKeyLog.i(TAG,"计算出来的mac==>"+adpuMac);


        if(adpuMac.substring(0,6).equals(mac.substring(0,6))){
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



    /*新bip*/
    /**
     * 获取bip第一步指令
     * @returnS
     */
    public static String[] getBipFirstInstruct() {
        try {
            String[]  result=new String[3];
            String head;
            String second="33";

            String iv= Utils.getGUID();
            String sdkVersion= Constants.SDK_NAME;
            ArrayList key= PublicPrivateKeyUtil.getPublicPrivateKey();
            PrivateKey privateKey= (PrivateKey) key.get(0);  //获取私鑰
            PublicKey publicKey= (PublicKey) key.get(1);  //获取公钥
            String nationalPriavteKey= FCharUtils.bytesToHexStr(privateKey.getEncoded());
            String nationalPublicKey= FCharUtils.bytesToHexStr(publicKey.getEncoded());
            nationalPriavteKey=nationalPriavteKey.substring(72,136);// 截取数据后72到104位数据为私鑰
            nationalPublicKey=nationalPublicKey.substring(nationalPublicKey.length()-128);// 截取数据后128位数据为公钥
             String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);    //时间戳
            String encrytData=second+iv+sdkVersion+nationalPublicKey+timeTag;

        String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBipNew(FCharUtils.hexString2ByteArray(iv),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(Constants.communicatioKey)));
        String data=second+iv+sdkVersion+nationalPublicKey+timeTag+mac;
        head=FCharUtils.int2HexStr2(data.length()/2);  //头部表示长度

            result[0]=nationalPriavteKey;  //公钥
            result[1]=nationalPublicKey;
            result[2]=head+data;

        /*日志打印*/
        TMKeyLog.i(TAG,"iv:"+iv);
        TMKeyLog.i(TAG,"sdkVersion:"+sdkVersion);
        TMKeyLog.i(TAG,"nationalPublicKey:"+nationalPublicKey);
        TMKeyLog.i(TAG,"timeTag:"+timeTag);
        TMKeyLog.i(TAG,"encrytData:"+encrytData);
        TMKeyLog.i(TAG,"mac:"+mac);
        TMKeyLog.i(TAG,"私钥:"+nationalPriavteKey);
        TMKeyLog.i(TAG,"公钥:"+nationalPublicKey);
        TMKeyLog.i(TAG,"第一条数据指令:"+head+data);
        return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 解密第一條指令
     * @return
     */
    public static String[] getBipFirstInstructResult(String miyao,String data) {
        try {
            TMKeyLog.i(TAG,"解密秘钥:"+miyao);
            TMKeyLog.i(TAG,"待解密数据:"+data);
            String[]  result=new String[6];
           // String  decryptdata= FCharUtils.bytesToHexStr(SM2Util.decrypt(FCharUtils.hexString2ByteArray(miyao), SM2.dealSm2EncResultAdd(FCharUtils.hexString2ByteArray(data.substring(2)),2)));
            String  decryptdata= FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(data),FCharUtils.hexString2ByteArray(miyao),SM4Util.DECRYPT,false,0));
            TMKeyLog.i(TAG,"解密后数据===>:"+decryptdata);
           if(decryptdata.length()<42){
               return  null;
           }
            result[0]=decryptdata.substring(0,8);
            result[1]=decryptdata.substring(8,40);
            result[2]=decryptdata.substring(40,42);
            int length=FCharUtils.hexStr2Len(result[2]);
            result[3]=FCharUtils.hexStr2String(decryptdata.substring(42,42+length*2),FConstant.UTF_8);  //卡号
            result[4]=decryptdata.substring(42+length*2,42+length*2+8); //时间戳
            result[5]=decryptdata.substring(42+length*2+8,42+length*2+8+2); //手机触摸事件支持标志
            TMKeyLog.i(TAG,"bip协议版本号:"+ result[0]);
            TMKeyLog.i(TAG,"临时会话密钥:"+ result[1]);
            TMKeyLog.i(TAG,"卡号长度:"+ length);
            TMKeyLog.i(TAG,"卡号:"+ result[3]);
            TMKeyLog.i(TAG,"后台时间戳:"+ result[4]);
            TMKeyLog.i(TAG,"手机触摸事件支持标志:"+ result[5]);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /*新bip*/
    /**
     * 获取bip第一步指令
     * @return
     */
    public static String[] getBipFirstInstructNew(){
        try {
            String[]  result=new String[3];
            String head;
            String second="33";

            String iv= Utils.getGUID();
            String sdkVersion= Constants.SDK_NAME;

            String key=Utils.getGUID();
            String sm2EncrytData=Utils.getGUID(); //会话秘钥

            TMKeyLog.i(TAG,"sm2待加密秘钥:"+Constants.sm2PublicKey);
            TMKeyLog.i(TAG,"sm2待加密数据:"+sm2EncrytData);
            String tongxunnmiwen=FCharUtils.bytesToHexStr(SM2.getInstance().sm2Encrypt(FCharUtils.hexString2ByteArray(Constants.sm2PublicKey),FCharUtils.hexString2ByteArray(sm2EncrytData),1));



            String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);    //时间戳
            String houTaiVersion="0001";

            String encrytData=second+iv+sdkVersion+tongxunnmiwen+timeTag+houTaiVersion;
            String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBipNew(FCharUtils.hexString2ByteArray(iv),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(Constants.communicatioKey)));
            String data=second+iv+sdkVersion+tongxunnmiwen+timeTag+houTaiVersion+mac;
            head=FCharUtils.int2HexStr2(data.length()/2);  //头部表示长度


            result [0]=key;
            result[1]=sm2EncrytData;
            result[2]=head+data;


            /*日志打印*/
            TMKeyLog.i(TAG,"iv:"+iv);
            TMKeyLog.i(TAG,"sdkVersion:"+sdkVersion);
            TMKeyLog.i(TAG,"tongxunnmiwen:"+tongxunnmiwen);
            TMKeyLog.i(TAG,"encrytData:"+encrytData);
            TMKeyLog.i(TAG,"timeTag:"+timeTag);
            TMKeyLog.i(TAG,"mac:"+mac);
            TMKeyLog.i(TAG,"第一条数据指令:"+head+data);
            TMKeyLog.i(TAG,"第一条数据指令长度:"+result[2].length());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    /*新bip*/
    /**
     * 获取bip第一步指令
     * @return
     */
    public static String[] getBipInstructNew(){
        try {
            String[]  result=new String[3];
            String head;
            String second="33";

            String iv= Utils.getGUID();
            String sdkVersion= Constants.SDK_NAME;

            String key=Utils.getGUID();
            String sm2EncrytData=Utils.getGUID(); //会话秘钥

            TMKeyLog.i(TAG,"sm2待加密秘钥:"+Constants.sm2PublicKey);
            TMKeyLog.i(TAG,"sm2待加密数据:"+sm2EncrytData);
            String tongxunnmiwen=FCharUtils.bytesToHexStr(SM2.getInstance().sm2Encrypt(FCharUtils.hexString2ByteArray(Constants.sm2PublicKey),FCharUtils.hexString2ByteArray(sm2EncrytData),1));



            String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);    //时间戳
            String houTaiVersion="0001";

            String encrytData=second+iv+sdkVersion+tongxunnmiwen+timeTag+houTaiVersion;
            String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBipNew(FCharUtils.hexString2ByteArray(iv),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(Constants.communicatioKey)));
            String data=second+iv+sdkVersion+tongxunnmiwen+timeTag+houTaiVersion+mac;
            head=FCharUtils.int2HexStr2(data.length()/2);  //头部表示长度


            result [0]=key;
            result[1]=sm2EncrytData;
            result[2]=head+data;


            /*日志打印*/
            TMKeyLog.i(TAG,"iv:"+iv);
            TMKeyLog.i(TAG,"sdkVersion:"+sdkVersion);
            TMKeyLog.i(TAG,"tongxunnmiwen:"+tongxunnmiwen);
            TMKeyLog.i(TAG,"encrytData:"+encrytData);
            TMKeyLog.i(TAG,"timeTag:"+timeTag);
            TMKeyLog.i(TAG,"mac:"+mac);
            TMKeyLog.i(TAG,"第一条数据指令:"+head+data);
            TMKeyLog.i(TAG,"第一条数据指令长度:"+result[2].length());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取绑定bip指令
     * @return
     */
    public static String[] getBipBindInstruct(String random,String duanxingTag){
        try {
            String carNo="0833731277001214";
            String[]  result=new String[3];
            String head;
            String second="35";
            String sdkVersion= Constants.SDK_NAME;
            String houTaiVersion="0001";



            String phoneVersion="02";
            String appType="03";
            String sdkVersionmiyao=Utils.getGUID();
            String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);    //时间戳
            String pipeileixing="02";    //匹配项

            String lv=FCharUtils.int2HexStr(carNo.length())+FCharUtils.string2HexStr(carNo);

            TMKeyLog.i(TAG,"LV项:"+lv);
            String five=phoneVersion+appType+duanxingTag+sdkVersionmiyao+timeTag+pipeileixing+lv;

            TMKeyLog.i(TAG,"第五项明文:"+five);
            String tongxunnmiwen=FCharUtils.bytesToHexStr(SM2.getInstance().sm2Encrypt(FCharUtils.hexString2ByteArray(Constants.sm2PublicKey),FCharUtils.hexString2ByteArray(five),1));
            TMKeyLog.i(TAG,"第五项密文:"+tongxunnmiwen);



            String encrytData=second+sdkVersion+houTaiVersion+tongxunnmiwen;
            String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBipNew(FCharUtils.hexString2ByteArray(random),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(Constants.communicatioKey)));
            String data=second+sdkVersion+houTaiVersion+tongxunnmiwen+mac;
            head=FCharUtils.int2HexStr2(data.length()/2);  //头部表示长度


            result [0]="test";
            result[1]=sdkVersionmiyao;  //解密秘钥
            result[2]=head+data;


            /*日志打印*/
            TMKeyLog.i(TAG,"sdkVersion:"+sdkVersion);
            TMKeyLog.i(TAG,"tongxunnmiwen:"+tongxunnmiwen);

            TMKeyLog.i(TAG,"随机数:"+random);
            TMKeyLog.i(TAG,"解密秘钥:"+sdkVersionmiyao);
            TMKeyLog.i(TAG,"encrytData:"+encrytData);
            TMKeyLog.i(TAG,"mac:"+mac);



            TMKeyLog.i(TAG,"timeTag:"+timeTag);
            TMKeyLog.i(TAG,"第一条数据指令:"+head+data);
            TMKeyLog.i(TAG,"第一条数据指令长度:"+result[2].length());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取绑定bip指令
     * @return
     */
    public static String[] getBipBindInstructForTest(String random,String duanxingTag,String datas,String typeFlag){
        try {
            TMKeyLog.i(TAG,"连接类型:"+typeFlag);
            TMKeyLog.i(TAG,"连接数据:"+datas);
            String carNo="0833731277001214";
            carNo=datas;
            String[]  result=new String[3];
            String head;
            String second="35";
            String sdkVersion= Constants.SDK_NAME;
            String houTaiVersion="0001";



            String phoneVersion="02";
            String appType="03";
            String sdkVersionmiyao=Utils.getGUID();
            String timeTag=String.valueOf(System.currentTimeMillis()).substring(0,8);    //时间戳
            String pipeileixing="02";    //匹配项
             pipeileixing=typeFlag;    //匹配项

            String lv=FCharUtils.int2HexStr(carNo.length())+FCharUtils.string2HexStr(carNo);

            TMKeyLog.i(TAG,"LV项:"+lv);
            String five=phoneVersion+appType+duanxingTag+sdkVersionmiyao+timeTag+pipeileixing+lv;

            TMKeyLog.i(TAG,"第五项明文:"+five);
            String tongxunnmiwen=FCharUtils.bytesToHexStr(SM2.getInstance().sm2Encrypt(FCharUtils.hexString2ByteArray(Constants.sm2PublicKey),FCharUtils.hexString2ByteArray(five),1));
            TMKeyLog.i(TAG,"第五项密文:"+tongxunnmiwen);



            String encrytData=second+sdkVersion+houTaiVersion+tongxunnmiwen;
            String mac=FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMacBipNew(FCharUtils.hexString2ByteArray(random),FCharUtils.hexString2ByteArray(encrytData),FCharUtils.hexString2ByteArray(Constants.communicatioKey)));
            String data=second+sdkVersion+houTaiVersion+tongxunnmiwen+mac;
            head=FCharUtils.int2HexStr2(data.length()/2);  //头部表示长度


            result [0]="test";
            result[1]=sdkVersionmiyao;  //解密秘钥
            result[2]=head+data;


            /*日志打印*/
            TMKeyLog.i(TAG,"sdkVersion:"+sdkVersion);
            TMKeyLog.i(TAG,"tongxunnmiwen:"+tongxunnmiwen);

            TMKeyLog.i(TAG,"随机数:"+random);
            TMKeyLog.i(TAG,"解密秘钥:"+sdkVersionmiyao);
            TMKeyLog.i(TAG,"encrytData:"+encrytData);
            TMKeyLog.i(TAG,"mac:"+mac);



            TMKeyLog.i(TAG,"timeTag:"+timeTag);
            TMKeyLog.i(TAG,"第一条数据指令:"+head+data);
            TMKeyLog.i(TAG,"第一条数据指令长度:"+result[2].length());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
