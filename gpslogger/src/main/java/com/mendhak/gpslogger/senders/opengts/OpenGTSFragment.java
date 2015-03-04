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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.webkit.URLUtil;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceValidationFragment;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.views.component.CustomSwitchPreference;
import org.slf4j.LoggerFactory;

public class OpenGTSFragment extends PreferenceValidationFragment implements
        OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OpenGTSFragment.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.opengtssettings);

        CustomSwitchPreference chkEnabled = (CustomSwitchPreference) findPreference("autoopengts_enabled");
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

    public boolean onPreferenceClick(Preference preference) {
        if (!IsFormValid()) {
            Utilities.MsgBox(getString(R.string.autoopengts_invalid_form),
                    getString(R.string.autoopengts_invalid_form_message),
                    getActivity());
            return false;
        }
        return true;
    }

    private boolean IsFormValid() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        return !prefs.getBoolean("opengts_enabled", false)
                || prefs.getString("opengts_server","").length() > 0
                && isNumeric(prefs.getString("opengts_server_port",""))
                && prefs.getString("opengts_server_communication_method", "").length() > 0
                && prefs.getString("opengts_device_id", "").length() > 0
                && URLUtil.isValidUrl("http://" + prefs.getString("opengts_server", "") + ":" + prefs.getString("opengts_server_port", "") + prefs.getString("autoopengts_server_path",""));

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
        return true;
    }

    @Override
    public boolean IsValid() {
        return IsFormValid();
    }
}
