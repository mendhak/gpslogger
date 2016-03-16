package com.mendhak.gpslogger.senders.opengts;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OpenGTSManagerTest {



    @Test
    public void IsAvailable_WhenAllValuesPresent_True(){
        PreferenceHelper pm = mock(PreferenceHelper.class);

        OpenGTSManager manager = new OpenGTSManager(pm);
        assertThat("Default state is unavailable", manager.isAvailable(), is(false));

        when(pm.getOpenGTSServer()).thenReturn("XXXXXXXXXXXX");
        when(pm.getOpenGTSServerPort()).thenReturn("9001");
        when(pm.getOpenGTSServerCommunicationMethod()).thenReturn("UDPTLSSSLSSH");
        when(pm.getOpenGTSDeviceId()).thenReturn("99");

        assertThat("With values, it becomes available", manager.isAvailable(), is(true));
    }

    @Test
    public void IsAutoSendAvailable_WhenUserCheckedPreference_True(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getOpenGTSServer()).thenReturn("XXXXXXXXXXXX");
        when(pm.getOpenGTSServerPort()).thenReturn("9001");
        when(pm.getOpenGTSServerCommunicationMethod()).thenReturn("UDPTLSSSLSSH");
        when(pm.getOpenGTSDeviceId()).thenReturn("99");
        when(pm.isOpenGtsAutoSendEnabled()).thenReturn(true);

        OpenGTSManager manager = new OpenGTSManager(pm);
        assertThat("Only available if user checked the preference", manager.isAutoSendAvailable(), is(true));
    }


    @Test
    public void IsAutoSendAvailable_WhenPortIsNotNumberic_False(){
        PreferenceHelper pm = mock(PreferenceHelper.class);

        OpenGTSManager manager = new OpenGTSManager(pm);
        assertThat("Default state is unavailable", manager.isAvailable(), is(false));

        when(pm.getOpenGTSServer()).thenReturn("XXXXXXXXXXXX");
        when(pm.getOpenGTSServerPort()).thenReturn("aaaaaaaaaaaaa");
        when(pm.getOpenGTSServerCommunicationMethod()).thenReturn("UDPTLSSSLSSH");
        when(pm.getOpenGTSDeviceId()).thenReturn("99");

        assertThat("When port is non numeric, validation fails", manager.isAvailable(), is(false));
    }

    @Test
    public void gprmcEncode_LatLongAccuracy() {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02"));
    }

    @Test
    public void gprmcEncode_LatLongAccuracyAltitude() {

        Location loc = MockLocations.builder("GPS", 51.35762965, -0.19564124).withTime(1457206433000l).withAccuracy(39).withAltitude(49).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,193353,A,5121.45778,N,011.73847,W,0.000000,0.000000,050316,,*01"));
    }



    @Test
    public void gprmcEncode_LatLongAccuracyAltitudeSpeed() {

        Location loc = MockLocations.builder("GPS", 51.35762965, -0.19564124).withTime(1457206433000l).withAccuracy(39).withAltitude(49).withSpeed(19).build();
        SerializableLocation sloc = new SerializableLocation(loc);
        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,193353,A,5121.45778,N,011.73847,W,36.933045,0.000000,050316,,*3C"));
    }


    @Test
    public void gprmcEncode_LatLongAccuracyAltitudeSpeedBearing() {

        Location loc = MockLocations.builder("GPS", 51.35762965, -0.19564124).withTime(1457206433000l).withAccuracy(39).withAltitude(49).withSpeed(19).withBearing(22).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        assertThat("GPRMC generated",  client.gprmcEncode(sloc)  , is("$GPRMC,193353,A,5121.45778,N,011.73847,W,36.933045,22.000000,050316,,*0C"));
    }

    @Test
    public void getUrl_BasicLocation() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        URL url = new URL("http://example.com:9001/?id=99&dev=99&acct=ACCT&batt=0&code=0xF020&alt=0.0&gprmc=$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02");
        assertThat("URL Generated from basic location",  OpenGTSManager.getUrl("99","ACCT",sloc,"http","","example.com",9001).toString() , is(url.toString()));
    }


    @Test
    public void getUrl_LeadingSlashes_Removed() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        URL url = new URL("http://example.com:9001/xqa?id=99&dev=99&acct=ACCT&batt=0&code=0xF020&alt=0.0&gprmc=$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02");
        assertThat("URL Generated without extra slashes",  OpenGTSManager.getUrl("99","ACCT",sloc,"http","/xqa","example.com",9001).toString() , is(url.toString()));
    }

    @Test
    public void getUrl_IncludesDummyBatteryValue() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);

        assertThat("URL contains battery due to OpenGTS Bug >_<",  OpenGTSManager.getUrl("99","ACCT",sloc,"http","/xqa","example.com",9001).toString() , containsString("batt"));
    }

    @Test
    public void getUrl_AccountMissing_UseIdInstead() throws Exception {

        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        URL url = new URL("http://example.com:9001/?id=99&dev=99&acct=99&batt=0&code=0xF020&alt=0.0&gprmc=$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02");
        assertThat("Uses id if account name is missing",  OpenGTSManager.getUrl("99",null,sloc,"HTTP","","example.com",9001).toString() , is(url.toString()));
    }


    @Test
    public void getUrl_CommunicationIsHttps_ReturnSSLUrl()throws Exception{
        Location loc = MockLocations.builder("GPS", 51.3579941, -0.1952438).withTime(1457205869949l).withAccuracy(20).build();
        SerializableLocation sloc = new SerializableLocation(loc);

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenGTSManager client = new OpenGTSManager(pm);
        URL url = new URL("https://example.com:9001/?id=99&dev=99&acct=99&batt=0&code=0xF020&alt=0.0&gprmc=$GPRMC,192429,A,5121.47965,N,011.71463,W,0.000000,0.000000,050316,,*02");
        assertThat("Uses id if account name is missing",  OpenGTSManager.getUrl("99",null,sloc, "HTTPS", "","example.com",9001).toString() , is(url.toString()));
    }

}