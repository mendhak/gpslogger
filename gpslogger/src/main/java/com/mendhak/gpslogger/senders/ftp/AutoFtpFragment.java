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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.PreferenceValidationFragment;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

public class AutoFtpFragment
        extends PreferenceValidationFragment implements IActionListener, Preference.OnPreferenceClickListener {
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(AutoFtpFragment.class.getSimpleName());

    private final Handler handler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.autoftpsettings);

        Preference testFtp = findPreference("autoftp_test");
        testFtp.setOnPreferenceClickListener(this);
    }


    private boolean IsFormValid() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        return !prefs.getBoolean("autoftp_enabled", false)
                || prefs.getString("autoftp_server","").length() > 0
                && prefs.getString("autoftp_username", "").length() > 0
                && prefs.getString("autoftp_port","21").length() > 0;
    }


    private final Runnable successfullySent = new Runnable() {
        public void run() {
            SuccessfulSending();
        }
    };

    private final Runnable failedSend = new Runnable() {

        public void run() {
            FailureSending();
        }
    };

    private void FailureSending() {
        Utilities.HideProgress();
        Utilities.MsgBox(getString(R.string.sorry), "FTP Test Failed", getActivity());
    }

    private void SuccessfulSending() {
        Utilities.HideProgress();
        Utilities.MsgBox(getString(R.string.success),
                "FTP Test Succeeded", getActivity());
    }

    @Override
    public void OnComplete() {
        Utilities.HideProgress();
        handler.post(successfullySent);
    }

    @Override
    public void OnFailure() {
        Utilities.HideProgress();
        handler.post(failedSend);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        FtpHelper helper = new FtpHelper(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if(!helper.ValidSettings(
                prefs.getString("autoftp_server", ""),
                prefs.getString("autoftp_username", ""),
                prefs.getString("autoftp_password", ""),
                Integer.valueOf(prefs.getString("autoftp_port", "21")),
                prefs.getBoolean("autoftp_useftps", false),
                prefs.getString("autoftp_ssltls", ""),
                prefs.getBoolean("autoftp_implicit", false)
        )){

            Utilities.MsgBox(getString(R.string.autoftp_invalid_settings),
                    getString(R.string.autoftp_invalid_summary),
                    getActivity());
            return false;
        }


        Utilities.ShowProgress(getActivity(), getString(R.string.autoftp_testing),
                getString(R.string.please_wait));


        helper.TestFtp( prefs.getString("autoftp_server", ""),
                prefs.getString("autoftp_username", ""),
                prefs.getString("autoftp_password", ""),
                prefs.getString("autoftp_directory", ""),
                Integer.valueOf(prefs.getString("autoftp_port", "21")),
                prefs.getBoolean("autoftp_useftps", false),
                prefs.getString("autoftp_ssltls", ""),
                prefs.getBoolean("autoftp_implicit", false));

        return true;
    }


    @Override
    public boolean IsValid() {
        return IsFormValid();
    }
}