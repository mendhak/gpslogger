package com.mendhak.gpslogger.loggers;

import android.location.Location;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


class Gpx10FileLogger implements IFileLogger
{
    protected final static Object lock = new Object();

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
    private File gpxFile = null;
    private boolean useSatelliteTime = false;
    private final boolean addNewTrackSegment;
    private final int satelliteCount;

    Gpx10FileLogger(File gpxFile, boolean useSatelliteTime, boolean addNewTrackSegment, int satelliteCount)
    {
        this.gpxFile = gpxFile;
        this.useSatelliteTime = useSatelliteTime;
        this.addNewTrackSegment = addNewTrackSegment;
        this.satelliteCount = satelliteCount;
    }


    public void Write(Location loc) throws Exception
    {
        Date now;

        if (useSatelliteTime)
        {
            now = new Date(loc.getTime());
        }
        else
        {
            now = new Date();
        }

        String dateTimeString = Utilities.GetIsoDateTime(now);

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment, satelliteCount);
        Utilities.LogDebug(String.format("There are currently %s tasks waiting on the GPX10 EXECUTOR.", EXECUTOR.getQueue().size()));
        EXECUTOR.execute(writeHandler);
    }

    public void Annotate(String description, Location loc) throws Exception
    {
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(description, gpxFile);
        Utilities.LogDebug(String.format("There are currently %s tasks waiting on the GPX10 EXECUTOR.", EXECUTOR.getQueue().size()));
        EXECUTOR.execute(annotateHandler);

    }


}

class Gpx10AnnotateHandler implements Runnable
{
    String description;
    File gpxFile;

    public Gpx10AnnotateHandler(String description, File gpxFile)
    {
        this.description = description;
        this.gpxFile = gpxFile;
    }

    @Override
    public void run()
    {

        synchronized (Gpx10FileLogger.lock)
        {
            if (!gpxFile.exists())
            {
                return;
            }

            if (!gpxFile.exists())
            {
                return;
            }
            int offsetFromEnd = 29;

            long startPosition = gpxFile.length() - offsetFromEnd;

            StringBuilder descXml = new StringBuilder();
            descXml.append("<name>");
            descXml.append(description);
            descXml.append("</name><desc>");
            descXml.append(description);
            descXml.append("</desc></trkpt></trkseg></trk></gpx>");

            RandomAccessFile raf;

            try
            {
                raf = new RandomAccessFile(gpxFile, "rw");
                raf.seek(startPosition);
                raf.write(descXml.toString().getBytes());
                raf.close();
                Utilities.LogDebug("Finished annotation to GPX10 File");
            }
            catch (Exception e)
            {
                Utilities.LogError("Gpx10FileLogger.Annotate", e);
            }

        }
    }
}


class Gpx10WriteHandler implements Runnable
{
    String dateTimeString;
    Location loc;
    private File gpxFile = null;
    private boolean addNewTrackSegment;
    private int satelliteCount;

    public Gpx10WriteHandler(String dateTimeString, File gpxFile, Location loc, boolean addNewTrackSegment, int satelliteCount)
    {
        this.dateTimeString = dateTimeString;
        this.addNewTrackSegment = addNewTrackSegment;
        this.gpxFile = gpxFile;
        this.loc = loc;
        this.satelliteCount = satelliteCount;
    }


    @Override
    public void run()
    {
        synchronized (Gpx10FileLogger.lock)
        {

            try
            {
                if (!gpxFile.exists())
                {
                    gpxFile.createNewFile();

                    FileOutputStream initialWriter = new FileOutputStream(gpxFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    StringBuilder initialXml = new StringBuilder();
                    initialXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
                    initialXml.append("<gpx version=\"1.0\" creator=\"GPSLogger - http://gpslogger.mendhak.com/\" ");
                    initialXml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
                    initialXml.append("xmlns=\"http://www.topografix.com/GPX/1/0\" ");
                    initialXml.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 ");
                    initialXml.append("http://www.topografix.com/GPX/1/0/gpx.xsd\">");
                    initialXml.append("<time>" + dateTimeString + "</time>" + "<bounds />" + "<trk></trk></gpx>");
                    initialOutput.write(initialXml.toString().getBytes());
                    initialOutput.flush();
                    initialOutput.close();
                }

                int offsetFromEnd = (addNewTrackSegment) ? 12 : 21;
                long startPosition = gpxFile.length() - offsetFromEnd;
                String trackPoint = GetTrackPointXml(loc, dateTimeString);

                RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
                raf.seek(startPosition);
                raf.write(trackPoint.getBytes());
                raf.close();
                Utilities.LogDebug("Finished writing to GPX10 file");

            }
            catch (Exception e)
            {
                Utilities.LogError("Gpx10FileLogger.Write", e);
            }

        }

    }

    private String GetTrackPointXml(Location loc, String dateTimeString)
    {

        StringBuilder track = new StringBuilder();

        if (addNewTrackSegment)
        {
            track.append("<trkseg>");
        }

        track.append("<trkpt lat=\"" + String.valueOf(loc.getLatitude()) + "\" lon=\""
                + String.valueOf(loc.getLongitude()) + "\">");

        if (loc.hasAltitude())
        {
            track.append("<ele>" + String.valueOf(loc.getAltitude()) + "</ele>");
        }

        if (loc.hasBearing())
        {
            track.append("<course>" + String.valueOf(loc.getBearing()) + "</course>");
        }

        if (loc.hasSpeed())
        {
            track.append("<speed>" + String.valueOf(loc.getSpeed()) + "</speed>");
        }

        track.append("<src>" + loc.getProvider() + "</src>");

        if (satelliteCount > 0)
        {
            track.append("<sat>" + String.valueOf(satelliteCount) + "</sat>");
        }

        track.append("<time>" + dateTimeString + "</time>");

        track.append("</trkpt>\n");

        track.append("</trkseg></trk></gpx>");

        return track.toString();
    }

}


