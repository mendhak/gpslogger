package com.mendhak.gpslogger.helpers;

import com.mendhak.gpslogger.GpsMainActivity;
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
				Utilities.LogInfo("Launching GPSMainActivity");
				Intent startActivity = new Intent(context, GpsMainActivity.class);
				startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity.putExtra("immediate", true);
				context.startActivity(startActivity);
			}
		}
		catch (Exception ex)
		{
			Utilities.LogError("StartupReceiver", ex);

		}

	}

}
