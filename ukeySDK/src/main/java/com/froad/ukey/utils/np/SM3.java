package com.froad.ukey.utils.np;

import com.froad.ukey.utils.SM3Digest;

/**
 * Created by FW on 2017/4/20.
 */

public class SM3 {

//    public static final byte[] iv = { 115, -128, 22, 111, 73,
//            20, -78, -71, 23, 36, 66, -41,
//            -38, -118, 6, 0, -87, 111, 48,
//            -68, 22, 49, 56, -86, -29,
//            -115, -18, 77, -80, -5, 14,
//            78 };
//
//    private static char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
//            '9', 'A', 'B', 'C', 'D', 'E', 'F'};
//    private static final String ivHexStr = "7380166f 4914b2b9 172442d7 da8a0600 a96f30bc 163138aa e38dee4d b0fb0e4e";
//    private static final BigInteger IV = new BigInteger(ivHexStr.replaceAll(" ",
//            ""), 16);
//    private static final Integer Tj15 = Integer.valueOf("79cc4519", 16);
//    private static final Integer Tj63 = Integer.valueOf("7a879d8a", 16);
//    private static final byte[] FirstPadding = {(byte) 0x80};
//    private static final byte[] ZeroPadding = {(byte) 0x00};
//
//    private static int T(int j) {
//        if (j >= 0 && j <= 15) {
//            return Tj15.intValue();
//        } else if (j >= 16 && j <= 63) {
//            return Tj63.intValue();
//        } else {
//            throw new RuntimeException("data invalid");
//        }
//    }
//
//    private static Integer FF(Integer x, Integer y, Integer z, int j) {
//        if (j >= 0 && j <= 15) {
//            return Integer.valueOf(x.intValue() ^ y.intValue() ^ z.intValue());
//        } else if (j >= 16 && j <= 63) {
//            return Integer.valueOf((x.intValue() & y.intValue())
//                    | (x.intValue() & z.intValue())
//                    | (y.intValue() & z.intValue()));
//        } else {
//            throw new RuntimeException("data invalid");
//        }
//    }
//
//    private static Integer GG(Integer x, Integer y, Integer z, int j) {
//        if (j >= 0 && j <= 15) {
//            return Integer.valueOf(x.intValue() ^ y.intValue() ^ z.intValue());
//        } else if (j >= 16 && j <= 63) {
//            return Integer.valueOf((x.intValue() & y.intValue())
//                    | (~x.intValue() & z.intValue()));
//        } else {
//            throw new RuntimeException("data invalid");
//        }
//    }
//
//    private static Integer P0(Integer x) {
//        return Integer.valueOf(x.intValue()
//                ^ Integer.rotateLeft(x.intValue(), 9)
//                ^ Integer.rotateLeft(x.intValue(), 17));
//    }
//
//    private static Integer P1(Integer x) {
//        return Integer.valueOf(x.intValue()
//                ^ Integer.rotateLeft(x.intValue(), 15)
//                ^ Integer.rotateLeft(x.intValue(), 23));
//    }
//
//    private static byte[] padding(byte[] source) throws IOException {
//        if (source.length >= 0x2000000000000000l) {
//            throw new RuntimeException("src data invalid.");
//        }
//        long l = source.length * 8;
//        long k = 448 - (l + 1) % 512;
//        if (k < 0) {
//            k = k + 512;
//        }
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        baos.write(source);
//        baos.write(FirstPadding);
//        long i = k - 7;
//        while (i > 0) {
//            baos.write(ZeroPadding);
//            i -= 8;
//        }
//        baos.write(long2bytes(l));
//        return baos.toByteArray();
//    }
//
//    private static byte[] long2bytes(long l) {
//        byte[] bytes = new byte[8];
//        for (int i = 0; i < 8; i++) {
//            bytes[i] = (byte) (l >>> ((7 - i) * 8));
//        }
//        return bytes;
//    }
//
//    public static byte[] hash(byte[] source) {
//        byte[] vi1 = null;
//        try {
//            byte[] m1 = padding(source);
//            int n = m1.length / (512 / 8);
//            byte[] b;
//            byte[] vi = IV.toByteArray();
//            for (int i = 0; i < n; i++) {
//                b = Arrays.copyOfRange(m1, i * 64, (i + 1) * 64);
//                vi1 = CF(vi, b);
//                vi = vi1;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return vi1;
//    }
//
//    private static byte[] CF(byte[] vi, byte[] bi) throws IOException {
//        int a, b, c, d, e, f, g, h;
//        a = toInteger(vi, 0);
//        b = toInteger(vi, 1);
//        c = toInteger(vi, 2);
//        d = toInteger(vi, 3);
//        e = toInteger(vi, 4);
//        f = toInteger(vi, 5);
//        g = toInteger(vi, 6);
//        h = toInteger(vi, 7);
//
//        int[] w = new int[68];
//        int[] w1 = new int[64];
//        for (int i = 0; i < 16; i++) {
//            w[i] = toInteger(bi, i);
//        }
//        for (int j = 16; j < 68; j++) {
//            w[j] = P1(w[j - 16] ^ w[j - 9] ^ Integer.rotateLeft(w[j - 3], 15))
//                    ^ Integer.rotateLeft(w[j - 13], 7) ^ w[j - 6];
//        }
//        for (int j = 0; j < 64; j++) {
//            w1[j] = w[j] ^ w[j + 4];
//        }
//        int ss1, ss2, tt1, tt2;
//        for (int j = 0; j < 64; j++) {
//            ss1 = Integer
//                    .rotateLeft(
//                            Integer.rotateLeft(a, 12) + e
//                                    + Integer.rotateLeft(T(j), j), 7);
//            ss2 = ss1 ^ Integer.rotateLeft(a, 12);
//            tt1 = FF(a, b, c, j) + d + ss2 + w1[j];
//            tt2 = GG(e, f, g, j) + h + ss1 + w[j];
//            d = c;
//            c = Integer.rotateLeft(b, 9);
//            b = a;
//            a = tt1;
//            h = g;
//            g = Integer.rotateLeft(f, 19);
//            f = e;
//            e = P0(tt2);
//        }
//        byte[] v = toByteArray(a, b, c, d, e, f, g, h);
//        for (int i = 0; i < v.length; i++) {
//            v[i] = (byte) (v[i] ^ vi[i]);
//        }
//        return v;
//    }
//
//    private static int toInteger(byte[] source, int index) {
//        StringBuilder valueStr = new StringBuilder("");
//        for (int i = 0; i < 4; i++) {
//            valueStr.append(chars[(byte) ((source[index * 4 + i] & 0xF0) >> 4)]);
//            valueStr.append(chars[(byte) (source[index * 4 + i] & 0x0F)]);
//        }
//        return Long.valueOf(valueStr.toString(), 16).intValue();
//
//    }
//
//    private static byte[] toByteArray(int a, int b, int c, int d, int e, int f,
//                                      int g, int h) throws IOException {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
//        baos.write(toByteArray(a));
//        baos.write(toByteArray(b));
//        baos.write(toByteArray(c));
//        baos.write(toByteArray(d));
//        baos.write(toByteArray(e));
//        baos.write(toByteArray(f));
//        baos.write(toByteArray(g));
//        baos.write(toByteArray(h));
//        return baos.toByteArray();
//    }
//
//    private static byte[] toByteArray(int i) {
//        byte[] byteArray = new byte[4];
//        byteArray[0] = (byte) (i >>> 24);
//        byteArray[1] = (byte) ((i & 0xFFFFFF) >>> 16);
//        byteArray[2] = (byte) ((i & 0xFFFF) >>> 8);
//        byteArray[3] = (byte) (i & 0xFF);
//        return byteArray;
//    }
//
//    private static void printIntArray(int[] intArray, int lineSize) {
//        for (int i = 0; i < intArray.length; i++) {
//            byte[] byteArray = toByteArray(intArray[i]);
//            int j = 0;
//            while (j < byteArray.length) {
//                System.out.print(chars[(byteArray[j] & 0xFF) >> 4]);
//                System.out.print(chars[byteArray[j] & 0xF]);
//                j++;
//            }
//            System.out.print(" ");
//            if (i % lineSize == (lineSize - 1)) {
//                System.out.println(" ");
//            }
//        }
//    }
//
//    public static void main(String[] args) throws IOException {
//        byte[] source, sm3HashValue;
//
//        source = new byte[]{97, 98, 99, 100, 97, 98, 99, 100, 97, 98, 99, 100,
//                97, 98, 99, 100, 97, 98, 99, 100, 97, 98, 99, 100, 97, 98, 99,
//                100, 97, 98, 99, 100, 97, 98, 99, 100, 97, 98, 99, 100, 97, 98,
//                99, 100, 97, 98, 99, 100, 97, 98, 99, 100, 97, 98, 99, 100, 97,
//                98, 99, 100, 97, 98, 99, 100};
//        sm3HashValue = SM3.hash(source);
//    }
//
//    public static byte[] CF_New(byte[] V, byte[] B)
//    {
//        int[] v = convert_New(V);
//        int[] b = convert_New(B);
//        return convert_New(CF_New(v, b));
//    }
//
//    public static int[] CF_New(int[] V, int[] B)
//    {
//        int a = V[0];
//        int b = V[1];
//        int c = V[2];
//        int d = V[3];
//        int e = V[4];
//        int f = V[5];
//        int g = V[6];
//        int h = V[7];
//
//        int[][] arr = expand(B);
//        int[] w = arr[0];
//        int[] w1 = arr[1];
//
//        for (int j = 0; j < 64; ++j)
//        {
//            int ss1 = bitCycleLeft(a, 12) + e + bitCycleLeft(Tj[j], j);
//            ss1 = bitCycleLeft(ss1, 7);
//            int ss2 = ss1 ^ bitCycleLeft(a, 12);
//            int tt1 = FFj(a, b, c, j) + d + ss2 + w1[j];
//            int tt2 = GGj(e, f, g, j) + h + ss1 + w[j];
//            d = c;
//            c = bitCycleLeft(b, 9);
//            b = a;
//            a = tt1;
//            h = g;
//            g = bitCycleLeft(f, 19);
//            f = e;
//            e = P0(tt2);
//        }
//
//        int[] out = new int[8];
//        out[0] = (a ^ V[0]);
//        out[1] = (b ^ V[1]);
//        out[2] = (c ^ V[2]);
//        out[3] = (d ^ V[3]);
//        out[4] = (e ^ V[4]);
//        out[5] = (f ^ V[5]);
//        out[6] = (g ^ V[6]);
//        out[7] = (h ^ V[7]);
//
//        return out;
//    }
//
//    public static byte[] padding_New(byte[] in, int bLen)
//    {
//        int k = 448 - (8 * in.length + 1) % 512;
//        if (k < 0)
//        {
//            k = 960 - (8 * in.length + 1) % 512;
//        }
//        ++k;
//        byte[] padd = new byte[k / 8];
//        padd[0] = -128;
//        long n = in.length * 8 + bLen * 512;
//        byte[] out = new byte[in.length + k / 8 + 8];
//        int pos = 0;
//        System.arraycopy(in, 0, out, 0, in.length);
//        pos += in.length;
//        System.arraycopy(padd, 0, out, pos, padd.length);
//        pos += padd.length;
//        byte[] tmp = back_New(FCharUtils.longToBytes(n));
//        System.arraycopy(tmp, 0, out, pos, tmp.length);
//        return out;
//    }
//
//    private static byte[] back_New(byte[] in)
//    {
//        byte[] out = new byte[in.length];
//        for (int i = 0; i < out.length; ++i)
//        {
//            out[i] = in[(out.length - i - 1)];
//        }
//
//        return out;
//    }
//
//    private static int[] convert_New(byte[] arr)
//    {
//        int[] out = new int[arr.length / 4];
//        byte[] tmp = new byte[4];
//        for (int i = 0; i < arr.length; i += 4)
//        {
//            System.arraycopy(arr, i, tmp, 0, 4);
//            out[(i / 4)] = bigEndianByteToInt_New(tmp);
//        }
//        return out;
//    }
//
//    private static byte[] convert_New(int[] arr)
//    {
//        byte[] out = new byte[arr.length * 4];
//        byte[] tmp = null;
//        for (int i = 0; i < arr.length; ++i)
//        {
//            tmp = bigEndianByteToInt_New(arr[i]);
//            System.arraycopy(tmp, 0, out, i * 4, 4);
//        }
//        return out;
//    }
//
//    private static int bigEndianByteToInt_New(byte[] bytes)
//    {
//        return FCharUtils.byteToInt(back_New(bytes));
//    }
//
//    private static byte[] bigEndianByteToInt_New(int num)
//    {
//        return back_New(FCharUtils.intToBytes(num));
//    }
//
//    private static int bitCycleLeft(int n, int bitLen)
//    {
//        bitLen %= 32;
//        byte[] tmp = bigEndianIntToByte(n);
//        int byteLen = bitLen / 8;
//        int len = bitLen % 8;
//        if (byteLen > 0)
//        {
//            tmp = byteCycleLeft(tmp, byteLen);
//        }
//
//        if (len > 0)
//        {
//            tmp = bitSmall8CycleLeft(tmp, len);
//        }
//
//        return bigEndianByteToInt(tmp);
//    }

