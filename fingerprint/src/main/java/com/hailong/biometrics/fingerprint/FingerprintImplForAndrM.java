package com.hailong.biometrics.fingerprint;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.TMKeyLog;
import com.hailong.biometrics.fingerprint.bean.VerificationDialogStyleBean;
import com.hailong.biometrics.fingerprint.uitls.AndrVersionUtil;
import com.hailong.biometrics.fingerprint.uitls.CipherHelper;

import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Android M == 6.0
 * Created by ZuoHailong on 2019/7/9.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintImplForAndrM implements IFingerprint {

    private final String TAG = FingerprintImplForAndrM.class.getName();
    private Activity context;

    //指纹验证框
    private  FingerprintDialog fingerprintDialog;
    //指向调用者的指纹回调
    private FingerprintCallback fingerprintCallback;

    //用于取消扫描器的扫描动作
    private CancellationSignal cancellationSignal;
    //指纹加密
    private  FingerprintManagerCompat.CryptoObject cryptoObject;
    //Android 6.0 指纹管理
    private FingerprintManagerCompat fingerprintManagerCompat;
    private Signature signature;

    private final int mkeyType;

    @Override
    public void authenticate(Activity context, VerificationDialogStyleBean bean, FingerprintCallback callback) {

        this.context = context;
        this.fingerprintCallback = callback;
        //Android 6.0 指纹管理 实例化
        fingerprintManagerCompat = FingerprintManagerCompat.from(context);

        //取消扫描，每次取消后需要重新创建新示例
        cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                fingerprintDialog.dismiss();
            }
        });

        cryptoObject = new FingerprintManagerCompat.CryptoObject(CipherHelper.getInstance().getSignature(mkeyType));

        //调起指纹验证
        fingerprintManagerCompat.authenticate(cryptoObject, 0, cancellationSignal, authenticationCallback, null);
        //指纹验证框
        fingerprintDialog = FingerprintDialog.newInstance().setActionListener(dialogActionListener).setDialogStyle(bean);
        fingerprintDialog.show(context.getFragmentManager(), TAG);
    }

    @Override
    public byte[] sign(byte [] message) {
        if (signature!=null){

            return getSignBytes(message, signature);
        }else {
            final Signature sm2Signature= CipherHelper.getInstance().getSM2Signature();
            if (sm2Signature!=null){
                TMKeyLog.d(TAG,"sm2Signature!=null");
                final byte[] signBytes = getSignBytes(message, sm2Signature);
                TMKeyLog.d("MainActivity","sm2Signature signBytes : \n"+ FCharUtils.bytesToHexStr(signBytes));
                return SM2Util.dealSm2SignResultC(signBytes);
            }else {

                final byte[] encoded = CipherHelper.getInstance().getPrivateKey().getEncoded();
                final PublicKey publicKey = CipherHelper.getInstance().getPublicKey();
                return SM2Util.sign(encoded,message,true,publicKey.getEncoded(),true);
            }
        }
    }

    private byte[] getSignBytes(byte[] message, Signature sm2Signature) {
        try {
            sm2Signature.update(message);
            byte[] sigBytes = sm2Signature.sign();
            return sigBytes;
        } catch (SignatureException e) {
            return null;
        }
    }

    public static FingerprintImplForAndrM newInstance(int keyType) {

        return new FingerprintImplForAndrM(keyType);
    }

    public FingerprintImplForAndrM(int keyType) {
        //指纹加密，提前进行Cipher初始化，防止指纹认证时还没有初始化完成
        mkeyType = keyType;
        try {
            if (AndrVersionUtil.isAboveAndrM()){
                cryptoObject = new FingerprintManagerCompat.CryptoObject(CipherHelper.getInstance().getSignature(mkeyType));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指纹验证框按键监听
     */
    private FingerprintDialog.OnDialogActionListener dialogActionListener = new FingerprintDialog.OnDialogActionListener() {
        @Override
        public void onUsepwd() {
//            if (fingerprintCallback != null)
//                fingerprintCallback.onUsepwd();
        }

        @Override
        public void onCancle() {//取消指纹验证，通知调用者
            if (fingerprintCallback != null)
                fingerprintCallback.onCancel();
        }

        @Override
        public void onDismiss() {//验证框消失，取消指纹验证
            if (cancellationSignal != null && !cancellationSignal.isCanceled())
                cancellationSignal.cancel();
        }
    };

    /**
     * 指纹验证结果回调
     */
    private FingerprintManagerCompat.AuthenticationCallback authenticationCallback = new FingerprintManagerCompat.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            super.onAuthenticationError(errMsgId, errString);
            if (errMsgId != 5)//用户取消指纹验证
                fingerprintDialog.setTip(errString.toString(), R.color.biometricprompt_color_FF5555);
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            super.onAuthenticationHelp(helpMsgId, helpString);
            fingerprintDialog.setTip(helpString.toString(), R.color.biometricprompt_color_FF5555);
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            if (CipherHelper.getInstance().isHasSetupKeyPair()){
                final FingerprintManagerCompat.CryptoObject resultCryptoObject = result.getCryptoObject();
               if (resultCryptoObject!=null){
                   signature=resultCryptoObject.getSignature();
               }
            }

            fingerprintDialog.setTip(context.getString(R.string.biometricprompt_verify_success), R.color.biometricprompt_color_82C785);
            fingerprintCallback.onSucceeded();
            fingerprintDialog.dismiss();
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            fingerprintDialog.setTip(context.getString(R.string.biometricprompt_verify_failed), R.color.biometricprompt_color_FF5555);
            fingerprintCallback.onFailed();
        }
    };

    /*
     * 在 Android Q，Google 提供了 Api BiometricManager.canAuthenticate() 用来检测指纹识别硬件是否可用及是否添加指纹
     * 不过尚未开放，标记为"Stub"(存根)
     * 所以暂时还是需要使用 Andorid 6.0 的 Api 进行判断
     * */

}
