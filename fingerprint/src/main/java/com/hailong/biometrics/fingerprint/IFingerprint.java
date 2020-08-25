package com.hailong.biometrics.fingerprint;

import android.app.Activity;

import com.hailong.biometrics.fingerprint.bean.VerificationDialogStyleBean;

/**
 * Created by ZuoHailong on 2019/7/9.
 */
public interface IFingerprint {


    /**
     * 检测指纹硬件是否可用，及是否添加指纹
     *
     * @param context
     * @return
     */
//    boolean canAuthenticate(Context context);

    /**
     * 初始化并调起指纹验证
     *
     * @param context
     * @param verificationDialogStyleBean
     * @param callback
     */
    void authenticate(Activity context, VerificationDialogStyleBean verificationDialogStyleBean, FingerprintCallback callback);

    byte[] sign(byte[] message);
}
