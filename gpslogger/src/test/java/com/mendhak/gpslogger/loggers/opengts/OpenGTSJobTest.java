package com.mendhak.gpslogger.loggers.opengts;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OpenGTSJobTest {


    @Test
    public void gprmcEncode_LatLongAccuracy() {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        OpenGTSJob client = new OpenGTSJob("",1,"","","","",new SerializableLocation[]{sloc});
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02"));
    }

    @Test
    public void gprmcEncode_LatLongAccuracyAltitude() {

        Location loc = MockLocations.builder("GPS", 51.35762965, -0.19564124).withTime(1457206433000l).withAccuracy(39).withAltitude(49).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        OpenGTSJob client = new OpenGTSJob("",1,"","","","",new SerializableLocation[]{sloc});
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,193353,A,5121.45778,N,011.73847,W,0.000000,0.000000,050316,,*01"));
    }



    @Test
    public void gprmcEncode_LatLongAccuracyAltitudeSpeed() {

        Location loc = MockLocations.builder("GPS", 51.35762965, -0.19564124).withTime(1457206433000l).withAccuracy(39).withAltitude(49).withSpeed(19).build();
        SerializableLocation sloc = new SerializableLocation(loc);
        OpenGTSJob client = new OpenGTSJob("",1,"","","","",new SerializableLocation[]{sloc});
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,193353,A,5121.45778,N,011.73847,W,36.933045,0.000000,050316,,*3C"));
    }


    @Test
    public void gprmcEncode_LatLongAccuracyAltitudeSpeedBearing() {

        Location loc = MockLocations.builder("GPS", 51.35762965, -0.19564124).withTime(1457206433000l).withAccuracy(39).withAltitude(49).withSpeed(19).withBearing(22).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        OpenGTSJob client = new OpenGTSJob("",1,"","","","",new SerializableLocation[]{sloc});
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,193353,A,5121.45778,N,011.73847,W,36.933045,22.000000,050316,,*0C"));
    }

    @Test
    public void getUrl_BasicLocation() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        OpenGTSJob client = new OpenGTSJob("example.com",9001,"","","","",new SerializableLocation[]{sloc});
        URL url = new URL("http://example.com:9001/?id=99&dev=99&acct=ACCT&batt=0&code=0xF020&alt=0.0&gprmc=$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02");
        assertThat("URL Generated from basic location",  client.getUrl("99","ACCT",sloc).toString() , is(url.toString()));
    }


    @Test
    public void getUrl_LeadingSlashes_Removed() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        OpenGTSJob client = new OpenGTSJob("example.com",9001,"","/xqa","","",new SerializableLocation[]{sloc});
        URL url = new URL("http://example.com:9001/xqa?id=99&dev=99&acct=ACCT&batt=0&code=0xF020&alt=0.0&gprmc=$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02");
        assertThat("URL Generated without extra slashes",  client.getUrl("99","ACCT",sloc).toString() , is(url.toString()));
    }

    @Test
    public void getUrl_IncludesDummyBatteryValue() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        OpenGTSJob client = new OpenGTSJob("example.com",9001,"","/xqa","","",new SerializableLocation[]{sloc});

        assertThat("URL contains battery due to OpenGTS Bug >_<",  client.getUrl("99","ACCT",sloc).toString() , containsString("batt"));
    }

    @Test
    public void getUrl_AccountMissing_UseIdInstead() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        OpenGTSJob client = new OpenGTSJob("example.com",9001,"","","","",new SerializableLocation[]{sloc});
        URL url = new URL("http://example.com:9001/?id=99&dev=99&acct=99&batt=0&code=0xF020&alt=0.0&gprmc=$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02");
        assertThat("Uses id if account name is missing",  client.getUrl("99",null,sloc).toString() , is(url.toString()));
    }

}