package com.hailong.biometrics.fingerprint;

import android.app.Activity;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import com.hailong.biometrics.fingerprint.bean.VerificationDialogStyleBean;
import com.hailong.biometrics.fingerprint.uitls.AndrVersionUtil;

/**
 * Created by ZuoHailong on 2019/7/9.
 */
public class FingerprintVerifyManager {

    private  IFingerprint fingerprint;
    private  VerificationDialogStyleBean bean;
    private Builder mBuilder;

    public FingerprintVerifyManager(Builder builder) {

        if (AndrVersionUtil.isAboveAndrP()) {
            if (builder.enableAndroidP) {
                fingerprint = FingerprintImplForAndrP.newInstance(builder.keyType);
            } else{
                fingerprint = FingerprintImplForAndrM.newInstance(builder.keyType);
            }
        }else {
            fingerprint = FingerprintImplForAndrM.newInstance(builder.keyType);
        }
        /**
         * 设定指纹验证框的样式
         */
        // >= Android 6.0
        bean = new VerificationDialogStyleBean();
        bean.setCancelTextColor(builder.cancelTextColor);
        bean.setUsepwdTextColor(builder.usepwdTextColor);
        bean.setFingerprintColor(builder.fingerprintColor);
        bean.setUsepwdVisible(builder.usepwdVisible);

        // >= Android 9.0
        bean.setTitle(builder.title);
        bean.setSubTitle(builder.subTitle);
        bean.setDescription(builder.description);
        bean.setCancelBtnText(builder.cancelBtnText);

        mBuilder = builder;
    }


    public void authenticate() {
         fingerprint.authenticate(mBuilder.context, bean, mBuilder.callback);
    }


    public byte[] getSignData(byte [] randomMessage){
        return fingerprint.sign(randomMessage);
    }



    /**
     * UpdateAppManager的构建器
     */
    public static class Builder {

        /*必选字段*/
        private Activity context;
        private FingerprintCallback callback;
        private int keyType;

        /*可选字段*/
        private int cancelTextColor;
        private int usepwdTextColor;
        private int fingerprintColor;
        private boolean usepwdVisible;

        private boolean enableAndroidP;//在Android 9.0系统上，是否开启google提供的验证方式及验证框
        private String title;
        private String subTitle;
        private String description;
        private String cancelBtnText;//取消按钮文字

        /**
         * 构建器
         *
         * @param activity
         */
        public Builder(@NonNull Activity activity) {
            this.context = activity;
        }

        /**
         * 指纹识别回调
         *
         * @param callback
         */
        public Builder callback(@NonNull FingerprintCallback callback) {
            this.callback = callback;
            return this;
        }

        /**
         * 取消按钮文本色
         *
         * @param color
         */
        public Builder cancelTextColor(@ColorInt int color) {
            this.cancelTextColor = color;
            return this;
        }

        /**
         * 密码验证按钮文本色
         *
         * @param color
         */
        public Builder usepwdTextColor(@ColorInt int color) {
            this.usepwdTextColor = color;
            return this;
        }

        /**
         * 指纹图标颜色
         *
         * @param color
         */
        public Builder fingerprintColor(@ColorInt int color) {
            this.fingerprintColor = color;
            return this;
        }

        /**
         * 密码登录按钮是否显示
         *
         * @param isVisible
         */
        public Builder usepwdVisible(boolean isVisible) {
            this.usepwdVisible = isVisible;
            return this;
        }

        /**
         * 在 >= Android 9.0 系统上，是否开启google提供的验证方式及验证框
         *
         * @param enableAndroidP
         */
        public Builder enableAndroidP(boolean enableAndroidP) {
            this.enableAndroidP = enableAndroidP;
            return this;
        }

        /**
         * >= Android 9.0 的验证框的主标题
         *
         * @param title
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * >= Android 9.0 的验证框的副标题
         *
         * @param subTitle
         */
        public Builder subTitle(String subTitle) {
            this.subTitle = subTitle;
            return this;
        }

        /**
         * >= Android 9.0 的验证框的描述内容
         *
         * @param description
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * >= Android 9.0 的验证框的取消按钮的文字
         *
         * @param cancelBtnText
         */
        public Builder cancelBtnText(String cancelBtnText) {
            this.cancelBtnText = cancelBtnText;
            return this;
        }
        public Builder keyType(int  keyType) {
            this.keyType = keyType;
            return this;
        }

        /**
         * 开始构建
         *
         * @return
         */
        public FingerprintVerifyManager build() {
            return new FingerprintVerifyManager(this);
        }
    }

}
