package com.cn.froad.ekeydemo_nxy.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.cn.froad.ekeydemo_nxy.BaseActivity;
import com.cn.froad.ekeydemo_nxy.R;
import com.cn.froad.ekeydemo_nxy.app.MyApplication;
import com.cn.froad.ekeydemo_nxy.manager.AppActivityManager;
import com.cn.froad.ekeydemo_nxy.utils.RefResTool;
import com.froad.eid.ecard.utils.AppExecutors;
import com.froad.eid.ecard.utils.TMKeyLog;

/**
 * Created by FW on 2018/6/27.
 */

public class DialogFragUtil {
    private static final String TAG = "DialogFragUtil";

    private static volatile CommonBaseDialog staticDialog;
    private static volatile BaseDialog commonBaseDialog;
    private static volatile BaseDialog loadingDialog;
    private static volatile BaseDialog nfcReadEndDialog;

    public static void showStaticAlertNetErrorDlg(final CommonBaseDialog.onDialogBtnListener listener) {
        showStaticAlertErrorDlg(true, "", RefResTool.getString(R.string.common_net_error_new), RefResTool.getString(R.string.app_retry), "", listener);
    }

    public static void showStaticCommonAlertDlg(String message, String confirmText, final CommonBaseDialog.onDialogBtnListener listener) {
        showStaticAlertErrorDlg(true, "", message, confirmText, "", listener);
    }

    public static void showStaticTitleAlertDlg(String title, String message, String confirmText, final CommonBaseDialog.onDialogBtnListener listener) {
        showStaticAlertErrorDlg(true, title, message, confirmText, "", listener);
    }

    public static void showStaticChooseDlg(String title, String message, final CommonBaseDialog.onDialogBtnListener listener) {
        showStaticAlertErrorDlg(false, title, message, RefResTool.getString(R.string.app_cancel), RefResTool.getString(R.string.app_confirm), listener);
    }

