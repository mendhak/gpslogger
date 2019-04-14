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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.ui.components.CustomSwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class PerformanceSettingsFragment  extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_performance);

        findPreference(PreferenceNames.LOG_SATELLITE_LOCATIONS).setOnPreferenceChangeListener(this);
        findPreference(PreferenceNames.LOG_NETWORK_LOCATIONS).setOnPreferenceChangeListener(this);


    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {

        if(preference.getKey().equals(PreferenceNames.LOG_SATELLITE_LOCATIONS)){
            boolean newValue = Boolean.parseBoolean(o.toString());
            if(!newValue){
                PreferenceHelper.getInstance().setShouldLogNetworkLocations(true);
                ((CustomSwitchPreference)findPreference(PreferenceNames.LOG_NETWORK_LOCATIONS)).setChecked(true);
            }
        }

        if(preference.getKey().equals(PreferenceNames.LOG_NETWORK_LOCATIONS)){
            boolean newValue = Boolean.parseBoolean(o.toString());
            if(!newValue){
                PreferenceHelper.getInstance().setShouldLogSatelliteLocations(true);
                ((CustomSwitchPreference)findPreference(PreferenceNames.LOG_SATELLITE_LOCATIONS)).setChecked(true);
            }
        }

        return true;
    }
}
