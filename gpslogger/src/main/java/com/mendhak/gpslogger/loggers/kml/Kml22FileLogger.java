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

package com.mendhak.gpslogger.loggers.kml;

import android.location.Location;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.FileLogger;
import com.mendhak.gpslogger.loggers.Files;
import org.slf4j.Logger;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

public class Kml22FileLogger implements FileLogger {
    protected final static Object lock = new Object();
    private final boolean addNewTrackSegment;
    private final File kmlFile;
    protected static final String name = "KML";
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(10), new RejectionHandler(name));


    public Kml22FileLogger(File kmlFile, boolean addNewTrackSegment) {
        this.kmlFile = kmlFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }


    public void write(Location loc) throws Exception {
        Kml22WriteHandler writeHandler = new Kml22WriteHandler(loc, kmlFile, addNewTrackSegment);
        EXECUTOR.execute(writeHandler);
    }

    public void annotate(String description, Location loc) throws Exception {
        
        description = Strings.cleanDescriptionForXml(description);
        
        Kml22AnnotateHandler annotateHandler = new Kml22AnnotateHandler(kmlFile, description, loc);
        EXECUTOR.execute(annotateHandler);
    }

    @Override
    public String getName() {
        return name;
    }
}

class Kml22AnnotateHandler implements Runnable {
    private static final Logger LOG = Logs.of(Kml22AnnotateHandler.class);
    File kmlFile;
    String description;
    Location loc;
    int kmlAnnotationOffset = 261;

    public Kml22AnnotateHandler(File kmlFile, String description, Location loc) {
        this.kmlFile = kmlFile;
        this.description = description;
        this.loc = loc;
    }


    @Override
    public void run() {
        if(!Files.reallyExists(kmlFile)){
            return;
        }

        try {
            synchronized (Kml22FileLogger.lock) {

                String descriptionNode = getPlacemarkXml(description, loc);


                RandomAccessFile r = new RandomAccessFile(kmlFile, "rw");
                File tmpFile = new File(kmlFile.getAbsolutePath() + "~");
                tmpFile.createNewFile();
                RandomAccessFile rtemp = new RandomAccessFile(tmpFile, "rw");
                long fileSize = r.length();
                FileChannel sourceChannel = r.getChannel();
                FileChannel targetChannel = rtemp.getChannel();
                sourceChannel.transferTo(kmlAnnotationOffset, (fileSize - kmlAnnotationOffset), targetChannel);
                sourceChannel.truncate(kmlAnnotationOffset);
                r.seek(kmlAnnotationOffset);
                r.write(descriptionNode.getBytes());
                long newOffset = r.getFilePointer();
                targetChannel.position(0L);
                sourceChannel.transferFrom(targetChannel, newOffset, (fileSize - kmlAnnotationOffset));
                sourceChannel.close();
                targetChannel.close();
                tmpFile.delete();


            }
        } catch (Exception e) {
            EventBus.getDefault().post(new CommandEvents.FileWriteFailure());
            LOG.error("Error writing KML annotation", e);
        }
    }

    String getPlacemarkXml(String description, Location loc) {
        StringBuilder descriptionNode = new StringBuilder();
        descriptionNode.append("\n<Placemark><name>");
        descriptionNode.append(description);
        descriptionNode.append("</name><Point><coordinates>");
        descriptionNode.append(String.valueOf(loc.getLongitude()));
        descriptionNode.append(",");
        descriptionNode.append(String.valueOf(loc.getLatitude()));
        descriptionNode.append(",");
        descriptionNode.append(String.valueOf(loc.getAltitude()));
        descriptionNode.append("</coordinates></Point></Placemark>\n");

        return descriptionNode.toString();
    }
}

class Kml22WriteHandler implements Runnable {

    private static final Logger LOG = Logs.of(Kml22WriteHandler.class);
    boolean addNewTrackSegment;
    File kmlFile;
    Location loc;


    public Kml22WriteHandler(Location loc, File kmlFile, boolean addNewTrackSegment) {

        this.loc = loc;
        this.kmlFile = kmlFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }


