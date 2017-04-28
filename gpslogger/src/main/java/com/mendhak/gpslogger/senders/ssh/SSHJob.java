package com.mendhak.gpslogger.senders.ssh;


import android.graphics.Path;
import android.util.Base64;
import com.jcraft.jsch.*;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
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
    private final String remoteDir;

    static ArrayList<String> sshServerResponses;


    public SSHJob(File localFile, String remoteDir, String host, int port, String pathToPrivateKey, String privateKeyPassphrase, String username, String password, String hostKey)
    {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(localFile)));
        this.localFile = localFile;
        this.remoteDir = remoteDir;
        this.host = host;
        this.port = port;
        this.pathToPrivateKey = pathToPrivateKey;
        this.privateKeyPassphrase = privateKeyPassphrase;
        this.username = username;
        this.password = password;
        this.hostKey = hostKey;

        sshServerResponses = new ArrayList<>();
    }


    @Override
    public void onAdded() {

        LOG.debug("SSH Job added");
    }

    @Override
    public void onRun() throws Throwable {
        LOG.debug("SSH Job onRun");
        com.jcraft.jsch.Session session = null;
        final JSch jsch = new JSch();
        FileInputStream fis = null;

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


                boolean ptimestamp = true;

                // exec 'scp -t rfile' remotely
                File remoteDir = new File(this.remoteDir, this.localFile.getName());
                LOG.debug("Upload to " + remoteDir.getAbsolutePath());
                String command="scp " + (ptimestamp ? "-p" :"") +" -t "+ remoteDir.getAbsolutePath();
                Channel channel=session.openChannel("exec");
                ((ChannelExec)channel).setCommand(command);

                // get I/O streams for remote scp
                OutputStream out=channel.getOutputStream();
                InputStream in=channel.getInputStream();

                channel.connect();

                int status = checkAck(in);
                if(status != 0){
                    UploadEvents.SSH sshException = new UploadEvents.SSH();
                    sshException.sshMessages = sshServerResponses;
                    EventBus.getDefault().post(sshException.failed("Could not get I/O stream for remote SCP"));
                    return;
                }

                //this.localFile

//                command="T "+(this.localFile.lastModified()/1000)+" 0";
//                // The access time should be sent here,
//                // but it is not accessible with JavaAPI ;-<
//                command+=(" "+(this.localFile.lastModified()/1000)+" 0\n");
//                out.write(command.getBytes());
//                out.flush();
//                if(checkAck(in)!=0){
//                    EventBus.getDefault().post(new UploadEvents.SSH().failed("Could not get MTIME for file"));
//                    return;
//                }

                long filesize=this.localFile.length();
                command="C0644 "+filesize+" "+this.localFile.getName();

                command+="\n";
                out.write(command.getBytes());
                out.flush();
                if(checkAck(in)!=0){
                    UploadEvents.SSH sshException = new UploadEvents.SSH();
                    sshException.sshMessages = sshServerResponses;
                    EventBus.getDefault().post(sshException.failed("Could not initiate C0644 command"));
                    return;
                }

                // send a content of lfile
                fis=new FileInputStream(this.localFile);
                byte[] buf=new byte[1024];
                while(true){
                    int len=fis.read(buf, 0, buf.length);
                    if(len<=0) break;
                    out.write(buf, 0, len); //out.flush();
                }
                fis.close();
                fis=null;
                // send '\0'
                buf[0]=0; out.write(buf, 0, 1);
                out.flush();
                if(checkAck(in)!=0){
                    UploadEvents.SSH sshException = new UploadEvents.SSH();
                    sshException.sshMessages = sshServerResponses;
                    EventBus.getDefault().post(sshException.failed("Could not send contents of file"));
                    return;
                }
                out.close();

                channel.disconnect();
                session.disconnect();



               // LOG.debug(this.getClass().getSimpleName() + " - " + jsch.getIdentityRepository().getName() + " " + session.getClientVersion() + " " + session.isConnected());
                EventBus.getDefault().post(new UploadEvents.SSH().succeeded());
                //session.disconnect();
            } else {
                LOG.debug(this.getClass().getSimpleName() + " NOT CONNECTED");
                EventBus.getDefault().post(new UploadEvents.SSH().failed("Could not connect, unknown reasons", null));
            }

        } catch (final JSchException jex) {

            try{if(fis!=null)fis.close();}catch(Exception ee){}

            LOG.error("", jex);
            if(jex.getMessage().contains("reject HostKey") || jex.getMessage().contains("HostKey has been changed")){
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

    int checkAck(InputStream in) throws IOException {

        int b=in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if(b==0) return b;
        if(b==-1) return b;

        if(b==1 || b==2){
            StringBuffer sb=new StringBuffer();

            int c;

            do {
                c=in.read();
                sb.append((char)c);
            } while(c!='\n');

            LOG.debug(sb.toString());
            sshServerResponses.add(sb.toString());

            if(b==1){ // error
                LOG.error(sb.toString());
            }
            if(b==2){ // fatal error
                LOG.error(sb.toString());
            }
        }
        return b;
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
