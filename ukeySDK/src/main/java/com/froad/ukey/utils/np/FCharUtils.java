package com.froad.ukey.utils.np;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.DisplayMetrics;

import com.froad.ukey.constant.FConstant;
import com.froad.ukey.utils.PkcsInfoUtil;

import org.bouncycastle.asn1.x509.X509CertificateStructure;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.cert.CertificateEncodingException;
import javax.security.cert.X509Certificate;

/**
 * @ClassName: FCharUtils
 * @Description: TODO
 * @author: froad-Floyd_feng 2015年7月28日
 * @modify: froad-Floyd_feng 2015年7月28日
 */
public class FCharUtils {

      private static String TAG = FConstant.LOG_TAG + "FCharUtils";

      public static char[] stringToASCIIArray(String s) {
            if (s == null) {
                  return new char[0];
            }
            return s.toCharArray();
      }

      /**
       * @param s
       * @return
       */
      public static byte[] stringToByteArray(String s) {
            int sl = s.length();
            byte[] charArray = new byte[sl];
            for (int i = 0; i < sl; i++) {
                  char charElement = s.charAt(i);
                  charArray[i] = (byte) charElement;
            }
            return charArray;
      }

      /**
       * @param bs
       * @return
       */
      public static char[] byteToCharArray(byte[] bs) {
            int bsl = bs.length;
            char[] charArray = new char[bsl];
            for (int i = 0; i < bsl; i++) {
                  charArray[i] = (char) (((char) bs[i]) & 0x00FF);
            }
            return charArray;
      }

      /**
       * @param b
       * @return
       */
      public static char byte2char(byte b) {
            return (char) (((char) b) & 0x00FF);
      }

      /**
       * @Title: int2HexStr
       * @Description: 将int型转换为16进制字符串
       * @author: Floyd_feng 2015年11月19日
       * @modify: Floyd_feng 2015年11月19日
       * @param: i
       */
      public static String int2HexStr(int i) {
            i = i & 0xFF;
            String si = Integer.toHexString(i);
            if (si.length() < 2) {
                  si = "0" + si;
            }
            si = si.toUpperCase();
            return si;
      }

      /**
       * 将长度转换为两字节Hex编码
       * @param i
       * @return
       */
      public static String int2HexStr2(int i) {
            i = i & 0xFFFF;
            String si = Integer.toHexString(i);
            int sl = si.length();
            if (sl < 4) {
                  StringBuffer sbf = new StringBuffer();
                  for (int k = 0; k < 4 - sl; k++ ) {
                        sbf.append("0");
                  }
                  sbf.append(si);
                  si = sbf.toString();
            } else {
                  si = si.substring(sl - 4);
            }
            si = si.toUpperCase();
            return si;
      }

      /**
       * 将数字转换为BCD码
       *
       * @param i
       * @return
       */
      public static String int2BCDStr(int i) {
            String is = "" + i;
            if ((is.length() % 2) != 0) {
                  is = "0" + is;
            }
            return is;
      }


      /**
       * 将长度转换，如果大于127，则用两字节表示，第一字节高位高字节不参与计算，否则，用一字节表示
       *
       * @param l
       * @return
       */
      public static String len2HexStr(int l) {
            String s = "";
            if (l > 127) {//长度大于127则用两字节表示
                  int ol1 = (char) (l >> 8);
                  int ol2 = (char) (l - (int) (ol1 << 8));
                  ol1 = ol1 + 0x80;
                  s = int2HexStr(ol1) + int2HexStr(ol2);
            } else {
                  s = int2HexStr(l);
            }
            s = s.toUpperCase();
            return s;
      }

    /**
     * EID指令交互通过P1P2表示地址偏移
     * @param l
     * @return
     */
      public static String[] len2P1P2 (int l) {
            String st = Integer.toHexString(l);
            int stl = 4 - st.length();
            if (stl < 0) {
                  return null;
            }
            for (int i = 0; i < stl ; i++){
                  st = "0" + st;
            }
            return new String[] {st.substring(0,2), st.substring(2)};
      }

      /**
       * 将长度字符串转换为数字
       *
       * @param st
       * @return
       */
      public static int hexStr2Len(String st) {
            int t = 0;
            if (st == null || "".equals(st)) {
                  return 0;
            } else {
                  if (st.length() == 4) {//长度大于127则用两字节表示
                        String t1 = st.substring(0, 2);
                        String t2 = st.substring(2, 4);
                        t = ((Integer.parseInt(t1, 16) - 0x80) << 8) + Integer.parseInt(t2, 16);
                  } else if (st.length() == 2 || st.length() == 1) {
                        t = Integer.parseInt(st, 16);
                  } else {
                        t = 0;
                  }
            }
            return t;
      }

      /**
       * BCD码字符串转成int
       *
       * @param st
       * @return 失败默认返回-1
       */
      public static int bcdStr2Int(String st) {
            int t = -1;
            if (st == null || "".equals(st)) {
                  return -1;
            } else { //十进制转换
                  t = Integer.parseInt(st, 10);
            }
            return t;
      }

      /**
       * 将字符串转换为16进制格式字符串
       *
       * @param s
       * @return
       */
      public static String string2HexStr(String s) {
            try {
                  byte[] bs = s.getBytes("UTF-8");
                  return showResult16Str(bs);
            } catch (UnsupportedEncodingException ue) {
                  ue.printStackTrace();
            }
            return null;
      }

