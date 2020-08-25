package com.cn.froad.ekeydemo_nxy.app;

import android.app.Activity;
import android.app.Application;

import com.froad.ukey.utils.np.TMKeyLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by HJH on 2017/2/8.
 */
public class MyApplication extends Application {

    private final static String TAG = "MyApplication";
      /**
       * 全局Application实例
       */
      public static MyApplication application;

      public static List<Activity> activities = new ArrayList<Activity>();
      // 用于存放倒计时时间
      public static Map<String, Long> map;

      @Override
      public void onCreate() {
          TMKeyLog.d(TAG, "onCreate");
          super.onCreate();
          disableAPIDialog();

          application = this;
      }


      /**
       * 获取Application实例
       **/
      public static MyApplication getInstance() {
            return application;
      }

      /***
       * 退出应用程序
       */
      public void appExit() {
            try {
                  finishAll();
            } catch (Exception e) {
            }
      }

      public static void addActivity(Activity activity) {
            if (!activities.contains(activity)) {
                  activities.add(activity);
            }
      }

      public static void removeActivity(Activity activity) {
            activities.remove(activity);
      }

      public static void finishAll() {
            for (Activity activity : activities) {
                  activity.finish();
            }
      }

    @Override
    public void onTerminate() {
        super.onTerminate();
        TMKeyLog.e(TAG, "onTerminate");
    }

    /**
            * android 9.0 调用私有api弹框的解决方案
     */
    private void disableAPIDialog(){
        try {
            Class clazz = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = clazz.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);
            Field mHiddenApiWarningShown = clazz.getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
