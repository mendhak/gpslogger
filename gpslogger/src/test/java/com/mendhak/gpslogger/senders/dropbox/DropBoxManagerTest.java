package com.mendhak.gpslogger.senders.dropbox;


import android.support.v4.util.Pair;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.common.PreferenceHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DropBoxManagerTest {

    @Test
    public void IsAvailable_WhenAllValuesPresent_ReturnsTrue(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isDropboxAutoSendEnabled()).thenReturn(true);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");
        when(pm.getDropBoxAccessSecretName()).thenReturn("bbbbbbb");

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        assertThat("All values present means is available", dropBoxManager.IsAvailable(), is(true));
    }

    @Test
    public void IsAvailable_WhenValueMissing_ReturnsFalse(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.isDropboxAutoSendEnabled()).thenReturn(true);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        assertThat("A key without a secret is useless", dropBoxManager.IsAvailable(), is(false));
    }

    @Test
    public void IsAvailable_WhenEnabledUnchecked_ReturnsFalse(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");
        when(pm.getDropBoxAccessSecretName()).thenReturn("bbbbbbb");

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        assertThat("Keys and secret without enabled means not available", dropBoxManager.IsAvailable(), is(false));

        //verify(pm).setDropBoxAccessKeyName();
    }

    @Test
    public void Unlink_WhenCalled_BothKeysCleared(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");
        when(pm.getDropBoxAccessSecretName()).thenReturn("bbbbbbb");

        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        dropBoxManager.UnLink();

        verify(pm).setDropBoxAccessKeyName(null);
        verify(pm).setDropBoxAccessSecret(null);
    }

    @Test
    public void GetKeys_WhenOneValueNotPresent_ReturnsNull(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");

        assertThat("Keys are null", dropBoxManager.getKeys(), is((Pair) null));

        when(pm.getDropBoxAccessKeyName()).thenReturn("");
        when(pm.getDropBoxAccessSecretName()).thenReturn("");

        assertThat("Keys are null", dropBoxManager.getKeys(), is((Pair) null));
    }

    @Test
    public void GetKeys_WhenBothValuesPresent_ReturnsKeys(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        DropBoxManager dropBoxManager = new DropBoxManager(pm);
        when(pm.getDropBoxAccessKeyName()).thenReturn("aaaaaa");
        when(pm.getDropBoxAccessSecretName()).thenReturn("bbbbbbb");

        assertThat("Keys are present", dropBoxManager.getKeys().first, is("aaaaaa"));
        assertThat("Keys are present", dropBoxManager.getKeys().second, is("bbbbbbb"));
    }

    @Test
    public void Accept_FileFilter_AcceptsAllFileTypes(){
        PreferenceHelper pm = mock(PreferenceHelper.class);
        DropBoxManager dropBoxManager = new DropBoxManager(pm);

        assertThat("Any file type", dropBoxManager.accept(null,null), is(true));
        assertThat("Any file type", dropBoxManager.accept(new File("/"),"abc.xyz"), is(true));
    }

}