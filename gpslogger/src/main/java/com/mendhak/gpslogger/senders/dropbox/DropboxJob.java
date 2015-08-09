package com.mendhak.gpslogger.senders.dropbox;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;

public class DropboxJob extends Job {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(DropboxJob.class.getSimpleName());
    String fileName;
    DropboxAPI<AndroidAuthSession> dropboxApi;
    String dropboxAppKey;
    String dropboxAppSecret;
    String[] storedKeys;
    final static private Session.AccessType ACCESS_TYPE = Session.AccessType.APP_FOLDER;

    protected DropboxJob(String fileName, String dropboxAppkey, String dropboxAppSecret, String[] storedKeys) {
        super(new Params(1).requireNetwork().persist());

        this.fileName = fileName;
        this.dropboxAppKey = dropboxAppkey;
        this.dropboxAppSecret = dropboxAppSecret;
        this.storedKeys = storedKeys;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        File gpsDir = new File(AppSettings.getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, fileName);

        FileInputStream fis = new FileInputStream(gpxFile);

        AndroidAuthSession session = buildSession();
        dropboxApi = new DropboxAPI<AndroidAuthSession>(session);
        DropboxAPI.Entry upEntry = dropboxApi.putFileOverwrite(gpxFile.getName(), fis, gpxFile.length(), null);
        tracer.info("DropBox upload complete. Rev: " + upEntry.rev);
        EventBus.getDefault().post(new UploadEvents.Dropbox(true));
        fis.close();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(dropboxAppKey, dropboxAppSecret);
        AndroidAuthSession session;

        if (storedKeys != null) {
            AccessTokenPair accessToken = new AccessTokenPair(storedKeys[0], storedKeys[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
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
}
