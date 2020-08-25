package com.cn.froad.ekeydemo_nxy;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.froad.ekeydemo_nxy.constant.RSAUtils;
import com.cn.froad.ekeydemo_nxy.constant.VDesCryptAction;
import com.cn.froad.ekeydemo_nxy.constant.VRSAUtilAction;
import com.cn.froad.ekeydemo_nxy.permission.DefaultActivityPermissionListener;
import com.cn.froad.ekeydemo_nxy.permission.PermissionsUtil;
import com.cn.froad.ekeydemo_nxy.utils.Bip;
import com.cn.froad.ekeydemo_nxy.utils.Client;
import com.cn.froad.ekeydemo_nxy.utils.Constants;
import com.cn.froad.ekeydemo_nxy.utils.MyDialog;
import com.cn.froad.ekeydemo_nxy.utils.NfcManager;
import com.cn.froad.ekeydemo_nxy.utils.RefResTool;
import com.cn.froad.ekeydemo_nxy.utils.ResultInterf;
import com.cn.froad.ekeydemo_nxy.utils.SharedPrefs;
import com.cn.froad.ekeydemo_nxy.view.FroadKeyboard;
import com.froad.eid.ecard.bean.ReadElecCardState;
import com.froad.eid.ecard.interf.ReadElecCardCallBack;
import com.froad.eid.ecard.manager.EIDCardOMAManager;
import com.froad.eid.ecard.service.CardService;
import com.froad.ukey.http.HttpConstants;
import com.froad.ukey.interf.CosUpdateCallBack;
import com.froad.ukey.interf.SelectCertDialogResult;
import com.froad.ukey.manager.SIMBaseManager;
import com.froad.ukey.manager.TmKeyManager;
import com.froad.ukey.manager.VCardApi_FFT;
import com.froad.ukey.simchannel.imp.SESDefaultHelper;
import com.froad.ukey.simchannel.imp.SMSHelper;
import com.froad.ukey.utils.np.AppExecutors;
import com.froad.ukey.utils.np.CardConnState;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.SM3;
import com.froad.ukey.utils.np.SM4Util;
import com.froad.ukey.utils.np.TMKeyLog;
import com.hailong.biometrics.fingerprint.FingerprintCallback;
import com.hailong.biometrics.fingerprint.FingerprintVerifyManager;
import com.hailong.biometrics.fingerprint.uitls.CipherHelper;
import com.micronet.api.IVCardApiInterface;
import com.micronet.api.Result;
import com.micronet.bakapp.utils.SM4;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.suke.widget.SwitchButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.cert.X509Certificate;

import bip.BipManager;
import bip.BipResult;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    @BindView(R.id.main_showResult_tv)
    public EditText main_showResult_tv;
    @BindView(R.id.main_fun_radioGroup)
    public RadioGroup main_fun_radioGroup;
    @BindView(R.id.main_fun_fftKey)
    public RadioButton main_fun_fftKey;

    @BindView(R.id.main_cos_update_env_rg)
    public RadioGroup main_cos_update_env_rg;
    @BindView(R.id.main_cos_update_env_test)
    public RadioButton main_cos_update_env_test;

    @BindView(R.id.main_root_layout)
    public LinearLayout main_root_layout;
    @BindView(R.id.inside_btn_layout)
    public LinearLayout inside_btn_layout;
    @BindView(R.id.inside_set_cos_version_btn)
    public Button inside_set_cos_version_btn;
    @BindView(R.id.inside_check_cos_version_btn)
    public Button inside_check_cos_version_btn;
    @BindView(R.id.inside_hasCard_btn)
    public Button inside_hasCard_btn;
    @BindView(R.id.inside_popStk_btn)
    public Button inside_popStk_btn;
    @BindView(R.id.inside_getSDKVer_btn)
    public Button inside_getSDKVer_btn;
    @BindView(R.id.inside_getRSA2048Cer_btn)
    public Button inside_getRSA2048Cer_btn;
    @BindView(R.id.inside_changepin_btn)
    public Button inside_changepin_btn;
    @BindView(R.id.inside_resetpin_btn)
    public Button inside_resetpin_btn;
    @BindView(R.id.inside_getErrorCode_btn)
    public Button inside_getErrorCode_btn;
    @BindView(R.id.inside_sign_RSA_1024_btn)
    public Button inside_sign_RSA_1024_btn;
    @BindView(R.id.inside_sign_RSA_2048_btn)
    public Button inside_sign_RSA_2048_btn;
    @BindView(R.id.inside_sign_SM2_btn)
    public Button inside_sign_SM2_btn;
    @BindView(R.id.inside_writeHashCode_btn)
    public Button inside_writeHashCode_btn;
    @BindView(R.id.inside_readHashCode_btn)
    public Button inside_readHashCode_btn;

    @BindView(R.id.init_card)
    public Button init_card;
    @BindView(R.id.create_key_pair)
    public Button create_key_pair;
    @BindView(R.id.create_p10)
    public Button create_p10;
    @BindView(R.id.create_cert)
    public Button create_cert;
    @BindView(R.id.write_cert)
    public Button write_cert;

    @BindView(R.id.main_isneed_oma)
    public CheckBox main_isneed_oma;
    @BindView(R.id.main_isneed_uicc)
    public CheckBox main_isneed_uicc;
    @BindView(R.id.main_isneed_adn)
    public CheckBox main_isneed_adn;

    @BindView(R.id.main_isneed_sdkhash)
    public CheckBox main_isneed_sdkhash;


    @BindView(R.id.main_isneed_bip)
    public CheckBox main_isneed_bip;

    //电子证照控件
    @BindView(R.id.inside_ecard_btn_layout)
    public LinearLayout inside_ecard_btn_layout;
    @BindView(R.id.main_isneed_ecard_uicc)
    public CheckBox main_isneed_ecard_uicc;
    @BindView(R.id.main_isneed_ecard_oma)
    public CheckBox main_isneed_ecard_oma;
    @BindView(R.id.main_check_ecard_channel)
    public Button main_check_ecard_channel;
    @BindView(R.id.main_read_ecard_id_carrier)
    public Button main_read_ecard_id_carrier;
    @BindView(R.id.main_verify_ecard_pin)
    public Button main_verify_ecard_pin;
    @BindView(R.id.main_change_ecard_pin)
    public Button main_change_ecard_pin;
    @BindView(R.id.main_check_ecard)
    public Button main_check_ecard;
    @BindView(R.id.main_read_ecard_pwd_info)
    public Button main_read_ecard_pwd_info;
    @BindView(R.id.main_open_read_ecard_channel)
    public Button main_open_read_ecard_channel;

    @BindView(R.id.bip_getcardno)
    public Button bip_getcardno;


    @BindView(R.id.bip_cosVersion)
    public Button bip_cosVersion;



    //输入模块
    @BindView(R.id.inside_input_layout)
    public LinearLayout inside_input_layout;
    @BindView(R.id.main_input_tv)
    public TextView main_input_tv;
    @BindView(R.id.main_input_et)
    public EditText main_input_et;


    @BindView(R.id.cardNO)
    public EditText cardNOEdt;

      @BindView(R.id.yangzhengma)
    public EditText yangzhengmaEdt;


    @BindView(R.id.input_cardno)
    public EditText input_cardno;


    @BindView(R.id.input_phone)
    public EditText input_phone;

    @BindView(R.id.yangzhengmashuijishu)
    public TextView yangzhengmashuijishu;


    private String cardNO="";//卡号
    private String yangzhengma="";//验证码

    public  static  Boolean isUseBip=true;

    private IVCardApiInterface mVCardApiFFT;

    private boolean ecHasCard;
    private boolean ecHasECard;
    private SpannableStringBuilder ecLogBuilder = new SpannableStringBuilder();
    private long ecStartTime = 0;//电子证照统计操作时间
    private long ecEndTime = 0;

    private boolean insideHasCard;
    private boolean insideUsableCard;
    private String signBeforeData;//签名原文
    private String signAfterData;//签名值
    private String sdfp = "yyyy-MM-dd hh:mm";
    private SimpleDateFormat sdf = new SimpleDateFormat(sdfp);
    private SharedPreferences sp = null;
    private String spName = "sysSpName";
    private String keyName = "keyName";
    private String pinStr;
    private Dialog d;
    private Result signResult = null;

    private final String HASHCODE_HEBAOZHIFU = "3CFB3A993DE5EE0F50B746F6F1B8F5A4AEFDF79A";
    private final String HASHCODE_WOQIANBAO = "5E4133C157990769C87F7586281B4EE4A16644CD";
    private final String HASHCODE_YIZHIFU = "7CF93DF551A792B87F293A455514A94BE1585633";
    private final String HASHCODE_APP = "BFBF32266573955D3DF14B8804722EB52CB64EDE";

    private final int SIGN_TYPE_RSA_1024 = 0;
    private final int SIGN_TYPE_RSA_2048 = 1;
    private final int SIGN_TYPE_SM2 = 2;
    private FingerprintVerifyManager fingerprintVerifyManager;

    ConnectivityManager.NetworkCallback callback;
    ConnectivityManager connectivityManager;


    String phone="";
    String cardNo="";
    String typeFlag="";
    String bipDatas="";

    public com.suke.widget.SwitchButton switchButton;



    private static final String[] m={bip.Constants.httpsUrlUat,bip.Constants.httpsUrlUat1,bip.Constants.httpsUrlUat2,bip.Constants.httpsUrlUat3,bip.Constants.httpsUrlUat4,bip.Constants.httpsUrlUat5,bip.Constants.httpsshengchan};
    private TextView view ;
    private Spinner spinner;
    private ArrayAdapter<String> adapter;

    public void setSpinner(){

spinner=(Spinner)findViewById(R.id.Spinner01);
//将可选内容与ArrayAdapter连接起来  
adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,m);

//设置下拉列表的风格  
adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

//将adapter 添加到spinner中  
spinner.setAdapter(adapter);

//添加事件Spinner事件监听    
spinner.setOnItemSelectedListener(new SpinnerSelectedListener());

