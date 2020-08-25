package bip;


import android.os.ConditionVariable;
import com.froad.ukey.manager.VCardApi_FFT;
import com.froad.ukey.utils.np.FCharUtils;
import com.froad.ukey.utils.np.TMKeyLog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/*
 *Created by chenji on 2020/3/11
 */ public class BipManager implements  BipInterf{
    public static BipManager bipManager;
    public static String TAG="BipManager";
    public static OkhttpUtil httpUtil;
    public static boolean isUseBip=false;
    public static     String error;
    public static boolean hasCard=false;
    public static String Instruct="";
    public static String  carNO="";
    public static String apdukey=""; //会话秘钥
    public static boolean hasBindCard=false; //是否绑定
    public static String   lastCount="";
    public static ConditionVariable conditionVariable = new ConditionVariable();//锁
    public  String  errorMessage="";
    public  String  errorCode="";
    public ArrayList<String>  arrayList=new ArrayList<>();
    public int flag=1;
    public int lastflag=1;
    public BipEntity bipEntity;
    public boolean macResult=false;
    public String instructs="";
    public String count=""; //计数器
    public Observable<String> observable;
    public Observer<String> observer;
    public String[] firstConnectArray;
    public BipResult  myBipResult;
    public ArrayList<BipResult>  myBipResultList;
    public Observable<String> resultobservable;
    public Observer<String> resultobserver;


    public static BipManager getInstance()
    {
        if (bipManager==null) {
            synchronized (BipManager.class) {//在创建对象时再进行同步锁定
                if (bipManager == null) {
                    bipManager = new BipManager();
                }
                if(conditionVariable==null){
                    conditionVariable = new ConditionVariable();
                }
            }
        }
        return bipManager;
    }


    @Override
    public ArrayList<BipResult> onStartBip(final String instruct) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                resultobservable = Observable.create(new ObservableOnSubscribe<String>() {
                    @Override public void subscribe(ObservableEmitter<String> e) throws Exception {
                        e.onComplete();
                    }
                });
                resultobserver = new Observer<String>() {
                    @Override public void onSubscribe(Disposable d) {

                    }
                    @Override public void onNext(String s) {

                    }   @Override public void onError(Throwable e) {
                        TMKeyLog.i(TAG,"错误信息代码："+errorCode+",错误信息："+errorMessage);
                        onDestroy();
                        myBipResult.isSuccess=false;
                        myBipResult.message="错误信息代码："+errorCode+", 错误信息："+errorMessage;
                        myBipResultList.add(myBipResult);
                        conditionVariable.open();
                    }   @Override public void onComplete() {
                        conditionVariable.open();   //获取数据完成后,解锁
                    }
                };        //订阅事件

           onCreate(instruct);

            }
        }).start();

        conditionVariable.close();//复位
        conditionVariable.block(30000);// 锁30秒后返回数据
        return myBipResultList;
    }

    @Override
    public void onCreate(String instruct) {
        TMKeyLog.i(TAG,"bip请求指令====>"+instruct);
        instructs="";
        bipEntity=null;
        httpUtil=null;
        instructs=instruct;
        httpUtil = new OkhttpUtil();
        bipEntity= VCardApi_FFT.mBipEntity;
        myBipResult=new BipResult();
        myBipResultList=new ArrayList<BipResult>();

        String yangzhengma=bipEntity.getYangzhengma();
        if(!yangzhengma.equals("")){
            instruct=instruct+bipEntity.getYangzhengma();
            TMKeyLog.i(TAG,"验证码为:"+yangzhengma);
        }
        bipEntity.setInstruct(instruct);

        TMKeyLog.i(TAG,"是否绑定过卡:"+hasBindCard);
        if(hasBindCard){   //判断卡是否连接成功
            onStartCommond(); //卡如果绑定成功直接发指令
        }else {
            observable = Observable.create(new ObservableOnSubscribe<String>() {
                @Override public void subscribe(ObservableEmitter<String> e) throws Exception {
                e.onNext("");
            }
            });
            observer = new Observer<String>() {
                @Override public void onSubscribe(Disposable d) {

            }
                @Override public void onNext(String s) {
                    onStartCommond();
            }   @Override public void onError(Throwable e) {

            }   @Override public void onComplete() {

            }
            };        //订阅事件


            onBind();//如果没有绑定过卡的话 重新绑定卡
        }





    }

    @Override
    public boolean onBind() {  //绑定卡号 获取卡号值
        try{                          //第一步获取随机数
                    hasBindCard=false;//重置是否绑定过
                    httpUtil.doMySocket(Constants.randomInstruct+"FF", new ResultInterf() {
                        @Override
                        public void onResult(Object object) {
                            String random=((String) object).substring(6,38);
                            carNO=((String) object).substring(38);
                            TMKeyLog.i(TAG,"随机数连接请求,总数据====>:"+object);
                            TMKeyLog.i(TAG,"随机数连接请求,获取随机数====>:"+random);
                            TMKeyLog.i(TAG,"随机数连接请求,获取连接id====>:"+carNO);
                            TMKeyLog.i(TAG,"传进来的数据:"+bipEntity.getDatas());

                            String[] connectArray=new String[3];
                            if(flag!=5){
                                TMKeyLog.i(TAG,"flag1值 ====>:"+flag);
                                connectArray=Bip.getBipBindInstructForTest(random,"00",bipEntity.getDatas(),bipEntity.getTypeflag());}
                            if(flag==5){
                                TMKeyLog.i(TAG,"flag2值 ====>:"+flag);
                                connectArray=Bip.getBipBindInstructForTest(random,"01",bipEntity.getDatas(),bipEntity.getTypeflag());  //通过mt下发短信 通知卡片打开bip通道
                            }if(flag>7){
                                flag=0;//重置flag
                                resultobserver.onError(new Throwable()); //绑定7失败 退出程序
                                return;
                            }
                            TMKeyLog.i(TAG,"第"+flag+"次连接:");
                            flag++;
                            final String privateKey=connectArray[1];   //私钥
                            httpUtil.doMySocket(connectArray[2], new ResultInterf() {  //bip连接请求
                                @Override
                                public void onResult(Object object) {
                                    String objectstring=(String)object;
                                    TMKeyLog.i(TAG,"BIP绑定结果:"+objectstring);
                                    String resultFalg=objectstring.substring(4,6);
                                    getErrorCode(objectstring.substring(6,14));
                                    if(resultFalg.equals("35")){
                                        if(objectstring.length()==22){  //如果绑定的过程中出现错误,重新连接
                                            try {
                                                Thread.sleep(3*1000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            onBind();  //如果绑定错误 重新连接
                                        }else {
                                            firstConnectArray=Bip.getBipFirstInstructResult(privateKey,objectstring.substring(6,objectstring.length()-8)); //获取解密数据
                                        if(firstConnectArray!=null&&!firstConnectArray[0].equals("")){
                                            hasBindCard=true;
                                        }else {
                                            hasBindCard=false;
                                            onBind();
                                        }
                                        TMKeyLog.i(TAG,"bip绑定后台获取到的会话秘钥====>:"+apdukey);
                                        observable.subscribe(observer);}
                                    }else {
                                        onBind();//如果绑定失败，重新连接
                                    }
                                }
                            },carNO);
                        }
                    },"");

                }catch (Exception E){

                }


        return false;
    }

    @Override
    public void onStartCommond() {
        TMKeyLog.i(TAG,"开始发送数据，onStartCommond:");
        count=getcount();
        if(count.equals(lastCount)){
            count=getcount();     //每次发请求的时候校验count
        }
        apdukey=Bip.getAdpuKeyforLast(firstConnectArray[3],firstConnectArray[4],firstConnectArray[1],count);  //会话秘钥
        String apdulength=FCharUtils.int2HexStr2(bipEntity.getInstruct().length()/2);
        String APDU=Bip.getAdpuInstruct(Constants.nxyInstructHead+apdulength+bipEntity.getInstruct(),apdukey); //adpu
        TMKeyLog.i(TAG,"指令长度====>:"+apdulength);
        TMKeyLog.i(TAG,"加密后apdu:"+APDU);
        String head="";
        if(!bipEntity.getYangzhengma().equals("")){
            head="92"+count;
            TMKeyLog.i(TAG,"验证码不为空，head 为:"+head);
        }else {
            head="12"+count;
            TMKeyLog.i(TAG,"验证码为空，head 为:"+head);
        }
        String apduinstruct=Bip.getAdpuInstructMacForLast(head,apdukey,APDU);
        TMKeyLog.i(TAG,"原来发往服务器apdu指令码:"+apduinstruct);

        httpUtil.doMySocket(apduinstruct, new ResultInterf() {
            @Override
            public void onResult(Object object) {
                lastCount=count;
                String objectstring=(String)object;
                String returnCOde=((String) object).substring(4,6);
                TMKeyLog.i(TAG," Apdu指令透传结果====>:"+object);
                if(returnCOde.equals("13")||returnCOde.equals("93")){ //如果数据返回异常，异常处理
                    String errorCode=((String) object).substring(6,14);
                    TMKeyLog.i(TAG,"需要重新绑定，错误代码:"+errorCode);
                    if(errorCode.equals("0A000005") ||errorCode.equals("0A00000D")||errorCode.equals("45525231")||errorCode.equals("45525232")){ //返回错误码 需要重新绑定
                        onBind();  }
                }else {
                arrayList=parseBipData(objectstring,apdukey);
                if(macResult==true){  //每条数据如果mac是对的话，返回数据给上层
                   myBipResult.isSuccess=true;
                   myBipResult.message="数据获取成功";
                   myBipResult.arrayList=arrayList;
                   myBipResultList.add(myBipResult);
                    resultobservable.subscribe(resultobserver);
                }else {
                    resultobserver.onError(new Throwable());               }
            }}
        },carNO);
    }

    @Override
    public void onDestroy() {
        lastflag=1;
        apdukey="";
        hasBindCard=false;
        firstConnectArray=null;
        arrayList.clear();
    }




     public String getcount(){
       String count;
       count=FCharUtils.int2HexStr2(lastflag);
       TMKeyLog.i(TAG,"bip请求count值:"+count);
       lastflag++;
       return count;
    }


    public  ArrayList<String>   parseBipData(String Data,String Key){  //多包解析数据
        TMKeyLog.i(TAG,"数据data："+Data);


        ArrayList<String>  arrayList=new ArrayList<>();
        int apduLength=0;
        String apduInstruct="";
        int lastLength=0;
        for(int i=0;i<10;i++){   //数据最多十包
            apduLength=FCharUtils.hexStr2Len(Data.substring(lastLength+2,lastLength+4))*2;
            apduInstruct=Data.substring(lastLength,lastLength+4+apduLength);
            lastLength=lastLength+apduInstruct.length();
            TMKeyLog.i(TAG," 第"+(i+1)+"条数据长度："+apduLength);
            TMKeyLog.i(TAG," 第"+(i+1)+"条数据："+apduInstruct);
            TMKeyLog.i(TAG," lastlength："+lastLength);
            Boolean macResulta=Bip.jiaoyangmac(apduInstruct.substring(4,apduInstruct.length()-8),Key,apduInstruct.substring(apduInstruct.length()-8));
            TMKeyLog.i(TAG,"第"+(i+1)+"条数据mac比对结果："+macResulta);
            if(macResulta==true){
                String result=Bip.jiemiResult(apduInstruct.substring(10,apduInstruct.length()-8),Key);
                arrayList.add(result);
                hasCard=true;
                macResult=true;
                TMKeyLog.i(TAG,"第"+(i+1)+"条数据："+result);
                modifyYangZhengma(result);
            }else {
                hasCard=false;
                macResult=false;
                break;
            }
            if(lastLength==Data.length()){
                TMKeyLog.i(TAG," 数据总长度："+Data.length());
                break;
            }
        }

        return  arrayList;

    }


  public void   modifyYangZhengma(String result){
       String error=result.substring(0,8);
       if(error.equals("595A4D31")){
           macResult=false;
           errorCode="595A4D31";
           errorMessage="交易验证码用户取消";
       }
      if(error.equals("595A4D32")){
          macResult=false;
          errorCode="595A4D32";
          errorMessage="交易验证码验证失败";
      }

  }

    public  String  getErrorCode(String code) {
       error="";
        Map map = Constants.errorMap;
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            if (key.equals(code)) {
                errorCode= (String) key;
                error = (String) val;
                errorMessage=error;
                TMKeyLog.d(TAG," bip错误码："+key);
                TMKeyLog.d(TAG," bip错误信息："+val);
            }
        }
       return  error;
    }


}
