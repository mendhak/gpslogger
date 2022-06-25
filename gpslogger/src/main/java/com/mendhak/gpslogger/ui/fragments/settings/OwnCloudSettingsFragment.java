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
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.network.ConscryptProviderInstaller;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.owncloud.OwnCloudManager;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class OwnCloudSettingsFragment
        extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener,
        SimpleDialog.OnDialogResultListener,
        PreferenceValidator {

    private static final Logger LOG = Logs.of(OwnCloudSettingsFragment.class);
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(PreferenceNames.OWNCLOUD_BASE_URL).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OWNCLOUD_BASE_URL).setSummary(preferenceHelper.getOwnCloudBaseUrl());

        findPreference(PreferenceNames.OWNCLOUD_USERNAME).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OWNCLOUD_USERNAME).setSummary(preferenceHelper.getOwnCloudUsername());

        findPreference(PreferenceNames.OWNCLOUD_PASSWORD).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OWNCLOUD_PASSWORD).setSummary(preferenceHelper.getOwnCloudPassword().replaceAll(".","*"));

        findPreference(PreferenceNames.OWNCLOUD_DIRECTORY).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.OWNCLOUD_DIRECTORY).setSummary(preferenceHelper.getOwnCloudDirectory());

        findPreference("owncloud_test").setOnPreferenceClickListener(this);
        findPreference("owncloud_validatecustomsslcert").setOnPreferenceClickListener(this);

        ConscryptProviderInstaller.addConscryptPreferenceItemIfNeeded(this.getPreferenceScreen());

        registerEventBus();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.owncloudsettings, rootKey);
    }

    @Override
    public void onDestroy() {

        unregisterEventBus();
        super.onDestroy();
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public boolean isValid() {
        boolean isEnabled = preferenceHelper.isOwnCloudAutoSendEnabled();
        String server = preferenceHelper.getOwnCloudBaseUrl();
        String user = preferenceHelper.getOwnCloudUsername();

        return !isEnabled || (
                server != null && server.length() > 0 &&
                user != null && user.length() > 0
        );
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OWNCLOUD_PASSWORD)){
            SimpleFormDialog.build().title(R.string.autoftp_password)
                    .fields(
                            Input.plain(PreferenceNames.OWNCLOUD_PASSWORD)
                                    .text(preferenceHelper.getOwnCloudPassword())
                                    .showPasswordToggle()
                                    .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        )
                    .show(this, PreferenceNames.OWNCLOUD_PASSWORD);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OWNCLOUD_DIRECTORY)){
            SimpleFormDialog.build()
                    .title(R.string.autoftp_directory)
                    .msg(R.string.owncloud_directory_summary)
                    .fields(
                            Input.plain(PreferenceNames.OWNCLOUD_DIRECTORY)
                                    .text(preferenceHelper.getOwnCloudDirectory())
                    )
                    .show(this, PreferenceNames.OWNCLOUD_DIRECTORY);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OWNCLOUD_BASE_URL)){
            SimpleFormDialog.build()
                    .title(R.string.owncloud_server_summary)
                    .fields(
                            Input.plain(PreferenceNames.OWNCLOUD_BASE_URL)
                                    .text(preferenceHelper.getOwnCloudBaseUrl())
                    )
                    .show(this, PreferenceNames.OWNCLOUD_BASE_URL);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OWNCLOUD_USERNAME)){
            SimpleFormDialog.build()
                    .title(R.string.autoftp_username)
                    .fields(
                            Input.plain(PreferenceNames.OWNCLOUD_USERNAME)
                                    .text(preferenceHelper.getOwnCloudUsername())
                    )
                    .show(this, PreferenceNames.OWNCLOUD_USERNAME);
            return true;
        }

        if(preference.getKey().equals("owncloud_validatecustomsslcert")){
            try {
                URL u = new URL(PreferenceHelper.getInstance().getOwnCloudBaseUrl());
                Networks.beginCertificateValidationWorkflow(getActivity(), u.getHost(), u.getPort() < 0 ? u.getDefaultPort() : u.getPort(), ServerType.HTTPS);
            } catch (MalformedURLException e) {
                LOG.error("Could not validate certificate, OwnCloud URL is not valid", e);
            }

        }
        else if(preference.getKey().equals("owncloud_test")){
            String server = preferenceHelper.getOwnCloudBaseUrl();
            String user = preferenceHelper.getOwnCloudUsername();
            String pass = preferenceHelper.getOwnCloudPassword();
            String directory = preferenceHelper.getOwnCloudDirectory();



            if (!OwnCloudManager.validSettings(
                    server,
                    user,
                    pass,
                    directory)) {
                Dialogs.alert(getString(R.string.autoftp_invalid_settings),
                        getString(R.string.autoftp_invalid_summary),
                        getActivity());
                return false;
            }

            Dialogs.progress((FragmentActivity) getActivity(), getString(R.string.owncloud_testing));
            OwnCloudManager helper = new OwnCloudManager(PreferenceHelper.getInstance());
            helper.testOwnCloud(server, user, pass, directory);
        }


        return true;
    }


    @EventBusHook
    public void onEventMainThread(UploadEvents.OwnCloud o){
        LOG.debug("OwnCloud Event completed, success: " + o.success);

        Dialogs.hideProgress();
        if(!o.success){
            Dialogs.showError(getString(R.string.sorry), "OwnCloud Test Failed", o.message, o.throwable, (FragmentActivity) getActivity());
        }
        else {
            Dialogs.alert(getString(R.string.success), "OwnCloud Test Succeeded", getActivity());
        }
    }



    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if(which != BUTTON_POSITIVE){ return true; }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OWNCLOUD_PASSWORD)){
            String ocPass = extras.getString(PreferenceNames.OWNCLOUD_PASSWORD);
            preferenceHelper.setOwnCloudPassword(ocPass);
            findPreference(PreferenceNames.OWNCLOUD_PASSWORD).setSummary(ocPass.replaceAll(".","*"));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OWNCLOUD_DIRECTORY)){
            String dir = extras.getString(PreferenceNames.OWNCLOUD_DIRECTORY);
            preferenceHelper.setOwnCloudDirectory(dir);
            findPreference(PreferenceNames.OWNCLOUD_DIRECTORY).setSummary(dir);
            return  true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OWNCLOUD_BASE_URL)){
            String base = extras.getString(PreferenceNames.OWNCLOUD_BASE_URL);
            preferenceHelper.setOwnCloudBaseUrl(base);
            findPreference(PreferenceNames.OWNCLOUD_BASE_URL).setSummary(base);
            return  true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OWNCLOUD_USERNAME)){
            String user = extras.getString(PreferenceNames.OWNCLOUD_USERNAME);
            preferenceHelper.setOwnCloudUsername(user);
            findPreference(PreferenceNames.OWNCLOUD_USERNAME).setSummary(user);
            return  true;
        }

        return false;
    }
}

