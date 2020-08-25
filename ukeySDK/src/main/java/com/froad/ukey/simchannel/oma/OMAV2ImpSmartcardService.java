package com.froad.ukey.simchannel.oma;

import android.os.RemoteException;

import com.froad.ukey.simchannel.ref.RefProxy;

import org.simalliance.openmobileapi.service.ISmartcardService;
import org.simalliance.openmobileapi.service.ISmartcardServiceReader;
import org.simalliance.openmobileapi.service.SmartcardError;

import java.lang.reflect.Method;

/**
 * Created by FW on 2018/1/5.
 */

public class OMAV2ImpSmartcardService extends RefProxy
{
    private Imp mAImp = null;

    public Imp getImp() {
        return this.mAImp;
    }

    public OMAV2ImpSmartcardService(ISmartcardService obj) {
        this.mAImp = ((Imp)init(Imp.class, obj.getClass(), obj));
    }

    public ISmartcardServiceReader getReader(String reader, SmartcardError err) throws RemoteException {
        int callType = 0;
        try {
            Class cls = this.mIns.getClass();
            Method[] allMethod = cls.getDeclaredMethods();
            for (Method method : allMethod) {
                if ((!method.getName().equals("getReader2")) || (
                        (method.getParameterTypes() != null) && (method.getParameterTypes().length != 2))) continue;
                callType = 1;
            }
        }
        catch (Exception localException)
        {
        }
        if (callType == 0) {
            return this.mAImp.getReader(reader, err);
        }
        return this.mAImp.getReader2(reader, err);
    }

    public String[] getReaders(SmartcardError err) throws RemoteException
    {
        int callType = 0;
        try {
            Class cls = this.mIns.getClass();
            Method[] allMethod = cls.getDeclaredMethods();
            for (Method method : allMethod) {
                if ((!method.getName().equals("getReaders")) || (
                        (method.getParameterTypes() != null) && (method.getParameterTypes().length != 0))) continue;
                callType = 1;
            }
        }
        catch (Exception localException)
        {
        }
        if (callType == 0) {
            return this.mAImp.getReaders(err);
        }
        return this.mAImp.getReaders();
    }

    public interface Imp
    {
        String[] getReaders()
                throws RemoteException;

        String[] getReaders(SmartcardError paramSmartcardError)
                throws RemoteException;

        ISmartcardServiceReader getReader2(String paramString, SmartcardError paramSmartcardError)
                throws RemoteException;

        ISmartcardServiceReader getReader(String paramString, SmartcardError paramSmartcardError)
                throws RemoteException;
    }
}
