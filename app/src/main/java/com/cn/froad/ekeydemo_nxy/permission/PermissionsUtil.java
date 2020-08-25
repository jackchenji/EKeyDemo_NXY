package com.cn.froad.ekeydemo_nxy.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;

import com.cn.froad.ekeydemo_nxy.R;
import com.cn.froad.ekeydemo_nxy.dialog.BaseDialog;
import com.cn.froad.ekeydemo_nxy.dialog.CommonBaseDialog;
import com.cn.froad.ekeydemo_nxy.utils.RefResTool;
import com.froad.eid.ecard.utils.TMKeyLog;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Request;

import java.util.List;

public class PermissionsUtil {
    private static final String TAG = "PermissionsUtil";
    private static final String[] RC_CAMERA_PERMISSION = {Manifest.permission.CAMERA};
    private static final String[] RC_LOCATION_PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private static final String[] RC_STORAGE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String[] RC_CALL_PHONE_PERMISSIONS = {Manifest.permission.READ_PHONE_STATE};
    private static final String[] RC_READ_CONTACTS_PERMISSIONS = {Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS};
    private static final String[] RC_READ_SMS_PERMISSIONS = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS};


    private static void requestPermission(final String[] permissions, final PermissionListener listener) {
        final Fragment fragment;
        final Activity activity;
        Activity context = null;
        Request options = null;
        if (listener instanceof DefaultFragmentPermissionListener) {

            fragment = ((DefaultFragmentPermissionListener) listener).getFragment();
            options = AndPermission.with(fragment);
            context = fragment.getActivity();
        } else if (listener instanceof DefaultActivityPermissionListener) {
            activity = ((DefaultActivityPermissionListener) listener).getActivity();
            options = AndPermission.with(activity);
            context = activity;
        }
        final Context mContext = context;
        if (options == null) {
            return;
        }
        options.permission(permissions)
//                .rationale(new RuntimeRationale())
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        TMKeyLog.d(TAG, "onAction: onGranted");
                        listener.onGranted();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(@NonNull List<String> permissions) {
                        TMKeyLog.d(TAG, "onAction: onDenied ");
                        if (mContext != null) {
                            if (AndPermission.hasAlwaysDeniedPermission(mContext, permissions)) {
                                listener.onAlwaysDenied();
                            } else {
                                listener.onDenied();
                            }
                        }
                    }
                }).start();
    }

    private static void requestPermission(final int type, final String[] permissions, final PermissionListener listener) {
        final Fragment fragment;
        final Activity activity;
        Activity context = null;
        Request options = null;
        if (listener instanceof DefaultFragmentPermissionListener) {

            fragment = ((DefaultFragmentPermissionListener) listener).getFragment();
            options = AndPermission.with(fragment);
            context = fragment.getActivity();
        } else if (listener instanceof DefaultActivityPermissionListener) {
            activity = ((DefaultActivityPermissionListener) listener).getActivity();
            options = AndPermission.with(activity);
            context = activity;
        }
        final Context mContext = context;
        if (options == null) {
            return;
        }
        options.permission(permissions)
//                .rationale(new RuntimeRationale())
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        TMKeyLog.d(TAG, "onAction: onGranted");
                        listener.onGranted();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(@NonNull List<String> permissions) {
                        TMKeyLog.d(TAG, "onAction: onDenied ");
                        if (mContext != null) {
                            if (AndPermission.hasAlwaysDeniedPermission(mContext, permissions)) {
                                listener.onAlwaysDenied(type);
                            } else {
                                listener.onDenied();
                            }
                        }
                    }
                }).start();
    }


    static void startInstalledAppDetailsActivity(final Context context) {
        if (context == null) {
            return;
        }
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    static void showNeverAskDialog(final Context context) {
        if (context == null) {
            return;
        }
        String sdMsgStr = String.format(RefResTool.getString(R.string.permission_denied_dialog_setmethod), RefResTool.getString(R.string.app_name), RefResTool.getString(R.string.app_name));
        final CommonBaseDialog baseDialog = new CommonBaseDialog(context, false, sdMsgStr,
                R.string.permission_button_cancel, R.string.permission_rationale_settings_button_text);
        baseDialog.setOnDialogBtnListener(new BaseDialog.onDialogBtnListener() {
            @Override
            public void onRightComplete(BaseDialog dialog, View view) {
                baseDialog.dismiss();
                startInstalledAppDetailsActivity(context);
            }

            @Override
            public void onLeftComplete(BaseDialog dialog, View view) {
                baseDialog.dismiss();
            }
        });
        baseDialog.show();
    }


    static void showNeverAskDialog(int type, final Context context) {
        if (context == null) {
            return;
        }
        String sdMsgStr;
        switch (type) {
            case 2:
                sdMsgStr = RefResTool.getString(R.string.common_location_permission_tip_deny_message);
                break;
            default:
                sdMsgStr = String.format(RefResTool.getString(R.string.permission_denied_dialog_setmethod), RefResTool.getString(R.string.app_name));
                break;
        }

        final CommonBaseDialog baseDialog = new CommonBaseDialog(context, false, sdMsgStr,
                R.string.app_cancel, R.string.permission_rationale_settings_button_text);
        baseDialog.setOnDialogBtnListener(new BaseDialog.onDialogBtnListener() {
            @Override
            public void onRightComplete(BaseDialog dialog, View view) {
                baseDialog.dismiss();
                startInstalledAppDetailsActivity(context);
            }

            @Override
            public void onLeftComplete(BaseDialog dialog, View view) {
                baseDialog.dismiss();
            }
        });
        baseDialog.show();
    }

    public static void requestQRScanPermission(PermissionListener listener) {
        requestPermission(RC_CAMERA_PERMISSION, listener);
    }

    public static void requestLocatePermission(PermissionListener listener) {
        requestPermission(2, RC_LOCATION_PERMISSIONS, listener);
    }

    public static void requestExternalStoragePermission(PermissionListener listener) {
        requestPermission(RC_STORAGE_PERMISSIONS, listener);
    }

    public static void requestCallPhonePermission(PermissionListener listener) {
        requestPermission(RC_CALL_PHONE_PERMISSIONS, listener);
    }

        public static void requestSmsPermission(PermissionListener listener){
        requestPermission(RC_READ_SMS_PERMISSIONS,listener);
    }

    public static void requestContactPermission(PermissionListener listener) {
        requestPermission(RC_READ_CONTACTS_PERMISSIONS, listener);
    }
}
