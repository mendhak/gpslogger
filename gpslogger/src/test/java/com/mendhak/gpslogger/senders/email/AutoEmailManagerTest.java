package com.mendhak.gpslogger.senders.email;

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
public class AutoEmailManagerTest {

    @Test
    public void IsAvailable_WhenAllValuesPresent_ReturnsTrue(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isEmailAutoSendEnabled()).thenReturn(true);
        when(pm.getAutoEmailTargets()).thenReturn("aaaaaa");
        when(pm.getSmtpServer()).thenReturn("bbbbbbb");
        when(pm.getSmtpPort()).thenReturn("bbbbbbb");
        when(pm.getSmtpUsername()).thenReturn("bbbbbbb");

        AutoEmailManager aem = new AutoEmailManager(pm);
        assertThat("All values present means is available", aem.IsAvailable(), is(true));
    }

    @Test
    public void IsAvailable_WhenSomeValuesMissing_ReturnsFalse(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isEmailAutoSendEnabled()).thenReturn(true);
        when(pm.getAutoEmailTargets()).thenReturn("aaaaaa");
        when(pm.getSmtpServer()).thenReturn("");
        when(pm.getSmtpPort()).thenReturn("bbbbbbb");
        when(pm.getSmtpUsername()).thenReturn("bbbbbbb");

        AutoEmailManager aem = new AutoEmailManager(pm);
        assertThat("Some values missing", aem.IsAvailable(), is(false));
    }

    @Test
    public void IsAvailable_WhenUserUnchecked_ReturnsFalse(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isEmailAutoSendEnabled()).thenReturn(false);
        when(pm.getAutoEmailTargets()).thenReturn("aaaaaa");
        when(pm.getSmtpServer()).thenReturn("bbbbbbb");
        when(pm.getSmtpPort()).thenReturn("bbbbbbb");
        when(pm.getSmtpUsername()).thenReturn("bbbbbbb");

        AutoEmailManager aem = new AutoEmailManager(pm);
        assertThat("All values present but user unchecked", aem.IsAvailable(), is(false));
    }
}