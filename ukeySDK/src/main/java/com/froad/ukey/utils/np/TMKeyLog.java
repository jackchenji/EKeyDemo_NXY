package com.froad.ukey.utils.np;

import android.util.Log;

import bip.LogUtility;

public class TMKeyLog {

      /**
       * 打印Log控制开关，0为不打印，1为打印
       */
      private static int outputLogLevel = 1;
      public static StringBuffer sbf = new StringBuffer();

      public static void e(String tag, String msg) {
            if (0 != outputLogLevel) {
                  Log.e(tag, msg);
                  printLogShow(tag, msg);
                  LogUtility.getInstance().appendLog(msg);
            }
      }

      public static void e(String tag, String msg, Throwable t) {
            if (0 != outputLogLevel) {
                  Log.e(tag, msg, t);
                  printLogShow(tag, msg);
                  LogUtility.getInstance().appendLog(msg);
            }
      }

      public static void i(String tag, String msg) {
            if (0 != outputLogLevel) {
                  Log.i(tag, msg);
                  printLogShow(tag, msg);
                  LogUtility.getInstance().appendLog(msg);
            }
      }

      public static void i(String tag, String msg, Throwable tr) {
            if (0 != outputLogLevel) {
                  Log.i(tag, msg, tr);
                  printLogShow(tag, msg);
                  LogUtility.getInstance().appendLog(msg);
            }
      }

      public static void d(String tag, String msg) {
            if (0 != outputLogLevel) {
                  Log.e(tag, msg);
                  printLogShow(tag, msg);
                  LogUtility.getInstance().appendLog(msg);
            }
      }

      public static void w(String tag, String msg) {
            if (0 != outputLogLevel) {
                  Log.w(tag, msg);
                  printLogShow(tag, msg);
                  LogUtility.getInstance().appendLog(msg);
            }
      }

      public static void v(String tag, String msg) {
            if (0 != outputLogLevel) {
                  Log.v(tag, msg);
                  printLogShow(tag, msg);
            }
      }

      private static void printLogShow (String tag, String msg) {
            boolean isShow = true;//是否需要在屏幕上显示LOG
            if (isShow) {
                  sbf.append(tag + "-->" + msg + "\n");
            }
      }

      public static void newSbf () {
            sbf = new StringBuffer();
      }
}
