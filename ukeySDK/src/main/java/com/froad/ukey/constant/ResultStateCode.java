package com.froad.ukey.constant;

/**
 * Created by FW on 2017/12/1.
 */

public class ResultStateCode {
    public final static String STATE_OK = "000000";
    public final static String STATE_FAIL_1000 = "1000";//调用失败接口异常
    public final static String STATE_FAIL_1001 = "1001";//密码错了，您还可以输入?次。|调用获取密文失败|获取X509为空。
    public final static String STATE_FAIL_1002 = "1002";//密码错误次数已达上限，锁卡
    public final static String STATE_FAIL_1003 = "1003";//签名处理失败
    public final static String STATE_FAIL_1004 = "1004";//密码修改失败
    public final static String STATE_FAIL_1005 = "1005";//V盾证书异常
    public final static String STATE_FAIL_1006 = "1006";//签名异常
    public final static String STATE_FAIL_1007 = "1007";//验签不通过
    public final static String STATE_FAIL_1008 = "1008";//密码错了，您还可以输入?次。（修改密码）
    public final static String STATE_FAIL_1009 = "1009";//密码错误次数已达上限，锁卡。（修改密码）
    public final static String STATE_2000 = "2000";//V盾签名功能一直开启
    public final static String STATE_2001 = "2001";//V盾按需开启，同时V盾签名功能已开启
    public final static String STATE_2002 = "2002";//V盾按需开启，同时V盾签名功能已关闭
    public final static String STATE_3000 = "3000";//初始密码，请先修改密码再使用签名功能
    public final static String STATE_FFFF = "FFFF";//发送数据失败
    public final static String STATE_FFFE = "FFFE";//接收数据失败
    public final static String STATE_FFFD = "FFFD";//接收数据解析失败
    public final static String STATE_FFFC = "FFFC";//获取CSN失败，无卡
    public final static String STATE_FFFB = "FFFB";//未检测到卡
    public final static String STATE_FFFA = "FFFA";//卡片COS版本号大于当前客户端可识别的最大版本号，需要更新客户端SDK
    public final static String STATE_FFF9 = "FFF9";//发送数据格式有误
    public final static String STATE_FFF8 = "FFF8";//CRC校验错误
}
