package com.mendhak.gpslogger.senders.sftp;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.jcraft.jsch.*;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import de.greenrobot.event.EventBus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import java.io.*;
import java.security.Security;
import java.util.Properties;

public class SFTPJob extends Job {
    private static final Logger LOG = Logs.of(SFTPJob.class);
    private final File localFile;
    private final String host;
    private final int port;
    private final String pathToPrivateKey;
    private final String privateKeyPassphrase;
    private final String username;
    private final String password;
    private final String hostKey;
    private final String remoteDir;

    public SFTPJob(File localFile, String remoteDir, String host, int port, String pathToPrivateKey, String privateKeyPassphrase, String username, String password, String hostKey) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(localFile)));

        try {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
        catch(Exception ex){
            LOG.error("Could not add BouncyCastle provider.", ex);
        }

        this.localFile = localFile;
        this.remoteDir = remoteDir;
        this.host = host;
        this.port = port;
        this.pathToPrivateKey = pathToPrivateKey;
        this.privateKeyPassphrase = privateKeyPassphrase;
        this.username = username;
        this.password = password;
        this.hostKey = hostKey;
    }


    @Override
    public void onAdded() {
        LOG.debug("SFTP Job added");
    }

    @Override
    public void onRun() throws Throwable {
        LOG.debug("SFTP Job onRun");
        com.jcraft.jsch.Session session = null;
        JSch.setLogger(new SftpLogger());
        final JSch jsch = new JSch();
        FileInputStream fis = null;

        try {
            String keystring = this.hostKey;

            if (!Strings.isNullOrEmpty(keystring)) {
                byte[] key = Base64.decode(keystring, Base64.DEFAULT);
                jsch.getHostKeyRepository().add(new HostKey(host, key), null);
            }

            if(!Strings.isNullOrEmpty(this.pathToPrivateKey)){
                jsch.addIdentity(this.pathToPrivateKey, this.privateKeyPassphrase);
            }

            session = jsch.getSession(this.username, this.host, this.port);
            session.setPassword(this.password);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "yes");
            session.setConfig(prop);

            LOG.debug("Connecting...");
            session.connect();

            if (session.isConnected()) {

                LOG.debug("Connected, opening SFTP channel");
                Channel channel = session.openChannel("sftp");
                channel.connect();
                ChannelSftp channelSftp = (ChannelSftp) channel;
                LOG.debug("Changing directory to " + this.remoteDir);
                channelSftp.cd(this.remoteDir);
                LOG.debug("Uploading " + this.localFile.getName() + " to remote server");
                channelSftp.put(new FileInputStream(this.localFile), this.localFile.getName(), ChannelSftp.OVERWRITE);

                LOG.debug("Disconnecting");
                channelSftp.disconnect();
                channel.disconnect();
                session.disconnect();

                LOG.info("SFTP - file {} uploaded", this.localFile.getName());
                EventBus.getDefault().post(new UploadEvents.SFTP().succeeded());
            } else {
                EventBus.getDefault().post(new UploadEvents.SFTP().failed("Could not connect, unknown reasons", null));
            }

        } catch (SftpException sftpex) {
            LOG.error(sftpex.getMessage(), sftpex);
            EventBus.getDefault().post(new UploadEvents.SFTP().failed(sftpex.getMessage(), sftpex));
        } catch (final JSchException jex) {
            LOG.error(jex.getMessage(), jex);
            if (jex.getMessage().contains("reject HostKey") || jex.getMessage().contains("HostKey has been changed")) {
                LOG.debug(session.getHostKey().getKey());
                UploadEvents.SFTP sftpException = new UploadEvents.SFTP();
                sftpException.hostKey = session.getHostKey().getKey();
                sftpException.fingerprint = session.getHostKey().getFingerPrint(jsch);
                EventBus.getDefault().post(sftpException.failed(jex.getMessage(), jex));
            } else {
                throw jex;
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            EventBus.getDefault().post(new UploadEvents.SFTP().failed(ex.getMessage(), ex));
        } finally {
            try {
                fis.close();
            } catch (Exception ee) {
            }
        }
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        LOG.debug("SFTP Job Cancelled");
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        LOG.error("Could not upload to SFTP server", throwable);
        EventBus.getDefault().post(new UploadEvents.SFTP().failed(throwable.getMessage(), throwable));
        return RetryConstraint.CANCEL;
    }


    public static String getJobTag(File gpxFile) {
        return "SFTP" + gpxFile.getName();
    }


    public static class SftpLogger implements com.jcraft.jsch.Logger {

        public boolean isEnabled(int level){
            return true;
        }
        public void log(int level, String message){
            switch(level){
                case FATAL:
                case ERROR:
                    LOG.error(message);
                    break;
                case WARN:
                case INFO:
                case DEBUG:
                    LOG.debug(message);
                    break;
            }
        }
    }

}


