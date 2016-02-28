package com.mendhak.gpslogger.loggers.gpx;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class Gpx10AnnotateHandlerTest {

    @Test
    public void GetWaypointXml_BasicLocation_BasicWptNodeReturned(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null);


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).build();


        when(loc.hasAccuracy()).thenReturn(false);

        String actual =  annotateHandler.GetWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertThat("Basic waypoint XML", actual, is(expected));
    }


    @Test
    public void GetWaypointXml_LocationWithAltitude_WptNodeWithElevationReturned(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(9001d).build();

        String actual =  annotateHandler.GetWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertThat("Basic waypoint XML", actual, is(expected));
    }

}