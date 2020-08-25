package com.cn.froad.ekeydemo_nxy.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.cn.froad.ekeydemo_nxy.app.MyApplication;
import com.froad.eid.ecard.service.CardService;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class NfcManager {

    private  CardEmulation cardEmulation;
    private NfcAdapter nfcAdapter;
    private ComponentName mComponentName;
    private static class SingletonHolder{
        private static final NfcManager INSTANCE = new NfcManager();
    }


    private NfcManager()
    {
        nfcAdapter = NfcAdapter.getDefaultAdapter(MyApplication.getInstance());
        if (nfcAdapter != null){
            cardEmulation = CardEmulation.getInstance(nfcAdapter);
        }
        mComponentName = new ComponentName(MyApplication.getInstance(), CardService.class);
    }

    //获取单例
    public static NfcManager getInstance(){
        return NfcManager.SingletonHolder.INSTANCE;
    }

    public boolean isSupportNfc(){
        return nfcAdapter != null;
    }

    public boolean isEnabled(){
        if (!isSupportNfc()){
            return false;
        }
        return nfcAdapter.isEnabled();
    }

    public void startNfcRead(Activity activity,PendingIntent pi){
        nfcAdapter.enableForegroundDispatch(activity, pi, null, null);
    }

    public void stopNfcRead(Activity activity){
        nfcAdapter.disableForegroundDispatch(activity);
    }

    public boolean isDefaultServiceForCategory(){
        if (cardEmulation!=null){
           return cardEmulation.isDefaultServiceForCategory(mComponentName,CardEmulation.CATEGORY_PAYMENT);
        }
        return false;
    }


}
