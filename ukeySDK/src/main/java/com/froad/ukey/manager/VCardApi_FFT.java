package com.froad.ukey.manager;

import android.app.Application;
import android.content.Context;
import android.os.ConditionVariable;
import android.text.TextUtils;

import com.froad.ukey.bean.CardInfo;
import com.froad.ukey.bean.CardResult;
import com.froad.ukey.constant.FConstant;
import com.froad.ukey.constant.ResultStateCode;
import com.froad.ukey.http.HttpConstants;
import com.froad.ukey.http.HttpUtil;
import com.froad.ukey.http.bean.AuthRequestParams;
import com.froad.ukey.http.bean.AuthResponseResult;
import com.froad.ukey.http.bean.AuthResultParser;
import com.froad.ukey.http.bean.GetCosUpdateBean;
import com.froad.ukey.http.exception.AuthError;
import com.froad.ukey.http.interf.OnResultListener;
import com.froad.ukey.http.interf.Parser;
import com.froad.ukey.interf.CosUpdateCallBack;
import com.froad.ukey.interf.OpenChannelCallBack;
import com.froad.ukey.simchannel.imp.SMSHelper;
import com.froad.ukey.utils.CRC16Util;
import com.froad.ukey.utils.SystemUtil;
import com.froad.ukey.utils.np.AppExecutors;
import com.froad.ukey.utils.np.CardConnState;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.SM4Util;
import com.froad.ukey.utils.np.TMKeyLog;
import com.google.gson.Gson;
import com.micronet.api.IVCardApiInterface;
import com.micronet.api.Result;

import org.bouncycastle.asn1.x509.X509CertificateStructure;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.cert.X509Certificate;

import bip.BipEntity;
import bip.BipInterf;
import bip.BipManager;
import bip.Constants;

/**
 * Created by FW on 2017/3/30.
 */
public class VCardApi_FFT implements IVCardApiInterface {

    private static final String TAG = FConstant.LOG_TAG + "VCardApi_FFT";

    private static VCardApi_FFT mVCardApiFFT;
    private TmKeyManager mTmKeyManager;
    private Context mContext;
    private boolean hasCard;
    private String sdfStr = "yyyy-MM-dd HH:mm:ss";
    private SimpleDateFormat sdf = new SimpleDateFormat(sdfStr);
    private ConditionVariable mConditionVariable = new ConditionVariable();  //线程锁
    private boolean omaLock = false;
    private boolean adnLock = false;

    private String cos_atr = ""; //临时保存卡片响应ATR
    private String cos_new_atr = ""; //获取更新指令数据响应最新的ATR
    private String cos_random = ""; //临时保存卡片COS更新使用的随机数
    private String cos_pin = ""; //临时保存卡片COS更新使用的PIN
    private String filePath = "";
    private String fileName = "";
    public static BipEntity mBipEntity; //bip实体参数


    public static class SignTypeData {
        //证书类型
        public final static int CER_TYPE_RSA1024 = 1;//1024签名方式
        public final static int CER_TYPE_RSA2048 = 2;// 2048签名方式
        public final static int CER_TYPE_SM2_SIGNCER = 3;//签名证书
        public final static int CER_TYPE_SM2_ENCCER = 4;//加密证书
        //签名方式
        public final static int SIGN_TYPE_RSA1024_HASH = 9;//1024签名方式
        public final static int SIGN_TYPE_RSA2048_HASH = 0x0A;// 2048签名方式
        public final static int SIGN_TYPE_SM2_SIGNCER_HASH = 0x0B;//签名证书
        public final static int SIGN_TYPE_RSA1024 = 1;//1024签名方式  带原文
        public final static int SIGN_TYPE_RSA2048 = 2;// 2048签名方式  带原文
        public final static int SIGN_TYPE_SM2_SIGNCER = 3;//签名证书  带原文

        //摘要填充规则
        public final static String ABSTRACT_RULE_HASH = "0";//普通hash
        public final static String ABSTRACT_RULE_SHA1 = "1";//SHA1
        public final static String ABSTRACT_RULE_SHA256 = "2";//SHA256

        //摘要算法
        public final static String ABSTRACT_TYPE_SHA1 = "1";//SHA1
        public final static String ABSTRACT_TYPE_SHA256 = "2";//SHA256
        public final static String ABSTRACT_TYPE_SM3 = "3";//SM3
    }

    public VCardApi_FFT() {
    }

    public static VCardApi_FFT getInstance() {
        if (mVCardApiFFT == null) {
            mVCardApiFFT = new VCardApi_FFT();
        }
        return mVCardApiFFT;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        this.mContext = context;
        filePath = mContext.getFilesDir() + "/localcos";
        fileName = "cosFile.txt";

            TMKeyLog.d(TAG, "mTmKeyManager is null");
            mTmKeyManager = TmKeyManager.getInstance();
            AppExecutors.getAppExecutors().postScheduledExecutorThread(new Runnable() {
                @Override
                public void run() {
                    openChannel(true);
                }
            });

        TMKeyLog.d(TAG, "mTmKeyManager:" + mTmKeyManager.hashCode());
    }

    /**
     * 判断是否有卡
     *
     * @return
     */
    public boolean hasVCard() {
        if (mTmKeyManager == null) {
            return false;
        }
        if (hasCard) {
            return true;
        }
        boolean tHasCard = mTmKeyManager.hasCard();
        TMKeyLog.d(TAG, "hasVCard end>>>tHasCard:" + tHasCard);
        return tHasCard;
    }

    /**
     * 获取X509证书信息
     *
     * @return
     */
    public Result getX509() {
        return getX509(RSA, SHA1);
    }

