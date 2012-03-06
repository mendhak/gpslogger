package com.mendhak.gpslogger.senders.gdocs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;


public class GDocsSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener, IActionListener
{
    private final Handler handler = new Handler();


    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gdocssettings);

        Preference resetPref = findPreference("gdocs_resetauth");
        Preference testPref = findPreference("gdocs_test");
        
        ResetPreferenceAppearance(resetPref, testPref);

        testPref.setOnPreferenceClickListener(this);
        resetPref.setOnPreferenceClickListener(this);
        
    }

    public void onResume()
    {
        super.onResume();
        Preference resetPref = findPreference("gdocs_resetauth");
        Preference testPref = findPreference("gdocs_test");
        ResetPreferenceAppearance(resetPref, testPref);

    }

    private void ResetPreferenceAppearance(Preference resetPref, Preference testPref)
    {
        if(GDocsHelper.IsLinked(getApplicationContext()))
        {
            resetPref.setTitle(R.string.gdocs_clearauthorization);
            resetPref.setSummary(R.string.gdocs_clearauthorization_summary);
            testPref.setEnabled(true);
        }
        else
        {
            testPref.setEnabled(false);
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        if(preference.getKey().equalsIgnoreCase("gdocs_test"))
        {
              UploadTestFileToGoogleDocs();
        }
        else
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
        }
        
        return true;
    }

    private void UploadTestFileToGoogleDocs()
    {

        Utilities.ShowProgress(GDocsSettingsActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));
        GDocsHelper helper = new GDocsHelper(getApplicationContext(),this);
        helper.UploadTestFile();
    }


    private final Runnable failedUpload = new Runnable()
    {
        public void run()
        {
            FailureUploading();
        }
    };

    private final Runnable successUpload = new Runnable()
    {
        public void run()
        {
            SuccessUploading();
        }
    };



    private void FailureUploading()
    {
        Utilities.MsgBox(getString(R.string.sorry), getString(R.string.gdocs_testupload_error), this);
    }
    
    private void SuccessUploading()
    {
        Utilities.MsgBox(getString(R.string.success), getString(R.string.gdocs_testupload_success), this);
    }

    @Override
    public void OnComplete()
    {
        Utilities.HideProgress();
        handler.post(successUpload);
    }

    @Override
    public void OnFailure()
    {
        Utilities.HideProgress();
        handler.post(failedUpload);

    }
}