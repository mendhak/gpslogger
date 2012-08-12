package com.mendhak.gpslogger.shortcuts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.common.Utilities;

public class ShortcutStart extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Utilities.LogInfo("Shortcut - start logging");
        Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
        serviceIntent.putExtra("immediate", true);
        getApplicationContext().startService(serviceIntent);

        finish();

    }
}