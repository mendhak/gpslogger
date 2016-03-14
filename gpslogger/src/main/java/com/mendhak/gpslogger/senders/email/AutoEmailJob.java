package com.mendhak.gpslogger.senders.email;

import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.smtp.*;
import org.slf4j.Logger;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;


public class AutoEmailJob extends Job {

    private static final Logger LOG = Logs.of(AutoEmailJob.class);
    String smtpServer;
    String smtpPort;
    String smtpUsername;
    String smtpPassword;
    boolean smtpUseSsl;
    String csvEmailTargets;
    String fromAddress;
    String subject;
    String body;
    File[] files;
    //Mail m;
    static ArrayList<String> smtpServerResponses;
    static UploadEvents.AutoEmail smtpFailureEvent;



    protected AutoEmailJob(String smtpServer,
                           String smtpPort, String smtpUsername, String smtpPassword,
                           boolean smtpUseSsl, String csvEmailTargets, String fromAddress,
                            String subject, String body, File[] files) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(files)));
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpPassword = smtpPassword;
        this.smtpUsername = smtpUsername;
        this.smtpUseSsl = smtpUseSsl;
        this.csvEmailTargets = csvEmailTargets;
        this.fromAddress = fromAddress;
        this.subject = subject;
        this.body = body;
        this.files = files;

        smtpServerResponses = new ArrayList<>();
        smtpFailureEvent = null;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        int port = Strings.toInt(smtpPort,25);

        if (Strings.isNullOrEmpty(fromAddress)) {
            fromAddress = smtpUsername;
        }

        AuthenticatingSMTPClient client = new AuthenticatingSMTPClient();

        try {

            client.addProtocolCommandListener(new ProtocolCommandListener() {
                @Override
                public void protocolCommandSent(ProtocolCommandEvent event) {
                    LOG.debug(event.getMessage());
                    smtpServerResponses.add(event.getMessage());
                }

                @Override
                public void protocolReplyReceived(ProtocolCommandEvent event) {
                    LOG.debug(event.getMessage());
                    smtpServerResponses.add(event.getMessage());
                }
            });

            if(smtpUseSsl){
                client = new AuthenticatingSMTPClient("TLS",true);
            }


            // optionally set a timeout to have a faster feedback on errors
            client.setDefaultTimeout(10 * 1000);
            checkReply(client);
            // you connect to the SMTP server
            client.connect(smtpServer, port);
            checkReply(client);
            // you say ehlo  and you specify the host you are connecting from, could be anything
            client.ehlo("localhost");
            checkReply(client);
            // if your host accepts STARTTLS, we're good everything will be encrypted, otherwise we're done here
            LOG.debug("Checking TLS...");

            boolean tlsAccepted = client.execTLS();

            client.auth(AuthenticatingSMTPClient.AUTH_METHOD.LOGIN, smtpUsername, smtpPassword);
            checkReply(client);

            client.setSender(fromAddress);
            checkReply(client);

            for (String target : csvEmailTargets.split(",")) {
                client.addRecipient(target);
            }

            checkReply(client);

            Writer writer = client.sendMessageData();

            if (writer != null) {
                SimpleSMTPHeader header = new SimpleSMTPHeader(fromAddress, csvEmailTargets.split(",")[0], subject);
                writer.write(header.toString());
                writer.write(body);
                writer.close();
                if (!client.completePendingCommand()) {// failure
                    smtpFailureEvent = new UploadEvents.AutoEmail().failed("Failure to send the email");
                }
                else {
                    EventBus.getDefault().post(new UploadEvents.AutoEmail().succeeded());
                }
            }
            else {
                smtpFailureEvent = new UploadEvents.AutoEmail().failed("Failure to send the email");
            }

        }
        catch (Exception e) {
            LOG.error("Could not send email ", e);
            smtpFailureEvent = new UploadEvents.AutoEmail().failed("Could not send email " + e.getMessage() , e);
        }
        finally {
            try{
                client.logout();
                client.disconnect();
            }
            catch (Exception ignored) {
            }

            if(smtpFailureEvent != null){
                smtpFailureEvent.smtpMessages = smtpServerResponses;
                EventBus.getDefault().post(smtpFailureEvent);
            }
        }



//        m = new Mail(smtpUsername, smtpPassword);
//
//        String[] toArr = csvEmailTargets.split(",");
//        m.setTo(toArr);
//
//        if (fromAddress != null && fromAddress.length() > 0) {
//            m.setFrom(fromAddress);
//        } else {
//            m.setFrom(smtpUsername);
//        }
//
//        m.setSubject(subject);
//        m.setBody(body);
//
//        m.setPort(smtpPort);
//        m.setSecurePort(smtpPort);
//        m.setSmtpHost(smtpServer);
//        m.setSsl(smtpUseSsl);
//
//        for (File f : files) {
//            m.addAttachment(f.getName(), f.getAbsolutePath());
//        }
//
//        m.setDebuggable(true);
//
//        LOG.info("Sending email...");
//        if (m.send()) {
//            EventBus.getDefault().post(new UploadEvents.AutoEmail().succeeded());
//        } else {
//            EventBus.getDefault().post(new UploadEvents.AutoEmail().failed());
//        }
    }


    private static void checkReply(SMTPClient sc) throws Exception {
        if (SMTPReply.isNegativeTransient(sc.getReplyCode())) {
            throw new Exception("Transient SMTP error " +  sc.getReplyString());
        } else if (SMTPReply.isNegativePermanent(sc.getReplyCode())) {
            throw new Exception("Permanent SMTP error " +  sc.getReplyString());
        }
    }

    @Override
    protected void onCancel() {
        LOG.debug("Email job cancelled");
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {

        LOG.error("Could not send email", throwable);
        //EventBus.getDefault().post(new UploadEvents.AutoEmail().failed(m.getSmtpMessages(), throwable));

        return false;
    }

    public static String getJobTag(File[] files) {
        StringBuilder sb = new StringBuilder();
        for(File f : files){
            sb.append(f.getName()).append(".");
        }
        return "EMAIL" + sb.toString();

    }
}
