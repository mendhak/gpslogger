package com.mendhak.gpslogger.senders.dropbox;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import de.greenrobot.event.EventBus;

public class DropboxWorker extends Worker {

    private static final Logger LOG = Logs.of(DropboxWorker.class);
    public DropboxWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String fileName = getInputData().getString("fileName");
        if(Strings.isNullOrEmpty(fileName)) {
            EventBus.getDefault().post(new UploadEvents.Dropbox().failed("Dropbox upload failed", new Throwable("Nothing to upload.")));
            return Result.failure();
        }

        File gpsDir = new File(PreferenceHelper.getInstance().getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, fileName);

        try {
            LOG.debug("Beginning upload to dropbox...");
            InputStream inputStream = new FileInputStream(gpxFile);
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("GPSLogger").build();
            DbxClientV2 mDbxClient;

            if(!Strings.isNullOrEmpty(PreferenceHelper.getInstance().getDropboxRefreshToken())){
                DbxCredential dropboxCred = DbxCredential.Reader.readFully(PreferenceHelper.getInstance().getDropboxRefreshToken());
                mDbxClient = new DbxClientV2(requestConfig, dropboxCred);
            }
            else {
                //For existing users that already have long lived access tokens stored.
                mDbxClient = new DbxClientV2(requestConfig, PreferenceHelper.getInstance().getDropboxLongLivedAccessKey());
            }

            mDbxClient.files().uploadBuilder("/" + fileName).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);

            EventBus.getDefault().post(new UploadEvents.Dropbox().succeeded());
            LOG.info("Dropbox - file uploaded");
        } catch (Exception e) {
            LOG.error("Could not upload to Dropbox" , e);
            EventBus.getDefault().post(new UploadEvents.Dropbox().failed(e.getMessage(), e));
            return Result.failure();
        }



        return Result.success();
    }
}
