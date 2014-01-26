/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.osm.OSMHelper;

import net.kataplop.gpslogger.R;

import java.util.ArrayList;

public class GpsSettingsActivity extends SherlockPreferenceActivity
{

    private final Handler handler = new Handler();
    private SharedPreferences prefs;

    protected boolean filterAdvanced(PreferenceGroup pg){
        boolean reload = false;
        ArrayList<Preference> to_remove = new ArrayList<Preference>();

        for (int i=0; i < pg.getPreferenceCount(); i++){
            Preference p = pg.getPreference(i);
            if (p.getKey() != null && p.getKey().endsWith("_advancedP")){
                to_remove.add(p);
            } else if(p instanceof PreferenceGroup){
                PreferenceGroup pc = (PreferenceGroup)p;
                reload |= filterAdvanced(pc);
            }
        }
        for (Preference p : to_remove){
            pg.removePreference(p);
        }
        reload |= !to_remove.isEmpty();

        return reload;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable the home button so you can go back to the main screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.settings);

        if (getIntent().getBooleanExtra("autosend_preferencescreen_advancedP", false))
        {
            PreferenceScreen screen = (PreferenceScreen) findPreference("gpslogger_preferences");
            int pos = findPreference("autosend_preferencescreen_advancedP").getOrder();
            screen.onItemClick(null, null, pos, 0);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (prefs.getString("preference_mode", "simple").equals("simple")){
            filterAdvanced(getPreferenceScreen());
        }

        Preference prefmode = findPreference("preference_mode");
        if (prefmode != null)
            prefmode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        recreate();
                    } else {
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    }
                    return true;
                }
            });

        boolean useImperial = prefs.getBoolean("useImperial", false);

        EditTextPreference distanceBeforeLogging = (EditTextPreference) findPreference("distance_before_logging");

        if (useImperial)
        {
            distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_feet);
            distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);
        }
        else
        {
            distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_meters);
            distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
        }
        
        EditTextPreference accuracyBeforeLogging = (EditTextPreference) findPreference("accuracy_before_logging");

        if (useImperial)
        {
            accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_feet);
            accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);
        }
        else
        {
            accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_meters);
            accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
        }

        CheckBoxPreference imperialCheckBox = (CheckBoxPreference) findPreference("useImperial");
        if (imperialCheckBox != null)
            imperialCheckBox.setOnPreferenceChangeListener(new ImperialPreferenceChangeListener(prefs, distanceBeforeLogging, accuracyBeforeLogging));

        Preference enableDisablePref = findPreference("enableDisableGps");
        if (enableDisablePref != null)
            enableDisablePref.setOnPreferenceClickListener(new AndroidLocationPreferenceClickListener());

        Preference osmSetupPref = findPreference("osm_setup");
        if (osmSetupPref != null)
            osmSetupPref.setOnPreferenceClickListener(new OSMPreferenceClickListener());

        CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
        if (chkLog_opengts != null)
            chkLog_opengts.setOnPreferenceClickListener(new LogOpenGTSPreferenceClickListener(prefs));

        CheckBoxPreference chkLog_skylines = (CheckBoxPreference) findPreference("log_skylines");
        if (chkLog_skylines != null)
            chkLog_skylines.setOnPreferenceClickListener(new LogSkylinesPreferenceClickListener(prefs));

        CheckBoxPreference chkLog_livetrack24 = (CheckBoxPreference) findPreference("log_livetrack24");
        if (chkLog_livetrack24 != null)
            chkLog_livetrack24.setOnPreferenceClickListener(new LogLivetrack24PreferenceClickListener(prefs));

        ListPreference newFilePref = (ListPreference) findPreference("new_file_creation");
        if (newFilePref != null)
            newFilePref.setOnPreferenceChangeListener(new FileCreationPreferenceChangeListener());

        if (!prefs.getString("new_file_creation", "static").equals("static"))
//        if(!newFilePref.getValue().equals("static"))
        {
            Preference staticPref = (Preference)findPreference("new_file_static_name");
            staticPref.setEnabled(false);
        }
