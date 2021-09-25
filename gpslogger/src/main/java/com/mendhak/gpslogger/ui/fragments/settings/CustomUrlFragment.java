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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.text.InputType;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.Dialogs;


import org.slf4j.Logger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;


public class CustomUrlFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        PreferenceValidator,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(CustomUrlFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        EditTextPreference urlPathPreference = (EditTextPreference)findPreference(PreferenceNames.LOG_TO_URL_PATH);
        urlPathPreference.setSummary(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setText(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setOnPreferenceChangeListener(this);

        findPreference(PreferenceNames.LOG_TO_URL_HEADERS).setSummary(preferenceHelper.getCustomLoggingHTTPHeaders());
        findPreference(PreferenceNames.LOG_TO_URL_HEADERS).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.LOG_TO_URL_METHOD).setSummary(preferenceHelper.getCustomLoggingHTTPMethod());
        findPreference(PreferenceNames.LOG_TO_URL_METHOD).setOnPreferenceChangeListener(this);
        findPreference(PreferenceNames.LOG_TO_URL_BODY).setSummary(preferenceHelper.getCustomLoggingHTTPBody());
        findPreference(PreferenceNames.LOG_TO_URL_BODY).setOnPreferenceChangeListener(this);

        findPreference("customurl_legend_1").setOnPreferenceClickListener(this);
        findPreference("customurl_validatecustomsslcert").setOnPreferenceClickListener(this);

        findPreference("log_customurl_basicauth").setOnPreferenceClickListener(this);
        if(!Strings.isNullOrEmpty(preferenceHelper.getCustomLoggingBasicAuthUsername())){
            findPreference("log_customurl_basicauth").setSummary(preferenceHelper.getCustomLoggingBasicAuthUsername() + ":" + preferenceHelper.getCustomLoggingBasicAuthPassword().replaceAll(".","*"));
        }


    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.customurlsettings, rootKey);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference.getKey().equals(PreferenceNames.LOG_TO_URL_PATH)){
            preference.setSummary(newValue.toString());
            return true;
        }
        if(preference.getKey().equals(PreferenceNames.LOG_TO_URL_METHOD)){
            preference.setSummary(newValue.toString());
            return true;
        }
        if(preference.getKey().equals(PreferenceNames.LOG_TO_URL_BODY)){
            preference.setSummary(newValue.toString());
            return true;
        }

        return true;
    }


    @Override
    public boolean isValid() {
        return true;
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals("customurl_legend_1")){

            String legend1 = MessageFormat.format("{0} %LAT<br />{1} %LON<br />{2} %DESC<br />{3} %SAT<br />{4} %ALT<br />" +
                            "{5} %SPD<br />{6} %ACC<br />{7} %DIR<br />{8} %PROV<br />{9} %TIMESTAMP<br />" +
                            "{10} %TIME<br />{11} %DATE<br />{12} %STARTTIMESTAMP<br />{13} %BATT<br />{14} %AID<br />{15} %SER<br />" +
                            "{16} %FILENAME<br />{17} %PROFILE<br />" +
                            "{18} %HDOP<br />{19} %VDOP<br />{20} %PDOP<br />{21} %DIST",
                    getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                    getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                    getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                    getString(R.string.txt_timestamp_epoch),
                    getString(R.string.txt_time_isoformat),
                    getString(R.string.txt_date_isoformat),
                    getString(R.string.txt_starttimestamp_epoch),
                    getString(R.string.txt_battery), "Android ID ", "Serial ", getString(R.string.summary_current_filename), "Profile:", "HDOP:", "VDOP:", "PDOP:", getString(R.string.txt_travel_distance));
            Dialogs.alert(getString(R.string.parameters), legend1, getActivity());

        }
        else if(preference.getKey().equals("customurl_validatecustomsslcert")){

            try {
                URL u = new URL(PreferenceHelper.getInstance().getCustomLoggingUrl());
                Networks.beginCertificateValidationWorkflow(getActivity(), u.getHost(), u.getPort() < 0 ? u.getDefaultPort() : u.getPort(), ServerType.HTTPS);
            } catch (MalformedURLException e) {
                LOG.error("Could not start certificate validation", e);
            }

            return true;
        }
        else if(preference.getKey().equals("log_customurl_basicauth")){

            SimpleFormDialog.build()
                    .title(R.string.customurl_http_basicauthentication)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME).text(preferenceHelper.getCustomLoggingBasicAuthUsername()).hint(R.string.autoftp_username).required(),
                            Input.plain(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD).text(preferenceHelper.getCustomLoggingBasicAuthPassword()).hint(R.string.autoftp_password).showPasswordToggle().inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD).required()
                    )
                    .show(this,PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME);

            return true;
        }
        else if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_URL_HEADERS)){
            SimpleFormDialog.build()
                    .title(R.string.customurl_http_headers)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_URL_HEADERS)
                                    .text(preferenceHelper.getCustomLoggingHTTPHeaders())
                                    .hint("Content-Type: application/json\nAuthorization: Basic abcdefg\nApiToken: 12345")
                                    .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE).required()
                    )
                    .show(this, PreferenceNames.LOG_TO_URL_HEADERS);
        }

        return false;
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME) && which == BUTTON_POSITIVE){
            String basicAuthUsername = extras.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME);
            String basicAuthPass = extras.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD);
            preferenceHelper.setCustomLoggingBasicAuthUsername(basicAuthUsername);
            preferenceHelper.setCustomLoggingBasicAuthPassword(basicAuthPass);
            findPreference("log_customurl_basicauth").setSummary(preferenceHelper.getCustomLoggingBasicAuthUsername() + ":" + preferenceHelper.getCustomLoggingBasicAuthPassword().replaceAll(".","*"));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOG_TO_URL_HEADERS) && which == BUTTON_POSITIVE){
            String headers = extras.getString(PreferenceNames.LOG_TO_URL_HEADERS);
            preferenceHelper.setCustomLoggingHTTPHeaders(headers);
            findPreference(PreferenceNames.LOG_TO_URL_HEADERS).setSummary(headers);
            return true;
        }


        return false;
    }
}