      /**
       * 将字符串转换为16进制格式字符串
       *
       * @param s
       * @param enCode
       * @return
       */
      public static String string2HexStr(String s, String enCode) {
            try {
                  byte[] bs = s.getBytes(enCode);
                  return showResult16Str(bs);
            } catch (UnsupportedEncodingException ue) {
                  ue.printStackTrace();
            }
            return null;
      }

      /**
       * 将16进制字符串转换为常规字符串
       *
       * @param s 16进制字符串
       * @param s 编码
       * @return
       */
      public static String hexStr2String(String s, String encodeType) {
            try {
                  return new String(hexString2ByteArray(s), encodeType);
            } catch (UnsupportedEncodingException ue) {
                  ue.printStackTrace();
            }
            return null;
      }

      /**
       * hex字符串转unicode编码
       * @param s
       * @return
       */
      public static String hexStr2UCString(String s) {
            if (s == null) {
                  return "";
            }
            int sl = s.length();
            if (sl % 4 != 0) {
                  return "";
            }
            StringBuffer sbf = new StringBuffer();
            for (int i = 0; i < sl; i = i + 4) {
                  sbf.append((char)Integer.parseInt(s.substring(i, i + 4),16));
            }
            return sbf.toString();
      }

      /**
       * 将16进制数据转换为LV格式数据
       *
       * @param s
       * @return
       */
      public static String hexStr2LV(String s) {
            if (s == null || "".equals(s)) {
                  return "";
            }
            String rs = FCharUtils.len2HexStr(s.length() / 2) + s;
            TMKeyLog.i(TAG, "hexStr2LV>>>rs:" + rs);
            return rs;
      }

      /**
       * 在数据前补一字节的长度
       * @param s
       * @return
       */
      public static String hexStr2LV_1(String s) {
            if (s == null || "".equals(s)) {
                  return "";
            }
            String rs = FCharUtils.int2HexStr(s.length() / 2) + s;
            return rs;
      }

      /**
       * 在数据前补两字节的长度
       * @param s
       * @return
       */
      public static String hexStr2LV_2(String s) {
            if (s == null || "".equals(s)) {
                  return "";
            }
            String rs = FCharUtils.int2HexStr2(s.length() / 2) + s;
            return rs;
      }

      /**
       * @Title: hexString2ByteArray
       * @Description: 将16进制字符串转成byte数组
       * @author: Floyd_feng 2015年12月10日
       * @modify: Floyd_feng 2015年12月10日
       * @param: @param bs
       * @param: @return
       * @throws：
       */
      public static byte[] hexString2ByteArray(String bs) {
            int bsLength = bs.length();
            if (bsLength % 2 != 0) {
                  return null;
            }
            byte[] cs = new byte[bsLength / 2];
            String st = "";
            for (int i = 0; i < bsLength; i = i + 2) {
                  st = bs.substring(i, i + 2);
                  cs[i / 2] = (byte) Integer.parseInt(st, 16);
            }
            return cs;
      }

      /**
       * 处理hex字符串，忽略非Hex字符错误
       * @param hexStr
       * @return
       */
      public static byte[] unHex2Bytes (String hexStr) {
            if (hexStr == null) {
                  return null;
            }
            hexStr = hexStr.replaceAll(" ", "");
            int hLen = hexStr.length();
            if (hLen % 2 != 0) {
                  hexStr = "0" + hexStr;
            }
            hLen = hexStr.length();
            byte[] ba1 = hexStr.getBytes();
            byte[] ba = new byte[hLen / 2];
            for (int i = 0 ; i < hLen; i = i + 2) {
                  ba[i / 2] = (byte)(h2b(ba1[i]) * 0x10 + h2b(ba1[i + 1]));
            }
            return ba;
      }

      private static byte h2b(byte c){
            if (c<='9' && c>='0'){
                  return (byte) (c-'0');
            }else if (c>='a' && c<='z'){
                  return (byte) (c-'a'+10);
            }else{
                  return (byte) (c-'A'+10);
            }
      }

      /**
       * @throws
       * @Title: showResult16Str
       * @Description: 将byte数组转成16进制字符串
       * @author: Floyd_feng 2015年12月10日
       * @modify: Floyd_feng 2015年12月10日
       * @param: @param b
       * @param: @return
       */
      @SuppressLint("DefaultLocale")
      public static String showResult16Str(byte[] b) {
            if (b == null) {
                  return "";
            }
            StringBuffer sbf= new StringBuffer();
            int bl = b.length;
            byte bt;
            String bts = "";
            int btsl;
            for (int i = 0; i < bl; i++) {
                  bt = b[i];
                  bts = Integer.toHexString(bt);
                  btsl = bts.length();
                  if (btsl > 2) {
                        bts = bts.substring(btsl - 2).toUpperCase();
                  } else if (btsl == 1) {
                        bts = "0" + bts.toUpperCase();
                  } else {
                        bts = bts.toUpperCase();
                  }
                  // System.out.println("i::"+i+">>>bts::"+bts);
                  sbf.append(bts);
            }
            return sbf.toString();
      }

