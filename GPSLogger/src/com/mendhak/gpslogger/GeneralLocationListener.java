/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger;

import android.location.*;
import android.os.Bundle;
import com.mendhak.gpslogger.common.Utilities;
import net.kataplop.gpslogger.R;

import java.util.Iterator;

class GeneralLocationListener implements LocationListener, GpsStatus.Listener
{

    private static GpsLoggingService mainActivity;

    GeneralLocationListener(GpsLoggingService activity)
    {
        Utilities.LogDebug("GeneralLocationListener constructor");
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
                Utilities.LogVerbose("GeneralLocationListener.onLocationChanged");
                mainActivity.OnLocationChanged(loc);
            }

        }
        catch (Exception ex)
        {
            Utilities.LogError("GeneralLocationListener.onLocationChanged", ex);
            mainActivity.SetStatus(ex.getMessage());
        }

    }

    public void onProviderDisabled(String provider)
    {
        Utilities.LogInfo("Provider disabled");
        Utilities.LogDebug(provider);
        mainActivity.RestartGpsManagers();
    }

    public void onProviderEnabled(String provider)
    {

        Utilities.LogInfo("Provider enabled");
        Utilities.LogDebug(provider);
        mainActivity.RestartGpsManagers();
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        if (status == LocationProvider.OUT_OF_SERVICE)
        {
            Utilities.LogDebug(provider + " is out of service");
            mainActivity.StopManagerAndResetAlarm();
        }

        if (status == LocationProvider.AVAILABLE)
        {
            Utilities.LogDebug(provider + " is available");
        }

        if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
        {
            Utilities.LogDebug(provider + " is temporarily unavailable");
            mainActivity.StopManagerAndResetAlarm();
        }
    }

    public void onGpsStatusChanged(int event)
    {

        switch (event)
        {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                Utilities.LogDebug("GPS Event First Fix");
                mainActivity.SetStatus(mainActivity.getString(R.string.fix_obtained));
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

                Utilities.LogDebug("GPS Satellite status obtained");
                GpsStatus status = mainActivity.gpsLocationManager.getGpsStatus(null);

                int maxSatellites = status.getMaxSatellites();

                Iterator<GpsSatellite> it = status.getSatellites().iterator();
                int count = 0;

                while (it.hasNext() && count <= maxSatellites)
                {
                    it.next();
                    count++;
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
