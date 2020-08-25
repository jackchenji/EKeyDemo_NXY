package com.froad.ukey.utils;

import android.content.Context;

import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.SM2Util;
import com.froad.ukey.utils.np.SM3;
import com.froad.ukey.utils.np.TMKeyLog;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * Created by FW on 2017/7/5.
 */

public class SM2 {

    private final static String TAG = "SM2";
    public static String[] ecc_param = {
            "FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF",
            "FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC",
            "28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93",
            "FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123",
            "32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7",
            "BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0" };
    public final BigInteger ecc_p;
    public final BigInteger ecc_a;
    public final BigInteger ecc_b;
    public final BigInteger ecc_n;
    public final BigInteger ecc_gx;
    public final BigInteger ecc_gy;
    public final ECCurve ecc_curve;
    public final ECPoint ecc_point_g;
    public final ECDomainParameters ecc_bc_spec;
    public final ECKeyPairGenerator ecc_key_pair_generator;
    public final ECFieldElement ecc_gx_fieldelement;
    public final ECFieldElement ecc_gy_fieldelement;

    public static SecureRandom random = new SecureRandom();

    private static SM2 sm2;

    public static SM2 getInstance()
    {
        if (sm2 == null) {
            sm2 = new SM2();
        }
        return sm2;
    }

    private SM2()
    {
        this.ecc_p = new BigInteger(ecc_param[0], 16);
        this.ecc_a = new BigInteger(ecc_param[1], 16);
        this.ecc_b = new BigInteger(ecc_param[2], 16);
        this.ecc_n = new BigInteger(ecc_param[3], 16);
        this.ecc_gx = new BigInteger(ecc_param[4], 16);
        this.ecc_gy = new BigInteger(ecc_param[5], 16);

        this.ecc_gx_fieldelement = new ECFieldElement.Fp(this.ecc_p, this.ecc_gx);
        this.ecc_gy_fieldelement = new ECFieldElement.Fp(this.ecc_p, this.ecc_gy);

        this.ecc_curve = new ECCurve.Fp(this.ecc_p, this.ecc_a, this.ecc_b);
        this.ecc_point_g = new ECPoint.Fp(this.ecc_curve, this.ecc_gx_fieldelement, this.ecc_gy_fieldelement);

        this.ecc_bc_spec = new ECDomainParameters(this.ecc_curve, this.ecc_point_g, this.ecc_n);

        ECKeyGenerationParameters ecc_ecgenparam = new ECKeyGenerationParameters(this.ecc_bc_spec, new SecureRandom());

        this.ecc_key_pair_generator = new ECKeyPairGenerator();
        this.ecc_key_pair_generator.init(ecc_ecgenparam);
    }

    public byte[] sm2GetZ(byte[] userId, ECPoint userKey)
    {
        SM3Digest sm3 = new SM3Digest();

        int len = userId.length * 8;
        sm3.update((byte)(len >> 8 & 0xFF));
        sm3.update((byte)(len & 0xFF));
        sm3.update(userId, 0, userId.length);

        byte[] p = FCharUtils.byteConvert32Bytes(this.ecc_a);
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(this.ecc_b);
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(this.ecc_gx);
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(this.ecc_gy);
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(userKey.getX().toBigInteger());
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(userKey.getY().toBigInteger());
        sm3.update(p, 0, p.length);

        byte[] md = new byte[sm3.getDigestSize()];
        sm3.doFinal(md, 0);
        return md;
    }

    public byte[] sm2GetZSM(byte[] userId, String smX, String smY) {
        SM3Digest sm3 = new SM3Digest();

        int len = userId.length * 8;
        sm3.update((byte)(len >> 8 & 0xFF));
        sm3.update((byte)(len & 0xFF));
        sm3.update(userId, 0, userId.length);

        byte[] p = FCharUtils.byteConvert32Bytes(this.ecc_a);
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(this.ecc_b);
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(this.ecc_gx);
        sm3.update(p, 0, p.length);

        p = FCharUtils.byteConvert32Bytes(this.ecc_gy);
        sm3.update(p, 0, p.length);

        p = FCharUtils.hexString2ByteArray(smX);
        sm3.update(p, 0, p.length);

        p = FCharUtils.hexString2ByteArray(smY);
        sm3.update(p, 0, p.length);

        byte[] md = new byte[sm3.getDigestSize()];
        sm3.doFinal(md, 0);
        return md;
    }

