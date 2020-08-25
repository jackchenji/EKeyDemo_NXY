package com.froad.ukey.manager;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.ConditionVariable;
import android.os.SystemClock;

import com.froad.ukey.bean.CardInfo;
import com.froad.ukey.bean.CardResult;
import com.froad.ukey.constant.FConstant;
import com.froad.ukey.constant.InsideDataStateCode;
import com.froad.ukey.constant.ResultStateCode;
import com.froad.ukey.jni.tmjni;
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.simchannel.imp.BipHelper;
import com.froad.ukey.simchannel.imp.SMSCenOppoHelper;
import com.froad.ukey.simchannel.imp.SMSHelper;
import com.froad.ukey.simchannel.imp.UICCHelper;
import com.froad.ukey.utils.SharedPrefs;
import com.froad.ukey.utils.np.CardConnState;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.LogToFile;
import com.froad.ukey.utils.np.TMKeyLog;
import com.micronet.api.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import bip.Bip;
import bip.BipManager;
import bip.BipResult;
import bip.Constants;

/**
 * SIMBaseManager
 *
 * 卡片通信基类
 *
 * @author  by FW.
 * @date    16/12/26
 * @modify  修改者 FW
 */

public abstract class SIMBaseManager {

    private static final String TAG = FConstant.LOG_TAG + "SIMBaseManager";
    protected SIMHelper simHelper;
    protected SIMHelper sesHelper;//OMA模式
    protected SIMHelper sesDefaultHelper;//OMA模式，9.0以下系统
    protected SIMHelper sesSystemHelper;//OMA模式,9.0及以上系统
    protected SIMHelper smsHelper;//ADN模式
    protected UICCHelper uiccHelper;//UICC模式
    protected BipHelper bipHelper;//bip模式
    protected boolean isADN = true;//是否是通讯录模式
    public static   boolean isUseBip = false;//是否是bip模式
    protected static boolean isSMSCen = true;//是否是短信中心模式
    protected static boolean isSMS = true;//是否是通讯录发送短信模式
    protected boolean sesDefaultHelperConState = false;//OMA默认模式连接状态
    protected boolean sesHelperConState = false;//OMA模式连接状态
    public static final int MAXCARDVERSION = 3;//当前最大能识别的卡片版本
    public static final int CARDVERSION_02 = 2;//卡片02版本，修改通信协议，需要做特殊处理
    public static final int CARDVERSION_03 = 3;//卡片03版本，修改通信协议，卡片密码由SM3模式修改为支持SM2加密方式
    protected boolean hasCard = false;//记录是否有卡
    protected int initChannelState = CardConnState.CARDCONN_FAIL;//记录初始化时通道状态
    protected int cardConnState = CardConnState.CARDCONN_FAIL;//卡片连接状态
    private StringBuilder revSb;//用于缓存接收的数据
    private String stateCode = "1000";//指令交互的状态码

    public static String initCardInfoStr = "";//判断卡片是否可用时读取一些基础信息
    public static String CardSmsVersion = "00";//通过读取短信获取版本号，用于区别处理,00--老版本协议；01--ADN协议优化；02--通道层协议优化
    private static CardInfo cardInfo = null;//保存卡片一些初始值

    protected static String cos_atr = "";

