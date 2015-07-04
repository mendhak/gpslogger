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

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.mendhak.gpslogger.common.PreferenceValidationFragment;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.dropbox.DropboxAuthorizationFragment;
import com.mendhak.gpslogger.senders.email.AutoEmailFragment;
import com.mendhak.gpslogger.senders.ftp.AutoFtpFragment;
import com.mendhak.gpslogger.senders.gdocs.GDocsSettingsFragment;
import com.mendhak.gpslogger.senders.opengts.OpenGTSFragment;
import com.mendhak.gpslogger.senders.osm.OSMAuthorizationFragment;
import com.mendhak.gpslogger.senders.owncloud.OwnCloudSettingsFragment;
import com.mendhak.gpslogger.settings.GeneralSettingsFragment;
import com.mendhak.gpslogger.settings.LoggingSettingsFragment;
import com.mendhak.gpslogger.settings.PerformanceSettingsFragment;
import com.mendhak.gpslogger.settings.UploadSettingsFragment;
import org.slf4j.LoggerFactory;



public class MainPreferenceActivity extends ActionBarActivity {

    private org.slf4j.Logger tracer;

    PreferenceFragment preferenceFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        tracer = LoggerFactory.getLogger(MainPreferenceActivity.class.getSimpleName());

        String whichFragment = PreferenceConstants.GENERAL;

        if(getIntent().getExtras() != null) {
           whichFragment = getIntent().getExtras().getString("preference_fragment");
        }

        //If OpenStreetMap has returned with OAuth token
        if(getIntent().getData() != null) {
            tracer.debug("OSM Authorization returned: " + getIntent().getData().getQuery());
            whichFragment = PreferenceConstants.OSM;
        }

        switch(whichFragment){
            case PreferenceConstants.GENERAL:
                setTitle(R.string.settings_screen_name);
                preferenceFragment = new GeneralSettingsFragment();
                break;
            case PreferenceConstants.LOGGING:
                setTitle(R.string.pref_logging_title);
                preferenceFragment = new LoggingSettingsFragment();
                break;
            case PreferenceConstants.PERFORMANCE:
                setTitle(R.string.pref_performance_title);
                preferenceFragment = new PerformanceSettingsFragment();
                break;
            case PreferenceConstants.UPLOAD:
                setTitle(R.string.title_drawer_uploadsettings);
                preferenceFragment = new UploadSettingsFragment();
                break;
            case PreferenceConstants.FTP:
                setTitle(R.string.autoftp_setup_title);
                preferenceFragment = new AutoFtpFragment();
                break;
            case PreferenceConstants.EMAIL:
                setTitle(R.string.autoemail_title);
                preferenceFragment = new AutoEmailFragment();
                break;
            case PreferenceConstants.OPENGTS:
                setTitle(R.string.opengts_setup_title);
                preferenceFragment = new OpenGTSFragment();
                break;
            case PreferenceConstants.GDOCS:
                setTitle(R.string.gdocs_setup_title);
                preferenceFragment = new GDocsSettingsFragment();
                break;
            case PreferenceConstants.DROPBOX:
                setTitle(R.string.dropbox_setup_title);
                preferenceFragment = new DropboxAuthorizationFragment();
                break;
            case PreferenceConstants.OSM:
                setTitle(R.string.osm_setup_title);
                preferenceFragment = new OSMAuthorizationFragment();
                break;
            case PreferenceConstants.OWNCLOUD:
                setTitle(R.string.owncloud_setup_title);
                preferenceFragment = new OwnCloudSettingsFragment();
                break;
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, preferenceFragment)
                .commit();

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

    public static class PreferenceConstants{
        public static final String GENERAL = "GeneralSettingsFragment";
        public static final String LOGGING = "LoggingSettingsFragment";
        public static final String PERFORMANCE = "PerformanceSettingsFragment";
        public static final String UPLOAD = "UploadSettingsFragment";
        public static final String FTP = "AutoFtpFragment";
        public static final String EMAIL = "AutoEmailFragment";
        public static final String OPENGTS = "OpenGTSFragment";
        public static final String GDOCS = "GDocsSettingsFragment";
        public static final String DROPBOX = "DropBoxAuthorizationFragment";
        public static final String OWNCLOUD = "OwnCloudAuthorizationFragment";
        public static final String OSM = "OSMAuthorizationFragment";
    }

}
