package com.mendhak.gpslogger.common;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
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
        }, getNetworkIntentFilter());
    }

    @Override
    public boolean isConnected(Context context) {

        if (isDozing(context)) {
            return false;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        boolean isWifiRequired = PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly();
        boolean isDeviceOnWifi = true;
        if(isWifiRequired && netInfo != null){
            isDeviceOnWifi = (netInfo.getType() == ConnectivityManager.TYPE_WIFI);
        }

        return netInfo != null && netInfo.isConnectedOrConnecting() && isDeviceOnWifi;
    }

    @TargetApi(23)
    private static IntentFilter getNetworkIntentFilter() {
        IntentFilter networkIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= 23) {
            networkIntentFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }
        return networkIntentFilter;
    }

    /**
     * Returns true if the device is in Doze/Idle mode. Should be called before checking the network connection because
     * the ConnectionManager may report the device is connected when it isn't during Idle mode.
     * https://github.com/yigit/android-priority-jobqueue/blob/master/jobqueue/src/main/java/com/path/android/jobqueue/network/NetworkUtilImpl.java#L60
     */
    @TargetApi(23)
    public static boolean isDozing(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager.isDeviceIdleMode() &&
                    !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }
}