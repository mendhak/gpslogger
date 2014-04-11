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

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class GDocsSettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceClickListener, IActionListener
{

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GDocsSettingsActivity.class.getSimpleName());
    private final Handler handler = new Handler();
    boolean messageShown = false;
    String accountName;


    static final int REQUEST_CODE_MISSING_GPSF = 1;
    static final int REQUEST_CODE_ACCOUNT_PICKER = 2;
    static final int REQUEST_CODE_RECOVERED=3;


    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.gdocssettings);

        VerifyGooglePlayServices();


    }

    private void VerifyGooglePlayServices()
    {
        Preference resetPref = findPreference("gdocs_resetauth");
        Preference testPref = findPreference("gdocs_test");

        int availability = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if (availability != ConnectionResult.SUCCESS)
        {
            resetPref.setEnabled(false);
            testPref.setEnabled(false);

            if (!messageShown)
            {
                Dialog d = GooglePlayServicesUtil.getErrorDialog(availability, this, REQUEST_CODE_MISSING_GPSF);
                if (d != null)
                {
                    d.show();
                }
                else
                {
                    Utilities.MsgBox(getString(R.string.gpsf_missing), getString(R.string.gpsf_missing_description), this);
                }
                messageShown = true;
            }

        }
        else
        {
            ResetPreferenceAppearance(resetPref, testPref);

            testPref.setOnPreferenceClickListener(this);
            resetPref.setOnPreferenceClickListener(this);
        }

    }


    public void onResume()
    {
        super.onResume();
        VerifyGooglePlayServices();

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
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                false, null, null, null, null);

        startActivityForResult(intent, REQUEST_CODE_ACCOUNT_PICKER);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {

            case REQUEST_CODE_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK)
                {
                    String accountName = data.getStringExtra(
                            AccountManager.KEY_ACCOUNT_NAME);

                    GDocsHelper.SetAccountName(getApplicationContext(),accountName);
                    tracer.debug(accountName);
                    getAndUseAuthTokenInAsyncTask();
                }
                break;
            case REQUEST_CODE_RECOVERED:
                if(resultCode == RESULT_OK)
                {
                    getAndUseAuthTokenInAsyncTask();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // Example of how to use the GoogleAuthUtil in a blocking, non-main thread context
    String getAndUseAuthTokenBlocking()
    {
        try
        {
            // Retrieve a token for the given account and scope. It will always return either
            // a non-empty String or throw an exception.
            final String token = GoogleAuthUtil.getToken(getApplicationContext(), GDocsHelper.GetAccountName(getApplicationContext()), GDocsHelper.GetOauth2Scope());

            return token;
        }
        catch (GooglePlayServicesAvailabilityException playEx)
        {
            Dialog alert = GooglePlayServicesUtil.getErrorDialog(
                    playEx.getConnectionStatusCode(),
                    this,
                    REQUEST_CODE_RECOVERED);
            alert.show();

        }
        catch (UserRecoverableAuthException userAuthEx)
        {
            // Start the user recoverable action using the intent returned by
            // getIntent()
            startActivityForResult(
                    userAuthEx.getIntent(),
                    REQUEST_CODE_RECOVERED);

        }
        catch (IOException transientEx)
        {
            // network or server error, the call is expected to succeed if you try again later.
            // Don't attempt to call again immediately - the request is likely to
            // fail, you'll hit quotas or back-off.


        }
        catch (GoogleAuthException authEx)
        {
            // Failure. The call is not expected to ever succeed so it should not be
            // retried.

        }
        return null;
    }


    void getAndUseAuthTokenInAsyncTask()
    {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
               return getAndUseAuthTokenBlocking();

            }

            @Override
            protected void onPostExecute(String authToken)
            {
                if(authToken != null)
                {
                    GDocsHelper.SaveAuthToken(getApplicationContext(),authToken);
                    tracer.debug(authToken);
                    VerifyGooglePlayServices();
                }

            }
        };
        task.execute();
    }


    private void UploadTestFileToGoogleDocs()
    {

        Utilities.ShowProgress(GDocsSettingsActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));
        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
        if (!gpxFolder.exists())
        {
            gpxFolder.mkdirs();
        }

        tracer.debug("Creating gpslogger_test.xml");
        File testFile = new File(gpxFolder.getPath(), "gpslogger_test.xml");

        try
        {
            if(!testFile.exists())
            {
                testFile.createNewFile();

                FileOutputStream initialWriter = new FileOutputStream(testFile, true);
                BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                StringBuilder initialString = new StringBuilder();
                initialString.append("<x>This is a test file</x>");
                initialOutput.write(initialString.toString().getBytes());
                initialOutput.flush();
                initialOutput.close();
            }

        }
        catch(Exception ex)
        {
            OnFailure();
        }


        GDocsHelper helper = new GDocsHelper(getApplicationContext(), this);

        ArrayList<File> files = new ArrayList<File>();
        files.add(testFile);

        helper.UploadFile(files);

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