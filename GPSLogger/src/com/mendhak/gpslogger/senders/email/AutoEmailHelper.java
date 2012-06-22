package com.mendhak.gpslogger.senders.email;

import android.os.Environment;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;
import com.mendhak.gpslogger.senders.ZipHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AutoEmailHelper implements IActionListener, IFileSender
{


    private boolean forcedSend = false;
    IActionListener callback;

    public AutoEmailHelper(IActionListener callback)
    {
        this.callback = callback;
    }


    @Override
    public void UploadFile(String fileName)
    {
        this.forcedSend = true;

        File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

        if (!gpxFolder.exists())
        {
            OnFailure();
            return;
        }
        
        File chosenFile = new File(gpxFolder.getPath(), fileName);
        
        if(!chosenFile.exists())
        {
            OnFailure();
            return;
        }

        ArrayList<File> files = new ArrayList<File>();
        files.add(chosenFile);

        Thread t = new Thread(new AutoSendHandler(files.toArray(new File[files.size()]), this));
        t.start();


    }

    public void SendLogFile(final String currentFileName, boolean forcedSend, boolean sendZipFile)
    {
        this.forcedSend = forcedSend;


        File gpxFolder = new File(Environment.getExternalStorageDirectory(),
                "GPSLogger");

        if (!gpxFolder.exists())
        {
            OnFailure();
            return;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(gpxFolder.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File file, String s)
            {
                return s.contains(currentFileName) && !s.contains("zip");
            }
        })));

        if (files.size() == 0)
        {
            OnFailure();
            return;
        }

        if (sendZipFile)
        {
            File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");
            ArrayList<String> filePaths = new ArrayList<String>();

            for (File f : files)
            {
                filePaths.add(f.getAbsolutePath());
            }

            Utilities.LogInfo("Zipping file");
            ZipHelper zh = new ZipHelper(filePaths.toArray(new String[filePaths.size()]), zipFile.getAbsolutePath());
            zh.Zip();

            files.clear();
            files.add(zipFile);
        }

        Thread t = new Thread(new AutoSendHandler(files.toArray(new File[files.size()]), this));
        t.start();

    }

    void SendTestEmail(String smtpServer, String smtpPort,
                       String smtpUsername, String smtpPassword, boolean smtpUseSsl,
                       String emailTarget, String fromAddress, IActionListener helper)
    {

        Thread t = new Thread(new TestEmailHandler(helper, smtpServer,
                smtpPort, smtpUsername, smtpPassword, smtpUseSsl, emailTarget, fromAddress));
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

    File[] files;
    private final IActionListener helper;

    public AutoSendHandler(File[] files, IActionListener helper)
    {
        this.files = files;
        this.helper = helper;
    }

    public void run()
    {
        try
        {
            Mail m = new Mail(AppSettings.getSmtpUsername(),
                    AppSettings.getSmtpPassword());

            String csvEmailTargets = AppSettings.getAutoEmailTargets();
            String[] toArr = csvEmailTargets.split(",");

            m.setTo(toArr);
            m.setFrom(AppSettings.getSenderAddress());
            m.setSubject("GPS Log file generated at "
                    + Utilities.GetReadableDateTime(new Date()));
            m.setBody("GPS Log file generated at "
                    + Utilities.GetReadableDateTime(new Date()));

            m.setPort(AppSettings.getSmtpPort());
            m.setSecurePort(AppSettings.getSmtpPort());
            m.setSmtpHost(AppSettings.getSmtpServer());
            m.setSsl(AppSettings.isSmtpSsl());

            for (File f : files)
            {
                m.addAttachment(f.getName(), f.getAbsolutePath());
            }

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
    String csvEmailTargets;
    IActionListener helper;
    String fromAddress;

    public TestEmailHandler(IActionListener helper, String smtpServer,
                            String smtpPort, String smtpUsername, String smtpPassword,
                            boolean smtpUseSsl, String csvEmailTargets, String fromAddress)
    {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpPassword = smtpPassword;
        this.smtpUsername = smtpUsername;
        this.smtpUseSsl = smtpUseSsl;
        this.csvEmailTargets = csvEmailTargets;
        this.helper = helper;
        this.fromAddress = fromAddress;
    }

    public void run()
    {
        try
        {

            Mail m = new Mail(smtpUsername, smtpPassword);

            String[] toArr = csvEmailTargets.split(",");
            m.setTo(toArr);

            if (fromAddress != null && fromAddress.length() > 0)
            {
                m.setFrom(fromAddress);
            }
            else
            {
                m.setFrom(smtpUsername);
            }


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
