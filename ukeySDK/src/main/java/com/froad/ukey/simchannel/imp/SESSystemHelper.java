package com.froad.ukey.simchannel.imp;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.se.omapi.Channel;
import android.se.omapi.Reader;
import android.se.omapi.SEService;
import android.se.omapi.Session;
import android.support.annotation.RequiresApi;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.interf.CardConCallBack;
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by FW on 2018/1/5.
 */

@RequiresApi(Build.VERSION_CODES.P)
public class SESSystemHelper extends SIMHelper {

    private static final String TAG = FConstant.LOG_TAG + "SESSystemHelper";

    private String receiveStr = null;//接收的字符串
    private SEService seService;

    private Channel channel = null;
    private Context mContext;
    private CardConCallBack mCardConCallBack;
    private Session session;

    public SESSystemHelper() {
    }

    public SESSystemHelper(Context con, CardConCallBack cardConCallBack) {

        try {
            TMKeyLog.i(TAG, "creating SEService object...");
            mContext = con;
            this.mCardConCallBack = cardConCallBack;
            isOpen = false;
            TMKeyLog.d(TAG, "当前线程ID：" + Thread.currentThread().getId());
            seService = new SEService(mContext, Executors.newSingleThreadExecutor(), new SeCallBack());
            TMKeyLog.d(TAG, "SESSystemHelper>>>seService:" + seService);
        } catch (Exception e) {
            TMKeyLog.e(TAG, "creating SEService objec exception...");
            e.printStackTrace();
            isOpen = false;
            if (seService != null) {
                seService.shutdown();
                seService = null;
            }
            omaConTimeOut();
        }
    }

    /**
     * 判断是否已经打开通道
     *
     * @return true 已打开; false 未打开
     */
    public boolean open() {
        return isOpen;
    }


    /**
     * 打开通道
     */
    private void openChannel() {
        TMKeyLog.d(TAG, "openChannel");
        try {
            if (isOpen) {
                TMKeyLog.d(TAG, "openChannel>>>isOpen:" + isOpen);
                omaConSuccess();
                return;
            }
            if (seService == null) {
                TMKeyLog.d(TAG, "openChannel>>>seService is null");
            } else if (!seService.isConnected()) {
                TMKeyLog.d(TAG, "openChannel>>>seService is not connected");
            }
            if ((this.seService != null) && (this.seService.isConnected())) {
                Reader[] readers = this.seService.getReaders();
                TMKeyLog.e(TAG, "readers.length：" + readers.length);
                if (readers.length < 1) {
                    TMKeyLog.e(TAG, "readers.length < 1");
                    this.isOpen = false;
                    omaConTimeOut();
                    return;
                }
                String readerName = "";
                for (int i = 0; i < readers.length; i++) {
                    readerName = readers[i].getName().toUpperCase();
                    TMKeyLog.e(TAG, "readers[" + i + "].Name--->" + readerName);
                    if (readerName.startsWith("SIM")) {
                        try {
                            session = readers[i].openSession();
                            if (session == null) {
                                continue;
                            }
                            TMKeyLog.e(TAG, "openSession success");
                            break;//openSessin成功后需要结束循环
                        } catch (Exception e) {
                            TMKeyLog.e(TAG, "openSession exception--->" + e.getMessage());
                            e.printStackTrace();
                            isOpen = false;
                            omaConTimeOut();
                            return;
                        }
                    } else {
                        continue;
                    }
                }
                if (session != null) {
                    TMKeyLog.e(TAG, "session is not null");
                    try {
                        channel = session.openLogicalChannel(FCharUtils.hexString2ByteArray(AID));
                        TMKeyLog.e(TAG, "openLogicalChannel success>>>channel:" + channel);

                        if (channel == null) {
                            TMKeyLog.d(TAG, "channel == null");
                            isOpen = false;
                            omaConTimeOut();
                            return;
                        }
                        TMKeyLog.d(TAG, "channel is not null");
                        if (channel.isOpen()) {
                            TMKeyLog.d(TAG, "openChannel>>>omaConSuccess");
                            this.isOpen = true;
                            omaConSuccess();
                            return;
                        } else {
                            TMKeyLog.d(TAG, "channel.is not open");
                            isOpen = false;
                            omaConTimeOut();
                            return;
                        }
                    } catch (Exception e) {
                        TMKeyLog.e(TAG, "openLogicalChannel exception--->" + e.getMessage());
                        e.printStackTrace();
                        if (this.channel != null) {
                            this.channel.close();
                        }
                        isOpen = false;
                        omaConTimeOut();
                        return;
                    }
                } else {
                    TMKeyLog.d(TAG, "session == null");
                    isOpen = false;
                    omaConTimeOut();
                    return;
                }
            } else {
                TMKeyLog.d(TAG, "seService." + (
                        (this.seService == null) ? "null" : Boolean.valueOf(this.seService.isConnected())));
                isOpen = false;
                omaConTimeOut();
                return;
            }
        } catch (Exception e) {
            TMKeyLog.e(TAG, "openChannel Exception--->" + e.getMessage());
            e.printStackTrace();
            isOpen = false;
            omaConTimeOut();
        }
    }

