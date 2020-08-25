package com.froad.ukey.simchannel.imp;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.interf.CardConCallBack;
import com.froad.ukey.jni.tmjni;
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.simchannel.ref.RefUtil;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;

import org.simalliance.openmobileapi.Channel;
import org.simalliance.openmobileapi.Reader;
import org.simalliance.openmobileapi.SEService;
import org.simalliance.openmobileapi.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by FW on 2018/1/5.
 */

public class SESDefaultHelper extends SIMHelper {

    private static final String TAG = FConstant.LOG_TAG + "SESDefaultHelper";
    public final static long CONNECT_TIME_OUT = 2000;//连接超时时间
    public static byte[] selectResponse = null;

    private String receiveStr = null;//接收的字符串
    private SEService seService;

    private Channel channel = null;
    private Context mContext;
    private CardConCallBack mCardConCallBack;
    private Session session;
    private int initSeCount = 0;

    public SESDefaultHelper(){}

    public SESDefaultHelper(Context con, CardConCallBack cardConCallBack){

        try {
            TMKeyLog.i(TAG, "creating SEService object...");
            mContext = con;
            initSeCount = 0;
            this.mCardConCallBack = cardConCallBack;
            isOpen = false;
            TMKeyLog.d(TAG, "当前线程ID：" + Thread.currentThread().getId());
            initSeCount ++;
            seService = new SEService(mContext, new SeCallBack());
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
            } else if (! seService.isConnected()) {
                TMKeyLog.d(TAG, "openChannel>>>seService is not connected");
            }
            if ((this.seService != null) && (this.seService.isConnected())) {
                TMKeyLog.d(TAG, "openChannel getReaders start");
                Reader[] readers = this.seService.getReaders();
                TMKeyLog.e(TAG, "readers.length：" + readers.length);
                if (readers.length < 1) {
                    TMKeyLog.e(TAG, "readers.length < 1");
                    this.isOpen = false;
                    omaConTimeOut();
                    return;
                }
//                session = null;
                if (readers[0] != null) {
                    TMKeyLog.e(TAG, "readers[0] != null");
                    for (int i = 0; i < readers.length; i++) {
                        TMKeyLog.e(TAG, "readers[" + i + "].Name--->" + readers[i].getName());
                    }
                    //5.1以下系统不能使用UICC
                    int sysVer = Build.VERSION.SDK_INT;
                    TMKeyLog.e(TAG, "sysVer:" + sysVer);
                    if (sysVer < Build.VERSION_CODES.LOLLIPOP_MR1 && readers[0].getName().contains("UICC")) {
                        TMKeyLog.e(TAG, "readers[0].getName().contains(\"UICC\")");
                        isOpen = false;
                        omaConTimeOut();
                        return;
                    }
                    try {
                        session = readers[0].openSession();
                        TMKeyLog.e(TAG, "openSession success");
                    } catch (Exception e) {
                        TMKeyLog.e(TAG, "openSession exception--->" + e.getMessage());
                        e.printStackTrace();
                        isOpen = false;
                        omaConTimeOut();
                        return;
                    }
                }
                if (session != null) {
                    TMKeyLog.e(TAG, "session is not null");
                    selectResponse = null;
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
                        if (channel.isClosed()) {
                            TMKeyLog.d(TAG, "channel.isClosed()");
                            isOpen = false;
                            omaConTimeOut();
                            return;
                        }
                        TMKeyLog.d(TAG, "channel.is not Closed");
                        selectResponse = channel.getSelectResponse();
                        if (selectResponse != null) {
                            TMKeyLog.d(TAG, "channel.getSelectResponse:" + FCharUtils.bytesToHexStr(selectResponse));
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
                }
                this.isOpen = true;
                omaConSuccess();
                return;
            } else {
                TMKeyLog.d(TAG, "seService." + (
                        (this.seService == null) ? "null" : Boolean.valueOf(this.seService.isConnected())));
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
        try
        {
            isOpen = false;
            if ((this.seService == null) || (!this.seService.isConnected())) {
                initSeCount ++;
                this.seService = new SEService(mContext, new SeCallBack());
                boolean setVarRes = RefUtil.classSetVar(this.seService.getClass(), "mSmartcardService", this.seService, omaServie);
                TMKeyLog.d(TAG, "setVarRes:" + setVarRes);
            }
        } catch (Exception e) {
            TMKeyLog.d(TAG, "OMA : " + e.getMessage());
            e.printStackTrace();
            return this;
        }
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
                this.channel.close();
            }
            if (seService != null && seService.isConnected()) {
                seService.shutdown();
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
        receiveStr = tmjni.transHexOma(hexStr, SESDefaultHelper.this);
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

    public class SeCallBack implements SEService.CallBack {
        public SeCallBack () {
        }

        @Override
        public void serviceConnected(SEService arg0) {
            TMKeyLog.d(TAG, "serviceConnected");
            omaServie = RefUtil.classGetVar(SEService.class, "mSmartcardService", arg0);
            seService = arg0;
            openChannel();
        }
    }

    @Override
    public boolean isSupport() {
        return false;
    }

    public void deleteInitSeCount () {
        initSeCount -- ;
    }

    public int getInitSeCount() {
        return initSeCount;
    }
}
