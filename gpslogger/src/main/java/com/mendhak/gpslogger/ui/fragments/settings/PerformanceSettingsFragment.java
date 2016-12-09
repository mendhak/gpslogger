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

import java.util.ArrayList;
import java.util.List;

public class PerformanceSettingsFragment  extends PreferenceFragment implements Preference.OnPreferenceClickListener {


    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_performance);

        findPreference("listeners").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase("listeners")){

            ArrayList<Integer> chosenIndices = new ArrayList<>();
            final List<String> availableListeners = preferenceHelper.getAvailableListeners();

            for(String chosenListener : preferenceHelper.getChosenListeners()){
                chosenIndices.add(availableListeners.indexOf(chosenListener));
            }

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.listeners_title)
                    .items(R.array.listeners)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .itemsCallbackMultiChoice(chosenIndices.toArray(new Integer[chosenIndices.size()]), new MaterialDialog.ListCallbackMultiChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {

                            preferenceHelper.setChosenListeners(integers);

                            return true;
                        }
                    }).show();

            return true;
        }

        return false;
    }
}