    @Override
    public SIMHelper initSimHelper() {
        TMKeyLog.d(TAG, "initSimHelper");
        return this;
    }

    /**
     * 关闭通道
     *
     * @return 是否关闭成功, true 关闭成功; false 关闭失败
     */
    @Override
    public boolean close() {
        TMKeyLog.d(TAG, "close");
        try {
            isOpen = false;
            if (this.channel != null) {
                TMKeyLog.d(TAG, "close>>>channel.close");
                this.channel.close();
            }
            if (session != null) {
                TMKeyLog.d(TAG, "close>>>session.close");
                session.close();
            }
            if (seService != null) {
                TMKeyLog.d(TAG, "close>>>seService is not null");
                if (seService.isConnected()) {
                    TMKeyLog.d(TAG, "close>>>seService.isConnected");
                    seService.shutdown();
                }
                seService = null;
            }
        } catch (Exception e) {
            TMKeyLog.d(TAG, "close Exception:" + e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> receiveDataList(){
        List<String> list = new ArrayList<>();
        list.add(receiveData());
        return list;
    }

    @Override
    public String receiveData() {
        TMKeyLog.i(TAG, "receiveStr :==> " + receiveStr);
        return receiveStr;
    }

    /**
     * 发送数据
     *
     * @param hexStr
     * @return
     * @throws IOException
     */
    @Override
    public boolean transmitHexData(String hexStr) throws IOException {
        if (!isOpen) {
            return false;
        }
        byte[] transRes = channel.transmit(FCharUtils.hexString2ByteArray(hexStr));
        if (transRes == null) {
            return false;
        }
        receiveStr = FCharUtils.showResult16Str(transRes);
        TMKeyLog.d(TAG, "receiveStr:" + receiveStr);
        if (receiveStr != null && ! "".equals(receiveStr)) {
            int recLen = receiveStr.length();
            if (recLen >= 4) {
                String sw12 = receiveStr.substring(recLen - 4);
                if (sw12.equals("9000") || sw12.contains("91")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * OMA连接失败
     */
    private void omaConTimeOut () {
        TMKeyLog.d(TAG, "omaConTimeOut");
        if (mCardConCallBack != null) {
            mCardConCallBack.OmaDefaultOpenState(false, "OMA通道连接失败");
        }
    }

    /**
     * OMA连接成功
     */
    private void omaConSuccess () {
        if (mCardConCallBack != null) {
            mCardConCallBack.OmaDefaultOpenState(true, "OMA通道连接成功");
        }
    }

    @Override
    public ContentValues getContentValues(String hexStr){
        return null;
    }

    @Override
    public boolean insetContentValues(List<ContentValues> list){
        return true;
    }

    public class SeCallBack implements SEService.OnConnectedListener {
        public SeCallBack() {
        }

        @Override
        public void onConnected() {
            TMKeyLog.d(TAG, "serviceConnected>>>currentThread:" + Thread.currentThread().getId());
            openChannel();
        }
    }

    @Override
    public boolean isSupport() {
        return false;
    }

}
