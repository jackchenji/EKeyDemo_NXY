package com.cn.froad.ekeydemo_nxy.utils;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串处理
 */

public class StringUtil {
    private final static String TAG = "StringUtil";

    /**
     * 描述: 检测字符串是否为空
     *
     * @param str
     * @return 为空返回false，否则返回true
     */
    public static boolean isNotEmpty(String str) {
        if (null == str) {
            return false;
        }
        return !"".equals(str.replaceAll(" ", "")) && !"null".equals(str.replaceAll(" ", ""));
    }

    /**
     * Check whether the specified string is empty after trim.
     * @param str
     * @return
     */
    public static boolean isTrimEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * To not null string
     *
     * @param str
     * @return
     */
    public static String toNotNullString(String str) {
        return isNotEmpty(str) ? str : "";
    }

    /**
     * 描述: 字符串 ===> Double
     */
    public static Double getDoubleData(Object str) {
        if (str == null) {
            return null;
        }
        if (isNotEmpty(str.toString())) {
            return Double.valueOf(str.toString());
        }
        return null;
    }

    /**
     * 描述: 字符串 ===> Inteter
     */
    public static Integer getIntegerData(Object str) {
        if (str == null) {
            return null;
        }
        if (isNotEmpty(str.toString())) {
            return Integer.valueOf(str.toString());
        }
        return null;
    }

    /**
     * 描述: 字符串 ===> Float
     */
    public static Float getFloatData(Object str) {
        if (str == null) {
            return null;
        }
        if (isNotEmpty(str.toString())) {
            return Float.valueOf(str.toString());
        }
        return null;
    }

    /**
     * 数字校验，判断字符串是否为数字
     *
     * @param str  源字符串
     * @param flag -1:校验是否为负数，1：校验是否为正数，0：表示校验所有数字
     * @return
     */
    public static boolean isNumeric(String str, int flag) {
        boolean temp = false;

        if (!isNotEmpty(str)) {
            return temp;
        }

        Pattern pattern = null;

        if (flag == 1) {
            pattern = Pattern.compile("[0-9]*");
        } else if (flag == -1) {
            pattern = Pattern.compile("^-?[0-9]+");
        } else if (flag == 0) {
            pattern = Pattern.compile("-?[0-9]+.?[0-9]+");
        }

        Matcher isNum = pattern.matcher(str);

        if (!isNum.matches()) {
            temp = false;
        } else {
            temp = true;
        }

        return temp;
    }

    /**
     * 检测是否为浮点数
     *
     * @param num  待判断的字符串
     * @param type 校验的类型："0+"-非负浮点数   ;"+"-//正浮点数   ;"-0"-非正浮点数   ;"-"-//负浮点数
     * @return
     */
    public static boolean isFloatType(String num, String type) {
        if (!isNotEmpty(num)) {    //为空直接返回false
            return false;
        }

        String eL = "";
        if (type.equals("0+")) eL = "^\\d+(\\.\\d+)?$";//非负浮点数
        else if (type.equals("+"))
            eL = "^((\\d+\\.\\d*[1-9]\\d*)|(\\d*[1-9]\\d*\\.\\d+)|(\\d*[1-9]\\d*))$";//正浮点数
        else if (type.equals("-0")) eL = "^((-\\d+(\\.\\d+)?)|(0+(\\.0+)?))$";//非正浮点数
        else if (type.equals("-"))
            eL = "^(-((\\d+\\.\\d*[1-9]\\d*)|(\\d*[1-9]\\d*\\.\\d+)|(\\d*[1-9]\\d*)))$";//负浮点数
        else eL = "^(-?\\d+)(\\.\\d+)?$";//浮点数
        Pattern p = Pattern.compile(eL);
        Matcher m = p.matcher(num);

        boolean b = m.matches();

        return b;
    }

    /**
     * 使用嵌套HTML处理特殊需求的字符串拼接,如控制字符串中部分字符的颜色与字体
     *
     * @param str      需要特殊处理的，如表红
     * @param jointStr 字体变小的源数据
     * @return
     */
    public static String jointHandler(String str, String jointStr) {
        if (!isNotEmpty(str)) {
            str = "--";
        }

        String tempString = "<big><font color='#eb4f4f'>" + str + "</big></font>" + "<small>" + jointStr + "</small>";

        return tempString;
    }

