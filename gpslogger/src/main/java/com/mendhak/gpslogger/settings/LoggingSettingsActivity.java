package com.mendhak.gpslogger.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.*;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.FileDialog.FileDialog;
import com.mendhak.gpslogger.common.Utilities;

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
public class LoggingSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    SharedPreferences prefs;
    private final static int SELECT_FOLDER_DIALOG = 420;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            NavUtils.navigateUpFromSameTask(this);
            return true;
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
        gpsloggerFolder.setSummary(prefs.getString("gpslogger_folder", Environment.getExternalStorageDirectory() + "/GPSLogger"));

        CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
        chkLog_opengts.setOnPreferenceClickListener(this);

        /**
         * Logging Details - New file creation
         */
        ListPreference newFilePref = (ListPreference) findPreference("new_file_creation");
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


    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("gpslogger_folder")) {
            Intent intent = new Intent(getBaseContext(), FileDialog.class);
            intent.putExtra(FileDialog.START_PATH, prefs.getString("gpslogger_folder",
                    Environment.getExternalStorageDirectory() + "/GPSLogger"));

            intent.putExtra(FileDialog.CAN_SELECT_DIR, true);
            startActivityForResult(intent, SELECT_FOLDER_DIALOG);
            return true;
        }

        if (preference.getKey().equals("log_opengts")) {
            CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
            boolean opengts_enabled = prefs.getBoolean("opengts_enabled", false);

            if (chkLog_opengts.isChecked() && !opengts_enabled) {
                startActivity(new Intent("com.mendhak.gpslogger.OPENGTS_SETUP"));
            }
            return true;
        }

        return false;
    }

    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {

        if (requestCode == SELECT_FOLDER_DIALOG) {
            if (resultCode == Activity.RESULT_OK) {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                Utilities.LogDebug("Folder path selected" + filePath);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("gpslogger_folder", filePath);
                editor.commit();

                Preference gpsloggerFolder = (Preference) findPreference("gpslogger_folder");
                gpsloggerFolder.setSummary(filePath);

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Utilities.LogDebug("No file selected");
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().equals("new_file_creation")) {

            boolean isFileStaticEnabled = newValue.equals("static");
            Preference prefFileStaticName = (Preference) findPreference("new_file_static_name");
            prefFileStaticName.setEnabled(isFileStaticEnabled);

            return true;
        }
        return false;
    }
}
