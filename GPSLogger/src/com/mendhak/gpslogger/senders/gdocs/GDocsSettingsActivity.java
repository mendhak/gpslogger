package com.mendhak.gpslogger.senders.gdocs;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;


public class GDocsSettingsActivity extends PreferenceActivity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gdocssettings);

        Preference resetPref = findPreference("gdocs_resetauth");

        ResetPreferenceText(resetPref);

        resetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                if(GDocsHelper.IsLinked(getApplicationContext()))
                {
                    //Clear authorization
                    GDocsHelper.ClearAccessToken(getApplicationContext());
                    startActivity(new Intent(getApplicationContext(), GpsMainActivity.class));
                    finish();
                }
                else
                {
                    startActivity(new Intent().setClass(getApplicationContext(), GDocsAuthorizationActivity.class));
                    finish();
                }
                
                return true;
            }
        });
        
    }

    public void onResume()
    {
        super.onResume();
        Preference resetPref = findPreference("gdocs_resetauth");
        ResetPreferenceText(resetPref);

    }

    private void ResetPreferenceText(Preference resetPref)
    {
        if(GDocsHelper.IsLinked(getApplicationContext()))
        {
            resetPref.setTitle(R.string.gdocs_clearauthorization);
            resetPref.setSummary(R.string.gdocs_clearauthorization_summary);
        }
    }
}