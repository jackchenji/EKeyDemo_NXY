package com.cn.froad.ekeydemo_nxy.utils;

import android.content.Context;

import com.cn.froad.ekeydemo_nxy.app.MyApplication;

/**
 * Created by FW on 2017/6/30.
 */

public class RefResTool {

    public static String getString(int t) {
        return MyApplication
                .getInstance().getApplicationContext().getString(t);
    }

    public static String[] getStringArray(int ts) {
        return getStringArray(MyApplication.getInstance().getApplicationContext(), ts);
    }

    public static String[] getStringArray(Context context, int ts) {
        return context.getResources().getStringArray(ts);
    }

    public static int getColor(int colorRes) {
        return MyApplication.getInstance().getApplicationContext().getResources().
                getColor(colorRes);
    }

    public static float getDPPixel(int dpRes) {
        return MyApplication.getInstance().getApplicationContext().getResources()
                .getDimensionPixelSize(dpRes);
    }
}
