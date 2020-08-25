package com.cn.froad.ekeydemo_nxy.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import com.cn.froad.ekeydemo_nxy.BaseActivity;
import com.froad.eid.ecard.utils.TMKeyLog;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * 此类实现应用中所有Activity的集中栈式管理
 */
public class AppActivityManager {

    private static final String TAG = "AppActivityManager";
    private static Stack<BaseActivity> activityStack;

    private static LinkedList<Activity> activities;
    private volatile static AppActivityManager instance;
    private BaseActivity mLastActivity;

    private AppActivityManager() {
        activityStack = new Stack<BaseActivity>();
        activities = new LinkedList<>();
    }

    public static AppActivityManager getAppManager() {

        if (instance == null) {
            synchronized (AppActivityManager.class) {
                if (instance == null) {
                    instance = new AppActivityManager();
                }
            }
        }
        return instance;
    }

    public void addListActivity(Activity activity) {
        if (!activities.contains(activity)) {
            activities.add(activity);
        }
    }

    public void removeListActivity(Activity activity) {
        if (activities.contains(activity)) {
            activities.remove(activity);
        }
    }

    public void finishAllListActivity() {
        for (Activity a : activities) {
            if (!a.isFinishing()) {
                a.finish();
            }
        }
        activities.clear();
    }

    public static LinkedList<Activity> getListActivities() {
        return activities;
    }

    /**
     * 将Activity入栈
     *
     * @param activity
     */
    public void addActivity(BaseActivity activity) {
        if (activityStack == null) {
            activityStack = new Stack<BaseActivity>();
        }
        activityStack.add(activity);
        mLastActivity = activity;
    }

    /**
     * 将Activity出栈
     *
     * @param activity
     */
    public void removeActivity(BaseActivity activity) {
        if (activity != null) {
            activityStack.remove(activity);
        }
    }

    /**
     * 获取当前栈中的Activity
     *
     * @return
     */
    public BaseActivity currentActivityInstack() {
        if (activityStack.isEmpty()) {
            TMKeyLog.d(TAG, "currentActivityInstack>>>Empty");
            return null;
        } else {
            TMKeyLog.d(TAG, "currentActivityInstack>>>NotEmpty");
            return activityStack.lastElement();
        }
    }

    /**
     * 获取当前的Activity
     *
     * @return
     */
    public BaseActivity currentActivity() {
        if (activityStack.isEmpty()) {
            TMKeyLog.d(TAG, "currentActivity>>>Empty");

            return mLastActivity;
        } else {
            TMKeyLog.d(TAG, "currentActivity>>>not Empty");
            return activityStack.lastElement();
        }
    }

    /**
     * 结束当前Activity,释放Activity所占空间
     */
    public void finishActivity() {
        try {
            Activity activity = activityStack.lastElement();
            if (activity != null) {
                finishActivity(activity);
            }
        } catch (NoSuchElementException e) {

        }
    }

    /**
     * 结束指定的Activity
     *
     * @param activity
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            activityStack.remove(activity);
            activity.finish();
        }
    }

    /**
     * 结束指定的Activity
     *
     * @param cls
     */
    public void finishActivity(Class<?> cls) {
        for (Activity activity : activities) {
            if (activity.getClass().equals(cls)) {
                finishActivity(activity);
            }
        }
    }

    public Activity findActivity(Class<?> cls) {
        for (Activity activity : activityStack) {
            TMKeyLog.d(TAG, "findActivity: "+activity.getClass().getName());
            if(activity.getClass().equals(cls)) {
                return activity;
            }
        }
        TMKeyLog.d(TAG, "findActivity: activityStack size == "+activityStack.size());
        return null;
    }

    /**
     * 结束所有的Activity
     */
    public void finishAllActivity() {
        int size = activityStack.size();
        TMKeyLog.d(TAG, "finishAllActivity>>>size:" + size);
        for (int i = 0; i < size; i++) {
            if (null != activityStack.get(i)) {
                activityStack.get(i).finish();
            }
        }
        activityStack.clear();
    }

    /**
     * 退出应用程序, 结束所有的Activity并退出系统
     *
     * @param context
     */
    public void AppExit(Context context) {
        try {
            finishAllActivity();
            ActivityManager activityMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityMgr.restartPackage(context.getPackageName());

            System.exit(0);
        } catch (Exception e) {
        }
    }

    public Stack<BaseActivity> getActivitySatck() {
        return activityStack;
    }

    /**
     * Clear the mLastActivity reference.
     *
     * @param baseFragmentActivity
     */
    public void destoryActivity(BaseActivity baseFragmentActivity) {
        if (baseFragmentActivity == mLastActivity) {
            mLastActivity = null;
        }
    }
}