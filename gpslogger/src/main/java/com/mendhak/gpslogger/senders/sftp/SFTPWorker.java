package com.mendhak.gpslogger.senders.sftp;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;


import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.security.Security;
import java.util.Properties;

import de.greenrobot.event.EventBus;

public class SFTPWorker extends Worker {

    private static final Logger LOG = Logs.of(SFTPWorker.class);

    public SFTPWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {

        super(context, workerParams);

        try {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
        catch(Exception ex){
            LOG.error("Could not add BouncyCastle provider.", ex);
        }
    }

    @NonNull
    @Override
    public Result doWork() {

        String filePath = getInputData().getString("filePath");
        if(Strings.isNullOrEmpty(filePath)) {
            LOG.error("No file path provided to upload to SFTP");
            EventBus.getDefault().post(new UploadEvents.SFTP().failed("No file path provided to upload to SFTP", null));
            return Result.failure();
        }
        File fileToUpload = new File(filePath);

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        String hostKey = preferenceHelper.getSFTPKnownHostKey();
        String host = preferenceHelper.getSFTPHost();
        String username = preferenceHelper.getSFTPUser();
        String password = preferenceHelper.getSFTPPassword();
        int port = preferenceHelper.getSFTPPort();
        String remoteDir = preferenceHelper.getSFTPRemoteServerPath();
        String pathToPrivateKey = preferenceHelper.getSFTPPrivateKeyFilePath();
        String privateKeyPassphrase = preferenceHelper.getSFTPPrivateKeyPassphrase();

        LOG.debug("SFTP Job onRun");
        com.jcraft.jsch.Session session = null;
        JSch.setLogger(new SftpLogger());
        final JSch jsch = new JSch();
        FileInputStream fis = null;

        try {
            String keystring = hostKey;

            if (!Strings.isNullOrEmpty(keystring)) {
                byte[] key = Base64.decode(keystring, Base64.DEFAULT);
                jsch.getHostKeyRepository().add(new HostKey(host, key), null);
            }

            if(!Strings.isNullOrEmpty(pathToPrivateKey)){
                jsch.addIdentity(pathToPrivateKey, privateKeyPassphrase);
            }

            session = jsch.getSession(username, host, port);
            session.setPassword(password);

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
                LOG.debug("Changing directory to " + remoteDir);
                channelSftp.cd(remoteDir);
                LOG.debug("Uploading " + fileToUpload.getName() + " to remote server");
                channelSftp.put(new FileInputStream(fileToUpload), fileToUpload.getName(), ChannelSftp.OVERWRITE);

                LOG.debug("Disconnecting");
                channelSftp.disconnect();
                channel.disconnect();
                session.disconnect();

                LOG.info("SFTP - file uploaded");
                EventBus.getDefault().post(new UploadEvents.SFTP().succeeded());
                return Result.success();
            } else {
                EventBus.getDefault().post(new UploadEvents.SFTP().failed("Could not connect, unknown reasons", null));
                return Result.failure();
            }

        } catch (SftpException sftpex) {
            LOG.error(sftpex.getMessage(), sftpex);
            EventBus.getDefault().post(new UploadEvents.SFTP().failed(sftpex.getMessage(), sftpex));
            return Result.failure();
        } catch (final JSchException jex) {
            LOG.error(jex.getMessage(), jex);

            if (jex.getMessage().contains("reject HostKey") || jex.getMessage().contains("HostKey has been changed")) {
                LOG.debug(session.getHostKey().getKey());
                UploadEvents.SFTP sftpException = new UploadEvents.SFTP();
                sftpException.hostKey = session.getHostKey().getKey();
                sftpException.fingerprint = session.getHostKey().getFingerPrint(jsch);
                EventBus.getDefault().post(sftpException.failed(jex.getMessage(), jex));
            } else {
                EventBus.getDefault().post(new UploadEvents.SFTP().failed(jex.getMessage(), jex));
            }

            return Result.failure();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            EventBus.getDefault().post(new UploadEvents.SFTP().failed(ex.getMessage(), ex));
            return Result.failure();
        } finally {
            try {
                fis.close();
            } catch (Exception ee) {
            }
        }
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
