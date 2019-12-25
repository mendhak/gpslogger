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

package com.mendhak.gpslogger.loggers;

import android.content.Context;
import android.location.Location;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.loggers.csv.CSVFileLogger;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlLogger;
import com.mendhak.gpslogger.loggers.geojson.GeoJSONLogger;
import com.mendhak.gpslogger.loggers.gpx.Gpx10FileLogger;
import com.mendhak.gpslogger.loggers.gpx.Gpx11FileLogger;
import com.mendhak.gpslogger.loggers.kml.Kml22FileLogger;
import com.mendhak.gpslogger.loggers.opengts.OpenGTSLogger;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Date; 

public class FileLoggerFactory {

    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private static Session session = Session.getInstance();
    private static long lastURLPostTime=0; 
    
    public static List<FileLogger> getFileLoggers(Context context) {

        List<FileLogger> loggers = new ArrayList<>();

        if(Strings.isNullOrEmpty(preferenceHelper.getGpsLoggerFolder())){
            return loggers;
        }

        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        int batteryLevel = Systems.getBatteryLevel(context);

        if (preferenceHelper.shouldLogToGpx()) {
            File gpxFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".gpx");
            if(preferenceHelper.shouldLogAsGpx11()) {
                loggers.add(new Gpx11FileLogger(gpxFile, session.shouldAddNewTrackSegment()));
            } else {
                loggers.add(new Gpx10FileLogger(gpxFile, session.shouldAddNewTrackSegment()));
            }
        }

        if (preferenceHelper.shouldLogToKml()) {
            File kmlFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, session.shouldAddNewTrackSegment()));
        }

        if (preferenceHelper.shouldLogToCSV()) {
            File file = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".csv");
            loggers.add(new CSVFileLogger(file, batteryLevel));
        }

        if (preferenceHelper.shouldLogToOpenGTS()) {
            loggers.add(new OpenGTSLogger(context, batteryLevel));
        }

        if (preferenceHelper.shouldLogToCustomUrl()) {
            Date date= new Date();
            if ( (date.getTime() - lastURLPostTime ) > preferenceHelper.getLog2URLMinTimeBetweenLog() * 1000) {
                lastURLPostTime=date.getTime();
                String androidId = Systems.getAndroidId(context);
                loggers.add(new CustomUrlLogger(preferenceHelper.getCustomLoggingUrl(),
                        batteryLevel,
                        androidId,
                        preferenceHelper.getCustomLoggingHTTPMethod(),
                        preferenceHelper.getCustomLoggingHTTPBody(),
                        preferenceHelper.getCustomLoggingHTTPHeaders(),
                        preferenceHelper.getCustomLoggingBasicAuthUsername(),
                        preferenceHelper.getCustomLoggingBasicAuthPassword()));
            }
        }

        if(preferenceHelper.shouldLogToGeoJSON()){
            File file = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".geojson");
            loggers.add(new GeoJSONLogger(file, session.shouldAddNewTrackSegment()));
        }


        return loggers;
    }

    public static void write(Context context, Location loc) throws Exception {
        for (FileLogger logger : getFileLoggers(context)) {
            logger.write(loc);
        }
    }

    public static void annotate(Context context, String description, Location loc) throws Exception {
        for (FileLogger logger : getFileLoggers(context)) {
            logger.annotate(description, loc);
        }
    }
    
    public static void resetLastURLPostTime(){
        this.lastURLPostTime=0;
    }
    
}
