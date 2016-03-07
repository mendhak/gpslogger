package com.mendhak.gpslogger.loggers.nmea;

import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NmeaSentenceTest {

    @Test
    public void NmeaSentence_EmptyNmeaSentence_VDOPIsNull(){
        NmeaSentence nmeaSentence = new NmeaSentence("blahasdfasdf");
        assertThat("VDOP null by default", nmeaSentence.getLatestVdop(), nullValue());
    }

    @Test
    public void NmeaSentence_EmptyNmeaSentence_HDOPIsNull(){

        NmeaSentence nmeaSentence = new NmeaSentence("$GPGGA,,,,,,,,,,,,,,*47");
        assertThat("HDOP null by default", nmeaSentence.getLatestHdop(), nullValue());
    }

    @Test
    public void NmeaSentence_EmptyNmeaSentence_DGPSIDIsNull(){

        NmeaSentence nmeaSentence = new NmeaSentence("$GPGGA,,");
        assertThat("DGPSID null by default", nmeaSentence.getDgpsId(), nullValue());

        nmeaSentence = new NmeaSentence("");
        assertThat("DGPSID null by default", nmeaSentence.getDgpsId(), nullValue());
    }

    @Test
    public void NmeaSentence_GPGGA_ReadValidValues(){
        NmeaSentence nmeaSentence = new NmeaSentence("$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,27*47");
        assertThat("GPGGA - read HDOP", nmeaSentence.getLatestHdop(), is("0.9"));
        assertThat("GPGGA - read GeoIdHeight", nmeaSentence.getGeoIdHeight(), is("46.9"));
        assertThat("GPGGA - read Last dgps update", nmeaSentence.getAgeOfDgpsData(), nullValue());
        assertThat("GPGGA - read dgps station id", nmeaSentence.getDgpsId(), is("27"));
    }

    @Test
    public void NmeaSentence_GPGSA_ReadValidValues(){

        NmeaSentence nmeaSentence = new NmeaSentence("$GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39");
        assertThat("GPGSA - read PDOP", nmeaSentence.getLatestPdop(), is("2.5"));
        assertThat("GPGSA - read HDOP", nmeaSentence.getLatestHdop(), is("1.3"));
        assertThat("GPGSA - read VDOP", nmeaSentence.getLatestVdop(), is("2.1"));
    }

    @Test
    public void NmeaSentence_Incomplete_ReadSomeValues(){

        NmeaSentence nmeaSentence = new NmeaSentence("$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545");
        assertThat("GPGGA - Incomplete, read HDOP", nmeaSentence.getLatestHdop(), is("0.9"));
        assertThat("GPGGA - Incomplete, no GeoIdHeight", nmeaSentence.getGeoIdHeight(), nullValue());
    }

    @Test
    public void NmeaSentence_Null_NoValuesRead(){

        NmeaSentence nmeaSentence = new NmeaSentence(null);
        assertThat("Null NMEA string", nmeaSentence.getLatestHdop(), nullValue());
    }

    @Test
    public void NmeaSentence_CheckForRelevantSentence(){

        NmeaSentence nmeaSentence = new NmeaSentence(null);
        assertThat("Null NMEA is not a valid location", nmeaSentence.isLocationSentence(), is(false));

        nmeaSentence = new NmeaSentence("$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545");
        assertThat("GPGGA is a valid location", nmeaSentence.isLocationSentence(), is(true));

        nmeaSentence = new NmeaSentence("$GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39");
        assertThat("GPGSA is a valid location", nmeaSentence.isLocationSentence(), is(true));
    }
}