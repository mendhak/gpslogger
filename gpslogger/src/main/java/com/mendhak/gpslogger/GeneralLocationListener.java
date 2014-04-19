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

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

class GeneralLocationListener implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener
{

    private static GpsLoggingService loggingService;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GeneralLocationListener.class.getSimpleName());
    private String latestHdop;
    private String latestPdop;
    private String latestVdop;
    private String geoIdHeight;
    private String ageOfDgpsData;
    private String dgpsId;

    GeneralLocationListener(GpsLoggingService activity)
    {
        tracer.debug("GeneralLocationListener constructor");
        loggingService = activity;
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
                tracer.debug("GeneralLocationListener.onLocationChanged");
                Bundle b = new Bundle();
                b.putString("HDOP", this.latestHdop);
                b.putString("PDOP", this.latestPdop);
                b.putString("VDOP", this.latestVdop);
                b.putString("GEOIDHEIGHT", this.geoIdHeight);
                b.putString("AGEOFDGPSDATA", this.ageOfDgpsData);
                b.putString("DGPSID", this.dgpsId);
                loc.setExtras(b);
                loggingService.OnLocationChanged(loc);

                this.latestHdop = "";
                this.latestPdop="";
                this.latestVdop="";
            }

        }
        catch (Exception ex)
        {
            tracer.error("GeneralLocationListener.onLocationChanged", ex);
            loggingService.SetStatus(ex.getMessage());
        }

    }

    public void onProviderDisabled(String provider)
    {
        tracer.info("Provider disabled: " + provider);
        loggingService.RestartGpsManagers();
    }

    public void onProviderEnabled(String provider)
    {

        tracer.info("Provider enabled: " + provider);
        loggingService.RestartGpsManagers();
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        if (status == LocationProvider.OUT_OF_SERVICE)
        {
            tracer.debug(provider + " is out of service");
            loggingService.StopManagerAndResetAlarm();
        }

        if (status == LocationProvider.AVAILABLE)
        {
            tracer.debug(provider + " is available");
        }

        if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
        {
            tracer.debug(provider + " is temporarily unavailable");
            loggingService.StopManagerAndResetAlarm();
        }
    }

    public void onGpsStatusChanged(int event)
    {

        switch (event)
        {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                tracer.debug("GPS Event First Fix");
                loggingService.SetStatus(loggingService.getString(R.string.fix_obtained));
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

                GpsStatus status = loggingService.gpsLocationManager.getGpsStatus(null);

                int maxSatellites = status.getMaxSatellites();

                Iterator<GpsSatellite> it = status.getSatellites().iterator();
                int count = 0;

                while (it.hasNext() && count <= maxSatellites)
                {
                    it.next();
                    count++;
                }

                tracer.debug(String.valueOf(count) + " satellites");
                loggingService.SetSatelliteInfo(count);
                break;

            case GpsStatus.GPS_EVENT_STARTED:
                tracer.info("GPS started, waiting for fix");
                loggingService.SetStatus(loggingService.getString(R.string.started_waiting));
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                tracer.info("GPS Event Stopped");
                loggingService.SetStatus(loggingService.getString(R.string.gps_stopped));
                break;

        }
    }

    @Override
    public void onNmeaReceived(long l, String nmeaSentence) {
        String[] nmeaParts = nmeaSentence.split(",");

        if(nmeaParts[0].equalsIgnoreCase("$GPGSA")){

            if(!Utilities.IsNullOrEmpty(nmeaParts[15])){
                this.latestPdop = nmeaParts[15];
            }

            if(!Utilities.IsNullOrEmpty(nmeaParts[16])){
                this.latestHdop = nmeaParts[16];
            }

            if(!Utilities.IsNullOrEmpty(nmeaParts[17]) && !nmeaParts[17].startsWith("*")){

                this.latestVdop = nmeaParts[17].split("\\*")[0];
            }

        }


        //height of geoid nmeaparts 11
        //time since last update 13
        // station id 14
        if(nmeaParts[0].equalsIgnoreCase("$GPGGA")){
            tracer.info(nmeaSentence);
            if(!Utilities.IsNullOrEmpty(nmeaParts[8])){
                this.latestHdop = nmeaParts[8];
            }

            if(!Utilities.IsNullOrEmpty(nmeaParts[11])){
                this.geoIdHeight = nmeaParts[11];
            }

            if(!Utilities.IsNullOrEmpty(nmeaParts[13])){
                this.ageOfDgpsData = nmeaParts[13];
            }

            if(!Utilities.IsNullOrEmpty(nmeaParts[14]) && !nmeaParts[14].startsWith("*")){
                this.dgpsId = nmeaParts[14].split("\\*")[0];
            }

        }

    }
}
