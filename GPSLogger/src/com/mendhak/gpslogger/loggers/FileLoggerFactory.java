package com.mendhak.gpslogger.loggers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.os.Environment;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;

public class FileLoggerFactory
{
	public static List<IFileLogger> GetFileLoggers()
	{
		File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
		if (!gpxFolder.exists())
		{
			gpxFolder.mkdirs();
		}
		
		List<IFileLogger> loggers = new ArrayList<IFileLogger>();
		
		if (AppSettings.shouldLogToGpx())
		{
			File gpxFile = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".gpx");
			loggers.add(new Gpx10FileLogger(gpxFile, AppSettings.shouldUseSatelliteTime(), Session.shouldAddNewTrackSegment(), Session.getSatelliteCount()));
		}
		
        if(AppSettings.shouldLogToKml() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
        {
            File kmlFile = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, AppSettings.shouldUseSatelliteTime(), Session.shouldAddNewTrackSegment()));
        }
        else if(AppSettings.shouldLogToKml())
        {
            File kmlFile = new File(gpxFolder.getPath(), Session.getCurrentFileName() + ".kml");
            loggers.add(new Kml10FileLogger(kmlFile, AppSettings.shouldUseSatelliteTime()));
        }
		
		return loggers;
	}
}
