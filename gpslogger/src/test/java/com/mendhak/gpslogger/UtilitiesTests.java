package com.mendhak.gpslogger;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.format.Time;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;

import java.io.File;
import java.util.Date;


public class UtilitiesTests extends AndroidTestCase {

    Context context;

    public void setUp() {
        this.context = new UtilitiesMockContext(getContext());
        final SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferencesEditor.clear().commit();
    }

    class UtilitiesMockContext extends MockContext {

        private Context mDelegatedContext;
        private static final String PREFIX = "com.mendhak.gpslogger.";

        public UtilitiesMockContext(Context context) {
            mDelegatedContext = context;
        }

        @Override
        public String getPackageName(){
            return PREFIX;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return mDelegatedContext.getSharedPreferences(name, mode);
        }

        @Override
        public File getExternalFilesDir(String type){
            return new File("/sdcard/GPSLogger");
        }


    }


    @SmallTest
    public void testHTMLDecoder(){


        String actual = Utilities.HtmlDecode("Bert &amp; Ernie are here. They wish to &quot;talk&quot; to you.");
        String expected = "Bert & Ernie are here. They wish to \"talk\" to you.";
        assertEquals("HTML Decode did not decode everything", expected, actual);

        actual = Utilities.HtmlDecode(null);
        expected = null;

        assertEquals("HTML Decode should handle null input", expected, actual);

    }

    @SmallTest
    public void testDropBoxSetup() {

        assertFalse("Dropbox defaults to not set up.", Utilities.IsDropBoxSetup(context));

        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(DropBoxHelper.ACCESS_KEY_NAME, "12345");
        editor.putString(DropBoxHelper.ACCESS_SECRET_NAME, "abcdef");
        editor.apply();
        assertEquals("Dropbox setup if KEY and SECRET exist in preferences", true, Utilities.IsDropBoxSetup(context));
    }

    @SmallTest
    public void testIsoDateTime() {

        String actual = Utilities.GetIsoDateTime(new Date(1417726140000l));
        String expected = "2014-12-04T20:49:00Z";
        assertEquals("Conversion of date to ISO string", expected, actual);
    }

    @SmallTest
    public void testCleanDescription() {
        String content = "This is some annotation that will end up in an " +
                "XML file.  It will either <b>break</b> or Bert & Ernie will show up" +
                "and cause all sorts of mayhem. Either way, it won't \"work\"";

        String expected = "This is some annotation that will end up in an " +
                "XML file.  It will either bbreak/b or Bert &amp; Ernie will show up" +
                "and cause all sorts of mayhem. Either way, it won't &quot;work&quot;";

        String actual = Utilities.CleanDescription(content);

        assertEquals("Clean Description should remove characters", expected, actual);
    }

    @SmallTest
    public void testFolderListFiles() {
        assertNotNull("Null File object should return empty list", Utilities.GetFilesInFolder(null));

        assertNotNull("Empty folder should return empty list", Utilities.GetFilesInFolder(new File("/")));

    }

    @SmallTest
    public void testFormattedCustomFileName() {


        String expected = "basename_" + Build.SERIAL;
        String actual = Utilities.GetFormattedCustomFileName("basename_%ser");
        assertEquals("Static file name %SER should be replaced with Build Serial", expected, actual);

        Time t = new Time();
        t.setToNow();

        expected = "basename_" +  String.valueOf(t.hour);

        actual = Utilities.GetFormattedCustomFileName("basename_%HOUR");
        assertEquals("Static file name %HOUR should be replaced with Hour", expected, actual);

        actual = Utilities.GetFormattedCustomFileName("basename_%HOUR%MIN");
        expected = "basename_" +  String.valueOf(t.hour) + String.valueOf(t.minute);
        assertEquals("Static file name %HOUR, %MIN should be replaced with Hour, Minute", expected, actual);

        actual = Utilities.GetFormattedCustomFileName("basename_%YEAR%MONTH%DAY");
        expected = "basename_" +  String.valueOf(t.year) + String.valueOf(t.month) + String.valueOf(t.monthDay);
        assertEquals("Static file name %YEAR, %MONTH, %DAY should be replaced with Year Month and Day", expected, actual);

    }

    @SmallTest
    public void testNmeaListenerStrings(){
        String expected = "44";
        GeneralLocationListener listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, "blahasdfasdf");
        assertNull("VDOP null by default", listener.latestVdop);

        listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, "$GPGGA,,,,,,,,,,,,,,*47");
        assertNull("Empty NMEA sentence", listener.latestHdop);

        listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, "$GPGGA,,");
        assertNull("Incomplete NMEA string", listener.dgpsId);

        listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, "");
        assertNull("Empty NMEA string", listener.dgpsId);

        listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,27*47");
        assertEquals("GPGGA - read HDOP", "0.9", listener.latestHdop);
        assertEquals("GPGGA - read GeoIdHeight",  "46.9", listener.geoIdHeight);
        assertEquals("GPGGA - read Last dgps update", null,  listener.ageOfDgpsData);
        assertEquals("GPGGA - read dgps station id",  "27", listener.dgpsId);

        listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, "$GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39");
        assertEquals("GPGSA - read PDOP","2.5", listener.latestPdop );
        assertEquals("GPGSA - read HDOP","1.3", listener.latestHdop );
        assertEquals("GPGSA - read VDOP","2.1", listener.latestVdop );

        listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545");
        assertEquals("GPGGA - Incomplete, read HDOP", "0.9", listener.latestHdop);
        assertEquals("GPGGA - Incomplete, no GeoIdHeight",  null, listener.geoIdHeight);

        listener = new GeneralLocationListener(new GpsLoggingService(), "TEST");
        listener.onNmeaReceived(0, null);
        assertNull("Null NMEA string", listener.latestHdop);

    }

}
