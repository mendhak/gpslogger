/*
 * Copyright (C) 2026 Jan-NiklasB
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
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.codekidlabs.storagechooser.StorageChooser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.BarcodeFormat;
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
import com.mendhak.gpslogger.ui.components.SimpleErrorDialog;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import org.slf4j.Logger;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Objects;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;


public class DawarichFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        PreferenceValidator,
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final Logger LOG = Logs.of(DawarichFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(PreferenceNames.DAWARICH_BASE_URL).setSummary(preferenceHelper.getDawarichBaseUrl());
        findPreference(PreferenceNames.DAWARICH_BASE_URL).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_APIKEY).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_DEVICE_ID).setSummary(preferenceHelper.getDawarichDeviceId());
        findPreference(PreferenceNames.DAWARICH_DEVICE_ID).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_BATCH_MIN).setSummary(preferenceHelper.getDawarichBatchMin().toString());
        findPreference(PreferenceNames.DAWARICH_BATCH_MIN).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_BATCH_MAX).setSummary(preferenceHelper.getDawarichBatchMax().toString());
        findPreference(PreferenceNames.DAWARICH_BATCH_MAX).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.DAWARICH_DISCARD_LOG_WHEN_OFFLINE).setOnPreferenceChangeListener(this);

        findPreference(PreferenceNames.LOG_TO_DAWARICH).setOnPreferenceChangeListener(this);

        findPreference(PreferenceNames.DAWARICH_QR_BUTTON).setOnPreferenceClickListener(this);


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
                                    .inputType(InputType.TYPE_CLASS_TEXT)
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

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_BATCH_MIN)){
            SimpleFormDialog.build()
                    .title(R.string.dawarich_bulk_min_title)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.DAWARICH_BATCH_MIN)
                                    .text(preferenceHelper.getDawarichBatchMin().toString())
                                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .show(this, PreferenceNames.DAWARICH_BATCH_MIN);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_BATCH_MAX)){
            SimpleFormDialog.build()
                    .title(R.string.dawarich_bulk_max_title)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.DAWARICH_BATCH_MAX)
                                    .text(preferenceHelper.getDawarichBatchMin().toString())
                                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .show(this, PreferenceNames.DAWARICH_BATCH_MAX);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_QR_BUTTON)){
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getContext().getString(R.string.dawarich_qr_prompt));
            options.setBarcodeImageEnabled(false);
            options.setOrientationLocked(false);
            readDawarichQR.launch(options);
        }

        return false;
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if(which != BUTTON_POSITIVE) { return true; }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.DAWARICH_APIKEY)){
            String apikey = extras.getString(PreferenceNames.DAWARICH_APIKEY);
            preferenceHelper.setDawarichApikey(apikey);
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
            preferenceHelper.setDawarichDeviceId(deviceId);
            findPreference(PreferenceNames.DAWARICH_DEVICE_ID).setSummary(deviceId);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.DAWARICH_BATCH_MIN)){
            Integer amount = Integer.parseInt(Objects.requireNonNull(extras.getString(PreferenceNames.DAWARICH_BATCH_MIN)));
            preferenceHelper.setDawarichBatchMin(amount);
            findPreference(PreferenceNames.DAWARICH_BATCH_MIN).setSummary(amount.toString());
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.DAWARICH_BATCH_MAX)){
            Integer amount = Integer.parseInt(Objects.requireNonNull(extras.getString(PreferenceNames.DAWARICH_BATCH_MAX)));
            preferenceHelper.setDawarichBatchMax(amount);
            findPreference(PreferenceNames.DAWARICH_BATCH_MAX).setSummary(amount.toString());
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_DAWARICH)){
            if ((boolean) newValue) {
                if (Strings.isNullOrEmpty(preferenceHelper.getDawarichBaseUrl())) {
                    SimpleErrorDialog.build()
                            .title(R.string.dawarich_baseUrl_not_set_title)
                            .msg(R.string.dawarich_baseUrl_not_set_msg)
                            .pos(R.string.ok)
                            .show(this);
                    preferenceHelper.setShouldLogToDawarich(false);
                    return false;
                }
                if (Strings.isNullOrEmpty(preferenceHelper.getDawarichApikey())) {
                    SimpleErrorDialog.build()
                            .title(R.string.dawarich_apiKey_not_set_title)
                            .msg(R.string.dawarich_apiKey_not_set_msg)
                            .pos(R.string.ok)
                            .show(this);
                    preferenceHelper.setShouldLogToDawarich(false);
                    return false;
                }
                preferenceHelper.setShouldLogToDawarich(true);
                return true;
            } else {
                preferenceHelper.setShouldLogToDawarich(false);
                return true;
            }
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.DAWARICH_DISCARD_LOG_WHEN_OFFLINE)){
            if((boolean) newValue) {
                preferenceHelper.setShouldDawarichLoggingDiscardOfflineLocations(true);
                return true;
            } else {
                preferenceHelper.setShouldDawarichLoggingDiscardOfflineLocations(false);
                return true;
            }
        }

        return false;
    }

    private final ActivityResultLauncher<ScanOptions> readDawarichQR = registerForActivityResult(new ScanContract(),
            result -> {
        if (result.getContents() == null) {
            Toast.makeText(getContext(), R.string.cancelled, Toast.LENGTH_LONG).show();
        } else {
            Gson gson = new Gson();
            try {
                JsonObject data = gson.fromJson(result.getContents(), JsonObject.class);
                String url = data.get("server_url").getAsString().replaceAll("/+$", "");
                String apikey = data.get("api_key").getAsString();
                assert !Strings.isNullOrEmpty(url);
                assert !Strings.isNullOrEmpty(apikey);
                preferenceHelper.setDawarichApikey(apikey);
                preferenceHelper.setDawarichBaseUrl(url);
                findPreference(PreferenceNames.DAWARICH_BASE_URL).setSummary(url);

            } catch (Exception e) {
                Toast.makeText(getContext(), R.string.dawarich_qr_not_valid, Toast.LENGTH_LONG).show();
            }
        }
    });
}