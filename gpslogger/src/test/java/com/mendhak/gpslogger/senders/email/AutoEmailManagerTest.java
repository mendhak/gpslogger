package com.mendhak.gpslogger.senders.email;

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
public class AutoEmailManagerTest {

    @Test
    public void IsAvailable_WhenAllValuesPresent_ReturnsTrue(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getAutoEmailTargets()).thenReturn("aaaaaa");
        when(pm.getSmtpServer()).thenReturn("bbbbbbb");
        when(pm.getSmtpPort()).thenReturn("bbbbbbb");
        when(pm.getSmtpUsername()).thenReturn("bbbbbbb");

        AutoEmailManager aem = new AutoEmailManager(pm);
        assertThat("All values present means is available", aem.isAvailable(), is(true));
    }

    @Test
    public void IsAvailable_AllValuesButAutoSendUnchecked_ReturnsTrue(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getAutoEmailTargets()).thenReturn("aaaaaa");
        when(pm.getSmtpServer()).thenReturn("bbbbbbb");
        when(pm.getSmtpPort()).thenReturn("bbbbbbb");
        when(pm.getSmtpUsername()).thenReturn("bbbbbbb");

        AutoEmailManager aem = new AutoEmailManager(pm);
        assertThat("All values present but auto-send unchecked, still available", aem.isAvailable(), is(true));
    }


    @Test
    public void IsAvailable_WhenSomeValuesMissing_ReturnsFalse(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getAutoEmailTargets()).thenReturn("aaaaaa");
        when(pm.getSmtpServer()).thenReturn("");
        when(pm.getSmtpPort()).thenReturn("bbbbbbb");
        when(pm.getSmtpUsername()).thenReturn("bbbbbbb");

        AutoEmailManager aem = new AutoEmailManager(pm);
        assertThat("Some values missing indicates invalid email settings", aem.isAvailable(), is(false));
    }

    @Test
    public void IsAutoSendAvailable_WhenAutoSendEnabled_ReturnsTrue(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isEmailAutoSendEnabled()).thenReturn(true);
        when(pm.getAutoEmailTargets()).thenReturn("aaaaaa");
        when(pm.getSmtpServer()).thenReturn("bbbbbbb");
        when(pm.getSmtpPort()).thenReturn("bbbbbbb");
        when(pm.getSmtpUsername()).thenReturn("bbbbbbb");

        AutoEmailManager aem = new AutoEmailManager(pm);
        assertThat("User checked auto send means allow autosending", aem.isAutoSendAvailable(), is(true));
    }



}