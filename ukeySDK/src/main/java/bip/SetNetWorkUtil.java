package bip;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import com.froad.ukey.utils.np.TMKeyLog;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

/*
 *Created by chenji on 2020/5/12
 */ public class SetNetWorkUtil {
     ConnectivityManager.NetworkCallback callback;
     @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public  void forceSendRequestByMobileData(Context context) {
         final ConnectivityManager connectivityManager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
         NetworkRequest.Builder builder = new NetworkRequest.Builder();
         builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
         builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);


        // 设置指定的网络传输类型(蜂窝传输) 等于手机网络


        NetworkRequest request = builder.build();
        callback = new ConnectivityManager.NetworkCallback() {
            /**
             * Called when the framework connects and has declared a new network ready for use.
             * This callback may be called more than once if the {@link Network} that is
             * satisfying the request changes.
             *
             */
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);

                // 可以通过下面代码将app接下来的请求都绑定到这个网络下请求
                if (Build.VERSION.SDK_INT >= 23) {
                    connectivityManager.bindProcessToNetwork(network);
                } else {
                    // 23后这个方法舍弃了
                    ConnectivityManager.setProcessDefaultNetwork(network);
                }

               if(callback!=null){
                connectivityManager.unregisterNetworkCallback(callback);}

            };
        };


        if(callback!=null){
            connectivityManager.registerNetworkCallback(request, callback);
            connectivityManager.requestNetwork(request, callback);}else{
            return;
        }
    }



}