      /**
       * @throws
       * @Title: showResult0xStr
       * @Description: 将byte数组转成16进制字符串，0x01 0x02 0x03形式
       * @author: Floyd_feng 2015年12月10日
       * @modify: Floyd_feng 2015年12月10日
       * @param: @param b
       * @param: @return
       */
      public static String showResult0xStr(byte[] b) {
            String rs = "";
            int bl = b.length;
            byte bt;
            String bts = "";
            int btsl;
            for (int i = 0; i < bl; i++) {
                  bt = b[i];
                  bts = Integer.toHexString(bt);
                  btsl = bts.length();
                  if (btsl > 2) {
                        bts = "0x" + bts.substring(btsl - 2);
                  } else if (btsl == 1) {
                        bts = "0x0" + bts;
                  } else {
                        bts = "0x" + bts;
                  }
                  // System.out.println("i::"+i+">>>bts::"+bts);
                  rs += bts + " ";
            }
            // System.out.println("rs::"+rs);
            return rs;
      }

      /**
       * @param str
       */
      public static String enc21Int(String str) {
            try {
                  int len = str.length();
                  int endi = 0;
                  int sum = 0;
                  char[] cs = str.toCharArray();
                  for (int i = 0; i < len; i++) {
                        sum = sum + cs[i] * (i % 2 == 0 ? 2 : 1);
                  }
                  // System.out.println("enc21Int>>>sum::"+sum);
                  endi = (10 - (sum % 10)) % 10;
                  return str + endi;
            } catch (Exception e) {
                  return "ERROR";
            }
      }

      /**
       * @param str
       */
      public static String checkOutStr_21(String str) {
            try {
                  int len = str.length();
                  int endi = Integer.parseInt(str.substring(len - 1, len));
                  int sum = 0;
                  char[] cs = str.substring(0, len - 1).toCharArray();
                  int i1 = 2;
                  for (int i = 0; i < len - 1; i++) {
                        if (i % 2 == 0) {
                              i1 = 2;
                        } else {
                              i1 = 1;
                        }
                        sum = sum + cs[i] * i1;
                  }
                  // System.out.println("checkOutStr_21>>>sum::"+sum);
                  if (endi == (10 - (sum % 10)) % 10) {
                        return str.substring(0, len - 1);
                  }
            } catch (Exception e) {
                  return "ERROR";
            }
            return "ERROR";
      }

      /**
       * @param str
       */
      public static String encSumInt(String str) {
            try {
                  int len = str.length();
                  int sum = 0;
                  char[] cs = str.toCharArray();
                  for (int i = 0; i < len; i++) {
                        sum = sum + cs[i];
                  }
                  String sumStr = Integer.toHexString(sum);
                  // System.out.println("sum_str::"+sum_str);
                  int ssl = sumStr.length();
                  if (ssl < 2) {
                        char nc = (char) sum;
                        return str + nc;
                  }
                  String sumStrSub = sumStr.substring(ssl - 2);
                  int ci = Integer.decode("0x" + sumStrSub);
                  char c = (char) ci;
                  return str + c;
            } catch (Exception e) {
                  return "ERROR";
            }
      }

      /**
       * @throws
       * @Title: checkOutStr_sum
       * @Description: 通过校验和的方式校验指令数据
       * @author: Floyd_feng 2015年7月27日
       * @modify: Floyd_feng 2015年7月27日
       */
      public static String checkOutStrSum(String str) {
            try {
                  int len = str.length();
                  if (len < 3) {
                        return "ERROR";
                  }
                  String strInt = str.substring(0, len - 1);
                  String strSub = str.substring(len - 1, len);
                  int sum = 0;
                  char[] cs = strInt.toCharArray();
                  for (int i = 0; i < len - 1; i++) {
                        sum = sum + cs[i];
                  }
                  String sumStr = Integer.toHexString(sum);
                  // System.out.println("checkOutStr_sum>>>sum_str::"+sum_str);
                  int ssl = sumStr.length();
                  if (ssl < 2) {
                        if (strSub.charAt(0) == sum) {
                              return strInt;
                        }
                  } else {
                        String sumStrSub = sumStr.substring(ssl - 2);
                        if (strSub.charAt(0) == Integer.decode("0x" + sumStrSub)) {
                              return strInt;
                        }
                  }
            } catch (Exception e) {
                  return "ERROR";
            }
            return "ERROR";
      }

      /**
       * @param str
       */
      public static char encLRCInt(String str) {
            int len = str.length();
            int sum = 0;
            char[] cs = str.toCharArray();
            for (int i = 0; i < len; i++) {
                  sum = sum + cs[i];
            }
            String sumStr = Integer.toHexString(sum);
            // System.out.println("sum_str::"+sum_str);
            int ssl = sumStr.length();
            if (ssl > 2) {
                  sumStr = sumStr.substring(ssl - 2);
            }
            int ci = Integer.decode("0x" + sumStr);
            char c = (char) ci;
            char cF = 0xFF;
            char c1 = 0x01;
            char nc = (char) (cF - c);
            char ncLrc = (char) ((char) (nc + c1) & 0x00FF);
            // System.out.println("ncLrc::"+ncLrc+">>>int(ncLrc)::"+(int)ncLrc);
            return ncLrc;
      }

