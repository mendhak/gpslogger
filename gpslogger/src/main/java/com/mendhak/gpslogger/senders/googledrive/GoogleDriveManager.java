package com.mendhak.gpslogger.senders.googledrive;

import android.content.Context;
import android.net.Uri;

import com.birbit.android.jobqueue.CancelResult;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;

public class GoogleDriveManager extends FileSender {

    private static final Logger LOG = Logs.of(GoogleDriveManager.class);
    private final PreferenceHelper preferenceHelper;

    public GoogleDriveManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public static String getGoogleDriveApplicationClientID(){
        return "207286146661-6kgeq88ktnjho995oetdv78lmcjfjrqc.apps.googleusercontent.com";
    }

    public static String getGoogleDriveApplicationOauth2Redirect(){
        //Needs to match in androidmanifest.xml
        return "com.mendhak.gpslogger:/oauth2googledrive";
    }

    public static String[] getGoogleDriveApplicationScopes(){
        return new String[] {"https://www.googleapis.com/auth/drive.file"};
    }

    public static AuthorizationService getAuthorizationService(Context context){
        return new AuthorizationService(context, new AppAuthConfiguration.Builder().build());
    }

    public static AuthorizationServiceConfiguration getAuthorizationServiceConfiguration() {
        return new AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://www.googleapis.com/oauth2/v4/token"),
                null,
                Uri.parse("https://accounts.google.com/o/oauth2/revoke?token=")
        );
    }

    @Override
    public void uploadFile(List<File> files) {

    }

    public void uploadFile(String fileName) {
        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new GoogleDriveJob(fileName));
            }
        }, TagConstraint.ANY, GoogleDriveJob.getJobTag(fileName));
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean accept(File file, String s) {
        return false;
    }
}
