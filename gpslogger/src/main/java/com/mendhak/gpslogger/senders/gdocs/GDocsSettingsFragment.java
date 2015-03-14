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
import android.preference.Preference;
import android.preference.PreferenceFragment;
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
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class GDocsSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GDocsSettingsFragment.class.getSimpleName());
    boolean messageShown = false;

    static final int REQUEST_CODE_MISSING_GPSF = 1;
    static final int REQUEST_CODE_ACCOUNT_PICKER = 2;
    static final int REQUEST_CODE_RECOVERED = 3;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.gdocssettings);

        VerifyGooglePlayServices();
        RegisterEventBus();
    }

    @Override
    public void onDestroy() {

        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
        super.onDestroy();
    }

    private void RegisterEventBus() {
        EventBus.getDefault().register(this);
    }

    private void VerifyGooglePlayServices() {
        Preference resetPref = findPreference("gdocs_resetauth");
        Preference testPref = findPreference("gdocs_test");

        int availability = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        if (availability != ConnectionResult.SUCCESS) {
            resetPref.setEnabled(false);
            testPref.setEnabled(false);

            if (!messageShown) {
                Dialog d = GooglePlayServicesUtil.getErrorDialog(availability, getActivity(), REQUEST_CODE_MISSING_GPSF);
                if (d != null) {
                    d.show();
                } else {
                    Utilities.MsgBox(getString(R.string.gpsf_missing), getString(R.string.gpsf_missing_description), getActivity());
                }
                messageShown = true;
            }

        } else {
            ResetPreferenceAppearance(resetPref, testPref);

            testPref.setOnPreferenceClickListener(this);
            resetPref.setOnPreferenceClickListener(this);
        }

    }


    public void onResume() {
        super.onResume();
        VerifyGooglePlayServices();

    }

    private void ResetPreferenceAppearance(Preference resetPref, Preference testPref) {
        if (GDocsHelper.IsLinked(getActivity())) {
            resetPref.setTitle(R.string.gdocs_clearauthorization);
            resetPref.setSummary(R.string.gdocs_clearauthorization_summary);
            testPref.setEnabled(true);
        } else {
            testPref.setEnabled(false);
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equalsIgnoreCase("gdocs_test")) {
            UploadTestFileToGoogleDocs();
        } else {
            if (GDocsHelper.IsLinked(getActivity())) {
                //Clear authorization
                GoogleAuthUtil.invalidateToken(getActivity(), GDocsHelper.GetAuthToken(getActivity()));
                GDocsHelper.ClearAuthToken(getActivity());
                startActivity(new Intent(getActivity(), GpsMainActivity.class));
                getActivity().finish();
            } else {
                //Re-authorize
                Authorize();

            }
        }

        return true;
    }

    private void Authorize() {
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                false, null, null, null, null);

        startActivityForResult(intent, REQUEST_CODE_ACCOUNT_PICKER);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_CODE_ACCOUNT_PICKER:
                if (resultCode == getActivity().RESULT_OK) {
                    String accountName = data.getStringExtra(
                            AccountManager.KEY_ACCOUNT_NAME);

                    GDocsHelper.SetAccountName(getActivity(), accountName);
                    tracer.debug(accountName);
                    getAndUseAuthTokenInAsyncTask();
                }
                break;
            case REQUEST_CODE_RECOVERED:
                if (resultCode == getActivity().RESULT_OK) {
                    getAndUseAuthTokenInAsyncTask();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // Example of how to use the GoogleAuthUtil in a blocking, non-main thread context
    String getAndUseAuthTokenBlocking() {
        try {
            // Retrieve a token for the given account and scope. It will always return either
            // a non-empty String or throw an exception.

            return GoogleAuthUtil.getToken(getActivity(), GDocsHelper.GetAccountName(getActivity()), GDocsHelper.GetOauth2Scope());
        } catch (GooglePlayServicesAvailabilityException playEx) {
            Dialog alert = GooglePlayServicesUtil.getErrorDialog(
                    playEx.getConnectionStatusCode(),
                    getActivity(),
                    REQUEST_CODE_RECOVERED);
            alert.show();

        } catch (UserRecoverableAuthException userAuthEx) {
            // Start the user recoverable action using the intent returned by
            // getIntent()
            startActivityForResult(
                    userAuthEx.getIntent(),
                    REQUEST_CODE_RECOVERED);

        } catch (IOException transientEx) {
            // network or server error, the call is expected to succeed if you try again later.
            // Don't attempt to call again immediately - the request is likely to
            // fail, you'll hit quotas or back-off.


        } catch (GoogleAuthException authEx) {
            // Failure. The call is not expected to ever succeed so it should not be
            // retried.

        }
        return null;
    }


    void getAndUseAuthTokenInAsyncTask() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return getAndUseAuthTokenBlocking();

            }

            @Override
            protected void onPostExecute(String authToken) {
                if (authToken != null) {
                    GDocsHelper.SaveAuthToken(getActivity(), authToken);
                    tracer.debug(authToken);
                    VerifyGooglePlayServices();
                }

            }
        };
        task.execute();
    }


    private void UploadTestFileToGoogleDocs() {

        Utilities.ShowProgress(getActivity(), getString(R.string.please_wait), getString(R.string.please_wait));
        File gpxFolder = new File(AppSettings.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        tracer.debug("Creating gpslogger_test.xml");
        File testFile = new File(gpxFolder.getPath(), "gpslogger_test.xml");

        try {
            if (!testFile.exists()) {
                testFile.createNewFile();

                FileOutputStream initialWriter = new FileOutputStream(testFile, true);
                BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                initialOutput.write("<x>This is a test file</x>".getBytes());
                initialOutput.flush();
                initialOutput.close();
            }

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.GDocsEvent(false));
        }


        GDocsHelper helper = new GDocsHelper(getActivity());

        ArrayList<File> files = new ArrayList<File>();
        files.add(testFile);

        helper.UploadFile(files);

    }



    @EventBusHook
    public void onEventMainThread(UploadEvents.GDocsEvent o){
        tracer.debug("GDocs Event completed, success: " + o.success);
        Utilities.HideProgress();
        if(!o.success){
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.gdocs_testupload_error), getActivity());
        }
        else {
            Utilities.MsgBox(getString(R.string.success), getString(R.string.gdocs_testupload_success), getActivity());
        }
    }

}