    public static final byte[] iv = { 115, -128, 22, 111, 73,
            20, -78, -71, 23, 36, 66, -41,
            -38, -118, 6, 0, -87, 111, 48,
            -68, 22, 49, 56, -86, -29,
            -115, -18, 77, -80, -5, 14,
            78 };

    public static int[] Tj = new int[64];

    static
    {
        for (int i = 0; i < 16; ++i)
        {
            Tj[i] = 2043430169;
        }

        for (int i = 16; i < 64; ++i)
        {
            Tj[i] = 2055708042;
        }
    }

    public static byte[] CF(byte[] V, byte[] B)
    {
        int[] v = convert(V);
        int[] b = convert(B);
        return convert(CF(v, b));
    }

    private static int[] convert(byte[] arr)
    {
        int[] out = new int[arr.length / 4];
        byte[] tmp = new byte[4];
        for (int i = 0; i < arr.length; i += 4)
        {
            System.arraycopy(arr, i, tmp, 0, 4);
            out[(i / 4)] = bigEndianByteToInt(tmp);
        }
        return out;
    }

    private static byte[] convert(int[] arr)
    {
        byte[] out = new byte[arr.length * 4];
        byte[] tmp = null;
        for (int i = 0; i < arr.length; ++i)
        {
            tmp = bigEndianIntToByte(arr[i]);
            System.arraycopy(tmp, 0, out, i * 4, 4);
        }
        return out;
    }

