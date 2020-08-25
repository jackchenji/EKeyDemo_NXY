package com.froad.ukey.jni;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsMessage;

import com.froad.ukey.simchannel.imp.SESDefaultHelper;
import com.froad.ukey.simchannel.imp.SMSHelper;
import com.froad.ukey.utils.np.TMKeyLog;

import java.util.ArrayList;

/**
 * tmjni
 *
 * 此类包括一些本地函数的声明
 *
 * @author  by FW.
 * @date    16/12/26
 * @modify  修改者 FW
 */

public class tmjni {

    private static final String TAG = "tmjni";

    static {
        try {
            //导入动态库
            System.loadLibrary("tmjni");
            TMKeyLog.i(TAG, "loadLibrary tmjni");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * 加密数据
     *
     * @param mainKey
     * @param factor1
     * @param dataStr
     * @return 字节数组
     */
    public static native byte[] encData(String mainKey, String factor1, String dataStr);

    /**
     * 解密数据
     *
     * @param mainKey
     * @param factor1
     * @param dataStr
     * @return 字节数组
     */
    public static native byte[] decData(String mainKey, String factor1, String dataStr);

    /**
     * 计算 MAC
     *
     * @param mainKey
     * @param factor1
     * @param dataStr
     * @return 字节数组
     */
    public static native byte[] mac(String mainKey, String factor1, String dataStr);

    /**
     * 计算数据域的MAC
     * @param dataStr
     * @return
     */
    public static native byte[] insideMac(String dataStr);


    /**
	 * 打开ADN通道
	 * 
     * @param smsHelper
     * @param c1
     * @param c2
     * @param c3
     */
    public static native void openDeviceADN(SMSHelper smsHelper, Class c1, Class c2, Class c3);

    /**
     * 通过短信的方式向卡片发送给数据
     *
     * @param smsHelper
     * @param str
     * @return 0 成功，1 失败
     */
    public static native int transmitHexDataADN(SMSHelper smsHelper, String str);

    /**
     * 通过短信的方式向卡片发送给数据
     *
     * @param smsHelper
     * @return 0 成功，1 失败
     */
    public static native ArrayList<SmsMessage> method1ADN(SMSHelper smsHelper);

    /**
     * 判断是否有卡
     *
     * @param str
     * @return 0 成功，1 失败
     */
    public static native int hasCardADN(String str);


    /**
     * 获取PDU数据
     *
     * @param smsMessage
     * @return 字节数组
     */
    public static native byte[] myGetPDUADN(SmsMessage smsMessage);

    /**
     * 从卡片端接收数据
     *
     * @param str
     * @param smsMessage
     * @param list
     * @return 0 continue，1 正常执行
     */
    public static native long receiveDataListADN(String str, SmsMessage smsMessage, ArrayList<String> list);

    /**
     * 初始化Uri
     *
     * @return
     */
    public static native Uri initUriADN();

    /**
     * 通过ADN通道向卡片端发送数据
     *
     * @param cursor
     * @param contentResolver
     * @param uri
     * @param contentValues
     * @return 0 成功，1 失败
     */
    public static native int insetDataADN(SMSHelper smsHelper, Cursor cursor, ContentResolver contentResolver, Uri uri, ContentValues contentValues);

    /**
     * OMA通道数据传输
     *
     * @param hexString 待传输的数据, 十六进制表示的字符串
     * @param sesHelper SESHelper实例对象
     * @return 传输结果的返回码
     */
    public static native String transHexOma(String hexString, SESDefaultHelper sesHelper);

    /**
     * 关闭OMA通道
     *
     * @param sesHelper SESHelper实例
     * @return true 成功, false 失败
     */
    public static native boolean close(SESDefaultHelper sesHelper);

    /**
     * 设置应用的AID
     *
     * @param aid
     */
    public static native void setFroadAid(String aid);

    public static native byte[] deseseEncCbc (byte[] data);

}
