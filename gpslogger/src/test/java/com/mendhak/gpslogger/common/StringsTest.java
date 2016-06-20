package com.mendhak.gpslogger.common;

import android.content.Context;
import android.os.Build;
import android.test.suitebuilder.annotation.SmallTest;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class StringsTest {



    @Test
    public void HtmlDecode_WhenEntitiesPresent_EntitiesAreDecoded(){


        String actual = Strings.htmlDecode("Bert &amp; Ernie are here. They wish to &quot;talk&quot; to you.");
        String expected = "Bert & Ernie are here. They wish to \"talk\" to you.";
        assertThat("HTML Decode did not decode everything", actual, is(expected));

        actual = Strings.htmlDecode(null);
        expected = null;

        assertThat("HTML Decode should handle null input", actual, is(expected));

    }



    @Test
    public void GetIsoDateTime_DateObject_ConvertedToIso() {

        String actual = Strings.getIsoDateTime(new Date(1417726140000l));
        String expected = "2014-12-04T20:49:00Z";
        assertThat("Conversion of date to ISO string", actual, is(expected));
    }

    @Test
    public void CleanDescription_WhenAnnotationHasHtml_HtmlIsRemoved() {
        String content = "This is some annotation that will end up in an " +
                "XML file.  It will either <b>break</b> or Bert & Ernie will alert up" +
                "and cause all sorts of mayhem. Either way, it won't \"work\"";

        String expected = "This is some annotation that will end up in an " +
                "XML file.  It will either bbreak/b or Bert &amp; Ernie will alert up" +
                "and cause all sorts of mayhem. Either way, it won't &quot;work&quot;";

        String actual = Strings.cleanDescription(content);

        assertThat("Clean Description should remove characters", actual, is(expected));
    }


    @Test
    public void GetFormattedCustomFileName_Serial_ReplacedWithBuildSerial() {
        PreferenceHelper ph = mock(PreferenceHelper.class);
        Calendar gc = mock(Calendar.class);

        String expected = "basename_" + Build.SERIAL;
        String actual = Strings.getFormattedCustomFileName("basename_%ser", gc, ph);
        assertThat("Static file name %SER should be replaced with Build Serial", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_Version_ReplacedWithBuildVersion() {

        PreferenceHelper ph = mock(PreferenceHelper.class);
        Calendar gc = mock(Calendar.class);

        String expected = "basename_" + BuildConfig.VERSION_NAME;
        String actual = Strings.getFormattedCustomFileName("basename_%ver", gc,ph);
        assertThat("Static file name %VER should be replaced with Build Version", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_HOUR_ReplaceWithPaddedHour(){

        PreferenceHelper ph = mock(PreferenceHelper.class);
        Calendar gc = mock(Calendar.class);
        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(4);

        String expected = "basename_04";
        String actual = Strings.getFormattedCustomFileName("basename_%HOUR", gc, ph);
        assertThat("%HOUR 4 AM should be replaced with 04", actual, is(expected));

        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(23);
        expected = "basename_23";
        actual = Strings.getFormattedCustomFileName("basename_%HOUR", gc, ph);
        assertThat("%HOUR 11PM should be relpaced with 23", actual, is(expected));

        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(0);
        expected = "basename_00";
        actual = Strings.getFormattedCustomFileName("basename_%HOUR", gc, ph);
        assertThat("%HOUR 0 should be relpaced with 00", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_MIN_ReplaceWithPaddedMinute(){

        PreferenceHelper ph = mock(PreferenceHelper.class);
        Calendar gc = mock(Calendar.class);
        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(4);
        when(gc.get(Calendar.MINUTE)).thenReturn(7);

        String actual = Strings.getFormattedCustomFileName("basename_%HOUR%MIN", gc, ph);
        String expected = "basename_0407";
        assertThat(" %MIN 7 should be replaced with 07", actual, is(expected));

        when(gc.get(Calendar.HOUR_OF_DAY)).thenReturn(0);
        when(gc.get(Calendar.MINUTE)).thenReturn(0);

        actual = Strings.getFormattedCustomFileName("basename_%HOUR%MIN", gc, ph);
        expected = "basename_0000";
        assertThat(" %MIN 0 should be replaced with 00", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_YEARMONDAY_ReplaceWithYearMonthDay(){

        PreferenceHelper ph = mock(PreferenceHelper.class);
        Calendar gc = mock(Calendar.class);
        when(gc.get(Calendar.YEAR)).thenReturn(2016);
        when(gc.get(Calendar.MONTH)).thenReturn(Calendar.FEBRUARY);
        when(gc.get(Calendar.DAY_OF_MONTH)).thenReturn(1);

        String actual = Strings.getFormattedCustomFileName("basename_%YEAR%MONTH%DAY", gc, ph);
        String expected = "basename_20160201";
        assertThat("Year 2016 Month February Day 1 should be replaced with 20160301", actual, is(expected));


        when(gc.get(Calendar.MONTH)).thenReturn(Calendar.DECEMBER);
        actual = Strings.getFormattedCustomFileName("basename_%YEAR%MONTH%DAY", gc, ph);
        expected = "basename_20161201";
        assertThat("December month should be replaced with 12", actual, is(expected));

        when(gc.get(Calendar.MONTH)).thenReturn(0);
        actual = Strings.getFormattedCustomFileName("basename_%YEAR%MONTH%DAY", gc, ph);
        expected = "basename_20160101";
        assertThat("Zero month should be replaced with 1", actual, is(expected));

    }

    @Test
    public void GetFormattedCustomFileName_PROFILE_ReplaceWithProfileName(){

        PreferenceHelper ph = mock(PreferenceHelper.class);
        Calendar gc = mock(Calendar.class);
        when(ph.getCurrentProfileName()).thenReturn("OOMPALOOMPA");

        String actual = Strings.getFormattedCustomFileName("basename_%PROFILE",gc, ph);
        String expected = "basename_OOMPALOOMPA";

        assertThat("Profile replaced with profile name", actual, is(expected));

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

        String actual = Strings.getDescriptiveDurationString(0, GetDescriptiveTimeString_Context());
        String expected = "";
        assertThat("0 seconds is empty string", actual, is(expected));
    }

    @Test
    public void GetDescriptiveTimeString_1800Seconds_ReturnsHalfHourString(){
        String actual = Strings.getDescriptiveDurationString(1800, GetDescriptiveTimeString_Context());
        String expected = "½ hour";
        assertThat("1800 seconds returns half hour", actual, is(expected));
    }

    @Test
    public void GetDescriptiveTimeString_2700Seconds_Returns45minutesString(){
        String actual = Strings.getDescriptiveDurationString(2700, GetDescriptiveTimeString_Context());
        String expected = "0h 45m 0s";
        assertThat("2700 seconds returns 45 minutes", actual, is(expected));
    }

    @Test
    public void GetDescriptiveTimeString_9001Seconds_ReturnsCorrespondingHours(){
        String actual = Strings.getDescriptiveDurationString(9001, GetDescriptiveTimeString_Context());
        String expected = "2h 30m 1s";
        assertThat("9001 seconds returns correspnding time", actual, is(expected));
    }


}