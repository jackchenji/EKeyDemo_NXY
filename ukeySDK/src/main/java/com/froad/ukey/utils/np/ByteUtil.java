package com.froad.ukey.utils.np;

import android.telephony.SmsMessage;

import com.froad.ukey.constant.FConstant;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * byte转换工具
 *
 * @author lwz
 * @date 2016-6-22
 */
public class ByteUtil {

      private static final String TAG = FConstant.LOG_TAG + "ByteUtil";
      /**
       * 默认编码
       */
      public final static String DEFAULT_CHARSET = "GBK";

      /**
       * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt2（）配套使用
       * 小端
       *
       * @param value 要转换的int值
       * @return byte数组
       */
      public static byte[] intToBytes2(int value) {
            byte[] src = new byte[4];
            src[3] = (byte) ((value >> 24) & 0xFF);
            src[2] = (byte) ((value >> 16) & 0xFF);
            src[1] = (byte) ((value >> 8) & 0xFF);
            src[0] = (byte) (value & 0xFF);
            return src;
      }

      /**
       * 将int数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。 和bytesToInt（）配套使用
       * 大端
       */
      public static byte[] intToBytes(int value) {
            byte[] src = new byte[4];
            src[0] = (byte) ((value >> 24) & 0xFF);
            src[1] = (byte) ((value >> 16) & 0xFF);
            src[2] = (byte) ((value >> 8) & 0xFF);
            src[3] = (byte) (value & 0xFF);
            return src;
      }

      /**
       * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes2（）配套使用
       * 小端
       *
       * @param src    byte数组
       * @param offset 从数组的第offset位开始
       * @return int数值
       */
      public static int bytesToInt2(byte[] src, int offset) {
            int value;
            value = (int) ((src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8)
                    | ((src[offset + 2] & 0xFF) << 16) | ((src[offset + 3] & 0xFF) << 24));
            return value;
      }

      /**
       * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes（）配套使用
       * 大端
       */
      public static int bytesToInt(byte[] src, int offset) {
            int value;
            value = (int) (((src[offset] & 0xFF) << 24)
                    | ((src[offset + 1] & 0xFF) << 16)
                    | ((src[offset + 2] & 0xFF) << 8) | (src[offset + 3] & 0xFF));
            return value;
      }

      /**
       * byte转boolean
       *
       * @param b
       * @return
       */
      public static boolean bytesToBoolean(byte b) {

            return b == 0x01;
      }

      /**
       * boolean转byte
       *
       * @param b
       * @return
       */
      public static byte booleanToByte(boolean b) {

            return (byte) (b ? 0x01 : 0x00);
      }


      /**
       * byte[]转hex
       *
       * @param src
       * @return
       */
      public static String bytesToHexStr(byte[] src) {
            StringBuilder stringBuilder = new StringBuilder("");
            if (src == null || src.length <= 0) {
                  return null;
            }
            for (int i = 0; i < src.length; i++) {
                  int v = src[i] & 0xFF;
                  String hv = Integer.toHexString(v);
                  if (hv.length() < 2) {
                        stringBuilder.append(0);
                  }
                  stringBuilder.append(hv);
            }
            return stringBuilder.toString().toUpperCase();
      }

      /**
       * hex转byte[]
       *
       * @param hexString
       * @return
       */
      public static byte[] hexStrToBytes(String hexString) {
            if (hexString == null || hexString.equals("")) {
                  return null;
            }
            if (hexString.length() % 2 != 0) {
                  hexString = "F" + hexString;
            }
            hexString = hexString.toUpperCase();
            int length = hexString.length() / 2;
            char[] hexChars = hexString.toCharArray();
            byte[] d = new byte[length];
            for (int i = 0; i < length; i++) {
                  int pos = i * 2;
                  d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
            }
            return d;
      }

