package com.cn.froad.ekeydemo_nxy.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Base64;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cn.froad.ekeydemo_nxy.R;
import com.cn.froad.ekeydemo_nxy.interf.InputDialogResult;
import com.froad.ukey.interf.SelectCertDialogResult;
import com.froad.ukey.jni.GmSSL;
import com.froad.ukey.manager.VCardApi_FFT;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by FW on 2017/5/17.
 */
public class MyDialog {

    private final static String TAG = "MyDialog";
    private static ProgressDialog processDialog;
    private static Dialog dialog;

    /**
     * @param mContext
     * @param titleStr       标题
     * @param msgStr         消息
     * @param sureBtnStr     确定按钮文字
     * @param cancleBenStr   取消按钮文字
     * @param sureListener   确定按钮监听
     * @param cancleListener 取消按钮监听
     * @param isShowLine     是否显示分割线
     * @return
     */
    public static Dialog createPermissionDialog(Context mContext,
                                                String titleStr,
                                                String msgStr,
                                                String sureBtnStr,
                                                String cancleBenStr,
                                                View.OnClickListener sureListener,
                                                View.OnClickListener cancleListener,
                                                boolean isShowLine) {
        Dialog permiDialog = new Dialog(mContext, R.style.dextetPermissionDialog);
        permiDialog.setContentView(R.layout.dexter_dialog_reqpermission);
        RelativeLayout dialog_bg_layout = (RelativeLayout) permiDialog.findViewById(R.id
                .dialog_bg_layout);
        dialog_bg_layout.setBackgroundResource(R.drawable.dexter_froad_frame_perdialog_bg);
        LinearLayout permission_msg_layout = (LinearLayout) permiDialog.findViewById(R.id
                .permission_msg_layout);
        TextView permission_title_text = (TextView) permiDialog.findViewById(R.id
                .permission_title_text);
        ImageView permission_line = (ImageView) permiDialog.findViewById(R.id.permission_line);
        permission_title_text.setTextSize(18);
        if (isShowLine) {
            permission_line.setVisibility(View.VISIBLE);
        } else {
            permission_line.setVisibility(View.GONE);
        }
        TextView msgTextV = null;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        String[] ms = msgStr.split("\\|");
        int msl = ms.length;
        for (int i = 0; i < msl; i++) {
            if (ms[i] != null && !"".equals(ms[i])) {
                msgTextV = new TextView(mContext);
                msgTextV.setText(ms[i]);
                msgTextV.setTextSize(12);
                msgTextV.setTextColor(mContext.getResources().getColor(R.color.white));
                msgTextV.setPadding(8, 0, 0, 0);
                msgTextV.setLayoutParams(params);
                msgTextV.setBackgroundDrawable(null);
                permission_msg_layout.addView(msgTextV);
            }
        }

        Button agreeBtn = (Button) permiDialog.findViewById(R.id.permission_agree);
        Button refuseBtn = (Button) permiDialog.findViewById(R.id.permission_refuse);
        agreeBtn.setText(sureBtnStr);
        refuseBtn.setText(cancleBenStr);
        agreeBtn.setTextSize(14);
        refuseBtn.setTextSize(14);

        permission_title_text.setText(titleStr);
        agreeBtn.setOnClickListener(sureListener);
        refuseBtn.setOnClickListener(cancleListener);

        permiDialog.setCancelable(false);
        permiDialog.setCanceledOnTouchOutside(false);
        return permiDialog;
    }


    /**
     * 显示系统的alertDialog对话框
     *
     * @param mContext
     * @param title
     * @param msg
     * @param okBtnStr
     * @param okListener
     * @return
     */
    public static Dialog showSysDialogOk(Context mContext,
                                         String title,
                                         String msg,
                                         String okBtnStr,
                                         DialogInterface.OnClickListener okListener
    ) {
        AlertDialog.Builder b = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setPositiveButton(okBtnStr, okListener);
        if (msg != null && !msg.equals("")) {
            b.setMessage(msg);
        }

        AlertDialog d = b.create();
        d.setCancelable(false);
        d.setCanceledOnTouchOutside(false);
        return d;
    }

    /**
     *
     * @param mContext
     * @param msg
     * @param okBtnStr
     * @param okListener
     * @return
     */
    public static Dialog showMyDialogNoTitleOk(Context mContext,
                                         String msg,
                                         String okBtnStr,
                                         View.OnClickListener okListener
    ) {
        Dialog myDialog = new Dialog(mContext, R.style.dextetPermissionDialog);
        myDialog.setContentView(R.layout.dialog_notitle_ok);
        TextView dialog_msg_tv = (TextView) myDialog.findViewById(R.id
                .dialog_msg_tv);
        dialog_msg_tv.setText(msg);
        Button agreeBtn = (Button) myDialog.findViewById(R.id.dialog_agree);
        agreeBtn.setText(okBtnStr);

        agreeBtn.setOnClickListener(okListener);

        myDialog.setCancelable(false);
        myDialog.setCanceledOnTouchOutside(false);
        myDialog.show();
        return myDialog;
    }

