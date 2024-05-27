package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;
import androidx.annotation.NonNull;

import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import de.greenrobot.event.EventBus;

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
    private final static String NUMERIC_ATTRIBUTE_TEMPLATE = ",\"%s\":%s";
    private final static String COORD_TEMPLATE = "[%s,%s]";
    private final static String DIVIDER = ",";
    private final String dateTimeString;
    private String desc;
    private File file;
    private Location location;

    public GeoJSONWriterPoints(File file, Location location, String desc, String dateTimeString) {
        this.file = file;
        this.location = location;
        this.desc = desc;
        this.dateTimeString = dateTimeString;
    }

    @Override
    public void run() {
        try {

            synchronized (GeoJSONLogger.lock) {
                byte[] value = getString(Files.reallyExists(file)).getBytes();

                RandomAccessFile raf;
                if (!Files.reallyExists(file)) {
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
            EventBus.getDefault().post(new CommandEvents.FileWriteFailure());
            LOG.error("Failed to write to GeoJSON file", e);
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
        String extra = "";
        StringBuilder attributes = new StringBuilder();
        attributes.append("\"time\":\"").append(dateTimeString).append("\"");
        attributes.append(String.format(ATTRIBUTE_TEMPLATE, "provider", location.getProvider()));
        attributes.append(String.format(NUMERIC_ATTRIBUTE_TEMPLATE, "time_long", location.getTime()));
        if (!Strings.isNullOrEmpty(desc)) {
            attributes.append(String.format(ATTRIBUTE_TEMPLATE, "description", Strings.cleanDescriptionForJson(desc)));
        }
        if (location.hasAccuracy()) {
            attributes.append(String.format(NUMERIC_ATTRIBUTE_TEMPLATE, "accuracy", location.getAccuracy()));
        }
        if (location.hasAltitude()) {
            attributes.append(String.format(NUMERIC_ATTRIBUTE_TEMPLATE, "altitude", location.getAltitude()));
        }
        if (location.hasBearing()) {
            attributes.append(String.format(NUMERIC_ATTRIBUTE_TEMPLATE, "bearing", location.getBearing()));
        }
        if (location.hasSpeed()) {
            attributes.append(String.format(NUMERIC_ATTRIBUTE_TEMPLATE, "speed", location.getSpeed()));
        }

        extra = attributes.toString();
        value.append(String.format(TEMPLATE,
                extra,
                coords)).append(TRAILER);
        return value.toString();
    }
}