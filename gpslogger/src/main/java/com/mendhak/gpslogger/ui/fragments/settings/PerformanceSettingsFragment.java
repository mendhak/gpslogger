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
import android.text.InputType;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class PerformanceSettingsFragment
        extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, SimpleDialog.OnDialogResultListener {

    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(PreferenceNames.MINIMUM_INTERVAL).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.MINIMUM_INTERVAL).setSummary(String.valueOf(preferenceHelper.getMinimumLoggingInterval()) + getString(R.string.seconds));

        findPreference(PreferenceNames.MINIMUM_DISTANCE).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.MINIMUM_DISTANCE).setSummary(String.valueOf(preferenceHelper.getMinimumDistanceInterval()) + getString(R.string.meters));

        findPreference(PreferenceNames.MINIMUM_ACCURACY).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.MINIMUM_ACCURACY).setSummary(String.valueOf(preferenceHelper.getMinimumAccuracy()) + getString(R.string.meters));


        findPreference(PreferenceNames.LOGGING_RETRY_TIME).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.LOGGING_RETRY_TIME).setSummary(String.valueOf(preferenceHelper.getLoggingRetryPeriod()) + getString(R.string.seconds));

        findPreference(PreferenceNames.ABSOLUTE_TIMEOUT).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.ABSOLUTE_TIMEOUT).setSummary(String.valueOf(preferenceHelper.getAbsoluteTimeoutForAcquiringPosition()) + getString(R.string.seconds));

        findPreference(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET).setSummary(String.valueOf(preferenceHelper.getSubtractAltitudeOffset()) + getString(R.string.meters));

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_performance, rootKey);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equalsIgnoreCase(PreferenceNames.MINIMUM_INTERVAL)){
            SimpleFormDialog.build()
                    .title(R.string.time_before_logging_dialog_title)
                    .msg(R.string.time_before_logging_summary)
                    .fields(
                            Input.plain(PreferenceNames.MINIMUM_INTERVAL).hint(R.string.time_before_logging_hint).inputType(InputType.TYPE_CLASS_NUMBER).required().text(String.valueOf(preferenceHelper.getMinimumLoggingInterval())).max(4)
                    )
                    .show(this, PreferenceNames.MINIMUM_INTERVAL);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.MINIMUM_DISTANCE)){
            SimpleFormDialog.build()
                    .title(R.string.settings_distance_in_meters)
                    .msg(R.string.distance_filter_summary)
                    .fields(
                            Input.plain(PreferenceNames.MINIMUM_DISTANCE).hint(R.string.settings_enter_meters).inputType(InputType.TYPE_CLASS_NUMBER).required().text(String.valueOf(preferenceHelper.getMinimumDistanceInterval())).max(4)
                    )
                    .show(this, PreferenceNames.MINIMUM_DISTANCE);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.MINIMUM_ACCURACY)){
            SimpleFormDialog.build()
                    .title(R.string.settings_accuracy_in_meters)
                    .msg(R.string.accuracy_filter_summary)
                    .fields(
                            Input.plain(PreferenceNames.MINIMUM_ACCURACY).hint(R.string.settings_enter_meters).inputType(InputType.TYPE_CLASS_NUMBER).required().text(String.valueOf(preferenceHelper.getMinimumAccuracy())).max(4)
                    )
                    .show(this, PreferenceNames.MINIMUM_ACCURACY);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOGGING_RETRY_TIME)){
            SimpleFormDialog.build()
                    .title(R.string.time_before_logging_dialog_title)
                    .msg(R.string.retry_time_summary)
                    .fields(
                            Input.plain(PreferenceNames.LOGGING_RETRY_TIME).hint(R.string.time_before_logging_hint).inputType(InputType.TYPE_CLASS_NUMBER).required().text(String.valueOf(preferenceHelper.getLoggingRetryPeriod())).max(4)
                    )
                    .show(this, PreferenceNames.LOGGING_RETRY_TIME);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.ABSOLUTE_TIMEOUT)){
            SimpleFormDialog.build()
                    .title(R.string.time_before_logging_dialog_title)
                    .msg(R.string.absolute_timeout_summary)
                    .fields(
                            Input.plain(PreferenceNames.ABSOLUTE_TIMEOUT).hint(R.string.time_before_logging_hint).inputType(InputType.TYPE_CLASS_NUMBER).required().text(String.valueOf(preferenceHelper.getAbsoluteTimeoutForAcquiringPosition())).max(4)
                    )
                    .show(this, PreferenceNames.ABSOLUTE_TIMEOUT);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET)){
            SimpleFormDialog.build()
                    .title(R.string.settings_enter_meters)
                    .msg(R.string.altitude_subtractoffset_summary)
                    .fields(
                            Input.plain(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET).inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED).required().text(String.valueOf(preferenceHelper.getSubtractAltitudeOffset()))
                    )
                    .show(this, PreferenceNames.ALTITUDE_SUBTRACT_OFFSET);
            return true;
        }

        return false;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(which != BUTTON_POSITIVE){ return true; }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.MINIMUM_INTERVAL)){
            String minInterval = extras.getString(PreferenceNames.MINIMUM_INTERVAL);
            preferenceHelper.setMinimumLoggingInterval(Integer.valueOf(minInterval));
            findPreference(PreferenceNames.MINIMUM_INTERVAL).setSummary(String.valueOf(preferenceHelper.getMinimumLoggingInterval()) + getString(R.string.seconds));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.MINIMUM_DISTANCE)){
            String dist = extras.getString(PreferenceNames.MINIMUM_DISTANCE);
            preferenceHelper.setMinimumDistanceInMeters(Integer.valueOf(dist));
            findPreference(PreferenceNames.MINIMUM_DISTANCE).setSummary(String.valueOf(preferenceHelper.getMinimumDistanceInterval()) + getString(R.string.meters));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.MINIMUM_ACCURACY)){
            String dist = extras.getString(PreferenceNames.MINIMUM_ACCURACY);
            preferenceHelper.setMinimumAccuracy(Integer.valueOf(dist));
            findPreference(PreferenceNames.MINIMUM_ACCURACY).setSummary(String.valueOf(preferenceHelper.getMinimumAccuracy()) + getString(R.string.meters));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.LOGGING_RETRY_TIME)){
            String time = extras.getString(PreferenceNames.LOGGING_RETRY_TIME);
            preferenceHelper.setLoggingRetryPeriod(Integer.valueOf(time));
            findPreference(PreferenceNames.LOGGING_RETRY_TIME).setSummary(String.valueOf(preferenceHelper.getLoggingRetryPeriod()) + getString(R.string.seconds));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.ABSOLUTE_TIMEOUT)){
            String time = extras.getString(PreferenceNames.ABSOLUTE_TIMEOUT);
            preferenceHelper.setAbsoluteTimeoutForAcquiringPosition(Integer.valueOf(time));
            findPreference(PreferenceNames.ABSOLUTE_TIMEOUT).setSummary(String.valueOf(preferenceHelper.getAbsoluteTimeoutForAcquiringPosition()) + getString(R.string.seconds));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET)){
            String offset = extras.getString(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET);
            preferenceHelper.setSubtractAltitudeOffset(offset);
            findPreference(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET).setSummary(String.valueOf(preferenceHelper.getSubtractAltitudeOffset()) + getString(R.string.meters));
            return true;
        }

        return false;
    }
}
