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
import android.provider.Settings;
import android.text.InputType;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.codekidlabs.storagechooser.StorageChooser;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.Dialogs;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;


public class DawarichFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        PreferenceValidator,
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final Logger LOG = Logs.of(DawarichFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preference filepath = findPreference(PreferenceNames.DAWARICH_FILE_PATH);
        filepath.setSummary(PreferenceHelper.getInstance().getDawarichFilePath());
        filepath.setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_BASE_URL).setSummary(preferenceHelper.getDawarichBaseUrl());
        findPreference(PreferenceNames.DAWARICH_BASE_URL).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_APIKEY).setSummary(preferenceHelper.getDawarichApikey());
        findPreference(PreferenceNames.DAWARICH_APIKEY).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_DEVICE_ID).setSummary(preferenceHelper.getDawarichDeviceId());
        findPreference(PreferenceNames.DAWARICH_DEVICE_ID).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_DISCARD_LOG_WHEN_OFFLINE).setOnPreferenceChangeListener(this);

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dawarichsettings, rootKey);
    }




    @Override
    public boolean isValid() {
        return true;
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        LOG.debug(preference.getKey());

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_BASE_URL)){
            SimpleFormDialog.build()
                    .title(R.string.log_dawarich_url)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.DAWARICH_BASE_URL)
                                    .text(preferenceHelper.getDawarichBaseUrl())
                                    .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                                    .validatePattern("[^\\n]+"," ")
                                    .required()
                    )
                    .show(this, PreferenceNames.DAWARICH_BASE_URL);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_APIKEY)){
            SimpleFormDialog.build()
                    .title(R.string.log_dawarich_apikey)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.DAWARICH_APIKEY)
                                    .text(preferenceHelper.getDawarichApikey())
                                    .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    )
                    .show(this, PreferenceNames.DAWARICH_APIKEY);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_DEVICE_ID)){
            SimpleFormDialog.build()
                    .title(R.string.log_dawarich_device_id)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.DAWARICH_DEVICE_ID)
                                    .text(preferenceHelper.getDawarichDeviceId())
                                    .inputType(InputType.TYPE_CLASS_TEXT)
                    )
                    .show(this, PreferenceNames.DAWARICH_DEVICE_ID);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_FILE_PATH)){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                SimpleDialog.build()
                        .title(R.string.error)
                        .msg(R.string.gpslogger_custom_path_need_permission)
                        .show(this, "FILE_PERMISSIONS_REQUIRED");

                return false;
            }

            StorageChooser chooser = Dialogs.directoryChooser(getActivity());
            chooser.setOnSelectListener(path -> {
                LOG.debug(path);
                if(Strings.isNullOrEmpty(path)) {
                    path = Files.storageFolder(getActivity()).getAbsolutePath();
                }
                File testFile = new File(path, "testfile.txt");
                try {
                    testFile.createNewFile();
                    if(testFile.exists()){
                        testFile.delete();
                        LOG.debug("Test file successfully created and deleted.");
                    }
                } catch (Exception ex) {
                    LOG.error("Could not create a test file in the chosen directory.", ex);
                    path = preferenceHelper.getGpsLoggerFolder();
                    Dialogs.alert(getString(R.string.error), getString(R.string.pref_logging_file_no_permissions), getActivity());
                }

                findPreference(PreferenceNames.DAWARICH_FILE_PATH).setSummary(path);
                preferenceHelper.setDawarichFilepath(path);

            });
            chooser.show();
        }

        return false;
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if(which != BUTTON_POSITIVE) { return true; }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.DAWARICH_APIKEY)){
            String apikey = extras.getString(PreferenceNames.DAWARICH_APIKEY);
            preferenceHelper.setDawarichApikey(apikey);
            findPreference(PreferenceNames.DAWARICH_APIKEY).setSummary(apikey);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.DAWARICH_BASE_URL)){
            String url = extras.getString(PreferenceNames.DAWARICH_BASE_URL);
            url = url.replaceAll("\n","");
            preferenceHelper.setDawarichBaseUrl(url);
            findPreference(PreferenceNames.DAWARICH_BASE_URL).setSummary(url);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.DAWARICH_DEVICE_ID)){
            String deviceId = extras.getString(PreferenceNames.DAWARICH_DEVICE_ID);
            preferenceHelper.setDawarichApikey(deviceId);
            findPreference(PreferenceNames.DAWARICH_DEVICE_ID).setSummary(deviceId);
            return true;
        }

        if(dialogTag.equalsIgnoreCase("FILE_PERMISSIONS_REQUIRED")){
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            getActivity().startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
            return true;
        }


        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
//        if(preference.getKey().equalsIgnoreCase(PreferenceNames.AUTOSEND_CUSTOMURL_ENABLED)){
//            Boolean isEnabled = (Boolean)newValue;
//            if(isEnabled){
//                // Custom URL SENDER requires CSV logging. Custom URL logging is independent.
//                preferenceHelper.setShouldLogToCSV(true);
//            }
//            return true;
//        }
        return false;
    }
}
