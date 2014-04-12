package com.mendhak.gpslogger;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;

import java.util.Date;


public class UtilitiesTests extends AndroidTestCase {

    Context context;

    public void setUp() {
        this.context = new UtilitiesMockContext(getContext());
        Utilities.PopulateAppSettings(context);
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
    }


    public void testHTMLDecoder(){


        String actual = Utilities.HtmlDecode("Bert &amp; Ernie are here. They wish to &quot;talk&quot; to you.");
        String expected = "Bert & Ernie are here. They wish to \"talk\" to you.";
        assertEquals("HTML Decode did not decode everything", expected, actual);

        actual = Utilities.HtmlDecode(null);
        expected = null;

        assertEquals("HTML Decode should handle null input", expected, actual);

    }

    public void testDropBoxSetup() {

        assertFalse("Dropbox defaults to not set up.", Utilities.IsDropBoxSetup(context));

        final SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferencesEditor.putString(DropBoxHelper.ACCESS_KEY_NAME, "12345");
        preferencesEditor.putString(DropBoxHelper.ACCESS_SECRET_NAME, "abcdef");
        preferencesEditor.commit();
        assertEquals("Dropbox setup if KEY and SECRET exist in preferences", true, Utilities.IsDropBoxSetup(context));
    }

    public void testIsoDateTime() {

        String actual = Utilities.GetIsoDateTime(new Date(1417726140000l));
        String expected = "2014-12-04T20:49:00Z";
        assertEquals("Conversion of date to ISO string", expected, actual);
    }

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








}
