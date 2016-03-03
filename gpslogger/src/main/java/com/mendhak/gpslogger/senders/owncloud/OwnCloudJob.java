package com.mendhak.gpslogger.senders.owncloud;

import android.net.Uri;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.GeneralSecurityException;

public class OwnCloudJob extends Job implements OnRemoteOperationListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OwnCloudJob.class.getSimpleName());


    String servername;
    String username;
    String password;
    String directory;
    File localFile;
    String remoteFileName;

    protected OwnCloudJob(String servername, String username, String password, String directory,
                         File localFile, String remoteFileName)
    {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(localFile)));
        this.servername = servername;
        this.username = username;
        this.password = password;
        this.directory = directory;
        this.localFile = localFile;
        this.remoteFileName = remoteFileName;

    }

    @Override
    public void onAdded() {
        tracer.debug("ownCloud Job: onAdded");
    }

    @Override
    public void onRun() throws Throwable {

        tracer.debug("ownCloud Job: Uploading  '"+localFile.getName()+"'");

        Protocol pr = Protocol.getProtocol("https");
        if (pr == null || !(pr.getSocketFactory() instanceof SelfSignedConfidentSslSocketFactory)) {
            try {
                ProtocolSocketFactory psf = new SelfSignedConfidentSslSocketFactory();
                Protocol.registerProtocol( "https", new Protocol("https", psf, 443));

            } catch (GeneralSecurityException e) {
                tracer.error("Self-signed confident SSL context could not be loaded", e);
            }
        }

        OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(servername), AppSettings.getInstance(), true);
        client.setDefaultTimeouts('\uea60', '\uea60');
        client.setFollowRedirects(true);
        client.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(username, password)
        );

        //Create the folder, in case it doesn't already exist on OwnCloud.
        CreateRemoteFolderOperation createOperation = new CreateRemoteFolderOperation(directory, false);
        createOperation.execute( client);

        String remotePath = directory + FileUtils.PATH_SEPARATOR + localFile.getName();
        String mimeType = "application/octet-stream"; //unused
        UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(localFile.getAbsolutePath(), remotePath, mimeType);
        uploadOperation.execute(client,this,null);
    }

    @Override
    protected void onCancel() {

        tracer.debug("ownCloud Job: onCancel");
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not upload to OwnCloud", throwable);
        EventBus.getDefault().post(new UploadEvents.OwnCloud().failed("Could not upload to OwnCloud", throwable));
        return false;
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation remoteOperation, RemoteOperationResult result) {

        if (!result.isSuccess()) {
            tracer.error(result.getLogMessage(), result.getException());
            EventBus.getDefault().post(new UploadEvents.OwnCloud().failed(result.getLogMessage(), result.getException()));
        } else  {
            EventBus.getDefault().post(new UploadEvents.OwnCloud().succeeded());
        }

        tracer.debug("ownCloud Job: onRun finished");
    }

    public static String getJobTag(File gpxFile) {
        return "OWNCLOUD" + gpxFile.getName();
    }
}