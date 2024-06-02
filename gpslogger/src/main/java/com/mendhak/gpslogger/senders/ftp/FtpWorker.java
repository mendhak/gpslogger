package com.mendhak.gpslogger.senders.ftp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.LoggingOutputStream;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import de.greenrobot.event.EventBus;

public class FtpWorker extends Worker {

    private static final Logger LOG = Logs.of(FtpWorker.class);
    static UploadEvents.Ftp jobResult;
    static ArrayList<String> ftpServerResponses;

    public FtpWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ftpServerResponses = new ArrayList<>();
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        String server = preferenceHelper.getFtpServerName();
        int port = preferenceHelper.getFtpPort();
        String username = preferenceHelper.getFtpUsername();
        String password = preferenceHelper.getFtpPassword();
        boolean useFtps = preferenceHelper.shouldFtpUseFtps();
        String protocol = preferenceHelper.getFtpProtocol();
        boolean implicit = preferenceHelper.isFtpImplicit();

        String filePath = getInputData().getString("filePath");
        File file = new File(filePath);

        String directory = preferenceHelper.getFtpDirectory();

        if (upload(server, username, password, directory, port, useFtps, protocol, implicit, file, file.getName())) {
            LOG.info("FTP - file uploaded");
            // Notify internal listeners
            EventBus.getDefault().post(new UploadEvents.Ftp().succeeded());
            // Notify external listeners
            Systems.sendFileUploadedBroadcast(getApplicationContext(), new String[]{file.getAbsolutePath()}, "ftp");
            return Result.success();
        } else {
            jobResult.ftpMessages = ftpServerResponses;
            EventBus.getDefault().post(jobResult);
        }


        return Result.success();
    }


    public synchronized static boolean upload(String server, String username, String password, String directory, int port,
                                              boolean useFtps, String protocol, boolean implicit,
                                              File gpxFile, String fileName) {
        FTPClient client;

        try {
            if (useFtps) {
                client = new FTPSClient(protocol, implicit);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(Networks.getKnownServersStore(AppSettings.getInstance()), null);
                KeyManager km = kmf.getKeyManagers()[0];

                ((FTPSClient) client).setKeyManager(km);
                ((FTPSClient) client).setTrustManager(Networks.getTrustManager(AppSettings.getInstance()));

            } else {
                client = new FTPClient();
            }

        } catch (Exception e) {
            jobResult = new UploadEvents.Ftp().failed( "Could not create FTP Client" , e);
            LOG.error("Could not create FTP Client", e);
            return false;
        }


        try {

            client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(new LoggingOutputStream(LOG))));
            client.setControlEncoding("UTF-8");
            client.setDefaultTimeout(60000);
            client.setConnectTimeout(60000);
            client.connect(server, port);
            client.setSoTimeout(60000);
            client.setDataTimeout(60000);
            logServerReply(client);


            if (client.login(username, password)) {

                if(useFtps){
                    ((FTPSClient)client).execPBSZ(0);
                    logServerReply(client);
                    ((FTPSClient)client).execPROT("P");
                    logServerReply(client);
                }


                client.enterLocalPassiveMode();
                logServerReply(client);

                LOG.debug("Uploading file to FTP server " + server);
                LOG.debug("Checking for FTP directory " + directory);
                FTPFile[] existingDirectory = client.listFiles(directory);
                logServerReply(client);

                if (existingDirectory.length <= 0) {
                    LOG.debug("Attempting to create FTP directory " + directory);
                    ftpCreateDirectoryTree(client, directory);
                    logServerReply(client);
                }

                FileInputStream inputStream = new FileInputStream(gpxFile);
                client.changeWorkingDirectory(directory);
                client.setFileType(FTP.BINARY_FILE_TYPE);
                boolean result = client.storeFile(fileName, inputStream);
                inputStream.close();
                logServerReply(client);
                if (result) {
                    LOG.debug("Successfully FTPd file " + fileName);
                } else {
                    jobResult = new UploadEvents.Ftp().failed( "Failed to FTP file " + fileName , null);
                    LOG.debug("Failed to FTP file " + fileName);
                    return false;
                }

            } else {
                logServerReply(client);
                jobResult = new UploadEvents.Ftp().failed( "Could not log in to FTP server" , null);
                LOG.debug("Could not log in to FTP server");
                return false;
            }

        } catch (Exception e) {
            logServerReply(client);
            jobResult = new UploadEvents.Ftp().failed( "Could not connect or upload to FTP server.", e);
            LOG.error("Could not connect or upload to FTP server.", e);
            return false;
        } finally {
            try {
                client.logout();
                logServerReply(client);

                client.disconnect();
                logServerReply(client);
            } catch (Exception e) {
                if(jobResult == null){
                    jobResult = new UploadEvents.Ftp().failed( "Could not logout or disconnect", e);
                }

                LOG.error("Could not logout or disconnect", e);
                return false;
            }
        }

        return true;
    }

    private static void ftpCreateDirectoryTree(FTPClient client, String dirTree) throws IOException {

        boolean dirExists = true;

        //tokenize the string and attempt to change into each directory level.  If you cannot, then start creating.
        String[] directories = dirTree.split("/");
        for (String dir : directories) {
            if (dir.length() > 0) {
                if (dirExists) {
                    dirExists = client.changeWorkingDirectory(dir);
                    logServerReply(client);
                }
                if (!dirExists) {
                    client.makeDirectory(dir);
                    logServerReply(client);
                    client.changeWorkingDirectory(dir);
                    logServerReply(client);
                }
            }
        }
    }


    private static void logServerReply(FTPClient client) {
        String singleReply = client.getReplyString();
        if(!Strings.isNullOrEmpty(singleReply)){
            ftpServerResponses.add(singleReply);
        }

        String[] replies = client.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                if(!Strings.isNullOrEmpty(aReply)){
                    ftpServerResponses.add(aReply);
                }
            }
        }
    }
}
