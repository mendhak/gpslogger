package com.mendhak.gpslogger.loggers.gpx;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;

import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class Gpx10AnnotateHandlerTest {

    @Test
    public void GetWaypointXml_BasicLocation_BasicWptNodeReturned(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null,0);


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).build();


        when(loc.hasAccuracy()).thenReturn(false);

        String actual =  annotateHandler.getWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertThat("Basic waypoint XML", actual, is(expected));
    }


    @Test
    public void GetWaypointXml_LocationWithAltitude_WptNodeWithElevationReturned(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null,0);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(9001d).build();

        String actual =  annotateHandler.getWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertThat("Basic waypoint XML", actual, is(expected));
    }

    @Test
    public void InitialXmlLength_Verify(){
        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true);
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null,
                null, null,
                writeHandler.getBeginningXml(Strings.getIsoDateTime(new Date(1483054318298l))).length());


        String actual = writeHandler.getBeginningXml(Strings.getIsoDateTime(new Date(1483054318298l)));
        String expected =   "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><gpx version=\"1.0\" creator=\"GPSLogger "+ BuildConfig.VERSION_CODE  +" - http://gpslogger.mendhak.com/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"><time>2016-12-29T23:31:58.298Z</time>";

        assertThat("InitialXml matches", actual, is(expected));
        assertThat("Initial XML Length is correct", actual.length(), is(343));
        assertThat("Initial XML length constant is set for others to use", actual.length(), is(annotateHandler.annotateOffset));
    }

}