    /**
     *
     * @param mContext
     * @param msg
     * @param okBtnStr
     * @param okListener
     * @return
     */
    public static Dialog showMyDialogNoTitleOkCancle(Context mContext,
                                         String msg,
                                         String okBtnStr,
                                         View.OnClickListener okListener,
                                         String cancleBtnStr,
                                         View.OnClickListener cancleListener
    ) {
        Dialog myDialog = new Dialog(mContext, R.style.dextetPermissionDialog);
        myDialog.setContentView(R.layout.dialog_notitle_ok_cancle);
        TextView dialog_msg_tv = (TextView) myDialog.findViewById(R.id
                .dialog_msg_tv);
        dialog_msg_tv.setText(msg);
        Button agreeBtn = (Button) myDialog.findViewById(R.id.dialog_agree);
        agreeBtn.setText(okBtnStr);
        Button cancleBtn = (Button) myDialog.findViewById(R.id.dialog_cancle);
        cancleBtn.setText(cancleBtnStr);

        agreeBtn.setOnClickListener(okListener);
        cancleBtn.setOnClickListener(cancleListener);

        myDialog.setCancelable(false);
        myDialog.setCanceledOnTouchOutside(false);
        myDialog.show();
        return myDialog;
    }

    /**
     * 显示系统的alertDialog对话框
     *
     * @param mContext
     * @param title
     * @param msg
     * @param okBtnStr
     * @param cancleBtnStr
     * @param okListener
     * @param cancleListener
     * @return
     */
    public static Dialog showSysDialogOkCancle(Context mContext,
                                               String title,
                                               String msg,
                                               String okBtnStr,
                                               String cancleBtnStr,
                                               DialogInterface.OnClickListener okListener,
                                               DialogInterface.OnClickListener cancleListener
    ) {
        AlertDialog.Builder b = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setPositiveButton(okBtnStr, okListener)
                .setNegativeButton(cancleBtnStr, cancleListener);
        if (msg != null && !msg.equals("")) {
            b.setMessage(msg);
        }

        AlertDialog d = b.create();
        d.setCancelable(false);
        d.setCanceledOnTouchOutside(false);
        return d;
    }

    /**
     * 带等待图标的等待框 参数message：需要显示给用户的字符串
     */
    public static void showWaitPro(final Activity mContext, final String message) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    processDialog = new ProgressDialog(
                            mContext);
                    processDialog.setIndeterminate(true);
                    processDialog.setCancelable(true);
                    processDialog.setCanceledOnTouchOutside(false);
                    processDialog.setMessage(message);
                    processDialog.show();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 隐藏等待框
     */
    public static void dismissWaitPro() {
        if (processDialog != null && processDialog.isShowing()) {
            processDialog.dismiss();
        }
    }

