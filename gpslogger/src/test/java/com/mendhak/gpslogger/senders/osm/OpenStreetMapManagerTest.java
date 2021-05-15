package com.mendhak.gpslogger.senders.osm;


import androidx.test.filters.SmallTest;
import com.mendhak.gpslogger.common.PreferenceHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OpenStreetMapManagerTest {

    @Test
    public void IsAvailable_WithValidValues_IsAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenStreetMapManager osm = new OpenStreetMapManager(pm);
        assertThat("Account and token indicate availability", osm.isAvailable(), is(false));

        when(pm.getOSMAccessToken()).thenReturn("");
        assertThat("Account and token indicate availability", osm.isAvailable(), is(false));

        when(pm.getOSMAccessToken()).thenReturn("923487234");
        assertThat("Account and token indicate availability", osm.isAvailable(), is(true));
    }

    @Test
    public void IsAutosendAvailable_WhenUserChecked_IsAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenStreetMapManager osm = new OpenStreetMapManager(pm);
        when(pm.getOSMAccessToken()).thenReturn("923487234");

        assertThat("Autosend available when user checked preference", osm.isAutoSendAvailable(), is(false));

        when(pm.isOsmAutoSendEnabled()).thenReturn(true);
        assertThat("Autosend available when user checked preference", osm.isAutoSendAvailable(), is(true));
    }

    @Test
    public void Accept_OnlyGpxAllowed(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenStreetMapManager osm = new OpenStreetMapManager(pm);


        assertThat("Only GPX files allowed", osm.accept(new File("/"), "abc.def"), is(false));

        assertThat("Only GPX files allowed", osm.accept(new File("/"), "abc.gpx"), is(true));

    }

}