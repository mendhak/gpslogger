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
import com.mendhak.gpslogger.loggers.nmea.NmeaSentence;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

class GeneralLocationListener implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener {

    private static String listenerName;
    private static GpsLoggingService loggingService;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GeneralLocationListener.class.getSimpleName());
    protected String latestHdop;
    protected String latestPdop;
    protected String latestVdop;
    protected String geoIdHeight;
    protected String ageOfDgpsData;
    protected String dgpsId;

    GeneralLocationListener(GpsLoggingService activity, String name) {
        loggingService = activity;
        listenerName = name;
    }

    /**
     * Event raised when a new fix is received.
     */
    public void onLocationChanged(Location loc) {

        try {
            if (loc != null) {
                Bundle b = new Bundle();
                b.putString("HDOP", this.latestHdop);
                b.putString("PDOP", this.latestPdop);
                b.putString("VDOP", this.latestVdop);
                b.putString("GEOIDHEIGHT", this.geoIdHeight);
                b.putString("AGEOFDGPSDATA", this.ageOfDgpsData);
                b.putString("DGPSID", this.dgpsId);

                b.putBoolean("PASSIVE", listenerName.equalsIgnoreCase("PASSIVE"));
                b.putString("LISTENER", listenerName);

                loc.setExtras(b);
                loggingService.onLocationChanged(loc);

                this.latestHdop = "";
                this.latestPdop = "";
                this.latestVdop = "";
            }

        } catch (Exception ex) {
            tracer.error("GeneralLocationListener.onLocationChanged", ex);
        }

    }

    public void onProviderDisabled(String provider) {
        tracer.info("Provider disabled: " + provider);
        loggingService.restartGpsManagers();
    }

    public void onProviderEnabled(String provider) {

        tracer.info("Provider enabled: " + provider);
        loggingService.restartGpsManagers();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (status == LocationProvider.OUT_OF_SERVICE) {
            tracer.info(provider + " is out of service");
            loggingService.stopManagerAndResetAlarm();
        }

        if (status == LocationProvider.AVAILABLE) {
            tracer.info(provider + " is available");
        }

        if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
            tracer.info(provider + " is temporarily unavailable");
            loggingService.stopManagerAndResetAlarm();
        }
    }

    public void onGpsStatusChanged(int event) {

        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                tracer.debug(loggingService.getString(R.string.fix_obtained));
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

                GpsStatus status = loggingService.gpsLocationManager.getGpsStatus(null);

                int maxSatellites = status.getMaxSatellites();

                Iterator<GpsSatellite> it = status.getSatellites().iterator();
                int count = 0;

                while (it.hasNext() && count <= maxSatellites) {
                    it.next();
                    count++;
                }

                tracer.debug(String.valueOf(count) + " satellites");
                loggingService.setSatelliteInfo(count);
                break;

            case GpsStatus.GPS_EVENT_STARTED:
                tracer.info(loggingService.getString(R.string.started_waiting));
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                tracer.info(loggingService.getString(R.string.gps_stopped));
                break;

        }
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmeaSentence) {
        loggingService.onNmeaSentence(timestamp, nmeaSentence);

        if(Utilities.IsNullOrEmpty(nmeaSentence)){
            return;
        }

        NmeaSentence nmea = new NmeaSentence(nmeaSentence);

        this.latestPdop = nmea.getLatestPdop();
        this.latestHdop = nmea.getLatestHdop();
        this.latestVdop = nmea.getLatestVdop();
        this.geoIdHeight = nmea.getGeoIdHeight();
        this.ageOfDgpsData = nmea.getAgeOfDgpsData();
        this.dgpsId = nmea.getDgpsId();

    }
}
