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
    protected int numSensorSamples = 0;

    protected long lastTimestamp = 0;
    protected ArrayList<SensorDataObject.Accelerometer> latestAccelerometer = new ArrayList<>();
    protected ArrayList<SensorDataObject.Compass> latestCompass  = new ArrayList<>();
    protected ArrayList<SensorDataObject.Orientation> latestOrientation = new ArrayList<>();

    //FIXME: These two arrays and their usage is for debug usage. Remove once sensor collection is done.
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

    //This is the main part for sensordata collection
    //FIXME: Remove excess debugging parts once sensor collection is in desired state
    @Override
    public void onSensorChanged(SensorEvent event){
        LOG.debug("onSensorChanged Event. event.sensor="+event.sensor.toString()+"\n event.values="+ Arrays.toString(event.values));

        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                this.accelerometer.add(event);
                this.mGravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                this.magneticField.add(event);
                this.mGeomagnetic = event.values.clone();
                break;
            default:
                LOG.debug(String.format("onSensorChanged undesired type recieved: %d sensor: %s, values: %s",event.sensor.getType(),event.sensor.toString(),Arrays.toString(event.values)));
        }

        LOG.debug(String.format("onSensorChanged \n accel event list size: %d content %s \n magnetic event list size: %d content %s  " +
                "\n this.accel=%s this.magnetic=%s \n reported accuracy %d \n reported type: %s"
                ,this.accelerometer.size(),Arrays.toString(this.accelerometer.toArray()),
                this.magneticField.size(),Arrays.toString(this.magneticField.toArray()),
                Arrays.toString(this.mGravity),
                Arrays.toString(this.mGeomagnetic),
                event.accuracy,
                event.sensor.toString()+":"+event.sensor.getStringType()));

        /*
            Main problem here: accelerometer and magnetic field are required to calculate the metrics visible above and below.
            However, the magnetic field sensor delivers data vastly slower and lesser in quantity such that it may happen that
            within a reasonable amount of time only accelerometer events arrived making it impossible to calculate the required
            metrics.
            Thus a configurable amount of sensor events is accepted to have a sufficiently high possibility to see all required
            events.
            If that amount is exceeded collection is stopped.
            If this is set too high or data collection is not stopped at all, this creates unneeded strain on battery and cpu
            due to the frequency of event delivery especially in the case of the accelerometer.
            In the case of the emulator and a recent Samsung Galaxy phone between 5 and 10 sensor event empirically have been
            found to work well.
         */
        if (this.numSensorSamples >= preferenceHelper.getSensorDataSampleSize()) {
            LOG.debug(String.format("Stopping sensor manager, recorded more than %d samples without success completion of measurement",preferenceHelper.getSensorDataSampleSize()));
            loggingService.stopSensorManagerAndResetAlarm(System.currentTimeMillis(),null,null,null);
            this.numSensorSamples = 0;
        } else {
            this.numSensorSamples++;
            LOG.debug(String.format("onSensorChanged current number of samples %d of %d",this.numSensorSamples, preferenceHelper.getSensorDataSampleSize()));
            calculateSensors(event);
        }
    }


    /**
     * Based on code found here: https://github.com/shiptrail/android-main
     * Records sensordata to be shipped with next location
     * https://developer.android.com/guide/topics/sensors/sensors_overview.html
     * orientation is created from accel + magnetic -> cheap gyro
     * Other, cleaner approaches to position calculation are possible
     */
    //FIXME: Remove excess debugging once sensor data logging is done
    public void calculateSensors(SensorEvent event) {
        LOG.debug("calculateSensors Event. event.sensor="+event.sensor.toString()+"\n event.values="+ Arrays.toString(event.values));
        //check if we want to get sensor data already
        SensorDataObject.Orientation oo = null;
        SensorDataObject.Accelerometer ao = null;
        SensorDataObject.Compass co = null;
        long current = System.currentTimeMillis();

        LOG.debug(String.format("calculate sensors: ts: %d mGravity!=null: %b, mGeomagnetic!=null: %b, gravity: %s, geomag: %s",
                    current,this.mGravity!=null,this.mGeomagnetic!=null
                    ,Arrays.toString(this.mGravity),Arrays.toString(this.mGeomagnetic)));
            if (this.mGravity != null && this.mGeomagnetic != null) {
                float Rs[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(Rs, I, this.mGravity, this.mGeomagnetic);
                LOG.debug(String.format("calcSensors in getRotationMatrix, success=%b, R=%s,I=%s",success, Arrays.toString(Rs),Arrays.toString(I)));
                if (success) {
                    LOG.debug(String.format("calcSensors in getRotationMatrix, in success"));
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(Rs, orientation);

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

                    mGravity = null;
                    mGeomagnetic = null;

                    LOG.debug("all sensor readings present, calculated data + stop,reset&reschedule sensormanager now");
                    loggingService.stopSensorManagerAndResetAlarm(current, ao, oo, co);
                    this.numSensorSamples = 0;
                }
            }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
