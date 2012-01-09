package com.mendhak.gpslogger.senders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.mendhak.gpslogger.common.Utilities;


public class AlarmReceiver extends BroadcastReceiver
{


    @Override
    public void onReceive(Context context, Intent intent)
    {
        try
        {
            Utilities.LogInfo("Email alarm received");
            Intent serviceIntent = new Intent(context.getPackageName() + ".GpsLoggingService");
            serviceIntent.putExtra("emailAlarm", true);
            // Start the service in case it isn't already running
            context.startService(serviceIntent);
        }
        catch (Exception ex)
        {

        }


    }
}
