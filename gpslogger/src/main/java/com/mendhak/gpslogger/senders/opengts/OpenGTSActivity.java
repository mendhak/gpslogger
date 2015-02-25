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

package com.mendhak.gpslogger.senders.opengts;

import android.content.Intent;
import android.os.Bundle;
import android.preference.SwitchPreference;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.URLUtil;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

public class OpenGTSActivity extends PreferenceActivity implements
        OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OpenGTSActivity.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.opengtssettings);

        SwitchPreference chkEnabled = (SwitchPreference) findPreference("autoopengts_enabled");
        MaterialEditTextPreference txtOpenGTSServer = (MaterialEditTextPreference) findPreference("opengts_server");
        MaterialEditTextPreference txtOpenGTSServerPort = (MaterialEditTextPreference) findPreference("opengts_server_port");
        MaterialListPreference txtOpenGTSCommunicationMethod = (MaterialListPreference) findPreference("opengts_server_communication_method");
        MaterialEditTextPreference txtOpenGTSServerPath = (MaterialEditTextPreference) findPreference("autoopengts_server_path");
        MaterialEditTextPreference txtOpenGTSDeviceId = (MaterialEditTextPreference) findPreference("opengts_device_id");

        chkEnabled.setOnPreferenceChangeListener(this);
        txtOpenGTSServer.setOnPreferenceChangeListener(this);
        txtOpenGTSServerPort.setOnPreferenceChangeListener(this);
        txtOpenGTSCommunicationMethod.setOnPreferenceChangeListener(this);
        txtOpenGTSServerPath.setOnPreferenceChangeListener(this);
        txtOpenGTSDeviceId.setOnPreferenceChangeListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent intent = new Intent(this, GpsMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public boolean onPreferenceClick(Preference preference) {
        if (!IsFormValid()) {
            Utilities.MsgBox(getString(R.string.autoopengts_invalid_form),
                    getString(R.string.autoopengts_invalid_form_message),
                    OpenGTSActivity.this);
            return false;
        }
        return true;
    }

    private boolean IsFormValid() {
        SwitchPreference chkEnabled = (SwitchPreference) findPreference("opengts_enabled");
        MaterialEditTextPreference txtOpenGTSServer = (MaterialEditTextPreference) findPreference("opengts_server");
        MaterialEditTextPreference txtOpenGTSServerPort = (MaterialEditTextPreference) findPreference("opengts_server_port");
        MaterialListPreference txtOpenGTSCommunicationMethod = (MaterialListPreference) findPreference("opengts_server_communication_method");
        MaterialEditTextPreference txtOpenGTSServerPath = (MaterialEditTextPreference) findPreference("autoopengts_server_path");
        MaterialEditTextPreference txtOpenGTSDeviceId = (MaterialEditTextPreference) findPreference("opengts_device_id");

        return !chkEnabled.isChecked()
                || txtOpenGTSServer.getText() != null && txtOpenGTSServer.getText().length() > 0
                && txtOpenGTSServerPort.getText() != null && isNumeric(txtOpenGTSServerPort.getText())
                && txtOpenGTSCommunicationMethod.getValue() != null && txtOpenGTSCommunicationMethod.getValue().length() > 0
                && txtOpenGTSDeviceId.getText() != null && txtOpenGTSDeviceId.getText().length() > 0
                && URLUtil.isValidUrl("http://" + txtOpenGTSServer.getText() + ":" + txtOpenGTSServerPort.getText() + txtOpenGTSServerPath.getText());

    }

    private static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!IsFormValid()) {
                Utilities.MsgBox(getString(R.string.autoopengts_invalid_form),
                        getString(R.string.autoopengts_invalid_form_message),
                        this);
                return false;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

}
