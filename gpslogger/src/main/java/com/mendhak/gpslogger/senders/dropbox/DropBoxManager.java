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

package com.mendhak.gpslogger.senders.dropbox;


import android.content.Context;
import android.os.AsyncTask;
import com.dropbox.core.*;
import com.dropbox.core.android.Auth;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import com.path.android.jobqueue.CancelResult;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Locale;


public class DropBoxManager extends FileSender {

    private static final Logger LOG = Logs.of(DropBoxManager.class);
    private final PreferenceHelper preferenceHelper;

    public DropBoxManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    /**
     * Whether the user has authorized GPSLogger with DropBox
     *
     * @return True/False
     */
    public boolean isLinked() {
        return !Strings.isNullOrEmpty(preferenceHelper.getDropBoxAccessKeyName());
    }

    public boolean finishAuthorization() {
        if(!isLinked()){
            String accessToken = Auth.getOAuth2Token();
            if(!Strings.isNullOrEmpty(accessToken)){
                storeKeys(accessToken);
                return true;
            }
        }

        return false;
    }


    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     *
     * @param key    The Access Key
     */
    private void storeKeys(String key) {
        preferenceHelper.setDropBoxAccessKeyName(key);
    }

    public void startAuthentication(Context context) {

        Auth.startOAuth2Authentication(context, BuildConfig.DROPBOX_APP_KEY);
    }

    public void unLink() {
        preferenceHelper.setDropBoxAccessKeyName(null);
        preferenceHelper.setDropBoxOauth1Secret(null);
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            LOG.debug(f.getName());
            uploadFile(f.getName());
        }
    }

    @Override
    public boolean isAvailable() {
        return isLinked() && preferenceHelper.getDropBoxAccessKeyName() != null;
    }


    @Override
    public boolean hasUserAllowedAutoSending() {
        return  preferenceHelper.isDropboxAutoSendEnabled();
    }

    public void uploadFile(final String fileName) {

        if(!Strings.isNullOrEmpty(preferenceHelper.getDropBoxOauth1Secret())){
            convertOauth1ToOauth2Token(fileName);
            return;
        }

        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new DropboxJob(fileName));
            }
        }, TagConstraint.ANY, DropboxJob.getJobTag(fileName));

    }

    /**
     * Attempts to upgrade Oauth1 tokens to Oauth2 before performing a file upload
     * @param pendingFileName
     */
    void convertOauth1ToOauth2Token(final String pendingFileName) {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

            DbxOAuth1Upgrader upgrader;
            DbxOAuth1AccessToken oAuth1AccessToken;

            @Override
            protected String doInBackground(Void... params) {

                LOG.warn("Found old Dropbox Oauth1 tokens! Attempting upgrade now.");
                try {
                    DbxRequestConfig requestConfig = new DbxRequestConfig("GPSLogger", Locale.getDefault().toString());
                    DbxAppInfo appInfo = new DbxAppInfo(BuildConfig.DROPBOX_APP_KEY, BuildConfig.DROPBOX_APP_SECRET);
                    upgrader = new DbxOAuth1Upgrader(requestConfig, appInfo);
                    oAuth1AccessToken = new DbxOAuth1AccessToken(preferenceHelper.getDropBoxAccessKeyName(), preferenceHelper.getDropBoxOauth1Secret());
                    LOG.debug("Requesting Oauth2 token...");
                    String newToken = upgrader.createOAuth2AccessToken(oAuth1AccessToken);
                    LOG.debug("New token is " + newToken);
                    LOG.debug("Disabling the old Oauth1 token ");
                    upgrader.disableOAuth1AccessToken(oAuth1AccessToken);
                    return newToken;

                } catch (Exception e) {
                    EventBus.getDefault().post(new UploadEvents.Dropbox().failed("DropBox Oauth2 Token upgrade failed. Please reauthorize DropBox from the settings.", e));
                    LOG.error("Could not upgrade to Oauth2", e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(String authToken) {
                if (authToken != null) {
                    unLink();
                    storeKeys(authToken);
                    uploadFile(pendingFileName);
                }

            }
        };
        task.execute();
    }



    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

}




