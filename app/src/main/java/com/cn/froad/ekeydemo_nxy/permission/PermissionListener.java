package com.cn.froad.ekeydemo_nxy.permission;

public interface PermissionListener {
    void onGranted();

    void onDenied();

    void onAlwaysDenied();

    void onAlwaysDenied(int type);
}
