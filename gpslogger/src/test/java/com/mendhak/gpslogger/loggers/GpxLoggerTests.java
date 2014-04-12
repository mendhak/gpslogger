package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.test.AndroidTestCase;

public class GpxLoggerTests extends AndroidTestCase {

    public void setUp() {

    }

    public void testWaypointXml(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null);

        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);

        String actual =  annotateHandler.GetWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertEquals("Basic waypoint XML", expected, actual);
    }
}
