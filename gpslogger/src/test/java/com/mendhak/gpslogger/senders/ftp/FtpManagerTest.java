package com.mendhak.gpslogger.senders.ftp;


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
public class FtpManagerTest {
    @Test
    public void IsAvailable_WhenAllValuesPresent_ReturnsTrue(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getFtpServerName()).thenReturn("example.com");
        when(pm.getFtpUsername()).thenReturn("aaa");
        when(pm.getFtpPassword()).thenReturn("BBBB");
        when(pm.getFtpPort()).thenReturn(9001);
        when(pm.FtpUseFtps()).thenReturn(true);
        when(pm.getFtpProtocol()).thenReturn("SSL");
        when(pm.FtpImplicit()).thenReturn(false);

        FtpManager aem = new FtpManager(pm);
        assertThat("ALL FTP values indicate valid", aem.isAvailable(), is(true));
    }

    @Test
    public void IsAvailable_ServerNameAndPort_ReturnsTrue(){

        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getFtpServerName()).thenReturn("example.com");


        FtpManager aem = new FtpManager(pm);
        assertThat("Without port, not available", aem.isAvailable(), is(false));

        when(pm.getFtpPort()).thenReturn(9001);
        assertThat("Username with port, available", aem.isAvailable(), is(true));
    }


    @Test
    public void IsAvailable_WhenFTPS_ProtocolRequired(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getFtpServerName()).thenReturn("example.com");
        when(pm.getFtpPort()).thenReturn(9001);
        when(pm.FtpUseFtps()).thenReturn(true);

        FtpManager aem = new FtpManager(pm);

        assertThat("If FTPS specified, a protocol is required", aem.isAvailable(), is(false));

        when(pm.getFtpProtocol()).thenReturn("TLS");
        assertThat("If FTPS specified, a protocol is required", aem.isAvailable(), is(true));
    }





}