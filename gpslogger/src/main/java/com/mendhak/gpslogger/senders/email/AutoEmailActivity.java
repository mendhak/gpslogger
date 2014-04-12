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
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.IMessageBoxCallback;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

public class AutoEmailActivity extends PreferenceActivity implements
        OnPreferenceChangeListener, IMessageBoxCallback, IActionListener,
        OnPreferenceClickListener
{

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(AutoEmailActivity.class.getSimpleName());
    private final Handler handler = new Handler();


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.autoemailsettings);

        CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("autoemail_enabled");

        chkEnabled.setOnPreferenceChangeListener(this);

        ListPreference lstPresets = (ListPreference) findPreference("autoemail_preset");
        lstPresets.setOnPreferenceChangeListener(this);

        EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
        EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
        txtSmtpServer.setOnPreferenceChangeListener(this);
        txtSmtpPort.setOnPreferenceChangeListener(this);

        Preference testEmailPref = findPreference("smtp_testemail");

        testEmailPref.setOnPreferenceClickListener(this);

    }



    public boolean onPreferenceClick(Preference preference)
    {

        if (!IsFormValid())
        {
            Utilities.MsgBox(getString(R.string.autoemail_invalid_form),
                    getString(R.string.autoemail_invalid_form_message),
                    AutoEmailActivity.this);
            return false;
        }

        Utilities.ShowProgress(this, getString(R.string.autoemail_sendingtest),
                getString(R.string.please_wait));

        CheckBoxPreference chkUseSsl = (CheckBoxPreference) findPreference("smtp_ssl");
        EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
        EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
        EditTextPreference txtUsername = (EditTextPreference) findPreference("smtp_username");
        EditTextPreference txtPassword = (EditTextPreference) findPreference("smtp_password");
        EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");
        EditTextPreference txtFrom = (EditTextPreference) findPreference("smtp_from");


        AutoEmailHelper aeh = new AutoEmailHelper(null);
        aeh.SendTestEmail(txtSmtpServer.getText(), txtSmtpPort.getText(),
                txtUsername.getText(), txtPassword.getText(),
                chkUseSsl.isChecked(), txtTarget.getText(), txtFrom.getText(),
                AutoEmailActivity.this);

        return true;
    }

    private boolean IsFormValid()
    {

        CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("autoemail_enabled");
        EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
        EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
        EditTextPreference txtUsername = (EditTextPreference) findPreference("smtp_username");
        EditTextPreference txtPassword = (EditTextPreference) findPreference("smtp_password");
        EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");

        return !chkEnabled.isChecked() || txtSmtpServer.getText() != null
                && txtSmtpServer.getText().length() > 0 && txtSmtpPort.getText() != null
                && txtSmtpPort.getText().length() > 0 && txtUsername.getText() != null
                && txtUsername.getText().length() > 0 && txtPassword.getText() != null
                && txtPassword.getText().length() > 0 && txtTarget.getText() != null
                && txtTarget.getText().length() > 0;

    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (!IsFormValid())
            {
                CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("autoemail_enabled");
                chkEnabled.setChecked(false);
                Utilities.MsgBox(getString(R.string.autoemail_invalid_form),
                        getString(R.string.autoemail_invalid_form_message),
                        this);
                return false;
            }
            else
            {
                return super.onKeyDown(keyCode, event);
            }
        }
        else
        {
            return super.onKeyDown(keyCode, event);
        }
    }


    public void MessageBoxResult(int which)
    {
        finish();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue)
    {

        if (preference.getKey().equals("autoemail_preset"))
        {
            int newPreset = Integer.valueOf(newValue.toString());

            switch (newPreset)
            {
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

    private void SetSmtpValues(String server, String port, boolean useSsl)
    {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
        EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
        CheckBoxPreference chkUseSsl = (CheckBoxPreference) findPreference("smtp_ssl");

        // Yahoo
        txtSmtpServer.setText(server);
        editor.putString("smtp_server", server);
        txtSmtpPort.setText(port);
        editor.putString("smtp_port", port);
        chkUseSsl.setChecked(useSsl);
        editor.putBoolean("smtp_ssl", useSsl);

        editor.commit();

    }


    private final Runnable successfullySent = new Runnable()
    {
        public void run()
        {
            SuccessfulSending();
        }
    };

    private final Runnable failedSend = new Runnable()
    {

        public void run()
        {
            FailureSending();
        }
    };

    private void FailureSending()
    {
        Utilities.HideProgress();
        Utilities.MsgBox(getString(R.string.sorry), getString(R.string.error_connection), this);
    }

    private void SuccessfulSending()
    {
        Utilities.HideProgress();
        Utilities.MsgBox(getString(R.string.success),
                getString(R.string.autoemail_testresult_success), this);
    }

    public void OnComplete()
    {
        handler.post(successfullySent);
    }

    public void OnFailure()
    {

        handler.post(failedSend);

    }
}