    public static int[] CF(int[] V, int[] B)
    {
        int a = V[0];
        int b = V[1];
        int c = V[2];
        int d = V[3];
        int e = V[4];
        int f = V[5];
        int g = V[6];
        int h = V[7];

        int[][] arr = expand(B);
        int[] w = arr[0];
        int[] w1 = arr[1];

        for (int j = 0; j < 64; ++j)
        {
            int ss1 = bitCycleLeft(a, 12) + e + bitCycleLeft(Tj[j], j);
            ss1 = bitCycleLeft(ss1, 7);
            int ss2 = ss1 ^ bitCycleLeft(a, 12);
            int tt1 = FFj(a, b, c, j) + d + ss2 + w1[j];
            int tt2 = GGj(e, f, g, j) + h + ss1 + w[j];
            d = c;
            c = bitCycleLeft(b, 9);
            b = a;
            a = tt1;
            h = g;
            g = bitCycleLeft(f, 19);
            f = e;
            e = P0(tt2);
        }

        int[] out = new int[8];
        out[0] = (a ^ V[0]);
        out[1] = (b ^ V[1]);
        out[2] = (c ^ V[2]);
        out[3] = (d ^ V[3]);
        out[4] = (e ^ V[4]);
        out[5] = (f ^ V[5]);
        out[6] = (g ^ V[6]);
        out[7] = (h ^ V[7]);

        return out;
    }

