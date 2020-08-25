package com.froad.ukey.manager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;

import com.froad.ukey.bean.CardInfo;
import com.froad.ukey.bean.CardResult;
import com.froad.ukey.constant.FConstant;
import com.froad.ukey.constant.InsideDataStateCode;
import com.froad.ukey.constant.ResultStateCode;
import com.froad.ukey.interf.CardConCallBack;
import com.froad.ukey.interf.OpenChannelCallBack;
import com.froad.ukey.jni.GmSSL;
import com.froad.ukey.jni.tmjni;
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.simchannel.imp.BipHelper;
import com.froad.ukey.simchannel.imp.SESDefaultHelper;
import com.froad.ukey.simchannel.imp.SESHelper;
import com.froad.ukey.simchannel.imp.SESSystemHelper;
import com.froad.ukey.simchannel.imp.SMSCenOppoHelper;
import com.froad.ukey.simchannel.imp.SMSHelper;
import com.froad.ukey.simchannel.imp.SMSIccEFHelper;
import com.froad.ukey.simchannel.imp.SMSSIDHelper;
import com.froad.ukey.simchannel.imp.UICCHelper;
import com.froad.ukey.utils.LogToFile;
import com.froad.ukey.utils.SHA;
import com.froad.ukey.utils.SM2;
import com.froad.ukey.utils.SystemUtil;
import com.froad.ukey.utils.np.CardConnState;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.SM3;
import com.froad.ukey.utils.np.TMKeyLog;
import com.micronet.bakapp.Result;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.X509CertificateStructure;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

/**
 * TmKeyManager
 * <p>
 * UKEY管理类 *
 * @author by FW.
 * @date 16/12/26
 * @modify 修改者 FW
 */
public class TmKeyManager extends SIMBaseManager {

    public static String sm2SignPubKey = "";

    private static final String TAG = FConstant.LOG_TAG + "TmKeyManager";
    private Context mContext;
    private OpenChannelCallBack mOpenChannelCallBack;
    private static TmKeyManager tmKeyManager;

    private boolean isInitSimHelper = false;//是否已经初始化了OMA对象
    private boolean isInitBipHelper = false;//是否已经初始化了OMA对象
    private boolean hasOMACallBack = false;//是否已经返回了OMA通道打开成功的结果
    private final String sdkVersion = "V2.1.5";//SDK版本号
    private int sysVersion = 0;//系统版本

    private boolean signCloseFun = false;//自动关闭签名功能关闭
    private String SignCloseStateName = "SIGN_CLOSE_STATE_NAME";//签名功能自动关闭
    private boolean isStartUpCard = false;//是否已启用V盾签名功能
    private boolean isSESHelperFail = false;//OMA接口模式结束
    private boolean isSESDefaultHelperFail = false;//OMA默认模式结束

    private List<SIMHelper> channelList = new ArrayList<>();//保存通信通道对象
    private int currentChannelIndex = 0;//当前通道索引

    //统计每一步与卡通信的时间
    private long startTime = 0;
    private long endTime = 0;

    private String cerDirPath = "";//证书缓存文件夹路径
    private String cerFileName = "";//证书文件名
    private boolean isTransmitting = false;//是否正在进行指令交互
    private boolean isOpenSesHelper = false;//是否能正常打开SESHelper
    private boolean isOpenSesDefaultHelper = false;//是否能正常打开SESDefaultHelper

    private boolean hasOMAPackage = false;//是否有OMA相关jar包支持
    private String sm2SignSupplement = "";//SM2签名补位信息（公钥）

    private CardConCallBack mCardConCallBack = new CardConCallBack() {
        @Override
        public void OmaOpenState(boolean isSuccess, String msg) { //接口模式OMA回调
            TMKeyLog.d(TAG, "mCardConCallBack>>>OmaOpenState>>>当前线程ID：" + Thread.currentThread().getId());
            TMKeyLog.d(TAG, "mCardConCallBack>>>OmaOpenState>>>isSuccess:" + isSuccess + ">>>msg:" + msg);
            int t = 0;
            if(sesHelper != null) {
                ((SESHelper)sesHelper).deleteInitSeCount();
                t = ((SESHelper)sesHelper).getInitSeCount();
                TMKeyLog.d(TAG, "OmaOpenState>>>getInitSeCount:" + t);
                if (isSuccess) {
                    if (cardConnState == CardConnState.CARDCONN_SUCCESS_UICC) {
                        sesHelperConState = true;
                        TMKeyLog.d(TAG, "OMA sesHelper模式打开通道成功,UICC 连接成功，记录OMA通道状态,msg:" + msg);
                        logEndTime("OmaOpenState>>>OMA_OPEN_SUCCESS>>>initSESHelper");
                        return;
                    }
                    if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA && ((simHelper instanceof SESDefaultHelper) || (simHelper instanceof SESSystemHelper))) {
                        sesHelper.close();
                        sesHelperConState = false;
                        TMKeyLog.d(TAG, "OMA sesHelper模式打开通道成功,sesDefaultHelper已经连接成功，关闭sesHelper通道");
                        logEndTime("OmaOpenState>>>OMA_OPEN_SUCCESS>>>initSESHelper");
                        return;
                    }
                    TMKeyLog.d(TAG, "OMA sesHelper模式打开通道成功,msg:" + msg);
                    logEndTime("OmaOpenState>>>OMA_OPEN_SUCCESS>>>initSESHelper");
                    isADN = false;
                    cardConnState = CardConnState.CARDCONN_SUCCESS_OMA;
                    simHelper = sesHelper;
                    isOpenSesHelper = true;
                } else {
                    sesHelperConState = false;
                    TMKeyLog.e(TAG, "OMA sesHelper模式打开通道失败>>>msg:" + msg + ">>>isSESDefaultHelperFail:" + isSESDefaultHelperFail);
                }
            }

            if (t == 0) { //OMA模式结束
                isSESHelperFail = true;
                TMKeyLog.d(TAG, "OmaOpenState>>>cardConnState:" + cardConnState + ">>>isOpenSesHelper:" + isOpenSesHelper);
                if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA && isOpenSesHelper) {
                    TMKeyLog.d(TAG, "OmaOpenState>>>hasOMACallBack:" + hasOMACallBack);
                    if (! hasOMACallBack) {
                        hasCard = hasCard();
                        if (hasCard) {
                            hasOMACallBack = true;
                            if (mOpenChannelCallBack != null) {
                                mOpenChannelCallBack.openChannelResult(true, CardConnState.CARDCONN_SUCCESS_OMA, "OMA");
                            }
                        }
                    }
                } else if (cardConnState == CardConnState.CARDCONN_FAIL) { //失败
                    TMKeyLog.d(TAG, "OmaOpenState>>>isSESDefaultHelperFail:" + isSESDefaultHelperFail);
                    if (isSESDefaultHelperFail) {
                        hasCard = false;
                        isInitSimHelper= false;
                        if (mOpenChannelCallBack != null) {
                            mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "OMA模式打开通道失败");
                        }
                    }
                }
            }

//            if (isSuccess) {
//                if (cardConnState == CardConnState.CARDCONN_SUCCESS_UICC) {
//                    sesHelperConState = true;
//                    TMKeyLog.d(TAG, "OMA sesHelper模式打开通道成功,UICC 连接成功，记录OMA通道状态,msg:" + msg);
//                    logEndTime("OmaOpenState>>>OMA_OPEN_SUCCESS>>>initSESHelper");
//                    return;
//                }
//                if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA && ((simHelper instanceof SESDefaultHelper) || (simHelper instanceof SESSystemHelper))) {
//                    sesHelper.close();
//                    sesHelperConState = false;
//                    TMKeyLog.d(TAG, "OMA sesHelper模式打开通道成功,sesDefaultHelper已经连接成功，关闭sesHelper通道");
//                    logEndTime("OmaOpenState>>>OMA_OPEN_SUCCESS>>>initSESHelper");
//                    return;
//                }
//                TMKeyLog.d(TAG, "OMA sesHelper模式打开通道成功,msg:" + msg);
//                logEndTime("OmaOpenState>>>OMA_OPEN_SUCCESS>>>initSESHelper");
//                isADN = false;
//                cardConnState = CardConnState.CARDCONN_SUCCESS_OMA;
//                simHelper = sesHelper;
//                TMKeyLog.d(TAG, "OmaOpenState>>>hasOMACallBack:" + hasOMACallBack);
//                if (! hasOMACallBack) {
//                    hasCard = hasCard();
//                    if (hasCard) {
//                        hasOMACallBack = true;
//                        if (mOpenChannelCallBack != null) {
//                            mOpenChannelCallBack.openChannelResult(true, CardConnState.CARDCONN_SUCCESS_OMA, "OMA");
//                        }
//                    }
//                }
//            } else {
//                TMKeyLog.e(TAG, "OMA sesHelper模式打开通道失败>>>msg:" + msg);
//                if (cardConnState == CardConnState.CARDCONN_FAIL && sesHelper != null) {
//                    ((SESHelper)sesHelper).deleteInitSeCount();
//                    int t = ((SESHelper)sesHelper).getInitSeCount();
//                    TMKeyLog.d(TAG, "sesHelper>>>getInitSeCount:" + t + ">>>isSESDefaultHelperFail:" + isSESDefaultHelperFail);
//                    if (t == 0) {
//                        isSESHelperFail = true;
//                        if (isSESDefaultHelperFail) {
//                            hasCard = false;
//                            isInitSimHelper= false;
//                            if (mOpenChannelCallBack != null) {
//                                mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "OMA模式打开通道失败");
//                            }
//                        }
//                    }
//                }
//                sesHelperConState = false;
//            }
        }

        @Override
        public void OmaDefaultOpenState(boolean isSuccess, String msg) {
            TMKeyLog.d(TAG, "mCardConCallBack>>>OmaDefaultOpenState>>>当前线程ID：" + Thread.currentThread().getId());
            TMKeyLog.d(TAG, "mCardConCallBack>>>OmaDefaultOpenState>>>isSuccess:" + isSuccess + ">>>msg:" + msg);
            int t = 0;
            if(sysVersion < Build.VERSION_CODES.P && sesDefaultHelper != null) {
                ((SESDefaultHelper)sesDefaultHelper).deleteInitSeCount();
                t = ((SESDefaultHelper)sesDefaultHelper).getInitSeCount();
                TMKeyLog.d(TAG, "getInitSeCount:" + t);
                if (isSuccess) {
                    if (cardConnState == CardConnState.CARDCONN_SUCCESS_UICC) {
                        sesDefaultHelperConState = true;
                        TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,UICC 连接成功，记录OMA通道状态,msg:" + msg);
                        logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
                        return;
                    }
                    if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA && (simHelper instanceof SESHelper)) {
                        if (sysVersion >= Build.VERSION_CODES.P) {
                            sesSystemHelper.close();
                        } else {
                            sesDefaultHelper.close();
                        }
                        sesDefaultHelperConState = false;
                        TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,sesHelper已经连接成功，关闭sesDefaultHelper通道");
                        logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
                        return;
                    }
                    TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,msg:" + msg);
                    logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
                    isADN = false;
                    cardConnState = CardConnState.CARDCONN_SUCCESS_OMA;
                    isOpenSesDefaultHelper = true;
                    if (sysVersion >= Build.VERSION_CODES.P) {
                        simHelper = sesSystemHelper;
                    } else {
                        simHelper = sesDefaultHelper;
                    }
                } else {
                    sesDefaultHelperConState = false;
                    TMKeyLog.e(TAG, "OMA sesDefaultHelper模式打开通道失败>>>msg:" + msg + ">>>isSESHelperFail:" + isSESHelperFail);
                }
            } else if (sysVersion >= Build.VERSION_CODES.P && sesSystemHelper != null) {
                if (isSuccess) {
                    if (cardConnState == CardConnState.CARDCONN_SUCCESS_UICC) {
                        sesDefaultHelperConState = true;
                        TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,UICC 连接成功，记录OMA通道状态,msg:" + msg);
                        logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
                        return;
                    }
                    if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA && (simHelper instanceof SESHelper)) {
                        if (sysVersion >= Build.VERSION_CODES.P) {
                            sesSystemHelper.close();
                        } else {
                            sesDefaultHelper.close();
                        }
                        sesDefaultHelperConState = false;
                        TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,sesHelper已经连接成功，关闭sesDefaultHelper通道");
                        logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
                        return;
                    }
                    TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,msg:" + msg);
                    logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
                    isADN = false;
                    cardConnState = CardConnState.CARDCONN_SUCCESS_OMA;
                    isOpenSesDefaultHelper = true;
                    if (sysVersion >= Build.VERSION_CODES.P) {
                        simHelper = sesSystemHelper;
                    } else {
                        simHelper = sesDefaultHelper;
                    }
                } else {
                    sesDefaultHelperConState = false;
                    TMKeyLog.e(TAG, "OMA sesDefaultHelper模式打开通道失败>>>msg:" + msg + ">>>isSESHelperFail:" + isSESHelperFail);
                }
            }

            if (t == 0) { //OMA模式结束
                isSESDefaultHelperFail = true;
                if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA && isOpenSesDefaultHelper) {
                    TMKeyLog.d(TAG, "OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>hasCard:" + hasCard + ">>>hasOMACallBack:" + hasOMACallBack);
                    if (!hasOMACallBack) {
                        hasCard = hasCard();
                        if (hasCard) {
                            hasOMACallBack = true;
                            if (mOpenChannelCallBack != null) {
                                mOpenChannelCallBack.openChannelResult(true, CardConnState.CARDCONN_SUCCESS_OMA, "OMA");
                            }
                        }
                    }
                } else if (cardConnState == CardConnState.CARDCONN_FAIL) { //失败
                    if (isSESHelperFail) {
                        hasCard = false;
                        isInitSimHelper = false;
                        if (mOpenChannelCallBack != null) {
                            mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "OMA模式打开通道失败");
                        }
                    }
                }
            }
