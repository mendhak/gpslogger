package com.mendhak.gpslogger.senders.sftp;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.senders.FileSender;
import com.path.android.jobqueue.CancelResult;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;

import java.io.File;
import java.util.List;

public class SFTPManager extends FileSender {

    private PreferenceHelper preferenceHelper;

    public SFTPManager(PreferenceHelper preferenceHelper){
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            uploadFile(f);
        }
    }

    public void uploadFile(final File file){
        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new SFTPJob(file, preferenceHelper.getSFTPRemoteServerPath(), preferenceHelper.getSFTPHost(),preferenceHelper.getSFTPPort(),preferenceHelper.getSFTPPrivateKeyFilePath(),
                        preferenceHelper.getSFTPPrivateKeyPassphrase(),preferenceHelper.getSFTPUser(),preferenceHelper.getSFTPPassword(),preferenceHelper.getSFTPKnownHostKey()));
            }
        }, TagConstraint.ANY, SFTPJob.getJobTag(file));
    }

    @Override
    public boolean isAvailable() {
        return validSettings(preferenceHelper.getSFTPRemoteServerPath(), preferenceHelper.getSFTPHost(),preferenceHelper.getSFTPPort(),preferenceHelper.getSFTPPrivateKeyFilePath(),
                preferenceHelper.getSFTPPrivateKeyPassphrase(),preferenceHelper.getSFTPUser(),preferenceHelper.getSFTPPassword(),preferenceHelper.getSFTPKnownHostKey());
    }

    private boolean validSettings(String sftpRemoteServerPath, String sftpHost, int sftpPort, String sftpPrivateKeyFilePath, String sftpPrivateKeyPassphrase, String sftpUser, String sftpPassword, String sftpKnownHostKey) {
        if (Strings.isNullOrEmpty(sftpRemoteServerPath)
                || Strings.isNullOrEmpty(sftpHost)
                || sftpPort <= 0
                || (Strings.isNullOrEmpty(sftpPrivateKeyFilePath) && Strings.isNullOrEmpty(sftpPassword) )){
            return false;
        }

        return true;
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isSFTPEnabled();
    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }
}
