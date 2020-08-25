package com.froad.ukey.bean;

/**
 * Created by FW on 2017/4/6.
 */
public class CardInfo {

    private boolean isInitPwd = true;
    /**
     * 以下四个证书状态
     * 00：已个人化未生成密钥对
     * 01：已生成密钥对未预制证书
     * 10：已预制证书
     */
    private String cer_state_rsa1024 = "";//rsa1024证书状态
    private String cer_state_rsa2048 = "";//rsa2048证书状态
    private String cer_state_sm2_sign = "";//国密签名证书状态
    private String cer_state_sm2_enc = "";//国密加密证书状态
    private String merchantInfo = "";//商户信息
    private String cosVersion = "";//商户COS版本号
    private String csn = "";//卡号
    private String rsaSha1 = "";
    private String sm2SignSha1 = "";
    private String sm2EncSha1 = "";

    public boolean isInitPwd() {
        return isInitPwd;
    }

    public void setInitPwd(boolean initPwd) {
        isInitPwd = initPwd;
    }

    public String getCer_state_rsa1024() {
        return cer_state_rsa1024;
    }

    public void setCer_state_rsa1024(String cer_state_rsa1024) {
        this.cer_state_rsa1024 = cer_state_rsa1024;
    }

    public String getCer_state_rsa2048() {
        return cer_state_rsa2048;
    }

    public void setCer_state_rsa2048(String cer_state_rsa2048) {
        this.cer_state_rsa2048 = cer_state_rsa2048;
    }

    public String getCer_state_sm2_sign() {
        return cer_state_sm2_sign;
    }

    public void setCer_state_sm2_sign(String cer_state_sm2_sign) {
        this.cer_state_sm2_sign = cer_state_sm2_sign;
    }

    public String getCer_state_sm2_enc() {
        return cer_state_sm2_enc;
    }

    public void setCer_state_sm2_enc(String cer_state_sm2_enc) {
        this.cer_state_sm2_enc = cer_state_sm2_enc;
    }

    public String getMerchantInfo() {
        return merchantInfo;
    }

    public void setMerchantInfo(String merchantInfo) {
        this.merchantInfo = merchantInfo;
    }

    public String getCosVersion() {
        return cosVersion;
    }

    public void setCosVersion(String cosVersion) {
        this.cosVersion = cosVersion;
    }

    public String getCsn() {
        return csn;
    }

    public void setCsn(String csn) {
        this.csn = csn;
    }

    public String getRsaSha1() {
        return rsaSha1;
    }

    public void setRsaSha1(String rsaSha1) {
        this.rsaSha1 = rsaSha1;
    }

    public String getSm2SignSha1() {
        return sm2SignSha1;
    }

    public void setSm2SignSha1(String sm2SignSha1) {
        this.sm2SignSha1 = sm2SignSha1;
    }

    public String getSm2EncSha1() {
        return sm2EncSha1;
    }

    public void setSm2EncSha1(String sm2EncSha1) {
        this.sm2EncSha1 = sm2EncSha1;
    }
}
