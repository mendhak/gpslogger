package com.mendhak.gpslogger.loggers.gpx;

import android.location.Location;
import androidx.test.filters.SmallTest;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class Gpx11WriteHandlerTest {
    @Test
    public void InitialXmlLength_Verify(){
        Gpx11WriteHandler writeHandler = new Gpx11WriteHandler(null, null, null, true);
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null,
                null, null,
                writeHandler.getBeginningXml(Strings.getIsoDateTime(new Date(1483054318298l))).length());


        String actual = writeHandler.getBeginningXml(Strings.getIsoDateTime(new Date(1483054318298l)));
        String expected =   "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><gpx version=\"1.1\" creator=\"GPSLogger " + BuildConfig.VERSION_CODE + " - http://gpslogger.mendhak.com/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v2\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v2 https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd \"><metadata><time>2016-12-29T23:31:58.298Z</time></metadata>";


        assertThat("InitialXml matches", actual, is(expected));
        assertThat("Initial XML Length is correct", actual.length(), is(554));
        assertThat("Initial XML length constant is set for others to use", actual.length(), is(annotateHandler.annotateOffset));
    }


    @Test
    public void GetTrackpointXml_BasicLocation_BasicTrkptNodeReturned(){

        Gpx11WriteHandler writeHandler = new Gpx11WriteHandler(null, null, null, false);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Basic trackpoint XML", actual, is(expected));
    }


    @Test
    public void GetTrackPointXml_LocationWithAltBearingSpeed_TrkptWithEleCourseSpeedReturned(){

        Gpx11WriteHandler writeHandler = new Gpx11WriteHandler(null, null, null, false);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111).withAltitude(9001d).withBearing(91.88f).withSpeed(188.44f).build();

        String actual = writeHandler.getTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<src>MOCK</src><extensions><gpxtpx:TrackPointExtension><gpxtpx:bearing>91.88</gpxtpx:bearing><gpxtpx:speed>188.44</gpxtpx:speed></gpxtpx:TrackPointExtension></extensions></trkpt>\n</trkseg></trk></gpx>";

        assertThat("Trackpoint XML with all info", actual, is(expected));
    }

}
