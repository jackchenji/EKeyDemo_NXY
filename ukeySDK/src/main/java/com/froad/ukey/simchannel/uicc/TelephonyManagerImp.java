package com.froad.ukey.simchannel.uicc;

import android.content.Context;

import com.froad.ukey.simchannel.ref.RefProxy;
import com.froad.ukey.simchannel.ref.RefUtil;
import com.froad.ukey.utils.np.TMKeyLog;

import java.lang.reflect.Method;

/**
 * Created by FW on 2017/12/28.
 */

public class TelephonyManagerImp extends RefProxy {
    private final static String TAG = "TelephonyManagerImp";
    private Imp mAImp = null;

    private int mSim1ID = -1;
    private int paramsType = 1;//????

    public Imp getImp()
    {
        return this.mAImp;
    }

    public TelephonyManagerImp(Context context, Class<?> cls) {
        this.mAImp = ((Imp)init(Imp.class, cls, null));
        this.mIns = this.mAImp.getDefault();

//        if (RefUtil.classHassMethod(cls, "getDefaultSim")) {
//            TMKeyLog.d(TAG, "RefUtil.classHassMethod(cls, \"getDefaultSim\")");
            Method method = RefUtil.classGetMethod(cls, "iccOpenLogicalChannel");
            Class[] params = method.getParameterTypes();
            if (params == null) {
                TMKeyLog.d(TAG, "params is null");
            } else {
                TMKeyLog.d(TAG, "iccOpenLogicalChannel params.length:" + params.length);
                if (params.length == 3) {//????OPPOR15???????,openchannel?3???
                    TMKeyLog.d(TAG, "iccOpenLogicalChannel params.length:" + params.length + ">>>0:" + params[0].getName() + ">>>1:" + params[1].getName() + ">>>2:" + params[2].getName());
                    if ((Integer.TYPE.equals(params[0])) && (String.class.equals(params[1])) && (Integer.TYPE.equals(params[2]))) {
                        paramsType = 3;
                        this.mSim1ID = 0;
                    } else {
                        TMKeyLog.d(TAG, "iccOpenLogicalChannel params.length:" + params.length + ">>>params type error");
                    }
                } else if (params.length == 2) {
                    TMKeyLog.d(TAG, "iccOpenLogicalChannel params.length:" + params.length + ">>>0:" + params[0].getName() + ">>>1:" + params[1].getName());
                    if ((Integer.TYPE.equals(params[0])) && (String.class.equals(params[1]))) {
                        paramsType = 2;
                        this.mSim1ID = 0;
                    } else {
                        TMKeyLog.d(TAG, "iccOpenLogicalChannel params.length:" + params.length + ">>>params type error");
                    }
                }
            }
//        } else {
//            TMKeyLog.d(TAG, "RefUtil.classHassMethod(cls, \"getDefaultSim\") is null");
//        }
        TMKeyLog.d(TAG, "mSim1ID:" + mSim1ID);
    }

    public Object iccOpenLogicalChannel(String paramString)
    {
        if (this.mSim1ID != -1) {
            if (paramsType == 3) {
                return this.mAImp.iccOpenLogicalChannel(this.mSim1ID, paramString, this.mSim1ID);
            } else if (paramsType == 2) {
                return this.mAImp.iccOpenLogicalChannel(this.mSim1ID, paramString);
            }
        }
        return this.mAImp.iccOpenLogicalChannel(paramString);
    }

    public boolean hasIccCard()
    {
        if (this.mSim1ID != -1) {
            return this.mAImp.hasIccCard(this.mSim1ID);
        }
        return this.mAImp.hasIccCard();
    }

    public boolean iccCloseLogicalChannel(int paramInt1)
    {
        if (this.mSim1ID != -1) {
            return this.mAImp.iccCloseLogicalChannel(this.mSim1ID, paramInt1);
        }
        return this.mAImp.iccCloseLogicalChannel(paramInt1);
    }

    public String iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data)
    {
        if (this.mSim1ID != -1) {
            return this.mAImp.iccTransmitApduLogicalChannel(this.mSim1ID, channel, cla, instruction, p1, p2, p3, data);
        }
        return this.mAImp.iccTransmitApduLogicalChannel(channel, cla, instruction, p1, p2, p3, data);
    }

    public int getmSim1ID()
    {
        return this.mSim1ID;
    }

    public interface Imp
    {
        Object getDefault();

        String getDeviceId();

        boolean hasCarrierPrivileges();

        Object iccOpenLogicalChannel(String paramString);

        Object iccOpenLogicalChannel(int paramInt, String paramString);

        Object iccOpenLogicalChannel(int paramInt, String paramString, int p1);

        boolean hasIccCard();

        boolean hasIccCard(int paramInt);

        boolean iccCloseLogicalChannel(int paramInt);

        boolean iccCloseLogicalChannel(int paramInt1, int paramInt2);

        String iccTransmitApduLogicalChannel(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, String paramString);

        String iccTransmitApduLogicalChannel(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, int paramInt7, String paramString);

        int checkCarrierPrivilegesForPackage(String paramString);
    }
}
