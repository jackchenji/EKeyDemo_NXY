package com.froad.ukey.simchannel.uicc;

import android.content.Context;

import com.froad.ukey.simchannel.ref.RefProxy;

/**
 * Created by FW on 2017/12/28.
 */

public class IccOpenLogicalChannelResponseImp extends RefProxy
{
    public static final int STATUS_MISSING_RESOURCE = 2;//No logical channels available
    public static final int STATUS_NO_ERROR = 1;//Open channel command returned successfully.
    public static final int STATUS_NO_SUCH_ELEMENT = 3;//AID not found on UICC.
    public static final int STATUS_UNKNOWN_ERROR = 4;//Unknown error in open channel command
    private Imp mAImp = null;

    public Imp getImp() {
        return this.mAImp;
    }

    public IccOpenLogicalChannelResponseImp(Context context, Object object) {
        this.mAImp = ((Imp)init(Imp.class, object.getClass(), object));
    }

    public interface Imp
    {
        int getChannel();

        int getStatus();
    }}
