package com.mendhak.gpslogger.helpers;

import java.util.Iterator;

import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.Utilities;
import com.mendhak.gpslogger.model.Session;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class GeneralLocationListener implements LocationListener, GpsStatus.Listener
{

	static GpsLoggingService mainActivity;

	public GeneralLocationListener(GpsLoggingService activity)
	{
		mainActivity = activity;
	}

	/**
	 * Event raised when a new fix is received.
	 */
	public void onLocationChanged(Location loc)
	{

		Utilities.LogInfo("Location changed");
		try
		{
			if (loc != null)
			{
				mainActivity.OnLocationChanged(loc);
			}

		}
		catch (Exception ex)
		{
			mainActivity.SetStatus(ex.getMessage());
		}

	}

	public void onProviderDisabled(String provider)
	{
		Utilities.LogInfo("Provider disabled");
		mainActivity.RestartGpsManagers();
	}

	public void onProviderEnabled(String provider)
	{

		Utilities.LogInfo("Provider enabled");
		mainActivity.RestartGpsManagers();
	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		Utilities.LogInfo("Status changed");
	}

	public void onGpsStatusChanged(int event)
	{

		Utilities.LogInfo("GPS Status Changed");
		switch (event)
		{
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				Utilities.LogInfo("GPS Event First Fix");
				mainActivity.SetStatus(mainActivity.getString(R.string.fix_obtained));
				break;

			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

				Utilities.LogInfo("GPS Satellite status obtained");
				GpsStatus status = mainActivity.gpsLocationManager.getGpsStatus(null);

				int maxSatellites = status.getMaxSatellites();

				Iterator<GpsSatellite> it = status.getSatellites().iterator();
				int count = 0;
				//while (it.hasNext() && count <= maxSatellites)
				while (it.hasNext() && count <= maxSatellites)
				{
					@SuppressWarnings("unused")
					GpsSatellite s = it.next();
//					if (s.usedInFix())
//					{
						count++;
//					}
					//GpsSatellite oSat = (GpsSatellite) it.next();

					// Log.i("Main",
					// "LocationActivity - onGpsStatusChange: Satellites:"
					// + oSat.getSnr());
				}

				mainActivity.SetSatelliteInfo(count);
				break;

			case GpsStatus.GPS_EVENT_STARTED:
				Utilities.LogInfo("GPS started, waiting for fix");
				mainActivity.SetStatus(mainActivity.getString(R.string.started_waiting));
				break;

			case GpsStatus.GPS_EVENT_STOPPED:
				Utilities.LogInfo("GPS Stopped");
				mainActivity.SetStatus(mainActivity.getString(R.string.gps_stopped));
				break;

		}

	}

}
