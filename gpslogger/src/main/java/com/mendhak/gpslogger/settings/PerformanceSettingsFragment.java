package com.mendhak.gpslogger.settings;


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

        Preference prefListeners = findPreference("listeners");
        prefListeners.setOnPreferenceClickListener(this);
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
