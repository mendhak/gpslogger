//https://www.dropbox.com/developers/start/setup#android

package com.mendhak.gpslogger.senders.dropbox;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;

public class DropBoxAuthorizationActivity extends PreferenceActivity
{

    DropBoxHelper helper;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dropboxsettings);

        Preference pref = findPreference("dropbox_resetauth");
        
        helper = new DropBoxHelper(getApplicationContext(), null);
        
        if(helper.IsLinked())
        {
            pref.setTitle(R.string.dropbox_unauthorize);
            pref.setSummary(R.string.dropbox_unauthorize_description);
        }
        else
        {
            pref.setTitle(R.string.dropbox_authorize);
            pref.setSummary(R.string.dropbox_authorize_description);
        }
        
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                // This logs you out if you're logged in, or vice versa
                if(helper.IsLinked())
                {
                    helper.UnLink();
                    startActivity(new Intent(getApplicationContext(), GpsMainActivity.class));
                    finish();
                }
                else
                {
                    try
                    {
                        helper.StartAuthentication(DropBoxAuthorizationActivity.this);
                    }
                    catch (Exception e)
                    {
                        Utilities.LogError("DropBoxAuthorizationActivity.onPreferenceClick", e);
                    }
                }

                return true;
            }
        });
        
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        try
        {
            if(helper.FinishAuthorization())
            {
                startActivity(new Intent(getApplicationContext(), GpsMainActivity.class));
                finish();
            }
        }
        catch (Exception e)
        {
            Utilities.MsgBox(getString(R.string.error), getString(R.string.dropbox_couldnotauthorize),
                    DropBoxAuthorizationActivity.this);
            Utilities.LogError("DropBoxAuthorizationActivity.onResume", e);
        }

    }







}
