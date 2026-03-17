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
public class HttpUploadManagerTest {

    @Test
    public void isAvailable_WhenUrlAndMethodPresent_ReturnsTrue() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getHttpUploadUrl()).thenReturn("https://example.com/upload");
        when(pm.getHttpUploadMethod()).thenReturn("POST");

        HttpUploadManager manager = new HttpUploadManager(pm);
        assertThat("URL and method are required", manager.isAvailable(), is(true));
    }

    @Test
    public void isAvailable_WhenUrlMissing_ReturnsFalse() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getHttpUploadUrl()).thenReturn("");
        when(pm.getHttpUploadMethod()).thenReturn("POST");

        HttpUploadManager manager = new HttpUploadManager(pm);
        assertThat("URL is required", manager.isAvailable(), is(false));
    }

    @Test
    public void isAvailable_WhenMethodMissing_ReturnsFalse() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getHttpUploadUrl()).thenReturn("https://example.com/upload");
        when(pm.getHttpUploadMethod()).thenReturn("");

        HttpUploadManager manager = new HttpUploadManager(pm);
        assertThat("HTTP method is required", manager.isAvailable(), is(false));
    }

    @Test
    public void hasUserAllowedAutoSending_UsesPreferenceFlag() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isHttpUploadAutoSendEnabled()).thenReturn(true);

        HttpUploadManager manager = new HttpUploadManager(pm);
        assertThat("Autosend preference should drive behavior", manager.hasUserAllowedAutoSending(), is(true));
    }

    @Test
    public void getName_ReturnsHttpUploadSenderName() {
        HttpUploadManager manager = new HttpUploadManager(mock(PreferenceHelper.class));
        assertThat("Sender name should be HTTP upload", manager.getName(), is(FileSender.SenderNames.HTTPUPLOAD));
    }

    @Test
    public void accept_AnyFileName_ReturnsTrue() {
        HttpUploadManager manager = new HttpUploadManager(mock(PreferenceHelper.class));
        assertThat("HTTP upload accepts any log file type", manager.accept(null, "anything.txt"), is(true));
    }
}