//            if (isSuccess) {
//                if (cardConnState == CardConnState.CARDCONN_SUCCESS_UICC) {
//                    sesDefaultHelperConState = true;
//                    TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,UICC 连接成功，记录OMA通道状态,msg:" + msg);
//                    logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
//                    return;
//                }
//                if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA && (simHelper instanceof SESHelper)) {
//                    if (sysVersion >= Build.VERSION_CODES.P) {
//                        sesSystemHelper.close();
//                    } else {
//                        sesDefaultHelper.close();
//                    }
//                    sesDefaultHelperConState = false;
//                    TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,sesHelper已经连接成功，关闭sesDefaultHelper通道");
//                    logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
//                    return;
//                }
//                TMKeyLog.d(TAG, "OMA sesDefaultHelper模式打开通道成功,msg:" + msg);
//                logEndTime("OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>initSESDefaultHelper");
//                isADN = false;
//                cardConnState = CardConnState.CARDCONN_SUCCESS_OMA;
////                initChannelState = cardConnState;
//                if (sysVersion >= Build.VERSION_CODES.P) {
//                    simHelper = sesSystemHelper;
//                } else {
//                    simHelper = sesDefaultHelper;
//                }
//                TMKeyLog.d(TAG, "OmaDefaultOpenState>>>OMA_OPEN_SUCCESS>>>hasCard:" + hasCard + ">>>hasOMACallBack:" + hasOMACallBack);
//                if (!hasOMACallBack) {
//                    hasCard = hasCard();
//                    if (hasCard) {
//                        hasOMACallBack = true;
//                        if (mOpenChannelCallBack != null) {
//                            mOpenChannelCallBack.openChannelResult(true, CardConnState.CARDCONN_SUCCESS_OMA, "OMA");
//                        }
//                    }
//                }
//            } else {
//                sesDefaultHelperConState = false;
//                TMKeyLog.e(TAG, "OMA sesDefaultHelper模式打开通道失败>>>msg:" + msg + ">>>isSESHelperFail:" + isSESHelperFail);
//                if (cardConnState == CardConnState.CARDCONN_FAIL) {
//                    if (sysVersion < Build.VERSION_CODES.P && sesDefaultHelper != null) {
//                        ((SESDefaultHelper) sesDefaultHelper).deleteInitSeCount();
//                        int t = ((SESDefaultHelper) sesDefaultHelper).getInitSeCount();
//                        TMKeyLog.d(TAG, "getInitSeCount:" + t);
//                        if (t == 0) {
//                            isSESDefaultHelperFail = true;
//                            if (isSESHelperFail) {
//                                hasCard = false;
//                                isInitSimHelper = false;
//                                if (mOpenChannelCallBack != null) {
//                                    mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "OMA模式打开通道失败");
//                                }
//                            }
//                        }
//                    } else if (sysVersion >= Build.VERSION_CODES.P && sesSystemHelper != null && isSESHelperFail) {
//                        isSESDefaultHelperFail = true;
//                        hasCard = false;
//                        isInitSimHelper = false;
//                        if (mOpenChannelCallBack != null) {
//                            mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "OMA模式打开通道失败");
//                        }
//                    }
//                }
//            }
        }

        @Override
        public void AdnOpenState(boolean isSuccess, String msg) {
            TMKeyLog.d(TAG, "AdnOpenState>>>isSuccess:" + isSuccess + "msg:" + msg);
            if (isSuccess) {
                hasCard = true;
                TMKeyLog.d(TAG, "ADN_OPEN_SUCCESS>>>msg:" + msg);
                logEndTime("ADN_OPEN_SUCCESS>>>initSmsHelper");
                TMKeyLog.d(TAG, "isADN:" + isADN);
                if (isADN) {
                    TMKeyLog.d(TAG, "isSMSCen:" + isSMSCen + ">>>isSMS:" + isSMS);
                    simHelper = smsHelper;
                    if (isSMSCen) {
                        cardConnState = CardConnState.CARDCONN_SUCCESS_SMS_CEN;
                    } else if (isSMS) {
                        cardConnState = CardConnState.CARDCONN_SUCCESS_SMS;
                    } else {
                        cardConnState = CardConnState.CARDCONN_SUCCESS_ADN;
                        //ADN模式连接成功，将保存的TagKey设置到smsHelper中
                        PhoneTagStr = sp.getString(PhoneTagKeyStr, PHONEKEY_TAG);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(PhoneTagKeyStr, PhoneTagStr);
                        editor.commit();
                        if (smsHelper != null) {
                            ((SMSHelper) smsHelper).setPhoneTagStr(PhoneTagStr);
                        }
                    }
                }
            } else {
                TMKeyLog.e(TAG, "CARD_CONNECT_FAIL>>>msg:" + msg);
                logEndTime("CARD_CONNECT_FAIL>>>initSmsHelper");
                TMKeyLog.d(TAG, "isADN:"+ isADN);
                if (isADN) {
                    hasCard = false;
                }
            }

        }
    };

    /**
     * 私有化构造函数
     */
    private TmKeyManager() {
    }

    /**
     * 构造唯一对象
     *
     * @return
     */
    public static TmKeyManager getInstance() {
        TMKeyLog.d(TAG, "getInstace");
        if (tmKeyManager == null) {
            TMKeyLog.e(TAG, "tmKeyManager is null");
            tmKeyManager = new TmKeyManager();
            tmKeyManager.isInitSimHelper = false;
            tmKeyManager.initChannelState = CardConnState.CARDCONN_FAIL;
            CardSmsVersion = "00";//重置卡片版本号
        }
        return tmKeyManager;
    }

    /**
     *
     * @param context
     */
    public void init(Context context, boolean isInitADN, OpenChannelCallBack channelCallBack) {
        TMKeyLog.d(TAG, "init>>>isInitADN:" + isInitADN);
        mContext = context;
        mOpenChannelCallBack = channelCallBack;
        if (isInitADN) {
            cerDirPath = mContext.getFilesDir() + "/localcer/";
            //通过SharedPreferences保存TagKey
            if (sp == null) {
                if (mContext != null) {
                    sp = mContext.getSharedPreferences(FConstant.fft_sdk_sp_name, Context.MODE_PRIVATE);
                    signCloseFun = sp.getBoolean(SignCloseStateName, false);
                }
            }

//            //---------test----------
//            // 测试阶段临时添加dn保存
//            SharedPreferences.Editor editor = sp.edit();
//            editor.putString(DN_RSA1024, "C=CN,O=Rural Credit Banks Funds Clearing Center,OU=Customers,CN=063@833782600162602");
//            editor.putString(DN_RSA2048, "C=CN,O=Rural Credit Banks Funds Clearing Center,OU=Customers,CN=063@833782600162602");
////            editor.putString(DN_RSA2048, "CN=063@0371202199209267314@Zhangwu01@1000000174,OU=Enterprises,O=111111,C=CN");
//            editor.putString(DN_SM2, "C=CN,O=Rural Credit Banks Funds Clearing Center,OU=Customers,CN=063@833782600162602");
//            editor.commit();
//            //---------test----------

            //初始化各个通道对象
            channelList.clear();
            channelList.add(new SMSCenOppoHelper(mContext, mCardConCallBack));//短信中心号码
            channelList.add(new SMSIccEFHelper(mContext, mCardConCallBack));//短信 ADN
            channelList.add(new SMSSIDHelper(mContext, mCardConCallBack));//短信 ADN，C网
            channelList.add(new SMSHelper(mContext, mCardConCallBack));//短信 ADN，G网
            channelList.add(new BipHelper(mContext, mCardConCallBack));//bip
        }
        TMKeyLog.d(TAG, "isInitSimHelper:" + isInitSimHelper);
        if (! isInitSimHelper) {
            //删除日志文件
            LogToFile.init(mContext);
            LogToFile.deleteLogFile();
            initSimHelper();
        }
    }

    /**
     * 初始化ADN模式
     */
    public boolean initADN (Context c) {
        TMKeyLog.d(TAG, "initADN");
        mContext = c;
        hasCard = initSmsHelper(false);
        return hasCard;
    }

    /**
     * 初始化Bip模式
     */
    public boolean initBip (Context c) {
        TMKeyLog.d(TAG, "initbip");
        mContext = c;
        hasCard = initBipHelper();
        cardConnState=CardConnState.CARDCONN_SUCCESS_BIP;
        initChannelState=CardConnState.CARDCONN_SUCCESS_BIP;
        CardSmsVersion="02";//暂时把cos版本写固定
        return hasCard;
    }


    /**
     * 初始化BIP连接对象
     */
    private boolean initBipHelper () {
        TMKeyLog.d(TAG, "initBipHelper");
        isADN = false;
        isInitSimHelper = false;
        isOpenSesHelper = false;
        isOpenSesDefaultHelper = false;
        isInitBipHelper = true;  //初始化BipHelper
        bipHelper= (BipHelper) channelList.get(4);
        simHelper=bipHelper;
        boolean bipCanSurpport=bipHelper.open();

        return  bipCanSurpport;
    }




    /**
     * 初始化OMA连接对象
     */
    private void initSimHelper () {
        TMKeyLog.d(TAG, "initSimHelper");
        isInitSimHelper = true;
        isOpenSesHelper = false;
        isOpenSesDefaultHelper = false;
        String model = SystemUtil.getMODEL();
        TMKeyLog.d(TAG, "initSimHelper>>>model:" + model);
        sysVersion = Build.VERSION.SDK_INT;
        TMKeyLog.d(TAG, "initSimHelper>>>sysVersion:" + sysVersion);
        int targetSDKVersion = mContext.getApplicationInfo().targetSdkVersion;
        TMKeyLog.d(TAG, "initSimHelper>>>targetSDKVersion:" + targetSDKVersion);
        if ((sysVersion == 28) && (targetSDKVersion >= 28)) { //系统版本为9.0且APP targetSDK版本大于等于9.0，不能使用UICC
            isNeedUICC = false;
        }
        TMKeyLog.d(TAG, "initSimHelper>>>isNeedUICC:" + isNeedUICC + ">>>isNeedOMA:" + isNeedOMA);
        boolean suportUICC = false;
        if (isNeedUICC) {
            suportUICC = initUICCHelper();//初始化UICC模块
        }
        TMKeyLog.d(TAG, "initSimHelper>>>supportUICC:" + suportUICC);
        if (!suportUICC) { //不支持UICC才走OMA
            if (isNeedOMA) {
                initSESHelper();
            } else {
                hasCard = false;
                isInitSimHelper = false;
                if (mOpenChannelCallBack != null) {
                    mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "UICC模式打开通道失败");
                }
            }
        }
    }

    /**
     * 检测是否支持OMA
     * @return
     */
    public boolean checkOMASupport (OmaType omaType) {
        try {
            hasOMAPackage = false;
            switch (omaType) {
                case OMA_P:
                    if (sysVersion >= Build.VERSION_CODES.P) {
                        Class.forName("android.se.omapi.SEService");
                        Class.forName("android.se.omapi.SEService$OnConnectedListener");
                        TMKeyLog.d(TAG, "SEService$OnConnectedListener is exist");
                        hasOMAPackage = true;
                    }
                    break;
                case OMA_INTERFACE:
                    final String SMARTCARD_SERVICE_PACKAGE = "org.simalliance.openmobileapi.service";
                    PackageInfo pi;
                    pi = mContext.getPackageManager().getPackageInfo(SMARTCARD_SERVICE_PACKAGE, 0);
                    String versionName = pi.versionName;
                    String versionCode = pi.versionCode + "";

                    if (versionName != null || versionCode != null) {
                        TMKeyLog.d(TAG, "versionName=" + versionName + ">>>versionCode = " +
                                versionCode);
                    }
                    //检测OMA相关类是否存在
                    Class.forName("org.simalliance.openmobileapi.service.ISmartcardServiceCallback$Stub");
                    TMKeyLog.d(TAG, "ISmartcardServiceCallback$Stub is exist");
                    hasOMAPackage = true;
                    break;
                default:
                    final String SMARTCARD_SERVICE_PACKAGE1 = "org.simalliance.openmobileapi.service";
                    PackageInfo pi1;
                    pi1 = mContext.getPackageManager().getPackageInfo(SMARTCARD_SERVICE_PACKAGE1, 0);
                    String versionName1 = pi1.versionName;
                    String versionCode1 = pi1.versionCode + "";

                    if (versionName1 != null || versionCode1 != null) {
                        TMKeyLog.d(TAG, "versionName=" + versionName1 + ">>>versionCode = " +
                                versionCode1);
                    }
                    //检测OMA相关类是否存在
                    Class.forName("org.simalliance.openmobileapi.SEService$CallBack");
                    TMKeyLog.d(TAG, "SEService$CallBack is exist");
                    hasOMAPackage = true;
                    break;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            hasOMAPackage = false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            hasOMAPackage = false;
        }
        return hasOMAPackage;
    }

    /**
     * 初始化OMA模式
     */
    private void initSESHelper() {
        TMKeyLog.d(TAG, "initSIMHelper");
        logStartTime("initSIMHelper");
        //优先初始化OMA模式，如果OMA模式初始化失败则初始化短信通讯录模式
        TMKeyLog.d(TAG, "cardConnState:" + cardConnState);
        TMKeyLog.d(TAG, "SESHelper instance...");
        if (sysVersion >= Build.VERSION_CODES.P) {
            if (checkOMASupport (OmaType.OMA_P)) {
                TMKeyLog.d(TAG, "initSESHelper>>> support OMA_P");
                isSESDefaultHelperFail = false;
                initSESPHelper();
            } else {
                isSESDefaultHelperFail = true;
                hasCard = false;
                isInitSimHelper = false;
                if (mOpenChannelCallBack != null) {
                    mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "OMA模式打开通道失败,通道不支持");
                }
            }
        } else {
            boolean omaServiceSupport = checkOMASupport(OmaType.OMA_SERVICE);
            TMKeyLog.d(TAG, "initSESHelper>>>omaServiceSupport:" + omaServiceSupport);
            boolean omaInterfaceSupport = checkOMASupport(OmaType.OMA_INTERFACE);
            TMKeyLog.d(TAG, "initSESHelper>>>omaInterfaceSupport:" + omaInterfaceSupport);
            if (!omaServiceSupport) {
                isSESDefaultHelperFail = true;
            }
            if (!omaInterfaceSupport) {
                isSESHelperFail = true;
            }
            if (!omaServiceSupport && !omaInterfaceSupport) { //不支持OMA
                hasCard = false;
                isInitSimHelper = false;
                if (mOpenChannelCallBack != null) {
                    mOpenChannelCallBack.openChannelResult(false, CardConnState.CARDCONN_FAIL, "OMA模式打开通道失败,通道不支持");
                }
                return;
            }
            if (omaServiceSupport) {
                TMKeyLog.d(TAG, "initSESHelper>>> support OMA_SERVICE");
                isSESDefaultHelperFail = false;
                initSESDefaultHelper();
            }
            if (omaInterfaceSupport) {
                TMKeyLog.d(TAG, "initSESHelper>>> support OMA_INTERFACE");
                isSESHelperFail = false;
                sesHelper = new SESHelper(mContext, mCardConCallBack);
                sesHelper.initSimHelper();
            }
        }
    }

    @SuppressWarnings("all")
    private void initSESPHelper () {
        sesSystemHelper = new SESSystemHelper(mContext, mCardConCallBack);
        sesSystemHelper.initSimHelper();
    }

    private void initSESDefaultHelper() {
        TMKeyLog.d(TAG, "initSESDefaultHelper");
        logStartTime("initSESDefaultHelper");
        //优先初始化OMA模式，如果OMA模式初始化失败则初始化短信通讯录模式
        TMKeyLog.d(TAG, "cardConnState:" + cardConnState);
        TMKeyLog.d(TAG, "initSESDefaultHelper instance...");
        sesDefaultHelper = new SESDefaultHelper(mContext, mCardConCallBack);
        sesDefaultHelper.initSimHelper();
    }

    /**
     * 初始化ADN模式
     */
    private boolean initSmsHelper(boolean isLast) {
        TMKeyLog.d(TAG, "initSmsHelper");
        if (isNeedADN) {
            logStartTime("initSmsHelper");
            if (isLast) {//当前已经是OMA或者UICC模式
                return dealSMSHelper();
            } else {
                return dealSMSPer();
            }
        }
        return false;
    }

    /**
     * 初始化UICC模式
     */
    private boolean initUICCHelper() {
        TMKeyLog.d(TAG, "initUICCHelper");
        logStartTime("initUICCHelper");
        uiccHelper = new UICCHelper(mContext, mCardConCallBack);
        boolean isUICC = false;
        if (uiccHelper.isSupport()) { //支持UICC模式
            isUICC = uiccHelper.open();
            TMKeyLog.d(TAG, "isUICC:" + isUICC);
            if (isUICC) {
                cardConnState = CardConnState.CARDCONN_SUCCESS_UICC;
                isADN = false;
                simHelper = uiccHelper;
                if (hasCard()) { //确认是否有卡
                    if (mOpenChannelCallBack != null) {
                        mOpenChannelCallBack.openChannelResult(true, CardConnState.CARDCONN_SUCCESS_UICC, "UICC");
                    }
                    return true;
                } else {
                    isUICC = false;
                    cardConnState = CardConnState.CARDCONN_FAIL;
                    simHelper = null;
                }
            }
        }
        uiccHelper = null;
        logEndTime("UICC connect end");
        return false;
    }

    /**
     * 查找下一个通道
     * @return
     */
    public boolean findNextChannel () {
        TMKeyLog.d(TAG, "findNextChannel>>>currentChannelIndex:" + currentChannelIndex);
        while (currentChannelIndex < channelList.size()) {
            TMKeyLog.d(TAG, "findNextChannel>>>while>>>currentChannelIndex:" + currentChannelIndex);
            smsHelper = channelList.get(currentChannelIndex);
            TMKeyLog.d(TAG, "smsHelper:" + smsHelper.getClass().getName());
            if (! smsHelper.isSupport()) {
                TMKeyLog.d(TAG, "findNextChannel>>>isSupport:" + false);
                if (smsHelper instanceof SMSCenOppoHelper) {
                    isSMSCen = false;
                }
                currentChannelIndex ++;
                continue;
            }
            TMKeyLog.d(TAG, "findNextChannel>>>isSupport:" + true);

            if (smsHelper instanceof SMSHelper) {
                TMKeyLog.d(TAG, "findNextChannel>>>CardSmsVersion:" + CardSmsVersion);
                if ("01".equals(CardSmsVersion)) { //针对老卡，不循环C网的两个ADN通道
                    if (smsHelper instanceof SMSIccEFHelper) {
                        TMKeyLog.d(TAG, "findNextChannel>>>CardSmsVersion is 01 && smsHelper instanceof SMSIccEFHelper>>>continue");
                        currentChannelIndex ++;
                        continue;
                    } else if (smsHelper instanceof SMSSIDHelper) {
                        TMKeyLog.d(TAG, "findNextChannel>>>CardSmsVersion is 01 && smsHelper instanceof SMSSIDHelper>>>continue");
                        currentChannelIndex ++;
                        continue;
                    }
                }
                isADN = true;
                isSMS = true;
                isSurePhoneTag = false;
                PhoneTagStr = PHONEKEY_TAG;
                SharedPreferences.Editor edit = sp.edit();
                edit.putString(PhoneTagKeyStr, PHONEKEY_TAG);
                edit.commit();
                boolean openSmsRes = smsHelper.open();
                if ("01".equals(CardSmsVersion)) { //针对老卡，不循环C网的两个ADN通道
                    if (smsHelper instanceof SMSIccEFHelper) {
                        TMKeyLog.d(TAG, "findNextChannel>>>CardSmsVersion is 01 && smsHelper instanceof SMSIccEFHelper>>>continue");
                        currentChannelIndex ++;
                        continue;
                    } else if (smsHelper instanceof SMSSIDHelper) {
                        TMKeyLog.d(TAG, "findNextChannel>>>CardSmsVersion is 01 && smsHelper instanceof SMSSIDHelper>>>continue");
                        currentChannelIndex ++;
                        continue;
                    }
                    TMKeyLog.d(TAG, "findNextChannel>>>CardSmsVersion is 01>> openDevice success >>> smsHelper:" + smsHelper.getClass().getName());
                }
                if (! openSmsRes) {
                    TMKeyLog.d(TAG, "findNextChannel>>>openDevice:" + false);
                    currentChannelIndex ++;
                    continue;
                } else { //通道打开成功
                    TMKeyLog.d(TAG, "openDevice success >>> smsHelper:" + smsHelper.getClass().getName());
                    boolean openRes = checkChannel_ADN();
                    TMKeyLog.d(TAG, "openDevice success >>> openRes:" + openRes);
                    if (openRes) {
                        hasCard = true;
                        simHelper = smsHelper;
                        return true;
                    } else {
                        currentChannelIndex ++;
                        continue;
                    }
                }
            } else {
                TMKeyLog.d(TAG, "findNextChannel>>> current helper is not SMSHelper");
                currentChannelIndex ++;
                continue;
            }
        }

        if (currentChannelIndex >= channelList.size()) {//最后一个通道
            TMKeyLog.d(TAG, "currentChannelIndex is last openDevice failed");
            setErrorStateCode(ResultStateCode.STATE_FAIL_1000);
            return false;
        }
        return false;
    }

    /**
     * 开启读写短信和读写联系人权限之后的操作
     */
    private boolean dealSMSPer() {
        TMKeyLog.d(TAG, "dealSMSPer");
        boolean findChannelRes = findNextChannel();
        TMKeyLog.d(TAG, "dealSMSPer>>>findChannelRes:" + findChannelRes);
        return findChannelRes;
    }

    /**
     * 针对老卡的处理
     * @return
     */
    private boolean dealSMSHelper() {
        TMKeyLog.e(TAG, "dealSMSHelper");
        currentChannelIndex = channelList.size() - 1;
        smsHelper = channelList.get(currentChannelIndex);
        boolean openSmsRes = smsHelper.open();
        if (! openSmsRes) {
            setErrorStateCode(ResultStateCode.STATE_FAIL_1000);
        }
        TMKeyLog.d(TAG, "openSmsRes:" + openSmsRes);
        return openSmsRes;
    }

    /**
     * 清除key
     */
    public void clearKey() {
        TMKeyLog.d(TAG, "clearKey");
        TRN_KEY = "";
        SSC = 0x99;//会话ID
    }

    /**
     * 关闭通道
     */
    public void closeChannel() {
        TMKeyLog.d(TAG, "closeChannel");
        sesDefaultHelperConState = false;
        sesHelperConState = false;
        isInitSimHelper = false;
        hasOMACallBack = false;
        isOpenSesHelper = false;
        isOpenSesDefaultHelper = false;
        hasCard = false;
        SSC = 0;
        cardConnState = CardConnState.CARDCONN_FAIL;
        if (sesHelper != null) {
            try {
                sesHelper.close();
            } catch (Exception e) {
                TMKeyLog.d(TAG, "sesHelper.close>>>Exception:" + e.getMessage());
            }
        }
        if (sesDefaultHelper != null) {
            try {
                sesDefaultHelper.close();
            } catch (Exception e) {
                TMKeyLog.d(TAG, "sesDefaultHelper.close>>>Exception:" + e.getMessage());
            }
        }
        if (sesSystemHelper != null) {
            try {
                sesSystemHelper.close();
            } catch (Exception e) {
                TMKeyLog.d(TAG, "sesSystemHelper.close>>>Exception:" + e.getMessage());
            }
        }
        if (uiccHelper != null) {
            try {
                uiccHelper.close();
            } catch (Exception e) {
                TMKeyLog.d(TAG, "uiccHelper.close>>>Exception:" + e.getMessage());
            }
        }
        if (smsHelper != null) {
            try {
                smsHelper.close();
            } catch (Exception e) {
                TMKeyLog.d(TAG, "smsHelper.close>>>Exception:" + e.getMessage());
            }
        }
        simHelper = null;
        sesHelper = null;
        sesDefaultHelper = null;
        sesSystemHelper = null;
        uiccHelper = null;
        smsHelper = null;
        tmKeyManager = null;
    }

    /**
     * 针对OMA和UICC通道临时关闭
     */
    public void closeChannel_OMA() {
        TMKeyLog.d(TAG, "closeChannel_OMA");
        sesDefaultHelperConState = false;
        sesHelperConState = false;
        isInitSimHelper = false;
        hasOMACallBack = false;
        isOpenSesHelper = false;
        isOpenSesDefaultHelper = false;
        cardConnState = getCardConState();
        TMKeyLog.d(TAG, "closeChannel_OMA>>>cardConnState:" + cardConnState);
        if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA || cardConnState == CardConnState.CARDCONN_SUCCESS_UICC) {
            hasCard = false;
            SSC = 0;
            //临时关闭通道，不重置通道状态
//            cardConnState = CardConnState.CARDCONN_FAIL;
            if (sesHelper != null) {
                try {
                    sesHelper.close();
                } catch (Exception e) {
                    TMKeyLog.d(TAG, "closeChannel_OMA>>>sesHelper.close>>>Exception:" + e.getMessage());
                }
            }
            if (sesDefaultHelper != null) {
                try {
                    sesDefaultHelper.close();
                } catch (Exception e) {
                    TMKeyLog.d(TAG, "closeChannel_OMA>>>sesDefaultHelper.close>>>Exception:" + e.getMessage());
                }
            }
            if (sesSystemHelper != null) {
                try {
                    sesSystemHelper.close();
                } catch (Exception e) {
                    TMKeyLog.d(TAG, "closeChannel_OMA>>>sesSystemHelper.close>>>Exception:" + e.getMessage());
                }
            }
            if (uiccHelper != null) {
                try {
                    uiccHelper.close();
                } catch (Exception e) {
                    TMKeyLog.d(TAG, "closeChannel_OMA>>>uiccHelper.close>>>Exception:" + e.getMessage());
                }
            }
            simHelper = null;
            sesHelper = null;
            sesDefaultHelper = null;
            sesSystemHelper = null;
            uiccHelper = null;
        }
    }

    //获取当前时间
    private long getCurrentTime() {
        return new Date().getTime();
    }

    private void logStartTime(String s) {
        startTime = getCurrentTime();
        TMKeyLog.e(TAG, s + ">>>startTime:" + startTime);
    }

    private void logEndTime(String s) {
        endTime = getCurrentTime();
        TMKeyLog.e(TAG, s + ">>>endTime:" + endTime + ">>>totalTime:" + (endTime - startTime));
        startTime = 0;
        endTime = 0;
    }

    /**
     * 获取SDK版本号
     * @return
     */
    public String getSDKVersion () {
        return sdkVersion;
    }

    /**
     * 判断是否有贴膜卡
     * @return
     */
    public boolean hasCard () {
        TMKeyLog.d(TAG, "hasCard>>>hasCard:" + hasCard + ">>>initChannelState:" + initChannelState);
        if (hasCard || initChannelState == CardConnState.CARDCONN_SUCCESS_OMA || initChannelState == CardConnState.CARDCONN_SUCCESS_UICC) {
            //有卡或者初始化确认通道是OMA、UICC
            return true;
        }
        TMKeyLog.d(TAG, "hasCard>>>cardConnState:" + cardConnState);
        if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA) { //OMA通道打开成功
            //老卡会返回指令不识别的错误6D00
            int checkRes = checkOmaChannel_New();
            TMKeyLog.d(TAG, "hasCard>>>OMA>>>checkRes:" + checkRes);
            if (checkRes == 1) { //老协议卡
                hasCard = initSmsHelper(true);
            } else if (checkRes == 0) {
                hasCard = true;
            } else if (checkRes == 3) {//COS版本号太高
                hasCard = false;
            } else if (checkRes == 2) { //不支持OMA
                if (sesDefaultHelper != null) {
                    sesDefaultHelper.close();
                    sesDefaultHelper = null;
                }
                if (sesSystemHelper != null) {
                    sesSystemHelper.close();
                    sesSystemHelper = null;
                }
                if (sesHelper != null) {
                    sesHelper.close();
                    sesHelper = null;
                }
                hasCard = false;
                cardConnState = CardConnState.CARDCONN_FAIL;
                return false;
            }
            if (hasCard) {
                initChannelState = CardConnState.CARDCONN_SUCCESS_OMA;
            }
        } else if (cardConnState == CardConnState.CARDCONN_SUCCESS_UICC) {
            //老卡会返回指令不识别的错误6D00
            int checkRes = checkUICCChannel_New();
            TMKeyLog.d(TAG, "hasCard>>>UICC>>>checkRes:" + checkRes);
            if (checkRes == 1) { //老协议卡
                hasCard = initSmsHelper(true);
            } else if (checkRes == 0) {
                hasCard = true;
            } else if (checkRes == 3) { //COS版本号太高
                hasCard = false;
            } else if (checkRes == 2) { //不支持UICC
                if (uiccHelper != null) {
                    cardConnState = CardConnState.CARDCONN_FAIL;
                    uiccHelper.close();
                    uiccHelper = null;
                }
                if (sesDefaultHelperConState) {
                    cardConnState = CardConnState.CARDCONN_SUCCESS_OMA;
                    if (sysVersion >= Build.VERSION_CODES.P) {
                        simHelper = sesSystemHelper;
                    } else {
                        simHelper = sesDefaultHelper;
                        if (sesHelperConState && sesHelper != null) {
                            sesHelper.close();
                        }
                    }
                }
                if (sesHelperConState) {
                    cardConnState = CardConnState.CARDCONN_SUCCESS_OMA;
                    simHelper = sesHelper;
                }
                hasCard = false;
                if (cardConnState == CardConnState.CARDCONN_SUCCESS_OMA) {
                    return hasCard();
                }
                return false;
            }
            if (hasCard) {
                initChannelState = CardConnState.CARDCONN_SUCCESS_UICC;
            }
//        } else {
//            hasCard = initSmsHelper(false);
        }
        TMKeyLog.d(TAG, "hasCard" + hasCard + ">>>CardSmsVersion:" + CardSmsVersion);
        if (Integer.parseInt(CardSmsVersion, 16) > MAXCARDVERSION) { //大于客户端可识别的最大版本号
            TMKeyLog.d(TAG, "Error---------> CardSmsVersion > MAXCARDVERSION");
            setErrorStateCode(ResultStateCode.STATE_FFFA);
            hasCard = false;
        }
        return hasCard;
    }

