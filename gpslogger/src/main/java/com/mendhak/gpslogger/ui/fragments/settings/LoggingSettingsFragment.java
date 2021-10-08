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

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.codekidlabs.storagechooser.StorageChooser;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.MainPreferenceActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import org.slf4j.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Check;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

public class LoggingSettingsFragment extends PreferenceFragmentCompat
        implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener,
        SimpleDialog.OnDialogResultListener
{

    private static final Logger LOG = Logs.of(LoggingSettingsFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Preference gpsloggerFolder = findPreference(PreferenceNames.GPSLOGGER_FOLDER);

        String gpsLoggerFolderPath = preferenceHelper.getGpsLoggerFolder();
        gpsloggerFolder.setDefaultValue(gpsLoggerFolderPath);
        gpsloggerFolder.setSummary(gpsLoggerFolderPath);
        gpsloggerFolder.setOnPreferenceClickListener(this);

        if(!(new File(gpsLoggerFolderPath)).canWrite()){
            gpsloggerFolder.setSummary(Html.fromHtml("<font color='red'>" + gpsLoggerFolderPath + "</font>"));
        }


        SwitchPreferenceCompat logGpx = findPreference(PreferenceNames.LOG_TO_GPX);
        SwitchPreferenceCompat logGpx11 = findPreference(PreferenceNames.LOG_AS_GPX_11);
        logGpx11.setTitle("      " + logGpx11.getTitle());
        logGpx11.setSummary("      " + logGpx11.getSummary());
        logGpx.setOnPreferenceChangeListener(this);
        logGpx11.setEnabled(logGpx.isChecked());


        Preference newFilePref = findPreference(PreferenceNames.NEW_FILE_CREATION_MODE);
        newFilePref.setOnPreferenceClickListener(this);
        newFilePref.setSummary(getFileCreationLabelFromValue(preferenceHelper.getNewFileCreationMode()));


        SwitchPreferenceCompat chkfile_prefix_serial = findPreference(PreferenceNames.PREFIX_SERIAL_TO_FILENAME);
        if (Strings.isNullOrEmpty(Strings.getBuildSerial())) {
            chkfile_prefix_serial.setEnabled(false);
            chkfile_prefix_serial.setSummary("This option not available on older phones or if a serial id is not present");
        } else {
            chkfile_prefix_serial.setEnabled(true);
            chkfile_prefix_serial.setSummary(chkfile_prefix_serial.getSummary().toString() + "(" + Strings.getBuildSerial() + ")");
        }


        findPreference(PreferenceNames.CUSTOM_FILE_NAME).setOnPreferenceClickListener(this);
        if(!Strings.isNullOrEmpty(preferenceHelper.getCustomFileName())){
            findPreference(PreferenceNames.CUSTOM_FILE_NAME).setSummary(preferenceHelper.getCustomFileName());
        }

        findPreference(PreferenceNames.LOG_TO_URL).setOnPreferenceChangeListener(this);
        findPreference(PreferenceNames.LOG_TO_OPENGTS).setOnPreferenceChangeListener(this);

        findPreference(PreferenceNames.LOGGING_WRITE_TIME_WITH_OFFSET).setOnPreferenceChangeListener(this);
        setPreferenceTimeZoneOffsetSummary(preferenceHelper.shouldWriteTimeWithOffset());

        findPreference("log_plain_text_csv_advanced").setOnPreferenceClickListener(this);
        setPreferenceCsvSummary(preferenceHelper.getCSVDelimiter(), preferenceHelper.shouldCSVUseCommaInsteadOfPoint());

    }

    private void setPreferenceCsvSummary(String delimiter, Boolean useComma){
        String sample = "lorem,ipsum,";
        String number = "12.345";
        sample = sample.replaceAll(",", delimiter);
        if(useComma){
            number = number.replace(".", ",");
        }
        findPreference("log_plain_text_csv_advanced").setSummary(sample+number);

    }

    private void setPreferenceTimeZoneOffsetSummary(boolean shouldIncludeOffset){
        String dateTimeString = Strings.getIsoDateTime(new Date());
        if(shouldIncludeOffset){
            dateTimeString = Strings.getIsoDateTimeWithOffset(new Date());
        }

        findPreference(PreferenceNames.LOGGING_WRITE_TIME_WITH_OFFSET).setSummary(getString(R.string.file_logging_log_time_with_offset_summary) + " " + dateTimeString);
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_logging, rootKey);
    }


    @Override
    public void onResume() {
        super.onResume();

        setPreferencesEnabledDisabled();
    }



    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.NEW_FILE_CREATION_MODE)){

            String[] values = getResources().getStringArray(R.array.filecreation_values);
            ArrayList<String> valuesArray = new ArrayList<>(Arrays.asList(values));
            int position = valuesArray.indexOf(preferenceHelper.getNewFileCreationMode());

            SimpleListDialog.build()
                    .title(R.string.new_file_creation_title)
                    .msg(R.string.new_file_creation_summary)
                    .pos(R.string.ok)
                    .items(getActivity(), R.array.filecreation_entries)
                    .choiceMode(SimpleListDialog.SINGLE_CHOICE_DIRECT)
                    .choicePreset(position)
                    .show(this, PreferenceNames.NEW_FILE_CREATION_MODE);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase("log_plain_text_csv_advanced")){
            SimpleFormDialog.build()
                    .title(R.string.log_plain_text_csv_advanced_title)
                    .pos(R.string.ok)
                    .neg(R.string.cancel)
                    .fields(
                            Input.plain(PreferenceNames.LOG_TO_CSV_DELIMITER)
                                    .hint(R.string.log_plain_text_csv_field_delimiter)
                                    .text(preferenceHelper.getCSVDelimiter())
                                    .max(1)
                                    .min(1)
                                    .required(),
                            Check.box(PreferenceNames.LOG_TO_CSV_DECIMAL_COMMA)
                                    .label(R.string.log_plain_text_decimal_comma)
                                    .check(preferenceHelper.shouldCSVUseCommaInsteadOfPoint())
                    )
                    .show(this,"log_plain_text_csv_advanced");
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.GPSLOGGER_FOLDER)){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                SimpleDialog.build()
                        .title(R.string.error)
                        .msg(R.string.gpslogger_custom_path_need_permission)
                        .show(this, "FILE_PERMISSIONS_REQUIRED");

                return false;
            }

            StorageChooser chooser = Dialogs.directoryChooser(getActivity());
            chooser.setOnSelectListener(path -> {
                LOG.debug(path);
                if(Strings.isNullOrEmpty(path)) {
                    path = Files.storageFolder(getActivity()).getAbsolutePath();
                }
                File testFile = new File(path, "testfile.txt");
                try {
                    testFile.createNewFile();
                    if(testFile.exists()){
                        testFile.delete();
                        LOG.debug("Test file successfully created and deleted.");
                    }
                } catch (Exception ex) {
                    LOG.error("Could not create a test file in the chosen directory.", ex);
                    path = preferenceHelper.getGpsLoggerFolder();
                    Dialogs.alert(getString(R.string.error), getString(R.string.pref_logging_file_no_permissions), getActivity());
                }

                findPreference(PreferenceNames.GPSLOGGER_FOLDER).setSummary(path);
                preferenceHelper.setGpsLoggerFolder(path);

            });
            chooser.show();
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.CUSTOM_FILE_NAME)){

            SimpleFormDialog.build()
                    .title(R.string.new_file_custom_title)
                    .msgHtml(getString(R.string.new_file_custom_summary) + "<br /><br/>" +  getString(R.string.new_file_custom_message))
                    .pos(R.string.ok)
                    .neg(R.string.cancel)
                    .fields(
                            Input.plain(PreferenceNames.CUSTOM_FILE_NAME)
                                    .hint(R.string.letters_numbers)
                                    .text(preferenceHelper.getCustomFileName())
                                    .required()
                    )
                    .show(this,PreferenceNames.CUSTOM_FILE_NAME);
        }

        return false;
    }


    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_GPX)){
            SwitchPreferenceCompat logGpx11 = findPreference(PreferenceNames.LOG_AS_GPX_11);
            logGpx11.setEnabled((Boolean)newValue);
            return true;
        }


        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOGGING_WRITE_TIME_WITH_OFFSET)){
            setPreferenceTimeZoneOffsetSummary(Boolean.valueOf(newValue.toString()));
            return true;
        }

        if (preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_OPENGTS)) {

            if(!((SwitchPreferenceCompat) preference).isChecked() && (Boolean)newValue  ) {

                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS);
                startActivity(targetActivity);
            }

            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.LOG_TO_URL) ){

            // Bug in SwitchPreference: http://stackoverflow.com/questions/19503931/switchpreferences-calls-multiple-times-the-onpreferencechange-method
            // Check if isChecked == false && newValue == true
            if(!((SwitchPreferenceCompat) preference).isChecked() && (Boolean)newValue  ) {
                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.CUSTOMURL);
                startActivity(targetActivity);
           }

            return true;
        }

        return false;
    }


    private void setPreferencesEnabledDisabled() {

        Preference prefFileCustomName = findPreference(PreferenceNames.CUSTOM_FILE_NAME);
        Preference prefAskEachTime = findPreference(PreferenceNames.ASK_CUSTOM_FILE_NAME);
        Preference prefSerialPrefix = findPreference(PreferenceNames.PREFIX_SERIAL_TO_FILENAME);
        Preference prefDynamicFileName = findPreference(PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING);

        prefFileCustomName.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefAskEachTime.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefSerialPrefix.setEnabled(!preferenceHelper.shouldCreateCustomFile());
        prefDynamicFileName.setEnabled(preferenceHelper.shouldCreateCustomFile());
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(which != BUTTON_POSITIVE){ return true; }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.CUSTOM_FILE_NAME)){
            String customFilename = extras.getString(PreferenceNames.CUSTOM_FILE_NAME);
            preferenceHelper.setCustomFileName(customFilename);
            findPreference(PreferenceNames.CUSTOM_FILE_NAME).setSummary(preferenceHelper.getCustomFileName());
        }

        if(dialogTag.equalsIgnoreCase("FILE_PERMISSIONS_REQUIRED")){
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            getActivity().startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.NEW_FILE_CREATION_MODE)){
            String chosenLabel = extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL);
            String chosenValue = getFileCreationValueFromLabel(chosenLabel);

            preferenceHelper.setNewFileCreationMode(chosenValue);
            findPreference(PreferenceNames.NEW_FILE_CREATION_MODE).setSummary(chosenLabel);
            setPreferencesEnabledDisabled();
            return true;
        }

        if(dialogTag.equalsIgnoreCase("log_plain_text_csv_advanced")){
            String delimiter = extras.getString(PreferenceNames.LOG_TO_CSV_DELIMITER);
            boolean useComma = extras.getBoolean(PreferenceNames.LOG_TO_CSV_DECIMAL_COMMA);
            preferenceHelper.setCSVDelimiter(delimiter);
            preferenceHelper.setShouldCSVUseCommaInsteadOfDecimal(useComma);
            setPreferenceCsvSummary(preferenceHelper.getCSVDelimiter(), preferenceHelper.shouldCSVUseCommaInsteadOfPoint());
            return true;
        }

        return false;
    }

    private String getFileCreationLabelFromValue(String value){
        String[] values = getResources().getStringArray(R.array.filecreation_values);
        ArrayList<String> valuesArray = new ArrayList<>(Arrays.asList(values));
        int chosenIndex = valuesArray.indexOf(value);
        String[] labels = getResources().getStringArray(R.array.filecreation_entries);
        String chosenLabel = labels[chosenIndex];
        return chosenLabel;
    }

    private String getFileCreationValueFromLabel(String label){
        String[] labels = getResources().getStringArray(R.array.filecreation_entries);
        ArrayList<String> valuesArray = new ArrayList<>(Arrays.asList(labels));
        int chosenIndex = valuesArray.indexOf(label);

        String[] values = getResources().getStringArray(R.array.filecreation_values);
        String chosenValue = values[chosenIndex];
        return chosenValue;
    }

}
