package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;

import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

/**
 * Created by clemens on 10.05.17.
 */


public class GeoJSONWriterFeatureCollections implements GeoJSONWriter {
    private static final Logger LOG = Logs.of(GeoJSONWriterFeatureCollections.class);
    private final boolean addNewTrackSegment;
    String desc;
    File file;
    Location location;
    private final static String TEMPLATE =
            "{\"type\": \"Feature\"," +
                    "\"properties\":{" +
                    "\"time\":\"%s\"," +
                    "\"altitude\":\"%s\"" +
                    "%s" +
                    "}," +
                    "\"geometry\":{" +
                    "\"type\":\"Point\",\"coordinates\":[" +
                    "%s, %s]}}";
    private final static String COLLECTION_TEMPLATE = "{\"type\": \"FeatureCollection\",\"features\":[%s]}";
    private final static String DESC_TEMPLATE = ",\"description\":\"%s\"";

    public GeoJSONWriterFeatureCollections(File file, Location location, String desc, boolean addNewTrackSegment) {
        this.file = file;
        this.location = location;
        this.desc=desc;
        this.addNewTrackSegment = addNewTrackSegment;
    }

    @Override
    public void run() {
        try {
            String dateTimeString = Strings.getIsoDateTime(new Date(location.getTime()));
            String extra = "";
            if (desc != null) {
                extra = String.format(DESC_TEMPLATE, desc);
            }

            synchronized (GeoJSONLogger.lock) {
                String value = String.format(TEMPLATE,
                        dateTimeString,
                        location.getAltitude(),
                        extra,
                        location.getLongitude(), location.getLatitude());
                int offset;
                if (addNewTrackSegment || !file.exists()){
                    value = String.format(COLLECTION_TEMPLATE, value) + "\n]}";
                    offset = -2;
                }else{
                    value += "\n]}]}";
                    offset = -5;
                }
                if (!file.exists()) {
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"type\": \"FeatureCollection\",\"features\": [\n");
                    sb.append(value);
                    //sb.append("\n]}");
                    bos.write(sb.toString().getBytes());
                    bos.flush();
                    bos.close();
                }else {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.seek(file.length() + offset);
                    raf.write(("," + value).getBytes());
                    raf.close();
                }

            }
            System.out.println("leave sync");
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("GeoJSONWriterLine", e);
        }

    }
}