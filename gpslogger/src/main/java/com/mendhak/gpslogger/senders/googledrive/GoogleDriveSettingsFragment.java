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

package com.mendhak.gpslogger.senders.googledrive;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.canelmas.let.AskPermission;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class GoogleDriveSettingsFragment extends PermissionedPreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(GoogleDriveSettingsFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    boolean messageShown = false;

    static final int REQUEST_CODE_MISSING_GPSF = 1;
    static final int REQUEST_CODE_ACCOUNT_PICKER = 2;
    static final int REQUEST_CODE_RECOVERED = 3;

    GoogleDriveManager manager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.gdocssettings);

        manager = new GoogleDriveManager(preferenceHelper);

        verifyGooglePlayServices();
        registerEventBus();
    }

    @Override
    public void onDestroy() {

        unregisterEventBus();
        super.onDestroy();
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }
    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void verifyGooglePlayServices() {
        Preference resetPref = findPreference("gdocs_resetauth");
        Preference testPref = findPreference("gdocs_test");
        Preference folderPref = findPreference("gdocs_foldername");

        int availability = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        if (availability != ConnectionResult.SUCCESS) {
            resetPref.setEnabled(false);
            testPref.setEnabled(false);
            folderPref.setEnabled(false);

            if (!messageShown) {
                Dialog d = GooglePlayServicesUtil.getErrorDialog(availability, getActivity(), REQUEST_CODE_MISSING_GPSF);
                if (d != null) {
                    d.show();
                } else {
                    Dialogs.alert(getString(R.string.gpsf_missing), getString(R.string.gpsf_missing_description), getActivity());
                }
                messageShown = true;
            }

        } else {
            resetPreferenceAppearance(resetPref, testPref, folderPref);

            testPref.setOnPreferenceClickListener(this);
            resetPref.setOnPreferenceClickListener(this);
        }

    }


    public void onResume() {
        super.onResume();
        verifyGooglePlayServices();

    }

    private void resetPreferenceAppearance(Preference resetPref, Preference testPref, Preference folderPref) {
        if (manager.isLinked()) {
            resetPref.setTitle(R.string.gdocs_clearauthorization);
            resetPref.setSummary(R.string.gdocs_clearauthorization_summary);
            testPref.setEnabled(true);
            folderPref.setEnabled(true);
        } else {
            testPref.setEnabled(false);
            folderPref.setEnabled(false);
        }

    }

    @Override
    @AskPermission({Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equalsIgnoreCase("gdocs_test")) {
            uploadTestFileToGoogleDocs();
        } else {
            if (manager.isLinked()) {
                //Clear authorization
                GoogleAuthUtil.invalidateToken(getActivity(), preferenceHelper.getGoogleDriveAuthToken());
                preferenceHelper.setGoogleDriveAuthToken("");
                preferenceHelper.setGoogleDriveAccountName("");

                startActivity(new Intent(getActivity(), GpsMainActivity.class));
                getActivity().finish();
            } else {
                //Re-authorize
                authorize();

            }
        }

        return true;
    }

    private void authorize() {
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

                    preferenceHelper.setGoogleDriveAccountName(accountName);

                    LOG.debug("Account:" + accountName);
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

            return GoogleAuthUtil.getToken(getActivity(), preferenceHelper.getGoogleDriveAccountName(), manager.getOauth2Scope());
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
            LOG.error("Temporary failure", transientEx);
            // network or server error, the call is expected to succeed if you try again later.
            // Don't attempt to call again immediately - the request is likely to
            // fail, you'll hit quotas or back-off.


        } catch (GoogleAuthException authEx) {
            LOG.error("Authentication failure", authEx);
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
                    preferenceHelper.setGoogleDriveAuthToken(authToken);
                    LOG.debug("Auth token:" + authToken);
                    verifyGooglePlayServices();
                }

            }
        };
        task.execute();
    }


    @AskPermission({Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    private void uploadTestFileToGoogleDocs() {

        Dialogs.progress(getActivity(), getString(R.string.please_wait), getString(R.string.please_wait));
        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        LOG.debug("Creating gpslogger_test.xml");
        File testFile = new File(gpxFolder.getPath(), "gpslogger_test.xml");

        try {
            if (!testFile.exists()) {
                testFile.createNewFile();

                FileOutputStream initialWriter = new FileOutputStream(testFile, true);
                BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                initialOutput.write("<x>This is a test file</x>".getBytes());
                initialOutput.flush();
                initialOutput.close();

                Files.addToMediaDatabase(testFile, "text/xml");
            }

        } catch (Exception ex) {
            LOG.error("Could not create local test file", ex);
            EventBus.getDefault().post(new UploadEvents.GDocs().failed("Could not create local test file", ex));
        }

        MaterialEditTextPreference folderPref = (MaterialEditTextPreference)findPreference("gdocs_foldername");
        manager.uploadTestFile(testFile, folderPref.getText());

    }



    @EventBusHook
    public void onEventMainThread(UploadEvents.GDocs o){
        LOG.debug("GDocs Event completed, success: " + o.success);
        Dialogs.hideProgress();
        if(!o.success){
            Dialogs.alert(getString(R.string.sorry), getString(R.string.gdocs_testupload_error), getActivity());
        }
        else {
            Dialogs.alert(getString(R.string.success), getString(R.string.gdocs_testupload_success), getActivity());
        }
    }

}