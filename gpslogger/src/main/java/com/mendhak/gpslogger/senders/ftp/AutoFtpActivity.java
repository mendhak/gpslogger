/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
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

package com.mendhak.gpslogger.senders.ftp;

import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

public class AutoFtpActivity extends PreferenceActivity implements IActionListener, Preference.OnPreferenceClickListener
{
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(AutoFtpActivity.class.getSimpleName());

    private final Handler handler = new Handler();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.autoftpsettings);

        Preference testFtp = findPreference("autoftp_test");
        testFtp.setOnPreferenceClickListener(this);

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
        Utilities.MsgBox(getString(R.string.sorry), "FTP Test Failed", this);
    }

    private void SuccessfulSending()
    {
        Utilities.HideProgress();
        Utilities.MsgBox(getString(R.string.success),
                "FTP Test Succeeded", this);
    }

    @Override
    public void OnComplete()
    {
        Utilities.HideProgress();
        handler.post(successfullySent);
    }

    @Override
    public void OnFailure()
    {
        Utilities.HideProgress();
        handler.post(failedSend);
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {

        FtpHelper helper = new FtpHelper(this);

        EditTextPreference servernamePreference = (EditTextPreference)findPreference("autoftp_server");
        EditTextPreference usernamePreference = (EditTextPreference)findPreference("autoftp_username");
        EditTextPreference passwordPreference = (EditTextPreference)findPreference("autoftp_password");
        EditTextPreference portPreference = (EditTextPreference)findPreference("autoftp_port");
        CheckBoxPreference useFtpsPreference = (CheckBoxPreference)findPreference("autoftp_useftps");
        ListPreference sslTlsPreference = (ListPreference)findPreference("autoftp_ssltls");
        CheckBoxPreference implicitPreference = (CheckBoxPreference)findPreference("autoftp_implicit");
        EditTextPreference directoryPreference = (EditTextPreference)findPreference("autoftp_directory");

        if(!helper.ValidSettings(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                Integer.valueOf(portPreference.getText()), useFtpsPreference.isChecked(), sslTlsPreference.getValue(),
                implicitPreference.isChecked()))
        {
            Utilities.MsgBox(getString(R.string.autoftp_invalid_settings),
                    getString(R.string.autoftp_invalid_summary),
                    AutoFtpActivity.this);
            return false;
        }


        Utilities.ShowProgress(this, getString(R.string.autoftp_testing),
                getString(R.string.please_wait));


        helper.TestFtp(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                directoryPreference.getText(), Integer.valueOf(portPreference.getText()), useFtpsPreference.isChecked(),
                sslTlsPreference.getValue(), implicitPreference.isChecked());

        return true;
    }


}