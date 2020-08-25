package com.froad.ukey.simchannel.oma;

/**
 * Created by FW on 2018/1/5.
 */

public interface SuperOMATranInterface {
    boolean init(byte[] paramArrayOfByte);

    byte[] transmit(byte[] paramArrayOfByte);

    void close();
}
