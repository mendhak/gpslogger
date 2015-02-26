package com.mendhak.gpslogger;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import com.mendhak.gpslogger.settings.LoggingSettingsFragment;


public class MainPreferenceActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String fragmentName = getIntent().getExtras().getString("preference_fragment");

        PreferenceFragment preferenceFragment = null;

        switch(fragmentName){
            case "LoggingSettingsFragment":
                setTitle(R.string.pref_logging_title);
                preferenceFragment = new LoggingSettingsFragment();

        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, preferenceFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }




}
