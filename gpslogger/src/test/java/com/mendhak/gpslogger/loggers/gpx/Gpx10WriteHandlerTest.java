package com.mendhak.gpslogger.loggers.gpx;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;



@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class Gpx10WriteHandlerTest {

    @Test
    public void GetTrackpointXml_BasicLocation_BasicTrkptNodeReturned(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Basic trackpoint XML", actual, is(expected));
    }


    @Test
    public void GetTrackPointXml_LocationWithAltBearingSpeed_TrkptWithEleCourseSpeedReturned(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111).withAltitude(9001d).withBearing(91.88f).withSpeed(188.44f).build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with all info", actual, is(expected));
    }


    @Test
    public void GetTrackPointXml_LocationWithoutSatellites_TrkptNodeReturned(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .withAccuracy(55f)
                .build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML without satellites", actual, is(expected));
    }

    @Test
    public void GetTrackpointXml_NumberOfSatellites_TrkptNodeUsesSatellitesUsedInFix(){
        //loc.getExtras().getInt("satellites",-1) should contain the provider specified satellites used in fix
        //If that isn't present, use the one we passed in as our own extra - SATELLITES_FIX
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .withAccuracy(55f)
                .putExtra("satellites",9)
                .putExtra("SATELLITES_FIX",22)
                .build();

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false);

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><sat>9</sat></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint uses satellites used in fix", actual, is(expected));

    }



    @Test
    public void GetTrackpointXml_DefaultSatellitesNotPresent_TrkptNodeUsesSelfTrackedSatellites(){
        //loc.getExtras().getInt("satellites",-1) should contain the provider specified satellites used in fix
        //If that isn't present, use the one we passed in as our own extra - SATELLITES_FIX
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .withAccuracy(55f)
                .putExtra("SATELLITES_FIX",22)
                .build();

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false);

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><sat>22</sat></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint uses satellites used in fix", actual, is(expected));

    }

    @Test
    public void GetTrackPointXml_NewTrackSegmentPref_NewTrkSegReturned(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true);


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .build();


        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with a new segment", actual, is(expected));
    }


    @Test
    public void GetTrackPointXml_WhenHDOPPresent_ThenFormattedInXML(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .putExtra("HDOP", "LOOKATTHISHDOP!")
                .build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><hdop>LOOKATTHISHDOP!</hdop></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with an HDOP", actual, is(expected));
    }


    @Test
    public void GetTrackPointXml_BundledGeoIdHeight_GeoIdHeightNode(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .putExtra("GEOIDHEIGHT", "MYGEOIDHEIGHT")
                .build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time><course>91.88</course><speed>188.44</speed><geoidheight>MYGEOIDHEIGHT</geoidheight><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with a geoid height", actual, is(expected));
    }
}