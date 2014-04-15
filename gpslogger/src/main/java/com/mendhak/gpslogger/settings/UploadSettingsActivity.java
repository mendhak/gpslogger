package com.mendhak.gpslogger.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.senders.osm.OSMHelper;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@SuppressWarnings("deprecation")
public class UploadSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent intent = new Intent(this, GpsMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_upload);


        Preference osmSetupPref = findPreference("osm_setup");
        osmSetupPref.setOnPreferenceClickListener(this);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equals("osm_setup")){
            startActivity(OSMHelper.GetOsmSettingsIntent(getApplicationContext()));
            return true;
        }

        return false;
    }
}