//    /**
//     * 重置密码，待实现
//     * @param cardNo
//     * @return
//     */
//    private Result restPassword (String cardNo) {
//        Result result = null;
//        String data = "B12F010100";
//        try {
//            clearRevSb();//清空数据缓存准备接收数据
//            CardResult tmCardResult = sendInsideDataApdu(data, true);
//            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
//                String resData = tmCardResult.getData();
//                TMKeyLog.d(TAG, "restPassword>>>resData:" + resData);
//                String state = resData.substring(resData.length() - 4);
//                if (state.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) {//读取数据成功
//                    result = new Result(ResultStateCode.STATE_OK, "重置成功！", "");
//                    return result;
//                }
//            } else if (tmCardResult.getResCode() == FConstant.RES_FAIL_SEND_DATA) {
//                result = new Result("1002", "发送数据失败", "");
//                return result;
//            } else if (tmCardResult.getResCode() == FConstant.RES_FAIL_RECEIVE_DATA) {
//                result = new Result(ResultStateCode.STATE_FAIL_1003, "接收数据失败", "");
//                return result;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        result = new Result(ResultStateCode.STATE_FAIL_1001, "异常", "");
//        return result;
//    }

    /**
     * 获取卡片SN
     * @return
     */
    public String getCardSN (boolean isNeedSend) {
        TMKeyLog.d(TAG, "getCardSN>>>CardSmsVersion:" + CardSmsVersion + ">>>isNeedSend:" + isNeedSend);
        /*if (!isNeedSend && (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= CARDVERSION_02)) {
            CardInfo cardInfo = SIMBaseManager.getInitCardInfo();
            if (cardInfo != null) {
                String cardCsn = cardInfo.getCsn();
                TMKeyLog.d(TAG, "getCardSN>>>cardInfo is not null getCsn:" + cardInfo.getCsn());
                if (! TextUtils.isEmpty(cardCsn)) {
                    return cardInfo.getCsn();
                }
            }
        }*/
        LogToFile.d(TAG, "\n\ngetCardSN");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String sendData = "B11000000C";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getCardSN>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "recData:" + recData);
                if (recData.endsWith(InsideDataStateCode.RES_SUCCESS)) { //接收数据成功
                    if (recData == null || recData.length() < 12) {//接收数据有误
                        setErrorStateCode(ResultStateCode.STATE_FFFE);
                        return "";
                    }
                    int trcLen = recData.length();
                    String tmRecData = recData.substring(0, trcLen - 12);
                    String tmRecMac = recData.substring(trcLen - 12, trcLen - 4);
                    TMKeyLog.d(TAG, "getCardSN>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                    String comTag = "83";
                    if (tmRecData.contains(comTag)) {//卡号包含83的标志位才是正确的
                        TMKeyLog.d(TAG, "getCardSN>>>tmRecData has contains 83");
                        int cIndex = tmRecData.indexOf(comTag);
                        tmRecData = tmRecData.substring(cIndex);
                        return tmRecData;
                    } else {
                        TMKeyLog.d(TAG, "getCardSN>>>tmRecData has not contains 83");
                        return tmRecData;
                    }
                } else {
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                }
            } else {
                setErrorStateCode(ResultStateCode.STATE_FFFC);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return "";
    }

    /**
     * 获取证书信息
     * @return
     */
    public X509Certificate getCerInfo (final int t) {
        TMKeyLog.d(TAG, "getCerInfo>>>t:" + t );
        try {
            byte[] cerBytes = getLocalCerInfo(t);
            if (cerBytes != null) { //读取sha1值进行比较
                byte[] localCerSha1Byte = SHA.getSha1(cerBytes);
                String locss = FCharUtils.showResult16Str(localCerSha1Byte);
                if (localCerSha1Byte != null) {
                    String cerSha1Str = getCerSha1(t);
                    if (cerSha1Str != null && ! "".equals(cerSha1Str)) {
                        if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= CARDVERSION_02) {
                            if (cerSha1Str.length() == 12) {//从固定信息中读取
                                TMKeyLog.d(TAG, "cerSha1Str:" + cerSha1Str + ">>>localcerSha1Str:" + locss);
                                if (cerSha1Str.equalsIgnoreCase(locss.substring(0,6) + locss.substring(locss.length() - 6))) {//两个SHA1值相同
                                    X509Certificate localCer = parseCertInfo(cerDirPath + cerFileName, FCharUtils.showResult16Str(cerBytes));
                                    if (localCer != null) {
                                        TMKeyLog.d(TAG, "localCer is not null");
                                        isTransmitting = false;
                                        return localCer;
                                    }
                                }
                            } else {
                                TMKeyLog.d(TAG, "cerSha1Str:" + cerSha1Str + ">>>localcerSha1Str:" + locss);
                                if (cerSha1Str.equalsIgnoreCase(locss)) {//两个SHA1值相同
                                    X509Certificate localCer = parseCertInfo(cerDirPath + cerFileName, FCharUtils.showResult16Str(cerBytes));
                                    if (localCer != null) {
                                        TMKeyLog.d(TAG, "localCer is not null");
                                        isTransmitting = false;
                                        return localCer;
                                    }
                                }
                            }
                        } else {
                            TMKeyLog.d(TAG, "cerSha1Str:" + cerSha1Str + ">>>localcerSha1Str:" + locss);
                            if (cerSha1Str.equalsIgnoreCase(locss)) {//两个SHA1值相同
                                X509Certificate localCer = parseCertInfo(cerDirPath + cerFileName, FCharUtils.showResult16Str(cerBytes));
                                if (localCer != null) {
                                    TMKeyLog.d(TAG, "localCer is not null");
                                    isTransmitting = false;
                                    return localCer;
                                }
                            }
                        }
                    }
                }
                //证书不为空，删除本地缓存证书
                deleteErrorCer(cerDirPath + cerFileName);
            }

            LogToFile.d(TAG, "\n\ngetCerInfo");
            while (isTransmitting) {
                SystemClock.sleep(FConstant.TRANSMITTINGTIME);
            }
            isNeedVerifyMac = true;
            isTransmitting = true;
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //需要发送指令读取证书数据
                        String sendData = "B1250" + t + "0000";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getCerInfo>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            VCardApi_FFT.mBipEntity.setYangzhengma(""); //验证码设置为空防止多次弹框
            TMKeyLog.d(TAG, "获取证书信息,通道状态码:" + tmCardResult.getResCode() + ">>>通道状态信息:" + FConstant.getCardErrorMsg(tmCardResult.getResCode()));
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String resData = tmCardResult.getData();
                int resLen = resData.length();
                String stateCode = "";
                if (resLen >= 4) {
                    stateCode = resData.substring(resLen - 4);
                    TMKeyLog.d(TAG, "获取证书信息,数据域状态码:" + resData + ">>>数据域状态信息:" + InsideDataStateCode.getInsideErrorMsg(resData));
                    if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) { //返回状态码61XX，继续接收数据
                        String le = stateCode.substring(2);
                        if (resLen >= 12) {
                            resData = resData.substring(0, resLen - 12);//获取有效数据
                        } else {
                            resData = resData.substring(0, resLen - 4);//获取有效数据
                        }
                        String cerHex =  getResponseData(le);
                        if (! "".equals(resData)) {
                            cerHex = resData + cerHex;
                        }
                        return (X509Certificate) saveLocalCerInfo(t, cerHex);
                    } else if (stateCode.contains(InsideDataStateCode.RES_SUCCESS)){
                        if (resLen >= 12){
                            String tmRecData = resData.substring(0, resLen - 12);
                            String tmRecMac = resData.substring(resLen - 12, resLen - 4);
                            TMKeyLog.d(TAG, "getResponseData>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                            return (X509Certificate) saveLocalCerInfo(t, tmRecData);
                        }
                    }
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return null;
    }

    /**
     * 获取国密证书
     * @param t 3--国密签名证书，4--国密加密证书
     * @return
     */
    public X509CertificateStructure getCerInfo_Sm2 (final int t, boolean needSend) {
        TMKeyLog.d(TAG, "getCerInfo_Sm2>>>t:" + t + ">>>needSend:" + needSend);
        try {
            byte[] cerBytes = getLocalCerInfo(t);
            if (cerBytes != null) { //读取sha1值进行比较
                byte[] localCerSha1Byte = SHA.getSha1(cerBytes);
                String locss = FCharUtils.showResult16Str(localCerSha1Byte);
                if (localCerSha1Byte != null) {
                    if (needSend) { //需要发送指令读取sha1值比较
                        String cerSha1Str = getCerSha1(t);
                        if (cerSha1Str != null && ! "".equals(cerSha1Str)) {
                            if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= CARDVERSION_02) {
                                if (cerSha1Str.length() == 12) {//从固定信息中读取
                                    TMKeyLog.d(TAG, "cerSha1Str:" + cerSha1Str + ">>>localcerSha1Str:" + locss);
                                    if (cerSha1Str.equalsIgnoreCase(locss.substring(0,6) + locss.substring(locss.length() - 6))) {//两个SHA1值相同
                                        X509CertificateStructure localCer = parseSm2CertInfo(cerDirPath + cerFileName, FCharUtils.showResult16Str(cerBytes));
                                        if (localCer != null) {
                                            TMKeyLog.d(TAG, "localSM2Cer is not null");
                                            isTransmitting = false;
                                            return localCer;
                                        }
                                    }
                                } else {
                                    TMKeyLog.d(TAG, "cerSha1Str:" + cerSha1Str + ">>>localcerSha1Str:" + locss);
                                    if (cerSha1Str.equalsIgnoreCase(locss)) {//两个SHA1值相同
                                        X509CertificateStructure localCer = parseSm2CertInfo(cerDirPath + cerFileName, FCharUtils.showResult16Str(cerBytes));
                                        if (localCer != null) {
                                            TMKeyLog.d(TAG, "localSM2Cer is not null");
                                            isTransmitting = false;
                                            return localCer;
                                        }
                                    }
                                }
                            } else {
                                TMKeyLog.d(TAG, "cerSha1Str:" + cerSha1Str + ">>>localcerSha1Str:" + locss);
                                if (cerSha1Str.equalsIgnoreCase(locss)) {//两个SHA1值相同
                                    X509CertificateStructure localCer = parseSm2CertInfo(cerDirPath + cerFileName, FCharUtils.showResult16Str(cerBytes));
                                    if (localCer != null) {
                                        TMKeyLog.d(TAG, "localSM2Cer is not null");
                                        isTransmitting = false;
                                        return localCer;
                                    }
                                }
                            }
                        }
                    } else {
                        X509CertificateStructure localCer = parseSm2CertInfo(cerDirPath + cerFileName, FCharUtils.showResult16Str(cerBytes));
                        if (localCer != null) {
                            TMKeyLog.d(TAG, "localSM2Cer is not null");
                            isTransmitting = false;
                            return localCer;
                        }
                    }
                }
                //证书不为空，删除本地缓存证书
                deleteErrorCer(cerDirPath + cerFileName);
            }

            LogToFile.d(TAG, "\n\ngetCerInfo_Sm2");
            while (isTransmitting) {
                SystemClock.sleep(FConstant.TRANSMITTINGTIME);
            }
            isNeedVerifyMac = true;
            isTransmitting = true;
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //需要发送指令读取证书数据
                        String sendData = "B1250" + t + "0000";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getCerInfo_Sm2>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            VCardApi_FFT.mBipEntity.setYangzhengma(""); //验证码设置为空防止多次弹框
            TMKeyLog.d(TAG, "获取证书信息,通道状态码:" + tmCardResult.getResCode() + ">>>通道状态信息:" + FConstant.getCardErrorMsg(tmCardResult.getResCode()));
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String resData = tmCardResult.getData();
                int resLen = resData.length();
                String stateCode = "";
                if (resLen >= 4) {
                    stateCode = resData.substring(resLen - 4);
                    TMKeyLog.d(TAG, "获取证书信息,数据域状态码:" + resData + ">>>数据域状态信息:" + InsideDataStateCode.getInsideErrorMsg(resData));
                    if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) { //返回状态码61XX，继续接收数据
                        String le = stateCode.substring(2);
                        if (resLen >= 12) {
                            resData = resData.substring(0, resLen - 12);//获取有效数据
                        } else {
                            resData = resData.substring(0, resLen - 4);//获取有效数据
                        }
                        String cerHex =  getResponseData(le);
                        if (! "".equals(resData)) {
                            cerHex = resData + cerHex;
                        }
                        return (X509CertificateStructure) saveLocalCerInfo(t, cerHex);
                    } else if (stateCode.contains(InsideDataStateCode.RES_SUCCESS)){
                        if (resLen >= 12){
                            String tmRecData = resData.substring(0, resLen - 12);
                            String tmRecMac = resData.substring(resLen - 12, resLen - 4);
                            TMKeyLog.d(TAG, "getResponseData>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                            return (X509CertificateStructure) saveLocalCerInfo(t, tmRecData);
                        }
                    }
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return null;
    }

    /**
     * 比较缓存的证书SH1值是否与卡片中的值相同
     * @param cerType
     * @return
     */
    private String getCerSha1 (final int cerType) {
        TMKeyLog.d(TAG, "getCerSha1>>>CardSmsVersion:" + CardSmsVersion);
        if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= CARDVERSION_02) {
            CardInfo cardInfo = SIMBaseManager.getInitCardInfo();
            if (cardInfo != null) {
                String shaStr = "";
                if (cerType == VCardApi_FFT.SignTypeData.CER_TYPE_RSA1024 || cerType == VCardApi_FFT.SignTypeData.CER_TYPE_RSA2048) {
                    shaStr = cardInfo.getRsaSha1();
                } else if (cerType == VCardApi_FFT.SignTypeData.CER_TYPE_SM2_SIGNCER) {
                    shaStr = cardInfo.getSm2SignSha1();
                } else {
                    shaStr = cardInfo.getSm2EncSha1();
                }
                TMKeyLog.d(TAG, "getCerSha1>>>cardInfo is not null shaStr:" + shaStr);
                return shaStr;
            }
        }

        LogToFile.d(TAG, "\n\ngetCerSha1");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //获取证书hash值（sha1）
                        String sendData = "B1250" + cerType + "0118";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getCerSha1>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            TMKeyLog.d(TAG, "获取证书摘要信息,通道状态码:" + tmCardResult.getResCode() + ">>>通道状态信息:" + FConstant.getCardErrorMsg(tmCardResult.getResCode()));
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String resData = tmCardResult.getData();
                int resLen = resData.length();
                String stateCode = resData;
                if (resLen > 4) {
                    stateCode = resData.substring(resLen - 4);
                }
                TMKeyLog.d(TAG, "获取证书摘要信息,数据域状态码:" + stateCode + ">>>数据域状态信息:" + InsideDataStateCode.getInsideErrorMsg(resData));
                if (stateCode.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) { //返回状态码9000
                    if (resLen < 12) {//接收数据有误
                        return "";
                    }
                    String tmRecData = resData.substring(0, resLen - 12);
                    String tmRecMac = resData.substring(resLen - 12, resLen - 4);
                    TMKeyLog.d(TAG, "getCerSha1>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                    return  tmRecData;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return "";
    }

    /**
     * 保存证书到本地
     * @param cerType 证书类型
     * @param cerType 证书内容
     * @return
     */
    private Object saveLocalCerInfo (int cerType, String hCerStr) {
        TMKeyLog.d(TAG, "saveLocalCerInfo");
        try {
            File dirFile = new File(cerDirPath);
            if (! dirFile.exists()) {
                dirFile.mkdirs();
            }
            if (VCardApi_FFT.SignTypeData.CER_TYPE_RSA1024 == cerType) {
                cerFileName = "rsa1024.cer";
            } else if (VCardApi_FFT.SignTypeData.CER_TYPE_RSA2048 == cerType) {
                cerFileName = "rsa2048.cer";
            } else if (VCardApi_FFT.SignTypeData.CER_TYPE_SM2_SIGNCER == cerType) {
                cerFileName = "sm2sign.cer";
            } else if (VCardApi_FFT.SignTypeData.CER_TYPE_SM2_ENCCER == cerType) {
                cerFileName = "sm2enc.cer";
            } else {
                return null;
            }
            File cerFile = new File(cerDirPath + cerFileName);
            if (! cerFile.exists()) {
                cerFile.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(cerDirPath + cerFileName, false);
            outputStream.write(FCharUtils.hexString2ByteArray(hCerStr));
            outputStream.close();

            //保存证书成功后进行证书信息解析
            if ((VCardApi_FFT.SignTypeData.CER_TYPE_RSA1024 == cerType) || (VCardApi_FFT.SignTypeData.CER_TYPE_RSA2048 == cerType)) {
                return parseCertInfo(cerDirPath + cerFileName, hCerStr);
            } else if ((VCardApi_FFT.SignTypeData.CER_TYPE_SM2_SIGNCER == cerType) || (VCardApi_FFT.SignTypeData.CER_TYPE_SM2_ENCCER == cerType)) {
                return parseSm2CertInfo(cerDirPath + cerFileName, hCerStr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取本地缓存的证书信息
     * @return
     */
    private byte[] getLocalCerInfo (int ct) {
        TMKeyLog.d(TAG, "getLocalCerInfo");
        try {
            //判断是否已经本地缓存了证书，如果有则直接读取，否则，从卡端读取
            File dirFile = new File(cerDirPath);
            if (! dirFile.exists()) {
                TMKeyLog.d(TAG, "getLocalCerInfo>>>cerDirPath is not exist");
                return null;
            }
            if (VCardApi_FFT.SignTypeData.CER_TYPE_RSA1024 == ct) {
                cerFileName = "rsa1024.cer";
            } else if (VCardApi_FFT.SignTypeData.CER_TYPE_RSA2048 == ct) {
                cerFileName = "rsa2048.cer";
            } else if (VCardApi_FFT.SignTypeData.CER_TYPE_SM2_SIGNCER == ct) {
                cerFileName = "sm2sign.cer";
            } else if (VCardApi_FFT.SignTypeData.CER_TYPE_SM2_ENCCER == ct) {
                cerFileName = "sm2enc.cer";
            }
            File cerFile = new File(cerDirPath + cerFileName);
            TMKeyLog.d(TAG, "getLocalCerInfo>>>cerFile:" + cerFile.getAbsolutePath());
            if (! cerFile.exists() || !cerFile.isFile()) {
                TMKeyLog.d(TAG, "getLocalCerInfo>>>cerFile is not exist");
                return null;
            }
            FileInputStream fis = new FileInputStream(cerFile);
            int length = fis.available();
            byte [] buffer = new byte[length];
            fis.read(buffer);
            fis.close();
            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析RSA证书
     * @param cerinfoHexStr
     * @return
     */
    private X509Certificate parseCertInfo (String fp, String cerinfoHexStr) {
        try {
            // 读取证书文件
            if ("".equals(cerinfoHexStr) || cerinfoHexStr == null)
                return null;

            TMKeyLog.d(TAG, "parseCertInfo>>>cerinfoStr=" + cerinfoHexStr);
            X509Certificate oCert = X509Certificate.getInstance(FCharUtils.hexString2ByteArray(cerinfoHexStr));

            return oCert;
        } catch (Exception e) {
            e.printStackTrace();
            deleteErrorCer(fp);
            TMKeyLog.e("CertManager", "解析证书出错！");
        }
        return null;
    }

    /**
     * 解析SM2证书
     *
     * @param cerinfoHexStr
     * @return
     */
    private X509CertificateStructure parseSm2CertInfo(String fp, String cerinfoHexStr) {
        TMKeyLog.d(TAG, "parseSm2CertInfo");
        try {
            // 读取证书文件
            if ("".equals(cerinfoHexStr) || cerinfoHexStr == null)
                return null;
            TMKeyLog.d(TAG, "cerinfoStr=" + cerinfoHexStr);

            byte[] csCert = FCharUtils.hexString2ByteArray(cerinfoHexStr);
            InputStream inStream = new ByteArrayInputStream(csCert);
            ASN1Sequence seq = null;
            ASN1InputStream aIn;

            aIn = new ASN1InputStream(inStream);
            seq = (ASN1Sequence) aIn.readObject();
            X509CertificateStructure oCert = new X509CertificateStructure(seq);
            return  oCert;

        } catch (Exception e) {
            e.printStackTrace();
            deleteErrorCer(fp);
            TMKeyLog.e("CertManager", "解析证书出错！");
        }
        return null;
    }

    /**
     * 如果证书信息解析错误，则删除本地保存的证书
     */
    private void deleteErrorCer (String filePath) {
        TMKeyLog.d(TAG, "deleteErrorCer");
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        try {
            File f = new File(filePath);
            if (! f.exists()) {
                return;
            }
            f.delete();
        } catch (Exception e) {
            TMKeyLog.d(TAG, "证书文件删除异常，路径为：" + filePath);
        }
    }

    /**
     * 获取Cos版本号，判断是否需要更新
     */
    public String checkCosVersion() {
        TMKeyLog.d(TAG, "checkCosVersion");
        CardResult cr = getAtr();
        if (cr.getResCode() == FConstant.RES_SUCCESS) { //获取ATR成功
            cos_atr = cr.getData();
            return cos_atr;
        }
        return "Error";
    }

    /**
     * 获取卡片信息
     * @return
     * @throws IOException
     */
    public CardInfo getCardInfo (){
        TMKeyLog.d(TAG, "getCardInfo>>>CardSmsVersion:" + CardSmsVersion);
        if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= CARDVERSION_02) {
            CardInfo cardInfo = getInitCardInfo();
            if (cardInfo != null) {
                TMKeyLog.d(TAG, "getCardInfo>>>cardInfo is not null");
                return cardInfo;
            }
        }

        LogToFile.d(TAG, "\n\ngetCardInfo");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            final String sendData = "B1110001FF";
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getCardInfo>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                TMKeyLog.d(TAG, "getCardInfo>>>throw new IOException");
                throw new IOException(exception);
            }
            CardInfo ci = null;
            TMKeyLog.d(TAG, "getCardInfo>>>tmCardResult.getResCode():" + tmCardResult.getResCode());
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String rsStr = tmCardResult.getData();
                TMKeyLog.d(TAG, "rsStr111:" + rsStr);
                if (rsStr.contains(InsideDataStateCode.RES_SUCCESS_CONTINUE_6C)) { //需要继续接收数据
                    int index = rsStr.indexOf(InsideDataStateCode.RES_SUCCESS_CONTINUE_6C);
                    TMKeyLog.d(TAG, "index:" + index);
                    String le = rsStr.substring(index + 2, index + 4);
                    rsStr = getResponseData6C(sendData, le);
                } else if (rsStr.equals(InsideDataStateCode.RES_SUCCESS)){
                    if (rsStr == null || rsStr.length() < 12) {
                        return null;
                    }
                    int trcLen = rsStr.length();
                    String tmRecData = rsStr.substring(0, trcLen - 12);
                    String tmRecMac = rsStr.substring(trcLen - 12, trcLen - 4);
                    TMKeyLog.d(TAG, "getResponseData>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                    rsStr = tmRecData;
                } else {
                    return null;
                }
                TMKeyLog.d(TAG, "rsStr222:" + rsStr);
                if ("".equals(rsStr)) {
                    return null;
                }
                ArrayList<String> rsList = FCharUtils.parseDataLV(rsStr, false);
                if (rsList == null || rsList.size() < 1) {
                    return null;
                }
                String infoData = rsList.get(0);
                ArrayList<String> infoList = FCharUtils.parseDataLV(infoData, false);
                if (infoList == null || infoList.size() < 4) {
                    return null;
                }
                ci = new CardInfo();
                String cerState = infoList.get(0);//证书状态
                byte csc = (byte) (cerState.charAt(0) & 0x00FF);
                cerState = Integer.toBinaryString(csc);
                TMKeyLog.d(TAG, "cerState:" + cerState);
                int cerStateLen = cerState.length();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 8 - cerStateLen; i++) {//补足8位
                    sb.append("0");
                }
                sb.append(cerState);
                cerState = sb.toString();
                String merchantInfo = infoList.get(1);
                String cosVersion = infoList.get(2);
                String csnStr = infoList.get(3).substring(1);
                TMKeyLog.d("TAG", "cerState:" + cerState +
                        "\nmerchantInfo:" + merchantInfo + ">>>" + FCharUtils.hexStr2String(merchantInfo, FConstant.UTF_8) +
                        "\ncosVersion:" + cosVersion + ">>>" + FCharUtils.hexStr2String(cosVersion, FConstant.UTF_8) +
                        "\ncsnStr:" + csnStr
                );
                merchantInfo = FCharUtils.hexStr2String(merchantInfo, FConstant.UTF_8);
                cosVersion = dealCosVer(cosVersion);
                ci.setCer_state_rsa1024(cerState.substring(0, 2));
                ci.setCer_state_rsa2048(cerState.substring(2, 4));
                ci.setCer_state_sm2_sign(cerState.substring(4, 6));
                ci.setCer_state_sm2_enc(cerState.substring(6, 8));
                ci.setMerchantInfo(merchantInfo);
                ci.setCosVersion(cosVersion);
                ci.setCsn(csnStr);
                return ci;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return null;
    }

    /**
     * 校验PIN码
     * @param pinData HEX格式PIN密文
     * @return
     * @throws IOException
     */
    public Result verifyPin (final String pinData) {
        TMKeyLog.d(TAG, "verifyPin");
        LogToFile.d(TAG, "\n\nverifyPin");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String sendData = "B11D0000";
                        String macStr = FCharUtils.bytesToHexStr(tmjni.insideMac(pinData));
                        String pm = pinData + macStr;
                        sendData += FCharUtils.int2HexStr(pm.length() / 2) + pm;//拼接需要发送的指令数据
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "verifyPin>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String rsStr = tmCardResult.getData();
                TMKeyLog.d(TAG, "verifyPin>>>rsStr:" + rsStr);
                int rsLen = rsStr.length();
                if (rsLen >= 4) { //获取状态码
                    rsStr = rsStr.substring(rsLen - 4);
                }
                if (rsStr.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) {
                    return new Result(ResultStateCode.STATE_OK, "登录成功", "6");
                } else if (rsStr.contains(InsideDataStateCode.RES_FAIL_VERIFY_PIN_ERROE)) {
                    String times = rsStr.substring(rsStr.length() - 1);
                    TMKeyLog.d(TAG, "times:" + times);
                    if(times.equals("0")) {
                        return new Result(ResultStateCode.STATE_FAIL_1002, "登录失败", times);
                    }
                    return new Result(ResultStateCode.STATE_FAIL_1001, "登录失败", times);
                } else if (rsStr.equalsIgnoreCase(InsideDataStateCode.RES_FAIL_CARD_LOCK)){
                    TMKeyLog.e(TAG, "PIN码校验错误，错误码：" + rsStr + ">>>错误信息：" + InsideDataStateCode.getInsideErrorMsg(rsStr));
                    return new Result(ResultStateCode.STATE_FAIL_1002, "登录失败", "0");
                } else {
                    TMKeyLog.e(TAG, "PIN码校验错误，错误码：" + rsStr + ">>>错误信息：" + InsideDataStateCode.getInsideErrorMsg(rsStr));
                    return new Result(ResultStateCode.STATE_FAIL_1003, "登录失败", "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            isTransmitting = false;
            return null;
        }
        isTransmitting = false;
        TMKeyLog.e(TAG, "PIN码校验通道指令错误，错误码：" + tmCardResult.getResCode() + ">>>错误信息：" + FConstant.getCardErrorMsg(tmCardResult.getResCode()));
        return new Result(ResultStateCode.STATE_FAIL_1003, "登录失败", "");
        //TODO =============== 测试使用 =================
//        return new Result(ResultStateCode.STATE_FAIL_1003, FConstant.getCardErrorMsg(tmCardResult.getResCode()), "");
        //TODO =============== 测试使用 =================
    }

    /**
     * 获取V盾签名功能自动关闭设置状态
     * @return
     */
    public Result getVCardSignState()
    {
        Result result = null;
        if (sp == null) {
            sp = mContext.getSharedPreferences(FConstant.fft_sdk_sp_name, Activity.MODE_PRIVATE);
        }
        signCloseFun = sp.getBoolean(SignCloseStateName, false);
        if (! signCloseFun) {//V盾签名功能自动关闭
            result = new Result(ResultStateCode.STATE_2000, "V盾签名一直开启，V盾签名将一直在线有效", "");
        } else {
            if (isStartUpCard) {
                result = new Result(ResultStateCode.STATE_2001, "V盾按需开启，同时V盾签名功能已开启", "");
            } else {
                result = new Result(ResultStateCode.STATE_2002, "V盾按需开启，同时V盾签名功能已关闭", "");
            }
        }
        TMKeyLog.d(TAG, "result.state:" + result.getState());
        return result;
    }

    /**
     *
     * @param isClose
     */
    public void setVCardSignState(boolean isClose) {
        TMKeyLog.d(TAG, "setVCardSignState>>>isClose:" + isClose);
        this.signCloseFun = isClose;
        if (sp == null) {
            sp = mContext.getSharedPreferences(FConstant.fft_sdk_sp_name, Activity.MODE_PRIVATE);
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(SignCloseStateName, signCloseFun);
        editor.commit();
    }

    /**
     * 设置签名功能开始状态
     * @param isOpen
     */
    public void setSignOpenState (boolean isOpen) {
        isStartUpCard = isOpen;
    }

    /**
     * 修改PIN码
     * @param oldPin 旧PIN密文
     * @param newPin 新PIN密文
     */
    public Result changePin (final String oldPin, final String newPin) {
        TMKeyLog.d(TAG, "changePin");
        LogToFile.d(TAG, "\n\nchangePin");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            Result result = null;
            if (oldPin == null || "".equals(oldPin)) {
                result = new Result(ResultStateCode.STATE_FAIL_1001, "旧密码为空", "");
                isTransmitting = false;
                return result;
            }
            if (newPin == null || "".equals(newPin)) {
                result = new Result(ResultStateCode.STATE_FAIL_1001, "新密码为空", "");
                isTransmitting = false;
                return result;
            }

            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String sd1 = oldPin + newPin;
                        String macStr = FCharUtils.bytesToHexStr(tmjni.insideMac(sd1));
                        String sendData = "B11E0000" + FCharUtils.int2HexStr(sd1.length() / 2 + 4) +  sd1 + macStr;
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "changePin>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            TMKeyLog.d(TAG, "修改PIN码,通道状态码:" + tmCardResult.getResCode() + ">>>通道状态信息:" + FConstant.getCardErrorMsg(tmCardResult.getResCode()));
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String resData = tmCardResult.getData();
                int resLen = resData.length();
                String stateCode = resData;
                if (resLen > 4) {
                    stateCode = resData.substring(resLen - 4);
                }
                TMKeyLog.d(TAG, "修改PIN码,数据域状态码:" + resData + ">>>数据域状态信息:" + InsideDataStateCode.getInsideErrorMsg(resData));
                if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                    if (SIMBaseManager.getInitCardInfo() != null) {
                        SIMBaseManager.getInitCardInfo().setInitPwd(false);
                    }
                    return new Result(ResultStateCode.STATE_OK, "密码修改成功", "6");
                } else if (stateCode.contains(InsideDataStateCode.RES_FAIL_VERIFY_PIN_ERROE)) {
                    return new Result(ResultStateCode.STATE_FAIL_1000, "密码修改失败", stateCode.substring(stateCode.length() - 1));
                } else if (stateCode.contains(InsideDataStateCode.RES_FAIL_CARD_LOCK)){
                    TMKeyLog.e(TAG, "PIN码校验错误，错误码：" + stateCode + ">>>错误信息：" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                    return new Result(ResultStateCode.STATE_FAIL_1000, "密码修改失败", "0");
                } else if (stateCode.contains(InsideDataStateCode.RES_SUCCESS_CHANGE_INITPIN_NO)){ //初始密码
                    if (SIMBaseManager.getInitCardInfo() != null) {
                        SIMBaseManager.getInitCardInfo().setInitPwd(true);
                    }
                    return new Result(ResultStateCode.STATE_FAIL_1004, "V盾密码为初始密码", "6");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return new Result(ResultStateCode.STATE_FAIL_1004, "密码修改失败", "");
    }

    /**
     * 获取密码重置时的加密数据
     * @return
     */
    public String getCiphertext () {
        TMKeyLog.d(TAG, "getCiphertext");
        LogToFile.d(TAG, "\n\ngetCiphertext");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data = "B12F000084";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(data, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getCiphertext>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String resData = tmCardResult.getData();
                TMKeyLog.d(TAG, "getCiphertext>>>resData:" + resData);
                int trcLen = resData.length();
                String state = resData.substring(resData.length() - 4);
                if (state.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) { //读取数据成功
                    if (resData == null || resData.length() < 12) {
                        return "";
                    }
                    String tmRecData = resData.substring(0, trcLen - 12);
                    String tmRecMac = resData.substring(trcLen - 12, trcLen - 4);
                    TMKeyLog.d(TAG, "getResponseData>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                    return tmRecData;
                } else {
                    if (state.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) {//获取6184后继续读取数据
                        String Le = state.substring(2);
                        if (trcLen >= 12) {
                            resData = resData.substring(0, trcLen - 12);//获取有效数据
                        } else {
                            resData = resData.substring(0, trcLen - 4);//获取有效数据
                        }
                        String ciphertext = getResponseData(Le);
                        if (! "".equals(resData)) {
                            ciphertext = resData + ciphertext;
                        }
                        return ciphertext;
                    }
                }
            } else if (tmCardResult.getResCode() == FConstant.RES_FAIL_SEND_DATA) {
                return ResultStateCode.STATE_FAIL_1002;
            } else if (tmCardResult.getResCode() == FConstant.RES_FAIL_RECEIVE_DATA) {
                return ResultStateCode.STATE_FAIL_1003;
                //TODO =============== 测试使用 =================
//            } else {
//                return FConstant.getCardErrorMsg(tmCardResult.getResCode());
                //TODO =============== 测试使用 =================
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return "";
    }

    /**
     * 密码重置
     * @param encryptedData
     * @return
     */
    public Result resetPwd(final String encryptedData) {
        TMKeyLog.d(TAG, "resetPwd");
        LogToFile.d(TAG, "\n\nresetPwd");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        Result result = null;
        if ((encryptedData == null) || (encryptedData.length() == 0)) {
            result = new Result(ResultStateCode.STATE_FAIL_1001, "encryptedData 为空！", "");
            isTransmitting = false;
            return result;
        }
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data = "B12F0001";
                        String macStr = FCharUtils.bytesToHexStr(tmjni.insideMac(encryptedData));
                        data = data + FCharUtils.int2HexStr(encryptedData.length() / 2 + 4) + encryptedData + macStr;
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(data, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "resetPwd>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String resData = tmCardResult.getData();
                TMKeyLog.d(TAG, "resetPwd>>>resData:" + resData);
                String state = resData.substring(resData.length() - 4);
                if (state.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) {//读取数据成功
                    if (SIMBaseManager.getInitCardInfo() != null) {
                        SIMBaseManager.getInitCardInfo().setInitPwd(true);
                    }
                    result = new Result(ResultStateCode.STATE_OK, "重置成功！", "");
                    return result;
                } else if (state.equalsIgnoreCase(InsideDataStateCode.RES_FAIL_INVALID_RANDOM) || state.equalsIgnoreCase(InsideDataStateCode.RES_FAIL_RESET_PWD_VERIFY)) {
                    result = new Result(ResultStateCode.STATE_FAIL_1004, "V盾不允许重置！", "");
                    return result;
                }
            } else if (tmCardResult.getResCode() == FConstant.RES_FAIL_SEND_DATA) {
                result = new Result(ResultStateCode.STATE_FAIL_1002, "发送数据失败", "");
                return result;
            } else if (tmCardResult.getResCode() == FConstant.RES_FAIL_RECEIVE_DATA) {
                result = new Result(ResultStateCode.STATE_FAIL_1003, "接收数据失败", "");
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        result = new Result(ResultStateCode.STATE_FAIL_1001, "异常", "");
        return result;
    }

    /**
     * 弹出STK菜单
     */
    public void sendPopSTK () {
        TMKeyLog.d(TAG, "sendPopSTK");
        LogToFile.d(TAG, "\n\nsendPopSTK");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        isPopStk = false;//返回了结果后说明Stk菜单已关闭
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data = "";//发送指令确认开启自动关闭V盾签名功能
                        TMKeyLog.d(TAG, "signCloseFun:" + signCloseFun);
                        if (signCloseFun) {
                            data = "B122060000";//发送指令确认关闭自动关闭V盾签名功能
                        } else {
                            data = "B122050000";//发送指令确认开启自动关闭V盾签名功能
                        }
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(data, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "sendPopSTK>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String resData = tmCardResult.getData();
                TMKeyLog.d(TAG, "sendPopSTK>>>resData:" + resData);
                if (resData.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS_ClOSEMODEL_OPEN)) {//确认开启自动关闭功能
                    setSignOpenState(false);
                    setVCardSignState(true);
                } else if (resData.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS_CLOSEMODEL_CLOSE)) {//确认关闭自动关闭功能
                    setSignOpenState(true);
                    setVCardSignState(false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
    }

    /**
     * 检测是否能正常弹出STK菜单
     * @return
     */
    public boolean isCanPopSTK () {
        return  isPopStk;
    }

    /**
     * 是否是初始密码
     * @return
     */
    public boolean isInitPassword () {
        TMKeyLog.d(TAG, "isInitPassword>>>CardSmsVersion:" + CardSmsVersion);
        if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= CARDVERSION_02) {
            CardInfo cardInfo = SIMBaseManager.getInitCardInfo();
            if (cardInfo != null) {
                TMKeyLog.d(TAG, "isInitPassword>>>cardInfo is not null isInitPwd:" + cardInfo.isInitPwd());
                return cardInfo.isInitPwd();
            }
        }

        LogToFile.d(TAG, "\n\nisInitPassword");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data = "B122040000";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(data, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "isInitPassword>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String resData = tmCardResult.getData();
                TMKeyLog.d(TAG, "isInitPassword>>>resData:" + resData);
                if (resData.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS_CHANGE_INITPIN_YES)) {//已修改过PIN
                    return false;
                } else if (resData.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS_CHANGE_INITPIN_NO)){
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        //如果指令交互失败，则认为是修改过密码，避免强制跳转到修改密码界面
        return false;
    }

    /**
     * 弹出STK菜单确认临时开启V盾签名功能
     */
    public void callStkFunctionSetting () {
        LogToFile.d(TAG, "\n\ncallStkFunctionSetting");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data = "B122020000";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(data, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "callStkFunctionSetting>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String resData = tmCardResult.getData();
                TMKeyLog.d(TAG, "callStkFunctionSetting>>>resData:" + resData);
                if (resData.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) {
                    isStartUpCard = true;//开启V盾签名功能
                } else if (resData.equalsIgnoreCase(InsideDataStateCode.RES_FAIL_NOT_STARTUP_CARD)){
                    isStartUpCard = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            isStartUpCard = false;
        }
        isTransmitting = false;
    }

    /**
     * 检测通道是否可用
     * @return
     */
    public boolean checkChannel () {
        TMKeyLog.d(TAG, "checkChannel>>>hasCard111111:" + hasCard);
        return hasCard();
//        if (! hasCard) {
//            hasCard = hasCard();
//        }
//        TMKeyLog.d(TAG, "checkChannel>>>hasCard222222:" + hasCard);
//        if (hasCard) {
//            TMKeyLog.d(TAG, "checkChannel>>>cardConnState:" + cardConnState);
//            String csn = getCardSN(true);
//            TMKeyLog.d(TAG, "checkChannel>>>csn:" + csn);
//            if (csn != null && !"".equals(csn)) { //获取CSN成功
//                hasCard = true;
//                return true;
//            } else {
//                currentChannelIndex++ ;
//                boolean findChannelRes = findNextChannel();
//                if (! findChannelRes) {
//                    hasCard = false;
//                    cardConnState = CardConnState.CARDCONN_FAIL;
//                    setErrorStateCode(ResultStateCode.STATE_FFFB);
//                } else {
//                    return false;
//                }
//            }
//        } else {
//            hasCard = false;
//            cardConnState = CardConnState.CARDCONN_FAIL;
//            setErrorStateCode(ResultStateCode.STATE_FFFB);
//        }
//        return false;
    }

    /**
     * 检测ADN通道是否能正常发送消息
     * @return
     */
    public boolean checkChannel_ADN () {
        TMKeyLog.d(TAG, "checkChannel_ADN>>>hasCard111111:" + hasCard);
        if (hasCard) {
            TMKeyLog.d(TAG, "checkChannel_ADN>>>cardConnState:" + cardConnState);
            String csn = getCardSN(true);
            TMKeyLog.d(TAG, "checkChannel_ADN>>>csn:" + csn);
            if (csn != null && !"".equals(csn)) { //获取CSN成功
                return true;
            } else {
              return false;
            }
        }
        return false;
    }

    /**
     * 计算签名
     * @param application
     * @param signSrcDataName 签名原文
     * @param st 签名类型，1--RSA1024签名，2---RSA2048签名, 3---SM2签名证书
     * @param signRule 需要执行摘要算法时，签名填充规则 0---当算法为SM3时表示普通hash，1--当算法为SM3时表示以签名为目的hash，否则表示SHA1，2---SHA256
     * @param algorithmType 需要执行摘要算法时，算法 1---SHA1，2---SHA256,3---SM3
     * @param pinCode 密码，字符串
     * @param needReadCert 是否需要读取SM2证书获取公钥
     */
    public String getSignData(Application application, final byte[] signSrcDataName, final int st, final String signRule, final String algorithmType , final String pinCode, boolean needReadCert, boolean isUpdateCert) {
        TMKeyLog.d(TAG, "getSignData");
        if (signSrcDataName == null) {
            TMKeyLog.d(TAG, "getSignData>>> signSrcDataName is null");
            return null;
        }
        try {
            tmCardResult = null;
            String tmSignSrcDataName = FCharUtils.bytesToHexStr(signSrcDataName);
            tmCardResult = dealSDGetSignData(tmSignSrcDataName, pinCode, st, signRule, algorithmType, needReadCert, isUpdateCert);
            isTransmitting = false;
            setSignOpenState(false);//签名结束，将V盾签名功能关闭
            TMKeyLog.d(TAG, "======= dealGetSignData end ========");
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String recData = tmCardResult.getData();
                int recLen = recData.length();
                if (recLen < 4) {
                    //签名失败
                    TMKeyLog.e(TAG, "签名失败, 获取签名数据长度错误");
                    return "";
                }
                String stateCode = recData.substring(recLen - 4);
                TMKeyLog.d(TAG, "getSignData>>>stateCode:" + stateCode);
                if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) { //返回状态码61XX，开始接收数据
                    String le = stateCode.substring(2);
                    if (recLen >= 12) {
                        recData = recData.substring(0, recLen - 12);//获取有效数据
                    } else {
                        recData = recData.substring(0, recLen - 4);//获取有效数据
                    }
                    String signAfterData =  getResponseData(le);
                    if (signAfterData == null || "".equals(signAfterData)) {
                        TMKeyLog.e(TAG, "获取签名数据操作失败");
                        return "";
                    }
                    if (! "".equals(recData)) {
                        signAfterData = recData + signAfterData;
                    }
                    //TODO 签名成功
                    return signAfterData;
                } else if (stateCode.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) { //签名成功
                    if (recData == null || recData.length() < 12) {
                        return "";
                    }
                    int trcLen = recData.length();
                    String tmRecData = recData.substring(0, trcLen - 12);
                    String tmRecMac = recData.substring(trcLen - 12, trcLen - 4);
                    TMKeyLog.d(TAG, "getResponseData>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                    return tmRecData;
                } else if (stateCode.contains(InsideDataStateCode.RES_FAIL_VERIFY_PIN_ERROE)) {
                    String times = stateCode.substring(stateCode.length() - 1);
                    TMKeyLog.d(TAG, "times:" + times);
                    return times;
                } else if (stateCode.equalsIgnoreCase(InsideDataStateCode.RES_FAIL_CARD_LOCK)){
                    TMKeyLog.e(TAG, "PIN码校验错误，错误码：" + stateCode + ">>>错误信息：" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                    return "0";
                } else {//错误状态
                    TMKeyLog.e(TAG, "签名失败，错误码：" + stateCode + ">>>错误信息：" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                    return "";
                }
            }
            TMKeyLog.e(TAG, "签名失败，通道指令错误，错误码：" + tmCardResult.getResCode() + ">>>错误信息：" + FConstant.getCardErrorMsg(tmCardResult.getResCode()));
            return "";
            //======= 私有指令流程签名END =======
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        return "";
    }

    /**
     * 发送签名指令获取签名
     * @param signData 待签名数据
     * @param st 签名类型，1--RSA1024签名，2---RSA2048签名, 3---SM2签名证书
     * @param signRule 需要执行摘要算法时，签名填充规则 0---当算法为SM3时表示普通hash，1--当算法为SM3时表示以签名为目的hash，否则表示SHA1，2---SHA256
     * @param algorithmType 需要执行摘要算法时，算法 1---SHA1，2---SHA256,3---SM3
     * @return
     * @throws IOException
     */
    private CardResult dealSDGetSignData (String signData, String pinData, int st, String signRule, String algorithmType, boolean needReadCert, boolean isUpdateCert) throws IOException {
        TMKeyLog.e(TAG, "dealSDGetSignData>>>原文:" + signData + ">>>pinData:" + pinData + ">>>signType:" + st);
        tmCardResult = null;
        if ((st == VCardApi_FFT.SignTypeData.SIGN_TYPE_SM2_SIGNCER_HASH)
                && signRule.equals(VCardApi_FFT.SignTypeData.ABSTRACT_RULE_SHA1)
                && algorithmType.equals(VCardApi_FFT.SignTypeData.ABSTRACT_TYPE_SM3)) {

            //国密签名需要将公钥添加到原文前
            //如果是通过创建密钥对获取的公钥信息直接将公钥信息
            TMKeyLog.d(TAG, "dealSDGetSignData>>>needReadCert:" + needReadCert);
            if (needReadCert) {
                X509CertificateStructure x = getCerInfo_Sm2(VCardApi_FFT.SignTypeData.CER_TYPE_SM2_SIGNCER, false);
                if (x != null) {
                    try {
                        sm2SignSupplement = FCharUtils.showResult16Str(x.getSubjectPublicKeyInfo().getPublicKeyData().getEncoded()).substring(8);
                        if (isNeedSignHashData) {
                            if (! isUpdateCert) {
                                String smX = sm2SignSupplement.substring(0, 64);
                                String smY = sm2SignSupplement.substring(64);
                                byte[] zpading = SM2.getInstance().sm2GetZSM(FCharUtils.hexString2ByteArray(SM2Util.SM2UserId), smX, smY);
                                sm2SignSupplement = FCharUtils.bytesToHexStr(zpading);
                                TMKeyLog.d(TAG, "sm2SignSupplement:" + sm2SignSupplement);
                                signData = sm2SignSupplement + signData;
                                signData = SM3.sm3Hash(signData);
                            }
                        } else {
                            TMKeyLog.d(TAG, "sm2SignSupplement:" + sm2SignSupplement);
                            st = VCardApi_FFT.SignTypeData.SIGN_TYPE_SM2_SIGNCER;
                            signData = sm2SignSupplement + signData;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (isNeedSignHashData) {
                    if (! isUpdateCert) {
                        String smX = sm2SignSupplement.substring(0, 64);
                        String smY = sm2SignSupplement.substring(64);
                        byte[] zpading = SM2.getInstance().sm2GetZSM(FCharUtils.hexString2ByteArray(SM2Util.SM2UserId), smX, smY);
                        sm2SignSupplement = FCharUtils.bytesToHexStr(zpading);
                        TMKeyLog.d(TAG, "sm2SignSupplement:" + sm2SignSupplement);
                        signData = sm2SignSupplement + signData;
                        signData = SM3.sm3Hash(signData);
                    }
                } else {
                    TMKeyLog.d(TAG, "sm2SignSupplement:" + sm2SignSupplement);
                    st = VCardApi_FFT.SignTypeData.SIGN_TYPE_SM2_SIGNCER;
                    signData = sm2SignSupplement + signData;
                }
            }
        } else if ((st == VCardApi_FFT.SignTypeData.SIGN_TYPE_RSA1024_HASH)
                && signRule.equals(VCardApi_FFT.SignTypeData.ABSTRACT_RULE_SHA1)
                && algorithmType.equals(VCardApi_FFT.SignTypeData.ABSTRACT_TYPE_SHA1)) {
            if (isNeedSignHashData) {
                if (! isUpdateCert) {
                    signData = FCharUtils.bytesToHexStr(SHA.getSha1(FCharUtils.hexString2ByteArray(signData)));
                }
            } else {
                st = VCardApi_FFT.SignTypeData.SIGN_TYPE_RSA1024;
            }
        } else if ((st == VCardApi_FFT.SignTypeData.SIGN_TYPE_RSA2048_HASH)
                && signRule.equals(VCardApi_FFT.SignTypeData.ABSTRACT_RULE_SHA256)
                && algorithmType.equals(VCardApi_FFT.SignTypeData.ABSTRACT_TYPE_SHA256)) {
            if (isNeedSignHashData) {
                if (!isUpdateCert) {
                    signData = FCharUtils.bytesToHexStr(SHA.getSha256(FCharUtils.hexString2ByteArray(signData)));
                }
            } else {
                st = VCardApi_FFT.SignTypeData.SIGN_TYPE_RSA2048;
            }
        }
        TMKeyLog.d(TAG, "计算摘要之后的signData:" + signData);
        sm2SignSupplement = "";

        final ArrayList<String> sList = packSignSendData(0, signData, st, signRule, algorithmType, pinData);

        if (sList == null) {
            TMKeyLog.e(TAG, "数据分包处理错误");
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_UNKNOWN_APP);
            return tmCardResult;
        }

        LogToFile.d(TAG, "\n\nSignData");
        String recData = "";
        tmCardResult = null;
        exception = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //循环发送指令完成数据摘要的计算
                    int ll = sList.size();
                    TMKeyLog.d(TAG, "sList.size:" + ll);
                    String sendData = "";
                    clearRevSb();//清空数据缓存准备接收数据
                    for (int i = 0; i < ll; i ++) {
                        while (isTransmitting) {
                            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
                        }
                        isNeedVerifyMac = true;
                        isTransmitting = true;
                        sendData = sList.get(i);
                        tmCardResult = sendInsideDataApdu(sendData, true);
                        isTransmitting = false;
                    }
                } catch (IOException e) {
                    exception = e;
                    e.printStackTrace();
                } finally {
                    TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                    //解锁
                    conditionVariable.open();
                }
            }
        }).start();
        TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
        //加锁
        conditionVariable.close();//复位
        boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
        TMKeyLog.d(TAG, "dealSDGetSignData>>>notTimeOut:" + notTimeOut);
        isTransmitting = false;
        if (exception != null || tmCardResult == null) {//发送数据异常导致
            throw new IOException(exception);
        }
        if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
            recData = tmCardResult.getData();
            if (recData == null) {
                tmCardResult = new CardResult();
                tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                return tmCardResult;
            }
            int recLen = recData.length();
            TMKeyLog.d(TAG, "recLen:" + recLen);
            if (recLen < 4) {
                tmCardResult = new CardResult();
                tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
                return tmCardResult;
            }
        }
        return tmCardResult;
    }

    /**
     * 对发送数据进行分包
     * @param dataType 数据类型，0--签名数据，1--设置指纹公钥或指纹验证
     * @param data 待签名数据
     * @param st 签名类型，1--RSA1024签名，2---RSA2048签名, 3---SM2签名证书
     * @param signRule 需要执行摘要算法时，签名填充规则 0---当算法为SM3时表示普通hash，1--当算法为SM3时表示以签名为目的hash，否则表示SHA1，2---SHA256
     * @param algorithmType 需要执行摘要算法时，算法 1---SHA1，2---SHA256,3---SM3
     * @return
     */
    private ArrayList<String> packSignSendData (int dataType, String data, int st, String signRule, String algorithmType, String pd) {
        ArrayList<String> sendDataList = new ArrayList<String>();
        StringBuilder sb = null;
        int dLen = data.length();
        TMKeyLog.d(TAG, "dLen:" + dLen);
        int pdL = pd.length();
//        int currentPackLen = 0xF0 - ( pdL / 2);//当前包长
        int currentPackLen = 0xB0 - ( pdL / 2);//当前包长，由于多包传输时，卡片端接收数据受限，长度调整为0xB0
        String currentPackData = "";//当前包数据
        String currentPackMac = "";//当前包MAC
        int currentIndex = 0;//索引
        int totalPack = dLen / (currentPackLen * 2);
        if (dLen % (currentPackLen * 2) != 0) {
            totalPack += 1;
        }
        TMKeyLog.d(TAG, "totalPack:" + totalPack);
        String cmdStart = (dataType == 0 ? "B12D" : (dataType == 1 ? "B039" : "B12D"));
        if (totalPack == 1) { //只有一包数据
            sb = new StringBuilder(cmdStart);
            sb.append(signRule);
            sb.append(algorithmType);
            if (dataType == 1) {
                sb.append("1");
            } else {
                sb.append(Integer.toHexString(st).toUpperCase());
            }
            sb.append("0");
            sb.append(FCharUtils.int2HexStr(dLen / 2 + pdL / 2 + 4));
            data = pd + data;
            sb.append(data);
            currentPackMac = FCharUtils.showResult16Str(tmjni.insideMac(data));
            sb.append(currentPackMac);
            sendDataList.add(sb.toString());
        } else { //多包数据，保存为多条指令
            for (int i = 0; i < totalPack; i++) {
                sb = new StringBuilder(cmdStart);
                sb.append(signRule);
                sb.append(algorithmType);
                if (dataType == 1) {
                    sb.append("1");
                } else {
                    sb.append(Integer.toHexString(st).toUpperCase());
                }
                if (i == 0) {
                    sb.append("1");
                } else if (i == totalPack - 1) {
                    sb.append("3");
                    currentPackLen = dLen / 2 - (i * currentPackLen);
                } else {
                    sb.append("2");
                }
                sb.append(FCharUtils.int2HexStr(currentPackLen + pdL / 2 + 4));
                currentPackData =  pd + data.substring(currentIndex, currentIndex + currentPackLen * 2);
                sb.append(currentPackData);
                currentPackMac = FCharUtils.showResult16Str(tmjni.insideMac(currentPackData));
                sb.append(currentPackMac);
                TMKeyLog.e(TAG, "packSignSendData>>>i:" + i + ">>>currentIndex:" + currentIndex + ">>>sb:" + sb.toString());
                sendDataList.add(sb.toString());
                currentIndex += currentPackLen * 2;
            }
        }
        return sendDataList;
    }

    /**
     * 检测是否包含OMA相关jar包
     * @return
     */
    public boolean isHasOMAPackage() {
        return hasOMAPackage;
    }

    /**
     * 写入Hash值
     * @return
     */
    public CardResult writeHashCode (final String ydHashCode, final String ltHashCode, final String dxHashCode, final String appHashCode, final String pinStr) {
        TMKeyLog.d(TAG, "writeHashCode>>>ydHashCode:" + ydHashCode + ">>>ltHashCode:" + ltHashCode + ">>>dxHashCode:" + dxHashCode + ">>>appHashCode:" + appHashCode);
        if (ydHashCode == null || ydHashCode.length() != 40) {
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
            tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return tmCardResult;
        }
        if (ltHashCode == null || ltHashCode.length() != 40) {
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
            tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return tmCardResult;
        }
        if (dxHashCode == null || dxHashCode.length() != 40) {
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
            tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return tmCardResult;
        }
        Result verifyRes = verifyPin(pinStr);
        if (ResultStateCode.STATE_FAIL_1001.equals(verifyRes.getState())){
            tmCardResult.setResCode(FConstant.RES_FAIL_VERIFY_PIN);//PIN码校验错误
            tmCardResult.setErrMsg("密码错了，您还可以输入" + verifyRes.getTimes() + "次。");
            return tmCardResult;
        } else if (ResultStateCode.STATE_FAIL_1002.equals(verifyRes.getState())) {
            tmCardResult.setResCode(FConstant.RES_FAIL_CARD_LOCK);//锁卡
            tmCardResult.setErrMsg("您已连续6次输入错误的V盾密码，卡片已锁定，请到银行柜台办理“V盾密码重置”业务解除锁定");
            return tmCardResult;
        } else if (ResultStateCode.STATE_FAIL_1003.equals(verifyRes.getState())){
            tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//锁卡
            tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return tmCardResult;
        }

        LogToFile.d(TAG, "\n\nwriteHashCode");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "30";
                        String P1 = "00";
                        String P2 = "0";
                        String dt = ydHashCode + ltHashCode + dxHashCode;
                        if (appHashCode == null || "".equals(appHashCode) || appHashCode.length() != 40) {//不需要写入APP的hashCode
                            P2 += "0";
                        } else {
                            P2 += "1";
                            dt += appHashCode;
                        }
                        String macStr = FCharUtils.bytesToHexStr(tmjni.insideMac(dt));
                        dt = dt + macStr;
                        String sendData = CLA + INS + P1 + P2 + FCharUtils.int2HexStr(dt.length() / 2) + dt;
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "writeHashCode>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收错误
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
                        tmCardResult.setErrMsg("错误码：" + stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 读取Hash值
     * @return
     */
    public CardResult readHashCode () {
        TMKeyLog.d(TAG, "readHashCode");
        LogToFile.d(TAG, "\n\nreadHashCode");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "30";
                        String P1 = "00";
                        String P2 = "11";
                        String sendData = CLA + INS + P1 + P2 + "54";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "readHashCode>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 12) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "readHashCode>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        String dt = recData.substring(0, trcLen - 12);
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(dt);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
                        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 获取卡片初始化随机数
     * @return
     */
    private CardResult getInitRandom () {
        TMKeyLog.d(TAG, "getInitRandom");
        LogToFile.d(TAG, "\n\ngetInitRandom");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "12";
                        String P1 = "00";
                        String P2 = "00";
                        String sendData = CLA + INS + P1 + P2 + "0C";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getInitRandom>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "getInitRandom>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 12) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "getInitRandom>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        String dt = recData.substring(0, trcLen - 12);
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(dt);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 初始化卡片校验
     * @return
     */
    private CardResult initVerify () {
        TMKeyLog.d(TAG, "initVerify");
        tmCardResult = getInitRandom();
        if (tmCardResult.getResCode() != FConstant.RES_SUCCESS) { //获取预制随机数成功
            tmCardResult.setErrMsg("获取随机数失败，" + tmCardResult.getErrMsg());
            return tmCardResult;
        }

        LogToFile.d(TAG, "\n\ninitVerify");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "13";
                        String P1 = "00";
                        String P2 = "00";
                        String ranStr = tmCardResult.getData();
                        String data = FCharUtils.showResult16Str(tmjni.deseseEncCbc(FCharUtils.hexString2ByteArray(ranStr)));
                        String mac = FCharUtils.showResult16Str(tmjni.insideMac(data));
                        TMKeyLog.d(TAG, "initVerify>>>data:" + data + ">>>mac:" + mac);
                        String sendData = CLA + INS + P1 + P2 + (FCharUtils.int2HexStr(data.length() / 2 + 4)) + data + mac;
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "initVerify>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "initVerify>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) { //接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "initVerify>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 初始化卡片
     * @return
     */
    public CardResult initCard() {
        TMKeyLog.d(TAG, "initCard");
        tmCardResult = initVerify();
        if (tmCardResult.getResCode() != FConstant.RES_SUCCESS) {
            String errMsg = tmCardResult.getErrMsg();
            if (errMsg.startsWith("获取随机数失败")) {
                return tmCardResult;
            }
            tmCardResult.setErrMsg("预制授权认证失败，" + tmCardResult.getErrMsg());
            return tmCardResult;
        }

        LogToFile.d(TAG, "\n\ninitCard");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "16";
                        String P1 = "00";
                        String P2 = "00";
                        String sendData = CLA + INS + P1 + P2 + "00";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "initCard>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "initCard>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "initCard>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 创建P10字符串
     * @param certKeyTypes
     * @param hashTypes
     * @return
     */
    public Map<Integer, CardResult> createCertP10(final ArrayList<Integer> certKeyTypes, ArrayList<Integer> hashTypes) {
        TMKeyLog.d(TAG, "createCertP10");
        LogToFile.d(TAG, "\n\ncreateCertP10");
        if (certKeyTypes == null || certKeyTypes.isEmpty()) {
            TMKeyLog.d(TAG, "createCertP10>>>certKeyTypes == null || certKeyTypes.isEmpty()");
            return null;
        }
        int ctsLen = certKeyTypes.size();
        TMKeyLog.d(TAG, "createCertP10>>>ctsLen:" + ctsLen);
        if (hashTypes == null || hashTypes.isEmpty() || hashTypes.size() != ctsLen) {
            TMKeyLog.d(TAG, "createCertP10>>>hashTypes == null || hashTypes.isEmpty() || hashTypes.size() != ctsLen");
            return null;
        }
        Map<Integer, CardResult> map = new HashMap<>();
        String pubKey = "";
        int certKeyType = VCardApi_FFT.RSA;
        int hashType = 0;
        boolean isInitCert = false; //是否是第一次创建证书
        for (int i = 0; i < certKeyTypes.size(); i ++) {
            certKeyType = certKeyTypes.get(i);
            hashType = hashTypes.get(i);
            TMKeyLog.d(TAG, "createCertP10>>>certKeyType:" + certKeyType + ">>>hashType:" + hashType);
            tmCardResult = createKeypairPart(certKeyType, hashType);
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS) { //创建密钥对成功，对dn做签名
                pubKey = tmCardResult.getData();
                TMKeyLog.d(TAG, "createCertP10>>>pubKey:" + pubKey);
//                tmCardResult = new CardResult();
//                if (certKeyType == VCardApi_FFT.RSA && hashType == VCardApi_FFT.SHA1) { //RSA1024
//                    pubKey = "D26AC5E9F62BC958C2CDC1D2A161E379B96BD085BDA64F0B4B7C203AF651060A24DA8A0009AECA149077D49D68AF35A74EBDC205F2E6FC4485C5D28EB924E7CADF16314B5DF1ABA87117544A9928ED3953913ECFE1B6E0EEFAE9E34114D58273FCC95F33D3E691603161EA957C584CC07EF6AECF312B9E960B3F1CDB7D9DFF6F";
//                } else if (certKeyType == VCardApi_FFT.RSA && hashType == VCardApi_FFT.SHA256) { //RSA2048
//                    pubKey = "CC476E369DD4C9E2BD2C45E2BFDC480B47EC3E5DF2851C0D6BFDC8251270068B8340A41A479CD4A6CFCA3C55C74AE3EA1B6AE3AC9D8530CD0AE4C5FE9FF56A9C718E25E93865D5CFAAAA0FB186327F692C9824D05692E7E4EF62E30B4789419E76FD4D8DB62F9065194AC94F2E63E9E092698C85CE7ECD86460ECFA2D361F8E0A5F13AFC117828316F1DF920FA7F826C7B5EF323E7C93B78B917006F5053CA83F557D264001454E23B9D65E0C95A289E36712B1A5FFB30E8961B948122AB163CBD9F6CC2F305411073EAFBB51D6B4C404E696C69D0C8DD4EA0BDE3B926239C07A608C7D648B38E2C3DEB345EDEF9F77E450F004786B0C56181228C92670B8E13";
//                } else { //SM2
//                    pubKey = "92E4869E3133964D899444740E8746E599CD748AA709F72EA560E1927A1AB8D8CFFD42278E08F88B2C84CE81EF08A5128869DAA7E2D0E03FDD3F2AEEA86A0E99";
//                }
                //dn中第一个0xFF字节替换为公钥信息，然后做签名获取P10字符串
                if (TextUtils.isEmpty(pubKey)) {
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
                    tmCardResult.setErrMsg("创建密钥对失败");
                    map.put(certKeyTypes.get(i), tmCardResult);
                } else {
                    String certDn = "";
                    GmSSL gmSSL = new GmSSL();
                    String lFiled = "";
                    byte[] pubKeyBytes = FCharUtils.hexStrToBytes(pubKey);
                    byte[] backHash = null;
                    if (certKeyType == VCardApi_FFT.RSA) {
                        int iAlgType = (hashType == VCardApi_FFT.SHA1 ? GmSSL.CERT_ALG_TYPE.SHA1 : (hashType == VCardApi_FFT.SHA256 ? GmSSL.CERT_ALG_TYPE.SHA256 : GmSSL.CERT_ALG_TYPE.MD5));

                        TMKeyLog.d(TAG, "createCertP10>>>isInitCert:" + isInitCert);
                        if (! isInitCert) {
                            int readCertType = VCardApi_FFT.SignTypeData.CER_TYPE_RSA1024;
                            if (hashType == VCardApi_FFT.SHA1) {
                                certDn = sp.getString(DN_RSA1024, "");
                                readCertType = VCardApi_FFT.SignTypeData.CER_TYPE_RSA1024;
                            } else {
                                certDn = sp.getString(DN_RSA2048, "");
                                readCertType = VCardApi_FFT.SignTypeData.CER_TYPE_RSA2048;
                            }
                            if (TextUtils.isEmpty(certDn)) {
                                TMKeyLog.d(TAG, "createCertP10>>>RSA certDn is null");
                                X509Certificate cert = getCerInfo(readCertType);
                                if (cert == null) {
                                    TMKeyLog.d(TAG, "createCertP10>>>RSA cert is null");
                                    isInitCert = true;
                                    lFiled = getCardSN(false);
                                } else {
                                    TMKeyLog.d(TAG, "createCertP10>>>RSA cert is not null");
                                    certDn = cert.getSubjectDN().toString();
                                    lFiled = certDn;
                                    SharedPreferences.Editor editor = sp.edit();
                                    if (hashType == VCardApi_FFT.SHA1) {
                                        editor.putString(DN_RSA1024, certDn);
                                    } else {
                                        editor.putString(DN_RSA2048, certDn);
                                    }
                                    editor.commit();
                                }
                            } else {
                                TMKeyLog.d(TAG, "createCertP10>>>RSA certDn is not null-->" + certDn);
                                String csn = getCardSN(false);
                                TMKeyLog.d(TAG, "createCertP10>>>RSA csn:" + csn);
                                if (certDn.contains(csn)) {
                                    lFiled = certDn;
                                } else {
                                    isInitCert = true;
                                    lFiled = getCardSN(false);
                                }
                            }
                        } else {
                            lFiled = getCardSN(false);
                        }
                        TMKeyLog.d(TAG, "createCertP10>>>lFiled:" + lFiled);
                        long getRsaHashRes = gmSSL.genRSAHash(iAlgType, isInitCert, lFiled, pubKeyBytes, pubKeyBytes.length);
                        TMKeyLog.d(TAG, "createCertP10>>>getRsaHashRes:" + getRsaHashRes);
                        backHash = gmSSL.RSAbackHash;
                        TMKeyLog.d(TAG, "createCertP10>>>getRsaHashResHex:" + FCharUtils.bytesToHexStr(backHash));
                    } else {
                        TMKeyLog.d(TAG, "createCertP10>>>SM2 isInitCert:" + isInitCert);
                        int readCertType = VCardApi_FFT.SignTypeData.CER_TYPE_SM2_SIGNCER;
                        if (! isInitCert) {
                            certDn = sp.getString(DN_SM2, "");
                            if (TextUtils.isEmpty(certDn)) {
                                TMKeyLog.d(TAG, "createCertP10>>>SM2 certDn is null");
                                X509CertificateStructure cert = getCerInfo_Sm2(readCertType, true);
                                if (cert == null) {
                                    TMKeyLog.d(TAG, "createCertP10>>>SM2 cert is null");
                                    isInitCert = true;
                                    lFiled = getCardSN(false);
                                } else {
                                    TMKeyLog.d(TAG, "createCertP10>>>SM2 cert is not null");
                                    certDn = cert.getSubject().toString();
                                    lFiled = certDn;
                                    SharedPreferences.Editor editor = sp.edit();
                                    editor.putString(DN_SM2, certDn);
                                    editor.commit();
                                }
                            } else {
                                TMKeyLog.d(TAG, "createCertP10>>>SM2 certDn is not null-->" + certDn);
                                String csn = getCardSN(false);
                                TMKeyLog.d(TAG, "createCertP10>>>SM2 csn:" + csn);
                                if (certDn.contains(csn)) {
                                    lFiled = certDn;
                                } else {
                                    isInitCert = true;
                                    lFiled = getCardSN(false);
                                }
                            }
                        } else {
                            lFiled = getCardSN(false);
                        }
                        TMKeyLog.d(TAG, "createCertP10>>>lFiled:" + lFiled);
                        long getSM2HashRes = gmSSL.genSM2Hash(isInitCert, lFiled, pubKeyBytes, pubKeyBytes.length);
                        TMKeyLog.d(TAG, "createCertP10>>>getSM2HashRes:" + getSM2HashRes);
                        backHash = gmSSL.SM2backHash;
                        TMKeyLog.d(TAG, "createCertP10>>>getSM2HashResHex:" + FCharUtils.bytesToHexStr(backHash));
                    }
                    //签名
                    String signature_ = "";
                    int signType = 0;
                    String signRule = "1";
                    try {
                        if (certKeyType == VCardApi_FFT.RSA && hashType == VCardApi_FFT.SHA1) {
                            signType = VCardApi_FFT.SignTypeData.SIGN_TYPE_RSA1024_HASH;
                            signRule = VCardApi_FFT.SignTypeData.ABSTRACT_RULE_SHA1;
                        } else if (certKeyType == VCardApi_FFT.RSA && hashType == VCardApi_FFT.SHA256) {
                            signType = VCardApi_FFT.SignTypeData.SIGN_TYPE_RSA2048_HASH;
                            signRule = VCardApi_FFT.SignTypeData.ABSTRACT_RULE_SHA256;
                        } else if (certKeyType == VCardApi_FFT.SM2) {
                            signType = VCardApi_FFT.SignTypeData.SIGN_TYPE_SM2_SIGNCER_HASH;
                            signRule = VCardApi_FFT.SignTypeData.ABSTRACT_RULE_SHA1;
                            sm2SignSupplement = pubKey;
                            sm2SignPubKey = pubKey;//临时保存公钥信息
                        } else {
                            signType = VCardApi_FFT.SignTypeData.SIGN_TYPE_RSA1024_HASH;
                            signRule = VCardApi_FFT.SignTypeData.ABSTRACT_RULE_SHA1;
                        }

                        String algorithmType = "1";
                        if (hashType == VCardApi_FFT.SHA1) {
                            algorithmType = VCardApi_FFT.SignTypeData.ABSTRACT_TYPE_SHA1;
                        } else if (hashType == VCardApi_FFT.SHA256) {
                            algorithmType = VCardApi_FFT.SignTypeData.ABSTRACT_TYPE_SHA256;
                        } else if (hashType == VCardApi_FFT.SM3) {
                            algorithmType = VCardApi_FFT.SignTypeData.ABSTRACT_TYPE_SM3;
                        } else {
                            algorithmType = VCardApi_FFT.SignTypeData.ABSTRACT_TYPE_SHA1;
                        }
                        //强制SDK计算Hash
                        boolean needReset = false;
                        if (! isNeedSignHashData) {
                            needReset = true;
                            isNeedSignHashData = true;
                        }
                        signature_ = getSignData(null, backHash, signType, signRule, algorithmType, SM3.sm3Hash(FCharUtils.string2HexStr("123456")), false, true);
//                        signature_ = "70B08892810D8A57008875DBECA8700F39159695244C8AB5478BB8693CABB938087AD2B212CA8F748A09D0AB051A4E64B35D4953CFC9467E1AD7E4B9B52D4D43E92EFC8D10DA1F3B925603E99DD43D7042C26DAA41232F4897BA7EDA2D1ED74D207EE78EFC2B3C7502F4F1AC7AAAFC7E9936221541221618BB97C14EE7902852528DA769D68E2D49007964805E28064544526F96BB988EE7E3E9610C83F59FD2F72596207CE9CEC3A7B4E089C9FD239F44F23DB8D45240ECE7C250583DF84DF107F1A830F682367F9BDF549B1361CC25C6EB54A65BBC8B5EF7BFADA3EC4FB71F2667E90A811B638699869AA3C449797D832E203BAEC846D9BC0E31C87D0DA9E4";
                        if (needReset) {
                            isNeedSignHashData = false;
                        }
                        TMKeyLog.d(TAG, "createCertP10>>>signature_:" + signature_);
                        if (signature_ == null || "".equals(signature_)) {
                            tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
                            tmCardResult.setErrMsg("签名操作失败");
                            map.put(certKeyType, tmCardResult);
                            continue;
                        }
                        if (signature_.length() <= 2) {
                            String errStr1 = "密码错误，您还可以输入";
                            String errStr2 = "次。";
                            if ("5".equals(signature_)
                            || "4".equals(signature_)
                            || "3".equals(signature_)
                            || "2".equals(signature_)
                            || "1".equals(signature_)
                            ) {
                                tmCardResult.setResCode(FConstant.RES_FAIL_VERIFY_PIN);
                                tmCardResult.setErrMsg(errStr1 + signature_ + errStr2);
                            } else if ("0".equals(signature_)) {
                                tmCardResult.setResCode(FConstant.RES_FAIL_CARD_LOCK);
                                tmCardResult.setErrMsg("PIN错误次数超限，卡片已锁定");
                            }
                            map.put(certKeyType, tmCardResult);
                            continue;
                        }
                        signature_ = signature_.replaceAll(" ", "");
                        TMKeyLog.d(TAG, "createCertP10>>>签名值：" + signature_);

                        String p10Str = "";
                        if (certKeyType == VCardApi_FFT.RSA) { //RSA
                            byte[] signBytes = FCharUtils.hexStrToBytes(signature_);
                            long rsaP10Res = gmSSL.genRSAP10(signBytes, signBytes.length);
                            TMKeyLog.d(TAG, "createCertP10>>>rsaP10Res:" + rsaP10Res);
                            p10Str = new String (gmSSL.RSAPkcs10);
                        } else { //SM2
                            byte[] signBytes = FCharUtils.hexStrToBytes(signature_);
                            long sm2P10Res = gmSSL.genSM2P10(signBytes, signBytes.length);
                            TMKeyLog.d(TAG, "createCertP10>>>sm2P10Res:" + sm2P10Res);
                            p10Str = new String(gmSSL.SM2Pkcs10);
                        }
                        TMKeyLog.d(TAG, "createCertP10>>>p10StrBase64:" + p10Str);
                        TMKeyLog.d(TAG, "createCertP10>>>p10StrHex:" + FCharUtils.bytesToHexStr(Base64.decode(p10Str, Base64.NO_WRAP)));
                        TMKeyLog.d(TAG, "createCertP10>>>p10Str:" + new String (Base64.decode(p10Str, Base64.NO_WRAP)));
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);
                        tmCardResult.setData(p10Str);
                        map.put(certKeyType, tmCardResult);
                    } catch (Exception e1) {
                        tmCardResult.setResCode(FConstant.RES_FAIL_SIGN);
                        tmCardResult.setErrMsg("签名异常");
                        map.put(certKeyType, tmCardResult);
                        continue;
                    }
                }
            } else { //创建密钥对失败
                map.put(certKeyType, tmCardResult);
            }
        }
        return map;
    }

    /**
     * 创建密钥对
     * @param certKeyType
     * @return
     */
    public CardResult createKeypairPart(final Integer certKeyType, final int hashType) {
        TMKeyLog.d(TAG, "createKeypairPart");
        LogToFile.d(TAG, "\n\ncreateKeypairPart");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = new CardResult();
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "26";
                        String P1 = "0";
                        if (certKeyType == VCardApi_FFT.RSA && hashType == VCardApi_FFT.SHA1) {
                            P1 += "1";
                        } else if (certKeyType == VCardApi_FFT.RSA && hashType == VCardApi_FFT.SHA256) {
                            P1 += "2";
                        } else if (certKeyType == VCardApi_FFT.SM2) {
                            P1 += "3";
                        }
                        String P2 = "00";
                        String sendData = CLA + INS + P1 + P2 + "00";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "createKeypair>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "createKeypair>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "createKeypair>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        String dt = recData.substring(0, trcLen - 12);
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(dt);
                        return tmCardResult;
                    } else if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) {
                        String Le = stateCode.substring(2);
                        if (trcLen >= 12) {
                            recData = recData.substring(0, trcLen - 12);//获取有效数据
                        } else {
                            recData = recData.substring(0, trcLen - 4);//获取有效数据
                        }
                        String pubKey = getResponseData(Le);
                        if (! "".equals(recData)) {
                            pubKey = recData + pubKey;
                        }
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(pubKey);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 导入证书
     * @param certMap
     * @return
     */
    public Map<Integer, CardResult>

    importCert(Map<Integer, byte[]> certMap, String encCertPriKey, String encProKey) {
        TMKeyLog.d(TAG, "importCert");
        LogToFile.d(TAG, "\n\nimportCert");
        if (certMap == null || certMap.isEmpty()) {
            return null;
        }
        Set<Integer> set = certMap.keySet();
        Iterator iterator = set.iterator();
        Map<Integer, CardResult> resultMap = new HashMap<>();
        int ckt = 0;
        while (iterator.hasNext()) {
            ckt = (Integer) iterator.next();
            if (ckt == VCardApi_FFT.SM2_ENC) {
                tmCardResult = importEncCert(certMap.get(ckt), ckt, encCertPriKey, encProKey);
            } else {
                tmCardResult = importCertDeal(certMap.get(ckt), ckt);
            }
            resultMap.put(ckt, tmCardResult);
        }
        return resultMap;
    }

    /**
     * 导入指定证书
     * @param certB
     * @param certType
     * @return
     */
    private CardResult importCertDeal(byte[] certB, int certType) {
        TMKeyLog.d(TAG, "importCertDeal");
        LogToFile.d(TAG, "\n\nimportCertDeal");
        String certData = FCharUtils.showResult16Str(certB);
        int tempCertType = 1;//1--RSA1024;2--RSA2048;3--SM2;4--SM2_ENC
        if (certType == VCardApi_FFT.RSA) {
            //通过公钥长度判断是RSA1024或RSA2048
            try {
                TMKeyLog.d(TAG, "importCertDeal>>>RSA>>>certB111:" + FCharUtils.bytesToHexStr(certB));
                GmSSL gmSSL= new GmSSL();
                long converRes = gmSSL.convertPkcs7ToPemHex(certB, certB.length);
                TMKeyLog.d(TAG, "importCertDeal>>>RSA>>>converRes:" + converRes);
                if (converRes != 0) { //转换数据成功
                    certB = gmSSL.pemBytes;
                    TMKeyLog.d(TAG, "importCertDeal>>>RSA>>>certB222:" + FCharUtils.bytesToHexStr(certB));
                }
                X509Certificate oCert = X509Certificate.getInstance(certB);
                byte[] pubKeyBs = oCert.getPublicKey().getEncoded();
                TMKeyLog.d(TAG, "importCertDeal>>>pubKeyBsLen:" + pubKeyBs.length);
                if (pubKeyBs.length > 128) { //RSA2048
                    tempCertType = 2;
                } else {
                    tempCertType = 1;
                }
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        } else if (certType == VCardApi_FFT.SM2) {
            tempCertType = 3;
        } else if (certType == VCardApi_FFT.SM2_ENC) {
            tempCertType = 4;
        }
        TMKeyLog.d(TAG, "importCertDeal>>>tempCertType:" + tempCertType);
        int packLen = 0x80;
        TMKeyLog.d(TAG, "importCertDeal>>certData:" + certData);
        int dataLen = certData.length() / 2;
        int packCount = dataLen / packLen;
        if (dataLen % packLen != 0) {
            packCount ++;
        }

        String a = "0";
        String b = "1";
        if (tempCertType == 1) {
            b = "1";
        } else if (tempCertType == 2) {
            b = "2";
        } else if (tempCertType == 3) {
            b = "3";
        } else if (tempCertType == 4) {
            b = "4";
        }
        if (packCount == 1) { //只有一包数据
            a = "0";
            return importCertPart(certData, a, b, "01");
        } else {
            String certPart = "";
            for (int i = 0; i < packCount; i ++) {
                if (i == 0) {
                    a = "1";
                    certPart = certData.substring(i * packLen * 2, (i + 1) * packLen * 2);
                } else if (i == packCount - 1) {
                    a = "3";
                    certPart = certData.substring(i * packLen * 2);
                } else {
                    a = "2";
                    certPart = certData.substring(i * packLen * 2, (i + 1) * packLen * 2);
                }
                tmCardResult = importCertPart(certPart, a, b, FCharUtils.int2HexStr(i + 1));
                if (tmCardResult.getResCode() == FConstant.RES_SUCCESS) {
                    String recData = tmCardResult.getData();
                    if (InsideDataStateCode.RES_SUCCESS.equals(recData)) {
                        continue;
                    } else {
                        return tmCardResult;
                    }
                } else {
                    return tmCardResult;
                }
            }
        }
        tmCardResult = new CardResult();
        setErrorStateCode(ResultStateCode.STATE_FFFF);
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA);//发送接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA));
        return tmCardResult;
    }

    /**
     * 导入加密证书
     * @param certB
     * @param encCertPriKey
     * @param encProKey
     * @return
     */
    private CardResult importEncCert (byte[] certB, int ckt, String encCertPriKey, String encProKey) {
        TMKeyLog.d(TAG, "importEncCert");
        LogToFile.d(TAG, "\n\nimportEncCert");
        if (TextUtils.isEmpty(encCertPriKey)
                ) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("加密证书私钥密文数据有误");
            return tmCardResult;
        }
        if (TextUtils.isEmpty(encProKey)
                ) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("加密证书私钥保护密钥密文数据有误");
            return tmCardResult;
        }
        //将encCertPriKey和encProKey转为Hex编码
        encCertPriKey = FCharUtils.bytesToHexStr(Base64.decode(encCertPriKey, Base64.NO_WRAP));
        encProKey = FCharUtils.bytesToHexStr(Base64.decode(encProKey, Base64.NO_WRAP));
        //获取加密证书公钥
        X509CertificateStructure ci = parseSm2CertInfo(null, FCharUtils.bytesToHexStr(certB));
        if (ci == null) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("加密证书解析失败");
            return tmCardResult;
        }
        TMKeyLog.d(TAG, "importEncCert>>>parseSm2CertInfo success");
        String sm2EncPubKey = "";
        try {
            sm2EncPubKey = FCharUtils.showResult16Str(ci.getSubjectPublicKeyInfo().getPublicKeyData().getEncoded()).substring(8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        TMKeyLog.d(TAG, "importEncCert>>>sm2EncPubKey:" + sm2EncPubKey);
        if (sm2EncPubKey.length() != 128) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("加密证书公钥长度解析错误，为:" + sm2EncPubKey.length());
            return tmCardResult;
        }
        tmCardResult = importEncCertPubKey(sm2EncPubKey);

        if (tmCardResult.getResCode() != FConstant.RES_SUCCESS) {
            return tmCardResult;
        }
        TMKeyLog.d(TAG, "importEncCert>>>importEncCertPubKey success");
        int encProKeyLen = encProKey.length();
        TMKeyLog.d(TAG, "importEncCert>>>encProKeyLen:" + encProKeyLen + ">>>encProKey111:" + encProKey);
        if (encProKeyLen > (96 * 2 + 16 * 2)) {
            String c1c3 = encProKey.substring(0, 192);
            String c2 = encProKey.substring(encProKeyLen - 32);
            encProKey = c1c3 + c2;
        }
        TMKeyLog.d(TAG, "importEncCert>>>encProKey222:" + encProKey);

        tmCardResult = decEncProKey(encProKey);
        if (tmCardResult.getResCode() != FConstant.RES_SUCCESS) {
            return tmCardResult;
        }
        TMKeyLog.d(TAG, "importEncCert>>>decEncProKey success");
        tmCardResult = decncCertPriKey(encCertPriKey);
        if (tmCardResult.getResCode() != FConstant.RES_SUCCESS) {
            return tmCardResult;
        }
        TMKeyLog.d(TAG, "importEncCert>>>decncCertPriKey success");
        tmCardResult = importCertDeal(certB, ckt);
        return tmCardResult;
    }

    /**
     * 导入加密证书公钥
     * @param sm2EncPubKey
     * @return
     */
    private CardResult importEncCertPubKey (final String sm2EncPubKey) {
        TMKeyLog.d(TAG, "importEncCertPubKey");
        LogToFile.d(TAG, "\n\nimportEncCertPubKey");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = new CardResult();
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "2B";
                        String P1 = "00";
                        String P2 = "00";
                        String sendData = CLA + INS + P1 + P2 + FCharUtils.int2HexStr(sm2EncPubKey.length() / 2 + 4) + sm2EncPubKey + FCharUtils.showResult16Str(tmjni.insideMac(sm2EncPubKey));
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "importEncCertPubKey>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "importEncCertPubKey>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "importEncCertPubKey>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(recData);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 解密加密证书私钥保护密钥
     * @param encProKey
     * @return
     */
    private CardResult decEncProKey (final String encProKey) {
        TMKeyLog.d(TAG, "decEncProKey");
        LogToFile.d(TAG, "\n\ndecEncProKey");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = new CardResult();
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "23";
                        String P1 = "00";
                        String P2 = "00";
                        String sendData = CLA + INS + P1 + P2 + FCharUtils.int2HexStr(encProKey.length() / 2 + 2) + encProKey + FCharUtils.showResult16Str(tmjni.insideMac(encProKey)).substring(0, 4);
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "decEncProKey>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "decEncProKey>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "decEncProKey>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(recData);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 解密加密证书私钥
     * @param encCertPriKey
     * @return
     */
    private CardResult decncCertPriKey (final String encCertPriKey) {
        TMKeyLog.d(TAG, "decncCertPriKey");
        LogToFile.d(TAG, "\n\ndecncCertPriKey");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = new CardResult();
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "28";
                        String P1 = "00";
                        String P2 = "00";
                        String sendData = CLA + INS + P1 + P2 + FCharUtils.int2HexStr(encCertPriKey.length() / 2 + 4) + encCertPriKey + FCharUtils.showResult16Str(tmjni.insideMac(encCertPriKey));
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "decncCertPriKey>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "decncCertPriKey>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "decncCertPriKey>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(recData);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 导入证书指令交互
     * @param certData 证书内容
     * @param a
     * @param b
     * @param xx
     * @return
     */
    private CardResult importCertPart(final String certData, final String a, final String b, final String xx) {
        TMKeyLog.d(TAG, "importCertPart");
        LogToFile.d(TAG, "\n\nimportCertPart");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = new CardResult();
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String CLA = "B1";
                        String INS = "15";
                        String P1 = a + b;
                        String P2 = xx;
                        String sendData = CLA + INS + P1 + P2 + FCharUtils.int2HexStr(certData.length() / 2 + 4) + certData + FCharUtils.showResult16Str(tmjni.insideMac(certData));
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "importCert>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "importCert>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "importCert>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(recData);
                        return tmCardResult;
                    } else if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) {
                        String Le = stateCode.substring(2);
                        String cr = getResponseData(Le);
                        if (TextUtils.isEmpty(cr)) { //数据接收错误
                            setErrorStateCode(ResultStateCode.STATE_FFFE);
                            tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                            tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                            return tmCardResult;
                        }
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(cr + InsideDataStateCode.RES_SUCCESS);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * Cos更新获取ATR
     * @return
     */
    public CardResult getAtr () {
        TMKeyLog.d(TAG, "getAtr");
        LogToFile.d(TAG, "\n\ngetAtr");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = false;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String sendData = "B236000011";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getAtr>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) { //指令交互成功
                String rsStr = tmCardResult.getData();
                TMKeyLog.d(TAG, "getAtr>>>rsStr:" + rsStr);
                int rsLen = rsStr.length();
                if (rsLen < 4) { //获取状态码
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                }
                String stateCode = rsStr.substring(rsLen - 4);
                TMKeyLog.d(TAG, "getAtr>>>stateCode:" + stateCode);
                if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                    String data = rsStr.substring(0, rsLen - 4);
                    tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                    tmCardResult.setData(data);
                    return tmCardResult;
                } else if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) {
                    String Le = stateCode.substring(2);
                    rsStr = rsStr.substring(0, rsLen - 4);//获取有效数据
                    String cr = getResponseData(Le);
                    if (! "".equals(rsStr)) {
                        cr = rsStr + cr;
                    }
                    tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                    tmCardResult.setData(cr);
                    return tmCardResult;
                } else {
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                    tmCardResult.setData(stateCode);
                    return tmCardResult;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据发送错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * COS更新获取随机数
     * @return
     */
    public CardResult getCosRandom() {
        TMKeyLog.d(TAG, "getCosRandom>>>CardSmsVersion:" + CardSmsVersion);
        LogToFile.d(TAG, "\n\ngetCosRandom");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = false;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String sendData = "B220010008";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getCosRandom>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "getCosRandom>>>recData:" + recData);
                int recLen = recData.length();
                String stateCode = recData.substring(recLen - 4);
                if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) { //接收数据成功
                    if (recLen < 20) {//接收数据有误
                        setErrorStateCode(ResultStateCode.STATE_FFFE);
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                        return tmCardResult;
                    }
                    String tmRecData = recData.substring(0, recLen - 4);
                    TMKeyLog.d(TAG, "getCosRandom>>>tmRecData:" + tmRecData);
                    tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                    tmCardResult.setData(tmRecData);
                    return tmCardResult;
                } else if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) {
                    String le = stateCode.substring(2, 4);
                    recData = recData.substring(0, recLen - 4);//获取有效数据
                    String csvHex = getResponseData(le);
                    if (!"".equals(recData)) {
                        csvHex = recData + csvHex;
                    }
                    recLen = csvHex.length();
                    if (recLen < 20) {//接收数据有误
                        setErrorStateCode(ResultStateCode.STATE_FFFE);
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                        return tmCardResult;
                    }
                    String tmRecData = csvHex.substring(0, recLen - 4);
                    TMKeyLog.d(TAG, "getCosRandom>>>tmRecData:" + tmRecData);
                    tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                    tmCardResult.setData(tmRecData);
                    return tmCardResult;
                } else {
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(stateCode + "--->" + InsideDataStateCode.getInsideErrorMsg(stateCode));
                    tmCardResult.setData(stateCode);
                    return tmCardResult;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据发送错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     * 透传指令
     * @return
     */
    public CardResult transApdu (final String apdu) {
        TMKeyLog.d(TAG, "transApdu>>>apdu:" + apdu);
        LogToFile.d(TAG, "\n\ntransApdu");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = false;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(apdu, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "transApdu>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "transApdu>>>recData:" + recData);
                int trcLen = recData.length();
                if (trcLen < 4) {//接收数据有误
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                    tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                    return tmCardResult;
                } else {
                    String stateCode = recData.substring(trcLen - 4);
                    TMKeyLog.d(TAG, "transApdu>>>stateCode:" + stateCode);
                    if (stateCode.equals(InsideDataStateCode.RES_SUCCESS)) {
                        tmCardResult.setResCode(FConstant.RES_SUCCESS);//数据接收成功
                        tmCardResult.setData(recData);
                        return tmCardResult;
                    } else {
                        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);//数据接收错误
                        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
                        tmCardResult.setData(stateCode);
                        return tmCardResult;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);//数据接收错误
        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
        return tmCardResult;
    }

    /**
     *  检测当前版本是否支持指纹认证
     * @return
     */
    public boolean checkFingerSupport () {
        TMKeyLog.d(TAG, "checkFingerSupport");
        CardInfo cardInfo = getInitCardInfo();
        if (cardInfo == null) {
            return false;
        }
        String ccv = cardInfo.getCosVersion();
        TMKeyLog.d(TAG, "checkFingerSupport>>>ccv:" + ccv);
        String[] ccvs = ccv.split("\\.");
        if (ccvs.length < 4) {
            return false;
        }
        if ("1".equals(ccvs[0]) && "0".equals(ccvs[1])) { //老版本不支持指纹功能
            return false;
        }
        return true;
    }

    /**
     * 获取指纹认证随机数
     * @return
     */
    public CardResult getFingerRandom () {
        TMKeyLog.d(TAG, "getFingerRandom");
        LogToFile.d(TAG, "\n\ngetFingerRandom");
        while (isTransmitting) {
            SystemClock.sleep(FConstant.TRANSMITTINGTIME);
        }
        isNeedVerifyMac = true;
        isTransmitting = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String sendData = "B03900000C";
                        clearRevSb();//清空数据缓存准备接收数据
                        tmCardResult = sendInsideDataApdu(sendData, true);
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "getFingerRandom>>>notTimeOut:" + notTimeOut);
            isTransmitting = false;
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "getFingerRandom>>>recData:" + recData);
                if (recData.endsWith(InsideDataStateCode.RES_SUCCESS)) { //接收数据成功
                    if (recData == null || recData.length() < 12) { //接收数据有误
                        setErrorStateCode(ResultStateCode.STATE_FFFE);
                        tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
                        tmCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
                        return tmCardResult;
                    }
                    int trcLen = recData.length();
                    String tmRecData = recData.substring(0, trcLen - 12);
                    String tmRecMac = recData.substring(trcLen - 12, trcLen - 4);
                    TMKeyLog.d(TAG, "getFingerRandom>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
                    tmCardResult.setResCode(FConstant.RES_SUCCESS);
                    tmCardResult.setData(tmRecData);
                    return tmCardResult;
                } else {
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                    tmCardResult.setErrMsg("接收数据错误");
                    return tmCardResult;
                }
            } else {
                setErrorStateCode(ResultStateCode.STATE_FFFC);
                tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                tmCardResult.setErrMsg("指令交互失败");
                return tmCardResult;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
        tmCardResult.setErrMsg("获取签名原文失败");
        return tmCardResult;
    }

    /**
     * 获取指纹认证随机数
     * @return
     */
    public CardResult setFingerPubKey (final int pubKeyType, byte[] fingerPubKey, byte[] pinData) {
        TMKeyLog.d(TAG, "setFingerPubKey");
        LogToFile.d(TAG, "\n\nsetFingerPubKey");
        if (pubKeyType != 0 && pubKeyType != 1 && pubKeyType != 2) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("公钥类型错误");
            return tmCardResult;
        }

        if (fingerPubKey == null) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("公钥数据为空");
            return tmCardResult;
        }
        String sendData = FCharUtils.bytesToHexStr(fingerPubKey) + FCharUtils.hexStr2LV_1(FCharUtils.bytesToHexStr(pinData));
        final ArrayList<String> sList = packSignSendData(1, sendData, 0, "1", "" + pubKeyType, "");

        if (sList == null) {
            TMKeyLog.e(TAG, "数据分包处理错误");
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_UNKNOWN_APP);
            return tmCardResult;
        }

        isNeedVerifyMac = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int ll = sList.size();
                        TMKeyLog.d(TAG, "sList.size:" + ll);
                        String sendData = "";
                        clearRevSb();//清空数据缓存准备接收数据
                        for (int i = 0; i < ll; i ++) {
                            while (isTransmitting) {
                                SystemClock.sleep(FConstant.TRANSMITTINGTIME);
                            }
                            isNeedVerifyMac = true;
                            isTransmitting = true;
                            sendData = sList.get(i);
                            tmCardResult = sendInsideDataApdu(sendData, true);
                            isTransmitting = false;
                        }
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "setFingerPubKey>>>notTimeOut:" + notTimeOut);
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "setFingerPubKey>>>recData:" + recData);
                if (recData.endsWith(InsideDataStateCode.RES_SUCCESS)) { //接收数据成功
                    tmCardResult.setResCode(FConstant.RES_SUCCESS);
                    tmCardResult.setData("设置指纹认证功能成功");
                    return tmCardResult;
                } else {
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                    tmCardResult.setErrMsg("接收数据错误");
                    return tmCardResult;
                }
            } else {
                setErrorStateCode(ResultStateCode.STATE_FFFC);
                tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                tmCardResult.setErrMsg("指令交互失败");
                return tmCardResult;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
        tmCardResult.setErrMsg("设置指纹认证功能失败");
        return tmCardResult;
    }

    /**
     * 获取指纹认证随机数
     * @return
     */
    public CardResult verifyFinger (final int pubKeyType, byte[] fingerData) {
        TMKeyLog.d(TAG, "verifyFinger");
        LogToFile.d(TAG, "\n\nverifyFinger");
        if (pubKeyType != 0 && pubKeyType != 1 && pubKeyType != 2) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("公钥类型错误");
            return tmCardResult;
        }

        if (fingerData == null) {
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            tmCardResult.setErrMsg("验证数据为空");
            return tmCardResult;
        }

        final ArrayList<String> sList = packSignSendData(1, FCharUtils.bytesToHexStr(fingerData), 0, "2", "" + pubKeyType, "");

        if (sList == null) {
            TMKeyLog.e(TAG, "数据分包处理错误");
            tmCardResult = new CardResult();
            tmCardResult.setResCode(FConstant.RES_FAIL_UNKNOWN_APP);
            return tmCardResult;
        }

        isNeedVerifyMac = true;
        try {
            tmCardResult = null;
            exception = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int ll = sList.size();
                        TMKeyLog.d(TAG, "sList.size:" + ll);
                        String sendData = "";
                        clearRevSb();//清空数据缓存准备接收数据
                        for (int i = 0; i < ll; i ++) {
                            while (isTransmitting) {
                                SystemClock.sleep(FConstant.TRANSMITTINGTIME);
                            }
                            isNeedVerifyMac = true;
                            isTransmitting = true;
                            sendData = sList.get(i);
                            tmCardResult = sendInsideDataApdu(sendData, true);
                            isTransmitting = false;
                        }
                    } catch (IOException e) {
                        exception = e;
                        e.printStackTrace();
                    } finally {
                        TMKeyLog.e(TAG, "==startTmie--->endTime==" + new Date().getTime());
                        //解锁
                        conditionVariable.open();
                    }
                }
            }).start();
            TMKeyLog.e(TAG, "==startTmie==" + new Date().getTime());
            //加锁
            conditionVariable.close();//复位
            boolean notTimeOut = conditionVariable.block(FConstant.LOCKTIME);//锁定
            TMKeyLog.d(TAG, "verifyFinger>>>notTimeOut:" + notTimeOut);
            if (exception != null || tmCardResult == null) {//发送数据异常导致
                throw new IOException(exception);
            }
            if (tmCardResult.getResCode() == FConstant.RES_SUCCESS && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
                String recData = tmCardResult.getData();
                TMKeyLog.d(TAG, "verifyFinger>>>recData:" + recData);
                if (recData.endsWith(InsideDataStateCode.RES_SUCCESS)) { //接收数据成功
                    tmCardResult.setResCode(FConstant.RES_SUCCESS);
                    tmCardResult.setData("指纹认证成功");
                    return tmCardResult;
                } else {
                    setErrorStateCode(ResultStateCode.STATE_FFFE);
                    tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                    tmCardResult.setErrMsg("接收数据错误");
                    return tmCardResult;
                }
            } else {
                setErrorStateCode(ResultStateCode.STATE_FFFC);
                tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                tmCardResult.setErrMsg("指令交互失败");
                return tmCardResult;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTransmitting = false;
        tmCardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
        tmCardResult.setErrMsg("指纹认证失败");
        return tmCardResult;
    }
}
