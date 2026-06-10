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
import android.text.InputType;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.Dialogs;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import org.slf4j.Logger;

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

        findPreference("log_customurl_basicauth").setOnPreferenceClickListener(this);
        if(!Strings.isNullOrEmpty(preferenceHelper.getCustomLoggingBasicAuthUsername())){
            findPreference("log_customurl_basicauth").setSummary(preferenceHelper.getCustomLoggingBasicAuthUsername() + ":" + preferenceHelper.getCustomLoggingBasicAuthPassword().replaceAll(".","*"));
        }


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
                                    .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                                    .validatePattern("[^\\n]+"," ")
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
            url = url.replaceAll("\n","");
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
