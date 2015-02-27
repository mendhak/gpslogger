package com.mendhak.gpslogger;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import com.mendhak.gpslogger.common.PreferenceValidationFragment;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.email.AutoEmailFragment;
import com.mendhak.gpslogger.senders.ftp.AutoFtpFragment;
import com.mendhak.gpslogger.senders.opengts.OpenGTSActivity;
import com.mendhak.gpslogger.settings.GeneralSettingsFragment;
import com.mendhak.gpslogger.settings.LoggingSettingsFragment;
import com.mendhak.gpslogger.settings.UploadSettingsFragment;
import org.slf4j.LoggerFactory;


public class MainPreferenceActivity extends ActionBarActivity {

    private org.slf4j.Logger tracer;

    PreferenceFragment preferenceFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracer = LoggerFactory.getLogger(MainPreferenceActivity.class.getSimpleName());

        String whichFragment = getIntent().getExtras().getString("preference_fragment");

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
                break;
            case "AutoFtpFragment":
                setTitle(R.string.autoftp_setup_title);
                preferenceFragment = new AutoFtpFragment();
                break;
            case "AutoEmailFragment":
                setTitle(R.string.autoemail_title);
                preferenceFragment = new AutoEmailFragment();
                break;
            case "OpenGTSFragment":
                setTitle(R.string.opengts_setup_title);
                preferenceFragment = new OpenGTSActivity();
                break;

        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, preferenceFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public void onBackPressed() {

       if(isFormValid()){
           super.onBackPressed();
       }

    }

    private boolean isFormValid(){
        if(preferenceFragment instanceof  PreferenceValidationFragment){
            if( !((PreferenceValidationFragment)preferenceFragment).IsValid() ){
                Utilities.MsgBox(getString(R.string.autoemail_invalid_form),
                        getString(R.string.autoemail_invalid_form_message),
                        this);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int id = item.getItemId();
        if (id == android.R.id.home) {
           return !isFormValid();
        }

        return false;
    }
}
