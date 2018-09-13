package com.mendhak.gpslogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

public class TaskerReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(TaskerReceiver.class);
    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.info("---------------------WORKED-------------------");

        Intent serviceIntent = new Intent(context, GpsLoggingService.class);
        serviceIntent.putExtra(IntentConstants.IMMEDIATE_START, true);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
