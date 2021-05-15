package com.mendhak.gpslogger.loggers;

import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;



@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class FilesTest {



    @Test
    public void GetFilesInFolder_WhenNullOrEmpty_ReturnEmptyList() {
        assertThat("Null File object should return empty list", Files.fromFolder(null), notNullValue());

        assertThat("Empty folder should return empty list", Files.fromFolder(new File("/")), notNullValue());

    }

}