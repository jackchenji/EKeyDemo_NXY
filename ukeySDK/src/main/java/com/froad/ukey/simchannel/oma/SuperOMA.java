package com.froad.ukey.simchannel.oma;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.simchannel.ref.RefUtil;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;

import org.simalliance.openmobileapi.service.ISmartcardService;
import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.SmartcardError;

/**
 * Created by FW on 2018/1/5.
 */

public class SuperOMA {
    private static String TAG = FConstant.LOG_TAG  + "SuperOMA";
    private Context mContext;
    private CallBack mLocalCallBack = null;
    private ISmartcardService mSmartcardService = null;
    private ISmartcardServiceCallback mCallback = null;

    private ServiceConnection mServiceConnection = null;

    private SuperOMATranInterface mOMATran = null;

    public SuperOMA(Context context, CallBack callBack)
    {
        this.mContext = context;
        this.mLocalCallBack = callBack;
        try {
            this.mCallback = new ISmartcardServiceCallback.Stub() {
            };
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setService(ISmartcardService service) {
        TMKeyLog.d(TAG, "setService");
        this.mSmartcardService = service;

        if (RefUtil.classHassMethod(this.mSmartcardService.getClass(), "transmit"))
            this.mOMATran = new SuperOMATranV1(this.mSmartcardService, this.mCallback);
        else {
            this.mOMATran = new SuperOMATranV2(this.mSmartcardService, this.mCallback);
        }
        if (this.mLocalCallBack != null)
            this.mLocalCallBack.serviceConnected(this, this.mSmartcardService);
    }

    public boolean connect() {
        Intent serviceIntent = new Intent(ISmartcardService.class.getName());
        TMKeyLog.d(TAG, "targetSDK:" + this.mContext.getApplicationInfo().targetSdkVersion);
        if (this.mContext.getApplicationInfo().targetSdkVersion >= 21) {
            serviceIntent.setClassName("org.simalliance.openmobileapi.service", "org.simalliance.openmobileapi.service.SmartcardService");
        }

        this.mServiceConnection = new ServiceConnection()
        {
            public void onServiceDisconnected(ComponentName arg0)
            {
                TMKeyLog.d(TAG, "onServiceDisconnected");
            }

            public void onServiceConnected(ComponentName arg0, IBinder service)
            {
                TMKeyLog.d(TAG, "onServiceConnected");
                SuperOMA.this.setService(ISmartcardService.Stub.asInterface(service));
            }
        };
        boolean bindingSuccessful = false;
        try {
            bindingSuccessful = this.mContext.bindService(serviceIntent, this.mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        catch (Exception localException) {
        }
        TMKeyLog.d(TAG,"bindingSuccessful : " + bindingSuccessful);
        return bindingSuccessful;
    }

    public boolean isConnect()
    {
        return this.mSmartcardService != null;
    }

    public boolean select(byte[] aid)
    {
        TMKeyLog.d(TAG, "select>>>aid:" + FCharUtils.showResult16Str(aid));
        if (this.mOMATran != null) {
            TMKeyLog.d(TAG, "mOMATran is not null");
            return this.mOMATran.init(aid);
        }
        return false;
    }

    public byte[] transmit(byte[] data) {
        if (this.mOMATran != null) {
            return this.mOMATran.transmit(data);
        }
        return null;
    }

    public void close()
    {
        if (this.mOMATran != null) {
            this.mOMATran.close();
        }
        if ((this.mServiceConnection != null) && (this.mContext != null))
            this.mContext.unbindService(this.mServiceConnection);
    }

    public static void checkForException(SmartcardError error) throws Exception
    {
        try {
            error.throwException();
        } catch (Exception exp) {
            throw exp;
        }
    }

    public interface CallBack
    {
        void serviceConnected(SuperOMA paramSuperOMA, ISmartcardService paramISmartcardService);
    }
}
