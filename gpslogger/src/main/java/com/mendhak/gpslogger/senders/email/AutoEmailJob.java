package com.mendhak.gpslogger.senders.email;

import com.mendhak.gpslogger.common.events.AutoEmailEvent;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;
import java.io.File;


public class AutoEmailJob extends Job {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(AutoEmailJob.class.getSimpleName());
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


    protected AutoEmailJob(String smtpServer,
                           String smtpPort, String smtpUsername, String smtpPassword,
                           boolean smtpUseSsl, String csvEmailTargets, String fromAddress,
                            String subject, String body, File[] files) {
        super(new Params(1).requireNetwork().persist());
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
        Mail m = new Mail(smtpUsername, smtpPassword);

        String[] toArr = csvEmailTargets.split(",");
        m.setTo(toArr);

        if (fromAddress != null && fromAddress.length() > 0) {
            m.setFrom(fromAddress);
        } else {
            m.setFrom(smtpUsername);
        }

        m.setSubject(subject);
        m.setBody(body);

        m.setPort(smtpPort);
        m.setSecurePort(smtpPort);
        m.setSmtpHost(smtpServer);
        m.setSsl(smtpUseSsl);

        for (File f : files) {
            m.addAttachment(f.getName(), f.getAbsolutePath());
        }

        m.setDebuggable(false);

        tracer.info("Sending email...");
        if (m.send()) {
            EventBus.getDefault().post(new AutoEmailEvent(true));
        } else {
            EventBus.getDefault().post(new AutoEmailEvent(false));
        }
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new AutoEmailEvent(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }
}
