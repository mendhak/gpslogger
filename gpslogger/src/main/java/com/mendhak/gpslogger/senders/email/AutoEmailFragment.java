/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.senders.email;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.PreferenceValidationFragment;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.AutoEmailEvent;
import com.mendhak.gpslogger.common.events.OpenGTSLoggedEvent;
import com.mendhak.gpslogger.views.component.CustomSwitchPreference;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

public class AutoEmailFragment extends PreferenceValidationFragment implements
        OnPreferenceChangeListener,  OnPreferenceClickListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(AutoEmailFragment.class.getSimpleName());
    private final Handler handler = new Handler();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.autoemailsettings);

        CustomSwitchPreference chkEnabled = (CustomSwitchPreference) findPreference("autoemail_enabled");

        chkEnabled.setOnPreferenceChangeListener(this);

        MaterialListPreference lstPresets = (MaterialListPreference) findPreference("autoemail_preset");
        lstPresets.setOnPreferenceChangeListener(this);

        MaterialEditTextPreference txtSmtpServer = (MaterialEditTextPreference) findPreference("smtp_server");
        MaterialEditTextPreference txtSmtpPort = (MaterialEditTextPreference) findPreference("smtp_port");
        txtSmtpServer.setOnPreferenceChangeListener(this);
        txtSmtpPort.setOnPreferenceChangeListener(this);

        Preference testEmailPref = findPreference("smtp_testemail");

        testEmailPref.setOnPreferenceClickListener(this);

        RegisterEventBus();
    }

    @Override
    public void onDestroy() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
        super.onDestroy();
    }

    private void RegisterEventBus() {
        EventBus.getDefault().register(this);
    }


    public boolean onPreferenceClick(Preference preference) {

        if (!IsFormValid()) {
            Utilities.MsgBox(getString(R.string.autoemail_invalid_form),
                    getString(R.string.autoemail_invalid_form_message),
                    getActivity());
            return true;
        }

        if (!Utilities.isNetworkAvailable(getActivity())) {
            Utilities.MsgBox(getString(R.string.sorry),getString(R.string.no_network_message), getActivity());
            return true;
        }

        Utilities.ShowProgress(getActivity(), getString(R.string.autoemail_sendingtest),
                getString(R.string.please_wait));

        CustomSwitchPreference chkUseSsl = (CustomSwitchPreference) findPreference("smtp_ssl");
        MaterialEditTextPreference txtSmtpServer = (MaterialEditTextPreference) findPreference("smtp_server");
        MaterialEditTextPreference txtSmtpPort = (MaterialEditTextPreference) findPreference("smtp_port");
        MaterialEditTextPreference txtUsername = (MaterialEditTextPreference) findPreference("smtp_username");
        MaterialEditTextPreference txtPassword = (MaterialEditTextPreference) findPreference("smtp_password");
        MaterialEditTextPreference txtTarget = (MaterialEditTextPreference) findPreference("autoemail_target");
        MaterialEditTextPreference txtFrom = (MaterialEditTextPreference) findPreference("smtp_from");

        AutoEmailHelper aeh = new AutoEmailHelper(getActivity());
        aeh.SendTestEmail(txtSmtpServer.getText(), txtSmtpPort.getText(),
                txtUsername.getText(), txtPassword.getText(),
                chkUseSsl.isChecked(), txtTarget.getText(), txtFrom.getText());

        return true;
    }

    private boolean IsFormValid() {

        CustomSwitchPreference chkEnabled = (CustomSwitchPreference) findPreference("autoemail_enabled");
        MaterialEditTextPreference txtSmtpServer = (MaterialEditTextPreference) findPreference("smtp_server");
        MaterialEditTextPreference txtSmtpPort = (MaterialEditTextPreference) findPreference("smtp_port");
        MaterialEditTextPreference txtUsername = (MaterialEditTextPreference) findPreference("smtp_username");
        MaterialEditTextPreference txtPassword = (MaterialEditTextPreference) findPreference("smtp_password");
        MaterialEditTextPreference txtTarget = (MaterialEditTextPreference) findPreference("autoemail_target");

        return !chkEnabled.isChecked() || txtSmtpServer.getText() != null
                && txtSmtpServer.getText().length() > 0 && txtSmtpPort.getText() != null
                && txtSmtpPort.getText().length() > 0 && txtUsername.getText() != null
                && txtUsername.getText().length() > 0 && txtPassword.getText() != null
                && txtPassword.getText().length() > 0 && txtTarget.getText() != null
                && txtTarget.getText().length() > 0;

    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().equals("autoemail_preset")) {
            int newPreset = Integer.valueOf(newValue.toString());

            switch (newPreset) {
                case 0:
                    // Gmail
                    SetSmtpValues("smtp.gmail.com", "465", true);
                    break;
                case 1:
                    // Windows live mail
                    SetSmtpValues("smtp.live.com", "587", false);
                    break;
                case 2:
                    // Yahoo
                    SetSmtpValues("smtp.mail.yahoo.com", "465", true);
                    break;
                case 99:
                    // manual
                    break;
            }

        }

        return true;
    }

    private void SetSmtpValues(String server, String port, boolean useSsl) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();

        MaterialEditTextPreference txtSmtpServer = (MaterialEditTextPreference) findPreference("smtp_server");
        MaterialEditTextPreference txtSmtpPort = (MaterialEditTextPreference) findPreference("smtp_port");
        CustomSwitchPreference chkUseSsl = (CustomSwitchPreference) findPreference("smtp_ssl");

        // Yahoo
        txtSmtpServer.setText(server);
        editor.putString("smtp_server", server);
        txtSmtpPort.setText(port);
        editor.putString("smtp_port", port);
        chkUseSsl.setChecked(useSsl);
        editor.putBoolean("smtp_ssl", useSsl);

        editor.commit();

    }


    @Override
    public boolean IsValid() {
        return IsFormValid();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(AutoEmailEvent o){

        Utilities.HideProgress();

        if(o.success){
            Utilities.MsgBox(getString(R.string.success),
                    getString(R.string.autoemail_testresult_success), getActivity());
        } else {
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.error_connection), getActivity());
        }
    }
}
