package com.mendhak.gpslogger.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.mendhak.gpslogger.R;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
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
public class GeneralSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    int aboutClickCounter=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);

        Preference enableDisablePref = findPreference("enableDisableGps");
        enableDisablePref.setOnPreferenceClickListener(this);

        Preference aboutInfo = findPreference("about_version_info");
        try {

            aboutInfo.setTitle("GPSLogger version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            aboutInfo.setOnPreferenceClickListener(this);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equals("enableDisableGps")){
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }

        if(preference.getKey().equals("about_version_info")){
            aboutClickCounter++;

            if(aboutClickCounter==3){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://dQw4w9WgXcQ"));
                startActivity(intent);
            }

        }
        return false;
    }
}
