package com.mendhak.gpslogger;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GpsLoggingService extends Service
{

	public static Activity Parent;

	@Override
	public IBinder onBind(Intent arg0)
	{

		return null;
	}

	public static void SetParent(Activity ownerActivity)
	{
		Parent = ownerActivity;
	}

	@Override
	public void onCreate()
	{
		// comment
	}

}
