package com.mendhak.gpslogger.senders.googledrive;

import com.birbit.android.jobqueue.CancelResult;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.dropbox.DropboxJob;


import org.slf4j.Logger;

import java.io.File;
import java.util.List;

public class GoogleDriveManager extends FileSender {

    private static final Logger LOG = Logs.of(GoogleDriveManager.class);
    private final PreferenceHelper preferenceHelper;

    public GoogleDriveManager(PreferenceHelper preferenceHelper){
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {

    }

    public void uploadFile(String fileName){
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
