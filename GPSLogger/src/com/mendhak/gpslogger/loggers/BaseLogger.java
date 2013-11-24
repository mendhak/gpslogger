package com.mendhak.gpslogger.loggers;

/**
 * Created by peter on 11/10/13.
 */

import android.os.SystemClock;

import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;

public class BaseLogger {

    private long latestTimeStamp;
    private int minimumSeconds;
    private double lastLoggedLon;
    private double lastLoggedLat;
    private int minimumDistance;
    public boolean timeNeedLog;
    public boolean distNeedLog;

    BaseLogger(int minsec, int mindist) {
        Utilities.LogDebug("Creating BaseLogger("+minsec+","+mindist+")");
        minimumSeconds=minsec;
        minimumDistance=mindist;
        latestTimeStamp=0;
        lastLoggedLon=0.0;
        lastLoggedLat=0.0;
        timeNeedLog=false;
        distNeedLog=false;
    }

    public void SetMinSec(int minsec) {
        minimumSeconds=minsec;
    }

    public void SetMinDist(int mindist) {
        minimumDistance=mindist;
    }

    public int GetMinSec() {
        return minimumSeconds;
    }

    public int GetMinDist() {
        return minimumDistance;
    }

    public long getNextPointTime() {
        Utilities.LogDebug("BaseLogger getNextPointTime latestTimeStamp: " + String.valueOf(latestTimeStamp) + " minimumSeconds: " + String.valueOf(minimumSeconds));
        if( latestTimeStamp>0 ) return latestTimeStamp + minimumSeconds * 1000;
            else return System.currentTimeMillis() + minimumSeconds * 1000;
    }

    public boolean isTimeToLog() {
        long currentTimeStamp = System.currentTimeMillis();
        if ( (currentTimeStamp - latestTimeStamp) < (minimumSeconds * 1000) ) return false;
            else {
            timeNeedLog=true;
            return true;
            }
    }

    public boolean isDistToLog(double lon, double lat) {
        double distanceFromLastPoint = Utilities.CalculateDistance(lat, lon, lastLoggedLat, lastLoggedLon);
        if (minimumDistance > distanceFromLastPoint) return false;
            else {
            distNeedLog=true;
            return true;
            }
    }

    public void SetLatestTimeStamp(long timestamp) {
        Utilities.LogDebug("SetLatestTimeStamp: " + String.valueOf(timestamp));
        latestTimeStamp=timestamp;
    }

}
