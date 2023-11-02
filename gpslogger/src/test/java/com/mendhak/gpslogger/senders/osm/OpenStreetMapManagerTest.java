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
    public void Accept_OnlyGpxAllowed(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        OpenStreetMapManager osm = new OpenStreetMapManager(pm);


        assertThat("Only GPX files allowed", osm.accept(new File("/"), "abc.def"), is(false));

        assertThat("Only GPX files allowed", osm.accept(new File("/"), "abc.gpx"), is(true));

    }

}