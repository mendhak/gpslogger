/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

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

        if (Systems.isDozing(context)) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkIntentFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }
        return networkIntentFilter;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }
}