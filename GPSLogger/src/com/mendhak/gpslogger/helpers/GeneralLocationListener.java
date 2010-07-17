package com.mendhak.gpslogger.helpers;

import java.util.Iterator;

import com.mendhak.gpslogger.GpsMainActivity;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class GeneralLocationListener implements LocationListener, GpsStatus.Listener
{

	static GpsMainActivity mainActivity;

	public GeneralLocationListener(GpsMainActivity activity)
	{
		mainActivity = activity;
	}

	/**
	 * Event raised when a new fix is received.
	 */
	public void onLocationChanged(Location loc)
	{

		try
		{
			if (loc != null)
			{

				mainActivity.currentLatitude = loc.getLatitude();
				mainActivity.currentLongitude = loc.getLongitude();

				mainActivity.DisplayLocationInfo(loc);
			}

		}
		catch (Exception ex)
		{
			mainActivity.SetStatus(ex.getMessage());
		}

	}

	public void onProviderDisabled(String provider)
	{

		mainActivity.RestartGpsManagers();
	}

	public void onProviderEnabled(String provider)
	{

		mainActivity.RestartGpsManagers();
	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{

	}

	public void onGpsStatusChanged(int event)
	{

		switch (event)
		{
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				mainActivity.SetStatus("Fix obtained");
				break;

			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

				GpsStatus status = mainActivity.gpsLocationManager.getGpsStatus(null);

				Iterator<GpsSatellite> it = status.getSatellites().iterator();
				int count = 0;
				while (it.hasNext())
				{
					count++;
					//GpsSatellite oSat = (GpsSatellite) it.next();

					// Log.i("Main",
					// "LocationActivity - onGpsStatusChange: Satellites:"
					// + oSat.getSnr());
				}

				mainActivity.SetSatelliteInfo(count);
				break;

			case GpsStatus.GPS_EVENT_STARTED:
				mainActivity.SetStatus("GPS Started, waiting for fix");
				break;

			case GpsStatus.GPS_EVENT_STOPPED:
				mainActivity.SetStatus("GPS Stopped");
				break;

		}

	}

}
