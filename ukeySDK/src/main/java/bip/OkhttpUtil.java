package bip;

import com.froad.ukey.utils.np.TMKeyLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/*
 *Created by chenji on 2020/5/28
 */ public class OkhttpUtil  extends Thread {
    private  final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private  OkHttpClient client = new OkHttpClient();


     public OkhttpUtil() {
    }

    public  Response post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        OkHttpClient client =new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();
        Response response = client.newCall(request).execute();   //设置读取超时和连接超时
        return response;
    }

    public  Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }





    public void doMySocket(String instruct,ResultInterf interf,String cardNo) {
        try {
            JSONObject  jsonObject=new JSONObject();
            jsonObject.put("randId",cardNo);
            jsonObject.put("cmd",instruct);
            Response response=post(Constants.httpsUrl,jsonObject.toString());
            String temp = response.body().string();
            JSONObject a=new JSONObject(temp);
            String data=a.getString("data");
            TMKeyLog.d("指令：",instruct);
            TMKeyLog.d("randId：",cardNo);
            TMKeyLog.d("返回数据：",temp);
            TMKeyLog.d("网络请求地址：",Constants.httpsUrl);
            TMKeyLog.d("请求数据：",jsonObject.toString());
            TMKeyLog.d("data数据：",a.getString("data"));
            interf.onResult(data);



        } catch (Exception E) {
            BipManager.getInstance().resultobserver.onError(new Throwable());//如果出现网络异常那么通知
            TMKeyLog.d("client","client 异常信息"+E.getMessage());

        }
    }



}

