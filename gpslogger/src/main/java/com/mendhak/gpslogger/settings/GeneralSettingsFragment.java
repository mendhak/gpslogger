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
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.loggers.Files;

import java.io.File;

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

        findPreference("enableDisableGps").setOnPreferenceClickListener(this);

        findPreference("gpsvisualizer_link").setOnPreferenceClickListener(this);

        findPreference("debuglogtoemail").setOnPreferenceClickListener(this);

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

        if(preference.getKey().equals("debuglogtoemail")){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "GPSLogger Debug Log");

            StringBuilder diagnostics = new StringBuilder();
            diagnostics.append("Android version: ").append(Build.VERSION.SDK_INT).append("\r\n");
            diagnostics.append("OS version: ").append(System.getProperty("os.version")).append("\r\n");
            diagnostics.append("Manufacturer: ").append(Build.MANUFACTURER).append("\r\n");
            diagnostics.append("Model: ").append(Build.MODEL).append("\r\n");
            diagnostics.append("Product: ").append(Build.PRODUCT).append("\r\n");
            diagnostics.append("Brand: ").append(Build.BRAND).append("\r\n");


            intent.putExtra(Intent.EXTRA_TEXT, diagnostics.toString());
            File root = Files.storageFolder(getActivity());
            File file = new File(root, "/debuglog.txt");
            if (file.exists() && file.canRead()) {
                Uri uri = Uri.parse("file://" + file);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, "Send debug log"));
            }
            else {
                Toast.makeText(getActivity(), "debuglog.txt not found", Toast.LENGTH_LONG).show();
            }

        }

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
                intent.putExtra("force_fullscreen",true);
                startActivity(intent);
            }

        }
        return false;
    }
}
