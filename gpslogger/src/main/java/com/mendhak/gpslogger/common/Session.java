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


import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import com.google.android.gms.location.DetectedActivity;

public class Session {


    private static Session instance = null;
    private SharedPreferences prefs;
    private Location previousLocationInfo;
    private Location currentLocationInfo;

    private Session() {

    }

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
            instance.prefs = PreferenceManager.getDefaultSharedPreferences(AppSettings.getInstance().getApplicationContext());
        }

        return instance;
    }

    private String get(String key, String defaultValue) {
        return prefs.getString("SESSION_" + key, defaultValue);
    }

    private void set(String key, String value) {
        prefs.edit().putString("SESSION_" + key, value).apply();
    }


    public boolean isSinglePointMode() {
        return Boolean.valueOf(get("isSinglePointMode", "false"));
    }

    public void setSinglePointMode(boolean singlePointMode) {
        set("isSinglePointMode", String.valueOf(singlePointMode));
    }

    /**
     * @return whether GPS (tower) is enabled
     */
    public boolean isTowerEnabled() {
        return Boolean.valueOf(get("towerEnabled", "false"));
    }

    /**
     * @param towerEnabled set whether GPS (tower) is enabled
     */
    public void setTowerEnabled(boolean towerEnabled) {
        set("towerEnabled", String.valueOf(towerEnabled));
    }

    /**
     * @return whether GPS (satellite) is enabled
     */
    public boolean isGpsEnabled() {
        return Boolean.valueOf(get("gpsEnabled", "false"));
    }

    /**
     * @param gpsEnabled set whether GPS (satellite) is enabled
     */
    public void setGpsEnabled(boolean gpsEnabled) {
        set("gpsEnabled", String.valueOf(gpsEnabled));
    }

    /**
     * @return whether accelerometer sensor is enabled
     */
    public boolean isSensorAccelerometerEnabled() {
        return Boolean.valueOf(get("accelerometerEnabled", "false"));
    }

    /**
     * @param accelerometerEnabled set whether accelerometer sensor is enabled
     */
    public void setSensorAccelerometerEnabled(boolean accelerometerEnabled) { set("accelerometerEnabled", String.valueOf(accelerometerEnabled));
    }

    /**
     * @return whether magnetic field sensor (compass) is enabled
     */
    public boolean isSensorMagneticFieldEnabled() {
        return Boolean.valueOf(get("magneticFieldEnabled", "false"));
    }

    /**
     * @param magneticFieldEnabled set whether magnetic field sensor (compass) is enabled
     */
    public void setSensorMagneticFieldEnabled(boolean magneticFieldEnabled) { set("magneticFieldEnabled", String.valueOf(magneticFieldEnabled));
    }

    /**
     * @return whether logging has started
     */
    public boolean isStarted() {
        return Boolean.valueOf(get("LOGGING_STARTED", "false"));
    }

    /**
     * @param isStarted set whether logging has started
     */
    public void setStarted(boolean isStarted) {

        set("LOGGING_STARTED", String.valueOf(isStarted));

        if (isStarted) {
            set("startTimeStamp", String.valueOf(System.currentTimeMillis()));
        }
    }

    /**
     * @return the isUsingGps
     */
    public boolean isUsingGps() {
        return Boolean.valueOf(get("isUsingGps", "false"));
    }

    /**
     * @param isUsingGps the isUsingGps to set
     */
    public void setUsingGps(boolean isUsingGps) {
        set("isUsingGps", String.valueOf(isUsingGps));
    }

    /**
     * @return the isUsingAccelerometer
     */
    public boolean isUsingAccelerometer() {
        return Boolean.valueOf(get("isUsingAccelerometer", "false"));
    }

    /**
     * @param isUsingAccelerometer the isUsingAccelerometer to set
     */
    public void setUsingAccelerometer(boolean isUsingAccelerometer) { set("isUsingAccelerometer", String.valueOf(isUsingAccelerometer));
    }

    /**
     * @return the isUsingAccelerometer
     */
    public boolean isUsingMagneticField() {
        return Boolean.valueOf(get("isUsingMagneticField", "false"));
    }

    /**
     * @param isUsingMagneticField the isUsingAccelerometer to set
     */
    public void setUsingMagneticField(boolean isUsingMagneticField) { set("isUsingMagneticField", String.valueOf(isUsingMagneticField));
    }


    /**
     * @return the currentFileName (without extension)
     */
    public String getCurrentFileName() {
        return get("currentFileName", "");
    }


    /**
     * @param currentFileName the currentFileName to set
     */
    public void setCurrentFileName(String currentFileName) {
        set("currentFileName", currentFileName);
    }

    /**
     * @return the number of satellites visible
     */
    public int getVisibleSatelliteCount() {
        return Integer.valueOf(get("satellites", "0"));
    }

    /**
     * @param satellites sets the number of visible satellites
     */
    public void setVisibleSatelliteCount(int satellites) {
        set("satellites", String.valueOf(satellites));
    }


    /**
     * @return the currentLatitude
     */
    public double getCurrentLatitude() {
        if (getCurrentLocationInfo() != null) {
            return getCurrentLocationInfo().getLatitude();
        } else {
            return 0;
        }
    }

    public double getPreviousLatitude() {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLatitude() : 0;
    }

    public double getPreviousLongitude() {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLongitude() : 0;
    }

    public double getTotalTravelled() {
        return Double.valueOf(get("totalTravelled", "0"));
    }

    public int getNumLegs() {
        return Integer.valueOf(get("numLegs", "0"));
    }

    public void setNumLegs(int numLegs) {
        set("numLegs", String.valueOf(numLegs));
    }

    public void setTotalTravelled(double totalTravelled) {
        if (totalTravelled == 0) {
            setNumLegs(1);
        } else {
            setNumLegs(getNumLegs() + 1);
        }
        set("totalTravelled", String.valueOf(totalTravelled));
    }

    public Location getPreviousLocationInfo() {
        return previousLocationInfo;
    }

    public void setPreviousLocationInfo(Location previousLocationInfo) {
        this.previousLocationInfo = previousLocationInfo;
    }


    /**
     * Determines whether a valid location is available
     */
    public boolean hasValidLocation() {
        return (getCurrentLocationInfo() != null && getCurrentLatitude() != 0 && getCurrentLongitude() != 0);
    }

    /**
     * @return the currentLongitude
     */
    public double getCurrentLongitude() {
        if (getCurrentLocationInfo() != null) {
            return getCurrentLocationInfo().getLongitude();
        } else {
            return 0;
        }
    }

    /**
     * @return the latestTimeStamp (for location info)
     */
    public long getLatestTimeStamp() {
        return Long.valueOf(get("latestTimeStamp", "0"));
    }

    /**
     * @return the timestamp when measuring was started
     */
    public long getStartTimeStamp() {
        return Long.valueOf(get("startTimeStamp", String.valueOf(System.currentTimeMillis())));
    }

    /**
     * @param latestTimeStamp the latestTimeStamp (for location info) to set
     */
    public void setLatestTimeStamp(long latestTimeStamp) {
        set("latestTimeStamp", String.valueOf(latestTimeStamp));
    }

    /**
     * @return whether to create a new track segment
     */
    public boolean shouldAddNewTrackSegment() {
        return Boolean.valueOf(get("addNewTrackSegment", "false"));
    }

    /**
     * @param addNewTrackSegment set whether to create a new track segment
     */
    public void setAddNewTrackSegment(boolean addNewTrackSegment) {
        set("addNewTrackSegment", String.valueOf(addNewTrackSegment));
    }

    /**
     * @param autoSendDelay the autoSendDelay to set
     */
    public void setAutoSendDelay(float autoSendDelay) {
        set("autoSendDelay", String.valueOf(autoSendDelay));
    }

    /**
     * @return the autoSendDelay to use for the timer
     */
    public float getAutoSendDelay() {
        return Float.valueOf(get("autoSendDelay", "0"));
    }

    /**
     * @param currentLocationInfo the latest Location class
     */
    public void setCurrentLocationInfo(Location currentLocationInfo) {
        this.currentLocationInfo = currentLocationInfo;
    }

    /**
     * @return the Location class containing latest lat-long information
     */
    public Location getCurrentLocationInfo() {
        return currentLocationInfo;

    }

    /**
     * @param isBound set whether the activity is bound to the GpsLoggingService
     */
    public void setBoundToService(boolean isBound) {
        set("isBound", String.valueOf(isBound));
    }

    /**
     * @return whether the activity is bound to the GpsLoggingService
     */
    public boolean isBoundToService() {
        return Boolean.valueOf(get("isBound", "false"));
    }

    public boolean hasDescription() {
        return !(getDescription().length() == 0);
    }

    public String getDescription() {
        return get("description", "");
    }

    public void clearDescription() {
        setDescription("");
    }

    public void setDescription(String newDescription) {
        set("description", newDescription);
    }

    public void setWaitingForLocation(boolean waitingForLocation) {
        set("waitingForLocation", String.valueOf(waitingForLocation));
    }

    public boolean isWaitingForLocation() {
        return Boolean.valueOf(get("waitingForLocation", "false"));
    }

    public boolean isAnnotationMarked() {
        return Boolean.valueOf(get("annotationMarked", "false"));
    }

    public void setAnnotationMarked(boolean annotationMarked) {
        set("annotationMarked", String.valueOf(annotationMarked));
    }

    public String getCurrentFormattedFileName() {
        return get("currentFormattedFileName", "");
    }

    public void setCurrentFormattedFileName(String currentFormattedFileName) {
        set("currentFormattedFileName", currentFormattedFileName);
    }

    public long getUserStillSinceTimeStamp() {
        return Long.valueOf(get("userStillSinceTimeStamp", "0"));
    }

    public void setUserStillSinceTimeStamp(long lastUserStillTimeStamp) {
        set("userStillSinceTimeStamp", String.valueOf(lastUserStillTimeStamp));
    }

    public void setFirstRetryTimeStamp(long firstRetryTimeStamp) {
        set("firstRetryTimeStamp", String.valueOf(firstRetryTimeStamp));
    }

    public long getFirstRetryTimeStamp() {
        return Long.valueOf(get("firstRetryTimeStamp", "0"));
    }

    public void setLatestDetectedActivity(DetectedActivity latestDetectedActivity) {
        set("latestDetectedActivity", Strings.getDetectedActivityName(latestDetectedActivity));
    }

    public String getLatestDetectedActivityName() {
        return get("latestDetectedActivity", "");
    }


}
