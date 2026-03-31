package com.mendhak.gpslogger.loggers.gpx;

import android.location.Location;
import androidx.test.filters.SmallTest;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Date;

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
                .putExtra(BundleConstants.SATELLITES_FIX,22)
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
                .putExtra(BundleConstants.SATELLITES_FIX,22)
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
    public void GetTrackPointXml_NewTrackSegmentPref_AdditionalTrkSegAddedIfTrkSegExists() throws Exception{

        // Here I have to create a temporary file so that I can have a file with an existing TRKPT.
        // There's no method that takes the full file contents as an input so there is no way to test that.
        File tempFile = File.createTempFile("test",".gpx");
        String existingContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><gpx version=\"1.0\" creator=\"GPSLogger 135 - http://gpslogger.mendhak.com/\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">" +
                "<time>2026-03-31T05:58:37.506Z</time><trk><name>20260331</name>" +
                "<trkseg>" +
                "<trkpt lat=\"51.35660812382162\" lon=\"-0.20237763405425874\"><ele>88.7111657481467</ele>" +
                "<time>2026-03-31T05:58:37.506Z</time><speed>0.0</speed><geoidheight>47.2</geoidheight><src>gps</src><sat>7</sat><hdop>1.5</hdop><vdop>1.3</vdop><pdop>2.0</pdop></trkpt>\n" +
                "</trkseg>" +
                "</trk></gpx>";
        try(FileWriter writer = new FileWriter(tempFile)){
            writer.write(existingContent);
        }

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, tempFile, null, true);
        writeHandler.run();

        String fileContent = new String(Files.readAllBytes(tempFile.toPath()));

        assertThat("File should contain two track segments", fileContent.split("<trkseg>").length, is(2));
        assertThat("File should contain two track segments", fileContent.split("</trkseg>").length, is(2));


    }


    @Test
    public void GetTrackPointXml_WhenHDOPPresent_ThenFormattedInXML(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111)
                .withAltitude(9001d)
                .withBearing(91.88f)
                .withSpeed(188.44f)
                .putExtra(BundleConstants.HDOP, "LOOKATTHISHDOP!")
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
                .putExtra(BundleConstants.GEOIDHEIGHT, "MYGEOIDHEIGHT")
                .build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time><course>91.88</course><speed>188.44</speed><geoidheight>MYGEOIDHEIGHT</geoidheight><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with a geoid height", actual, is(expected));
    }



    @Test
    public void GetEndXml_Verify(){
        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true);
        String expected = "</trk></gpx>";
        String actual = writeHandler.getEndXml();

        assertThat("End XML Matches", actual, is(expected));
        assertThat("End XML length matches", actual.length(), is(12));
    }

    @Test
    public void GetEndXmlWithSegment_Verify(){
        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true);
        String expected = "</trkseg></trk></gpx>";
        String actual = writeHandler.getEndXmlWithSegment();

        assertThat("End Xml with track segment matches", actual, is(expected));
        assertThat("End Xml length matches", actual.length(), is(21));

    }
}