//设置默认值  
spinner.setVisibility(View.VISIBLE);
    }




    class  SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {
        /**
         * <p>Callback method to be invoked when an item in this view has been
         * selected. This callback is invoked only when the newly selected
         * position is different from the previously selected position or if
         * there was no selected item.</p>
         * <p>
         * Implementers can call getItemAtPosition(position) if they need to access the
         * data associated with the selected item.
         *
         * @param parent   The AdapterView where the selection happened
         * @param view     The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id       The row id of the item that is selected
         */
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            TMKeyLog.d(TAG, "网络请求选择地址:"+m[position]);
            bip.Constants.httpsUrl=m[position];
        }

        /**
         * Callback method to be invoked when the selection disappears from this
         * view. The selection can disappear for instance when touch is activated
         * or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         */
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }






    /**
     * handler_postDelayed 方法实现
     */
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Message msg = handler.obtainMessage(0);
            handler.sendMessage(msg);
        }

    };
    // handler+postDelayed 方式，反复发送延时消息
    private void handlerPostDelayed() {
        isNeedLog=true;
        TMKeyLog.sbf.setLength(0);
        handler.postDelayed(mRunnable, 15000); //8秒后显示错误日志
    }

    boolean isNeedLog=true;

    public  void getBipConfig(){
        typeFlag="";
        phone=input_phone.getText().toString();
        cardNo=input_cardno.getText().toString();
        if(!phone.equals("")){
            typeFlag="01";
        }else {
            typeFlag="02";
        }

        if(!phone.equals("")){
            bipDatas=phone;
        }else {
            bipDatas=cardNo;
        }
        TMKeyLog.d(TAG, "switchButton 是否开启:"+switchButton.isChecked()+"验证码"+yangzhengmashuijishu.getText().toString());
        if(switchButton.isChecked()){
            eidyangzhengma=yangzhengmashuijishu.getText().toString();
            TMKeyLog.d(TAG, "验证码"+eidyangzhengma);
        }


        mVCardApiFFT.setBipConfig(bipDatas,typeFlag,eidyangzhengma);
        handlerPostDelayed(); //添加错误日志
    }

    public  void modifyBip(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!SIMBaseManager.bipErrorStrig.equals("")){
                    main_showResult_tv.setText("");
                    main_showResult_tv.setText(SIMBaseManager.mybipResult.message);}
                SIMBaseManager.mybipResult=null;
                SIMBaseManager.bipErrorStrig="";

            }
        });
    }


    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void forceSendRequestByMobileData() {
        try {
            handlerPostDelayed();
            ArrayList<BipResult> carNo;
            typeFlag="";
            phone=input_phone.getText().toString();
            cardNo=input_cardno.getText().toString();
                 /*   if(phone.equals("")&&cardNO.equals("")){
                        Toast.makeText(getApplicationContext(), "请输入电话或者卡号", 3000);
                        return;
                    }*/
            String testInstruct="B11000000C";
            if(switchButton.isChecked()){
                eidyangzhengma=yangzhengmashuijishu.getText().toString();
                TMKeyLog.d(TAG, "bip测试验证码"+eidyangzhengma);
            }else {
                eidyangzhengma="";
            }
            if(!eidyangzhengma.equals("")){
                testInstruct=testInstruct+eidyangzhengma;
            }


            Message msg =Message.obtain();
            BipResult bipResult=new BipResult();

            mVCardApiFFT=VCardApi_FFT.getInstance();

           getBipConfig();
            ArrayList<BipResult>  myBipResultList= BipManager.getInstance().onStartBip("B11000000C");
            if(myBipResultList.get(0).getSuccess()){
                TMKeyLog.d(TAG, "bip 返回为true");
                ArrayList<String>  arrayList=myBipResultList.get(0).arrayList;
                String carNos;
                if(arrayList.get(0).length()>26){
                    carNos=arrayList.get(0).substring(10,26);}else {
                    carNos="卡片返回长度异常";
                }

                //从全局池中返回一个message实例，避免多次创建message（如new Message）
                msg.obj = carNos;
                msg.what=1;   //标志消息的标志
                handler.sendMessage(msg);
            }else {
                TMKeyLog.d(TAG, "bip 返回为false");
                //从全局池中返回一个message实例，避免多次创建message（如new Message）
                TMKeyLog.d(TAG, "bip 返回信息："+bipResult.getMessage());
                msg.obj = "bip获取数据失败，请重试";
                msg.what = 1;   //标志消息的标志
                handler.sendMessage(msg);
            }

        }catch (Exception E){
            TMKeyLog.d(TAG, "主页面异常数据信息:"+E.getMessage());
        }


    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        connectivityManager= (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        initView();
        TMKeyLog.newSbf();



//        mVCardApiFFT = VCardApi_FFT.getInstance();
//        mVCardApiFFT.init(getApplicationContext());

//        String certData = "MIIECgYJKoZIhvcNAQcCoIID+zCCA/cCAQExADALBgkqhkiG9w0BBwGgggPfMIID2zCCAsOgAwIBAgIFAfi7fR0wDQYJKoZIhvcNAQELBQAwOjESMBAGA1UEAwwJUlNBQ0E2MjI0MRcwFQYDVQQKDA5PbmNlRG93bkNlcnRDQTELMAkGA1UEBhMCY24wHhcNMTkxMDIxMDI1ODA0WhcNMjAxMDIwMDI1ODA0WjBrMTUwMwYDVQQDDCwwNjNAMDM3MTIwMjE5OTcwOTI2NzQwOUBaaGFuZ3d1MDNAMTAwMDAwMDI2MTEUMBIGA1UECwwLRW50ZXJwcmlzZXMxDzANBgNVBAsMBjExMTExMTELMAkGA1UEBhMCY24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCgTNJFfHY4YwcPsSGX1W5UVI0XpKbVxyjGqJRWt8HLzAvop9w0OrNOkReB5LgtUwQxEwh9NfyWnnooMWVMHBT7ltLCkUOB/HFOGW4VBt/mjUT9L2ptuc60Sc7aNMesdHZbU3b05y9FWZ9uMuXaWyLi4taLT57o2jWtbzTUz1LDxsxlwCvrwslNhgC+qotXYr8BmBF8NQiIH6ttDAgHsB1ONklTjcnXTn7FQ3a9g8FoGGyRGxdgc2LVVeHtfeFOyrl8GcFlzDyzJrziihy2+nojty32DLIbGml10ynWgbjwvKE4Ggb40x0R4k2FYJl8ntHCat26+UhY3JEcuP74CaYtAgMBAAGjgbYwgbMwHwYDVR0jBBgwFoAUmPVQ2tFv9AKd/52c5TqAraWNc9EwCQYDVR0TBAIwADBWBgNVHR8ETzBNMEugSaBHpEUwQzENMAsGA1UEAwwEY3JsMTEMMAoGA1UECwwDY3JsMRcwFQYDVQQKDA5PbmNlRG93bkNlcnRDQTELMAkGA1UEBhMCY24wDgYDVR0PAQH/BAQDAgbAMB0GA1UdDgQWBBQLXouujN0PvbVhEpdbTJlu6Wt32DANBgkqhkiG9w0BAQsFAAOCAQEAEQ4/ofgUXeJ6zIQYzRhhzCMZPVIS/jkMrFlPZVtH3R6AaESSSPWlDLghNCeCs3+HhZkCw6cocAkwwHS1iRdO9Xpgf+IaZhcPcC7Ff4lzbl3GnuMXkQ1QCADPi9TIwxkRH+OdB0IYU/batyqJtrGo0lw6IYbJARWMMZVW/PYpb5RLppsPIUE4/vLbtEpRr4dy/RuRgSI/KXtRazVb3sKDqc15GR1DClkhdNxW47VOrCvdc2pMDB0h/Pxh57xAmFXlP1epRk77Wx9yiM4gABTI15jpkcLcKeRKSKVDv8VLOftDpHMH+v+xzo2aUKX9qumTu2+4i5+0BSMgesFQF7V+qDEA";
//        byte[] certDataBytes = Base64.decode(certData, Base64.NO_WRAP);
//        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>certDataBytes111:" + FCharUtils.bytesToHexStr(certDataBytes));
//        GmSSL gmSSL= new GmSSL();
//        long converRes = gmSSL.convertPkcs7ToPemHex(certDataBytes, certDataBytes.length);
//        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>converRes:" + converRes);
//        if (converRes != 0) { //转换数据成功
//            certDataBytes = gmSSL.pemBytes;
//            TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>certDataBytes222:" + FCharUtils.bytesToHexStr(certDataBytes));
//        }
//
//        certData = "MIICUDCCAfWgAwIBAgIJAKqqqqqqqrApMAwGCCqBHM9VAYN1BQAwOjELMAkGA1UEBhMCQ04xFzAVBgNVBAoMDk9uY2VEb3duQ2VydENBMRIwEAYDVQQDDAlTTTJDQTYyMjQwHhcNMTkxMDIxMDI1OTQwWhcNMjAxMDIwMDI1OTQwWjBlMQswCQYDVQQGEwJjbjEPMA0GA1UECwwGMTExMTExMQ4wDAYDVQQLDAVVbml0czE1MDMGA1UEAwwsMDYzQDAzNzEyMDIxOTk3MDkyNjc0MDlAWmhhbmd3dTAzQDEwMDAwMDAyNjIwWTATBgcqhkjOPQIBBggqgRzPVQGCLQNCAATaE600wuGZPk9prHRD4qHJUCFm+76O+UD/FQrUeoLuA7+Tm8AS+txaQpiIfC7luIFpTb1j/WvjwUbAJXACg+eho4G2MIGzMB8GA1UdIwQYMBaAFBAKcTyFiiXG25MbRqalB84XGA7wMAkGA1UdEwQCMAAwVgYDVR0fBE8wTTBLoEmgR6RFMEMxDTALBgNVBAMMBGNybDIxDDAKBgNVBAsMA2NybDEXMBUGA1UECgwOT25jZURvd25DZXJ0Q0ExCzAJBgNVBAYTAkNOMA4GA1UdDwEB/wQEAwIGwDAdBgNVHQ4EFgQURFDkzzyhkBoVBG7IX+FO57nmsT4wDAYIKoEcz1UBg3UFAANHADBEAiBC+53HISp0JqW5bySX/oKTJT3sK8Glizdc+5LXZE9VIgIgakDxNAr9FZsu+5b0U4sa6Mf3XRYyAe6jM3nlKXzWpcA=";
//        certDataBytes = Base64.decode(certData, Base64.DEFAULT);
//        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_sm2_sign>>>certDataBytes:" + FCharUtils.bytesToHexStr(certDataBytes));
//
//        certData = "MIICUTCCAfWgAwIBAgIJAKqqqqqqqrAoMAwGCCqBHM9VAYN1BQAwOjELMAkGA1UEBhMCQ04xFzAVBgNVBAoMDk9uY2VEb3duQ2VydENBMRIwEAYDVQQDDAlTTTJDQTYyMjQwHhcNMTkxMDIxMDI1OTQwWhcNMjAxMDIwMDI1OTQwWjBlMQswCQYDVQQGEwJjbjEPMA0GA1UECwwGMTExMTExMQ4wDAYDVQQLDAVVbml0czE1MDMGA1UEAwwsMDYzQDAzNzEyMDIxOTk3MDkyNjc0MDlAWmhhbmd3dTAzQDEwMDAwMDAyNjIwWTATBgcqhkjOPQIBBggqgRzPVQGCLQNCAASre/Z5kBoHSzP2Jc+sDZUQpWg4i4Le+dKRifSFQkHAsqUE/DqsikmS1zqXTQfSvJMNDca7D4/v44OfmMqsRLGao4G2MIGzMB8GA1UdIwQYMBaAFBAKcTyFiiXG25MbRqalB84XGA7wMAkGA1UdEwQCMAAwVgYDVR0fBE8wTTBLoEmgR6RFMEMxDTALBgNVBAMMBGNybDIxDDAKBgNVBAsMA2NybDEXMBUGA1UECgwOT25jZURvd25DZXJ0Q0ExCzAJBgNVBAYTAkNOMA4GA1UdDwEB/wQEAwIDODAdBgNVHQ4EFgQUGgyKD8s3HJAONjUgdbPkDu5v6mgwDAYIKoEcz1UBg3UFAANIADBFAiEAnVXLuxskI6Iexe5cQngJniFRHJF8MWklUOJTpDYTvPoCIACpIBJa5v7NhbkK5Wq0j6yeL3tWhE7jdhZlke/cFhu9";
//        certDataBytes = Base64.decode(certData, Base64.DEFAULT);
//        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_sm2_enc>>>certDataBytes:" + FCharUtils.bytesToHexStr(certDataBytes));

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        TMKeyLog.d(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    //判断移动数据是否打开
    public static boolean isMobile(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return true;
        }
        return false;
    }



    public void initView() {
        main_fun_radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.main_fun_fftKey) {
                    inside_btn_layout.setVisibility(View.VISIBLE);
                    inside_input_layout.setVisibility(View.VISIBLE);
                    inside_ecard_btn_layout.setVisibility(View.GONE);
                } else {
                    inside_btn_layout.setVisibility(View.GONE);
                    inside_input_layout.setVisibility(View.GONE);
                    inside_ecard_btn_layout.setVisibility(View.VISIBLE);
                }
            }
        });
        main_fun_fftKey.toggle();

        main_cos_update_env_rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.main_cos_update_env_test) {
                    HttpConstants.URL_GETCOSBYCLIENT = HttpConstants.URL_BASE_TEST + "/site/vCardQrcode/getCosByClient";
//                    a.c = a.a + "/site/vCardQrcode/getCosByClient";
                } else {
                    HttpConstants.URL_GETCOSBYCLIENT = HttpConstants.URL_BASE_PRODUCT + "/site/vCardQrcode/getCosByClient";
//                    a.c = a.b + "/site/vCardQrcode/getCosByClient";
                }
            }
        });
        main_cos_update_env_test.toggle();

        inside_set_cos_version_btn.setOnClickListener(this);
        inside_check_cos_version_btn.setOnClickListener(this);
        inside_hasCard_btn.setOnClickListener(this);
        inside_popStk_btn.setOnClickListener(this);
        inside_getSDKVer_btn.setOnClickListener(this);
        inside_getRSA2048Cer_btn.setOnClickListener(this);
        inside_changepin_btn.setOnClickListener(this);
        inside_resetpin_btn.setOnClickListener(this);
        inside_getErrorCode_btn.setOnClickListener(this);
        inside_sign_RSA_1024_btn.setOnClickListener(this);
        inside_sign_RSA_2048_btn.setOnClickListener(this);
        inside_sign_SM2_btn.setOnClickListener(this);
        inside_writeHashCode_btn.setOnClickListener(this);
        inside_readHashCode_btn.setOnClickListener(this);

        init_card.setOnClickListener(this);
        create_key_pair.setOnClickListener(this);
        create_p10.setOnClickListener(this);
        create_cert.setOnClickListener(this);
        write_cert.setOnClickListener(this);

        inside_set_cos_version_btn.setVisibility(View.GONE);
        inside_btn_layout.setVisibility(View.VISIBLE);
        inside_writeHashCode_btn.setVisibility(View.GONE);
        inside_readHashCode_btn.setVisibility(View.GONE);

        bip_getcardno.setOnClickListener(this);
        bip_cosVersion.setOnClickListener(this);

        refreshButtons();

        main_isneed_uicc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SIMBaseManager.isNeedUICC = true;
                } else {
                    SIMBaseManager.isNeedUICC = false;
                }
            }
        });


        main_isneed_bip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SIMBaseManager.isNeedBip = true;
                } else {
                    SIMBaseManager.isNeedBip = false;
                }
            }
        });

        main_isneed_uicc.setChecked(true);

        main_isneed_oma.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SIMBaseManager.isNeedOMA = true;
                } else {
                    SIMBaseManager.isNeedOMA = false;
                }
            }
        });
        main_isneed_oma.setChecked(true);

        main_isneed_adn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SIMBaseManager.isNeedADN = true;
                } else {
                    SIMBaseManager.isNeedADN = false;
                }
            }
        });
        main_isneed_adn.setChecked(true);

        main_isneed_sdkhash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SIMBaseManager.isNeedSignHashData = true;
                } else {
                    SIMBaseManager.isNeedSignHashData = false;
                }
            }
        });
        main_isneed_sdkhash.setChecked(true);

        main_input_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int sl = s.length();
                if (sl == 0) {
                    main_input_tv.setText("请输入");
                } else {
                    main_input_tv.setText("已输入 " + s.length() + " 个字符");
                }
            }
        });

        input_cardno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input_phone.setText("");
            }
        });

        input_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input_cardno.setText("");
            }
        });




        initECardView();
    }

    public void initECardView() {
        main_isneed_ecard_uicc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
//                    com.froad.eid.ecard.manager.SIMBaseManager.isCanOpenUICC = true;
                    com.froad.eid.ecard.manager.a.X = true;
                } else {
//                    com.froad.eid.ecard.manager.SIMBaseManager.isCanOpenUICC = false;
                    com.froad.eid.ecard.manager.a.X = false;
                }
            }
        });
        main_isneed_ecard_uicc.setChecked(true);

        main_isneed_ecard_oma.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
