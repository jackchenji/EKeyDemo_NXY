package com.froad.ukey.simchannel.imp;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;

import com.froad.ukey.interf.CardConCallBack;
import com.froad.ukey.simchannel.SIMHelper;
import com.froad.ukey.simchannel.ref.RefUtil;
import com.froad.ukey.simchannel.uicc.IccOpenLogicalChannelResponseImp;
import com.froad.ukey.simchannel.uicc.TelephonyManagerImp;
import com.froad.ukey.utils.LogToFile;
import com.froad.ukey.utils.np.TMKeyLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by FW on 2017/12/27.
 */

public class UICCHelper extends SIMHelper {

    private final static String TAG = "UICCHelper";
    private Context mContext;
    private CardConCallBack mCcardConCallBack;
    private TelephonyManagerImp mTelephonyManagerImp;
    private int mChannel = -1;
    private String recData = "";

    public UICCHelper () {}

    public UICCHelper (Context con, CardConCallBack cardConCallBack) {
        this.mContext = con;
        this.mCcardConCallBack = cardConCallBack;
    }

    @Override
    public SIMHelper initSimHelper() {
        return this;
    }

    @Override
    public boolean open() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            TMKeyLog.d(TAG, "open>>>SDK_INT < 21");
            return false;
        }
        if (!isSupport()) {
            return false;
        }
        try
        {
            TMKeyLog.d(TAG, "open>>>isSupport");
            this.mTelephonyManagerImp = new TelephonyManagerImp(this.mContext, RefUtil.tryClass("android.telephony.TelephonyManager"));
            if (mTelephonyManagerImp == null) {
                TMKeyLog.d(TAG, "mTelephonyManagerImp is null--->false");
                return false;
            }
            if (!mTelephonyManagerImp.hasIccCard()) {
                TMKeyLog.d(TAG, "mTelephonyManagerImp.hasIccCard--->false");
                return false;
            }
            TMKeyLog.d(TAG, "mTelephonyManagerImp.hasIccCard--->true");
            Object object = this.mTelephonyManagerImp.iccOpenLogicalChannel(AID);
            if (object == null) {
                TMKeyLog.d(TAG, "iccOpenLogicalChannel failed, object == null");
                return false;
            }
            TMKeyLog.d(TAG, "iccOpenLogicalChannel success, object is not null");
            IccOpenLogicalChannelResponseImp channelResponseImp = new IccOpenLogicalChannelResponseImp(this.mContext, object);
            int channelStatus = channelResponseImp.getImp().getStatus();
            TMKeyLog.d(TAG, "channelStatus:" + channelStatus);
            if (channelStatus != IccOpenLogicalChannelResponseImp.STATUS_NO_ERROR) {
                TMKeyLog.d(TAG, "channel status != STATUS_NO_ERROR");
                close();
                return false;
            }
            this.mChannel = channelResponseImp.getImp().getChannel();
            TMKeyLog.d(TAG, "mChannel:" + mChannel);

            close();
            return true;
        } catch (Exception e) {
            TMKeyLog.e(TAG, "init Exception>>>getMessage:" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean close() {
        try {
            TMKeyLog.d(TAG, "close>>>mTelephonyManagerImp:" + mTelephonyManagerImp + ">>>mChannel:" + mChannel);
            if ((this.mTelephonyManagerImp != null) && (this.mChannel != -1)) {
                this.mTelephonyManagerImp.iccCloseLogicalChannel(this.mChannel);
                this.mChannel = -1;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean transmitHexData(String hexStr) throws IOException {
        TMKeyLog.d(TAG, "transmitHexData>>>hexStr:" + hexStr);
        if (this.mTelephonyManagerImp == null) {
            TMKeyLog.d(TAG,"mTelephonyManagerImp == null");
            return false;
        }
        if (hexStr == null) {
            TMKeyLog.d(TAG,"hexStr == null");
            return false;
        }
        try
        {
            this.recData = "";
            TMKeyLog.d(TAG, "transmitHexData>>>mChannel:" + mChannel);
            if (this.mChannel == -1) {
                Object object = this.mTelephonyManagerImp.iccOpenLogicalChannel(AID);
                if (object == null) {
                    TMKeyLog.d(TAG, "channel open error.");
                    return false;
                }
                IccOpenLogicalChannelResponseImp channelResponseImp = new IccOpenLogicalChannelResponseImp(this.mContext, object);
                if (channelResponseImp.getImp().getStatus() != IccOpenLogicalChannelResponseImp.STATUS_NO_ERROR) {
                    TMKeyLog.d(TAG,"channel statu != STATUS_NO_ERROR");
                    close();
                    return false;
                }
                this.mChannel = channelResponseImp.getImp().getChannel();
            }
            TMKeyLog.d(TAG, "transmitHexData>>>iccOpenLogicalChannel>>>mChannel:" + mChannel);
            int sLen = hexStr.length();
            if (sLen < 10) {
                TMKeyLog.d(TAG, "数据信息有误");
                return false;
            }
            int CLA = Integer.parseInt(hexStr.substring(0,2), 16);
            int INS = Integer.parseInt(hexStr.substring(2,4), 16);
            int P1 = Integer.parseInt(hexStr.substring(4,6), 16);
            int P2 = Integer.parseInt(hexStr.substring(6,8), 16);
            int P3 = Integer.parseInt(hexStr.substring(8,10), 16);
            String ret = "";
            if (INS == 0xDC) {//发送数据指令
                String st = null;
                if (sLen > 10) {
                    st = hexStr.substring(10);
                }
                ret = this.mTelephonyManagerImp.iccTransmitApduLogicalChannel(this.mChannel, CLA, INS, P1, 0x42, P3, st);
                LogToFile.d(TAG, "receiveData:" + ret);
                TMKeyLog.d(TAG, "iccTransmitApduLogicalChannel-sendData-ret:" + ret);
            } else if (INS == 0xB2) {//读取数据指令
                ret = this.mTelephonyManagerImp.iccTransmitApduLogicalChannel(this.mChannel, CLA, INS, P1, P2, P3, null);
                TMKeyLog.d(TAG, "iccTransmitApduLogicalChannel-receiveData-ret:" + ret);
            }
            if (ret != null && !"".equals(ret)) {
                this.recData = ret.toUpperCase();
                close();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
        return false;
    }

    @Override
    public String receiveData() {
        return recData;
    }

    @Override
    public List<String> receiveDataList() {
        List<String> list = new ArrayList<>();
        list.add(receiveData());
        return list;
    }

    @Override
    public ContentValues getContentValues(String hexStr) {
        return null;
    }

    @Override
    public boolean insetContentValues(List<ContentValues> list) throws Exception {
        return false;
    }

    public boolean isSupport () {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            TMKeyLog.d(TAG, "UICC Build.VERSION.SDK_INT  < 21");
            return false;
        }
        Class clas = RefUtil.tryClass("android.telephony.TelephonyManager");
        if (clas == null) {
            TMKeyLog.d(TAG, "UICC TelephonyManager not fount.");
            return false;
        }

        if (!RefUtil.classHassMethod(clas, "iccOpenLogicalChannel")) {
            TMKeyLog.d(TAG, "UICC iccOpenLogicalChannel not found.");
            return false;
        }
        if (!RefUtil.classHassMethod(clas, "iccCloseLogicalChannel")) {
            TMKeyLog.d(TAG, "UICC iccCloseLogicalChannel not found.");
            return false;
        }
        return true;
    }
}
