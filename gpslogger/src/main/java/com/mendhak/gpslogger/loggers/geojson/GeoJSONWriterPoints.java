package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.File;
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
                    "%s" +
                    "}," +
                    "\"geometry\":{" +
                    "\"type\":\"Point\",\"coordinates\":" +
                    "%s}}\n";
    private final static String ATTRIBUTE_TEMPLATE = ",\"%s\":\"%s\"";
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

            synchronized (GeoJSONLogger.lock) {
                byte[] value = getString(file.exists()).getBytes();

                RandomAccessFile raf;
                if (!file.exists()) {
                    file.createNewFile();
                    raf = new RandomAccessFile(file, "rw");
                } else {
                    raf = new RandomAccessFile(file, "rw");
                    raf.seek(file.length() + TRAILER_LENGTH);
                }
                raf.write(value);
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("GeoJSONWriterPoints", e);
        }

    }

    /**
     * Format the GeoJSON entry
     *
     * @param append False to prepend the GeoJSON header (initialize file), true to prepend a list dividing character
     * @return A formatted {@link String} to write at position -TRAILER_LENGTH (or 0 for a new file)
     */
    @NonNull
    protected String getString(boolean append) {
        String coords = String.format(COORD_TEMPLATE, location.getLongitude(), location.getLatitude());
        StringBuilder value = new StringBuilder();
        if (append) {
            value.append(DIVIDER);
        } else {
            value.append(HEADER);
        }
        String dateTimeString = Strings.getIsoDateTime(new Date(location.getTime()));
        String extra = "";
        StringBuilder attributes = new StringBuilder();
        attributes.append("\"time\":\"").append(dateTimeString).append("\"");
        attributes.append(String.format(ATTRIBUTE_TEMPLATE, "provider", location.getProvider()));
        attributes.append(String.format(ATTRIBUTE_TEMPLATE, "time_long", location.getTime()));
        if (desc != null) {
            attributes.append(String.format(ATTRIBUTE_TEMPLATE, "description", desc));
        }
        if (location.hasAccuracy()) {
            attributes.append(String.format(ATTRIBUTE_TEMPLATE, "accuracy", location.getAccuracy()));
        }
        if (location.hasAltitude()) {
            attributes.append(String.format(ATTRIBUTE_TEMPLATE, "altitude", location.getAltitude()));
        }
        if (location.hasBearing()) {
            attributes.append(String.format(ATTRIBUTE_TEMPLATE, "bearing", location.getBearing()));
        }
        if (location.hasSpeed()) {
            attributes.append(String.format(ATTRIBUTE_TEMPLATE, "speed", location.getSpeed()));
        }

        extra = attributes.toString();
        value.append(String.format(TEMPLATE,
                extra,
                coords)).append(TRAILER);
        return value.toString();
    }
}