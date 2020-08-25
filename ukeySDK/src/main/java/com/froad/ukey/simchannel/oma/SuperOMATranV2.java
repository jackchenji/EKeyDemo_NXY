package com.froad.ukey.simchannel.oma;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.utils.np.TMKeyLog;

import org.simalliance.openmobileapi.service.ISmartcardService;
import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.ISmartcardServiceChannel;
import org.simalliance.openmobileapi.service.ISmartcardServiceReader;
import org.simalliance.openmobileapi.service.ISmartcardServiceSession;
import org.simalliance.openmobileapi.service.SmartcardError;

/**
 * Created by FW on 2018/1/5.
 */

public class SuperOMATranV2 implements SuperOMATranInterface
{
    private static String TAG = FConstant.LOG_TAG  + "SuperOMATranV2";
    private ISmartcardService mSmartcardService = null;
    private ISmartcardServiceCallback mCallback = null;

    private OMAV2ImpSmartcardChannel mChannel = null;

    public SuperOMATranV2(ISmartcardService smartcardService, ISmartcardServiceCallback callback) {
        TMKeyLog.d(TAG, "SuperOMATranV2");
        this.mSmartcardService = smartcardService;
        this.mCallback = callback;
    }

    public boolean init(byte[] aid) {
        try {
            SmartcardError error = new SmartcardError();

            OMAV2ImpSmartcardService omav2ImpSmartcardService = new OMAV2ImpSmartcardService(this.mSmartcardService);
            String[] readers = omav2ImpSmartcardService.getReaders(error);
            SuperOMA.checkForException(error);
            if ((readers == null) || (readers.length == 0)) {
                TMKeyLog.d(TAG, "SuperOMA not fount readers");
                return false;
            }
            for (String string : readers) {
                TMKeyLog.d(TAG, "reader:" + string);
            }
            ISmartcardServiceReader smartcardServiceReader = omav2ImpSmartcardService.getReader(readers[0], error);

            SuperOMA.checkForException(error);
            if (smartcardServiceReader == null) {
                TMKeyLog.d(TAG, "SuperOMA smartcardServiceReader == null");
                return false;
            }
            ISmartcardServiceSession smartcardServiceSession = smartcardServiceReader.openSession(error);
            SuperOMA.checkForException(error);
            if (smartcardServiceSession == null) {
                TMKeyLog.d(TAG,"SuperOMA smartcardServiceSession == null");
                return false;
            }

            OMAV2ImpServiceSession omav2ImpServiceSession = new OMAV2ImpServiceSession(smartcardServiceSession);
            ISmartcardServiceChannel channel = omav2ImpServiceSession.openLogicalChannel(aid, this.mCallback, error);
            SuperOMA.checkForException(error);
            if (channel == null) {
                TMKeyLog.d(TAG,"SuperOMA channel == null");
                return false;
            }
            this.mChannel = new OMAV2ImpSmartcardChannel(channel);
            if ((this.mChannel == null) || (this.mChannel.getImp().isClosed())) {
                TMKeyLog.d(TAG,"SuperOMA mChannel == null || mChannel.getImp().isClosed()");
                return false;
            }
            return true;
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
        }
        return false;
    }

    public byte[] transmit(byte[] data) {
        try {
            if (this.mSmartcardService == null) {
                TMKeyLog.d(TAG , "SuperOMA mSmartcardService == null");
                return null;
            }

            if (this.mChannel == null) {
                TMKeyLog.d(TAG, "SuperOMA open first,pls!!!");
                return null;
            }

            SmartcardError error = new SmartcardError();
            byte[] ret = this.mChannel.getImp().transmit(data, error);
            SuperOMA.checkForException(error);
            if (ret == null) {
                TMKeyLog.d(TAG,"SuperOMA transmit error!");
                return null;
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        if ((this.mSmartcardService == null) || (this.mChannel == null)) return;
        try {
            SmartcardError error = new SmartcardError();
            this.mChannel.getImp().close(error);
            SuperOMA.checkForException(error);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