    /**
     *
     * @param mContext
     * @param type 0--创建证书P10， 1--写入证书
     * @param result
     * @return
     */
    public static Dialog showCheckBoxDialog (final Context mContext, final int type, final SelectCertDialogResult result) {

        // 获取Dialog布局
        final View view = LayoutInflater.from(mContext).inflate(R.layout.select_cert_dialog, null);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        // 获取自定义Dialog布局中的控件
        final LinearLayout hash_dialog_view = (LinearLayout) view.findViewById(R.id.hash_dialog_view);
        // 定义Dialog布局和参数
        dialog = new Dialog(mContext, R.style.froad_alert_dialog_style);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        // 调整dialog背景大小
        hash_dialog_view.setLayoutParams(new FrameLayout.LayoutParams((int) (display
                .getWidth() * 0.85), FrameLayout.LayoutParams.WRAP_CONTENT));
        final CheckBox cert_type_rsa = view.findViewById(R.id.cert_type_rsa);
        final RadioGroup cert_type_rsa_rg = view.findViewById(R.id.cert_type_rsa_rg);
        final RadioButton cert_type_rsa1024= view.findViewById(R.id.cert_type_rsa1024);
        final RadioButton cert_type_rsa2048= view.findViewById(R.id.cert_type_rsa2048);
        final CheckBox cert_type_sm2_sign= view.findViewById(R.id.cert_type_sm2_sign);
        final CheckBox cert_type_sm2_enc= view.findViewById(R.id.cert_type_sm2_enc);

        cert_type_rsa1024.setVisibility(View.GONE);//农信银证书不用考虑RSA1024证书
        if (type == 0) { //密钥对类型
            cert_type_sm2_enc.setVisibility(View.GONE);
            cert_type_rsa1024.setText("RSA1024密钥对");
            cert_type_rsa2048.setText("RSA2048密钥对");
            cert_type_sm2_sign.setText("SM2签名密钥对");
        } else { //证书类型
            cert_type_sm2_enc.setVisibility(View.VISIBLE);
            cert_type_rsa1024.setText("RSA1024证书");
            cert_type_rsa2048.setText("RSA2048证书");
            cert_type_sm2_sign.setText("SM2签名证书");
            cert_type_sm2_enc.setText("SM2加密证书");
        }

        Button      sureBtn= view.findViewById(R.id.hash_sure_btn);
        ImageView   dialog_close_img= view.findViewById(R.id.dialog_close_img);
        dialog_close_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (result != null) {
                    result.cancle();
                }
                dialog.dismiss();
            }
        });

        final ArrayList<Integer> list = new ArrayList<>();
        final ArrayList<Integer> hashTyslist = new ArrayList<>();
        final Map<Integer, byte[]> map = new HashMap<>();
        cert_type_rsa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TMKeyLog.d(TAG, "cert_type_rsa>>>isChecked:" + isChecked);
                if (isChecked) {
                    if (! cert_type_rsa2048.isChecked()) {
                        cert_type_rsa2048.toggle();
                    } else {
                        if (type == 0) {
                            list.add(VCardApi_FFT.RSA);
                            hashTyslist.add(VCardApi_FFT.SHA256);
                        } else {
                            String certData = "MIIECgYJKoZIhvcNAQcCoIID+zCCA/cCAQExADALBgkqhkiG9w0BBwGgggPfMIID2zCCAsOgAwIBAgIFAfi7fR8wDQYJKoZIhvcNAQELBQAwOjESMBAGA1UEAwwJUlNBQ0E2MjI0MRcwFQYDVQQKDA5PbmNlRG93bkNlcnRDQTELMAkGA1UEBhMCY24wHhcNMTkxMDIzMDI0NjM0WhcNMjAxMDIyMDI0NjM0WjBrMTUwMwYDVQQDDCwwNjNAMDM3MTIwMjE5OTcwOTI2NzQxMUBaaGFuZ3d1MDNAMTAwMDAwMDI2NTEUMBIGA1UECwwLRW50ZXJwcmlzZXMxDzANBgNVBAsMBjExMTExMTELMAkGA1UEBhMCY24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCAUt5v3HO67tA5jAKrmmPHEDkRrgjb/nIOS7tcx7bqY9wT0WpylDUOaJLp1wP1uXCvjBkXM7k42od2WazNdXXI9G03IJwUgTuuAtGkfHT0X+LpGVvh5ylUiTi1Rbj7BMQWr3JxOdZDVL+T6kT0r0Bqt6x8+7i/wF5hq+JaqZTAcp5usE4tkfR1ahTL1OrCv3uSJ68AbcpFh1DNEfYy4GLfBMeqMLZp8EFEbrV7V7AhrnIsz9llYhi34WnxjQKTv7I5ho6A9VJCFmYDm9oahBWLft6bvpsxLXyY6csXNgnmz0yCcd8wwMdQAncSQ1uSELRISfs+q8JpllN/tU9365wpAgMBAAGjgbYwgbMwHwYDVR0jBBgwFoAUmPVQ2tFv9AKd/52c5TqAraWNc9EwCQYDVR0TBAIwADBWBgNVHR8ETzBNMEugSaBHpEUwQzENMAsGA1UEAwwEY3JsMTEMMAoGA1UECwwDY3JsMRcwFQYDVQQKDA5PbmNlRG93bkNlcnRDQTELMAkGA1UEBhMCY24wDgYDVR0PAQH/BAQDAgbAMB0GA1UdDgQWBBTdyJeQFmznyVpEHrLD+iEc5BFEijANBgkqhkiG9w0BAQsFAAOCAQEAHTtrJag+JQUI331CRB9TNTfHlSdYeUBIzMXe393/iCjc7LUxfcP8FCvgQDTvAorlEtjgJ/oc0CQMjeXNnfbJPfy1IG12JqwZOdlYNqcrpaPJg6FwlJcPC+7zciqev+u7dgaALx3uNDvSsIdF9EXzJ3s9vbyae2gWKRxKTv47muz3jBkJ42Mo5ZMCWYAtpM36+c5LfsykWPO3Zzp0BMacvwncZrBx1pSaFz4GF6upkeR2wP83ogvblJPtiPFWnpSM85U8+z/5Mtv7F2wO1tm22qLI9eClSqjDv62bUGWVpVfh/A0+3kEJMqWogLJPQsQXacqz6ol3adbW34nh6ZaLQzEA";
                            byte[] certDataBytes = Base64.decode(certData, Base64.NO_WRAP);
                            TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>certDataBytes111:" + FCharUtils.bytesToHexStr(certDataBytes));
                            GmSSL gmSSL= new GmSSL();
                            long converRes = gmSSL.convertPkcs7ToPemHex(certDataBytes, certDataBytes.length);
                            TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>converRes:" + converRes);
                            if (converRes != 0) { //转换数据成功
                                certDataBytes = gmSSL.pemBytes;
                                TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>certDataBytes222:" + FCharUtils.bytesToHexStr(certDataBytes));
                                map.put(VCardApi_FFT.RSA, certDataBytes);
                            }
                        }
                    }
                } else {
                    if (cert_type_rsa1024.isChecked()) {
                        if (type == 0) {
                            if (list.contains(VCardApi_FFT.RSA)) {
                                int index = list.indexOf(VCardApi_FFT.RSA);
                                TMKeyLog.d(TAG, "cert_type_rsa1024>>>index:" + index);
                                if (index >= 0) {
                                    list.remove(index);
                                }
                                index = hashTyslist.indexOf(VCardApi_FFT.SHA1);
                                TMKeyLog.d(TAG, "cert_type_rsa1024>>>hashTyslist>>>index:" + index);
                                if (index >= 0) {
                                    hashTyslist.remove(index);
                                }
                            }
                        } else {
                            if (map.containsKey(VCardApi_FFT.RSA)) {
                                map.remove(VCardApi_FFT.RSA);
                            }
                        }
                    } else {
                        if (type == 0) {
                            if (list.contains(VCardApi_FFT.RSA)) {
                                int index = list.indexOf(VCardApi_FFT.RSA);
                                TMKeyLog.d(TAG, "cert_type_rsa2048>>>index:" + index);
                                if (index >= 0) {
                                    list.remove(index);
                                }
                                index = hashTyslist.indexOf(VCardApi_FFT.SHA256);
                                TMKeyLog.d(TAG, "cert_type_rsa2048>>hashTyslist>>>index:" + index);
                                if (index >= 0) {
                                    hashTyslist.remove(index);
                                }
                            }
                        } else {
                            if (map.containsKey(VCardApi_FFT.RSA)) {
                                map.remove(VCardApi_FFT.RSA);
                            }
                        }
                    }
                }
            }
        });

        cert_type_rsa_rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (! cert_type_rsa.isChecked()) {
                    TMKeyLog.d(TAG, "cert_type_rsa is not checked");
                    return;
                }
                if (checkedId == R.id.cert_type_rsa1024) {
                    if (type == 0) {
                        if (list.contains(VCardApi_FFT.RSA)) {
                            int index = list.indexOf(VCardApi_FFT.RSA);
                            TMKeyLog.d(TAG, "cert_type_rsa2048>>>index:" + index);
                            if (index >= 0) {
                                list.remove(index);
                            }
                            index = hashTyslist.indexOf(VCardApi_FFT.SHA256);
                            TMKeyLog.d(TAG, "cert_type_rsa2048>>hashTyslist>>>index:" + index);
                            if (index >= 0) {
                                hashTyslist.remove(index);
                            }
                        }
                        TMKeyLog.d(TAG, "cert_type_rsa1024 select ");
                        list.add(VCardApi_FFT.RSA);
                        hashTyslist.add(VCardApi_FFT.SHA1);
                    } else {
                        if (map.containsKey(VCardApi_FFT.RSA)) {
                            map.remove(VCardApi_FFT.RSA);
                        }
                        String certData = "MIIDaDCCAlCgAwIBAgIFEDZEaIcwDQYJKoZIhvcNAQEFBQAwWDELMAkGA1UEBhMCQ04xMDAuBgNVBAoTJ0NoaW5hIEZpbmFuY2lhbCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTEXMBUGA1UEAxMOQ0ZDQSBURVNUIE9DQTEwHhcNMTgxMjI1MDM1MDQ0WhcNMTkwMzI1MDM1MDQ0WjBfMQswCQYDVQQGEwJDTjENMAsGA1UEChMET0NBMTEPMA0GA1UECxMGQ0NGQ0NCMRUwEwYDVQQLEwxJbmRpdmlkdWFsLTIxGTAXBgNVBAMTEEpTTlg3NkMxMDAwMDQ2ODUwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALaEoUVgO5rpGwHK1lrl4+uHxi8ErR2eIFW7gtdFBp90SG9dHLPIwDqZoAO1CjRbJAJvVNEvnMjD2W8ufaPHL+1n2MiQ1GmHI0Ia6QRGg1PP3VCIeDBhSvp7AmCdRrCFqrMS1etcQnE9Tu9CoyotdjaIgEUl+pAKa4HgRAjTZWatAgMBAAGjgbUwgbIwHwYDVR0jBBgwFoAUz3CdYeudfC6498sCQPcJnf4zdIAwOQYDVR0fBDIwMDAuoCygKoYoaHR0cDovL3VjcmwuY2ZjYS5jb20uY24vUlNBL2NybDcwMTUxLmNybDALBgNVHQ8EBAMCA8gwHQYDVR0OBBYEFMlxCMZaiTpilu3i0xcdhfrhMT0BMCgGA1UdJQQhMB8GCCsGAQUFBwMCBggrBgEFBQcDBAYJKoZIhvcvAQEFMA0GCSqGSIb3DQEBBQUAA4IBAQB7md2AOzIlemccK2cpXKUNVmKXl8Nm/WjuhiPgCUub0GmTDIIyaGRyeTPpmQLrMLbs3DEtjazP75zlKkdOuZp99htDjxG2NOWc/j6GwuRYTNGBLUaL2fXCQTUrYblHBLbUvuEICJ/OeacHM8DbGB9EehjRiLGdvA7/Px62arhQhPkHARrD0tithsElENl3te+2035QwF/H/7tEA/mAmy2zbpxmQz19srFuLDGm4/6YL5QbkhUdLwfoQxqmvbjRLubiwOM/YmyMuPeNGrDlpEzTL6mhPTf3DbqOnyTUFWFNzJqaDmItFVHCQ7NUeoNbSJlNHNwqpuLL6J0a9P8GoQP+";
                        byte[] certDataB = Base64.decode(certData, Base64.DEFAULT);
                        map.put(VCardApi_FFT.RSA, certDataB);
                    }
                } else {
                    if (type == 0) {
                        if (list.contains(VCardApi_FFT.RSA)) {
                            int index = list.indexOf(VCardApi_FFT.RSA);
                            TMKeyLog.d(TAG, "cert_type_rsa1024>>>index:" + index);
                            if (index >= 0) {
                                list.remove(index);
                            }
                            index = hashTyslist.indexOf(VCardApi_FFT.SHA1);
                            TMKeyLog.d(TAG, "cert_type_rsa1024>>hashTyslist>>>index:" + index);
                            if (index >= 0) {
                                hashTyslist.remove(index);
                            }
                        }
                        TMKeyLog.d(TAG, "cert_type_rsa2048 select ");
                        list.add(VCardApi_FFT.RSA);
                        hashTyslist.add(VCardApi_FFT.SHA256);
                    } else {
                        if (map.containsKey(VCardApi_FFT.RSA)) {
                            map.remove(VCardApi_FFT.RSA);
                        }
                        String certData = "MIIECgYJKoZIhvcNAQcCoIID+zCCA/cCAQExADALBgkqhkiG9w0BBwGgggPfMIID2zCCAsOgAwIBAgIFAfi7fSIwDQYJKoZIhvcNAQELBQAwOjESMBAGA1UEAwwJUlNBQ0E2MjI0MRcwFQYDVQQKDA5PbmNlRG93bkNlcnRDQTELMAkGA1UEBhMCY24wHhcNMTkxMTA4MTAwMzMwWhcNMjAxMTA3MTAwMzMwWjBrMTUwMwYDVQQDDCwwNjNAMDM3MTIwMjE5OTcwOTI2NzQxNEBaaGFuZ3d1MDNAMTAwMDAwMDI3MTEUMBIGA1UECwwLRW50ZXJwcmlzZXMxDzANBgNVBAsMBjExMTExMTELMAkGA1UEBhMCY24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCL3Rbl1mEpwazrpiEcZFqEl/ojS9Pt9XGYGEwHtAIngZYXeZddtBbJGp/nm+SakyFZOdG7BgKnbaNCErLqnqS8RBmAQvMZa3naPhlLkrVrLOZ4RfsEs/XP70KKjkNX31FNroMWkioa00Y4gNW9iFkYfTYSd7e3m48XmWlzlsD0iZ0xfeZ+oajecS9iu4T8GuMyIadLSuftPnzK2n7sPNktJWHkCkMnYMh3/bj9FAizCejnfLKNe3W+XKMcPO5q2JixbeIxjggQY4S1pkPLvDXnp8oIpCVlpot2RQxxXAp4tNzJ5EoB5u4VYMPTyWHhBlmz/Y3dyMwuHw49Jx3nPjZxAgMBAAGjgbYwgbMwHwYDVR0jBBgwFoAUmPVQ2tFv9AKd/52c5TqAraWNc9EwCQYDVR0TBAIwADBWBgNVHR8ETzBNMEugSaBHpEUwQzENMAsGA1UEAwwEY3JsMTEMMAoGA1UECwwDY3JsMRcwFQYDVQQKDA5PbmNlRG93bkNlcnRDQTELMAkGA1UEBhMCY24wDgYDVR0PAQH/BAQDAgbAMB0GA1UdDgQWBBRKNdCc/XTQoQr4LMLuXJ378tyP0TANBgkqhkiG9w0BAQsFAAOCAQEAK2NGbthm9Bg9a/P6kXbv17LPI0KjnmfKwvc0XIOeRoHLz8lz54PqzEcWetj2le0JllE6z9hacrDh+JOl6RaZIoqoUP4IoqbxxWcz3U3DlTJPELBj28QBMa+gu/V1iYyU5GXNbmzKb4vF5ImlMIV44jcKs6saNtx0svlRu7x4VAoJaycJ2fus1yoQtkYtMuSyLHsFqHyCPOWmQoyjBehjFftMAjWRBQKCBE7RTvgT5BNInKKZowOIm1tNTMQUQkKu/qkJb4lvOCAbpnS2HA18fnz6auy1FQOlTbUN657XmYxq809IqUxq7kbt0ZeNMhbLlz9b8jdJ5q4CniAVHG4OQTEA";
                        byte[] certDataBytes = Base64.decode(certData, Base64.NO_WRAP);
//                        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>certDataBytes111:" + FCharUtils.bytesToHexStr(certDataBytes));
//                        GmSSL gmSSL= new GmSSL();
//                        long converRes = gmSSL.convertPkcs7ToPemHex(certDataBytes, certDataBytes.length);
//                        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>converRes:" + converRes);
//                        if (converRes != 0) { //转换数据成功
//                            certDataBytes = gmSSL.pemBytes;
//                            TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_rsa2048>>>certDataBytes222:" + FCharUtils.bytesToHexStr(certDataBytes));
//                        }
                        map.put(VCardApi_FFT.RSA, certDataBytes);
                    }
                }
            }
        });
        cert_type_rsa2048.toggle();

        cert_type_sm2_sign.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (type == 0) {
                    if (isChecked) {
                        list.add(VCardApi_FFT.SM2);
                        hashTyslist.add(VCardApi_FFT.SM3);
                    } else {
                        if (list.contains(VCardApi_FFT.SM2)) {
                            int index = list.indexOf(VCardApi_FFT.SM2);
                            TMKeyLog.d(TAG, "cert_type_sm2>>>index:" + index);
                            if (index >= 0) {
                                list.remove(index);
                            }
                            index = hashTyslist.indexOf(VCardApi_FFT.SM3);
                            TMKeyLog.d(TAG, "cert_type_sm2>>hashTyslist>>>index:" + index);
                            if (index >= 0) {
                                hashTyslist.remove(index);
                            }
                        }
                    }
                } else {
                    if (isChecked) {
//                        String certData = "MIICbDCCAhGgAwIBAgIFEDZFQhAwDAYIKoEcz1UBg3UFADBcMQswCQYDVQQGEwJDTjEwMC4GA1UECgwnQ2hpbmEgRmluYW5jaWFsIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MRswGQYDVQQDDBJDRkNBIFRFU1QgU00yIE9DQTEwHhcNMTgxMjI1MDQwMjAzWhcNMTkwMzI1MDQwMjAzWjBfMQswCQYDVQQGEwJDTjENMAsGA1UECgwET0NBMTEPMA0GA1UECwwGQ0NGQ0NCMRUwEwYDVQQLDAxJbmRpdmlkdWFsLTIxGTAXBgNVBAMMEEpTTlg3NkUxMDAwMDQ2ODEwWTATBgcqhkjOPQIBBggqgRzPVQGCLQNCAAQfTijERTWHzu047ZtWLXfBOYI4YCAeox47enhZIFoGq4WpfI+WXLvdr0yA3EhLFLymjAGDg/FxEfd5d1Go7eZro4G6MIG3MB8GA1UdIwQYMBaAFGv+GNqPQjqmuG2zLoiDOjSiwTDhMAwGA1UdEwEB/wQCMAAwOAYDVR0fBDEwLzAtoCugKYYnaHR0cDovL3VjcmwuY2ZjYS5jb20uY24vU00yL2NybDQzNzUuY3JsMA4GA1UdDwEB/wQEAwIGwDAdBgNVHQ4EFgQU0WvvUYehOoj+OU6cJqsrME35S4IwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMEMAwGCCqBHM9VAYN1BQADRwAwRAIgbjr2DXGwAl8PjhqMsKR+5x39fH8nW9uSrX9AOuzF5tYCIBb/CLMxhvfTzvRVut0hZICEID6e8Dorb9LTo5MJTl29";
                        String certData = "MIICUDCCAfWgAwIBAgIJAKqqqqqqqrBDMAwGCCqBHM9VAYN1BQAwOjELMAkGA1UEBhMCQ04xFzAVBgNVBAoMDk9uY2VEb3duQ2VydENBMRIwEAYDVQQDDAlTTTJDQTYyMjQwHhcNMTkxMTA4MTAwMzMwWhcNMjAxMTA3MTAwMzMwWjBlMQswCQYDVQQGEwJjbjEPMA0GA1UECwwGMTExMTExMQ4wDAYDVQQLDAVVbml0czE1MDMGA1UEAwwsMDYzQDAzNzEyMDIxOTk3MDkyNjc0MTRAWmhhbmd3dTAzQDEwMDAwMDAyNzIwWTATBgcqhkjOPQIBBggqgRzPVQGCLQNCAASIeRuiUfPEw4pSjbN3ZjgQLG5XU4XOkcT4nUN0J6aQXivXw31vYMlsiutY6z8eyLP1rtx0l6QFGXQcyIhMiu8ro4G2MIGzMB8GA1UdIwQYMBaAFBAKcTyFiiXG25MbRqalB84XGA7wMAkGA1UdEwQCMAAwVgYDVR0fBE8wTTBLoEmgR6RFMEMxDTALBgNVBAMMBGNybDIxDDAKBgNVBAsMA2NybDEXMBUGA1UECgwOT25jZURvd25DZXJ0Q0ExCzAJBgNVBAYTAkNOMA4GA1UdDwEB/wQEAwIGwDAdBgNVHQ4EFgQUv05HjtUczhIwvN+zVItGreFis+owDAYIKoEcz1UBg3UFAANHADBEAiAl2mxUuu2nrkcBGOy52z3M20vGBQfSDTvggvw9gmfsugIgSMRtpgT1aekw0Z2bF14iKxSHisETbBqL94x93sJfQhE=";
                        byte[] certDataBytes = Base64.decode(certData, Base64.DEFAULT);
                        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_sm2_sign>>>certDataBytes:" + FCharUtils.bytesToHexStr(certDataBytes));
                        map.put(VCardApi_FFT.SM2, certDataBytes);
                    } else {
                        if (map.containsKey(VCardApi_FFT.SM2)) {
                            map.remove(VCardApi_FFT.SM2);
                        }
                    }
                }
            }
        });

        cert_type_sm2_enc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (type == 1) {
                    if (isChecked) {
//                        String certData = "MIICbTCCAhGgAwIBAgIFEDZFQhEwDAYIKoEcz1UBg3UFADBcMQswCQYDVQQGEwJDTjEwMC4GA1UECgwnQ2hpbmEgRmluYW5jaWFsIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MRswGQYDVQQDDBJDRkNBIFRFU1QgU00yIE9DQTEwHhcNMTgxMjI1MDQwMjAzWhcNMTkwMzI1MDQwMjAzWjBfMQswCQYDVQQGEwJDTjENMAsGA1UECgwET0NBMTEPMA0GA1UECwwGQ0NGQ0NCMRUwEwYDVQQLDAxJbmRpdmlkdWFsLTIxGTAXBgNVBAMMEEpTTlg3NkUxMDAwMDQ2ODEwWTATBgcqhkjOPQIBBggqgRzPVQGCLQNCAAS3bPe4xrLBeVtL/OFeRBY+gzEiWt8avMwf9l81acleAi6yHDNZ2JmVDyQiAVGWo4G0cMGzszkwWplOyUa/D0x0o4G6MIG3MB8GA1UdIwQYMBaAFGv+GNqPQjqmuG2zLoiDOjSiwTDhMAwGA1UdEwEB/wQCMAAwOAYDVR0fBDEwLzAtoCugKYYnaHR0cDovL3VjcmwuY2ZjYS5jb20uY24vU00yL2NybDQzNzUuY3JsMA4GA1UdDwEB/wQEAwIDODAdBgNVHQ4EFgQUAoyhBHFrP4RW7ZFkRAHEdfL58YQwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMEMAwGCCqBHM9VAYN1BQADSAAwRQIhAJBSWQQ5vce8KnHE9MlF03w8nAdCOptneRHOsrm9jRd/AiAE15gimRwu1HxnVxj2j6Yv9uKWf2+HkvWXDh/qG0N8NQ==";
                        String certData = "MIICUjCCAfWgAwIBAgIJAKqqqqqqqrBCMAwGCCqBHM9VAYN1BQAwOjELMAkGA1UEBhMCQ04xFzAVBgNVBAoMDk9uY2VEb3duQ2VydENBMRIwEAYDVQQDDAlTTTJDQTYyMjQwHhcNMTkxMTA4MTAwMzMwWhcNMjAxMTA3MTAwMzMwWjBlMQswCQYDVQQGEwJjbjEPMA0GA1UECwwGMTExMTExMQ4wDAYDVQQLDAVVbml0czE1MDMGA1UEAwwsMDYzQDAzNzEyMDIxOTk3MDkyNjc0MTRAWmhhbmd3dTAzQDEwMDAwMDAyNzIwWTATBgcqhkjOPQIBBggqgRzPVQGCLQNCAARaX2+BmunHLdGPaJHy2PEWIdvZBNVGBc5NmuRwznxl5txJ+IbQ4YRa6L0/r7HYMzGuqN2y/IV6+WSZpz69bX5Wo4G2MIGzMB8GA1UdIwQYMBaAFBAKcTyFiiXG25MbRqalB84XGA7wMAkGA1UdEwQCMAAwVgYDVR0fBE8wTTBLoEmgR6RFMEMxDTALBgNVBAMMBGNybDIxDDAKBgNVBAsMA2NybDEXMBUGA1UECgwOT25jZURvd25DZXJ0Q0ExCzAJBgNVBAYTAkNOMA4GA1UdDwEB/wQEAwIDODAdBgNVHQ4EFgQUKK1ZlgS8jaBn+enEMGdlTJ0VjpAwDAYIKoEcz1UBg3UFAANJADBGAiEA6lSbrwXL0yFZfve1e5xtiS85dqKIUA1icnx1ZrgFqQECIQChBSh42WAj/j0Irqc/AxtD0sBhf3U3jA0pSuxdKT+pcA==";
                        byte[] certDataBytes = Base64.decode(certData, Base64.DEFAULT);
                        TMKeyLog.d(TAG, "showCheckBoxDialog>>>cert_type_sm2_enc>>>certDataBytes:" + FCharUtils.bytesToHexStr(certDataBytes));
                        map.put(VCardApi_FFT.SM2_ENC, certDataBytes);
                    } else {
                        if (map.containsKey(VCardApi_FFT.SM2_ENC)) {
                            map.remove(VCardApi_FFT.SM2_ENC);
                        }
                    }
                }
            }
        });

        sureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type == 0) {
                    result.success(list, hashTyslist);
                } else {
                    result.success(map, null);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
        return dialog;
    }

    /**
     *
     * @param mContext
     * @param inputDialogResult
     * @return
     */
    public static Dialog showInputPINDialog(Context mContext,
                                            int tyte,
                                            View.OnClickListener etListener,
                                            final InputDialogResult inputDialogResult

    ) {
        dialog = new Dialog(mContext, R.style.dextetPermissionDialog);
        dialog.setContentView(R.layout.input_pwd_dialog);

        setDialogLayoutParams(mContext, dialog, 0,
                Gravity.CENTER, (float) 0.8);

        TextView dialog_tv_title = dialog.findViewById(R.id.dialog_tv_title);
        final EditText pwdInputEt = dialog.findViewById(R.id.sdk_input_pwd_et);
        if (tyte == 1) {
            dialog_tv_title.setText("请输入COS版本号");
            pwdInputEt.setHint("请输入COS版本号");
        } else {
            dialog_tv_title.setText("请输入PIN码");
            pwdInputEt.setHint("请输入PIN码");
            if (etListener != null) {
                pwdInputEt.setOnClickListener(etListener);
            }
        }
        Button agreeBtn = dialog.findViewById(R.id.sdk_input_pwd_sure_btn);
        agreeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (inputDialogResult != null) {
                    String text = pwdInputEt.getText().toString();
                    inputDialogResult.success(text);
                }
            }
        });
        ImageView dialog_close_img = dialog.findViewById(R.id.dialog_close_img);
        dialog_close_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (inputDialogResult != null) {
                    inputDialogResult.cancle();
                }
            }
        });

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return dialog;
    }

    public static void setDialogLayoutParams(Context context, Dialog dialog, int dialogPadding,
                                             int gravity, float alpha) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(gravity);
        lp.alpha = alpha;
        lp.dimAmount = 0.6f;
        lp.y = dialogPadding;
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager m = ((Activity)context).getWindowManager();
        Display d = m.getDefaultDisplay();
        lp.width = (int) ((int) d.getWidth());
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setAttributes(lp);
        dialog.setCanceledOnTouchOutside(true);
    }
}
