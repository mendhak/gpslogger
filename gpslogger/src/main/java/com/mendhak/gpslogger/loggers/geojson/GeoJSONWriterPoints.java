package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;
import android.support.annotation.NonNull;

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


public class GeoJSONWriterPoints implements Runnable {
    private final static Logger LOG = Logs.of(GeoJSONWriterPoints.class);
    private final static String HEADER = "{\"type\": \"FeatureCollection\",\"features\": [\n";
    private final static String TRAILER = "]}";
    private final static int TRAILER_LENGTH = -TRAILER.length();
    private final static String TEMPLATE =
            "{\"type\": \"Feature\"," +
                    "\"properties\":{" +
                    "\"time\":\"%s\"," +
                    "\"altitude\":\"%s\"" +
                    "%s" +
                    "}," +
                    "\"geometry\":{" +
                    "\"type\":\"Point\",\"coordinates\":" +
                    "%s}}\n";
    private final static String DESC_TEMPLATE = ",\"description\":\"%s\"";
    private final static String COORD_TEMPLATE = "[%s,%s]";
    private final static String DIVIDER = ",";
    private String desc;
    private File file;
    private Location location;

    public GeoJSONWriterPoints(File file, Location location, String desc, boolean addNewTrackSegment) {
        this.file = file;
        this.location = location;
        this.desc = desc;
    }

    @Override
    public void run() {
        try {
            String coords = String.format(COORD_TEMPLATE, location.getLongitude(), location.getLatitude());

            synchronized (GeoJSONLogger.lock) {
                byte[] value = getString(coords, file.exists());

                RandomAccessFile raf;
                if (!file.exists()){
                    file.createNewFile();
                    raf = new RandomAccessFile(file, "rw");
                }else {
                    raf = new RandomAccessFile(file, "rw");
                    raf.seek(file.length()+TRAILER_LENGTH);
                }
                raf.write(value);
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("GeoJSONWriterPoints", e);
        }

    }

    @NonNull
    private byte[] getString(String coords, boolean exists) {
        StringBuilder value = new StringBuilder();
        if (exists){
            value.append(DIVIDER);
        }else {
            value.append(HEADER);
        }
        String dateTimeString = Strings.getIsoDateTime(new Date(location.getTime()));
        String extra = "";
        if (desc != null) {
            extra = String.format(DESC_TEMPLATE, desc);
        }
        value.append(String.format(TEMPLATE,
                dateTimeString,
                location.getAltitude(),
                extra,
                coords)).append(TRAILER);
        return value.toString().getBytes();
    }
}