package com.froad.ukey.interf;

/**
 * Created by FW on 2017/3/31.
 */
public interface CardConCallBack {
    /**
     * OMA连接状态
     * @param isSuccess 是否打开成功
     * @param msg 提示信息
     */
    void OmaOpenState (boolean isSuccess, String msg);

    /**
     * OMA连接状态
     * @param isSuccess 是否打开成功
     * @param msg 提示信息
     */
    void OmaDefaultOpenState (boolean isSuccess, String msg);

    /**
     * ADN连接状态
     * @param isSuccess 是否打开成功
     * @param msg 提示信息
     */
    void AdnOpenState (boolean isSuccess, String msg);

}
