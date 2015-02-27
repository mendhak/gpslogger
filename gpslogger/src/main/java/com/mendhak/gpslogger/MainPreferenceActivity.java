package com.mendhak.gpslogger;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import com.mendhak.gpslogger.settings.GeneralSettingsFragment;
import com.mendhak.gpslogger.settings.LoggingSettingsFragment;
import org.slf4j.LoggerFactory;


public class MainPreferenceActivity extends ActionBarActivity {

    private org.slf4j.Logger tracer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracer = LoggerFactory.getLogger(MainPreferenceActivity.class.getSimpleName());

        int fragmentNumber = getIntent().getExtras().getInt("preference_fragment");

        PreferenceFragment preferenceFragment = null;

        switch(fragmentNumber){
            case 1:
                setTitle(R.string.settings_screen_name);
                preferenceFragment = new GeneralSettingsFragment();
                break;
            case 2:
                setTitle(R.string.pref_logging_title);
                preferenceFragment = new LoggingSettingsFragment();
                break;

        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, preferenceFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }




}
