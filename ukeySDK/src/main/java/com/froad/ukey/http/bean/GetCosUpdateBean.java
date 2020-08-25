package com.froad.ukey.http.bean;

/**
 * 手机方式请求结果
 */
public class GetCosUpdateBean extends AuthResponseResult {

    private String upgradeSign;
    private String cosInstruction;
    private String pin;
    private String newAtr;
    private String mac;

    public String getUpgradeSign() {
        return upgradeSign;
    }

    public void setUpgradeSign(String upgradeSign) {
        this.upgradeSign = upgradeSign;
    }

    public String getCosInstruction() {
        return cosInstruction;
    }

    public void setCosInstruction(String cosInstruction) {
        this.cosInstruction = cosInstruction;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getNewAtr() {
        return newAtr;
    }

    public void setNewAtr(String newAtr) {
        this.newAtr = newAtr;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public String toString() {
        return "GetCosUpdateBean{" +
                "cosInstruction='" + cosInstruction + '\'' +
                ", pin='" + pin + '\'' +
                ", newAtr='" + newAtr + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}
