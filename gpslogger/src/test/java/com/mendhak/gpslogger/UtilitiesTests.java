package com.mendhak.gpslogger;


import android.os.Build;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.format.Time;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.nmea.NmeaSentence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


import java.io.File;
import java.util.Date;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class UtilitiesTests  {





    @Test
    public void testHTMLDecoder(){


        String actual = Utilities.HtmlDecode("Bert &amp; Ernie are here. They wish to &quot;talk&quot; to you.");
        String expected = "Bert & Ernie are here. They wish to \"talk\" to you.";
        assertThat("HTML Decode did not decode everything", actual, is(expected));

        actual = Utilities.HtmlDecode(null);
        expected = null;

        assertThat("HTML Decode should handle null input", actual, is(expected));

    }



    @Test
    public void testIsoDateTime() {

        String actual = Utilities.GetIsoDateTime(new Date(1417726140000l));
        String expected = "2014-12-04T20:49:00Z";
        assertThat("Conversion of date to ISO string", actual, is(expected));
    }

    @Test
    public void testCleanDescription() {
        String content = "This is some annotation that will end up in an " +
                "XML file.  It will either <b>break</b> or Bert & Ernie will show up" +
                "and cause all sorts of mayhem. Either way, it won't \"work\"";

        String expected = "This is some annotation that will end up in an " +
                "XML file.  It will either bbreak/b or Bert &amp; Ernie will show up" +
                "and cause all sorts of mayhem. Either way, it won't &quot;work&quot;";

        String actual = Utilities.CleanDescription(content);

        assertThat("Clean Description should remove characters", actual, is(expected));
    }

    @Test
    public void testFolderListFiles() {
        assertThat("Null File object should return empty list", Utilities.GetFilesInFolder(null), notNullValue());

        assertThat("Empty folder should return empty list", Utilities.GetFilesInFolder(new File("/")), notNullValue());

    }

    @Test
    public void testFormattedCustomFileName() {


        String expected = "basename_" + Build.SERIAL;
        String actual = Utilities.GetFormattedCustomFileName("basename_%ser");
        assertThat("Static file name %SER should be replaced with Build Serial", actual, is(expected));

        Time t = new Time();
        t.setToNow();

        expected = "basename_" +  String.format("%02d", t.hour);

        actual = Utilities.GetFormattedCustomFileName("basename_%HOUR");
        assertThat("Static file name %HOUR should be replaced with Hour", actual, is(expected));

        actual = Utilities.GetFormattedCustomFileName("basename_%HOUR%MIN");
        expected = "basename_" +  String.format("%02d", t.hour) + String.format("%02d", t.minute);
        assertThat("Static file name %HOUR, %MIN should be replaced with Hour, Minute", actual, is(expected));

        actual = Utilities.GetFormattedCustomFileName("basename_%YEAR%MONTH%DAY");
        expected = "basename_" +  String.valueOf(t.year) + String.format("%02d", t.month+1) + String.format("%02d", t.monthDay);
        assertThat("Static file name %YEAR, %MONTH, %DAY should be replaced with Year Month and Day", actual, is(expected));

    }


}
