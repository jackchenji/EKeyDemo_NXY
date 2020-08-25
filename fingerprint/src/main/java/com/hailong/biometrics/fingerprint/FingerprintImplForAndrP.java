package com.hailong.biometrics.fingerprint;

import android.app.Activity;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.TMKeyLog;
import com.hailong.biometrics.fingerprint.bean.VerificationDialogStyleBean;
import com.hailong.biometrics.fingerprint.uitls.CipherHelper;

import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.concurrent.Executor;

/**
 * Android P == 9.0
 * Created by ZuoHailong on 2019/7/9.
 */
@RequiresApi(api = Build.VERSION_CODES.P)
public class FingerprintImplForAndrP implements IFingerprint {

    private static final String TAG = "FingerprintImplForAndrP";
    private final int mkeyType;
    //指向调用者的指纹回调
    private FingerprintCallback fingerprintCallback;

    //用于取消扫描器的扫描动作
    private CancellationSignal cancellationSignal;
    //指纹加密
    private  BiometricPrompt.CryptoObject cryptoObject;
    private Signature signature;

    @Override
    public void authenticate(Activity context, VerificationDialogStyleBean verificationDialogStyleBean, FingerprintCallback callback) {

        this.fingerprintCallback = callback;

        /*
         * 初始化 BiometricPrompt.Builder
         */
        String title = TextUtils.isEmpty(verificationDialogStyleBean.getTitle()) ?
                context.getString(R.string.biometricprompt_fingerprint_verification) :
                verificationDialogStyleBean.getTitle();
        String cancelText = TextUtils.isEmpty(verificationDialogStyleBean.getCancelBtnText()) ?
                context.getString(R.string.biometricprompt_cancel) :
                verificationDialogStyleBean.getCancelBtnText();
        BiometricPrompt.Builder builder = new BiometricPrompt.Builder(context)
                .setTitle(title)
                .setNegativeButton(cancelText, new Executor() {
                    @Override
                    public void execute(Runnable command) {
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        if (!TextUtils.isEmpty(verificationDialogStyleBean.getSubTitle()))
            builder.setSubtitle(verificationDialogStyleBean.getSubTitle());
        if (!TextUtils.isEmpty(verificationDialogStyleBean.getDescription()))
            builder.setDescription(verificationDialogStyleBean.getDescription());

        //构建 BiometricPrompt
        BiometricPrompt biometricPrompt = builder.build();

        //取消扫描，每次取消后需要重新创建新示例
        cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
            }
        });

        /*
         * 拉起指纹验证模块，等待验证
         * Executor：
         * context.getMainExecutor()
         */
        cryptoObject = new BiometricPrompt.CryptoObject( CipherHelper.getInstance().getSignature(mkeyType));
        biometricPrompt.authenticate(cryptoObject, cancellationSignal, context.getMainExecutor(), authenticationCallback);
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

    public static FingerprintImplForAndrP newInstance(int keyType) {

        return new FingerprintImplForAndrP(keyType);

    }

    public FingerprintImplForAndrP(int keyType) {
        //指纹加密，提前进行Cipher初始化，防止指纹认证时还没有初始化完成
        mkeyType = keyType;
        try {
            cryptoObject = new BiometricPrompt.CryptoObject( CipherHelper.getInstance().getSignature(mkeyType));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 认证结果回调
     */
    private BiometricPrompt.AuthenticationCallback authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            if (fingerprintCallback != null) {
                if (errorCode == 5) {//用户取消指纹验证，不必向用户抛提示信息
                    fingerprintCallback.onCancel();
                    return;
                }
            }
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
        }

        @Override
        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            if (CipherHelper.getInstance().isHasSetupKeyPair()){
//                signature = result.getCryptoObject().getSignature();
                BiometricPrompt.CryptoObject resultCryptoObject = result.getCryptoObject();
                if (resultCryptoObject!=null){
                    signature=resultCryptoObject.getSignature();
                }
            }
            if (fingerprintCallback != null)
                fingerprintCallback.onSucceeded();
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            if (fingerprintCallback != null)
                fingerprintCallback.onFailed();
        }
    };

    /*
     * 在 Android Q，Google 提供了 Api BiometricManager.canAuthenticate() 用来检测指纹识别硬件是否可用及是否添加指纹
     * 不过尚未开放，标记为"Stub"(存根)
     * 所以暂时还是需要使用 Andorid 6.0 的 Api 进行判断
     * */


}