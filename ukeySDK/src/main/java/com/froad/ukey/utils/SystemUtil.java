package com.froad.ukey.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.utils.np.TMKeyLog;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by FW on 2017/1/16.
 */
public class SystemUtil {

    private static final String TAG = FConstant.LOG_TAG + SystemUtil.class.getSimpleName();
    /**
     * @return 返回手机型号
     */
    public static String getMODEL() {
        return android.os.Build.MODEL;
    }

    /**
     * @return 返回手机SDK版本
     */
    public static String getSDKVersion() {
        return android.os.Build.VERSION.SDK;

    }

    /**
     * @return 返回手机系统版本
     */
    public static String getReleaseVersion() {
        String release = "";
        try {
            release = Build.VERSION.RELEASE;
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            release = "";
        }
        return release;
    }

    /**
     *
     * @Title: getImei
     * @Description: 获取手机Imei
     * @author: Floyd_feng 2015年8月10日
     * @modify: Floyd_feng 2015年8月10日
     * @throws
     */
    public static String getImei(Context mContext){
        String imei = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getDeviceId();
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            imei = "";
        }
        TMKeyLog.d(TAG, "imei:"+imei);
        return (imei == null ? "" : imei);
    }

    private static String getIpAddress(Context mContext) {
        String ia = null;
        try {
            WifiManager wifiManager = (WifiManager) mContext
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            TMKeyLog.d(TAG, "getIpAddress:" + i);
            if (i == 0) {
                ia = "";
            } else {
                ia = int2ip(i);
            }
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            ia = "";
        }
        return ia;
    }

    /**
     * 将ip的整数形式转换成ip形式
     *
     * @param ipInt
     * @return
     */
    public static String int2ip(int ipInt) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    /**
     * 获取手机Ip地址
     * @Title: getLocalIpAddress
     * @Description: TODO
     * @author: Floyd_feng 2015年8月10日
     * @modify: Floyd_feng 2015年8月10日
     * @throws
     */
    public static String getLocalIpAddress(Context mContext) {
        String ipStr = "";
        try {
            NetworkInterface intf = null;
            InetAddress inetAddress = null;
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ipStr = inetAddress.getHostAddress().toString();
                        if(ipStr.contains(".0") || ipStr.contains(".0.")){
                            ipStr = "";
                            continue;
                        }
                        return ipStr;
                    }
                }
            }
        } catch (Exception ex) {
            TMKeyLog.e(TAG, "Exception:" + ex.getMessage());
            ipStr = "";
        }
        if ("".equals(ipStr)) {
            ipStr = getIpAddress(mContext);
        }
        return ipStr == null ? "" : ipStr;
    }

    /**
     * 获取MAC地址
     * @param mContext
     * @return
     */
    public static String getSysMac(Context mContext){
        String mac = null;
        if (Build.VERSION.SDK_INT < 23) {
            boolean mac0b = false; //判断MAC地址是否有效，全0为无效，空值为无效
            try {
                WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifi.getConnectionInfo();
                mac = info.getMacAddress();
                mac0b = false;
                if(mac != null) {
                    String mac0 = mac.replaceAll(":","");
                    int mac0l = mac0.length();
                    if (mac0l == 0) {
                        mac0b = false;
                    }
                    for (int i = 0; i < mac0l; i++) {
                        if (!"0".equals(mac0.substring(i, i + 1))) {
                            mac0b = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                TMKeyLog.e(TAG, "Exception:" + e.getMessage());
                mac0b = false;
            }
            TMKeyLog.d(TAG, "SDK_INT<23>>>mac:" + mac + ">>>mac0b:" + mac0b);
            if (! mac0b) { //获取的MAC地址无效
                try {
                    mac = getMac();
                } catch (Exception e) {
                    TMKeyLog.e(TAG, "Exception:" + e.getMessage());
                    mac = null;
                }
            }
        } else {
            try {
                mac = getMac();
            } catch (Exception e) {
                TMKeyLog.e(TAG, "Exception:" + e.getMessage());
                mac = null;
            }
        }
        TMKeyLog.d(TAG, "mac:"+mac);
        return (mac == null ? "" : mac) ;
    }

    /**
     * 获取手机的MAC地址,针对6.0系统不能通过系统API获取
     * @return
     */
    private static String getMac() {
        TMKeyLog.d(TAG, "getMac");
        String str="";
        String macSerial="";
        boolean hasWlan0 = false;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                TMKeyLog.d(TAG, "interfaces.hasMoreElements");
                NetworkInterface iF = interfaces.nextElement();

                byte[] addr = iF.getHardwareAddress();
                if (addr == null || addr.length == 0) {
                    continue;
                }

                StringBuilder buf = new StringBuilder();
                for (byte b : addr) {
                    buf.append(String.format("%02X:", b));
                }
                if (buf.length() > 0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                macSerial = buf.toString();
                String interfaceName = iF.getName();
                TMKeyLog.d("mac", "interfaceName=" + interfaceName + ", macSerial=" + macSerial);
                if (interfaceName.contains("wlan0")) {//如果标签名称包含wlan0则表示为WIFI的MAC地址
                    hasWlan0 = true;
                    break;
                }
            }
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            hasWlan0 = false;
        }
        if (! hasWlan0) {//如果读取的网络信息地址内面不包含wlan0，则将mac地址
            macSerial = "";
        }
        try {
            //如果获取不到则通过读取文件获取mac
            if (macSerial == null || "".equals(macSerial)) {
                TMKeyLog.d(TAG, "Runtime getRuntime().exec>>>cat /sys/class/net/wlan0/address");
                Process pp = Runtime.getRuntime().exec(
                        "cat /sys/class/net/wlan0/address ");
                InputStreamReader ir = new InputStreamReader(pp.getInputStream());
                LineNumberReader input = new LineNumberReader(ir);

                for (; null != str;) {
                    str = input.readLine();
                    if (str != null) {
                        macSerial = str.trim();// 去空格
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            TMKeyLog.e(TAG, "Exception:" + ex.getMessage());
            macSerial = null;
        }
        TMKeyLog.d(TAG, "macSerial:" + macSerial);
        if (macSerial == null || "".equals(macSerial)) {
            try {
                return loadFileAsString("/sys/class/net/eth0/address")
                        .toUpperCase().substring(0, 17);
            } catch (Exception e) {
                TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            }
        }
        return macSerial;
    }

    public static String loadFileAsString(String fileName) throws Exception {
        TMKeyLog.d(TAG, "loadFileAsString>>>" + fileName);
        FileReader reader = new FileReader(fileName);
        String text = loadReaderAsString(reader);
        TMKeyLog.d(TAG, "text:" + text);
        reader.close();
        return text;
    }

    public static String loadReaderAsString(Reader reader) throws Exception {
        TMKeyLog.d(TAG, "loadReaderAsString");
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int readLength = reader.read(buffer);
        while (readLength >= 0) {
            builder.append(buffer, 0, readLength);
            readLength = reader.read(buffer);
        }
        return builder.toString();
    }

    /**
     * 获取手机serial
     * @return
     */
    public static String getSerial () {
        String serial = null;
        try {
            serial = Build.SERIAL;
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            serial = null;
        }
        return serial == null ? "" : serial;
    }

    /**
     * 获取手机serial
     * @return
     */
    public static String getSerialNumber(){
        String serial = null;
        try {
            Class<?> c =Class.forName("android.os.SystemProperties");
            Method get =c.getMethod("get", String.class);
            serial = (String)get.invoke(c, "ro.serialno");
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            serial = null;
        }
        return serial == null ? "" : serial;
    }

    /**
     * 获取设备AndroidID
     * @param mContext
     * @return
     */
    public static String getAndroidId(Context mContext) {
        String aId = null;
        try {
            aId = android.provider.Settings.Secure.getString(
                    mContext.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            aId = null;
        }
        return aId == null ? "" : aId;
    }

    /**
     * 获取手机号
     * @param mContext
     * @return
     */
    public static String getPhoneNum (Context mContext) {
        String nativePhoneNumber = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            nativePhoneNumber = telephonyManager.getLine1Number();
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
            nativePhoneNumber = null;
        }
        return nativePhoneNumber == null ? "" : nativePhoneNumber;
    }

    /**
     *判断是否为平板
     * //测试手机基本上都是false，包括MI MAX（大屏）也是，基本上可以验证手机和平板
     * @return
     */
    public static boolean isTablet(Context context) {
        try {
            return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
        }
        return false;
    }

    /**
     *判断是否为平板
     * //测试发现大部分手机都为false，但是MI MAX测试为true，大屏手机可能为true，不准确
     * @return
     */
    public static boolean isPad(Context mContext) {
        try {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
//		//屏幕宽度
//		float screenWidth = display.getWidth();
//		//屏幕高度
//		float screenHeight = display.getHeight();
            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);
            double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
            double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
            //屏幕尺寸
            double screenInches = Math.sqrt(x + y);
            TMKeyLog.d(TAG, "screenInches:" + screenInches);
            //大于6尺寸则为Pad
            if (screenInches >= 6.0) {
                return true;
            }
        } catch (Exception e) {
            TMKeyLog.e(TAG, "Exception:" + e.getMessage());
        }
        return false;
    }

    /**
     * 获取应用名称
     * @param mContext
     * @return
     */
    public static String getApplicationName(Context mContext) {
        if (mContext == null) {
            return "";
        }
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = mContext.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(mContext.getPackageName(), 0);
            String applicationName =
                    (String) packageManager.getApplicationLabel(applicationInfo);
            return applicationName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取当前应用程序的包名
     * @param context 上下文对象
     * @return 返回包名
     */
    public static String getAppProcessName(Context context) {
        //当前应用pid
        int pid = android.os.Process.myPid();
        //任务管理类
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //遍历所有应用
        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.pid == pid)//得到当前应用
                return info.processName;//返回包名
        }
        return "";
    }

}
