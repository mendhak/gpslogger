package com.mendhak.gpslogger.common;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.path.android.jobqueue.network.NetworkEventProvider;
import com.path.android.jobqueue.network.NetworkUtil;

/**
 * default implementation for network Utility to observe network events
 */
public class WifiNetworkUtil implements NetworkUtil, NetworkEventProvider {
    private NetworkEventProvider.Listener listener;
    public WifiNetworkUtil(Context context) {
        context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(listener == null) {//shall not be but just be safe
                    return;
                }
                //http://developer.android.com/reference/android/net/ConnectivityManager.html#EXTRA_NETWORK_INFO
                //Since NetworkInfo can vary based on UID, applications should always obtain network information
                // through getActiveNetworkInfo() or getAllNetworkInfo().
                listener.onNetworkChange(isConnected(context));
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        boolean isWifiRequired = PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly();
        boolean isWifi = true;
        if(isWifiRequired && netInfo != null){
            isWifi = (netInfo.getType() == ConnectivityManager.TYPE_WIFI);
        }


        return netInfo != null && netInfo.isConnectedOrConnecting() && isWifi;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }
}