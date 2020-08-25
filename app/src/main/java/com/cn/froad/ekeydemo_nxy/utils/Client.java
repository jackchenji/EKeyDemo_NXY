package com.cn.froad.ekeydemo_nxy.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Client extends Thread {

    //定义一个Socket对象
    Socket socket = null;

    public Client(String host, int port) {
        try {
            //需要服务器的IP地址和端口号，才能获得正确的Socket对象
            socket = new Socket(host, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void run() {
        //客户端一连接就可以写数据给服务器了
        new sendMessThread().start();
        super.run();
        try {
            // 读Sock里面的数据
            InputStream s = socket.getInputStream();
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = s.read(buf)) != -1) {
                int i = 0;
                byte[] data = new byte[len];
                System.arraycopy(buf, 0, data, 0, len);
                System.out.println("服务器说：" + StringUtil.byte2HexStr(data));
                System.out.println("服务器计数器：" + i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //往Socket里面写数据，需要新开一个线程
    class sendMessThread extends Thread {
        @Override
        public void run() {
            super.run();
            //写操作
            OutputStream os = null;
            try {
                os = socket.getOutputStream();
                String in = "00028410";
                do {
                    os.write(("" + in).getBytes());
                    os.flush();
                } while (!in.equals("bye"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getdate() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String result = format.format(date);
        return result;
    }

    //函数入口
    public static void main(String[] args) {
        //需要服务器的正确的IP地址和端口号
        Client clientTest = new Client("127.0.0.1", 1234);
        clientTest.start();
    }


    public void doSocket(String instruct,ResultInterf interf) {
        try {
            String result="";
            // 与服务端建立连接
            if(socket==null){
                socket = new Socket(Constants.waiwangip, Constants.waiwnangport);}
            // 建立连接后获得输出流
            OutputStream outputStream = socket.getOutputStream();
            socket.getOutputStream().write(StringUtil.hexString2ByteArray(instruct));
            //通过shutdownOutput高速服务器已经发送完数据，后续只能接受数据
            InputStream inputStream = socket.getInputStream();
            byte[] bytes = new byte[1024];
            int len;
            StringBuilder sb = new StringBuilder();
            while ((len = inputStream.read(bytes)) != -1) {
                byte[] data = new byte[len];
                System.arraycopy(bytes, 0, data, 0, len);
                result=StringUtil.byte2HexStr(data);
                interf.onResult(result);
            }
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (Exception E) {

        }
}


}


