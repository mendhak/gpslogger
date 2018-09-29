package com.mendhak.gpslogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

public class RestarterReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(RestarterReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.warn("GPSLogger service is being killed, broadcast received. Attempting to restart");
        boolean wasRunning = intent.getBooleanExtra("was_running", false);
        LOG.info("was_running:" + wasRunning);

        Intent serviceIntent = new Intent(context, GpsLoggingService.class);

        if(wasRunning){
            serviceIntent.putExtra(IntentConstants.IMMEDIATE_START, true);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
        else {
            serviceIntent.putExtra(IntentConstants.IMMEDIATE_STOP, true);
            ContextCompat.startForegroundService(context, serviceIntent);
        }

    }
}
