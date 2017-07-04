/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger;

import android.location.*;
import android.os.Bundle;
import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.nmea.NmeaSentence;
import org.slf4j.Logger;

import java.util.Iterator;

class GeneralLocationListener implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener {

    private static String listenerName;
    private static GpsLoggingService loggingService;
    private static final Logger LOG = Logs.of(GeneralLocationListener.class);
    protected String latestHdop;
    protected String latestPdop;
    protected String latestVdop;
    protected String geoIdHeight;
    protected String ageOfDgpsData;
    protected String dgpsId;
    protected int satellitesUsedInFix;
    private Session session = Session.getInstance();

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
                b.putString(BundleConstants.HDOP, this.latestHdop);
                b.putString(BundleConstants.PDOP, this.latestPdop);
                b.putString(BundleConstants.VDOP, this.latestVdop);
                b.putString(BundleConstants.GEOIDHEIGHT, this.geoIdHeight);
                b.putString(BundleConstants.AGEOFDGPSDATA, this.ageOfDgpsData);
                b.putString(BundleConstants.DGPSID, this.dgpsId);

                b.putBoolean(BundleConstants.PASSIVE, listenerName.equalsIgnoreCase(BundleConstants.PASSIVE));
                b.putString(BundleConstants.LISTENER, listenerName);
                b.putInt(BundleConstants.SATELLITES_FIX, satellitesUsedInFix);
                b.putString(BundleConstants.DETECTED_ACTIVITY, session.getLatestDetectedActivityName());

                loc.setExtras(b);
                loggingService.onLocationChanged(loc);

                this.latestHdop = "";
                this.latestPdop = "";
                this.latestVdop = "";
                session.setLatestDetectedActivity(null);
            }

        } catch (Exception ex) {
            LOG.error("GeneralLocationListener.onLocationChanged", ex);
        }

    }

    public void onProviderDisabled(String provider) {
        LOG.info("Provider disabled: " + provider);
        loggingService.restartGpsManagers();
    }

    public void onProviderEnabled(String provider) {

        LOG.info("Provider enabled: " + provider);
        loggingService.restartGpsManagers();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (status == LocationProvider.OUT_OF_SERVICE) {
            LOG.info(provider + " is out of service");
            loggingService.stopManagerAndResetAlarm();
        }

        if (status == LocationProvider.AVAILABLE) {
            LOG.info(provider + " is available");
        }

        if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
            LOG.info(provider + " is temporarily unavailable");
            loggingService.stopManagerAndResetAlarm();
        }
    }

    public void onGpsStatusChanged(int event) {

        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                LOG.debug(loggingService.getString(R.string.fix_obtained));
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

                GpsStatus status = loggingService.gpsLocationManager.getGpsStatus(null);

                int maxSatellites = status.getMaxSatellites();

                Iterator<GpsSatellite> it = status.getSatellites().iterator();
                int satellitesVisible = 0;
                satellitesUsedInFix=0;

                while (it.hasNext() && satellitesVisible <= maxSatellites) {
                    GpsSatellite sat = it.next();
                    if(sat.usedInFix()){
                        satellitesUsedInFix++;
                    }
                    satellitesVisible++;
                }

                LOG.debug(String.valueOf(satellitesVisible) + " satellites");
                loggingService.setSatelliteInfo(satellitesVisible);
                break;

            case GpsStatus.GPS_EVENT_STARTED:
                LOG.info(loggingService.getString(R.string.started_waiting));
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                LOG.info(loggingService.getString(R.string.gps_stopped));
                break;

        }
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmeaSentence) {
        loggingService.onNmeaSentence(timestamp, nmeaSentence);

        if(Strings.isNullOrEmpty(nmeaSentence)){
            return;
        }

        NmeaSentence nmea = new NmeaSentence(nmeaSentence);

        if(nmea.isLocationSentence()){
            if(nmea.getLatestPdop() != null){
                this.latestPdop = nmea.getLatestPdop();
            }

            if(nmea.getLatestHdop() != null){
                this.latestHdop = nmea.getLatestHdop();
            }

            if(nmea.getLatestVdop() != null){
                this.latestVdop = nmea.getLatestVdop();
            }

            if(nmea.getGeoIdHeight() != null){
                this.geoIdHeight = nmea.getGeoIdHeight();
            }

            if(nmea.getAgeOfDgpsData() != null){
                this.ageOfDgpsData = nmea.getAgeOfDgpsData();
            }

            if(nmea.getDgpsId() != null){
                this.dgpsId = nmea.getDgpsId();
            }

        }

    }
}