    public void sm2Sign(byte[] md, BigInteger userD, ECPoint userKey, SM2Result sm2Result) {
        BigInteger e = new BigInteger(1, md);
        BigInteger k = null;
        ECPoint kp = null;
        BigInteger r = null;
        BigInteger s = null;
        do
        {
            do
            {
                k = userD;
                kp = userKey;

                System.out.println("计算曲线点X1: " + kp.getX().toBigInteger().toString(16));
                System.out.println("计算曲线点Y1: " + kp.getY().toBigInteger().toString(16));
                System.out.println("");

                r = e.add(kp.getX().toBigInteger());
                r = r.mod(this.ecc_n);
            }while ((r.equals(BigInteger.ZERO)) || (r.add(k).equals(this.ecc_n)));

            BigInteger da_1 = userD.add(BigInteger.ONE);
            da_1 = da_1.modInverse(this.ecc_n);

            s = r.multiply(userD);
            s = k.subtract(s).mod(this.ecc_n);
            s = da_1.multiply(s).mod(this.ecc_n);
        }while (s.equals(BigInteger.ZERO));

        sm2Result.r = r;
        sm2Result.s = s;
    }

    public byte[] sign(byte[] userId, byte[] privateKey, byte[] sourceData, boolean isNeedAddPubKey, byte[] pubKey, boolean isCType) {
        if (privateKey != null && privateKey.length != 0) {
            if (sourceData != null && sourceData.length != 0) {
                SM2 sm2 = SM2.getInstance();
                if (isNeedAddPubKey) { //需要在签名数据前添加公钥ZA
                    sourceData = dealSignData(pubKey, sourceData);
                }
                System.out.println("sourceData:" + FCharUtils.bytesToHexStr(sourceData));
                BigInteger userD = new BigInteger(privateKey);
                ECPoint userKey = sm2.generateKeyPair(userD);
                SM3Digest sm3 = new SM3Digest();
                byte[] z = sm2.sm2GetZ(userId, userKey);
                sm3.update(z, 0, z.length);
                sm3.update(sourceData, 0, sourceData.length);
                byte[] md = new byte[32];
                sm3.doFinal(md, 0);
                SM2Result sm2Result = new SM2Result();
                sm2.sm2Sign(md, userD, userKey, sm2Result);
                DERInteger d_r = new DERInteger(sm2Result.r);
                DERInteger d_s = new DERInteger(sm2Result.s);
                ASN1EncodableVector v2 = new ASN1EncodableVector();
                v2.add(d_r);
                v2.add(d_s);
                DERObject sign = new DERSequence(v2);
                byte[] signdata = sign.getDEREncoded();
                System.out.println("signdata:" + FCharUtils.bytesToHexStr(signdata));
                if (isCType) { //是否需要处理为C格式
                    signdata = dealSm2SignResultC(signdata);
                    System.out.println("dealSm2SignResultC>>>signdata:" + FCharUtils.bytesToHexStr(signdata));
                }
                return signdata;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public ECPoint generateKeyPair(BigInteger d) {
        ECPoint ep = this.ecc_point_g.multiply(d);
        boolean checkRes = this.checkPublicKey(ep);
        return checkRes ? ep : null;
    }

    private boolean checkPublicKey(ECPoint publicKey) {
        if (!publicKey.isInfinity()) {
            BigInteger x = publicKey.getX().toBigInteger();
            BigInteger y = publicKey.getY().toBigInteger();
            if (FCharUtils.between(x, new BigInteger("0"), ecc_p) && FCharUtils.between(y, new BigInteger("0"), ecc_p)) {
                BigInteger xResult = x.pow(3).add(ecc_a.multiply(x)).add(ecc_b).mod(ecc_p);
                BigInteger yResult = y.pow(2).mod(ecc_p);
                if (yResult.equals(xResult) && publicKey.multiply(ecc_n).isInfinity()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public void sm2Verify(byte[] md, ECPoint userKey, BigInteger r, BigInteger s, SM2Result sm2Result)
    {
        sm2Result.R = null;
        BigInteger e = new BigInteger(1, md);
        BigInteger t = r.add(s).mod(this.ecc_n);
        if (t.equals(BigInteger.ZERO))
        {
            return;
        }

        ECPoint x1y1 = this.ecc_point_g.multiply(sm2Result.s);
        System.out.println("计算曲线点X0: " + x1y1.getX().toBigInteger().toString(16));
        System.out.println("计算曲线点Y0: " + x1y1.getY().toBigInteger().toString(16));
        System.out.println("");

        x1y1 = x1y1.add(userKey.multiply(t));
        System.out.println("计算曲线点X1: " + x1y1.getX().toBigInteger().toString(16));
        System.out.println("计算曲线点Y1: " + x1y1.getY().toBigInteger().toString(16));
        System.out.println("");
        sm2Result.R = e.add(x1y1.getX().toBigInteger()).mod(this.ecc_n);
        System.out.println("R: " + sm2Result.R.toString(16));
    }

    /**
     * 加密
     * @param publicKey
     * @param data
     * @param resType 0--不需要处理，1--处理为c123,2--处理为c132
     * @return
     * @throws IOException
     */
    public byte[] sm2Encrypt (byte[] publicKey, byte[] data, int resType) {
        if (publicKey != null && publicKey.length != 0) {
            if (data != null && data.length != 0) {
                byte[] source = new byte[data.length];
                System.arraycopy(data, 0, source, 0, data.length);
                byte[] formatedPubKey;
                if (publicKey.length == 64) {
                    formatedPubKey = new byte[65];
                    formatedPubKey[0] = 4;
                    System.arraycopy(publicKey, 0, formatedPubKey, 1, publicKey.length);
                } else {
                    formatedPubKey = publicKey;
                }

                Cipher cipher = new Cipher();
                ECPoint userKey = ecc_curve.decodePoint(formatedPubKey);
                ECPoint c1 = cipher.Init_enc(this, userKey);
                cipher.Encrypt(source);
                byte[] c3 = new byte[32];
                cipher.Dofinal(c3);
                DERInteger x = new DERInteger(c1.getX().toBigInteger());
                DERInteger y = new DERInteger(c1.getY().toBigInteger());
                DEROctetString derDig = new DEROctetString(c3);
                DEROctetString derEnc = new DEROctetString(source);
                ASN1EncodableVector v = new ASN1EncodableVector();
                v.add(x);
                v.add(y);
                v.add(derDig);
                v.add(derEnc);
                DERSequence seq = new DERSequence(v);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DEROutputStream dos = new DEROutputStream(bos);

                try {
                    dos.writeObject(seq);
                    byte[] encRes = bos.toByteArray();//SM2加密结果
                    return dealSm2EncResultC(encRes, resType);
                } catch (IOException var18) {
                    var18.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 处理SM2加密结果为C1C3C2拼接样式
     * @param res
     * @return
     */
    public byte[] dealSm2EncResult (byte[] res) {
        if (res == null) {
            return null;
        }
        StringBuffer sbf = new StringBuffer();
        String resHex = FCharUtils.bytesToHexStr(res);
        if (resHex.startsWith("30")) {
            resHex = resHex.substring(2);
            String tl = resHex.substring(0, 2);
            byte[] bl = FCharUtils.hexString2ByteArray(tl);
            int bt = bl[0] & 0xFF;
            int l = 0;
            if (bt > 0x80) {
                l = bt - 0x80;
            }
            resHex = resHex.substring((l + 1) * 2);
            //解析C1 X部分
            if (resHex.startsWith("0220")) {
                sbf.append(resHex.substring(4, 68));
                resHex = resHex.substring(68);
            } else if (resHex.startsWith("022100")) {
                sbf.append(resHex.substring(6, 70));
                resHex = resHex.substring(70);
            } else {
                return null;
            }
            //解析C1 Y部分
            if (resHex.startsWith("0220")) {
                sbf.append(resHex.substring(4, 68));
                resHex = resHex.substring(68);
            } else if (resHex.startsWith("022100")) {
                sbf.append(resHex.substring(6, 70));
                resHex = resHex.substring(70);
            } else {
                return null;
            }
            //解析C3
            if (resHex.startsWith("0420")) {
                sbf.append(resHex.substring(4, 68));
                resHex = resHex.substring(68);
            } else if (resHex.startsWith("042100")) {
                sbf.append(resHex.substring(6, 70));
                resHex = resHex.substring(70);
            } else {
                return null;
            }
            //解析C2
            if (resHex.startsWith("04")) {
                resHex = resHex.substring(2);
                if (resHex.length() < 2) {
                    return null;
                }
                tl = resHex.substring(0, 2);
                bl = FCharUtils.hexString2ByteArray(tl);
                bt = bl[0] & 0xFF;
                l = 0;
                if (bt > 0x80) {
                    l = bt - 0x80;
                }
                sbf.append(resHex.substring((l + 1) * 2));
            } else {
                return null;
            }
            resHex = sbf.toString();
            return FCharUtils.hexString2ByteArray(resHex);
        }
        return null;
    }


    /**
     * 处理SM2加密结果为C1C2C3拼接样式
     * @param res
     * @return
     */
    public byte[] dealSm2EncResultC(byte[] res, int resType) {
        if (resType == 0) {
            return res;
        }
        String c1x;
        String c1y;
        String c2;
        String c3;

        if (res == null) {
            return null;
        }

        String resHex = FCharUtils.bytesToHexStr(res);
        if (resHex.startsWith("30")) {
            resHex = resHex.substring(2);
            String tl = resHex.substring(0, 2);
            byte[] bl = FCharUtils.hexString2ByteArray(tl);
            int bt = bl[0] & 0xFF;
            int l = 0;
            if (bt > 0x80) {
                l = bt - 0x80;
            }
            resHex = resHex.substring((l + 1) * 2);
            //解析C1 X部分
            if (resHex.startsWith("0220")) {
                c1x=(resHex.substring(4, 68));
                resHex = resHex.substring(68);
            } else if (resHex.startsWith("022100")) {
                c1x=(resHex.substring(6, 70));
                resHex = resHex.substring(70);
            } else {
                return null;
            }
            //解析C1 Y部分
            if (resHex.startsWith("0220")) {
                c1y=(resHex.substring(4, 68));
                resHex = resHex.substring(68);
            } else if (resHex.startsWith("022100")) {
                c1y=(resHex.substring(6, 70));
                resHex = resHex.substring(70);
            } else {
                return null;
            }
            //解析C3
            if (resHex.startsWith("0420")) {
                c3=(resHex.substring(4, 68));
                resHex = resHex.substring(68);
            } else if (resHex.startsWith("042100")) {
                c3=(resHex.substring(6, 70));
                resHex = resHex.substring(70);
            } else {
                return null;
            }
            //解析C2
            if (resHex.startsWith("04")) {
                resHex = resHex.substring(2);
                if (resHex.length() < 2) {
                    return null;
                }
                tl = resHex.substring(0, 2);
                bl = FCharUtils.hexString2ByteArray(tl);
                bt = bl[0] & 0xFF;
                l = 0;
                if (bt > 0x80) {
                    l = bt - 0x80;
                }
                c2=(resHex.substring((l + 1) * 2));
            } else {
                return null;
            }
            if (resType == 1) {
                resHex = c1x+c1y+c2+c3;
            } else {
                resHex = c1x+c1y+c3+c2;
            }
            return FCharUtils.hexString2ByteArray(resHex);
        }
        return null;
    }


    /**
     * 处理SM2加密结果为3081格式
     * @param res
     * @param type 1--C1C2C3, 2--C1C3C2
     * @return
     */
    public static byte[] dealSm2EncResultAdd (byte[] res, int type) {
        if (res == null) {
            return null;
        }
        if (res.length < 97) {
            return null;
        }
        String resStr = FCharUtils.bytesToHexStr(res);
        String c1x = resStr.substring(0, 64);
        String c1y = resStr.substring(64, 128);
        String c3 = resStr.substring(128, 192);
        String c2 = resStr.substring(192);
        if (type == 1) {
            c2 = resStr.substring(128, resStr.length() - 64);
            c3 = resStr.substring(resStr.length() - 64);
        }

        //处理C2
        int c2Len = c2.length() / 2;
        c2 = "04" + FCharUtils.int2HexStr(c2Len) + c2;
        //处理C3
        c3 = "0420" + c3;
        //处理C1Y
        char tc = c1y.charAt(0);
        if (tc > '7') {
            c1y = "022100" + c1y;
        } else {
            c1y = "0220" + c1y;
        }
        //处理C1X
        tc = c1x.charAt(0);
        if (tc > '7') {
            c1x = "022100" + c1x;
        } else {
            c1x = "0220" + c1x;
        }
        String dealStr = c1x + c1y + c3 + c2;
        int dealDataLen = dealStr.length() / 2;
        String lenStr = FCharUtils.int2HexStr(dealDataLen);
        dealStr = "30" + FCharUtils.int2HexStr(0x80 + lenStr.length() / 2) + lenStr + dealStr;
        return FCharUtils.hexString2ByteArray(dealStr);
    }


    /**
     * 处理SM2签名结果为C1C2拼接样式
     * @param res
     * @return
     */
    public byte[] dealSm2SignResultC (byte[] res) {
        if (res == null) {
            return null;
        }
        String s1 = "";
        String s2 = "";
        String resHex = FCharUtils.bytesToHexStr(res);
        if (resHex.startsWith("30")) {
            resHex = resHex.substring(4);
            //解析C1 X部分
            if (resHex.startsWith("0220")) {
                s1 = resHex.substring(4, 68);
                resHex = resHex.substring(68);
            } else if (resHex.startsWith("022100")) {
                s1 = resHex.substring(6, 70);
                resHex = resHex.substring(70);
            } else {
                return null;
            }
            //解析C1 Y部分
            if (resHex.startsWith("0220")) {
                s2 = resHex.substring(4, 68);
            } else if (resHex.startsWith("022100")) {
                s2 = resHex.substring(6, 70);
            } else {
                return null;
            }

            resHex = s1 + s2;
            return FCharUtils.hexString2ByteArray(resHex);
        }
        return null;
    }

    /**
     * 解密
     * @param privateKey
     * @param encryptedData
     * @return
     * @throws IOException
     */
    public byte[] sm2Decrypt (byte[] privateKey, byte[] encryptedData) {
        if (privateKey != null && privateKey.length != 0) {
            if (encryptedData != null && encryptedData.length != 0) {
                byte[] enc = new byte[encryptedData.length];
                System.arraycopy(encryptedData, 0, enc, 0, encryptedData.length);
                BigInteger userD = new BigInteger(1, privateKey);
                ByteArrayInputStream bis = new ByteArrayInputStream(enc);
                ASN1InputStream dis = new ASN1InputStream(bis);

                try {
                    //bc-jdk16-146
                    DERObject derObj = dis.readObject();
                    ASN1Sequence asn1 = (ASN1Sequence)derObj;
                    DERInteger x = (DERInteger)asn1.getObjectAt(0);
                    DERInteger y = (DERInteger)asn1.getObjectAt(1);
                    ECPoint c1 = ecc_curve.createPoint(x.getValue(), y.getValue(), true);
                    Cipher cipher = new Cipher();
                    cipher.Init_dec(userD, c1);
                    DEROctetString data = (DEROctetString)asn1.getObjectAt(3);

//                    //bc-jdk15on-151
//                    ASN1Primitive derObj = dis.readObject();
//                    Enumeration e = ((ASN1Sequence)derObj).getObjects();
//                    BigInteger r = ((ASN1Integer)e.nextElement()).getValue();
//                    BigInteger s = ((ASN1Integer)e.nextElement()).getValue();
//                    ECPoint c1 = ecc_curve.createPoint(r, s);
//                    Cipher cipher = new Cipher();
//                    cipher.Init_dec(userD, c1);
//                    e.nextElement();//此项为C3不需要,解密需要用后面一项C2
//                    DEROctetString data = (DEROctetString)e.nextElement();

                    enc = data.getOctets();
                    cipher.Decrypt(enc);
                    byte[] c3 = new byte[32];
                    cipher.Dofinal(c3);
                    return enc;
                } catch (IOException var15) {
                    var15.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * SM2验证签名
     * @param userId
     * @param publicKey
     * @param sourceData
     * @param signData
     * @return
     */
    public boolean sm2VerifySign (byte[] userId, byte[] publicKey, byte[] sourceData, byte[] signData, boolean isNeedAddPubKey) {
        if ((publicKey == null) || (publicKey.length == 0))
        {
            return false;
        }

        if ((sourceData == null) || (sourceData.length == 0))
        {
            return false;
        }
        byte[] formatedPubKey;
        if (publicKey.length == 64)
        {
            formatedPubKey = new byte[65];
            formatedPubKey[0] = 4;
            System.arraycopy(publicKey, 0, formatedPubKey, 1, publicKey.length);
        }
        else {
            formatedPubKey = publicKey;
        }
        ECPoint userKey = ecc_curve.decodePoint(formatedPubKey);

        if (isNeedAddPubKey) {
            sourceData = dealSignData(publicKey, sourceData);
        }
        SM3Digest sm3 = new SM3Digest();
        byte[] z = sm2GetZ(userId, userKey);
        sm3.update(z, 0, z.length);
        sm3.update(sourceData, 0, sourceData.length);
        byte[] md = new byte[32];
        sm3.doFinal(md, 0);

        ByteArrayInputStream bis = new ByteArrayInputStream(signData);
        ASN1InputStream dis = new ASN1InputStream(bis);
        SM2Result sm2Result = null;
        try {

            //bc-jdk16-146
            DERObject derObj = dis.readObject();
            Enumeration e = ((ASN1Sequence)derObj).getObjects();
            BigInteger r = ((DERInteger)e.nextElement()).getValue();
            BigInteger s = ((DERInteger)e.nextElement()).getValue();
            sm2Result = new SM2Result();
            sm2Result.r = r;
            sm2Result.s = s;
            sm2Verify(md, userKey, sm2Result.r, sm2Result.s, sm2Result);
            return sm2Result.r.equals(sm2Result.R);

//            //bc-jdk15on-151
//            ASN1Primitive derObj = dis.readObject();
//            Enumeration e = ((ASN1Sequence)derObj).getObjects();
//            BigInteger r = ((ASN1Integer)e.nextElement()).getValue();
//            BigInteger s = ((ASN1Integer)e.nextElement()).getValue();
//            sm2Result = new SM2Result();
//            sm2Result.r = r;
//            sm2Result.s = s;
//            System.out.println("r: " + sm2Result.r.toString(16));
//            System.out.println("s: " + sm2Result.s.toString(16));
//            sm2Verify(md, userKey, sm2Result.r, sm2Result.s, sm2Result);
//            return sm2Result.r.equals(sm2Result.R);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return false;
    }

    /**
     * 签名数据补位处理
     * @param signature_
     * @return
     */
    public String signDataAdd (String signature_) {
        byte[] smRS = FCharUtils.hexString2ByteArray(signature_);
        byte[] smR = new byte[32];
        byte[] smS = new byte[32];
        System.arraycopy(smRS, 0, smR, 0, 32);
        System.arraycopy(smRS, 32, smS, 0, 32);
        String ret = "";
        if ((smR[0] & 0xF0) >> 7 == 1) {
            ret = ret + "022100" + FCharUtils.bytesToHexStr(smR);
        } else {
            ret = ret + "0220" + FCharUtils.bytesToHexStr(smR);
        }

        if ((smS[0] & 0xF0) >> 7 == 1) {
            ret = ret + "022100" + FCharUtils.bytesToHexStr(smS);
        } else {
            ret = ret + "0220" + FCharUtils.bytesToHexStr(smS);
        }
        int allLenght = ret.length() / 2;
        signature_ = "30" + FCharUtils.intToByte(allLenght) + ret;
        return signature_;
    }
    /**
     * 从本地导入公钥
     *
     * @param path
     * @return
     */
    public ECPoint importPublicKey(Context mContext, String path) {
        try {

            InputStream fis = mContext.getAssets().open(path);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte buffer[] = new byte[16];
            int size;
            while ((size = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, size);
            }
            fis.close();
            byte[] pubKs = baos.toByteArray();
            TMKeyLog.d(TAG, "importPublicKey>>>pubKs:" + FCharUtils.showResult16Str(pubKs) + ">>>pubKeyLen:" + pubKs.length);
            return ecc_curve.decodePoint(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在签名数据前添加公钥ZA
     * @param pubKey
     * @param signDataBytes
     * @return
     */
    private byte[] dealSignData (byte[] pubKey, byte[] signDataBytes) {
        if (pubKey == null) {
            return null;
        }
        String sm2SignSupplement = FCharUtils.bytesToHexStr(pubKey);
        if (sm2SignSupplement.length() != 128) {
            return null;
        }
        String smX = sm2SignSupplement.substring(0, 64);
        String smY = sm2SignSupplement.substring(64);
        byte[] zpading = SM2.getInstance().sm2GetZSM(FCharUtils.hexString2ByteArray(SM2Util.SM2UserId), smX, smY);
        sm2SignSupplement = FCharUtils.bytesToHexStr(zpading);
        String signData = sm2SignSupplement + FCharUtils.bytesToHexStr(signDataBytes);
        signData = SM3.sm3Hash(signData);
        return FCharUtils.hexString2ByteArray(signData);
    }
}
