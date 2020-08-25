package bip;

import java.util.HashMap;

/*
 *Created by chenji on 2020/2/27
 */ public class Constants {
     public  static  String ip="10.24.242.202";  //ip地址
     public  static  int port=60006;  //端口号
     public  static  String randomInstruct="00038410";  //随机数指令

     public  static  String shengchangip="211.147.70.240";  //生产ip地址
     public  static  int shengchangport=8080;  //生产端口号


     public  static  String waiwangip="180.163.110.180";  //ip地址
     public  static  int waiwnangport=26383;  //端口号


     public  static String  soKey ="5F4E58595F434552545F4D41434B4559";  //so秘钥


     public  static String  nxyInstructHead ="465543";  //农信银指令头

     public  static String  getCardSn ="B11000000C";  //获取卡号指令
     public  static String  getCos ="B236000011";  //获取cos版本

     public  static String  SDK_NAME ="424D4453444B";  //sdk name 暂时写固定


     public  static String   communicatioKey="00112233445566778899AABBCCDDEEFF";  //通讯秘钥


     public  static String   sm2PublicKey="F125F4B7591E47E2597E542C818BCD8686712EE27E672038851F93EC8AA9CCC4EA47D29EAB9FD585DC1DE10974917779C28C2BEB3F44478F8C247C4D4B4D4773";  //sm2通讯秘钥


     public  static  String httpsUrlUat="http://10.24.253.9:8412/bip/direct_send_cmd";  //https url 测试环境1
     public  static  String httpsUrlUat1="https://testfzydemo.ubank365.com/bip/direct_send_cmd";  //https url 测试环境
     public  static  String httpsUrlUat2="http://10.43.1.10:80/bip/direct_send_cmd";  //https url 测试环境
      public  static  String httpsUrlUat3="http://10.24.253.9:8309/bip/direct_send_cmd";  //http url 测试环境2
      public  static  String httpsUrlUat4="http://fzydemo2.ubank365.com/fzy-demo2-server/bip/send_cmd";  //灰度测试
      public  static  String httpsUrlUat5="https://testfzy.f-road.com.cn/fzy-demo-server/bip/direct_send_cmd";
      public  static  String httpsshengchan="http://180.163.110.180:26372/server1/api/froad-contract/contract/eid/send_cmd";  //生产环境



      public  static  String httpsUrl="http://10.24.253.9:8309/bip/direct_send_cmd";  //网络请求地址


     public  static HashMap<String,String>  errorMap=new HashMap<>();  //错误图

     static {
          errorMap.put("0A000001","失败，服务器发生系统错误");
          errorMap.put("0A000002","异常错误，服务器发生异常错误");
          errorMap.put("0A000003","不支持的服务，服务器不支持此服务");
          errorMap.put("0A000005","长时间未操作，客户端绑定会话过期");
          errorMap.put("0A00000D","对象错误/掉线，请检查SIMeID卡状态，若已打开BIP通道请稍后再试");
          errorMap.put("0A00000E","内存错误，服务器发生内存错误");
          errorMap.put("0A00000F","超时，服务器转发数据超时");
          errorMap.put("0A000010","输入数据长度错误，服务器检测数据长度错误");
          errorMap.put("0A000011","输入数据错误，服务器检测数据错误");
          errorMap.put("0A00001B","未发现手机号的匹配项，根据给定条件服务器未找到对应的卡片");
          errorMap.put("0A000024","MAC不正确，服务器校验MAC失败");
          errorMap.put("0A00002E","卡片服务不存在，请检查SIMeID卡状态或者重新打开BIP通道");
          errorMap.put("0A000031","文件不存在，服务器未检测到相关文件");
          errorMap.put("0B000001","加密机连接失败，服务器连接加密机失败");
          errorMap.put("0B000002","加密机认证失败，服务器使用加密机认证失败");
          errorMap.put("0D000001","发送短信失败，服务器发送短信失败");
          errorMap.put("0D0000FF","未发现卡号的匹配项，服务器未检测到匹配的手机号");
          errorMap.put("45525230","透传指令处理通用失败，卡片解析数据发生未知错误");
          errorMap.put("45525231","透传指令处理COUNTER错，SDK  需要Count计数器+1，并重新发送上次的透传数据，网络异常，请重新操作");
          errorMap.put("45525232","透传指令处理MAC错，SDK  需要重新发起一次注册，并自动发送上次的透传数据, 最多重复操作3次，片返回通讯数据MAC错误(3)");
          errorMap.put("45525233","透传指令处理解密失败,卡片解密数据发生错误");
          errorMap.put("45525234","透传指令处理数据格式错误,卡片解析数据Tag标识错误");
          errorMap.put("4D414345","获取通讯主密钥MAC错");
          errorMap.put("595A4D31","交易验证码用户取消,您已经取消验证码输入操作");
          errorMap.put("595A4D32","交易验证码验证失败,验证码校验失败，请重新操作,透传数据异常，操作超时，请重试");
     }


}