//                    com.froad.eid.ecard.manager.SIMBaseManager.isCanOpenOMA = true;
                    com.froad.eid.ecard.manager.a.Y = true;
                } else {
//                    com.froad.eid.ecard.manager.SIMBaseManager.isCanOpenOMA = false;
                    com.froad.eid.ecard.manager.a.Y = false;
                }
            }
        });
        main_isneed_ecard_oma.setChecked(true);

        main_check_ecard_channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openECardChannel();
            }
        });
        main_check_ecard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkEidCardInfo();
            }
        });
        main_open_read_ecard_channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EIDCardOMAManager.getInstance(MainActivity.this).setReadElecCardCallBack(new ReadElecCardCallBack() {
                    @Override
                    public void onResult(ReadElecCardState state) {
                        switch (state) {
                            case READ_ELEC_CARD_START:
                                com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "READ_ELEC_CARD_START");
                                MyDialog.showWaitPro(MainActivity.this, "开始读取电子证照");
                                appendLog(true, "开始读取电子证照", Color.RED, true, false);
                                ecStartTime = getCurrentTime();
                                break;
                            case READ_ELEC_CARD_SUCCESS:
                                ecEndTime = getCurrentTime();
                                com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "READ_ELEC_CARD_SUCCESS");
                                EIDCardOMAManager.getInstance(MainActivity.this).setOpenReadCardChannel(false);
                                appendLog(true, "读取电子证照成功，功能已关闭\n耗时:" + (ecEndTime - ecStartTime) + "ms", Color.RED, true, false);
                                MyDialog.dismissWaitPro();
                                MyDialog.showSysDialogOk(MainActivity.this, "提示", "读取电子证照成功\n耗时:" + (ecEndTime - ecStartTime) + "ms", "确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                break;
                            case READ_ELEC_CARD_FAILED:
                                com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "READ_ELEC_CARD_FAILED");
                                EIDCardOMAManager.getInstance(MainActivity.this).setOpenReadCardChannel(false);
                                appendLog(true, "读取电子证照失败，功能已关闭", Color.RED, true, false);
                                MyDialog.dismissWaitPro();
                                MyDialog.showSysDialogOk(MainActivity.this, "提示", "读取电子证照失败", "确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                break;
                        }
                    }

                    @Override
                    public void showMsg(String s) {

                    }
                });
                if (EIDCardOMAManager.getInstance(MainActivity.this).isOpenReadCardChannel()) {
                    EIDCardOMAManager.getInstance(MainActivity.this).setOpenReadCardChannel(false);
                    appendLog(true, "读取电子证照功能已关闭", Color.RED, false, false);
                } else {
                    EIDCardOMAManager.getInstance(MainActivity.this).setOpenReadCardChannel(true);
                    appendLog(true, "读取电子证照功能已开启", Color.RED, false, false);
                }
            }
        });
        main_read_ecard_id_carrier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getEidCarrier();
            }
        });
        main_read_ecard_pwd_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readEidCardPwdInfo();
            }
        });
        main_verify_ecard_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPin();
            }
        });
        main_change_ecard_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePIN();
            }
        });

        refreshECardButtons();

        changeDefaultPlaymentApp();
        EIDCardOMAManager.setSDKLogState(true);


        switchButton= (com.suke.widget.SwitchButton)
                findViewById(R.id.switch_button);

        switchButton.setChecked(false);//设置为真，即默认为真
        switchButton.isChecked();//被选中
        switchButton.toggle();     //开关状态
        switchButton.toggle(true);//开关有动画
        switchButton.setShadowEffect(false);//禁用阴影效果
        switchButton.setEnabled(true);//false为禁用按钮
        switchButton.setEnableEffect(true);//false为禁用开关动画
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if(isChecked){
                    yangzhengmashuijishu.setVisibility(View.VISIBLE);
                    hasYangzhengma=true;
                    eidyangzhengma=getGUID();
                yangzhengmashuijishu.setText(eidyangzhengma);}else {
                    eidyangzhengma="";
                    yangzhengmashuijishu.setVisibility(View.GONE);
                }
            }
        });
        setSpinner();//显示下拉选择框

    }

    boolean hasYangzhengma=false;
    String  eidyangzhengma="";

    public static String getGUID() {
        StringBuilder uid = new StringBuilder();
        //产生16位的强随机数
        Random rd = new SecureRandom();
        for (int i = 0; i < 6; i++) {
            //产生0-2的3位随机数
            uid.append(rd.nextInt(10));
        }
        return uid.toString();
    }

    /**
     * 显示操作结果
     *
     * @param s 需要显示的字信息
     */
    public void setShowText(String s, boolean append) {
        isNeedLog=false;
        TMKeyLog.d(TAG, "setShowText>>>:" + s);
        if ("".equals(s.replaceAll(" ", ""))) {
            return;
        }
        if (append) {
            main_showResult_tv.append("\n" + s);
        } else {
            main_showResult_tv.setText(s);
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {      //判断标志位
                case 0:
                    /**
                     获取数据，更新UI
                     */

                    if(isNeedLog){
                    main_showResult_tv.setText(TMKeyLog.sbf.toString());}

                    break;
                    case 1:
                    /**
                     获取数据，更新UI
                     */

                    main_showResult_tv.setText("bip返回信息:"+msg.obj);
                    isNeedLog=false;
                    break;
                case 2:
                    /**
                     获取数据，更新UI
                     */

                    main_showResult_tv.setText("");

                    break;
                case 3:
                    /**
                     获取数据，更新UI
                     */

                    main_showResult_tv.setText(msg.obj.toString());
                    isNeedLog=false;
                    break;




            }
        }
    };



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bip_getcardno:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{

                            Message msg =Message.obtain();
                            msg.what=2;   //标志消息的标志
                            handler.sendMessage(msg);

                            TMKeyLog.i(TAG,"bip 连接请求");
                          /*  ArrayList<BipResult> carNo=BipManager.getInstance().getInstructResultNewForLast("B11000000C");
                            BipResult bipResult=carNo.get(0);
                            if(bipResult.getSuccess()){
                                ArrayList<String>  arrayList=carNo.get(0).arrayList;
                                String carNos;
                                if(arrayList.get(0).length()>26){
                                    carNos=arrayList.get(0).substring(10,26);}else {
                                    carNos="卡片返回长度异常";
                                }

                                //从全局池中返回一个message实例，避免多次创建message（如new Message）
                                Message msgs =Message.obtain();
                                msgs.obj = carNos;
                                msgs.what=1;   //标志消息的标志
                                handler.sendMessage(msgs);
                            }else {

                                //从全局池中返回一个message实例，避免多次创建message（如new Message）
                                msg.obj = bipResult.getMessage();
                                msg.what=1;   //标志消息的标志
                                handler.sendMessage(msg);
                            }*/
                      /*      if(!isMobile(MainActivity.this)){
                                Message msgs =Message.obtain();
                                msgs.what=3;   //标志消息的标志
                                msgs.obj="bip返回信息：请先打开数据流量开关";
                                handler.sendMessage(msgs);
                                return;
                            }*/

                                 forceSendRequestByMobileData();
                        }catch (Exception e){
                            Message msg =Message.obtain();
                            msg.obj = "发生异常，异常信息"+e.getMessage();
                            msg.what=1;   //标志消息的标志
                            handler.sendMessage(msg);
                        }
                    }
                }).start();
            break;
            case R.id.bip_cosVersion:
                //TODO 待实现
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Client client = new Client(Constants.waiwangip, Constants.waiwnangport);
                        client.doSocket(Constants.randomInstruct, new ResultInterf() {
                            @Override
                            public void onResult(Object object) {
                                String random=((String) object).substring(6);
                                TMKeyLog.i(TAG,"mac连接请求,获取随机数====>:"+random);
                                String bipInstruct=Bip.getBipConnect("0833707000115223","FDC88FFA",random);
                                TMKeyLog.i(TAG,"bip连接请求指令:"+bipInstruct);
                                client.doSocket(bipInstruct, new ResultInterf() {
                                    @Override
                                    public void onResult(Object object) {
                                        String objectstring=(String)object;
                                        TMKeyLog.i(TAG,"BIP连接请求随机数====>:"+objectstring.substring(6,14));
                                        String apdukey=Bip.getAdpuKey("0833707000115223",objectstring.substring(6,14));  //会话秘钥
                                        TMKeyLog.i(TAG,"apdu会话秘钥====>:"+apdukey);
                                        String APDU=Bip.getAdpuInstruct(Constants.nxyInstructHead+Constants.getCos,apdukey); //获取版本号
                                        TMKeyLog.i(TAG,"apdu:"+APDU);
                                        String apduinstruct=Bip.getAdpuInstructMac("12"+APDU,apdukey);
                                        TMKeyLog.i(TAG,"发往服务器apdu指令码:"+apduinstruct);
                                        client.doSocket(apduinstruct, new ResultInterf() {
                                            @Override
                                            public void onResult(Object object) {
                                                String objectstring=(String)object;
                                                TMKeyLog.i(TAG," Apdu指令透传结果====>:"+object);
                                                Boolean macResult=Bip.jiaoyangmac(objectstring.substring(4,objectstring.length()-8),apdukey,objectstring.substring(objectstring.length()-8));
                                                TMKeyLog.i(TAG," mac比对结果====>:"+macResult);
                                                if(macResult==true){
                                                    String result=Bip.jiemiResult(objectstring.substring(6,objectstring.length()-8),apdukey);
                                                    TMKeyLog.i(TAG," 服务器返回值解密结果："+result);
                                                    //  main_showResult_tv.setText("cos版本号:"+result.substring(10,26));
                                                }else{
                                                    TMKeyLog.i(TAG," mac:对比失败");
                                                }
                                            }
                                        });

                                    }
                                });


                            }
                        });


                    }
                }).start();


            break;
            case R.id.inside_set_cos_version_btn:
//                MyDialog.showInputPINDialog(MainActivity.this,
//                        1,
//                        null,
//                        new InputDialogResult() {
//                            @Override
//                            public void success(String text) {
//                                try {
//                                    int ci = Integer.parseInt(text);
//                                    if (ci > 0xFF || ci < 1) {
//                                        setShowText("设置可更新Cos版本失败，请输入1-255之间的数字", false);
//                                        return;
//                                    }
//                                    VCardApi_FFT.curNeedUpdateCosVersoin = ci;
//                                    SharedPreferences.Editor editor = preferences.edit();
//                                    editor.putInt(COS_VERSION, VCardApi_FFT.curNeedUpdateCosVersoin);
//                                    editor.commit();
//                                    setShowText("设置可更新Cos成功，为:" + VCardApi_FFT.curNeedUpdateCosVersoin, false);
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                    setShowText("设置可更新Cos版本失败，请输入1-255之间的数字", false);
//                                }
//                            }
//
//                            @Override
//                            public void cancle() {
//                                setShowText("设置可更新COS版本操作取消", false);
//                            }
//                        }
//                );
                break;
            case R.id.inside_check_cos_version_btn:
                if (mVCardApiFFT == null) {
                    setShowText("检测COS版本更新操作，请先打开通道", false);
                    return;
                }
                MyDialog.showWaitPro(MainActivity.this, "正在检测COS版本是否可更新操作，请稍候");
                AppExecutors.getAppExecutors().postDiskIOThread(new Runnable() {
                    @Override
                    public void run() {
                        ((VCardApi_FFT) mVCardApiFFT).checkCosUpdatehttp(new CosUpdateCallBack() {
                            @Override
                            public void result(int errCode, final String result) {
                                AppExecutors.getAppExecutors().postMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MyDialog.dismissWaitPro();
                                        setShowText("检测COS版本更新操作:\n" + result, false);
                                    }
                                });
                            }
                        });
                    }
                });
                break;
            case R.id.inside_hasCard_btn:
                PermissionsUtil.requestContactPermission(new DefaultActivityPermissionListener(MainActivity.this) {
                    @Override
                    public void onGranted() {
                        super.onGranted();
                        PermissionsUtil.requestSmsPermission(new DefaultActivityPermissionListener(MainActivity.this) {
                            @Override
                            public void onGranted() {
                                super.onGranted();
                                PermissionsUtil.requestExternalStoragePermission(new DefaultActivityPermissionListener(MainActivity.this) {
                                    @Override
                                    public void onGranted() {
                                        super.onGranted();

                                        clickHasCard();
                                    }
                                });
                            }
                        });
                    }
                });
                break;
            case R.id.inside_popStk_btn:
                try {
                    setShowText("正在执行开启V盾确认操作，请稍候...", false);
                    com.micronet.bakapp.Result result1 = mVCardApiFFT.getVCardSignState();
                    String signState = result1.getState();
                    TMKeyLog.d(TAG, "getVCardSignState:" + signState);
                    if ("2000".equals(signState)) {
                        //TODO 需要关闭自动关闭功能
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mVCardApiFFT.sendPopSTK();
                                boolean canPopStk = mVCardApiFFT.isCanPopSTK();
                                TMKeyLog.d(TAG, "2000>>>sendPopSTK>>>canPopStk:" + canPopStk);
                            }
                        }).start();
                    } else {
                        if ("2001".equals(signState)) {
                            //TODO 需要开启自动关闭功能
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    mVCardApiFFT.sendPopSTK();
                                    boolean canPopStk = mVCardApiFFT.isCanPopSTK();
                                    TMKeyLog.d(TAG, "2001>>>sendPopSTK>>>canPopStk:" + canPopStk);
                                }
                            }).start();
                        } else if ("2002".equals(signState)) {
                            //V盾自动关闭功能已开启，同时V盾签名功能临时关闭，需要弹出STK菜单临时开启
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    mVCardApiFFT.callStkFunctionSetting(getApplication());
                                }
                            }).start();
                        }
                    }
                    setShowText("修改之前的状态:" + signState, false);
                    setShowText("现在状态:" + mVCardApiFFT.getVCardSignState().getState(), true);
                } catch (Exception e) {
                    e.printStackTrace();
                    setShowText("isPopSTKSubmitFlag IOException", false);
                }
                break;
            case R.id.inside_getSDKVer_btn:        //获取卡号
                inside_getSDKVer_btn.setEnabled(false);
                logStartTime();
                MyDialog.showWaitPro(MainActivity.this, RefResTool.getString(R.string.waitting));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logStartTime();
                        //获取卡片CSN
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setShowText("正在执行获取CSN操作，请稍候...", false);
                            }
                        });
                        Boolean isUseBip=main_isneed_bip.isChecked();
                        SharedPrefs.saveParameters(mSQApplication.getApplicationContext(),"isusebip",isUseBip.toString());

                        getBipConfig();


                        Result result = mVCardApiFFT.getCardNumber(main_isneed_bip.isChecked());
                        if (result.isFlag()) {
                            final String csnStr = result.getCardNo();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (csnStr == null || "".equals(csnStr)) {//获取CSN错误
                                        setShowText("CSN获取失败", false);
                                    } else {
                                        setShowText("CSN获取成功，为:" + csnStr, false);
                                    }
                                }
                            });
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showTotalTime("获取CSN信息");
                                logStartTime();
                            }
                        });
                        VCardApi_FFT.mBipEntity.setYangzhengma("");
                        result = mVCardApiFFT.getCardInfo();
                        if (result.isFlag()) {
                            final String cosVersion = result.getCosVersion();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (cosVersion == null || "".equals(cosVersion)) {//获取CSN错误
                                        setShowText("卡片信息获取失败", true);
                                    } else {
                                        setShowText("卡片信息获取成功，cosVersion为:" + cosVersion, true);
                                    }
                                }
                            });

                            final String appVersion = result.getAppVersion();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (appVersion == null || "".equals(appVersion)) {//获取CSN错误
                                        setShowText("卡片信息获取失败", true);
                                    } else {
                                        setShowText("卡片信息获取成功，appVersion为:" + appVersion, true);
                                    }
                                }
                            });
                            final String interVersion = result.getInterfaceVersion();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (interVersion == null || "".equals(interVersion)) {//获取CSN错误
                                        setShowText("卡片信息获取失败", true);
                                    } else {
                                        setShowText("卡片信息获取成功，interVersion为:" + interVersion, true);
                                    }
                                }
                            });
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showTotalTime("获取卡片信息");
                                inside_getSDKVer_btn.setEnabled(true);
                                MyDialog.dismissWaitPro();
                            }
                        });
                        modifyBip();
                    }
                }).start();
                break;
            case R.id.inside_getRSA2048Cer_btn:
                //获取证书信息
                logStartTime();
                Result r2 = mVCardApiFFT.getX509();
                if (r2 != null && r2.isFlag()) {
                    X509Certificate x509Certificate = r2.getX509Certificate();
                    setShowText("获取RSA证书信息成功" +
                                    "\n 证书序列号：" + FCharUtils.showResult16Str(x509Certificate.getSerialNumber().toByteArray()) +
                                    "\n 版本号：" + x509Certificate.getVersion() +
                                    "\n 颁发者名称：" + x509Certificate.getIssuerDN().getName() +
                                    "\n 颁发者ID：" + x509Certificate.getSigAlgOID() +
                                    "\n 有效起始时间：" + sdf.format(x509Certificate.getNotBefore()) +
                                    "\n 有效中止时间：" + sdf.format(x509Certificate.getNotAfter()) +
                                    "\n 证书持有者：" + x509Certificate.getSubjectDN().getName() +
                                    "\n 公钥：" + FCharUtils.bytesToHexStr(x509Certificate.getPublicKey().getEncoded())
                            , false);
                } else {
                    setShowText(r2.getMessage(), false);
                }
                showTotalTime("获取RSA证书信息");
                VCardApi_FFT.mBipEntity.setYangzhengma("");
                String rs = mVCardApiFFT.getCertTime(IVCardApiInterface.SM2, IVCardApiInterface.SM3);
                TMKeyLog.d(TAG, "getCertTime:" + rs);
                break;
            case R.id.inside_resetpin_btn:
              //  inside_resetpin_btn.setEnabled(false);
                MyDialog.showWaitPro(MainActivity.this, RefResTool.getString(R.string.waitting));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logStartTime();
                        Boolean isUseBip=main_isneed_bip.isChecked();
                        SharedPrefs.saveParameters(mSQApplication.getApplicationContext(),"isusebip",isUseBip.toString());
                       getBipConfig();
                        final String cipher = mVCardApiFFT.getCiphertext();
                        if (cipher == null || cipher.length() < 100) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MyDialog.dismissWaitPro();
                                    setShowText("重置密码获取签名数据:" + cipher, false);
                                    modifyBip();

                                }
                            });
                            return;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setShowText("重置密码获取签名数据:" + cipher, false);
                                modifyBip();
                            }
                        });
                        //TODO RSA解密
                        String rsaDecStr = VRSAUtilAction.execute(cipher);
                        if (rsaDecStr != null && rsaDecStr.contains("_")) {
                            String[] kvs = rsaDecStr.split("_");
                            //TODO 3DES加密
                            VCardApi_FFT.mBipEntity.setYangzhengma("");
                            String des3EncStr = VDesCryptAction.execute(kvs[0], kvs[1]);
                            if (des3EncStr != null && !"".equals(des3EncStr)) {
                                Result resetPwdResult = mVCardApiFFT.resetPwd(des3EncStr);
                                if (resetPwdResult != null) {
                                    boolean resetRes = resetPwdResult.isFlag();
                                    if (resetRes) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                setShowText("重置密码成功", false);
                                                showTotalTime("重置密码操作完成");
                                                inside_resetpin_btn.setEnabled(true);
                                                MyDialog.dismissWaitPro();
                                            }
                                        });
                                        return;
                                    }else {
                                        MyDialog.showMyDialogNoTitleOk(MainActivity.this, "重置密码失败", RefResTool.getString(R.string.app_ok), new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                d.dismiss();
                                                finish();
                                            }
                                        });
                                    }
                                }
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setShowText("重置密码失败", false);
                                showTotalTime("重置密码操作完成");
                                inside_resetpin_btn.setEnabled(true);
                                MyDialog.dismissWaitPro();
                                modifyBip();
                            }
                        });
                    }
                }).start();
                break;
            case R.id.inside_changepin_btn:
                //修改PIN码
                if(!main_isneed_bip.isChecked()){
                    isUseBip=false;
                }
                Boolean isUseBip=main_isneed_bip.isChecked();
                SharedPrefs.saveParameters(mSQApplication.getApplicationContext(),"isusebip",isUseBip.toString());
                Intent it = new Intent();
                it.putExtra("bipDatas",bipDatas);
                it.putExtra("typeFlag",typeFlag);
                it.putExtra("eidyangzhengma",eidyangzhengma);
                it.setClass(MainActivity.this, UpdatePwdActivity.class);
                startActivity(it);
                break;
            case R.id.inside_getErrorCode_btn:
                mVCardApiFFT.getErrorStateCode();
                break;
            case R.id.inside_sign_RSA_1024_btn:
                String signSrcStr = main_input_et.getText().toString();
                if (TextUtils.isEmpty(signSrcStr)) {
                    setShowText("请先输入签名原文", false);
                    return;
                }