    public static void showStaticAlertErrorDlg(final boolean isAert, final String tittle, final String message, final String leftButtonText, final String rightButtonText, final CommonBaseDialog.onDialogBtnListener listener) {
        AppExecutors.getAppExecutors().postMainThread(new Runnable() {
            @Override
            public void run() {
                BaseActivity activity = AppActivityManager.getAppManager().currentActivity();
                if (activity == null && staticDialog == null) {
                    staticDialog = new CommonBaseDialog(MyApplication.getInstance(), isAert, tittle, message, leftButtonText, rightButtonText);
                    staticDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                }
                if (activity != null && staticDialog == null) {
                    staticDialog = new CommonBaseDialog(activity, isAert, tittle, message, leftButtonText, rightButtonText);
                }
                staticDialog.setCanceledOnTouchOutside(false);
                staticDialog.setCancelable(false);
                staticDialog.clearAllText();
                staticDialog.setAlert(isAert);
                staticDialog.setTitleText(tittle);
                staticDialog.setContentTextViewText(message);
                staticDialog.setLeftButtonText(leftButtonText);
                staticDialog.setRightButtonText(rightButtonText);
                staticDialog.setOnDialogBtnListener(new CommonBaseDialog.DialogListener() {
                    @Override
                    public void onRightComplete(BaseDialog dialog, View view) {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onRightComplete(dialog, view);
                        }
                    }

                    @Override
                    public void onLeftComplete(BaseDialog dialog, View view) {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onLeftComplete(dialog, view);
                        }
                    }
                });
                staticDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        staticDialog = null;
                    }
                });
                staticDialog.show();
            }
        });

    }

    public static void showLoadingDialog() {
        AppExecutors.getAppExecutors().postMainThread(new Runnable() {
            @Override
            public void run() {
                if (loadingDialog!=null) {
                    if (loadingDialog.isShowing()){
                        return;
                    } else {
                        loadingDialog.dismiss();
                    }
                }
                BaseActivity activity = AppActivityManager.getAppManager().currentActivity();
                if (loadingDialog ==null){
                    if (activity != null){
                        loadingDialog = new BaseDialog(activity);
                    }else {
                        loadingDialog = new BaseDialog(MyApplication.getInstance());
                        loadingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                    }
                    loadingDialog.setContentView(R.layout.dialog_eid_loading_progress);
                }
                loadingDialog.setCanceledOnTouchOutside(false);
                loadingDialog.setCancelable(true);
                loadingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        loadingDialog = null;
                    }
                });
                if (checkLoadingActivity()) return;
                loadingDialog.show();
            }
        });
    }

    public static void hideAllDialog(){
        hideLoadingDialog();
        hideCommonBaseDialog();
        hideNfcReadEndDialog();
    }

    public static void hideLoadingDialog(){
        if (loadingDialog!=null && loadingDialog.isShowing()){
            if (checkLoadingActivity()) return;
            loadingDialog.dismiss();
        }
    }


    public static boolean checkLoadingActivity() {
        final Context context = loadingDialog.getContext();
        if (context instanceof Activity) {
            Activity a = (Activity) context;
            if (a.isDestroyed() || a.isFinishing()) {
                loadingDialog = null;
                return true;
            }
        }
        return false;
    }
    public static void hideCommonBaseDialog(){
        if (commonBaseDialog!=null && commonBaseDialog.isShowing()){
            if (checkCommonActivity()) return;
            commonBaseDialog.dismiss();
        }
    }

    public static boolean checkCommonActivity() {
        final Context context = commonBaseDialog.getContext();
        if (context instanceof Activity) {
            Activity a = (Activity) context;
            if (a.isDestroyed() || a.isFinishing()) {
                commonBaseDialog = null;
                return true;
            }
        }
        return false;
    }

    public static void hideNfcReadEndDialog(){
        if (nfcReadEndDialog!=null && nfcReadEndDialog.isShowing()){
            if (checkNfcReadActivity()) return;
            nfcReadEndDialog.dismiss();
        }
    }


    public static boolean checkNfcReadActivity() {
        final Context context = nfcReadEndDialog.getContext();
        if (context instanceof Activity){
            Activity a = (Activity) context;
            if (a.isDestroyed() || a.isFinishing()){
                nfcReadEndDialog =null;
                return true;
            }
        }
        return false;
    }

    public static void showNFCReadWaitDialog() {
        AppExecutors.getAppExecutors().postMainThread(new Runnable() {
            @Override
            public void run() {
                hideAllDialog();
                BaseActivity activity = AppActivityManager.getAppManager().currentActivity();
                commonBaseDialog = new BaseDialog(activity);
                commonBaseDialog.setContentView(R.layout.dialog_eid_card_read_wait_layout);
                commonBaseDialog.setCanceledOnTouchOutside(false);
                commonBaseDialog.setCancelable(true);
                commonBaseDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        commonBaseDialog = null;
                        TMKeyLog.d(TAG, "showNFCReadWaitDialog>>>onDismiss");
                    }
                });
                if (checkCommonActivity()) return;
                commonBaseDialog.show();
            }
        });
    }

    public static void showNFCReadEndDialog(final String tip, final CommonBaseDialog.onDialogBtnListener listener) {
        AppExecutors.getAppExecutors().postMainThread(new Runnable() {
            @Override
            public void run() {
                hideAllDialog();
                BaseActivity activity = AppActivityManager.getAppManager().currentActivity();
                nfcReadEndDialog = new BaseDialog(activity);
                nfcReadEndDialog.setContentView(R.layout.dialog_eid_card_read_end_layout);
                nfcReadEndDialog.setCanceledOnTouchOutside(false);
                nfcReadEndDialog.setCancelable(false);
                final TextView sure_tv = nfcReadEndDialog.findViewById(R.id.sure_tv);
                String tms = tip;
                if (TextUtils.isEmpty(tip)) {
                    tms = "身份信息读取成功";
                }
                sure_tv.setText(tms);
                final TextView sure = nfcReadEndDialog.findViewById(R.id.sure_btn);
                final TextView textView = nfcReadEndDialog.findViewById(R.id.sure_tv);
                textView.setText(tip);
                sure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            if (nfcReadEndDialog!=null){
                                listener.onLeftComplete(nfcReadEndDialog, nfcReadEndDialog.getCurrentFocus());
                            }else {
                                listener.onLeftComplete(null, null);
                            }
                        }
                        hideNfcReadEndDialog();
                    }
                });
                nfcReadEndDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        nfcReadEndDialog = null;
                        TMKeyLog.d(TAG, "showNFCReadEndDialog>>>onDismiss");
                    }
                });

                if (checkNfcReadActivity()) return;
                nfcReadEndDialog.show();
            }
        });
    }


}