    //指令数据头
    private final char CLA = 0x00;
    private final char INS_W = 0xDC;
    private final char INS_R = 0xB2;
    private final char P1 = 0x01;
    private final char P1_OMA = 0x02;
    private final char P2 = 0x04;
    //指令控制字节
    public final String TAR = "465543";//FUC
    public final String TAR_ADN = "FUC";//FUCF
//    public static String TAR_CARD = "464654";//FFT
    public static String TAR_CARD = "4654";//FT
    public static String TAR_CARD_START = "46";//F
    private String[] ADNSSCStrs = new String[]{"0","1","2","3","4","5","6","7","8","9",
            "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
            "a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
    private int ADNSSCIndex = 0;//ADN通信指令SSC索引

    /**
     * 写短信模式由于采用OMA的指令组装模式，但是需要与ADN指令进行切换，所以获取的随机数只能有一个字节，
     * 这里用一个固定的字节0x43进行补充后发送到卡片.
     */
    protected final String RANDOM_SUPPLY = "43";
    protected static int SSC = 0x99;//会话ID,OMA模式不需要处理高两位，短信和通讯录模式均需要处理高两位
    protected String randomStr = "";//SO加密使用的随机数
    protected int packCount = 0x01;//总包数
    protected String apduId = "01";//指令ID
    protected int SSC_Card = 0x00;//卡片返回的会话ID
    protected String resRandomStr = "01";//接收随机数
    protected int resPackCount = 0x01;//接收总包数
    protected int resPackCurrent = 0x01;//接收数据当前包索引
    protected String resApduId = "01";//接收指令ID
    protected int resCode = 0x00;//响应码
    protected int LC = 0;//当前指令数据域长度
    protected int LE = 0;//当前指令需要接收的最大数据长度

    protected String revData = "";//接收的所有有效数据
    protected String revOnePackData = "";//接收一包完整的数据（最后包括状态字）
    protected CardResult cardResult = null;//接收的指令数据封装对象
    protected int OMAPackageLen = 0xFF - 0x0D;//OMA模式传输时一包数据的长度
    protected int SMSPackageLen = 0xB0 -1 - 0x0C;//SMS模式传输时一包数据的长度，经测试（最大0xB0-1）

    /**
     * 通讯录模式传输时一包数据的长度 总共17个字节 10个控制字节和7个数据字节
     */
    protected final int ADNChangeLen_01 = 4;//01版ADN转换字符数
    protected final int ADNChangeLen_02 = 3;//02版ADN转换字符数
    protected final int ADNPackageLen1_01 = 0xFF - 0x0C;//第一次分包跟OMA一样
    protected int ADNPackageLen1_02 = 0xFF - 0x0C;//第一次分包跟OMA一样
    protected final int ADNPackageLen2_01 = 2 * ADNChangeLen_01 + 0x07;//每一包ADN分包数据
    protected final int ADNPackageLen2_02 = 2 * ADNChangeLen_02 + 0x05;//每一包ADN分包数据
    protected final int ADNPackageLen1_00 = 0x07;//每一包ADN分包数据
    protected final int SMSPChangeLen_02 = 3;//02版SMSP转换字符数
    protected final int SMSCenPackageLen = 2 * SMSPChangeLen_02;//每一包短信中心号码分包数据
    protected int packageLen = 0;//传输时一包数据的长度
    protected String TRN_KEY = "";//临时TRN秘钥
    public static String E_UKEY = TAR_CARD + "008278470001010099";//最后一字节现在作为版本号
    private String sendDataStr = "";//保存发送的数据
    private ArrayList<String> sendDataList = new ArrayList<>();//保存发送的数据

    protected SharedPreferences sp;
    protected final String PhoneTagKeyStr = "PHONETAGKEY";
    protected String PhoneTagStr = "tag";
    protected final String PHONEKEY_TAG = "tag";
    protected final String PHONEKEY_NAME = "name";
    protected final String DN_RSA1024 = "DN_RSA1024";
    protected final String DN_RSA2048 = "DN_RSA2048";
    protected final String DN_SM2 = "DN_SM2";
    protected boolean isSurePhoneTag = false;//是否已确认ADN写联系人的Tag标签
    protected boolean isPopStk = false;//是否已弹出STK菜单
    protected ConditionVariable conditionVariable = new ConditionVariable();//锁
    protected CardResult tmCardResult;//临时保存获取的结果
    protected IOException exception;
    private int readSMSCount = 0;//读取短信的总次数

    public static boolean isNeedUICC = true;
    public static boolean isNeedOMA = true;
    public static boolean isNeedADN = true;

    public static boolean isNeedBip = true;//进来的时候是否需要bip

    public static boolean isNeedSignHashData = true;//是否需要SDK做hash

    protected boolean isNeedVerifyMac = true;//是否需要校验MAC

    public static BipResult mybipResult=null;

    /**
     * 由于大部分手机写联系人时都是用的tag标签，使用name标签时直接报错，提示错误时采用tag标签报错的信息
     */
    private CardResult errCardResult = null;

    protected class APDU_ID { //指令ID
        public static final String readCard = "01";//读取卡信息
        public static final String wtiteTrn = "14";//下发TRN秘钥
        public static final String EIDTrn = "90";//EID指令交互
        public static final String NXYTrn = "91";//农信银项目指令交互
    }

    protected enum  OmaType { //OMA调用方式
        OMA_P, //OMA9.0系统接口
        OMA_SERVICE,//OMA service形式调用
        OMA_INTERFACE;//OMA接口形式调用
    }

    /**
     * OMA通道发送指令判断老卡或新卡以及新卡的版本号
     * 注：这里很重要，一旦通信错误就会认为是老协议卡
     * @return 0--新卡支持OMA模式， 1--老卡但是支持OMA模式（返回值6D00）， 2--不支持OMA模式(返回值为空)， 3--卡片版本号大于最大可识别版本
     */
    protected int checkOmaChannel_New () {
        //TODO 指令数据待定
        try {
            cardResult = new CardResult();
            LC = 0x50;
            String apduStr = "" + FCharUtils.int2HexStr(CLA)
                    + FCharUtils.int2HexStr(INS_R)
                    + FCharUtils.int2HexStr(P1_OMA)
                    + FCharUtils.int2HexStr(P2)
                    + FCharUtils.int2HexStr(LC);
            //发送读取数据的指令
            List<String> resStrList = sendApduOne(apduStr);
            if (resStrList == null || resStrList.size() < 1) { //发送数据失败返回null
                stateCode = ResultStateCode.STATE_FFFE;
                return 2;
            }
            String resData = resStrList.get(0);
            TMKeyLog.d(TAG, "checkOmaChannel_New>>>resData:" + resData);
            if (resData.length() < 26) {
                stateCode = ResultStateCode.STATE_FFFD;
                return 1;
            }
            String state = resData.substring(resData.length() - 4);
            if (state.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) { //读取数据成功
                initCardInfoStr = resData;
                if (resData.contains(E_UKEY)) {
                    int eL = E_UKEY.length();
                    int eIndex = resData.indexOf(E_UKEY);
                    CardSmsVersion = resData.substring(eIndex + eL, eIndex + eL + 2);
                    if (Integer.parseInt(CardSmsVersion, 16) > MAXCARDVERSION) { //大于客户端可识别的最大版本号
                        TMKeyLog.d(TAG, "Error---------> CardSmsVersion > MAXCARDVERSION");
                        return 3;
                    }
                    parseCardInfo ();
                    return 0;
                } else {
                    stateCode = ResultStateCode.STATE_FFFD;
                    return 1;
                }
            } else {
                stateCode = ResultStateCode.STATE_FFFD;
                return 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stateCode = ResultStateCode.STATE_FAIL_1000;
        return 2;
    }

    /**
     * UICC通道通信
     * @return 0--新卡支持UICC模式， 1--老卡但是支持UICC模式（返回值6D00）， 2--不支持UICC模式(返回值为空)， 3--卡片版本号大于最大可识别版本
     */
    protected int checkUICCChannel_New () {
        //TODO 指令数据待定
        try {
            cardResult = new CardResult();
            LC = 0x50;
            String apduStr = "" + FCharUtils.int2HexStr(CLA)
                    + FCharUtils.int2HexStr(INS_R)
                    + FCharUtils.int2HexStr(P1_OMA)
                    + FCharUtils.int2HexStr(P2)
                    + FCharUtils.int2HexStr(LC);
            //发送读取数据的指令
            List<String> resStrList = sendApduOne(apduStr);
            if (resStrList == null || resStrList.size() < 1) {//发送数据失败返回null
                stateCode = ResultStateCode.STATE_FFFE;
                return 2;
            }
            String resData = resStrList.get(0);
            TMKeyLog.d(TAG, "checkUICCChannel_New>>>resData:" + resData);
            if (resData.length() < 26) {
                stateCode = ResultStateCode.STATE_FFFD;
                return 1;
            }
            String state = resData.substring(resData.length() - 4);
            if (state.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS)) {//读取数据成功
                initCardInfoStr = resData;
                if (resData.contains(E_UKEY)) {
                    int eL = E_UKEY.length();
                    int eIndex = resData.indexOf(E_UKEY);
                    CardSmsVersion = resData.substring(eIndex + eL, eIndex + eL + 2);
                    if (Integer.parseInt(CardSmsVersion, 16) > MAXCARDVERSION) { //大于客户端可识别的最大版本号
                        TMKeyLog.d(TAG, "Error---------> CardSmsVersion > MAXCARDVERSION");
                        return 3;
                    }
                    parseCardInfo ();
                    return 0;
                } else {
                    stateCode = ResultStateCode.STATE_FFFD;
                    return 1;
                }
            } else {
                stateCode = ResultStateCode.STATE_FFFD;
                return 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stateCode = ResultStateCode.STATE_FAIL_1000;
        return 2;
    }

    /**
     * 封装需要发送的指令包集合
     *
     * @param aId    指令码
     * @param random 随机数
     * @param data   发送的数据
     * @return apdu指令
     */
    public ArrayList<String> getApduPackData(String aId,
                                             String random, String data) {
        TMKeyLog.i(TAG, "getApduPackData>>>data:" + data);
        apduId = aId;
        dealSSC();//增加会话ID的值
        randomStr = random;//随机数

        LC = 0;//重新初始化当前指令数据长度
        if (apduId == APDU_ID.wtiteTrn) { //写密钥的报文由后台组装
            SSC = 0x00;//与后台的分散因子保持一致
            sendDataStr = data;
        } else {

            String keyVer = "";
            if (TRN_KEY != null && !"".equals(TRN_KEY)) {
                keyVer = "01";
            } else {
                keyVer = "00";
            }
            TMKeyLog.i(TAG, "keyVer is :" + keyVer);
            String encDataStr = data;//不需要加密数据，初始值
            String macDataStr = "FFFFFFFF";//不需要计算MAC，初始值
            // 根据协议组装数据包： 秘钥版本(00) + 密文长度(04) + 加密数据(03123456)  + mac值(FFFFFFFF)
            sendDataStr = keyVer + FCharUtils.len2HexStr(encDataStr.length() / 2) + encDataStr + macDataStr;
        }
        TMKeyLog.i(TAG, "sendDataStr:" + sendDataStr);
        return getSendDataList(sendDataStr);
    }

    /**
     * 获取发送数据的List
     *
     * @param sendData
     * @return
     */
    private ArrayList<String> getSendDataList(String sendData) {
        TMKeyLog.d(TAG, "getSendDataList>>>isADN:" + isADN + ">>>isSMS:" + isSMS + ">>>isSMSCen:" + isSMSCen);
        String apduStr = "";
        String apduData = "";
        ArrayList<String> adpuList = new ArrayList<String>();//包集合
        if (isADN) { //通讯录模式包长特殊处理
            if (isSMS) {
                packageLen = SMSPackageLen;//短信模式传输时一包数据的长度
            } else {
                if (CardSmsVersion.equals("00")) {//老版本协议
                    packageLen = ADNPackageLen1_00;//通讯录模式传输时一大包数据的长度
                } else if (CardSmsVersion.equals("01")) {
                    packageLen = ADNPackageLen1_01;//通讯录模式传输时一大包数据的长度
                } else {
                    packageLen = ADNPackageLen1_01;//通讯录模式传输时一大包数据的长度
                }
            }
        } else {
            packageLen = OMAPackageLen;//OMA模式传输时一包数据的长度
        }
        if (sendData != null) {
            int onePackageLen = 2 * packageLen;//一包的长度
            int currentPackageLen = 0;//当前包长
            //计算总包数
            int totalPakageLen = sendData.length();
            TMKeyLog.i(TAG, "sl:" + totalPakageLen + ">>>onePackageLen:" + onePackageLen);
            packCount = totalPakageLen / onePackageLen;//计算总包数量，总共多少条
            if (totalPakageLen % onePackageLen != 0) {
                packCount += 1;
            }
            //总包数
            TMKeyLog.i(TAG, "packCount:" + packCount);
            //分包
            for (int i = 0; i < packCount; i++) {
                if (i == packCount - 1) {//最后一包
                    apduData = sendData.substring(onePackageLen * i);
                    currentPackageLen = apduData.length() / 2;
                } else {
                    apduData = sendData.substring(onePackageLen * i, onePackageLen * (i + 1));
                    currentPackageLen = packageLen;
                }

                //当前指令数据长度
                LC = currentPackageLen + 11;
                //按协议文档拼接标准的APDU指令格式
                if (isADN) { //通讯录模式指令格式特殊处理
                    if (isSMSCen) { //短信中心号码方式
                        if (CardSmsVersion.equals("00")) { //老版本协议
                            apduStr = TAR
                                    + FCharUtils.int2HexStr(SSC)
                                    + randomStr
                                    + packPackCount(packCount, (i + 1))//总条数+高位+当前条数
                                    + FCharUtils.int2BCDStr(currentPackageLen * 2)//当前包长
                                    + apduId
                                    + apduData;
                        } else { //新协议
                            apduStr = TAR
                                    + FCharUtils.int2HexStr(SSC)
                                    + randomStr
                                    + packPackCount(packCount, (i + 1))//总条数+高位+当前条数
                                    + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                                    + apduId
                                    + apduData;
                        }
                        String SMSCenApduStr = "";
                        int SMSCenOnePackageLen = SMSCenPackageLen * 2;
                        while (apduStr.length() % SMSCenOnePackageLen != 0) {//后补FF，补足6字节整数倍
                            apduStr += "FF";
                        }
                        int SMSCenTotalDataLen = apduStr.length();
                        int SMSCenPackCount = SMSCenTotalDataLen / SMSCenOnePackageLen;//计算总包数量，总共多少条
                        //SMSP分包总包数
                        TMKeyLog.i(TAG, "SMSCen>>>SMSCenPackCount:" + SMSCenPackCount);
                        String orderPartStr = "";
                        for (int j = 0; j < SMSCenPackCount; j++) {
                            TMKeyLog.i(TAG, "SMSCen>>>j:" + j);
                            if (j == SMSCenPackCount - 1) {//最后一包
                                SMSCenApduStr = apduStr.substring(SMSCenOnePackageLen * j);
                            } else {
                                SMSCenApduStr = apduStr.substring(SMSCenOnePackageLen * j, SMSCenOnePackageLen * (j + 1));
                            }
                            TMKeyLog.i(TAG, "SMSCenApduStr:" + SMSCenApduStr);
                            orderPartStr = dealADNApdu(SMSCenApduStr, SMSPChangeLen_02);
                            TMKeyLog.i(TAG, "orderPartStr:" + orderPartStr);
                            adpuList.add(orderPartStr);
                        }
                        continue;
                    } else if (isSMS) { //发送短信
                        String randomSupply = "";
                        if (CardSmsVersion.equals("00")) {
                            randomSupply = RANDOM_SUPPLY;//随机数补充位0x43
                        } else if (CardSmsVersion.equals("01")) {
                            randomSupply = "";//随机数补充位
                        }
                        apduStr = FCharUtils.int2HexStr(LC)
                                + TAR
                                //+ FCharUtils.int2BCDStr(SSC)//会话密钥
                                + FCharUtils.int2HexStr(SSC)//会话密钥
                                + randomStr//随机数
                                + randomSupply
                                + packPackCount(packCount, (i + 1))//总条数+高位+当前条数
                                + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                                + apduId
                                + apduData;
                    } else { //写通讯录
                        if (CardSmsVersion.equals("00")) { //老版本协议
                            apduStr = TAR
                                    + FCharUtils.int2HexStr(SSC)
                                    + randomStr
                                    + packPackCount(packCount, (i + 1))//总条数+高位+当前条数
                                    + FCharUtils.int2BCDStr(currentPackageLen * 2)//当前包长
                                    + apduId
                                    + apduData;
                        } else if (CardSmsVersion.equals("01")) { //新协议
                            apduStr = TAR
                                    + FCharUtils.int2HexStr(SSC)
                                    + randomStr
                                    + packPackCount(packCount, (i + 1))//总条数+高位+当前条数
                                    + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                                    + apduId
                                    + apduData;
                            //ADN数据分包处理
                            String ADNApduStr = "";
                            int ADNOnePackageLen = ADNPackageLen2_01 * 2;
                            int ADNTotalPakageLen = apduStr.length();
                            TMKeyLog.i(TAG, "ADN>>>ADNTotalPakageLen:" + ADNTotalPakageLen + ">>>ADNOnePackageLen:" + ADNOnePackageLen);
                            while (apduStr.length() % ADNOnePackageLen != 0) {//后补FF，补足15字节整数倍
                                apduStr += "FF";
                                ADNTotalPakageLen = apduStr.length();
                            }
                            int ADNPackCount = ADNTotalPakageLen / ADNOnePackageLen;//计算总包数量，总共多少条
                            //ADN分包总包数
                            TMKeyLog.i(TAG, "ADN>>>ADNPackCount:" + ADNPackCount);
                            String orderPartStr = "";
                            for (int j = 0; j < ADNPackCount; j++) {
                                TMKeyLog.i(TAG, "ADN>>>j:" + j);
                                if (j == ADNPackCount - 1) {//最后一包
                                    ADNApduStr = apduStr.substring(ADNOnePackageLen * j);
                                } else {
                                    ADNApduStr = apduStr.substring(ADNOnePackageLen * j, ADNOnePackageLen * (j + 1));
                                }
                                TMKeyLog.i(TAG, "ADNApduStr:" + ADNApduStr);
                                orderPartStr = dealADNApdu(ADNApduStr, ADNChangeLen_01);
                                TMKeyLog.i(TAG, "orderPartStr:" + orderPartStr);
                                adpuList.add(orderPartStr);
                            }
                            continue;
                        } else {
                            apduStr = TAR
                                    + FCharUtils.int2HexStr(SSC)
                                    + randomStr
                                    + packPackCount(packCount, (i + 1))//总条数+高位+当前条数
                                    + FCharUtils.int2BCDStr(currentPackageLen * 2)//当前包长
                                    + apduId
                                    + apduData;
                        }
                    }
                } else { //OMA模式
                    apduStr = FCharUtils.int2HexStr(CLA)
                            + FCharUtils.int2HexStr(INS_W)
                            + FCharUtils.int2HexStr(P1)
                            + FCharUtils.int2HexStr(P2)
                            + FCharUtils.int2HexStr(LC)
                            + TAR
                            //+ FCharUtils.int2BCDStr(SSC)//会话密钥
                            + FCharUtils.int2HexStr(SSC)//会话密钥
                            + randomStr//随机数
                            + packPackCount(packCount, (i + 1))//总条数+高位+当前条数
                            + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                            + apduId
                            + apduData;
                }
                adpuList.add(apduStr);
            }
        }
        return adpuList;
    }

    /**
     * 封装需要发送的指令包集合
     *
     * @param sendData
     * @return
     */
    private ArrayList<String> getSendDataList_New(String sendData) {
        TMKeyLog.d(TAG, "getSendDataList_New>>>isADN:" + isADN + ">>>isSMS:" + isSMS + ">>>isSMSCen:" + isSMSCen);
        String apduStr = "";
        String apduData = "";
        ArrayList<String> adpuList = new ArrayList<String>();//包集合
        if (isADN) { //通讯录模式包长特殊处理
            if (isSMS) {
                packageLen = SMSPackageLen;//短信模式传输时一包数据的长度
            } else { //短信中心号码模式和ADN模式包长相同
                packageLen = ADNPackageLen1_02;//通讯录模式传输时一大包数据的长度
            }
        } else {
            packageLen = OMAPackageLen;//OMA模式传输时一包数据的长度
        }
        if (sendData != null) {
            int onePackageLen = 2 * packageLen;//一包的长度
            int currentPackageLen = 0;//当前包长
            //计算总包数
            int totalPakageLen = sendData.length();
            TMKeyLog.i(TAG, "sl:" + totalPakageLen + ">>>onePackageLen:" + onePackageLen);
            packCount = totalPakageLen / onePackageLen;//计算总包数量，总共多少条
            if (totalPakageLen % onePackageLen != 0) {
                packCount += 1;
            }
            //总包数
            TMKeyLog.i(TAG, "packCount:" + packCount);
            //分包
            for (int i = 0; i < packCount; i++) {
                dealSSC_New();
                if (i == packCount - 1) { //最后一包
                    apduData = sendData.substring(onePackageLen * i);
                    currentPackageLen = apduData.length() / 2;
                    if (i == 0) {//首包也是尾包
                        SSC = SSC | 0x00;
                    } else {
                        SSC = SSC | 0x20;
                    }
                } else {
                    apduData = sendData.substring(onePackageLen * i, onePackageLen * (i + 1));
                    currentPackageLen = packageLen;
                    if (i == 0) {//首包
                        SSC = SSC | 0x10;
                    } else {
                        SSC = SSC | 0x30;
                    }
                }

                //当前指令数据长度
                LC = currentPackageLen + 5;
                //按协议文档拼接标准的APDU指令格式
                if (isADN) { //通讯录模式指令格式特殊处理
                    if (isSMSCen) { //短信中心号码
                        apduStr = TAR
                                + FCharUtils.int2HexStr(SSC)//指令控制字节
                                + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                                + apduData;
                        String SMSCenApduStr = "";
                        int SMSCenOnePackageLen = SMSCenPackageLen * 2;
                        while (apduStr.length() % SMSCenOnePackageLen != 0) {//后补FF，补足6字节整数倍
                            apduStr += "FF";
                        }
                        int SMSCenTotalDataLen = apduStr.length();
                        int SMSCenPackCount = SMSCenTotalDataLen / SMSCenOnePackageLen;//计算总包数量，总共多少条
                        //SMSP分包总包数
                        TMKeyLog.i(TAG, "SMSCen>>>SMSCenPackCount:" + SMSCenPackCount);
                        String orderPartStr = "";
                        for (int j = 0; j < SMSCenPackCount; j++) {
                            TMKeyLog.i(TAG, "SMSCen>>>j:" + j);
                            if (j == SMSCenPackCount - 1) {//最后一包
                                SMSCenApduStr = apduStr.substring(SMSCenOnePackageLen * j);
                            } else {
                                SMSCenApduStr = apduStr.substring(SMSCenOnePackageLen * j, SMSCenOnePackageLen * (j + 1));
                            }
                            TMKeyLog.i(TAG, "SMSCenApduStr:" + SMSCenApduStr);
                            orderPartStr = dealADNApdu(SMSCenApduStr, SMSPChangeLen_02);
                            TMKeyLog.i(TAG, "orderPartStr:" + orderPartStr);
                            adpuList.add(orderPartStr);
                        }
                        continue;
                    } else if (isSMS) { //发送短信
                        apduStr = FCharUtils.int2HexStr(LC)
                                + TAR
                                + FCharUtils.int2HexStr(SSC)//指令控制字节
                                + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                                + apduData;
                    } else { //写通讯录
                        apduStr = TAR
                                + FCharUtils.int2HexStr(SSC)//指令控制字节
                                + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                                + apduData;
                        String ADNApduStr = "";
                        int ADNOnePackageLen = ADNPackageLen2_02 * 2;
                        int ADNTotalPakageLen = apduStr.length();
                        TMKeyLog.i(TAG, "ADN>>>ADNTotalPakageLen:" + ADNTotalPakageLen + ">>>ADNOnePackageLen:" + ADNOnePackageLen);
                        while (apduStr.length() % ADNOnePackageLen != 0) {//后补FF，补足10字节整数倍
                            apduStr += "FF";
                        }
                        ADNTotalPakageLen = apduStr.length();
                        int ADNPackCount = ADNTotalPakageLen / ADNOnePackageLen;//计算总包数量，总共多少条
                        //ADN分包总包数
                        TMKeyLog.i(TAG, "ADN>>>ADNPackCount:" + ADNPackCount);
                        String orderPartStr = "";
                        ADNSSCIndex = 0;//每一包索引从0开始
                        for (int j = 0; j < ADNPackCount; j++) {
                            TMKeyLog.i(TAG, "ADN>>>j:" + j);
                            if (j == 0) {
                                ADNSSCIndex = 0;
                            }
                            if (j == ADNPackCount - 1) {//最后一包
                                ADNApduStr = apduStr.substring(ADNOnePackageLen * j);
                            } else {
                                ADNApduStr = apduStr.substring(ADNOnePackageLen * j, ADNOnePackageLen * (j + 1));
                            }
                            ADNApduStr = ADNApduStr + TAR_ADN + ADNSSCStrs[ADNSSCIndex];//每一个字符串后面添加标识符
                            TMKeyLog.i(TAG, "ADNApduStr:" + ADNApduStr);
                            orderPartStr = dealADNApdu(ADNApduStr, ADNChangeLen_02);
                            TMKeyLog.i(TAG, "orderPartStr:" + orderPartStr);
                            adpuList.add(orderPartStr);
                            ADNSSCIndex ++;
                            if (ADNSSCIndex == ADNSSCStrs.length) {
                                ADNSSCIndex = 0;
                            }
                        }
                        continue;
                    }
                } else { //OMA模式
                    apduStr = FCharUtils.int2HexStr(CLA)
                            + FCharUtils.int2HexStr(INS_W)
                            + FCharUtils.int2HexStr(P1)
                            + FCharUtils.int2HexStr(P2)
                            + FCharUtils.int2HexStr(LC)
                            + TAR
                            + FCharUtils.int2HexStr(SSC)//会话密钥
                            + FCharUtils.int2HexStr(currentPackageLen)//当前包长
                            + apduData;
                }
                adpuList.add(apduStr);
            }
        }
        return adpuList;
    }

    /**
     * 循环发送写数据指令
     *
     * @param al 多条指令
     * @return CardResult
     * @throws IOException
     */
    protected CardResult sendWriteApdu(ArrayList<String> al, boolean isNeedSend) throws IOException {
        TMKeyLog.i(TAG, "sendWriteApdu");
        cardResult = new CardResult();
        if (simHelper == null) {
            cardResult.setResCode(FConstant.RES_FAIL_CARD_NO_CONNECTED);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_CARD_NO_CONNECTED));
            return cardResult;
        }
        if (al == null || al.isEmpty()) {
            cardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return cardResult;
        }
        sendDataList = al;//临时保存发送的数据
        int all = al.size();
        TMKeyLog.i(TAG, "list size:" + all);
        boolean sendBool = false;
        String sendApdu = "";


        //OMA模式
        TMKeyLog.i(TAG, "isADN:" + isADN);
        if (!isADN) {
            //循环发送指令
            if(!isUseBip){     //是否是bip模式
                TMKeyLog.i(TAG, "使用非bip模式");
                for (int i = 0; i < all; i++) {
                sendApdu = al.get(i);
                TMKeyLog.i(TAG, "index:" + i + ">>>sendApdu:" + sendApdu);
                LogToFile.d(TAG, "sendCMD:" + sendApdu);
                sendBool = simHelper.transmitHexData(sendApdu);
                TMKeyLog.i(TAG, "sendBool:" + sendBool);
                if (!sendBool) {
                    LogToFile.d(TAG, "===OMA或UICC指令模式发送失败");
                    cardResult.setResCode(FConstant.RES_FAIL_SEND_DATA);
                    cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA));
                    return cardResult;
                }
            }}else {
                TMKeyLog.i(TAG, "使用bip模式");
                CardResult tempCardRes= sendBipApdu();
                return tempCardRes;
            }
        } else {// ADN模式
            TMKeyLog.i(TAG, "isNeedSend:" + isNeedSend);
            if (isNeedSend) {//需要发送读取数据指令时才发送指令
                TMKeyLog.i(TAG, "isSMSCen:" + isSMSCen + ">>>isSMS:" + isSMS + ">>>isADN:" + isADN);
                if (isSMSCen) {
                    //循环发送指令
                    boolean isInit = false;
                    for (int i = 0; i < all; i++) {
                        sendApdu = al.get(i);
                        TMKeyLog.i(TAG, "index:" + i + ">>>sendApdu:" + sendApdu);
                        sendBool = false;
                        if (simHelper instanceof SMSCenOppoHelper) {
                            if (i == (all - 1)) {
                                isInit = true;
                            } else {
                                isInit = false;
                            }
                            LogToFile.d(TAG, "sendCMD:" + sendApdu);
                            sendBool = ((SMSCenOppoHelper)simHelper).writeBySmsCenter(sendApdu, isInit);
                        }
                        TMKeyLog.i(TAG, "sendBool:" + sendBool);
                        if (!sendBool) { //短信模式发送失败后再以写通讯录模式发送
                            LogToFile.d(TAG, "===SMSP模式指令发送失败");
                            isSMSCen = false;
                            cardConnState = CardConnState.CARDCONN_SUCCESS_SMS;
                            if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) {//新版协议
                                sendDataList = getSendDataList_New(sendDataStr);
                            } else {
                                sendDataList = getSendDataList(sendDataStr);
                            }
                            return sendWriteApdu(sendDataList, isNeedSend);
                        } else {
                            cardConnState = CardConnState.CARDCONN_SUCCESS_SMS_CEN;
                    }
                    }
                } else if (isSMS) { //发送短信模式
                    //循环发送指令
                    for (int i = 0; i < all; i++) {
                        sendApdu = al.get(i);
                        TMKeyLog.i(TAG, "index:" + i + ">>>sendApdu:" + sendApdu);
                        LogToFile.d(TAG, "sendCMD:" + sendApdu);
                        sendBool = simHelper.transmitHexData(sendApdu);
                        TMKeyLog.i(TAG, "sendBool:" + sendBool);
                        if (!sendBool) { //短信模式发送失败后再以写通讯录模式发送
                            LogToFile.d(TAG, "===SMS模式指令发送失败");
                            isSMS = false;
                            cardConnState = CardConnState.CARDCONN_SUCCESS_ADN;
                            if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) {//新版协议
                                sendDataList = getSendDataList_New(sendDataStr);
                            } else {
                                sendDataList = getSendDataList(sendDataStr);
                        }
                            return sendWriteApdu(sendDataList, isNeedSend);
                        } else {
                            cardConnState = CardConnState.CARDCONN_SUCCESS_SMS;
                        }
                    }
                } else {
                    isSMS = false;
                    isSMSCen = false;
                    cardConnState = CardConnState.CARDCONN_SUCCESS_ADN;
                    PhoneTagStr = sp.getString(PhoneTagKeyStr, PHONEKEY_TAG);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(PhoneTagKeyStr, PhoneTagStr);
                    editor.commit();
                    ((SMSHelper) smsHelper).setPhoneTagStr(PhoneTagStr);
                    TMKeyLog.i(TAG, "ADN sendData PhoneTagStr:" + PhoneTagStr);

                    List<ContentValues> list = new ArrayList<ContentValues>();
                    for (int i = 0; i < all; i++) {
                        sendApdu = al.get(i);
                        TMKeyLog.i(TAG, "index:" + i + ">>>sendApdu:" + sendApdu);
                        list.add(simHelper.getContentValues(sendApdu));
                    }
                    TMKeyLog.i(TAG, "ContentValues list size:" + list.size());
                    if (list.size() > 0) {
                        try {
                            LogToFile.d(TAG, "sendCMD:" + sendApdu);
                            sendBool = simHelper.insetContentValues(list);
                        } catch (Exception e) {
                            TMKeyLog.i(TAG, "insetContentValues Exception:" + e.getMessage());
                            e.printStackTrace();
                            sendBool = false;
                        }
                        TMKeyLog.i(TAG, "insetContentValues sendBool:" + sendBool);
                        if (!sendBool) { //发送失败
                            LogToFile.d(TAG, "===ADN模式指令发送失败");
                            if (isSurePhoneTag) { //已经确认写联系人的标签
                                cardConnState = CardConnState.CARDCONN_FAIL;
                                if (errCardResult == null) {
                                    errCardResult = new CardResult();
                                    errCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA);
                                    errCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA) + "\n" + FConstant.RES_FAIL_CHECK_PERMISSION_STR);
                                }
                                cardResult = errCardResult;
                                return cardResult;
                            }
                            PhoneTagStr = sp.getString(PhoneTagKeyStr, PHONEKEY_TAG);
                            TMKeyLog.i(TAG, "ADN insetContentValues fail PhoneTagStr:" + PhoneTagStr);
                            if (PhoneTagStr.equals(PHONEKEY_TAG)) { //以tag为key插入联系人
                                //发送数据失败
                                errCardResult = new CardResult();
                                errCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA);
                                errCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA) + "\n" + FConstant.RES_FAIL_CHECK_PERMISSION_STR);

                                editor = sp.edit();
                                editor.putString(PhoneTagKeyStr, PHONEKEY_NAME);
                                editor.commit();
                                ((SMSHelper) smsHelper).setPhoneTagStr(PHONEKEY_NAME);
                                return sendWriteApdu(sendDataList, isNeedSend);
                            } else { //以name为key插入联系人
                                if (errCardResult == null) {
                                    errCardResult = new CardResult();
                                    errCardResult.setResCode(FConstant.RES_FAIL_SEND_DATA);
                                    errCardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA) + "\n" + FConstant.RES_FAIL_CHECK_PERMISSION_STR);
                                }
                                TMKeyLog.i(TAG, "ADN insetContentValues fail PhoneTagStr 'name' errCardResult：" + errCardResult.getResCode() + ">>>errCardResult.getErrMsg():" + errCardResult.getErrMsg());
                                editor = sp.edit();
                                editor.putString(PhoneTagKeyStr, PHONEKEY_TAG);
                                editor.commit();
                                ((SMSHelper) smsHelper).setPhoneTagStr(PHONEKEY_TAG);
                                isSMS = true;//所有通信模式都发送数据失败，下次连接重新以短信模式开始尝试
                                cardConnState = CardConnState.CARDCONN_FAIL;
                                cardResult = errCardResult;
                                return cardResult;
                            }
                        }
                    }
                }
            }
        }

        //数据发送完成，将总包数和当前包数重置
        resPackCount = 1;
        resPackCurrent = 1;
        //发送完成处理
        revData = "";//将接收数据缓存清空
        CardResult tempCardRes= sendReadApdu();

        if (isADN) {
            TMKeyLog.i(TAG, "isSMS:" + isSMS + ">>>isSMSCen:" + isSMSCen + ">>>isADN:" + isADN +  ">>>isSurePhoneTag:" + isSurePhoneTag + ">>>resCode:" + tempCardRes.getResCode() + ">>>isCounterNotZero:" + ((SMSHelper)smsHelper).isCounterNotZero);
            if (((SMSHelper)smsHelper).isCounterNotZero && isSurePhoneTag) {
                return tempCardRes;
            } else if (isSMSCen) {
                if (tempCardRes.getResCode() != FConstant.RES_SUCCESS) { //读取数据失败
                    isSMSCen = false;
                    cardConnState = CardConnState.CARDCONN_SUCCESS_SMS;
                    if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) {//新版协议
                        sendDataList = getSendDataList_New(sendDataStr);
                    } else {
                        sendDataList = getSendDataList(sendDataStr);
                    }
                    return sendWriteApdu(sendDataList, isNeedSend);
                }
            } else if (isSMS) {
                if (tempCardRes.getResCode() != FConstant.RES_SUCCESS) { //读取数据失败
                    isSMS = false;
                    cardConnState = CardConnState.CARDCONN_SUCCESS_ADN;
                    if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) {//新版协议
                        sendDataList = getSendDataList_New(sendDataStr);
                    } else {
                        sendDataList = getSendDataList(sendDataStr);
                    }
                    return sendWriteApdu(sendDataList, isNeedSend);
                }
            } else {
                TMKeyLog.i(TAG, "ADN sendData resCode:" + tempCardRes.getResCode());
                if (tempCardRes.getResCode() != FConstant.RES_SUCCESS) { //读取数据失败
                    PhoneTagStr = sp.getString(PhoneTagKeyStr, PHONEKEY_TAG);
                    TMKeyLog.i(TAG, "ADN sendData fail PhoneTagStr:" + PhoneTagStr);
                    if (PhoneTagStr.equals(PHONEKEY_TAG)) { //以tag为key插入联系人
                        String errMsg = tempCardRes.getErrMsg();
                        errMsg += ("\n" + FConstant.RES_FAIL_CHECK_PERMISSION_STR);
                        errCardResult = tempCardRes;//保存tag标签数据交互失败
                        errCardResult.setErrMsg(errMsg);
                        TMKeyLog.d(TAG, "PhoneTagStr.equals(PHONEKEY_TAG) >>>SMSHelper.adnWriteType:" + SMSHelper.adnWriteType);
                        if (SMSHelper.adnWriteType == 1) {
                            SMSHelper.adnWriteType = 2;
                            return sendWriteApdu(sendDataList, isNeedSend);
                        }
                        SMSHelper.adnWriteType = 1;
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(PhoneTagKeyStr, PHONEKEY_NAME);
                        editor.commit();
                        ((SMSHelper) smsHelper).setPhoneTagStr(PHONEKEY_NAME);
                        return sendWriteApdu(sendDataList, isNeedSend);
                    } else { //以name为key插入联系人
                        TMKeyLog.d(TAG, "PhoneTagStr.equals(PHONEKEY_NAME) >>>SMSHelper.adnWriteType:" + SMSHelper.adnWriteType);
                        if (SMSHelper.adnWriteType == 1) {
                            SMSHelper.adnWriteType = 2;
                            return sendWriteApdu(sendDataList, isNeedSend);
                        }
                        if (errCardResult == null) {
                            errCardResult = new CardResult();
                        }
                        TMKeyLog.i(TAG, "ADN sendData fail PhoneTagStr 'name' errCardResult：" + errCardResult.getResCode() + ">>>errCardResult.getErrMsg():" + errCardResult.getErrMsg());
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(PhoneTagKeyStr, PHONEKEY_TAG);
                        editor.commit();
                        ((SMSHelper) smsHelper).setPhoneTagStr(PHONEKEY_TAG);
                        isSMS = true;//ADN模式连接失败，下次连接重新以短信模式开始尝试
                        cardConnState = CardConnState.CARDCONN_FAIL;
                        cardResult = errCardResult;
                        return errCardResult;
                    }
                } else {
                    PhoneTagStr = sp.getString(PhoneTagKeyStr, PHONEKEY_TAG);
                    TMKeyLog.d(TAG, "trans success >>>PhoneTagStr:" + PhoneTagStr + ">>>adnWriteType:" + SMSHelper.adnWriteType);
                    isSurePhoneTag = true;
                }
            }
        }
        return tempCardRes;
    }

    /**
     * 发送接收数据的APDU指令
     *
     * @return CardResult
     * @throws IOException
     */
    protected CardResult sendReadApdu() throws IOException {
        TMKeyLog.i(TAG, "sendReadApdu");
        readSMSCount = 0;
        sendReadApduTemp();
        if (cardResult.getResCode() != FConstant.RES_SUCCESS) {//卡片返回指令错误，结束处理
            return cardResult;
        }
        TMKeyLog.i(TAG, "接收到的数据信息>>>revData:" + revData);
        if (revData == null || revData.length() == 0) {
            TMKeyLog.i(TAG, "sendReadApdu指令处理失败");
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return cardResult;
        }

        if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) { //新版协议
            if (revData == null || revData.length() < 4) {//返回数据有误
                cardResult = new CardResult();
                cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                return cardResult;
            }
            if (resCode == FConstant.RES_SUCCESS) { //获取数据成功
                cardResult.setApduDecData(revData);
            } else {
                stateCode = revData.substring(revData.length() - 4);//保存当前指令交互的状态码
            }
        } else { //老版本协议 00、01
            ArrayList<String> al = FCharUtils.parseDataLV(revData, true);//解析出数据域和MAC
            if (al == null) { //返回应用指令格式错误
                setErrorStateCode(ResultStateCode.STATE_FFFD);
                TMKeyLog.i(TAG, "sendReadApdu应用指令格式错误");
                cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
                return cardResult;
            }
            String encData = "";
            if (al.size() > 1) {
                encData = al.get(0);//获取的加密数据
            }
            if (encData == null || encData.length() < 4) {
                stateCode = ResultStateCode.STATE_FFFE;//保存当前指令交互的状态码
                cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);//接收数据错误
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
                return cardResult;
            }
            if (resCode == FConstant.RES_SUCCESS) { //获取指令成功
                cardResult.setApduDecData(encData);//返回数据
            } else {
                stateCode = revData.substring(revData.length() - 4);//保存当前指令交互的状态码
            }
        }
        cardResult.setResCode(resCode);
        cardResult.setErrMsg(FConstant.getCardErrorMsg(resCode));
        return cardResult;
    }


    public static  String bipErrorStrig="";


    /**
     * 发送接收数据的APDU指令
     *
     * @return CardResult
     * @throws IOException
     */
    protected CardResult sendBipApdu() throws IOException {
        TMKeyLog.i(TAG, "sendBipApdu进来了");
        ArrayList<String> resStr = new ArrayList<String>();
        long time=System.currentTimeMillis();
        if(BipManager.getInstance().arrayList!=null){
            BipManager.getInstance().arrayList.clear();//进去前先清理下数据
        }
        ArrayList<BipResult> carNo= BipManager.getInstance().onStartBip(BipManager.Instruct);
      if(carNo!=null&&carNo.size()>0){
        BipResult bipResult=carNo.get(0);
        resStr=bipResult.arrayList;
        if(bipResult!=null &&!bipResult.getSuccess()){
             mybipResult=bipResult;
            bipErrorStrig=bipErrorStrig+bipResult.getMessage();
            VCardApi_FFT.mBipEntity.setYangzhengma("");
         }else{
            VCardApi_FFT.mBipEntity.setYangzhengma("");
        }

      }else {
          BipManager.getInstance().onDestroy();//如果出错，清除缓存数据
      }
        time=System.currentTimeMillis()-time;
        TMKeyLog.d(TAG, "获取bip数据耗时:" + time+"毫秒");
        TMKeyLog.d(TAG, "测试bip通道返回的数据:" + revData);

        if(!revData.equals("")){

            resStr.add(revData);}

        if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) {
        //    parseReceivBip_Data_New(resStr);
            parseReceiveBip_Data_New(resStr);
        }

        if(!revData.equals("")){
           resCode=0;
         }


        if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) { //新版协议
            if (revData == null || revData.length() < 4) {//返回数据有误
                cardResult = new CardResult();
                cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                return cardResult;
            }
            if (resCode == FConstant.RES_SUCCESS) { //获取数据成功
                cardResult.setApduDecData(revData);
            } else {
                stateCode = revData.substring(revData.length() - 4);//保存当前指令交互的状态码
            }
        } else { //老版本协议 00、01
            ArrayList<String> al = FCharUtils.parseDataLV(revData, true);//解析出数据域和MAC
            if (al == null) { //返回应用指令格式错误
                setErrorStateCode(ResultStateCode.STATE_FFFD);

                TMKeyLog.i(TAG, "sendReadApdu应用指令格式错误");
                cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
                return cardResult;
            }
            String encData = "";
            if (al.size() > 1) {
                encData = al.get(0);//获取的加密数据


            }
            if (encData == null || encData.length() < 4) {
                stateCode = ResultStateCode.STATE_FFFE;//保存当前指令交互的状态码
                cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);//接收数据错误
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
                return cardResult;
            }
            if (resCode == FConstant.RES_SUCCESS) { //获取指令成功
                cardResult.setApduDecData(encData);//返回数据
            } else {
                stateCode = revData.substring(revData.length() - 4);//保存当前指令交互的状态码
            }
        }
        revData="";
        cardResult.setResCode(resCode);
        cardResult.setErrMsg(FConstant.getCardErrorMsg(resCode));
        return cardResult;
    }




    /**
     * 循环发送接收数据的APDU指令
     *
     * @throws IOException
     */
    protected void sendReadApduTemp() throws IOException {
        TMKeyLog.i(TAG, "sendReadApduTemp");
        //通用APDU指令数据组装
        String apduStr = "";
        int conState = getCardConState();

        if (conState == CardConnState.CARDCONN_SUCCESS_OMA
                || conState == CardConnState.CARDCONN_SUCCESS_UICC
                ) {
            LC = 0xFF;
            apduStr = "" + FCharUtils.int2HexStr(CLA)
                    + FCharUtils.int2HexStr(INS_R)
                    + FCharUtils.int2HexStr(P1)
                    + FCharUtils.int2HexStr(P2)
                    + FCharUtils.int2HexStr(LC);
        }

        TMKeyLog.i(TAG, "sendReadApdu:" + apduStr);
        //发送读取数据的指令
        LogToFile.d(TAG, "send&ReceiveCMD:" + apduStr);
        List<String> resStr = sendApduOne(apduStr);
        if (resStr == null) { //发送数据失败返回null
            cardResult.setResCode(FConstant.RES_FAIL_SEND_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA));
            return;
        }
        if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) {
            parseReceiveData_New(resStr);
        } else {
            parseReceiveData(resStr);
        }
    }

    /**
     * 发送一条获取数据的APDU指令
     *
     * @param as
     * @return
     * @throws IOException
     */
    public List<String> sendApduOne(String as) throws IOException {
        TMKeyLog.i(TAG, "sendApduOne:" + as);
        if (as != null && !"".equals(as)) { //如果有数据就发送指令
            boolean sendBool = simHelper.transmitHexData(as);
            TMKeyLog.i(TAG, "sendBool:" + sendBool);
            if (!sendBool) {
                stateCode = ResultStateCode.STATE_FFFF;
                return null;
            }
        }

        //通讯录模式读取数据时不需要发送指令，直接读取短信内容
        //获取卡端返回数据,
        List<String> resStr = simHelper.receiveDataList();
        return resStr;
    }

    /**
     * 解析接收的数据
     *
     * @param list
     * @throws IOException
     */
    public void parseReceiveData(List<String> list) throws IOException {
        TMKeyLog.d(TAG, "parseReceiveData");
        if (list == null || list.size() == 0) {
            resCode = -1;
            revData = "";
            resPackCount = 1;
            resPackCurrent = 1;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return;
        }
        boolean parseBool = false;
        for (String d : list) {
            parseBool = parseReceiveData(d);
            if (! parseBool) {
                break;
            }
        }

        TMKeyLog.d(TAG, "resPackCount:" + resPackCount + ">>>resPackCurrent:" + resPackCurrent);
        if (resPackCount > resPackCurrent) {//递归调用函数获取数据
            sendReadApduTemp();
        }

        if (resCode == FConstant.RES_FAIL_STK_RUNNING) {//如果STK菜单弹出，则等待200ms后重新读取数据
            TMKeyLog.d(TAG, "resCode is " + FConstant.RES_FAIL_STK_RUNNING + ", STK Running");
            isPopStk = true;
            SystemClock.sleep(200);
            sendReadApduTemp();
        }
    }

    /**
     * 解析接收的数据
     *
     * @param list
     * @throws IOException
     */
    public void parseReceiveData_New(List<String> list) throws IOException {
        TMKeyLog.d(TAG, "parseReceiveData_New");
        if (list == null || list.size() == 0) {
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return;
        }
        boolean parseBool = false;
        for (String d : list) {
            LogToFile.d(TAG, "receiveData:" + d);
            parseBool = parseReceiveData_New(d);
            if (! parseBool) {
                LogToFile.d(TAG, "===接收数据解析失败");
                break;
            }
        }
        //高四位中的低两位表示：00--只有一包；01--首包、11--中间包、10--尾包
        if ((SSC_Card & 0x10) != 0) { //递归调用函数获取数据
            readSMSCount ++ ;
            if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02
                    && (cardConnState == CardConnState.CARDCONN_SUCCESS_ADN || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS_CEN)
                    && readSMSCount > 3
                    ) {
                return;
            }
            sendReadApduTemp();
        }

        if (resCode == FConstant.RES_FAIL_STK_RUNNING) {//如果STK菜单弹出，则等待200ms后重新读取数据
            TMKeyLog.d(TAG, "resCode is " + FConstant.RES_FAIL_STK_RUNNING + ", STK Running");
            isPopStk = true;
            SystemClock.sleep(200);
            sendReadApduTemp();
        }
    }

    /**
     * 解析接收的数据
     *
     * @param list
     * @throws IOException
     */
    public void parseReceiveBip_Data_New(List<String> list) throws IOException {
        TMKeyLog.d(TAG, "parseReceiveBip_Data_New");
        if (list == null || list.size() == 0) {
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return;
        }
        boolean parseBool = false;
        for (int i=0;i<list.size();i++)
        {   LogToFile.d(TAG, "receiveData:" + list.get(i));
        if(i!=list.size()-1){
            parseBool = parseReceivBipData_New(list.get(i),false);}else {
            parseBool = parseReceivBipData_New(list.get(i),true);
        }
            if (! parseBool) {
                LogToFile.d(TAG, "===接收数据解析失败");
                break;
            }
        }
        //高四位中的低两位表示：00--只有一包；01--首包、11--中间包、10--尾包
        if ((SSC_Card & 0x10) != 0) { //递归调用函数获取数据
            readSMSCount ++ ;
            if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02
                    && (cardConnState == CardConnState.CARDCONN_SUCCESS_ADN || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS_CEN)
                    && readSMSCount > 3
            ) {
                return;
            }
            sendReadApduTemp();
        }

        if (resCode == FConstant.RES_FAIL_STK_RUNNING) {//如果STK菜单弹出，则等待200ms后重新读取数据
            TMKeyLog.d(TAG, "resCode is " + FConstant.RES_FAIL_STK_RUNNING + ", STK Running");
            isPopStk = true;
            SystemClock.sleep(200);
            sendReadApduTemp();
        }
    }





    /**
     * 解析接收的数据
     *
     * @param d
     * @throws IOException
     */
    private boolean parseReceiveData(String d) throws IOException {
        TMKeyLog.i(TAG, "parseReceiveData d= " + d);
        int startTagLen = TAR_CARD.length();
        if (d == null || d.length() < (18 + startTagLen)) {
            resCode = -1;
            revData = "";
            resPackCount = 1;
            resPackCurrent = 1;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return false;
        }
        if (! d.contains(TAR_CARD)) {
            TMKeyLog.i(TAG, "d not contains(TAR_CARD)>>>" + ", TAR_CARD is " + TAR_CARD);
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return false;
        }
        int indexStart = d.indexOf(TAR_CARD);
        String sessionId = d.substring(indexStart + startTagLen, indexStart + startTagLen + 2);
        TMKeyLog.i(TAG, "session id= " + sessionId);
        SSC_Card = Integer.parseInt(sessionId, 16);//保存会话ID，用于so解密和校验MAC
        resRandomStr = d.substring(indexStart + startTagLen + 2, indexStart + startTagLen + 6);//保存随机数，用于so解密和校验MAC

        TMKeyLog.i(TAG, "SSC_Card:" + SSC_Card);
        TMKeyLog.i(TAG, "resRandomStr:" + resRandomStr);

        String totalCount = d.substring(indexStart + startTagLen + 6, indexStart + startTagLen + 8);//总条数
        String tPs = d.substring(indexStart + startTagLen + 8, indexStart + startTagLen + 10);//包的补位字节,高位标识
        String tPackCurrent = d.substring(indexStart + startTagLen + 10, indexStart + startTagLen + 12);//当前条数
        String tLe = d.substring(indexStart + startTagLen + 12, indexStart + startTagLen + 14);
        int dataLenInt = FCharUtils.hexStr2Len(tLe);//应用数据一包长度
        LE = dataLenInt;
        TMKeyLog.i(TAG, "tLeInt:" + LE);

        if (LE * 2 > (d.length() - indexStart - startTagLen - 18)) {//
            TMKeyLog.i(TAG, "LE error...");
            resCode = -1;
            revData = "";
            resPackCount = 1;
            resPackCurrent = 1;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
            return false;
        }

        //获取指令码
        String tAId = d.substring(indexStart + startTagLen + 14, indexStart + startTagLen + 16);//指令ID
        //判断指令码是否正确
        int tAIdInt = Integer.parseInt(tAId, 16);
        TMKeyLog.i(TAG, "parseCardResLv>>>tAId:" + tAId);
        cardResult.setApduId(tAIdInt);//设置指令码
        if (!apduId.equals(tAId)) {
            TMKeyLog.i(TAG, "接收数据指令码有误");
            resCode = -1;
            revData = "";
            resPackCount = 1;
            resPackCurrent = 1;
            cardResult.setResCode(FConstant.RES_FAIL_UNKNOWN_APP);//未知应用指令错误
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_UNKNOWN_APP));
            return false;
        }
        resApduId = tAId;

        String tResCode = d.substring(indexStart + startTagLen + 16, indexStart + startTagLen + 18);//响应码
        TMKeyLog.i(TAG, "响应码：" + tResCode);

        //获取响应码
        int tResCodeInt = FCharUtils.bcdStr2Int(tResCode);
        resCode = tResCodeInt;
        cardResult.setResCode(tResCodeInt);//设置响应码
        if (resCode != FConstant.RES_SUCCESS && resCode != FConstant.RES_FAIL_STK_RUNNING) {//响应码错误直接结束
            revData = "";
            resPackCount = 1;
            resPackCurrent = 1;
            cardResult.setErrMsg(FConstant.getCardErrorMsg(tResCodeInt));
            return false;
        }

        if (resCode == FConstant.RES_FAIL_STK_RUNNING) {
            return true;
        }
        String tData = d.substring(indexStart + startTagLen + 18, (indexStart + startTagLen + 18 + LE * 2));
        revData += tData;//将每次接收的部分数据拼接保存
        //判断包数和当前包索引，确认是否需要继续获取数据
        int[] pInt = parsePackCount(tPs + totalCount + tPackCurrent);
        resPackCount = pInt[0];
        resPackCurrent = pInt[1];
        TMKeyLog.i(TAG, "resPackCount:" + resPackCount + ">>>resPackCurrent:" + resPackCurrent);
        return true;
    }

    /**
     * 解析数据
     * @param d
     * @throws IOException
     */
    private boolean parseReceiveData_New(String d) throws IOException {
        TMKeyLog.i(TAG, "parseReceiveData_New d= " + d);
        resCode = -1;
        int startTagLen = TAR_CARD.length();
        if (d == null || d.length() < (startTagLen + 4)) {
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return false;
        }
        if (! d.contains(TAR_CARD)) {
            TMKeyLog.i(TAG, "d not contains(TAR_CARD)>>>" + ", TAR_CARD is " + TAR_CARD);
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return false;
        }
        int indexStart = d.indexOf(TAR_CARD);
        String sessionId = d.substring(indexStart + startTagLen, indexStart + startTagLen + 2);
        TMKeyLog.i(TAG, "session id= " + sessionId);
        SSC_Card = Integer.parseInt(sessionId, 16);

        TMKeyLog.i(TAG, "SSC_Card:" + SSC_Card);

        String tLe = d.substring(indexStart + startTagLen + 2, indexStart + startTagLen + 4);
        int dataLenInt = FCharUtils.hexStr2Len(tLe);//应用数据一包长度
        LE = dataLenInt;
        TMKeyLog.i(TAG, "tLeInt:" + LE);

        if (LE * 2 > (d.length() - indexStart - startTagLen - 4)) {//数据长度有误
            TMKeyLog.i(TAG, "LE error...");
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
            return false;
        }

        String tData = d.substring(indexStart + startTagLen + 4, (indexStart + startTagLen + 4  + LE * 2));
        resCode = 0;
        cardResult.setResCode(resCode);//设置响应码
        if (LE >= 2) {
            if((SSC_Card & 0x10) == 0) {//最后一包才判断响应码
                String tmCode = tData.substring(LE *2 - 4);
                if (tmCode.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS_STK_RUNNING)) {//STK菜单正在执行
                    resCode = FConstant.RES_FAIL_STK_RUNNING;
                    cardResult.setResCode(resCode);//设置响应码
                    return true;
                }
            }
        }
        TMKeyLog.d(TAG, "revData:" + revData + ">>>tData:" + tData);
        if (cardConnState == CardConnState.CARDCONN_SUCCESS_ADN || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS_CEN) {
            int l = 4;
            if ((SSC_Card & 0x30) == 0x00) { //只有一包
                if (! verifyDataMac(tData)) {
                    resCode = -1;
                    revData = "";
                    resPackCount = 1;
                    resPackCurrent = 1;
                    cardResult.setResCode(FConstant.RES_FAIL_VERIFY_MAC);//MAC错误
                    cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_VERIFY_MAC));
                    return false;
                }
                if (!"".equals(revOnePackData)) { //已经存储了部分数据
                    int ropdLen = revOnePackData.length();
                    if ((ropdLen < 4) || (ropdLen > 4 && ropdLen <= 12)) {//错误
                        resCode = -1;
                        revData = "";
                        resPackCount = 1;
                        resPackCurrent = 1;
                        cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                        return false;
                    } else if (ropdLen == 4) {//只有状态码
                        l = 4;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉状态字
                        }
                    } else { //数据和状态码
                        l = 12;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉MAC和状态字
                        }
                    }
                }
                revOnePackData = tData;
                revData += tData;
            } else if ((SSC_Card & 0x30) == 0x10) {//第一包
                if (!"".equals(revOnePackData)) { //已经存储了部分数据
                    int ropdLen = revOnePackData.length();
                    if ((ropdLen < 4) || (ropdLen > 4 && ropdLen < 12)) {//错误
                        resCode = -1;
                        revData = "";
                        resPackCount = 1;
                        resPackCurrent = 1;
                        cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                        return false;
                    } else if (ropdLen == 4) {//只有状态码
                        l = 4;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉MAC和状态字
                        }
                    } else { //数据和状态码
                        l = 12;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉MAC和状态字
                        }
                    }
                }
                revOnePackData = tData;
                revData += tData;
            } else if ((SSC_Card & 0x30) == 0x30) {//中间包
                revOnePackData += tData;
                revData += tData;
            } else if ((SSC_Card & 0x30) == 0x20) {//结尾包
                revOnePackData += tData;
                if (! verifyDataMac(revOnePackData)) {
                    resCode = -1;
                    revData = "";
                    resPackCount = 1;
                    resPackCurrent = 1;
                    cardResult.setResCode(FConstant.RES_FAIL_VERIFY_MAC);//MAC错误
                    cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_VERIFY_MAC));
                    return false;
                }
                revData += tData;
            }
        } else {//OMA
            if (! verifyDataMac(tData)){
                resCode = -1;
                revData = "";
                SSC_Card = 0;
                cardResult.setResCode(FConstant.RES_FAIL_VERIFY_MAC);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_VERIFY_MAC));
                return false;
            }
            revData += tData;
        }
        return true;
    }

    /**
     * 解析数据
     * @param d
     * @throws IOException
     */
    private boolean parseReceivBipData_New(String d,Boolean isLast) throws IOException {
        TMKeyLog.i(TAG, "parseReceiveData_New d= " + d);
        resCode = -1;
        int startTagLen = TAR_CARD.length();
        if (d == null || d.length() < (startTagLen + 4)) {
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return false;
        }
        if (! d.contains(TAR_CARD)) {
            TMKeyLog.i(TAG, "d not contains(TAR_CARD)>>>" + ", TAR_CARD is " + TAR_CARD);
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
            return false;
        }
        int indexStart = d.indexOf(TAR_CARD);
        String sessionId = d.substring(indexStart + startTagLen, indexStart + startTagLen + 2);
        TMKeyLog.i(TAG, "session id= " + sessionId);
        SSC_Card = Integer.parseInt(sessionId, 16);

        TMKeyLog.i(TAG, "SSC_Card:" + SSC_Card);

        String tLe = d.substring(indexStart + startTagLen + 2, indexStart + startTagLen + 4);
        int dataLenInt = FCharUtils.hexStr2Len(tLe);//应用数据一包长度
        LE = dataLenInt;
        TMKeyLog.i(TAG, "tLeInt:" + LE);

        if (LE * 2 > (d.length() - indexStart - startTagLen - 4)) {//数据长度有误
            TMKeyLog.i(TAG, "LE error...");
            resCode = -1;
            revData = "";
            SSC_Card = 0;
            cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
            return false;
        }

        String tData = d.substring(indexStart + startTagLen + 4, (indexStart + startTagLen + 4  + LE * 2));
        resCode = 0;
        cardResult.setResCode(resCode);//设置响应码
        if (LE >= 2) {
            if((SSC_Card & 0x10) == 0) {//最后一包才判断响应码
                String tmCode = tData.substring(LE *2 - 4);
                if (tmCode.equalsIgnoreCase(InsideDataStateCode.RES_SUCCESS_STK_RUNNING)) {//STK菜单正在执行
                    resCode = FConstant.RES_FAIL_STK_RUNNING;
                    cardResult.setResCode(resCode);//设置响应码
                    return true;
                }
            }
        }
        TMKeyLog.d(TAG, "revData:" + revData + ">>>tData:" + tData);
        if (cardConnState == CardConnState.CARDCONN_SUCCESS_ADN || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS_CEN) {
            int l = 4;
            if ((SSC_Card & 0x30) == 0x00) { //只有一包
                if (! verifyDataMac(tData)) {
                    resCode = -1;
                    revData = "";
                    resPackCount = 1;
                    resPackCurrent = 1;
                    cardResult.setResCode(FConstant.RES_FAIL_VERIFY_MAC);//MAC错误
                    cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_VERIFY_MAC));
                    return false;
                }
                if (!"".equals(revOnePackData)) { //已经存储了部分数据
                    int ropdLen = revOnePackData.length();
                    if ((ropdLen < 4) || (ropdLen > 4 && ropdLen <= 12)) {//错误
                        resCode = -1;
                        revData = "";
                        resPackCount = 1;
                        resPackCurrent = 1;
                        cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                        return false;
                    } else if (ropdLen == 4) {//只有状态码
                        l = 4;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉状态字
                        }
                    } else { //数据和状态码
                        l = 12;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉MAC和状态字
                        }
                    }
                }
                revOnePackData = tData;
                revData += tData;
            } else if ((SSC_Card & 0x30) == 0x10) {//第一包
                if (!"".equals(revOnePackData)) { //已经存储了部分数据
                    int ropdLen = revOnePackData.length();
                    if ((ropdLen < 4) || (ropdLen > 4 && ropdLen < 12)) {//错误
                        resCode = -1;
                        revData = "";
                        resPackCount = 1;
                        resPackCurrent = 1;
                        cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);//数据接收错误
                        cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                        return false;
                    } else if (ropdLen == 4) {//只有状态码
                        l = 4;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉MAC和状态字
                        }
                    } else { //数据和状态码
                        l = 12;
                        if (!"".equals(revData) && revData.length() >= l) {//已经存储了部分数据
                            TMKeyLog.d(TAG, "revData>>>delete control byte");
                            revData = revData.substring(0, revData.length() - l);//去掉MAC和状态字
                        }
                    }
                }
                revOnePackData = tData;
                revData += tData;
            } else if ((SSC_Card & 0x30) == 0x30) {//中间包
                revOnePackData += tData;
                revData += tData;
            } else if ((SSC_Card & 0x30) == 0x20) {//结尾包
                revOnePackData += tData;
                if (! verifyDataMac(revOnePackData)) {
                    resCode = -1;
                    revData = "";
                    resPackCount = 1;
                    resPackCurrent = 1;
                    cardResult.setResCode(FConstant.RES_FAIL_VERIFY_MAC);//MAC错误
                    cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_VERIFY_MAC));
                    return false;
                }
                revData += tData;
            }
        } else {//OMA
            if (! verifyDataMac(tData)){
                resCode = -1;
                revData = "";
                SSC_Card = 0;
                cardResult.setResCode(FConstant.RES_FAIL_VERIFY_MAC);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_VERIFY_MAC));
                return false;
            }
            revData += tData;
            if(!isLast){
                revData=revData.substring(0,revData.length()-12);
            }
        }
        return true;
    }


    /**
     * 发送交互指令数据
     *
     * @return
     */
    public CardResult sendInsideDataApdu(String dataStr, boolean isNeedSend) throws IOException {
        TMKeyLog.e(TAG, "sendInsideDataApdu>>>CardSmsVersion:" + CardSmsVersion + ">>>cardConnState:" + cardConnState);
        BipManager.Instruct="";
        BipManager.Instruct=dataStr;
        if (Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) { //新协议卡
            OMAPackageLen = 0xFF - 0x06;//OMA模式传输时一包数据的长度
            SMSPackageLen = 0xB0 - 0x07;//SMS模式传输时一包数据的长度，经测试（最大0xB0-1,6个控制字节）

            /**
             * 通讯录模式传输时一包数据的长度 总共17个字节 10个控制字节和7个数据字节
             */
            ADNPackageLen1_02 = 0xFF - 0x05;//第一次分包跟OMA一样
            return sendInsideDataApdu_New(dataStr, isNeedSend);
        }
        stateCode = "1000";
        cardResult = new CardResult();
        if (getCardConState() == CardConnState.CARDCONN_FAIL) { //卡片未连接
            cardResult.setResCode(FConstant.RES_FAIL_CARD_NO_CONNECTED);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_CARD_NO_CONNECTED));
            return cardResult;
        }
        if (dataStr == null) {
            cardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return cardResult;
        }
        String tempDataStr = FCharUtils.hexStr2LV(dataStr);//组装需要下送的LV格式数据
        TMKeyLog.e(TAG, "==tempDataStr==" + tempDataStr);
        randomStr = getRandom();
        ArrayList<String> al = getApduPackData(APDU_ID.NXYTrn, randomStr, tempDataStr);
        TMKeyLog.i(TAG, "进来这里了了");
        cardResult = sendWriteApdu(al, true);
        if (cardResult == null) {
            cardResult = new CardResult();
            cardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return cardResult;
        }
        resCode = cardResult.getResCode();
        TMKeyLog.i(TAG, resCode + "---------resCode" + ">>>resMsg:" + FConstant.getCardErrorMsg(resCode));
        if (resCode == FConstant.RES_SUCCESS) { //判断状态码，成功
            //将数据域部分进行解析，得到对应的数据项
            String tData = cardResult.getApduDecData();
            ArrayList<String> al1 = FCharUtils.parseDataLV(tData, false);
            if (al1 == null) {
                TMKeyLog.e(TAG, "parseData is null");
                cardResult.setInsideCode(FConstant.RES_FAIL_DATATRN);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_DATATRN));
                return cardResult;
            }
            if (al1.size() < 1) {
                TMKeyLog.e(TAG, "parseData size is 0");
                cardResult.setInsideCode(FConstant.RES_FAIL_DATATRN);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_DATATRN));
                return cardResult;
            }
            String d = al1.get(0);
            TMKeyLog.d(TAG, "al.get(0):" + d);
            if (d == null || d.length() < 4) {
                TMKeyLog.e(TAG, "d size is 0");
                cardResult.setInsideCode(FConstant.RES_FAIL_RECEIVE_DATA_ERROR);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA_ERROR));
                return cardResult;
            }
            if (! verifyDataMac(d)) {
                cardResult.setResCode(FConstant.RES_FAIL_VERIFY_MAC);//MAC错误
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_VERIFY_MAC));
                return cardResult;
            }
            stateCode = d.substring(d.length() - 4);//保存当前指令交互的状态码
            cardResult.setData(d);
        }
        TMKeyLog.e(TAG, "sendDataApdu end");
        return cardResult;
    }

    /**
     * 发送交互指令数据，简化指令格式之后的通信协议
     *
     * @return
     */
    public CardResult sendInsideDataApdu_New(final String dataStr, final boolean isNeedSend) throws IOException {
        TMKeyLog.e(TAG, "sendInsideDataApdu_New");
        BipManager.Instruct="";
        BipManager.Instruct=dataStr;
        stateCode = "1000";
        cardResult = new CardResult();
        if (getCardConState() == CardConnState.CARDCONN_FAIL) { //卡片未连接
            cardResult.setResCode(FConstant.RES_FAIL_CARD_NO_CONNECTED);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_CARD_NO_CONNECTED));
            return cardResult;
        }
        if (dataStr == null) {
            cardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return cardResult;
        }
        TMKeyLog.e(TAG, "==dataStr==" + dataStr);
        sendDataStr = dataStr;
        ArrayList<String> al = getSendDataList_New(dataStr);
        TMKeyLog.i(TAG, "进来这里了111");
        cardResult = sendWriteApdu(al, isNeedSend);
        if (cardResult == null) {
            cardResult = new CardResult();
            cardResult.setResCode(FConstant.RES_FAIL_SEND_DATA_ERROR);
            cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_SEND_DATA_ERROR));
            return cardResult;
        }

        resCode = cardResult.getResCode();
        TMKeyLog.i(TAG, resCode + "---------resCode" + ">>>resMsg:" + FConstant.getCardErrorMsg(resCode));
        if (resCode == FConstant.RES_SUCCESS) { //判断状态码，成功
            //将数据域部分进行解析，得到对应的数据项
            String tData = cardResult.getApduDecData();
            if (tData == null || tData.length() < 4) {//返回数据有误
                cardResult = new CardResult();
                cardResult.setResCode(FConstant.RES_FAIL_RECEIVE_DATA);
                cardResult.setErrMsg(FConstant.getCardErrorMsg(FConstant.RES_FAIL_RECEIVE_DATA));
                return cardResult;
            }
            stateCode = tData.substring(tData.length() - 4);//保存当前指令交互的状态码
            TMKeyLog.d(TAG, "tData:" + tData + ">>>stateCode:" + stateCode);
            if (InsideDataStateCode.RES_SUCCESS_STK_NORESPONCE.equals(stateCode)) {//卡片忙，需要重新发送指令
                TMKeyLog.d(TAG, "sendInsideDataApdu_New>>>RES_SUCCESS_STK_NORESPONCE");
                return sendInsideDataApdu_New(dataStr, isNeedSend);
            }
            cardResult.setData(tData);
        }
        TMKeyLog.e(TAG, "sendDataApdu end");
        return cardResult;
    }

    /**
     * 获取数据
     * @param orderData 指令数据
     * @param Le 长度
     * @return
     * @throws IOException
     */
    protected String getResponseData6C (final String orderData, final String Le) throws IOException{
        TMKeyLog.d(TAG, "getResponseData6C>>>orderData:" + orderData + ">>>Le:" + Le);
        String recData = "";
        tmCardResult = null;
        exception = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //将原有指令的最后一字节Le替换
                    String ps = orderData.substring(0, orderData.length() - 2);
                    ps = ps + Le;
                    tmCardResult = sendInsideDataApdu(ps, true);
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
        TMKeyLog.d(TAG, "getSignData>>>notTimeOut:" + notTimeOut);
        if (exception != null || tmCardResult == null) {//发送数据异常导致
            throw new IOException(exception);
        }

        if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
            recData = tmCardResult.getData();
            if (recData == null || recData.length() < 12) {
                clearRevSb();
                return "";
            }
            int trcLen = recData.length();
            String tmRecData = recData.substring(0, trcLen - 12);
            String tmRecMac = recData.substring(trcLen - 12, trcLen - 4);
            TMKeyLog.d(TAG, "getResponseData6C>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
            if ((Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) && (cardConnState == CardConnState.CARDCONN_SUCCESS_ADN || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS_CEN || cardConnState == CardConnState.CARDCONN_SUCCESS_BIP)) {
                revSb.append(tmRecData);
            } else {
                //校验MAC
                String soMac = FCharUtils.showResult16Str(tmjni.insideMac(tmRecData));
                TMKeyLog.d(TAG, "getResponseData6C>>>soMac:" + soMac);
                if (tmRecMac.equalsIgnoreCase(soMac)) {//MAC校验通过
                    revSb.append(tmRecData);
                } else {
                    clearRevSb();
                    return "";
                }
            }
            int recLen = recData.length();
            if (recLen < 4) {
                return "";
            }
            String stateCode = recData.substring(recLen - 4);
            if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) { //返回状态码61XX，继续接收数据
                String le = stateCode.substring(2);
                //TODO 已经保存了部分数据，继续接收数据，不需要重新初始化 revSb
                return getResponseData(le);
            }else if (recData.endsWith(InsideDataStateCode.RES_SUCCESS)) {
                return revSb.toString();
            }
        }
        clearRevSb();
        return "";
    }

    /**
     * 获取数据
     * @param Le 长度
     * @return
     * @throws IOException
     */
    protected String getResponseData (final String Le) throws IOException{
        TMKeyLog.d(TAG, "getResponseData>>>Le:" + Le);
        String recData = "";
        tmCardResult = null;
        exception = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isNeedSend = true;
                    if ((Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) && (cardConnState == CardConnState.CARDCONN_SUCCESS_ADN || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS_CEN)) {
                        isNeedSend = false;
                    }
                    String sendData = "B1C00000" + Le;
                    tmCardResult = sendInsideDataApdu(sendData, isNeedSend);
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
        TMKeyLog.d(TAG, "getSignData>>>notTimeOut:" + notTimeOut);
        if (exception != null || tmCardResult == null) {//发送数据异常导致
            throw new IOException(exception);
        }

        if (tmCardResult.getResCode() == FConstant.RES_SUCCESS  && (tmCardResult.getInsideCode() != FConstant.RES_FAIL_DATATRN)) {
            recData = tmCardResult.getData();
            if (recData == null || recData.length() < 12) {
                clearRevSb();
                return "";
            }
            int trcLen = recData.length();
            String tmRecData = recData.substring(0, trcLen - 12);
            String tmRecMac = recData.substring(trcLen - 12, trcLen - 4);
            TMKeyLog.d(TAG, "getResponseData>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
            if ((Integer.parseInt(CardSmsVersion, 16) >= CARDVERSION_02) && (cardConnState == CardConnState.CARDCONN_SUCCESS_ADN || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS || cardConnState == CardConnState.CARDCONN_SUCCESS_SMS_CEN || cardConnState == CardConnState.CARDCONN_SUCCESS_BIP)) {
                revSb.append(tmRecData);
            } else {
                //校验MAC
                String soMac = FCharUtils.showResult16Str(tmjni.insideMac(tmRecData));
                TMKeyLog.d(TAG, "getResponseData>>>soMac:" + soMac);
                if (tmRecMac.equalsIgnoreCase(soMac)) {//MAC校验通过
                    revSb.append(tmRecData);
                } else {
                    clearRevSb();
                    return "";
                }
            }

            int recLen = recData.length();
            if (recLen < 4) {
                return "";
            }
            String stateCode = recData.substring(recLen - 4);
            if (stateCode.startsWith(InsideDataStateCode.RES_SUCCESS_CONTINUE)) {//返回状态码61XX，继续接收数据
                String le = stateCode.substring(2);
                return getResponseData(le);
            } else if (recData.endsWith(InsideDataStateCode.RES_SUCCESS)) {
                return revSb.toString();
            } else { //错误状态
                clearRevSb();
                return "";
            }
        }
        clearRevSb();
        return "";
    }

    protected String getFactor(int ssc, String randStr) {
        TMKeyLog.i(TAG, "randStr = " + randStr);
        int rand1 = Integer.parseInt(randStr.substring(0, 2), 16);
        int rand2 = Integer.parseInt(randStr.substring(2), 16);

        int xx = ssc ^ rand1 ^ rand2;
        String result = FCharUtils.int2HexStr(ssc) + randStr + FCharUtils.int2HexStr(xx);

        TMKeyLog.i(TAG, "Factor = " + result);
        return result;
    }

    /**
     * 通过SO校验MAC
     *
     * @param ranStr 16进制字符串
     * @param ssc
     * @param ds     16进制字符串
     * @param macStr 16进制字符串
     * @return
     */
    public boolean verifyMacSo(String ranStr, int ssc, String ds, String macStr) {
        TMKeyLog.i(TAG, "verifyMacSo...>>>ranStr:" + ranStr + ">>>ssc:" + ssc + ">>>macStr=" + macStr);

        //会话ID+XXXX + (id ^ XX ^ XX)
        String result = getFactor(ssc, ranStr);

        //String mainKey = SPUtil.getSharedPreferences(FConstant.TRN_KEY, "");
        String factor = TAR_CARD + result + "FF";//将SSC和随机数拼接为完整的8字节分散因子,注意SessionId,不能为00
        TMKeyLog.i(TAG, "factor:" + factor + ">>>ds:" + ds + ">>>mainKey:" + TRN_KEY);

        byte[] macRes = tmjni.mac(TRN_KEY, factor, ds);
        if (macRes == null) {
            return false;
        }

        String mac = FCharUtils.showResult16Str(macRes);
        TMKeyLog.i(TAG, "mac:" + mac);
        if (mac.equalsIgnoreCase(macStr)) {
            return true;
        }
        return false;
    }


    /**
     * 获取随机数，全由0-9组成
     *
     * @return
     */
    protected String getRandom() {
        StringBuffer sbf = new StringBuffer();
        Random r = new Random();
        int dl = 4;
        if (CardSmsVersion.equals("00") && isADN) {//通讯录模式随机数长度为2
            dl = 2;
        }
        for (int i = 0; i < dl; i++) {
            sbf.append(r.nextInt(10));
        }

        TMKeyLog.i(TAG, "random = " + sbf.toString());
        return sbf.toString();
    }

    /**
     * 递增SSC
     */
    public void dealSSC() {
        if (isADN) {
            SSC = SSC & 0x1F;
            String SSCStr = FCharUtils.int2HexStr(SSC);
            if ("9".equals(SSCStr.substring(1))) {//如果以9结尾下一次直接加7
                SSC += 7;
            } else {
                SSC++;
            }
            if (SSC > 0x19) {
                SSC = 0;
            }
            if (SIMHelper.isNeedShift) { //电信模式需要做移位处理，则将高两位置为10
                SSC = SSC | 0x80;
            } else {//否则，高两位置为01
                SSC = SSC | 0x40;
            }
        } else {
            String SSCStr = FCharUtils.int2HexStr(SSC);
            if ("9".equals(SSCStr.substring(1))) {//如果以9结尾下一次直接加7
                SSC += 7;
            } else {
                SSC++;
            }
            if (SSC > 0x99) {
                SSC = 0;
            }
        }
        TMKeyLog.i(TAG, "SSC:" + SSC);
    }
    /**
     * SSC
     */
    public void dealSSC_New() {
        if (isADN) {
            SSC = SSC & 0x07;
            if (SSC == 0x07) {
                SSC = 0;
            }
            if (SIMHelper.isNeedShift) { //电信模式需要做移位处理，则将高两位置为10
                SSC = SSC | 0x80;
            } else {//否则，高两位置为01
                SSC = SSC | 0x40;
            }
        } else {
            SSC = SSC & 0x07;
            if (SSC == 0x07) {
                SSC = 0;
            }
        }
        SSC ++;
        TMKeyLog.i(TAG, "dealSSC>>>SSC:" + SSC);
    }

    /**
     * 将总包数和当前包数组合成三个字节，第一个字节的前4位是拼接到总包数，后4位拼接到当前包数
     *
     * @param pc
     * @param pi
     * @return 总条数+高位+当前条数
     */
    public String packPackCount(int pc, int pi) {
        String pStr = "";
        String pcStr = "" + pc;
        String piStr = "" + pi;
        if (pcStr.length() == 1) {//处理总包数
            pStr = "0";//确认需要拆分的字节高位
            pcStr = "0" + pcStr;//补位
        } else if (pcStr.length() == 2) {
            pStr = "0";//确认需要拆分的字节高位
        } else if (pcStr.length() == 3) {
            pStr = pcStr.substring(0, 1);//确认需要拆分的字节高位
            pcStr = pcStr.substring(1);//截取
        } else {//数据长度太长，错误
            return "0000000";
        }
        if (piStr.length() == 1) {//处理当前包数
            pStr += "0";//确认需要拆分的字节高位
            piStr = "0" + piStr;//补位
        } else if (piStr.length() == 2) {
            pStr += "0";//确认需要拆分的字节高位
        } else if (piStr.length() == 3) {
            pStr += piStr.substring(0, 1);//确认需要拆分的字节高位
            piStr = piStr.substring(1);//截取
        } else {//数据长度太长，错误
            return "0000000";
        }
        return pcStr + pStr + piStr;
    }

    /**
     * 将3个字节拆分，第一个字节的前4位是拼接到总包数，后4位拼接到当前包数
     *
     * @param p 16进制字符串
     * @return
     */
    public int[] parsePackCount(String p) {
        int[] ri = new int[2];
        String pStr = p.substring(0, 2);
        String pcStr = p.substring(2, 4);
        String piStr = p.substring(4, 6);
        pcStr = pStr.substring(0, 1) + pcStr;
        piStr = pStr.substring(1, 2) + piStr;

        ri[0] = Integer.parseInt(pcStr, 10);
        ri[1] = Integer.parseInt(piStr, 10);
        return ri;
    }

    /**
     * 将t字节hex编码转换为(t + 1)字节dec编码
     * @param s
     * @return
     */
    private String dealBigIntegerApdu (String s, int t) {
        if (s == null || s.length() != (2 * t)) {
            return "";
        }
        StringBuffer sbf = new StringBuffer();
        s = "" + Long.parseLong(s, 16);
        int so1l = (2 * (t + 1)) - s.length();
        for (int i = 0; i < so1l; i ++){
            sbf.append("0");
        }
        sbf.append(s);
        return sbf.toString();
    }

    /**
     * 将ADN数据，6字节转换为8字节
     * @param s
     * @param t 老版的ADN数据域是两个4字节转5字节，t = 4
     *           新版的ADN数据域是两个3字节转4字节，t = 3
     * @return
     */
    private String dealADNApdu (String s, int t) {
        if (s == null || s.length() < 4 * t) {
            return "";
        }
        StringBuffer sbf = new StringBuffer();
        String so1 = s.substring(0, 2*t);
        String so2 = s.substring(2*t, 4 * t);
        String sd = s.substring(4 * t);
        so1 = dealBigIntegerApdu(so1, t);
        sbf.append(so1);
        so2 = dealBigIntegerApdu(so2, t);
        sbf.append(so2);
        sbf.append(sd);
        return sbf.toString();
    }

    /**
     * 卡片连接状态
     * <p>
     * CardConnState.CARDCONN_FAIL 0
     * CardConnState.CARDCONN_CONNECTING 1
     * CardConnState.CARDCONN_SUCCESS_OMA 2
     * CardConnState.CARDCONN_SUCCESS_SMS 3
     * CardConnState.CARDCONN_SUCCESS_ADN 4
     *
     * @return
     */
    public int getCardConState() {
        TMKeyLog.d(TAG, "getCardConState>>>hasCard:" + hasCard + ">>>cardConnState:" + cardConnState);
        if (hasCard || initChannelState == CardConnState.CARDCONN_SUCCESS_OMA || initChannelState == CardConnState.CARDCONN_SUCCESS_UICC||initChannelState == CardConnState.CARDCONN_SUCCESS_BIP) {
            return cardConnState;
        }
        return CardConnState.CARDCONN_FAIL;
    }

    /**
     * 获取错误状态码
     * @return
     */
    public String getErrorStateCode () {
        return stateCode;
    }

    /**
     * 获取错误状态码
     * @return
     */
    public void setErrorStateCode (String sc) {
        this.stateCode = sc;
    }

    /**
     * 清空数据缓存
     */
    public void clearRevSb () {
        revSb = new StringBuilder();
    }

    public boolean verifyDataMac(String resData) {
        TMKeyLog.d(TAG, "verifyDataMac>>>isNeedVerifyMac:" + isNeedVerifyMac + ">>>resData:" + resData);
        if (! isNeedVerifyMac) {
            return true;
        }
        if (resData == null || "".equals(resData)) {
            return false;
        }
        int trcLen = resData.length();
        if(trcLen < 4) {
            return false;
        }
        if(trcLen < 12) {
            return true;
        }
        String tmRecData = resData.substring(0, trcLen - 12);
        String tmRecMac = resData.substring(trcLen - 12, trcLen - 4);
        TMKeyLog.d(TAG, "verifyDataMac>>>tmRecData:" + tmRecData + ">>>tmRecMac:" + tmRecMac);
        //校验MAC
        String soMac = FCharUtils.showResult16Str(tmjni.insideMac(tmRecData));
        TMKeyLog.d(TAG, "verifyDataMac>>>soMac:" + soMac);
        if (tmRecMac.equalsIgnoreCase(soMac)) {//MAC校验通过
            return true;
        }
        return false;
    }

    /**
     * 解析卡片信息
     */
    public static void parseCardInfo () {
        TMKeyLog.d(TAG, "parseCardInfo>>>CardSmsVersion:" + CardSmsVersion);
        cardInfo = null;
        if (Integer.parseInt(SIMBaseManager.CardSmsVersion, 16) >= SIMBaseManager.CARDVERSION_02) {
            TMKeyLog.d(TAG, "initCardInfoStr:" + initCardInfoStr);
            if (initCardInfoStr != null && !"".equals(initCardInfoStr)) {
                if (initCardInfoStr.contains(E_UKEY)) {
                    TMKeyLog.d(TAG, "initCardInfoStr contains E_UKEY");
                    int E_UKEYIndex =initCardInfoStr.indexOf(E_UKEY);
                    TMKeyLog.d(TAG, "E_UKEYIndex:" + E_UKEYIndex);
                    int E_UKEYLen = E_UKEY.length();
                    TMKeyLog.d(TAG, "E_UKEYLen:" + E_UKEYLen);
                    int cardInfoLen = initCardInfoStr.length();
                    TMKeyLog.d(TAG, "cardInfoLen:" + cardInfoLen);
                    if (cardInfoLen < E_UKEYIndex + E_UKEYLen + 110) {
                        cardInfo = null;
                        return;
                    }
                    String dataStr = initCardInfoStr.substring(E_UKEYIndex, E_UKEYIndex + E_UKEYLen + 102);
                    TMKeyLog.d(TAG, "dataStr:" + dataStr);
                    String macStr = initCardInfoStr.substring(E_UKEYIndex + E_UKEYLen + 102, E_UKEYIndex + E_UKEYLen + 110);
                    TMKeyLog.d(TAG, "macStr:" + macStr);
                    if (!verifyCardInfoMac(TAR_CARD_START + dataStr, macStr)) { //MAC校验失败
                        TMKeyLog.d(TAG, "verifyCardInfoMac fail");
                        cardInfo = null;
                        return;
                    }
                    TMKeyLog.d(TAG, "verifyCardInfoMac success");
                    cardInfo = new CardInfo();
                    E_UKEYIndex += E_UKEYLen;
                    CardSmsVersion = initCardInfoStr.substring(E_UKEYIndex, E_UKEYIndex + 2);
                    E_UKEYIndex += 2;
                    String initPwdStr = initCardInfoStr.substring(E_UKEYIndex, E_UKEYIndex + 2);
                    if (initPwdStr.equals("30")) {
                        cardInfo.setInitPwd(true);
                    } else {
                        cardInfo.setInitPwd(false);
                    }
                    E_UKEYIndex += 2;
                    String infoData = initCardInfoStr.substring(E_UKEYIndex, E_UKEYIndex + 62);
                    ArrayList<String> infoList = FCharUtils.parseDataLV(infoData, false);
                    if (infoList == null || infoList.size() < 4) {
                        cardInfo = null;
                        return;
                    }
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
                    cardInfo.setCer_state_rsa1024(cerState.substring(0, 2));
                    cardInfo.setCer_state_rsa2048(cerState.substring(2, 4));
                    cardInfo.setCer_state_sm2_sign(cerState.substring(4, 6));
                    cardInfo.setCer_state_sm2_enc(cerState.substring(6, 8));
                    cardInfo.setMerchantInfo(merchantInfo);
                    cardInfo.setCosVersion(cosVersion);
                    cardInfo.setCsn(csnStr);
                    E_UKEYIndex += 62;
                    String rsaSha1 = initCardInfoStr.substring(E_UKEYIndex, E_UKEYIndex + 12);
                    TMKeyLog.d(TAG, "rsaSha1:" + rsaSha1);
                    cardInfo.setRsaSha1(rsaSha1);
                    E_UKEYIndex += 12;
                    String sm2SignSha1 = initCardInfoStr.substring(E_UKEYIndex, E_UKEYIndex + 12);
                    TMKeyLog.d(TAG, "sm2SignSha1:" + sm2SignSha1);
                    cardInfo.setSm2SignSha1(sm2SignSha1);
                    E_UKEYIndex += 12;
                    String sm2EncSha1 = initCardInfoStr.substring(E_UKEYIndex, E_UKEYIndex + 12);
                    TMKeyLog.d(TAG, "sm2EncSha1:" + sm2EncSha1);
                    cardInfo.setSm2EncSha1(sm2EncSha1);
                }
            }
        }
    }

    /**
     * 处理Cos版本号转换显示字符
     * @param hs
     * @return
     */
    public static String dealCosVer (String hs) {
        if (hs == null || hs.length() != 8) {
            return "";
        }
        byte[] hsb = FCharUtils.hexString2ByteArray(hs);
        int v1 = hsb[0];
        int v2 = hsb[1];
        int v3 = hsb[2];
        int v4 = hsb[3];
        return v1 + "." + v2 + "." + v3 + "." + v4;
    }

    public static boolean verifyCardInfoMac(String dataStr, String macStr) {
        TMKeyLog.d(TAG, "verifyCardInfoMac>>>dataStr:" + dataStr + ">>>macStr:" + macStr);
        if (dataStr == null || "".equals(dataStr)) {
            return false;
        }
        if (macStr == null || "".equals(macStr)) {
            return false;
        }
        //校验MAC
        String soMac = FCharUtils.showResult16Str(tmjni.insideMac(dataStr));
        TMKeyLog.d(TAG, "verifyCardInfoMac>>>soMac:" + soMac);
        if (macStr.equalsIgnoreCase(soMac)) {//MAC校验通过
            return true;
        }
        return false;
    }

    public static CardInfo getInitCardInfo () {
        return cardInfo;
    }

}
