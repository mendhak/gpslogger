package com.mendhak.gpslogger;

import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExternalControlReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if ("com.mendhak.gpslogger.STARTL".equals(intent.getAction())) {
			Utilities.LogInfo("Go Go Go");
			Intent serviceIntent = new Intent(context, GpsLoggingService.class);
			serviceIntent.putExtra("immediate", true);
			context.startService(serviceIntent);
		}
		if ("com.mendhak.gpslogger.STOPL".equals(intent.getAction())) {
			Utilities.LogInfo("Stop Stop Stop");
			Session.setStarted(false);
		}
	}

}
