package com.mendhak.gpslogger.senders.dropbox;




import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;


public class DropboxJob extends Job {


    private static final Logger LOG = Logs.of(DropboxJob.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    String fileName;


    protected DropboxJob(String fileName) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(fileName)));

        this.fileName = fileName;
    }

    @Override
    public void onAdded() {
        LOG.debug("Dropbox job added");
    }

    @Override
    public void onRun() throws Throwable {
        File gpsDir = new File(preferenceHelper.getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, fileName);

        try {
            LOG.debug("Beginning upload to dropbox...");
            InputStream inputStream = new FileInputStream(gpxFile);
            DbxRequestConfig requestConfig = new DbxRequestConfig(
                    "GPSLogger",
                    Locale.getDefault().toString());
            DbxClientV2 mDbxClient = new DbxClientV2(requestConfig, PreferenceHelper.getInstance().getDropBoxAccessKeyName());
            mDbxClient.files().uploadBuilder("/" + fileName).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
            EventBus.getDefault().post(new UploadEvents.Dropbox().succeeded());
        } catch (Exception e) {
            LOG.error("Could not upload to Dropbox" , e);
            EventBus.getDefault().post(new UploadEvents.Dropbox().failed(e.getMessage(), e));
        }

    }


    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        EventBus.getDefault().post(new UploadEvents.Dropbox().failed("Could not upload to Dropbox", throwable));
        LOG.error("Could not upload to Dropbox", throwable);
        return false;
    }

    public static String getJobTag(String fileName) {
        return "DROPBOX" + fileName;
    }
}
