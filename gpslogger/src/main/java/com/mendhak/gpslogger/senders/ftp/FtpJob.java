package com.mendhak.gpslogger.senders.ftp;


import com.mendhak.gpslogger.common.events.FtpEvent;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FtpJob extends Job {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(FtpJob.class.getSimpleName());

    String server;
    int port;
    String username;
    String password;
    boolean useFtps;
    String protocol;
    boolean implicit;
    File gpxFile;
    String fileName;
    String directory;

    protected FtpJob(String server, int port, String username,
                     String password, String directory, boolean useFtps, String protocol, boolean implicit,
                     File gpxFile, String fileName) {
        super(new Params(1).requireNetwork().persist());

        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useFtps = useFtps;
        this.protocol = protocol;
        this.implicit = implicit;
        this.gpxFile = gpxFile;
        this.fileName = fileName;
        this.directory = directory;

    }

    public synchronized static boolean Upload(String server, String username, String password, String directory, int port,
                                              boolean useFtps, String protocol, boolean implicit,
                                              File gpxFile, String fileName) {
        FTPClient client = null;

        try {
            if (useFtps) {
                client = new FTPSClient(protocol, implicit);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(null, null);
                KeyManager km = kmf.getKeyManagers()[0];
                ((FTPSClient) client).setKeyManager(km);
            } else {
                client = new FTPClient();
            }

        } catch (Exception e) {
            tracer.error("Could not create FTP Client", e);
            return false;
        }


        try {
            tracer.debug("Connecting to FTP");
            client.connect(server, port);
            showServerReply(client);

            tracer.debug("Logging in to FTP server");
            if (client.login(username, password)) {
                client.enterLocalPassiveMode();
                showServerReply(client);

                tracer.debug("Uploading file to FTP server " + server);

                tracer.debug("Checking for FTP directory " + directory);
                FTPFile[] existingDirectory = client.listFiles(directory);
                showServerReply(client);

                if (existingDirectory.length <= 0) {
                    tracer.debug("Attempting to create FTP directory " + directory);
                    //client.makeDirectory(directory);
                    ftpCreateDirectoryTree(client, directory);
                    showServerReply(client);
                }

                FileInputStream inputStream = new FileInputStream(gpxFile);
                client.changeWorkingDirectory(directory);
                boolean result = client.storeFile(fileName, inputStream);
                inputStream.close();
                showServerReply(client);
                if (result) {
                    tracer.debug("Successfully FTPd file " + fileName);
                } else {
                    tracer.debug("Failed to FTP file " + fileName);
                    return false;
                }

            } else {
                tracer.debug("Could not log in to FTP server");
                return false;
            }

        } catch (Exception e) {
            tracer.error("Could not connect or upload to FTP server.", e);
            return false;
        } finally {
            try {
                tracer.debug("Logging out of FTP server");
                client.logout();
                showServerReply(client);

                tracer.debug("Disconnecting from FTP server");
                client.disconnect();
                showServerReply(client);
            } catch (Exception e) {
                tracer.error("Could not logout or disconnect", e);
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
                    showServerReply(client);
                }
                if (!dirExists) {
                    client.makeDirectory(dir);
                    showServerReply(client);
                    client.changeWorkingDirectory(dir);
                    showServerReply(client);
                }
            }
        }
    }

    private static void showServerReply(FTPClient client) {
        String[] replies = client.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                tracer.debug("FTP SERVER: " + aReply);
            }
        }
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        if (Upload(server, username, password, directory, port, useFtps, protocol, implicit, gpxFile, fileName)) {
            EventBus.getDefault().post(new FtpEvent(true));
        } else {
            EventBus.getDefault().post(new FtpEvent(false));
        }
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new FtpEvent(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not FTP file", throwable);
        return false;
    }
}
