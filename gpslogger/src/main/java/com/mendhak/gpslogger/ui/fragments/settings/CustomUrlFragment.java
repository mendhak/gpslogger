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
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.view.WindowManager;
import android.widget.EditText;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.events.UploadEvents;
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

import de.greenrobot.event.EventBus;

public class CustomUrlFragment extends PreferenceFragment implements
        PreferenceValidator,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(CustomUrlFragment.class);


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.customurlsettings);

        EditTextPreference urlPathPreference = (EditTextPreference)findPreference(PreferenceNames.LOG_TO_URL_PATH);
        urlPathPreference.setSummary(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setText(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setOnPreferenceChangeListener(this);

        findPreference("customurl_legend_1").setOnPreferenceClickListener(this);
        findPreference("customurl_validatecustomsslcert").setOnPreferenceClickListener(this);
        findPreference("log_customurl_basicauth").setOnPreferenceClickListener(this);

        registerEventBus();

    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference.getKey().equals(PreferenceNames.LOG_TO_URL_PATH)){
            preference.setSummary(newValue.toString());
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
                            "{16} %ACT<br />{17} %FILENAME<br />{18} %PROFILE<br />" +
                            "{19} %HDOP<br />{20} %VDOP<br />{21} %PDOP<br />{22} %DIST",
                    getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                    getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                    getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                    getString(R.string.txt_timestamp_epoch),
                    getString(R.string.txt_time_isoformat),
                    getString(R.string.txt_date_isoformat),
                    getString(R.string.txt_starttimestamp_epoch),
                    getString(R.string.txt_battery), "Android ID ", "Serial ", getString(R.string.txt_activity), getString(R.string.summary_current_filename), "Profile:", "HDOP:", "VDOP:", "PDOP:", getString(R.string.txt_travel_distance));
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
            MaterialDialog alertDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.customurl_http_basicauthentication)
                    .customView(R.layout.customurl_basicauthview, true)

                    .autoDismiss(false)
                    .negativeText(R.string.cancel)
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            String basicAuthUsername = ((EditText)materialDialog.getView().findViewById(R.id.basicauth_username)).getText().toString();
                            PreferenceHelper.getInstance().setCustomLoggingBasicAuthUsername(basicAuthUsername);

                            String basicAuthPassword = ((EditText)materialDialog.getView().findViewById(R.id.basicauth_pwd)).getText().toString();
                            PreferenceHelper.getInstance().setCustomLoggingBasicAuthPassword(basicAuthPassword);

                            materialDialog.dismiss();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .build();


            final AppCompatEditText bauthUsernameText = (AppCompatEditText) alertDialog.getCustomView().findViewById(R.id.basicauth_username);
            bauthUsernameText.setText(PreferenceHelper.getInstance().getCustomLoggingBasicAuthUsername());
            final AppCompatEditText bauthPwdText = (AppCompatEditText) alertDialog.getCustomView().findViewById(R.id.basicauth_pwd);
            bauthPwdText.setText(PreferenceHelper.getInstance().getCustomLoggingBasicAuthPassword());

            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();
            return true;
        }

        return false;
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.CustomUrl c){

        if(!isAdded()) { return; }

        LOG.debug("Custom URL test, success: " + c.success);
        Dialogs.hideProgress();
        if(!c.success){
            Dialogs.error(getString(R.string.error), c.message, c.throwable.getMessage(),c.throwable, getActivity());
        }
        else {
            Dialogs.alert(getString(R.string.success), "", getActivity());
        }
    }


}