    private static int[][] expand(int[] B)
    {
        int[] W = new int[68];
        int[] W1 = new int[64];
        for (int i = 0; i < B.length; ++i)
        {
            W[i] = B[i];
        }

        for (int i = 16; i < 68; ++i)
        {
            W[i] =
                    (P1(W[(i - 16)] ^ W[(i - 9)] ^ bitCycleLeft(W[(i - 3)], 15)) ^
                            bitCycleLeft(W[(i - 13)], 7) ^ W[(i - 6)]);
        }

        for (int i = 0; i < 64; ++i)
        {
            W1[i] = (W[i] ^ W[(i + 4)]);
        }

        int[][] arr = { W, W1 };
        return arr;
    }

    private static byte[] bigEndianIntToByte(int num)
    {
        return back(FCharUtils.intToBytes(num));
    }

    private static int bigEndianByteToInt(byte[] bytes)
    {
        return FCharUtils.byteToInt(back(bytes));
    }

    private static int FFj(int X, int Y, int Z, int j)
    {
        if ((j >= 0) && (j <= 15))
        {
            return FF1j(X, Y, Z);
        }

        return FF2j(X, Y, Z);
    }

    private static int GGj(int X, int Y, int Z, int j)
    {
        if ((j >= 0) && (j <= 15))
        {
            return GG1j(X, Y, Z);
        }

        return GG2j(X, Y, Z);
    }

