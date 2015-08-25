package com.mendhak.gpslogger.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PerformanceSettingsFragment  extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_performance);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Preference prefListeners = (Preference)findPreference("listeners");
        prefListeners.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase("listeners")){

            final SharedPreferences.Editor editor = prefs.edit();
            final Set<String> currentListeners = AppSettings.getChosenListeners();
            ArrayList<Integer> chosenIndices = new ArrayList<Integer>();
            final List<String> defaultListeners = AppSettings.GetDefaultListeners();

            for(String chosenListener : currentListeners){
                chosenIndices.add(defaultListeners.indexOf(chosenListener));
            }

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.listeners_title)
                    .items(R.array.listeners)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .itemsCallbackMultiChoice(chosenIndices.toArray(new Integer[0]), new MaterialDialog.ListCallbackMultiChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {

                            List<Integer> selectedItems = Arrays.asList(integers);
                            final Set<String> chosenListeners = new HashSet<String>();

                            for (Integer selectedItem : selectedItems) {
                                chosenListeners.add(defaultListeners.get(selectedItem));
                            }

                            if (chosenListeners.size() > 0) {
                                editor.putStringSet("listeners", chosenListeners);
                                editor.apply();
                            }

                            return true;
                        }
                    }).show();

            return true;
        }

        return false;
    }
}