    /**
     * 将byte数组转为HEX格式字符串显示
     *
     * @param b
     * @return
     */
    public static String byte2HexStr(byte[] b) {
        if (b == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int bl = b.length;
        byte bt;
        String bts = "";
        int btsl = 0;
        for (int i = 0; i < bl; i++) {
            bt = b[i];
            bts = Integer.toHexString(bt);
            btsl = bts.length();
            if (btsl > 2) {
                bts = bts.substring(btsl - 2);
            } else if (btsl == 1) {
                bts = "0" + bts;
            }
            sb.append(bts);
        }
        return sb.toString().toUpperCase();
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
            return byte2HexStr(bs);
        } catch (UnsupportedEncodingException ue) {
            ue.printStackTrace();
        }
        return null;
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



    public static boolean isEmpty(String s) {
        return !isNotEmpty(s);
    }

    /**
     * Check whether the string is numeric.
     *
     * @param string
     * @return
     */
    public static boolean isNumeric(String string) {
        return string.matches("^[-+]?\\d+(\\.\\d+)?$");
    }

    public static String formatString(int fristIndex, String string) {
        String value = "";
        if (string != null && !"".equals(string)) {
            String fristStr = "";
            String otherStr = "";
            string = string.replaceAll(" ", "");
            if (string.length() >= fristIndex) {
                StringBuffer sbf = new StringBuffer();
                fristStr = string.substring(0, fristIndex);
                otherStr = string.substring(fristIndex);
                if (!"".equals(otherStr)) {
                    String regex = "(.{4})";
                    otherStr = otherStr.replaceAll(regex, "$1 ");
                }
                sbf.append(fristStr);
                sbf.append(otherStr);
                sbf.insert(fristIndex, " ");
                value = sbf.toString();
            } else {
                value = string;
            }
        }
        return value.trim();
    }

    /**
     * 验证输入的字符串是否合理
     *
     * @param s
     * @return
     */
    public static boolean verifyCertNum(String s, int t) {
        //SQLog.d(TAG, "verifyNum>>>s:" + s + ">>>t:" + t);
        if (s.equals("")) {
            return true;
        }
        String reg = "";
        switch (t) {
            case 0:
                reg = "[^0-9a-zA-Z*\\s]";
                break;
            case 1:
                reg = "[^0-9a-zA-Z\\s]]";
                break;
            case 2:
                reg = "[^0-9\\s]";
                break;
            case 3:
                reg = "[^0-9a-zA-Z*]";
                break;
            case 4:
                reg = "[^0-9\\s]";
                break;
            case 5:
                reg = "[^0-9xX\\s]";
                break;
            case 6:
                reg = "[^0-9*\\s]";
                break;
            case 7:
                reg = "[^0-9a-zA-Z\\s]]";
                break;
        }
        Pattern pattern = Pattern.compile(reg);
        Matcher math = pattern.matcher(s);
        if (math.find()) {
            //SQLog.d(TAG, "find true replaceAll");
            return false;
        }
        return true;
    }

    public static boolean isMobile(String mobile) {

        String telRegex = "^1\\d{10}$";// 1开头后面任意数
        if (TextUtils.isEmpty(mobile)) {
            return false;
        } else {
            return mobile.matches(telRegex);
        }
    }

    public static boolean isMobileSafe(String mobile) {

        String telRegex = "^1[0-9]{2}[*]{4}\\d{4}$";// 1开头的手机号，中间屏蔽4位
        if (TextUtils.isEmpty(mobile)) {
            return false;
        } else {
            return mobile.matches(telRegex);
        }
    }

    public static boolean isCerID(String str) {
        String telRegex = "^[0-9]{16}[0-9]{1,}[0-9xX]";
        if (TextUtils.isEmpty(str)) {
            return false;
        } else {
            return str.matches(telRegex);
        }
    }

    public static String getNotNullStr (String string) {
        if (string == null) {
            return "";
        }
        return string;
    }
}
