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
*
*    Copyright Marc Poulhiès <dkm@kataplop.net>
*/

package com.mendhak.gpslogger.senders.skylines;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.KeyEvent;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.mendhak.gpslogger.GpsMainActivity;
import net.kataplop.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;

public class SkylinesActivity extends SherlockPreferenceActivity implements
        OnPreferenceChangeListener,
        OnPreferenceClickListener
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable the home button so you can go back to the main screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.skylinessettings);

        CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("skylines_enabled");
        EditTextPreference txtSkyLinesServer = (EditTextPreference) findPreference("skylines_server");
        EditTextPreference txtSkyLinesServerPort = (EditTextPreference) findPreference("skylines_server_port");
        EditTextPreference txtSkyLinesKey = (EditTextPreference) findPreference("skylines_key");
        EditTextPreference txtSkyLinesInterval = (EditTextPreference) findPreference("skylines_interval");

        chkEnabled.setOnPreferenceChangeListener(this);
        txtSkyLinesServer.setOnPreferenceChangeListener(this);
        txtSkyLinesServerPort.setOnPreferenceChangeListener(this);
        txtSkyLinesKey.setOnPreferenceChangeListener(this);
        txtSkyLinesInterval.setOnPreferenceChangeListener(this);
    }

    /**
     * Called when one of the menu items is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

        switch (itemId)
        {
            case android.R.id.home:
                Intent intent = new Intent(this, GpsMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onPreferenceClick(Preference preference)
    {
        if (!IsFormValid())
        {
            Utilities.MsgBox(getString(R.string.skylines_invalid_form),
                    getString(R.string.skylines_invalid_form_message),
                    SkylinesActivity.this);
            return false;
        }
        return true;
    }

    private boolean IsFormValid()
    {
        CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("skylines_enabled");
        EditTextPreference txtSkyLinesServer = (EditTextPreference) findPreference("skylines_server");
        EditTextPreference txtSkyLinesServerPort = (EditTextPreference) findPreference("skylines_server_port");
        EditTextPreference txtSkyLinesKey = (EditTextPreference) findPreference("skylines_key");
        EditTextPreference txtSkyLinesInterval = (EditTextPreference) findPreference("skylines_interval");

        return !chkEnabled.isChecked()
                || txtSkyLinesServer.getText() != null && txtSkyLinesServer.getText().length() > 0
                && txtSkyLinesServerPort.getText() != null && isNumeric(txtSkyLinesServerPort.getText())
                && txtSkyLinesInterval.getText() != null && isNumeric(txtSkyLinesInterval.getText())
                && txtSkyLinesKey.getText() != null && txtSkyLinesKey.getText().length() > 0;
    }

    private static boolean isNumeric(String str)
    {
        if (str.isEmpty()){
            return false;
        }

        for (char c : str.toCharArray())
        {
            if (!Character.isDigit(c))
            {
                return false;
            }
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (!IsFormValid())
            {
                Utilities.MsgBox(getString(R.string.skylines_invalid_form),
                        getString(R.string.skylines_invalid_form_message),
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

    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        return true;
    }
}