//                String signSrcStr = "付款账号:6231810010802353312;收款账号:6231810010802;付款账号:6231810010802353312;收款账号:6231810010802;";
                TMKeyLog.newSbf();
                showSignVerifyPinView(SIGN_TYPE_RSA_1024, signSrcStr);
                break;
            case R.id.inside_sign_RSA_2048_btn:
                String signSrcStr1 = main_input_et.getText().toString();
                getBipConfig();
                if (TextUtils.isEmpty(signSrcStr1)) {
                    setShowText("请先输入签名原文", false);
                    return;
                }
                //组装需要签名的数据
//               String signSrcStr1 = "转出账号:6231 8100 1080 2353 312;转入账号:6231 8100 1080 2353 320;转入户名:手机二八;金额:0.13;VK_SN:833762400000012;GMFlag:2;0999151082454524";
//                String signSrcStr1 = "付款账号:6231810010802353312;收款账号:6231810010802;付款账号:6231810010802353312;收款账号:6231810010802;";
//                int t = new Random().nextInt();
//                signSrcStr += t;
//                String tst = "2D353839373736373730";
//                signSrcStr += FCharUtils.hexStr2String(tst, "UTF-8");
//               String signSrcStr = "<?xml version='1.0' encoding='utf-8'?><t><d><M><k>交易类型:</k><v>活期转定期</v></M><M><k>账号:</k><v>6231 8101 6490 0024 760</v></M><M><k>转存金额:</k><v>50.00</v></M></d></t>";
//               String signSrcStr = "转出帐号:6231 8100 1070 1904 538;转入帐号:6231 8100 1070 1108 197;转入户名:薄骥伟;金额:2.00;VK_SN:833762400000015;GMFlag:2;185149982960176E";
                TMKeyLog.newSbf();
                showSignVerifyPinView(SIGN_TYPE_RSA_2048, signSrcStr1);
                break;
            case R.id.inside_sign_SM2_btn:
                String signSrcStr2 = main_input_et.getText().toString();
                if (TextUtils.isEmpty(signSrcStr2)) {
                    setShowText("请先输入签名原文", false);
                    return;
                }
                //组装需要签名的数据
