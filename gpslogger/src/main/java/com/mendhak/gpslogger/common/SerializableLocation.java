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

package com.mendhak.gpslogger.common;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class SerializableLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    private double altitude;
    private double accuracy;
    private float bearing;
    private double latitude;
    private double longitude;
    private String provider;
    private float speed;
    private long time;
    private boolean hasAltitude;
    private boolean hasAccuracy;
    private boolean hasBearing;
    private boolean hasSpeed;
    private int satelliteCount;
    private String hdop;
    private String vdop;
    private String pdop;
    private String description;
    private int batteryLevel;
    private boolean batteryCharging;
    private String fileName;
    private long startTimeStamp;
    private double distance;
    private String profileName;
    private final String timeWithOffset;

    public SerializableLocation(){
        timeWithOffset = null;
    }

    public SerializableLocation(Location loc) {

        altitude = loc.getAltitude();
        accuracy = loc.getAccuracy();
        bearing = loc.getBearing();
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        provider = loc.getProvider();
        speed = loc.getSpeed();
        time = loc.getTime();
        hasAltitude = loc.hasAltitude();
        hasAccuracy = loc.hasAccuracy();
        hasBearing = loc.hasBearing();
        hasSpeed = loc.hasSpeed();
        satelliteCount = Maths.getBundledSatelliteCount(loc);
        hdop = extractExtra(loc, BundleConstants.HDOP);
        vdop = extractExtra(loc, BundleConstants.VDOP);
        pdop = extractExtra(loc, BundleConstants.PDOP);
        description = extractExtra(loc,BundleConstants.ANNOTATION);

        if(loc.getExtras() != null){
            batteryLevel = loc.getExtras().getInt(BundleConstants.BATTERY_LEVEL, 0);
            batteryCharging = loc.getExtras().getBoolean(BundleConstants.BATTERY_CHARGING, false);
            startTimeStamp = loc.getExtras().getLong(BundleConstants.STARTTIMESTAMP, 0);
            distance = loc.getExtras().getDouble(BundleConstants.DISTANCE, 0);
        }

        fileName = extractExtra(loc, BundleConstants.FILE_NAME);
        profileName = extractExtra(loc, BundleConstants.PROFILE_NAME);
        timeWithOffset = getPreferredIsoDateTime(loc);
    }

    /**
     * Methode checks if a bundled time with offset is available and in ISO-8601 format, is yes, return it, else return
     * formated fix time provided by the location object (which can vary depending on location source)
     * @param loc location object tot get the time from
     * @return ISO-8601 formated date-time string
     */
    private String getPreferredIsoDateTime(Location loc) {
        if (loc.getExtras() != null
                && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.TIME_WITH_OFFSET))) {
            String dt = loc.getExtras().getString(BundleConstants.TIME_WITH_OFFSET);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    DateTimeFormatter.ISO_DATE_TIME.parse(dt);
                    return dt;
                } else {
                    String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX"};
                    for (String pattern : patterns) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                            sdf.setLenient(false);
                            sdf.parse(dt);
                            return dt;
                        } catch (Exception e) {
                            // Ignore exception and try next
                        }
                    }
                }
            } catch (Exception e) {
                return Strings.getIsoDateTimeWithOffset(new Date(this.time));
            }
        }
        return Strings.getIsoDateTimeWithOffset(new Date(this.time));
    }


    private String extractExtra(Location loc, String key) {
        if (loc.getExtras() != null
                && !Strings.isNullOrEmpty(loc.getExtras().getString(key))) {
            return loc.getExtras().getString(key);
        }

        return "";
    }

    public boolean hasAltitude(){
        return hasAltitude;
    }

    public boolean hasAccuracy() {
        return hasAccuracy;
    }

    public boolean hasBearing() {
        return hasBearing;
    }

    public boolean hasSpeed() {
        return hasSpeed;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAltitude(double altitude){
        this.altitude = altitude;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getSatelliteCount() {
        return satelliteCount;
    }

    public String getHDOP() { return hdop; }

    public String getVDOP() { return vdop; }

    public String getPDOP() { return pdop; }

    public String getDescription() { return description; }

    public int getBatteryLevel() { return batteryLevel; }

    public boolean getBatteryCharging() { return batteryCharging; }

    public String getFileName() { return fileName; }

    public long getStartTimeStamp() { return startTimeStamp; }

    public double getDistance() { return distance; }

    public String getProfileName() { return profileName; }

    public String getTimeWithOffset() { return timeWithOffset; }
}
