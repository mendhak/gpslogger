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

import android.os.Environment;
import android.util.Log;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileLoggerFactory
{
    public static List<String> GetFileLoggersNames() {

        List<String> loggers = new ArrayList<String>();

        if (AppSettings.shouldLogToGpx())
        {
            loggers.add(Gpx10FileLogger.name);
        }

        if (AppSettings.shouldLogToIgc())
        {
            loggers.add(IgcFileLogger.name);
        }

        if (AppSettings.shouldLogToKml())
        {
            loggers.add(Kml22FileLogger.name);
        }

        if (AppSettings.shouldLogToPlainText())
        {
            loggers.add(PlainTextFileLogger.name);
        }

        if (AppSettings.shouldLogToSkylines())
        {
            loggers.add(SkyLinesLogger.name);
        }

        if (AppSettings.shouldLogToLivetrack24())
        {
            loggers.add(LiveTrack24FileLogger.name);
        }

        if (AppSettings.shouldLogToOpenGTS())
        {
            loggers.add(OpenGTSLogger.name);
        }
        return loggers;
    }

    public static List<IFileLogger> GetFileLoggers()
    {
        File gpx_or_igcFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
        if (!gpx_or_igcFolder.exists())
        {
            gpx_or_igcFolder.mkdirs();
        }

        List<IFileLogger> loggers = new ArrayList<IFileLogger>();

        if (AppSettings.shouldLogToGpx())
        {
            File gpxFile = new File(gpx_or_igcFolder.getPath(), Session.getCurrentFileName() + ".gpx");
            loggers.add(new Gpx10FileLogger(gpxFile,  Session.shouldAddNewTrackSegment(), Session.getSatelliteCount(), AppSettings.getMinimumSeconds(), AppSettings.getMinimumDistanceInMeters()));
        }

        if (AppSettings.shouldLogToIgc())
        {
            File igcFile = new File(gpx_or_igcFolder.getPath(), Session.getCurrentFileName() + ".igc");
            try{
                loggers.add(IgcFileLogger.getIgcFileLogger(igcFile, AppSettings.getIgcPrivateKey(), AppSettings.getMinimumSeconds(), AppSettings.getMinimumDistanceInMeters()));
            } catch (IOException ioe){

            }
        }

        if (AppSettings.shouldLogToKml())
        {
            File kmlFile = new File(gpx_or_igcFolder.getPath(), Session.getCurrentFileName() + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, Session.shouldAddNewTrackSegment(), AppSettings.getMinimumSeconds(), AppSettings.getMinimumDistanceInMeters()));
        }

        if (AppSettings.shouldLogToPlainText())
        {
            File file = new File(gpx_or_igcFolder.getPath(), Session.getCurrentFileName() + ".txt");
            loggers.add(new PlainTextFileLogger(file, AppSettings.getMinimumSeconds(), AppSettings.getMinimumDistanceInMeters()));
        }

        if (AppSettings.shouldLogToSkylines())
        {
            try{
                loggers.add(SkyLinesLogger.getSkyLinesLogger(Long.parseLong(AppSettings.getSkylinesKey(), 16),
                        AppSettings.getSkylinesInterval(),
                        AppSettings.getSkylinesMinimumDistanceInMeters(),
                        AppSettings.getSkylinesServer(),
                        AppSettings.getSkylinesServerPort()
                ));
            } catch (Exception e){
                Log.e("FileLoggerFactory", "Error creating Skylines logger", e);
            }
        }

        if (AppSettings.shouldLogToLivetrack24())
        {
            try{
                loggers.add(LiveTrack24FileLogger.getLiveTrack24Logger(AppSettings.getLivetrack24ServerURL(),
                        AppSettings.getLivetrack24Username(),
                        AppSettings.getLivetrack24Password(),
                        AppSettings.getLivetrack24Interval(),
                        AppSettings.getLivetrack24MinimumDistanceInMeters()));
            } catch (Exception e){
                Log.e("FileLoggerFactory", "Error creating Livetrack24 logger", e);
            }
        }

        if (AppSettings.shouldLogToOpenGTS())
        {
            loggers.add(new OpenGTSLogger(AppSettings.getOpenGTSInterval(), AppSettings.getOpenGTSMinimumDistanceInMeters() ) );
        }

        for(IFileLogger lg : loggers)
        {
            Utilities.LogDebug("FileLoggerFactory created logger: " + lg.getName()+ "("+ lg.getMinSec() + "sec," + lg.getMinDist() + "m)");
        }
        return loggers;
    }
}
