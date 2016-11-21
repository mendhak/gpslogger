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
import android.preference.EditTextPreference;
import android.preference.Preference;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;

import java.text.MessageFormat;

public class CustomUrlFragment extends PermissionedPreferenceFragment implements
        PreferenceValidator,
        Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.customurlsettings);

        EditTextPreference urlPathPreference = (EditTextPreference)findPreference(PreferenceNames.LOG_TO_URL_PATH);
        urlPathPreference.setSummary(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setText(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setOnPreferenceChangeListener(this);


        String legend1 = MessageFormat.format("{0} %LAT\n{1} %LON\n{2} %DESC\n{3} %SAT\n{4} %ALT",
                getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                getString(R.string.txt_satellites), getString(R.string.txt_altitude));

        Preference urlLegendPreference1 = (Preference)findPreference("customurl_legend_1");
        urlLegendPreference1.setSummary(legend1);

        String legend2 = MessageFormat.format("{0} %SPD\n{1} %ACC\n{2} %DIR\n{3} %PROV",
                getString(R.string.txt_speed),
                getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider)
                );

        Preference urlLegendPreference2 = (Preference)findPreference("customurl_legend_2");
        urlLegendPreference2.setSummary(legend2);

        String legend3 = MessageFormat.format("{0} %TIME\n{1} %BATT\n{2} %AID\n{3} %SER\n{4} %ACT",
                getString(R.string.txt_time_isoformat), "Battery:", "Android ID:", "Serial:", getString(R.string.txt_activity)
                );

        Preference urlLegendPreference3 = (Preference)findPreference("customurl_legend_3");
        urlLegendPreference3.setSummary(legend3);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference.getKey().equals(PreferenceNames.LOG_TO_URL_PATH)){
            preference.setSummary(newValue.toString());
        }
        return true;
    }


    @Override
    public boolean isValid() {
        return true;
    }
}