//
//        final ListPreference mode_selection= (ListPreference) findPreference("preference_mode");
//        if (mode_selection.getValue().equals("simple")){
//            removeAdvanced();
//        }
//        mode_selection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newVal) {
//                if (newVal.equals("simple")){
//                    removeAdvanced();
//                }
//                return true;
//            }
//        });
    }


    /**
     * Called when one of the menu items is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int itemId = item.getItemId();
        Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

        switch (itemId)
        {
            case android.R.id.home:
                Intent intent = new Intent(this, GpsMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }



    private final Runnable updateResults = new Runnable()
    {
        public void run()
        {
            finish();

            startActivity(getIntent());
        }

    };


    /**
     * Opens the Android Location preferences screen
     */
    private class AndroidLocationPreferenceClickListener implements OnPreferenceClickListener
    {
        public boolean onPreferenceClick(Preference preference)
        {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }
    }

    /**
     * Opens the OpenStreetMap preferences screen
     */
    private class OSMPreferenceClickListener implements OnPreferenceClickListener
    {

        public boolean onPreferenceClick(Preference preference)
        {
            Context cnt = getApplicationContext();
            Intent intent = OSMHelper.GetOsmSettingsIntent(cnt);
            startActivity(intent);
//            startActivity(OSMHelper.GetOsmSettingsIntent(getApplicationContext()));

            return true;
        }
    }

    private class FileCreationPreferenceChangeListener implements Preference.OnPreferenceChangeListener
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            System.out.print(newValue.toString());
            Preference staticPref = (Preference)findPreference("new_file_static_name");
            if(newValue.equals("static"))
            {
                staticPref.setEnabled(true);
            }
            else
            {
                staticPref.setEnabled(false);
            }

            return true;
        }
    }


    private class ImperialPreferenceChangeListener implements Preference.OnPreferenceChangeListener
    {
        EditTextPreference distanceBeforeLogging;
        EditTextPreference accuracyBeforeLogging;
        SharedPreferences prefs;

        public ImperialPreferenceChangeListener(SharedPreferences prefs, EditTextPreference distanceBeforeLogging, EditTextPreference accuracyBeforeLogging)
        {
            this.prefs = prefs;
            this.distanceBeforeLogging = accuracyBeforeLogging;
            this.accuracyBeforeLogging = accuracyBeforeLogging;
        }

        public boolean onPreferenceChange(Preference preference, final Object newValue)
        {

            Utilities.ShowProgress(GpsSettingsActivity.this, getString(R.string.settings_converting_title),
                    getString(R.string.settings_converting_description));

            new Thread()
            {

                public void run()
                {

                    try
                    {
                        sleep(3000); // Give user time to read the message
                    }
                    catch (InterruptedException e)
                    {

                        Log.e("Settings", e.getMessage());

                    }

                    boolean useImp = Boolean.parseBoolean(newValue.toString());

                    String minimumDistanceString = prefs.getString("distance_before_logging", "0");
                    String minimumAccuracyString = prefs.getString("accuracy_before_logging", "0");
                    
                    int minimumDistance;

                    if (minimumDistanceString != null && minimumDistanceString.length() > 0)
                    {
                        minimumDistance = Integer.valueOf(minimumDistanceString);
                    }
                    else
                    {
                        minimumDistance = 0;
                    }
                    
                    int minimumAccuracy;

                    if (minimumAccuracyString != null && minimumAccuracyString.length() > 0)
                    {
                        minimumAccuracy = Integer.valueOf(minimumAccuracyString);
                    }
                    else
                    {
                        minimumAccuracy = 0;
                    }

                    SharedPreferences.Editor editor = prefs.edit();

                    if (useImp)
                    {
                        distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_feet);
                        distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);

                        minimumDistance = Utilities.MetersToFeet(minimumDistance);
                        
                        accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_feet);
                        accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);

                        minimumAccuracy = Utilities.MetersToFeet(minimumAccuracy);
                    }
                    else
                    {
                        minimumDistance = Utilities.FeetToMeters(minimumDistance);
                        distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_meters);
                        distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
                                                    
                        minimumAccuracy = Utilities.FeetToMeters(minimumAccuracy);
                        accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_meters);
                        accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
                        

                    }

                    if (minimumDistance >= 9999)
                    {
                        minimumDistance = 9999;
                    }
                    
                    if (minimumAccuracy >= 9999)
                    {
                        minimumAccuracy = 9999;
                    }

                    editor.putString("distance_before_logging", String.valueOf(minimumDistance));
                    
                    editor.putString("accuracy_before_logging", String.valueOf(minimumAccuracy));
                    editor.commit();

                    handler.post(updateResults);
                    Utilities.HideProgress();
                }
            }.start();

            return true;
        }

    }

    /**
     * Opens the OpenGTS preferences
     * Listener to ensure that the server is configured when the user wants to enable OpenGTS logging logger
     */
    private class LogOpenGTSPreferenceClickListener implements OnPreferenceClickListener
    {
        private SharedPreferences prefs;

        public LogOpenGTSPreferenceClickListener(SharedPreferences prefs)
        {
            this.prefs = prefs;
        }

        public boolean onPreferenceClick(Preference preference)
        {
            CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
            boolean opengts_enabled = prefs.getBoolean("opengts_enabled", false);

            if (chkLog_opengts.isChecked() && !opengts_enabled)
            {
                startActivity(new Intent("com.mendhak.gpslogger.OPENGTS_SETUP"));
            }
            return true;
        }
    }

      /**
     * Opens the SkyLines preferences
     * Listener to ensure that the server is configured when the user wants to enable SkyLines logging logger
     */
    private class LogSkylinesPreferenceClickListener implements OnPreferenceClickListener
    {
        private SharedPreferences prefs;

        public LogSkylinesPreferenceClickListener(SharedPreferences prefs)
        {
            this.prefs = prefs;
        }

        public boolean onPreferenceClick(Preference preference)
        {
            CheckBoxPreference chkLog_skylines = (CheckBoxPreference) findPreference("log_skylines");
            boolean skylines_enabled = prefs.getBoolean("skylines_enabled", false);

            if (chkLog_skylines.isChecked() && !skylines_enabled)
            {
                startActivity(new Intent("com.mendhak.gpslogger.SKYLINES_SETUP"));
            }
            return true;
        }
    }


      /**
     * Opens the Livetrack24 preferences
     * Listener to ensure that the server is configured when the user wants to enable Livetrack24 logging logger
     */
    private class LogLivetrack24PreferenceClickListener implements OnPreferenceClickListener
    {
        private SharedPreferences prefs;

        public LogLivetrack24PreferenceClickListener(SharedPreferences prefs)
        {
            this.prefs = prefs;
        }

        public boolean onPreferenceClick(Preference preference)
        {
            CheckBoxPreference chkLog_livetrack24 = (CheckBoxPreference) findPreference("log_livetrack24");
            boolean livetrack24_enabled = prefs.getBoolean("livetrack24_enabled", false);

            if (chkLog_livetrack24.isChecked() && !livetrack24_enabled)
            {
                startActivity(new Intent("com.mendhak.gpslogger.LIVETRACK24_SETUP"));
            }
            return true;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        Utilities.LogDebug("GpsSettingsActivity.onWindowFocusChanged");
        if (hasFocus)
        {

            CheckBoxPreference chkLog_opengts = (CheckBoxPreference) findPreference("log_opengts");
            boolean opengts_enabled = prefs.getBoolean("opengts_enabled", false);

            if (chkLog_opengts.isChecked() && !opengts_enabled)
            {
                chkLog_opengts.setChecked(false);
            }

            CheckBoxPreference chkLog_skylines = (CheckBoxPreference) findPreference("log_skylines");
            boolean skylines_enabled = prefs.getBoolean("skylines_enabled", false);

            if (chkLog_skylines.isChecked() && !skylines_enabled)
            {
                chkLog_skylines.setChecked(false);
            }


            CheckBoxPreference chkLog_livetrack24 = (CheckBoxPreference) findPreference("log_livetrack24");
            boolean livetrack24_enabled = prefs.getBoolean("livetrack24_enabled", false);

            if (chkLog_livetrack24.isChecked() && !livetrack24_enabled)
            {
                chkLog_livetrack24.setChecked(false);
            }
        }
    }
}
