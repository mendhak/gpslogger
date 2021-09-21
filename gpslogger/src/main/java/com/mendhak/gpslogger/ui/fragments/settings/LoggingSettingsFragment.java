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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.text.Html;
import android.text.InputType;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.codekidlabs.storagechooser.StorageChooser;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.MainPreferenceActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.components.CustomSwitchPreference;
import org.slf4j.Logger;
import java.io.File;

public class LoggingSettingsFragment extends PreferenceFragment
        implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener
{

    private static final Logger LOG = Logs.of(LoggingSettingsFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_logging);

        Preference gpsloggerFolder = findPreference("gpslogger_folder");

        String gpsLoggerFolderPath = preferenceHelper.getGpsLoggerFolder();
        gpsloggerFolder.setDefaultValue(gpsLoggerFolderPath);
        gpsloggerFolder.setSummary(gpsLoggerFolderPath);
        gpsloggerFolder.setOnPreferenceClickListener(this);

        if(!(new File(gpsLoggerFolderPath)).canWrite()){
            gpsloggerFolder.setSummary(Html.fromHtml("<font color='red'>" + gpsLoggerFolderPath + "</font>"));
        }

        CustomSwitchPreference logGpx = (CustomSwitchPreference)findPreference("log_gpx");
        CustomSwitchPreference logGpx11 = (CustomSwitchPreference)findPreference("log_gpx_11");
        logGpx11.setTitle("      " + logGpx11.getTitle());
        logGpx11.setSummary("      " + logGpx11.getSummary());


        logGpx.setOnPreferenceChangeListener(this);
        logGpx11.setEnabled(logGpx.isChecked());


        /**
         * Logging Details - New file creation
         */
        MaterialListPreference newFilePref = (MaterialListPreference) findPreference("new_file_creation");
        newFilePref.setOnPreferenceChangeListener(this);
        /* Trigger artificially the listener and perform validations. */
        newFilePref.getOnPreferenceChangeListener()
                .onPreferenceChange(newFilePref, newFilePref.getValue());

        CustomSwitchPreference chkfile_prefix_serial = (CustomSwitchPreference) findPreference("new_file_prefix_serial");
        if (Strings.isNullOrEmpty(Strings.getBuildSerial())) {
            chkfile_prefix_serial.setEnabled(false);
            chkfile_prefix_serial.setSummary("This option not available on older phones or if a serial id is not present");
        } else {
            chkfile_prefix_serial.setEnabled(true);
            chkfile_prefix_serial.setSummary(chkfile_prefix_serial.getSummary().toString() + "(" + Strings.getBuildSerial() + ")");
        }


        findPreference("new_file_custom_name").setOnPreferenceClickListener(this);
        findPreference("log_customurl_enabled").setOnPreferenceChangeListener(this);
        findPreference("log_opengts").setOnPreferenceChangeListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();

        setPreferencesEnabledDisabled();
    }



    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase("gpslogger_folder")){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                Dialogs.alert(getString(R.string.error),getString(R.string.gpslogger_custom_path_need_permission),getActivity(), which -> {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    getActivity().startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
                });
                return true;
            }

            StorageChooser chooser = Dialogs.directoryChooser(getActivity(), getFragmentManager());
            chooser.setOnSelectListener(path -> {
                LOG.debug(path);
                if(Strings.isNullOrEmpty(path)) {
                    path = Files.storageFolder(getActivity()).getAbsolutePath();
                }
                findPreference(PreferenceNames.GPSLOGGER_FOLDER).setSummary(path);
                preferenceHelper.setGpsLoggerFolder(path);
            });
            chooser.show();
        }

        if(preference.getKey().equalsIgnoreCase("new_file_custom_name")){


            new MaterialDialog.Builder(getActivity())
                    .title(R.string.new_file_custom_title)
                    .content(R.string.new_file_custom_message)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .negativeText(R.string.cancel)
                    .input(getString(R.string.letters_numbers), preferenceHelper.getCustomFileName(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog materialDialog, CharSequence input) {
                            preferenceHelper.setCustomFileName(input.toString());
                        }
                    })
                    .show();

        }

        return false;
    }


    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {



        if(preference.getKey().equalsIgnoreCase("log_gpx")){
            CustomSwitchPreference logGpx11 = (CustomSwitchPreference)findPreference("log_gpx_11");
            logGpx11.setEnabled((Boolean)newValue);
            return true;
        }


        if (preference.getKey().equalsIgnoreCase("log_opengts")) {

            if(!((CustomSwitchPreference) preference).isChecked() && (Boolean)newValue  ) {

                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS);
                startActivity(targetActivity);
            }

            return true;
        }

        if(preference.getKey().equalsIgnoreCase("log_customurl_enabled") ){

            // Bug in SwitchPreference: http://stackoverflow.com/questions/19503931/switchpreferences-calls-multiple-times-the-onpreferencechange-method
            // Check if isChecked == false && newValue == true
            if(!((CustomSwitchPreference) preference).isChecked() && (Boolean)newValue  ) {
                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.CUSTOMURL);
                startActivity(targetActivity);
           }

            return true;
        }

        if (preference.getKey().equals("new_file_creation")) {

            findPreference(PreferenceNames.ASK_CUSTOM_FILE_NAME).setEnabled(newValue.equals("custom"));
            findPreference(PreferenceNames.CUSTOM_FILE_NAME).setEnabled(newValue.equals("custom"));
            findPreference(PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING).setEnabled(newValue.equals("custom"));
            findPreference(PreferenceNames.PREFIX_SERIAL_TO_FILENAME).setEnabled(!newValue.equals("custom"));


            return true;
        }
        return false;
    }

    private void displayFileAccessError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Dialogs.alert(getString(R.string.error),getString(R.string.pref_logging_file_need_permission),getActivity(), which -> {
                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                getActivity().startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
            });
        } else {
            Dialogs.alert(getString(R.string.error), getString(R.string.pref_logging_file_no_permissions), getActivity());
        }
    }

    private void setPreferencesEnabledDisabled() {

        Preference prefFileCustomName = findPreference(PreferenceNames.CUSTOM_FILE_NAME);
        Preference prefAskEachTime = findPreference(PreferenceNames.ASK_CUSTOM_FILE_NAME);
        Preference prefSerialPrefix = findPreference(PreferenceNames.PREFIX_SERIAL_TO_FILENAME);
        Preference prefDynamicFileName = findPreference(PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING);

        prefFileCustomName.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefAskEachTime.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefSerialPrefix.setEnabled(!preferenceHelper.shouldCreateCustomFile());
        prefDynamicFileName.setEnabled(preferenceHelper.shouldCreateCustomFile());
    }


}
