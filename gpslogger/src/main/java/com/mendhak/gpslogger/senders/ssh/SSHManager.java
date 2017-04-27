package com.mendhak.gpslogger.senders.ssh;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.senders.FileSender;
import com.path.android.jobqueue.CancelResult;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;

import java.io.File;
import java.util.List;

public class SSHManager extends FileSender {
    @Override
    public void uploadFile(List<File> files) {

    }

    public void uploadFile(final File file){
        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new SSHJob(file,"192.168.1.91",2999,"/storage/emulated/0/test_id_rsa", "","joe","hunter2",""));
            }
        }, TagConstraint.ANY, SSHJob.getJobTag(file));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return false;
    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }
}
