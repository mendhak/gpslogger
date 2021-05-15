package com.mendhak.gpslogger.senders.owncloud;

import androidx.test.filters.SmallTest;
import com.mendhak.gpslogger.common.PreferenceHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OwnCloudManagerTest {

    @Test
    public void IsAvailable_WithValidValues_IsAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        OwnCloudManager ocm = new OwnCloudManager(pm);

        assertThat("Default state is false", ocm.isAvailable(), is(false));

        when(pm.getOwnCloudServerName()).thenReturn("");
        assertThat("Server name should not be empty", ocm.isAvailable(), is(false));

        when(pm.getOwnCloudServerName()).thenReturn("sadfasdf");
        assertThat("Server name should not be empty", ocm.isAvailable(), is(true));
    }

    @Test
    public void IsAutoSendAvailable_WhenUserCheckedPreference_ThenAvailable(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        OwnCloudManager ocm = new OwnCloudManager(pm);

        when(pm.getOwnCloudServerName()).thenReturn("sadfasdf");
        assertThat("Valid but unchecked - not available", ocm.isAutoSendAvailable(), is(false));

        when(pm.isOwnCloudAutoSendEnabled()).thenReturn(true);
        assertThat("Valid and checked - available", ocm.isAutoSendAvailable(), is(true));
    }
}