    @Override
    public void run() {
        try {

            RandomAccessFile raf;

            String dateTimeString = Strings.getIsoDateTime(new Date(loc.getTime()));
            if(PreferenceHelper.getInstance().shouldWriteTimeWithOffset()){
                dateTimeString = Strings.getIsoDateTimeWithOffset(new Date(loc.getTime()));
            }

            synchronized (Kml22FileLogger.lock) {

                if(!Files.reallyExists(kmlFile)){
                    kmlFile.createNewFile();

                    FileOutputStream initialWriter = new FileOutputStream(kmlFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    StringBuilder initialXml = new StringBuilder();
                    initialXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    initialXml.append("\n<kml xmlns=\"http://www.opengis.net/kml/2.2\" ");
                    initialXml.append("xmlns:gx=\"http://www.google.com/kml/ext/2.2\" ");
                    initialXml.append("xmlns:kml=\"http://www.opengis.net/kml/2.2\" ");
                    initialXml.append("xmlns:atom=\"http://www.w3.org/2005/Atom\">");
                    initialXml.append("\n<Document>");
                    initialXml.append("\n<name>").append(dateTimeString).append("</name>\n");

                    initialXml.append("</Document></kml>\n");
                    initialOutput.write(initialXml.toString().getBytes());
                    initialOutput.flush();
                    initialOutput.close();

                    //New file, so new track segment
                    addNewTrackSegment = true;
                }


                if (addNewTrackSegment) {
                    raf = new RandomAccessFile(kmlFile, "rw");
                    raf.seek(kmlFile.length() - "</Document></kml>\n".length());
                    String placemarkHead = "\n<Placemark>" + "\n<name>" + dateTimeString + "</name>\n" + "<gx:Track>\n";
                    String placemarkTail = "</gx:Track>\n</Placemark>\n</Document></kml>\n";
                    raf.write((placemarkHead + placemarkTail).getBytes());
                    raf.close();
                }

                int targetTrackLineIndex = -1;
                int lastWhenLineIndex = -1;
                int lastTrackCloseLineIndex = -1;
                int currentLine = 0;

                // I need to find the closing when, to add the next when; closing gx:Track to add next gx:Coord.
                // There can be multiple gx:Tracks in a file so just find the last one in the file.
                try (BufferedReader reader = new BufferedReader(new FileReader(kmlFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("<gx:Track>")) {
                            // A new gx:Track, reset other indexes.
                            targetTrackLineIndex = currentLine;
                            lastWhenLineIndex = -1;
                            lastTrackCloseLineIndex = -1;
                        }

                        if (line.contains("</when>")) {
                            lastWhenLineIndex = currentLine;
                        }
                        if (line.contains("</gx:Track>")) {
                            lastTrackCloseLineIndex = currentLine;
                        }
                        currentLine++;
                    }
                }

                // Act on targetTrackLineIndex. If lastWhen is -1, then just write both.
                // If lastWhen have values, write at those positions
                File tempFile = new File(kmlFile.getAbsolutePath() + ".tmp");
                currentLine = 0;

                try (BufferedReader reader = new BufferedReader(new FileReader(kmlFile));
                     BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                    String line;
                    while((line = reader.readLine()) != null){

                        writer.write(line);
                        writer.newLine();

                        if(lastWhenLineIndex == -1){
                            if(currentLine ==  targetTrackLineIndex){
                                // If lastWhen is -1, that's an empty gx Track, so write both just before the closing gx:Track
                                writer.write("  <gx:altitudeMode>absolute</gx:altitudeMode>");
                                writer.newLine();
                                writer.write("  <when>" + dateTimeString + "</when>");
                                writer.newLine();
                                writer.write("  <gx:coord>"
                                        + String.valueOf(loc.getLongitude())
                                        + " "
                                        + String.valueOf(loc.getLatitude())
                                        + " "
                                        + String.valueOf(loc.getAltitude())
                                        + "</gx:coord>");
                                writer.newLine();
                            }
                        }
                        else {
                            if(currentLine == lastWhenLineIndex){
                                writer.write("  <when>" + dateTimeString + "</when>");
                                writer.newLine();
                            }
                            if(currentLine+1 == lastTrackCloseLineIndex){
                                writer.write("  <gx:coord>"
                                        + String.valueOf(loc.getLongitude())
                                        + " "
                                        + String.valueOf(loc.getLatitude())
                                        + " "
                                        + String.valueOf(loc.getAltitude())
                                        + "</gx:coord>");
                                writer.newLine();
                            }

                        }
                        currentLine++;
                    }
                }

                if (kmlFile.delete()){
                    tempFile.renameTo(kmlFile);
                }



//                // Can't use ReversedLinesFileReader because that needs Android 8+/API26.
//                try (BufferedReader reader = new BufferedReader(new FileReader(kmlFile))){
//                    String line;
//                    int length = 0;
//                    int latestGxTrackClosePosition = -1;
//                    int latestWhenClosePosition = -1;
//
//                    while((line = reader.readLine()) != null) {
//
//                        if(line.contains("</gx:Track>")){
//                            // We put the latest <gx:coord> just before this
//                            latestGxTrackClosePosition = length + line.indexOf("</gx:Track>");
//                            LOG.info("Found gx track close at file position: " + latestGxTrackClosePosition);
//                        }
//                        if(line.contains("</when>")){
//                            // If other when are present, we'd put the latest <when> just after this
//                            latestWhenClosePosition = length + line.indexOf("</when>") + "</when>".length();
//                            LOG.info("Found when close at file position: " + latestWhenClosePosition);
//                        }
//
//                        length += line.length();
//                    }
//                }

//                try (ReversedLinesFileReader reader = new ReversedLinesFileReader(kmlFile, StandardCharsets.UTF_8)){
//                    String line;
//                    int length = 0;
//                    while((line = reader.readLine()) != null) {
//                        length += line.length();
//                        if(line.contains("</gx:Track>")){
//                            LOG.info("Found at file position from end: " + length);
//                            addNewTrackSegment = false;
//                            break;
//                        }
//
//                    }
//
//                }

//                StringBuilder coords = new StringBuilder();
//                coords.append("\n<when>");
//                coords.append(dateTimeString);
//                coords.append("</when>\n<gx:coord>");
//                coords.append(String.valueOf(loc.getLongitude()));
//                coords.append(" ");
//                coords.append(String.valueOf(loc.getLatitude()));
//                coords.append(" ");
//                coords.append(String.valueOf(loc.getAltitude()));
//                coords.append("</gx:coord>\n");
//                coords.append(placemarkTail);
//
//                raf = new RandomAccessFile(kmlFile, "rw");
//                raf.seek(kmlFile.length() - 42);
//                raf.write(coords.toString().getBytes());
//                raf.close();
                LOG.debug("Finished writing to KML22 File");
            }

        } catch (Exception e) {
            EventBus.getDefault().post(new CommandEvents.FileWriteFailure());
            LOG.error("Error writing KML file", e);
        }
    }
}
