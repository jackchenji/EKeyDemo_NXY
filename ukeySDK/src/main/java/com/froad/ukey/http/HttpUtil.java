package com.froad.ukey.http;

import android.os.Handler;
import android.os.Looper;

import com.froad.ukey.http.HttpsClient.Callback;
import com.froad.ukey.http.HttpsClient.RequestBody;
import com.froad.ukey.http.HttpsClient.RequestInfo;
import com.froad.ukey.http.exception.AuthError;
import com.froad.ukey.http.exception.SDKError;
import com.froad.ukey.http.interf.OnResultListener;
import com.froad.ukey.http.interf.Parser;
import com.froad.ukey.http.interf.RequestParams;
import com.froad.ukey.utils.np.TMKeyLog;

public class HttpUtil {

    private final static String TAG = "HttpUtil";
    private Handler handler;
    private static volatile HttpUtil instance;
    private static HttpUtil.Options options = new HttpUtil.Options();

    public static void setOptions(HttpUtil.Options options) {
        options = options;
    }

    public static HttpUtil.Options getOptions() {
        return options;
    }

    public static HttpUtil getInstance() {
        if (instance == null) {
            synchronized(HttpUtil.class) {
                instance = new HttpUtil();
            }
        }
        instance.init();
        return instance;
    }

    public void init() {
        this.handler = new Handler(Looper.getMainLooper());
    }

    public <T> void post(String path, RequestParams params, final Parser<T> parser, final OnResultListener<T> listener) {
        HttpsClient cl = new HttpsClient();
        RequestBody body = new RequestBody();
        body.setStrParams(params.getStringParams());
        RequestInfo reqInfo = new RequestInfo(path, body);
        reqInfo.build();
        cl.newCall(reqInfo).enqueue(new Callback() {
            public void onFailure(final Throwable e) {
                TMKeyLog.d(TAG, "onFailure>>>e:" + e.getMessage());
                handler.post(new Runnable() {
                    public void run() {
                        HttpUtil.throwSDKError(listener, AuthError.ErrorCode.SERVICE_NET_ERROR, "Network error", e);
                    }
                });
            }

            public void onResponse(String resultStr) {
                TMKeyLog.d(TAG, "onResponse>>>resultStr:" + resultStr);
                String responseString = resultStr;

                try {
                    final T result = parser.parse(responseString);
                    handler.post(new Runnable() {
                        public void run() {
                            listener.onResult(result);
                        }
                    });
                } catch (final AuthError authError) {
                    handler.post(new Runnable() {
                        public void run() {
                            listener.onError(authError);
                        }
                    });
                }

            }
        });
    }

    private static void throwSDKError(OnResultListener listener, String errorCode, String msg) {
        SDKError error = new SDKError(errorCode, msg);
        listener.onError(error);
    }

    private static void throwSDKError(OnResultListener listener, String errorCode, String msg, Throwable cause) {
        SDKError error = new SDKError(errorCode, msg, cause);
        listener.onError(error);
    }

    public void release() {
        this.handler = null;
    }

    public static class Options {
        private int connectionTimeoutInMillis = 10000;
        private int socketTimeoutInMillis = 10000;

        public Options() {
        }

        public int getConnectionTimeoutInMillis() {
            return this.connectionTimeoutInMillis;
        }

        public void setConnectionTimeoutInMillis(int connectionTimeoutInMillis) {
            this.connectionTimeoutInMillis = connectionTimeoutInMillis;
        }

        public int getSocketTimeoutInMillis() {
            return this.socketTimeoutInMillis;
        }

        public void setSocketTimeoutInMillis(int socketTimeoutInMillis) {
            this.socketTimeoutInMillis = socketTimeoutInMillis;
        }
    }
}