      /**
       * @param str
       */
      public static boolean checkOutStr_LRC(String str) {
            int len = str.length();
            if (len < 3) {
                  return false;
            }
            String strInt = str.substring(0, len - 1);
            String strSub = str.substring(len - 1, len);
            int sum = 0;
            char[] cs = strInt.toCharArray();
            for (int i = 0; i < len - 1; i++) {
                  sum = sum + cs[i];
            }
            String sumStr = Integer.toHexString(sum);
            // System.out.println("checkOutStr_sum>>>sum_str::"+sum_str);
            int ssl = sumStr.length();
            if (ssl > 2) {
                  sumStr = sumStr.substring(ssl - 2);
            }
            int ci = Integer.decode("0x" + sumStr);
            char c = (char) ci;
            char cF = 0xFF;
            char c1 = 0x01;
            char nc = (char) (cF - c);
            char ncLrc = (char) ((char) (nc + c1) & 0x00FF);
            // System.out.println("checkOutStr_sum>>>ncLrc::"+(int)ncLrc);
            if (strSub.charAt(0) == ncLrc) {
                  return true;
            }
            return false;
      }

      /**
       * @throws
       * @Title: encXORInt
       * @Description: TODO
       * @author: Floyd_feng 2015年7月28日
       * @modify: Floyd_feng 2015年7月28日
       */
      public static char encXORInt(String str) {
            int len = str.length();
            char tor = 0;
            char[] cs = str.toCharArray();
            for (int i = 0; i < len; i++) {
                  tor = (char) (tor ^ cs[i]);
            }
            // System.out.println("tor::"+tor+">>>int(tor)::"+(int)tor);
            return tor;
      }

      /**
       * @param str
       */
      public static boolean checkOutStr_XOR(String str) {
            int len = str.length();
            if (len < 3) {
                  return false;
            }
            String strInt = str.substring(0, len - 1);
            String strSub = str.substring(len - 1, len);
            char tor = 0;
            char[] cs = strInt.toCharArray();
            for (int i = 0; i < len - 2; i++) {
                  tor = (char) (tor ^ cs[i]);
            }
            // System.out.println("tor::"+tor+">>>int(tor)::"+(int)tor);
            if (strSub.charAt(0) == tor) {
                  return true;
            }
            return false;
      }

      /**
       * 检测数字字符串
       *
       * @param str
       * @return
       */
      public static boolean checkNum(String str) {
            String regEx = "[^0-9]";
            Pattern p = Pattern.compile(regEx);
            Matcher m;
            boolean isNum = true;
            char[] pns = str.toCharArray();
            int pl = pns.length;
            for (int i = 0; i < pl; i++) {
                  m = p.matcher("" + pns[i]);
                  if (m.find()) {
                        isNum = false;
                        break;
                  }
            }
            return isNum;
      }

      /**
       * 检测数字和点字符串
       *
       * @param str
       * @return
       */
      public static boolean checkNumIsMoney(String str) {
            String regEx = "[^0-9.]";
            Pattern p = Pattern.compile(regEx);
            Matcher m;
            boolean isNum = true;
            char[] pns = str.toCharArray();
            int pl = pns.length;
            for (int i = 0; i < pl; i++) {
                  m = p.matcher("" + pns[i]);
                  if (m.find()) {
                        isNum = false;
                        break;
                  }
            }
            return isNum;
      }

      /**
       * 检测数字和字母字符串
       *
       * @param str
       * @return
       */
      public static boolean checkNumOrZ(String str) {
            String regEx = "[^(0-9a-zA-Z)]";
            Pattern p = Pattern.compile(regEx);
            Matcher m;
            boolean isNum = true;
            char[] pns = str.toCharArray();
            int pl = pns.length;
            for (int i = 0; i < pl; i++) {
                  m = p.matcher("" + pns[i]);
                  if (m.find()) {
                        isNum = false;
                        break;
                  }
            }
            return isNum;
      }

      /**
       * 检测十六进制字符串
       *
       * @param str
       * @return
       */
      public static boolean checkHexStr(String str) {
            String regEx = "[^(0-9a-fA-F)]";
            Pattern p = Pattern.compile(regEx);
            Matcher m;
            boolean isNum = true;
            char[] pns = str.toCharArray();
            int pl = pns.length;
            for (int i = 0; i < pl; i++) {
                  m = p.matcher("" + pns[i]);
                  if (m.find()) {
                        isNum = false;
                        break;
                  }
            }
            return isNum;
      }

      /**
       * 检测蓝牙4.0
       */
      public static boolean checkBleVer() {
            try {
                  @SuppressWarnings("rawtypes")
                  Class cls = Class
                          .forName("android.bluetooth.BluetoothAdapter$LeScanCallback");
                  if (cls != null) {
                        return true;
                  }
            } catch (Exception e) {
                  e.printStackTrace();
            }
            return false;
      }

      @SuppressLint("DefaultLocale")
      public static String toHexCode(byte[] data, int offset, int length) {
            final StringBuilder stringBuilder = new StringBuilder(data.length * 2);
            for (int i = offset; i < offset + length; i++) {
                  byte byteChar = data[i];
                  stringBuilder.append(String.format("%02X", byteChar).toUpperCase());
            }
            return stringBuilder.toString();
      }

      public static boolean verifyIP(String ipStr) {
            if (ipStr == null || "".equals(ipStr)) {
                  return false;
            }
            String[] ips = ipStr.split("\\.");
            int ipsl = ips.length;
            if (ipsl != 4) {
                  return false;
            }
            String ipch = "";
            for (int i = 0; i < ipsl; i++) {
                  ipch = ips[i];
                  if (!checkNum(ipch) || ipch.length() > 3 || ipch.length() < 1) {
                        return false;
                  }
            }
            return true;
      }

