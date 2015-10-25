/*******************************************************************************
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
 ******************************************************************************/

package com.mendhak.gpslogger.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import com.mendhak.gpslogger.R;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@SuppressWarnings("deprecation")
public class GeneralSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    int aboutClickCounter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);

        Preference enableDisablePref = findPreference("enableDisableGps");
        enableDisablePref.setOnPreferenceClickListener(this);

        Preference gpsvisualizerPref = findPreference("gpsvisualizer_link");
        gpsvisualizerPref.setOnPreferenceClickListener(this);

//        Preference btcPref = findPreference("bitcoindonate_link");
//        btcPref.setOnPreferenceClickListener(this);

        Preference aboutInfo = findPreference("about_version_info");
        try {

            aboutInfo.setTitle("GPSLogger version " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
            aboutInfo.setOnPreferenceClickListener(this);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("enableDisableGps")) {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }

//        if(preference.getKey().equalsIgnoreCase("bitcoindonate_link")){
//
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("bitcoin:1DhQCEkzEJkEQRFVLbGqv6Qksoc4aGpuAS"));
//            if( intent.resolveActivity(getActivity().getPackageManager()) == null){
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coinbase.com/mendhak")));
//            }
//            else {
//                startActivity(intent);
//            }
//
//            return true;
//
//        }

        if(preference.getKey().equalsIgnoreCase("gpsvisualizer_link")){
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.mendhak.gpsvisualizer"));
            startActivity(intent);
            return true;
        }

        if (preference.getKey().equals("about_version_info")) {
            aboutClickCounter++;

            if (aboutClickCounter == 3) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://dQw4w9WgXcQ"));
                startActivity(intent);
            }

        }
        return false;
    }
}
