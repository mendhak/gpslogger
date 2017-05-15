package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;

import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

/**
 * Created by clemens on 10.05.17.
 */


public class GeoJSONWriter implements Runnable {
    private static final Logger LOG = Logs.of(GeoJSONWriter.class);
    private final String desc;
    File file;
    Location location;
    private final static String TEMPLATE =
            "{\"type\": \"Feature\"," +
                    "\"properties\":{" +
                    "\"time\":\"%s\"" +
                    "\"altitude\":\"%s\"" +
                    "\"description\":\"%s\"" +
                    "}," +
                    "\"geometry\":{" +
                    "\"type\":\"Point\",\"coordinates\":[" +
                    "%s,%s]}}";

    public GeoJSONWriter(File file, Location location, String desc) {
        this.file = file;
        this.location = location;
        this.desc=desc;
    }

    @Override
    public void run() {
        try {
            String dateTimeString = Strings.getIsoDateTime(new Date(location.getTime()));

            String value = String.format(TEMPLATE,
                    dateTimeString,
                    location.getAltitude(),
                    desc,
                    location.getLongitude(),
                    location.getLatitude());

            synchronized (GeoJSONLogger.lock) {
                if (!file.exists()) {
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"type\": \"FeatureCollection\",\"features\": [");
                    sb.append(value);
                    sb.append("]}");
                    bos.write(sb.toString().getBytes());
                    bos.flush();
                    bos.close();
                } else {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.seek(file.length() - 2);
                    raf.write(("," + value).getBytes());
                    raf.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("GeoJSONWriter", e);
        }

    }
}