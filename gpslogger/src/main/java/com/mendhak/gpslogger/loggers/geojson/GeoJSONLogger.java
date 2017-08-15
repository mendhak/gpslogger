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
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
    private final File file;
    protected final String name;
    private final boolean addNewTrackSegment;

    public GeoJSONLogger(File file, boolean addNewTrackSegment) {
        this.file = file;
        name = "GeoJSON";
        this.addNewTrackSegment = addNewTrackSegment;
    }

    @Override
    public void write(Location loc) throws Exception {
        annotate(null, loc);
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {
        Runnable gw = new GeoJSONWriterPoints(file, loc, description, addNewTrackSegment);
        EXECUTOR.execute(gw);
    }

    @Override
    public String getName() {
        return name;
    }

    public static int getCount(){
        return EXECUTOR.getActiveCount();
    }
}

