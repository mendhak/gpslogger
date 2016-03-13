package com.mendhak.gpslogger.senders.email;

import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SimpleSMTPHeader;
import org.slf4j.Logger;
import org.apache.commons.net.smtp.SMTPClient;

import java.io.File;
import java.io.Writer;


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
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        int port = Strings.toInt(smtpPort,25);

        if (!Strings.isNullOrEmpty(fromAddress)) {
            fromAddress = smtpUsername;
        }

        AuthenticatingSMTPClient client = new AuthenticatingSMTPClient();
        try {
            String to = "recipient@email.com";
            // optionally set a timeout to have a faster feedback on errors
            client.setDefaultTimeout(10 * 1000);
            // you connect to the SMTP server
            client.connect(smtpServer, port);
            // you say ehlo  and you specify the host you are connecting from, could be anything
            client.ehlo("localhost");
            // if your host accepts STARTTLS, we're good everything will be encrypted, otherwise we're done here
            if (client.execTLS()) {

                client.auth(AuthenticatingSMTPClient.AUTH_METHOD.LOGIN, smtpUsername, smtpPassword);
                checkReply(client);

                client.setSender(fromAddress);
                checkReply(client);

                client.addRecipient(to);
                checkReply(client);

                Writer writer = client.sendMessageData();

                if (writer != null) {
                    SimpleSMTPHeader header = new SimpleSMTPHeader(fromAddress, to, subject);
                    writer.write(header.toString());
                    writer.write(body);
                    writer.close();
                    if(!client.completePendingCommand()) {// failure
                        throw new Exception("Failure to send the email "+ client.getReply() + client.getReplyString());
                    }
                } else {
                    throw new Exception("Failure to send the email "+ client.getReply() + client.getReplyString());
                }
            } else {
                throw new Exception("STARTTLS was not accepted "+ client.getReply() + client.getReplyString());
            }
        } catch (Exception e) {
            throw e;
        } finally {
            client.logout();
            client.disconnect();
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
            throw new Exception("Transient SMTP error " + sc.getReply() + sc.getReplyString());
        } else if (SMTPReply.isNegativePermanent(sc.getReplyCode())) {
            throw new Exception("Permanent SMTP error " + sc.getReply() + sc.getReplyString());
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
