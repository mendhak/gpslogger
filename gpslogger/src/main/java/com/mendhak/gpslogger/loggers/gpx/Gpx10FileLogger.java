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

package com.mendhak.gpslogger.loggers.gpx;

import android.location.Location;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.Maths;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.loggers.Files;
import org.slf4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Gpx10FileLogger implements FileLogger {
    protected final static Object lock = new Object();

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(10), new RejectionHandler());
    private File gpxFile = null;
    private final boolean addNewTrackSegment;
    protected final String name = "GPX";

    public Gpx10FileLogger(File gpxFile, boolean addNewTrackSegment) {
        this.gpxFile = gpxFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }

    public void write(Location loc) throws Exception {
        long time = loc.getTime();
        if (time <= 0) {
            time = System.currentTimeMillis();
        }
        String dateTimeString = Strings.getIsoDateTime(new Date(time));

        if(PreferenceHelper.getInstance().shouldWriteTimeWithOffset()){
            dateTimeString = Strings.getIsoDateTimeWithOffset(new Date(time));
        }

        Runnable writeHandler = getWriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment);
        EXECUTOR.execute(writeHandler);
    }

    public Runnable getWriteHandler(String dateTimeString, File gpxFile, Location loc, boolean addNewTrackSegment)
    {
        return new Gpx10WriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment);
    }

    public void annotate(String description, Location loc) throws Exception {

        description = Strings.cleanDescriptionForXml(description);

        long time = loc.getTime();
        if (time <= 0) {
            time = System.currentTimeMillis();
        }
        String dateTimeString = Strings.getIsoDateTime(new Date(time));

        if(PreferenceHelper.getInstance().shouldWriteTimeWithOffset()){
            dateTimeString = Strings.getIsoDateTimeWithOffset(new Date(time));
        }

        Runnable annotateHandler = getAnnotateHandler(description, gpxFile, loc, dateTimeString);
        EXECUTOR.execute(annotateHandler);
    }

    public Runnable getAnnotateHandler(String description, File gpxFile, Location loc, String dateTimeString){
        //Use the writer to calculate initial XML length, use that as offset for annotations
        Gpx10WriteHandler writer = (Gpx10WriteHandler)getWriteHandler(dateTimeString, gpxFile, loc, true);
        return new Gpx10AnnotateHandler(description, gpxFile, loc, dateTimeString, writer.getBeginningXml(dateTimeString).length());
    }

    @Override
    public String getName() {
        return name;
    }


}

class Gpx10AnnotateHandler implements Runnable {
    private static final Logger LOG = Logs.of(Gpx10AnnotateHandler.class);
    String description;
    File gpxFile;
    Location loc;
    String dateTimeString;
    int annotateOffset;

    public Gpx10AnnotateHandler(String description, File gpxFile, Location loc, String dateTimeString, int annotateOffset) {
        this.description = description;
        this.gpxFile = gpxFile;
        this.loc = loc;
        this.dateTimeString = dateTimeString;
        this.annotateOffset = annotateOffset;
    }

    @Override
    public void run() {

        synchronized (Gpx10FileLogger.lock) {
            if(!Files.reallyExists(gpxFile)){
                return;
            }

            String wpt = getWaypointXml(loc, dateTimeString, description);

            try {

                //write to a temp file, delete original file, move temp to original
                File gpxTempFile = new File(gpxFile.getAbsolutePath() + ".tmp");

                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(gpxFile));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(gpxTempFile));

                int written = 0;
                int readSize;
                byte[] buffer = new byte[annotateOffset];
                while ((readSize = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, readSize);
                    written += readSize;

                    System.out.println(written);

                    if (written == annotateOffset) {
                        bos.write(wpt.getBytes());
                        buffer = new byte[20480];
                    }

                }

                bis.close();
                bos.close();

                gpxFile.delete();
                gpxTempFile.renameTo(gpxFile);

                LOG.debug("Finished annotation to GPX10 File");
            } catch (Exception e) {
                LOG.error("Gpx10FileLogger.annotate", e);
            }

        }
    }

    String getWaypointXml(Location loc, String dateTimeString, String description) {

        StringBuilder waypoint = new StringBuilder();

        waypoint.append("\n<wpt lat=\"")
                .append(String.valueOf(loc.getLatitude()))
                .append("\" lon=\"")
                .append(String.valueOf(loc.getLongitude()))
                .append("\">");

        if (loc.hasAltitude()) {
            waypoint.append("<ele>").append(String.valueOf(loc.getAltitude())).append("</ele>");
        }

        waypoint.append("<time>").append(dateTimeString).append("</time>");
        waypoint.append("<name>").append(description).append("</name>");

        waypoint.append("<src>").append(loc.getProvider()).append("</src>");
        waypoint.append("</wpt>\n");

        return waypoint.toString();
    }
}


class Gpx10WriteHandler implements Runnable {
    private static final Logger LOG = Logs.of(Gpx10WriteHandler.class);
    String dateTimeString;
    Location loc;
    private File gpxFile = null;
    private boolean addNewTrackSegment;

