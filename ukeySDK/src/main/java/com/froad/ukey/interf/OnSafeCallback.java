package com.froad.ukey.interf;

public interface OnSafeCallback<T> {
    void callBack (boolean flag, int state, String msg);
}
