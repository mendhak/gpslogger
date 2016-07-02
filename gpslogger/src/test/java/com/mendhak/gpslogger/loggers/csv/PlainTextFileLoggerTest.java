package com.mendhak.gpslogger.loggers.csv;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PlainTextFileLoggerTest {

    @Test
    public void getCsvLine_BasicLocation_ReturnsBasicCSV(){
        PlainTextFileLogger plain = new PlainTextFileLogger(null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,0.000000,0.000000,0.000000,0.000000,0,MOCK\n";
        assertThat("Basic CSV line", actual, is(expected));

    }

    @Test
    public void getCsvLine_LocationWithAltitudeAccuracyBearing_ReturnsCSVLine(){
        PlainTextFileLogger plain = new PlainTextFileLogger(null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.000000,41.000000,119.000000,0.000000,0,MOCK\n";
        assertThat("CSV line with altitude, accuracy, bearing", actual, is(expected));
    }

    @Test
    public void getCsvLine_LocationWithSpeedProvider_ReturnsCSVLine(){
        PlainTextFileLogger plain = new PlainTextFileLogger(null);
        Location loc = MockLocations.builder("BRAINS", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9).build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.000000,41.000000,119.000000,9.000000,0,BRAINS\n";
        assertThat("CSV line with speed and provider", actual, is(expected));
    }


    @Test
    public void getCsvLine_LocationWithSatellites_ReturnsCSVLine(){
        PlainTextFileLogger plain = new PlainTextFileLogger(null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
                .putExtra("satellites",7)
                .putExtra("SATELLITES_FIX",22)
                .build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.000000,41.000000,119.000000,9.000000,7,MOCK\n";
        assertThat("CSV line with satellites or SATELLITES_FIX", actual, is(expected));
    }

//    @Test
//    public void getCsvLine_LocationWithExtras_ReturnsCSVLine(){
//        PlainTextFileLogger plain = new PlainTextFileLogger(null);
//        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
//                .putExtra("satellites",7)
//                .putExtra("SATELLITES_FIX",22)
//                .build();
//
//        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
//        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.000000,41.000000,119.000000,9.000000,7,MOCK\n";
//        assertThat("CSV line with satellites or SATELLITES_FIX", actual, is(expected));
//    }



}