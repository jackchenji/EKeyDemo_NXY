package com.froad.ukey.simchannel.oma;

import android.os.RemoteException;

import com.froad.ukey.simchannel.ref.RefProxy;

import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.ISmartcardServiceChannel;
import org.simalliance.openmobileapi.service.ISmartcardServiceSession;
import org.simalliance.openmobileapi.service.SmartcardError;

import java.lang.reflect.Method;

/**
 * Created by FW on 2018/1/5.
 */

public class OMAV2ImpServiceSession extends RefProxy
{
    private Imp mAImp = null;

    public Imp getImp() {
        return this.mAImp;
    }

    public OMAV2ImpServiceSession(ISmartcardServiceSession obj) {
        this.mAImp = ((Imp)init(Imp.class, obj.getClass(), obj));
    }

    public ISmartcardServiceChannel openLogicalChannel(byte[] aid, ISmartcardServiceCallback cb, SmartcardError err) throws RemoteException {
        int callType = 0;
        try {
            Class cls = this.mIns.getClass();
            Method[] allMethod = cls.getDeclaredMethods();
            for (Method method : allMethod) {
                if ((!method.getName().equals("openLogicalChannel")) ||
                        (method.getParameterTypes() == null) || (method.getParameterTypes().length == 0)) continue;
                Class[] param = method.getParameterTypes();
                if ((param.length == 4) && (param[1] == Byte.TYPE)) {
                    callType = 1;
                }
            }
        }
        catch (Exception localException)
        {
        }
        if (callType == 0) {
            return this.mAImp.openLogicalChannel(aid, cb, err);
        }
        return this.mAImp.openLogicalChannel(aid, (byte)0, cb, err);
    }

    public interface Imp
    {
        ISmartcardServiceChannel openLogicalChannel(byte[] paramArrayOfByte, byte paramByte, ISmartcardServiceCallback paramISmartcardServiceCallback, SmartcardError paramSmartcardError)
                throws RemoteException;

        ISmartcardServiceChannel openLogicalChannel(byte[] paramArrayOfByte, ISmartcardServiceCallback paramISmartcardServiceCallback, SmartcardError paramSmartcardError)
                throws RemoteException;
    }
}
