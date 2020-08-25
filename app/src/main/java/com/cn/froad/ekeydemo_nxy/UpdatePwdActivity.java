package com.cn.froad.ekeydemo_nxy;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.froad.ekeydemo_nxy.utils.MyDialog;
import com.cn.froad.ekeydemo_nxy.utils.RefResTool;
import com.froad.ukey.manager.SIMBaseManager;
import com.froad.ukey.manager.VCardApi_FFT;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM3;
import com.froad.ukey.utils.np.TMKeyLog;
import com.micronet.api.Result;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by FW on 2017/11/18.
 */

public class UpdatePwdActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.oldPwd_layout)
    public LinearLayout oldPwd_layout;
    @BindView(R.id.frag_update_oldPwd_edit)
    public EditText frag_update_oldPwd_edit;
    @BindView(R.id.frag_update_pass_new_pass)
    public EditText frag_update_pass_new_pass;
    @BindView(R.id.frag_update_pass_confirm_pass)
    public EditText frag_update_pass_confirm_pass;
    @BindView(R.id.frag_update_pass_confirm_btn)
    public TextView frag_update_pass_confirm_btn;

    private VCardApi_FFT vCardApi_fft;
    private Dialog d;
    private String oldPwdStr = "";


    private String bipDatas = "";
    private String typeFlag = "";
    private String eidyangzhengma = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vCardApi_fft = VCardApi_FFT.getInstance();

        setContentView(R.layout.activity_update_pwd);
        ButterKnife.bind( this );
        initViews();


       bipDatas=getIntent().getStringExtra("bipDatas");
        typeFlag=getIntent().getStringExtra("typeFlag");
        eidyangzhengma=getIntent().getStringExtra("eidyangzhengma");

        TMKeyLog.d("修改密码页面", "bip数据："+bipDatas);
        TMKeyLog.d("修改密码页面", "数据类型:"+typeFlag);
        TMKeyLog.d("修改密码页面", "eid 验证码："+eidyangzhengma);

    }

    protected void initViews() {
//        frag_update_oldPwd_edit = (EditText) findViewById(R.id.frag_update_oldPwd_edit);
//        frag_update_pass_new_pass = (EditText) findViewById(R.id.frag_update_pass_new_pass);
//        frag_update_pass_confirm_pass = (EditText) findViewById(R.id.frag_update_pass_confirm_pass);
//        frag_update_pass_confirm_btn = (TextView) findViewById(R.id.frag_update_pass_confirm_btn);
        frag_update_pass_confirm_btn.setOnClickListener(this);
        boolean isInitPwd = vCardApi_fft.isModPasswordOver();
        if (isInitPwd) {
            oldPwd_layout.setVisibility(View.GONE);
        } else {
            oldPwd_layout.setVisibility(View.VISIBLE);
        }
    }

    protected void modifyBIpData() {
        if(!SIMBaseManager.bipErrorStrig.equals("")){

            Toast.makeText(this, SIMBaseManager.bipErrorStrig, Toast.LENGTH_LONG).show();

        }
        SIMBaseManager.mybipResult=null;
        SIMBaseManager.bipErrorStrig="";
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.frag_update_pass_confirm_btn:
                oldPwdStr = "123456";
                if (oldPwd_layout.isShown()) {
                    oldPwdStr = frag_update_oldPwd_edit.getText().toString();
                    if ("".equals(oldPwdStr)) {
                        showToast("旧密码不能为空，请输入");
                        return;
                    }
                    if (oldPwdStr.length() != 6) {
                        showToast("请输入6位数字密码");
                        return;
                    }
                }
                final String pwdStr = frag_update_pass_new_pass.getText().toString();
                final String confirmPwdStr = frag_update_pass_confirm_pass.getText().toString();
                if ("".equals(pwdStr)) {
                    showToast("密码不能为空，请输入");
                    return;
                }
                if (pwdStr.length() != 6) {
                    showToast("请输入6位数字密码");
                    return;
                }
                if ("".equals(confirmPwdStr)) {
                    showToast("密码不能为空，请输入");
                    return;
                }
                if (confirmPwdStr.length() != 6) {
                    showToast("请输入6位数字密码");
                    return;
                }
                if (! pwdStr.equals(confirmPwdStr)) {
                    showToast("两次输入的密码不一致，请重新输入");
                    return;
                }
                MyDialog.showWaitPro(UpdatePwdActivity.this, RefResTool.getString(R.string.waitting));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        vCardApi_fft.setBipConfig(bipDatas,typeFlag,eidyangzhengma);
                        final Result result = vCardApi_fft.setPin(SM3.sm3Hash(FCharUtils.string2HexStr(oldPwdStr)), SM3.sm3Hash(FCharUtils.string2HexStr(pwdStr)),MainActivity.isUseBip);
                        vCardApi_fft.mBipEntity.setYangzhengma("");
                        //获取卡片CSN
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MyDialog.dismissWaitPro();
                                modifyBIpData();
                                if (result.isFlag()) {
                                    d = MyDialog.showMyDialogNoTitleOk(UpdatePwdActivity.this, result.getMessage(), RefResTool.getString(R.string.app_ok), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            d.dismiss();
                                            finish();
                                        }
                                    });
                                    d.show();
                                } else {
                                    if (result.getState().equals("1009")) {
                                        d = MyDialog.showMyDialogNoTitleOk(UpdatePwdActivity.this, "密码修改失败", RefResTool.getString(R.string.app_ok), new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                d.dismiss();
                                                finish();
                                            }
                                        });
                                        d.show();
                                    } else {
                                        showToast(result.getMessage());
                                    }
                                }
                            }
                        });
                    }
                }).start();
                break;
        }
    }
}
