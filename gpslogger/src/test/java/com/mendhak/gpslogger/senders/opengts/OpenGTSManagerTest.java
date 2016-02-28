package com.mendhak.gpslogger.senders.opengts;

import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.common.PreferenceHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

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


}