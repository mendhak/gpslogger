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

package com.mendhak.gpslogger.senders.gdocs;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;


public class GDocsSettingsActivity extends SherlockPreferenceActivity
        implements Preference.OnPreferenceClickListener, IActionListener
{
    private final Handler handler = new Handler();



    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable the home button so you can go back to the main screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.gdocssettings);

        Preference resetPref = findPreference("gdocs_resetauth");
        Preference testPref = findPreference("gdocs_test");


        int availability = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if(availability != ConnectionResult.SUCCESS)
        {
            resetPref.setEnabled(false);
            testPref.setEnabled(false);
            Utilities.MsgBox(getString(R.string.gpsf_missing), getString(R.string.gpsf_missing_description), this );
            GooglePlayServicesUtil.getErrorDialog(availability, this, 1);
        }
        else
        {
            ResetPreferenceAppearance(resetPref, testPref);

            testPref.setOnPreferenceClickListener(this);
            resetPref.setOnPreferenceClickListener(this);
        }

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


    public void onResume()
    {
        super.onResume();
        Preference resetPref = findPreference("gdocs_resetauth");
        Preference testPref = findPreference("gdocs_test");
        ResetPreferenceAppearance(resetPref, testPref);

    }

    private void ResetPreferenceAppearance(Preference resetPref, Preference testPref)
    {
        if (GDocsHelper.IsLinked(getApplicationContext()))
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
        if (preference.getKey().equalsIgnoreCase("gdocs_test"))
        {
            UploadTestFileToGoogleDocs();
        }
        else
        {
            if (GDocsHelper.IsLinked(getApplicationContext()))
            {
                //Clear authorization
                GoogleAuthUtil.invalidateToken(getApplicationContext(), GDocsHelper.GetAuthToken(getApplicationContext()));
                GDocsHelper.ClearAuthToken(getApplicationContext());
                startActivity(new Intent(getApplicationContext(), GpsMainActivity.class));
                finish();
            }
            else
            {
                //Re-authorize
                Authorize();

            }
        }

        return true;
    }

    private void Authorize()
    {

    }


    @Override
    protected Dialog onCreateDialog(int id)
    {
      return null;
    }









    private void UploadTestFileToGoogleDocs()
    {

        Utilities.ShowProgress(GDocsSettingsActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));

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


}