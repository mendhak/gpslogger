package com.mendhak.gpslogger.senders.googledrive;


import androidx.test.filters.SmallTest;
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
public class GoogleDriveManagerTest {


    @Test
    public void IsAvailable_AccountAndToken_IsAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getGoogleDriveAccountName()).thenReturn("XXXXXXX");
        when(pm.getGoogleDriveAuthToken()).thenReturn("YYYYYYYYYYY");
        when(pm.isGDocsAutoSendEnabled()).thenReturn(false);

        GoogleDriveManager gdm = new GoogleDriveManager(pm);
        assertThat("Account and token indicate availability", gdm.isAvailable(), is(true));
    }

    @Test
    public void IsAutoSendAvailable_UserCheckedAutosend_IsAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getGoogleDriveAccountName()).thenReturn("XXXXXXX");
        when(pm.getGoogleDriveAuthToken()).thenReturn("YYYYYYYYYYY");
        when(pm.isGDocsAutoSendEnabled()).thenReturn(true);

        GoogleDriveManager gdm = new GoogleDriveManager(pm);
        assertThat("Account and token indicate availability", gdm.isAutoSendAvailable(), is(true));
    }




}