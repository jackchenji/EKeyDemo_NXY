package com.froad.ukey.constant;

import com.froad.ukey.utils.np.TMKeyLog;

/**
 * @author Ali
 */
public class FConstant {

      private final static String TAG = "FConstant";
      public final static String LOG_TAG = "FroadEID_";//Sdk中log标签头
      public final static String fft_sdk_sp_name = "fft_sdk_sp_name";//sdk中需要保持数据的preferenceName
      public final static String UTF_8 = "UTF-8";//sdk中需要保持数据的preferenceName
      public final static long LOCKTIME = 60*60*1000;//锁定超时时间
      public final static long TRANSMITTINGTIME = 200;//发送指令时如果有其他指令正在处理时循环等待时间

      public final static int RES_FAIL_NORMAL_COMMADN_FORMAT = 1;//通用指令格式错误
      //错误码
      public final static int RES_SUCCESS = 0;//卡片指令处理成功
      public final static int RES_FAIL_CONTROL_BYTE = 2;//控制字节格式错误
      public final static int RES_FAIL_APP_COMMADN_FORMAT = 3;//应用指令格式错误
      public final static int RES_FAIL_MAC = 4;//MAC错误
      public final static int RES_FAIL_UNKNOWN_APP = 5;//未知应用指令错误
      public final static int RES_FAIL_APP_COMMADN_REPEAT = 6;//应用指令重复错误
      public final static int RES_FAIL_KEY_VERSION = 7;//密钥版本号不匹配
      public final static int RES_FAIL_STK_RUNNING = 9;//STK菜单正在运行，等待接收
      public final static int RES_FAIL_APDU = 10;//通道层指令数据处理失败
      public final static int RES_FAIL_VERIFY_PIN = 21;//PIN码校验错误
      public final static int RES_FAIL_CARD_LOCK = 22;//卡锁死错误
      public final static int RES_FAIL_SAFE_STATE = 23;//安全状态不满足
      public final static int RES_FAIL_CHANGE_PIN = 24;//强制修改PIN码
      public final static int RES_FAIL_PIN_SAME = 25;//新旧PIN码相同
      public final static int RES_FAIL_SIGN = 26;//签名失败
      public final static int RES_FAIL_DATATRN = 90;//数据层指令数据处理失败
      //SO处理指令错误状态
      public final static int RES_FAIL_VERIFY_MAC = 27;//接收数据MAC校验错误
      public final static int RES_FAIL_SEND_DATA = 28;//发送数据失败
      public final static int RES_FAIL_RECEIVE_DATA = 29;//接收数据失败
      public final static int RES_FAIL_CARD_NO_CONNECTED = 30;//卡片未连接
      public final static int RES_FAIL_CARD_TRNKEY_NO_UPDATE = 31;//传输密钥未更新
      public final static int RES_FAIL_SEND_DATA_ERROR = 32;//发送数据为空或者格式有误
      public final static int RES_FAIL_RECEIVE_DATA_ERROR = 33;//接收数据格式有误，无法解析
      public final static int RES_FAIL_CARD_VERSION_TOO_MUCH_ERROR = 34;//卡片COS版本太高，需要升级SDK
      public final static int RES_SUCCE_COS_IS_NEWEST = 35;//COS版本是最新版，不需要升级
      public final static int RES_FAIL_OTHER_ERROR = 36;//其他错误
      public final static int RES_FAIL_BIP_ERROR = 37;//BIP错误


      //卡端错误码对应的提示
      public static String RES_SUCCESS_STR = "卡片指令处理成功";
      public static String RES_FAIL_NORMAL_COMMADN_FORMAT_STR = "通用指令格式错误";
      public static String RES_FAIL_CONTROL_BYTE_STR = "控制字节格式错误";
      public static String RES_FAIL_APP_COMMADN_FORMAT_STR = "应用指令格式错误";
      public static String RES_FAIL_MAC_STR = "MAC错误";
      public static String RES_FAIL_UNKNOWN_APP_STR = "未知应用指令错误";
      public static String RES_FAIL_APP_COMMADN_REPEAT_STR = "应用指令重复错误";
      public static String RES_FAIL_KEY_VERSION_STR = "密钥版本号不匹配";
      public static String RES_FAIL_APDU_STR = "通道层指令数据处理失败";
      public static String RES_FAIL_VERIFY_PIN_STR = "PIN码校验错误";
      public static String RES_FAIL_CARD_LOCK_STR = "卡锁死错误";
      public static String RES_FAIL_SAFE_STATE_STR = "安全状态不满足";
      public static String RES_FAIL_CHANGE_PIN_STR = "需要修改PIN码";
      public static String RES_FAIL_PIN_SAME_STR = "新旧PIN码相同";
      public static String RES_FAIL_SIGN_STR = "签名失败";
      public static String RES_FAIL_DATATRN_STR = "数据层指令数据处理失败";
      public static String RES_FAIL_CARD_VERSION_TOO_MUCH_ERROR_STR = "卡片COS版本太高，需要升级SDK";
      //SO处理指令错误状态
      public static String RES_FAIL_VERIFY_MAC_STR = "接收数据MAC校验错误";
      public static String RES_FAIL_SEND_DATA_STR = "发送数据失败";
      public static String RES_FAIL_RECEIVE_DATA_STR = "接收数据失败";
      public static String RES_FAIL_CARD_NO_CONNECTED_STR = "卡片未连接，请先打开通道";
      public static String RES_FAIL_CARD_TRNKEY_NO_UPDATE_STR = "卡片密钥未更新，请先更新密钥";
      public static String RES_FAIL_SEND_DATA_ERROR_STR = "发送数据为空或格式有误，请检查";
      public static String RES_FAIL_RECEIVE_DATA_ERROR_STR = "接收数据格式有误，无法解析";
      public static String RES_FAIL_CHECK_PERMISSION_STR = "请检查是否开启了读/写短信和读/写联系人权限";
      public static String RES_SUCCE_COS_IS_NEWEST_STR = "当前卡片COS为最新版，不需要更新";
      public static String RES_FAIL_OTHER_ERROR_STR = "其他错误";


