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

package com.mendhak.gpslogger.ui.fragments.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.InputType;
import android.widget.EditText;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.mendhak.gpslogger.MainPreferenceActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.components.CustomSwitchPreference;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;
import org.slf4j.Logger;

import java.io.File;
import java.text.MessageFormat;


public class LoggingSettingsFragment extends PreferenceFragment
        implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener,
        DirectoryChooserFragment.OnFragmentInteractionListener
{

    private static final Logger LOG = Logs.of(LoggingSettingsFragment.class);
    private DirectoryChooserFragment folderDialog;
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_logging);

        Preference gpsloggerFolder = findPreference("gpslogger_folder");
        gpsloggerFolder.setOnPreferenceClickListener(this);
        String gpsLoggerFolderPath = preferenceHelper.getGpsLoggerFolder();
        gpsloggerFolder.setSummary(gpsLoggerFolderPath);
        if(!(new File(gpsLoggerFolderPath)).canWrite()){
            gpsloggerFolder.setSummary(Html.fromHtml("<font color='red'>" + gpsLoggerFolderPath + "</font>"));
        }

        /**
         * Logging Details - New file creation
         */
        MaterialListPreference newFilePref = (MaterialListPreference) findPreference("new_file_creation");
        newFilePref.setOnPreferenceChangeListener(this);
        /* Trigger artificially the listener and perform validations. */
        newFilePref.getOnPreferenceChangeListener()
                .onPreferenceChange(newFilePref, newFilePref.getValue());

        CustomSwitchPreference chkfile_prefix_serial = (CustomSwitchPreference) findPreference("new_file_prefix_serial");
        if (Strings.isNullOrEmpty(Strings.getBuildSerial())) {
            chkfile_prefix_serial.setEnabled(false);
            chkfile_prefix_serial.setSummary("This option not available on older phones or if a serial id is not present");
        } else {
            chkfile_prefix_serial.setEnabled(true);
            chkfile_prefix_serial.setSummary(chkfile_prefix_serial.getSummary().toString() + "(" + Strings.getBuildSerial() + ")");
        }


        Preference prefNewFileCustomName = findPreference("new_file_custom_name");
        prefNewFileCustomName.setOnPreferenceClickListener(this);


        CustomSwitchPreference prefCustomUrl = (CustomSwitchPreference)findPreference("log_customurl_enabled");
        prefCustomUrl.setOnPreferenceChangeListener(this);

        CustomSwitchPreference chkLog_opengts = (CustomSwitchPreference) findPreference("log_opengts");
        chkLog_opengts.setOnPreferenceChangeListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();

        setPreferencesEnabledDisabled();
    }



    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("gpslogger_folder")) {

            final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                    .initialDirectory(preferenceHelper.getGpsLoggerFolder())
                    .newDirectoryName("GPSLogger")
                    .allowReadOnlyDirectory(false)
                    .allowNewDirectoryNameModification(true)
                    .build();

            folderDialog = DirectoryChooserFragment.newInstance(config);
            folderDialog.setTargetFragment(this, 0);
            folderDialog.show(getActivity().getFragmentManager(), null);

            return true;
        }


        if(preference.getKey().equalsIgnoreCase("new_file_custom_name")){


            new MaterialDialog.Builder(getActivity())
                    .title(R.string.new_file_custom_title)
                    .content(R.string.new_file_custom_message)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .negativeText(R.string.cancel)
                    .input(getString(R.string.letters_numbers), preferenceHelper.getCustomFileName(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog materialDialog, CharSequence input) {
                            preferenceHelper.setCustomFileName(input.toString());
                        }
                    })
                    .show();

        }

        return false;
    }


    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {

        if (preference.getKey().equalsIgnoreCase("log_opengts")) {

            if(!((CustomSwitchPreference) preference).isChecked() && (Boolean)newValue  ) {

                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS);
                startActivity(targetActivity);
            }

            return true;
        }

        if(preference.getKey().equalsIgnoreCase("log_customurl_enabled") ){

            // Bug in SwitchPreference: http://stackoverflow.com/questions/19503931/switchpreferences-calls-multiple-times-the-onpreferencechange-method
            // Check if isChecked == false && newValue == true
            if(!((CustomSwitchPreference) preference).isChecked() && (Boolean)newValue  ) {
                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.CUSTOMURL);
                startActivity(targetActivity);
           }

            return true;
        }

        if (preference.getKey().equals("new_file_creation")) {

            Preference prefFileStaticName = findPreference("new_file_custom_name");
            Preference prefAskEachTime = findPreference("new_file_custom_each_time");
            Preference prefSerialPrefix = findPreference("new_file_prefix_serial");
            prefAskEachTime.setEnabled(newValue.equals("custom"));
            prefFileStaticName.setEnabled(newValue.equals("custom"));
            prefSerialPrefix.setEnabled(!newValue.equals("custom"));


            return true;
        }
        return false;
    }

    private void setPreferencesEnabledDisabled() {

        Preference prefFileStaticName = findPreference("new_file_custom_name");
        Preference prefAskEachTime = findPreference("new_file_custom_each_time");
        Preference prefSerialPrefix = findPreference("new_file_prefix_serial");

        prefFileStaticName.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefAskEachTime.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefSerialPrefix.setEnabled(!preferenceHelper.shouldCreateCustomFile());
    }

    @Override
    public void onSelectDirectory(@NonNull String folderPath) {
        folderDialog.dismiss();

        LOG.debug(folderPath);

        File folder = new File(folderPath);

        if(!folder.isDirectory()) {
            folderPath = folder.getParent();
        }

        if(!folder.canWrite()){
            Dialogs.alert(getString(R.string.sorry), getString(R.string.pref_logging_file_no_permissions), getActivity());
            return;
        }

        preferenceHelper.setGpsLoggerFolder(folderPath);
        Preference gpsloggerFolder = findPreference("gpslogger_folder");
        gpsloggerFolder.setSummary(folderPath);
    }

    @Override
    public void onCancelChooser() {
        folderDialog.dismiss();
    }
}
