package com.froad.ukey.utils;

import com.froad.ukey.utils.np.FCharUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by FW on 2017/6/7.
 */

public class PkcsInfoUtil {

    private String hexString = "";
    private byte[] pkcs7;
    private static Map<String, String> hasHMap;
    private static final char[] base64EncodeChars = { 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c',
            'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
            'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    public byte[] getPkcs7()
    {
        this.pkcs7 = FCharUtils.hexString2ByteArray(this.hexString);
        return this.pkcs7;
    }

    public void setPkcs7(byte[] pkcs7) {
        this.pkcs7 = pkcs7;
    }

    public String getHexString() {
        return this.hexString;
    }

    public void setHexString(String hexString) {
        this.hexString = hexString;
    }

    public Map<String, String> getHasHMap()
    {
        return hasHMap;
    }

    public void setHasHMap(Map<String, String> hasHMap) {
        hasHMap = hasHMap;
    }

    public void setTlvByTv(byte Tag, byte[] v)
    {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length() / 2;
        String mylen = getLenthBy16(lenth);
        String tagHex = FCharUtils.bytesToHexStr(new byte[] { Tag });
        String all = tagHex + mylen + vHex;
        this.hexString += all;
    }

    public void setBoolean_TLV(Boolean v)
    {
    }

    public void setInteger_TLV(Integer v)
    {
        String myv = getLenthBy16(v.intValue());
        int lenth = myv.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "02" + lenth16 + myv);
    }

