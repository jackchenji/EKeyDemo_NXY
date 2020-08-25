package com.froad.ukey.utils.np;

/**
 * Created by FW on 2017/1/3.
 */
public class CardConnState {
    public final static int CARDCONN_FAIL = 0;//卡片连接失败
    public final static int CARDCONN_CONNECTING = 1;//卡片连接中
    public final static int CARDCONN_SUCCESS_OMA = 2;//OMA模式
    public final static int CARDCONN_SUCCESS_SMS = 3;//SMS模式
    public final static int CARDCONN_SUCCESS_ADN = 4;//ADN模式
    public final static int CARDCONN_SUCCESS_SMS_CEN = 5;//短信中心模式
    public final static int CARDCONN_SUCCESS_UICC = 6;//UICC模式
    public final static int CARDCONN_SUCCESS_BIP = 7;//BIP模式
}
