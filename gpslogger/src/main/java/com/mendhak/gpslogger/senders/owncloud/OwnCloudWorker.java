package com.mendhak.gpslogger.senders.owncloud;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.LocalX509TrustManager;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.AdvancedSslSocketFactory;
import com.owncloud.android.lib.common.network.AdvancedX509TrustManager;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.slf4j.Logger;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import de.greenrobot.event.EventBus;

public class OwnCloudWorker extends Worker implements OnRemoteOperationListener {

    private static final Logger LOG = Logs.of(OwnCloudWorker.class);

    final AtomicInteger taskStatus = new AtomicInteger(-1);
    Throwable failureThrowable = null;
    String failureMessage = "";

    public OwnCloudWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String filePath = getInputData().getString("filePath");
        if(Strings.isNullOrEmpty(filePath)) {
            LOG.error("No file path provided to upload to OwnCloud");
            return Result.failure();
        }
        File fileToUpload = new File(filePath);

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        String servername = preferenceHelper.getOwnCloudBaseUrl();
        String username = preferenceHelper.getOwnCloudUsername();
        String password = preferenceHelper.getOwnCloudPassword();
        String directory = preferenceHelper.getOwnCloudDirectory();


        try {
            LOG.debug("ownCloud Job: Uploading  '" + fileToUpload.getName() + "'");


            Protocol pr = Protocol.getProtocol("https");

            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(
                        null,
                        new TrustManager[] { new LocalX509TrustManager(Networks.getKnownServersStore(AppSettings.getInstance())) },
                        null
                );

                ProtocolSocketFactory psf = new AdvancedSslSocketFactory(sslContext,
                        new AdvancedX509TrustManager(Networks.getKnownServersStore(AppSettings.getInstance())), new StrictHostnameVerifier());


                Protocol.registerProtocol( "https", new Protocol("https", psf, 443));

            } catch (GeneralSecurityException e) {
                LOG.error("Self-signed confident SSL context could not be loaded", e);
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

            String remotePath = directory + FileUtils.PATH_SEPARATOR + fileToUpload.getName();
            String mimeType = "application/octet-stream"; //unused
            UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(fileToUpload.getAbsolutePath(), remotePath, mimeType);
            uploadOperation.execute(client,this,null);

            /*
            Doing this simply because ListenerWorker is too complicated to implement,
            And the documentation is not clear on how to use it.
             */
            for(int i = 0; i < 24; i++) {
                Thread.sleep(5000);
                if(taskStatus.get() != -1) {
                    break;
                }
            }

            // If it failed, OR never completed in time.
            if(taskStatus.get() <= 0) {
                LOG.error("Failed to upload to OwnCloud");
                EventBus.getDefault().post(new UploadEvents.OwnCloud().failed(failureMessage, failureThrowable));
                return Result.failure();
            }
            else {
                LOG.info("OwnCloud - file uploaded");

                // Notify internal listeners
                EventBus.getDefault().post(new UploadEvents.OwnCloud().succeeded());
                // Notify external listeners
                Systems.sendFileUploadedBroadcast(getApplicationContext(), new String[]{fileToUpload.getAbsolutePath()}, "owncloud");
                return Result.success();
            }
        }
        catch (Exception ex) {
            LOG.error("Error in OwnCloudWorker.doWork(): " + ex.getMessage());
            EventBus.getDefault().post(new UploadEvents.OwnCloud().failed(ex.getMessage(), ex));
            return Result.failure();
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation remoteOperation, RemoteOperationResult result) {

        if (!result.isSuccess()) {
            LOG.error(result.getLogMessage(), result.getException());
            failureThrowable = result.getException();
            failureMessage = result.getLogMessage();
            taskStatus.set(0);
        } else  {
            taskStatus.set(1);
        }

        LOG.debug("ownCloud Job: onRun finished");
    }
}
