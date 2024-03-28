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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.ftp.FtpManager;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;


import org.slf4j.Logger;

public class FtpFragment
        extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener,
        SimpleDialog.OnDialogResultListener,
        PreferenceValidator {
    private static final Logger LOG = Logs.of(FtpFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        findPreference(PreferenceNames.FTP_SERVER).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.FTP_SERVER).setSummary(preferenceHelper.getFtpServerName());

        findPreference(PreferenceNames.FTP_USERNAME).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.FTP_USERNAME).setSummary(preferenceHelper.getFtpUsername());

        findPreference(PreferenceNames.FTP_PASSWORD).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.FTP_PASSWORD).setSummary(preferenceHelper.getFtpPassword().replaceAll(".","*"));

        findPreference(PreferenceNames.FTP_DIRECTORY).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.FTP_DIRECTORY).setSummary(preferenceHelper.getFtpDirectory());

        findPreference(PreferenceNames.FTP_PORT).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.FTP_PORT).setSummary(String.valueOf(preferenceHelper.getFtpPort()));


        findPreference("autoftp_test").setOnPreferenceClickListener(this);
        findPreference("ftp_validatecustomsslcert").setOnPreferenceClickListener(this);

        registerEventBus();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.autoftpsettings, rootKey);
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
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equals("ftp_validatecustomsslcert")){
            Networks.beginCertificateValidationWorkflow(getActivity(), preferenceHelper.getFtpServerName(), preferenceHelper.getFtpPort(), ServerType.FTP);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.FTP_SERVER)){
            SimpleFormDialog.build()
                    .title(R.string.autoopengts_server)
                    .msg(R.string.autoopengts_server_summary)
                    .fields(
                            Input.plain(PreferenceNames.FTP_SERVER)
                                    .required()
                                    .text(preferenceHelper.getFtpServerName())
                                    .hint(R.string.autoopengts_server_summary)
                    ).show(this, PreferenceNames.FTP_SERVER);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.FTP_USERNAME)){
            SimpleFormDialog.build()
                    .title(R.string.autoftp_username)
                    .fields(
                            Input.plain(PreferenceNames.FTP_USERNAME)
                                    .required()
                                    .text(preferenceHelper.getFtpUsername())
                    ).show(this, PreferenceNames.FTP_USERNAME);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.FTP_PASSWORD)){
            SimpleFormDialog.build().title(R.string.autoftp_password)
                    .fields(
                            Input.plain(PreferenceNames.FTP_PASSWORD)
                                    .text(preferenceHelper.getFtpPassword())
                                    .showPasswordToggle()
                                    .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    ).show(this, PreferenceNames.FTP_PASSWORD);
            return true;
        }


        if(preference.getKey().equalsIgnoreCase(PreferenceNames.FTP_DIRECTORY)){
            SimpleFormDialog.build()
                    .title(R.string.autoftp_directory)
                    .fields(
                            Input.plain(PreferenceNames.FTP_DIRECTORY)
                                    .required()
                                    .text(preferenceHelper.getFtpDirectory())
                    ).show(this, PreferenceNames.FTP_DIRECTORY);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.FTP_PORT)){
            SimpleFormDialog.build().title(R.string.autoftp_port)
                    .fields(
                            Input.plain(PreferenceNames.FTP_PORT)
                                    .required()
                                    .text(String.valueOf(preferenceHelper.getFtpPort()))
                                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .show( this, PreferenceNames.FTP_PORT);
            return true;
        }

        if (preference.getKey().equalsIgnoreCase("autoftp_test")) {
            FtpManager helper = new FtpManager(preferenceHelper);

            String servernamePreference = preferenceHelper.getFtpServerName();
            String usernamePreference = preferenceHelper.getFtpUsername();
            String passwordPreference = preferenceHelper.getFtpPassword();
            int portPreference = preferenceHelper.getFtpPort();
            boolean useFtpsPreference = preferenceHelper.shouldFtpUseFtps();
            String sslTlsPreference = preferenceHelper.getFtpProtocol();
            boolean implicitPreference = preferenceHelper.isFtpImplicit();


            if (!helper.validSettings(servernamePreference, usernamePreference, passwordPreference,
                    portPreference,
                    useFtpsPreference, sslTlsPreference,
                    implicitPreference)) {
                Dialogs.alert(getString(R.string.autoftp_invalid_settings),
                        getString(R.string.autoftp_invalid_summary),
                        getActivity());
                return false;
            }

            Dialogs.progress((FragmentActivity) getActivity(), getString(R.string.autoftp_testing));
            helper.testFtp();
        }



        return true;
    }


    @Override
    public boolean isValid() {
        FtpManager manager = new FtpManager(preferenceHelper);

        return !manager.hasUserAllowedAutoSending() || manager.isAvailable();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Ftp o){
            LOG.debug("FTP Event completed, success: " + o.success);
            Dialogs.hideProgress();
            if(!o.success){
                String ftpMessages = (o.ftpMessages == null) ? "" : TextUtils.join("",o.ftpMessages);
                Dialogs.showError(getString(R.string.sorry), "FTP Test Failed", o.message + "\r\n" + ftpMessages, o.throwable, (FragmentActivity) getActivity());
            }
            else {
                Dialogs.alert(getString(R.string.success), "FTP Test Succeeded", getActivity());
            }
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if(which != BUTTON_POSITIVE){ return true; }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.FTP_PASSWORD)){
            String ftpPass = extras.getString(PreferenceNames.FTP_PASSWORD);
            preferenceHelper.setFtpPassword(ftpPass);
            findPreference(PreferenceNames.FTP_PASSWORD).setSummary(ftpPass.replaceAll(".","*"));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.FTP_PORT)){
            String port = extras.getString(PreferenceNames.FTP_PORT);
            preferenceHelper.setFtpPort(port);
            findPreference(PreferenceNames.FTP_PORT).setSummary(port);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.FTP_SERVER)){
            String server = extras.getString(PreferenceNames.FTP_SERVER);
            preferenceHelper.setFtpServerName(server);
            findPreference(PreferenceNames.FTP_SERVER).setSummary(server);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.FTP_USERNAME)){
            String user = extras.getString(PreferenceNames.FTP_USERNAME);
            preferenceHelper.setFtpUsername(user);
            findPreference(PreferenceNames.FTP_USERNAME).setSummary(user);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.FTP_DIRECTORY)){
            String dir = extras.getString(PreferenceNames.FTP_DIRECTORY);
            preferenceHelper.setFtpDirectory(dir);
            findPreference(PreferenceNames.FTP_DIRECTORY).setSummary(dir);
            return true;
        }

        return false;
    }
}