      /**
       * 通过错误码得到对应的错误提示语
       *
       * @param errCode 错误码
       * @return 错误提示
       */
      public static String getCardErrorMsg(int errCode) {
            TMKeyLog.d(TAG, "getCardErrorMsg>>>errCode:" + errCode);
            switch (errCode) {
                  case RES_SUCCESS:
                        return RES_SUCCESS_STR;
                  case RES_FAIL_NORMAL_COMMADN_FORMAT:
                        return RES_FAIL_NORMAL_COMMADN_FORMAT_STR;
                  case RES_FAIL_CONTROL_BYTE:
                        return RES_FAIL_CONTROL_BYTE_STR;
                  case RES_FAIL_APP_COMMADN_FORMAT:
                        return RES_FAIL_APP_COMMADN_FORMAT_STR;
                  case RES_FAIL_MAC:
                        return RES_FAIL_MAC_STR;
                  case RES_FAIL_UNKNOWN_APP:
                        return RES_FAIL_UNKNOWN_APP_STR;
                  case RES_FAIL_APP_COMMADN_REPEAT:
                        return RES_FAIL_APP_COMMADN_REPEAT_STR;
                  case RES_FAIL_KEY_VERSION:
                        return RES_FAIL_KEY_VERSION_STR;
                  case RES_FAIL_APDU:
                        return RES_FAIL_APDU_STR;
                  case RES_FAIL_VERIFY_PIN:
                        return RES_FAIL_VERIFY_PIN_STR;
                  case RES_FAIL_CARD_LOCK:
                        return RES_FAIL_CARD_LOCK_STR;
                  case RES_FAIL_SAFE_STATE:
                        return RES_FAIL_SAFE_STATE_STR;
                  case RES_FAIL_CHANGE_PIN:
                        return RES_FAIL_CHANGE_PIN_STR;
                  case RES_FAIL_PIN_SAME:
                        return RES_FAIL_PIN_SAME_STR;
                  case RES_FAIL_SIGN:
                        return RES_FAIL_SIGN_STR;
                  case RES_FAIL_VERIFY_MAC:
                        return RES_FAIL_VERIFY_MAC_STR;
                  case RES_FAIL_SEND_DATA:
                        return RES_FAIL_SEND_DATA_STR;
                  case RES_FAIL_DATATRN:
                        return RES_FAIL_DATATRN_STR;
                  case RES_FAIL_RECEIVE_DATA:
                        return RES_FAIL_RECEIVE_DATA_STR;
                  case RES_FAIL_CARD_NO_CONNECTED:
                        return RES_FAIL_CARD_NO_CONNECTED_STR;
                  case RES_FAIL_CARD_TRNKEY_NO_UPDATE:
                        return RES_FAIL_CARD_TRNKEY_NO_UPDATE_STR;
                  case RES_FAIL_SEND_DATA_ERROR:
                        return RES_FAIL_SEND_DATA_ERROR_STR;
                  case RES_FAIL_RECEIVE_DATA_ERROR:
                        return RES_FAIL_RECEIVE_DATA_ERROR_STR;
                  case RES_FAIL_CARD_VERSION_TOO_MUCH_ERROR:
                        return RES_FAIL_CARD_VERSION_TOO_MUCH_ERROR_STR;
                  case RES_SUCCE_COS_IS_NEWEST:
                        return RES_SUCCE_COS_IS_NEWEST_STR;
                  default:
                        return RES_FAIL_OTHER_ERROR_STR;
            }
      }
}
