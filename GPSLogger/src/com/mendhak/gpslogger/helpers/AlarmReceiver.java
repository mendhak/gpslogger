package com.mendhak.gpslogger.helpers;

import com.mendhak.gpslogger.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class AlarmReceiver extends BroadcastReceiver
{


	@Override
	public void onReceive(Context context, Intent intent)
	{
		try
		{
			Utilities.LogInfo("Email alarm received");
			Intent serviceIntent = new Intent("com.mendhak.gpslogger.GpsLoggingService");
			serviceIntent.putExtra("alarmWentOff", true);
			// Start the service in case it isn't already running
			context.startService(serviceIntent);
		}
		catch(Exception ex)
		{
			String b = ex.getMessage();
		}
		

	}
}
