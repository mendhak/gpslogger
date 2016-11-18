package com.mendhak.gpslogger.common;


import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MathsTest {

    @Test
    public void DecimalDegreesToDegreesMinutesSeconds_BasicConversion(){
        double lat = 11.812244d;
        String expected = "11° 48' 44.0784\" N";
        String actual = Maths.DecimalDegreesToDegreesMinutesSeconds(lat, true);

        assertThat("Degree Decimals converted to Degree Minute Seconds", actual, is(expected));
    }

    @Test
    public void DecimalDegreesToDegreesMinutesSeconds_NegativeIsSouth(){
        double lat = -16.44299d;
        String expected = "16° 26' 34.764\" S";
        String actual = Maths.DecimalDegreesToDegreesMinutesSeconds(lat, true);

        assertThat("Negative degree decimals converted to southerly degree minute second", actual, is(expected));
    }

    @Test
    public void DecimalDegreesToDegreesMinutesSeconds_Longitude_ReturnsEastWest(){
        double lon = 17.072754d;
        String expected = "17° 4' 21.9144\" E";
        String actual = Maths.DecimalDegreesToDegreesMinutesSeconds(lon, false);

        assertThat("Longitude values have east west cardinality", actual, is(expected));

        lon = -137.072754;
        expected = "137° 4' 21.9144\" W";
        actual = Maths.DecimalDegreesToDegreesMinutesSeconds(lon, false);

        assertThat("Longitude values have east west cardinality", actual, is(expected));
    }


    @Test
    public void DecimalDegreesToDegreesDecimalMinutes_BasicConversion(){
        double lat = 14.24231d;
        String expected = "14° 14.5386' N";
        String actual = Maths.DecimalDegreesToDegreesDecimalMinutes(lat, true);

        assertThat("Degree Decimals converted to Degree Decimal Minutes", actual, is(expected));
    }

    @Test
    public void DecimalDegreesToDegreesDecimalMinutes_NegativeIsSouth(){
        double lat = -54.81774d;
        String expected = "54° 49.0644' S";
        String actual = Maths.DecimalDegreesToDegreesDecimalMinutes(lat, true);

        assertThat("Negative Degree Decimals converted to southerly Degree Decimal Minutes", actual, is(expected));
    }

    @Test
    public void DecimalDegreesToDegreesDecimalMinutes_Longitude_ReturnsEastWest(){
        double lat = 101.898d;
        String expected = "101° 53.88' E";
        String actual = Maths.DecimalDegreesToDegreesDecimalMinutes(lat, false);

        assertThat("Longitude values have east west cardinality", actual, is(expected));

        lat = -111.111d;
        expected = "111° 6.66' W";
        actual = Maths.DecimalDegreesToDegreesDecimalMinutes(lat, false);

        assertThat("Longitude values have east west cardinality", actual, is(expected));
    }


}
