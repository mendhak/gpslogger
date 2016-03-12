package com.mendhak.gpslogger.senders.dropbox;


import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.common.PreferenceHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DropBoxManagerTest {

    @Test
    public void IsAvailable_WhenKeyPresent_ReturnsTrue(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isDropboxAutoSendEnabled()).thenReturn(true);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        assertThat("All values present means is available", dropBoxManager.isAvailable(), is(true));
    }

    @Test
    public void IsAvailable_WithKeyButAutoSendDisabled_ReturnsTrue(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isDropboxAutoSendEnabled()).thenReturn(false);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        assertThat("Allow normal sending even if autosend disabled", dropBoxManager.isAvailable(), is(true));
    }



    @Test
    public void Unlink_WhenCalled_KeyCleared(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        dropBoxManager.unLink();

        verify(pm).setDropBoxAccessKeyName(null);
    }





    @Test
    public void Accept_FileFilter_AcceptsAllFileTypes(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        DropBoxManager dropBoxManager = new DropBoxManager(pm);

        assertThat("Any file type", dropBoxManager.accept(null,null), is(true));
        assertThat("Any file type", dropBoxManager.accept(new File("/"),"abc.xyz"), is(true));
    }


    @Test
    public void IsAutoSendAvailable_WhenUserCheckedAutoSend_IsAvailable(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");
        when(pm.isDropboxAutoSendEnabled()).thenReturn(true);

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        assertThat("User checked preference, auto send available", dropBoxManager.isAutoSendAvailable(), is(true));

        when(pm.isDropboxAutoSendEnabled()).thenReturn(false);
        assertThat("User checked preference, auto send available", dropBoxManager.isAutoSendAvailable(), is(false));

    }

}