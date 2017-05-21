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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.*;
import android.os.Bundle;

import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.nmea.NmeaSentence;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

class GeneralLocationListener implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener, SensorEventListener {

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

    // Sensor Data Extensions
    protected float[] mGravity = null;
    protected float[] mGeomagnetic = null;

    protected long nextTimestampToSave = 0;
    protected long lastTimestamp = 0;
    protected ArrayList<SensorDataObject.Accelerometer> latestAccelerometer = new ArrayList<>();
    protected ArrayList<SensorDataObject.Compass> latestCompass  = new ArrayList<>();
    protected ArrayList<SensorDataObject.Orientation> latestOrientation = new ArrayList<>();

    protected ArrayList<SensorEvent> accelerometer = new ArrayList<SensorEvent>();
    protected ArrayList<SensorEvent> magneticField = new ArrayList<SensorEvent>();

    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


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

                //Extras for Sensordatalogging
                b.putSerializable(BundleConstants.ACCELEROMETER, this.latestAccelerometer);
                b.putSerializable(BundleConstants.COMPASS, this.latestCompass);
                b.putSerializable(BundleConstants.ORIENTATION, this.latestOrientation);

                loc.setExtras(b);
                LOG.debug("general loc listener on loc changed, latest accel:"+Arrays.toString(this.latestAccelerometer.toArray())+"\n latestCompass:"+Arrays.toString(this.latestCompass.toArray())+"\n latestOrientation:"+Arrays.toString(this.latestOrientation.toArray()));
                loggingService.onLocationChanged(loc);

                this.latestHdop = "";
                this.latestPdop = "";
                this.latestVdop = "";

                this.latestAccelerometer = new ArrayList<>();
                this.latestCompass = new ArrayList<>();
                this.latestOrientation  = new ArrayList<>();
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
    @Override
    public void onSensorChanged(SensorEvent event){
        LOG.debug("onSensorChanged Event. event.sensor="+event.sensor.toString()+"\n event.values="+ Arrays.toString(event.values));

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            accelerometer.add(event);
            mGravity = event.values.clone();
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            magneticField.add(event);
            mGeomagnetic = event.values.clone();

        calculateSensors(event);
        //mGravity = null;
        //mGeomagnetic = null;
        loggingService.onSensorChanged(event);
    }

    
    /**
     * Based on code found here: https://github.com/shiptrail/android-main
     * Records sensordata to be shipped with next location
     * https://developer.android.com/guide/topics/sensors/sensors_overview.html
     * orientation is created from accel + magnetic -> billig gyro
     */
    public void calculateSensors(SensorEvent event) {
        LOG.debug("onSensorChanged Event. event.sensor="+event.sensor.toString()+"\n event.values="+ Arrays.toString(event.values));
        //check if we want to get sensor data already
        SensorDataObject.Orientation oo = null;
        SensorDataObject.Accelerometer ao = null;
        SensorDataObject.Compass co = null;
        long current = System.currentTimeMillis();
        session.setLatestSensorDataTimeStamp(current);
        //if (current >= nextTimestampToSave) {
        //    lastTimestamp = nextTimestampToSave;
        //    nextTimestampToSave = current;


            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values.clone();
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values.clone();
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    float azimuth = orientation[0] * (180 / (float) Math.PI);
                    float compass = azimuth;
                    if (azimuth < 0){
                        compass = 360+azimuth;
                    } else if (azimuth >= 360) {
                        compass = azimuth-360;
                    }

                    float pitch = orientation[1]* (180 / (float) Math.PI);
                    float roll = orientation[2]* (180 / (float) Math.PI);

                    //save ORIENTATION data
                    long toffset = current - lastTimestamp;
                    int toffsetInteger = -1;
                    if (toffset < Integer.MAX_VALUE && toffset > Integer.MIN_VALUE) {
                        toffsetInteger = (int) toffset;
                    }

                    oo = new SensorDataObject.Orientation(compass,pitch,roll,toffsetInteger);
                    this.latestOrientation.add(oo);
                    LOG.debug(String.format("onSensorChanged orient obj added: %s \n orient array: %s",oo.toString(),Arrays.toString(this.latestOrientation.toArray())));

                    //acceleration
                    float x = mGravity[0];
                    float y = mGravity[1];
                    float z = mGravity[2];

                    //save acceleration
                    ao = new SensorDataObject.Accelerometer(x,y,z,toffsetInteger);
                    this.latestAccelerometer.add(ao);
                    LOG.debug(String.format("onSensorChanged accel obj added: %s \n accel array: %s",ao.toString(),Arrays.toString(this.latestAccelerometer.toArray())));

                    //save compass
                    co = new SensorDataObject.Compass(compass, toffsetInteger);
                    this.latestCompass.add(co);
                    LOG.debug(String.format("onSensorChanged compass obj added: %s \n compass array: %s",co.toString(),Arrays.toString(this.latestCompass.toArray())));

                    //determine when the next sensor data shall be monitored
                    nextTimestampToSave += preferenceHelper.getSensorDataInterval(); // had a /10, should be placed somewhere else

                    mGravity = null;
                    mGeomagnetic = null;
                }
            }
        //}
        //loggingService.onSensorChanged();
        LOG.debug("processed sensor data calling loggingservice, ts="+current);
        //loggingService.stopSensorManagerAndResetAlarm(current,ao,oo,co);
        //TODO: Would need onLocationChanged similar call here that processes + deactivates sensors and reschedules sensors here
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
