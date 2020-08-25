package com.cn.froad.ekeydemo_nxy;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import android.widget.Toast;

import com.cn.froad.ekeydemo_nxy.app.MyApplication;
import com.cn.froad.ekeydemo_nxy.manager.AppActivityManager;

public abstract class BaseActivity extends FragmentActivity {
    private static final String TAG = "BaseActivity";
    // Alli pay according to third pay.
    public static final int THIRD_PAY_ALLI_PAY_FLAG = 100001;
    protected MyApplication mSQApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSQApplication = MyApplication.getInstance();
        super.onCreate(savedInstanceState);
        // Remove titlebar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        AppActivityManager.getAppManager().addListActivity(this);
    }

    @Override
    protected void onResume() {
        AppActivityManager.getAppManager().addActivity(this);
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        AppActivityManager.getAppManager().removeActivity(this);
    }

    @Override
    protected void onDestroy() {
        AppActivityManager.getAppManager().removeListActivity(this);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void showToast (String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
