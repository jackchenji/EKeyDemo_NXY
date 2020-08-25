package com.hailong.biometrics.fingerprint.uitls;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import com.froad.ukey.utils.np.TMKeyLog;

import org.bc.jce.provider.BCProvider;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

/**
 * 加密类，用于判定指纹合法性
 */
@RequiresApi(Build.VERSION_CODES.M)
public class CipherHelper {
    // This can be key name you want. Should be unique for the app.
    static final String KEY_NAME_RSA1024 = "eid_fingerprint.CipherHelper_KEY_NAME_RSA1024";
    static final String KEY_NAME_RSA2048 = "eid_fingerprint.CipherHelper_";

    // We always use this keystore on Android.
    static final String KEYSTORE_NAME = "AndroidKeyStore";

    // Should be no need to change these values.

    public static final int TYPESM2 = 0;
    public static final int TYPERSA1024 = 1;
    public static final int TYPERSA2048 = 2;

    private static final String sm2pub = "60C2F72F69B61A03C7EEAB63BC24CF1074B7F28B55AEAE588708D0B36F9F5AC401686AA575420F7C414D7E498C1D784888243DCBB61CD21A14B9503C4121074C";
    private static final String sm2pri = "0DD59589246B681E520D23E421BCABBB60FDE47AB7341BF95F3232C9F7A68595";

    private KeyStore _keystore;
    private PublicKey mPublicKey;
    private PrivateKey mPrivateKey;
    private Signature mSignature;

    private boolean hasSetupKeyPair;
    private boolean hasSetupSMKeyPair;
    private int mkeyType;

    private static final String TAG = "CipherHelper";
    private static class SingletonHolder{
        private static final CipherHelper INSTANCE = new CipherHelper();
    }

    private CipherHelper()
    {
        try {
            _keystore = KeyStore.getInstance(KEYSTORE_NAME);
            _keystore.load(null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获取单例
    public static CipherHelper getInstance(){
        return CipherHelper.SingletonHolder.INSTANCE;
    }


    /**
     * 获得Cipher
     * @return
     */
    public Signature getSignature(int type) {
        TMKeyLog.d(TAG, "getSignature() called");
        mkeyType = type;
        try {
            switch (type){
                case TYPESM2:
                    CreateKeySM2();
                    break;
                case TYPERSA1024:
                    CreateKeyRSA1024();
                    break;
                case TYPERSA2048:
                    CreateKeyRSA2048();
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mSignature;
    }

    private void CreateKeySM2() {
        TMKeyLog.d(TAG, "CreateKeySM2() called");

            if (!hasSetupSMKeyPair){

//            //生成SM2密钥对
//            // 获取SM2椭圆曲线的参数
                final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
// 获取一个椭圆曲线类型的密钥对生成器
                final KeyPairGenerator kpg;
                try {
                    kpg = KeyPairGenerator.getInstance("EC", new BCProvider());
                    // 使用SM2参数初始化生成器
                    kpg.initialize(sm2Spec);

// 使用SM2的算法区域初始化密钥生成器
//            kpg.initialize(sm2Spec, new SecureRandom());
// 获取密钥对
                    KeyPair keyPair = kpg.generateKeyPair();
                    mPrivateKey = keyPair.getPrivate();
                    mPublicKey = keyPair.getPublic();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }
                hasSetupKeyPair = false;

                hasSetupSMKeyPair = true;

            }else {
                hasSetupKeyPair = true;

            }



        mSignature = null;

    }

    public Signature getSM2Signature() {

        Signature sm3withSM2 = null;
        try {
            if (hasSetupSMKeyPair){
                sm3withSM2 = Signature.getInstance("SM3withSM2", new BCProvider());
                sm3withSM2.initSign(mPrivateKey);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return sm3withSM2;
    }

    private void CreateKeyRSA1024() throws Exception {

        TMKeyLog.d(TAG, "CreateKeyRSA1024() called");

        final Certificate certificate = _keystore.getCertificate(KEY_NAME_RSA1024);
        if (certificate == null){
            TMKeyLog.d(TAG, "CreateKey: publicKey == null");
            hasSetupKeyPair = false;

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_NAME);
            keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(KEY_NAME_RSA1024, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA1)
                    .setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4))
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    // 设置需要用户验证
                    .setUserAuthenticationRequired(true)
                    .build());
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            mPublicKey = keyPair.getPublic();
            mPrivateKey = keyPair.getPrivate();
        }else {
            TMKeyLog.d(TAG, "CreateKey: publicKey != null");
            hasSetupKeyPair = true;
            mPublicKey = certificate.getPublicKey();
            KeyStore.Entry entry = _keystore.getEntry(KEY_NAME_RSA1024, null);
            mPrivateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        }


        mSignature = Signature.getInstance("SHA1withRSA");
        mSignature.initSign(mPrivateKey);
    }
    private void CreateKeyRSA2048() throws Exception {

        TMKeyLog.d(TAG, "CreateKeyRSA2048() called");

        final Certificate certificate = _keystore.getCertificate(KEY_NAME_RSA2048);
        if (certificate == null){
            TMKeyLog.d(TAG, "CreateKey: publicKey == null");
            hasSetupKeyPair = false;

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_NAME);
            keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(KEY_NAME_RSA2048, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    // 设置需要用户验证
                    .setUserAuthenticationRequired(true)
                    .build());
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            mPublicKey = keyPair.getPublic();
            mPrivateKey = keyPair.getPrivate();
        }else {
            TMKeyLog.d(TAG, "CreateKey: publicKey != null");
            hasSetupKeyPair = true;
            mPublicKey = certificate.getPublicKey();
            KeyStore.Entry entry = _keystore.getEntry(KEY_NAME_RSA2048, null);
            mPrivateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        }


        mSignature = Signature.getInstance("SHA256withRSA");
        mSignature.initSign(mPrivateKey);
    }




    public PublicKey getPublicKey() {
        return mPublicKey;
    }

    public boolean isHasSetupKeyPair() {
        return hasSetupKeyPair;
    }

    public void deleteCurrentPublicKey(int keyType) {
        TMKeyLog.d(TAG, "deleteCurrentPublicKey");
        try {
            if (keyType == TYPERSA1024){
                _keystore.deleteEntry(KEY_NAME_RSA1024);
            }else if (keyType == TYPERSA2048){
                _keystore.deleteEntry(KEY_NAME_RSA2048);
            }else if (keyType == TYPESM2){
                hasSetupSMKeyPair = false;
            }
            hasSetupKeyPair = false;
        } catch (KeyStoreException e) {
            TMKeyLog.d(TAG, "deleteCurrentPublicKey KeyStoreException : "+e.getMessage());
        }
    }

    public int getMkeyType() {
        return mkeyType;
    }

    public PrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    public void setHasSetupKeyPair(boolean hasSetupKeyPair) {
        this.hasSetupKeyPair = hasSetupKeyPair;
    }
}