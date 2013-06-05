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

package com.mendhak.gpslogger.senders.livetrack24;

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

public class Livetrack24Activity extends SherlockPreferenceActivity implements
        OnPreferenceChangeListener,
        OnPreferenceClickListener
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable the home button so you can go back to the main screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.livetrack24settings);

        CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("livetrack24_enabled");
        EditTextPreference txtlivetrack24Server = (EditTextPreference) findPreference("livetrack24_server_url");
        EditTextPreference txtlivetrack24Username = (EditTextPreference) findPreference("livetrack24_username");
        EditTextPreference txtlivetrack24Password = (EditTextPreference) findPreference("livetrack24_password");
        EditTextPreference txtlivetrack24Interval = (EditTextPreference) findPreference("livetrack24_interval");

        chkEnabled.setOnPreferenceChangeListener(this);
        txtlivetrack24Server.setOnPreferenceChangeListener(this);
        txtlivetrack24Username.setOnPreferenceChangeListener(this);
        txtlivetrack24Password.setOnPreferenceChangeListener(this);
        txtlivetrack24Interval.setOnPreferenceChangeListener(this);
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
            Utilities.MsgBox(getString(R.string.livetrack24_invalid_form),
                    getString(R.string.livetrack24_invalid_form_message),
                    Livetrack24Activity.this);
            return false;
        }
        return true;
    }

    private boolean IsFormValid()
    {
        CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("livetrack24_enabled");
        EditTextPreference txtlivetrack24Server = (EditTextPreference) findPreference("livetrack24_server_url");
        EditTextPreference txtlivetrack24Username = (EditTextPreference) findPreference("livetrack24_username");
        EditTextPreference txtlivetrack24Password = (EditTextPreference) findPreference("livetrack24_password");
        EditTextPreference txtlivetrack24Interval = (EditTextPreference) findPreference("livetrack24_interval");

        return !chkEnabled.isChecked()
                || txtlivetrack24Server.getText() != null && !txtlivetrack24Server.getText().isEmpty()
                && txtlivetrack24Username.getText() != null && !txtlivetrack24Username.getText().isEmpty()
                && txtlivetrack24Interval.getText() != null && isNumeric(txtlivetrack24Interval.getText())
                && txtlivetrack24Password.getText() != null && !txtlivetrack24Password.getText().isEmpty();
    }

    private static boolean isNumeric(String str)
    {
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
                Utilities.MsgBox(getString(R.string.livetrack24_invalid_form),
                        getString(R.string.livetrack24_invalid_form_message),
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