      /**
       * Convert char to byte
       *
       * @param c char
       * @return byte
       */
      private static byte charToByte(char c) {
            return (byte) "0123456789ABCDEF".indexOf(c);
      }


      /**
       * bytes转String
       *
       * @return
       */
      public static String bytesToStr(byte[] bytes) {
            try {
                  return new String(bytes, DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                  e.printStackTrace();
            }
            return null;
      }

      /**
       * String转bytes
       *
       * @return
       */
      public static byte[] strToBytes(String str) {
            try {
                  return str.getBytes(DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                  e.printStackTrace();
            }
            return null;
      }

      /**
       * 字节拼接
       *
       * @param bytes1
       * @param bytes2
       */
      public static byte[] mergeBytes(byte[] bytes1, byte[] bytes2) {

            byte[] bytes = new byte[bytes1.length + bytes2.length];
            System.arraycopy(bytes1, 0, bytes, 0, bytes1.length);
            System.arraycopy(bytes2, 0, bytes, bytes1.length, bytes2.length);

            return bytes;
      }

      /**
       * 字节剪切
       *
       * @param bytes
       * @param pos
       * @param length
       * @return
       */
      public static byte[] cutBytes(byte[] bytes, int pos, int length) {

            byte[] newBytes = new byte[length];

            System.arraycopy(bytes, pos, newBytes, 0, length);

            return newBytes;
      }

      /**
       * 字节拼接
       *
       * @param bytes1
       * @param byte2
       */
      public static byte[] mergeBytes(byte[] bytes1, byte byte2) {
            return mergeBytes(bytes1, new byte[]{byte2});
      }

      /**
       * 字节拼接
       *
       * @param byte1
       * @param bytes2
       */
      public static byte[] mergeBytes(byte byte1, byte[] bytes2) {
            return mergeBytes(new byte[]{byte1}, bytes2);
      }


      /**
       * byte[]转number ,支持number+"-"、" "、"*"、":"、".",转换
       *
       * @param src
       * @return
       */
      public static String bytesToNumStr(byte[] src) {

            return hexStrToSpecialChar(bytesToHexStr(src));
      }

      /**
       * num转byte,支持number+"-"、" "、"*"、":"、".",转换
       *
       * @param numStr
       * @return
       */
      public static byte[] numStrToBytes(String numStr) {

            return hexStrToBytes(specialCharToHexStr(numStr));
      }

      private static String specialCharToHexStr(String str) {
            if (str == null || "".equals(str)) {
                  return null;
            }
            initSpecialCharMap();
            for (String key : specialCharMap.keySet()) {
                  String value = specialCharMap.get(key);
                  if ("".equals(value)) {
                        continue;
                  }
                  str = str.replaceAll(Pattern.quote(value), key);
            }
            if (str.length() % 2 != 0) {
                  str = "F" + str;
            }
            return str;
      }

      private static String hexStrToSpecialChar(String hexStr) {
            if (hexStr == null || "".equals(hexStr)) {
                  return null;
            }
            initSpecialCharMap();
            for (String key : specialCharMap.keySet()) {
                  String value = specialCharMap.get(key);
                  hexStr = hexStr.replaceAll(key, value);
            }
            return hexStr;
      }

      private static Map<String, String> specialCharMap;

      private static void initSpecialCharMap() {
            if (specialCharMap == null) {
                  synchronized (DEFAULT_CHARSET) {
                        if (specialCharMap == null) {
                              specialCharMap = new HashMap<String, String>();
                              specialCharMap.put("A", "*");
                              specialCharMap.put("B", "-");
                              specialCharMap.put("C", ".");
                              specialCharMap.put("D", ":");
                              specialCharMap.put("E", " ");
                              specialCharMap.put("F", "");
                        }
                  }
            }
      }

      public static void main(String[] args) {
            String s = "2015-21-53** 21:12:20";
            System.out.println(specialCharToHexStr(s));
            System.out.println(hexStrToSpecialChar(specialCharToHexStr(s)));
      }

      /**
       * 处理移位操作
       *
       * @param b
       * @return
       */
      public static byte[] dealShift(byte[] b, int shiftValue) {
            TMKeyLog.e(TAG, "b:" + FCharUtils.bytesToHexStr(b) + ">>>shiftValue:" + shiftValue);
            if (b == null) {
                  return null;
            }
            if (shiftValue < 1 || shiftValue > 7) {
                  return b;
            }
            int bl = b.length;
            byte[] tempBytes = new byte[bl];
            System.arraycopy(b, 0, tempBytes, 0, bl);
            char maskH = (char) (0xFF << shiftValue);
            TMKeyLog.e(TAG, "maskH:" + FCharUtils.bytesToHexStr(new byte[]{(byte) (maskH & 0x00FF)}));
            char maskL = (char) (0xFF >> (8 - shiftValue));
            TMKeyLog.e(TAG, "maskL:" + FCharUtils.bytesToHexStr(new byte[]{(byte) (maskL & 0x00FF)}));

            int i = 0;
            for (i = 0; i < bl - 1; i++) {
                  tempBytes[i] = charToByteNew((char) ((tempBytes[i] << shiftValue) & maskH));
                  tempBytes[i] = charToByteNew((char) (tempBytes[i] | ((tempBytes[i + 1] >> (8 - shiftValue)) & maskL)));
            }
            tempBytes[i] = charToByteNew((char) ((tempBytes[i] << shiftValue) & maskH));
            TMKeyLog.e(TAG, "shift end tempBytes:" + FCharUtils.bytesToHexStr(tempBytes));
            return tempBytes;
      }

      public static byte[] dealReverseShift(byte[] b, int shiftValue) {
            TMKeyLog.e(TAG, "b:" + FCharUtils.bytesToHexStr(b) + ">>>shiftValue:" + shiftValue);
            if (b == null) {
                  return null;
            }
            if (shiftValue < 1 || shiftValue > 7) {
                  return b;
            }
            int bl = b.length;
            byte[] tempBytes = new byte[bl];
            System.arraycopy(b, 0, tempBytes, 0, bl);
            char maskH = (char) (0xFF << shiftValue);
            TMKeyLog.e(TAG, "maskH:" + FCharUtils.bytesToHexStr(new byte[]{(byte) (maskH & 0x00FF)}));
            char maskL = (char) (0xFF >> (8 - shiftValue));
            TMKeyLog.e(TAG, "maskL:" + FCharUtils.bytesToHexStr(new byte[]{(byte) (maskL & 0x00FF)}));

            int i = 0;
            for (i = 0; i < bl - 1; i++) {
                  tempBytes[i] = charToByteNew((char) ((tempBytes[i] << shiftValue) & maskH));
                  tempBytes[i] = charToByteNew((char) (tempBytes[i] | ((tempBytes[i + 1] >> (8 - shiftValue)) & maskL)));
            }
            tempBytes[i] = charToByteNew((char) ((tempBytes[i] << shiftValue) & maskH));
            TMKeyLog.e(TAG, "shift end tempBytes:" + FCharUtils.bytesToHexStr(tempBytes));
            return tempBytes;
      }

      public static byte charToByteNew(char c) {
            return (byte) (c & 0x00FF);
      }

      public static boolean invokeMethod(Method method, Object object, int i1, int i2, byte[] b1){
            try{
                  boolean bool = (Boolean)method.invoke(object, i1, i2, b1);
                  return bool;
            }catch (Exception e){
                  e.printStackTrace();
            }
            return false;
      }

      public static ArrayList<SmsMessage> invokeMethod1(Method method, Object object){
            try{
                  ArrayList<SmsMessage> res = (ArrayList<SmsMessage>)method.invoke(object);
                  return res;
            }catch (Exception e){
                  e.printStackTrace();
            }
            return null;
      }

}
