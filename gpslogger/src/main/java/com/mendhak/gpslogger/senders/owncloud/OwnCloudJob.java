package com.mendhak.gpslogger.senders.owncloud;


import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;

import de.greenrobot.event.EventBus;

public class OwnCloudJob extends Job {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OwnCloudJob.class.getSimpleName());

    String fileName;

    protected OwnCloudJob(String servername, int port, String username, String password, String directory,
                          boolean useHttps, File localFile, String remoteFileName)
    {
        super(new Params(1).requireNetwork().persist());
        this.fileName = fileName;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        File gpsDir = new File(AppSettings.getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, fileName);

        FileInputStream fis = new FileInputStream(gpxFile);

        /*
        AndroidAuthSession session = buildSession();
        dropboxApi = new DropboxAPI<AndroidAuthSession>(session);
        DropboxAPI.Entry upEntry = dropboxApi.putFileOverwrite(gpxFile.getName(), fis, gpxFile.length(), null);
        tracer.info("DropBox uploaded file rev is: " + upEntry.rev);
        EventBus.getDefault().post(new UploadEvents.Dropbox(true));
        fis.close();
        */
    }
    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new UploadEvents.OwnCloud(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not upload to OwnCloud", throwable);
        return false;
    }
}