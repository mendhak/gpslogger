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
public class CSVFileLoggerTest {

    @Test
    public void getCsvLine_BasicLocation_ReturnsBasicCSV(){
        CSVFileLogger plain = new CSVFileLogger(null,null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,,,,,0,MOCK,,,,,,,,,\n";
        assertThat("Basic CSV line", actual, is(expected));

    }

    @Test
    public void getCsvLine_LocationWithAltitudeAccuracyBearing_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,41.0,119.0,,0,MOCK,,,,,,,,,\n";
        assertThat("CSV line with altitude, accuracy, bearing", actual, is(expected));
    }

    @Test
    public void getCsvLine_LocationWithSpeedProvider_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,null);
        Location loc = MockLocations.builder("BRAINS", 12.193, 19.111).withAltitude(101).withBearing(119).withSpeed(9).build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,,119.0,9.0,0,BRAINS,,,,,,,,,\n";
        assertThat("CSV line with speed and provider", actual, is(expected));
    }


    @Test
    public void getCsvLine_LocationWithSatellites_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
                .putExtra("satellites",7)
                .putExtra("SATELLITES_FIX",22)
                .build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,41.0,119.0,9.0,7,MOCK,,,,,,,,,\n";
        assertThat("CSV line with satellites or SATELLITES_FIX", actual, is(expected));
    }

    @Test
    public void getCsvLine_LocationWithHdopPdopVdop_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
                .putExtra("satellites",7)
                .putExtra("HDOP", "LOOKATTHISHDOP!")
                .build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,41.0,119.0,9.0,7,MOCK,LOOKATTHISHDOP!,,,,,,,,\n";
        assertThat("CSV line with HDOP", actual, is(expected));

        Location loc2 = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
                .putExtra("satellites",7)
                .putExtra("HDOP", "LOOKATTHISHDOP!")
                .putExtra("VDOP", "392.13")
                .putExtra("PDOP", "Papepeodpe")
                .build();

        actual = plain.getCsvLine(loc2,"2011-09-17T18:45:33Z");
        expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,41.0,119.0,9.0,7,MOCK,LOOKATTHISHDOP!,392.13,Papepeodpe,,,,,,\n";
        assertThat("CSV line with HDOP PDOP VDOP", actual, is(expected));
    }

    @Test
    public void getCsvLine_LocationWithGeoIdDgps_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,null);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
                .putExtra("satellites",7)
                .putExtra("GEOIDHEIGHT","tall")
                .putExtra("AGEOFDGPSDATA", "oldddd")
                .putExtra("DGPSID","777")
                .build();


        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,41.0,119.0,9.0,7,MOCK,,,,tall,oldddd,777,,,\n";
        assertThat("CSV line with geoid and dgps", actual, is(expected));

        Location loc2 = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
                .putExtra("satellites",7)
                .putExtra("HDOP", "LOOKATTHISHDOP!")
                .putExtra("VDOP", "392.13")
                .putExtra("PDOP", "Papepeodpe")
                .putExtra("GEOIDHEIGHT","tall")
                .putExtra("AGEOFDGPSDATA", "oldddd")
                .putExtra("DGPSID","777")
                .build();

        actual = plain.getCsvLine(loc2,"2011-09-17T18:45:33Z");
        expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,41.0,119.0,9.0,7,MOCK,LOOKATTHISHDOP!,392.13,Papepeodpe,tall,oldddd,777,,,\n";
        assertThat("CSV line with all extras", actual, is(expected));
    }

    @Test
    public void getCsvLine_LocationWithActivity_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,null);
        Location loc = MockLocations.builder("MOCK", 12.222,14.151).putExtra("DETECTED_ACTIVITY","WALKING").build();

        String actual = plain.getCsvLine(loc,"2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.222000,14.151000,,,,,0,MOCK,,,,,,,WALKING,,\n";

        assertThat("CSV with activity", actual, is(expected));

        Location loc2 = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(101).withAccuracy(41).withBearing(119).withSpeed(9)
                .putExtra("satellites",7)
                .putExtra("HDOP", "LOOKATTHISHDOP!")
                .putExtra("VDOP", "392.13")
                .putExtra("PDOP", "Papepeodpe")
                .putExtra("GEOIDHEIGHT","tall")
                .putExtra("AGEOFDGPSDATA", "oldddd")
                .putExtra("DGPSID","777")
                .putExtra("DETECTED_ACTIVITY","UNKNOWN")
                .build();

        actual = plain.getCsvLine(loc2,"2011-09-17T18:45:33Z");
        expected = "2011-09-17T18:45:33Z,12.193000,19.111000,101.0,41.0,119.0,9.0,7,MOCK,LOOKATTHISHDOP!,392.13,Papepeodpe,tall,oldddd,777,UNKNOWN,,\n";
        assertThat("CSV with activity", actual, is(expected));

    }


    @Test
    public void getCsvLine_LocationWithBattery_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,47.238f);
        Location loc = MockLocations.builder("MOCK", 12.222,14.151).build();

        String actual = plain.getCsvLine(loc, "2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.222000,14.151000,,,,,0,MOCK,,,,,,,,47.238,\n";

        assertThat("CSV With battery", actual, is(expected));

    }


    @Test
    public void getCsvLine_LocationWithAnnotation_ReturnsCSVLine(){
        CSVFileLogger plain = new CSVFileLogger(null,47.238f);
        Location loc = MockLocations.builder("MOCK", 12.222,14.151).build();

        String actual = plain.getCsvLine("はい", loc, "2011-09-17T18:45:33Z");
        String expected = "2011-09-17T18:45:33Z,12.222000,14.151000,,,,,0,MOCK,,,,,,,,47.238,\"はい\"\n";

        assertThat("CSV With annotation", actual, is(expected));

    }





}