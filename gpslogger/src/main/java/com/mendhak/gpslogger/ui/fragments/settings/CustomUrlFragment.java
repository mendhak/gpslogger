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
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import android.text.InputType;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.network.ConscryptProviderInstaller;
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
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final Logger LOG = Logs.of(CustomUrlFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        Preference urlPathPreference = findPreference(PreferenceNames.LOG_TO_URL_PATH);
        urlPathPreference.setSummary(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setOnPreferenceClickListener(this);


        findPreference(PreferenceNames.LOG_TO_URL_HEADERS).setSummary(preferenceHelper.getCustomLoggingHTTPHeaders());
        findPreference(PreferenceNames.LOG_TO_URL_HEADERS).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.LOG_TO_URL_METHOD).setSummary(preferenceHelper.getCustomLoggingHTTPMethod());
        findPreference(PreferenceNames.LOG_TO_URL_METHOD).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.LOG_TO_URL_BODY).setSummary(preferenceHelper.getCustomLoggingHTTPBody());
        findPreference(PreferenceNames.LOG_TO_URL_BODY).setOnPreferenceClickListener(this);

        findPreference(PreferenceNames.AUTOSEND_CUSTOMURL_ENABLED).setOnPreferenceChangeListener(this);

        findPreference("customurl_legend_1").setOnPreferenceClickListener(this);
        findPreference("customurl_validatecustomsslcert").setOnPreferenceClickListener(this);

        ConscryptProviderInstaller.addConscryptPreferenceItemIfNeeded(this.getPreferenceScreen());

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
    public boolean isValid() {
        return true;
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals("customurl_legend_1")){

            String codeGreen = Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.accentColorComplementary)).substring(2);
            String legendFormat =
                    "{1} <font color=''#{0}'' face=''monospace''>%LAT</font><br />" +
                            "{2} <font color=''#{0}'' face=''monospace''>%LON</font><br />" +
                            "{3} <font color=''#{0}'' face=''monospace''>%DESC</font><br />" +
                            "{4} <font color=''#{0}'' face=''monospace''>%SAT</font><br />" +
                            "{5} <font color=''#{0}'' face=''monospace''>%ALT</font><br />" +
                            "{6} <font color=''#{0}'' face=''monospace''>%SPD</font><br />" +
                            "{7} <font color=''#{0}'' face=''monospace''>%ACC</font><br />" +
                            "{8} <font color=''#{0}'' face=''monospace''>%DIR</font><br />" +
                            "{9} <font color=''#{0}'' face=''monospace''>%PROV</font><br />" +
                            "{10} <font color=''#{0}'' face=''monospace''>%TIMESTAMP</font><br />" +
                            "{11} <font color=''#{0}'' face=''monospace''>%TIME</font><br />" +
                            "{12} <font color=''#{0}'' face=''monospace''>%TIMEOFFSET</font><br />" +
                            "{13} <font color=''#{0}'' face=''monospace''>%DATE</font><br />" +
                            "{14} <font color=''#{0}'' face=''monospace''>%STARTTIMESTAMP</font><br />" +
                            "{15} <font color=''#{0}'' face=''monospace''>%BATT</font><br />" +
                            "{16} <font color=''#{0}'' face=''monospace''>%ISCHARGING</font><br />" +
                            "{17} <font color=''#{0}'' face=''monospace''>%AID</font><br />" +
                            "{18} <font color=''#{0}'' face=''monospace''>%SER</font><br />" +
                            "{19} <font color=''#{0}'' face=''monospace''>%FILENAME</font><br />" +
                            "{20} <font color=''#{0}'' face=''monospace''>%PROFILE</font><br />" +
                            "{21} <font color=''#{0}'' face=''monospace''>%HDOP</font><br />" +
                            "{22} <font color=''#{0}'' face=''monospace''>%VDOP</font><br />" +
                            "{23} <font color=''#{0}'' face=''monospace''>%PDOP</font><br />" +
                            "{24} <font color=''#{0}'' face=''monospace''>%DIST</font><br />" +
                            "{25} <font color=''#{0}'' face=''monospace''>%ALL</font>";
            String legend1 = MessageFormat.format(legendFormat,
                    codeGreen,
                    getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                    getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                    getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                    getString(R.string.txt_timestamp_epoch),
                    getString(R.string.txt_time_isoformat),
                    getString(R.string.txt_time_with_offset_isoformat),
                    getString(R.string.txt_date_isoformat),
                    getString(R.string.txt_starttimestamp_epoch),
                    getString(R.string.txt_battery), getString(R.string.txt_battery_charging), "Android ID ", "Serial ",
                    getString(R.string.summary_current_filename), "Profile:", "HDOP:", "VDOP:", "PDOP:",
                    getString(R.string.txt_travel_distance), getString(R.string.customurl_all_parameters));
            Dialogs.alert(getString(R.string.parameters), legend1, getActivity());
            return true;
        }

        if(preference.getKey().equals("customurl_validatecustomsslcert")){

            try {
                URL u = new URL(PreferenceHelper.getInstance().getCustomLoggingUrl());
                Networks.beginCertificateValidationWorkflow(getActivity(), u.getHost(), u.getPort() < 0 ? u.getDefaultPort() : u.getPort(), ServerType.HTTPS);
            } catch (MalformedURLException e) {
                LOG.error("Could not start certificate validation", e);
            }
            return true;
        }

        if(preference.getKey().equals("log_customurl_basicauth")){

            SimpleFormDialog.build()
                    .title(R.string.customurl_http_basicauthentication)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME)
                                    .text(preferenceHelper.getCustomLoggingBasicAuthUsername())
                                    .hint(R.string.autoftp_username),
                            Input.plain(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD)
                                    .text(preferenceHelper.getCustomLoggingBasicAuthPassword())
                                    .hint(R.string.autoftp_password)
                                    .showPasswordToggle()
                                    .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    )
                    .show(this,PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME);

            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_URL_HEADERS)){
            SimpleFormDialog.build()
                    .title(R.string.customurl_http_headers)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .msgHtml("<font face='monospace'>Content-Type: application/json</font><br /><font face='monospace'>Authorization: Basic abcdefg</font><br /><font face='monospace'>ApiToken: 12345</font>")
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_URL_HEADERS)
                                    .text(preferenceHelper.getCustomLoggingHTTPHeaders())
                                    .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                    )
                    .show(this, PreferenceNames.LOG_TO_URL_HEADERS);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_URL_PATH)){
            SimpleFormDialog.build()
                    .title("URL")
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_URL_PATH)
                                    .text(preferenceHelper.getCustomLoggingUrl())
                                    .required()
                    )
                    .show(this, PreferenceNames.LOG_TO_URL_PATH);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_URL_METHOD)){
            SimpleFormDialog.build()
                    .title(R.string.customurl_http_method)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_URL_METHOD)
                                    .text(preferenceHelper.getCustomLoggingHTTPMethod())
                                    .required()
                    )
                    .show(this, PreferenceNames.LOG_TO_URL_METHOD);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_URL_BODY)){
            SimpleFormDialog.build()
                    .title(R.string.customurl_http_body)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_URL_BODY)
                                    .text(preferenceHelper.getCustomLoggingHTTPBody())
                                    .hint("lat=%LAT&lon=%LON")
                                    .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                    )
                    .show(this, PreferenceNames.LOG_TO_URL_BODY);
            return true;
        }

        return false;
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if(which != BUTTON_POSITIVE) { return true; }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME)){
            String basicAuthUsername = extras.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME);
            String basicAuthPass = extras.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD);
            preferenceHelper.setCustomLoggingBasicAuthUsername(basicAuthUsername);
            preferenceHelper.setCustomLoggingBasicAuthPassword(basicAuthPass);
            findPreference("log_customurl_basicauth").setSummary(preferenceHelper.getCustomLoggingBasicAuthUsername() + ":" + preferenceHelper.getCustomLoggingBasicAuthPassword().replaceAll(".","*"));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOG_TO_URL_HEADERS)){
            String headers = extras.getString(PreferenceNames.LOG_TO_URL_HEADERS);
            preferenceHelper.setCustomLoggingHTTPHeaders(headers);
            findPreference(PreferenceNames.LOG_TO_URL_HEADERS).setSummary(headers);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOG_TO_URL_PATH)){
            String url = extras.getString(PreferenceNames.LOG_TO_URL_PATH);
            preferenceHelper.setCustomLoggingUrl(url);
            findPreference(PreferenceNames.LOG_TO_URL_PATH).setSummary(url);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOG_TO_URL_METHOD)){
            String method = extras.getString(PreferenceNames.LOG_TO_URL_METHOD);
            preferenceHelper.setCustomLoggingHTTPMethod(method);
            findPreference(PreferenceNames.LOG_TO_URL_METHOD).setSummary(method);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOG_TO_URL_BODY)){
            String body = extras.getString(PreferenceNames.LOG_TO_URL_BODY);
            preferenceHelper.setCustomLoggingHTTPBody(body);
            findPreference(PreferenceNames.LOG_TO_URL_BODY).setSummary(body);
            return true;
        }


        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference.getKey().equalsIgnoreCase(PreferenceNames.AUTOSEND_CUSTOMURL_ENABLED)){
            Boolean isEnabled = (Boolean)newValue;
            if(isEnabled){
                // Custom URL SENDER requires CSV logging. Custom URL logging is independent.
                preferenceHelper.setShouldLogToCSV(true);
            }
            return true;
        }
        return false;
    }
}