    public void setBitString_TLV(byte[] v)
    {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "03" + lenth16 + vHex);
    }

    public void setOctetString_TLV(byte[] v) {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "04" + lenth16 + vHex);
    }

    public void setObjectIdentifier_TLV(byte[] v) {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "06" + lenth16 + vHex);
    }

    public void setObjectDescriptor_TLV(byte[] v)
    {
    }

    public void setExternalInstancceOf_TLV(byte[] v)
    {
    }

    public void setReal_TLV(byte[] v)
    {
    }

    public void setENUMERATED_TLV(byte[] v)
    {
    }

    public void setUTF8String_TLV(byte[] v)
    {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "0C" + lenth16 + vHex);
    }

    public void setRELATIVE_OID_TLV(byte[] v)
    {
    }

    public void setSEQUENCE_TLV(byte[] v)
    {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length() / 2;
        String lenth16 = getLenthBy16(lenth);
        this.hexString = (this.hexString + "30" + lenth16 + vHex);
    }

    public void packSEQUENCE_TLV()
    {
        byte[] v = getPkcs7();
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length() / 2;
        String lenth16 = getLenthBy16(lenth);
        this.hexString = ("30" + lenth16 + vHex);
    }

    public void setSET_SET_OF_TLV(byte[] v) {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "31" + lenth16 + vHex);
    }

    public void addHeadSET_TLV(byte[] v)
    {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = ("31" + lenth16 + vHex);
    }

    public void setNumeric_String_TLV(byte[] v)
    {
    }

    public void setPrintable_String_TLV(byte[] v) {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "13" + lenth16 + vHex);
    }

    public void setIA5_String_TLV(String v)
    {
        this.hexString += v;
    }

    public void setTeletexString_T61String_TLV(byte[] v)
    {
    }

    public void setVideotexString_TLV(byte[] v)
    {
    }

    public void setUTCTime_TLV(byte[] v)
    {
    }

    public void setGeneralizedTime_TLV(byte[] v)
    {
    }

    public void setGraphicString_TLV(byte[] v)
    {
    }

    public void setVisibleString_ISO646String_TLV(byte[] v)
    {
    }

    public void setGeneralString_TLV(byte[] v)
    {
    }

    public void setUniversalString_TLV(byte[] v)
    {
    }

    public void setCHARACTER_STRING_TLV(byte[] v)
    {
    }

    public void setBMPString_TLV(byte[] v)
    {
    }

    public void setContext_TLV(byte[] v)
    {
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = (this.hexString + "A0" + lenth16 + vHex);
    }

    public void packContext_TLV()
    {
        byte[] v = getPkcs7();
        String vHex = FCharUtils.bytesToHexStr(v);
        int lenth = vHex.length();
        String lenth16 = getLenthBy16(lenth / 2);
        this.hexString = ("A0" + lenth16 + vHex);
    }

    public void setNull_TLV()
    {
        this.hexString += "0500";
    }

    public String getLenthBy16(int value)
    {
        String value16 = Integer.toHexString(value);
        int len = value16.length();
        if (len % 2 != 0)
        {
            value16 = "0" + value16;
        }

        int lenNew = value16.length() / 2;
        if (value > 128) {
            value16 = "8" + lenNew + value16;
        }
        if (value == 128) {
            value16 = "81" + value16;
        }
        return value16;
    }

    public void makeMapTable()
    {
        hasHMap = new HashMap<>();
        hasHMap.put("CN", "550403");
        hasHMap.put("SN", "550404");
        hasHMap.put("C", "550406");
        hasHMap.put("L", "550407");
        hasHMap.put("ST", "550408");
        hasHMap.put("street", "550409");
        hasHMap.put("O", "55040A");
        hasHMap.put("OU", "55040B");
        hasHMap.put("title", "55040C");
        hasHMap.put("member", "55041F");
        hasHMap.put("owner", "550420");
        hasHMap.put("seeAlso", "550422");
        hasHMap.put("name", "550429");
        hasHMap.put("GN", "55042A");
        hasHMap.put("initials", "55042B");
        hasHMap.put("dnQualifier", "55042E");
        hasHMap.put("dmdName", "550436");
        hasHMap.put("role", "550448");
    }

    public void packSM2SEQUENCEByDN_Oid(String oid)
    {
        makeMapTable();
        String[] oidArry = oid.split(",");
        String res = "";
        int i = 1;
        for (int j = 2; j >= 0; --j) {
            String[] tempArry = oidArry[j].split("=");
            if ((tempArry != null) && (tempArry.length >= 2)) {
                tempArry[0] = tempArry[0].replaceAll(" ", "");
                tempArry[0] = tempArry[0].replaceAll(" ", "");
                String value = (String)hasHMap.get(tempArry[0]);
                if ((value == null) || ("".equals(value))) {
                    setObjectIdentifier_TLV(FCharUtils.hexString2ByteArray("2a864886f70d010901"));
                    tempArry[1] = tempArry[1].replace("#", "");
                    setIA5_String_TLV(tempArry[1]);
                } else {
                    setObjectIdentifier_TLV(FCharUtils.hexString2ByteArray(value));
                    try {
                        if (i == 3)
                            setPrintable_String_TLV(tempArry[1].getBytes("gb2312"));
                        else {
                            setUTF8String_TLV(tempArry[1].getBytes("gb2312"));
                        }
                    }
                    catch (UnsupportedEncodingException localUnsupportedEncodingException)
                    {
                    }
                }
                packSEQUENCE_TLV();
            }
            addHeadSET_TLV(getPkcs7());
            res = getHexString() + res;
            clear();
            ++i;
        }
        setHexString(res);
        packSEQUENCE_TLV();
    }

    public void packSEQUENCEByDN_Oid(String oid)
    {
        makeMapTable();
        String[] oidArry = oid.split(",");

        String res = "";
        int i = 1;
        for (String temp : oidArry) {
            String[] tempArry = temp.split("=");
            if ((tempArry != null) && (tempArry.length >= 2)) {
                tempArry[0] = tempArry[0].replaceAll(" ", "");
                String value = (String)hasHMap.get(tempArry[0]);
                if ((value == null) || ("".equals(value))) {
                    setObjectIdentifier_TLV(FCharUtils.hexString2ByteArray("2a864886f70d010901"));
                    tempArry[1] = tempArry[1].replace("#", "");
                    setIA5_String_TLV(tempArry[1]);
                } else {
                    setObjectIdentifier_TLV(FCharUtils.hexString2ByteArray(value));
                    try {
                        if (i == 3)
                            setPrintable_String_TLV(tempArry[1].getBytes("gb2312"));
                        else {
                            setUTF8String_TLV(tempArry[1].getBytes("gb2312"));
                        }
                    }
                    catch (UnsupportedEncodingException localUnsupportedEncodingException)
                    {
                    }
                }
                packSEQUENCE_TLV();
            }
            addHeadSET_TLV(getPkcs7());
            res = getHexString() + res;
            clear();
            ++i;
        }
        setHexString(res);
        packSEQUENCE_TLV();
    }

    public void clear()
    {
        this.hexString = "";
    }

    public static String encode(byte[] data)
    {
        StringBuffer sb = new StringBuffer();
        int len = data.length;
        int i = 0;

        while (i < len) {
            int b1 = data[(i++)] & 0xFF;
            if (i == len) {
                sb.append(base64EncodeChars[(b1 >>> 2)]);
                sb.append(base64EncodeChars[((b1 & 0x3) << 4)]);
                sb.append("==");
                break;
            }
            int b2 = data[(i++)] & 0xFF;
            if (i == len) {
                sb.append(base64EncodeChars[(b1 >>> 2)]);
                sb.append(base64EncodeChars[
                        ((b1 & 0x3) << 4 |
                                (b2 & 0xF0) >>> 4)]);
                sb.append(base64EncodeChars[((b2 & 0xF) << 2)]);
                sb.append("=");
                break;
            }
            int b3 = data[(i++)] & 0xFF;
            sb.append(base64EncodeChars[(b1 >>> 2)]);
            sb.append(base64EncodeChars[
                    ((b1 & 0x3) << 4 |
                            (b2 & 0xF0) >>> 4)]);
            sb.append(base64EncodeChars[
                    ((b2 & 0xF) << 2 |
                            (b3 & 0xC0) >>> 6)]);
            sb.append(base64EncodeChars[(b3 & 0x3F)]);
        }
        return sb.toString();
    }
}
