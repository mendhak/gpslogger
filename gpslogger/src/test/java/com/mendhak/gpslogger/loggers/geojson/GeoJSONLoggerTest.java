package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;

import com.mendhak.gpslogger.loggers.MockLocations;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by clemens on 15.05.17.
 */
public class GeoJSONLoggerTest {
    GeoJSONLogger log;

    @Test
    public void write() throws Exception {
        File file = new File("test.geojson");
        //file.delete();
        log = new GeoJSONLogger(file,false);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .build();
        log.write(loc);
       // log.write(loc);
        while (GeoJSONLogger.getCount()>0){}
        assertEquals(0, GeoJSONLogger.getCount());
        assertTrue(file.exists());
    }

    @Test
    public void annotate() throws Exception {
        fail("implement me");
    }

}