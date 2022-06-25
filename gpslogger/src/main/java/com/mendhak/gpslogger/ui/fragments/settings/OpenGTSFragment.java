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
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.network.ConscryptProviderInstaller;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.Dialogs;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class OpenGTSFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        PreferenceValidator,
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(PreferenceNames.AUTOSEND_OPENGTS_ENABLED).setOnPreferenceChangeListener(this);

        findPreference(PreferenceNames.OPENGTS_SERVER).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OPENGTS_SERVER).setSummary(preferenceHelper.getOpenGTSServer());

        findPreference(PreferenceNames.OPENGTS_PORT).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OPENGTS_PORT).setSummary(preferenceHelper.getOpenGTSServerPort());


        findPreference(PreferenceNames.OPENGTS_PROTOCOL).setOnPreferenceChangeListener(this);
        if(!Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServerCommunicationMethod())){
            findPreference(PreferenceNames.OPENGTS_PROTOCOL).setSummary(preferenceHelper.getOpenGTSServerCommunicationMethod());
        }


        findPreference(PreferenceNames.OPENGTS_SERVER_PATH).setOnPreferenceClickListener(this);
        if(!Strings.isNullOrEmpty(preferenceHelper.getOpenGTSServerPath())){
            findPreference(PreferenceNames.OPENGTS_SERVER_PATH).setSummary(preferenceHelper.getOpenGTSServerPath());
        }


        findPreference(PreferenceNames.OPENGTS_ACCOUNT_NAME).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OPENGTS_ACCOUNT_NAME).setSummary(preferenceHelper.getOpenGTSAccountName());

        findPreference(PreferenceNames.OPENGTS_DEVICE_ID).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OPENGTS_DEVICE_ID).setSummary(preferenceHelper.getOpenGTSDeviceId());

        findPreference("opengts_validatecustomsslcert").setOnPreferenceClickListener(this);

        ConscryptProviderInstaller.addConscryptPreferenceItemIfNeeded(this.getPreferenceScreen());

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.opengtssettings, rootKey);
    }

    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals("opengts_validatecustomsslcert")){
            if (!isFormValid()) {
                Dialogs.alert(getString(R.string.autoftp_invalid_settings),
                        getString(R.string.autoftp_invalid_summary),
                        getActivity());
                return true;
            }
            Networks.beginCertificateValidationWorkflow(
                    getActivity(),
                    PreferenceHelper.getInstance().getOpenGTSServer(),
                    Strings.toInt(PreferenceHelper.getInstance().getOpenGTSServerPort(),443),
                    ServerType.HTTPS
            );
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENGTS_PORT)){
            SimpleFormDialog.build().title(R.string.autoftp_port)
                    .neg(R.string.cancel)
                    .fields(
                            Input.plain(PreferenceNames.OPENGTS_PORT)
                                    .required().text(preferenceHelper
                                    .getOpenGTSServerPort())
                                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .show( this, PreferenceNames.OPENGTS_PORT);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENGTS_SERVER_PATH)){
            SimpleFormDialog.build().title(R.string.autoopengts_server_path)
                    .neg(R.string.cancel)
                    .msg(R.string.autoopengts_server_path_summary)
                    .fields(
                            Input.plain(PreferenceNames.OPENGTS_SERVER_PATH)
                                    .text(preferenceHelper.getOpenGTSServerPath())
                    )
                    .show(this, PreferenceNames.OPENGTS_SERVER_PATH);
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENGTS_SERVER)){
            SimpleFormDialog.build().title(R.string.autoopengts_server)
                    .neg(R.string.cancel)
                    .fields(
                            Input.plain(PreferenceNames.OPENGTS_SERVER)
                                    .required()
                                    .hint(R.string.autoopengts_server_summary)
                                    .text(preferenceHelper.getOpenGTSServer())
                    )
                    .show(this, PreferenceNames.OPENGTS_SERVER);
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENGTS_ACCOUNT_NAME)){
            SimpleFormDialog.build().title(R.string.autoopengts_accountname)
                    .neg(R.string.cancel)
                    .fields(
                            Input.plain(PreferenceNames.OPENGTS_ACCOUNT_NAME)
                                    .text(preferenceHelper.getOpenGTSAccountName())
                    )
                    .show(this, PreferenceNames.OPENGTS_ACCOUNT_NAME);
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENGTS_DEVICE_ID)){
            SimpleFormDialog.build().title(R.string.autoopengts_device_id)
                    .neg(R.string.cancel)
                    .fields(
                            Input.plain(PreferenceNames.OPENGTS_DEVICE_ID)
                                    .text(preferenceHelper.getOpenGTSDeviceId())
                    )
                    .show(this, PreferenceNames.OPENGTS_DEVICE_ID);
        }

        return true;
    }

    private boolean isFormValid() {

        if(!preferenceHelper.isOpenGtsAutoSendEnabled()) {
            return true;
        }

        String openGtsServer = preferenceHelper.getOpenGTSServer();
        String openGtsPort = preferenceHelper.getOpenGTSServerPort();
        String openGtsCommunication = preferenceHelper.getOpenGTSServerCommunicationMethod();
        String openGtsServerPath = preferenceHelper.getOpenGTSServerPath();
        String openGtsDeviceId = preferenceHelper.getOpenGTSDeviceId();

        return  openGtsServer != null && openGtsServer.length() > 0
                && openGtsPort != null && isNumeric(openGtsPort)
                && openGtsCommunication != null && openGtsCommunication.length() > 0
                && openGtsDeviceId != null && openGtsDeviceId.length() > 0
                && URLUtil.isValidUrl("http://" + openGtsServer + ":" + openGtsPort + openGtsServerPath);

    }

    private static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENGTS_PROTOCOL)){
            preference.setSummary(newValue.toString());
        }

        return true;
    }

    @Override
    public boolean isValid() {
        return isFormValid();
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(which != BUTTON_POSITIVE){
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENGTS_PORT)){
            String port = extras.getString(PreferenceNames.OPENGTS_PORT);
            preferenceHelper.setOpenGTSServerPort(port);
            findPreference(PreferenceNames.OPENGTS_PORT).setSummary(port);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENGTS_SERVER_PATH)){
            String path = extras.getString(PreferenceNames.OPENGTS_SERVER_PATH);
            preferenceHelper.setOpenGTSServerPath(path);
            findPreference(PreferenceNames.OPENGTS_SERVER_PATH).setSummary(path);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENGTS_SERVER)){
            String server = extras.getString(PreferenceNames.OPENGTS_SERVER);
            preferenceHelper.setOpenGTSServer(server);
            findPreference(PreferenceNames.OPENGTS_SERVER).setSummary(server);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENGTS_ACCOUNT_NAME)){
            String acct = extras.getString(PreferenceNames.OPENGTS_ACCOUNT_NAME);
            preferenceHelper.setOpenGTSAccountName(acct);
            findPreference(PreferenceNames.OPENGTS_ACCOUNT_NAME).setSummary(acct);
            return true;
        }


        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENGTS_DEVICE_ID)){
            String dev = extras.getString(PreferenceNames.OPENGTS_DEVICE_ID);
            preferenceHelper.setOpenGTSDeviceId(dev);
            findPreference(PreferenceNames.OPENGTS_DEVICE_ID).setSummary(dev);
            return true;
        }

        return false;
    }
}
