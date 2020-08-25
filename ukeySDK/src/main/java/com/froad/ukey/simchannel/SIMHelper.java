package com.froad.ukey.simchannel;

import android.content.ContentValues;

import java.io.IOException;
import java.util.List;

/**
 * SESHelper
 *
 * 通信通道管理的抽象类
 *
 * @author  by FW.
 * @date    16/12/26
 * @modify  修改者 FW
 */
public abstract class SIMHelper {

    public boolean isOpen = false;// 是否已经打开通道
    public static boolean isNeedShift = false;// 是否是电信卡，需要将短信内容移位
    public static final String AID = "A000000046582D552D4B65793031";
//    public static final String AID = "A000000003454944";//EID应用的AID
    public Object omaServie;
    public final long READ_SMS_TIME_OUT = 5000;//读取短信超时时间

    /**
     * 打开设备
     *
     * @return true 打开成功; false 打开失败
     */
    public abstract boolean open();

    /**
     * 关闭设备
     *
     * @return true 关闭成功; fasle 关闭失败
     */
    public abstract boolean close();

    /**
     * 发送数据
     *
     * @param hexStr
     * @return true 发送成功; false 发送失败
     * @throws IOException
     */
    public abstract boolean transmitHexData(String hexStr) throws IOException;

    /**
     * 接收数据
     *
     * @return 接收到的字符串
     */
    public abstract String receiveData();

    /**
     * 返回多条数据
     *
     * @return List类型的数据列表
     */
    public abstract List<String> receiveDataList();

    /**
     * 得到一个联系人ContentValues
     *
     * @param hexStr
     * @return
     */
    public abstract ContentValues getContentValues(String hexStr);

    /**
     * ADN方式发送数据
     *
     * @param list
     * @return true 发送成功; false 发送失败
     * @throws Exception
     */
    public abstract boolean insetContentValues(List<ContentValues> list) throws Exception;

    public abstract SIMHelper initSimHelper () ;

    public abstract boolean isSupport ();
}
