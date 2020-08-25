package com.cn.froad.ekeydemo_nxy.permission;


import android.support.v4.app.Fragment;

public class DefaultFragmentPermissionListener implements PermissionListener {

    private Fragment mFragment;


    public DefaultFragmentPermissionListener(Fragment fragment) {
        mFragment = fragment;
    }

    public Fragment getFragment() {
        return mFragment;
    }

    @Override
    public void onGranted() {

    }

    @Override
    public void onDenied() {

    }

    @Override
    public void onAlwaysDenied() {
        if (mFragment != null) {
            PermissionsUtil.showNeverAskDialog(mFragment.getActivity());
        }
    }

    @Override
    public void onAlwaysDenied(int type) {
        if (mFragment != null) {
            PermissionsUtil.showNeverAskDialog(type, mFragment.getActivity());
        }
    }
}