    public Gpx10WriteHandler(String dateTimeString, File gpxFile, Location loc, boolean addNewTrackSegment) {
        this.dateTimeString = dateTimeString;
        this.addNewTrackSegment = addNewTrackSegment;
        this.gpxFile = gpxFile;
        this.loc = loc;
    }

    @Override
    public void run() {
        synchronized (Gpx10FileLogger.lock) {

            try {
                if (!Files.reallyExists(gpxFile)) {
                    gpxFile.createNewFile();

                    FileOutputStream initialWriter = new FileOutputStream(gpxFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    initialOutput.write(getBeginningXml(dateTimeString).getBytes());
                    initialOutput.write("<trk>".getBytes());
                    initialOutput.write(getEndXml().getBytes());
                    initialOutput.flush();
                    initialOutput.close();

                    //New file, so new segment.
                    addNewTrackSegment = true;
                }

                int offsetFromEnd = (addNewTrackSegment) ? getEndXml().length() : getEndXmlWithSegment().length();
                long startPosition = gpxFile.length() - offsetFromEnd;
                String trackPoint = getTrackPointXml(loc, dateTimeString);

                RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
                raf.seek(startPosition);
                raf.write(trackPoint.getBytes());
                raf.close();
                Files.addToMediaDatabase(gpxFile, "text/plain");
                LOG.debug("Finished writing to GPX10 file");

            } catch (Exception e) {
                LOG.error("Gpx10FileLogger.write", e);
            }

        }

    }

    String getBeginningXml(String dateTimeString){
        StringBuilder initialXml = new StringBuilder();
        initialXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        initialXml.append("<gpx version=\"1.0\" creator=\"GPSLogger " + BuildConfig.VERSION_CODE + " - http://gpslogger.mendhak.com/\" ");
        initialXml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        initialXml.append("xmlns=\"http://www.topografix.com/GPX/1/0\" ");
        initialXml.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 ");
        initialXml.append("http://www.topografix.com/GPX/1/0/gpx.xsd\">");
        initialXml.append("<time>").append(dateTimeString).append("</time>");
        return initialXml.toString();
    }

    String getEndXml(){
        return "</trk></gpx>";
    }

    String getEndXmlWithSegment(){
        return "</trkseg></trk></gpx>";
    }

    String getTrackPointXml(Location loc, String dateTimeString) {

        StringBuilder track = new StringBuilder();

        if (addNewTrackSegment) {
            track.append("<trkseg>");
        }

        track.append("<trkpt lat=\"")
                .append(String.valueOf(loc.getLatitude()))
                .append("\" lon=\"")
                .append(String.valueOf(loc.getLongitude()))
                .append("\">");

        if (loc.hasAltitude()) {
            track.append("<ele>").append(String.valueOf(loc.getAltitude())).append("</ele>");
        }

        track.append("<time>").append(dateTimeString).append("</time>");

        appendCourseAndSpeed(track, loc);

        if (loc.getExtras() != null) {
            String geoidheight = loc.getExtras().getString(BundleConstants.GEOIDHEIGHT);

            if (!Strings.isNullOrEmpty(geoidheight)) {
                track.append("<geoidheight>").append(geoidheight).append("</geoidheight>");
            }
        }

        track.append("<src>").append(loc.getProvider()).append("</src>");

        if (loc.getExtras() != null) {

            int sat = Maths.getBundledSatelliteCount(loc);

            if(sat > 0){
                track.append("<sat>").append(String.valueOf(sat)).append("</sat>");
            }


            String hdop = loc.getExtras().getString(BundleConstants.HDOP);
            String pdop = loc.getExtras().getString(BundleConstants.PDOP);
            String vdop = loc.getExtras().getString(BundleConstants.VDOP);
            String ageofdgpsdata = loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA);
            String dgpsid = loc.getExtras().getString(BundleConstants.DGPSID);

            if (!Strings.isNullOrEmpty(hdop)) {
                track.append("<hdop>").append(hdop).append("</hdop>");
            }

            if (!Strings.isNullOrEmpty(vdop)) {
                track.append("<vdop>").append(vdop).append("</vdop>");
            }

            if (!Strings.isNullOrEmpty(pdop)) {
                track.append("<pdop>").append(pdop).append("</pdop>");
            }

            if (!Strings.isNullOrEmpty(ageofdgpsdata)) {
                track.append("<ageofdgpsdata>").append(ageofdgpsdata).append("</ageofdgpsdata>");
            }

            if (!Strings.isNullOrEmpty(dgpsid)) {
                track.append("<dgpsid>").append(dgpsid).append("</dgpsid>");
            }
        }


        track.append("</trkpt>\n");

        track.append("</trkseg></trk></gpx>");

        return track.toString();
    }

    public void appendCourseAndSpeed(StringBuilder track, Location loc)
    {
        if (loc.hasBearing()) {
            track.append("<course>").append(String.valueOf(loc.getBearing())).append("</course>");
        }

        if (loc.hasSpeed()) {
            track.append("<speed>").append(String.valueOf(loc.getSpeed())).append("</speed>");
        }
    }
}