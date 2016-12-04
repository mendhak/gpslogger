package com.mendhak.gpslogger.common;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LocationsTest {

    @Test
    public void AdjustLocationAltitude_WhenNoAltitude_NothingAdjusted(){
        Location loc = MockLocations.builder("MOCK", 12.193,19.111).build();

        PreferenceHelper ph = mock(PreferenceHelper.class);
        Location actual = Locations.getLocationWithAdjustedAltitude(loc, ph);

        assertThat("Location without altitude is not adjusted", actual.hasAltitude(), is(actual.hasAltitude()));
        assertThat("Location without altitude is not adjusted", actual.hasAltitude(), is(false));
        verify(loc, times(0)).setAltitude(anyDouble());
    }

    @Test
    public void AdjustLocationAltitude_WhenAltitudePresentNoPreferences_NothingAdjusted(){

        PreferenceHelper ph = mock(PreferenceHelper.class);
        when(ph.shouldAdjustAltitudeFromGeoIdHeight()).thenReturn(true);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111).withAltitude(100).build();

        Location actual = Locations.getLocationWithAdjustedAltitude(loc, ph);

        assertThat("Location with altitude but no preference not adjusted", actual.getAltitude(), is(100d));
        verify(loc, times(0)).setAltitude(anyDouble());
    }

    @Test
    public void AdjustLocationAltitude_AdjustAltitudeFromGeoIdHeight_AdjustedLocation() {

        PreferenceHelper ph = mock(PreferenceHelper.class);
        when(ph.shouldAdjustAltitudeFromGeoIdHeight()).thenReturn(true);

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111).withAltitude(100).putExtra("GEOIDHEIGHT", "15").build();
        Location actual = Locations.getLocationWithAdjustedAltitude(loc, ph);
        verify(loc, times(1)).setAltitude(85);
    }
    @Test
    public void AdjustLocationAltitude_GeoIdHeightNotPresent_RemoveAltitude(){

        PreferenceHelper ph = mock(PreferenceHelper.class);
        when(ph.shouldAdjustAltitudeFromGeoIdHeight()).thenReturn(true);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111).withAltitude(100).putExtra("a","b").build();
        Location actual = Locations.getLocationWithAdjustedAltitude(loc, ph);
        verify(loc,times(1)).removeAltitude();
    }

    @Test
    public void AdjustLocationAltitude_AdjustAltitudeFromUserOffset_AdjustedLocation(){
        PreferenceHelper ph = mock(PreferenceHelper.class);
        when(ph.shouldAdjustAltitudeFromGeoIdHeight()).thenReturn(false);
        when(ph.getSubtractAltitudeOffset()).thenReturn(20);

        Location loc = MockLocations.builder("MOCK", 12.193,19.111).withAltitude(100).putExtra("GEOIDHEIGHT","15").build();
        Location actual = Locations.getLocationWithAdjustedAltitude(loc,ph);
        verify(loc, times(1)).setAltitude(80);
        verify(loc, times(0)).setAltitude(85);

    }

}