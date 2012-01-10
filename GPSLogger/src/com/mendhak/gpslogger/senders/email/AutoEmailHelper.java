package com.mendhak.gpslogger.senders.email;

import android.os.Environment;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.ZipHelper;

import java.io.File;
import java.util.Date;

public class AutoEmailHelper implements IActionListener
{


    private boolean forcedSend = false;
    IActionListener callback;

    public AutoEmailHelper(IActionListener callback)
    {
        this.callback = callback;
    }

    public void SendLogFile(String currentFileName, boolean forcedSend)
    {
        this.forcedSend = forcedSend;


        File gpxFolder = new File(Environment.getExternalStorageDirectory(),
                "GPSLogger");

        if (!gpxFolder.exists())
        {
            OnFailure();
            return;
        }

        File gpxFile = new File(gpxFolder.getPath(), currentFileName + ".gpx");
        File kmlFile = new File(gpxFolder.getPath(), currentFileName + ".kml");

        File foundFile = null;

        if (kmlFile.exists())
        {
            foundFile = kmlFile;
        }
        if (gpxFile.exists())
        {
            foundFile = gpxFile;
        }

        if (foundFile == null)
        {
            OnFailure();
            return;
        }

        String[] files = new String[]
                {foundFile.getAbsolutePath()};
        File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");

        Utilities.LogInfo("Zipping file");
        ZipHelper zh = new ZipHelper(files, zipFile.getAbsolutePath());
        zh.Zip();

        Thread t = new Thread(new AutoSendHandler(zipFile, this));
        t.start();

    }

    void SendTestEmail(String smtpServer, String smtpPort,
                       String smtpUsername, String smtpPassword, boolean smtpUseSsl,
                       String emailTarget, IActionListener helper)
    {

        Thread t = new Thread(new TestEmailHandler(helper, smtpServer,
                smtpPort, smtpUsername, smtpPassword, smtpUseSsl, emailTarget));
        t.start();
    }


    public void OnComplete()
    {
        // This was a success
        Utilities.LogInfo("Email sent");

        if (!forcedSend)
        {
            Utilities.LogDebug("setEmailReadyToBeSent = false");
            Session.setEmailReadyToBeSent(false);
        }
        callback.OnComplete();
    }

    public void OnFailure()
    {
        callback.OnFailure();
    }
}

class AutoSendHandler implements Runnable
{

    final File zipFile;
    private final IActionListener helper;

    public AutoSendHandler(File zipFile, IActionListener helper)
    {
        this.zipFile = zipFile;
        this.helper = helper;
    }

    public void run()
    {
        try
        {
            Mail m = new Mail(AppSettings.getSmtpUsername(),
                    AppSettings.getSmtpPassword());

            String[] toArr =
                    {AppSettings.getAutoEmailTarget()};
            m.setTo(toArr);
            m.setFrom(AppSettings.getSmtpUsername());
            m.setSubject("GPS Log file generated at "
                    + Utilities.GetReadableDateTime(new Date()) + " - "
                    + zipFile.getName());
            m.setBody(zipFile.getName());


            m.setPort(AppSettings.getSmtpPort());
            m.setSecurePort(AppSettings.getSmtpPort());
            m.setSmtpHost(AppSettings.getSmtpServer());
            m.setSsl(AppSettings.isSmtpSsl());
            m.addAttachment(zipFile.getAbsolutePath());

            Utilities.LogInfo("Sending email...");

            if (m.send())
            {
                helper.OnComplete();
            }
            else
            {
                helper.OnFailure();
            }
        }
        catch (Exception e)
        {
            helper.OnFailure();
            Utilities.LogError("AutoSendHandler.run", e);
        }

    }

}

class TestEmailHandler implements Runnable
{
    String smtpServer;
    String smtpPort;
    String smtpUsername;
    String smtpPassword;
    boolean smtpUseSsl;
    String emailTarget;
    IActionListener helper;

    public TestEmailHandler(IActionListener helper, String smtpServer,
                            String smtpPort, String smtpUsername, String smtpPassword,
                            boolean smtpUseSsl, String emailTarget)
    {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpPassword = smtpPassword;
        this.smtpUsername = smtpUsername;
        this.smtpUseSsl = smtpUseSsl;
        this.emailTarget = emailTarget;
        this.helper = helper;
    }

    public void run()
    {
        try
        {

            Mail m = new Mail(smtpUsername, smtpPassword);

            String[] toArr =
                    {emailTarget};
            m.setTo(toArr);
            m.setFrom(smtpUsername);
            m.setSubject("Test Email from GPSLogger at "
                    + Utilities.GetReadableDateTime(new Date()));
            m.setBody("Test Email from GPSLogger at "
                    + Utilities.GetReadableDateTime(new Date()));

            m.setPort(smtpPort);
            m.setSecurePort(smtpPort);
            m.setSmtpHost(smtpServer);
            m.setSsl(smtpUseSsl);
            m.setDebuggable(true);

            Utilities.LogInfo("Sending email...");
            if (m.send())
            {
                helper.OnComplete();
            }
            else
            {
                helper.OnFailure();
            }
        }
        catch (Exception e)
        {
            helper.OnFailure();
            Utilities.LogError("AutoSendHandler.run", e);
        }

    }
}
