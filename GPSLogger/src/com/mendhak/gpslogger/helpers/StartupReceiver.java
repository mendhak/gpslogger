package com.mendhak.gpslogger.helpers;

import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class StartupReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			boolean startImmediately = prefs.getBoolean("startonbootup", false);

			Utilities.LogInfo("Did the user ask for start on bootup? - "
					+ String.valueOf(startImmediately));

			if (startImmediately)
			{
				Utilities.LogInfo("Launching GPSLoggingService");
				Intent serviceIntent = new Intent(context, GpsLoggingService.class);
				serviceIntent.putExtra("immediate", true);
				context.startService(serviceIntent);
			}
		}
		catch (Exception ex)
		{
			Utilities.LogError("StartupReceiver", ex);

		}

	}

}
