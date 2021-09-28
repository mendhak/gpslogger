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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;

import org.slf4j.Logger;

import java.io.File;
import java.util.*;

import eltos.simpledialogfragment.SimpleDialog;


public class GeneralSettingsFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {

    Logger LOG = Logs.of(GeneralSettingsFragment.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference("enableDisableGps").setOnPreferenceClickListener(this);
        findPreference("debuglogtoemail").setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.APP_THEME_SETTING).setOnPreferenceChangeListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            findPreference("resetapp").setOnPreferenceClickListener(this);
        }
        else {
            findPreference("resetapp").setEnabled(false);
        }


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            SwitchPreferenceCompat hideNotificiationPreference = findPreference("hide_notification_from_status_bar");
            hideNotificiationPreference.setEnabled(false);
            hideNotificiationPreference.setDefaultValue(false);
            hideNotificiationPreference.setChecked(false);
            hideNotificiationPreference.setSummary(getString(R.string.hide_notification_from_status_bar_disallowed));
        }

        setCoordinatesFormatPreferenceItem();
        setLanguagesPreferenceItem();

        Preference aboutInfo = findPreference("about_version_info");
        try {

            aboutInfo.setTitle("GPSLogger version " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_general, rootKey);
    }

    private void setLanguagesPreferenceItem() {
        ListPreference langs = findPreference("changelanguage");

        Map<String,String> localeDisplayNames = Strings.getAvailableLocales(getActivity());

        String[] locales = localeDisplayNames.keySet().toArray(new String[localeDisplayNames.keySet().size()]);
        String[] displayValues = localeDisplayNames.values().toArray(new String[localeDisplayNames.values().size()]);

        langs.setEntries(displayValues);
        langs.setEntryValues(locales);
        langs.setDefaultValue("en");
        langs.setOnPreferenceChangeListener(this);
    }

    private void setCoordinatesFormatPreferenceItem() {
        ListPreference coordFormats = findPreference("coordinatedisplayformat");
        String[] coordinateDisplaySamples = new String[]{"12° 34' 56.7890\" S","12° 34.5678' S","-12.345678"};
        coordFormats.setEntries(coordinateDisplaySamples);
        coordFormats.setEntryValues(new String[]{PreferenceNames.DegreesDisplayFormat.DEGREES_MINUTES_SECONDS.toString(),PreferenceNames.DegreesDisplayFormat.DEGREES_DECIMAL_MINUTES.toString(),PreferenceNames.DegreesDisplayFormat.DECIMAL_DEGREES.toString()});
        coordFormats.setDefaultValue("0");
        coordFormats.setOnPreferenceChangeListener(this);
        coordFormats.setSummary(coordinateDisplaySamples[PreferenceHelper.getInstance().getDisplayLatLongFormat().ordinal()]);
    }



    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("enableDisableGps")) {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }

        if (preference.getKey().equals("resetapp")) {
            SimpleDialog.build()
                    .title(getString(R.string.reset_app_title))
                    .msgHtml(getString(R.string.reset_app_summary))
                    .neg(R.string.cancel)
                    .show(this, "RESET_APP");
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

            return true;

        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if(preference.getKey().equals("changelanguage")){
            PreferenceHelper.getInstance().setUserSpecifiedLocale((String) newValue);
            LOG.debug("Language chosen: " + PreferenceHelper.getInstance().getUserSpecifiedLocale());
            return true;
        }
        if(preference.getKey().equals("coordinatedisplayformat")){
            PreferenceHelper.getInstance().setDisplayLatLongFormat(PreferenceNames.DegreesDisplayFormat.valueOf(newValue.toString()));
            LOG.debug("Coordinate format chosen: " + PreferenceHelper.getInstance().getDisplayLatLongFormat());
            setCoordinatesFormatPreferenceItem();
            return true;
        }
        if(preference.getKey().equalsIgnoreCase(PreferenceNames.APP_THEME_SETTING)){
            Dialogs.alert("", getString(R.string.restart_required), getActivity());
            return true;
        }
        return false;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(dialogTag.equalsIgnoreCase("RESET_APP") && which == BUTTON_POSITIVE){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    ((ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
            }
        }
        return false;
    }
}
