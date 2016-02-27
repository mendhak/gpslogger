package com.mendhak.gpslogger.common;

import android.content.Context;
import android.os.Build;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class UtilitiesTest {

    @Test
    public void HtmlDecode_WhenEntitiesPresent_EntitiesAreDecoded(){


        String actual = Utilities.HtmlDecode("Bert &amp; Ernie are here. They wish to &quot;talk&quot; to you.");
        String expected = "Bert & Ernie are here. They wish to \"talk\" to you.";
        assertThat("HTML Decode did not decode everything", actual, is(expected));

        actual = Utilities.HtmlDecode(null);
        expected = null;

        assertThat("HTML Decode should handle null input", actual, is(expected));

    }



    @Test
    public void GetIsoDateTime_DateObject_ConvertedToIso() {

        String actual = Utilities.GetIsoDateTime(new Date(1417726140000l));
        String expected = "2014-12-04T20:49:00Z";
        assertThat("Conversion of date to ISO string", actual, is(expected));
    }

    @Test
    public void CleanDescription_WhenAnnotationHasHtml_HtmlIsRemoved() {
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
    public void GetFilesInFolder_WhenNullOrEmpty_ReturnEmptyList() {
        assertThat("Null File object should return empty list", Utilities.GetFilesInFolder(null), notNullValue());

        assertThat("Empty folder should return empty list", Utilities.GetFilesInFolder(new File("/")), notNullValue());

    }

    @Test
    public void GetFormattedCustomFileName_Serial_ReplacedWithBuildSerial() {

        String expected = "basename_" + Build.SERIAL;
        String actual = Utilities.GetFormattedCustomFileName("basename_%ser");
        assertThat("Static file name %SER should be replaced with Build Serial", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_HOUR_ReplaceWithPaddedHour(){

        Calendar gc = mock(Calendar.class);
        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(4);

        String expected = "basename_04";
        String actual = Utilities.GetFormattedCustomFileName("basename_%HOUR", gc);
        assertThat("%HOUR 4 AM should be replaced with 04", actual, is(expected));

        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(23);
        expected = "basename_23";
        actual = Utilities.GetFormattedCustomFileName("basename_%HOUR", gc);
        assertThat("%HOUR 11PM should be relpaced with 23", actual, is(expected));

        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(0);
        expected = "basename_00";
        actual = Utilities.GetFormattedCustomFileName("basename_%HOUR", gc);
        assertThat("%HOUR 0 should be relpaced with 00", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_MIN_ReplaceWithPaddedMinute(){

        Calendar gc = mock(Calendar.class);
        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(4);
        when(gc.get(Calendar.MINUTE)).thenReturn(7);

        String actual = Utilities.GetFormattedCustomFileName("basename_%HOUR%MIN", gc);
        String expected = "basename_0407";
        assertThat(" %MIN 7 should be replaced with 07", actual, is(expected));

        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(0);
        when(gc.get(Calendar.MINUTE)).thenReturn(0);

        actual = Utilities.GetFormattedCustomFileName("basename_%HOUR%MIN", gc);
        expected = "basename_0000";
        assertThat(" %MIN 0 should be replaced with 00", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_YEARMONDAY_ReplaceWithYearMonthDay(){

        Calendar gc = mock(Calendar.class);
        when(gc.get(Calendar.YEAR)).thenReturn(2016);
        when(gc.get(Calendar.MONTH)).thenReturn(Calendar.FEBRUARY);
        when(gc.get(Calendar.DAY_OF_MONTH)).thenReturn(1);

        String actual = Utilities.GetFormattedCustomFileName("basename_%YEAR%MONTH%DAY",gc);
        String expected = "basename_20160201";
        assertThat("Year 2016 Month February Day 1 should be replaced with 20160301", actual, is(expected));


        when(gc.get(Calendar.MONTH)).thenReturn(Calendar.DECEMBER);
        actual = Utilities.GetFormattedCustomFileName("basename_%YEAR%MONTH%DAY",gc);
        expected = "basename_20161201";
        assertThat("December month should be replaced with 12", actual, is(expected));

        when(gc.get(Calendar.MONTH)).thenReturn(0);
        actual = Utilities.GetFormattedCustomFileName("basename_%YEAR%MONTH%DAY",gc);
        expected = "basename_20160101";
        assertThat("Zero month should be replaced with 1", actual, is(expected));

    }

    private Context GetDescriptiveTimeString_Context(){
        Context ctx = mock(Context.class);
        when(ctx.getString(R.string.time_onesecond)).thenReturn("1 second");
        when(ctx.getString(R.string.time_halfminute)).thenReturn("&#189; minute");
        when(ctx.getString(R.string.time_oneminute)).thenReturn("1 minute");
        when(ctx.getString(R.string.time_onehour)).thenReturn("1 hour");
        when(ctx.getString(R.string.time_quarterhour)).thenReturn("15 minutes");
        when(ctx.getString(R.string.time_halfhour)).thenReturn("½ hour");
        when(ctx.getString(R.string.time_oneandhalfhours)).thenReturn("1½ hours");
        when(ctx.getString(R.string.time_twoandhalfhours)).thenReturn("2½ hours");
        when(ctx.getString(R.string.time_hms_format)).thenReturn("%sh %sm %ss");

        return ctx;
    }

    @Test
    public void GetDescriptiveTimeString_ZeroSeconds_ReturnsEmptyString(){

        String actual = Utilities.GetDescriptiveDurationString(0, GetDescriptiveTimeString_Context());
        String expected = "";
        assertThat("0 seconds is empty string", actual, is(expected));
    }

    @Test
    public void GetDescriptiveTimeString_1800Seconds_ReturnsHalfHourString(){
        String actual = Utilities.GetDescriptiveDurationString(1800, GetDescriptiveTimeString_Context());
        String expected = "½ hour";
        assertThat("1800 seconds returns half hour", actual, is(expected));
    }

    @Test
    public void GetDescriptiveTimeString_2700Seconds_Returns45minutesString(){
        String actual = Utilities.GetDescriptiveDurationString(2700, GetDescriptiveTimeString_Context());
        String expected = "0h 45m 0s";
        assertThat("2700 seconds returns 45 minutes", actual, is(expected));
    }

    @Test
    public void GetDescriptiveTimeString_9001Seconds_ReturnsCorrespondingHours(){
        String actual = Utilities.GetDescriptiveDurationString(9001, GetDescriptiveTimeString_Context());
        String expected = "2h 30m 1s";
        assertThat("9001 seconds returns correspnding time", actual, is(expected));
    }

}