    /**
     * @param certType 证书类型
     * @param hashType hash类型
     * @return
     */
    private Result getX509(int certType, int hashType) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        if ((certType == RSA) && (hashType == SHA1)) {//RSA1024
            return getRSACer(SignTypeData.CER_TYPE_RSA1024);
        } else if ((certType == RSA) && (hashType == SHA256)) {//RSA2048
            return getRSACer(SignTypeData.CER_TYPE_RSA2048);
        } else if ((certType == SM2) && (hashType == SM3)) {//SM2
            return getSm2Cer(SignTypeData.CER_TYPE_SM2_SIGNCER);
        }
        return getRSACer(SignTypeData.CER_TYPE_RSA1024);
    }

    /**
     * 获取RSA证书
     *
     * @param t
     * @return
     */
    private Result getRSACer(int t) {
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getRSACer>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB, "", "", "通道打开失败");
        }
        X509Certificate x509Certificate = mTmKeyManager.getCerInfo(t);
        closeChannel_OMA();
        if (x509Certificate != null) {
            Result result = new Result(true, ResultStateCode.STATE_OK, "调用成功", x509Certificate);
            String startTime = new SimpleDateFormat("yyyy-MM-dd").format(x509Certificate.getNotBefore());
            String endTime = new SimpleDateFormat("yyyy-MM-dd").format(x509Certificate.getNotAfter());
            result.setCertStartTime(startTime);
            result.setCertendTime(endTime);
            result.setCertTime(startTime + ":" + endTime);
            return result;
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1001, "", "", "获取X509为空。");
    }

    /**
     * 获取国密证书
     *
     * @return
     */
    private Result getSm2Cer(int t) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getSm2Cer>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB,"通道打开失败");
        }
        X509CertificateStructure x509CertificateStructure = mTmKeyManager.getCerInfo_Sm2(t, true);
        closeChannel_OMA();
        if (x509CertificateStructure != null) {
            TMKeyLog.d(TAG, "x509CertificateStructure is not null");
            Result result = new Result(true, ResultStateCode.STATE_OK, "调用成功", "");
            result.setIssuerDN(x509CertificateStructure.getIssuer().toString());
            result.setSubjectDN(x509CertificateStructure.getSubject().toString());
            String startTime = new SimpleDateFormat("yyyy-MM-dd").format(x509CertificateStructure.getStartDate().getDate());
            String endTime = new SimpleDateFormat("yyyy-MM-dd").format(x509CertificateStructure.getEndDate().getDate());
            result.setCertStartTime(startTime);
            result.setCertendTime(endTime);
            result.setCertTime(startTime + ":" + endTime);
            Map map = splitSubjectDN(x509CertificateStructure.getSubject().toString());
            String[] value = ((String) map.get("CN")).split("@");
            result.setSerialNumber(value[1]);
            return result;
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1001, "获取X509为空。");
    }

    @Override
    public String getCertTime(int certType, int hashType) {
        TMKeyLog.d(TAG, "getCertTime>>>certType:" + certType + ">>>hashType:" + hashType);
        Result result = getX509(certType, hashType);
        return result.getCertTime();
    }

    /**
     * 获取卡号
     *
     * @return
     */
    public Result getCardNumber(boolean isUseBip) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getCardNumber>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB,"通道打开失败");
        }
        String cardNo="";
        BipManager.isUseBip=isUseBip;  //是否使用Bip模式
        cardNo = mTmKeyManager.getCardSN(false);  //不使用bip模式
        TMKeyLog.d(TAG, "普通通道模式获取到的数据:" + cardNo);
        closeChannel_OMA();
        if (cardNo == null || "".equals(cardNo)) {
            return new Result(false, "1000", "调用失败接口异常" + cardNo, "");
        }else {
        return new Result(true, ResultStateCode.STATE_OK, "调用成功", cardNo);
        }
    }

    /**
     * 获取卡信息
     *
     * @return
     */
    public Result getCardInfo() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        String cosVersion = "";
        String appVersion = "";
        String interfaceVersion = "";

        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getCardInfo>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB, "通道打开失败", "", "", "");
        }
        CardInfo ci = mTmKeyManager.getCardInfo();
        closeChannel_OMA();
        if (ci != null) {
            cosVersion = ci.getCosVersion();
        }
        String type = "O";
        int transType = mTmKeyManager.getCardConState();
        if (transType == CardConnState.CARDCONN_SUCCESS_OMA) {
            type = "O";//OMA
        } else if (transType == CardConnState.CARDCONN_SUCCESS_UICC) {
            type = "U";//UICC
        } else if (transType == CardConnState.CARDCONN_SUCCESS_SMS_CEN) {
            type ="SP";//SMSP
        } else if (transType == CardConnState.CARDCONN_SUCCESS_SMS) {
            type = "S";//SMS
        } else if (transType == CardConnState.CARDCONN_SUCCESS_ADN) {
            if (SMSHelper.isNeedShift) {//需要移位是C网
                type = "C";//ADN C网
            } else {
                type = "G";//ADN G网
            }
        }
        appVersion = mTmKeyManager.getSDKVersion() + "_" + type;
        interfaceVersion = mTmKeyManager.getSDKVersion() + "_" + type;
        return new Result(true, ResultStateCode.STATE_OK, "调用成功", cosVersion, appVersion, interfaceVersion);
    }

    public Result getSign(Application application, String signSrcDataName, String pinCode) {
        return getSign(application, signSrcDataName, pinCode, RSA, SHA1);
    }

    public Result getSign(Application application, String signSrcDataName, String pinCode, int certType, int hashType) {
        TMKeyLog.d(TAG, "getSign>>>pinCode:" + pinCode + ">>>certType:" + certType + ">>>hashType:" + hashType);
        if (certType == RSA && hashType == SHA1) {
            return getSign(application, signSrcDataName, SignTypeData.SIGN_TYPE_RSA1024_HASH, SignTypeData.ABSTRACT_RULE_SHA1, SignTypeData.ABSTRACT_TYPE_SHA1, pinCode);
        } else if (certType == RSA && hashType == SHA256) {
            return getSign(application, signSrcDataName, SignTypeData.SIGN_TYPE_RSA2048_HASH, SignTypeData.ABSTRACT_RULE_SHA256, SignTypeData.ABSTRACT_TYPE_SHA256, pinCode);
        } else if (certType == SM2 && hashType == SM3) {
            return getSign(application, signSrcDataName, SignTypeData.SIGN_TYPE_SM2_SIGNCER_HASH, SignTypeData.ABSTRACT_RULE_SHA1, SignTypeData.ABSTRACT_TYPE_SM3, pinCode);
        }
        return getSign(application, signSrcDataName, SignTypeData.SIGN_TYPE_RSA1024_HASH, SignTypeData.ABSTRACT_RULE_SHA1, SignTypeData.ABSTRACT_TYPE_SHA1, pinCode);
    }

    /**
     * 修改密码
     *
     * @param oldPass1
     * @param newPass1
     * @param isUseBip
     * @return
     */
    @Override
    public Result setPin(String oldPass1, String newPass1, boolean isUseBip) {
        BipManager.isUseBip=isUseBip; //前端传过来是否使用bip
        return setPin(oldPass1, newPass1, true, true);
    }


    /**
     * 获取签名信息
     *
     * @param application
     * @param signSrcDataName 签名原文
     * @param st              签名类型，1--RSA1024签名，2---RSA2048签名, 3---SM2签名证书
     * @param signRule        需要执行摘要算法时，签名填充规则 0---当算法为SM3时表示普通hash，1--当算法为SM3时表示以签名为目的hash，否则表示SHA1，2---SHA256
     * @param algorithmType   需要执行摘要算法时，算法 1---SHA1，2---SHA256,3---SM3
     * @param pinCode         密码，字符串
     * @return
     */
    private Result getSign(Application application, String signSrcDataName, int st, String signRule, String algorithmType, String pinCode) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getSign>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB, "", "", "通道打开失败");
        }
        if (TextUtils.isEmpty(signSrcDataName)) {
            TMKeyLog.d(TAG, "getSign>>>signSrcDataName is null");
            closeChannel_OMA();
            return new Result(false, ResultStateCode.STATE_FAIL_1006, "", "", "签名异常,原文为空");
        }
        String valString = signSrcDataName;

        String  yangzhengma=VCardApi_FFT.mBipEntity.getYangzhengma();
        VCardApi_FFT.mBipEntity.setYangzhengma(""); //验证码设置为空防止多次弹框
        boolean isInitPassword = mTmKeyManager.isInitPassword();

        if (isInitPassword) {//初始密码
            closeChannel_OMA();
            return new Result(false, ResultStateCode.STATE_3000, "", "", "请先修改初始密码，再使用签名功能");
        }
        com.micronet.bakapp.Result resultTemp = mTmKeyManager.getVCardSignState();

        if (ResultStateCode.STATE_2002.equals(resultTemp.getState())) {
            closeChannel_OMA();
            return new Result(false, ResultStateCode.STATE_2001, "", "", "您已开启手动激活V盾签名功能，请按确定进入V盾设置菜单，打开V盾数字签名再使用，如无法进入V盾设置菜单，则说明您的手机不支持该功能。");
        }

        //获取证书
        X509Certificate hcert = null;
        X509CertificateStructure x509CertificateStructure = null;
        try {
            if ((st == SignTypeData.SIGN_TYPE_RSA1024_HASH) || (st == SignTypeData.SIGN_TYPE_RSA2048_HASH)) {
                int cerType = (st == SignTypeData.SIGN_TYPE_RSA1024_HASH) ? SignTypeData.CER_TYPE_RSA1024 : SignTypeData.CER_TYPE_RSA2048;
                hcert = mTmKeyManager.getCerInfo(cerType);
                TMKeyLog.d(TAG, "hcert:" + FCharUtils.showResult16Str(hcert.getEncoded()));
                TMKeyLog.d(TAG, "公钥信息：" + FCharUtils.bytesToHexStr(hcert.getPublicKey().getEncoded()));
                TMKeyLog.d(TAG, "DN：" + hcert.getSubjectDN().toString());
            } else if (st == SignTypeData.SIGN_TYPE_SM2_SIGNCER_HASH) {
                int cerType = SignTypeData.CER_TYPE_SM2_SIGNCER;
                x509CertificateStructure = mTmKeyManager.getCerInfo_Sm2(cerType, true);
                TMKeyLog.d(TAG, "x509CertificateStructure:" + FCharUtils.showResult16Str(x509CertificateStructure.getEncoded()));
                TMKeyLog.d(TAG, "公钥信息：" + FCharUtils.bytesToHexStr(x509CertificateStructure.getSubjectPublicKeyInfo().getPublicKeyData().getEncoded()).substring(8));
                TMKeyLog.d(TAG, "DN：" + x509CertificateStructure.getSubject().toString());
            }
        } catch (Exception e) {
            closeChannel_OMA();
            TMKeyLog.d(TAG, "签名验签异常信息：" + e.getMessage());
            return new Result(false, ResultStateCode.STATE_FAIL_1005, "", "", "V盾证书异常");
        }


        VCardApi_FFT.mBipEntity.setYangzhengma(yangzhengma); //验证码设置为空防止多次弹框

        //签名
        String signature_ = "";
        try {
            byte[] valStringByte = valString.getBytes();
            signature_ = mTmKeyManager.getSignData(application, valStringByte, st, signRule, algorithmType, pinCode, true, false);
            TMKeyLog.d(TAG, "getSign>>>signature_:" + signature_);
            if (signature_ == null || "".equals(signature_)) {
                closeChannel_OMA();
                return new Result(false, ResultStateCode.STATE_FAIL_1006, "", "", "签名异常");
            }
            if (signature_.length() <= 2) {
                String curPackName = SystemUtil.getAppProcessName(application.getApplicationContext());
                TMKeyLog.d(TAG, "getSign>>>curPackName:" + curPackName);
                String errStr1 = "密码错误，您还可以输入";
                String errStr2 = "次。";
//                  辽宁APK特殊提示处理
//                    if (curPackName.equals("com.nxy.mobilebank.ln")) {
//                        errStr1 = "密码错误，还可以尝试的次数：";
//                        errStr2 = "";
//                    }
                closeChannel_OMA();
                if ("5".equals(signature_))
                    return new Result(false, ResultStateCode.STATE_FAIL_1001, "", "", errStr1 + signature_ + errStr2);
                if ("4".equals(signature_))
                    return new Result(false, ResultStateCode.STATE_FAIL_1001, "", "", errStr1 + signature_ + errStr2);
                if ("3".equals(signature_))
                    return new Result(false, ResultStateCode.STATE_FAIL_1001, "", "", errStr1 + signature_ + errStr2);
                if ("2".equals(signature_))
                    return new Result(false, ResultStateCode.STATE_FAIL_1001, "", "", errStr1 + signature_ + errStr2);
                if ("1".equals(signature_))
                    return new Result(false, ResultStateCode.STATE_FAIL_1001, "", "", errStr1 + signature_ + errStr2);
                if ("0".equals(signature_)) {
                    return new Result(false, ResultStateCode.STATE_FAIL_1002, "", "", "您已连续6次输入错误的V盾密码，盾已锁定，请到银行柜台办理“V盾密码重置”业务解除锁定。");
                }
                return new Result(false, ResultStateCode.STATE_FAIL_1003, "", "", "签名登陆失败,请重试。");
            }
            signature_ = signature_.replaceAll(" ", "");
            TMKeyLog.d(TAG, "签名值：" + signature_);
            if (st == SignTypeData.SIGN_TYPE_SM2_SIGNCER_HASH
            || st == SignTypeData.SIGN_TYPE_SM2_SIGNCER) {
                byte[] smRS = FCharUtils.hexString2ByteArray(signature_);
                byte[] smR = new byte[32];
                byte[] smS = new byte[32];
                System.arraycopy(smRS, 0, smR, 0, 32);
                System.arraycopy(smRS, 32, smS, 0, 32);
                String ret = "";
                if ((smR[0] & 0xF0) >> 7 == 1) {
                    ret = ret + "022100" + FCharUtils.bytesToHexStr(smR);
                } else {
                    ret = ret + "0220" + FCharUtils.bytesToHexStr(smR);
                }

                if ((smS[0] & 0xF0) >> 7 == 1) {
                    ret = ret + "022100" + FCharUtils.bytesToHexStr(smS);
                } else {
                    ret = ret + "0220" + FCharUtils.bytesToHexStr(smS);
                }
                int allLenght = ret.length() / 2;
                signature_ = "30" + FCharUtils.intToByte(allLenght) + ret;
                TMKeyLog.d(TAG, "SM2补位后签名值：" + signature_);
            }
        } catch (Exception e1) {
            closeChannel_OMA();
            return new Result(false, ResultStateCode.STATE_FAIL_1006, "", "", "签名异常");
        }

        if ((st == SignTypeData.SIGN_TYPE_RSA1024_HASH) || (st == SignTypeData.SIGN_TYPE_RSA2048_HASH)) {
            //验签RSA签名
            try {
                String stime = sdf.format(hcert.getNotAfter());
                TMKeyLog.d(TAG, "getSign>>>stime:" + stime);
                boolean verifyRes = verifySignData(valString, FCharUtils.hexString2ByteArray(signature_), hcert, st);
                TMKeyLog.e(TAG, "verifyRSARes:" + verifyRes);
                if (!verifyRes) {
                    closeChannel_OMA();
                    return new Result(false, ResultStateCode.STATE_FAIL_1007, "", "", "验签不通过");
                }
            } catch (Exception e) {
                e.printStackTrace();
                closeChannel_OMA();
                return new Result(false, ResultStateCode.STATE_FAIL_1006, "", "", "签名异常");
            }
        } else if (st == SignTypeData.SIGN_TYPE_SM2_SIGNCER_HASH) {
            try {
                String stime = sdf.format(x509CertificateStructure.getEndDate().getDate());
                TMKeyLog.d(TAG, "getSign>>>stime:" + stime);
                boolean verifyRes = SM2Util.verifySign(FCharUtils.hexString2ByteArray(FCharUtils.bytesToHexStr(x509CertificateStructure.getSubjectPublicKeyInfo().getPublicKeyData().getEncoded()).substring(8)), valString.getBytes("UTF-8"), FCharUtils.hexString2ByteArray(signature_), false);
                TMKeyLog.e(TAG, "verifySM2Res:" + verifyRes);
                if (!verifyRes) {
                    closeChannel_OMA();
                    return new Result(false, ResultStateCode.STATE_FAIL_1007, "", "", "验签不通过");
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeChannel_OMA();
                return new Result(false, ResultStateCode.STATE_FAIL_1006, "", "", "签名异常");
            }
        }

        String signInfoP7 = "";
        try {
            if (st == SignTypeData.SIGN_TYPE_RSA1024_HASH) {
                signInfoP7 = FCharUtils.makep7bPack(valString.getBytes(), hcert, FCharUtils.hexString2ByteArray(signature_));
            } else if (st == SignTypeData.SIGN_TYPE_RSA2048_HASH) {
                signInfoP7 = FCharUtils.makeRSA2048p7bPack(valString.getBytes(), hcert, FCharUtils.hexString2ByteArray(signature_));
            } else if (st == SignTypeData.SIGN_TYPE_SM2_SIGNCER_HASH) {
                signInfoP7 = FCharUtils.makep7bPackSM2(valString.getBytes(), x509CertificateStructure, FCharUtils.hexString2ByteArray(signature_));
            }
            TMKeyLog.d(TAG, "signInfoP7:" + signInfoP7);
        } catch (Exception e) {
            closeChannel_OMA();
            return new Result(false, ResultStateCode.STATE_FAIL_1006, "", "", "生成P7异常");
        }
        closeChannel_OMA();
        return new Result(true, ResultStateCode.STATE_OK, valString, signInfoP7, "加签成功！");
    }



    public Result setPin(String oldPass1, String newPass1, boolean isOpen, boolean isClose) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        if (isOpen) {
            hasCard = openChannel(false);
            TMKeyLog.d(TAG, "setPin>>>hasCard:" + hasCard);
            if (! hasCard) {
                return new Result(false, ResultStateCode.STATE_FFFB, "通道打开失败");
            }
        }
        com.micronet.bakapp.Result restbak = mTmKeyManager.changePin(oldPass1, newPass1);
        if (isClose) {
            closeChannel_OMA();
        }
        String resState = restbak.getState();
        if (ResultStateCode.STATE_OK.equalsIgnoreCase(resState)) {
            return new Result(true, ResultStateCode.STATE_OK, "V盾密码修改成功。");
        }
        String isright = restbak.getTimes();
        if ("6".equals(isright))
            return new Result(false, ResultStateCode.STATE_FAIL_1004, restbak.getMessage());
        if ("5".equals(isright))
            return new Result(false, ResultStateCode.STATE_FAIL_1008, "密码错误，您还可以输入5次。");
        if ("4".equals(isright))
            return new Result(false, ResultStateCode.STATE_FAIL_1008, "密码错误，您还可以输入4次。");
        if ("3".equals(isright))
            return new Result(false, ResultStateCode.STATE_FAIL_1008, "密码错误，您还可以输入3次。");
        if ("2".equals(isright))
            return new Result(false, ResultStateCode.STATE_FAIL_1008, "密码错误，您还可以输入2次。");
        if ("1".equals(isright))
            return new Result(false, ResultStateCode.STATE_FAIL_1008, "密码错误，您还可以输入1次。");
        if ("0".equals(isright)) {
            return new Result(false, ResultStateCode.STATE_FAIL_1009, "您已连续6次输入错误的V盾密码，盾已锁定，请到银行柜台办理“V盾密码重置”业务解除锁定。");
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1004, "V盾密码修改失败,请重试。");
    }

    /**
     * 获取密码重置时的加密数据
     *
     * @return
     */
    public String getCiphertext() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getCiphertext>>>hasCard:" + hasCard);
        if (! hasCard) {
            return "";
        }
        String out = mTmKeyManager.getCiphertext();
        closeChannel_OMA();
        if ((out == null) || ("".equals(out)) || (ResultStateCode.STATE_FAIL_1002.equals(out)) || (ResultStateCode.STATE_FAIL_1003.equals(out))) {
            return "";
        }
        return out;
    }

    /**
     * 密码重置
     *
     * @param encryptedData
     * @return
     */
    public Result resetPwd(String encryptedData) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "resetPwd>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB, "通道打开失败");
        }
        com.micronet.bakapp.Result out = mTmKeyManager.resetPwd(encryptedData);
        closeChannel_OMA();
        if (out == null || "".equals(out)) {
            return new Result(false, ResultStateCode.STATE_FAIL_1001, "调用获取密文失败。");
        }
        if (!ResultStateCode.STATE_OK.equals(out.getState())) {
            return new Result(false, ResultStateCode.STATE_FAIL_1001, out.getMessage());
        }
        return new Result(true, ResultStateCode.STATE_OK, "重置成功！");
    }

    /**
     * 获取V盾签名功能开启状态
     *
     * @return
     */
    public com.micronet.bakapp.Result getVCardSignState() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        return mTmKeyManager.getVCardSignState();
    }

    /**
     * 弹出STK菜单
     */
    public void sendPopSTK() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "sendPopSTK>>>hasCard:" + hasCard);
        if (! hasCard) {
            return ;
        }
        mTmKeyManager.sendPopSTK();
        closeChannel_OMA();
    }

    /**
     * 检测是否正常弹出STK菜单
     *
     * @return
     */
    public boolean isCanPopSTK() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        return mTmKeyManager.isCanPopSTK();
    }

    /**
     * 检测是否未修改过初始密码
     *
     * @return
     */
    public boolean isModPasswordOver() {
        TMKeyLog.d(TAG, "isModPasswordOver");
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "isModPasswordOver>>>hasCard:" + hasCard);
        if (! hasCard) {
            return false;
        }
        boolean isInitPwd = mTmKeyManager.isInitPassword();
        closeChannel_OMA();
        return isInitPwd;
    }

    /**
     * V盾代码中通过此值决定上层部分功能菜单是否显示，在V盾SDK中通过此返回值决定使用的签名流程
     *
     * @return
     */
    public boolean isAddSignByHand() {
        return true;
    }

    /**
     * 关闭通道
     */
    public void close() {
        TMKeyLog.d(TAG, "close");
        hasCard = false;
        if (mTmKeyManager != null) {
            mTmKeyManager.clearKey();
            mTmKeyManager.closeChannel();
            mTmKeyManager = null;
        }
    }

    /**
     * 关闭通道
     */
    private void closeChannel_OMA() {
        TMKeyLog.d(TAG, "closeChannel_OMA");
//        if (mTmKeyManager != null) {
//            int conState = mTmKeyManager.getCardConState();
//            TMKeyLog.d(TAG, "closeChannel_OMA>>>conState:" + conState);
//            if (conState == CardConnState.CARDCONN_SUCCESS_OMA || conState == CardConnState.CARDCONN_SUCCESS_UICC) {
//                hasCard = false;
//                mTmKeyManager.clearKey();
//                mTmKeyManager.closeChannel_OMA();
//            }
//        }
    }

    /**
     * 弹出STK菜单，临时开启V盾签名功能
     *
     * @param application
     */
    public void callStkFunctionSetting(Application application) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "callStkFunctionSetting>>>hasCard:" + hasCard);
        if (! hasCard) {
            return ;
        }
        mTmKeyManager.callStkFunctionSetting();
        closeChannel_OMA();
    }

    /**
     * 检测通道是否可用
     *
     * @return
     */
    public boolean checkChannel() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        return mTmKeyManager.checkChannel();
    }

    /**
     * 获取错误状态码
     *
     * @return
     */
    public String getErrorStateCode() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        return mTmKeyManager.getErrorStateCode();
    }

    private boolean verifySignData(String signSrc, byte[] signature_, X509Certificate hcert, int st) {
        boolean verify = false;
        try {
            Signature mySig = null;
            if (st == SignTypeData.SIGN_TYPE_RSA2048_HASH) {
                mySig = Signature.getInstance("SHA256WithRSA");
            } else {
                mySig = Signature.getInstance("SHA1WithRSA");
            }

            mySig.initVerify(hcert.getPublicKey());
            mySig.update(signSrc.getBytes());
            verify = mySig.verify(signature_);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return verify;
    }

    private Map<String, String> splitSubjectDN(String sbDn) {
        Map map = new HashMap();
        String[] values = sbDn.split(",");
        for (int i = 0; i < values.length; ++i) {
            String[] childValue = values[i].split("=");
            map.put(childValue[0], childValue[1]);
        }

        return map;
    }

    /**
     * 获取通信方式
     *
     * @return
     */
    public int getTransType() {
        TMKeyLog.d(TAG, "getTransType");
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        return mTmKeyManager.getCardConState();
    }

    public Result writeHashCode(String ydHashCode, String ltHashCode, String dxHashCode, String appHashCode, String pinStr) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "writeHashCode>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB, "通道打开失败");
        }
        CardResult cardResult = mTmKeyManager.writeHashCode(ydHashCode, ltHashCode, dxHashCode, appHashCode, pinStr);
        closeChannel_OMA();
        if (cardResult.getResCode() == FConstant.RES_SUCCESS) {
            return new Result(true, ResultStateCode.STATE_OK, "");
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1000, cardResult.getErrMsg());
    }

    public Result readHashCode() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "readHashCode>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB, "通道打开失败");
        }
        CardResult cardResult = mTmKeyManager.readHashCode();
        closeChannel_OMA();
        if (cardResult.getResCode() == FConstant.RES_SUCCESS) {
            return new Result(true, ResultStateCode.STATE_OK, cardResult.getData());
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1000, cardResult.getData());
    }

    @Override
    public Result verifyPin(String pinStr) {
        return verifyPin(pinStr, true, true);
    }

    public Result verifyPin(String pinStr, boolean isOpen, boolean isClose) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        if (isOpen) {
            hasCard = openChannel(false);
            TMKeyLog.d(TAG, "verifyPin>>>hasCard:" + hasCard);
            if (! hasCard) {
                return new Result(false, ResultStateCode.STATE_FFFB, "通道打开失败");
            }
        }
        com.micronet.bakapp.Result rs = mTmKeyManager.verifyPin(pinStr);
        if (isClose) {
            closeChannel_OMA();
        }
        String state = rs.getState();
        if (ResultStateCode.STATE_OK.equals(state)) {
            return new Result(true, ResultStateCode.STATE_OK, "");
        } else if (ResultStateCode.STATE_FAIL_1001.equals(state)) { //PIN码错误
            String time = rs.getTimes();
            return new Result(false, ResultStateCode.STATE_FAIL_1000, "密码错误，您还可以输入" + time + "次。");
        } else if (ResultStateCode.STATE_FAIL_1002.equals(state)) { //PIN码错误次数超限
            return new Result(false, ResultStateCode.STATE_FAIL_1000, "您已连续6次输入错误的V盾密码，盾已锁定，请到银行柜台办理“V盾密码重置”业务解除锁定。");
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1000, rs.getMessage());
    }

    @Override
    public Result initCard(String pinStr) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        Result rs = verifyPin(pinStr, true, false);
        if (rs.isFlag()) {
            CardResult cardResult = mTmKeyManager.initCard();
            if (cardResult.getResCode() == FConstant.RES_SUCCESS) {
                boolean initP = false;
                if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) < SIMBaseManager.CARDVERSION_03) {
                    rs = setPin(pinStr, com.froad.ukey.utils.np.SM3.sm3Hash(FCharUtils.string2HexStr("123456")), false, true);
                    TMKeyLog.d(TAG, "initCard>>>" + rs.getMessage());
                    if (rs.isFlag()) {
                        initP = true;
                    }
                } else {
                    initP = true;
                }
                TMKeyLog.d(TAG, "initCard>>>" + rs.getMessage());
                if (initP && (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= SIMBaseManager.CARDVERSION_02)) {
                    CardInfo cardInfo = SIMBaseManager.getInitCardInfo();
                    if (cardInfo != null) {
                        cardInfo.setInitPwd(true);
                    }
                }
                return new Result(true, ResultStateCode.STATE_OK, "");
            } else {
                closeChannel_OMA();
            }
            return new Result(false, ResultStateCode.STATE_FAIL_1000, cardResult.getErrMsg());
        } else {
            closeChannel_OMA();
            return rs;
        }
    }

    /**
     * 创建P10
     * @param certKeyTypes 密钥对类型
     * @param hashTypes
     * @return
     */
    @Override
    public Map<Integer, Result> createCertP10(ArrayList<Integer> certKeyTypes, ArrayList<Integer> hashTypes) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        if (certKeyTypes == null || certKeyTypes.isEmpty()) {
            return null;
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "createCertP10>>>hasCard:" + hasCard);
        if (! hasCard) {
            return null;
        }
        Map<Integer, CardResult> cardResults = mTmKeyManager.createCertP10(certKeyTypes, hashTypes);
        closeChannel_OMA();
        if (cardResults == null) {
            return null;
        }
        Set<Integer> set = cardResults.keySet();
        Iterator<Integer> iterator = set.iterator();
        Map<Integer, Result> resultMap = new HashMap<>();
        int ckt = RSA;
        CardResult cr = null;
        while (iterator.hasNext()) {
            ckt = iterator.next();
            cr = cardResults.get(ckt);
            if (cr.getResCode() == FConstant.RES_SUCCESS) {
                resultMap.put(ckt, new Result(true, ResultStateCode.STATE_OK, cr.getData()));
            } else {
                resultMap.put(ckt, new Result(false, ResultStateCode.STATE_FAIL_1000, cr.getErrMsg()));
            }
        }
        return resultMap;
    }

    /**
     * 导入证书
     * @param certMap MAP key--证书类型，value--证书内容
     * @param encCertPriKey 加密证书私钥密文（使用加密证书私钥保护密钥做对称加密）
     * @param encProKey 加密证书私钥保护密钥密文（加密加密证书私钥的对称密钥，使用国密签名证书公钥加密处理）
     * @return
     */
    @Override
    public Map<Integer, Result> importCert(Map<Integer, byte[]> certMap, String encCertPriKey, String encProKey) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "importCert>>>hasCard:" + hasCard);
        if (! hasCard) {
            return null;
        }
        Map<Integer, CardResult> cardResults = mTmKeyManager.importCert(certMap, encCertPriKey, encProKey);
        closeChannel_OMA();
        Set<Integer> set = cardResults.keySet();
        Iterator iterator = set.iterator();
        Map<Integer, Result> resultMap = new HashMap<>();
        int ckt = 0;
        CardResult cr = null;
        while (iterator.hasNext()) {
            ckt = (Integer) iterator.next();
            cr = cardResults.get(ckt);
            TMKeyLog.d(TAG, "importCert>>>cr:" + cr.getData());
            if (cr.getResCode() == FConstant.RES_SUCCESS) {
                String crKey = cr.getData().substring(0, 4);//卡片获取的CRC值
                //CRC校验
                byte[] certs = certMap.get(ckt);
                TMKeyLog.d(TAG, "importCert>>>certs:" + FCharUtils.showResult16Str(certs));
                byte[] crc16Java = CRC16Util.getCrc16Key(certs);
                String crJava = FCharUtils.showResult16Str(crc16Java);
                TMKeyLog.d(TAG, "importCert>>>crc16Java:" + crJava);
                if (crKey.equalsIgnoreCase(crJava)) {
                    resultMap.put(ckt, new Result(true, ResultStateCode.STATE_OK, cr.getData()));
                } else {
                    resultMap.put(ckt, new Result(false, ResultStateCode.STATE_FFF8, "CRC校验错误"));
                }
            } else {
                resultMap.put(ckt, new Result(false, ResultStateCode.STATE_FAIL_1000, cr.getErrMsg()));
            }
        }
        return resultMap;
    }

    /**
     * 反复打开通道使用，只限于OMA和UICC
     * @param isInitADN 是否需要初始化ADN通道
     * @return
     */
    private boolean openChannel(final boolean isInitADN) {
        TMKeyLog.d(TAG, "openChannel>>>hasCard:" + hasCard + ">>>isInitADN:" + isInitADN);
        if (mTmKeyManager == null) {
            TMKeyLog.d(TAG, "mTmKeyManager is null");
            mTmKeyManager = TmKeyManager.getInstance();
        }
        if (hasCard) {
            return true;
        } else {

            AppExecutors.getAppExecutors().postScheduledExecutorThread(new Runnable() {
                @Override
                public void run() {
                    mTmKeyManager.init(mContext, isInitADN, new OpenChannelCallBack() {
                        @Override
                        public void openChannelResult(boolean isOpen, int channelType, String msg) {
                            TMKeyLog.d(TAG, "openChannelResult>>>isOpen:" + isOpen + ">>>channelType:" + channelType + ">>>msg:" + msg + ">>>omaLock:" + omaLock);
                            if (omaLock) {
                                if (isOpen) {
                                    hasCard = true;
                                } else {
                                    hasCard = false;
                                }
                                if (mConditionVariable != null) {
                                    mConditionVariable.open();
                                }
                            }
                        }
                    });
                }
            });
            omaLock = true;
            //加锁等待OMA和UICC通道打开结果返回
            mConditionVariable.close();
            boolean noOpenTimeOut = mConditionVariable.block(3 * 1000);
            omaLock = false;
            TMKeyLog.d(TAG, "openChannel111>>>noOpenTimeOut:" + noOpenTimeOut + ">>>hasCard:" + hasCard);


            //是否初始化bip，如果有bip的话 初始化bip
            if (!hasCard && SIMBaseManager.isNeedBip) {
                //打开ADN或SMS
                AppExecutors.getAppExecutors().postScheduledExecutorThread(new Runnable() {
                    @Override
                    public void run() {
                        TmKeyManager.isUseBip=false;
                        hasCard = mTmKeyManager.initBip(mContext);  //初始化bip模式
                        if(hasCard){
                        TmKeyManager.isUseBip=true;}else {
                            TmKeyManager.isUseBip=false;
                        }
                        TMKeyLog.d(TAG, "bip模式下检测是否有卡>>>:" + hasCard);
                        if (mConditionVariable != null && adnLock) {
                            mConditionVariable.open();
                        }
                    }
                });
                adnLock = true;
                //加锁等待ADN和SMS通道打开结果返回
                mConditionVariable.close();
                noOpenTimeOut = mConditionVariable.block(10 * 1000);
                adnLock = false;
                TMKeyLog.d(TAG, "openChannel222>>>noOpenTimeOut:" + noOpenTimeOut + ">>>hasCard:" + hasCard);
            }


            if (!hasCard && isInitADN) {
                //打开ADN或SMS
                AppExecutors.getAppExecutors().postScheduledExecutorThread(new Runnable() {
                    @Override
                    public void run() {
                        hasCard = mTmKeyManager.initADN(mContext);
                        TMKeyLog.d(TAG, "openChannel222>>>hasCard:" + hasCard);
                        if (mConditionVariable != null && adnLock) {
                            mConditionVariable.open();
                        }
                    }
                });
                adnLock = true;
                //加锁等待ADN和SMS通道打开结果返回
                mConditionVariable.close();
                noOpenTimeOut = mConditionVariable.block(10 * 1000);
                adnLock = false;
                TMKeyLog.d(TAG, "openChannel222>>>noOpenTimeOut:" + noOpenTimeOut + ">>>hasCard:" + hasCard);
            }

            TMKeyLog.d(TAG, "openChannel333>>>hasCard:" + hasCard + ">>>isInitADN:" + isInitADN);
            if (hasCard) {
                if (isInitADN) {
                    closeChannel_OMA();
                }
                return true;
            } else {
                close();
                return false;
            }
        }
    }

    /**
     * 检测Cos更新
     * @param callBack
     */
    public void checkCosUpdatehttp (final CosUpdateCallBack callBack) {
        TMKeyLog.d(TAG, "checkCosUpdatehttp");
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>hasCard:" + hasCard);
        if (! hasCard) {
            if (callBack != null) {
                callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS更新，通道未成功打开");
            }
            return ;
        }
        final String cardCsn = mTmKeyManager.getCardSN(false);
        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>cardCsn：" + cardCsn);
        if (TextUtils.isEmpty(cardCsn)) {
            closeChannel_OMA();
            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>检测COS获取CSN失败");
            if (callBack != null) {
                callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS获取CSN失败");
            }
            return ;
        }
        String curCosVer = mTmKeyManager.checkCosVersion();
        if ("Error".equalsIgnoreCase(curCosVer)) {
            closeChannel_OMA();
            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>检测COS获取ATR失败");
            if (callBack != null) {
                callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS获取ATR失败");
            }
            return ;
        }
        if (curCosVer == null || curCosVer.length() < 20) { //数据错误
            closeChannel_OMA();
            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>检测COS获取ATR长度错误");
            if (callBack != null) {
                callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS获取ATR长度错误");
            }
            return ;
        }
        cos_atr = curCosVer;
        CardResult cr = mTmKeyManager.getCosRandom();
        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>cos_atr:" + cos_atr);
        if (cr.getResCode() == FConstant.RES_SUCCESS) {
            cos_random = cr.getData();
            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>cos_random:" + cos_random);
            //请求后台获取COS更新信息
            AuthRequestParams httpParamsInfo = new AuthRequestParams();
            String randomStr = FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(cos_random), FCharUtils.hexString2ByteArray(SM4Util.ENCRK), SM4Util.ENCRYPT, false, 1));
            String atrStr = FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(cos_atr), FCharUtils.hexString2ByteArray(SM4Util.ENCRK), SM4Util.ENCRYPT, false, 1));
            String macStr = FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMac(FCharUtils.hexString2ByteArray(randomStr + atrStr), FCharUtils.hexString2ByteArray(SM4Util.ENCRK)));
            httpParamsInfo.putParam("cardNo", cardCsn);
            httpParamsInfo.putParam("random", randomStr);
            httpParamsInfo.putParam("atr", atrStr);
            httpParamsInfo.putParam("mac", macStr);
            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>randomStr:" + randomStr);
            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>atrStr:" + atrStr);
            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>macStr:" + macStr);
            httpPost(httpParamsInfo, new OnResultListener<AuthResponseResult>() {
                @Override
                public void onResult(AuthResponseResult obj) {
                    String data = obj.getData();
                    TMKeyLog.d(TAG, "checkCosUpdatehttp>>>data:" + data);
                    Gson gson = new Gson();
                    GetCosUpdateBean cosBean = gson.fromJson(data, GetCosUpdateBean.class);
                    if (cosBean != null) {
                        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>cosBean:" + cosBean.toString());
                        String upgradeSign = cosBean.getUpgradeSign();
                        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>upgradeSign:" + upgradeSign);
                        if ("2".equalsIgnoreCase(upgradeSign)) { //不需要升级COS
                            closeChannel_OMA();
                            if (callBack != null) {
                                callBack.result(FConstant.RES_SUCCE_COS_IS_NEWEST, FConstant.getCardErrorMsg(FConstant.RES_SUCCE_COS_IS_NEWEST));
                            }
                            return ;
                        }
                        cos_pin = cosBean.getPin();
                        cos_new_atr = cosBean.getNewAtr();
                        String cosInstruction = cosBean.getCosInstruction();
                        String dealMacStr = FCharUtils.bytesToHexStr(SM4Util.getInstance().dealMac((cosInstruction + cos_pin + cos_new_atr).getBytes(),
                                FCharUtils.hexString2ByteArray(SM4Util.ENCRK)));

                        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>dealMacStr:" + dealMacStr);
                        if (cosBean.getMac().equalsIgnoreCase(dealMacStr)) {
                            cos_pin = FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(cos_pin), FCharUtils.hexString2ByteArray(SM4Util.ENCRK), SM4Util.DECRYPT, false, 1));
                            cos_new_atr = FCharUtils.bytesToHexStr(SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(cos_new_atr), FCharUtils.hexString2ByteArray(SM4Util.ENCRK), SM4Util.DECRYPT, false, 1));
                            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>cos_pin:" + cos_pin + ">>>cos_new_atr:" + cos_new_atr);

                            TMKeyLog.d(TAG, "checkCosUpdatehttp>>>cosInstruction:" + cosInstruction);
                            if (cosInstruction.contains("%9000")) {
                                ArrayList<String> apduList = new ArrayList();
                                apduList.add("B220000518" + cos_pin);
                                String[] cosInstructions = cosInstruction.split("%9000");
                                int cosInstructionsLen = cosInstructions.length;
                                for (int i = 0; i < cosInstructionsLen; i++) {
                                    cosInstruction = cosInstructions[i].replaceAll(" ", "");
                                    if (TextUtils.isEmpty(cosInstruction)) {
                                        continue;
                                    }
                                    apduList.add(cosInstruction);
                                }
                                int ll = apduList.size();
                                CardResult cr = null;
                                String curCmd = "";
                                int transResCode = 0;
                                boolean isAllSuccess = true;
                                String recStateCode = "";
                                for (int i = 0; i < ll; i ++) {
                                    curCmd = apduList.get(i);
                                    TMKeyLog.d(TAG, "checkCosUpdatehttp>>>i:" + i + ">>>curCmd:" + curCmd);
                                    cr = mTmKeyManager.transApdu(curCmd);
                                    transResCode = cr.getResCode();
                                    if (transResCode == FConstant.RES_SUCCESS) {
                                        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>cr.getData:" + cr.getData());
                                        continue;
                                    } else if (transResCode == FConstant.RES_FAIL_RECEIVE_DATA_ERROR) {
                                        isAllSuccess = false;
                                        recStateCode = cr.getData();
                                        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>recStateCode:" + recStateCode);
                                        break;
                                    } else {
                                        isAllSuccess = false;
                                        break;
                                    }
                                }
                                TMKeyLog.d(TAG, "checkCosUpdatehttp>>>isAllSuccess:" + isAllSuccess);
                                if (isAllSuccess) {
                                    //cos更新成功，读取ATR对比
                                    cr = mTmKeyManager.getAtr();
                                    transResCode = cr.getResCode();
                                    if (transResCode == FConstant.RES_SUCCESS) {
                                        String nAtr = cr.getData();
                                        TMKeyLog.d(TAG, "checkCosUpdatehttp>>>nAtr:" + nAtr);
                                        if (cos_new_atr.equalsIgnoreCase(nAtr)) {
                                            //ATR对比成功，COS更新成功
                                            closeChannel_OMA();
                                            if (callBack != null) {
                                                callBack.result(FConstant.RES_SUCCESS, "薄膜盾Cos更新成功，需要重启手机后再使用，请手动重启手机");
                                            }
                                        } else {
                                            closeChannel_OMA();
                                            if (callBack != null) {
                                                callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS更新操作失败，对比更新后ATR不一致");
                                            }
                                        }
                                    } else {
                                        //读取ATR失败，认为更新COS失败
                                        closeChannel_OMA();
                                        if (callBack != null) {
                                            callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS更新操作失败，读取新ATR失败");
                                        }

                                    }
                                } else {
                                    closeChannel_OMA();
                                    if (callBack != null) {
                                        callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS更新操作指令交互失败");
                                    }
                                }
                            } else {
                                closeChannel_OMA();
                                if (callBack != null) {
                                    callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS更新失败,脚本数据格式错误");
                                }
                            }
                        } else {
                            closeChannel_OMA();
                            TMKeyLog.d(TAG, "COS检测更新失败，返回数据MAC校验错误");
                            callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "COS检测更新失败，返回数据MAC校验错误");
                        }
                    } else {
                        closeChannel_OMA();
                        TMKeyLog.d(TAG, "COS检测更新失败，返回数据解析错误");
                        callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "COS检测更新失败，返回数据解析错误");
                    }
                }

                @Override
                public void onError(AuthError authError) {
                    closeChannel_OMA();
                    TMKeyLog.d(TAG, "COS检测更新失败，网络请求错误");
                    callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "COS检测更新失败，网络请求错误-->" + authError.getMessage());
                }
            }, HttpConstants.URL_GETCOSBYCLIENT);
        } else {
            closeChannel_OMA();
            TMKeyLog.d(TAG, "checkCosUpdate>>>检测COS获取随机数失败");
            if (callBack != null) {
                callBack.result(FConstant.RES_FAIL_OTHER_ERROR, "检测COS获取随机数失败");
            }
            return;
        }
    }

    protected void httpPost(final AuthRequestParams params, final OnResultListener<AuthResponseResult> listener, final String url) {
        final Parser<AuthResponseResult> authResultParser = new AuthResultParser();
        TMKeyLog.d(TAG, "httpPost>>>url:" + url);
        HttpUtil.getInstance().post(url, params, authResultParser, new OnResultListener<AuthResponseResult>() {
            public void onResult(AuthResponseResult result) {
                if (listener != null) {
                    listener.onResult(result);
                }
            }

            public void onError(AuthError error) {
                if (listener != null) {
                    listener.onError(error);
                }
            }
        });
    }

    @Override
    public boolean checkFingerSupport() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getFingerRandom>>>hasCard:" + hasCard);
        if (! hasCard) {
            return false;
        }
        boolean supRes = mTmKeyManager.checkFingerSupport();
        closeChannel_OMA();
        return supRes;
    }

    @Override
    public Result getFingerRandom() {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "getFingerRandom>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB,"通道打开失败");
        }
        CardResult cr = mTmKeyManager.getFingerRandom();
        closeChannel_OMA();
        if (cr.getResCode() == FConstant.RES_SUCCESS) {
            return new Result(true, ResultStateCode.STATE_OK, cr.getData());
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1000, cr.getErrMsg());
    }

    @Override
    public Result setFingerPubKey(int pubKeyType, byte[] fingerPubKey, byte[] pinData) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "setFingerPubKey>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB,"通道打开失败");
        }
        CardResult cr = mTmKeyManager.setFingerPubKey(pubKeyType, fingerPubKey, pinData);
        closeChannel_OMA();
        if (cr.getResCode() == FConstant.RES_SUCCESS) {
            return new Result(true, ResultStateCode.STATE_OK, "设置指纹认证功能成功");
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1000, "设置指纹认证功能失败");
    }

    @Override
    public Result verifyFinger(int pubKeyType, byte[] fingerData) {
        if (mTmKeyManager == null) {
            mTmKeyManager = TmKeyManager.getInstance();
        }
        hasCard = openChannel(false);
        TMKeyLog.d(TAG, "verifyFinger>>>hasCard:" + hasCard);
        if (! hasCard) {
            return new Result(false, ResultStateCode.STATE_FFFB,"通道打开失败");
        }
        CardResult cr = mTmKeyManager.verifyFinger(pubKeyType, fingerData);
        closeChannel_OMA();
        if (cr.getResCode() == FConstant.RES_SUCCESS) {
            return new Result(true, ResultStateCode.STATE_OK, "指纹认证成功");
        }
        return new Result(false, ResultStateCode.STATE_FAIL_1000, "指纹认证失败");
    }

    /**
     * 初始化 bip通道 参数
     *
     * @param
     * @param datas
     * @param typeflag
     * @param yangzhengma
     */
    @Override
    public void setBipConfig(String datas, String typeflag, String yangzhengma) {
        mBipEntity=null;
        mBipEntity=new BipEntity(datas,typeflag,yangzhengma);
    }
}
