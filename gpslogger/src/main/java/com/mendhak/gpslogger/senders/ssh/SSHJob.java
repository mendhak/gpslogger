package com.mendhak.gpslogger.senders.ssh;


import android.util.Base64;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;
import java.util.Properties;

public class SSHJob  extends Job  {
    private static final Logger LOG = Logs.of(SSHJob.class);
    private final File localFile;
    private final String host;
    private final int port;
    private final String pathToPrivateKey;
    private final String privateKeyPassphrase;
    private final String username;
    private final String password;
    private final String hostKey;


    public SSHJob(File localFile, String host, int port, String pathToPrivateKey, String privateKeyPassphrase, String username, String password, String hostKey)
    {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(localFile)));
        this.localFile = localFile;
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

    }

    @Override
    public void onRun() throws Throwable {
        com.jcraft.jsch.Session session = null;
        final JSch jsch = new JSch();

        try {
            String keystring = this.hostKey;

            if (!Strings.isNullOrEmpty(keystring)) {
                byte[] key = Base64.decode(keystring, Base64.DEFAULT);
                jsch.getHostKeyRepository().add(new HostKey(host, key), null);
            }

            jsch.addIdentity(this.pathToPrivateKey, this.privateKeyPassphrase);

            session = jsch.getSession(this.username, this.host, this.port);

            session.setPassword(this.password);

            // Avoid asking for key confirmation
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "yes");
            session.setConfig(prop);

            session.connect();

            if (session.isConnected()) {

                LOG.debug(this.getClass().getSimpleName() + " CONNECTED");
                LOG.debug(this.getClass().getSimpleName() + " YOO " + jsch.getIdentityRepository().getName() + " " + session.getClientVersion() + " " + session.isConnected());
                EventBus.getDefault().post(new UploadEvents.SSH().succeeded());
                session.disconnect();
            } else {
                LOG.debug(this.getClass().getSimpleName() + " NOT CONNECTED");
                EventBus.getDefault().post(new UploadEvents.SSH().failed("Could not connect, unknown reasons", null));
            }

        } catch (final JSchException jex) {
            LOG.error("", jex);
            if(jex.getMessage().contains("reject HostKey")){
                LOG.debug(session.getHostKey().getKey());
                UploadEvents.SSH sshException = new UploadEvents.SSH();
                sshException.hostKey = session.getHostKey().getKey();
                sshException.fingerprint = session.getHostKey().getFingerPrint(jsch);
                EventBus.getDefault().post(sshException.failed(jex.getMessage(), jex));
            }
            else {
                throw jex;
            }

        }

    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        LOG.error("Could not upload to SSH server", throwable);
        EventBus.getDefault().post(new UploadEvents.SSH().failed(throwable.getMessage(), throwable));
        return false;
    }

    public static String getJobTag(File gpxFile) {
        return "SSH" + gpxFile.getName();
    }
}
