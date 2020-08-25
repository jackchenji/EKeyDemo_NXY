package com.froad.ukey.utils;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * Created by FW on 2017/7/5.
 */

public class SM2Result {
    public BigInteger r;
    public BigInteger s;
    public BigInteger R;
    public byte[] sa;
    public byte[] sb;
    public byte[] s1;
    public byte[] s2;
    public ECPoint keyra;
    public ECPoint keyrb;
}
