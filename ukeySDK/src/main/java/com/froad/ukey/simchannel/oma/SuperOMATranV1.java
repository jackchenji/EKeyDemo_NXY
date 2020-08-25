package com.froad.ukey.simchannel.oma;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.utils.np.TMKeyLog;

import org.simalliance.openmobileapi.service.ISmartcardService;
import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.SmartcardError;

/**
 * Created by FW on 2018/1/5.
 */

public class SuperOMATranV1 implements SuperOMATranInterface {
    private static String TAG = FConstant.LOG_TAG  + "SuperOMATranV1";
    private ISmartcardService mSmartcardService = null;
    private ISmartcardServiceCallback mCallback = null;

    private long mChannelID = -1L;
    private OMAV1ImpSmartcardService mSmartcardImpService;

    public SuperOMATranV1(ISmartcardService smartcardService, ISmartcardServiceCallback callback)
    {
        TMKeyLog.d(TAG, "SuperOMATranV1");
        this.mSmartcardService = smartcardService;
        this.mCallback = callback;
    }

    public boolean init(byte[] aid) {
        try {
            SmartcardError error = new SmartcardError();
            this.mSmartcardImpService = new OMAV1ImpSmartcardService(this.mSmartcardService);
            String[] readers = this.mSmartcardImpService.getImp().getReaders(error);
            SuperOMA.checkForException(error);
            if ((readers == null) || (readers.length == 0)) {
                TMKeyLog.d(TAG, "SuperOMA not fount readers");
                return false;
            }
            TMKeyLog.d(TAG, "SuperOMATranV1 reader:" + readers[0]);

            this.mChannelID = this.mSmartcardImpService.getImp().openLogicalChannel(readers[0], aid, this.mCallback, error);
            TMKeyLog.d(TAG,"SuperOMATranV1 mChannelID:" + this.mChannelID);
            SuperOMA.checkForException(error);
            if (this.mChannelID == -1L) {
                TMKeyLog.d(TAG,"mChannelID == -1");
                return false;
            }
            TMKeyLog.d(TAG,"SuperOMATranV1 openLogicalChannel success");
            return true;
        } catch (Exception e) {
            TMKeyLog.d(TAG,"Exception:" + e.getMessage());
        }
        return false;
    }

    public byte[] transmit(byte[] data) {
        try {
            if ((this.mSmartcardService == null) || (this.mSmartcardImpService == null)) {
                TMKeyLog.d(TAG, "SuperOMA mSmartcardService == null");
                return null;
            }

            if (this.mChannelID == -1L) {
                TMKeyLog.d(TAG, "SuperOMA open first,pls!!!");
                return null;
            }

            SmartcardError error = new SmartcardError();
            byte[] ret = this.mSmartcardImpService.getImp().transmit(this.mChannelID, data, error);
            SuperOMA.checkForException(error);
            if (ret == null) {
                TMKeyLog.d(TAG, "SuperOMA transmit error!");
                return null;
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            TMKeyLog.d(TAG,"SuperOMATranV1" + e.getMessage());
        }
        return null;
    }

    public void close() {
        if ((this.mSmartcardService == null) || (this.mChannelID == -1L) || (this.mSmartcardImpService == null)) return;
        try {
            SmartcardError error = new SmartcardError();
            this.mSmartcardImpService.getImp().closeChannel(this.mChannelID, error);
            SuperOMA.checkForException(error);
        } catch (Exception e) {
            e.printStackTrace();
            TMKeyLog.d(TAG, "Exception:" + e.getMessage());
        }
    }
}
