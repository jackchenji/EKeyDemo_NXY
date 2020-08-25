package com.micronet.api;

import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.Map;

public interface IVCardApiInterface {

    //证书类型定义
    int RSA = 0x00000001;
    int SM2 = 0x00000002;
    int SM2_ENC = 0x00000003;

    // HASH算法定义
    int MD5 = 0x00000001;
    int SHA1 = 0x00000002;
    int SHA256 = 0x00000003;
    int SHA384 = 0x00000004;
    int SHA512 = 0x00000005;
    int SM3 = 0x00000006;
	
	/**
     * 初始化
     * @param context
     */
    void init(Context context);

    /**
     * 判断是否有卡
     * @return
     */
    boolean hasVCard();

    /**
     * 获取X509证书信息
     * @return
     */
    Result getX509();
    
	/**
	 *    获取证书有效期
	 * @param certType
	 * @param hashType
	 * @return
	 */
    public String getCertTime(int certType, int hashType);

    /**
     * 获取卡号
     * @return
     */
    Result getCardNumber(boolean isUseBip);

    /**
     * 获取卡信息
     * @return
     */
    Result getCardInfo();

    /**
     * 获取签名信息
     * @param application
     * @param signSrcDataName
     * @param pinCode
     * @return
     */
    Result getSign(Application application, String signSrcDataName, String pinCode);
    
    /**
     * 获取签名信息
     * @param application
     * @param signSrcDataName
     * @param pinCode
     * @return
     */
    Result getSign(Application application, String signSrcDataName, String pinCode, int certType, int hashType);

    /**
     * 修改密码
     * @param oldPass1
     * @param newPass1
     * @return
     */
    Result setPin(String oldPass1, String newPass1,boolean isUseBip);

    /**
     * 获取密码重置时的加密数据
     * @return
     */
    String getCiphertext();

    /**
     * 密码重置
     * @param encryptedData
     * @return
     */
    Result resetPwd(String encryptedData);

    /**
     * 获取V盾签名功能开启状态
     * @return
     */
    com.micronet.bakapp.Result getVCardSignState();

    /**
     * 弹出STK菜单
     */
    void sendPopSTK();

    /**
     * 检测是否正常弹出STK菜单
     * @return
     */
    boolean isCanPopSTK();

    /**
     * 检测是否未修改过初始密码
     * @return
     */
    boolean isModPasswordOver();

    /**
     * 是否需要显示开启/关闭V盾自动关闭功能界面
     * @return
     */
    boolean isAddSignByHand();

    /**
     * 关闭通道
     */
    void close();

    /**
     * 弹出STK菜单，临时开启V盾签名功能
     * @param application
     */
    void callStkFunctionSetting(Application application);


    /**
     * 检测通道是否可用
     * @return
     */
    boolean checkChannel();

    /**
     * 获取错误状态码
     * @return
     */
    String getErrorStateCode();

    /**
     * 校验PIN
     * @param pinStr
     * @return
     */
    Result verifyPin (String pinStr);

    //====== 证书更新接口 ======
    /**
     * 初始化卡片
     * @param pinStr PIN码
     * @return
     */
    Result initCard (String pinStr);

    /**
     * 创建P10数据
     * @param certKeyTypes 密钥对类型
     * @param hashTypes
     * @return
     */
    Map<Integer, Result> createCertP10 (ArrayList<Integer> certKeyTypes, ArrayList<Integer> hashTypes);

    /**
     * 导入证书
     * @param certMap MAP key--证书类型，value--证书内容
     * @param encCertPriKey 加密证书私钥密文（使用加密证书私钥保护密钥做对称加密）
     * @param encProKey 加密证书私钥保护密钥密文（加密加密证书私钥的对称密钥，使用国密签名证书公钥加密处理）
     * @return
     */
    Map<Integer, Result> importCert(Map<Integer, byte[]> certMap, String encCertPriKey, String encProKey);

    //====== 指纹认证功能接口 ======

    /**
     * 检测是否支持指纹认证
     * @return
     */
    boolean checkFingerSupport ();

    /**
     * 获取指纹认证随机数
     * @return
     */
    Result getFingerRandom ();

    /**
     * 设置指纹公钥
     * @param pubKeyType 公钥类型：0--SM2，1--RSA1024&SHA1,2--RSA2048&SHA256
     * @param fingerPubKey
     * @return
     */
    Result setFingerPubKey (int pubKeyType, byte[] fingerPubKey, byte[] pinData);

    /**
     * 验证指纹签名数据
     * @param pubKeyType 公钥类型：0--SM2，1--RSA1024&SHA1,2--RSA2048&SHA256
     * @param fingerData
     * @return
     */
    Result verifyFinger (int pubKeyType, byte[] fingerData);


    /**
     * 初始化 bip通道 参数
     *
     */
    void setBipConfig(String datas, String typeflag, String yangzhengma);


}
