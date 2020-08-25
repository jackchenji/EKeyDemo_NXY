package com.cn.froad.ekeydemo_nxy.permission;

import android.app.Activity;

public class DefaultActivityPermissionListener implements PermissionListener {


    private Activity mActivity;

    public DefaultActivityPermissionListener(Activity activity) {
        mActivity = activity;
    }

    public Activity getActivity() {
        return mActivity;
    }


    @Override
    public void onGranted() {

    }

    @Override
    public void onDenied() {

    }

    @Override
    public void onAlwaysDenied() {
        if (mActivity != null) {
            PermissionsUtil.showNeverAskDialog(mActivity);
        }
    }

    @Override
    public void onAlwaysDenied(int type) {
        if (mActivity != null) {
            PermissionsUtil.showNeverAskDialog(type, mActivity);
        }
    }
}
