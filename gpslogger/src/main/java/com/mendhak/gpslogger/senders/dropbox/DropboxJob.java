package com.mendhak.gpslogger.senders.dropbox;




import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;


public class DropboxJob extends Job {


    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(DropboxJob.class.getSimpleName());
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    String fileName;
    DropboxAPI<AndroidAuthSession> dropboxApi;



    protected DropboxJob(String fileName) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(fileName)));

        this.fileName = fileName;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        File gpsDir = new File(preferenceHelper.getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, fileName);

        FileInputStream fis = new FileInputStream(gpxFile);

        DropBoxManager manager = new DropBoxManager(PreferenceHelper.getInstance());
        AndroidAuthSession session = manager.getSession();
        dropboxApi = new DropboxAPI<>(session);
        DropboxAPI.Entry upEntry = dropboxApi.putFileOverwrite(gpxFile.getName(), fis, gpxFile.length(), null);
        tracer.info("DropBox upload complete. Rev: " + upEntry.rev);
        EventBus.getDefault().post(new UploadEvents.Dropbox(true));
        fis.close();
    }


    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new UploadEvents.Dropbox(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not upload to Dropbox", throwable);
        return false;
    }

    public static String getJobTag(String fileName) {
        return "DROPBOX" + fileName;
    }
}
