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

package com.mendhak.gpslogger.loggers;

import android.content.Context;
import android.location.Location;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.loggers.csv.PlainTextFileLogger;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlLogger;
import com.mendhak.gpslogger.loggers.gpx.Gpx10FileLogger;
import com.mendhak.gpslogger.loggers.kml.Kml22FileLogger;
import com.mendhak.gpslogger.loggers.opengts.OpenGTSLogger;
import com.mendhak.gpslogger.loggers.wear.AndroidWearLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLoggerFactory {

    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public static List<FileLogger> getFileLoggers(Context context) {

        List<FileLogger> loggers = new ArrayList<>();

        if(Strings.isNullOrEmpty(preferenceHelper.getGpsLoggerFolder())){
            return loggers;
        }

        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        if (preferenceHelper.shouldLogToGpx()) {
            File gpxFile = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".gpx");
            loggers.add(new Gpx10FileLogger(gpxFile, Session.shouldAddNewTrackSegment()));
        }

        if (preferenceHelper.shouldLogToKml()) {
            File kmlFile = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, Session.shouldAddNewTrackSegment()));
        }

        if (preferenceHelper.shouldLogToPlainText()) {
            File file = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".txt");
            loggers.add(new PlainTextFileLogger(file));
        }

        if (preferenceHelper.shouldLogToOpenGTS()) {
            loggers.add(new OpenGTSLogger(context));
        }

        if (preferenceHelper.shouldLogToCustomUrl()) {
            float batteryLevel = Systems.getBatteryLevel(context);
            String androidId = Systems.getAndroidId(context);
            loggers.add(new CustomUrlLogger(preferenceHelper.getCustomLoggingUrl(), batteryLevel, androidId));
        }

        if(/* Should log to Android Wear */  true){
            loggers.add(new AndroidWearLogger(context));
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
}
