package com.mendhak.gpslogger.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PerformanceSettingsFragment  extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(LoggingSettingsFragment.class.getSimpleName());
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
            final Set<String> currentListeners = prefs.getStringSet("listeners", new HashSet<String>(Utilities.GetListeners()));
            ArrayList<Integer> chosenIndices = new ArrayList<Integer>();
            final List<String> defaultListeners = Utilities.GetListeners();

            for(String chosenListener : currentListeners){
                chosenIndices.add(defaultListeners.indexOf(chosenListener));
            }

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.listeners_title)
                    .items(R.array.listeners)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .itemsCallbackMultiChoice(chosenIndices.toArray(new Integer[0]), new MaterialDialog.ListCallbackMulti() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {

                            List<Integer> selectedItems = Arrays.asList(integers);
                            final Set<String> chosenListeners = new HashSet<String>();

                            for (Integer selectedItem : selectedItems) {
                                tracer.debug(defaultListeners.get(selectedItem));
                                chosenListeners.add(defaultListeners.get(selectedItem));
                            }

                            if (chosenListeners.size() > 0) {
                                editor.putStringSet("listeners", chosenListeners);
                                editor.commit();
                            }

                        }
                    }).show();

            return true;
        }

        return false;
    }
}