    private static int FF1j(int X, int Y, int Z)
    {
        int tmp = X ^ Y ^ Z;
        return tmp;
    }

    private static int FF2j(int X, int Y, int Z)
    {
        int tmp = X & Y | X & Z | Y & Z;
        return tmp;
    }

    private static int GG1j(int X, int Y, int Z)
    {
        int tmp = X ^ Y ^ Z;
        return tmp;
    }

    private static int GG2j(int X, int Y, int Z)
    {
        int tmp = X & Y | (X ^ 0xFFFFFFFF) & Z;
        return tmp;
    }

    private static int P0(int X)
    {
        int y = rotateLeft(X, 9);
        y = bitCycleLeft(X, 9);
        int z = rotateLeft(X, 17);
        z = bitCycleLeft(X, 17);
        int t = X ^ y ^ z;
        return t;
    }

    private static int P1(int X)
    {
        int t = X ^ bitCycleLeft(X, 15) ^ bitCycleLeft(X, 23);
        return t;
    }

    public static byte[] padding(byte[] in, int bLen)
    {
        int k = 448 - (8 * in.length + 1) % 512;
        if (k < 0)
        {
            k = 960 - (8 * in.length + 1) % 512;
        }
        ++k;
        byte[] padd = new byte[k / 8];
        padd[0] = -128;
        long n = in.length * 8 + bLen * 512;
        byte[] out = new byte[in.length + k / 8 + 8];
        int pos = 0;
        System.arraycopy(in, 0, out, 0, in.length);
        pos += in.length;
        System.arraycopy(padd, 0, out, pos, padd.length);
        pos += padd.length;
        byte[] tmp = back(FCharUtils.longToBytes(n));
        System.arraycopy(tmp, 0, out, pos, tmp.length);
        return out;
    }

    private static byte[] back(byte[] in)
    {
        byte[] out = new byte[in.length];
        for (int i = 0; i < out.length; ++i)
        {
            out[i] = in[(out.length - i - 1)];
        }

        return out;
    }

    public static int rotateLeft(int x, int n)
    {
        return x << n | x >> 32 - n;
    }

    private static int bitCycleLeft(int n, int bitLen)
    {
        bitLen %= 32;
        byte[] tmp = bigEndianIntToByte(n);
        int byteLen = bitLen / 8;
        int len = bitLen % 8;
        if (byteLen > 0)
        {
            tmp = byteCycleLeft(tmp, byteLen);
        }

        if (len > 0)
        {
            tmp = bitSmall8CycleLeft(tmp, len);
        }

        return bigEndianByteToInt(tmp);
    }

    private static byte[] bitSmall8CycleLeft(byte[] in, int len)
    {
        byte[] tmp = new byte[in.length];

        for (int i = 0; i < tmp.length; ++i)
        {
            int t1 = (byte)((in[i] & 0xFF) << len);
            int t2 = (byte)((in[((i + 1) % tmp.length)] & 0xFF) >> 8 - len);
            int t3 = (byte)(t1 | t2);
            tmp[i] = (byte)t3;
        }

        return tmp;
    }

    private static byte[] byteCycleLeft(byte[] in, int byteLen)
    {
        byte[] tmp = new byte[in.length];
        System.arraycopy(in, byteLen, tmp, 0, in.length - byteLen);
        System.arraycopy(in, 0, tmp, in.length - byteLen, byteLen);
        return tmp;
    }

    public static String sm3Hash(String scrData) {
        byte[] md = new byte[32];
        byte[] msg1 = FCharUtils.hexString2ByteArray(scrData);
        SM3Digest sm3 = new SM3Digest();
        sm3.update(msg1, 0, msg1.length);
        sm3.doFinal(md, 0);

        return FCharUtils.bytesToHexStr(md);
    }
    public static byte[] sm3Hash(byte[] scrData) {
        byte[] md = new byte[32];
        byte[] msg1 = scrData;
        SM3Digest sm3 = new SM3Digest();
        sm3.update(msg1, 0, msg1.length);
        sm3.doFinal(md, 0);

        return md;
    }
}

