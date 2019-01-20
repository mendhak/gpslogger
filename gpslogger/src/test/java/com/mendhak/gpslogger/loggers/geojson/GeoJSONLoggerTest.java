package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;

import com.mendhak.gpslogger.loggers.MockLocations;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by clemens on 15.05.17.
 */
public class GeoJSONLoggerTest {
    GeoJSONLogger log;

    private Location getLocation() {
        return MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .build();
    }

    @Test
    public void annotate() throws Exception {
        GeoJSONWriterPoints geojson = new GeoJSONWriterPoints(null, getLocation(), "test", false);
        String result = geojson.getString(false);
        String expected = "{\"type\": \"FeatureCollection\",\"features\": [\n" +
                "{\"type\": \"Feature\",\"properties\":{\"time\":\"1970-01-01T00:00:00.000Z\"," +
                "\"provider\":\"MOCK\",\"time_long\":0,\"description\":\"test\"" +
                ",\"altitude\":9001.0,\"bearing\":91.88,\"speed\":188.44}," +
                "\"geometry\":{\"type\":\"Point\",\"coordinates\":[19.111,12.193]}}\n]}";
        assertEquals("annotation", expected, result);
    }

    @Test
    public void annotate_with_remove_badchars() throws Exception {
        GeoJSONWriterPoints geojson = new GeoJSONWriterPoints(null, getLocation(), "\"Double Quotes\" and \\Backslashes need to go\\", false);
        String result = geojson.getString(false);
        String expected = "{\"type\": \"FeatureCollection\",\"features\": [\n" +
                "{\"type\": \"Feature\",\"properties\":{\"time\":\"1970-01-01T00:00:00.000Z\"," +
                "\"provider\":\"MOCK\",\"time_long\":0,\"description\":\"Double Quotes and Backslashes need to go\"" +
                ",\"altitude\":9001.0,\"bearing\":91.88,\"speed\":188.44}," +
                "\"geometry\":{\"type\":\"Point\",\"coordinates\":[19.111,12.193]}}\n]}";
        assertEquals("annotation", expected, result);
    }


    @Test
    public void testLocationString() throws Exception {
        GeoJSONWriterPoints geojson = new GeoJSONWriterPoints(null, getLocation(), null, false);
        String result = geojson.getString(false);
        String expected = "{\"type\": \"FeatureCollection\",\"features\": [\n" +
                "{\"type\": \"Feature\",\"properties\":{\"time\":\"1970-01-01T00:00:00.000Z\"," +
                "\"provider\":\"MOCK\",\"time_long\":0,\"altitude\":9001.0," +
                "\"bearing\":91.88,\"speed\":188.44}," +
                "\"geometry\":{\"type\":\"Point\",\"coordinates\":[19.111,12.193]}}\n]}";
        assertEquals("locationString", expected, result);


    }
    @Test
    public void testLocationStringAppend() throws Exception {
        GeoJSONWriterPoints geojson = new GeoJSONWriterPoints(null, getLocation(), null, false);
        String result = geojson.getString(true);
        String expected = ",{\"type\": \"Feature\",\"properties\":{\"time\":\"1970-01-01T00:00:00.000Z\"," +
                "\"provider\":\"MOCK\",\"time_long\":0,\"altitude\":9001.0," +
                "\"bearing\":91.88,\"speed\":188.44}," +
                "\"geometry\":{\"type\":\"Point\",\"coordinates\":[19.111,12.193]}}\n]}";
        assertEquals("locationString", expected, result);


    }
}