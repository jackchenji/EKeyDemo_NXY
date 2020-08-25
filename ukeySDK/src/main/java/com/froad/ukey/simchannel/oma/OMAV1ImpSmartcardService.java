package com.froad.ukey.simchannel.oma;

import android.os.RemoteException;

import com.froad.ukey.simchannel.ref.RefProxy;

import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.SmartcardError;

/**
 * Created by FW on 2018/1/5.
 */

public class OMAV1ImpSmartcardService extends RefProxy
{
    private SmartcardServiceImp mAImp = null;

    public SmartcardServiceImp getImp() {
        return this.mAImp;
    }

    public OMAV1ImpSmartcardService(Object ins) {
        this.mAImp = ((SmartcardServiceImp)init(SmartcardServiceImp.class, ins.getClass(), ins));
    }

    public interface SmartcardServiceImp
    {
        String[] getReaders(SmartcardError paramSmartcardError)
                throws RemoteException;

        long openLogicalChannel(String paramString, byte[] paramArrayOfByte, ISmartcardServiceCallback paramISmartcardServiceCallback, SmartcardError paramSmartcardError)
                throws RemoteException;

        long openBasicChannelAid(String paramString, byte[] paramArrayOfByte, ISmartcardServiceCallback paramISmartcardServiceCallback, SmartcardError paramSmartcardError)
                throws RemoteException;

        byte[] transmit(long paramLong, byte[] paramArrayOfByte, SmartcardError paramSmartcardError)
                throws RemoteException;

        void closeChannel(long paramLong, SmartcardError paramSmartcardError)
                throws RemoteException;

        void initSEAccessControl(String paramString)
                throws RemoteException;
    }
}
