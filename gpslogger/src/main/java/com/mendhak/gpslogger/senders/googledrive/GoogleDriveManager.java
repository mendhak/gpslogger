/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.senders.googledrive;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.CancelResult;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class GoogleDriveManager extends FileSender {

    private static final Logger LOG = Logs.of(GoogleDriveManager.class);
    final PreferenceHelper preferenceHelper;

    /*
    To revoke permissions:
    (new Android)
    ./adb -e shell 'sqlite3 /data/system/users/0/accounts.db "delete from grants;"'
    or
    (old Android)
   ./adb -e shell 'sqlite3 /data/system/accounts.db "delete from grants;"'
     */

    public GoogleDriveManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public String getOauth2Scope() {
        return "oauth2:https://www.googleapis.com/auth/drive.file";
    }


    /**
     * Returns whether the app is authorized to perform Google API operations
     *
     */
    public boolean isLinked() {
        return !Strings.isNullOrEmpty(preferenceHelper.getGoogleDriveAccountName()) && !Strings.isNullOrEmpty(preferenceHelper.getGoogleDriveAuthToken());
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            uploadFile(f.getName(), null);
        }
    }


    @Override
    public boolean isAvailable() {
        return isLinked();
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isGDocsAutoSendEnabled();
    }

    public void uploadTestFile(File file, String googleDriveFolderName){

        uploadFile(file.getName(), googleDriveFolderName);
    }

    public void uploadFile(final String fileName, @Nullable String googleDriveFolderName) {
        if (!isLinked()) {
            EventBus.getDefault().post(new UploadEvents.GDrive().failed("Not authorized"));
            return;
        }

        try {
            File gpsDir = new File(preferenceHelper.getGpsLoggerFolder());
            final File gpxFile = new File(gpsDir, fileName);

            LOG.debug("Submitting Google Docs job");

            String uploadFolderName = googleDriveFolderName;

            if(Strings.isNullOrEmpty(googleDriveFolderName)){
                uploadFolderName = preferenceHelper.getGoogleDriveFolderName();
            }

            if(Strings.isNullOrEmpty(uploadFolderName)){
                uploadFolderName = "GPSLogger for Android";
            }

            final JobManager jobManager = AppSettings.getJobManager();
            final String finalUploadFolderName = uploadFolderName;
            jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
                @Override
                public void onCancelled(CancelResult cancelResult) {
                    jobManager.addJobInBackground(new GoogleDriveJob(gpxFile, finalUploadFolderName));
                }
            }, TagConstraint.ANY, GoogleDriveJob.getJobTag(gpxFile));


        } catch (Exception e) {
            EventBus.getDefault().post(new UploadEvents.GDrive().failed("Failed to upload file", e));
            LOG.error("GoogleDriveManager.uploadFile", e);
        }
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

    public String getToken() throws GoogleAuthException, IOException {
        String token = GoogleAuthUtil.getTokenWithNotification(AppSettings.getInstance(), preferenceHelper.getGoogleDriveAccountName(), getOauth2Scope(), new Bundle());
        LOG.debug("GDrive token: " + token);
        preferenceHelper.setGoogleDriveAuthToken(token);
        return token;
    }


}
