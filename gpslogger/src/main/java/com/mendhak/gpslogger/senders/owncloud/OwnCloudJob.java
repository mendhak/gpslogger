package com.mendhak.gpslogger.senders.owncloud;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;

import de.greenrobot.event.EventBus;


import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudClient;

public class OwnCloudJob extends Job {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OwnCloudJob.class.getSimpleName());

    // OwnCloudClient mClient;
    String servername;
    String username;
    String password;
    String directory;
    File localFile;
    String remoteFileName;
    // Handler mHandler;

    protected OwnCloudJob(String servername, String username, String password, String directory,
                         File localFile, String remoteFileName
                         /*, OwnCloudClient client, Handler handler */ )
    {
        super(new Params(1).requireNetwork().persist());
        this.servername = servername;
        this.username = username;
        this.password = password;
        this.directory = directory;
        this.localFile = localFile;
        this.remoteFileName = remoteFileName;
        //this.mClient = client;
        //this.mHandler = handler;
    }

    @Override
    public void onAdded() {
        tracer.debug("ownCloud Job: onAdded");
    }

    @Override
    public void onRun() throws Throwable {
        /*
        File gpsDir = new File(AppSettings.getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, localFileName);
        FileInputStream fis = new FileInputStream(gpxFile);
        AndroidAuthSession session = buildSession();
        dropboxApi = new DropboxAPI<AndroidAuthSession>(session);
        DropboxAPI.Entry upEntry = dropboxApi.putFileOverwrite(gpxFile.getName(), fis, gpxFile.length(), null);
        tracer.info("DropBox uploaded file rev is: " + upEntry.rev);
        EventBus.getDefault().post(new UploadEvents.Dropbox(true));
        fis.close();
        */

        tracer.debug("ownCloud Job: Uploading  '"+localFile.getName()+"'");
        OwnCloudClient client = new OwnCloudClient(Uri.parse(servername), NetworkUtils.getMultiThreadedConnManager());
        client.setDefaultTimeouts('\uea60', '\uea60');
        client.setFollowRedirects(true);
        client.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(username, password)
        );
        //File upFolder = new File(, getString(R.string.upload_folder_path));
        //File fileToUpload = upFolder.listFiles()[0];
        String remotePath = directory + FileUtils.PATH_SEPARATOR + localFile.getName();
        String mimeType = "application/gpx+xml";
        UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(localFile.getAbsolutePath(), remotePath, mimeType);
        RemoteOperationResult result = uploadOperation.run(client);
        if (!result.isSuccess()) {
            tracer.error(result.getLogMessage(), result.getException());
            EventBus.getDefault().post(new UploadEvents.OwnCloud(false));
        } else  {
            EventBus.getDefault().post(new UploadEvents.OwnCloud(true));
        }
        tracer.debug("ownCloud Job: onRun finished");
    }
    @Override
    protected void onCancel() {
        tracer.debug("ownCloud Job: onCancel");
        EventBus.getDefault().post(new UploadEvents.OwnCloud(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not upload to OwnCloud", throwable);
        return false;
    }

}