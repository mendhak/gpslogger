package com.mendhak.gpslogger.senders.sftp;

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
public class SFTPManagerTest {

    @Test
    public void IsAvailable_WhenBasicValuesPresent_ReturnsTrue() {
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getSFTPHost()).thenReturn("aaa");
        when(pm.getSFTPPort()).thenReturn(99);
        when(pm.getSFTPPassword()).thenReturn("hunter2");
        when(pm.getSFTPRemoteServerPath()).thenReturn("/tmp");

        SFTPManager manager = new SFTPManager(pm);
        assertThat("Basic values means is available", manager.isAvailable(), is(true));
    }

    @Test
    public void IsAvailable_WhenHostMissting_Invalid(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
            when(pm.getSFTPPort()).thenReturn(99);
        when(pm.getSFTPPassword()).thenReturn("hunter2");
        when(pm.getSFTPRemoteServerPath()).thenReturn("/tmp");

        SFTPManager manager = new SFTPManager(pm);
        assertThat("Host value required", manager.isAvailable(), is(false));
    }


    @Test
    public void IsAvailable_WhenRemoteDirMissting_Invalid(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getSFTPHost()).thenReturn("aaa");
        when(pm.getSFTPPort()).thenReturn(99);
        when(pm.getSFTPPassword()).thenReturn("hunter2");

        SFTPManager manager = new SFTPManager(pm);
        assertThat("Remote dir value required", manager.isAvailable(), is(false));
    }

    @Test
    public void IsAvailable_WhenPasswordOrPrivateKeyMissing_Invalid(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getSFTPHost()).thenReturn("aaa");
        when(pm.getSFTPPort()).thenReturn(99);
        when(pm.getSFTPRemoteServerPath()).thenReturn("/tmp");

        SFTPManager manager = new SFTPManager(pm);
        assertThat("Either password or private key required", manager.isAvailable(), is(false));

        when(pm.getSFTPPrivateKeyFilePath()).thenReturn("/home/user/id_rsa");
        assertThat("Private key will suffice in the absence of a password", manager.isAvailable(), is(true));
    }

    @Test
    public void IsAvailable_WhenPortNumberInvalid_Invalid(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getSFTPHost()).thenReturn("aaa");
        when(pm.getSFTPPort()).thenReturn(0);
        when(pm.getSFTPPassword()).thenReturn("hunter2");
        when(pm.getSFTPRemoteServerPath()).thenReturn("/tmp");

        SFTPManager manager = new SFTPManager(pm);
        assertThat("Valid port required", manager.isAvailable(), is(false));

        when(pm.getSFTPPort()).thenReturn(-100);
        assertThat("Valid port required", manager.isAvailable(), is(false));

        when(pm.getSFTPPort()).thenReturn(2999);
        assertThat("Port is valid", manager.isAvailable(), is(true));
    }
}
