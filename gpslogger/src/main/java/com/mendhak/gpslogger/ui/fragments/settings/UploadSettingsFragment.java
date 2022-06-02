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
import android.text.InputType;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mendhak.gpslogger.MainPreferenceActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.ui.components.SwitchPlusClickPreference;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

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

public class UploadSettingsFragment
        extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, SimpleDialog.OnDialogResultListener {

    PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(PreferenceNames.AUTOSEND_FREQUENCY).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.AUTOSEND_FREQUENCY).setSummary(preferenceHelper.getAutoSendInterval() + getString(R.string.minutes));

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_OSM_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {
                    @Override
                    public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                        // No need to do anything, the value gets propagated.
                    }

                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                        intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.OSM);
                        startActivity(intent);
                    }
                });

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_EMAIL_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {
                    @Override
                    public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                        // No need to do anything, the value gets propagated.
                    }

                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                        intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.EMAIL);
                        startActivity(intent);
                    }
                });

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_OPENGTS_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {
                    @Override
                    public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                        // No need to do anything, the value gets propagated.
                    }

                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                        intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS);
                        startActivity(intent);
                    }
                });

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_FTP_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {
                    @Override
                    public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                        // No need to do anything, the value gets propagated.
                    }

                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                        intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.FTP);
                        startActivity(intent);
                    }
                });

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_OWNCLOUD_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {
            @Override
            public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                // No need to do anything, the value gets propagated.
            }

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.OWNCLOUD);
                startActivity(intent);
            }
        });

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.SFTP_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {
            @Override
            public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                // No need to do anything, the value gets propagated.
            }

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.SFTP);
                startActivity(intent);
            }
        });

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_CUSTOMURL_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {

                    @Override
                    public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                        if(isChecked){
                            preferenceHelper.setShouldLogToCSV(true);
                        }
                    }

                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                        intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.CUSTOMURL);
                        startActivity(intent);
                    }
                });

        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_DROPBOX_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {

                    @Override
                    public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                        // No need to do anything, the value gets propagated.
                    }

                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                        intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.DROPBOX);
                        startActivity(intent);
                    }
                });


        ((SwitchPlusClickPreference)findPreference(PreferenceNames.AUTOSEND_GOOGLE_DRIVE_ENABLED))
                .setSwitchClickListener(new SwitchPlusClickPreference.SwitchPlusClickListener() {

            @Override
            public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked) {
                // No need to do anything, the value gets propagated.
            }

            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
                intent.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.GOOGLEDRIVE);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_upload, rootKey);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.AUTOSEND_FREQUENCY)){
            SimpleFormDialog.build().title(R.string.autosend_frequency)
                    .msg(R.string.autosend_frequency_summary)
                    .fields(
                            Input.plain(PreferenceNames.AUTOSEND_FREQUENCY)
                                    .required()
                                    .hint(R.string.autosend_frequency_hint)
                                    .text(String.valueOf(preferenceHelper.getAutoSendInterval()))
                                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .show(this, PreferenceNames.AUTOSEND_FREQUENCY);
        }

        return false;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(dialogTag.equalsIgnoreCase(PreferenceNames.AUTOSEND_FREQUENCY) && which == BUTTON_POSITIVE){
            String freq = extras.getString(PreferenceNames.AUTOSEND_FREQUENCY);
            preferenceHelper.setAutoSendInterval(freq);
            findPreference(PreferenceNames.AUTOSEND_FREQUENCY).setSummary(freq + getString(R.string.minutes));
            return true;
        }
        return false;
    }
}
