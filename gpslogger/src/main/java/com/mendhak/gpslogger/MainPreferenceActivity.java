package com.mendhak.gpslogger;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import com.mendhak.gpslogger.settings.GeneralSettingsFragment;
import com.mendhak.gpslogger.settings.LoggingSettingsFragment;
import com.mendhak.gpslogger.settings.UploadSettingsFragment;
import org.slf4j.LoggerFactory;


public class MainPreferenceActivity extends ActionBarActivity {

    private org.slf4j.Logger tracer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracer = LoggerFactory.getLogger(MainPreferenceActivity.class.getSimpleName());

        String whichFragment = getIntent().getExtras().getString("preference_fragment");

        PreferenceFragment preferenceFragment = null;

        switch(whichFragment){
            case "GeneralSettingsFragment":
                setTitle(R.string.settings_screen_name);
                preferenceFragment = new GeneralSettingsFragment();
                break;
            case "LoggingSettingsFragment":
                setTitle(R.string.pref_logging_title);
                preferenceFragment = new LoggingSettingsFragment();
                break;
            case "UploadSettingsFragment":
                setTitle(R.string.title_drawer_uploadsettings);
                preferenceFragment = new UploadSettingsFragment();

        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, preferenceFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }




}
