package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;

import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.loggers.FileLogger;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by clemens on 10.05.17.
 */

public class GeoJSONLogger implements FileLogger {
    final static Object lock = new Object();
    private final ThreadPoolExecutor executor;
    private final File file;
    protected final String name;

    public GeoJSONLogger(File file) {
        executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
        this.file = file;
        name = "GeoJSON";
    }

    @Override
    public void write(Location loc) throws Exception {
        GeoJSONWriter gw = new GeoJSONWriter(file, loc, "");
        executor.execute(gw);
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {
        GeoJSONWriter gw = new GeoJSONWriter(file, loc, description);
        executor.execute(gw);
    }

    @Override
    public String getName() {
        return null;
    }
}

