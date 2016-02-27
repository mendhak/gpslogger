package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.os.Bundle;

import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class FileLoggerTests  {


    private static final class MockLocations {

        private final Location loc;
        private final Bundle bundle;

        private MockLocations(Location loc, Bundle bundle) {
            this.loc = loc;
            this.bundle = bundle;
        }

        public static MockLocations builder(String providerName, double lat, double lon){

            MockLocations m = new MockLocations(mock(Location.class), mock(Bundle.class));
            when(m.loc.getProvider()).thenReturn(providerName);
            when(m.loc.getLatitude()).thenReturn(lat);
            when(m.loc.getLongitude()).thenReturn(lon);
            return m;
        }


        public MockLocations withAccuracy(float val) {
            when(loc.hasAccuracy()).thenReturn(true);
            when(loc.getAccuracy()).thenReturn(val);
            return this;
        }

        public Location build(){
            return loc;
        }

        public MockLocations withAltitude(double altitude) {
            when(loc.hasAltitude()).thenReturn(true);
            when(loc.getAltitude()).thenReturn(altitude);
            return this;
        }

        public MockLocations withBearing(float bearing) {
            when(loc.hasBearing()).thenReturn(true);
            when(loc.getBearing()).thenReturn(bearing);
            return this;
        }


        public MockLocations withSpeed(float speed) {
            when(loc.hasSpeed()).thenReturn(true);
            when(loc.getSpeed()).thenReturn(speed);
            return this;
        }

        public MockLocations putExtra(String k, String v) {

//            Bundle b = new Bundle();
//            b.putString("HDOP", "LOOKATTHISHDOP!");

            when(loc.getExtras()).thenReturn(bundle);
            when(bundle.getString(eq(k))).thenReturn(v);

            return this;
        }
    }

    @Test
    public void testWaypointXml_BasicInfo(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null);


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).build();


        when(loc.hasAccuracy()).thenReturn(false);

        String actual =  annotateHandler.GetWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertThat("Basic waypoint XML", actual, is(expected));
    }


    @Test
    public void testWaypointXml_WithAltitude(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(9001d).build();

        String actual =  annotateHandler.GetWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertThat("Basic waypoint XML", actual, is(expected));
    }

    @Test
    public void testTrackPointXml_LatLongOnly(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false, 41);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111).build();

        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><src>MOCK</src><sat>41</sat></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Basic trackpoint XML", actual, is(expected));
    }


    @Test
    public void testTrackPointXml_ExtraInfo(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false, 41);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111).withAltitude(9001d).withBearing(91.88f).withSpeed(188.44f).build();

        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><sat>41</sat></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with all info", actual, is(expected));
    }


    @Test
    public void testTrackPointXml_ExtraInfoWithoutSatellites(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false, 0);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .withAccuracy(55f)
                .build();

        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML without satellites", actual, is(expected));
    }

    @Test
    public void testTrackPointXml_NewTrackSegment(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true, 0);


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .build();


        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with a new segment", actual, is(expected));
    }


    @Test
    public void GetTrackPointXml_WhenHDOPPresent_ThenFormattedInXML(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true, 0);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .putExtra("HDOP", "LOOKATTHISHDOP!")
                .build();

        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><hdop>LOOKATTHISHDOP!</hdop></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with an HDOP", actual, is(expected));
    }


    @Test
    public void testTrackPointXml_BundledGeoIdHeight(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true, 0);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .putExtra("GEOIDHEIGHT", "MYGEOIDHEIGHT")
                .build();

        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><geoidheight>MYGEOIDHEIGHT</geoidheight></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with a geoid height", actual, is(expected));
    }

    @Test
    public void testPlacemarkXml_BasicInfo() {

        Kml22AnnotateHandler kmlHandler = new Kml22AnnotateHandler(null, null, null);
        Location loc = MockLocations.builder("MOCK", 12.193,19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .build();

        String actual = kmlHandler.GetPlacemarkXml("This is the annotation",loc);
        String expected = "<Placemark><name>This is the annotation</name><Point><coordinates>19.111,12.193,9001.0</coordinates></Point></Placemark>\n";

        assertThat("Basic Placemark XML", actual, is(expected));
    }


}
