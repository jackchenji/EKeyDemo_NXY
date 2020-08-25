package bip.internet;

/*
 *Created by chenji on 2020/7/17
 */ public class BipRequest {
    String randId;
    String cmd;


    public BipRequest(String randId, String cmd) {
        this.randId = randId;
        this.cmd = cmd;
    }

    public String getRandId() {
        return randId;
    }

    public void setRandId(String randId) {
        this.randId = randId;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }


}
