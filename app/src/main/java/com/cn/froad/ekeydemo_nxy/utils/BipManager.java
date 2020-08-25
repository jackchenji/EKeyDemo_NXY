package com.cn.froad.ekeydemo_nxy.utils;


import com.froad.ukey.utils.np.TMKeyLog;

/*
 *Created by user on 2020/3/11
 */ public class BipManager {
    public static BipManager bipManager;
    private static String TAG="BipManager";
    private static Client client;

    public static BipManager getInstance()
    {
        if (bipManager == null) {
            bipManager = new BipManager();
            client = new Client(Constants.waiwangip, Constants.waiwnangport);
        }
        return bipManager;
    }

   //获取随机数结果
    public static void   getInstructResult(String instruct,BipInterf interf) {
        if(client==null){
            client = new Client(Constants.waiwangip, Constants.waiwnangport);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.doSocket(Constants.randomInstruct, new ResultInterf() {
                    @Override
                    public void onResult(Object object) {
                        String random=((String) object).substring(6);
                        TMKeyLog.i(TAG,"mac连接请求,获取随机数====>:"+random);
                        String bipInstruct=Bip.getBipConnect("0833707000115223","FDC88FFA",random);
                        TMKeyLog.i(TAG,"bip连接请求指令:"+bipInstruct);
                        client.doSocket(bipInstruct, new ResultInterf() {
                            @Override
                            public void onResult(Object object) {
                                String objectstring=(String)object;
                                TMKeyLog.i(TAG,"BIP连接请求随机数====>:"+objectstring.substring(6,14));
                                String apdukey=Bip.getAdpuKey("0833707000115223",objectstring.substring(6,14));  //会话秘钥
                                TMKeyLog.i(TAG,"apdu会话秘钥====>:"+apdukey);
                                String APDU=Bip.getAdpuInstruct(Constants.nxyInstructHead+instruct,apdukey); //获取版本号
                                TMKeyLog.i(TAG,"apdu:"+APDU);
                                String apduinstruct=Bip.getAdpuInstructMac("12"+APDU,apdukey);
                                TMKeyLog.i(TAG,"发往服务器apdu指令码:"+apduinstruct);
                                client.doSocket(apduinstruct, new ResultInterf() {
                                    @Override
                                    public void onResult(Object object) {
                                        String objectstring=(String)object;
                                        TMKeyLog.i(TAG," Apdu指令透传结果====>:"+object);
                                        if(objectstring.length()==16){
                                            if(objectstring.substring(10).equals("")){
                                                TMKeyLog.i(TAG," 需要重新绑定");

                                            }
                                        }

                                        Boolean macResult=Bip.jiaoyangmac(objectstring.substring(4,objectstring.length()-8),apdukey,objectstring.substring(objectstring.length()-8));
                                        TMKeyLog.i(TAG," mac比对结果====>:"+macResult);
                                        if(macResult==true){
                                            String result=Bip.jiemiResult(objectstring.substring(6,objectstring.length()-8),apdukey);
                                            TMKeyLog.i(TAG," 服务器返回值解密结果："+result);
                                            interf.onSuccess(result);
                                        }else{
                                            TMKeyLog.i(TAG," mac:对比失败");
                                            interf.onFailure("服务器随机数获取失败");
                                        }
                                    }
                                });

                            }
                        });


                    }
                });


            }
        }).start();
    }







}
