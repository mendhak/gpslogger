package com.mendhak.gpslogger;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import com.mendhak.gpslogger.senders.osm.OSMHelper;

public class GpsSettingsActivity extends PreferenceActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        Preference enableDisablePref = findPreference("enableDisableGps");
        enableDisablePref.setOnPreferenceClickListener(new AndroidLocationPreferenceClickListener());

        Preference osmSetupPref = findPreference("osm_setup");
        osmSetupPref.setOnPreferenceClickListener(new OSMPreferenceClickListener());

    }


    /**
     * Opens the Android Location preferences screen
     */
    private class AndroidLocationPreferenceClickListener implements OnPreferenceClickListener
    {
        public boolean onPreferenceClick(Preference preference)
        {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }
    }

    /**
     * Opens the OpenStreetMap preferences screen
     */
    private class OSMPreferenceClickListener implements OnPreferenceClickListener
    {

        public boolean onPreferenceClick(Preference preference)
        {
            startActivity(OSMHelper.GetOsmSettingsIntent(getApplicationContext()));

            return true;
        }
    }

}