//               String signSrcStr = "转出账号:6231 8100 1080 2353 312;转入账号:6231 8100 1080 2353 320;转入户名:手机二八;金额:0.13;VK_SN:833762400000012;GMFlag:2;0999151082454524";
//                String signSrcStr2 = "付款账号:6230550400000082;缴费类型:水费;金额:126.00;VK_SN:833715200000003;GMFlag:2;154829585585";
//               String signSrcStr = "<?xml version='1.0' encoding='utf-8'?><t><d><M><k>交易类型:</k><v>活期转定期</v></M><M><k>账号:</k><v>6231 8101 6490 0024 760</v></M><M><k>转存金额:</k><v>50.00</v></M></d></t>";
//               String signSrcStr = "转出帐号:6231 8100 1070 1904 538;转入帐号:6231 8100 1070 1108 197;转入户名:薄骥伟;金额:2.00;VK_SN:833762400000015;GMFlag:2;185149982960176E";
                showSignVerifyPinView(SIGN_TYPE_SM2, signSrcStr2);
                break;
            case R.id.inside_writeHashCode_btn:
                showWriteHashVerifyPinView();
                break;
            case R.id.inside_readHashCode_btn:
                MyDialog.showWaitPro(MainActivity.this, "正在读取Hash值，请稍候...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logStartTime();
                        final Result result = ((VCardApi_FFT) mVCardApiFFT).readHashCode();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MyDialog.dismissWaitPro();
                                if (result.isFlag()) {//更新HashCode成功
                                    String hashCode = result.getMessage();
                                    if (hashCode.length() < 160) {
                                        setShowText("读取HashCode失败，数据长度错误", false);
                                        return;
                                    }
                                    String yidong = hashCode.substring(0, 40);
                                    String liantong = hashCode.substring(40, 80);
                                    String dianxin = hashCode.substring(80, 120);
                                    String app = hashCode.substring(120);
                                    setShowText("读取HashCode成功，"
                                                    + "\n移动:" + yidong
                                                    + "\n联通:" + liantong
                                                    + "\n电信:" + dianxin
                                                    + "\nAPP:" + app,
                                            false
                                    );
                                } else {
                                    setShowText("Hash值读取失败，错误码：" + result.getMessage(), false);
                                }
                                showTotalTime("签名操作完成");
                            }
                        });
                    }
                }).start();
                break;
            case R.id.init_card:
                showInitCardVerifyPinView();
                break;
            case R.id.create_p10:
                MyDialog.showCheckBoxDialog(MainActivity.this, 0, new SelectCertDialogResult() {
                    @Override
                    public void success(final Object ckt, final Object hashTys) {
                        MyDialog.showWaitPro(MainActivity.this, "正在创建密钥对，请稍候...");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBipConfig();
                                logStartTime();

                                Boolean isUseBip=main_isneed_bip.isChecked();
                                SharedPrefs.saveParameters(mSQApplication.getApplicationContext(),"isusebip",isUseBip.toString());
                                final Map<Integer, Result> results = mVCardApiFFT.createCertP10((ArrayList<Integer>) ckt, (ArrayList<Integer>) hashTys);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MyDialog.dismissWaitPro();
                                        if (results == null || results.isEmpty()) {
                                            setShowText("创建密钥对失败，条件有误", false);
                                        } else {
                                            Set<Integer> set = results.keySet();
                                            Iterator<Integer> iterator = set.iterator();
                                            int ck = VCardApi_FFT.RSA;
                                            Result result = null;
                                            StringBuffer sbf = new StringBuffer();
                                            while (iterator.hasNext()) {
                                                ck = iterator.next();
                                                result = results.get(ck);
                                                if (ck == VCardApi_FFT.RSA) {
                                                    if (result.isFlag()) {//创建P10成功
                                                        String pubKeyStr = result.getMessage();
                                                        sbf.append("创建RSA密钥对成功，P10信息为：" + pubKeyStr);
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    } else { //创建密钥对失败
                                                        sbf.append("创建RSA密钥对失败，错误信息：" + result.getMessage());
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    }
                                                } else if (ck == VCardApi_FFT.SM2) {
                                                    if (result.isFlag()) {//创建密钥对成功
                                                        String pubKeyStr = result.getMessage();
                                                        sbf.append("创建SM2密钥对成功，P10信息为：" + pubKeyStr);
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    } else { //创建密钥对失败
                                                        sbf.append("创建SM2密钥对失败，错误信息：" + result.getMessage());
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    }
                                                }
                                            }
                                            setShowText(sbf.toString(), false);
                                        }
                                        showTotalTime("创建密钥对操作完成");
                                        modifyBip();
                                    }
                                });
                            }
                        }).start();
                    }

                    @Override
                    public void cancle() {
                        setShowText("创建密钥对操作被取消", false);
                    }
                });
                break;
            case R.id.write_cert:
                MyDialog.showCheckBoxDialog(MainActivity.this, 1, new SelectCertDialogResult() {
                    @Override
                    public void success(final Object ct, final Object hashTys) {
                        MyDialog.showWaitPro(MainActivity.this, "正在写入证书信息，请稍候...");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                logStartTime();
                                getBipConfig();
                                Boolean isUseBip=main_isneed_bip.isChecked();
                                SharedPrefs.saveParameters(mSQApplication.getApplicationContext(),"isusebip",isUseBip.toString());
                                Map<Integer, byte[]> cts = (Map<Integer, byte[]>) ct;
                                String encCertPriKeys = null;
                                String encProKeys = null;
                                if (cts.containsKey(VCardApi_FFT.SM2_ENC)) {
                                    String certPriKey = "0000000000000000000000000000000000000000000000000000000000000000020845E47E653CEC89BA021D7282AB8CD243469D4B95D8E4E56080F8AB85EBEA";//加密证书私钥
                                    String proKey = "3082026C30820211A003020102020510";//保护密钥
                                    TMKeyLog.d(TAG, "write_cert>>>certPriKey:" + certPriKey);

                                    //通过SM4-ECB模式加密私钥
                                    byte[] encCertPriKeyB = SM4Util.getInstance().sms4_ecb(FCharUtils.hexString2ByteArray(certPriKey), FCharUtils.hexString2ByteArray(proKey), SM4Util.ENCRYPT, false, 0);//加密证书私钥密文
                                    encCertPriKeys = Base64.encodeToString(encCertPriKeyB, Base64.NO_WRAP);
                                    TMKeyLog.d(TAG, "write_cert>>>encCertPriKey:" + FCharUtils.showResult16Str(encCertPriKeyB));
//                                    //通过签名证书公钥加密保护密钥
                                    String sm2SignPubKey = TmKeyManager.sm2SignPubKey;
                                    TMKeyLog.d(TAG, "write_cert>>>sm2SignPubKey:" + sm2SignPubKey);
                                    if (TextUtils.isEmpty(sm2SignPubKey)) {//签名证书公钥为空
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                MyDialog.dismissWaitPro();
                                                setShowText("导入SM2加密证书失败，错误信息：请先创建SM2签名证书公私钥对", false);
                                            }
                                        });
                                        return;
                                    }
                                    byte[] encProKeyB = SM2Util.encrypt(FCharUtils.hexString2ByteArray(sm2SignPubKey), FCharUtils.hexString2ByteArray(proKey),2);//加密证书私钥保护密钥密文
                                    encProKeys = Base64.encodeToString(encProKeyB, Base64.NO_WRAP);
                                    TMKeyLog.d(TAG, "write_cert>>>encProKey:" + FCharUtils.showResult16Str(encProKeyB));
//                                    encCertPriKeys = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABhE+vDiTzStc+yfAj2QEGdNEse7eNH3W10gzIePhq8Ew==";
//                                    encProKeys = "wtwiAq0JYzBS/yKiGvJUSc9bSNiNGknX3Wq45D33kNT9nGHWlYS59bimQs24TRWk0gCv22WXebvqFc8+IR90VBAx0eMEVE1q3ZY+neZueUwzrA7h2E9z4NSlvrgSp565EAAAAHrVHi6rzQC9tahO9V6+xc0=";
                                }
                                final Map<Integer, Result> results = mVCardApiFFT.importCert((Map<Integer, byte[]>) ct, encCertPriKeys, encProKeys);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MyDialog.dismissWaitPro();
                                        if (results == null || results.isEmpty()) {
                                            setShowText("写入证书失败，条件有误", false);
                                        } else {
                                            Set<Integer> set = results.keySet();
                                            Iterator iterator = set.iterator();
                                            int ckt = 0;
                                            Result result = null;
                                            StringBuffer sbf = new StringBuffer();
                                            while (iterator.hasNext()) {
                                                ckt = (Integer) iterator.next();
                                                result = results.get(ckt);
                                                if (ckt == VCardApi_FFT.RSA) {
                                                    if (result.isFlag()) {//导入证书成功
                                                        sbf.append("导入RSA证书成功");
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    } else { //导入证书失败
                                                        sbf.append("导入RSA证书失败，错误信息：" + result.getMessage());
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    }
                                                } else if (ckt == VCardApi_FFT.SM2) {
                                                    if (result.isFlag()) {//导入证书成功
                                                        sbf.append("导入SM2签名证书成功");
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    } else { //导入证书失败
                                                        sbf.append("导入SM2签名证书失败，错误信息：" + result.getMessage());
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    }
                                                } else if (ckt == VCardApi_FFT.SM2_ENC) {
                                                    if (result.isFlag()) {//导入证书成功
                                                        sbf.append("导入SM2加密证书成功");
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    } else { //创建密钥对失败
                                                        sbf.append("导入SM2加密证书失败，错误信息：" + result.getMessage());
                                                        sbf.append("\n");
                                                        sbf.append("\n");
                                                    }
                                                }
                                            }
                                            setShowText(sbf.toString(), false);
                                        }
                                        showTotalTime("导入证书操作完成");
                                        modifyBip();
                                    }
                                });
                            }
                        }).start();
                    }

                    @Override
                    public void cancle() {
                        setShowText("导入证书操作被取消", false);
                    }
                });
                break;

        }
    }

    /**
     * 验证签名
     *
     * @param pKey
     * @param ssd  原文
     * @param sd   签名值
     * @param st   签名类型 1--RSA1024, 2--RSA2048, 3--SM2
     * @return
     */
    public boolean verifySign(String pKey, String ssd, String sd, int st) {
        if (pKey == null || "".equals(pKey)) {
            return false;
        }
        if (ssd == null || "".equals(ssd)) {
            return false;
        }
        if (sd == null || "".equals(sd)) {
            return false;
        }
        pKey = pKey.replaceAll(" ", "");
        ssd = ssd.replaceAll(" ", "");
        sd = sd.replaceAll(" ", "");
        byte[] pbKey = FCharUtils.hexString2ByteArray(pKey);
        if (pbKey == null) {
            return false;
        }
        try {
            if (st == 1) {
                //TODO 未实现
            } else if (st == 2) { //RSA2048
                TMKeyLog.d(TAG, "pKey.length:" + pKey.length() + "\npKey:" + pKey);
                pbKey = RSAUtils.packRsa2048PublicKey(pKey);
                TMKeyLog.d(TAG, "pbKey:" + FCharUtils.showResult16Str(pbKey));
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(pbKey));

//                //公钥对签名值加密获取到摘要原文
//                Cipher cipher = Cipher.getInstance("RSA");
//                cipher.init(Cipher.ENCRYPT_MODE, pubKey);
//                byte[] cipherText = cipher.doFinal(FCharUtils.hexString2ByteArray(sd));
//                TMKeyLog.d(TAG, "cipherText:" + FCharUtils.showResult16Str(cipherText));

                Signature signature = Signature
                        .getInstance("SHA1withRSA");

                signature.initVerify(pubKey);
                signature.update(FCharUtils.hexString2ByteArray(ssd));

                boolean bverify = signature.verify(FCharUtils.hexString2ByteArray(sd));
                TMKeyLog.d(TAG, "bverify:" + bverify);
                return bverify;
            } else {

            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 刷新button的状态
     */
    public void refreshButtons() {
        if (insideUsableCard) {
            inside_hasCard_btn.setEnabled(false);
            inside_check_cos_version_btn.setEnabled(true);
            inside_popStk_btn.setEnabled(true);
            inside_getSDKVer_btn.setEnabled(true);
            inside_getRSA2048Cer_btn.setEnabled(true);
            inside_changepin_btn.setEnabled(true);
            inside_resetpin_btn.setEnabled(true);
            inside_getErrorCode_btn.setEnabled(true);
            inside_sign_RSA_1024_btn.setEnabled(true);
            inside_sign_RSA_2048_btn.setEnabled(true);
            inside_sign_SM2_btn.setEnabled(true);
            inside_writeHashCode_btn.setEnabled(true);
            inside_readHashCode_btn.setEnabled(true);
            init_card.setEnabled(true);
            create_key_pair.setEnabled(true);
//            create_p10.setEnabled(true);
//            create_cert.setEnabled(true);
//            write_cert.setEnabled(true);
        } else {
            inside_hasCard_btn.setEnabled(true);
            inside_check_cos_version_btn.setEnabled(false);
            inside_popStk_btn.setEnabled(false);
            inside_getSDKVer_btn.setEnabled(false);
            inside_getRSA2048Cer_btn.setEnabled(false);
            inside_changepin_btn.setEnabled(false);
            inside_resetpin_btn.setEnabled(false);
            inside_getErrorCode_btn.setEnabled(false);
            inside_sign_RSA_1024_btn.setEnabled(false);
            inside_sign_RSA_2048_btn.setEnabled(false);
            inside_sign_SM2_btn.setEnabled(false);
            inside_writeHashCode_btn.setEnabled(false);
            inside_readHashCode_btn.setEnabled(false);
            init_card.setEnabled(false);
            create_key_pair.setEnabled(false);
            create_p10.setEnabled(false);
            create_cert.setEnabled(false);
            write_cert.setEnabled(false);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitBy2Click(MainActivity.this);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 按2次返回退出函数
     */
    private static Boolean isExit = false;

    public void exitBy2Click(Context context) {
        if (!isExit) {
            isExit = true;// 准备退出
            Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show();
            Timer mtTimer = new Timer();
            mtTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // 取消退出
                }
            }, 2000); // 如果2秒内没有按下返回键，则启动定时器取消刚才执行的任务
        } else {
            if (mVCardApiFFT != null) {
                mVCardApiFFT.close();
            }
            TMKeyLog.d(TAG, "finish...");
            ((Activity) context).finish();
//            System.exit(0);
        }
    }


    private long startTime, endTime;

    //获取当前时间
    private long getCurrentTime() {
        return new Date().getTime();
    }

    private void logStartTime() {
        startTime = getCurrentTime();
    }

    /**
     * 显示总共所花时间
     *
     * @param s 提示语
     * @return
     */
    private void showTotalTime(String s) {
        endTime = getCurrentTime();
        if (s != null && !"".equals(s)) {
            s += ",";
        }
        setShowText(s + "总共耗时:" + (endTime - startTime), true);
        TMKeyLog.e(TAG, s + ">>>endTime:" + endTime + ">>>totalTime:" + (endTime - startTime));
        startTime = 0;
        endTime = 0;
    }

    private void clickHasCard() {
        if(SIMBaseManager.isNeedBip){
            String      phone=input_phone.getText().toString();
            String     cardNo=input_cardno.getText().toString();
        if(phone.equals("")&&cardNo.equals("")){
            setShowText("", false);
            setShowText("请先输入卡号或者手机号码!", false);
            return;
        }
        }
        TMKeyLog.d(TAG, "clickHasCard");
        inside_getSDKVer_btn.setEnabled(false);
        MyDialog.showWaitPro(MainActivity.this, RefResTool.getString(R.string.waitting));
        mVCardApiFFT = VCardApi_FFT.getInstance();
        new Thread(new Runnable() {
            @Override
            public void run() {
                getBipConfig();
                VCardApi_FFT.mBipEntity.setYangzhengma("");
                ConditionVariable mConditionVariable = new ConditionVariable();
                AppExecutors.getAppExecutors().postScheduledExecutorThread(new Runnable() {
                    @Override
                    public void run() {
                        mVCardApiFFT.init(getApplicationContext());
                    }
                });
                //加锁等待OMA和UICC通道打开结果返回(8S)
                mConditionVariable.close();
                mConditionVariable.block(15 * 1000);

                insideHasCard = mVCardApiFFT.hasVCard();
                TMKeyLog.d(TAG, "mVCardApiFFT.hasVCard>>>insideHasCard:" + insideHasCard);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TMKeyLog.d(TAG, "Inside has Card ---> " + insideHasCard);
                        setShowText("Inside has Card ---> " + insideHasCard, false);
                        TMKeyLog.d(TAG, "----------------- LOG -----------------");
                    //    setShowText(TMKeyLog.sbf.toString(), true);
                        TMKeyLog.sbf = new StringBuffer();
                    }
                });

                insideUsableCard = mVCardApiFFT.checkChannel();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TMKeyLog.d(TAG, "checkChannel ---> " + insideUsableCard);
                        setShowText("checkChannel ---> " + insideUsableCard, true);
                   //     setShowText(TMKeyLog.sbf.toString(), true);
                  //      TMKeyLog.sbf = new StringBuffer();
                    }
                });
                if (insideUsableCard) {//检测通道成功才发后面的指令
                    final boolean isInitPsw = mVCardApiFFT.isModPasswordOver();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TMKeyLog.d(TAG, "isModPasswordOver ---> " + isInitPsw);
                            setShowText("isModPasswordOver ---> " + isInitPsw, true);
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String tts = "未连接";
                        int transType = ((VCardApi_FFT) mVCardApiFFT).getTransType();
                        if (transType == CardConnState.CARDCONN_SUCCESS_BIP) {
                            tts = "bip";
                        }else if (transType == CardConnState.CARDCONN_SUCCESS_OMA) {
                            tts = "OMA";
                            if (SESDefaultHelper.selectResponse != null) {
                                tts = tts + "\ngetSelectResponse:" + FCharUtils.showResult16Str(SESDefaultHelper.selectResponse);
                            }
                        } else if (transType == CardConnState.CARDCONN_SUCCESS_SMS) {
                            tts = "SMS";
                            if (SMSHelper.isNeedShift) {//需要移位是C网
                                tts = tts + "\n网络制式：C网";
                            } else {
                                tts = tts + "\n网络制式：G网";
                            }
                        } else if (transType == CardConnState.CARDCONN_SUCCESS_ADN) {
                            tts = "ADN";
                            if (SMSHelper.isNeedShift) {//需要移位是C网
                                tts = tts + "\n网络制式：C网";
                            } else {
                                tts = tts + "\n网络制式：G网";
                            }
                        } else if (transType == CardConnState.CARDCONN_SUCCESS_SMS_CEN) {
                            tts = "SMSP";
                            if (SMSHelper.isNeedShift) {//需要移位是C网
                                tts = tts + "\n网络制式：C网";
                            } else {
                                tts = tts + "\n网络制式：G网";
                            }
                        } else if (transType == CardConnState.CARDCONN_SUCCESS_UICC) {
                            tts = "UICC";
                        }
                        TMKeyLog.d(TAG, "通信方式：" + tts);
                        TMKeyLog.d(TAG, "hasOMAPackage：" + TmKeyManager.getInstance().isHasOMAPackage());
                        TMKeyLog.d(TAG, "Cos版本号：" + SIMBaseManager.CardSmsVersion);
                        setShowText("通信方式：" + tts, true);
                        setShowText("hasOMAPackage：" + TmKeyManager.getInstance().isHasOMAPackage(), true);
                        setShowText("Cos版本号：" + SIMBaseManager.CardSmsVersion, true);
                     //   setShowText(TMKeyLog.sbf.toString(), true);
                        TMKeyLog.sbf = new StringBuffer();
                        refreshButtons();
                        MyDialog.dismissWaitPro();
                    }
                });
            }
        }).start();
    }

    /**
     * 显示签名PIN码输入框
     */
    private void showSignVerifyPinView(final int signType, final String signBeforeData) {
        FroadKeyboard pinKeyboard = new FroadKeyboard(this, main_root_layout);
        pinKeyboard.showKeyboard();
        pinKeyboard.setOnConfirmClickListener(new FroadKeyboard.OnConfirmClickListener() {
            @Override
            public void onClick(final String password) {
                logStartTime();//开始计时
                pinStr = password;
                MyDialog.showWaitPro(MainActivity.this, RefResTool.getString(R.string.waitting));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Boolean isUseBip=main_isneed_bip.isChecked();
                        SharedPrefs.saveParameters(mSQApplication.getApplicationContext(),"isusebip",isUseBip.toString());
                        getBipConfig();
                        if (signType == SIGN_TYPE_RSA_1024) {
                            signResult = mVCardApiFFT.getSign(MainActivity.this.getApplication(), signBeforeData, SM3.sm3Hash(FCharUtils.string2HexStr(pinStr)), VCardApi_FFT.RSA, VCardApi_FFT.SHA1);
                        } else if (signType == SIGN_TYPE_RSA_2048) {
                            signResult = mVCardApiFFT.getSign(MainActivity.this.getApplication(), signBeforeData, SM3.sm3Hash(FCharUtils.string2HexStr(pinStr)), VCardApi_FFT.RSA, VCardApi_FFT.SHA256);
                        } else if (signType == SIGN_TYPE_SM2) {
                            signResult = mVCardApiFFT.getSign(MainActivity.this.getApplication(), signBeforeData, SM3.sm3Hash(FCharUtils.string2HexStr(pinStr)), VCardApi_FFT.SM2, VCardApi_FFT.SM3);
                        }
                        if (signResult.isFlag()) {
                            final String signSrcData = signResult.getSignSrcDataName();
                            final String signData = signResult.getSignData();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (signType == SIGN_TYPE_RSA_1024) {
                                        setShowText("RSA1024签名处理", false);
                                    } else if (signType == SIGN_TYPE_RSA_2048) {
                                        setShowText("RSA2048签名处理", false);
                                    } else if (signType == SIGN_TYPE_SM2) {
                                        setShowText("SM2签名处理", false);
                                    } else {
                                        setShowText("RSA1024签名处理", false);
                                    }
                                    setShowText("签名原文>>>signSrcData:" + signSrcData + "\n签名密文>>>signData:" + signData, true);
                                    showTotalTime("签名操作完成");
                                    MyDialog.dismissWaitPro();
                                }
                            });
                        } else {
                            final String state = signResult.getState();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MyDialog.dismissWaitPro();
                                    if (signType == SIGN_TYPE_RSA_1024) {
                                        setShowText("RSA1024签名处理", false);
                                    } else if (signType == SIGN_TYPE_RSA_2048) {
                                        setShowText("RSA2048签名处理", false);
                                    } else if (signType == SIGN_TYPE_SM2) {
                                        setShowText("SM2签名处理", false);
                                    } else {
                                        setShowText("RSA1024签名处理", false);
                                    }
                                    if (state.equals("2001")) {
                                        setShowText("签名操作失败，您已开启V盾签名自动关闭功能，请按 ISPOPSTK 按钮开启V盾签名功能后再执行签名操作", true);
                                    } else if ("1001".equals(state) || "3000".equals(state)) {
                                        d = MyDialog.showMyDialogNoTitleOk(MainActivity.this, signResult.getMessage(), RefResTool.getString(R.string.app_ok), new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                if (d != null) {
                                                    d.dismiss();
                                                }
                                                if ("1001".equals(state)) {
                                                    showSignVerifyPinView(signType, signBeforeData);
                                                }
                                            }
                                        });
                                        d.show();
                                    } else {
                                        setShowText("签名操作失败，错误信息：" + signResult.getMessage(), true);
                                        setShowText(TMKeyLog.sbf.toString(), true);
                                    }
                                }
                            });
                        }

                        modifyBip();
                    }
                }).start();
            }
        });
    }

    /**
     * 显示签名PIN码输入框
     */
    private void showWriteHashVerifyPinView() {
        FroadKeyboard pinKeyboard = new FroadKeyboard(this, main_root_layout);
        pinKeyboard.showKeyboard();
        pinKeyboard.setOnConfirmClickListener(new FroadKeyboard.OnConfirmClickListener() {
            @Override
            public void onClick(final String password) {
                logStartTime();//开始计时
                pinStr = password;
                MyDialog.showWaitPro(MainActivity.this, "正在更新Hash值，请稍候...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logStartTime();
                        final Result result = ((VCardApi_FFT) mVCardApiFFT).writeHashCode(HASHCODE_HEBAOZHIFU,
                                HASHCODE_WOQIANBAO,
                                HASHCODE_YIZHIFU,
                                HASHCODE_APP,
                                SM3.sm3Hash(FCharUtils.string2HexStr(pinStr))
                        );
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MyDialog.dismissWaitPro();
                                if (result.isFlag()) {//更新HashCode成功
                                    setShowText("Hash值更新成功", false);
                                } else {
                                    setShowText("Hash值更新失败，" + result.getMessage(), false);
                                }
                                showTotalTime("签名操作完成");
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private void showInitCardVerifyPinView() {
        FroadKeyboard pinKeyboard = new FroadKeyboard(this, main_root_layout);
        pinKeyboard.showKeyboard();
        pinKeyboard.setOnConfirmClickListener(new FroadKeyboard.OnConfirmClickListener() {
            @Override
            public void onClick(final String password) {
                logStartTime();//开始计时
                pinStr = password;
                getBipConfig();
                MyDialog.showWaitPro(MainActivity.this, "正在初始化卡片，请稍候...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logStartTime();
                        Boolean isUseBip=main_isneed_bip.isChecked();
                        SharedPrefs.saveParameters(mSQApplication.getApplicationContext(),"isusebip",isUseBip.toString());



                        final Result result = mVCardApiFFT.initCard(SM3.sm3Hash(FCharUtils.string2HexStr(pinStr)));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MyDialog.dismissWaitPro();
                                if (result.isFlag()) {//初始化证书
                                    setShowText("卡片初始化成功", false);
                                    create_p10.setEnabled(true);
                                    write_cert.setEnabled(true);
                                } else {
                                    setShowText("卡片初始化失败，错误信息：" + result.getMessage(), false);
                                }
                                showTotalTime("卡片初始化完成");
                                modifyBip();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    /**
     * 打开电子证照通道
     */
    public void openECardChannel() {
        ecStartTime = getCurrentTime();
        EIDCardOMAManager.getInstance(this).openChannel(new EIDCardOMAManager.ResultCallback() {
            @Override
            public void onResult(int code, Object msg) {
                ecEndTime = getCurrentTime();
                if (code == EIDCardOMAManager.SUCCESS) {
                    appendLog(false, msg + "\n耗时:" + (ecEndTime - ecStartTime) + "ms", Color.BLACK, false, false);
                    ecHasCard = true;
                } else {                    appendLog(true, "OMA通道不可用,短信通道可用\n耗时:" + (ecEndTime - ecStartTime) + "ms", Color.RED, false, false);
                    ecHasCard = false;
                }
                refreshECardButtons();
            }
        });
    }

    /**
     * 检测是否已写入电子证照
     */
    public void checkEidCardInfo() {
        ecStartTime = getCurrentTime();
        EIDCardOMAManager.getInstance(this).checkEidCardInfo(new EIDCardOMAManager.ResultCallback() {
            @Override
            public void onResult(int code, Object msg) {
                EIDCardOMAManager.getInstance(MainActivity.this).closeChannel();//关闭通道
                ecEndTime = getCurrentTime();
                if (code == EIDCardOMAManager.SUCCESS) {
                    appendLog(false, "已经写入电子证照数据\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.BLACK, false, false);
                    ecHasECard = true;
                } else {
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "未写入电子证照数据，错误信息：" + msg);
                    appendLog(true, "未写入电子证照数据，错误信息：" + msg + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.RED, false, false);
                    ecHasECard = false;
                }
                refreshECardButtons();
            }
        });
    }

    /**
     * 获取载体标识
     */
    public void getEidCarrier() {
        ecStartTime = getCurrentTime();
        EIDCardOMAManager.getInstance(this).getEidCarrier(new EIDCardOMAManager.ResultCallback() {
            @Override
            public void onResult(int code, Object msg) {
                ecEndTime = getCurrentTime();
                EIDCardOMAManager.getInstance(MainActivity.this).closeChannel();//关闭通道
                if (code == EIDCardOMAManager.SUCCESS) {
                    String idCarrier = (String) msg;
                    String idHex = com.froad.eid.ecard.utils.FCharUtils.showResult16Str(Base64.decode(idCarrier.getBytes(), Base64.DEFAULT));
                    appendLog(false, "获取载体标识：" + idCarrier + ">>>Hex:" + idHex + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.BLACK, false, false);
                } else {
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "获取载体标识，错误信息：" + msg);
                    appendLog(true, "获取载体标识,错误信息：" + msg + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.RED, false, false);
                }
            }
        });
    }

    /**
     * 获取脱敏信息
     */
    public void readEidCardPwdInfo() {
        ecStartTime = getCurrentTime();
        EIDCardOMAManager.getInstance(this).readEidCardPwdInfo(new EIDCardOMAManager.ResultCallback() {
            @Override
            public void onResult(int code, Object msg) {
                ecEndTime = getCurrentTime();
                EIDCardOMAManager.getInstance(MainActivity.this).closeChannel();//关闭通道
                if (code == EIDCardOMAManager.SUCCESS) {
                    String[] strs = (String[]) msg;
                    String nameStr = strs[0];
                    String idStr = strs[7];
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "readEidCardPwdInfo>>>nameStr:" + nameStr + ">>>idStr:" + idStr);
                    appendLog(false, "脱敏信息读取成功，姓名：" + nameStr + "\n身份证：" + idStr + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.BLACK, false, false);
                } else {
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "脱敏信息读取失败，错误信息:" + msg);
                    appendLog(true, "脱敏信息读取失败,错误信息:" + msg + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.RED, false, false);
                }
            }
        });
    }

    /**
     * 校验PIN，不关闭通道
     */



    public void verifyPin() {
        ecStartTime = getCurrentTime();
        EIDCardOMAManager.getInstance(this).verifyPIN(new EIDCardOMAManager.ResultCallback() {
            @Override
            public void onResult(int code, Object msg) {
                ecEndTime = getCurrentTime();
                if (code == EIDCardOMAManager.SUCCESS) {
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "verifyPin success");
                    appendLog(false, "校验PIN成功" + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.BLACK, false, false);
                } else {
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "verifyPin fail");
                    appendLog(true, "校验PIN失败, 错误信息：" + msg + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.RED, false, false);
                }
            }
        });
    }

    public void changePIN() {
        ecStartTime = getCurrentTime();
        EIDCardOMAManager.getInstance(this).changePIN(new EIDCardOMAManager.ResultCallback() {
            @Override
            public void onResult(int code, Object msg) {
                EIDCardOMAManager.getInstance(MainActivity.this).closeChannel();//关闭通道
                ecEndTime = getCurrentTime();
                if (code == EIDCardOMAManager.SUCCESS) {
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "changePIN success");
                    appendLog(false, "修改PIN成功" + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.BLACK, false, false);
                } else {
                    com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "changePIN fail");
                    appendLog(true, "修改PIN失败, 错误信息：" + msg + "\n耗时：" + (ecEndTime - ecStartTime) + "ms", Color.RED, false, false);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void changeDefaultPlaymentApp() {
        try {
            final boolean supportNfc = NfcManager.getInstance().isSupportNfc();
            final boolean isDefault = NfcManager.getInstance().isDefaultServiceForCategory();
            if (supportNfc && !isDefault) {
                com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "initData: is not Default");
                Intent intent = new Intent();
                intent.setAction(CardEmulation.ACTION_CHANGE_DEFAULT);
                intent.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT,
                        new ComponentName(this, CardService.class));
                intent.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);
                final PackageManager manager = this.getPackageManager();
                final ResolveInfo info = manager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (info != null) {
                    startActivity(intent);
                }
            } else {
                com.froad.eid.ecard.utils.TMKeyLog.d(TAG, "initData: isDefault ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 刷新button的状态
     */
    public void refreshECardButtons() {
        if (ecHasCard) {
            main_check_ecard_channel.setEnabled(false);
            main_read_ecard_id_carrier.setEnabled(true);
            main_verify_ecard_pin.setEnabled(true);
            main_change_ecard_pin.setEnabled(true);

            main_check_ecard.setEnabled(true);
            if (ecHasECard) {
                main_read_ecard_pwd_info.setEnabled(true);
                main_open_read_ecard_channel.setEnabled(true);
            } else {
                main_read_ecard_pwd_info.setEnabled(false);
                main_open_read_ecard_channel.setEnabled(false);
            }
        } else {
            main_check_ecard_channel.setEnabled(true);
            main_read_ecard_id_carrier.setEnabled(false);
            main_verify_ecard_pin.setEnabled(false);
            main_change_ecard_pin.setEnabled(false);

            main_check_ecard.setEnabled(false);
            main_read_ecard_pwd_info.setEnabled(false);
            main_open_read_ecard_channel.setEnabled(false);
        }
    }

    /**
     * 显示log
     *
     * @param needColor
     * @param more
     * @param color
     * @param needAppend
     */
    private void appendLog(boolean needColor, String more, int color, boolean needAppend, boolean needShowTime) {
        ecLogBuilder = new SpannableStringBuilder();
        main_showResult_tv.append("\n");
        if (needColor) {
            ecLogBuilder.append(more);
            if (needShowTime) {
                ecLogBuilder.append("-->" + getCurrentTime());
            }
            int end = ecLogBuilder.length();
            ForegroundColorSpan redSpan = new ForegroundColorSpan(color);
            ecLogBuilder.setSpan(redSpan, 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            ecLogBuilder.append(more);
            if (needShowTime) {
                ecLogBuilder.append("-->" + getCurrentTime());
            }
        }
        if (needAppend) { //需要追加
            main_showResult_tv.append(ecLogBuilder);
        } else {
            main_showResult_tv.setText(ecLogBuilder);
        }
        main_showResult_tv.append("\n");

    }

    @OnClick({R.id.btn_support_fingerprint, R.id.btn_get_random_number, R.id.btn_sign, R.id.btn_setup_public_key_sm2, R.id.btn_setup_public_key_rsa1024, R.id.btn_setup_public_key_rsa2048})
    public void onViewClicked(View view) {

        if (!canAuthenticate(this)) {
            return;
        }
        if (!mVCardApiFFT.checkFingerSupport()) {
            Toast.makeText(this, "该贴膜卡暂不支持指纹功能", Toast.LENGTH_SHORT).show();
            return;

        }
        switch (view.getId()) {
            case R.id.btn_support_fingerprint:
                break;
            case R.id.btn_get_random_number:
//                final Result random = mVCardApiFFT.getFingerRandom();
//                if (random.isFlag()) {
//                    randomMessage = random.getMessage();
//                    TMKeyLog.d(TAG, "btn_get_random_number : randomMessage "+randomMessage);
//
//                    Toast.makeText(this, "获取随机数成功", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(MainActivity.this, "获取随机数失败", Toast.LENGTH_SHORT).show();
//                }

                break;
            case R.id.btn_sign:
                fingerprintVerifyManager.authenticate();
                break;
            case R.id.btn_setup_public_key_sm2:
                fingerprintVerifyManager = new FingerprintVerifyManager.Builder(MainActivity.this).keyType(CipherHelper.TYPESM2).callback(mCallback).build();
                CipherHelper.getInstance().deleteCurrentPublicKey(CipherHelper.TYPESM2);
                fingerprintVerifyManager.authenticate();
                break;
            case R.id.btn_setup_public_key_rsa1024:
                fingerprintVerifyManager = new FingerprintVerifyManager.Builder(MainActivity.this).keyType(CipherHelper.TYPERSA1024).callback(mCallback).build();
                CipherHelper.getInstance().deleteCurrentPublicKey(CipherHelper.TYPERSA1024);
                fingerprintVerifyManager.authenticate();
                break;
            case R.id.btn_setup_public_key_rsa2048:
                fingerprintVerifyManager = new FingerprintVerifyManager.Builder(MainActivity.this).keyType(CipherHelper.TYPERSA2048).callback(mCallback).build();
                CipherHelper.getInstance().deleteCurrentPublicKey(CipherHelper.TYPERSA2048);
                fingerprintVerifyManager.authenticate();
                break;
        }
    }

    private FingerprintCallback mCallback = new FingerprintCallback() {
        @Override
        public void onSucceeded() {
            final CipherHelper cipherHelper = CipherHelper.getInstance();
            final int mkeyType = cipherHelper.getMkeyType();
            if (!cipherHelper.isHasSetupKeyPair()) {
                TMKeyLog.d(TAG, "onSucceeded: ! isHasSetupKeyPair");
                setupRsaPublicKey(cipherHelper, mkeyType);
            } else {

                TMKeyLog.d(TAG, "onSucceeded: isHasSetupKeyPair");

                final Result random = mVCardApiFFT.getFingerRandom();
                if (random.isFlag()) {
                    String randomMessage = random.getMessage();
                    TMKeyLog.d(TAG, "btn_get_random_number : randomMessage " + randomMessage);

                    final byte[] signData = fingerprintVerifyManager.getSignData(FCharUtils.hexStrToBytes(randomMessage));
                    final String toHexStr = FCharUtils.bytesToHexStr(signData);

                    TMKeyLog.d(TAG, "publicKey signData toHexStr : " + toHexStr);

                    final Result result = mVCardApiFFT.verifyFinger(mkeyType, signData);
                    if (result.isFlag()) {
                        Toast.makeText(MainActivity.this, "贴膜卡签名认证成功 keyType: "+ mkeyType, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "贴膜卡签名认证失败 keyType: "+ mkeyType, Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(MainActivity.this, "获取随机数失败", Toast.LENGTH_SHORT).show();
                }


            }
        }

        @Override
        public void onFailed() {

        }

        @Override
        public void onCancel() {

        }
    };

    private void setupRsaPublicKey(CipherHelper cipherHelper, int keyType) {
        final PublicKey publicKey = cipherHelper.getPublicKey();
        final byte[] encoded = publicKey.getEncoded();

        final String toHexStr = FCharUtils.bytesToHexStr(encoded);
        TMKeyLog.d(TAG, "keyType : " + keyType + " publicKey encoded toHexStr : " + toHexStr);

        byte[] efffect = null;
        String efffectToHexStr;
        if (keyType == CipherHelper.TYPERSA1024) {
            efffect = new byte[128];
            System.arraycopy(encoded, 29, efffect, 0, 128);
        } else if (keyType == CipherHelper.TYPERSA2048) {
            efffect = new byte[256];
            System.arraycopy(encoded, 33, efffect, 0, 256);

        } else if (keyType == CipherHelper.TYPESM2) {
            efffect = new byte[64];
            System.arraycopy(encoded, 27, efffect, 0, 64);
        }
        efffectToHexStr = FCharUtils.bytesToHexStr(efffect);
        TMKeyLog.d(TAG, "keyType : " + keyType + " publicKey efffect toHexStr : " + efffectToHexStr);

        //TODO 需要弹框输入PIN

        FroadKeyboard pinKeyboard = new FroadKeyboard(this, main_root_layout);
        pinKeyboard.showKeyboard();
        byte[] finalEfffect = efffect;
        pinKeyboard.setOnConfirmClickListener(new FroadKeyboard.OnConfirmClickListener() {
            @Override
            public void onClick(String password) {
                try {
                    byte[] pinDataNxy = SM3.sm3Hash(password.getBytes("utf-8"));
                    setFingerPubKey(cipherHelper, keyType, finalEfffect, pinDataNxy);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void setFingerPubKey(CipherHelper cipherHelper, int keyType, byte[] efffect, byte[] pinData) {

        Result result = mVCardApiFFT.setFingerPubKey(keyType, efffect, pinData);

        if (result.isFlag()) {
            Toast.makeText(MainActivity.this, "贴膜卡设置公钥成功 keyType: "+keyType, Toast.LENGTH_SHORT).show();
        } else {
            cipherHelper.deleteCurrentPublicKey(keyType);
            Toast.makeText(MainActivity.this, "贴膜卡设置公钥失败 keyType: "+keyType, Toast.LENGTH_SHORT).show();
        }
    }


    public boolean canAuthenticate(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            Toast.makeText(context, "您的系统版本过低，不支持指纹功能", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            KeyguardManager keyguardManager = context.getSystemService(KeyguardManager.class);
            FingerprintManager fingerprintManager = context.getSystemService(FingerprintManager.class);
            if (!fingerprintManager.isHardwareDetected()) {
                Toast.makeText(context, "您的手机不支持指纹功能", Toast.LENGTH_SHORT).show();
                return false;
            } else if (!keyguardManager.isKeyguardSecure()) {
                Toast.makeText(context, "您还未设置锁屏，请先设置锁屏并添加一个指纹", Toast.LENGTH_SHORT).show();
                return false;
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(context, "您至少需要在系统设置中添加一个指纹", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }
}
