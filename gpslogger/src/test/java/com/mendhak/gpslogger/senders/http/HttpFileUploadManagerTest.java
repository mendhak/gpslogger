package com.mendhak.gpslogger.senders.http;

import androidx.test.filters.SmallTest;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.senders.FileSender;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class HttpFileUploadManagerTest {

    @Test
    public void isAvailable_WhenUrlAndMethodPresent_ReturnsTrue() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getHttpFileUploadUrl()).thenReturn("https://example.com/upload");
        when(pm.getHttpFileUploadMethod()).thenReturn("POST");

        HttpFileUploadManager manager = new HttpFileUploadManager(pm);
        assertThat("URL and method are required", manager.isAvailable(), is(true));
    }

    @Test
    public void isAvailable_WhenUrlMissing_ReturnsFalse() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getHttpFileUploadUrl()).thenReturn("");

        HttpFileUploadManager manager = new HttpFileUploadManager(pm);
        assertThat("URL is required", manager.isAvailable(), is(false));
    }

    @Test
    public void isAvailable_WhenMethodMissing_ReturnsFalse() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getHttpFileUploadUrl()).thenReturn("https://example.com/upload");
        when(pm.getHttpFileUploadMethod()).thenReturn("");

        HttpFileUploadManager manager = new HttpFileUploadManager(pm);
        assertThat("HTTP method is required", manager.isAvailable(), is(false));
    }

    @Test
    public void hasUserAllowedAutoSending_UsesPreferenceFlag() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isHttpFileUploadAutoSendEnabled()).thenReturn(true);

        HttpFileUploadManager manager = new HttpFileUploadManager(pm);
        assertThat("Autosend preference should drive behavior", manager.hasUserAllowedAutoSending(), is(true));
    }

    @Test
    public void getName_ReturnsHttpFileUploadSenderName() {
        HttpFileUploadManager manager = new HttpFileUploadManager(mock(PreferenceHelper.class));
        assertThat("Sender name should be HTTP File Upload", manager.getName(), is(FileSender.SenderNames.HTTPFILEUPLOAD));
    }

    @Test
    public void accept_AnyFileName_ReturnsTrue() {
        HttpFileUploadManager manager = new HttpFileUploadManager(mock(PreferenceHelper.class));
        assertThat("HTTP File Upload accepts any log file type", manager.accept(null, "anything.txt"), is(true));
    }
}
