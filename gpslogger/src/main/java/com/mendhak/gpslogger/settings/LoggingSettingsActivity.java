/*******************************************************************************
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
 ******************************************************************************/

package com.mendhak.gpslogger.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.FileDialog.FolderSelectorDialog;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@SuppressWarnings("deprecation")
public class LoggingSettingsActivity extends PreferenceActivity
        implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener,
        FolderSelectorDialog.FolderSelectCallback
{

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(LoggingSettingsActivity.class.getSimpleName());
    SharedPreferences prefs;
    private final static int SELECT_FOLDER_DIALOG = 420;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (prefs.getString("new_file_creation", "onceaday").equals("static")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("new_file_creation", "custom");
            editor.commit();

            MaterialListPreference newFileCreation = (MaterialListPreference) findPreference("new_file_creation");
            if(newFileCreation !=null){
                newFileCreation.setValue("custom");
            }

        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        setPreferencesEnabledDisabled();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent intent = new Intent(this, GpsMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        addPreferencesFromResource(R.xml.pref_logging);


        Preference gpsloggerFolder = (Preference) findPreference("gpslogger_folder");
        gpsloggerFolder.setOnPreferenceClickListener(this);
        String gpsLoggerFolderPath = prefs.getString("gpslogger_folder", Utilities.GetDefaultStorageFolder(getApplicationContext()).getAbsolutePath());
        gpsloggerFolder.setSummary(gpsLoggerFolderPath);
        if(!(new File(gpsLoggerFolderPath)).canWrite()){
            gpsloggerFolder.setSummary(Html.fromHtml("<font color='red'>" + gpsLoggerFolderPath + "</font>"));
        }

        CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
        chkLog_opengts.setOnPreferenceClickListener(this);

        /**
         * Logging Details - New file creation
         */
        MaterialListPreference newFilePref = (MaterialListPreference) findPreference("new_file_creation");
        newFilePref.setOnPreferenceChangeListener(this);
        /* Trigger artificially the listener and perform validations. */
        newFilePref.getOnPreferenceChangeListener()
                .onPreferenceChange(newFilePref, newFilePref.getValue());

        CheckBoxPreference chkfile_prefix_serial = (CheckBoxPreference) findPreference("new_file_prefix_serial");
        if (Utilities.IsNullOrEmpty(Utilities.GetBuildSerial())) {
            chkfile_prefix_serial.setEnabled(false);
            chkfile_prefix_serial.setSummary("This option not available on older phones or if a serial id is not present");
        } else {
            chkfile_prefix_serial.setSummary(chkfile_prefix_serial.getSummary().toString() + "(" + Utilities.GetBuildSerial() + ")");
        }


        Preference prefNewFileCustomName = (Preference)findPreference("new_file_custom_name");
        prefNewFileCustomName.setOnPreferenceClickListener(this);

        Preference prefListeners = (Preference)findPreference("listeners");
        prefListeners.setOnPreferenceClickListener(this);

    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("gpslogger_folder")) {
            new FolderSelectorDialog().show(LoggingSettingsActivity.this);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase("listeners")){

            final SharedPreferences.Editor editor = prefs.edit();
            final Set<String> currentListeners = prefs.getStringSet("listeners", new HashSet<String>(Utilities.GetListeners()));
            ArrayList<Integer> chosenIndices = new ArrayList<Integer>();
            final List<String> defaultListeners = Utilities.GetListeners();

            for(String chosenListener : currentListeners){
                chosenIndices.add(defaultListeners.indexOf(chosenListener));
            }

            new MaterialDialog.Builder(this)
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
        }

        if(preference.getKey().equalsIgnoreCase("new_file_custom_name")){

            MaterialDialog alertDialog = new MaterialDialog.Builder(LoggingSettingsActivity.this)
                    .title(R.string.new_file_custom_title)
                    .customView(R.layout.alertview)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            EditText userInput = (EditText) dialog.getCustomView().findViewById(R.id.alert_user_input);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("new_file_custom_name", userInput.getText().toString());
                            editor.commit();
                        }
                    }).build();

            EditText userInput = (EditText) alertDialog.getCustomView().findViewById(R.id.alert_user_input);
            userInput.setText(prefs.getString("new_file_custom_name","gpslogger"));
            TextView tvMessage = (TextView)alertDialog.getCustomView().findViewById(R.id.alert_user_message);
            tvMessage.setText(R.string.new_file_custom_message);
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();
        }

        if (preference.getKey().equals("log_opengts")) {
            CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");

            if (chkLog_opengts.isChecked()) {
                startActivity(new Intent("com.mendhak.gpslogger.OPENGTS_SETUP"));
            }
            return true;
        }

        return false;
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().equals("new_file_creation")) {

            Preference prefFileStaticName = (Preference) findPreference("new_file_custom_name");
            Preference prefAskEachTime = (Preference)findPreference("new_file_custom_each_time");
            Preference prefSerialPrefix = (Preference) findPreference("new_file_prefix_serial");
            prefAskEachTime.setEnabled(newValue.equals("custom"));
            prefFileStaticName.setEnabled(newValue.equals("custom"));
            prefSerialPrefix.setEnabled(!newValue.equals("custom"));


            return true;
        }
        return false;
    }

    private void setPreferencesEnabledDisabled() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Preference prefFileStaticName = (Preference) findPreference("new_file_custom_name");
        Preference prefAskEachTime = (Preference)findPreference("new_file_custom_each_time");
        Preference prefSerialPrefix = (Preference) findPreference("new_file_prefix_serial");

        prefFileStaticName.setEnabled(prefs.getString("new_file_creation", "onceaday").equals("custom"));
        prefAskEachTime.setEnabled(prefs.getString("new_file_creation", "onceaday").equals("custom"));
        prefSerialPrefix.setEnabled(!prefs.getString("new_file_creation", "onceaday").equals("custom"));
    }

    @Override
    public void onFolderSelection(File folder) {
        String filePath = folder.getPath();

        if(!folder.isDirectory()) {
            filePath = folder.getParent();
        }
        tracer.debug("Folder path selected" + filePath);

        if(!folder.canWrite()){
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.pref_logging_file_no_permissions), LoggingSettingsActivity.this);
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("gpslogger_folder", filePath);
        editor.commit();

        Preference gpsloggerFolder = (Preference) findPreference("gpslogger_folder");
        gpsloggerFolder.setSummary(filePath);
    }
}
