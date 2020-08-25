package com.froad.ukey.http;

import com.froad.ukey.utils.np.TMKeyLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class HttpsClient {

    private final static String TAG = "HttpsClient";

    public HttpsClient.Call newCall(HttpsClient.RequestInfo requestInfo) {
        HttpsClient.Call call = new HttpsClient.Call(requestInfo);
        return call;
    }

    public static class Call implements Runnable {
        private HttpsClient.RequestInfo requestInfo;
        private Thread thread;
        private HttpsClient.Callback callback;

        public Call(HttpsClient.RequestInfo requestInfo) {
            this.requestInfo = requestInfo;
        }

        public HttpsClient.Call enqueue(HttpsClient.Callback callback) {
            this.callback = callback;
            this.thread = new Thread(this);
            this.thread.start();
            return this;
        }

        private void setHeaders(HttpURLConnection con, Map<String, String> headers) {
            Iterator iterator = headers.entrySet().iterator();

            while(iterator.hasNext()) {
                Entry<String, String> entry = (Entry)iterator.next();
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }

        }

        public void run() {
            HttpsClient.RequestInfo requestInfo = this.requestInfo;
            HttpURLConnection con = null;
            Exception buildException;
            if ((buildException = requestInfo.getBuildException()) != null) {
                this.callback.onFailure(buildException);
            } else {
                try {
                    URL url = requestInfo.getURL();
                    byte[] body = requestInfo.getBody();
                    con = (HttpURLConnection)url.openConnection();
                    this.setHeaders(con, requestInfo.getHeaders());
                    con.setRequestMethod("POST");
                    con.setConnectTimeout(requestInfo.getConTimeout());
                    con.setReadTimeout(requestInfo.getReadTimeout());
                    con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                    con.setRequestProperty("Accept", "application/json");
                    con.setDoOutput(true);
                    OutputStream out = con.getOutputStream();
                    out.write(body);
                    this.writeResp(con);
                } catch (Throwable t) {
                    t.printStackTrace();
                    this.callback.onFailure(t);
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }

                }

            }
        }

        public void writeResp(HttpURLConnection con) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer sb = new StringBuffer();
                char[] cs = new char[512];

                int readedNumber;
                while((readedNumber = br.read(cs)) != -1) {
                    sb.append(new String(cs, 0, readedNumber));
                }
                String res = sb.toString();
                TMKeyLog.d(TAG, "writeResp>>>res:" + res);

                this.callback.onResponse(res);
                br.close();
            } catch (IOException e) {
                this.callback.onFailure(e);
            }

        }
    }

    public static class RequestInfo {
        private String urlStr;
        private URL url;
        private Map<String, String> headers;
        private HttpsClient.RequestBody body;
        private Exception ex;
        private int conTimeout;
        private int readTimeout;

        public Exception getBuildException() {
            return this.ex;
        }

        public int getConTimeout() {
            return this.conTimeout;
        }

        public int getReadTimeout() {
            return this.readTimeout;
        }

        public RequestInfo(String urlStr, HttpsClient.RequestBody body) {
            this.urlStr = urlStr;
            this.body = body;
            this.headers = new HashMap();
            this.ex = null;
            this.conTimeout = HttpUtil.getOptions().getConnectionTimeoutInMillis();
            this.readTimeout = HttpUtil.getOptions().getSocketTimeoutInMillis();
        }

        public Map<String, String> getHeaders() {
            return this.headers;
        }

        public void setHeader(String key, String value) {
            this.headers.put(key, value);
        }

        public void build() {
            try {
                this.url = new URL(this.urlStr);
            } catch (Exception e) {
                this.ex = e;
            }

        }

        public URL getURL() {
            return this.url;
        }

        public byte[] getBody() {
            return this.body.getBytes();
        }
    }

    public static class RequestBody {
        private StringBuffer stringBuffer = new StringBuffer();
        private static String UTF8 = "UTF-8";

        public RequestBody() {
        }

        public void setBody(String body) {
            this.stringBuffer.append(body);
        }

        public void setStrParams(Map<String, String> params) {
            if (params != null && !params.isEmpty()) {
                JSONObject jsonObject = new JSONObject();
                Iterator iterator = params.entrySet().iterator();

                while(iterator.hasNext()) {
                    Entry<String, String> entry = (Entry)iterator.next();

                    String key = entry.getKey();
                    String value = entry.getValue();
                    try {
                        jsonObject.put(key, value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                this.stringBuffer.append(jsonObject.toString());
            }
        }

        public byte[] getBytes() {
            byte[] bytes = new byte[0];

            try {
                String bodyStr = stringBuffer.toString();
                TMKeyLog.d(TAG, "getBytes>>>bodyStr:" + bodyStr);
                bytes = bodyStr.getBytes("UTF-8");
                return bytes;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return bytes;
            }
        }
    }

    public interface Callback {
        void onFailure(Throwable throwable);

        void onResponse(String str);
    }
}
