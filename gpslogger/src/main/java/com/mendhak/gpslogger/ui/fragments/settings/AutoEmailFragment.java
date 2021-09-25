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
import androidx.preference.SwitchPreferenceCompat;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.*;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.senders.email.AutoEmailManager;
import com.mendhak.gpslogger.ui.Dialogs;
import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class AutoEmailFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener,
        PreferenceValidator,
        SimpleDialog.OnDialogResultListener {

    private final PreferenceHelper preferenceHelper;
    AutoEmailManager aem;

    public AutoEmailFragment(){
        this.preferenceHelper = PreferenceHelper.getInstance();
        this.aem = new AutoEmailManager(preferenceHelper);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(PreferenceNames.EMAIL_TARGET).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.EMAIL_TARGET).setSummary(preferenceHelper.getAutoEmailTargets());

        findPreference(PreferenceNames.EMAIL_SMTP_USERNAME).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.EMAIL_SMTP_USERNAME).setSummary(preferenceHelper.getSmtpUsername());

        findPreference(PreferenceNames.EMAIL_FROM).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.EMAIL_FROM).setSummary(preferenceHelper.getSmtpSenderAddress());

        findPreference(PreferenceNames.EMAIL_SMTP_PASSWORD).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.EMAIL_SMTP_PASSWORD).setSummary(preferenceHelper.getSmtpPassword().replaceAll(".","*"));

        findPreference(PreferenceNames.EMAIL_SMTP_SERVER).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.EMAIL_SMTP_SERVER).setSummary(preferenceHelper.getSmtpServer());

        findPreference(PreferenceNames.EMAIL_SMTP_PORT).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.EMAIL_SMTP_PORT).setSummary(preferenceHelper.getSmtpPort());


        findPreference("autoemail_enabled").setOnPreferenceChangeListener(this);
        findPreference("autoemail_preset").setOnPreferenceChangeListener(this);
        findPreference("smtp_testemail").setOnPreferenceClickListener(this);
        findPreference("smtp_validatecustomsslcert").setOnPreferenceClickListener(this);

        registerEventBus();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.autoemailsettings, rootKey);
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

    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equals("smtp_validatecustomsslcert")){
                Networks.beginCertificateValidationWorkflow(getActivity(), preferenceHelper.getSmtpServer(), Strings.toInt(preferenceHelper.getSmtpPort(),25), ServerType.SMTP);
                return true;
        }


        if(preference.getKey().equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_USERNAME)){
            SimpleFormDialog.build()
                    .title(R.string.autoftp_username)
                    .msg(R.string.autoemail_username_summary)
                    .fields(
                            Input.plain(PreferenceNames.EMAIL_SMTP_USERNAME).text(preferenceHelper.getSmtpUsername())
                    ).show(this, PreferenceNames.EMAIL_SMTP_USERNAME);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.EMAIL_TARGET)){
            SimpleFormDialog.build()
                    .title(R.string.autoemail_target)
                    .msg(R.string.autoemail_sendto_csv)
                    .fields(
                            Input.plain(PreferenceNames.EMAIL_TARGET).text(preferenceHelper.getAutoEmailTargets())
                    ).show(this,PreferenceNames.EMAIL_TARGET);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.EMAIL_FROM)){
            SimpleFormDialog.build()
                    .title(R.string.autoemail_from)
                    .msg(R.string.autoemail_from_summary)
                    .fields(
                            Input.plain(PreferenceNames.EMAIL_FROM).text(preferenceHelper.getSmtpSenderAddress())
                    ).show(this,PreferenceNames.EMAIL_FROM);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_PASSWORD)){
            SimpleFormDialog.build()
                    .title(R.string.autoftp_password)
                    .msg(R.string.autoemail_password_summary)
                    .fields(
                            Input.plain(PreferenceNames.EMAIL_SMTP_PASSWORD).text(preferenceHelper.getSmtpPassword()).showPasswordToggle().inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    ).show(this,PreferenceNames.EMAIL_SMTP_PASSWORD);
            return true;
        }


        if(preference.getKey().equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_SERVER)){
            SimpleFormDialog.build()
                    .title(R.string.autoopengts_server)
                    .msg(R.string.autoopengts_server_summary)
                    .fields(
                            Input.plain(PreferenceNames.EMAIL_SMTP_SERVER).text(preferenceHelper.getSmtpServer())
                    ).show(this,PreferenceNames.EMAIL_SMTP_SERVER);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_PORT)){
            SimpleFormDialog.build()
                    .title(R.string.autoftp_port)
                    .fields(
                            Input.plain(PreferenceNames.EMAIL_SMTP_PORT).required().text(String.valueOf(preferenceHelper.getSmtpPort())).inputType(InputType.TYPE_CLASS_NUMBER)

                    ).show(this,PreferenceNames.EMAIL_SMTP_PORT);
            return true;
        }


        if (preference.getKey().equals("smtp_testemail")){

            boolean useSsl = preferenceHelper.isSmtpSsl();
            String smtpServer = preferenceHelper.getSmtpServer();
            String smtpPort = preferenceHelper.getSmtpPort();
            String smtpUsername = preferenceHelper.getSmtpUsername();
            String smtpPassword = preferenceHelper.getSmtpPassword();
            String smtpTarget = preferenceHelper.getAutoEmailTargets();
            String senderAddress = preferenceHelper.getSmtpSenderAddress();

            if (!aem.isValid(smtpServer, smtpPort, smtpUsername, smtpPassword, smtpTarget)) {
                Dialogs.alert(getString(R.string.autoftp_invalid_settings),
                        getString(R.string.autoftp_invalid_summary),
                        getActivity());
                return true;
            }

            if (!Systems.isNetworkAvailable(getActivity())) {
                Dialogs.alert(getString(R.string.sorry), getString(R.string.no_network_message), getActivity());
                return true;
            }

            Dialogs.progress((FragmentActivity) getActivity(), getString(R.string.autoemail_sendingtest));



            aem.sendTestEmail(smtpServer, smtpPort,
                    smtpUsername, smtpPassword,
                    useSsl, smtpTarget, senderAddress);
            return true;
        }




        return true;
    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().equals("autoemail_preset")) {
            int newPreset = Integer.parseInt(newValue.toString());

            switch (newPreset) {
                case 0:
                    // Gmail
                    setSmtpValues("smtp.gmail.com", "465", true);
                    break;
                case 1:
                    // Windows live mail
                    setSmtpValues("smtp.live.com", "587", false);
                    break;
                case 2:
                    // Yahoo
                    setSmtpValues("smtp.mail.yahoo.com", "465", true);
                    break;
                case 99:
                    // manual
                    break;
            }

        }

        return true;
    }

    private void setSmtpValues(String server, String port, boolean useSsl) {

        Preference txtSmtpServer = findPreference("smtp_server");
        Preference txtSmtpPort = findPreference("smtp_port");
        SwitchPreferenceCompat chkUseSsl = (SwitchPreferenceCompat) findPreference("smtp_ssl");

        preferenceHelper.setSmtpServer(server);
        txtSmtpServer.setSummary(server);

        preferenceHelper.setSmtpPort(port);
        txtSmtpPort.setSummary(port);

        preferenceHelper.setSmtpSsl(useSsl);
        chkUseSsl.setChecked(useSsl);

    }


    @Override
    public boolean isValid() {
        return !aem.hasUserAllowedAutoSending() || aem.isAvailable();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.AutoEmail o){

        Dialogs.hideProgress();

        if(o.success){
            Dialogs.alert(getString(R.string.success),
                    getString(R.string.autoemail_testresult_success), getActivity());
        } else {
            String smtpMessages = (o.smtpMessages == null) ? "" : TextUtils.join("", o.smtpMessages);
            Dialogs.showError(getString(R.string.sorry), getString(R.string.error_connection), o.message + "\r\n" + smtpMessages, o.throwable, (FragmentActivity) getActivity());
        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(dialogTag.equalsIgnoreCase(PreferenceNames.EMAIL_TARGET) && which == BUTTON_POSITIVE){
            String emailCsv = extras.getString(PreferenceNames.EMAIL_TARGET);
            preferenceHelper.setAutoEmailTargets(emailCsv);
            findPreference(PreferenceNames.EMAIL_TARGET).setSummary(preferenceHelper.getAutoEmailTargets());
            return true;
        }
        if(dialogTag.equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_USERNAME) && which == BUTTON_POSITIVE){
            String smtpUsername = extras.getString(PreferenceNames.EMAIL_SMTP_USERNAME);
            preferenceHelper.setSmtpUsername(smtpUsername);
            findPreference(PreferenceNames.EMAIL_SMTP_USERNAME).setSummary(preferenceHelper.getSmtpUsername());
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.EMAIL_FROM) && which == BUTTON_POSITIVE){
            String smtpFrom = extras.getString(PreferenceNames.EMAIL_FROM);
            preferenceHelper.setSmtpFrom(smtpFrom);
            findPreference(PreferenceNames.EMAIL_FROM).setSummary(preferenceHelper.getSmtpSenderAddress());
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_PASSWORD) && which == BUTTON_POSITIVE){
            String smtpPass = extras.getString(PreferenceNames.EMAIL_SMTP_PASSWORD);
            preferenceHelper.setSmtpPassword(smtpPass);
            findPreference(PreferenceNames.EMAIL_SMTP_PASSWORD).setSummary(preferenceHelper.getSmtpPassword().replaceAll(".","*"));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_SERVER) && which == BUTTON_POSITIVE){
            String smtpServer = extras.getString(PreferenceNames.EMAIL_SMTP_SERVER);
            preferenceHelper.setSmtpServer(smtpServer);
            findPreference(PreferenceNames.EMAIL_SMTP_SERVER).setSummary(preferenceHelper.getSmtpServer());
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.EMAIL_SMTP_PORT) && which == BUTTON_POSITIVE){
            String smtpPort = extras.getString(PreferenceNames.EMAIL_SMTP_PORT);
            preferenceHelper.setSmtpPort(smtpPort);
            findPreference(PreferenceNames.EMAIL_SMTP_PORT).setSummary(preferenceHelper.getSmtpPort());
            return true;
        }

        return false;
    }
}
