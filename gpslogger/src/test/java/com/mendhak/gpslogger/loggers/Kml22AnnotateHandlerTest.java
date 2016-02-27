package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class Kml22AnnotateHandlerTest {



    @Test
    public void GetPlacemarkXml_BasicLocation_BasicPlacemarkNodeReturned() {

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