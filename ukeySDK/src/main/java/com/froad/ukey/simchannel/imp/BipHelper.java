package com.froad.ukey.simchannel.imp;

import android.content.ContentValues;
import android.content.Context;
import android.telephony.SmsManager;

import com.froad.ukey.interf.CardConCallBack;
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.utils.np.TMKeyLog;

import java.io.IOException;
import java.util.List;

import bip.BipManager;

/*
 *Created by user on 2020/5/13
 */ public class BipHelper extends SIMHelper {
    private Context mContext;
    private CardConCallBack mCardConCallBack = null;

    public BipHelper(Context con, CardConCallBack cardConCallBack){
        mContext = con;
        mCardConCallBack = cardConCallBack;

    }


    /**
     * 打开设备
     *
     * @return true 打开成功; false 打开失败
     */
    @Override
    public boolean open() {
        TMKeyLog.d("bips>>>", "进来了");
        BipManager.getInstance().onStartBip("B11000000C");  //通过获取卡号来判断是否有卡，有卡号代表有卡，没卡号代表没卡
        return BipManager.hasCard;
        //return  true;
    }

    /**
     * 关闭设备
     *
     * @return true 关闭成功; fasle 关闭失败
     */
    @Override
    public boolean close() {
        return false;
    }

    /**
     * 发送数据
     *
     * @param hexStr
     * @return true 发送成功; false 发送失败
     * @throws IOException
     */
    @Override
    public boolean transmitHexData(String hexStr) throws IOException {
        return false;
    }

    /**
     * 接收数据
     *
     * @return 接收到的字符串
     */
    @Override
    public String receiveData() {
        return null;
    }

    /**
     * 返回多条数据
     *
     * @return List类型的数据列表
     */
    @Override
    public List<String> receiveDataList() {
        return null;
    }

    /**
     * 得到一个联系人ContentValues
     *
     * @param hexStr
     * @return
     */
    @Override
    public ContentValues getContentValues(String hexStr) {
        return null;
    }

    /**
     * ADN方式发送数据
     *
     * @param list
     * @return true 发送成功; false 发送失败
     * @throws Exception
     */
    @Override
    public boolean insetContentValues(List<ContentValues> list) throws Exception {
        return false;
    }

    @Override
    public SIMHelper initSimHelper() {
        return null;
    }

    @Override
    public boolean isSupport() {
        return false;
    }
}
