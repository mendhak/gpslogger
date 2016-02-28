package com.mendhak.gpslogger.senders.googledrive;


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
public class GoogleDriveManagerTest {

    @Test
    public void IsAvailable_DefaultValues_NotAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);

        GoogleDriveManager gdm = new GoogleDriveManager(pm);
        assertThat("Default google drive availability", gdm.isAvailable(), is(false));
    }


    @Test
    public void IsAvailable_AccountAndToken_IsAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getGoogleDriveAccountName()).thenReturn("XXXXXXX");
        when(pm.getGoogleDriveAuthToken()).thenReturn("YYYYYYYYYYY");
        when(pm.isGDocsAutoSendEnabled()).thenReturn(true);

        GoogleDriveManager gdm = new GoogleDriveManager(pm);
        assertThat("Account and token indicate availability", gdm.isAvailable(), is(true));
    }


}