      public static byte[] cutBytes(byte[] d, int start, int offest) {
            int len = offest - start;
            byte[] resB = new byte[len];
            System.arraycopy(d, start, resB, 0, len);
            return resB;
      }

      public static String showUninstallAPKSignatures(String apkPath) {
            String PATH_PackageParser = "android.content.pm.PackageParser";
            try {
                  // apk包的文件路径
                  // 这是一个Package 解释器, 是隐藏的
                  // 构造函数的参数只有一个, apk文件的路径
                  // PackageParser packageParser = new PackageParser(apkPath);
                  Class pkgParserCls = Class.forName(PATH_PackageParser);
                  Class[] typeArgs = new Class[1];
                  typeArgs[0] = String.class;
                  Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
                  Object[] valueArgs = new Object[1];
                  valueArgs[0] = apkPath;
                  Object pkgParser = pkgParserCt.newInstance(valueArgs);
                  TMKeyLog.e(TAG, "pkgParser:" + pkgParser.toString());
                  // 这个是与显示有关的, 里面涉及到一些像素显示等等, 我们使用默认的情况
                  DisplayMetrics metrics = new DisplayMetrics();
                  metrics.setToDefaults();
                  // PackageParser.Package mPkgInfo = packageParser.parsePackage(new
                  // File(apkPath), apkPath,
                  // metrics, 0);
                  typeArgs = new Class[4];
                  typeArgs[0] = File.class;
                  typeArgs[1] = String.class;
                  typeArgs[2] = DisplayMetrics.class;
                  typeArgs[3] = Integer.TYPE;
                  Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod("parsePackage",
                          typeArgs);
                  valueArgs = new Object[4];
                  valueArgs[0] = new File(apkPath);
                  valueArgs[1] = apkPath;
                  valueArgs[2] = metrics;
                  valueArgs[3] = PackageManager.GET_SIGNATURES;
                  Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser, valueArgs);

                  typeArgs = new Class[2];
                  typeArgs[0] = pkgParserPkg.getClass();
                  typeArgs[1] = Integer.TYPE;
                  Method pkgParser_collectCertificatesMtd = pkgParserCls.getDeclaredMethod("collectCertificates",
                          typeArgs);
                  valueArgs = new Object[2];
                  valueArgs[0] = pkgParserPkg;
                  valueArgs[1] = PackageManager.GET_SIGNATURES;
                  pkgParser_collectCertificatesMtd.invoke(pkgParser, valueArgs);
                  // 应用程序信息包, 这个公开的, 不过有些函数, 变量没公开
                  Field packageInfoFld = pkgParserPkg.getClass().getDeclaredField("mSignatures");
                  Signature[] info = (Signature[]) packageInfoFld.get(pkgParserPkg);
                  TMKeyLog.e(TAG, "size:" + info.length);
                  TMKeyLog.e(TAG, "Sign:" + info[0].toCharsString());
                  TMKeyLog.e(TAG, apkPath + "\napk_md5:" + getMD5(info[0]));
                  return info[0].toCharsString();
            } catch (Exception e) {
                  e.printStackTrace();
            }
            return null;
      }

      public static String getMD5(Signature decript) {
            try {
                  MessageDigest digest = java.security.MessageDigest
                          .getInstance("MD5");
                  digest.update(decript.toByteArray());
                  byte messageDigest[] = digest.digest();
                  // Create Hex String
                  StringBuffer hexString = new StringBuffer();
                  // 字节数组转换为 十六进制 数
                  for (int i = 0; i < messageDigest.length; i++) {
                        String shaHex = Integer.toHexString(messageDigest[i] & 0xFF).toUpperCase();
                        if (shaHex.length() < 2) {
                              hexString.append(0);
                        }
                        hexString.append(shaHex);
                  }
                  return hexString.toString();

            } catch (NoSuchAlgorithmException e) {
                  e.printStackTrace();
            }
            return "";
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
//            TMKeyLog.d(TAG, "bytesToHexStr>>>" + stringBuilder.toString().toUpperCase());
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
       * 判断指定包名App是否正在后台运行
       * @param context
       * @return
       */
      public static boolean isAppForeground(Context context, String packName){
            TMKeyLog.d(TAG, "isAppForeground>>>packName:" + packName);
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Service.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
            if (runningAppProcessInfoList==null){
                  return false;
            }
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfoList) {
                  TMKeyLog.d(TAG, "RunningAppProcessInfo>>>processName:" + processInfo.processName);
                  if (processInfo.processName.equals(packName)){
                        TMKeyLog.d(TAG, "isAppForeground>>>true");
                        return true;
                  }
            }
            TMKeyLog.e(TAG, "isAppForeground>>>false");
            return false;
      }

    /**
     * 解析LV格式数据如果len大于80则由两位表示
     * @param data 需要解析的数据
     * @param hasKey LV数据之前是否有一个字节的key版本号
     * @return
     */
      public static ArrayList<String> parseDataLV(String data, boolean hasKey) {
            TMKeyLog.i(TAG, "parseDataLV>>>s:" + data + ">>>hasKey:" + hasKey);
            ArrayList<String> list = null;
            if (data == null || "".equals(data)) {
                  return list;
            }
            list = new ArrayList<String>();
            int sl = data.length();
            TMKeyLog.i(TAG, "slen:" + sl);
            int curIndex = 0;//当前偏移
            String tLen = "";
            int tLenInt = 0;//数据长度
            String tValue = "";//数据值

            if (hasKey) {
                  String keyVersion = data.substring(curIndex, curIndex + 2);//秘钥版本
                  curIndex += 2;
                  TMKeyLog.i(TAG, "parseCardResLv>>>keyVersion:" + keyVersion);
            }

            while (curIndex < sl) {
                  tLen = data.substring(curIndex, curIndex + 2);
                  curIndex += 2;
                  tLenInt = FCharUtils.hexStr2Len(tLen);
                  if (tLenInt == 0) { //L为0，直接解析下一个数据域
                        continue;
                  }
                  if (tLenInt >= 0x80) {
                        tLen = data.substring(curIndex - 2, curIndex + 2);
                        curIndex += 2;
                        tLenInt = FCharUtils.hexStr2Len(tLen);
                  }
                  TMKeyLog.i(TAG, "curIndex:" + curIndex + ">>>tLenInt:" + tLenInt);
                  if ((curIndex + tLenInt * 2) > sl) {//长度有误
                        TMKeyLog.e(TAG, "数据域长度有误");
                        return null;
                  }
                  if (hasKey) {
                        //加密Data
                        String encData = data.substring(curIndex, curIndex + tLenInt * 2);
                        TMKeyLog.i(TAG, "encData:" + encData);
                        curIndex += tLenInt * 2;
                        TMKeyLog.i(TAG, "curIndex:" + curIndex);
                        list.add(encData);

                        //mac ,固定四字节
                        tLenInt = 4;
                        String mac = data.substring(curIndex, curIndex + tLenInt * 2);
                        curIndex += tLenInt * 2;
                        list.add(mac);
                        TMKeyLog.i(TAG, "mac:" + mac);
                  } else {
                        tValue = data.substring(curIndex, curIndex + tLenInt * 2);
                        TMKeyLog.i(TAG, "tValue:" + tValue);
                        curIndex += tLenInt * 2;
                        TMKeyLog.i(TAG, "curIndex:" + curIndex);
                        list.add(tValue);
                  }
            }
            return list;
      }

      /**
       * 将签名数据转换为Pkcs7格式数据
       * @param text
       * @param x509Certificate
       * @param sig
       * @return
       */
      public static String makep7bPack(byte[] text, X509Certificate x509Certificate, byte[] sig)
      {
            if (x509Certificate == null) {
                  return "";
            }

            PkcsInfoUtil signedData = new PkcsInfoUtil();

            signedData.setInteger_TLV(Integer.valueOf(1));

            PkcsInfoUtil digestAlgorithmId = new PkcsInfoUtil();
            digestAlgorithmId.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 1, 5 });

            digestAlgorithmId.setNull_TLV();

            digestAlgorithmId.packSEQUENCE_TLV();

            signedData.setSET_SET_OF_TLV(digestAlgorithmId.getPkcs7());

            PkcsInfoUtil content = new PkcsInfoUtil();
            content.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 7, 1 });

            PkcsInfoUtil contextPkcsInfoUtil = new PkcsInfoUtil();
            contextPkcsInfoUtil.setOctetString_TLV(text);

            content.setContext_TLV(contextPkcsInfoUtil.getPkcs7());

            signedData.setSEQUENCE_TLV(content.getPkcs7());

            try
            {
                  signedData.setContext_TLV(x509Certificate.getEncoded());
            }
            catch (CertificateEncodingException localCertificateEncodingException)
            {
            }

            PkcsInfoUtil signValuePkcsInfoUtil = new PkcsInfoUtil();
            signValuePkcsInfoUtil.setInteger_TLV(Integer.valueOf(1));

            PkcsInfoUtil cerInfoPkcsInfoUtil = new PkcsInfoUtil();
            cerInfoPkcsInfoUtil.packSEQUENCEByDN_Oid(x509Certificate.getIssuerDN().getName());

            cerInfoPkcsInfoUtil.setTlvByTv((byte) 2, x509Certificate.getSerialNumber().toByteArray());

            signValuePkcsInfoUtil.setSEQUENCE_TLV(cerInfoPkcsInfoUtil.getPkcs7());

            PkcsInfoUtil sigalg = new PkcsInfoUtil();
            sigalg.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 1, 5 });
            sigalg.setNull_TLV();

            signValuePkcsInfoUtil.setSEQUENCE_TLV(sigalg.getPkcs7());

            PkcsInfoUtil encodealgPkcsInfoUtil = new PkcsInfoUtil();
            encodealgPkcsInfoUtil.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 1, 1 });
            encodealgPkcsInfoUtil.setNull_TLV();

            signValuePkcsInfoUtil.setSEQUENCE_TLV(encodealgPkcsInfoUtil.getPkcs7());

            signValuePkcsInfoUtil.setOctetString_TLV(sig);

            signValuePkcsInfoUtil.packSEQUENCE_TLV();

            signedData.setSET_SET_OF_TLV(signValuePkcsInfoUtil.getPkcs7());

            signedData.packSEQUENCE_TLV();

            PkcsInfoUtil retPkcsInfoUtil = new PkcsInfoUtil();
            retPkcsInfoUtil.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 7, 2 });
            retPkcsInfoUtil.setContext_TLV(signedData.getPkcs7());

            retPkcsInfoUtil.packSEQUENCE_TLV();

            return PkcsInfoUtil.encode(retPkcsInfoUtil.getPkcs7());
      }

      /**
       * 将签名数据转换为Pkcs7格式数据
       * @param text
       * @param x509Certificate
       * @param sig
       * @return
       */
      public static String makeRSA2048p7bPack(byte[] text, X509Certificate x509Certificate, byte[] sig)
      {
            if (x509Certificate == null) {
                  return "";
            }

            PkcsInfoUtil signedData = new PkcsInfoUtil();

            signedData.setInteger_TLV(Integer.valueOf(1));

            PkcsInfoUtil digestAlgorithmId = new PkcsInfoUtil();

            digestAlgorithmId.setObjectIdentifier_TLV(new byte[] { 96, -122, 72, 1, 101, 3, 4, 2, 1 });

            digestAlgorithmId.setNull_TLV();

            digestAlgorithmId.packSEQUENCE_TLV();

            signedData.setSET_SET_OF_TLV(digestAlgorithmId.getPkcs7());

            PkcsInfoUtil content = new PkcsInfoUtil();
            content.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 7, 1 });

            PkcsInfoUtil contextMakePackage = new PkcsInfoUtil();
            contextMakePackage.setOctetString_TLV(text);

            content.setContext_TLV(contextMakePackage.getPkcs7());

            signedData.setSEQUENCE_TLV(content.getPkcs7());

            if (x509Certificate != null) {
                  try
                  {
                        signedData.setContext_TLV(x509Certificate.getEncoded());
                  }
                  catch (CertificateEncodingException localCertificateEncodingException)
                  {
                  }

            }

            PkcsInfoUtil signValueMakePackage = new PkcsInfoUtil();
            signValueMakePackage.setInteger_TLV(Integer.valueOf(1));

            PkcsInfoUtil cerInfoMakePackage = new PkcsInfoUtil();
            cerInfoMakePackage.packSEQUENCEByDN_Oid(x509Certificate.getIssuerDN().getName());

            cerInfoMakePackage.setTlvByTv((byte)2, x509Certificate.getSerialNumber().toByteArray());

            signValueMakePackage.setSEQUENCE_TLV(cerInfoMakePackage.getPkcs7());

            PkcsInfoUtil sigalg = new PkcsInfoUtil();

            sigalg.setObjectIdentifier_TLV(new byte[] { 96, -122, 72, 1, 101, 3, 4, 2, 1 });

            sigalg.setNull_TLV();

            signValueMakePackage.setSEQUENCE_TLV(sigalg.getPkcs7());

            PkcsInfoUtil encodealgMakePackage = new PkcsInfoUtil();
            encodealgMakePackage.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 1, 1 });
            encodealgMakePackage.setNull_TLV();

            signValueMakePackage.setSEQUENCE_TLV(encodealgMakePackage.getPkcs7());

            signValueMakePackage.setOctetString_TLV(sig);

            signValueMakePackage.packSEQUENCE_TLV();

            signedData.setSET_SET_OF_TLV(signValueMakePackage.getPkcs7());

            signedData.packSEQUENCE_TLV();

            PkcsInfoUtil retMakePackage = new PkcsInfoUtil();
            retMakePackage.setObjectIdentifier_TLV(new byte[] { 42, -122, 72, -122, -9, 13, 1, 7, 2 });
            retMakePackage.setContext_TLV(signedData.getPkcs7());

            retMakePackage.packSEQUENCE_TLV();

            return PkcsInfoUtil.encode(retMakePackage.getPkcs7());
      }

      /**
       * 将签名数据转换为Pkcs7格式数据
       * @param text
       * @param x509CertificateStructure
       * @param sig
       * @return
       */
      public static String makep7bPackSM2(byte[] text, X509CertificateStructure x509CertificateStructure, byte[] sig)
      {
            if (x509CertificateStructure == null) {
                  return "";
            }

            PkcsInfoUtil signedData = new PkcsInfoUtil();

            signedData.setInteger_TLV(Integer.valueOf(1));

            PkcsInfoUtil digestAlgorithmId = new PkcsInfoUtil();
            digestAlgorithmId.setObjectIdentifier_TLV(new byte[] { 42, -127, 28, -49, 85, 1, -125, 17 });

            digestAlgorithmId.setNull_TLV();

            digestAlgorithmId.packSEQUENCE_TLV();

            signedData.setSET_SET_OF_TLV(digestAlgorithmId.getPkcs7());

            PkcsInfoUtil content = new PkcsInfoUtil();
            content.setObjectIdentifier_TLV(new byte[] { 42, -127, 28, -49, 85, 6, 1, 4, 2, 1});

            PkcsInfoUtil contextPkcsInfoUtil = new PkcsInfoUtil();
            contextPkcsInfoUtil.setOctetString_TLV(text);

            content.setContext_TLV(contextPkcsInfoUtil.getPkcs7());

            signedData.setSEQUENCE_TLV(content.getPkcs7());

            try
            {
                  signedData.setContext_TLV(x509CertificateStructure.getEncoded());
            }
            catch (IOException e)
            {
            }

            PkcsInfoUtil signValuePkcsInfoUtil = new PkcsInfoUtil();
            signValuePkcsInfoUtil.setInteger_TLV(Integer.valueOf(1));

            PkcsInfoUtil cerInfoPkcsInfoUtil = new PkcsInfoUtil();

            String issuer = x509CertificateStructure.getIssuer().toString();//颁发者
            System.out.println("issuer=" + issuer);
//            if (!"".equals(issuer) && issuer != null) {
//                String issuers[] = issuer.split(",");
//                String issuerId = issuers[2].substring(issuers[2].indexOf("=") + 1);//证书发行单位编号
//                String issuerName = issuers[1].substring(issuers[1].indexOf("=") + 1);//证书发行单位名称
//                System.out.println("证书发行单位编号 ID=" + issuerId);
//                System.out.println("证书发行单位名称 NAME=" + issuerName);
//            }
            cerInfoPkcsInfoUtil.packSM2SEQUENCEByDN_Oid(x509CertificateStructure.getIssuer().toString());

            String serialNumberStr = x509CertificateStructure.getSerialNumber().toString();
            String serialNumber = x509CertificateStructure.getSerialNumber().getPositiveValue().toString();
            System.out.println("serialNumberStr:" + serialNumberStr + ">>>serialNumber:" + serialNumber);
            cerInfoPkcsInfoUtil.setTlvByTv((byte) 2, x509CertificateStructure.getSerialNumber().getPositiveValue().toByteArray());

            signValuePkcsInfoUtil.setSEQUENCE_TLV(cerInfoPkcsInfoUtil.getPkcs7());

            PkcsInfoUtil sigalg = new PkcsInfoUtil();
            sigalg.setObjectIdentifier_TLV(new byte[] { 42, -127, 28, -49, 85, 1, -125, 17 });
            sigalg.setNull_TLV();

            signValuePkcsInfoUtil.setSEQUENCE_TLV(sigalg.getPkcs7());

            PkcsInfoUtil encodealgPkcsInfoUtil = new PkcsInfoUtil();
            encodealgPkcsInfoUtil.setObjectIdentifier_TLV(new byte[] { 42, -127, 28, -49, 85, 1, -126, 45 });
            encodealgPkcsInfoUtil.setNull_TLV();

            signValuePkcsInfoUtil.setSEQUENCE_TLV(encodealgPkcsInfoUtil.getPkcs7());

            signValuePkcsInfoUtil.setOctetString_TLV(sig);

            signValuePkcsInfoUtil.packSEQUENCE_TLV();

            signedData.setSET_SET_OF_TLV(signValuePkcsInfoUtil.getPkcs7());

            signedData.packSEQUENCE_TLV();

            PkcsInfoUtil retPkcsInfoUtil = new PkcsInfoUtil();
            retPkcsInfoUtil.setObjectIdentifier_TLV(new byte[] { 42, -127, 28, -49, 85, 6, 1, 4, 2, 2});
            retPkcsInfoUtil.setContext_TLV(signedData.getPkcs7());

            retPkcsInfoUtil.packSEQUENCE_TLV();

            return PkcsInfoUtil.encode(retPkcsInfoUtil.getPkcs7());
      }

      public static byte[] longToBytes(long num)
      {
            byte[] bytes = new byte[8];
            for (int i = 0; i < 8; ++i)
            {
                  bytes[i] = (byte)(int)(0xFF & num >> i * 8);
            }

            return bytes;
      }

      public static byte[] intToBytes(int num)
      {
            byte[] bytes = new byte[4];
            bytes[0] = (byte)(0xFF & num >> 0);
            bytes[1] = (byte)(0xFF & num >> 8);
            bytes[2] = (byte)(0xFF & num >> 16);
            bytes[3] = (byte)(0xFF & num >> 24);
            return bytes;
      }

      public static String intToByte(int num)
      {
            byte[] bytes = new byte[1];
            bytes[0] = (byte)(0xFF & num);
            return showResult16Str(bytes);
      }

      public static int byteToInt(byte[] bytes)
      {
            int num = 0;

            int temp = (0xFF & bytes[0]) << 0;
            num |= temp;
            temp = (0xFF & bytes[1]) << 8;
            num |= temp;
            temp = (0xFF & bytes[2]) << 16;
            num |= temp;
            temp = (0xFF & bytes[3]) << 24;
            num |= temp;
            return num;
      }

      public static byte[] byteConvert32Bytes(BigInteger n)
      {
            byte[] tmpd = (byte[])null;
            if (n == null)
            {
                  return null;
            }

            if (n.toByteArray().length == 33)
            {
                  tmpd = new byte[32];
                  System.arraycopy(n.toByteArray(), 1, tmpd, 0, 32);
            }
            else if (n.toByteArray().length == 32)
            {
                  tmpd = n.toByteArray();
            }
            else
            {
                  tmpd = new byte[32];
                  for (int i = 0; i < 32 - n.toByteArray().length; ++i)
                  {
                        tmpd[i] = 0;
                  }
                  System.arraycopy(n.toByteArray(), 0, tmpd, 32 - n.toByteArray().length, n.toByteArray().length);
            }
            return tmpd;
      }

      public static boolean between(BigInteger param, BigInteger min, BigInteger max) {
            return param.compareTo(min) >= 0 && param.compareTo(max) < 0;
      }
}
