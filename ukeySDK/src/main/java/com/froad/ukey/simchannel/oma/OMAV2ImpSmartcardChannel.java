package com.froad.ukey.simchannel.oma;

import com.froad.ukey.simchannel.ref.RefProxy;

import org.simalliance.openmobileapi.service.ISmartcardServiceChannel;
import org.simalliance.openmobileapi.service.SmartcardError;

/**
 * Created by FW on 2018/1/5.
 */

public class OMAV2ImpSmartcardChannel extends RefProxy
{
    private Imp mAImp = null;

    public Imp getImp() {
        return this.mAImp;
    }

    public OMAV2ImpSmartcardChannel(ISmartcardServiceChannel obj) {
        this.mAImp = ((Imp)init(Imp.class, obj.getClass(), obj));
    }

    public interface Imp
    {
        boolean isClosed();

        byte[] transmit(byte[] paramArrayOfByte, SmartcardError paramSmartcardError);

        void close(SmartcardError paramSmartcardError);
    }
}