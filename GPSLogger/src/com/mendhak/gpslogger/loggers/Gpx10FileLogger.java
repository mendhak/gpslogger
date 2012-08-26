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

import android.location.Location;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Utilities;

import java.io.*;
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
    protected final String name = "GPX";

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

        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(description, gpxFile, loc, dateTimeString);
        Utilities.LogDebug(String.format("There are currently %s tasks waiting on the GPX10 EXECUTOR.", EXECUTOR.getQueue().size()));
        EXECUTOR.execute(annotateHandler);

    }

    @Override
    public String getName()
    {
        return name;
    }


}

class Gpx10AnnotateHandler implements Runnable
{
    String description;
    File gpxFile;
    Location loc;
    String dateTimeString;

    public Gpx10AnnotateHandler(String description, File gpxFile, Location loc, String dateTimeString)
    {
        this.description = description;
        this.gpxFile = gpxFile;
        this.loc = loc;
        this.dateTimeString = dateTimeString;
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

            int startPosition = 346;

            String wpt = GetWaypointXml(loc, dateTimeString, description);

            try
            {

                //Write to a temp file, delete original file, move temp to original
                File gpxTempFile = new File(gpxFile.getAbsolutePath() + ".tmp");

                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(gpxFile));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(gpxTempFile));

                int written = 0;
                int readSize;
                byte[] buffer = new byte[startPosition];
                while ((readSize = bis.read(buffer)) > 0)
                {
                    bos.write(buffer, 0, readSize);
                    written += readSize;

                    System.out.println(written);

                    if (written == startPosition)
                    {
                        bos.write(wpt.getBytes());
                        buffer = new byte[20480];
                    }

                }

                bis.close();
                bos.close();

                gpxFile.delete();
                gpxTempFile.renameTo(gpxFile);

                Utilities.LogDebug("Finished annotation to GPX10 File");
            }
            catch (Exception e)
            {
                Utilities.LogError("Gpx10FileLogger.Annotate", e);
            }

        }
    }

    private String GetWaypointXml(Location loc, String dateTimeString, String description)
    {

        StringBuilder waypoint = new StringBuilder();

        waypoint.append("\n<wpt lat=\"" + String.valueOf(loc.getLatitude()) + "\" lon=\""
                + String.valueOf(loc.getLongitude()) + "\">");

        if (loc.hasAltitude())
        {
            waypoint.append("<ele>" + String.valueOf(loc.getAltitude()) + "</ele>");
        }

        if (loc.hasBearing())
        {
            waypoint.append("<course>" + String.valueOf(loc.getBearing()) + "</course>");
        }

        if (loc.hasSpeed())
        {
            waypoint.append("<speed>" + String.valueOf(loc.getSpeed()) + "</speed>");
        }

        waypoint.append("<name>" + description + "</name>");

        waypoint.append("<src>" + loc.getProvider() + "</src>");


        waypoint.append("<time>" + dateTimeString + "</time>");

        waypoint.append("</wpt>\n");

        return waypoint.toString();
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

                    //New file, so new segment.
                    addNewTrackSegment = true;
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

        if (loc.hasAccuracy() && loc.getAccuracy() > 0)
        {
            // Accuracy divided by 5 or 6 for approximate HDOP
            track.append("<hdop>" + String.valueOf(loc.getAccuracy() / 5) + "</hdop>");
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


