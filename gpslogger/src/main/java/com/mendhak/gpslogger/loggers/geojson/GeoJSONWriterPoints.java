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


public class GeoJSONWriterPoints implements GeoJSONWriter {
    private static final Logger LOG = Logs.of(GeoJSONWriterPoints.class);
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
                    "\"type\":\"Point\",\"coordinates\":" +
                    "%s}}\n";
    private final static String DESC_TEMPLATE = ",\"description\":\"%s\"";
    private final static String COORD_TEMPLATE = "[%s,%s]";

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
                String value;
                int offset;

                String dateTimeString = Strings.getIsoDateTime(new Date(location.getTime()));
                String extra = "";
                if (desc != null) {
                    extra = String.format(DESC_TEMPLATE, desc);
                }
                value = String.format(TEMPLATE,
                        dateTimeString,
                        location.getAltitude(),
                        extra,
                        coords) + "]}";
                offset = -2;

                if (!file.exists()) {
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"type\": \"FeatureCollection\",\"features\": [\n");
                    sb.append(value);
                    bos.write(sb.toString().getBytes());
                    bos.flush();
                    bos.close();
                } else {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.seek(file.length() + offset);
                    raf.write(("," + value).getBytes());
                    raf.close();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("GeoJSONWriterLine", e);
        }

    }
}