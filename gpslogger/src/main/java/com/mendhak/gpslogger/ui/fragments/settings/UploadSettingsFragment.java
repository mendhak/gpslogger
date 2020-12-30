/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.ui.fragments.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mendhak.gpslogger.MainPreferenceActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Strings;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */

public class UploadSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_upload);

        findPreference("osm_setup").setOnPreferenceClickListener(this);
        findPreference("autoemail_setup").setOnPreferenceClickListener(this);
        findPreference("opengts_setup").setOnPreferenceClickListener(this);
        findPreference("autoftp_setup").setOnPreferenceClickListener(this);
        findPreference("owncloud_setup").setOnPreferenceClickListener(this);
        findPreference("sftp_setup").setOnPreferenceClickListener(this);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        String launchFragment = "";

        if (preference.getKey().equalsIgnoreCase("osm_setup")) {
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.OSM;
        }

        if(preference.getKey().equalsIgnoreCase("autoemail_setup")){
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.EMAIL;
        }

        if(preference.getKey().equalsIgnoreCase("opengts_setup")){
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS;
        }

        if(preference.getKey().equalsIgnoreCase("autoftp_setup")){
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.FTP;
        }

        if(preference.getKey().equalsIgnoreCase("owncloud_setup")) {
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.OWNCLOUD;
        }

        if(preference.getKey().equalsIgnoreCase("sftp_setup")){
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.SFTP;
        }

        if(!Strings.isNullOrEmpty(launchFragment)){
            Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
            intent.putExtra("preference_fragment", launchFragment);
            startActivity(intent);
            return true;
        }

        return false;
    }
}
