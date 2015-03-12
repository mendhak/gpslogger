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
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlLogger;
import com.mendhak.gpslogger.loggers.opengts.OpenGTSLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLoggerFactory {
    public static List<IFileLogger> GetFileLoggers(Context context) {
        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        List<IFileLogger> loggers = new ArrayList<IFileLogger>();

        if (AppSettings.shouldLogToGpx()) {
            File gpxFile = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".gpx");
            loggers.add(new Gpx10FileLogger(gpxFile, Session.shouldAddNewTrackSegment(), Session.getSatelliteCount()));
        }

        if (AppSettings.shouldLogToKml()) {
            File kmlFile = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, Session.shouldAddNewTrackSegment()));
        }

        if (AppSettings.shouldLogToPlainText()) {
            File file = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".txt");
            loggers.add(new PlainTextFileLogger(file));
        }

        if (AppSettings.shouldLogToOpenGTS()) {
            loggers.add(new OpenGTSLogger(context));
        }

        if (AppSettings.shouldLogToCustomUrl()) {
            float batteryLevel = Utilities.GetBatteryLevel(context);
            String androidId = Utilities.GetAndroidId(context);
            loggers.add(new CustomUrlLogger(AppSettings.getCustomLoggingUrl(), Session.getSatelliteCount(), batteryLevel, androidId, context));
        }


        return loggers;
    }

    public static void Write(Context context, Location loc) throws Exception {
        for (IFileLogger logger : GetFileLoggers(context)) {
            logger.Write(loc);
        }
    }

    public static void Annotate(Context context, String description, Location loc) throws Exception {
        for (IFileLogger logger : GetFileLoggers(context)) {
            logger.Annotate(description, loc);
        }
    }
}
